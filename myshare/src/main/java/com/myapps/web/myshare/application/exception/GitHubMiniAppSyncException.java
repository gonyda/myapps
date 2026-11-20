package com.myapps.web.myshare.application.exception;

/** GitHub Contents API를 통해 미니앱 JSON 백업 파일을 커밋·푸시할 때 오류가 발생하면 던지는 예외. */
public class GitHubMiniAppSyncException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 예외 상세 메시지
     */
    public GitHubMiniAppSyncException(final String message) {
        super(message);
    }

    /**
     * 오류 메시지와 원인 예외를 포함하여 예외를 생성합니다.
     *
     * @param message 예외 상세 메시지
     * @param cause 원인 예외
     */
    public GitHubMiniAppSyncException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
