package com.myapps.web.mycalendar.interfaces.api;

import java.time.LocalDate;
import java.util.List;

import com.myapps.web.mycalendar.application.dto.ScheduleResponse;

import org.springframework.stereotype.Component;

/**
 * 캘린더 뷰 템플릿에서 사용하는 유틸리티 헬퍼.
 *
 * <p>Thymeleaf 템플릿에서 SpEL로 직접 표현하기 어려운 일정 필터링 로직을
 * 제공합니다. Model에 추가되어 템플릿에서 직접 호출됩니다.
 */
@Component
public class CalendarViewHelper {

    /**
     * 특정 날짜에 해당하는 일정 목록을 반환합니다.
     *
     * <p>일정이 해당 날짜와 겹치는 조건:
     * startDate &lt;= date AND (endDate &gt;= date OR (endDate == null AND startDate == date))
     *
     * @param schedules 전체 일정 목록
     * @param date      필터링 대상 날짜
     * @return 해당 날짜에 겹치는 일정 목록
     */
    public List<ScheduleResponse> getSchedulesForDate(final List<ScheduleResponse> schedules,
                                                      final LocalDate date) {
        return schedules.stream()
                .filter(s -> overlaps(s, date))
                .toList();
    }

    private boolean overlaps(final ScheduleResponse schedule, final LocalDate date) {
        final LocalDate start = schedule.startDate();
        if (start == null || start.isAfter(date)) {
            return false;
        }
        final LocalDate end = schedule.endDate();
        if (end != null) {
            return !end.isBefore(date);
        }
        return start.equals(date);
    }
}
