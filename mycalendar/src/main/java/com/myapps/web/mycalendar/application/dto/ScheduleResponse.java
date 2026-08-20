package com.myapps.web.mycalendar.application.dto;

import com.myapps.web.mycalendar.domain.model.Category;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 일정 조회 응답.
 *
 * <p>일정 조회 시 클라이언트에 반환되는 응답 DTO입니다.
 */
public record ScheduleResponse(
        Long id,
        Category category,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime scheduleTime,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
