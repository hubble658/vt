package com.studyflow.app.service.facility;

import com.studyflow.app.exception.ArgumentNotValidException;
import com.studyflow.app.model.facility.DailySchedule;
import com.studyflow.app.model.facility.Facility;
import com.studyflow.app.model.user.User;
import com.studyflow.app.repository.facility.FacilityRepository;
import com.studyflow.app.repository.facility.WeeklyCalendarRepository;
import com.studyflow.app.context.UserSessionContext;
import com.studyflow.app.util.annotation.RequireAdmin;
import com.studyflow.app.util.annotation.RequireLibrarian;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Component
public class FacilityService {
    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private WeeklyCalendarRepository weeklyCalendarRepository;

    @Autowired
    private UserSessionContext userSessionContext;

    @RequireAdmin
    public void createFacility(String name, String address, String imageUrl){
        if (name == null || address == null || imageUrl == null){
            throw new ArgumentNotValidException(name);
        }

        facilityRepository.saveNewFacility(name, address, imageUrl);
        Long facilityId = facilityRepository.getFacilityByImageUrl(imageUrl).getId();
        weeklyCalendarRepository.saveNewWeeklyCalendar(facilityId);
    }

    @RequireLibrarian
    public void addDailySchedule(DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, Boolean isClosed){
        User user = userSessionContext.getCurrentUser();
        Long calendarId = user.getFacilityInfo().getFacility().getWeeklyCalendar().getId();

        DailySchedule dailySchedule;

        if (isClosed){
            dailySchedule = new DailySchedule(dayOfWeek, null, null, true);
        } else {
            dailySchedule = new DailySchedule(dayOfWeek, openTime, closeTime, false);
        }

        user.getFacilityInfo().getFacility().getWeeklyCalendar().addDailySchedule(dailySchedule);
        weeklyCalendarRepository.deleteOldDailySchedule(calendarId, dayOfWeek.toString());
        weeklyCalendarRepository.addNewDailySchedule(calendarId, dayOfWeek.toString(), openTime, closeTime, isClosed);
    }

    public List<Facility> getAllFacilities(String order){
        List<Facility> facilities;
        if (order.equalsIgnoreCase("asc")){
            facilities = facilityRepository.getAllFacilitiesByOrder("ASC");
        } else if (order.equalsIgnoreCase("desc")){
            facilities = facilityRepository.getAllFacilitiesByOrder("DESC");
        } else {
            facilities = null;
        }
        return facilities;
    }

    public Facility getFacility(Long facilityId){
        Facility facility = facilityRepository.getFacilityById(facilityId);
        return facility;
    }

}
