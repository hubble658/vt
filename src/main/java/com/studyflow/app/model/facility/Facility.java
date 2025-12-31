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
@Table(name = "facilities")
public class Facility {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String address;

    private String imageUrl;

    @OneToOne(mappedBy = "facility")
    private WeeklyCalendar weeklyCalendar;

    @OneToMany(mappedBy = "facility")
    private List<FacilityBlock> blocks = new ArrayList<>();
}
