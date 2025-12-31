package com.studyflow.app.model.facility;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySchedule {
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private LocalTime openTime;
    private LocalTime closeTime;

    private Boolean isClosed;
}
