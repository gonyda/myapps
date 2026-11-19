package com.myapps.web.myshare.application.exception;

/** 요청한 슬러그에 해당하는 HTML 파일이 저장소에 존재하지 않을 때 발생하는 예외. */
public class ShareNotFoundException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 예외 상세 메시지
     */
    public ShareNotFoundException(final String message) {
        super(message);
    }
}
