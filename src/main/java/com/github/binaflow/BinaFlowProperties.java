package com.github.binaflow;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "binaflow")
public class BinaFlowProperties {
    private Schema schema;
    private String httpPath;
    private UnhandledExceptions unhandledExceptions;

    @Data
    public static class Schema {
        private String directory;
    }

    @Data
    public static class UnhandledExceptions {
        private Boolean fillMessage;
        private Boolean fillExceptionClass;
        private Boolean fillStackTrace;
    }
}
