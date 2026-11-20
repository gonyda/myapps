package com.myapps.web.myshare.domain.exception;

/** 허용되지 않은 형식의 미니앱 키({@code appKey})가 전달되었을 때 발생하는 예외. */
public class InvalidMiniAppKeyException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 예외 상세 메시지
     */
    public InvalidMiniAppKeyException(final String message) {
        super(message);
    }
}
