package com.senior.sws_gateway.exception;

import com.senior.sws_gateway.service.AuthenticationApplier;
import com.senior.sws_gateway.service.ProxyClient;
import com.senior.sws_gateway.service.ResponseHandler;
import com.senior.sws_gateway.service.ServiceRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Global exception handler for centralized error handling
 * Maps different exception types to appropriate HTTP status codes
 * Returns consistent error response format
 * Requirements: 1.3, 1.4, 2.3, 4.4, 7.1, 7.3, 7.4
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles service not found exceptions
     * Requirements: 2.3 - Handle service not found scenarios
     */
    @ExceptionHandler(ServiceRegistryService.ServiceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleServiceNotFoundException(
            ServiceRegistryService.ServiceNotFoundException ex, 
            HttpServletRequest request) {
        
        log.warn("Service not found for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message("Service not found")
                .details(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles authentication configuration exceptions
     * Requirements: 4.4 - Handle invalid authentication configuration
     */
    @ExceptionHandler(AuthenticationApplier.AuthenticationConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationConfigurationException(
            AuthenticationApplier.AuthenticationConfigurationException ex, 
            HttpServletRequest request) {
        
        log.error("Authentication configuration error for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Authentication configuration error")
                .details(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles proxy exceptions (service unavailable, timeout)
     * Requirements: 7.1, 7.3, 7.4 - Handle proxy errors
     */
    @ExceptionHandler(ProxyClient.ProxyException.class)
    public ResponseEntity<ErrorResponse> handleProxyException(
            ProxyClient.ProxyException ex, 
            HttpServletRequest request) {
        
        log.error("Proxy error for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        // Determine if it's a timeout or service unavailable based on message
        HttpStatus status = ex.getMessage().toLowerCase().contains("timeout") 
            ? HttpStatus.GATEWAY_TIMEOUT 
            : HttpStatus.BAD_GATEWAY;
        
        String message = status == HttpStatus.GATEWAY_TIMEOUT 
            ? "Service timeout" 
            : "Service unavailable";
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .details("Target service is currently unavailable")
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handles response handling exceptions
     * Requirements: 7.1 - Handle internal gateway errors
     */
    @ExceptionHandler(ResponseHandler.ResponseHandlingException.class)
    public ResponseEntity<ErrorResponse> handleResponseHandlingException(
            ResponseHandler.ResponseHandlingException ex, 
            HttpServletRequest request) {
        
        log.error("Response handling error for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Response processing error")
                .details(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles illegal argument exceptions (validation errors)
     * Requirements: 1.3, 1.4 - Handle validation errors
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, 
            HttpServletRequest request) {
        
        log.warn("Validation error for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Invalid request parameters")
                .details(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles authentication and authorization exceptions
     * Requirements: 1.3, 1.4 - Handle authentication errors
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(
            SecurityException ex, 
            HttpServletRequest request) {
        
        log.warn("Security error for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        // Determine if it's authentication (401) or authorization (403) based on message
        HttpStatus status = ex.getMessage().toLowerCase().contains("unauthorized") 
            ? HttpStatus.UNAUTHORIZED 
            : HttpStatus.FORBIDDEN;
        
        String message = status == HttpStatus.UNAUTHORIZED 
            ? "Authentication required" 
            : "Access forbidden";
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .details(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handles all other unexpected exceptions
     * Requirements: 7.1 - Handle internal gateway errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, 
            HttpServletRequest request) {
        
        log.error("Unexpected error for request {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Internal server error")
                .details("An unexpected error occurred")
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Standardized error response format
     */
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String details;
        private String path;

        // Default constructor for JSON serialization
        public ErrorResponse() {}

        private ErrorResponse(Builder builder) {
            this.timestamp = builder.timestamp;
            this.status = builder.status;
            this.error = builder.error;
            this.message = builder.message;
            this.details = builder.details;
            this.path = builder.path;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public int getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }

        public String getDetails() {
            return details;
        }

        public String getPath() {
            return path;
        }

        // Setters for JSON serialization
        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public void setError(String error) {
            this.error = error;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setDetails(String details) {
            this.details = details;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public static class Builder {
            private LocalDateTime timestamp;
            private int status;
            private String error;
            private String message;
            private String details;
            private String path;

            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder status(int status) {
                this.status = status;
                return this;
            }

            public Builder error(String error) {
                this.error = error;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder details(String details) {
                this.details = details;
                return this;
            }

            public Builder path(String path) {
                this.path = path;
                return this;
            }

            public ErrorResponse build() {
                return new ErrorResponse(this);
            }
        }
    }
}