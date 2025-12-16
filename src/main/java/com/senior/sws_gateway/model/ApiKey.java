package com.senior.sws_gateway.model;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_key")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ApiKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "api_key", nullable = false, unique = true)
    @NotBlank(message = "API key is required")
    private String apiKey;
    
    @Column(name = "api_service_id", nullable = false)
    @NotNull(message = "API service ID is required")
    private Long apiServiceId;
    
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @Column(nullable = false)
    @NotNull(message = "Active status is required")
    private Boolean active = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_service_id", insertable = false, updatable = false)
    private ApiService apiService;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserAccount userAccount;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}