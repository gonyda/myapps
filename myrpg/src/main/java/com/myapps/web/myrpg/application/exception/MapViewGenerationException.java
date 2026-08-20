package com.myapps.web.myrpg.application.exception;

/**
 * 맵 뷰(미니맵/전체지도) 생성 실패 시 발생하는 예외.
 *
 * <p>현재 노드의 좌표가 부재하거나 그래프가 미확인 상태일 때 발생하며, {@code @ControllerAdvice}에서 HTTP 500 응답으로 매핑됩니다.
 */
public class MapViewGenerationException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 뷰 생성 실패 상세 메시지
     */
    public MapViewGenerationException(final String message) {
        super(message);
    }

    /**
     * 오류 메시지와 원인 예외를 포함하여 예외를 생성합니다.
     *
     * @param message 뷰 생성 실패 상세 메시지
     * @param cause 원인 예외
     */
    public MapViewGenerationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
