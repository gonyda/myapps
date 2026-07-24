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
import com.myapps.web.mycalendar.domain.model.Category;
import com.myapps.web.mycalendar.domain.service.AnniversaryCalculator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 기존 동작 보존을 위한 MockMvc 기반 테스트.
 *
 * <p>수정 전 코드에서 정상 동작하는 HTTP 요청/응답 패턴을 검증합니다.
 * 일정 생성 POST 리다이렉트, 캘린더 조회 200, 일정 상세 조회 200을
 * 확인하여 수정 후에도 동일한 동작이 보존되는지 테스트합니다.
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6</b>
 */
@WebMvcTest({ScheduleController.class, CalendarController.class})
class PreservationMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    @MockitoBean
    private AnniversaryCalculator anniversaryCalculator;

    @MockitoBean
    private CalendarViewHelper calendarViewHelper;

    /**
     * POST /schedules 요청이 일정을 생성하고 캘린더 페이지로
     * 302 리다이렉트하는지 확인합니다.
     *
     * <p>startDate 기반으로 /calendar/{year}/{month}로 리다이렉트됩니다.
     *
     * <p><b>Validates: Requirements 3.1</b>
     */
    @Test
    void should_redirect302ToCalendar_when_scheduleCreated() throws Exception {
        final ScheduleResponse response = createScheduleResponse(
                1L, LocalDate.of(2026, 7, 15));
        when(scheduleService.create(any())).thenReturn(response);

        mockMvc.perform(post("/schedules")
                        .param("category", "SEUNGKWON")
                        .param("startDate", "2026-07-15")
                        .param("content", "테스트 일정 내용"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar/2026/7"));
    }

    /**
     * POST /schedules 요청에서 endDate와 scheduleTime이 있는 경우에도
     * 정상 302 리다이렉트하는지 확인합니다.
     *
     * <p><b>Validates: Requirements 3.1</b>
     */
    @Test
    void should_redirect302ToCalendar_when_scheduleCreatedWithEndDateAndTime() throws Exception {
        final ScheduleResponse response = createScheduleResponse(
                2L, LocalDate.of(2026, 3, 10));
        when(scheduleService.create(any())).thenReturn(response);

        mockMvc.perform(post("/schedules")
                        .param("category", "DATE")
                        .param("startDate", "2026-03-10")
                        .param("endDate", "2026-03-12")
                        .param("scheduleTime", "14:30")
                        .param("content", "데이트 일정"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar/2026/3"));
    }

    /**
     * GET /calendar/{year}/{month} 요청이 200을 반환하고
     * 모델에 올바른 prev/next 월 정보를 포함하는지 확인합니다.
     *
     * <p><b>Validates: Requirements 3.2, 3.4</b>
     */
    @Test
    void should_return200WithCorrectNavigation_when_calendarRequested() throws Exception {
        when(scheduleService.findByMonth(any(YearMonth.class))).thenReturn(List.of());
        when(anniversaryCalculator.getAnniversariesForMonth(any(YearMonth.class)))
                .thenReturn(List.of());
        when(anniversaryCalculator.calculateDDay(any(LocalDate.class))).thenReturn(100L);

        mockMvc.perform(get("/calendar/2026/7"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar"))
                .andExpect(model().attribute("year", 2026))
                .andExpect(model().attribute("month", 7))
                .andExpect(model().attribute("prevYear", 2026))
                .andExpect(model().attribute("prevMonth", 6))
                .andExpect(model().attribute("nextYear", 2026))
                .andExpect(model().attribute("nextMonth", 8));
    }

    /**
     * GET /calendar/{year}/{month} 요청에서 12월→1월 경계가 올바르게
     * 처리되는지 확인합니다.
     *
     * <p><b>Validates: Requirements 3.2, 3.6</b>
     */
    @Test
    void should_handleYearBoundary_when_calendarDecemberRequested() throws Exception {
        when(scheduleService.findByMonth(any(YearMonth.class))).thenReturn(List.of());
        when(anniversaryCalculator.getAnniversariesForMonth(any(YearMonth.class)))
                .thenReturn(List.of());
        when(anniversaryCalculator.calculateDDay(any(LocalDate.class))).thenReturn(100L);

        mockMvc.perform(get("/calendar/2026/12"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("prevYear", 2026))
                .andExpect(model().attribute("prevMonth", 11))
                .andExpect(model().attribute("nextYear", 2027))
                .andExpect(model().attribute("nextMonth", 1));
    }

    /**
     * GET /calendar/{year}/{month} 요청에서 1월→12월(전년) 경계가 올바르게
     * 처리되는지 확인합니다.
     *
     * <p><b>Validates: Requirements 3.2, 3.6</b>
     */
    @Test
    void should_handleYearBoundary_when_calendarJanuaryRequested() throws Exception {
        when(scheduleService.findByMonth(any(YearMonth.class))).thenReturn(List.of());
        when(anniversaryCalculator.getAnniversariesForMonth(any(YearMonth.class)))
                .thenReturn(List.of());
        when(anniversaryCalculator.calculateDDay(any(LocalDate.class))).thenReturn(100L);

        mockMvc.perform(get("/calendar/2026/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("prevYear", 2025))
                .andExpect(model().attribute("prevMonth", 12))
                .andExpect(model().attribute("nextYear", 2026))
                .andExpect(model().attribute("nextMonth", 2));
    }

    /**
     * GET /schedules/{id} 요청이 200을 반환하고 일정 JSON 데이터를
     * 정상적으로 반환하는지 확인합니다.
     *
     * <p><b>Validates: Requirements 3.3</b>
     */
    @Test
    void should_return200WithScheduleData_when_detailRequested() throws Exception {
        final ScheduleResponse response = new ScheduleResponse(
                1L,
                Category.CHIWON,
                LocalDate.of(2026, 5, 20),
                LocalDate.of(2026, 5, 22),
                LocalTime.of(10, 30),
                "상세 조회 테스트 일정",
                LocalDateTime.of(2026, 5, 20, 9, 0),
                LocalDateTime.of(2026, 5, 20, 9, 0)
        );
        when(scheduleService.findById(eq(1L))).thenReturn(response);

        mockMvc.perform(get("/schedules/1"))
                .andExpect(status().isOk());
    }

    private ScheduleResponse createScheduleResponse(final Long id,
                                                    final LocalDate startDate) {
        return new ScheduleResponse(
                id,
                Category.SEUNGKWON,
                startDate,
                null,
                null,
                "테스트 일정",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 1, 10, 0)
        );
    }
}
