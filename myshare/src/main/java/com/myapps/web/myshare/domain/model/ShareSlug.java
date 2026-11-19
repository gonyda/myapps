package com.myapps.web.myshare.domain.model;

import com.myapps.web.myshare.domain.exception.InvalidShareSlugException;
import java.util.regex.Pattern;

/**
 * 공유 HTML 파일을 식별하는 불변 슬러그 값 객체.
 *
 * <p>영문, 숫자, 하이픈({@code -}), 언더스코어({@code _}) 조합(1~200자)만 허용하여 경로 탐색 공격을 차단합니다.
 *
 * @param value 검증을 통과한 슬러그 문자열
 */
public record ShareSlug(String value) {

    private static final int MAX_LENGTH = 200;
    private static final Pattern ALLOWED_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_-]{1," + MAX_LENGTH + "}$");

    /** 슬러그 형식과 길이를 검증합니다. */
    public ShareSlug {
        if (value == null || !ALLOWED_PATTERN.matcher(value).matches()) {
            throw new InvalidShareSlugException("유효하지 않은 공유 슬러그 형식입니다.");
        }
    }

    /**
     * 원시 문자열로부터 검증된 {@link ShareSlug}를 생성합니다.
     *
     * @param rawSlug 요청 경로에서 추출한 원시 슬러그
     * @return 검증된 {@link ShareSlug} 인스턴스
     * @throws InvalidShareSlugException 허용되지 않은 문자 또는 길이인 경우
     */
    public static ShareSlug of(final String rawSlug) {
        return new ShareSlug(rawSlug);
    }
}
