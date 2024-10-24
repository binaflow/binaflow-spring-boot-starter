package com.github.binaflow.config;

import com.github.binaflow.BinaFlowProperties;
import com.github.binaflow.service.BinaFlowService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Autoconfiguration for BinaFlow.
 * <p>Register the {@link BinaFlowService} as a WebSocket handler on the specified path.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final BinaFlowProperties properties;
    private final BinaFlowService binaFlowService;

    public WebSocketConfig(BinaFlowProperties properties, BinaFlowService binaFlowService) {
        this.binaFlowService = binaFlowService;
        this.properties = properties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(binaFlowService, properties.getHttpPath());
    }

}
