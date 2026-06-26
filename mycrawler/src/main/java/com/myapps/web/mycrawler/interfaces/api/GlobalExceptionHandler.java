package com.myapps.web.mycrawler.interfaces.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 전역 예외 처리 핸들러.
 *
 * <p>AdminController 등 컨트롤러에서 발생하는 처리되지 않은 예외를
 * 캐치하여 사용자에게 에러 페이지를 표시합니다.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 모든 미처리 예외를 처리하여 에러 페이지를 반환합니다.
     *
     * @param exception 발생한 예외
     * @param model     Thymeleaf 모델
     * @return 에러 뷰 이름
     */
    @ExceptionHandler(Exception.class)
    public String handleException(final Exception exception, final Model model) {
        log.error("처리되지 않은 예외가 발생했습니다: {}", exception.getMessage(), exception);
        model.addAttribute("errorMessage", exception.getMessage());
        return "error";
    }
}
