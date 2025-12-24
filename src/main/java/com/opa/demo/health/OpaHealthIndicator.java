package com.opa.demo.health;

import com.opa.demo.config.OpaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class OpaHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(OpaHealthIndicator.class);
    private final WebClient webClient;
    private final OpaProperties opaProperties;

    public OpaHealthIndicator(OpaProperties opaProperties) {
        this.opaProperties = opaProperties;
        this.webClient = WebClient.builder()
                .baseUrl(opaProperties.url())
                .build();
    }

    @Override
    public Health health() {
        try {
            String response = webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(2000))
                    .block();

            return Health.up()
                    .withDetail("url", opaProperties.url())
                    .withDetail("status", "UP")
                    .withDetail("response", response)
                    .build();

        } catch (Exception e) {
            logger.warn("OPA health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("url", opaProperties.url())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
