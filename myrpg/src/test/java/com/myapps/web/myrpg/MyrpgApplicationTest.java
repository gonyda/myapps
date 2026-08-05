package com.myapps.web.myrpg;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 애플리케이션 컨텍스트 로드를 검증하는 통합 테스트.
 *
 * <p>Spring Boot 전체 컨텍스트가 올바르게 로드되는지 확인합니다.
 */
@SpringBootTest
class MyrpgApplicationTest {

    /**
     * 애플리케이션 컨텍스트가 정상적으로 로드되는지 검증한다.
     */
    @Test
    void should_loadContext_when_applicationStarts() {
        // 컨텍스트 로드 성공 시 테스트 통과
    }
}
