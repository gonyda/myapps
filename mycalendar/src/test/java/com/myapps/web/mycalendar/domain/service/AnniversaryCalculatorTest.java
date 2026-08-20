package com.myapps.web.mycalendar.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.myapps.web.mycalendar.domain.model.Anniversary;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** AnniversaryCalculator 도메인 서비스 단위 테스트. */
class AnniversaryCalculatorTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 6, 17);

    private AnniversaryCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new AnniversaryCalculator();
    }

    @Test
    @DisplayName("D-Day: BASE_DATE 당일은 1이어야 한다")
    void should_returnOne_when_dateIsBaseDate() {
        final long dDay = calculator.calculateDDay(BASE_DATE);

        assertEquals(1L, dDay);
    }

    @Test
    @DisplayName("D-Day: BASE_DATE 다음 날은 2이어야 한다")
    void should_returnTwo_when_dateIsOneDayAfterBaseDate() {
        final long dDay = calculator.calculateDDay(BASE_DATE.plusDays(1));

        assertEquals(2L, dDay);
    }

    @Test
    @DisplayName("D-Day: BASE_DATE + 99일은 100이어야 한다")
    void should_returnHundred_when_dateIs99DaysAfterBaseDate() {
        final long dDay = calculator.calculateDDay(BASE_DATE.plusDays(99));

        assertEquals(100L, dDay);
    }

    @Test
    @DisplayName("100일 기념일: 날짜는 BASE_DATE + 99일이어야 한다")
    void should_calculateHundredDayAnniversary_correctDate() {
        final List<Anniversary> anniversaries = calculator.calculateHundredDayAnniversaries();

        assertEquals(10, anniversaries.size());

        final Anniversary hundredDay = anniversaries.getFirst();
        assertEquals(BASE_DATE.plusDays(99), hundredDay.date());
        assertEquals("100일", hundredDay.name());
    }

    @Test
    @DisplayName("1000일 기념일: 날짜는 BASE_DATE + 999일이어야 한다")
    void should_calculateThousandDayAnniversary_correctDate() {
        final List<Anniversary> anniversaries = calculator.calculateHundredDayAnniversaries();

        final Anniversary thousandDay = anniversaries.getLast();
        assertEquals(BASE_DATE.plusDays(999), thousandDay.date());
        assertEquals("1000일", thousandDay.name());
    }

    @Test
    @DisplayName("1주년 기념일: 날짜는 BASE_DATE + 1년이어야 한다")
    void should_calculateFirstYearlyAnniversary_correctDate() {
        final List<Anniversary> anniversaries = calculator.calculateYearlyAnniversaries();

        assertEquals(10, anniversaries.size());

        final Anniversary firstYear = anniversaries.getFirst();
        assertEquals(BASE_DATE.plusYears(1), firstYear.date());
        assertEquals("1주년", firstYear.name());
    }

    @Test
    @DisplayName("10주년 기념일: 날짜는 BASE_DATE + 10년이어야 한다")
    void should_calculateTenthYearlyAnniversary_correctDate() {
        final List<Anniversary> anniversaries = calculator.calculateYearlyAnniversaries();

        final Anniversary tenthYear = anniversaries.getLast();
        assertEquals(BASE_DATE.plusYears(10), tenthYear.date());
        assertEquals("10주년", tenthYear.name());
    }

    @Test
    @DisplayName("월별 필터링: 해당 월에 기념일이 있으면 반환한다")
    void should_returnAnniversaries_when_monthContainsThem() {
        // 100일 = BASE_DATE + 99 = 2026-09-24
        final YearMonth september2026 = YearMonth.of(2026, 9);
        final List<Anniversary> result = calculator.getAnniversariesForMonth(september2026);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(a -> a.name().equals("100일")));
    }

    @Test
    @DisplayName("월별 필터링: 해당 월에 기념일이 없으면 빈 목록을 반환한다")
    void should_returnEmptyList_when_monthContainsNoAnniversaries() {
        // 2026년 7월에는 100일/200일.../1주년 등이 포함되지 않을 가능성이 높음
        // BASE_DATE가 6월 17일이므로 7월에는 100일(9/24)도 아직 아니고 1주년(6/17)도 아님
        final YearMonth july2026 = YearMonth.of(2026, 7);
        final List<Anniversary> result = calculator.getAnniversariesForMonth(july2026);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("월별 필터링: 반환된 기념일은 모두 해당 월 범위 내에 있어야 한다")
    void should_returnOnlyAnniversariesWithinMonth() {
        final YearMonth targetMonth = YearMonth.of(2027, 6);
        final List<Anniversary> result = calculator.getAnniversariesForMonth(targetMonth);

        final LocalDate startOfMonth = targetMonth.atDay(1);
        final LocalDate endOfMonth = targetMonth.atEndOfMonth();

        for (final Anniversary anniversary : result) {
            assertTrue(
                    !anniversary.date().isBefore(startOfMonth)
                            && !anniversary.date().isAfter(endOfMonth),
                    "기념일 " + anniversary.name() + "(" + anniversary.date() + ")이 월 범위를 벗어남");
        }
    }
}
