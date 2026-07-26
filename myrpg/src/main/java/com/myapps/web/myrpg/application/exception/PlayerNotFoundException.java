package com.myapps.web.myrpg.application.exception;

/**
 * 존재하지 않는 플레이어에 접근할 때 발생하는 예외.
 *
 * <p>주어진 식별자로 플레이어를 조회할 수 없는 경우 이 예외를 던진다.
 */
public class PlayerNotFoundException extends RuntimeException {

    /**
     * 예외를 생성한다.
     *
     * @param message 오류 메시지
     */
    public PlayerNotFoundException(final String message) {
        super(message);
    }
}
