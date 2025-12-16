# Design Document

## Overview

The SWS Gateway is a Spring Boot-based API Gateway that provides authentication, routing, monitoring, and transparent proxying capabilities. The system follows a layered architecture with clear separation of concerns, ensuring maintainability and extensibility. The gateway acts as a transparent proxy that validates API keys, identifies target services based on URL patterns, logs consumption data, and forwards requests without modifying payloads or responses.

## Architecture

The system follows a traditional Spring Boot layered architecture:

```
┌─────────────────┐
│   HTTP Client   │
└─────────┬───────┘
          │
┌─────────▼───────┐
│   Controller    │ ← Receives generic HTTP requests
└─────────┬───────┘
          │
┌─────────▼───────┐
│ Filter/         │ ← Validates API Key
│ Interceptor     │
└─────────┬───────┘
          │
┌─────────▼───────┐
│   Service       │ ← Business logic (consumption, service resolution)
└─────────┬───────┘
          │
┌─────────▼───────┐
│ Gateway Client  │ ← HTTP proxy functionality
└─────────┬───────┘
          │
┌─────────▼───────┐
│  Repository     │ ← Database access
└─────────────────┘
```

## Components and Interfaces

### Controller Layer
- **GatewayController**: Handles all incoming HTTP requests with wildcard mapping
- **ExceptionHandler**: Centralized error handling and response formatting

### Security Layer
- **ApiKeyFilter**: Servlet filter for API key validation
- **AuthenticationService**: Validates API keys and retrieves user information

### Business Logic Layer
- **GatewayService**: Orchestrates request processing workflow
- **ServiceRegistryService**: Manages service lookup and configuration
- **ConsumptionService**: Tracks and records API usage

### Integration Layer
- **ProxyClient**: HTTP client for forwarding requests to target services
- **AuthenticationApplier**: Applies service-specific authentication

### Data Layer
- **ApiKeyRepository**: API key and user data access
- **ServiceRepository**: Service configuration data access
- **ConsumptionRepository**: Usage tracking data access

## Data Models

### UserAccount Entity
```java
@Entity
@Table(name = "user_account")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 150)
    private String name;
    
    @Column(nullable = false, unique = true, length = 150)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

### ApiService Entity
```java
@Entity
@Table(name = "api_service")
public class ApiService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "service_name", nullable = false, unique = true, length = 100)
    private String serviceName;
    
    @Column(name = "base_url", nullable = false)
    private String baseUrl;
    
    @Column(name = "authentication_type", nullable = false, length = 50)
    private String authenticationType;
    
    @Column(name = "authentication_credential", nullable = false)
    private String authenticationCredential;
    
    @Column(name = "cost_per_request", precision = 10, scale = 4)
    private BigDecimal costPerRequest;
    
    @Column(name = "documentation_url")
    private String documentationUrl;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

### ApiKey Entity
```java
@Entity
@Table(name = "api_key")
public class ApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey;
    
    @Column(name = "api_service_id", nullable = false)
    private Long apiServiceId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_service_id", insertable = false, updatable = false)
    private ApiService apiService;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserAccount userAccount;
}
```

### Consumption Entity
```java
@Entity
@Table(name = "consumption")
public class Consumption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "api_key_id", nullable = false)
    private Long apiKeyId;
    
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;
    
    @Column(nullable = false)
    private Integer year;
    
    @Column(nullable = false)
    private Integer month;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id", insertable = false, updatable = false)
    private ApiKey apiKey;
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After analyzing all acceptance criteria, several properties can be consolidated to eliminate redundancy:

- Properties 3.1-3.5 (request forwarding) can be combined into a comprehensive "transparent forwarding" property
- Properties 6.1, 6.2, 6.4 (payload handling) are covered by the transparent forwarding property
- Properties 4.1-4.3 (authentication) can be combined into a single authentication application property
- Properties 5.1-5.2 (consumption recording) can be combined into a single consumption tracking property

### Core Properties

**Property 1: API Key Validation**
*For any* API key submitted in the `x-api-key` header, the gateway should accept it if and only if the key exists, is active, and is linked to a valid user
**Validates: Requirements 1.1, 1.2, 1.3**

**Property 2: Service Identification**
*For any* URL with multiple segments, extracting the service identifier should always return the last segment, and lookup should succeed if and only if a matching active service exists in the registry
**Validates: Requirements 2.1, 2.2, 2.4**

**Property 3: Transparent Request Forwarding**
*For any* valid request, the forwarded request should preserve the original HTTP method, all headers, all query parameters, and the request body without modification
**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 6.1, 6.2, 6.4**

**Property 4: Transparent Response Forwarding**
*For any* response received from a target service, the gateway should return the exact response to the client without modification
**Validates: Requirements 3.5, 7.2**

**Property 5: Authentication Application**
*For any* service with authentication configuration, the gateway should add the required authentication headers to the forwarded request, and for services without authentication configuration, no additional headers should be added
**Validates: Requirements 4.1, 4.2, 4.3**

**Property 6: Consumption Tracking**
*For any* valid request processed by the gateway, a consumption record should be created containing the user identifier, service identifier, and current month
**Validates: Requirements 5.1, 5.2**

**Property 7: Consumption Aggregation**
*For any* sequence of requests from the same user to the same service within the same month, the consumption count should equal the total number of requests
**Validates: Requirements 5.3, 5.4**

## Error Handling

The system implements comprehensive error handling at multiple levels:

### Authentication Errors
- **401 Unauthorized**: Missing or invalid API key
- **403 Forbidden**: Inactive or expired API key

### Routing Errors
- **404 Not Found**: Service identifier not found in registry
- **503 Service Unavailable**: Target service is unavailable

### Proxy Errors
- **500 Internal Server Error**: Gateway internal errors
- **504 Gateway Timeout**: Target service timeout

### Validation Errors
- **400 Bad Request**: Invalid authentication configuration

Error responses maintain consistency with standard HTTP status codes and include descriptive error messages for debugging purposes.

## Testing Strategy

The testing approach combines unit testing and property-based testing to ensure comprehensive coverage:

### Unit Testing Framework
- **JUnit 5**: Primary testing framework for Spring Boot
- **MockMvc**: For integration testing of web layer
- **Testcontainers**: For database integration tests
- **WireMock**: For mocking external service calls

### Property-Based Testing Framework
- **jqwik**: Java property-based testing library
- **Minimum 100 iterations** per property test to ensure statistical confidence
- Each property test tagged with format: `**Feature: api-gateway-core, Property {number}: {property_text}**`

### Testing Approach
**Unit Tests**: Verify specific examples, integration points, and edge cases including:
- API key validation with specific valid/invalid examples
- Service lookup with known service configurations
- Error handling for specific failure scenarios
- Database operations with known data sets

**Property-Based Tests**: Verify universal properties across all inputs:
- API key validation behavior across randomly generated keys
- Service identification across randomly generated URLs
- Request/response transparency across randomly generated HTTP requests
- Consumption tracking across randomly generated request sequences
- Authentication application across randomly generated service configurations

### Test Configuration
- Property tests configured to run 100+ iterations minimum
- Each property test explicitly references its corresponding design property
- Test data generators create realistic but varied input scenarios
- Edge cases handled through dedicated generators for boundary conditions