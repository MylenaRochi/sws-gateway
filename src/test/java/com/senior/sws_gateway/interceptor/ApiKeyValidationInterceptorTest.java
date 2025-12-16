package com.senior.sws_gateway.interceptor;

import com.senior.sws_gateway.model.ApiKey;
import com.senior.sws_gateway.model.UserAccount;
import com.senior.sws_gateway.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyValidationInterceptorTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private ApiKeyValidationInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Object handler;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        handler = new Object();
    }

    @Test
    void preHandle_WithValidApiKey_ShouldReturnTrue() throws Exception {
        // Given
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setEmail("test@example.com");
        
        ApiKey apiKey = new ApiKey();
        apiKey.setId(1L);
        apiKey.setApiKey("valid-key");
        apiKey.setUserAccount(user);
        
        AuthenticationService.AuthenticationResult validResult = 
            AuthenticationService.AuthenticationResult.valid(apiKey, user);
        
        request.addHeader("x-api-key", "valid-key");
        when(authenticationService.validateApiKey("valid-key")).thenReturn(validResult);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertTrue(result);
        assertEquals(200, response.getStatus());
        assertEquals(user, request.getAttribute("authenticatedUser"));
        assertEquals(apiKey, request.getAttribute("authenticatedApiKey"));
        verify(authenticationService).validateApiKey("valid-key");
    }

    @Test
    void preHandle_WithMissingApiKey_ShouldReturnFalse() throws Exception {
        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertFalse(result);
        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("Missing API key"));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void preHandle_WithEmptyApiKey_ShouldReturnFalse() throws Exception {
        // Given
        request.addHeader("x-api-key", "   ");

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertFalse(result);
        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("Missing API key"));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void preHandle_WithInvalidApiKey_ShouldReturnFalse() throws Exception {
        // Given
        AuthenticationService.AuthenticationResult invalidResult = 
            AuthenticationService.AuthenticationResult.invalid("Invalid API key");
        
        request.addHeader("x-api-key", "invalid-key");
        when(authenticationService.validateApiKey("invalid-key")).thenReturn(invalidResult);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertFalse(result);
        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("Authentication failed"));
        assertTrue(response.getContentAsString().contains("Invalid API key"));
        verify(authenticationService).validateApiKey("invalid-key");
    }

    @Test
    void getAuthenticatedUser_WithUserInRequest_ShouldReturnUser() {
        // Given
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setEmail("test@example.com");
        request.setAttribute("authenticatedUser", user);

        // When
        UserAccount result = ApiKeyValidationInterceptor.getAuthenticatedUser(request);

        // Then
        assertEquals(user, result);
    }

    @Test
    void getAuthenticatedApiKey_WithApiKeyInRequest_ShouldReturnApiKey() {
        // Given
        ApiKey apiKey = new ApiKey();
        apiKey.setId(1L);
        apiKey.setApiKey("test-key");
        request.setAttribute("authenticatedApiKey", apiKey);

        // When
        ApiKey result = ApiKeyValidationInterceptor.getAuthenticatedApiKey(request);

        // Then
        assertEquals(apiKey, result);
    }
}