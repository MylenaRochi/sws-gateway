package com.senior.sws_gateway.service;

import com.senior.sws_gateway.model.Consumption;
import com.senior.sws_gateway.repository.ConsumptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumptionServiceTest {

    @Mock
    private ConsumptionRepository consumptionRepository;

    @InjectMocks
    private ConsumptionService consumptionService;

    private Consumption existingConsumption;
    private Long apiKeyId;
    private Integer currentYear;
    private Integer currentMonth;

    @BeforeEach
    void setUp() {
        apiKeyId = 1L;
        YearMonth currentYearMonth = YearMonth.now();
        currentYear = currentYearMonth.getYear();
        currentMonth = currentYearMonth.getMonthValue();

        existingConsumption = new Consumption();
        existingConsumption.setId(1L);
        existingConsumption.setApiKeyId(apiKeyId);
        existingConsumption.setYear(currentYear);
        existingConsumption.setMonth(currentMonth);
        existingConsumption.setUsageCount(5);
        existingConsumption.setCreatedAt(LocalDateTime.now());
        existingConsumption.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void recordConsumption_WithExistingRecord_ShouldIncrementUsageCount() {
        // Given
        when(consumptionRepository.incrementUsageCount(apiKeyId, currentYear, currentMonth)).thenReturn(1);
        when(consumptionRepository.findByApiKeyIdAndYearAndMonth(apiKeyId, currentYear, currentMonth))
                .thenReturn(Optional.of(existingConsumption));

        // When
        Consumption result = consumptionService.recordConsumption(apiKeyId);

        // Then
        assertNotNull(result);
        assertEquals(existingConsumption, result);
        verify(consumptionRepository).incrementUsageCount(apiKeyId, currentYear, currentMonth);
        verify(consumptionRepository).findByApiKeyIdAndYearAndMonth(apiKeyId, currentYear, currentMonth);
        verify(consumptionRepository, never()).save(any(Consumption.class));
    }

    @Test
    void recordConsumption_WithNoExistingRecord_ShouldCreateNewRecord() {
        // Given
        when(consumptionRepository.incrementUsageCount(apiKeyId, currentYear, currentMonth)).thenReturn(0);
        
        Consumption newConsumption = new Consumption();
        newConsumption.setId(2L);
        newConsumption.setApiKeyId(apiKeyId);
        newConsumption.setYear(currentYear);
        newConsumption.setMonth(currentMonth);
        newConsumption.setUsageCount(1);
        
        when(consumptionRepository.save(any(Consumption.class))).thenReturn(newConsumption);

        // When
        Consumption result = consumptionService.recordConsumption(apiKeyId);

        // Then
        assertNotNull(result);
        assertEquals(newConsumption, result);
        verify(consumptionRepository).incrementUsageCount(apiKeyId, currentYear, currentMonth);
        verify(consumptionRepository).save(any(Consumption.class));
    }

    @Test
    void recordConsumption_WithNullApiKeyId_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.recordConsumption(null));
        
        assertEquals("API key ID cannot be null", exception.getMessage());
        verifyNoInteractions(consumptionRepository);
    }

    @Test
    void recordConsumption_WithRaceCondition_ShouldHandleGracefully() {
        // Given - first increment fails (no existing record)
        when(consumptionRepository.incrementUsageCount(apiKeyId, currentYear, currentMonth)).thenReturn(0);
        
        // Save throws exception (race condition - another thread created the record)
        when(consumptionRepository.save(any(Consumption.class)))
                .thenThrow(new RuntimeException("Duplicate key constraint"));
        
        // Second increment succeeds (record now exists from other thread)
        when(consumptionRepository.incrementUsageCount(apiKeyId, currentYear, currentMonth)).thenReturn(1);
        when(consumptionRepository.findByApiKeyIdAndYearAndMonth(apiKeyId, currentYear, currentMonth))
                .thenReturn(Optional.of(existingConsumption));

        // When
        Consumption result = consumptionService.recordConsumption(apiKeyId);

        // Then
        assertNotNull(result);
        assertEquals(existingConsumption, result);
        verify(consumptionRepository, times(2)).incrementUsageCount(apiKeyId, currentYear, currentMonth);
        verify(consumptionRepository).save(any(Consumption.class));
        verify(consumptionRepository).findByApiKeyIdAndYearAndMonth(apiKeyId, currentYear, currentMonth);
    }

    @Test
    void getConsumption_WithValidParameters_ShouldReturnConsumption() {
        // Given
        when(consumptionRepository.findByApiKeyIdAndYearAndMonth(apiKeyId, currentYear, currentMonth))
                .thenReturn(Optional.of(existingConsumption));

        // When
        Optional<Consumption> result = consumptionService.getConsumption(apiKeyId, currentYear, currentMonth);

        // Then
        assertTrue(result.isPresent());
        assertEquals(existingConsumption, result.get());
        verify(consumptionRepository).findByApiKeyIdAndYearAndMonth(apiKeyId, currentYear, currentMonth);
    }

    @Test
    void getConsumption_WithNullParameters_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getConsumption(null, currentYear, currentMonth));
        
        assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getConsumption(apiKeyId, null, currentMonth));
        
        assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getConsumption(apiKeyId, currentYear, null));
        
        verifyNoInteractions(consumptionRepository);
    }

    @Test
    void getConsumption_WithInvalidMonth_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getConsumption(apiKeyId, currentYear, 0));
        
        assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getConsumption(apiKeyId, currentYear, 13));
        
        verifyNoInteractions(consumptionRepository);
    }

    @Test
    void getConsumptionHistory_WithValidApiKeyId_ShouldReturnHistory() {
        // Given
        List<Consumption> expectedHistory = Arrays.asList(existingConsumption);
        when(consumptionRepository.findMonthlyAggregationByApiKeyId(apiKeyId))
                .thenReturn(expectedHistory);

        // When
        List<Consumption> result = consumptionService.getConsumptionHistory(apiKeyId);

        // Then
        assertNotNull(result);
        assertEquals(expectedHistory, result);
        verify(consumptionRepository).findMonthlyAggregationByApiKeyId(apiKeyId);
    }

    @Test
    void getConsumptionHistory_WithNullApiKeyId_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getConsumptionHistory(null));
        
        assertEquals("API key ID cannot be null", exception.getMessage());
        verifyNoInteractions(consumptionRepository);
    }

    @Test
    void getTotalUsage_WithValidParameters_ShouldReturnUsageCount() {
        // Given
        Integer expectedUsage = 10;
        when(consumptionRepository.getTotalUsageByApiKeyIdAndYearAndMonth(apiKeyId, currentYear, currentMonth))
                .thenReturn(expectedUsage);

        // When
        Integer result = consumptionService.getTotalUsage(apiKeyId, currentYear, currentMonth);

        // Then
        assertEquals(expectedUsage, result);
        verify(consumptionRepository).getTotalUsageByApiKeyIdAndYearAndMonth(apiKeyId, currentYear, currentMonth);
    }

    @Test
    void getTotalUsage_WithNullParameters_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getTotalUsage(null, currentYear, currentMonth));
        
        assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getTotalUsage(apiKeyId, null, currentMonth));
        
        assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getTotalUsage(apiKeyId, currentYear, null));
        
        verifyNoInteractions(consumptionRepository);
    }

    @Test
    void getTotalUsage_WithInvalidMonth_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getTotalUsage(apiKeyId, currentYear, 0));
        
        assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getTotalUsage(apiKeyId, currentYear, 13));
        
        verifyNoInteractions(consumptionRepository);
    }

    @Test
    void getConsumptionSummary_WithValidParameters_ShouldReturnSummary() {
        // Given
        List<Consumption> expectedSummary = Arrays.asList(existingConsumption);
        when(consumptionRepository.getConsumptionSummaryByPeriod(currentYear, currentMonth))
                .thenReturn(expectedSummary);

        // When
        List<Consumption> result = consumptionService.getConsumptionSummary(currentYear, currentMonth);

        // Then
        assertNotNull(result);
        assertEquals(expectedSummary, result);
        verify(consumptionRepository).getConsumptionSummaryByPeriod(currentYear, currentMonth);
    }

    @Test
    void getConsumptionSummary_WithNullParameters_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getConsumptionSummary(null, currentMonth));
        
        assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getConsumptionSummary(currentYear, null));
        
        verifyNoInteractions(consumptionRepository);
    }

    @Test
    void getConsumptionSummary_WithInvalidMonth_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getConsumptionSummary(currentYear, 0));
        
        assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getConsumptionSummary(currentYear, 13));
        
        verifyNoInteractions(consumptionRepository);
    }

    @Test
    void getAllConsumptionByApiKey_WithValidApiKeyId_ShouldReturnAllConsumption() {
        // Given
        List<Consumption> expectedConsumption = Arrays.asList(existingConsumption);
        when(consumptionRepository.findByApiKeyId(apiKeyId)).thenReturn(expectedConsumption);

        // When
        List<Consumption> result = consumptionService.getAllConsumptionByApiKey(apiKeyId);

        // Then
        assertNotNull(result);
        assertEquals(expectedConsumption, result);
        verify(consumptionRepository).findByApiKeyId(apiKeyId);
    }

    @Test
    void getAllConsumptionByApiKey_WithNullApiKeyId_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> consumptionService.getAllConsumptionByApiKey(null));
        
        assertEquals("API key ID cannot be null", exception.getMessage());
        verifyNoInteractions(consumptionRepository);
    }
}