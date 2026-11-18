package com.myapps.web.mycalendar.application.dto;

import com.myapps.web.mycalendar.domain.model.Category;

/**
 * GitHub 스냅샷 JSON의 {@code events} 배열에 직렬화되는 개별 일정 항목 DTO.
 *
 * <p>실제 {@link com.myapps.web.mycalendar.domain.model.Schedule} 엔티티에 존재하는 일정 필드만 매핑합니다.
 */
public record CalendarSnapshotEvent(
        Long id,
        Category category,
        String startDate,
        String endDate,
        String scheduleTime,
        String content) {}
