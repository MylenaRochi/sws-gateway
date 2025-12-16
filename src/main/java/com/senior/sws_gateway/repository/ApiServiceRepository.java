package com.senior.sws_gateway.repository;

import com.senior.sws_gateway.model.ApiService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiServiceRepository extends JpaRepository<ApiService, Long> {
    
    /**
     * Find API service by service name
     * @param serviceName the service name
     * @return Optional containing the API service if found
     */
    Optional<ApiService> findByServiceName(String serviceName);
    
    /**
     * Check if an API service exists with the given service name
     * @param serviceName the service name
     * @return true if service exists, false otherwise
     */
    boolean existsByServiceName(String serviceName);
}