package com.myapps.web.myrpg.domain.exception;

/**
 * 장비 관련 잘못된 조작 시 발생하는 예외.
 *
 * <p>착용 중인 장비를 판매하려 하거나, 판매 불가 아이템(스킬북/포션)을
 * 판매하려 할 때 이 예외를 던진다.
 */
public class IllegalEquipmentException extends RuntimeException {

    /**
     * 예외를 생성한다.
     *
     * @param message 오류 메시지
     */
    public IllegalEquipmentException(final String message) {
        super(message);
    }
}
