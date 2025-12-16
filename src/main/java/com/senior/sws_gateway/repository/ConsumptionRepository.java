package com.senior.sws_gateway.repository;

import com.senior.sws_gateway.model.Consumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumptionRepository extends JpaRepository<Consumption, Long> {
    
    /**
     * Find consumption record by API key ID, year, and month
     * @param apiKeyId the API key ID
     * @param year the year
     * @param month the month
     * @return Optional containing the consumption record if found
     */
    Optional<Consumption> findByApiKeyIdAndYearAndMonth(Long apiKeyId, Integer year, Integer month);
    
    /**
     * Find all consumption records for a specific API key
     * @param apiKeyId the API key ID
     * @return List of consumption records for the API key
     */
    List<Consumption> findByApiKeyId(Long apiKeyId);
    
    /**
     * Find all consumption records for a specific year and month
     * @param year the year
     * @param month the month
     * @return List of consumption records for the specified period
     */
    List<Consumption> findByYearAndMonth(Integer year, Integer month);
    
    /**
     * Get monthly aggregated consumption by API key ID
     * @param apiKeyId the API key ID
     * @return List of consumption records aggregated by month
     */
    @Query("SELECT c FROM Consumption c WHERE c.apiKeyId = :apiKeyId ORDER BY c.year DESC, c.month DESC")
    List<Consumption> findMonthlyAggregationByApiKeyId(@Param("apiKeyId") Long apiKeyId);
    
    /**
     * Get total usage count for a specific API key in a given year and month
     * @param apiKeyId the API key ID
     * @param year the year
     * @param month the month
     * @return the total usage count
     */
    @Query("SELECT COALESCE(SUM(c.usageCount), 0) FROM Consumption c WHERE c.apiKeyId = :apiKeyId AND c.year = :year AND c.month = :month")
    Integer getTotalUsageByApiKeyIdAndYearAndMonth(@Param("apiKeyId") Long apiKeyId, @Param("year") Integer year, @Param("month") Integer month);
    
    /**
     * Increment usage count for existing consumption record
     * @param apiKeyId the API key ID
     * @param year the year
     * @param month the month
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE Consumption c SET c.usageCount = c.usageCount + 1, c.updatedAt = CURRENT_TIMESTAMP WHERE c.apiKeyId = :apiKeyId AND c.year = :year AND c.month = :month")
    int incrementUsageCount(@Param("apiKeyId") Long apiKeyId, @Param("year") Integer year, @Param("month") Integer month);
    
    /**
     * Get consumption summary grouped by API key for a specific period
     * @param year the year
     * @param month the month
     * @return List of consumption records for the specified period
     */
    @Query("SELECT c FROM Consumption c WHERE c.year = :year AND c.month = :month ORDER BY c.usageCount DESC")
    List<Consumption> getConsumptionSummaryByPeriod(@Param("year") Integer year, @Param("month") Integer month);
}