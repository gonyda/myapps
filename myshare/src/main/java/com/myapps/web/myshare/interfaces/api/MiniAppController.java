package com.myapps.web.myshare.interfaces.api;

import com.myapps.web.myshare.application.service.MiniAppGitHubSyncService;
import com.myapps.web.myshare.application.service.MiniAppStoreService;
import com.myapps.web.myshare.domain.exception.InvalidMiniAppKeyException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 정적 HTML 미니앱들이 공용 JSON 상태를 저장·조회할 수 있도록 {@code /api/mini/{appKey}} 엔드포인트를 제공하는 REST 컨트롤러.
 *
 * <p>저장 성공 시 DB 트랜잭션 완료 후 {@code @Async} 백그라운드로 {@code gonyda/mike} 저장소 동기화를 트리거합니다.
 */
@RestController
@RequestMapping("/api/mini")
public class MiniAppController {

    private static final String HEADER_X_ROBOTS_TAG = "X-Robots-Tag";
    private static final String HEADER_X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String HEADER_REFERRER_POLICY = "Referrer-Policy";
    private static final String NOINDEX_NOFOLLOW = "noindex, nofollow";
    private static final String NOSNIFF = "nosniff";
    private static final String NO_REFERRER = "no-referrer";

    private final MiniAppStoreService miniAppStoreService;
    private final MiniAppGitHubSyncService miniAppGitHubSyncService;

    /**
     * MiniAppController를 생성합니다.
     *
     * @param miniAppStoreService 미니앱 DB 저장소 서비스
     * @param miniAppGitHubSyncService 미니앱 GitHub 비동기 동기화 서비스
     */
    public MiniAppController(
            final MiniAppStoreService miniAppStoreService,
            final MiniAppGitHubSyncService miniAppGitHubSyncService) {
        this.miniAppStoreService = miniAppStoreService;
        this.miniAppGitHubSyncService = miniAppGitHubSyncService;
    }

    /**
     * 루트 경로({@code /api/mini}, {@code /api/mini/}) 요청을 400 Bad Request로 차단합니다.
     *
     * @return 항상 {@link InvalidMiniAppKeyException} 발생
     */
    @RequestMapping({"", "/"})
    public ResponseEntity<String> rejectRootPath() {
        throw new InvalidMiniAppKeyException("미니앱 키가 필요합니다.");
    }

    /**
     * 하위 경로 탐색 요청({@code /api/mini/{appKey}/...})을 400 Bad Request로 차단합니다.
     *
     * @param appKey 첫 번째 경로 세그먼트
     * @return 항상 {@link InvalidMiniAppKeyException} 발생
     */
    @RequestMapping("/{appKey}/**")
    public ResponseEntity<String> rejectNestedPath(@PathVariable final String appKey) {
        throw new InvalidMiniAppKeyException("허용되지 않은 미니앱 경로 형식입니다.");
    }

    /**
     * 지정된 {@code appKey}에 저장된 JSON 본문을 조회합니다.
     *
     * <p>데이터가 아직 존재하지 않으면 {@code 200 OK}와 함께 빈 JSON 객체({@code {}})를 반환합니다.
     *
     * @param appKey 미니앱 식별자 ({@code ^[a-z0-9_-]{1,64}$})
     * @return {@code 200 OK} {@code application/json} 응답
     */
    @GetMapping("/{appKey}")
    public ResponseEntity<String> getAppData(@PathVariable final String appKey) {
        final String json = miniAppStoreService.loadJson(appKey);
        return buildJsonResponse(json);
    }

    /**
     * 원시 JSON 요청 바디를 검증한 뒤 {@code MINI_APP_STORE}에 업서트하고 비동기 GitHub 동기화를 예약합니다.
     *
     * @param appKey 미니앱 식별자 ({@code ^[a-z0-9_-]{1,64}$})
     * @param rawBody 자유 형식의 원시 JSON 문자열
     * @return 저장된 JSON을 담은 {@code 200 OK} {@code application/json} 응답
     */
    @PutMapping("/{appKey}")
    public ResponseEntity<String> putAppData(
            @PathVariable final String appKey,
            @RequestBody(required = false) final String rawBody) {
        final String savedJson = miniAppStoreService.upsertJson(appKey, rawBody);
        miniAppGitHubSyncService.syncAsync(appKey, savedJson);
        return buildJsonResponse(savedJson);
    }

    private ResponseEntity<String> buildJsonResponse(final String jsonBody) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setCacheControl(CacheControl.noStore());
        headers.set(HEADER_X_ROBOTS_TAG, NOINDEX_NOFOLLOW);
        headers.set(HEADER_X_CONTENT_TYPE_OPTIONS, NOSNIFF);
        headers.set(HEADER_REFERRER_POLICY, NO_REFERRER);
        return ResponseEntity.ok().headers(headers).body(jsonBody);
    }
}
