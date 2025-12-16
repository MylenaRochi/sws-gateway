package com.senior.sws_gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProxyClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ProxyClient proxyClient;

    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockRequest = new MockHttpServletRequest();
        mockRequest.setMethod("GET");
        mockRequest.setRequestURI("/api/test");
        mockRequest.addHeader("Content-Type", "application/json");
        mockRequest.addHeader("Authorization", "Bearer token123");
    }

    @Test
    void forwardRequest_WithValidGetRequest_ShouldReturnProxyResponse() throws Exception {
        // Given
        String targetUrl = "http://target-service.com/api";
        byte[] responseBody = "{\"result\":\"success\"}".getBytes();
        
        ResponseEntity<byte[]> mockResponse = new ResponseEntity<>(
            responseBody,
            createResponseHeaders(),
            HttpStatus.OK
        );
        
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
            .thenReturn(mockResponse);

        // When
        ProxyClient.ProxyResponse result = proxyClient.forwardRequest(mockRequest, targetUrl, null);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatus());
        assertArrayEquals(responseBody, result.getBody());
        assertTrue(result.hasBody());
        
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class));
    }

    @Test
    void forwardRequest_WithPostRequestAndBody_ShouldForwardBody() throws Exception {
        // Given
        mockRequest.setMethod("POST");
        byte[] requestBody = "{\"data\":\"test\"}".getBytes();
        String targetUrl = "http://target-service.com/api";
        
        ResponseEntity<byte[]> mockResponse = new ResponseEntity<>(
            "OK".getBytes(),
            HttpStatus.CREATED
        );
        
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(byte[].class)))
            .thenReturn(mockResponse);

        // When
        ProxyClient.ProxyResponse result = proxyClient.forwardRequest(mockRequest, targetUrl, requestBody);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatus());
        
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(byte[].class));
    }

    @Test
    void forwardRequest_WithQueryParameters_ShouldIncludeQueryParams() throws Exception {
        // Given
        mockRequest.setQueryString("param1=value1&param2=value2");
        String targetUrl = "http://target-service.com/api";
        
        ResponseEntity<byte[]> mockResponse = new ResponseEntity<>(HttpStatus.OK);
        
        when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), eq(byte[].class)))
            .thenReturn(mockResponse);

        // When
        proxyClient.forwardRequest(mockRequest, targetUrl, null);

        // Then
        verify(restTemplate).exchange(
            argThat(uri -> uri.toString().contains("param1=value1&param2=value2")),
            any(HttpMethod.class),
            any(HttpEntity.class),
            eq(byte[].class)
        );
    }

    @Test
    void forwardRequest_WithRestClientException_ShouldThrowProxyException() {
        // Given
        String targetUrl = "http://target-service.com/api";
        
        when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), eq(byte[].class)))
            .thenThrow(new RestClientException("Connection failed"));

        // When & Then
        ProxyClient.ProxyException exception = assertThrows(
            ProxyClient.ProxyException.class,
            () -> proxyClient.forwardRequest(mockRequest, targetUrl, null)
        );
        
        assertEquals("Failed to forward request to target service", exception.getMessage());
        assertTrue(exception.getCause() instanceof RestClientException);
    }

    @Test
    void forwardRequest_WithEmptyResponse_ShouldHandleEmptyBody() throws Exception {
        // Given
        String targetUrl = "http://target-service.com/api";
        
        ResponseEntity<byte[]> mockResponse = new ResponseEntity<>(
            null,
            HttpStatus.NO_CONTENT
        );
        
        when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), eq(byte[].class)))
            .thenReturn(mockResponse);

        // When
        ProxyClient.ProxyResponse result = proxyClient.forwardRequest(mockRequest, targetUrl, null);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.NO_CONTENT, result.getStatus());
        assertFalse(result.hasBody());
    }

    private HttpHeaders createResponseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Custom-Header", "custom-value");
        return headers;
    }
}