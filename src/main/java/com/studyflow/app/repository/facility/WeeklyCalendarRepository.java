package com.studyflow.app.repository.facility;

import com.studyflow.app.model.facility.WeeklyCalendar;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;

public interface WeeklyCalendarRepository extends JpaRepository<WeeklyCalendar, Long> {
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO facility_calendars(facility_id)" +
            "VALUES(:facilityId)", nativeQuery = true)
    void saveNewWeeklyCalendar(@Param("facilityId") Long facilityId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO facility_calendar_days(calendar_id, day_of_week, open_time, close_time, is_closed)" +
            "VALUES(:calendarId, :dayOfWeek, :openTime, :closeTime, :isClosed)", nativeQuery = true)
    void addNewDailySchedule(@Param("calendarId") Long calendarId, @Param("dayOfWeek")String dayOfWeek,
                             @Param("openTime")LocalTime openTime, @Param("closeTime") LocalTime closeTime,
                             @Param("isClosed") Boolean isClosed);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM facility_calendar_days " +
            "WHERE calendar_id = :calendarId " +
            "AND day_of_week = :dayOfWeek",
            nativeQuery = true)
    void deleteOldDailySchedule(@Param("calendarId") Long calendarId, @Param("dayOfWeek") String dayOfWeek);
}
