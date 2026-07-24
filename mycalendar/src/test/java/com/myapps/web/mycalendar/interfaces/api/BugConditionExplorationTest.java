package com.myapps.web.mycalendar.interfaces.api;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.myapps.web.mycalendar.application.dto.ScheduleResponse;
import com.myapps.web.mycalendar.application.service.ScheduleService;
import com.myapps.web.mycalendar.domain.model.Category;
import com.myapps.web.mycalendar.domain.service.AnniversaryCalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 버그 조건 탐색 테스트.
 *
 * <p>수정 전 코드에서 버그를 재현하여 근본 원인 분석을 확인합니다.
 * 이 테스트들은 미수정 코드에서 실패(FAIL)하는 것이 정상이며,
 * 실패는 버그의 존재를 증명합니다.
 *
 * <p>Bug Condition 1: PUT 라우팅 실패 (HiddenHttpMethodFilter 비활성화)
 * <p>Bug Condition 2: DELETE 라우팅 실패 (HiddenHttpMethodFilter 비활성화)
 * <p>Bug Condition 3: 타임존 불일치 (LocalDate.now() ZoneId 미지정)
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 1.3</b>
 */
@WebMvcTest({ScheduleController.class, CalendarController.class})
class BugConditionExplorationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    @MockitoBean
    private AnniversaryCalculator anniversaryCalculator;

    @MockitoBean
    private CalendarViewHelper calendarViewHelper;

    /**
     * Bug Condition 1: POST /schedules/{id} + _method=PUT 전송 시
     * HiddenHttpMethodFilter를 통해 @PutMapping("/{id}")으로 정상 라우팅되어
     * 302 리다이렉트가 반환되는지 확인합니다.
     *
     * <p>미수정 코드에서는 HiddenHttpMethodFilter가 비활성화되어 있으므로
     * _method=PUT이 무시되고 POST /schedules/1로 처리되어 405가 반환됩니다.
     * 따라서 이 테스트는 미수정 코드에서 실패합니다.
     *
     * <p><b>Validates: Requirements 2.1</b>
     */
    @Test
    void should_routeToPutMapping_when_postWithMethodPut() throws Exception {
        final Long scheduleId = 1L;
        final ScheduleResponse response = createScheduleResponse(scheduleId);
        when(scheduleService.update(eq(scheduleId), any())).thenReturn(response);

        mockMvc.perform(post("/schedules/{id}", scheduleId)
                        .param("_method", "PUT")
                        .param("category", "SEUNGKWON")
                        .param("startDate", "2026-07-01")
                        .param("content", "수정된 일정"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/schedules/1"));
    }

    /**
     * Bug Condition 2: POST /schedules/{id} + _method=DELETE 전송 시
     * HiddenHttpMethodFilter를 통해 @DeleteMapping("/{id}")으로 정상 라우팅되어
     * 302 리다이렉트가 반환되는지 확인합니다.
     *
     * <p>미수정 코드에서는 HiddenHttpMethodFilter가 비활성화되어 있으므로
     * _method=DELETE가 무시되고 POST /schedules/1로 처리되어 405가 반환됩니다.
     * 따라서 이 테스트는 미수정 코드에서 실패합니다.
     *
     * <p><b>Validates: Requirements 2.2</b>
     */
    @Test
    void should_routeToDeleteMapping_when_postWithMethodDelete() throws Exception {
        final Long scheduleId = 1L;
        doNothing().when(scheduleService).delete(scheduleId);

        mockMvc.perform(post("/schedules/{id}", scheduleId)
                        .param("_method", "DELETE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    /**
     * Bug Condition 3: CalendarController가 한국 시간(Asia/Seoul) 기준 정확한 오늘 날짜를
     * 모델에 설정하는지 검증합니다.
     *
     * <p>CalendarController.showCalendar()가 {@code LocalDate.now(ZoneId.of("Asia/Seoul"))}을
     * 사용하여 today 속성을 설정해야 합니다. JVM 기본 타임존에 관계없이 한국 시간 기준
     * 정확한 오늘 날짜가 모델에 전달되어야 합니다.
     *
     * <p>이 테스트는 MockMvc를 통해 실제 CalendarController를 호출하고,
     * 모델의 today 속성이 {@code LocalDate.now(ZoneId.of("Asia/Seoul"))}과
     * 일치하는지 확인합니다. 미수정 코드에서 LocalDate.now()가 ZoneId 없이
     * 호출되면 JVM 타임존에 의존하여 잘못된 값이 설정될 수 있습니다.
     *
     * <p><b>Validates: Requirements 2.3</b>
     *
     * @throws Exception MockMvc 호출 시 발생할 수 있는 예외
     */
    @Test
    void should_returnKoreanDate_when_calendarIsRendered() throws Exception {
        when(scheduleService.findByMonth(any())).thenReturn(Collections.emptyList());
        when(anniversaryCalculator.getAnniversariesForMonth(any())).thenReturn(Collections.emptyList());
        when(anniversaryCalculator.calculateDDay(any())).thenReturn(1L);

        final LocalDate expectedToday = LocalDate.now(ZoneId.of("Asia/Seoul"));

        final MvcResult result = mockMvc.perform(get("/calendar/2026/7"))
                .andExpect(status().isOk())
                .andReturn();

        final LocalDate modelToday = (LocalDate) result.getModelAndView().getModel().get("today");

        assertThat(modelToday)
                .as("CalendarController의 today 속성은 항상 Asia/Seoul 기준 오늘 날짜여야 합니다")
                .isEqualTo(expectedToday);
    }

    private ScheduleResponse createScheduleResponse(final Long id) {
        return new ScheduleResponse(
                id,
                Category.SEUNGKWON,
                LocalDate.of(2026, 7, 1),
                null,
                null,
                "테스트 일정",
                java.time.LocalDateTime.of(2026, 7, 1, 10, 0),
                java.time.LocalDateTime.of(2026, 7, 1, 10, 0)
        );
    }
}
