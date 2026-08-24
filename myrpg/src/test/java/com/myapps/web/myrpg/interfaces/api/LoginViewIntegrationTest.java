package com.myapps.web.myrpg.interfaces.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.myapps.web.myrpg.application.dto.UserSession;
import com.myapps.web.myrpg.infrastructure.interceptor.AuthInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 로그인 화면 및 세션 인터셉터 E2E 통합 테스트. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginViewIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("GET /login 요청 시 다크 판타지 로그인 카드와 퀵 로그인 프리셋이 정상 렌더링된다")
    void should_renderLoginPageWithQuickPresets_when_loginRequested() throws Exception {
        // given & when & then
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(containsString("MYRPG")))
                .andExpect(content().string(containsString("bbsk")))
                .andExpect(content().string(containsString("admin")))
                .andExpect(content().string(containsString("빠른 테스트 로그인")))
                .andExpect(content().string(containsString("아이디")))
                .andExpect(content().string(containsString("비밀번호")));
    }

    @Test
    @DisplayName("미인증 사용자가 보호된 GET / 접근 시 /login으로 302 리다이렉트된다")
    void should_redirectToLogin_when_unauthenticatedUserAccessesRoot() throws Exception {
        // given & when & then
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("미인증 사용자가 보호된 API에 AJAX 요청 시 401 Unauthorized가 반환된다")
    void should_return401_when_unauthenticatedAjaxRequest() throws Exception {
        // given & when & then
        mockMvc.perform(post("/move").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("bbsk 계정으로 POST /login 시 세션이 발급되고 /로 정상 리다이렉트된다")
    void should_loginSuccessfully_when_validPresetCredentials() throws Exception {
        // given & when & then
        mockMvc.perform(post("/login").param("username", "bbsk").param("password", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("로그인된 세션으로 GET / 접근 시 200 OK와 함께 메인 게임 화면이 렌더링된다")
    void should_renderGameScreen_when_authenticatedSession() throws Exception {
        // given
        final MockHttpSession session = new MockHttpSession();
        final UserSession userSession = new UserSession(1L, "bbsk", "고니", 1L);
        session.setAttribute(AuthInterceptor.SESSION_USER_KEY, userSession);

        // when & then
        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("play"))
                .andExpect(content().string(containsString("로그아웃")));
    }
}
