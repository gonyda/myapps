package com.myapps.web.myrpg.domain.exception;

/**
 * 포션 구매 시 골드가 부족할 때 발생하는 예외.
 *
 * <p>플레이어의 보유 골드가 구매가 미만일 경우 이 예외를 던진다.
 */
public class InsufficientGoldException extends RuntimeException {

    /**
     * 예외를 생성한다.
     *
     * @param message 오류 메시지
     */
    public InsufficientGoldException(final String message) {
        super(message);
    }
}
