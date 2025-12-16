# Implementation Plan

- [x] 1. Set up project structure and dependencies




  - Add required Spring Boot dependencies (Web, JPA, Validation)
  - Configure application properties for database and logging
  - _Requirements: All requirements need proper project setup_

- [x] 2. Implement core data models and repositories





- [x] 2.1 Create JPA entities for domain models


  - Implement UserAccount, ApiKey, ApiService, and Consumption entities
  - Add proper JPA annotations matching database schema
  - Include validation annotations and relationships
  - _Requirements: 1.1, 1.2, 2.2, 5.1, 5.2_


- [x] 2.2 Create repository interfaces

  - Implement ApiKeyRepository with findByApiKey method
  - Implement ApiServiceRepository with findByServiceName method  
  - Implement ConsumptionRepository with monthly aggregation queries
  - Implement UserAccountRepository for user management
  - _Requirements: 1.1, 2.2, 5.3, 5.4_

- [x] 3. Implement API key validation




- [x] 3.1 Create API key validation interceptor


  - Implement HandlerInterceptor for API key validation
  - Extract API key from x-api-key header
  - Validate key exists and is active in database
  - Store user context for request processing
  - _Requirements: 1.1, 1.3_

- [x] 3.2 Implement authentication service


  - Create AuthenticationService for API key validation
  - Add methods for key validation and user retrieval
  - Handle inactive and expired keys
  - Return validation results without Spring Security integration
  - _Requirements: 1.2, 1.4_

- [x] 4. Implement service registry and routing logic




- [x] 4.1 Create service registry service


  - Implement ServiceRegistryService for ApiService lookup
  - Add URL parsing logic to extract service name from last URL segment
  - Handle service not found scenarios
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 4.2 Implement authentication configuration handler


  - Create AuthenticationApplier for service-specific auth
  - Support different authentication types (Bearer, Basic, API Key)
  - Apply authentication headers based on service configuration
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 5. Implement HTTP proxy client





- [x] 5.1 Create proxy client for request forwarding


  - Implement ProxyClient using RestTemplate or WebClient
  - Handle HTTP method, headers, query parameters, and body forwarding
  - Support both JSON and binary payload types
  - Configure timeout and error handling
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 6.1, 6.2, 6.3_

- [x] 5.2 Implement response handling


  - Forward response status, headers, and body without modification
  - Handle different content types (JSON, binary, empty)
  - Preserve response streaming for large payloads
  - _Requirements: 3.5, 7.2_

- [x] 6. Implement consumption tracking




- [x] 6.1 Create consumption service

  - Implement ConsumptionService for usage tracking
  - Add methods for recording and aggregating consumption by api_key_id
  - Handle monthly aggregation with year/month columns
  - Ensure thread-safe consumption recording with usage_count increment
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 7. Implement main gateway controller and orchestration





- [x] 7.1 Create gateway controller


  - Implement GatewayController with wildcard request mapping
  - Handle all HTTP methods (GET, POST, PUT, DELETE, etc.)
  - Integrate authentication, routing, proxy, and consumption services
  - _Requirements: All requirements integrated_

- [x] 7.2 Implement gateway service orchestration


  - Create GatewayService to coordinate request processing workflow
  - Handle the complete request lifecycle from authentication to response
  - Implement proper error handling and logging
  - _Requirements: All requirements orchestrated_

- [x] 8. Implement error handling and exception management





- [x] 8.1 Create global exception handler


  - Implement @ControllerAdvice for centralized error handling
  - Map different exception types to appropriate HTTP status codes
  - Return consistent error response format
  - _Requirements: 1.3, 1.4, 2.3, 4.4, 7.1, 7.3, 7.4_

- [x] 8.2 Add comprehensive logging


  - Log authentication attempts and failures
  - Log service routing decisions
  - Log proxy request/response details (without sensitive data)
  - Log consumption recording events
  - _Requirements: All requirements for monitoring and debugging_

- [x] 9. Final configuration and deployment preparation




- [x] 9.1 Set up production configuration

  - Create production application properties
  - Configure database connection settings
  - Set up logging configuration for production
  - _Requirements: All requirements need production setup_