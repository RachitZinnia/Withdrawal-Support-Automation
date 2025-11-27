package com.withdrawal.support.util;

import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for calculating business days (excluding weekends and holidays)
 */
@Slf4j
public class BusinessDaysCalculator {

    // Define company holidays (can be moved to configuration if needed)
    private static final Set<LocalDate> HOLIDAYS = new HashSet<>();
    
    static {
        // Add US federal holidays for 2025 (example)
        // You can populate this from a configuration file or database
        HOLIDAYS.add(LocalDate.of(2025, 1, 1));   // New Year's Day
        HOLIDAYS.add(LocalDate.of(2025, 7, 4));   // Independence Day
        HOLIDAYS.add(LocalDate.of(2025, 12, 25)); // Christmas
        // Add more holidays as needed
    }

    /**
     * Checks if a date is older than the specified number of business days
     * 
     * @param dateTime The date to check
     * @param businessDaysThreshold Number of business days threshold
     * @return true if the date is older than the threshold (excluding weekends)
     */
    public static boolean isOlderThanBusinessDays(LocalDateTime dateTime, int businessDaysThreshold) {
        if (dateTime == null) {
            return false;
        }
        
        LocalDate targetDate = dateTime.toLocalDate();
        LocalDate today = LocalDate.now();
        
        int businessDaysPassed = calculateBusinessDaysBetween(targetDate, today);
        
        log.debug("Date: {}, Today: {}, Business days passed: {}, Threshold: {}", 
                targetDate, today, businessDaysPassed, businessDaysThreshold);
        
        return businessDaysPassed > businessDaysThreshold;
    }

    /**
     * Calculates the number of business days between two dates (excluding weekends)
     * 
     * @param startDate Start date (inclusive)
     * @param endDate End date (exclusive)
     * @return Number of business days between the dates
     */
    public static int calculateBusinessDaysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        
        // If start date is after end date, return 0
        if (startDate.isAfter(endDate) || startDate.isEqual(endDate)) {
            return 0;
        }
        
        int businessDays = 0;
        LocalDate currentDate = startDate;
        
        // Iterate through each day and count business days
        while (currentDate.isBefore(endDate)) {
            if (isBusinessDay(currentDate)) {
                businessDays++;
            }
            currentDate = currentDate.plusDays(1);
        }
        
        return businessDays;
    }

    /**
     * Checks if a given date is a business day (not weekend, not holiday)
     * 
     * @param date The date to check
     * @return true if it's a business day
     */
    public static boolean isBusinessDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        
        // Check if it's a weekend
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }
        
        // Check if it's a holiday
        if (HOLIDAYS.contains(date)) {
            return false;
        }
        
        return true;
    }

    /**
     * Adds business days to a date (excluding weekends)
     * 
     * @param startDate Starting date
     * @param businessDaysToAdd Number of business days to add
     * @return The resulting date after adding business days
     */
    public static LocalDate addBusinessDays(LocalDate startDate, int businessDaysToAdd) {
        if (startDate == null || businessDaysToAdd <= 0) {
            return startDate;
        }
        
        LocalDate currentDate = startDate;
        int daysAdded = 0;
        
        while (daysAdded < businessDaysToAdd) {
            currentDate = currentDate.plusDays(1);
            if (isBusinessDay(currentDate)) {
                daysAdded++;
            }
        }
        
        return currentDate;
    }

    /**
     * Subtracts business days from a date (excluding weekends)
     * 
     * @param startDate Starting date
     * @param businessDaysToSubtract Number of business days to subtract
     * @return The resulting date after subtracting business days
     */
    public static LocalDate subtractBusinessDays(LocalDate startDate, int businessDaysToSubtract) {
        if (startDate == null || businessDaysToSubtract <= 0) {
            return startDate;
        }
        
        LocalDate currentDate = startDate;
        int daysSubtracted = 0;
        
        while (daysSubtracted < businessDaysToSubtract) {
            currentDate = currentDate.minusDays(1);
            if (isBusinessDay(currentDate)) {
                daysSubtracted++;
            }
        }
        
        return currentDate;
    }
}




