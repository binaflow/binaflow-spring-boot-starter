package io.github.binaflow;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "binaflow")
public record BinaFlowProperties(Schema schema, String httpPath, UnhandledExceptions unhandledExceptions) {

    public record Schema(String directory) {
    }

    public record UnhandledExceptions(Boolean fillMessage, Boolean fillExceptionClass, Boolean fillStackTrace) {
    }
}
