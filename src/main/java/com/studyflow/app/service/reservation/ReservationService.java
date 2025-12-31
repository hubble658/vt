package com.studyflow.app.service.reservation;

import com.studyflow.app.context.GlobalParamsContext;
import com.studyflow.app.exception.ArgumentNotValidException;
import com.studyflow.app.model.dto.AvailabilityDTO;
import com.studyflow.app.model.facility.*;
import com.studyflow.app.model.reservation.Reservation;
import com.studyflow.app.model.user.User;
import com.studyflow.app.context.UserSessionContext;
import com.studyflow.app.repository.facility.DeskRepository;
import com.studyflow.app.repository.facility.FacilityBlockRepository;
import com.studyflow.app.repository.facility.SeatRepository;
import com.studyflow.app.repository.reservation.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReservationService {

    @Autowired private ReservationRepository reservationRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private DeskRepository deskRepository;
    @Autowired private FacilityBlockRepository blockRepository;
    @Autowired private UserSessionContext userSessionContext;
    @Autowired private GlobalParamsContext globalParamsContext;

    // ============================================
    // 1. REZERVASYON OLUŞTURMA (INSERT)
    // ============================================
    
    /**
     * Yeni bir rezervasyon oluşturur.
     */
    public void createReservation(Long seatId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        User user = userSessionContext.getCurrentUser();
        Facility facility = globalParamsContext.getSelectedFacility();
        FacilityBlock facilityBlock = globalParamsContext.getSelectedFacilityBlock();
        Desk desk = globalParamsContext.getSelectedDesk();

        // 1. Kullanıcı kontrolü
        if (user == null) {
            throw new RuntimeException("No active user found.");
        }

        // 2. Koltuk var mı?
        Seat seat = seatRepository.findById(seatId).orElseThrow(() -> new ArgumentNotValidException("Seat not found"));

        // 3. Aktif rezervasyon sayısı kontrolü (max 3)
        int activeCount = reservationRepository.countActiveReservationsByUserId(user.getId());
        if (activeCount >= 3) {
            throw new RuntimeException("Maksimum 3 aktif rezervasyonunuz olabilir. Mevcut: " + activeCount);
        }

        // 4. Kullanıcı Zaman Çakışması Kontrolü (aynı kullanıcı aynı saatte başka yerde rezervasyon var mı?)
        int userTimeConflicts = reservationRepository.countUserTimeConflicts(user.getId(), date, startTime, endTime);
        if (userTimeConflicts > 0) {
            throw new RuntimeException("Bu zaman diliminde zaten bir rezervasyonunuz bulunmaktadır. " +
                    "Aynı anda birden fazla yerde rezervasyon yapamazsınız.");
        }

        // 5. Koltuk Çakışması Kontrolü (bu koltuk başkası tarafından alınmış mı?)
        int conflicts = reservationRepository.countConflictingReservations(seatId, date, startTime, endTime);
        if (conflicts > 0) {
            throw new RuntimeException("Bu koltuk seçilen zaman dilimi için zaten rezerve edilmiş.");
        }

        // 6. Kaydet (Native Insert - Trigger da kontrol edecek)
        reservationRepository.createReservation(user.getId(), seatId, facility.getId(), facilityBlock.getId(), desk.getId(), date, startTime, endTime);

        System.out.println("ActiveReservation created for User: " + user.getEmail() + " Seat: " + seat.getSeatNumber());
    }

    // ============================================
    // 2. REZERVASYON GÜNCELLEME (UPDATE)
    // ============================================

    /**
     * Rezervasyon zamanini gunceller.
     * Sadece ACTIVE durumundaki ve baslangicina 1 saatten fazla kalan rezervasyonlar guncellenebilir.
     * Sure siniri: Minimum 1 saat, maksimum 3 saat.
     * Gecmis zamana guncelleme yapilamaz.
     */
    public String updateReservation(Long reservationId, LocalDate newDate, LocalTime newStartTime, LocalTime newEndTime) {
        // 1. Rezervasyonu bul
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ArgumentNotValidException("Rezervasyon bulunamadi"));

        // 2. Yetki kontrolu
        User currentUser = userSessionContext.getCurrentUser();
        if (currentUser == null || !currentUser.getId().equals(reservation.getUser().getId())) {
            throw new RuntimeException("Bu rezervasyonu guncelleme yetkiniz yok.");
        }

        // 3. Durum kontrolu
        if (!"ACTIVE".equals(reservation.getStatus())) {
            throw new RuntimeException("Sadece aktif rezervasyonlar guncellenebilir.");
        }

        // 4. Gecmis zaman kontrolu
        java.time.LocalDateTime newDateTime = java.time.LocalDateTime.of(newDate, newStartTime);
        if (newDateTime.isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Gecmis bir zamana rezervasyon guncellenemez.");
        }

        // 5. Sure kontrolu (1-3 saat)
        long durationMinutes = java.time.Duration.between(newStartTime, newEndTime).toMinutes();
        if (durationMinutes < 60) {
            throw new RuntimeException("Rezervasyon suresi en az 1 saat olmalidir.");
        }
        if (durationMinutes > 180) {
            throw new RuntimeException("Rezervasyon suresi en fazla 3 saat olabilir.");
        }

        // 5. Zaman kontrolu (1 saat kurali)
        if (!reservation.isCancellable()) {
            long minutesLeft = reservation.getMinutesUntilCancelDeadline();
            throw new RuntimeException("Rezervasyon baslangicina 1 saatten az kaldigi icin guncellenemez. " +
                    "Son guncelleme icin " + Math.abs(minutesLeft) + " dakika gecti.");
        }

        // 6. Kullanici zaman cakismasi kontrolu (kendi rezervasyonu haric)
        int userTimeConflicts = reservationRepository.countUserTimeConflictsExcluding(
                currentUser.getId(), newDate, newStartTime, newEndTime, reservationId);
        if (userTimeConflicts > 0) {
            throw new RuntimeException("Bu zaman diliminde zaten baska bir rezervasyonunuz bulunmaktadir. " +
                    "Ayni anda birden fazla yerde rezervasyon yapamazsiniz.");
        }

        // 7. Koltuk cakismasi kontrolu (kendi rezervasyonu haric)
        int conflicts = reservationRepository.countConflictingReservationsExcluding(
                reservation.getSeat().getId(), newDate, newStartTime, newEndTime, reservationId);
        if (conflicts > 0) {
            throw new RuntimeException("Secilen koltuk bu zaman diliminde baska biri tarafindan rezerve edilmis.");
        }

        // 8. Guncelle
        int updated = reservationRepository.updateReservationTime(reservationId, newDate, newStartTime, newEndTime);
        
        if (updated > 0) {
            return "Rezervasyon basariyla guncellendi. Yeni tarih: " + newDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) +
                   " Saat: " + newStartTime.format(DateTimeFormatter.ofPattern("HH:mm")) + "-" + 
                   newEndTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else {
            throw new RuntimeException("Rezervasyon guncellenemedi.");
        }
    }

    // ============================================
    // 3. REZERVASYON İPTAL (DELETE/CANCEL)
    // ============================================

    /**
     * Rezervasyonu iptal eder.
     * Sadece ACTIVE durumundaki ve başlangıcına 1 saatten fazla kalan rezervasyonlar iptal edilebilir.
     * Trigger: trg_check_cancellation_rules tetiklenir ve mesaj döner.
     */
    public String cancelReservation(Long reservationId, String reason) {
        // 1. Rezervasyonu bul
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ArgumentNotValidException("Rezervasyon bulunamadı"));

        // 2. Yetki kontrolü
        User currentUser = userSessionContext.getCurrentUser();
        if (currentUser == null || !currentUser.getId().equals(reservation.getUser().getId())) {
            throw new RuntimeException("Bu rezervasyonu iptal etme yetkiniz yok.");
        }

        // 3. Durum kontrolü
        if (!"ACTIVE".equals(reservation.getStatus())) {
            throw new RuntimeException("Bu rezervasyon zaten iptal edilmiş veya tamamlanmış.");
        }

        // 4. Zaman kontrolü (1 saat kuralı - Java tarafında da kontrol)
        if (!reservation.isCancellable()) {
            LocalDateTime reservationStart = LocalDateTime.of(reservation.getReservationDate(), reservation.getStartTime());
            LocalDateTime cancelDeadline = reservationStart.minusHours(1);
            throw new RuntimeException("Rezervasyon başlangıcına 1 saatten az kaldığı için iptal edilemez. " +
                    "Son iptal zamanı: " + cancelDeadline.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        }

        // 5. İptal et (Trigger tetiklenecek)
        try {
            int cancelled = reservationRepository.cancelReservation(reservationId, reason != null ? reason : "Kullanıcı isteği");
            
            if (cancelled > 0) {
                return "✅ Rezervasyon başarıyla iptal edildi.\n\n" +
                       "📋 Tesis: " + reservation.getFacility().getName() + "\n" +
                       "📅 Tarih: " + reservation.getReservationDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "\n" +
                       "⏰ Saat: " + reservation.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + "-" + 
                       reservation.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")) + "\n" +
                       "📝 Sebep: " + (reason != null && !reason.isBlank() ? reason : "Belirtilmedi");
            } else {
                throw new RuntimeException("Rezervasyon iptal edilemedi.");
            }
        } catch (Exception e) {
            // Trigger hatası kontrol et
            if (e.getMessage() != null && e.getMessage().contains("TRIGGER_ERROR")) {
                String[] parts = e.getMessage().split(":");
                if (parts.length >= 3) {
                    throw new RuntimeException(parts[2]);
                }
            }
            throw e;
        }
    }

    /**
     * Rezervasyonun iptal edilebilir olup olmadığını kontrol eder.
     */
    public boolean isReservationCancellable(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        return reservation != null && reservation.isCancellable();
    }

    /**
     * İptal için kalan süreyi döndürür.
     */
    public String getCancellationDeadlineInfo(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null) {
            return "Rezervasyon bulunamadı.";
        }
        
        if (!reservation.isCancellable()) {
            return "Bu rezervasyon artık iptal edilemez.";
        }
        
        long minutes = reservation.getMinutesUntilCancelDeadline();
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        
        if (hours > 0) {
            return "İptal için " + hours + " saat " + remainingMinutes + " dakika kaldı.";
        } else {
            return "İptal için " + remainingMinutes + " dakika kaldı.";
        }
    }

    /**
     * BLOKLARIN Doluluk Durumunu Hesaplar (Adım 2 Ekranı İçin)
     * Her blok için: Toplam Koltuk ve Dolu Koltuk sayısını döner.
     */
    public List<AvailabilityDTO> getBlockAvailability(Long facilityId, LocalDate date, LocalTime start, LocalTime end) {
        List<AvailabilityDTO> result = new ArrayList<>();

        // 1. O tesisteki tüm blokları çek
        List<FacilityBlock> blocks = blockRepository.findAllByFacilityId(facilityId);

        for (FacilityBlock block : blocks) {
            int total = 0;
            int occupied = 0;

            // Bloğa ait tüm masaları gez
            List<Desk> desks = deskRepository.findAllByBlockId(block.getId());
            for (Desk desk : desks) {
                // Masaya ait tüm koltukları gez
                List<Seat> seats = seatRepository.findAllByDeskId(desk.getId());
                total += seats.size();

                // Her koltuk için çakışma var mı bak
                for (Seat seat : seats) {
                    if (reservationRepository.countConflictingReservations(seat.getId(), date, start, end) > 0) {
                        occupied++;
                    }
                }
            }
            result.add(new AvailabilityDTO(block.getId(), total, occupied));
        }
        return result;
    }

    /**
     * MASALARIN Doluluk Durumunu Hesaplar (Adım 3 Ekranı İçin)
     */
    public List<AvailabilityDTO> getDeskAvailability(Long blockId, LocalDate date, LocalTime start, LocalTime end) {
        List<AvailabilityDTO> result = new ArrayList<>();

        List<Desk> desks = deskRepository.findAllByBlockId(blockId);
        for (Desk desk : desks) {
            List<Seat> seats = seatRepository.findAllByDeskId(desk.getId());
            int total = seats.size();
            int occupied = 0;

            for (Seat seat : seats) {
                if (reservationRepository.countConflictingReservations(seat.getId(), date, start, end) > 0) {
                    occupied++;
                }
            }
            result.add(new AvailabilityDTO(desk.getId(), total, occupied));
        }
        return result;
    }

    /**
     * Tek bir Masadaki DOLU koltukların ID listesini döner (Adım 4 Ekranı İçin)
     * Frontend bu listedeki ID'leri gri (disable) yapar, gerisini yeşil yapar.
     */
    public List<Long> getOccupiedSeatIds(Long deskId, LocalDate date, LocalTime start, LocalTime end) {
        return reservationRepository.findOccupiedSeatIdsByDesk(deskId, date, start, end);
    }

    /**
     * Kullanıcının AKTİF rezervasyonlarını getirir.
     */
    public List<Reservation> getCurrentUserActiveReservations() {
        User user = userSessionContext.getCurrentUser();
        if (user == null) return new ArrayList<>();
        return reservationRepository.findActiveReservationsByUserId(user.getId(), LocalDate.now(), LocalTime.now());
    }

    /**
     * Kullanıcının GEÇMİŞ rezervasyonlarını getirir.
     */
    public List<Reservation> getCurrentUserPastReservations() {
        User user = userSessionContext.getCurrentUser();
        if (user == null) return new ArrayList<>();

        // Repository'de özel metod olmadığı için Java tarafında filtreliyoruz
        return reservationRepository.findPastReservationsByUserId(user.getId(), LocalDate.now(), LocalTime.now());
    }

    public String findBestTimeSuggestion(Facility facility, LocalDate date) {
        // 1. Tesisin çalışma saatlerini al
        DailySchedule schedule = facility.getWeeklyCalendar().getScheduleForDay(date.getDayOfWeek());
        if (schedule == null || (schedule.getIsClosed() != null && schedule.getIsClosed())) {
            return null; // Kapalıysa öneri yok
        }

        LocalTime open = schedule.getOpenTime();
        LocalTime close = schedule.getCloseTime();
        LocalTime now = LocalTime.now();

        // Eğer gün "Bugün" ise ve kapanışa az kaldıysa öneri yapma
        if (date.equals(LocalDate.now())) {
            if (now.isAfter(close.minusHours(1))) return null;
            if (now.isAfter(open)) open = now.plusMinutes(30 - (now.getMinute() % 30)).withSecond(0); // Şimdiki zamana yuvarla
        }

        // 2. O günkü tüm rezervasyonları çek (Start, End)
        List<Object[]> reservations = reservationRepository.findReservationTimesForFacility(facility.getId(), date);

        // 3. Yoğunluk Haritası (Histogram) Oluştur
        // Key: Saat (14:00), Value: Dolu Koltuk Sayısı
        Map<LocalTime, Integer> occupancyMap = new HashMap<>();

        LocalTime t = open;
        while (t.isBefore(close)) {
            occupancyMap.put(t, 0);
            t = t.plusMinutes(30);
        }

        // Rezervasyonları haritaya işle
        for (Object[] row : reservations) {
            String[] row0Result = row[0].toString().split(":");
            String[] row1Result = row[1].toString().split(":");
            LocalTime start = LocalTime.of((Integer.parseInt(row0Result[0])), Integer.parseInt(row0Result[1]));
            LocalTime end = LocalTime.of((Integer.parseInt(row1Result[0])), Integer.parseInt(row1Result[1]));

            LocalTime temp = start;
            while (temp.isBefore(end)) {
                if (occupancyMap.containsKey(temp)) {
                    occupancyMap.put(temp, occupancyMap.get(temp) + 1);
                }
                temp = temp.plusMinutes(30);
            }
        }

        // 4. En Sakin 2 Saatlik Aralığı Bul (Sliding Window)
        LocalTime bestStart = null;
        int minOccupancy = Integer.MAX_VALUE;

        LocalTime scan = open;
        // Kapanışa 2 saat kalana kadar tara
        while (scan.isBefore(close.minusMinutes(90))) {
            // 4 adet 30 dakikalık dilimin (2 saat) toplam yoğunluğu
            int currentOccupancy = 0;
            boolean validWindow = true;

            for (int i = 0; i < 4; i++) {
                LocalTime slot = scan.plusMinutes(i * 30);
                if (!occupancyMap.containsKey(slot)) { validWindow = false; break; }
                currentOccupancy += occupancyMap.get(slot);
            }

            if (validWindow) {
                if (currentOccupancy < minOccupancy) {
                    minOccupancy = currentOccupancy;
                    bestStart = scan;
                }
            }
            scan = scan.plusMinutes(30);
        }

        if (bestStart != null) {
            return bestStart.format(DateTimeFormatter.ofPattern("HH:mm"))
                    + "-" + bestStart.plusHours(2).format(DateTimeFormatter.ofPattern("HH:mm"));
        }

        return null;
    }

    // ============================================
    // SQL FONKSİYONLARI ÇAĞRILARI
    // ============================================

    /**
     * Tesis doluluk oranını hesaplar (SQL Fonksiyonu 1)
     * fn_calculate_facility_occupancy çağrılır
     */
    public Map<String, Object> getFacilityOccupancy(Long facilityId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<Object[]> result = reservationRepository.calculateFacilityOccupancy(facilityId, date, startTime, endTime);
        Map<String, Object> response = new HashMap<>();
        
        if (result != null && !result.isEmpty()) {
            Object[] row = result.get(0);
            response.put("totalSeats", row[0]);
            response.put("occupiedSeats", row[1]);
            response.put("occupancyRate", row[2]);
        }
        return response;
    }

    /**
     * En uygun zaman dilimini önerir (SQL Fonksiyonu 2 - CURSOR kullanır)
     * fn_suggest_best_time_slot çağrılır
     */
    public Map<String, Object> suggestBestTimeSlotFromDB(Long facilityId, LocalDate date, int durationHours) {
        List<Object[]> result = reservationRepository.suggestBestTimeSlot(facilityId, date, durationHours);
        Map<String, Object> response = new HashMap<>();
        
        if (result != null && !result.isEmpty()) {
            Object[] row = result.get(0);
            response.put("suggestedStart", row[0]);
            response.put("suggestedEnd", row[1]);
            response.put("availableSeats", row[2]);
            response.put("occupancyPercentage", row[3]);
        }
        return response;
    }

    /**
     * Kullanıcı rezervasyon raporu (SQL Fonksiyonu 3 - RECORD kullanır)
     * fn_get_user_reservation_report çağrılır
     */
    public List<Map<String, Object>> getUserReservationReport(LocalDate startDate, LocalDate endDate) {
        User user = userSessionContext.getCurrentUser();
        if (user == null) return new ArrayList<>();
        
        List<Object[]> result = reservationRepository.getUserReservationReport(user.getId(), startDate, endDate);
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (Object[] row : result) {
            Map<String, Object> item = new HashMap<>();
            item.put("reservationId", row[0]);
            item.put("facilityName", row[1]);
            item.put("blockName", row[2]);
            item.put("seatNumber", row[3]);
            item.put("reservationDate", row[4]);
            item.put("startTime", row[5]);
            item.put("endTime", row[6]);
            item.put("durationMinutes", row[7]);
            item.put("status", row[8]);
            item.put("isCancellable", row[9]);
            response.add(item);
        }
        return response;
    }

    // ============================================
    // VIEW SORGULARI
    // ============================================

    /**
     * Aktif rezervasyonları VIEW'dan getirir (vw_active_reservations)
     */
    public List<Map<String, Object>> getActiveReservationsFromView() {
        User user = userSessionContext.getCurrentUser();
        if (user == null) return new ArrayList<>();
        
        List<Object[]> result = reservationRepository.findActiveReservationsFromView(user.getId());
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (Object[] row : result) {
            Map<String, Object> item = new HashMap<>();
            item.put("reservationId", row[0]);
            item.put("reservationDate", row[1]);
            item.put("startTime", row[2]);
            item.put("endTime", row[3]);
            item.put("status", row[4]);
            item.put("userId", row[5]);
            item.put("userFullName", row[6]);
            item.put("userEmail", row[7]);
            item.put("facilityId", row[8]);
            item.put("facilityName", row[9]);
            item.put("blockId", row[10]);
            item.put("blockName", row[11]);
            item.put("deskId", row[12]);
            item.put("deskRange", row[13]);
            item.put("seatId", row[14]);
            item.put("seatNumber", row[15]);
            response.add(item);
        }
        return response;
    }

    /**
     * Tesis istatistiklerini VIEW'dan getirir (vw_facility_statistics)
     */
    public Map<String, Object> getFacilityStatistics(Long facilityId) {
        List<Object[]> result = reservationRepository.getFacilityStatisticsFromView(facilityId);
        Map<String, Object> response = new HashMap<>();
        
        if (result != null && !result.isEmpty()) {
            Object[] row = result.get(0);
            response.put("facilityId", row[0]);
            response.put("facilityName", row[1]);
            response.put("totalBlocks", row[2]);
            response.put("totalDesks", row[3]);
            response.put("totalSeats", row[4]);
            response.put("activeReservations", row[5]);
            response.put("completedReservations", row[6]);
            response.put("cancelledReservations", row[7]);
        }
        return response;
    }

    /**
     * Tüm tesis istatistiklerini getirir
     */
    public List<Map<String, Object>> getAllFacilityStatistics() {
        List<Object[]> result = reservationRepository.getAllFacilityStatistics();
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (Object[] row : result) {
            Map<String, Object> item = new HashMap<>();
            item.put("facilityId", row[0]);
            item.put("facilityName", row[1]);
            item.put("totalBlocks", row[2]);
            item.put("totalDesks", row[3]);
            item.put("totalSeats", row[4]);
            item.put("activeReservations", row[5]);
            item.put("completedReservations", row[6]);
            item.put("cancelledReservations", row[7]);
            response.add(item);
        }
        return response;
    }

    // ============================================
    // UNION / EXCEPT SORGULARI
    // ============================================

    /**
     * Tüm kullanıcı aktivitelerini getirir (UNION kullanır)
     * fn_get_user_all_activities çağrılır
     */
    public List<Map<String, Object>> getUserAllActivities() {
        User user = userSessionContext.getCurrentUser();
        if (user == null) return new ArrayList<>();
        
        List<Object[]> result = reservationRepository.getUserAllActivities(user.getId());
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (Object[] row : result) {
            Map<String, Object> item = new HashMap<>();
            item.put("activityType", row[0]);
            item.put("activityDate", row[1]);
            item.put("activityTime", row[2]);
            item.put("description", row[3]);
            response.add(item);
        }
        return response;
    }

    /**
     * Hiç rezervasyon yapılmamış tesisleri getirir (EXCEPT kullanır)
     * fn_get_facilities_without_reservations çağrılır
     */
    public List<Map<String, Object>> getFacilitiesWithoutReservations() {
        List<Object[]> result = reservationRepository.getFacilitiesWithoutReservations();
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (Object[] row : result) {
            Map<String, Object> item = new HashMap<>();
            item.put("facilityId", row[0]);
            item.put("facilityName", row[1]);
            item.put("address", row[2]);
            response.add(item);
        }
        return response;
    }

    // ============================================
    // AGGREGATE SORGULARI (HAVING ile)
    // ============================================

    /**
     * En popüler tesisleri getirir (HAVING kullanır)
     * fn_get_popular_facilities çağrılır
     */
    public List<Map<String, Object>> getPopularFacilities(int minReservations) {
        List<Object[]> result = reservationRepository.getPopularFacilities(minReservations);
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (Object[] row : result) {
            Map<String, Object> item = new HashMap<>();
            item.put("facilityId", row[0]);
            item.put("facilityName", row[1]);
            item.put("totalReservations", row[2]);
            item.put("completedReservations", row[3]);
            item.put("cancellationRate", row[4]);
            response.add(item);
        }
        return response;
    }

    /**
     * Saatlik yoğunluk analizi (HAVING ile)
     */
    public List<Map<String, Object>> getHourlyOccupancyStats(Long facilityId, int minReservations) {
        List<Object[]> result = reservationRepository.getHourlyOccupancyStats(facilityId, minReservations);
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (Object[] row : result) {
            Map<String, Object> item = new HashMap<>();
            item.put("hour", row[0]);
            item.put("totalReservations", row[1]);
            item.put("avgDurationMinutes", row[2]);
            response.add(item);
        }
        return response;
    }

    // ============================================
    // AUDIT LOG VE TRIGGER MESAJLARI
    // ============================================

    /**
     * Rezervasyon audit loglarını getirir
     */
    public List<Map<String, Object>> getReservationAuditLog(Long reservationId) {
        List<Object[]> result = reservationRepository.getReservationAuditLog(reservationId);
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (Object[] row : result) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row[0]);
            item.put("reservationId", row[1]);
            item.put("actionType", row[2]);
            item.put("actionTimestamp", row[3]);
            item.put("userId", row[4]);
            item.put("oldStatus", row[5]);
            item.put("newStatus", row[6]);
            item.put("details", row[7]);
            response.add(item);
        }
        return response;
    }

    /**
     * Kullanıcının son audit loglarını getirir
     */
    public List<Map<String, Object>> getUserRecentAuditLogs(int limit) {
        User user = userSessionContext.getCurrentUser();
        if (user == null) return new ArrayList<>();
        
        List<Object[]> result = reservationRepository.getUserAuditLogs(user.getId(), limit);
        List<Map<String, Object>> response = new ArrayList<>();
        
        for (Object[] row : result) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row[0]);
            item.put("reservationId", row[1]);
            item.put("actionType", row[2]);
            item.put("actionTimestamp", row[3]);
            item.put("userId", row[4]);
            item.put("oldStatus", row[5]);
            item.put("newStatus", row[6]);
            item.put("details", row[7]);
            response.add(item);
        }
        return response;
    }

    /**
     * Süresi dolmuş rezervasyonları tamamlandı olarak işaretler
     * Scheduler veya uygulama başlangıcında çağrılabilir
     */
    public int markExpiredReservationsAsCompleted() {
        return reservationRepository.markExpiredReservationsAsCompleted(LocalDate.now(), LocalTime.now());
    }
}