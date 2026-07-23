package com.myapps.web.mycalendar.interfaces.api;

import com.myapps.web.mycalendar.application.exception.CommentNotFoundException;
import com.myapps.web.mycalendar.application.exception.InvalidCommentException;
import com.myapps.web.mycalendar.application.exception.InvalidScheduleException;
import com.myapps.web.mycalendar.application.exception.ScheduleNotFoundException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 전역 예외 처리를 담당하는 ControllerAdvice 클래스.
 *
 * <p>비즈니스 예외를 캐치하여 적절한 HTTP 상태 코드와 함께
 * 에러 뷰를 렌더링합니다.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_BAD_REQUEST = 400;

    /**
     * 일정을 찾을 수 없을 때 404 에러 페이지를 반환합니다.
     *
     * @param ex       발생한 예외
     * @param model    뷰에 전달할 모델
     * @param response HTTP 응답 객체
     * @return 에러 뷰 이름
     */
    @ExceptionHandler(ScheduleNotFoundException.class)
    public String handleScheduleNotFound(final ScheduleNotFoundException ex,
                                         final Model model,
                                         final HttpServletResponse response) {
        response.setStatus(HTTP_NOT_FOUND);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    /**
     * 댓글을 찾을 수 없을 때 404 에러 페이지를 반환합니다.
     *
     * @param ex       발생한 예외
     * @param model    뷰에 전달할 모델
     * @param response HTTP 응답 객체
     * @return 에러 뷰 이름
     */
    @ExceptionHandler(CommentNotFoundException.class)
    public String handleCommentNotFound(final CommentNotFoundException ex,
                                        final Model model,
                                        final HttpServletResponse response) {
        response.setStatus(HTTP_NOT_FOUND);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    /**
     * 일정 유효성 검증 실패 시 400 에러 메시지를 표시합니다.
     *
     * @param ex       발생한 예외
     * @param model    뷰에 전달할 모델
     * @param response HTTP 응답 객체
     * @return 에러 뷰 이름
     */
    @ExceptionHandler(InvalidScheduleException.class)
    public String handleInvalidSchedule(final InvalidScheduleException ex,
                                        final Model model,
                                        final HttpServletResponse response) {
        response.setStatus(HTTP_BAD_REQUEST);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    /**
     * 댓글 유효성 검증 실패 시 400 에러 메시지를 표시합니다.
     *
     * @param ex       발생한 예외
     * @param model    뷰에 전달할 모델
     * @param response HTTP 응답 객체
     * @return 에러 뷰 이름
     */
    @ExceptionHandler(InvalidCommentException.class)
    public String handleInvalidComment(final InvalidCommentException ex,
                                       final Model model,
                                       final HttpServletResponse response) {
        response.setStatus(HTTP_BAD_REQUEST);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }
}
