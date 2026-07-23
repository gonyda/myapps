package com.myapps.web.mycalendar.interfaces.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.myapps.web.mycalendar.application.dto.ScheduleResponse;
import com.myapps.web.mycalendar.application.exception.InvalidScheduleException;
import com.myapps.web.mycalendar.application.exception.ScheduleNotFoundException;
import com.myapps.web.mycalendar.application.service.ScheduleService;
import com.myapps.web.mycalendar.domain.model.Category;
import com.myapps.web.mycalendar.interfaces.dto.CommentForm;
import com.myapps.web.mycalendar.interfaces.dto.ScheduleForm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * ScheduleController의 웹 슬라이스 테스트.
 *
 * <p>MockMvc를 활용하여 일정 생성/수정/삭제 HTTP 요청 처리와
 * 유효성 검증 실패 시 에러 응답을 검증합니다.
 */
@WebMvcTest(ScheduleController.class)
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    /**
     * GET /schedules/{id} 요청 시 일정 상세 뷰와 모델 속성을 검증합니다.
     */
    @Test
    void should_returnScheduleDetailView_when_scheduleExists() throws Exception {
        final Long scheduleId = 1L;
        final ScheduleResponse response = createScheduleResponse(scheduleId);
        when(scheduleService.findById(scheduleId)).thenReturn(response);

        mockMvc.perform(get("/schedules/{id}", scheduleId))
                .andExpect(status().isOk())
                .andExpect(view().name("schedule-detail"))
                .andExpect(model().attribute("schedule", response))
                .andExpect(model().attribute("commentForm", new CommentForm(null, null)))
                .andExpect(model().attribute("categories", Category.values()));
    }

    /**
     * GET /schedules/new 요청 시 빈 ScheduleForm과 카테고리 목록이 포함된 폼 뷰를 검증합니다.
     */
    @Test
    void should_returnScheduleFormView_when_newFormRequested() throws Exception {
        mockMvc.perform(get("/schedules/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("schedule-form"))
                .andExpect(model().attribute("scheduleForm",
                        new ScheduleForm(null, null, null, null, null)))
                .andExpect(model().attribute("categories", Category.values()));
    }

    /**
     * POST /schedules 요청 시 일정 생성 성공 후 캘린더 뷰로 리다이렉트되는지 검증합니다.
     */
    @Test
    void should_redirectToCalendar_when_scheduleCreatedSuccessfully() throws Exception {
        final ScheduleResponse response = createScheduleResponse(1L);
        when(scheduleService.create(any())).thenReturn(response);

        mockMvc.perform(post("/schedules")
                        .param("category", "SEUNGKWON")
                        .param("startDate", "2026-07-01")
                        .param("content", "테스트 일정"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar/2026/7"));
    }

    /**
     * POST /schedules 요청 시 유효성 검증 실패로 InvalidScheduleException이 발생하면
     * 에러 뷰를 반환하는지 검증합니다.
     */
    @Test
    void should_returnErrorView_when_createScheduleValidationFails() throws Exception {
        when(scheduleService.create(any()))
                .thenThrow(new InvalidScheduleException("내용을 입력해주세요"));

        mockMvc.perform(post("/schedules")
                        .param("category", "SEUNGKWON")
                        .param("startDate", "2026-07-01")
                        .param("content", ""))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorMessage", "내용을 입력해주세요"));
    }

    /**
     * GET /schedules/{id}/edit 요청 시 기존 일정 데이터가 채워진 수정 폼을 검증합니다.
     */
    @Test
    void should_returnEditForm_when_editFormRequested() throws Exception {
        final Long scheduleId = 1L;
        final ScheduleResponse response = createScheduleResponse(scheduleId);
        when(scheduleService.findById(scheduleId)).thenReturn(response);

        final ScheduleForm expectedForm = new ScheduleForm(
                response.category(),
                response.startDate(),
                response.endDate(),
                response.scheduleTime(),
                response.content()
        );

        mockMvc.perform(get("/schedules/{id}/edit", scheduleId))
                .andExpect(status().isOk())
                .andExpect(view().name("schedule-form"))
                .andExpect(model().attribute("scheduleForm", expectedForm))
                .andExpect(model().attribute("scheduleId", scheduleId))
                .andExpect(model().attribute("categories", Category.values()));
    }

    /**
     * PUT /schedules/{id} 요청 시 일정 수정 성공 후 일정 상세 페이지로 리다이렉트되는지 검증합니다.
     */
    @Test
    void should_redirectToScheduleDetail_when_scheduleUpdatedSuccessfully() throws Exception {
        final Long scheduleId = 1L;
        final ScheduleResponse response = createScheduleResponse(scheduleId);
        when(scheduleService.update(eq(scheduleId), any())).thenReturn(response);

        mockMvc.perform(put("/schedules/{id}", scheduleId)
                        .param("category", "CHIWON")
                        .param("startDate", "2026-07-15")
                        .param("content", "수정된 일정"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/schedules/1"));
    }

    /**
     * PUT /schedules/{id} 요청 시 유효성 검증 실패로 InvalidScheduleException이 발생하면
     * 에러 뷰를 반환하는지 검증합니다.
     */
    @Test
    void should_returnErrorView_when_updateScheduleValidationFails() throws Exception {
        final Long scheduleId = 1L;
        when(scheduleService.update(eq(scheduleId), any()))
                .thenThrow(new InvalidScheduleException("200자를 초과할 수 없습니다"));

        mockMvc.perform(put("/schedules/{id}", scheduleId)
                        .param("category", "SEUNGKWON")
                        .param("startDate", "2026-07-01")
                        .param("content", "a".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorMessage", "200자를 초과할 수 없습니다"));
    }

    /**
     * DELETE /schedules/{id} 요청 시 일정 삭제 성공 후 루트 페이지로 리다이렉트되는지 검증합니다.
     */
    @Test
    void should_redirectToRoot_when_scheduleDeletedSuccessfully() throws Exception {
        final Long scheduleId = 1L;
        doNothing().when(scheduleService).delete(scheduleId);

        mockMvc.perform(delete("/schedules/{id}", scheduleId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    /**
     * GET /schedules/{id} 요청 시 일정이 존재하지 않으면 404 에러 페이지를 반환하는지 검증합니다.
     */
    @Test
    void should_return404ErrorView_when_scheduleNotFound() throws Exception {
        final Long scheduleId = 999L;
        when(scheduleService.findById(scheduleId))
                .thenThrow(new ScheduleNotFoundException("일정을 찾을 수 없습니다: ID=999"));

        mockMvc.perform(get("/schedules/{id}", scheduleId))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorMessage", "일정을 찾을 수 없습니다: ID=999"));
    }

    private ScheduleResponse createScheduleResponse(final Long id) {
        return new ScheduleResponse(
                id,
                Category.SEUNGKWON,
                LocalDate.of(2026, 7, 1),
                null,
                null,
                "테스트 일정",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 1, 10, 0),
                List.of()
        );
    }
}
