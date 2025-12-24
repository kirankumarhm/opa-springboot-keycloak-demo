package com.opa.demo.exception;

import com.opa.demo.service.OpaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.warn("Authentication failed [{}]: {}", errorId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .errorId(errorId)
            .code("AUTHENTICATION_FAILED")
            .message("Authentication required")
            .status(HttpStatus.UNAUTHORIZED.value())
            .timestamp(LocalDateTime.now())
            .path(extractPath(request))
            .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.warn("Access denied [{}]: {}", errorId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .errorId(errorId)
            .code("ACCESS_DENIED")
            .message("Access denied")
            .status(HttpStatus.FORBIDDEN.value())
            .timestamp(LocalDateTime.now())
            .path(extractPath(request))
            .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.warn("Validation failed [{}]: {}", errorId, ex.getMessage());
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        ErrorResponse error = ErrorResponse.builder()
            .errorId(errorId)
            .code("VALIDATION_FAILED")
            .message("Validation failed")
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(LocalDateTime.now())
            .path(extractPath(request))
            .validationErrors(validationErrors)
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.warn("Type mismatch [{}]: {}", errorId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .errorId(errorId)
            .code("INVALID_PARAMETER")
            .message(String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName()))
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(LocalDateTime.now())
            .path(extractPath(request))
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(OpaService.OpaServiceException.class)
    public ResponseEntity<ErrorResponse> handleOpaServiceException(OpaService.OpaServiceException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.error("OPA service error [{}]: {}", errorId, ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .errorId(errorId)
            .code("AUTHORIZATION_SERVICE_ERROR")
            .message("Authorization service temporarily unavailable")
            .status(HttpStatus.SERVICE_UNAVAILABLE.value())
            .timestamp(LocalDateTime.now())
            .path(extractPath(request))
            .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.warn("Invalid argument [{}]: {}", errorId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .errorId(errorId)
            .code("INVALID_ARGUMENT")
            .message(ex.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(LocalDateTime.now())
            .path(extractPath(request))
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        logger.error("Unexpected error [{}]: {}", errorId, ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .errorId(errorId)
            .code("INTERNAL_ERROR")
            .message("An unexpected error occurred")
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .timestamp(LocalDateTime.now())
            .path(extractPath(request))
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    public static class ErrorResponse {
        private final String errorId;
        private final String code;
        private final String message;
        private final int status;
        private final LocalDateTime timestamp;
        private final String path;
        private final Map<String, String> validationErrors;

        private ErrorResponse(Builder builder) {
            this.errorId = builder.errorId;
            this.code = builder.code;
            this.message = builder.message;
            this.status = builder.status;
            this.timestamp = builder.timestamp;
            this.path = builder.path;
            this.validationErrors = builder.validationErrors;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getErrorId() { return errorId; }
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public int getStatus() { return status; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getPath() { return path; }
        public Map<String, String> getValidationErrors() { return validationErrors; }

        public static class Builder {
            private String errorId;
            private String code;
            private String message;
            private int status;
            private LocalDateTime timestamp;
            private String path;
            private Map<String, String> validationErrors;

            public Builder errorId(String errorId) { this.errorId = errorId; return this; }
            public Builder code(String code) { this.code = code; return this; }
            public Builder message(String message) { this.message = message; return this; }
            public Builder status(int status) { this.status = status; return this; }
            public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
            public Builder path(String path) { this.path = path; return this; }
            public Builder validationErrors(Map<String, String> validationErrors) { this.validationErrors = validationErrors; return this; }

            public ErrorResponse build() {
                return new ErrorResponse(this);
            }
        }
    }
}
