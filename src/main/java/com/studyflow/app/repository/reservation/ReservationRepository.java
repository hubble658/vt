package com.studyflow.app.repository.reservation;

import com.studyflow.app.model.reservation.Reservation;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // ============================================
    // 1. TEMEL REZERVASYON İŞLEMLERİ
    // ============================================

    // Rezervasyon ekleme (status ile)
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO reservations (user_id, seat_id, facility_id, facility_block_id, desk_id, reservation_date, start_time, end_time, status) " +
            "VALUES (:userId, :seatId, :facilityId, :facilityBlockId, :deskId, :date, :startTime, :endTime, 'ACTIVE')", nativeQuery = true)
    void createReservation(@Param("userId") Long userId,
                           @Param("seatId") Long seatId,
                           @Param("facilityId") Long facilityId,
                           @Param("facilityBlockId") Long facilityBlockId,
                           @Param("deskId") Long deskId,
                           @Param("date") LocalDate date,
                           @Param("startTime") LocalTime startTime,
                           @Param("endTime") LocalTime endTime);

    // ============================================
    // 2. REZERVASYON GÜNCELLEME (UPDATE)
    // ============================================

    // Rezervasyon zamanını güncelle
    @Modifying
    @Transactional
    @Query(value = "UPDATE reservations SET " +
            "reservation_date = :newDate, " +
            "start_time = :newStartTime, " +
            "end_time = :newEndTime " +
            "WHERE id = :reservationId AND status = 'ACTIVE'", nativeQuery = true)
    int updateReservationTime(@Param("reservationId") Long reservationId,
                              @Param("newDate") LocalDate newDate,
                              @Param("newStartTime") LocalTime newStartTime,
                              @Param("newEndTime") LocalTime newEndTime);

    // ============================================
    // 3. REZERVASYON İPTAL (DELETE/CANCEL)
    // ============================================

    // Rezervasyon iptal et (status'ü CANCELLED yap - Trigger tetiklenir)
    @Modifying
    @Transactional
    @Query(value = "UPDATE reservations SET " +
            "status = 'CANCELLED', " +
            "cancellation_reason = :reason, " +
            "cancelled_at = CURRENT_TIMESTAMP " +
            "WHERE id = :reservationId AND status = 'ACTIVE'", nativeQuery = true)
    int cancelReservation(@Param("reservationId") Long reservationId,
                          @Param("reason") String reason);

    // Süresi dolmuş rezervasyonları tamamlandı olarak işaretle
    @Modifying
    @Transactional
    @Query(value = "UPDATE reservations SET status = 'COMPLETED' " +
            "WHERE status = 'ACTIVE' " +
            "AND (reservation_date < :currentDate " +
            "OR (reservation_date = :currentDate AND end_time <= :currentTime))", nativeQuery = true)
    int markExpiredReservationsAsCompleted(@Param("currentDate") LocalDate currentDate,
                                           @Param("currentTime") LocalTime currentTime);

    // Eski metod - geriye dönük uyumluluk
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM reservations WHERE id = :reservationId", nativeQuery = true)
    void deleteExpiredReservation(@Param("reservationId") Long reservationId);

    // ============================================
    // 4. ÇAKIŞMA KONTROLÜ
    // ============================================

    // Aynı koltukta çakışma kontrolü
    @Query(value = "SELECT COUNT(*) FROM reservations " +
            "WHERE seat_id = :seatId " +
            "AND reservation_date = :date " +
            "AND status = 'ACTIVE' " +
            "AND (start_time < :endTime AND end_time > :startTime)", nativeQuery = true)
    int countConflictingReservations(@Param("seatId") Long seatId,
                                     @Param("date") LocalDate date,
                                     @Param("startTime") LocalTime startTime,
                                     @Param("endTime") LocalTime endTime);

    // Kullanıcı bazlı zaman çakışması kontrolü (aynı kullanıcı aynı saatte başka yerde rezervasyon var mı?)
    @Query(value = "SELECT COUNT(*) FROM reservations " +
            "WHERE user_id = :userId " +
            "AND reservation_date = :date " +
            "AND status = 'ACTIVE' " +
            "AND (start_time < :endTime AND end_time > :startTime)", nativeQuery = true)
    int countUserTimeConflicts(@Param("userId") Long userId,
                               @Param("date") LocalDate date,
                               @Param("startTime") LocalTime startTime,
                               @Param("endTime") LocalTime endTime);

    // Güncelleme için çakışma kontrolü (kendi rezervasyonu hariç)
    @Query(value = "SELECT COUNT(*) FROM reservations " +
            "WHERE seat_id = :seatId " +
            "AND reservation_date = :date " +
            "AND status = 'ACTIVE' " +
            "AND id != :excludeReservationId " +
            "AND (start_time < :endTime AND end_time > :startTime)", nativeQuery = true)
    int countConflictingReservationsExcluding(@Param("seatId") Long seatId,
                                               @Param("date") LocalDate date,
                                               @Param("startTime") LocalTime startTime,
                                               @Param("endTime") LocalTime endTime,
                                               @Param("excludeReservationId") Long excludeReservationId);

    // Güncelleme için kullanıcı bazlı zaman çakışması kontrolü (kendi rezervasyonu hariç)
    @Query(value = "SELECT COUNT(*) FROM reservations " +
            "WHERE user_id = :userId " +
            "AND reservation_date = :date " +
            "AND status = 'ACTIVE' " +
            "AND id != :excludeReservationId " +
            "AND (start_time < :endTime AND end_time > :startTime)", nativeQuery = true)
    int countUserTimeConflictsExcluding(@Param("userId") Long userId,
                                         @Param("date") LocalDate date,
                                         @Param("startTime") LocalTime startTime,
                                         @Param("endTime") LocalTime endTime,
                                         @Param("excludeReservationId") Long excludeReservationId);

    // ============================================
    // 5. DOLU KOLTUKLAR
    // ============================================

    @Query(value = "SELECT s.id FROM reservations r " +
            "JOIN seats s ON r.seat_id = s.id " +
            "WHERE s.desk_id = :deskId " +
            "AND r.reservation_date = :date " +
            "AND r.status = 'ACTIVE' " +
            "AND (r.start_time < :endTime AND r.end_time > :startTime)", nativeQuery = true)
    List<Long> findOccupiedSeatIdsByDesk(@Param("deskId") Long deskId,
                                         @Param("date") LocalDate date,
                                         @Param("startTime") LocalTime startTime,
                                         @Param("endTime") LocalTime endTime);

    // ============================================
    // 6. KULLANICI REZERVASYONLARI
    // ============================================

    @Query(value = "SELECT * FROM reservations WHERE user_id = :userId ORDER BY reservation_date ASC, start_time ASC", nativeQuery = true)
    List<Reservation> findAllByUserId(@Param("userId") Long userId);

    // AKTİF REZERVASYONLAR (status = ACTIVE ve gelecekte)
    @Query(value = "SELECT * FROM reservations " +
            "WHERE user_id = :userId " +
            "AND status = 'ACTIVE' " +
            "AND (reservation_date > :currentDate OR (reservation_date = :currentDate AND end_time > :currentTime)) " +
            "ORDER BY reservation_date ASC, start_time ASC", nativeQuery = true)
    List<Reservation> findActiveReservationsByUserId(@Param("userId") Long userId,
                                                     @Param("currentDate") LocalDate currentDate,
                                                     @Param("currentTime") LocalTime currentTime);

    // GEÇMİŞ REZERVASYONLAR (COMPLETED veya CANCELLED veya süresi dolmuş)
    @Query(value = "SELECT * FROM reservations " +
            "WHERE user_id = :userId " +
            "AND (status IN ('COMPLETED', 'CANCELLED') " +
            "OR (reservation_date < :currentDate OR (reservation_date = :currentDate AND end_time <= :currentTime))) " +
            "ORDER BY reservation_date DESC, start_time DESC", nativeQuery = true)
    List<Reservation> findPastReservationsByUserId(@Param("userId") Long userId,
                                                   @Param("currentDate") LocalDate currentDate,
                                                   @Param("currentTime") LocalTime currentTime);

    // İptal edilen rezervasyonlar
    @Query(value = "SELECT * FROM reservations " +
            "WHERE user_id = :userId AND status = 'CANCELLED' " +
            "ORDER BY cancelled_at DESC", nativeQuery = true)
    List<Reservation> findCancelledReservationsByUserId(@Param("userId") Long userId);

    // Kullanıcının aktif rezervasyon sayısı (Trigger kontrolü için)
    @Query(value = "SELECT COUNT(*) FROM reservations " +
            "WHERE user_id = :userId " +
            "AND status = 'ACTIVE' " +
            "AND (reservation_date > CURRENT_DATE " +
            "OR (reservation_date = CURRENT_DATE AND end_time > CURRENT_TIME))", nativeQuery = true)
    int countActiveReservationsByUserId(@Param("userId") Long userId);

    // ============================================
    // 7. TESİS SORGULARI
    // ============================================

    @Query(value = "SELECT start_time, end_time FROM reservations " +
            "WHERE facility_id = :facilityId AND reservation_date = :date AND status = 'ACTIVE'", nativeQuery = true)
    List<Object[]> findReservationTimesForFacility(@Param("facilityId") Long facilityId, @Param("date") LocalDate date);

    @Query(value = "SELECT * FROM reservations WHERE status = 'ACTIVE'", nativeQuery = true)
    List<Reservation> getAllActiveReservations();

    // ============================================
    // 8. VIEW SORGULARI (Arayüzden çağrılan)
    // ============================================

    // Aktif rezervasyonlar view'ından sorgula
    @Query(value = "SELECT * FROM vw_active_reservations WHERE user_id = :userId", nativeQuery = true)
    List<Object[]> findActiveReservationsFromView(@Param("userId") Long userId);

    // Tesis istatistikleri view'ından sorgula
    @Query(value = "SELECT * FROM vw_facility_statistics WHERE facility_id = :facilityId", nativeQuery = true)
    List<Object[]> getFacilityStatisticsFromView(@Param("facilityId") Long facilityId);

    // Tüm tesis istatistikleri
    @Query(value = "SELECT * FROM vw_facility_statistics ORDER BY active_reservations DESC", nativeQuery = true)
    List<Object[]> getAllFacilityStatistics();

    // Kullanıcı profil istatistikleri view'ından
    @Query(value = "SELECT * FROM vw_user_profile_stats WHERE user_id = :userId", nativeQuery = true)
    List<Object[]> getUserProfileStatsFromView(@Param("userId") Long userId);

    // ============================================
    // 9. SQL FONKSİYON ÇAĞRILARI
    // ============================================

    // Fonksiyon 1: Tesis doluluk oranı
    @Query(value = "SELECT * FROM fn_calculate_facility_occupancy(:facilityId, :date, :startTime, :endTime)", nativeQuery = true)
    List<Object[]> calculateFacilityOccupancy(@Param("facilityId") Long facilityId,
                                               @Param("date") LocalDate date,
                                               @Param("startTime") LocalTime startTime,
                                               @Param("endTime") LocalTime endTime);

    // Fonksiyon 2: En uygun zaman önerisi (CURSOR kullanan)
    @Query(value = "SELECT * FROM fn_suggest_best_time_slot(:facilityId, :date, :durationHours)", nativeQuery = true)
    List<Object[]> suggestBestTimeSlot(@Param("facilityId") Long facilityId,
                                        @Param("date") LocalDate date,
                                        @Param("durationHours") int durationHours);

    // Fonksiyon 3: Kullanıcı rezervasyon raporu (RECORD kullanan)
    @Query(value = "SELECT * FROM fn_get_user_reservation_report(:userId, :startDate, :endDate)", nativeQuery = true)
    List<Object[]> getUserReservationReport(@Param("userId") Long userId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    // ============================================
    // 10. UNION / EXCEPT SORGULARI
    // ============================================

    // UNION: Tüm kullanıcı aktiviteleri
    @Query(value = "SELECT * FROM fn_get_user_all_activities(:userId)", nativeQuery = true)
    List<Object[]> getUserAllActivities(@Param("userId") Long userId);

    // EXCEPT: Hiç rezervasyon yapılmamış tesisler
    @Query(value = "SELECT * FROM fn_get_facilities_without_reservations()", nativeQuery = true)
    List<Object[]> getFacilitiesWithoutReservations();

    // UNION: Aktif ve geçmiş rezervasyonlar birleşik (inline)
    @Query(value = "SELECT r.id, 'ACTIVE' as type, r.reservation_date, r.start_time, r.end_time, f.name as facility_name " +
            "FROM reservations r " +
            "JOIN facilities f ON r.facility_id = f.id " +
            "WHERE r.user_id = :userId AND r.status = 'ACTIVE' " +
            "AND (r.reservation_date > CURRENT_DATE OR (r.reservation_date = CURRENT_DATE AND r.end_time > CURRENT_TIME)) " +
            "UNION ALL " +
            "SELECT r.id, 'PAST' as type, r.reservation_date, r.start_time, r.end_time, f.name as facility_name " +
            "FROM reservations r " +
            "JOIN facilities f ON r.facility_id = f.id " +
            "WHERE r.user_id = :userId " +
            "AND (r.status IN ('COMPLETED', 'CANCELLED') " +
            "OR (r.reservation_date < CURRENT_DATE OR (r.reservation_date = CURRENT_DATE AND r.end_time <= CURRENT_TIME))) " +
            "ORDER BY reservation_date DESC, start_time DESC", nativeQuery = true)
    List<Object[]> findAllReservationsUnion(@Param("userId") Long userId);

    // ============================================
    // 11. AGGREGATE SORGUSU (HAVING ile)
    // ============================================

    // En popüler tesisler (minimum rezervasyon sayısı olan)
    @Query(value = "SELECT * FROM fn_get_popular_facilities(:minReservations)", nativeQuery = true)
    List<Object[]> getPopularFacilities(@Param("minReservations") int minReservations);

    // Günlük rezervasyon istatistikleri (HAVING ile)
    @Query(value = "SELECT reservation_date, COUNT(*) as reservation_count, " +
            "COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled_count " +
            "FROM reservations " +
            "WHERE facility_id = :facilityId " +
            "AND reservation_date BETWEEN :startDate AND :endDate " +
            "GROUP BY reservation_date " +
            "HAVING COUNT(*) >= :minCount " +
            "ORDER BY reservation_date", nativeQuery = true)
    List<Object[]> getDailyReservationStats(@Param("facilityId") Long facilityId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            @Param("minCount") int minCount);

    // Saatlik yoğunluk analizi (HAVING ile)
    @Query(value = "SELECT EXTRACT(HOUR FROM start_time) as hour, " +
            "COUNT(*) as total_reservations, " +
            "AVG(EXTRACT(EPOCH FROM (end_time - start_time))/60) as avg_duration_minutes " +
            "FROM reservations " +
            "WHERE facility_id = :facilityId " +
            "AND status IN ('ACTIVE', 'COMPLETED') " +
            "GROUP BY EXTRACT(HOUR FROM start_time) " +
            "HAVING COUNT(*) >= :minReservations " +
            "ORDER BY hour", nativeQuery = true)
    List<Object[]> getHourlyOccupancyStats(@Param("facilityId") Long facilityId,
                                           @Param("minReservations") int minReservations);

    // ============================================
    // 12. TRIGGER MESAJ KONTROLÜ
    // ============================================

    @Query(value = "SELECT fn_get_last_trigger_message(:reservationId)", nativeQuery = true)
    String getLastTriggerMessage(@Param("reservationId") Long reservationId);

    // Audit log sorgulama
    @Query(value = "SELECT * FROM reservation_audit_log " +
            "WHERE reservation_id = :reservationId " +
            "ORDER BY action_timestamp DESC", nativeQuery = true)
    List<Object[]> getReservationAuditLog(@Param("reservationId") Long reservationId);

    // Kullanıcının tüm audit logları
    @Query(value = "SELECT * FROM reservation_audit_log " +
            "WHERE user_id = :userId " +
            "ORDER BY action_timestamp DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> getUserAuditLogs(@Param("userId") Long userId, @Param("limit") int limit);

    // ============================================
    // 13. INDEX KULLANAN ARAMA SORGULARI
    // ============================================

    // Tarih aralığında arama (idx_reservation_search kullanır)
    @Query(value = "SELECT * FROM reservations " +
            "WHERE reservation_date BETWEEN :startDate AND :endDate " +
            "AND status = 'ACTIVE' " +
            "ORDER BY reservation_date, start_time", nativeQuery = true)
    List<Reservation> searchReservationsByDateRange(@Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);
}
