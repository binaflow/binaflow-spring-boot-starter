package com.github.binaflow.exception;

/**
 * Exception thrown when message type is empty.
 */
public class EmptyMessageTypeException extends BinaFlowException {

    public EmptyMessageTypeException() {
        super("Message type is empty");
//        problemDetail.setType("URI");
        problemDetail.setTitle("Message type is empty");
        problemDetail.setDetail("Message type is empty");
    }
}
