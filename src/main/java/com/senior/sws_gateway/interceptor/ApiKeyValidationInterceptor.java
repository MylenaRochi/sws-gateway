package com.senior.sws_gateway.interceptor;

import com.senior.sws_gateway.service.AuthenticationService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for validating API keys in incoming requests
 */
@Component
@Slf4j
public class ApiKeyValidationInterceptor implements HandlerInterceptor {
    
    private static final String API_KEY_HEADER = "x-api-key";
    private static final String USER_CONTEXT_ATTRIBUTE = "authenticatedUser";
    private static final String API_KEY_CONTEXT_ATTRIBUTE = "authenticatedApiKey";
    
    private final AuthenticationService authenticationService;

    public ApiKeyValidationInterceptor(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = getClientIpAddress(request);
        
        log.info("Authentication attempt - Method: {}, URI: {}, Client IP: {}", method, requestUri, clientIp);
        
        // Extract API key from header
        String apiKey = request.getHeader(API_KEY_HEADER);
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Authentication failed - Missing API key header - Method: {}, URI: {}, Client IP: {}", 
                method, requestUri, clientIp);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing API key\",\"message\":\"x-api-key header is required\"}");
            return false;
        }
        
        // Mask API key for logging (show only first 8 characters)
        String maskedApiKey = maskApiKey(apiKey);
        log.debug("Validating API key: {} - Method: {}, URI: {}, Client IP: {}", 
            maskedApiKey, method, requestUri, clientIp);
        
        // Validate the API key
        AuthenticationService.AuthenticationResult result = authenticationService.validateApiKey(apiKey);
        
        if (!result.isValid()) {
            log.warn("Authentication failed - API key validation failed: {} - Key: {}, Method: {}, URI: {}, Client IP: {}", 
                result.getErrorMessage(), maskedApiKey, method, requestUri, clientIp);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format("{\"error\":\"Authentication failed\",\"message\":\"%s\"}", result.getErrorMessage()));
            return false;
        }
        
        // Store user context for request processing
        request.setAttribute(USER_CONTEXT_ATTRIBUTE, result.getUser());
        request.setAttribute(API_KEY_CONTEXT_ATTRIBUTE, result.getApiKey());
        
        log.info("Authentication successful - User: {}, API Key ID: {}, Method: {}, URI: {}, Client IP: {}", 
            result.getUser().getEmail(), result.getApiKey().getId(), method, requestUri, clientIp);
        return true;
    }
    
    /**
     * Extracts client IP address from request, considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in case of multiple proxies
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Masks API key for secure logging (shows only first 8 characters)
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 8) + "****";
    }
    
    /**
     * Utility method to get authenticated user from request
     */
    public static com.senior.sws_gateway.model.UserAccount getAuthenticatedUser(HttpServletRequest request) {
        return (com.senior.sws_gateway.model.UserAccount) request.getAttribute(USER_CONTEXT_ATTRIBUTE);
    }
    
    /**
     * Utility method to get authenticated API key from request
     */
    public static com.senior.sws_gateway.model.ApiKey getAuthenticatedApiKey(HttpServletRequest request) {
        return (com.senior.sws_gateway.model.ApiKey) request.getAttribute(API_KEY_CONTEXT_ATTRIBUTE);
    }
}