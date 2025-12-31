package com.studyflow.app.model.user;

import com.studyflow.app.model.facility.Facility;
import com.studyflow.app.model.reservation.Reservation;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @Email(regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    @Length(max = 128)
    @NotBlank
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Size(min = 2, max = 50)
    @NotBlank
    @Pattern(regexp = "^[\\p{L}]+(?: [\\p{L}]+)*$")
    private String firstName;

    @Size(min = 2, max = 30)
    @NotBlank
    @Pattern(regexp = "^[\\p{L}]+(?: [\\p{L}]+)*$")
    private String lastName;

    @OneToMany(mappedBy = "user")
    private List<Reservation> reservations = new ArrayList<>();

    @OneToOne(mappedBy = "user")
    private LibrarianFacilities facilityInfo;
}
