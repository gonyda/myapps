package com.myapps.web.myrpg.application.exception;

/**
 * AP(어빌리티 포인트) 부족으로 스킬 랭크업이 거부될 때 발생하는 비즈니스 예외.
 *
 * <p>스킬 서비스가 랭크업 사전 검증에서 보유 AP가 소모 비용 미만임을 감지하면
 * 이 예외를 던진다. {@code GlobalExceptionHandler}가 포착하여 사용자에게
 * 승급 불가 안내를 반환한다.
 *
 * <p>캐릭터 상태(랭크·카운트·AP)는 이 예외 발생 시 일절 변경되지 않는다.
 */
public class InsufficientAbilityPointsException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성한다.
     *
     * @param message AP 부족 상세 메시지 (필요량·보유량 포함 권장)
     */
    public InsufficientAbilityPointsException(final String message) {
        super(message);
    }
}
