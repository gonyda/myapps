package com.myapps.web.myshare.interfaces.api;

import com.myapps.web.myshare.application.exception.ShareNotFoundException;
import com.myapps.web.myshare.application.service.SharePageService;
import com.myapps.web.myshare.domain.exception.InvalidShareSlugException;
import com.myapps.web.myshare.domain.model.SharePage;
import java.nio.charset.StandardCharsets;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mike가 생성한 단일 HTML 페이지를 {@code /share/{slug}} 경로로 서빙하는 REST 컨트롤러.
 *
 * <p>별도 로그인 없이 모바일 브라우저(iOS Safari 등)에서 인라인 스타일·스크립트가 정상 실행되도록 {@code text/html; charset=utf-8} 응답을
 * 반환하며, 목록 인덱스 노출 및 경로 탐색 요청을 차단합니다.
 */
@RestController
public class ShareController {

    private static final MediaType TEXT_HTML_UTF8 =
            new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);
    private static final String HEADER_X_ROBOTS_TAG = "X-Robots-Tag";
    private static final String HEADER_X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String HEADER_REFERRER_POLICY = "Referrer-Policy";
    private static final String NOINDEX_NOFOLLOW = "noindex, nofollow";
    private static final String NOSNIFF = "nosniff";
    private static final String NO_REFERRER = "no-referrer";

    private final SharePageService sharePageService;

    /**
     * ShareController를 생성합니다.
     *
     * @param sharePageService 공유 HTML 조회 서비스
     */
    public ShareController(final SharePageService sharePageService) {
        this.sharePageService = sharePageService;
    }

    /**
     * 비공개 링크 보호를 위해 {@code /share} 및 {@code /share/} 목록 조회를 차단합니다.
     *
     * @return 항상 {@link ShareNotFoundException} 발생 (404 Not Found)
     */
    @GetMapping({"/share", "/share/"})
    public ResponseEntity<String> rejectShareIndex() {
        throw new ShareNotFoundException("공유 목록은 제공하지 않습니다.");
    }

    /**
     * 하위 슬래시가 포함된 경로 탐색 요청({@code /share/{slug}/...})을 400 Bad Request로 차단합니다.
     *
     * @param slug 첫 번째 경로 세그먼트
     * @return 항상 {@link InvalidShareSlugException} 발생 (400 Bad Request)
     */
    @GetMapping("/share/{slug}/**")
    public ResponseEntity<String> rejectNestedSharePath(@PathVariable final String slug) {
        throw new InvalidShareSlugException("허용되지 않은 공유 경로 형식입니다.");
    }

    /**
     * 지정된 슬러그의 단일 HTML 문서를 조회해 웹 브라우저로 서빙합니다.
     *
     * @param slug 공유 파일 슬러그 ({@code ^[a-zA-Z0-9_-]{1,200}$})
     * @param ifNoneMatch 클라이언트가 보낸 조건부 ETag 헤더
     * @return {@code 200 OK} HTML 응답 또는 ETag 일치 시 {@code 304 Not Modified}
     */
    @GetMapping("/share/{slug}")
    public ResponseEntity<String> getSharedHtml(
            @PathVariable final String slug,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false)
                    final String ifNoneMatch) {
        final SharePage page = sharePageService.loadPage(slug);
        final HttpHeaders headers = buildCommonHeaders(page.sha());

        if (!page.sha().isBlank() && isEtagMatched(ifNoneMatch, page.sha())) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).headers(headers).build();
        }

        headers.setContentType(TEXT_HTML_UTF8);
        return ResponseEntity.ok().headers(headers).body(page.html());
    }

    /**
     * 애플리케이션 기동 상태 확인용 경량 헬스 엔드포인트.
     *
     * @return {@code 200 OK} 텍스트 응답
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    private HttpHeaders buildCommonHeaders(final String sha) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().cachePrivate());
        headers.set(HEADER_X_ROBOTS_TAG, NOINDEX_NOFOLLOW);
        headers.set(HEADER_X_CONTENT_TYPE_OPTIONS, NOSNIFF);
        headers.set(HEADER_REFERRER_POLICY, NO_REFERRER);
        if (!sha.isBlank()) {
            headers.setETag("\"" + sha + "\"");
        }
        return headers;
    }

    private boolean isEtagMatched(final String ifNoneMatchHeader, final String currentSha) {
        if (ifNoneMatchHeader == null || ifNoneMatchHeader.isBlank()) {
            return false;
        }
        final String[] candidates = ifNoneMatchHeader.split(",", -1);
        for (final String candidate : candidates) {
            final String normalized = stripEtagDecorations(candidate);
            if ("*".equals(normalized) || currentSha.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String stripEtagDecorations(final String rawTag) {
        String value = rawTag.trim();
        if (value.startsWith("W/")) {
            value = value.substring(2).trim();
        }
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }
}
