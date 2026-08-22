package com.myapps.web.myrpg.application.exception;

/**
 * 아직 구현되지 않은 던전 진입을 시도할 때 발생하는 예외.
 *
 * <p>{@code implemented: false} 상태인 던전 진입 시 발생합니다.
 */
public class DungeonNotImplementedException extends RuntimeException {

    /**
     * 상세 메시지를 지정하여 예외를 생성합니다.
     *
     * @param message 오류 메시지
     */
    public DungeonNotImplementedException(final String message) {
        super(message);
    }

    /**
     * 상세 메시지와 원인 예외를 지정하여 예외를 생성합니다.
     *
     * @param message 오류 메시지
     * @param cause 원인 예외
     */
    public DungeonNotImplementedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
