package com.senior.sws_gateway.service;

import com.senior.sws_gateway.model.ApiService;
import com.senior.sws_gateway.repository.ApiServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceRegistryServiceTest {

    @Mock
    private ApiServiceRepository apiServiceRepository;

    private ServiceRegistryService serviceRegistryService;

    @BeforeEach
    void setUp() {
        serviceRegistryService = new ServiceRegistryService(apiServiceRepository);
    }

    @Test
    void extractServiceName_shouldReturnLastSegment() {
        // Test basic URL
        assertEquals("users", serviceRegistryService.extractServiceName("/api/v1/users"));
        
        // Test URL without leading slash
        assertEquals("orders", serviceRegistryService.extractServiceName("api/v1/orders"));
        
        // Test URL with trailing slash
        assertEquals("products", serviceRegistryService.extractServiceName("/api/v1/products/"));
        
        // Test single segment
        assertEquals("health", serviceRegistryService.extractServiceName("health"));
    }

    @Test
    void extractServiceName_shouldThrowExceptionForInvalidUrls() {
        assertThrows(IllegalArgumentException.class, () -> 
            serviceRegistryService.extractServiceName(null));
        
        assertThrows(IllegalArgumentException.class, () -> 
            serviceRegistryService.extractServiceName(""));
        
        assertThrows(IllegalArgumentException.class, () -> 
            serviceRegistryService.extractServiceName("   "));
        
        assertThrows(IllegalArgumentException.class, () -> 
            serviceRegistryService.extractServiceName("/"));
    }

    @Test
    void findServiceByName_shouldReturnServiceWhenFound() {
        // Arrange
        String serviceName = "users";
        ApiService expectedService = new ApiService();
        expectedService.setServiceName(serviceName);
        
        when(apiServiceRepository.findByServiceName(serviceName))
            .thenReturn(Optional.of(expectedService));

        // Act
        Optional<ApiService> result = serviceRegistryService.findServiceByName(serviceName);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(serviceName, result.get().getServiceName());
        verify(apiServiceRepository).findByServiceName(serviceName);
    }

    @Test
    void findServiceByName_shouldReturnEmptyWhenNotFound() {
        // Arrange
        String serviceName = "nonexistent";
        when(apiServiceRepository.findByServiceName(serviceName))
            .thenReturn(Optional.empty());

        // Act
        Optional<ApiService> result = serviceRegistryService.findServiceByName(serviceName);

        // Assert
        assertFalse(result.isPresent());
        verify(apiServiceRepository).findByServiceName(serviceName);
    }

    @Test
    void findServiceByName_shouldReturnEmptyForNullOrEmptyName() {
        // Test null
        Optional<ApiService> result1 = serviceRegistryService.findServiceByName(null);
        assertFalse(result1.isPresent());
        
        // Test empty string
        Optional<ApiService> result2 = serviceRegistryService.findServiceByName("");
        assertFalse(result2.isPresent());
        
        // Test whitespace
        Optional<ApiService> result3 = serviceRegistryService.findServiceByName("   ");
        assertFalse(result3.isPresent());
        
        // Verify repository was not called
        verifyNoInteractions(apiServiceRepository);
    }

    @Test
    void resolveServiceFromUrl_shouldReturnServiceWhenFound() throws Exception {
        // Arrange
        String url = "/api/v1/users";
        String serviceName = "users";
        ApiService expectedService = new ApiService();
        expectedService.setServiceName(serviceName);
        
        when(apiServiceRepository.findByServiceName(serviceName))
            .thenReturn(Optional.of(expectedService));

        // Act
        Optional<ApiService> result = serviceRegistryService.resolveServiceFromUrl(url);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(serviceName, result.get().getServiceName());
    }

    @Test
    void resolveServiceFromUrl_shouldThrowExceptionWhenServiceNotFound() {
        // Arrange
        String url = "/api/v1/nonexistent";
        String serviceName = "nonexistent";
        
        when(apiServiceRepository.findByServiceName(serviceName))
            .thenReturn(Optional.empty());

        // Act & Assert
        ServiceRegistryService.ServiceNotFoundException exception = 
            assertThrows(ServiceRegistryService.ServiceNotFoundException.class, () -> 
                serviceRegistryService.resolveServiceFromUrl(url));
        
        assertTrue(exception.getMessage().contains("Service not found for identifier: nonexistent"));
    }

    @Test
    void resolveServiceFromUrl_shouldThrowExceptionForInvalidUrl() {
        // Act & Assert
        ServiceRegistryService.ServiceNotFoundException exception = 
            assertThrows(ServiceRegistryService.ServiceNotFoundException.class, () -> 
                serviceRegistryService.resolveServiceFromUrl(""));
        
        assertTrue(exception.getMessage().contains("Invalid URL format"));
    }
}