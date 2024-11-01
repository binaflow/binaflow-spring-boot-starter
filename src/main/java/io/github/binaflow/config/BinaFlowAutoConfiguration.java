package io.github.binaflow.config;

import io.github.binaflow.BinaFlowProperties;
import io.github.binaflow.controller.PingController;
import io.github.binaflow.service.BinaFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration for BinaFlow.
 * <p>Instantiate the {@link BinaFlowService} and {@link PingController} beans if they are not already defined.
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(BinaFlowProperties.class)
public class BinaFlowAutoConfiguration {

    private final ApplicationContext applicationContext;

    @Bean
    @ConditionalOnMissingBean
    public BinaFlowService binaFlowService(BinaFlowProperties properties) {
        return new BinaFlowService(properties, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public PingController pingController() {
        return new PingController();
    }
}