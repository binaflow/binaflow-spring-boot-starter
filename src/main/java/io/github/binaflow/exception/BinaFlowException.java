package io.github.binaflow.exception;

import io.github.binaflow.dto.Error;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.ProblemDetail;

/**
 * Base exception for exceptions in BinaFlow.
 * <p>Any exception threw inside method annotated @MessageMapping and inherited from BinaFlowException will be intercepted by BinaFlowService and Error message will be sent to client.
 * Error message will be constructed from ProblemDetail object.
 */
@Getter
public class BinaFlowException extends NestedRuntimeException {

    protected ProblemDetail problemDetail;
    @Setter
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
}
