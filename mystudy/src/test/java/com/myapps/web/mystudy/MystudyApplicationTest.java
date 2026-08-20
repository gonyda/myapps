package com.myapps.web.mystudy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * mystudy 모듈의 Spring Boot 통합 테스트.
 *
 * <p>ApplicationContext가 오류 없이 로드되는지 검증합니다.
 */
@SpringBootTest
class MystudyApplicationTest {

    /** Spring Boot ApplicationContext가 정상적으로 로드되는지 검증합니다. */
    @Test
    void should_loadContext_when_applicationStarts() {}
}
