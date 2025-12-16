package com.senior.sws_gateway.service;

import com.senior.sws_gateway.model.ApiService;
import com.senior.sws_gateway.repository.ApiServiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for managing service registry operations including service lookup and URL parsing
 */
@Service
@Slf4j
public class ServiceRegistryService {
    
    private final ApiServiceRepository apiServiceRepository;
    
    @Autowired
    public ServiceRegistryService(ApiServiceRepository apiServiceRepository) {
        this.apiServiceRepository = apiServiceRepository;
    }
    
    /**
     * Extracts service name from URL by taking the last segment
     * Requirements: 2.1 - Extract last segment as Service_Identifier
     * 
     * @param url the request URL
     * @return the service identifier (last URL segment)
     * @throws IllegalArgumentException if URL is null, empty, or has no segments
     */
    public String extractServiceName(String url) {
        log.debug("Extracting service name from URL: {}", url);
        
        if (url == null || url.trim().isEmpty()) {
            log.warn("Service name extraction failed: URL is null or empty");
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        
        // Remove leading/trailing slashes and split by '/'
        String cleanUrl = url.trim().replaceAll("^/+|/+$", "");
        
        if (cleanUrl.isEmpty()) {
            log.warn("Service name extraction failed: URL contains no segments after cleaning: {}", url);
            throw new IllegalArgumentException("URL must contain at least one segment");
        }
        
        String[] segments = cleanUrl.split("/");
        
        if (segments.length == 0) {
            log.warn("Service name extraction failed: No segments found in URL: {}", url);
            throw new IllegalArgumentException("URL must contain at least one segment");
        }
        
        // Return the last segment as service identifier
        String serviceName = segments[segments.length - 1];
        log.debug("Service name extracted successfully: {} from URL: {}", serviceName, url);
        return serviceName;
    }
    
    /**
     * Looks up API service by service name
     * Requirements: 2.2 - Lookup corresponding Target_Service in Service_Registry
     * Requirements: 2.4 - Retrieve service configuration for request forwarding
     * 
     * @param serviceName the service name to lookup
     * @return Optional containing the ApiService if found
     */
    public Optional<ApiService> findServiceByName(String serviceName) {
        log.debug("Looking up service by name: {}", serviceName);
        
        if (serviceName == null || serviceName.trim().isEmpty()) {
            log.warn("Service lookup failed: service name is null or empty");
            return Optional.empty();
        }
        
        Optional<ApiService> service = apiServiceRepository.findByServiceName(serviceName.trim());
        
        if (service.isPresent()) {
            ApiService apiService = service.get();
            log.info("Service found - Name: {}, ID: {}, Base URL: {}, Auth Type: {}", 
                apiService.getServiceName(), apiService.getId(), 
                apiService.getBaseUrl(), apiService.getAuthenticationType());
        } else {
            log.warn("Service not found in registry: {}", serviceName);
        }
        
        return service;
    }
    
    /**
     * Resolves service from URL by extracting service name and looking it up
     * Requirements: 2.1, 2.2, 2.3, 2.4 - Complete service resolution workflow
     * 
     * @param url the request URL
     * @return Optional containing the ApiService if found
     * @throws ServiceNotFoundException if service is not found in registry
     */
    public Optional<ApiService> resolveServiceFromUrl(String url) throws ServiceNotFoundException {
        log.info("Resolving service from URL: {}", url);
        
        try {
            String serviceName = extractServiceName(url);
            Optional<ApiService> service = findServiceByName(serviceName);
            
            if (service.isEmpty()) {
                log.error("Service resolution failed - Service not found for identifier: {} from URL: {}", serviceName, url);
                throw new ServiceNotFoundException("Service not found for identifier: " + serviceName);
            }
            
            log.info("Service resolution successful - Service: {} resolved from URL: {}", serviceName, url);
            return service;
        } catch (IllegalArgumentException e) {
            log.error("Service resolution failed - Invalid URL format: {} - Error: {}", url, e.getMessage());
            throw new ServiceNotFoundException("Invalid URL format: " + e.getMessage());
        }
    }
    
    /**
     * Exception thrown when a service is not found in the registry
     * Requirements: 2.3 - Handle service not found scenarios
     */
    public static class ServiceNotFoundException extends Exception {
        public ServiceNotFoundException(String message) {
            super(message);
        }
        
        public ServiceNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}