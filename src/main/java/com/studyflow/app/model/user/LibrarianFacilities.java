package com.studyflow.app.model.user;

import com.studyflow.app.model.facility.Facility;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "librarian_facilities")
public class LibrarianFacilities {
    @Id
    @JoinColumn(name = "user_id")
    @OneToOne
    private User user;

    @ManyToOne
    private Facility facility;
}
