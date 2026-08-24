package com.myapps.web.myrpg.infrastructure.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.myapps.web.myrpg.application.dto.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link AuthInterceptor} 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private Object handler;

    @InjectMocks private AuthInterceptor authInterceptor;

    @Test
    @DisplayName("로그인 세션이 존재하는 경우 preHandle이 true를 반환한다")
    void should_allowRequest_when_sessionExists() throws Exception {
        // given
        final UserSession userSession = new UserSession(1L, "bbsk", "고니", 1L);
        given(request.getSession(false)).willReturn(session);
        given(session.getAttribute(AuthInterceptor.SESSION_USER_KEY)).willReturn(userSession);

        // when
        final boolean allowed = authInterceptor.preHandle(request, response, handler);

        // then
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("비로그인 일반 HTML 요청인 경우 /login으로 302 리다이렉트하고 false를 반환한다")
    void should_redirectToLogin_when_unauthenticatedHtmlRequest() throws Exception {
        // given
        given(request.getSession(false)).willReturn(null);
        given(request.getHeader("Accept")).willReturn("text/html,application/xhtml+xml");
        given(request.getHeader("X-Requested-With")).willReturn(null);

        // when
        final boolean allowed = authInterceptor.preHandle(request, response, handler);

        // then
        assertThat(allowed).isFalse();
        verify(response).sendRedirect("/login");
    }

    @Test
    @DisplayName("비로그인 AJAX/JSON 요청인 경우 401 오류를 전송하고 false를 반환한다")
    void should_send401Unauthorized_when_unauthenticatedAjaxRequest() throws Exception {
        // given
        given(request.getSession(false)).willReturn(null);
        given(request.getHeader("Accept")).willReturn("application/json, text/javascript");
        given(request.getHeader("X-Requested-With")).willReturn("XMLHttpRequest");

        // when
        final boolean allowed = authInterceptor.preHandle(request, response, handler);

        // then
        assertThat(allowed).isFalse();
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다.");
    }
}
