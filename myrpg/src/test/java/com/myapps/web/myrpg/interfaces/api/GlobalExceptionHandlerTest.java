package com.myapps.web.myrpg.interfaces.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.myapps.web.myrpg.application.exception.InsufficientAbilityPointsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * {@link GlobalExceptionHandler}의 {@link InsufficientAbilityPointsException} 핸들러 동작 검증.
 *
 * <p>AP 부족으로 승급이 거부될 때 HTTP 400과 사용자 안내 메시지가 반환되는지 확인한다. 스탠드얼론 MockMvc를 사용하여 전체 컨텍스트 로드 없이 핸들러 로직만
 * 검증한다.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    /** 테스트 컨트롤러와 {@link GlobalExceptionHandler}로 MockMvc를 구성한다. */
    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new TestController())
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    /** InsufficientAbilityPointsException 발생 시 400 상태, error 뷰, 안내 메시지를 반환하는지 검증한다. */
    @Test
    void should_returnBadRequestWithMessage_when_insufficientAbilityPoints() throws Exception {
        mockMvc.perform(get("/test/insufficient-ap"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("message", "AP가 부족하여 승급할 수 없습니다."));
    }

    /**
     * 예외 핸들러 테스트용 내부 컨트롤러.
     *
     * <p>테스트에서 특정 예외를 발생시키기 위한 용도로만 사용한다.
     */
    @Controller
    static class TestController {

        /**
         * InsufficientAbilityPointsException을 강제 발생시키는 엔드포인트.
         *
         * @return 반환값 없음 (항상 예외 발생)
         */
        @GetMapping("/test/insufficient-ap")
        public String throwInsufficientAp() {
            throw new InsufficientAbilityPointsException("AP 5 필요, 보유 2");
        }
    }
}
