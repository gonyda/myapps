package com.myapps.web.mycalendar.interfaces.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.myapps.web.mycalendar.application.dto.ScheduleResponse;
import com.myapps.web.mycalendar.application.service.ScheduleService;
import com.myapps.web.mycalendar.domain.model.Anniversary;
import com.myapps.web.mycalendar.domain.model.Category;
import com.myapps.web.mycalendar.domain.service.AnniversaryCalculator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * CalendarController의 웹 슬라이스 테스트.
 *
 * <p>MockMvc를 활용하여 루트 리다이렉트, 월별 캘린더 뷰 렌더링,
 * 모델 데이터 전달, D-Day 카운터 정확성을 검증합니다.
 */
@WebMvcTest(CalendarController.class)
class CalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    @MockitoBean
    private AnniversaryCalculator anniversaryCalculator;

    @MockitoBean
    private CalendarViewHelper calendarViewHelper;

    /**
     * GET / 요청 시 현재 월 캘린더 경로로 리다이렉트되는지 검증합니다.
     */
    @Test
    void should_redirectToCurrentMonth_when_rootPathAccessed() throws Exception {
        final LocalDate today = LocalDate.now();
        final String expectedUrl = "/calendar/" + today.getYear() + "/" + today.getMonthValue();

        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedUrl));
    }

    /**
     * GET /calendar/{year}/{month} 요청 시 캘린더 뷰와 모델 속성을 검증합니다.
     */
    @Test
    void should_returnCalendarView_when_yearAndMonthProvided() throws Exception {
        final int year = 2026;
        final int month = 7;
        final YearMonth yearMonth = YearMonth.of(year, month);
        final List<ScheduleResponse> schedules = List.of();
        final List<Anniversary> anniversaries = List.of(
                new Anniversary(LocalDate.of(2026, 7, 17), "1주년")
        );

        when(scheduleService.findByMonth(yearMonth)).thenReturn(schedules);
        when(anniversaryCalculator.getAnniversariesForMonth(yearMonth)).thenReturn(anniversaries);
        when(anniversaryCalculator.calculateDDay(any(LocalDate.class))).thenReturn(31L);

        mockMvc.perform(get("/calendar/{year}/{month}", year, month))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar"))
                .andExpect(model().attribute("year", year))
                .andExpect(model().attribute("month", month))
                .andExpect(model().attribute("schedules", schedules))
                .andExpect(model().attribute("anniversaries", anniversaries))
                .andExpect(model().attribute("dDay", 31L))
                .andExpect(model().attributeExists("today"))
                .andExpect(model().attribute("prevYear", 2026))
                .andExpect(model().attribute("prevMonth", 6))
                .andExpect(model().attribute("nextYear", 2026))
                .andExpect(model().attribute("nextMonth", 8));
    }

    /**
     * 1월의 이전 월이 전년도 12월로 계산되는지 검증합니다.
     */
    @Test
    void should_calculatePreviousYearDecember_when_januaryViewed() throws Exception {
        final int year = 2027;
        final int month = 1;

        when(scheduleService.findByMonth(any(YearMonth.class))).thenReturn(List.of());
        when(anniversaryCalculator.getAnniversariesForMonth(any(YearMonth.class))).thenReturn(List.of());
        when(anniversaryCalculator.calculateDDay(any(LocalDate.class))).thenReturn(199L);

        mockMvc.perform(get("/calendar/{year}/{month}", year, month))
                .andExpect(status().isOk())
                .andExpect(model().attribute("prevYear", 2026))
                .andExpect(model().attribute("prevMonth", 12))
                .andExpect(model().attribute("nextYear", 2027))
                .andExpect(model().attribute("nextMonth", 2));
    }

    /**
     * 12월의 다음 월이 다음 연도 1월로 계산되는지 검증합니다.
     */
    @Test
    void should_calculateNextYearJanuary_when_decemberViewed() throws Exception {
        final int year = 2026;
        final int month = 12;

        when(scheduleService.findByMonth(any(YearMonth.class))).thenReturn(List.of());
        when(anniversaryCalculator.getAnniversariesForMonth(any(YearMonth.class))).thenReturn(List.of());
        when(anniversaryCalculator.calculateDDay(any(LocalDate.class))).thenReturn(180L);

        mockMvc.perform(get("/calendar/{year}/{month}", year, month))
                .andExpect(status().isOk())
                .andExpect(model().attribute("prevYear", 2026))
                .andExpect(model().attribute("prevMonth", 11))
                .andExpect(model().attribute("nextYear", 2027))
                .andExpect(model().attribute("nextMonth", 1));
    }

    /**
     * D-Day 카운터 모델 데이터가 정확하게 전달되는지 검증합니다.
     *
     * <p>AnniversaryCalculator가 반환하는 D-Day 값이 모델에 올바르게 설정되며,
     * 한국식 D-Day 계산(Base_Date를 1일로 산정) 결과가 반영되는지 확인합니다.
     */
    @Test
    void should_includeDDayCounter_when_calendarViewed() throws Exception {
        final int year = 2027;
        final int month = 3;
        final long expectedDDay = 270L;

        when(scheduleService.findByMonth(any(YearMonth.class))).thenReturn(List.of());
        when(anniversaryCalculator.getAnniversariesForMonth(any(YearMonth.class))).thenReturn(List.of());
        when(anniversaryCalculator.calculateDDay(any(LocalDate.class))).thenReturn(expectedDDay);

        mockMvc.perform(get("/calendar/{year}/{month}", year, month))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("dDay"))
                .andExpect(model().attribute("dDay", expectedDDay));
    }

    /**
     * 캘린더 뷰에 모든 필수 모델 속성이 존재하는지 검증합니다.
     *
     * <p>year, month, schedules, anniversaries, dDay, today,
     * prevYear, prevMonth, nextYear, nextMonth 속성이 모두 존재해야 합니다.
     */
    @Test
    void should_includeAllModelAttributes_when_calendarViewed() throws Exception {
        final int year = 2026;
        final int month = 9;
        final LocalDateTime now = LocalDateTime.of(2026, 9, 15, 10, 0);
        final List<ScheduleResponse> schedules = List.of(
                new ScheduleResponse(1L, Category.DATE, LocalDate.of(2026, 9, 10),
                        null, LocalTime.of(18, 0), "저녁 약속",
                        now, now, List.of())
        );
        final List<Anniversary> anniversaries = List.of(
                new Anniversary(LocalDate.of(2026, 9, 24), "100일")
        );

        when(scheduleService.findByMonth(YearMonth.of(year, month))).thenReturn(schedules);
        when(anniversaryCalculator.getAnniversariesForMonth(YearMonth.of(year, month)))
                .thenReturn(anniversaries);
        when(anniversaryCalculator.calculateDDay(any(LocalDate.class))).thenReturn(91L);

        mockMvc.perform(get("/calendar/{year}/{month}", year, month))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar"))
                .andExpect(model().attributeExists(
                        "year", "month", "schedules", "anniversaries",
                        "dDay", "today", "prevYear", "prevMonth",
                        "nextYear", "nextMonth"))
                .andExpect(model().attribute("year", year))
                .andExpect(model().attribute("month", month))
                .andExpect(model().attribute("schedules", schedules))
                .andExpect(model().attribute("anniversaries", anniversaries))
                .andExpect(model().attribute("dDay", 91L))
                .andExpect(model().attribute("prevYear", 2026))
                .andExpect(model().attribute("prevMonth", 8))
                .andExpect(model().attribute("nextYear", 2026))
                .andExpect(model().attribute("nextMonth", 10));
    }
}
