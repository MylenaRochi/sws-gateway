package com.senior.sws_gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class ResponseHandlerTest {

    private ResponseHandler responseHandler;
    private MockHttpServletResponse mockResponse;

    @BeforeEach
    void setUp() {
        responseHandler = new ResponseHandler();
        mockResponse = new MockHttpServletResponse();
    }

    @Test
    void writeResponse_WithJsonResponse_ShouldWriteCorrectly() throws Exception {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Custom-Header", "custom-value");
        
        byte[] body = "{\"message\":\"success\"}".getBytes();
        
        ProxyClient.ProxyResponse proxyResponse = new ProxyClient.ProxyResponse(
            HttpStatus.OK,
            headers,
            body
        );

        // When
        responseHandler.writeResponse(proxyResponse, mockResponse);

        // Then
        assertEquals(200, mockResponse.getStatus());
        assertEquals("application/json", mockResponse.getContentType());
        assertEquals("custom-value", mockResponse.getHeader("Custom-Header"));
        assertEquals("{\"message\":\"success\"}", mockResponse.getContentAsString());
        assertEquals(body.length, mockResponse.getContentLength());
    }

    @Test
    void writeResponse_WithBinaryResponse_ShouldWriteCorrectly() throws Exception {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        
        byte[] binaryData = new byte[]{0x01, 0x02, 0x03, 0x04};
        
        ProxyClient.ProxyResponse proxyResponse = new ProxyClient.ProxyResponse(
            HttpStatus.OK,
            headers,
            binaryData
        );

        // When
        responseHandler.writeResponse(proxyResponse, mockResponse);

        // Then
        assertEquals(200, mockResponse.getStatus());
        assertEquals("application/octet-stream", mockResponse.getContentType());
        assertArrayEquals(binaryData, mockResponse.getContentAsByteArray());
        assertEquals(binaryData.length, mockResponse.getContentLength());
    }

    @Test
    void writeResponse_WithEmptyBody_ShouldWriteStatusAndHeaders() throws Exception {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.add("Custom-Header", "value");
        
        ProxyClient.ProxyResponse proxyResponse = new ProxyClient.ProxyResponse(
            HttpStatus.NO_CONTENT,
            headers,
            null
        );

        // When
        responseHandler.writeResponse(proxyResponse, mockResponse);

        // Then
        assertEquals(204, mockResponse.getStatus());
        assertEquals("value", mockResponse.getHeader("Custom-Header"));
        assertEquals(0, mockResponse.getContentLength());
    }

    @Test
    void writeResponse_WithMultipleHeaderValues_ShouldWriteAllValues() throws Exception {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.add("Multi-Header", "value1");
        headers.add("Multi-Header", "value2");
        
        ProxyClient.ProxyResponse proxyResponse = new ProxyClient.ProxyResponse(
            HttpStatus.OK,
            headers,
            "test".getBytes()
        );

        // When
        responseHandler.writeResponse(proxyResponse, mockResponse);

        // Then
        assertEquals(200, mockResponse.getStatus());
        // MockHttpServletResponse concatenates multiple header values
        assertTrue(mockResponse.getHeaders("Multi-Header").contains("value1"));
        assertTrue(mockResponse.getHeaders("Multi-Header").contains("value2"));
    }

    @Test
    void writeResponse_WithErrorStatus_ShouldPreserveErrorStatus() throws Exception {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        byte[] errorBody = "{\"error\":\"Not found\"}".getBytes();
        
        ProxyClient.ProxyResponse proxyResponse = new ProxyClient.ProxyResponse(
            HttpStatus.NOT_FOUND,
            headers,
            errorBody
        );

        // When
        responseHandler.writeResponse(proxyResponse, mockResponse);

        // Then
        assertEquals(404, mockResponse.getStatus());
        assertEquals("{\"error\":\"Not found\"}", mockResponse.getContentAsString());
    }

    @Test
    void isBinaryContent_WithBinaryContentTypes_ShouldReturnTrue() {
        // Test various binary content types
        assertTrue(responseHandler.isBinaryContent("image/jpeg"));
        assertTrue(responseHandler.isBinaryContent("image/png"));
        assertTrue(responseHandler.isBinaryContent("video/mp4"));
        assertTrue(responseHandler.isBinaryContent("audio/mpeg"));
        assertTrue(responseHandler.isBinaryContent("application/octet-stream"));
        assertTrue(responseHandler.isBinaryContent("application/pdf"));
        assertTrue(responseHandler.isBinaryContent("application/zip"));
        assertTrue(responseHandler.isBinaryContent("application/x-binary"));
    }

    @Test
    void isBinaryContent_WithTextContentTypes_ShouldReturnFalse() {
        // Test text content types
        assertFalse(responseHandler.isBinaryContent("text/plain"));
        assertFalse(responseHandler.isBinaryContent("text/html"));
        assertFalse(responseHandler.isBinaryContent("application/json"));
        assertFalse(responseHandler.isBinaryContent("application/xml"));
        assertFalse(responseHandler.isBinaryContent(null));
    }

    @Test
    void isJsonContent_WithJsonContentTypes_ShouldReturnTrue() {
        assertTrue(responseHandler.isJsonContent("application/json"));
        assertTrue(responseHandler.isJsonContent("application/json; charset=utf-8"));
        assertTrue(responseHandler.isJsonContent("text/json"));
    }

    @Test
    void isJsonContent_WithNonJsonContentTypes_ShouldReturnFalse() {
        assertFalse(responseHandler.isJsonContent("text/plain"));
        assertFalse(responseHandler.isJsonContent("application/xml"));
        assertFalse(responseHandler.isJsonContent("image/jpeg"));
        assertFalse(responseHandler.isJsonContent(null));
    }
}