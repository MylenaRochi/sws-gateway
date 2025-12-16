package com.senior.sws_gateway.service;

import com.senior.sws_gateway.model.ApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service for applying service-specific authentication to outgoing requests
 */
@Service
@Slf4j
public class AuthenticationApplier {
    
    // Supported authentication types
    public static final String AUTH_TYPE_BEARER = "Bearer";
    public static final String AUTH_TYPE_BASIC = "Basic";
    public static final String AUTH_TYPE_API_KEY = "ApiKey";
    public static final String AUTH_TYPE_NONE = "None";
    
    /**
     * Applies authentication headers to the request based on service configuration
     * Requirements: 4.1 - Apply configured Authentication_Config when Target_Service requires authentication
     * Requirements: 4.2 - Add required authentication headers to proxied request
     * Requirements: 4.3 - Forward request without additional authentication when no config specified
     * 
     * @param headers the HTTP headers to modify
     * @param apiService the service configuration containing authentication details
     * @throws AuthenticationConfigurationException if authentication configuration is invalid
     */
    public void applyAuthentication(HttpHeaders headers, ApiService apiService) throws AuthenticationConfigurationException {
        if (apiService == null) {
            log.error("Authentication application failed: ApiService is null");
            throw new AuthenticationConfigurationException("ApiService cannot be null");
        }
        
        String serviceName = apiService.getServiceName();
        String authType = apiService.getAuthenticationType();
        String credential = apiService.getAuthenticationCredential();
        
        log.info("Applying authentication - Service: {}, Auth Type: {}", serviceName, authType);
        
        if (authType == null || authType.trim().isEmpty()) {
            log.error("Authentication application failed - Service: {}, Error: Authentication type is null or empty", serviceName);
            throw new AuthenticationConfigurationException("Authentication type cannot be null or empty");
        }
        
        authType = authType.trim();
        
        // Requirements: 4.3 - No additional authentication for None type
        if (AUTH_TYPE_NONE.equalsIgnoreCase(authType)) {
            log.debug("Authentication application - Service: {}, No authentication headers added (type: None)", serviceName);
            return;
        }
        
        if (credential == null || credential.trim().isEmpty()) {
            log.error("Authentication application failed - Service: {}, Auth Type: {}, Error: Credential is null or empty", 
                serviceName, authType);
            throw new AuthenticationConfigurationException("Authentication credential cannot be null or empty for type: " + authType);
        }
        
        credential = credential.trim();
        
        try {
            switch (authType) {
                case AUTH_TYPE_BEARER:
                    applyBearerAuthentication(headers, credential);
                    log.info("Authentication applied - Service: {}, Type: Bearer token", serviceName);
                    break;
                    
                case AUTH_TYPE_BASIC:
                    applyBasicAuthentication(headers, credential);
                    log.info("Authentication applied - Service: {}, Type: Basic auth", serviceName);
                    break;
                    
                case AUTH_TYPE_API_KEY:
                    applyApiKeyAuthentication(headers, credential);
                    log.info("Authentication applied - Service: {}, Type: API Key", serviceName);
                    break;
                    
                default:
                    log.error("Authentication application failed - Service: {}, Error: Unsupported authentication type: {}", 
                        serviceName, authType);
                    throw new AuthenticationConfigurationException("Unsupported authentication type: " + authType);
            }
        } catch (AuthenticationConfigurationException e) {
            // Re-throw configuration exceptions with logging
            log.error("Authentication application failed - Service: {}, Auth Type: {}, Error: {}", 
                serviceName, authType, e.getMessage());
            throw e;
        } catch (Exception e) {
            // Requirements: 4.4 - Log error and reject request when authentication config is invalid
            log.error("Authentication application failed - Service: {}, Auth Type: {}, Error: {}", 
                serviceName, authType, e.getMessage(), e);
            throw new AuthenticationConfigurationException("Failed to apply authentication for type " + authType + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Applies Bearer token authentication
     * Requirements: 4.2 - Support Bearer authentication type
     * 
     * @param headers the HTTP headers to modify
     * @param token the bearer token
     */
    private void applyBearerAuthentication(HttpHeaders headers, String token) {
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        log.debug("Bearer token authentication header added");
    }
    
    /**
     * Applies Basic authentication
     * Requirements: 4.2 - Support Basic authentication type
     * Expected credential format: "username:password"
     * 
     * @param headers the HTTP headers to modify
     * @param credentials the basic auth credentials in format "username:password"
     * @throws AuthenticationConfigurationException if credential format is invalid
     */
    private void applyBasicAuthentication(HttpHeaders headers, String credentials) throws AuthenticationConfigurationException {
        if (!credentials.contains(":")) {
            log.error("Basic authentication failed: Invalid credential format (missing colon separator)");
            throw new AuthenticationConfigurationException("Basic authentication credential must be in format 'username:password'");
        }
        
        String[] parts = credentials.split(":", 2);
        String username = parts[0];
        
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);
        log.debug("Basic authentication header added for username: {}", username);
    }
    
    /**
     * Applies API Key authentication
     * Requirements: 4.2 - Support API Key authentication type
     * Expected credential format: "header_name:api_key_value"
     * 
     * @param headers the HTTP headers to modify
     * @param apiKeyConfig the API key configuration in format "header_name:api_key_value"
     * @throws AuthenticationConfigurationException if credential format is invalid
     */
    private void applyApiKeyAuthentication(HttpHeaders headers, String apiKeyConfig) throws AuthenticationConfigurationException {
        if (!apiKeyConfig.contains(":")) {
            log.error("API Key authentication failed: Invalid credential format (missing colon separator)");
            throw new AuthenticationConfigurationException("API Key authentication credential must be in format 'header_name:api_key_value'");
        }
        
        String[] parts = apiKeyConfig.split(":", 2);
        String headerName = parts[0].trim();
        String apiKeyValue = parts[1].trim();
        
        if (headerName.isEmpty() || apiKeyValue.isEmpty()) {
            log.error("API Key authentication failed: Header name or value is empty");
            throw new AuthenticationConfigurationException("API Key header name and value cannot be empty");
        }
        
        headers.set(headerName, apiKeyValue);
        log.debug("API Key authentication header added: {}", headerName);
    }
    
    /**
     * Checks if the given authentication type is supported
     * 
     * @param authenticationType the authentication type to check
     * @return true if supported, false otherwise
     */
    public boolean isAuthenticationTypeSupported(String authenticationType) {
        if (authenticationType == null) {
            return false;
        }
        
        String authType = authenticationType.trim();
        return AUTH_TYPE_BEARER.equalsIgnoreCase(authType) ||
               AUTH_TYPE_BASIC.equalsIgnoreCase(authType) ||
               AUTH_TYPE_API_KEY.equalsIgnoreCase(authType) ||
               AUTH_TYPE_NONE.equalsIgnoreCase(authType);
    }
    
    /**
     * Exception thrown when authentication configuration is invalid
     * Requirements: 4.4 - Handle invalid authentication configuration
     */
    public static class AuthenticationConfigurationException extends Exception {
        public AuthenticationConfigurationException(String message) {
            super(message);
        }
        
        public AuthenticationConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}