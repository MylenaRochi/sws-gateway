package com.senior.sws_gateway.model;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ApiService {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "service_name", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Service name is required")
    @Size(max = 100, message = "Service name must not exceed 100 characters")
    private String serviceName;
    
    @Column(name = "base_url", nullable = false)
    @NotBlank(message = "Base URL is required")
    private String baseUrl;
    
    @Column(name = "authentication_type", nullable = false, length = 50)
    @NotBlank(message = "Authentication type is required")
    @Size(max = 50, message = "Authentication type must not exceed 50 characters")
    private String authenticationType;
    
    @Column(name = "authentication_credential", nullable = false)
    @NotBlank(message = "Authentication credential is required")
    private String authenticationCredential;
    
    @Column(name = "cost_per_request", precision = 10, scale = 4)
    @DecimalMin(value = "0.0", inclusive = true, message = "Cost per request must be non-negative")
    private BigDecimal costPerRequest;
    
    @Column(name = "documentation_url")
    private String documentationUrl;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}