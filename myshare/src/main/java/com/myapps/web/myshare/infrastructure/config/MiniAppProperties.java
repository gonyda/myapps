package com.myapps.web.myshare.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 미니앱 저장소({@code /api/mini/**}) 인증 PIN, 페이로드 크기 제한 및 GitHub 비동기 동기화 설정을 바인딩하는 프로퍼티.
 *
 * @param pin {@code X-Mini-Pin} 헤더 검증용 공용 PIN
 * @param maxPayloadBytes 허용할 최대 JSON 요청 크기(바이트)
 * @param github {@code gonyda/mike} GitHub 비동기 동기화 세부 설정
 */
@ConfigurationProperties(prefix = "myshare.mini-app")
public record MiniAppProperties(String pin, int maxPayloadBytes, GitHub github) {

    private static final int DEFAULT_MAX_PAYLOAD_BYTES = 1_048_576;
    private static final String DEFAULT_GITHUB_BASE_PATH = "share";
    private static final String DEFAULT_COMMIT_PREFIX = "chore(mini): sync";

    /** 누락되거나 비어 있는 설정 필드에 안전한 기본값을 적용합니다. */
    public MiniAppProperties {
        pin = pin == null ? "" : pin.trim();
        maxPayloadBytes = maxPayloadBytes > 0 ? maxPayloadBytes : DEFAULT_MAX_PAYLOAD_BYTES;
        github = github == null ? new GitHub(true, null, null) : github;
    }

    /**
     * 미니앱 GitHub 동기화 기능이 활성화되어 있는지 반환합니다.
     *
     * @return 활성화 여부
     */
    public boolean isGitHubSyncEnabled() {
        return github.enabled();
    }

    /**
     * PIN 값이 설정되어 있는지 반환합니다.
     *
     * @return PIN이 비어 있지 않으면 {@code true}
     */
    public boolean isPinConfigured() {
        return !pin.isBlank();
    }

    /**
     * 로그 노출 시 PIN 원문을 마스킹한 요약 문자열을 반환합니다.
     *
     * @return PIN이 마스킹된 설정 요약 문자열
     */
    @Override
    public String toString() {
        return "MiniAppProperties[pinConfigured="
                + isPinConfigured()
                + ", maxPayloadBytes="
                + maxPayloadBytes
                + ", github="
                + github
                + "]";
    }

    /**
     * 미니앱 JSON 백업 파일의 GitHub 업로드 경로 및 커밋 메시지 설정.
     *
     * @param enabled 동기화 활성화 여부
     * @param basePath 저장소 내 기본 디렉터리 (기본값: {@code share})
     * @param commitMessagePrefix 커밋 메시지 접두사 (기본값: {@code chore(mini): sync})
     */
    public record GitHub(boolean enabled, String basePath, String commitMessagePrefix) {

        /** 누락된 하위 GitHub 설정 필드를 기본값으로 보완합니다. */
        public GitHub {
            basePath = isBlank(basePath) ? DEFAULT_GITHUB_BASE_PATH : basePath.trim();
            commitMessagePrefix =
                    isBlank(commitMessagePrefix)
                            ? DEFAULT_COMMIT_PREFIX
                            : commitMessagePrefix.trim();
        }

        private static boolean isBlank(final String value) {
            return value == null || value.isBlank();
        }
    }
}
