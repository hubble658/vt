package com.studyflow.app.repository.facility;

import com.studyflow.app.model.facility.FacilityBlock;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacilityBlockRepository extends JpaRepository<FacilityBlock, Long> {

    @Query(value = "SELECT * FROM facility_blocks WHERE facility_id = :facilityId", nativeQuery = true)
    List<FacilityBlock> findAllByFacilityId(@Param("facilityId") Long facilityId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO facility_blocks (name, pos_x, pos_y, width, height, color_hex, current_id_index, facility_id) " +
            "VALUES (:name, :x, :y, :width, :height, :colorHex, :currentIndex, :facilityId)", nativeQuery = true)
    void insertBlock(@Param("name") String name,
                     @Param("x") double x,
                     @Param("y") double y,
                     @Param("width") double width,
                     @Param("height") double height,
                     @Param("colorHex") String colorHex,
                     @Param("currentIndex") Integer currentIndex,
                     @Param("facilityId") Long facilityId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE facility_blocks SET " +
            "name = :name, pos_x = :x, pos_y = :y, width = :width, height = :height, color_hex = :colorHex " +
            "WHERE id = :id", nativeQuery = true)
    void updateBlock(@Param("id") Long id,
                     @Param("name") String name,
                     @Param("x") double x,
                     @Param("y") double y,
                     @Param("width") double width,
                     @Param("height") double height,
                     @Param("colorHex") String colorHex);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM facility_blocks WHERE id = :id", nativeQuery = true)
    void deleteBlockById(@Param("id") Long id);

    // EKLENEN METOT: Blok indeksini güncelle
    @Modifying
    @Transactional
    @Query(value = "UPDATE facility_blocks SET current_id_index = :currentIndex WHERE id = :id", nativeQuery = true)
    void updateBlockCurrentIndex(@Param("id") Long id, @Param("currentIndex") Integer currentIndex);
}