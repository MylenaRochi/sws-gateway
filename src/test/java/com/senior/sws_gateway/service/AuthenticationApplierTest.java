package com.senior.sws_gateway.service;

import com.senior.sws_gateway.model.ApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationApplierTest {

    private AuthenticationApplier authenticationApplier;

    @BeforeEach
    void setUp() {
        authenticationApplier = new AuthenticationApplier();
    }

    @Test
    void applyAuthentication_shouldApplyBearerToken() throws Exception {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        ApiService apiService = new ApiService();
        apiService.setAuthenticationType("Bearer");
        apiService.setAuthenticationCredential("test-token-123");

        // Act
        authenticationApplier.applyAuthentication(headers, apiService);

        // Assert
        assertEquals("Bearer test-token-123", headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void applyAuthentication_shouldApplyBasicAuth() throws Exception {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        ApiService apiService = new ApiService();
        apiService.setAuthenticationType("Basic");
        apiService.setAuthenticationCredential("username:password");

        String expectedEncoded = Base64.getEncoder().encodeToString("username:password".getBytes(StandardCharsets.UTF_8));

        // Act
        authenticationApplier.applyAuthentication(headers, apiService);

        // Assert
        assertEquals("Basic " + expectedEncoded, headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void applyAuthentication_shouldApplyApiKey() throws Exception {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        ApiService apiService = new ApiService();
        apiService.setAuthenticationType("ApiKey");
        apiService.setAuthenticationCredential("X-API-Key:secret-key-123");

        // Act
        authenticationApplier.applyAuthentication(headers, apiService);

        // Assert
        assertEquals("secret-key-123", headers.getFirst("X-API-Key"));
    }

    @Test
    void applyAuthentication_shouldNotAddHeadersForNoneType() throws Exception {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        ApiService apiService = new ApiService();
        apiService.setAuthenticationType("None");
        apiService.setAuthenticationCredential(""); // Can be empty for None type

        // Act
        authenticationApplier.applyAuthentication(headers, apiService);

        // Assert
        assertTrue(headers.isEmpty());
    }

    @Test
    void applyAuthentication_shouldThrowExceptionForNullService() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();

        // Act & Assert
        AuthenticationApplier.AuthenticationConfigurationException exception = 
            assertThrows(AuthenticationApplier.AuthenticationConfigurationException.class, () -> 
                authenticationApplier.applyAuthentication(headers, null));
        
        assertEquals("ApiService cannot be null", exception.getMessage());
    }

    @Test
    void applyAuthentication_shouldThrowExceptionForNullAuthType() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        ApiService apiService = new ApiService();
        apiService.setAuthenticationType(null);
        apiService.setAuthenticationCredential("test");

        // Act & Assert
        AuthenticationApplier.AuthenticationConfigurationException exception = 
            assertThrows(AuthenticationApplier.AuthenticationConfigurationException.class, () -> 
                authenticationApplier.applyAuthentication(headers, apiService));
        
        assertTrue(exception.getMessage().contains("Authentication type cannot be null or empty"));
    }

    @Test
    void applyAuthentication_shouldThrowExceptionForUnsupportedAuthType() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        ApiService apiService = new ApiService();
        apiService.setAuthenticationType("OAuth2");
        apiService.setAuthenticationCredential("test");

        // Act & Assert
        AuthenticationApplier.AuthenticationConfigurationException exception = 
            assertThrows(AuthenticationApplier.AuthenticationConfigurationException.class, () -> 
                authenticationApplier.applyAuthentication(headers, apiService));
        
        assertTrue(exception.getMessage().contains("Unsupported authentication type: OAuth2"));
    }

    @Test
    void applyAuthentication_shouldThrowExceptionForInvalidBasicCredentials() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        ApiService apiService = new ApiService();
        apiService.setAuthenticationType("Basic");
        apiService.setAuthenticationCredential("invalid-format");

        // Act & Assert
        AuthenticationApplier.AuthenticationConfigurationException exception = 
            assertThrows(AuthenticationApplier.AuthenticationConfigurationException.class, () -> 
                authenticationApplier.applyAuthentication(headers, apiService));
        
        assertTrue(exception.getMessage().contains("Basic authentication credential must be in format 'username:password'"));
    }

    @Test
    void applyAuthentication_shouldThrowExceptionForInvalidApiKeyCredentials() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        ApiService apiService = new ApiService();
        apiService.setAuthenticationType("ApiKey");
        apiService.setAuthenticationCredential("invalid-format");

        // Act & Assert
        AuthenticationApplier.AuthenticationConfigurationException exception = 
            assertThrows(AuthenticationApplier.AuthenticationConfigurationException.class, () -> 
                authenticationApplier.applyAuthentication(headers, apiService));
        
        assertTrue(exception.getMessage().contains("API Key authentication credential must be in format 'header_name:api_key_value'"));
    }

    @Test
    void isAuthenticationTypeSupported_shouldReturnTrueForSupportedTypes() {
        assertTrue(authenticationApplier.isAuthenticationTypeSupported("Bearer"));
        assertTrue(authenticationApplier.isAuthenticationTypeSupported("Basic"));
        assertTrue(authenticationApplier.isAuthenticationTypeSupported("ApiKey"));
        assertTrue(authenticationApplier.isAuthenticationTypeSupported("None"));
        
        // Test case insensitive
        assertTrue(authenticationApplier.isAuthenticationTypeSupported("bearer"));
        assertTrue(authenticationApplier.isAuthenticationTypeSupported("BASIC"));
    }

    @Test
    void isAuthenticationTypeSupported_shouldReturnFalseForUnsupportedTypes() {
        assertFalse(authenticationApplier.isAuthenticationTypeSupported("OAuth2"));
        assertFalse(authenticationApplier.isAuthenticationTypeSupported("JWT"));
        assertFalse(authenticationApplier.isAuthenticationTypeSupported(null));
        assertFalse(authenticationApplier.isAuthenticationTypeSupported(""));
    }
}