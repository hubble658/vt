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
@Table(name = "facility_blocks")
public class FacilityBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Integer currentIdIndex;

    @ManyToOne()
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @OneToMany(mappedBy = "facilityBlock")
    private List<Desk> desks = new ArrayList<>();

    @Column(name = "pos_x")
    private double x;

    @Column(name = "pos_y")
    private double y;

    @Column(name = "width")
    private double width;

    @Column(name = "height")
    private double height;

    private String colorHex;
}
