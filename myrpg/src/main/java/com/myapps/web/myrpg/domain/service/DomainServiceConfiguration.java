package com.myapps.web.myrpg.domain.service;

import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 도메인 서비스 빈 등록 설정.
 *
 * <p>Spring 어노테이션이 없는 순수 도메인 서비스를 애플리케이션 컨텍스트에 빈으로 등록합니다.
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

    /**
     * {@link StatProgression} 빈을 생성합니다.
     *
     * @return 레벨 기반 스탯/바이탈 최대치 계산 정책
     */
    @Bean
    public StatProgression statProgression() {
        return new StatProgression();
    }

    /**
     * {@link BattleResolver} 빈을 생성합니다.
     *
     * <p>크리티컬 판정·편차 산출에 사용할 {@link Random}을 주입받아 순수 전투 데미지 계산 도메인 서비스를 생성한다.
     *
     * @param random 난수 생성기 빈
     * @return 전투 데미지 계산 도메인 서비스
     */
    @Bean
    public BattleResolver battleResolver(final Random random) {
        return new BattleResolver(random);
    }
}
