package com.senior.sws_gateway.service;

import com.senior.sws_gateway.model.Consumption;
import com.senior.sws_gateway.repository.ConsumptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Service for tracking and managing API consumption data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumptionService {
    
    private final ConsumptionRepository consumptionRepository;
    
    /**
     * Records consumption for an API key in the current month.
     * This method is thread-safe and handles concurrent access properly.
     * 
     * @param apiKeyId the API key ID to record consumption for
     * @return the updated consumption record
     */
    @Transactional
    public Consumption recordConsumption(Long apiKeyId) {
        if (apiKeyId == null) {
            log.error("Consumption recording failed: API key ID is null");
            throw new IllegalArgumentException("API key ID cannot be null");
        }
        
        YearMonth currentMonth = YearMonth.now();
        int year = currentMonth.getYear();
        int month = currentMonth.getMonthValue();
        
        log.info("Recording consumption - API Key ID: {}, Period: {}/{}", apiKeyId, year, month);
        
        // Try to increment existing record first (atomic operation)
        int updatedRows = consumptionRepository.incrementUsageCount(apiKeyId, year, month);
        
        if (updatedRows > 0) {
            // Successfully incremented existing record
            log.info("Consumption recorded - Incremented existing record for API Key ID: {} in {}/{}", 
                apiKeyId, year, month);
            return consumptionRepository.findByApiKeyIdAndYearAndMonth(apiKeyId, year, month)
                    .orElseThrow(() -> new IllegalStateException("Consumption record not found after increment"));
        } else {
            // No existing record, create new one
            log.info("Consumption recorded - Creating new record for API Key ID: {} in {}/{}", 
                apiKeyId, year, month);
            return createNewConsumptionRecord(apiKeyId, year, month);
        }
    }
    
    /**
     * Creates a new consumption record for the specified API key and month.
     * Handles potential race conditions where multiple threads try to create the same record.
     * 
     * @param apiKeyId the API key ID
     * @param year the year
     * @param month the month
     * @return the created consumption record
     */
    private Consumption createNewConsumptionRecord(Long apiKeyId, Integer year, Integer month) {
        try {
            Consumption newConsumption = new Consumption();
            newConsumption.setApiKeyId(apiKeyId);
            newConsumption.setYear(year);
            newConsumption.setMonth(month);
            newConsumption.setUsageCount(1);
            
            Consumption saved = consumptionRepository.save(newConsumption);
            log.info("Consumption record created - ID: {}, API Key ID: {}, Period: {}/{}, Initial count: 1", 
                    saved.getId(), apiKeyId, year, month);
            return saved;
            
        } catch (Exception e) {
            // Handle race condition: another thread might have created the record
            log.warn("Race condition detected while creating consumption record for API Key ID: {} in {}/{}, attempting to increment existing record", 
                    apiKeyId, year, month);
            
            // Try to increment the record that was created by another thread
            int updatedRows = consumptionRepository.incrementUsageCount(apiKeyId, year, month);
            if (updatedRows > 0) {
                log.info("Consumption recorded - Resolved race condition by incrementing existing record for API Key ID: {} in {}/{}", 
                    apiKeyId, year, month);
                return consumptionRepository.findByApiKeyIdAndYearAndMonth(apiKeyId, year, month)
                        .orElseThrow(() -> new IllegalStateException("Consumption record not found after race condition handling"));
            } else {
                // If still no record exists, re-throw the original exception
                log.error("Consumption recording failed - Unable to create or increment record for API Key ID: {} in {}/{}: {}", 
                    apiKeyId, year, month, e.getMessage());
                throw new RuntimeException("Failed to create consumption record for API key " + apiKeyId, e);
            }
        }
    }
    
    /**
     * Retrieves consumption data for a specific API key and month
     * 
     * @param apiKeyId the API key ID
     * @param year the year
     * @param month the month (1-12)
     * @return Optional containing the consumption record if found
     */
    public Optional<Consumption> getConsumption(Long apiKeyId, Integer year, Integer month) {
        if (apiKeyId == null || year == null || month == null) {
            throw new IllegalArgumentException("API key ID, year, and month cannot be null");
        }
        
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        
        return consumptionRepository.findByApiKeyIdAndYearAndMonth(apiKeyId, year, month);
    }
    
    /**
     * Retrieves all consumption records for a specific API key, ordered by most recent first
     * 
     * @param apiKeyId the API key ID
     * @return List of consumption records for the API key
     */
    public List<Consumption> getConsumptionHistory(Long apiKeyId) {
        if (apiKeyId == null) {
            throw new IllegalArgumentException("API key ID cannot be null");
        }
        
        return consumptionRepository.findMonthlyAggregationByApiKeyId(apiKeyId);
    }
    
    /**
     * Gets the total usage count for a specific API key in a given month
     * 
     * @param apiKeyId the API key ID
     * @param year the year
     * @param month the month (1-12)
     * @return the total usage count (0 if no records found)
     */
    public Integer getTotalUsage(Long apiKeyId, Integer year, Integer month) {
        if (apiKeyId == null || year == null || month == null) {
            throw new IllegalArgumentException("API key ID, year, and month cannot be null");
        }
        
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        
        return consumptionRepository.getTotalUsageByApiKeyIdAndYearAndMonth(apiKeyId, year, month);
    }
    
    /**
     * Gets consumption summary for all API keys in a specific month
     * 
     * @param year the year
     * @param month the month (1-12)
     * @return List of consumption records ordered by usage count (highest first)
     */
    public List<Consumption> getConsumptionSummary(Integer year, Integer month) {
        if (year == null || month == null) {
            throw new IllegalArgumentException("Year and month cannot be null");
        }
        
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        
        return consumptionRepository.getConsumptionSummaryByPeriod(year, month);
    }
    
    /**
     * Gets all consumption records for a specific API key
     * 
     * @param apiKeyId the API key ID
     * @return List of all consumption records for the API key
     */
    public List<Consumption> getAllConsumptionByApiKey(Long apiKeyId) {
        if (apiKeyId == null) {
            throw new IllegalArgumentException("API key ID cannot be null");
        }
        
        return consumptionRepository.findByApiKeyId(apiKeyId);
    }
}