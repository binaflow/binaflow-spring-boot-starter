package com.github.binaflow.service;

import com.github.binaflow.dto.BaseMessage;
import com.github.binaflow.dto.Ping;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.github.binaflow.annotation.Controller;
import com.github.binaflow.annotation.MessageMapping;
import com.github.binaflow.BinaFlowProperties;
import com.github.binaflow.exception.EmptyMessageTypeException;
import com.github.binaflow.exception.MessageTypeNotFoundException;
import com.github.binaflow.exception.BinaFlowException;
import com.github.binaflow.util.StackTraceUtils;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The core of BinaFlow.
 * <p>Responsible for:<br>
 * - Loading message types from protobuf schemas.<br>
 * - Loading controllers and their methods.<br>
 * - Handling incoming messages.
 */
@Slf4j
@RequiredArgsConstructor
public class BinaFlowService extends BinaryWebSocketHandler {

    private final BinaFlowProperties properties;
    private final ApplicationContext applicationContext;
    private final Pattern messageTypePattern = Pattern.compile(".*message\\s+([a-zA-Z][a-zA-Z\\d]*)\\s*\\{.*");
    private final Map<String, MessageTypeMapping> messageTypeMappings = new HashMap<>(); // Key - simple class name

    @Override
    protected void handleBinaryMessage(WebSocketSession webSocketSession, BinaryMessage message) {
        String messageId = null;
        String messageType = "Undefined";
        try {
            if (!(webSocketSession instanceof StandardWebSocketSession)) {
                log.warn("Only StandardWebSocketSession supported, but used {}", webSocketSession.getClass());
                return;
            }
            var baseMessage = BaseMessage.parseFrom(message.getPayload());
            messageId = baseMessage.getMessageId();
            if (!StringUtils.hasText(baseMessage.getMessageType())) {
                log.warn("Message type is empty. MessageId {}", messageId);
                var emptyMessageTypeException = new EmptyMessageTypeException();
                emptyMessageTypeException.getProblemDetail().setStatus(400);
                emptyMessageTypeException.getProblemDetail().setDetail("Client sent empty message type field");
                throw emptyMessageTypeException;
            }
            messageType = baseMessage.getMessageType();
            var messageTypeMapping = messageTypeMappings.get(baseMessage.getMessageType());
            if (messageTypeMapping == null) {
                log.warn("Message type '{}' not found. MessageId {}", baseMessage.getMessageType(), messageId);
                throw new MessageTypeNotFoundException(baseMessage.getMessageType());
            }
            var typedMessage = messageTypeMapping.parseFromMethod.invoke(null, message.getPayload());
            log.trace("Received message: {}", typedMessage);
            Object response = null;
            var methodParameterCount = messageTypeMapping.handlerMethod.getParameterCount();
            if (methodParameterCount == 1) {
                response = messageTypeMapping.handlerMethod.invoke(messageTypeMapping.bean, typedMessage);
            } else if (methodParameterCount == 2) {
                response = messageTypeMapping.handlerMethod.invoke(messageTypeMapping.bean, typedMessage, webSocketSession);
            }
            if (response != null) {
                respond((GeneratedMessage) response, webSocketSession);
            }
        } catch (BinaFlowException binaFlowException) {
            binaFlowException.setMessageId(messageId);
            respondWithError(binaFlowException, webSocketSession);
        } catch (Exception e) {
            log.error("Unhandled exception. MessageId={}, MessageType={}", messageId, messageType, e);
            var binaflowException = new BinaFlowException("Unhandled exception.", e);
            binaflowException.setMessageId(messageId);
            var problemDetail = binaflowException.getProblemDetail();
            problemDetail.setTitle(properties.getUnhandledExceptions().getFillMessage() ? e.getMessage() : "Unhandled exception");
            problemDetail.setDetail("Unhandled exception" +
                                    (properties.getUnhandledExceptions().getFillMessage() ? "\nMessage: " + e.getMessage() : "") +
                                    (properties.getUnhandledExceptions().getFillExceptionClass() ? "\nException class: " + e.getClass() : "") +
                                    (properties.getUnhandledExceptions().getFillStackTrace() ? "\nStack trace: " + StackTraceUtils.toString(e.getStackTrace()) : ""));
            problemDetail.setInstance(URI.create(properties.getHttpPath() + "#" + messageType));
            respondWithError(binaflowException, webSocketSession);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        loadMessageTypesFromProtoSchemaFiles();
        loadParseMethodsForMessageTypes();
        loadControllers();
    }

    /**
     * Reading all *.proto files in binaflow.schema.directory.
     * For every file searching java_package line to define package,
     * then search all messages and add this info in to messageTypeMappings
     */
    private void loadMessageTypesFromProtoSchemaFiles() {
        try {
            messageTypeMappings.put("Ping", new MessageTypeMapping(Ping.class.getName(), null, null, Ping.class.getMethod("parseFrom", ByteBuffer.class)));
        } catch (NoSuchMethodException e) {
            // Not possible situation. Just catch it to avoid compilation error.
            throw new RuntimeException(e);
        }
        var schemaDirectory = properties.getSchema().getDirectory();
        if (StringUtils.hasText(schemaDirectory)) {
            try {
                var path = Path.of(schemaDirectory);
                log.info("Searching .proto files in directory '{}'", schemaDirectory);
                try (var directoryStream = Files.newDirectoryStream(path, "*.proto")) {
                    for (var schemaFile : directoryStream) {
                        log.debug("Searching java_package in '{}' file", schemaFile.getFileName());
                        var schemaFileLines = Files.readAllLines(schemaFile);
                        String dtoJavaPackage = null;
                        for (var schemaFileLine : schemaFileLines) {
                            if (schemaFileLine.matches(".*option\\s+java_package.*")) {
                                int startIndex = schemaFileLine.indexOf("\"") + 1;
                                int endIndex = schemaFileLine.lastIndexOf("\"");
                                dtoJavaPackage = schemaFileLine.substring(startIndex, endIndex);
                                log.debug("For schema '{}' dtoJavaPackage is '{}'", schemaFile.getFileName(), dtoJavaPackage);
                                break;
                            }
                        }
                        if (dtoJavaPackage == null) {
                            log.error("For schema '{}' java_package is not found.", schemaFile.getFileName());
                            System.exit(200);
                        }
                        for (var schemaFileLine : schemaFileLines) {
                            var matcher = messageTypePattern.matcher(schemaFileLine);
                            if (matcher.find()) {
                                var messageType = matcher.group(1);
                                var className = dtoJavaPackage + "." + messageType;
                                messageTypeMappings.put(messageType, new MessageTypeMapping(className, null, null, null));
                            }
                        }
                    }
                }
            } catch (InvalidPathException e) {
                log.error("Schema directory '{}' incorrect. Please check property binaflow.schema.directory", schemaDirectory, e);
                System.exit(201);
            } catch (IOException e) {
                log.error("Error while read proto schema files.", e);
                System.exit(202);
            }
        } else {
            log.error("Schema directory is empty. Please check property binaflow.schema.directory");
            System.exit(203);
        }

        log.info("Detected message types: {}", messageTypeMappings.keySet());
    }

    /**
     * For each messageTypeMapping load class by class name and find 'parseFrom' method and save it into map.
     */
    private void loadParseMethodsForMessageTypes() {
        for (var messageType : messageTypeMappings.keySet()) {
            if (messageType.equals("Ping")) {
                continue;
            }
            log.debug("Load class for message type '{}'", messageType);
            try {
                var messageClass = Class.forName(messageTypeMappings.get(messageType).className);
                messageTypeMappings.get(messageType).parseFromMethod = messageClass.getMethod("parseFrom", ByteBuffer.class);
            } catch (ClassNotFoundException e) {
                log.error("Class '{}' not found.", messageTypeMappings.get(messageType).className, e);
                System.exit(204);
            } catch (NoSuchMethodException e) {
                log.error("Method 'parseFrom' not found in in class '{}'", messageTypeMappings.get(messageType).className, e);
                System.exit(205);
            }
        }
    }

    /**
     * Load all classes marked as {@link Controller} and their methods marked as {@link MessageMapping},
     * then save bean and method in messageTypeMappings
     */
    private void loadControllers() {
        var controllerBeans = applicationContext.getBeansWithAnnotation(Controller.class);
        for (var controllerEntry : controllerBeans.entrySet()) {
            var methods = controllerEntry.getValue().getClass().getMethods();
            for (var method : methods) {
                if (method.getAnnotation(MessageMapping.class) == null) {
                    continue;
                }
                var methodParameters = method.getParameters();
                if (methodParameters == null) {
                    log.error("Binding error for mapping in '{}'.\n(method has not parameters)", method);
                    System.exit(206);
                }
                String requestMessageTypeClassSimpleName = null;
                if (GeneratedMessage.class.isAssignableFrom(methodParameters[0].getType())) {
                    requestMessageTypeClassSimpleName = methodParameters[0].getType().getSimpleName();
                } else {
                    log.error("Binding error for mapping in '{}'.\n(First parameter (request message type) must be proto class)", method);
                    System.exit(207);
                }
                if (method.getReturnType().equals(Void.class) || GeneratedMessage.class.isAssignableFrom(method.getReturnType())) {
                    log.trace("Method '{}' return type is '{}'", method.getName(), method.getReturnType());
                } else {
                    log.error("Binding error for mapping in '{}'.\n(Return type (response message type) must be proto class)", method);
                    System.exit(208);
                }
                if (methodParameters.length >= 2 && !WebSocketSession.class.isAssignableFrom(methodParameters[1].getType())) {
                    log.error("Binding error for mapping in '{}'.\n(Second parameter must be WebSocketSession)", method);
                    System.exit(209);
                }
                var messageTypeMapping = messageTypeMappings.get(requestMessageTypeClassSimpleName);
                if (messageTypeMapping == null) {
                    log.error("Binding error for mapping in '{}'.\n(request message type={} is not correlated to any message type from schema)",
                            method,
                            requestMessageTypeClassSimpleName
                    );
                    System.exit(210);
                }
                if (messageTypeMapping.bean != null && messageTypeMapping.handlerMethod != null) {
                    log.error("Duplicated handler for message type '{}'\n1) {}\n2) {}",
                            requestMessageTypeClassSimpleName,
                            method,
                            messageTypeMapping.handlerMethod
                    );
                    System.exit(211);
                }
                messageTypeMapping.bean = controllerEntry.getValue();
                messageTypeMapping.handlerMethod = method;
                log.debug("Handler for message type '{}' registered in '{}'",
                        requestMessageTypeClassSimpleName,
                        messageTypeMapping.handlerMethod
                );
            }
        }
    }

    private void respond(Message message, WebSocketSession webSocketSession) {
        try {
            var messageTypeFieldDescriptor = message.getDescriptorForType().findFieldByNumber(1);
            var messageTypeSimpleClassName = message.getClass().getSimpleName();
            if (!messageTypeSimpleClassName.equals(message.getField(messageTypeFieldDescriptor).toString())) {
                // if users code set wrong message type, let's fix it for him
                message = message.toBuilder().setField(messageTypeFieldDescriptor, messageTypeSimpleClassName).build();
            }
            log.trace("Responding with message. {}", message);
            webSocketSession.sendMessage(new BinaryMessage(message.toByteArray()));
        } catch (IOException e) {
            throw new BinaFlowException("Error while send response", e);
        }
    }

    private void respondWithError(BinaFlowException binaFlowException, WebSocketSession webSocketSession) {
        try {
            webSocketSession.sendMessage(new BinaryMessage(binaFlowException.toErrorMessage().toByteArray()));
        } catch (Exception e) {
            log.error("Error while send error response. BinaFlowException: {}", binaFlowException, e);
        }
    }

    @AllArgsConstructor
    static class MessageTypeMapping {
        private String className; // Package + class name
        private Object bean; // Controller
        private Method handlerMethod; // Handler method in controller
        private Method parseFromMethod; // 'parseFrom' method in java class for message type
    }
}
