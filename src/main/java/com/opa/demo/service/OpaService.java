package com.opa.demo.service;

import com.opa.demo.config.OpaProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

@Service
@EnableConfigurationProperties(OpaProperties.class)
public class OpaService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpaService.class);
    private static final String CIRCUIT_BREAKER_NAME = "opaService";
    private static final String RETRY_NAME = "opaService";
    
    private final WebClient webClient;
    private final OpaProperties opaProperties;
    private final Timer authorizationTimer;
    private final Counter successCounter;
    private final Counter failureCounter;
    
    public OpaService(OpaProperties opaProperties, MeterRegistry meterRegistry) {
        this.opaProperties = opaProperties;
        this.webClient = WebClient.builder()
                .baseUrl(opaProperties.url())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        
        // Metrics
        this.authorizationTimer = Timer.builder("opa.authorization.duration")
                .description("Time taken for OPA authorization requests")
                .register(meterRegistry);
        this.successCounter = Counter.builder("opa.authorization.success")
                .description("Successful OPA authorization requests")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("opa.authorization.failure")
                .description("Failed OPA authorization requests")
                .register(meterRegistry);
        
        logger.info("OPA Service initialized with URL: {} and policy path: {}", 
                   opaProperties.url(), opaProperties.policyPath());
    }
    
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackAuthorization")
    @Retry(name = RETRY_NAME)
    public boolean isAllowed(String user, String action, String resource) {
        return Timer.Sample.start(authorizationTimer)
                .stop(authorizationTimer.recordCallable(() -> performAuthorization(user, action, resource)));
    }
    
    private boolean performAuthorization(String user, String action, String resource) {
        validateInputs(user, action, resource);
        
        Map<String, Object> input = Map.of(
            "user", user,
            "action", action,
            "resource", resource
        );
        
        Map<String, Object> request = Map.of("input", input);
        
        try {
            logger.debug("Checking authorization for user: {}, action: {}, resource: {}", user, action, resource);
            
            Map<String, Object> response = webClient.post()
                    .uri(opaProperties.policyPath())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(opaProperties.timeoutMs()))
                    .block();
            
            boolean allowed = response != null && Boolean.TRUE.equals(response.get("result"));
            
            if (allowed) {
                successCounter.increment();
                logger.info("Authorization ALLOWED for user: {} on resource: {}", user, resource);
            } else {
                logger.warn("Authorization DENIED for user: {} on resource: {}", user, resource);
            }
            
            return allowed;
            
        } catch (WebClientResponseException e) {
            failureCounter.increment();
            logger.error("OPA server returned error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new OpaServiceException("OPA server error: " + e.getStatusCode(), e);
        } catch (WebClientException e) {
            failureCounter.increment();
            logger.error("Failed to communicate with OPA server: {}", e.getMessage(), e);
            throw new OpaServiceException("OPA communication failed", e);
        } catch (Exception e) {
            failureCounter.increment();
            logger.error("Unexpected error during authorization check: {}", e.getMessage(), e);
            throw new OpaServiceException("Authorization check failed", e);
        }
    }
    
    private void validateInputs(String user, String action, String resource) {
        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("User cannot be null or empty");
        }
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("Action cannot be null or empty");
        }
        if (resource == null || resource.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource cannot be null or empty");
        }
    }
    
    // Fallback method for circuit breaker
    public boolean fallbackAuthorization(String user, String action, String resource, Exception ex) {
        logger.error("OPA service fallback triggered for user: {} - denying access due to: {}", 
                    user, ex.getMessage());
        failureCounter.increment();
        return false; // Fail closed - deny access when OPA is unavailable
    }
    
    public static class OpaServiceException extends RuntimeException {
        public OpaServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
