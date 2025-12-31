package com.studyflow.app.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Tesis doluluk ve istatistik bilgilerini taşıyan DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityOccupancyDTO {
    private Long facilityId;
    private String facilityName;
    private Integer totalSeats;
    private Integer occupiedSeats;
    private BigDecimal occupancyRate;
    
    // En uygun zaman önerisi için
    private LocalTime suggestedStartTime;
    private LocalTime suggestedEndTime;
    private Integer availableSeatsAtSuggestedTime;
    
    // İstatistikler
    private Long totalBlocks;
    private Long totalDesks;
    private Long activeReservations;
    private Long completedReservations;
    private Long cancelledReservations;
}
