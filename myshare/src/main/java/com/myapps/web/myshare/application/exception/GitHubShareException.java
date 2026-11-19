package com.myapps.web.myshare.application.exception;

/** 외부 GitHub Contents API 호출 또는 응답 파싱 중 오류가 발생했을 때 던지는 예외. */
public class GitHubShareException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 예외 상세 메시지
     */
    public GitHubShareException(final String message) {
        super(message);
    }

    /**
     * 오류 메시지와 원인 예외를 포함하여 예외를 생성합니다.
     *
     * @param message 예외 상세 메시지
     * @param cause 원인 예외
     */
    public GitHubShareException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
