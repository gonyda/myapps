package com.myapps.web.myrpg.application.exception;

/**
 * 소지금 또는 은행 잔액 부족으로 골드 소모·입금·출금이 거부될 때 발생하는 비즈니스 예외.
 *
 * <p>골드 관련 서비스가 사전 검증에서 보유 골드가 요청 금액 미만임을 감지하면 이 예외를 던진다. {@code GlobalExceptionHandler}가 포착하여
 * 사용자에게 부족 안내를 반환한다.
 *
 * <p>캐릭터 소지금 및 은행 보관 골드는 이 예외 발생 시 일절 변경되지 않는다.
 */
public class InsufficientGoldException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성한다.
     *
     * @param message 골드 부족 상세 메시지 (요청량·보유량 포함 권장)
     */
    public InsufficientGoldException(final String message) {
        super(message);
    }
}
