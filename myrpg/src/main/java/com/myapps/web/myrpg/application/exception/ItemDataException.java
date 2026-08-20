package com.myapps.web.myrpg.application.exception;

/**
 * 아이템 카탈로그 로드 또는 검증 실패 시 발생하는 예외.
 *
 * <p>고정 데이터 무결성 위반으로 판단하며, 이 예외 발생 시 애플리케이션 기동이 실패합니다.
 */
public class ItemDataException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 아이템 데이터 오류 상세 메시지
     */
    public ItemDataException(final String message) {
        super(message);
    }

    /**
     * 오류 메시지와 원인 예외를 포함하여 예외를 생성합니다.
     *
     * @param message 아이템 데이터 오류 상세 메시지
     * @param cause 원인 예외
     */
    public ItemDataException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
