package com.myapps.web.myshare.infrastructure.config;

import com.myapps.web.myshare.application.service.MiniAppJsonValidator;
import java.util.concurrent.Executor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import tools.jackson.databind.ObjectMapper;

/** 미니앱 저장소 JSON 검증기 및 GitHub 비동기 동기화 단일 스레드 실행기를 구성하는 설정 클래스. */
@Configuration
@EnableAsync
@EnableConfigurationProperties(MiniAppProperties.class)
public class MiniAppConfiguration {

    private static final int SINGLE_THREAD_POOL_SIZE = 1;
    private static final int SYNC_QUEUE_CAPACITY = 1000;
    private static final int SHUTDOWN_AWAIT_SECONDS = 5;
    private static final String SYNC_THREAD_PREFIX = "github-miniapp-sync-";

    /**
     * 원시 JSON 요청 크기 및 문법 검증에 사용하는 {@link MiniAppJsonValidator} 빈을 생성합니다.
     *
     * @param properties 미니앱 설정 프로퍼티
     * @param objectMapper Jackson 3 {@link ObjectMapper}
     * @return 구성된 {@link MiniAppJsonValidator}
     */
    @Bean
    public MiniAppJsonValidator miniAppJsonValidator(
            final MiniAppProperties properties, final ObjectMapper objectMapper) {
        return new MiniAppJsonValidator(properties.maxPayloadBytes(), objectMapper);
    }

    /**
     * {@code gonyda/mike} 커밋·푸시 동시성 충돌 방지를 위해 작업을 FIFO로 직렬화하는 단일 스레드 비동기 실행기를 생성합니다.
     *
     * @return 단일 스레드 {@link Executor}
     */
    @Bean(name = "miniAppGitHubSyncExecutor")
    public Executor miniAppGitHubSyncExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(SINGLE_THREAD_POOL_SIZE);
        executor.setMaxPoolSize(SINGLE_THREAD_POOL_SIZE);
        executor.setQueueCapacity(SYNC_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(SYNC_THREAD_PREFIX);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(SHUTDOWN_AWAIT_SECONDS);
        executor.initialize();
        return executor;
    }
}
