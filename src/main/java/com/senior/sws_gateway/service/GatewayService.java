package com.senior.sws_gateway.service;

import com.senior.sws_gateway.model.ApiKey;
import com.senior.sws_gateway.model.ApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Gateway service that coordinates the complete request processing workflow
 * Requirements: All requirements orchestrated
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayService {
    
    private final ServiceRegistryService serviceRegistryService;
    private final AuthenticationApplier authenticationApplier;
    private final ProxyClient proxyClient;
    private final ConsumptionService consumptionService;
    
    /**
     * Processes a complete gateway request from authentication to response
     * Handles the complete request lifecycle with proper error handling and logging
     * Requirements: All requirements orchestrated
     * 
     * @param request the HTTP servlet request
     * @param apiKey the authenticated API key
     * @param requestBody the request body bytes
     * @return GatewayResponse containing the processed response
     * @throws ServiceRegistryService.ServiceNotFoundException when service is not found
     * @throws AuthenticationApplier.AuthenticationConfigurationException when auth config is invalid
     * @throws ProxyClient.ProxyException when proxy operations fail
     */
    public GatewayResponse processRequest(HttpServletRequest request, ApiKey apiKey, byte[] requestBody) 
            throws ServiceRegistryService.ServiceNotFoundException, 
                   AuthenticationApplier.AuthenticationConfigurationException, 
                   ProxyClient.ProxyException {
        
        // Step 1: Service Resolution
        log.debug("Step 1: Resolving service from URL: {}", request.getRequestURI());
        ApiService targetService = resolveTargetService(request.getRequestURI());
        
        // Step 2: Apply Service Authentication
        log.debug("Step 2: Applying authentication for service: {}", targetService.getServiceName());
        applyServiceAuthentication(request, targetService);
        
        // Step 3: Forward Request to Target Service
        log.debug("Step 3: Forwarding request to: {}", targetService.getBaseUrl());
        ProxyClient.ProxyResponse proxyResponse = forwardRequest(request, targetService, requestBody);
        
        // Step 4: Record Consumption (async to not block response)
        log.debug("Step 4: Recording consumption for API key: {}", apiKey.getId());
        recordConsumption(apiKey.getId());
        
        // Step 5: Return Response
        log.debug("Step 5: Returning response with status: {}", proxyResponse.getStatus());
        return new GatewayResponse(
            proxyResponse.getStatus(),
            proxyResponse.getHeaders(),
            proxyResponse.getBody()
        );
    }
    
    /**
     * Resolves the target service from the request URL
     * Requirements: 2.1, 2.2, 2.3, 2.4
     */
    private ApiService resolveTargetService(String requestUri) throws ServiceRegistryService.ServiceNotFoundException {
        Optional<ApiService> serviceOpt = serviceRegistryService.resolveServiceFromUrl(requestUri);
        
        if (serviceOpt.isEmpty()) {
            throw new ServiceRegistryService.ServiceNotFoundException("No service found for URL: " + requestUri);
        }
        
        return serviceOpt.get();
    }
    
    /**
     * Applies service-specific authentication to the request headers
     * Requirements: 4.1, 4.2, 4.3, 4.4
     */
    private void applyServiceAuthentication(HttpServletRequest request, ApiService targetService) 
            throws AuthenticationApplier.AuthenticationConfigurationException {
        
        // Create headers from original request
        HttpHeaders headers = new HttpHeaders();
        
        // Copy original headers (excluding hop-by-hop headers)
        if (request.getHeaderNames() != null) {
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                if (!isHopByHopHeader(headerName)) {
                    request.getHeaders(headerName).asIterator().forEachRemaining(headerValue -> {
                        headers.add(headerName, headerValue);
                    });
                }
            });
        }
        
        // Apply service-specific authentication
        authenticationApplier.applyAuthentication(headers, targetService);
        
        // Store the modified headers in request attributes for ProxyClient to use
        request.setAttribute("GATEWAY_AUTH_HEADERS", headers);
    }
    
    /**
     * Forwards the request to the target service
     * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 6.1, 6.2, 6.3
     */
    private ProxyClient.ProxyResponse forwardRequest(HttpServletRequest request, ApiService targetService, 
                                                   byte[] requestBody) 
            throws ProxyClient.ProxyException {
        
        // The ProxyClient will handle header extraction and forwarding
        // Authentication headers were already applied and stored in request attributes
        return proxyClient.forwardRequest(request, targetService.getBaseUrl(), requestBody);
    }
    
    /**
     * Records consumption for the API key
     * Requirements: 5.1, 5.2, 5.3, 5.4
     */
    private void recordConsumption(Long apiKeyId) {
        try {
            consumptionService.recordConsumption(apiKeyId);
            log.debug("Successfully recorded consumption for API key: {}", apiKeyId);
        } catch (Exception e) {
            // Log error but don't fail the request
            log.error("Failed to record consumption for API key {}: {}", apiKeyId, e.getMessage(), e);
        }
    }
    

    
    /**
     * Checks if a header is a hop-by-hop header that shouldn't be forwarded
     */
    private boolean isHopByHopHeader(String headerName) {
        String lowerCaseName = headerName.toLowerCase();
        return lowerCaseName.equals("connection") ||
               lowerCaseName.equals("keep-alive") ||
               lowerCaseName.equals("proxy-authenticate") ||
               lowerCaseName.equals("proxy-authorization") ||
               lowerCaseName.equals("te") ||
               lowerCaseName.equals("trailers") ||
               lowerCaseName.equals("transfer-encoding") ||
               lowerCaseName.equals("upgrade") ||
               lowerCaseName.equals("host");
    }
    
    /**
     * Response from the gateway processing
     */
    public static class GatewayResponse {
        private final HttpStatus status;
        private final HttpHeaders headers;
        private final byte[] body;
        
        public GatewayResponse(HttpStatus status, HttpHeaders headers, byte[] body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }
        
        public HttpStatus getStatus() {
            return status;
        }
        
        public HttpHeaders getHeaders() {
            return headers;
        }
        
        public byte[] getBody() {
            return body;
        }
        
        public boolean hasBody() {
            return body != null && body.length > 0;
        }
    }
}