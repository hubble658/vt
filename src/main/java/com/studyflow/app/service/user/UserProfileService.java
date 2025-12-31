package com.studyflow.app.service.user;

import com.studyflow.app.context.UserSessionContext;
import com.studyflow.app.model.reservation.Reservation;
import com.studyflow.app.model.user.User;
import com.studyflow.app.model.dto.UserProfileStats;
import com.studyflow.app.repository.reservation.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
public class UserProfileService {

    @Autowired private ReservationRepository reservationRepository;
    @Autowired private UserSessionContext userSessionContext;

    // --- MEVCUT METOTLAR (calculateUserStats vb.) AYNEN KALIYOR ---
    // (Kod tekrarı olmaması için buraya sadece YENİ eklenen metotları yazıyorum,
    // lüfen dosyanın içindeki eski metotları silme.)

    public UserProfileStats calculateUserStats() {
        // ... (Eski kodlar aynen kalacak) ...
        User user = userSessionContext.getCurrentUser();
        if (user == null) return null;
        List<Reservation> history = reservationRepository.findPastReservationsByUserId(user.getId(), LocalDate.now(), LocalTime.now());

        return UserProfileStats.builder()
                .totalTimeThisWeek(calculateWeeklyTotal(history, 0))
                .totalTimeLastWeek(calculateWeeklyTotal(history, 1))
                .weeklyComparison(calculateComparison(history))
                .isIncrease(isTrendIncreasing(history))
                .mostProductiveRange(findProductiveRange(history))
                .productivityMessage(getProductivityMessage(history))
                .averageDuration(formatDuration(calculateAverageDuration(history)))
                .longestSession(formatDuration(calculateMaxDuration(history)))
                .shortestSession(formatDuration(calculateMinDuration(history)))
                .last4WeeksTrend(calculate4WeeksTrend(history))
                .build();
    }

    // Ham veriyi (History Listesini) Controller'ın erişimine açıyoruz
    public List<Reservation> getAllHistory() {
        User user = userSessionContext.getCurrentUser();
        if (user == null) return new ArrayList<>();
        return reservationRepository.findPastReservationsByUserId(user.getId(), LocalDate.now(), LocalTime.now());
    }

    // --- YENİ: AYLIK TREND HESAPLAMA (Son X Ay) ---
    public Map<String, Double> calculateMonthlyTrend(List<Reservation> history, int monthsBack) {
        Map<String, Double> trend = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();

        // Son X ayın listesini oluştur (Örn: Nov, Dec, Jan)
        for (int i = monthsBack - 1; i >= 0; i--) {
            LocalDate targetDate = now.minusMonths(i);
            String monthName = targetDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            trend.put(monthName, 0.0); // Başlangıç değeri 0
        }

        // Verileri doldur
        for (Reservation r : history) {
            LocalDate date = r.getReservationDate();
            // Tarih, belirlediğimiz aralıkta mı? (Basit kontrol: Ay ismi eşleşiyor mu ve yıl mantıklı mı?)
            // Daha kesin kontrol için: date.isAfter(now.minusMonths(monthsBack))
            if (date.isAfter(now.minusMonths(monthsBack).withDayOfMonth(1).minusDays(1))) {
                String monthName = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

                // Map'te varsa topla
                if (trend.containsKey(monthName)) {
                    double hours = Duration.between(r.getStartTime(), r.getEndTime()).toMinutes() / 60.0;
                    trend.put(monthName, trend.get(monthName) + hours);
                }
            }
        }

        // Yuvarlama yap
        trend.replaceAll((k, v) -> Math.round(v * 10.0) / 10.0);
        return trend;
    }

    // --- YENİ: SPESİFİK BİR AYIN HAFTALIK ANALİZİ ---
    public Map<String, Double> calculateSpecificMonthTrend(List<Reservation> history, Month selectedMonth) {
        Map<String, Double> trend = new LinkedHashMap<>();
        // 4 veya 5 hafta olabilir, standart 5 hafta açalım
        trend.put("Week 1", 0.0);
        trend.put("Week 2", 0.0);
        trend.put("Week 3", 0.0);
        trend.put("Week 4", 0.0);

        int currentYear = LocalDate.now().getYear();

        for (Reservation r : history) {
            LocalDate date = r.getReservationDate();
            // Yıl ve Ay kontrolü
            if (date.getYear() == currentYear && date.getMonth() == selectedMonth) {
                // Ayın kaçıncı haftası?
                // AlignedWeekOfMonth: 1 ile 5 arası değer döner
                int weekNum = date.get(WeekFields.of(Locale.ENGLISH).weekOfMonth());
                String key = "Week " + weekNum;

                double hours = Duration.between(r.getStartTime(), r.getEndTime()).toMinutes() / 60.0;
                trend.merge(key, hours, Double::sum); // Varsa ekle, yoksa koy
            }
        }

        // Yuvarlama ve Boş Key Temizliği (Opsiyonel)
        trend.replaceAll((k, v) -> Math.round(v * 10.0) / 10.0);
        return trend;
    }

    // --- ESKİ YARDIMCI METOTLAR (Bunlar silinmemeli) ---
    private long getMinutesForWeek(List<Reservation> list, int weeksBack) {
        int targetWeek = LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear()) - weeksBack;
        return list.stream()
                .filter(r -> r.getReservationDate().get(WeekFields.ISO.weekOfWeekBasedYear()) == targetWeek)
                .mapToLong(r -> Duration.between(r.getStartTime(), r.getEndTime()).toMinutes())
                .sum();
    }

    private String calculateWeeklyTotal(List<Reservation> list, int weeksBack) {
        long totalMinutes = getMinutesForWeek(list, weeksBack);
        return formatMinutes(totalMinutes);
    }

    private String calculateComparison(List<Reservation> list) {
        long thisWeek = getMinutesForWeek(list, 0);
        long lastWeek = getMinutesForWeek(list, 1);
        long diff = thisWeek - lastWeek;

        String prefix = diff >= 0 ? "+" : "-";
        return prefix + formatMinutes(Math.abs(diff)) + (diff >= 0 ? " increase" : " decrease");
    }

    private boolean isTrendIncreasing(List<Reservation> list) {
        return getMinutesForWeek(list, 0) >= getMinutesForWeek(list, 1);
    }

    private String findProductiveRange(List<Reservation> list) {
        if (list.isEmpty()) return "N/A";
        int morning = 0, afternoon = 0, evening = 0;
        for (Reservation r : list) {
            int hour = r.getStartTime().getHour();
            if (hour >= 6 && hour < 12) morning++;
            else if (hour >= 12 && hour < 18) afternoon++;
            else evening++;
        }
        if (morning >= afternoon && morning >= evening) return "06:00 – 12:00";
        if (afternoon >= morning && afternoon >= evening) return "12:00 – 18:00";
        return "18:00 – 24:00";
    }

    private String getProductivityMessage(List<Reservation> list) {
        if (list.isEmpty()) return "Start studying to see stats!";
        String range = findProductiveRange(list);
        if (range.startsWith("06")) return "You are an early bird!";
        if (range.startsWith("12")) return "You prefer afternoon sessions.";
        return "You are a night owl!";
    }

    private Duration calculateAverageDuration(List<Reservation> list) {
        if (list.isEmpty()) return Duration.ZERO;
        long totalMin = list.stream().mapToLong(r -> Duration.between(r.getStartTime(), r.getEndTime()).toMinutes()).sum();
        return Duration.ofMinutes(totalMin / list.size());
    }

    private Duration calculateMaxDuration(List<Reservation> list) {
        return list.stream().map(r -> Duration.between(r.getStartTime(), r.getEndTime())).max(Duration::compareTo).orElse(Duration.ZERO);
    }

    private Duration calculateMinDuration(List<Reservation> list) {
        return list.stream().map(r -> Duration.between(r.getStartTime(), r.getEndTime())).min(Duration::compareTo).orElse(Duration.ZERO);
    }

    private Map<String, Double> calculate4WeeksTrend(List<Reservation> list) {
        Map<String, Double> trend = new LinkedHashMap<>();
        for (int i = 3; i >= 0; i--) {
            long minutes = getMinutesForWeek(list, i);
            double hours = Math.round((minutes / 60.0) * 10.0) / 10.0;
            String label = (i == 0) ? "This Week" : (i + " Weeks Ago");
            if (i == 1) label = "Last Week";
            trend.put(label, hours);
        }
        return trend;
    }

    private String formatDuration(Duration d) {
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        if (hours == 0 && minutes == 0) return "-";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    private String formatMinutes(long totalMinutes) {
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours == 0 && minutes == 0) return "0m";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }
}