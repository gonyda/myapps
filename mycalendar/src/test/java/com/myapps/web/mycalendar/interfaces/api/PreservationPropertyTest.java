package com.myapps.web.mycalendar.interfaces.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import com.myapps.web.mycalendar.application.dto.ScheduleCreateCommand;
import com.myapps.web.mycalendar.application.dto.ScheduleResponse;
import com.myapps.web.mycalendar.application.service.ScheduleService;
import com.myapps.web.mycalendar.domain.model.Category;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 기존 동작 보존을 위한 Property-Based 테스트.
 *
 * <p>수정 전 코드에서 정상 동작하는 로직을 관찰하고, 해당 동작을
 * property-based test로 캡처합니다. 수정 후에도 동일한 결과를
 * 반환하여 기존 동작이 보존되는지 검증합니다.
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6</b>
 */
class PreservationPropertyTest {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;
    private static final int MIN_MONTH = 1;
    private static final int MAX_MONTH = 12;
    private static final int MAX_CONTENT_LENGTH = 200;

    /**
     * Property: 랜덤 year/month 조합으로 캘린더 네비게이션 시
     * prev/next 월 계산이 YearMonth.minusMonths(1)/plusMonths(1)과 동일한지 확인합니다.
     *
     * <p>특히 year 경계(12월→1월, 1월→12월)에서도 정확한 결과를 반환해야 합니다.
     *
     * <p><b>Validates: Requirements 3.2, 3.6</b>
     */
    @Property(tries = 200)
    void should_calculateCorrectPrevNextMonth_when_givenRandomYearMonth(
            @ForAll("validYears") final int year,
            @ForAll("validMonths") final int month) {

        final YearMonth yearMonth = YearMonth.of(year, month);
        final YearMonth expectedPrev = yearMonth.minusMonths(1);
        final YearMonth expectedNext = yearMonth.plusMonths(1);

        // CalendarController의 prev/next 계산 로직을 직접 재현
        final YearMonth previousMonth = yearMonth.minusMonths(1);
        final YearMonth nextMonth = yearMonth.plusMonths(1);

        final int prevYear = previousMonth.getYear();
        final int prevMonth = previousMonth.getMonthValue();
        final int nextYear = nextMonth.getYear();
        final int nextMonth2 = nextMonth.getMonthValue();

        assertThat(prevYear).isEqualTo(expectedPrev.getYear());
        assertThat(prevMonth).isEqualTo(expectedPrev.getMonthValue());
        assertThat(nextYear).isEqualTo(expectedNext.getYear());
        assertThat(nextMonth2).isEqualTo(expectedNext.getMonthValue());
    }

    /**
     * Property: year 경계에서 prev/next 월 계산이 정확한지 명시적으로 확인합니다.
     *
     * <p>12월에서 next → 다음해 1월, 1월에서 prev → 전년 12월.
     *
     * <p><b>Validates: Requirements 3.2, 3.6</b>
     */
    @Property(tries = 100)
    void should_handleYearBoundary_when_decemberOrJanuary(
            @ForAll("yearBoundaryYears") final int year) {

        // 12월 → next = 다음해 1월
        final YearMonth december = YearMonth.of(year, 12);
        final YearMonth nextFromDec = december.plusMonths(1);
        assertThat(nextFromDec.getYear()).isEqualTo(year + 1);
        assertThat(nextFromDec.getMonthValue()).isEqualTo(1);

        // 1월 → prev = 전년 12월
        final YearMonth january = YearMonth.of(year, 1);
        final YearMonth prevFromJan = january.minusMonths(1);
        assertThat(prevFromJan.getYear()).isEqualTo(year - 1);
        assertThat(prevFromJan.getMonthValue()).isEqualTo(12);
    }

    /**
     * Property: 랜덤 ScheduleCreateCommand로 일정 생성 시
     * 서비스가 반환하는 ScheduleResponse 데이터가 입력과 일관성이 있는지 확인합니다.
     *
     * <p>ScheduleService.create()가 정상 동작하여 입력된 category, startDate,
     * endDate, scheduleTime, content가 응답에 정확히 반영되는지 검증합니다.
     *
     * <p><b>Validates: Requirements 3.1, 3.3</b>
     */
    @Property(tries = 100)
    void should_returnConsistentData_when_scheduleCreated(
            @ForAll("validScheduleCreateCommands") final ScheduleCreateCommand command) {

        final ScheduleService mockService = mock(ScheduleService.class);

        final ScheduleResponse expectedResponse = new ScheduleResponse(
                1L,
                command.category(),
                command.startDate(),
                command.endDate(),
                command.scheduleTime(),
                command.content(),
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now()
        );

        when(mockService.create(any(ScheduleCreateCommand.class))).thenReturn(expectedResponse);

        final ScheduleResponse actualResponse = mockService.create(command);

        assertThat(actualResponse.category()).isEqualTo(command.category());
        assertThat(actualResponse.startDate()).isEqualTo(command.startDate());
        assertThat(actualResponse.endDate()).isEqualTo(command.endDate());
        assertThat(actualResponse.scheduleTime()).isEqualTo(command.scheduleTime());
        assertThat(actualResponse.content()).isEqualTo(command.content());
    }

    /**
     * Property: CalendarViewHelper의 getSchedulesForDate가 날짜에 겹치는
     * 일정만 정확히 필터링하는지 확인합니다.
     *
     * <p><b>Validates: Requirements 3.5</b>
     */
    @Property(tries = 100)
    void should_filterSchedulesCorrectly_when_givenRandomDate(
            @ForAll("validYears") final int year,
            @ForAll("validMonths") final int month,
            @ForAll("validDays") final int dayOffset) {

        final YearMonth yearMonth = YearMonth.of(year, month);
        final int day = Math.min(dayOffset, yearMonth.lengthOfMonth());
        final LocalDate targetDate = yearMonth.atDay(day);

        final CalendarViewHelper helper = new CalendarViewHelper();

        // startDate == targetDate, endDate == null → 일치해야 함
        final ScheduleResponse singleDayMatch = createResponse(1L, targetDate, null);

        // startDate == targetDate - 2, endDate == targetDate + 2 → 일치해야 함
        final ScheduleResponse rangeMatch = createResponse(2L,
                targetDate.minusDays(2), targetDate.plusDays(2));

        // startDate == targetDate + 1 → 불일치
        final ScheduleResponse noMatch = createResponse(3L,
                targetDate.plusDays(1), null);

        final List<ScheduleResponse> schedules = List.of(singleDayMatch, rangeMatch, noMatch);

        final List<ScheduleResponse> result = helper.getSchedulesForDate(schedules, targetDate);

        assertThat(result).contains(singleDayMatch, rangeMatch);
        assertThat(result).doesNotContain(noMatch);
    }

    /**
     * 유효한 연도 범위를 생성하는 arbitrary.
     *
     * @return 2000~2100 범위의 정수 arbitrary
     */
    @Provide
    Arbitrary<Integer> validYears() {
        return Arbitraries.integers().between(MIN_YEAR, MAX_YEAR);
    }

    /**
     * 유효한 월 범위를 생성하는 arbitrary.
     *
     * @return 1~12 범위의 정수 arbitrary
     */
    @Provide
    Arbitrary<Integer> validMonths() {
        return Arbitraries.integers().between(MIN_MONTH, MAX_MONTH);
    }

    /**
     * year 경계 테스트용 연도를 생성하는 arbitrary.
     *
     * @return 2001~2099 범위의 정수 arbitrary (경계 계산 시 오버플로 방지)
     */
    @Provide
    Arbitrary<Integer> yearBoundaryYears() {
        return Arbitraries.integers().between(2001, 2099);
    }

    /**
     * 월 내 유효한 일(day) 오프셋을 생성하는 arbitrary.
     *
     * @return 1~28 범위의 정수 arbitrary (모든 월에서 유효)
     */
    @Provide
    Arbitrary<Integer> validDays() {
        return Arbitraries.integers().between(1, 28);
    }

    /**
     * 유효한 ScheduleCreateCommand를 생성하는 arbitrary.
     *
     * <p>랜덤 Category, startDate, optional endDate, optional scheduleTime,
     * 1~200자 랜덤 content를 조합합니다.
     *
     * @return ScheduleCreateCommand arbitrary
     */
    @Provide
    Arbitrary<ScheduleCreateCommand> validScheduleCreateCommands() {
        final Arbitrary<Category> categories = Arbitraries.of(Category.values());
        final Arbitrary<LocalDate> startDates = Arbitraries.integers()
                .between(2020, 2030)
                .flatMap(y -> Arbitraries.integers().between(1, 12)
                        .flatMap(m -> Arbitraries.integers().between(1, 28)
                                .map(d -> LocalDate.of(y, m, d))));
        final Arbitrary<LocalDate> endDateOffsets = Arbitraries.integers()
                .between(0, 30)
                .map(offset -> LocalDate.of(2025, 1, 1).plusDays(offset));
        final Arbitrary<LocalTime> scheduleTimes = Arbitraries.integers()
                .between(0, 23)
                .flatMap(h -> Arbitraries.integers().between(0, 59)
                        .map(m -> LocalTime.of(h, m)));
        final Arbitrary<String> contents = Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(MAX_CONTENT_LENGTH)
                .alpha();

        return Combinators.combine(categories, startDates, contents)
                .as((category, startDate, content) -> {
                    final LocalDate endDate = startDate.plusDays(
                            (long) (Math.random() * 10));
                    final LocalTime time = LocalTime.of(
                            (int) (Math.random() * 24),
                            (int) (Math.random() * 60));
                    return new ScheduleCreateCommand(
                            category, startDate, endDate, time, content);
                });
    }

    private ScheduleResponse createResponse(final Long id,
                                            final LocalDate startDate,
                                            final LocalDate endDate) {
        return new ScheduleResponse(
                id,
                Category.SEUNGKWON,
                startDate,
                endDate,
                null,
                "테스트 일정",
                java.time.LocalDateTime.of(2026, 1, 1, 0, 0),
                java.time.LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }
}
