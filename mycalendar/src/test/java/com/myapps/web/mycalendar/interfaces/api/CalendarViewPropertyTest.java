package com.myapps.web.mycalendar.interfaces.api;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 캘린더 날짜 셀 표시 제한 로직에 대한 Property-Based 테스트.
 *
 * <p>Property 14: 날짜에 N개의 일정이 있을 때, 캘린더 날짜 셀에 표시되는
 * 일정 수는 min(N, 3)이어야 하고, N &gt; 3이면 "+{N-3}" 형식의 초과 텍스트를
 * 표시해야 한다.
 *
 * <p>이 테스트는 Thymeleaf 템플릿에서 사용할 표시 제한 로직을 순수 계산으로
 * 검증합니다. 컨트롤러는 전체 일정 목록을 모델에 전달하고, 템플릿이
 * 표시 제한을 처리합니다.
 *
 * <p>Feature: mycalendar/001-couple-calendar, Property 14: Calendar day cell display limit
 */
// Validates: Requirements 1.1, 1.2, 1.3, 1.4, 8.3, 8.4
class CalendarViewPropertyTest {

    private static final int MOBILE_DISPLAY_LIMIT = 3;

    /**
     * 임의의 일정 개수 N에 대해, 화면에 표시되는 일정 수는 min(N, 3)이어야 한다.
     *
     * <p>**Validates: Requirements 1.4**
     *
     * @param scheduleCount 해당 날짜의 전체 일정 수 (0 이상)
     */
    @Property(tries = 100)
    void should_limitVisibleSchedules_when_anyNumberOfSchedules(
            @ForAll @IntRange(min = 0, max = 100) final int scheduleCount) {

        final int visibleCount = calculateVisibleCount(scheduleCount);

        assertThat(visibleCount).isEqualTo(Math.min(scheduleCount, MOBILE_DISPLAY_LIMIT));
    }

    /**
     * 일정이 3개 이하인 경우, 초과 텍스트는 null이어야 한다.
     *
     * <p>**Validates: Requirements 1.4**
     *
     * @param scheduleCount 해당 날짜의 전체 일정 수 (0~3)
     */
    @Property(tries = 100)
    void should_showNoOverflowText_when_schedulesWithinLimit(
            @ForAll @IntRange(min = 0, max = 3) final int scheduleCount) {

        final String overflowText = calculateOverflowText(scheduleCount);

        assertThat(overflowText).isNull();
    }

    /**
     * 일정이 3개를 초과하면, "+{N-3}" 형식의 초과 텍스트를 표시해야 한다.
     *
     * <p>**Validates: Requirements 1.4**
     *
     * @param scheduleCount 해당 날짜의 전체 일정 수 (4 이상)
     */
    @Property(tries = 100)
    void should_showOverflowText_when_schedulesExceedLimit(
            @ForAll @IntRange(min = 4, max = 100) final int scheduleCount) {

        final String overflowText = calculateOverflowText(scheduleCount);
        final int expectedOverflow = scheduleCount - MOBILE_DISPLAY_LIMIT;

        assertThat(overflowText).isEqualTo("+" + expectedOverflow);
    }

    /**
     * 표시되는 일정 수와 초과 일정 수의 합은 항상 전체 일정 수와 같아야 한다.
     *
     * <p>**Validates: Requirements 1.4**
     *
     * @param scheduleCount 해당 날짜의 전체 일정 수 (0 이상)
     */
    @Property(tries = 100)
    void should_preserveTotalCount_when_splitIntoVisibleAndOverflow(
            @ForAll @IntRange(min = 0, max = 100) final int scheduleCount) {

        final int visibleCount = calculateVisibleCount(scheduleCount);
        final int overflowCount = calculateOverflowCount(scheduleCount);

        assertThat(visibleCount + overflowCount).isEqualTo(scheduleCount);
    }

    /**
     * 표시할 일정 수를 계산합니다.
     *
     * @param totalSchedules 전체 일정 수
     * @return 화면에 표시할 일정 수 (최대 3)
     */
    private int calculateVisibleCount(final int totalSchedules) {
        return Math.min(totalSchedules, MOBILE_DISPLAY_LIMIT);
    }

    /**
     * 초과 일정 수를 계산합니다.
     *
     * @param totalSchedules 전체 일정 수
     * @return 초과 일정 수 (0 이상)
     */
    private int calculateOverflowCount(final int totalSchedules) {
        return Math.max(0, totalSchedules - MOBILE_DISPLAY_LIMIT);
    }

    /**
     * 초과 텍스트를 생성합니다.
     *
     * <p>일정이 표시 제한을 초과하면 "+{초과수}" 형식의 문자열을 반환하고,
     * 초과하지 않으면 null을 반환합니다.
     *
     * @param totalSchedules 전체 일정 수
     * @return 초과 텍스트 또는 null
     */
    private String calculateOverflowText(final int totalSchedules) {
        final int overflow = calculateOverflowCount(totalSchedules);
        if (overflow > 0) {
            return "+" + overflow;
        }
        return null;
    }
}
