package com.myapps.web.mycalendar.application.dto;

import java.util.List;

/** GitHub {@code calendar/mycalendar.json} 파일로 내보내는 전체 일정 스냅샷 루트 DTO. */
public record CalendarSnapshot(
        String syncedAt, String timezone, int totalCount, List<CalendarSnapshotEvent> events) {}
