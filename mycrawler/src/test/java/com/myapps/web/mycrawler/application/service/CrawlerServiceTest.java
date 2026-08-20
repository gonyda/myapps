package com.myapps.web.mycrawler.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.mycrawler.domain.model.CrawlResult;
import com.myapps.web.mycrawler.domain.model.CrawlStatus;
import com.myapps.web.mycrawler.domain.model.CrawlTarget;
import com.myapps.web.mycrawler.domain.model.TriggerSource;
import com.myapps.web.mycrawler.infrastructure.config.CrawlerConfig;
import com.myapps.web.mycrawler.infrastructure.crawler.CrawlerEngine;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CrawlerService의 단위 테스트.
 *
 * <p>executeSingle 메서드의 성공/실패 시나리오와 중복 실행 방지, 결과 저장을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class CrawlerServiceTest {

    private static final String TARGET_NAME_1 = "target-1";
    private static final String TARGET_URL_1 = "https://example.com/1";
    private static final String TARGET_NAME_2 = "target-2";
    private static final String TARGET_URL_2 = "https://example.com/2";

    @Mock private CrawlerEngine crawlerEngine;

    @Mock private CrawlerConfig crawlerConfig;

    private CrawlerService crawlerService;

    @BeforeEach
    void setUp() {
        crawlerService = new CrawlerService(crawlerEngine, crawlerConfig);
    }

    @Test
    void should_returnCrawlResult_when_executeSingleWithValidTargetName() {
        final CrawlTarget target1 = new CrawlTarget(TARGET_NAME_1, TARGET_URL_1);
        final CrawlTarget target2 = new CrawlTarget(TARGET_NAME_2, TARGET_URL_2);
        final List<CrawlTarget> targets = List.of(target1, target2);

        when(crawlerConfig.validTargets()).thenReturn(targets);

        final CrawlResult expectedResult = createSuccessResult(target2);
        when(crawlerEngine.crawl(target2, TriggerSource.MANUAL)).thenReturn(expectedResult);

        final CrawlResult result =
                crawlerService.executeSingle(TARGET_NAME_2, TriggerSource.MANUAL);

        assertThat(result).isNotNull();
        assertThat(result.targetName()).isEqualTo(TARGET_NAME_2);
        assertThat(result.status()).isEqualTo(CrawlStatus.SUCCESS);
        verify(crawlerEngine, times(1)).crawl(target2, TriggerSource.MANUAL);
    }

    @Test
    void should_returnNull_when_executeSingleWithNonExistentTargetName() {
        final CrawlTarget target1 = new CrawlTarget(TARGET_NAME_1, TARGET_URL_1);
        final List<CrawlTarget> targets = List.of(target1);

        when(crawlerConfig.validTargets()).thenReturn(targets);

        final CrawlResult result =
                crawlerService.executeSingle("non-existent", TriggerSource.MANUAL);

        assertThat(result).isNull();
        verify(crawlerEngine, never()).crawl(any(), any());
    }

    @Test
    void should_returnNull_when_executeSingleCalledWhileAlreadyRunning() throws Exception {
        setRunningState(true);

        final CrawlResult result =
                crawlerService.executeSingle(TARGET_NAME_1, TriggerSource.MANUAL);

        assertThat(result).isNull();
        verify(crawlerEngine, never()).crawl(any(), any());
        verify(crawlerConfig, never()).validTargets();
    }

    @Test
    void should_storeResultInRecentResults_when_executeSingleCompletes() {
        final CrawlTarget target1 = new CrawlTarget(TARGET_NAME_1, TARGET_URL_1);
        final List<CrawlTarget> targets = List.of(target1);

        when(crawlerConfig.validTargets()).thenReturn(targets);

        final CrawlResult expectedResult = createSuccessResult(target1);
        when(crawlerEngine.crawl(target1, TriggerSource.MANUAL)).thenReturn(expectedResult);

        crawlerService.executeSingle(TARGET_NAME_1, TriggerSource.MANUAL);

        final List<CrawlResult> recentResults = crawlerService.getRecentResults();
        assertThat(recentResults).hasSize(1);
        assertThat(recentResults.get(0).targetName()).isEqualTo(TARGET_NAME_1);
    }

    @Test
    void should_setRunningFalseAfterExecution_when_executeSingleCompletes() {
        final CrawlTarget target1 = new CrawlTarget(TARGET_NAME_1, TARGET_URL_1);
        final List<CrawlTarget> targets = List.of(target1);

        when(crawlerConfig.validTargets()).thenReturn(targets);
        when(crawlerEngine.crawl(target1, TriggerSource.MANUAL))
                .thenReturn(createSuccessResult(target1));

        crawlerService.executeSingle(TARGET_NAME_1, TriggerSource.MANUAL);

        assertThat(crawlerService.isRunning()).isFalse();
    }

    @Test
    void should_returnFailureResult_when_targetCrawlThrowsException() {
        final CrawlTarget target1 = new CrawlTarget(TARGET_NAME_1, TARGET_URL_1);
        final List<CrawlTarget> targets = List.of(target1);

        when(crawlerConfig.validTargets()).thenReturn(targets);
        when(crawlerEngine.crawl(target1, TriggerSource.MANUAL))
                .thenThrow(new RuntimeException("Network error"));

        final CrawlResult result =
                crawlerService.executeSingle(TARGET_NAME_1, TriggerSource.MANUAL);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(CrawlStatus.FAILURE);
        assertThat(result.errorMessage()).isEqualTo("Network error");
    }

    private CrawlResult createSuccessResult(final CrawlTarget target) {
        final LocalDateTime now = LocalDateTime.now();
        return new CrawlResult(
                target.name(),
                target.url(),
                CrawlStatus.SUCCESS,
                TriggerSource.MANUAL,
                "crawled content",
                null,
                now,
                now.plusSeconds(2));
    }

    private void setRunningState(final boolean state) throws Exception {
        final Field runningField = CrawlerService.class.getDeclaredField("running");
        runningField.setAccessible(true);
        final AtomicBoolean running = (AtomicBoolean) runningField.get(crawlerService);
        running.set(state);
    }
}
