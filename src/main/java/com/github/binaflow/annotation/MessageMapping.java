package com.github.binaflow.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an annotated method is a "MessageMapping" method.
 * <p>Method must have next signature:<br>
 * void methodName(GeneratedMessage message, WebSocketSession session).
 * <p>Class of annotated method must be annotated with {@link Controller}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageMapping {
}
