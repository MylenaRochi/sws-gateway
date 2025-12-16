package com.senior.sws_gateway.service;

import com.senior.sws_gateway.model.ApiKey;
import com.senior.sws_gateway.model.UserAccount;
import com.senior.sws_gateway.repository.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    private ApiKey validApiKey;
    private UserAccount user;

    @BeforeEach
    void setUp() {
        user = new UserAccount();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");
        user.setCreatedAt(LocalDateTime.now());

        validApiKey = new ApiKey();
        validApiKey.setId(1L);
        validApiKey.setApiKey("valid-api-key-123");
        validApiKey.setUserId(1L);
        validApiKey.setApiServiceId(1L);
        validApiKey.setActive(true);
        validApiKey.setUserAccount(user);
        validApiKey.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void validateApiKey_WithValidKey_ShouldReturnValidResult() {
        // Given
        when(apiKeyRepository.findByApiKey("valid-api-key-123")).thenReturn(Optional.of(validApiKey));

        // When
        AuthenticationService.AuthenticationResult result = authenticationService.validateApiKey("valid-api-key-123");

        // Then
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
        assertEquals(validApiKey, result.getApiKey());
        assertEquals(user, result.getUser());
        verify(apiKeyRepository).findByApiKey("valid-api-key-123");
    }

    @Test
    void validateApiKey_WithNullKey_ShouldReturnInvalidResult() {
        // When
        AuthenticationService.AuthenticationResult result = authenticationService.validateApiKey(null);

        // Then
        assertFalse(result.isValid());
        assertEquals("API key is required", result.getErrorMessage());
        assertNull(result.getApiKey());
        assertNull(result.getUser());
        verifyNoInteractions(apiKeyRepository);
    }

    @Test
    void validateApiKey_WithEmptyKey_ShouldReturnInvalidResult() {
        // When
        AuthenticationService.AuthenticationResult result = authenticationService.validateApiKey("   ");

        // Then
        assertFalse(result.isValid());
        assertEquals("API key is required", result.getErrorMessage());
        assertNull(result.getApiKey());
        assertNull(result.getUser());
        verifyNoInteractions(apiKeyRepository);
    }

    @Test
    void validateApiKey_WithNonExistentKey_ShouldReturnInvalidResult() {
        // Given
        when(apiKeyRepository.findByApiKey("non-existent-key")).thenReturn(Optional.empty());

        // When
        AuthenticationService.AuthenticationResult result = authenticationService.validateApiKey("non-existent-key");

        // Then
        assertFalse(result.isValid());
        assertEquals("Invalid API key", result.getErrorMessage());
        assertNull(result.getApiKey());
        assertNull(result.getUser());
        verify(apiKeyRepository).findByApiKey("non-existent-key");
    }

    @Test
    void validateApiKey_WithInactiveKey_ShouldReturnInvalidResult() {
        // Given
        validApiKey.setActive(false);
        when(apiKeyRepository.findByApiKey("inactive-key")).thenReturn(Optional.of(validApiKey));

        // When
        AuthenticationService.AuthenticationResult result = authenticationService.validateApiKey("inactive-key");

        // Then
        assertFalse(result.isValid());
        assertEquals("API key is inactive", result.getErrorMessage());
        assertNull(result.getApiKey());
        assertNull(result.getUser());
        verify(apiKeyRepository).findByApiKey("inactive-key");
    }

    @Test
    void validateApiKey_WithKeyButNoUser_ShouldReturnInvalidResult() {
        // Given
        validApiKey.setUserAccount(null);
        when(apiKeyRepository.findByApiKey("key-without-user")).thenReturn(Optional.of(validApiKey));

        // When
        AuthenticationService.AuthenticationResult result = authenticationService.validateApiKey("key-without-user");

        // Then
        assertFalse(result.isValid());
        assertEquals("Invalid user account", result.getErrorMessage());
        assertNull(result.getApiKey());
        assertNull(result.getUser());
        verify(apiKeyRepository).findByApiKey("key-without-user");
    }
}