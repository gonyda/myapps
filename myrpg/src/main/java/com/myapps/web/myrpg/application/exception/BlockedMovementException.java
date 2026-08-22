package com.myapps.web.myrpg.application.exception;

/**
 * 던전 내 미클리어 방에서 전진을 시도하거나 유효하지 않은 이동을 시도할 때 발생하는 비즈니스 예외.
 *
 * <p>미클리어 방에서는 이전 클리어 방으로의 후퇴만 허용되며, 새로운 미클리어 방으로의 전진은 차단됩니다.
 */
public class BlockedMovementException extends RuntimeException {

    /**
     * 상세 메시지를 지정하여 예외를 생성합니다.
     *
     * @param message 오류 메시지
     */
    public BlockedMovementException(final String message) {
        super(message);
    }

    /**
     * 상세 메시지와 원인 예외를 지정하여 예외를 생성합니다.
     *
     * @param message 오류 메시지
     * @param cause 원인 예외
     */
    public BlockedMovementException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
