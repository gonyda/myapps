package com.myapps.web.myrpg.application.exception;

/**
 * 저장소 용량(30) 초과로 아이템 이동 또는 획득이 거부될 때 발생하는 비즈니스 예외.
 *
 * <p>인벤토리 또는 은행에 신규 스택이 추가되어 항목 수 30을 초과하면 이 예외를 던진다. {@code GlobalExceptionHandler}가 포착하여 사용자에게 용량
 * 초과 안내를 반환한다.
 *
 * <p>이 예외 발생 시 저장소 상태는 일절 변경되지 않는다.
 */
public class InventoryFullException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성한다.
     *
     * @param message 용량 초과 상세 메시지
     */
    public InventoryFullException(final String message) {
        super(message);
    }
}
