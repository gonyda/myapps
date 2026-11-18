package com.myapps.web.mycalendar.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code mycalendar.github-sync} 설정 프로퍼티를 바인딩하는 불변 레코드.
 *
 * <p>외부 GitHub 레포지토리에 일정 스냅샷을 푸시하기 위한 대상 저장소 및 인증 토큰 설정을 보관합니다.
 */
@ConfigurationProperties(prefix = "mycalendar.github-sync")
public record GitHubSyncProperties(
        boolean enabled,
        String token,
        String owner,
        String repo,
        String branch,
        String path,
        String commitMessage) {

    private static final String DEFAULT_OWNER = "gonyda";
    private static final String DEFAULT_REPO = "mike";
    private static final String DEFAULT_BRANCH = "main";
    private static final String DEFAULT_PATH = "calendar/mycalendar.json";
    private static final String DEFAULT_COMMIT_MESSAGE = "chore(calendar): sync mycalendar events";

    /** 누락된 필드에 기본값을 보완하여 프로퍼티 레코드를 초기화합니다. */
    public GitHubSyncProperties {
        token = token != null ? token : "";
        owner = defaultIfBlank(owner, DEFAULT_OWNER);
        repo = defaultIfBlank(repo, DEFAULT_REPO);
        branch = defaultIfBlank(branch, DEFAULT_BRANCH);
        path = defaultIfBlank(path, DEFAULT_PATH);
        commitMessage = defaultIfBlank(commitMessage, DEFAULT_COMMIT_MESSAGE);
    }

    /**
     * 동기화가 활성화되어 있고 유효한(공백이 아닌) 토큰이 설정되었는지 여부를 반환합니다.
     *
     * @return 동기화 실행 가능하면 {@code true}
     */
    public boolean isActive() {
        return enabled && !token.isBlank();
    }

    /**
     * 토큰 노출을 방지하기 위해 민감 필드를 마스킹한 문자열 표현을 반환합니다.
     *
     * @return 마스킹된 프로퍼티 요약 문자열
     */
    @Override
    public String toString() {
        return "GitHubSyncProperties["
                + "enabled="
                + enabled
                + ", tokenConfigured="
                + !token.isBlank()
                + ", owner="
                + owner
                + ", repo="
                + repo
                + ", branch="
                + branch
                + ", path="
                + path
                + ", commitMessage="
                + commitMessage
                + "]";
    }

    private static String defaultIfBlank(final String value, final String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
