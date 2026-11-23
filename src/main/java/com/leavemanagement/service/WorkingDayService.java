package com.leavemanagement.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Decides which days count as leave days.
 *  - Saturday and Sunday never count.
 *  - Public holidays never count. They are NOT stored in a database table: they come from the
 *    "leave.public-holidays" property in application.properties.
 *    To use a real external holiday source later, only change isPublicHoliday() in this class.
 */
@Service
public class WorkingDayService {

    private final Set<LocalDate> publicHolidays = new HashSet<>();

    public WorkingDayService(@Value("${leave.public-holidays:}") String holidays) {
        if (holidays != null && !holidays.isBlank()) {
            for (String part : holidays.split(",")) {
                String text = part.trim();
                if (!text.isEmpty()) {
                    publicHolidays.add(LocalDate.parse(text)); // yyyy-MM-dd
                }
            }
        }
    }

    public boolean isWorkingDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        boolean weekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        return !weekend && !isPublicHoliday(date);
    }

    public boolean isPublicHoliday(LocalDate date) {
        return publicHolidays.contains(date);
    }

    /** Number of working days from start to end, both included. */
    public int countWorkingDays(LocalDate start, LocalDate end) {
        return (int) start.datesUntil(end.plusDays(1)).filter(this::isWorkingDay).count();
    }
}
