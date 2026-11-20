package com.myapps.web.myshare.domain.model;

import com.myapps.web.myshare.domain.exception.InvalidMiniAppKeyException;
import java.util.regex.Pattern;

/**
 * 정적 HTML 미니앱 저장소의 식별자({@code appKey})를 표현하고 형식을 강제하는 불변 값 객체.
 *
 * <p>영문 소문자, 숫자, 밑줄({@code _}), 하이픈({@code -})으로 구성된 1~64자 문자열만 허용합니다.
 *
 * @param value 검증된 미니앱 키 문자열
 */
public record MiniAppKey(String value) {

    private static final Pattern ALLOWED_PATTERN = Pattern.compile("^[a-z0-9_-]{1,64}$");

    /**
     * 원시 {@code appKey} 값을 검증하여 불변 {@link MiniAppKey}로 생성합니다.
     *
     * @param value 검증할 미니앱 키 문자열
     * @throws InvalidMiniAppKeyException null, 공백 또는 허용 패턴({@code ^[a-z0-9_-]{1,64}$}) 불일치 시
     */
    public MiniAppKey {
        if (value == null || !ALLOWED_PATTERN.matcher(value).matches()) {
            throw new InvalidMiniAppKeyException("허용되지 않은 미니앱 키 형식입니다.");
        }
    }

    /**
     * 원시 문자열로부터 검증된 {@link MiniAppKey} 인스턴스를 반환합니다.
     *
     * @param rawAppKey 요청 경로에서 전달된 미니앱 키
     * @return 검증된 {@link MiniAppKey}
     * @throws InvalidMiniAppKeyException 형식이 올바르지 않은 경우
     */
    public static MiniAppKey of(final String rawAppKey) {
        return new MiniAppKey(rawAppKey);
    }
}
