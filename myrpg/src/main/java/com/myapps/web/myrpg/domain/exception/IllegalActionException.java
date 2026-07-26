package com.myapps.web.myrpg.domain.exception;

/**
 * 잘못된 게임 행동 시 발생하는 예외.
 *
 * <p>전투 중 포기 시도, 던전 진행 중 장비 변경 등 허용되지 않는
 * 행동을 수행하려 할 때 이 예외를 던진다.
 */
public class IllegalActionException extends RuntimeException {

    /**
     * 예외를 생성한다.
     *
     * @param message 오류 메시지
     */
    public IllegalActionException(final String message) {
        super(message);
    }
}
