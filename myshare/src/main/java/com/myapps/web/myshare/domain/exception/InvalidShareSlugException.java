package com.myapps.web.myshare.domain.exception;

/** 허용되지 않은 형식의 공유 슬러그 또는 경로 탐색(Path Traversal) 시도가 감지되었을 때 발생하는 예외. */
public class InvalidShareSlugException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 예외 상세 메시지
     */
    public InvalidShareSlugException(final String message) {
        super(message);
    }
}
