package com.myapps.web.myrpg.application.exception;

/**
 * 존재하지 않는 노드 ID를 조회할 때 발생하는 예외.
 *
 * <p>{@code @ControllerAdvice}에서 HTTP 404 응답으로 매핑됩니다.
 */
public class NodeNotFoundException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 노드 조회 실패 상세 메시지
     */
    public NodeNotFoundException(final String message) {
        super(message);
    }

    /**
     * 오류 메시지와 원인 예외를 포함하여 예외를 생성합니다.
     *
     * @param message 노드 조회 실패 상세 메시지
     * @param cause 원인 예외
     */
    public NodeNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
