package com.myapps.web.myrpg.domain.exception;

/**
 * 스킬 사용 시 MP가 부족할 때 발생하는 예외.
 *
 * <p>플레이어의 현재 MP가 스킬 MP 비용보다 적을 경우 이 예외를 던진다.
 */
public class InsufficientMpException extends RuntimeException {

    /**
     * 예외를 생성한다.
     *
     * @param message 오류 메시지
     */
    public InsufficientMpException(final String message) {
        super(message);
    }
}
