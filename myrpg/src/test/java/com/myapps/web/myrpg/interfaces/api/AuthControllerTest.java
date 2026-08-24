package com.myapps.web.myrpg.interfaces.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.myapps.web.myrpg.application.dto.UserSession;
import com.myapps.web.myrpg.application.exception.AuthenticationException;
import com.myapps.web.myrpg.application.service.AuthService;
import com.myapps.web.myrpg.infrastructure.interceptor.AuthInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** {@link AuthController} 웹 슬라이스 테스트. */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuthService authService;

    @MockitoBean private AuthInterceptor authInterceptor;

    @Test
    @DisplayName("비로그인 사용자가 GET /login 접근 시 login 뷰가 200으로 반환된다")
    void should_returnLoginView_when_notLoggedIn() throws Exception {
        // given & when & then
        mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login"));
    }

    @Test
    @DisplayName("이미 로그인된 사용자가 GET /login 접근 시 메인 화면으로 리다이렉트된다")
    void should_redirectToHome_when_alreadyLoggedIn() throws Exception {
        // given
        final MockHttpSession session = new MockHttpSession();
        final UserSession userSession = new UserSession(1L, "bbsk", "고니", 1L);
        session.setAttribute(AuthInterceptor.SESSION_USER_KEY, userSession);

        // when & then
        mockMvc.perform(get("/login").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("올바른 자격증명으로 POST /login 시 세션이 등록되고 메인 화면으로 리다이렉트된다")
    void should_authenticateAndRedirectHome_when_validCredentials() throws Exception {
        // given
        final UserSession userSession = new UserSession(1L, "bbsk", "고니", 1L);
        given(authService.authenticate("bbsk", "1")).willReturn(userSession);

        // when & then
        mockMvc.perform(post("/login").param("username", "bbsk").param("password", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(
                        request().sessionAttribute(AuthInterceptor.SESSION_USER_KEY, userSession));
    }

    @Test
    @DisplayName("잘못된 자격증명으로 POST /login 시 error 모델과 함께 login 뷰가 반환된다")
    void should_returnLoginViewWithError_when_invalidCredentials() throws Exception {
        // given
        given(authService.authenticate("bbsk", "wrong"))
                .willThrow(new AuthenticationException("아이디 또는 비밀번호가 일치하지 않습니다."));

        // when & then
        mockMvc.perform(post("/login").param("username", "bbsk").param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "아이디 또는 비밀번호가 일치하지 않습니다."));
    }

    @Test
    @DisplayName("GET /logout 호출 시 세션이 무효화되고 /login으로 리다이렉트된다")
    void should_invalidateSessionAndRedirectLogin_when_logoutGet() throws Exception {
        // given
        final MockHttpSession session = new MockHttpSession();
        final UserSession userSession = new UserSession(1L, "bbsk", "고니", 1L);
        session.setAttribute(AuthInterceptor.SESSION_USER_KEY, userSession);

        // when & then
        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("POST /logout 호출 시 세션이 무효화되고 /login으로 리다이렉트된다")
    void should_invalidateSessionAndRedirectLogin_when_logoutPost() throws Exception {
        // given
        final MockHttpSession session = new MockHttpSession();
        final UserSession userSession = new UserSession(1L, "bbsk", "고니", 1L);
        session.setAttribute(AuthInterceptor.SESSION_USER_KEY, userSession);

        // when & then
        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
