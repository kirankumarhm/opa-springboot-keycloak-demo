package com.opa.demo.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.opa")
@Validated
public record OpaProperties(
    @NotBlank String url,
    @NotBlank String policyPath,
    @Positive int timeoutMs,
    @Positive int maxRetries,
    @Positive long retryDelayMs,
    @NotNull CircuitBreakerProperties circuitBreaker
) {
    public record CircuitBreakerProperties(
        @Positive int failureThreshold,
        @Positive long timeoutDuration,
        @Positive long waitDurationInOpenState
    ) {}
}
