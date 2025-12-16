package com.senior.sws_gateway.service;

import com.senior.sws_gateway.model.ApiKey;
import com.senior.sws_gateway.model.UserAccount;
import com.senior.sws_gateway.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for API key authentication and validation
 */
@Service
@Slf4j
public class AuthenticationService {
    
    private final ApiKeyRepository apiKeyRepository;

    public AuthenticationService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * Validates an API key and returns the associated user if valid
     * 
     * @param apiKeyValue the API key string to validate
     * @return AuthenticationResult containing validation status and user info
     */
    public AuthenticationResult validateApiKey(String apiKeyValue) {
        if (apiKeyValue == null || apiKeyValue.trim().isEmpty()) {
            log.debug("API key validation failed: empty or null key");
            return AuthenticationResult.invalid("API key is required");
        }
        
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKey(apiKeyValue.trim());
        
        if (apiKeyOpt.isEmpty()) {
            log.debug("API key validation failed: key not found - {}", apiKeyValue);
            return AuthenticationResult.invalid("Invalid API key");
        }
        
        ApiKey apiKey = apiKeyOpt.get();
        
        // Check if the API key is active
        if (!apiKey.getActive()) {
            log.debug("API key validation failed: inactive key - {}", apiKeyValue);
            return AuthenticationResult.invalid("API key is inactive");
        }
        
        // Check if user account exists and is valid
        UserAccount user = apiKey.getUserAccount();
        if (user == null) {
            log.error("API key validation failed: no associated user - {}", apiKeyValue);
            return AuthenticationResult.invalid("Invalid user account");
        }
        
        log.debug("API key validation successful for user: {}", user.getEmail());
        return AuthenticationResult.valid(apiKey, user);
    }
    
    /**
     * Result of API key authentication
     */
    public static class AuthenticationResult {
        private final boolean valid;
        private final String errorMessage;
        private final ApiKey apiKey;
        private final UserAccount user;
        
        private AuthenticationResult(boolean valid, String errorMessage, ApiKey apiKey, UserAccount user) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.apiKey = apiKey;
            this.user = user;
        }
        
        public static AuthenticationResult valid(ApiKey apiKey, UserAccount user) {
            return new AuthenticationResult(true, null, apiKey, user);
        }
        
        public static AuthenticationResult invalid(String errorMessage) {
            return new AuthenticationResult(false, errorMessage, null, null);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public ApiKey getApiKey() {
            return apiKey;
        }
        
        public UserAccount getUser() {
            return user;
        }
    }
}