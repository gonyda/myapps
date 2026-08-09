package com.myapps.web.myrpg.interfaces.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.myapps.web.myrpg.application.exception.CharacterCreationException;
import com.myapps.web.myrpg.application.exception.InsufficientAbilityPointsException;
import com.myapps.web.myrpg.application.exception.MapViewGenerationException;
import com.myapps.web.myrpg.application.exception.NodeNotFoundException;

/**
 * 전역 예외 처리 핸들러.
 *
 * <p>컨트롤러에서 발생하는 예외를 가로채어
 * 적절한 HTTP 상태 코드와 에러 뷰를 반환한다.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * {@link NodeNotFoundException} 발생 시 404 에러 뷰를 반환한다.
     *
     * @param exception 발생한 예외
     * @param model     Spring MVC 모델
     * @return 뷰 이름 {@code "error"}
     */
    @ExceptionHandler(NodeNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNodeNotFound(final NodeNotFoundException exception, final Model model) {
        LOG.warn("노드를 찾을 수 없음: {}", exception.getMessage());
        model.addAttribute("message", exception.getMessage());
        return "error";
    }

    /**
     * {@link MapViewGenerationException} 발생 시 500 에러 뷰를 반환한다.
     *
     * @param exception 발생한 예외
     * @param model     Spring MVC 모델
     * @return 뷰 이름 {@code "error"}
     */
    @ExceptionHandler(MapViewGenerationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleMapViewGeneration(final MapViewGenerationException exception, final Model model) {
        LOG.error("맵 뷰 생성 실패: {}", exception.getMessage(), exception);
        model.addAttribute("message", exception.getMessage());
        return "error";
    }

    /**
     * {@link CharacterCreationException} 발생 시 에러 뷰를 반환한다.
     *
     * @param exception 발생한 예외
     * @param model     Spring MVC 모델
     * @return 뷰 이름 {@code "error"}
     */
    @ExceptionHandler(CharacterCreationException.class)
    public String handleCharacterCreation(final CharacterCreationException exception, final Model model) {
        LOG.error("캐릭터 생성/저장 실패: {}", exception.getMessage(), exception);
        model.addAttribute("message", exception.getMessage());
        return "error";
    }

    /**
     * {@link InsufficientAbilityPointsException} 발생 시 승급 거부 안내를 반환한다.
     *
     * <p>AP 부족으로 스킬 랭크업이 거부되었음을 사용자에게 안내하며,
     * 캐릭터 상태(랭크·카운트·AP)는 변경되지 않는다.
     *
     * @param exception 발생한 예외
     * @param model     Spring MVC 모델
     * @return 뷰 이름 {@code "error"}
     */
    @ExceptionHandler(InsufficientAbilityPointsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInsufficientAbilityPoints(final InsufficientAbilityPointsException exception,
                                                  final Model model) {
        LOG.warn("AP 부족으로 승급 거부: {}", exception.getMessage());
        model.addAttribute("message", "AP가 부족하여 승급할 수 없습니다.");
        return "error";
    }
}
