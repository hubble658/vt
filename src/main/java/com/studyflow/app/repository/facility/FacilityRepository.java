package com.studyflow.app.repository.facility;

import com.studyflow.app.model.facility.Facility;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO facilities(name, address, image_url)" +
            "VALUES(:name, :address, :imageUrl)", nativeQuery = true)
    void saveNewFacility(@Param("name") String name, @Param("address") String address, @Param("imageUrl") String imageUrl);


    @Query(value = "SELECT * FROM facilities WHERE id = :facilityId", nativeQuery = true)
    Facility getFacilityById(@Param("facilityId") Long facilityId);

    @Query(value = "SELECT * FROM facilities WHERE image_url = :imageUrl", nativeQuery = true)
    Facility getFacilityByImageUrl(@Param("imageUrl") String imageUrl);


    @Query(value = "SELECT * FROM facilities f " +
                    "ORDER BY " +
                    "CASE WHEN UPPER(:order) = 'ASC'  THEN f.name END ASC, " +
                    "CASE WHEN UPPER(:order) = 'DESC' THEN f.name END DESC", nativeQuery = true)
    List<Facility> getAllFacilitiesByOrder(@Param("order") String order);
}
