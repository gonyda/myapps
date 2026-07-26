package com.myapps.web.myrpg.interfaces.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;

import com.myapps.web.myrpg.application.exception.MasterDataValidationException;
import com.myapps.web.myrpg.application.exception.PlayerNotFoundException;
import com.myapps.web.myrpg.domain.exception.IllegalActionException;
import com.myapps.web.myrpg.domain.exception.IllegalEquipmentException;
import com.myapps.web.myrpg.domain.exception.InsufficientGoldException;
import com.myapps.web.myrpg.domain.exception.InsufficientMpException;
import com.myapps.web.myrpg.domain.exception.MasterDataNotFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * GlobalExceptionHandler의 단위 테스트.
 *
 * <p>각 예외 유형별로 올바른 에러 뷰와 메시지가 반환되는지 검증한다.
 * MockMvc standaloneSetup을 사용하여 테스트 전용 컨트롤러에서 예외를 발생시키고,
 * ControllerAdvice가 적절히 처리하는지 확인한다.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ExceptionThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_renderGameError_when_insufficientMpExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/throw-insufficient-mp"))
                .andExpect(status().isOk())
                .andExpect(view().name("rpg/error"))
                .andExpect(model().attribute("errorMessage", "MP가 부족합니다."))
                .andExpect(model().attribute("errorType", "game"));
    }

    @Test
    void should_renderGameError_when_insufficientGoldExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/throw-insufficient-gold"))
                .andExpect(status().isOk())
                .andExpect(view().name("rpg/error"))
                .andExpect(model().attribute("errorMessage", "골드가 부족합니다."))
                .andExpect(model().attribute("errorType", "game"));
    }

    @Test
    void should_renderGameError_when_illegalEquipmentExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/throw-illegal-equipment"))
                .andExpect(status().isOk())
                .andExpect(view().name("rpg/error"))
                .andExpect(model().attribute("errorMessage", "착용 중인 장비는 판매할 수 없습니다."))
                .andExpect(model().attribute("errorType", "game"));
    }

    @Test
    void should_renderGameError_when_illegalActionExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/throw-illegal-action"))
                .andExpect(status().isOk())
                .andExpect(view().name("rpg/error"))
                .andExpect(model().attribute("errorMessage", "전투 중에는 포기할 수 없습니다."))
                .andExpect(model().attribute("errorType", "game"));
    }

    @Test
    void should_renderPlayerError_when_playerNotFoundExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/throw-player-not-found"))
                .andExpect(status().isOk())
                .andExpect(view().name("rpg/error"))
                .andExpect(model().attribute("errorMessage", "플레이어를 찾을 수 없습니다."))
                .andExpect(model().attribute("errorType", "player"));
    }

    @Test
    void should_renderSystemError_when_masterDataNotFoundExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/throw-master-data-not-found"))
                .andExpect(status().isOk())
                .andExpect(view().name("rpg/error"))
                .andExpect(model().attribute("errorMessage",
                        "시스템 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."))
                .andExpect(model().attribute("errorType", "system"));
    }

    @Test
    void should_renderSystemError_when_masterDataValidationExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/throw-master-data-validation"))
                .andExpect(status().isOk())
                .andExpect(view().name("rpg/error"))
                .andExpect(model().attribute("errorMessage",
                        "시스템 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."))
                .andExpect(model().attribute("errorType", "system"));
    }

    /**
     * 테스트 전용 컨트롤러. 각 예외를 발생시키는 엔드포인트를 제공한다.
     */
    @Controller
    static class ExceptionThrowingController {

        @GetMapping("/test/throw-insufficient-mp")
        public String throwInsufficientMp() {
            throw new InsufficientMpException("MP가 부족합니다.");
        }

        @GetMapping("/test/throw-insufficient-gold")
        public String throwInsufficientGold() {
            throw new InsufficientGoldException("골드가 부족합니다.");
        }

        @GetMapping("/test/throw-illegal-equipment")
        public String throwIllegalEquipment() {
            throw new IllegalEquipmentException("착용 중인 장비는 판매할 수 없습니다.");
        }

        @GetMapping("/test/throw-illegal-action")
        public String throwIllegalAction() {
            throw new IllegalActionException("전투 중에는 포기할 수 없습니다.");
        }

        @GetMapping("/test/throw-player-not-found")
        public String throwPlayerNotFound() {
            throw new PlayerNotFoundException("플레이어를 찾을 수 없습니다.");
        }

        @GetMapping("/test/throw-master-data-not-found")
        public String throwMasterDataNotFound() {
            throw new MasterDataNotFoundException("monster id=999 not found");
        }

        @GetMapping("/test/throw-master-data-validation")
        public String throwMasterDataValidation() {
            throw new MasterDataValidationException("gradeChance 합이 1.0이 아닙니다.");
        }
    }
}
