package com.myapps.web.myrpg.infrastructure.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 로그인 세션 유무를 검사하여 미인증 접근을 제어하는 Spring MVC 인터셉터.
 *
 * <p>세션 내 {@code "LOGIN_USER"} 어트리뷰트가 존재하지 않는 경우, 일반 HTML 요청은 {@code /login}으로 302 리다이렉트하고,
 * AJAX/JSON 요청은 HTTP 401 Unauthorized 오류를 반환합니다.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_USER_KEY = "LOGIN_USER";
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_X_REQUESTED_WITH = "X-Requested-With";
    private static final String JSON_MEDIA_TYPE = "application/json";
    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";
    private static final String LOGIN_REDIRECT_URL = "/login";
    private static final String UNAUTHORIZED_MESSAGE = "로그인이 필요합니다.";

    @Override
    public boolean preHandle(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler)
            throws Exception {
        final HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SESSION_USER_KEY) != null) {
            return true;
        }

        final String accept = request.getHeader(HEADER_ACCEPT);
        final String xRequestedWith = request.getHeader(HEADER_X_REQUESTED_WITH);
        final boolean isAjax =
                (accept != null && accept.contains(JSON_MEDIA_TYPE))
                        || XML_HTTP_REQUEST.equalsIgnoreCase(xRequestedWith);

        if (isAjax) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
        } else {
            response.sendRedirect(LOGIN_REDIRECT_URL);
        }
        return false;
    }
}
