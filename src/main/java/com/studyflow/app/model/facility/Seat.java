package com.studyflow.app.model.facility;

import com.studyflow.app.model.reservation.Reservation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "seats")
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Masadaki görünen numara (Örn: 1, 2, 3...)
    @Column(name = "seat_number")
    private Integer seatNumber;

    // Masa üzerindeki göreceli X konumu
    @Column(name = "rel_x")
    private Double relX;

    // Masa üzerindeki göreceli Y konumu
    @Column(name = "rel_y")
    private Double relY;

    @ManyToOne
    @JoinColumn(name = "desk_id")
    private Desk desk;

    @OneToMany(mappedBy = "seat")
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();
}