package com.myapps.web.myrpg.domain.exception;

/**
 * 마스터 데이터에서 요청한 id에 해당하는 템플릿을 찾을 수 없을 때 발생하는 예외.
 *
 * <p>MasterDataLoader 조회 메서드에서 존재하지 않는 id를 요청하면 이 예외를 던진다.
 */
public class MasterDataNotFoundException extends RuntimeException {

    /**
     * 예외를 생성한다.
     *
     * @param message 오류 메시지
     */
    public MasterDataNotFoundException(final String message) {
        super(message);
    }
}
