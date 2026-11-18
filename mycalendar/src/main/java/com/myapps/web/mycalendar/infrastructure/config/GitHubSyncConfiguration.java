package com.myapps.web.mycalendar.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** GitHub 일정 동기화에 필요한 설정 프로퍼티 및 HTTP 클라이언트를 등록하는 설정 클래스. */
@Configuration
@EnableConfigurationProperties(GitHubSyncProperties.class)
public class GitHubSyncConfiguration {

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
    public RestClient gitHubRestClient() {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .baseUrl(GITHUB_API_BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }
}
