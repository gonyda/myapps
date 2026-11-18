package com.myapps.web.mycalendar.application.exception;

/**
 * GitHub Contents API 호출 또는 스냅샷 직렬화 중 오류가 발생했을 때 던지는 예외.
 *
 * <p>비동기 동기화 파이프라인 내부에서 포착되어 로깅되며, 일정 CRUD 트랜잭션으로는 전파되지 않습니다.
 */
public class GitHubSyncException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 오류 상세 메시지
     */
    public GitHubSyncException(final String message) {
        super(message);
    }

    /**
     * 오류 메시지와 원인 예외를 포함하여 예외를 생성합니다.
     *
     * @param message 오류 상세 메시지
     * @param cause 원인 예외
     */
    public GitHubSyncException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
