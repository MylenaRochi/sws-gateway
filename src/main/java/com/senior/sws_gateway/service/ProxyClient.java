package com.senior.sws_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;

/**
 * HTTP proxy client for forwarding requests to target services
 * Requirements: 3.1, 3.2, 3.3, 3.4, 6.1, 6.2, 6.3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProxyClient {
    
    private final RestTemplate restTemplate;
    
    /**
     * Forwards an HTTP request to the target service
     * Requirements: 3.1, 3.2, 3.3, 3.4 - Preserve method, headers, query params, and body
     * Requirements: 6.1, 6.2, 6.3 - Support JSON, binary, and empty payloads
     * 
     * @param request the original HTTP request
     * @param targetUrl the target service URL
     * @param requestBody the request body (can be null)
     * @return ProxyResponse containing the response from target service
     * @throws ProxyException if forwarding fails
     */
    public ProxyResponse forwardRequest(HttpServletRequest request, String targetUrl, byte[] requestBody) throws ProxyException {
        long startTime = System.currentTimeMillis();
        String method = request.getMethod().toUpperCase();
        
        try {
            // Build the complete target URL with query parameters
            String completeUrl = buildCompleteUrl(targetUrl, request.getQueryString());
            
            // Create HTTP headers from original request
            HttpHeaders headers = extractHeaders(request);
            
            // Log proxy request details (without sensitive data)
            log.info("Proxy request - Method: {}, Target URL: {}, Headers count: {}, Body size: {} bytes", 
                method, completeUrl, headers.size(), requestBody != null ? requestBody.length : 0);
            
            // Log content type for debugging
            String contentType = headers.getFirst("Content-Type");
            if (contentType != null) {
                log.debug("Proxy request content type: {}", contentType);
            }
            
            // Create HTTP entity with body and headers
            HttpEntity<byte[]> entity = new HttpEntity<>(requestBody, headers);
            
            // Get HTTP method from original request
            HttpMethod httpMethod = HttpMethod.valueOf(method);
            
            log.debug("Forwarding {} request to: {}", httpMethod, completeUrl);
            
            // Forward the request
            ResponseEntity<byte[]> response = restTemplate.exchange(
                URI.create(completeUrl),
                httpMethod,
                entity,
                byte[].class
            );
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Log proxy response details (without sensitive data)
            log.info("Proxy response - Status: {}, Headers count: {}, Body size: {} bytes, Duration: {} ms", 
                response.getStatusCode(), 
                response.getHeaders().size(), 
                response.getBody() != null ? response.getBody().length : 0,
                duration);
            
            // Log response content type for debugging
            String responseContentType = response.getHeaders().getFirst("Content-Type");
            if (responseContentType != null) {
                log.debug("Proxy response content type: {}", responseContentType);
            }
            
            return new ProxyResponse(
                response.getStatusCode(),
                response.getHeaders(),
                response.getBody()
            );
            
        } catch (RestClientException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Proxy request failed - Method: {}, Target URL: {}, Duration: {} ms, Error: {}", 
                method, targetUrl, duration, e.getMessage());
            throw new ProxyException("Failed to forward request to target service", e);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Proxy request error - Method: {}, Target URL: {}, Duration: {} ms, Error: {}", 
                method, targetUrl, duration, e.getMessage());
            throw new ProxyException("Unexpected error during request forwarding", e);
        }
    }
    
    /**
     * Builds complete URL with query parameters
     * Requirements: 3.3 - Include all query parameters in proxied request
     */
    private String buildCompleteUrl(String baseUrl, String queryString) {
        if (queryString == null || queryString.trim().isEmpty()) {
            return baseUrl;
        }
        
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + queryString;
    }
    
    /**
     * Extracts headers from the original request
     * Requirements: 3.2 - Include all original headers in proxied request
     */
    private HttpHeaders extractHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        
        // Check if authentication headers were applied by GatewayService
        HttpHeaders authHeaders = (HttpHeaders) request.getAttribute("GATEWAY_AUTH_HEADERS");
        if (authHeaders != null) {
            // Use the headers with authentication already applied
            headers.addAll(authHeaders);
        } else {
            // Extract headers from original request
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    
                    // Skip hop-by-hop headers that shouldn't be forwarded
                    if (isHopByHopHeader(headerName)) {
                        continue;
                    }
                    
                    Enumeration<String> headerValues = request.getHeaders(headerName);
                    while (headerValues.hasMoreElements()) {
                        String headerValue = headerValues.nextElement();
                        headers.add(headerName, headerValue);
                    }
                }
            }
        }
        
        return headers;
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
               lowerCaseName.equals("host"); // Host will be set by RestTemplate
    }
    
    /**
     * Response from the proxy request
     */
    public static class ProxyResponse {
        private final HttpStatus status;
        private final HttpHeaders headers;
        private final byte[] body;
        
        public ProxyResponse(HttpStatus status, HttpHeaders headers, byte[] body) {
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
    
    /**
     * Exception thrown when proxy operations fail
     */
    public static class ProxyException extends Exception {
        public ProxyException(String message) {
            super(message);
        }
        
        public ProxyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}