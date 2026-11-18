package com.myapps.web.mycalendar.application.dto;

/** GitHub 스냅샷 동기화 실행 결과를 표현하는 응답 DTO. */
public record GitHubSyncResult(
        GitHubSyncStatus status, int totalCount, String syncedAt, String message) {

    /**
     * 동기화 성공 결과를 생성합니다.
     *
     * @param totalCount 동기화된 전체 일정 건수
     * @param syncedAt 동기화 시각(KST ISO-8601)
     * @return 성공 상태의 동기화 결과
     */
    public static GitHubSyncResult success(final int totalCount, final String syncedAt) {
        return new GitHubSyncResult(
                GitHubSyncStatus.SUCCESS, totalCount, syncedAt, "GitHub calendar sync completed");
    }

    /**
     * 동기화가 비활성화되었거나 토큰이 누락되어 스킵된 결과를 생성합니다.
     *
     * @param reason 스킵 사유
     * @return 스킵 상태의 동기화 결과
     */
    public static GitHubSyncResult skipped(final String reason) {
        return new GitHubSyncResult(GitHubSyncStatus.SKIPPED, 0, null, reason);
    }

    /**
     * 동기화 실패 결과를 생성합니다.
     *
     * @param reason 실패 상세 메시지
     * @return 실패 상태의 동기화 결과
     */
    public static GitHubSyncResult failed(final String reason) {
        return new GitHubSyncResult(GitHubSyncStatus.FAILED, 0, null, reason);
    }
}
