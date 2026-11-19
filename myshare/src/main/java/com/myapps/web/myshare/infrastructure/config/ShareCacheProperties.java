package com.myapps.web.myshare.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code myshare.cache} 인메모리 캐시 설정 프로퍼티를 바인딩하는 불변 레코드.
 *
 * @param ttlSeconds 캐시 유효 시간(초)
 * @param maxEntries 최대 캐시 엔트리 수
 */
@ConfigurationProperties(prefix = "myshare.cache")
public record ShareCacheProperties(long ttlSeconds, int maxEntries) {

    private static final long DEFAULT_TTL_SECONDS = 20L;
    private static final int DEFAULT_MAX_ENTRIES = 200;

    /** 음수 설정을 기본값 또는 안전한 범위로 정규화합니다. */
    public ShareCacheProperties {
        ttlSeconds = ttlSeconds < 0 ? DEFAULT_TTL_SECONDS : ttlSeconds;
        maxEntries = maxEntries < 0 ? DEFAULT_MAX_ENTRIES : maxEntries;
    }
}
