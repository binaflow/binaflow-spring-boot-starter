package com.github.binaflow.exception;

/**
 * Exception thrown when message type is not found in the message mapping.
 */
public class MessageTypeNotFoundException extends BinaFlowException {

    public MessageTypeNotFoundException(String messageType) {
        super("Message type not found");
//        problemDetail.setType("URI");
        problemDetail.setTitle("Message type not found");
        problemDetail.setStatus(400);
        problemDetail.setDetail("Message type '" + messageType + "' not found");
    }
}
