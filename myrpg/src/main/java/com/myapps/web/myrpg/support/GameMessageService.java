package com.myapps.web.myrpg.support;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

/**
 * 인게임 텍스트 및 로그 메시지 리졸버 서비스.
 *
 * <p>{@code messages.properties} 기반으로 한국어 로케일 메시지를 반환하며, 정의되지 않은 키가 요청될 경우 서버 에러 없이 키 이름을 안전하게
 * 반환합니다.
 */
@Service
public class GameMessageService {

    private final MessageSource messageSource;

    public GameMessageService(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * 프로퍼티 키와 포맷팅 인자를 받아 완성된 한국어 메시지를 반환합니다.
     *
     * @param code 메시지 프로퍼티 키
     * @param args 치환 인자 목록 ({0}, {1}, ...)
     * @return 포맷팅된 한국어 메시지 (키 미존재 시 code 반환)
     */
    public String get(final String code, final Object... args) {
        if (code == null) {
            return "";
        }
        try {
            return messageSource.getMessage(code, args, Locale.KOREAN);
        } catch (final NoSuchMessageException ex) {
            return code;
        }
    }
}
