package com.myapps.web.myrpg.application.exception;

/**
 * 기본 캐릭터 저장 실패 시 발생하는 예외.
 *
 * <p>트랜잭션 롤백 후 오류를 반환하며, error 뷰로 매핑됩니다.
 */
public class CharacterCreationException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 캐릭터 생성 실패 상세 메시지
     */
    public CharacterCreationException(final String message) {
        super(message);
    }

    /**
     * 오류 메시지와 원인 예외를 포함하여 예외를 생성합니다.
     *
     * @param message 캐릭터 생성 실패 상세 메시지
     * @param cause   원인 예외
     */
    public CharacterCreationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
