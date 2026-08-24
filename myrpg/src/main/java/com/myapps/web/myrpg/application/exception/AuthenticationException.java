package com.myapps.web.myrpg.application.exception;

/**
 * 로그인 인증 실패 시 발생하는 도메인 예외.
 *
 * <p>아이디 불일치, 비밀번호 불일치 등의 인증 오류 상황에 발생합니다.
 */
public class AuthenticationException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 인증 실패 상세 메시지
     */
    public AuthenticationException(final String message) {
        super(message);
    }

    /**
     * 오류 메시지와 원인 예외를 포함하여 예외를 생성합니다.
     *
     * @param message 인증 실패 상세 메시지
     * @param cause 원인 예외
     */
    public AuthenticationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
