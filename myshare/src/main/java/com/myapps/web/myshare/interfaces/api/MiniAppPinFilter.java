package com.myapps.web.myshare.interfaces.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code /api/mini/**} 경로로 들어오는 요청의 {@code X-Mini-Pin} 헤더를 검증하는 서블릿 필터.
 *
 * <p>기존 {@code /share/**} 및 {@code /health} 경로는 검증 대상에서 제외하며, 설정된 PIN이 비어 있거나 헤더 값이 일치하지 않으면 {@code
 * 401 Unauthorized} JSON 응답을 반환합니다.
 */
@Component
public class MiniAppPinFilter extends OncePerRequestFilter {

    private static final String MINI_APP_BASE_PATH = "/api/mini";
    private static final String HEADER_MINI_PIN = "X-Mini-Pin";
    private static final String UNAUTHORIZED_BODY = "{\"error\":\"Unauthorized\"}";

    private final String configuredPin;

    /**
     * MiniAppPinFilter를 생성합니다.
     *
     * @param configuredPin 서버 설정에 등록된 미니앱 공용 PIN
     */
    public MiniAppPinFilter(@Value("${myshare.mini-app.pin:}") final String configuredPin) {
        this.configuredPin = configuredPin == null ? "" : configuredPin.trim();
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String path = extractNormalizedPath(request);
        return !(MINI_APP_BASE_PATH.equals(path)
                || path.startsWith(MINI_APP_BASE_PATH + "/")
                || path.startsWith(MINI_APP_BASE_PATH + ";"));
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain)
            throws ServletException, IOException {
        final String providedPin = request.getHeader(HEADER_MINI_PIN);
        if (!isPinMatched(providedPin)) {
            writeUnauthorizedResponse(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPinMatched(final String providedPin) {
        if (configuredPin.isBlank() || providedPin == null || providedPin.isBlank()) {
            return false;
        }
        final byte[] expectedBytes = configuredPin.getBytes(StandardCharsets.UTF_8);
        final byte[] actualBytes = providedPin.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private String extractNormalizedPath(final HttpServletRequest request) {
        final String contextPath = request.getContextPath();
        final String requestUri = request.getRequestURI();
        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private void writeUnauthorizedResponse(final HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getWriter().write(UNAUTHORIZED_BODY);
    }
}
