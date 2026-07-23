package com.myapps.web.mycalendar.domain.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;

import com.myapps.web.mycalendar.domain.model.Anniversary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AnniversaryCalculator 도메인 서비스에 대한 Property-Based 테스트.
 *
 * <p>jqwik을 사용하여 기념일 계산, D-Day 계산, D-Day 표시 형식,
 * 월별 기념일 필터링의 정확성을 검증합니다.
 *
 * <p>Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 8.6
 */
class AnniversaryCalculatorPropertyTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 6, 17);

    private final AnniversaryCalculator calculator = new AnniversaryCalculator();

    // Feature: mycalendar/001-couple-calendar, Property 9: Anniversary date calculation

    /**
     * Property 9: N일 기념일(100, 200, ..., 1000)의 날짜는 BASE_DATE + (N - 1)일이어야 한다.
     *
     * <p>한국식 계산 방식으로 BASE_DATE를 1일로 산정하므로,
     * N일 기념일의 실제 날짜는 BASE_DATE.plusDays(N - 1)이다.
     *
     * <p>**Validates: Requirements 8.1, 8.2, 8.5**
     */
    @Property(tries = 100)
    void hundredDayAnniversaryDateMustBeBaseDatePlusNMinusOneDays(
            @ForAll("hundredDayMultiples") final int n) {

        final List<Anniversary> anniversaries = calculator.calculateHundredDayAnniversaries();
        final int index = (n / 100) - 1;
        final Anniversary anniversary = anniversaries.get(index);

        final LocalDate expectedDate = BASE_DATE.plusDays(n - 1L);

        assertEquals(expectedDate, anniversary.date(),
                n + "일 기념일 날짜가 BASE_DATE + " + (n - 1) + "일이어야 한다");
    }

    /**
     * Property 9: Y주년 기념일의 날짜는 BASE_DATE.plusYears(Y)이어야 한다.
     *
     * <p>**Validates: Requirements 8.1, 8.6**
     */
    @Property(tries = 100)
    void yearlyAnniversaryDateMustBeBaseDatePlusYears(
            @ForAll("yearValues") final int y) {

        final List<Anniversary> anniversaries = calculator.calculateYearlyAnniversaries();
        final Anniversary anniversary = anniversaries.get(y - 1);

        final LocalDate expectedDate = BASE_DATE.plusYears(y);

        assertEquals(expectedDate, anniversary.date(),
                y + "주년 기념일 날짜가 BASE_DATE + " + y + "년이어야 한다");
    }

    // Feature: mycalendar/001-couple-calendar, Property 10: Korean D-Day calculation

    /**
     * Property 10: 임의의 날짜 D (D >= BASE_DATE)에 대해
     * D-Day 값은 ChronoUnit.DAYS.between(BASE_DATE, D) + 1 이어야 한다.
     *
     * <p>한국식 D-Day 계산에서 BASE_DATE 자체를 1일로 산정한다.
     *
     * <p>**Validates: Requirements 8.2, 8.4**
     */
    @Property(tries = 100)
    void dDayValueMustEqualDaysBetweenBaseDateAndTargetPlusOne(
            @ForAll("datesOnOrAfterBaseDate") final LocalDate date) {

        final long actualDDay = calculator.calculateDDay(date);
        final long expectedDDay = ChronoUnit.DAYS.between(BASE_DATE, date) + 1;

        assertEquals(expectedDDay, actualDDay,
                date + "의 D-Day 값이 " + expectedDDay + "이어야 한다");
    }

    // Feature: mycalendar/001-couple-calendar, Property 11: D-Day display format

    /**
     * Property 11: 임의의 날짜 D (D >= BASE_DATE)에 대해
     * "D+" 접두사 + calculateDDay 결과로 구성되는 표시 문자열이
     * 정확한 형식을 가져야 한다.
     *
     * <p>AnniversaryCalculator는 숫자 값만 반환하므로, "D+" 접두사와 결합한
     * 표시 문자열이 "D+N" 형식을 올바르게 구성하는지 검증한다.
     *
     * <p>**Validates: Requirements 8.4**
     */
    @Property(tries = 100)
    void dDayDisplayFormatMustBeDPlusPrefixFollowedByElapsedDays(
            @ForAll("datesOnOrAfterBaseDate") final LocalDate date) {

        final long dDayValue = calculator.calculateDDay(date);
        final String displayString = "D+" + dDayValue;
        final long elapsedDays = ChronoUnit.DAYS.between(BASE_DATE, date) + 1;

        assertEquals("D+" + elapsedDays, displayString,
                date + "의 D-Day 표시가 'D+" + elapsedDays + "'이어야 한다");
        assertTrue(displayString.startsWith("D+"),
                "D-Day 표시 문자열은 'D+' 접두사로 시작해야 한다");
        assertTrue(dDayValue >= 1,
                "D-Day 값은 항상 1 이상이어야 한다 (BASE_DATE 자체가 1)");
    }

    // Feature: mycalendar/001-couple-calendar, Property 12: Anniversary month filtering

    /**
     * Property 12: 임의의 YearMonth에 대해, 해당 월의 기념일 목록은
     * 전체 기념일 목록에서 해당 월 범위(1일~말일)에 포함되는 기념일과 정확히 일치해야 한다.
     *
     * <p>**Validates: Requirements 8.3, 8.5, 8.6**
     */
    @Property(tries = 100)
    void anniversariesForMonthMustMatchFilteredFullList(
            @ForAll("yearMonths") final YearMonth yearMonth) {

        final List<Anniversary> filteredResult = calculator.getAnniversariesForMonth(yearMonth);

        final LocalDate startOfMonth = yearMonth.atDay(1);
        final LocalDate endOfMonth = yearMonth.atEndOfMonth();

        final List<Anniversary> allAnniversaries = new java.util.ArrayList<>();
        allAnniversaries.addAll(calculator.calculateHundredDayAnniversaries());
        allAnniversaries.addAll(calculator.calculateYearlyAnniversaries());

        final List<Anniversary> expectedInMonth = allAnniversaries.stream()
                .filter(anniversary -> !anniversary.date().isBefore(startOfMonth)
                        && !anniversary.date().isAfter(endOfMonth))
                .toList();

        assertEquals(expectedInMonth.size(), filteredResult.size(),
                yearMonth + " 월의 기념일 수가 일치해야 한다");
        assertTrue(filteredResult.containsAll(expectedInMonth),
                yearMonth + " 월의 기념일 목록이 전체 필터링 결과와 동일해야 한다");
        assertTrue(expectedInMonth.containsAll(filteredResult),
                yearMonth + " 월의 기념일 목록에 추가 항목이 없어야 한다");
    }

    // --- Arbitrary Providers ---

    @Provide
    Arbitrary<Integer> hundredDayMultiples() {
        return Arbitraries.integers().between(1, 10).map(i -> i * 100);
    }

    @Provide
    Arbitrary<Integer> yearValues() {
        return Arbitraries.integers().between(1, 10);
    }

    @Provide
    Arbitrary<LocalDate> datesOnOrAfterBaseDate() {
        return Arbitraries.integers().between(0, 3650)
                .map(offset -> BASE_DATE.plusDays(offset));
    }

    @Provide
    Arbitrary<YearMonth> yearMonths() {
        return Arbitraries.integers().between(0, 120)
                .map(offset -> YearMonth.from(BASE_DATE).plusMonths(offset));
    }
}
