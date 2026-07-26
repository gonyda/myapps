package com.myapps.web.myrpg.interfaces.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.myapps.web.myrpg.application.exception.MasterDataValidationException;
import com.myapps.web.myrpg.application.exception.PlayerNotFoundException;
import com.myapps.web.myrpg.domain.exception.IllegalActionException;
import com.myapps.web.myrpg.domain.exception.IllegalEquipmentException;
import com.myapps.web.myrpg.domain.exception.InsufficientGoldException;
import com.myapps.web.myrpg.domain.exception.InsufficientMpException;
import com.myapps.web.myrpg.domain.exception.MasterDataNotFoundException;

/**
 * 전역 예외 처리기.
 *
 * <p>도메인/애플리케이션 예외를 사용자 친화적 에러 화면으로 변환한다.
 * 게임 플레이 중 발생하는 비즈니스 예외는 안내 메시지와 함께
 * 마을로 돌아갈 수 있는 링크를 제공하고, 시스템 오류는 별도 안내를 표시한다.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String VIEW_ERROR = "rpg/error";
    private static final String ATTR_MESSAGE = "errorMessage";
    private static final String ATTR_ERROR_TYPE = "errorType";

    /**
     * MP 부족 예외를 처리한다.
     *
     * @param ex    발생한 예외
     * @param model 뷰 모델
     * @return 에러 뷰 이름
     */
    @ExceptionHandler(InsufficientMpException.class)
    public String handleInsufficientMp(final InsufficientMpException ex, final Model model) {
        log.warn("MP 부족: {}", ex.getMessage());
        model.addAttribute(ATTR_MESSAGE, ex.getMessage());
        model.addAttribute(ATTR_ERROR_TYPE, "game");
        return VIEW_ERROR;
    }

    /**
     * 골드 부족 예외를 처리한다.
     *
     * @param ex    발생한 예외
     * @param model 뷰 모델
     * @return 에러 뷰 이름
     */
    @ExceptionHandler(InsufficientGoldException.class)
    public String handleInsufficientGold(final InsufficientGoldException ex, final Model model) {
        log.warn("골드 부족: {}", ex.getMessage());
        model.addAttribute(ATTR_MESSAGE, ex.getMessage());
        model.addAttribute(ATTR_ERROR_TYPE, "game");
        return VIEW_ERROR;
    }

    /**
     * 장비 관련 잘못된 조작 예외를 처리한다.
     *
     * @param ex    발생한 예외
     * @param model 뷰 모델
     * @return 에러 뷰 이름
     */
    @ExceptionHandler(IllegalEquipmentException.class)
    public String handleIllegalEquipment(final IllegalEquipmentException ex, final Model model) {
        log.warn("잘못된 장비 조작: {}", ex.getMessage());
        model.addAttribute(ATTR_MESSAGE, ex.getMessage());
        model.addAttribute(ATTR_ERROR_TYPE, "game");
        return VIEW_ERROR;
    }

    /**
     * 잘못된 게임 행동 예외를 처리한다.
     *
     * @param ex    발생한 예외
     * @param model 뷰 모델
     * @return 에러 뷰 이름
     */
    @ExceptionHandler(IllegalActionException.class)
    public String handleIllegalAction(final IllegalActionException ex, final Model model) {
        log.warn("잘못된 행동: {}", ex.getMessage());
        model.addAttribute(ATTR_MESSAGE, ex.getMessage());
        model.addAttribute(ATTR_ERROR_TYPE, "game");
        return VIEW_ERROR;
    }

    /**
     * 플레이어 미존재 예외를 처리한다.
     *
     * @param ex    발생한 예외
     * @param model 뷰 모델
     * @return 에러 뷰 이름
     */
    @ExceptionHandler(PlayerNotFoundException.class)
    public String handlePlayerNotFound(final PlayerNotFoundException ex, final Model model) {
        log.warn("플레이어 미존재: {}", ex.getMessage());
        model.addAttribute(ATTR_MESSAGE, ex.getMessage());
        model.addAttribute(ATTR_ERROR_TYPE, "player");
        return VIEW_ERROR;
    }

    /**
     * 마스터 데이터 미존재 예외를 처리한다.
     *
     * @param ex    발생한 예외
     * @param model 뷰 모델
     * @return 에러 뷰 이름
     */
    @ExceptionHandler(MasterDataNotFoundException.class)
    public String handleMasterDataNotFound(final MasterDataNotFoundException ex, final Model model) {
        log.error("마스터 데이터 미존재: {}", ex.getMessage());
        model.addAttribute(ATTR_MESSAGE, "시스템 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        model.addAttribute(ATTR_ERROR_TYPE, "system");
        return VIEW_ERROR;
    }

    /**
     * 마스터 데이터 유효성 검증 실패 예외를 처리한다.
     *
     * @param ex    발생한 예외
     * @param model 뷰 모델
     * @return 에러 뷰 이름
     */
    @ExceptionHandler(MasterDataValidationException.class)
    public String handleMasterDataValidation(final MasterDataValidationException ex,
                                             final Model model) {
        log.error("마스터 데이터 검증 실패: {}", ex.getMessage());
        model.addAttribute(ATTR_MESSAGE, "시스템 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        model.addAttribute(ATTR_ERROR_TYPE, "system");
        return VIEW_ERROR;
    }
}
