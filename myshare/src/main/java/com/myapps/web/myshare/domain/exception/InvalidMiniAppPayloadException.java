package com.myapps.web.myshare.domain.exception;

/** 미니앱 저장소 JSON 요청 본문이 비어 있거나, 크기 제한을 초과했거나, JSON 문법이 올바르지 않을 때 발생하는 예외. */
public class InvalidMiniAppPayloadException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 예외 상세 메시지
     */
    public InvalidMiniAppPayloadException(final String message) {
        super(message);
    }

    /**
     * 오류 메시지와 원인 예외를 포함하여 예외를 생성합니다.
     *
     * @param message 예외 상세 메시지
     * @param cause 원인 예외
     */
    public InvalidMiniAppPayloadException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
