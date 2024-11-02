package io.github.binaflow.exception;

import io.github.binaflow.dto.Error;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.ProblemDetail;

/**
 * Base exception for exceptions in BinaFlow.
 * <p>Any exception threw inside method annotated @MessageMapping and inherited from BinaFlowException will be intercepted by BinaFlowService and Error message will be sent to client.
 * Error message will be constructed from ProblemDetail object.
 */
public class BinaFlowException extends NestedRuntimeException {

    protected ProblemDetail problemDetail;
    protected String messageId;

    public BinaFlowException(String msg) {
        super(msg);
        problemDetail = ProblemDetail.forStatus(500);
    }

    public BinaFlowException(String msg, Throwable cause) {
        super(msg, cause);
        problemDetail = ProblemDetail.forStatus(500);
    }

    public Error toErrorMessage() {
        return Error.newBuilder()
                .setMessageType("Error")
                .setMessageId(messageId)
                .setType(problemDetail.getType().toString())
                .setTitle(problemDetail.getTitle())
                .setStatus(problemDetail.getStatus())
                .setDetail(problemDetail.getDetail())
                .setInstance(String.valueOf(problemDetail.getInstance()))
                .build();
    }

    public ProblemDetail getProblemDetail() {
        return problemDetail;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
