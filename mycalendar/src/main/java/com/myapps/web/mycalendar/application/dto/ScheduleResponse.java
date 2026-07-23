package com.myapps.web.mycalendar.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.myapps.web.mycalendar.domain.model.Category;

/**
 * 일정 조회 응답.
 *
 * <p>일정 조회 시 클라이언트에 반환되는 응답 DTO입니다.
 * 해당 일정에 달린 댓글 목록을 함께 포함합니다.
 */
public record ScheduleResponse(
    Long id,
    Category category,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime scheduleTime,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<CommentResponse> comments
) {}
