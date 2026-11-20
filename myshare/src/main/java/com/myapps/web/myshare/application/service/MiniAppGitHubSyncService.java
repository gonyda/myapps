package com.myapps.web.myshare.application.service;

import com.myapps.web.myshare.application.port.GitHubMiniAppSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 저장된 미니앱 JSON 데이터를 백그라운드({@code @Async})로 GitHub 저장소에 비동기 동기화하는 서비스.
 *
 * <p>외부 GitHub API 호출 지연이나 실패가 DB 저장 트랜잭션 또는 HTTP 응답에 영향을 주지 않도록 예외를 내부에서 로깅하고 삼킵니다.
 */
@Service
public class MiniAppGitHubSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(MiniAppGitHubSyncService.class);

    private final GitHubMiniAppSyncClient gitHubMiniAppSyncClient;

    /**
     * MiniAppGitHubSyncService를 생성합니다.
     *
     * @param gitHubMiniAppSyncClient GitHub JSON 푸시 클라이언트
     */
    public MiniAppGitHubSyncService(final GitHubMiniAppSyncClient gitHubMiniAppSyncClient) {
        this.gitHubMiniAppSyncClient = gitHubMiniAppSyncClient;
    }

    /**
     * 단일 스레드 비동기 실행기({@code miniAppGitHubSyncExecutor})를 통해 {@code share/{appKey}/data.json}을
     * GitHub에 푸시합니다.
     *
     * @param appKey 검증된 미니앱 키
     * @param jsonContent 저장된 UTF-8 JSON 문자열
     */
    @Async("miniAppGitHubSyncExecutor")
    public void syncAsync(final String appKey, final String jsonContent) {
        try {
            final boolean pushed = gitHubMiniAppSyncClient.pushJson(appKey, jsonContent);
            if (pushed) {
                LOG.info("GitHub 미니앱 동기화 완료: appKey={}", appKey);
            }
        } catch (final Exception ex) {
            LOG.error(
                    "GitHub 미니앱 비동기 동기화 실패 (DB 저장은 정상 유지됨): appKey={}, error={}",
                    appKey,
                    ex.getMessage(),
                    ex);
        }
    }

    /**
     * 단일 스레드 비동기 실행기({@code miniAppGitHubSyncExecutor})를 통해 {@code share/{appKey}/data.json} 파일만
     * GitHub에서 삭제합니다.
     *
     * @param appKey 검증된 미니앱 키
     */
    @Async("miniAppGitHubSyncExecutor")
    public void deleteAsync(final String appKey) {
        try {
            final boolean deleted = gitHubMiniAppSyncClient.deleteJson(appKey);
            if (deleted) {
                LOG.info("GitHub 미니앱 파일 삭제 완료: appKey={}", appKey);
            }
        } catch (final Exception ex) {
            LOG.error(
                    "GitHub 미니앱 비동기 삭제 실패 (DB 삭제는 정상 유지됨): appKey={}, error={}",
                    appKey,
                    ex.getMessage(),
                    ex);
        }
    }
}
