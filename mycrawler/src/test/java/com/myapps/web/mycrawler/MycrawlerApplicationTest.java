package com.myapps.web.mycrawler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.myapps.web.mycrawler.infrastructure.config.CrawlerConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * mycrawler 모듈의 통합 테스트.
 *
 * <p>애플리케이션 컨텍스트 로드 및 설정 바인딩을 검증합니다.
 */
@SpringBootTest
class MycrawlerApplicationTest {

    @Autowired
    private CrawlerConfig crawlerConfig;

    /**
     * 애플리케이션 컨텍스트가 정상적으로 로드되는지 검증합니다.
     */
    @Test
    void should_loadContext_when_applicationStarts() {
        assertNotNull(crawlerConfig);
    }

    /**
     * CrawlerConfig의 cron 설정이 application.yml에서 올바르게 바인딩되는지 검증합니다.
     */
    @Test
    void should_bindCronExpression_when_configLoaded() {
        assertEquals("0 0 */6 * * *", crawlerConfig.cron());
    }

    /**
     * CrawlerConfig의 timeoutSeconds 설정이 application.yml에서 올바르게 바인딩되는지 검증합니다.
     */
    @Test
    void should_bindTimeoutSeconds_when_configLoaded() {
        assertEquals(30L, crawlerConfig.timeoutSeconds());
    }

    /**
     * CrawlerConfig의 targets 설정이 application.yml에서 올바르게 로드되는지 검증합니다.
     */
    @Test
    void should_loadTargetsWithFmkoreaStock_when_configLoaded() {
        assertNotNull(crawlerConfig.targets());
        assertFalse(crawlerConfig.targets().isEmpty());

        final CrawlerConfig.TargetConfig firstTarget = crawlerConfig.targets().getFirst();
        assertEquals("fmkorea-stock", firstTarget.name());
        assertEquals("https://www.fmkorea.com/stock", firstTarget.url());
    }
}
