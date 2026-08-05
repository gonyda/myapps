package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.util.Random;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.SessionScope;

import com.myapps.web.myrpg.domain.model.ActionLog;

/**
 * 애플리케이션 서비스 계층의 인프라 빈 등록 설정.
 *
 * <p>{@link Clock}과 {@link Random}을 빈으로 등록하여
 * 테스트 시 고정 시각·고정 시드 주입이 가능하도록 합니다.
 * {@link ActionLog}는 HTTP 세션 스코프로 등록하여 세션 단위로 보관합니다.
 */
@Configuration
public class ApplicationServiceConfiguration {

    /**
     * 시스템 기본 시계 빈을 생성합니다.
     *
     * @return 시스템 기본 Clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /**
     * 무작위 선택용 Random 빈을 생성합니다.
     *
     * @return Random 인스턴스
     */
    @Bean
    public Random random() {
        return new Random();
    }

    /**
     * 세션 스코프 행동 로그 빈을 생성합니다.
     *
     * <p>HTTP 세션 단위로 최대 10개의 행동 로그를 유지합니다.
     *
     * @param clock 타임스탬프 생성에 사용할 시계
     * @return 세션별 ActionLog 인스턴스
     */
    @Bean
    @SessionScope
    public ActionLog actionLog(final Clock clock) {
        return new ActionLog(clock);
    }
}
