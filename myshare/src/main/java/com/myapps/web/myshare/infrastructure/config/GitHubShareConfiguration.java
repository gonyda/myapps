package com.myapps.web.myshare.infrastructure.config;

import com.myapps.web.myshare.application.service.InMemoryShareCache;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** GitHub 공유 페이지 조회에 필요한 설정 프로퍼티, HTTP 클라이언트, 캐시 및 시계 빈을 등록하는 설정 클래스. */
@Configuration
@EnableConfigurationProperties({GitHubShareProperties.class, ShareCacheProperties.class})
public class GitHubShareConfiguration {

    private static final String GITHUB_API_BASE_URL = "https://api.github.com";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    /**
     * GitHub Contents API 호출용 {@link RestClient} 빈을 생성합니다.
     *
     * <p>연결 타임아웃 3초, 읽기 타임아웃 5초를 적용합니다.
     *
     * @return 타임아웃이 구성된 {@link RestClient} 인스턴스
     */
    @Bean
    public RestClient gitHubShareRestClient() {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .baseUrl(GITHUB_API_BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 캐시 만료 시각 계산에 사용하는 시스템 UTC {@link Clock} 빈을 제공합니다.
     *
     * @return UTC 기준 {@link Clock}
     */
    @Bean
    public Clock shareClock() {
        return Clock.systemUTC();
    }

    /**
     * 설정된 TTL과 용량 제한으로 바운디드 {@link InMemoryShareCache} 빈을 생성합니다.
     *
     * @param cacheProperties 캐시 설정 프로퍼티
     * @param shareClock 현재 시각 시계
     * @return 구성된 {@link InMemoryShareCache} 인스턴스
     */
    @Bean
    public InMemoryShareCache inMemoryShareCache(
            final ShareCacheProperties cacheProperties, final Clock shareClock) {
        return new InMemoryShareCache(
                cacheProperties.ttlSeconds(), cacheProperties.maxEntries(), shareClock);
    }
}
