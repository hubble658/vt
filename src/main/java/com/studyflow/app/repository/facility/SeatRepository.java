package com.studyflow.app.repository.facility;

import com.studyflow.app.model.facility.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Query(value = "SELECT * FROM seats WHERE desk_id = :deskId ORDER BY seat_number ASC", nativeQuery = true)
    List<Seat> findAllByDeskId(Long deskId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO seats (desk_id, seat_number, rel_x, rel_y) VALUES (:deskId, :seatNumber, :relX, :relY)", nativeQuery = true)
    void insertSeatWithCoordinates(Long deskId, Integer seatNumber, Double relX, Double relY);

    @Modifying
    @Transactional
    @Query(value = "UPDATE seats SET seat_number = :seatNumber, rel_x = :relX, rel_y = :relY WHERE id = :id", nativeQuery = true)
    void updateSeat(Long id, Integer seatNumber, Double relX, Double relY);

    @Modifying
    @Transactional
    @Query(value = "SELECT * FROM seats WHERE id = :id", nativeQuery = true)
    Seat findSeatById(Long id);

}