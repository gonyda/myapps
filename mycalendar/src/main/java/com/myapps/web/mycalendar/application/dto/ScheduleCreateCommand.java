package com.myapps.web.mycalendar.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.myapps.web.mycalendar.domain.model.Category;

/**
 * 일정 생성 커맨드.
 *
 * <p>새로운 일정을 생성할 때 필요한 데이터를 전달하는 DTO입니다.
 */
public record ScheduleCreateCommand(
    Category category,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime scheduleTime,
    String content
) {}
