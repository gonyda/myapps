package com.myapps.web.mycalendar.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.myapps.web.mycalendar.domain.model.Category;

/**
 * 일정 수정 커맨드.
 *
 * <p>기존 일정을 수정할 때 필요한 데이터를 전달하는 DTO입니다.
 */
public record ScheduleUpdateCommand(
    Category category,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime scheduleTime,
    String content
) {}
