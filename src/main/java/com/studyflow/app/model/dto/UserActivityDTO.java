package com.studyflow.app.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Kullanıcı aktivite geçmişi için DTO (UNION sorgusu sonucu)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityDTO {
    private String activityType; // ACTIVE_RESERVATION, COMPLETED_RESERVATION, CANCELLED_RESERVATION
    private LocalDate activityDate;
    private LocalTime activityTime;
    private String description;
    
    // Ek bilgiler
    private Long reservationId;
    private String facilityName;
    private String blockName;
    private Integer seatNumber;
    private String status;
    private boolean isCancellable;
    private Integer durationMinutes;
}
