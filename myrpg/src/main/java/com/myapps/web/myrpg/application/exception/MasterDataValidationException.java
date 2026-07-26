package com.myapps.web.myrpg.application.exception;

/**
 * 마스터 데이터 로딩 시 유효성 검증에 실패하면 발생하는 예외.
 *
 * <p>던전의 gradeChance 합이 1.0(허용오차 1e-6)이 아닌 경우 등
 * 데이터 무결성 위반 시 애플리케이션 기동을 중단시키기 위해 던진다.
 */
public class MasterDataValidationException extends RuntimeException {

    /**
     * 예외를 생성한다.
     *
     * @param message 오류 메시지
     */
    public MasterDataValidationException(final String message) {
        super(message);
    }
}
