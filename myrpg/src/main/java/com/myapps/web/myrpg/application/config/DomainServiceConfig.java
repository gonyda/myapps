package com.myapps.web.myrpg.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.myapps.web.myrpg.domain.random.RandomSource;
import com.myapps.web.myrpg.domain.service.DungeonService;
import com.myapps.web.myrpg.domain.service.EquipmentService;
import com.myapps.web.myrpg.domain.service.ShopService;

/**
 * 순수 도메인 서비스를 Spring 빈으로 등록하는 설정 클래스.
 *
 * <p>{@code @Service} 어노테이션이 없는 순수 도메인 서비스들을 팩토리 메서드로 빈 등록한다.
 * DungeonService는 RandomSource를 필요로 하며, ShopService·EquipmentService는 무의존이다.
 */
@Configuration
public class DomainServiceConfig {

    /**
     * DungeonService 빈을 등록한다.
     *
     * @param randomSource 난수 생성 인터페이스
     * @return DungeonService 인스턴스
     */
    @Bean
    public DungeonService dungeonService(final RandomSource randomSource) {
        return new DungeonService(randomSource);
    }

    /**
     * ShopService 빈을 등록한다.
     *
     * @return ShopService 인스턴스
     */
    @Bean
    public ShopService shopService() {
        return new ShopService();
    }

    /**
     * EquipmentService 빈을 등록한다.
     *
     * @return EquipmentService 인스턴스
     */
    @Bean
    public EquipmentService equipmentService() {
        return new EquipmentService();
    }
}
