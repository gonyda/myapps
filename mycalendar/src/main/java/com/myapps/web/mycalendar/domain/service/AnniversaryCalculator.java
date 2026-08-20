package com.myapps.web.mycalendar.domain.service;

import com.myapps.web.mycalendar.domain.model.Anniversary;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 기념일 및 D-Day를 계산하는 도메인 서비스.
 *
 * <p>Base_Date(사귄 날)를 기준으로 100일 단위, 연 단위 기념일을 계산하고, 한국식 D-Day 계산 방식을 적용합니다. DB 조회 없이 상수만 사용합니다.
 */
@Service
public class AnniversaryCalculator {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 6, 17);
    private static final int HUNDRED_DAY_INTERVAL = 100;
    private static final int MAX_HUNDRED_DAY_COUNT = 10;
    private static final int MAX_YEARLY_COUNT = 10;

    /**
     * 한국식 D-Day를 계산합니다.
     *
     * <p>Base_Date를 1일로 산정하여, 주어진 날짜까지의 경과 일수를 반환합니다. 예를 들어 Base_Date 자체는 1, 그 다음 날은 2입니다.
     *
     * @param today 계산 기준 날짜
     * @return 한국식 D-Day 값 (Base_Date가 1)
     */
    public long calculateDDay(final LocalDate today) {
        return ChronoUnit.DAYS.between(BASE_DATE, today) + 1;
    }

    /**
     * 100일 단위 기념일 목록을 계산합니다 (100일 ~ 1000일).
     *
     * <p>Base_Date를 1일로 산정하므로, N일 기념일의 실제 날짜는 BASE_DATE.plusDays(N - 1)입니다.
     *
     * @return 100일부터 1000일까지 10개의 기념일 목록
     */
    public List<Anniversary> calculateHundredDayAnniversaries() {
        final List<Anniversary> anniversaries = new ArrayList<>(MAX_HUNDRED_DAY_COUNT);
        for (int i = 1; i <= MAX_HUNDRED_DAY_COUNT; i++) {
            final int days = i * HUNDRED_DAY_INTERVAL;
            final LocalDate date = BASE_DATE.plusDays(days - 1L);
            final String name = days + "일";
            anniversaries.add(new Anniversary(date, name));
        }
        return anniversaries;
    }

    /**
     * 연 단위 기념일 목록을 계산합니다 (1주년 ~ 10주년).
     *
     * <p>Y주년 기념일의 날짜는 BASE_DATE.plusYears(Y)입니다.
     *
     * @return 1주년부터 10주년까지 10개의 기념일 목록
     */
    public List<Anniversary> calculateYearlyAnniversaries() {
        final List<Anniversary> anniversaries = new ArrayList<>(MAX_YEARLY_COUNT);
        for (int i = 1; i <= MAX_YEARLY_COUNT; i++) {
            final LocalDate date = BASE_DATE.plusYears(i);
            final String name = i + "주년";
            anniversaries.add(new Anniversary(date, name));
        }
        return anniversaries;
    }

    /**
     * 특정 월에 포함되는 기념일 목록을 반환합니다.
     *
     * <p>100일 단위 기념일과 연 단위 기념일을 모두 합산한 뒤, 주어진 월의 1일부터 말일 사이에 해당하는 기념일만 필터링합니다.
     *
     * @param yearMonth 조회 대상 연월
     * @return 해당 월에 포함되는 기념일 목록
     */
    public List<Anniversary> getAnniversariesForMonth(final YearMonth yearMonth) {
        final LocalDate startOfMonth = yearMonth.atDay(1);
        final LocalDate endOfMonth = yearMonth.atEndOfMonth();

        final List<Anniversary> allAnniversaries = new ArrayList<>();
        allAnniversaries.addAll(calculateHundredDayAnniversaries());
        allAnniversaries.addAll(calculateYearlyAnniversaries());

        return allAnniversaries.stream()
                .filter(
                        anniversary ->
                                !anniversary.date().isBefore(startOfMonth)
                                        && !anniversary.date().isAfter(endOfMonth))
                .toList();
    }
}
