package com.myapps.web.myshare.application.exception;

/** GitHub 공유 연동이 비활성화되었거나 필수 토큰 설정이 누락되었을 때 발생하는 예외. */
public class ShareConfigurationException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 예외 상세 메시지
     */
    public ShareConfigurationException(final String message) {
        super(message);
    }
}
