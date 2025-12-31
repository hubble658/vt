package com.studyflow.app.model.reservation;

import com.studyflow.app.model.facility.Desk;
import com.studyflow.app.model.facility.Facility;
import com.studyflow.app.model.facility.FacilityBlock;
import com.studyflow.app.model.facility.Seat;
import com.studyflow.app.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reservations")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne()
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @ManyToOne()
    @JoinColumn(name = "facility_block_id")
    private FacilityBlock facilityBlock;

    @ManyToOne()
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @ManyToOne()
    @JoinColumn(name = "desk_id")
    private Desk desk;

    @Column(nullable = false)
    private LocalDate reservationDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    // Yeni alanlar - Rezervasyon durumu ve iptal bilgileri
    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, COMPLETED, CANCELLED

    @Column(length = 255)
    private String cancellationReason;

    @Column
    private LocalDateTime cancelledAt;

    // Yardımcı metod: İptal edilebilir mi kontrolü (1 saat kuralı)
    public boolean isCancellable() {
        if (!"ACTIVE".equals(this.status)) {
            return false;
        }
        LocalDateTime reservationStart = LocalDateTime.of(this.reservationDate, this.startTime);
        LocalDateTime cancelDeadline = reservationStart.minusHours(1);
        return LocalDateTime.now().isBefore(cancelDeadline);
    }

    // İptal için kalan süreyi dakika cinsinden döndür
    public long getMinutesUntilCancelDeadline() {
        LocalDateTime reservationStart = LocalDateTime.of(this.reservationDate, this.startTime);
        LocalDateTime cancelDeadline = reservationStart.minusHours(1);
        return java.time.Duration.between(LocalDateTime.now(), cancelDeadline).toMinutes();
    }
}