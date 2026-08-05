package com.myapps.web.myrpg.domain.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.myapps.web.myrpg.domain.model.ExperiencePolicy;

/**
 * 도메인 서비스 빈 등록 설정.
 *
 * <p>Spring 어노테이션이 없는 순수 도메인 서비스를
 * 애플리케이션 컨텍스트에 빈으로 등록합니다.
 */
@Configuration
public class DomainServiceConfiguration {

    /**
     * {@link MapViewFactory} 빈을 생성합니다.
     *
     * @return 미니맵/전체지도 격자 생성 팩토리
     */
    @Bean
    public MapViewFactory mapViewFactory() {
        return new MapViewFactory();
    }

    /**
     * {@link ExperiencePolicy} 빈을 생성합니다.
     *
     * @return 경험치 정책 (다음 레벨 필요 경험치 산출)
     */
    @Bean
    public ExperiencePolicy experiencePolicy() {
        return new ExperiencePolicy();
    }
}
