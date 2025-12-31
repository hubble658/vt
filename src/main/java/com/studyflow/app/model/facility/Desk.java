package com.studyflow.app.model.facility;

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
@Table(name = "desks")
public class Desk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer size;

    // 10-20
    private String idRange;

    private Integer currentIdIndex;

    // --- GÖRSEL KOORDİNATLAR ---
    @Column(name = "pos_x")
    private double x;

    @Column(name = "pos_y")
    private double y;

    @Column(name = "width")
    private double width;

    @Column(name = "height")
    private double height;

    private String colorHex;

    @ManyToOne
    @JoinColumn(name = "facility_block_id")
    private FacilityBlock facilityBlock;

    @OneToMany(mappedBy = "desk")
    private List<Seat> seats = new ArrayList<>();
}