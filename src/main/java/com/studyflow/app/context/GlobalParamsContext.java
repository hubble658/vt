package com.studyflow.app.context;

import com.studyflow.app.model.facility.Desk;
import com.studyflow.app.model.facility.Facility;
import com.studyflow.app.model.facility.FacilityBlock;
import com.studyflow.app.model.facility.Seat;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Component
public class GlobalParamsContext {
    private Facility selectedFacility;
    private FacilityBlock selectedFacilityBlock;
    private Desk selectedDesk;
    private Seat selectedSeat;

    // YENİ: Rezervasyon Zamanı
    private LocalDate selectedDate;
    private LocalTime selectedStartTime;
    private LocalTime selectedEndTime;

    public void clearReservationData() {
        this.selectedFacilityBlock = null;
        this.selectedDesk = null;
        this.selectedSeat = null;
        this.selectedDate = null;
        this.selectedStartTime = null;
        this.selectedEndTime = null;
    }
}