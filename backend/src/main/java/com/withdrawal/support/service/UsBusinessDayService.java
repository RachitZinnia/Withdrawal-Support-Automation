package com.withdrawal.support.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
public class UsBusinessDayService {


    public boolean isDateDifferTwo(LocalDate inputDate, int days) {
        LocalDate currentDate = LocalDate.now();

        int cnt = 0;

        while (inputDate.isBefore(currentDate)) {
            if (isBusinessDay(inputDate)) {
                cnt++;
            }
            inputDate = inputDate.plusDays(1);
        }

        return cnt > days;
    }

    private boolean isBusinessDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
}
