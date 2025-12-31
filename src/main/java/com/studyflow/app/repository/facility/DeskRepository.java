package com.studyflow.app.repository.facility;

import com.studyflow.app.model.facility.Desk;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeskRepository extends JpaRepository<Desk, Long> {

    @Query(value = "SELECT * FROM desks WHERE facility_block_id = :blockId", nativeQuery = true)
    List<Desk> findAllByBlockId(@Param("blockId") Long blockId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO desks (size, id_range, current_id_index, pos_x, pos_y, width, height, color_hex, facility_block_id) " +
            "VALUES (:size, :idRange, :currentIndex, :x, :y, :width, :height, :colorHex, :blockId)", nativeQuery = true)
    void insertDesk(@Param("size") Integer size,
                    @Param("idRange") String idRange,
                    @Param("currentIndex") Integer currentIndex,
                    @Param("x") double x,
                    @Param("y") double y,
                    @Param("width") double width,
                    @Param("height") double height,
                    @Param("colorHex") String colorHex,
                    @Param("blockId") Long blockId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE desks SET size = :size, pos_x = :x, pos_y = :y, width = :width, height = :height, color_hex = :colorHex " +
            "WHERE id = :id", nativeQuery = true)
    void updateDesk(@Param("id") Long id,
                    @Param("size") Integer size,
                    @Param("x") double x,
                    @Param("y") double y,
                    @Param("width") double width,
                    @Param("height") double height,
                    @Param("colorHex") String colorHex);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM desks WHERE id = :id", nativeQuery = true)
    void deleteDeskById(@Param("id") Long id);
}