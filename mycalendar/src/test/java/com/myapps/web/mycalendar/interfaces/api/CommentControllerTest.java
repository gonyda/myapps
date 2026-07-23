package com.myapps.web.mycalendar.interfaces.api;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.myapps.web.mycalendar.application.dto.CommentResponse;
import com.myapps.web.mycalendar.application.exception.CommentNotFoundException;
import com.myapps.web.mycalendar.application.exception.InvalidCommentException;
import com.myapps.web.mycalendar.application.service.CommentService;
import com.myapps.web.mycalendar.domain.model.Author;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
 * CommentController의 웹 슬라이스 테스트.
 *
 * <p>MockMvc를 활용하여 댓글 생성, 수정 폼, 수정 처리, 삭제의
 * HTTP 요청/응답을 검증합니다.
 */
@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    /**
     * POST /schedules/{scheduleId}/comments 요청 시
     * 댓글 생성 후 일정 상세 페이지로 리다이렉트되는지 검증합니다.
     */
    @Test
    void should_redirectToScheduleDetail_when_commentCreatedSuccessfully() throws Exception {
        final Long scheduleId = 1L;
        final CommentResponse response = new CommentResponse(
                1L, Author.SEUNGKWON, "댓글 내용", LocalDateTime.now());

        when(commentService.create(eq(scheduleId), any())).thenReturn(response);

        mockMvc.perform(post("/schedules/{scheduleId}/comments", scheduleId)
                        .param("author", "SEUNGKWON")
                        .param("content", "댓글 내용"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/schedules/1"));
    }

    /**
     * POST /schedules/{scheduleId}/comments 요청 시
     * 유효성 검증 실패로 InvalidCommentException이 발생하면 에러 뷰를 반환하는지 검증합니다.
     */
    @Test
    void should_returnErrorView_when_commentCreationValidationFails() throws Exception {
        final Long scheduleId = 1L;

        when(commentService.create(eq(scheduleId), any()))
                .thenThrow(new InvalidCommentException("내용을 입력해주세요"));

        mockMvc.perform(post("/schedules/{scheduleId}/comments", scheduleId)
                        .param("author", "SEUNGKWON")
                        .param("content", ""))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorMessage", "내용을 입력해주세요"));
    }

    /**
     * GET /comments/{id}/edit 요청 시
     * 댓글 수정 폼 뷰와 모델 속성이 올바르게 전달되는지 검증합니다.
     */
    @Test
    void should_returnEditFormView_when_editFormRequested() throws Exception {
        final Long commentId = 1L;
        final Long scheduleId = 5L;
        final CommentResponse response = new CommentResponse(
                commentId, Author.SEUNGKWON, "기존 댓글 내용", LocalDateTime.now());

        when(commentService.findById(commentId)).thenReturn(response);
        when(commentService.findScheduleIdByCommentId(commentId)).thenReturn(scheduleId);

        mockMvc.perform(get("/comments/{id}/edit", commentId))
                .andExpect(status().isOk())
                .andExpect(view().name("comment-form"))
                .andExpect(model().attributeExists("commentForm"))
                .andExpect(model().attribute("commentId", commentId))
                .andExpect(model().attribute("scheduleId", scheduleId))
                .andExpect(model().attribute("authors", Author.values()));
    }

    /**
     * PUT /comments/{id} 요청 시
     * 댓글 수정 후 일정 상세 페이지로 리다이렉트되는지 검증합니다.
     */
    @Test
    void should_redirectToScheduleDetail_when_commentUpdatedSuccessfully() throws Exception {
        final Long commentId = 1L;
        final Long scheduleId = 5L;
        final CommentResponse response = new CommentResponse(
                commentId, Author.SEUNGKWON, "수정된 내용", LocalDateTime.now());

        when(commentService.update(eq(commentId), any())).thenReturn(response);

        mockMvc.perform(put("/comments/{id}", commentId)
                        .param("scheduleId", String.valueOf(scheduleId))
                        .param("content", "수정된 내용"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/schedules/5"));
    }

    /**
     * PUT /comments/{id} 요청 시
     * 유효성 검증 실패로 InvalidCommentException이 발생하면 에러 뷰를 반환하는지 검증합니다.
     */
    @Test
    void should_returnErrorView_when_commentUpdateValidationFails() throws Exception {
        final Long commentId = 1L;
        final Long scheduleId = 5L;

        when(commentService.update(eq(commentId), any()))
                .thenThrow(new InvalidCommentException("200자를 초과할 수 없습니다"));

        mockMvc.perform(put("/comments/{id}", commentId)
                        .param("scheduleId", String.valueOf(scheduleId))
                        .param("content", "a".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorMessage", "200자를 초과할 수 없습니다"));
    }

    /**
     * DELETE /comments/{id} 요청 시
     * 댓글 삭제 후 일정 상세 페이지로 리다이렉트되는지 검증합니다.
     */
    @Test
    void should_redirectToScheduleDetail_when_commentDeletedSuccessfully() throws Exception {
        final Long commentId = 1L;
        final Long scheduleId = 5L;

        doNothing().when(commentService).delete(commentId);

        mockMvc.perform(delete("/comments/{id}", commentId)
                        .param("scheduleId", String.valueOf(scheduleId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/schedules/5"));
    }

    /**
     * GET /comments/{id}/edit 요청 시
     * 댓글이 존재하지 않으면 404 에러 페이지를 반환하는지 검증합니다.
     */
    @Test
    void should_returnNotFoundErrorPage_when_commentNotFoundOnEdit() throws Exception {
        final Long commentId = 999L;

        when(commentService.findById(commentId))
                .thenThrow(new CommentNotFoundException("댓글을 찾을 수 없습니다: ID=999"));

        mockMvc.perform(get("/comments/{id}/edit", commentId))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorMessage", "댓글을 찾을 수 없습니다: ID=999"));
    }
}
