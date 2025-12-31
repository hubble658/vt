package com.studyflow.app.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityDTO {
    private Long id;        // Block ID veya Desk ID
    private int totalSeats;
    private int occupiedSeats;

    public int getFreeSeats() {
        return totalSeats - occupiedSeats;
    }

    // Doluluk oranına göre renk durumunu belirler (Frontend mantığı için yardımcı)
    public String getAvailabilityStatus() {
        if (getFreeSeats() == 0) return "FULL";   // Gri
        if (getFreeSeats() > 5) return "GOOD";    // Yeşil
        return "LIMITED";                         // Turuncu
    }
}