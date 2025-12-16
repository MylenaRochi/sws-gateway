package com.senior.sws_gateway.controller;

import com.senior.sws_gateway.interceptor.ApiKeyValidationInterceptor;
import com.senior.sws_gateway.model.ApiKey;
import com.senior.sws_gateway.model.ApiService;
import com.senior.sws_gateway.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Main gateway controller that handles all incoming HTTP requests
 * Requirements: All requirements integrated
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class GatewayController {
    
    private final GatewayService gatewayService;
    
    /**
     * Handles all HTTP requests with wildcard mapping
     * Integrates authentication, routing, proxy, and consumption services
     * Requirements: All requirements integrated
     */
    @RequestMapping("/**")
    public ResponseEntity<byte[]> handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.debug("Processing {} request to: {}", request.getMethod(), request.getRequestURI());
        
        // Get authenticated API key from interceptor
        ApiKey apiKey = ApiKeyValidationInterceptor.getAuthenticatedApiKey(request);
        if (apiKey == null) {
            log.error("No authenticated API key found in request context");
            throw new SecurityException("Authentication required - no valid API key found");
        }
        
        // Read request body
        byte[] requestBody;
        try {
            requestBody = StreamUtils.copyToByteArray(request.getInputStream());
        } catch (IOException e) {
            log.error("Failed to read request body: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to read request body: " + e.getMessage());
        }
        
        // Process the request through gateway service
        GatewayService.GatewayResponse gatewayResponse = gatewayService.processRequest(
            request, 
            apiKey, 
            requestBody
        );
        
        // Return the response
        return ResponseEntity
            .status(gatewayResponse.getStatus())
            .headers(gatewayResponse.getHeaders())
            .body(gatewayResponse.getBody());
    }
    

}