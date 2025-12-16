package com.senior.sws_gateway.repository;

import com.senior.sws_gateway.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    
    /**
     * Find API key by the key string with user account eagerly loaded
     * @param apiKey the API key string
     * @return Optional containing the API key if found
     */
    @Query("SELECT ak FROM ApiKey ak JOIN FETCH ak.userAccount WHERE ak.apiKey = :apiKey")
    Optional<ApiKey> findByApiKey(@Param("apiKey") String apiKey);
    
    /**
     * Find all API keys for a specific user
     * @param userId the user ID
     * @return List of API keys for the user
     */
    List<ApiKey> findByUserId(Long userId);
    
    /**
     * Find all active API keys for a specific user
     * @param userId the user ID
     * @param active the active status
     * @return List of active API keys for the user
     */
    List<ApiKey> findByUserIdAndActive(Long userId, Boolean active);
    
    /**
     * Find all API keys for a specific service
     * @param apiServiceId the API service ID
     * @return List of API keys for the service
     */
    List<ApiKey> findByApiServiceId(Long apiServiceId);
    
    /**
     * Check if an API key exists with the given key string
     * @param apiKey the API key string
     * @return true if key exists, false otherwise
     */
    boolean existsByApiKey(String apiKey);
}