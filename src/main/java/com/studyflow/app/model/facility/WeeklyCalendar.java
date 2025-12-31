package com.studyflow.app.model.facility;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "facility_calendars")
public class WeeklyCalendar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne()
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "facility_calendar_days",
            joinColumns = @JoinColumn(name = "calendar_id")
    )
    private List<DailySchedule> days = new ArrayList<>();

    public DailySchedule getScheduleForDay(java.time.DayOfWeek day) {
        return days.stream()
                .filter(d -> d.getDayOfWeek() == day)
                .findFirst()
                .orElse(null);
    }

    public void addDailySchedule(DailySchedule schedule) {
        this.days.removeIf(d -> d.getDayOfWeek() == schedule.getDayOfWeek());
        this.days.add(schedule);
    }
}
