package com.myapps.web.myshare.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code myshare.github} 설정 프로퍼티를 바인딩하는 불변 레코드.
 *
 * <p>공유 HTML 파일을 읽어올 GitHub 저장소 경로 및 읽기 전용 인증 토큰 설정을 보관합니다.
 *
 * @param enabled GitHub 조회 활성화 여부
 * @param token GitHub Fine-grained PAT (Contents: Read-only 권한)
 * @param owner 저장소 소유자
 * @param repo 저장소 이름
 * @param branch 대상 브랜치
 * @param basePath 공유 HTML이 저장된 디렉터리 경로
 */
@ConfigurationProperties(prefix = "myshare.github")
public record GitHubShareProperties(
        boolean enabled, String token, String owner, String repo, String branch, String basePath) {

    private static final String DEFAULT_OWNER = "gonyda";
    private static final String DEFAULT_REPO = "mike";
    private static final String DEFAULT_BRANCH = "main";
    private static final String DEFAULT_BASE_PATH = "share";

    /** 누락되거나 공백인 필드에 기본값을 보완합니다. */
    public GitHubShareProperties {
        token = token != null ? token : "";
        owner = defaultIfBlank(owner, DEFAULT_OWNER);
        repo = defaultIfBlank(repo, DEFAULT_REPO);
        branch = defaultIfBlank(branch, DEFAULT_BRANCH);
        basePath = defaultIfBlank(basePath, DEFAULT_BASE_PATH);
    }

    /**
     * GitHub 조회 기능이 활성화되어 있고 유효한 토큰이 설정되었는지 여부를 반환합니다.
     *
     * @return 조회 가능하면 {@code true}
     */
    public boolean isActive() {
        return enabled && !token.isBlank();
    }

    /**
     * 토큰 유출을 방지하기 위해 민감 필드를 마스킹한 문자열 표현을 반환합니다.
     *
     * @return 마스킹된 프로퍼티 요약 문자열
     */
    @Override
    public String toString() {
        return "GitHubShareProperties["
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
                + ", basePath="
                + basePath
                + "]";
    }

    private static String defaultIfBlank(final String value, final String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
