package com.studyflow.app.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class UserProfileStats {
    // 1. Total Time
    private String totalTimeThisWeek;       // "8h 40m"
    private String totalTimeLastWeek;       // "6h 00m"
    private String weeklyComparison;        // "+2h 40m increase" veya "-1h decrease"
    private boolean isIncrease;             // UI'da yeşil/kırmızı ok göstermek için

    // 2. Productive Time
    private String mostProductiveRange;     // "18:00 - 22:00"
    private String productivityMessage;     // "You reserve mostly in evenings."

    // 3. Session Durations
    private String averageDuration;         // "1h 25m"
    private String longestSession;          // "3h 10m"
    private String shortestSession;         // "25m"

    // 4. Trend (Grafik için)
    // Key: "Week 1", "Week 2" - Value: Saat cinsinden toplam (örn: 4.5)
    private Map<String, Double> last4WeeksTrend;
}