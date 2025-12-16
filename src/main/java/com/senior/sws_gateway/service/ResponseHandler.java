package com.senior.sws_gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Service for handling proxy responses and forwarding them to clients
 * Requirements: 3.5, 7.2 - Forward response status, headers, and body without modification
 */
@Service
@Slf4j
public class ResponseHandler {
    
    /**
     * Writes the proxy response to the HTTP servlet response
     * Requirements: 3.5 - Return exact response to client
     * Requirements: 7.2 - Handle different content types and preserve streaming
     * 
     * @param proxyResponse the response from the target service
     * @param httpResponse the HTTP servlet response to write to
     * @throws ResponseHandlingException if writing the response fails
     */
    public void writeResponse(ProxyClient.ProxyResponse proxyResponse, HttpServletResponse httpResponse) 
            throws ResponseHandlingException {
        try {
            // Set response status
            httpResponse.setStatus(proxyResponse.getStatus().value());
            
            // Copy headers from proxy response to HTTP response
            copyHeaders(proxyResponse.getHeaders(), httpResponse);
            
            // Write response body if present
            if (proxyResponse.hasBody()) {
                writeResponseBody(proxyResponse.getBody(), httpResponse);
            }
            
            log.debug("Successfully wrote response with status: {}", proxyResponse.getStatus());
            
        } catch (IOException e) {
            log.error("Failed to write response: {}", e.getMessage());
            throw new ResponseHandlingException("Failed to write response to client", e);
        } catch (Exception e) {
            log.error("Unexpected error while writing response: {}", e.getMessage());
            throw new ResponseHandlingException("Unexpected error while writing response", e);
        }
    }
    
    /**
     * Copies headers from proxy response to HTTP servlet response
     * Requirements: 3.5 - Preserve all response headers
     */
    private void copyHeaders(HttpHeaders proxyHeaders, HttpServletResponse httpResponse) {
        if (proxyHeaders == null || proxyHeaders.isEmpty()) {
            return;
        }
        
        for (Map.Entry<String, List<String>> headerEntry : proxyHeaders.entrySet()) {
            String headerName = headerEntry.getKey();
            List<String> headerValues = headerEntry.getValue();
            
            // Skip hop-by-hop headers that shouldn't be forwarded to client
            if (isHopByHopHeader(headerName)) {
                continue;
            }
            
            // Add all values for this header
            for (String headerValue : headerValues) {
                httpResponse.addHeader(headerName, headerValue);
            }
        }
    }
    
    /**
     * Writes the response body to the HTTP servlet response
     * Requirements: 7.2 - Handle different content types (JSON, binary, empty)
     * Requirements: 7.2 - Preserve response streaming for large payloads
     */
    private void writeResponseBody(byte[] body, HttpServletResponse httpResponse) throws IOException {
        if (body == null || body.length == 0) {
            return;
        }
        
        // Set content length for proper streaming
        httpResponse.setContentLength(body.length);
        
        // Write body to response output stream
        try (OutputStream outputStream = httpResponse.getOutputStream()) {
            outputStream.write(body);
            outputStream.flush();
        }
        
        log.debug("Wrote response body of {} bytes", body.length);
    }
    
    /**
     * Checks if a header is a hop-by-hop header that shouldn't be forwarded to client
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
               lowerCaseName.equals("upgrade");
    }
    
    /**
     * Determines if the content type indicates binary data
     * Used for optimizing streaming behavior for large binary payloads
     */
    public boolean isBinaryContent(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        String lowerContentType = contentType.toLowerCase();
        
        // Common binary content types
        return lowerContentType.startsWith("image/") ||
               lowerContentType.startsWith("video/") ||
               lowerContentType.startsWith("audio/") ||
               lowerContentType.equals("application/octet-stream") ||
               lowerContentType.equals("application/pdf") ||
               lowerContentType.startsWith("application/zip") ||
               lowerContentType.startsWith("application/x-");
    }
    
    /**
     * Determines if the content type indicates JSON data
     */
    public boolean isJsonContent(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        String lowerContentType = contentType.toLowerCase();
        return lowerContentType.equals("application/json") ||
               lowerContentType.startsWith("application/json;") ||
               lowerContentType.equals("text/json");
    }
    
    /**
     * Exception thrown when response handling fails
     */
    public static class ResponseHandlingException extends Exception {
        public ResponseHandlingException(String message) {
            super(message);
        }
        
        public ResponseHandlingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}