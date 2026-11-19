package com.myapps.web.myshare.interfaces.api;

import com.myapps.web.myshare.application.exception.GitHubShareException;
import com.myapps.web.myshare.application.exception.ShareConfigurationException;
import com.myapps.web.myshare.application.exception.ShareNotFoundException;
import com.myapps.web.myshare.domain.exception.InvalidShareSlugException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** {@code myshare} 웹 계층에서 발생하는 예외를 안전한 모바일 친화적 HTTP HTML 응답으로 변환하는 전역 예외 처리기. */
@RestControllerAdvice
public class ShareExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ShareExceptionHandler.class);
    private static final MediaType TEXT_HTML_UTF8 =
            new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

    /**
     * 잘못된 슬러그 또는 경로 탐색 요청을 400 Bad Request로 처리합니다.
     *
     * @param ex 슬러그 검증 예외
     * @return 400 응답
     */
    @ExceptionHandler(InvalidShareSlugException.class)
    public ResponseEntity<String> handleInvalidSlug(final InvalidShareSlugException ex) {
        return errorHtml(HttpStatus.BAD_REQUEST, "400 Bad Request", "잘못된 공유 링크 형식입니다.");
    }

    /**
     * 존재하지 않는 공유 페이지 또는 비공개 인덱스 접근을 404 Not Found로 처리합니다.
     *
     * @param ex 미존재 예외
     * @return 404 응답
     */
    @ExceptionHandler({ShareNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<String> handleNotFound(final Exception ex) {
        return errorHtml(HttpStatus.NOT_FOUND, "404 Not Found", "요청한 페이지를 찾을 수 없습니다.");
    }

    /**
     * GitHub API 통신 실패를 502 Bad Gateway로 처리합니다.
     *
     * @param ex GitHub 연동 예외
     * @return 502 응답
     */
    @ExceptionHandler(GitHubShareException.class)
    public ResponseEntity<String> handleGitHubFailure(final GitHubShareException ex) {
        LOG.warn("GitHub 공유 페이지 조회 실패: {}", ex.getMessage());
        return errorHtml(
                HttpStatus.BAD_GATEWAY, "502 Bad Gateway", "공유 페이지를 불러오는 중 일시적인 오류가 발생했습니다.");
    }

    /**
     * 토큰 미설정 등 구성 오류를 503 Service Unavailable로 처리합니다.
     *
     * @param ex 설정 예외
     * @return 503 응답
     */
    @ExceptionHandler(ShareConfigurationException.class)
    public ResponseEntity<String> handleConfigurationError(final ShareConfigurationException ex) {
        LOG.error("GitHub 공유 연동 설정 오류: {}", ex.getMessage());
        return errorHtml(
                HttpStatus.SERVICE_UNAVAILABLE,
                "503 Service Unavailable",
                "공유 서비스가 아직 구성되지 않았습니다.");
    }

    private ResponseEntity<String> errorHtml(
            final HttpStatus status, final String title, final String description) {
        final String body =
                "<!DOCTYPE html><html lang=\"ko\"><head><meta charset=\"UTF-8\">"
                        + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                        + "<title>"
                        + title
                        + "</title></head><body><h1>"
                        + title
                        + "</h1><p>"
                        + description
                        + "</p></body></html>";
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(TEXT_HTML_UTF8);
        headers.setCacheControl(CacheControl.noStore());
        headers.set("X-Robots-Tag", "noindex, nofollow");
        return ResponseEntity.status(status).headers(headers).body(body);
    }
}
