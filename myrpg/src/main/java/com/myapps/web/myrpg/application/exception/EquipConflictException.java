package com.myapps.web.myrpg.application.exception;

/**
 * 장비 착용 슬롯 충돌로 착용이 거부될 때 발생하는 비즈니스 예외.
 *
 * <p>착용하려는 장비의 필요 슬롯을 primary 슬롯이 다른 장비가 점유하여
 * 착용이 불가능한 상태에서 이 예외를 던진다.
 * {@code GlobalExceptionHandler}가 포착하여 사용자에게 안내를 반환한다.
 *
 * <p>이 예외 발생 시 장착 상태는 일절 변경되지 않는다.
 */
public class EquipConflictException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성한다.
     *
     * @param message 착용 충돌 상세 메시지
     */
    public EquipConflictException(final String message) {
        super(message);
    }
}
