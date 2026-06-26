package com.myapps.web.mycrawler.application.service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myapps.web.mycrawler.domain.model.CrawlResult;
import com.myapps.web.mycrawler.domain.model.CrawlStatus;
import com.myapps.web.mycrawler.domain.model.CrawlTarget;
import com.myapps.web.mycrawler.domain.model.TriggerSource;
import com.myapps.web.mycrawler.infrastructure.antidetect.AntiDetectionService;
import com.myapps.web.mycrawler.infrastructure.config.CrawlerConfig;
import com.myapps.web.mycrawler.infrastructure.crawler.CrawlerEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CrawlerService의 단위 테스트.
 *
 * <p>executeAll, executeSingle 메서드의 성공/실패 시나리오와
 * 중복 실행 방지, 결과 저장 크기 제한을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class CrawlerServiceTest {

    private static final String TARGET_NAME_1 = "target-1";
    private static final String TARGET_URL_1 = "https://example.com/1";
    private static final String TARGET_NAME_2 = "target-2";
    private static final String TARGET_URL_2 = "https://example.com/2";
    private static final String TARGET_NAME_3 = "target-3";
    private static final String TARGET_URL_3 = "https://example.com/3";

    @Mock
    private CrawlerEngine crawlerEngine;

    @Mock
    private AntiDetectionService antiDetectionService;

    @Mock
    private CrawlerConfig crawlerConfig;

    private CrawlerService crawlerService;

    @BeforeEach
    void setUp() {
        crawlerService = new CrawlerService(crawlerEngine, antiDetectionService, crawlerConfig);
    }

    @Test
    void should_returnAllSuccessResults_when_allTargetsCrawlSuccessfully() {
        final CrawlTarget target1 = new CrawlTarget(TARGET_NAME_1, TARGET_URL_1);
        final CrawlTarget target2 = new CrawlTarget(TARGET_NAME_2, TARGET_URL_2);
        final List<CrawlTarget> targets = List.of(target1, target2);

        when(crawlerConfig.validTargets()).thenReturn(targets);
        when(antiDetectionService.randomInterTargetDelay()).thenReturn(0L);

        final CrawlResult result1 = createSuccessResult(target1);
        final CrawlResult result2 = createSuccessResult(target2);
        when(crawlerEngine.crawl(target1)).thenReturn(result1);
        when(crawlerEngine.crawl(target2)).thenReturn(result2);

        final List<CrawlResult> results = crawlerService.executeAll(TriggerSource.SCHEDULED);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).status()).isEqualTo(CrawlStatus.SUCCESS);
        assertThat(results.get(1).status()).isEqualTo(CrawlStatus.SUCCESS);
        verify(crawlerEngine, times(2)).crawl(any());
        verify(antiDetectionService, times(1)).randomInterTargetDelay();
    }

    @Test
    void should_returnFailureResultsForFailedTargets_when_someTargetsThrowException() {
        final CrawlTarget target1 = new CrawlTarget(TARGET_NAME_1, TARGET_URL_1);
        final CrawlTarget target2 = new CrawlTarget(TARGET_NAME_2, TARGET_URL_2);
        final CrawlTarget target3 = new CrawlTarget(TARGET_NAME_3, TARGET_URL_3);
        final List<CrawlTarget> targets = List.of(target1, target2, target3);

        when(crawlerConfig.validTargets()).thenReturn(targets);
        when(antiDetectionService.randomInterTargetDelay()).thenReturn(0L);

        final CrawlResult result1 = createSuccessResult(target1);
        when(crawlerEngine.crawl(target1)).thenReturn(result1);
        when(crawlerEngine.crawl(target2)).thenThrow(new RuntimeException("Network error"));
        final CrawlResult result3 = createSuccessResult(target3);
        when(crawlerEngine.crawl(target3)).thenReturn(result3);

        final List<CrawlResult> results = crawlerService.executeAll(TriggerSource.MANUAL);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).status()).isEqualTo(CrawlStatus.SUCCESS);
        assertThat(results.get(1).status()).isEqualTo(CrawlStatus.FAILURE);
        assertThat(results.get(1).errorMessage()).isEqualTo("Network error");
        assertThat(results.get(2).status()).isEqualTo(CrawlStatus.SUCCESS);
        verify(crawlerEngine, times(3)).crawl(any());
    }

    @Test
    void should_returnEmptyList_when_executeAllCalledWhileAlreadyRunning() throws Exception {
        setRunningState(true);

        final List<CrawlResult> results = crawlerService.executeAll(TriggerSource.SCHEDULED);

        assertThat(results).isEmpty();
        verify(crawlerEngine, never()).crawl(any());
        verify(crawlerConfig, never()).validTargets();
    }

    @Test
    void should_returnCrawlResult_when_executeSingleWithValidTargetName() {
        final CrawlTarget target1 = new CrawlTarget(TARGET_NAME_1, TARGET_URL_1);
        final CrawlTarget target2 = new CrawlTarget(TARGET_NAME_2, TARGET_URL_2);
        final List<CrawlTarget> targets = List.of(target1, target2);

        when(crawlerConfig.validTargets()).thenReturn(targets);

        final CrawlResult expectedResult = createSuccessResult(target2);
        when(crawlerEngine.crawl(target2)).thenReturn(expectedResult);

        final CrawlResult result = crawlerService.executeSingle(TARGET_NAME_2, TriggerSource.MANUAL);

        assertThat(result).isNotNull();
        assertThat(result.targetName()).isEqualTo(TARGET_NAME_2);
        assertThat(result.status()).isEqualTo(CrawlStatus.SUCCESS);
        verify(crawlerEngine, times(1)).crawl(target2);
    }

    @Test
    void should_returnNull_when_executeSingleWithNonExistentTargetName() {
        final CrawlTarget target1 = new CrawlTarget(TARGET_NAME_1, TARGET_URL_1);
        final List<CrawlTarget> targets = List.of(target1);

        when(crawlerConfig.validTargets()).thenReturn(targets);

        final CrawlResult result = crawlerService.executeSingle("non-existent", TriggerSource.MANUAL);

        assertThat(result).isNull();
        verify(crawlerEngine, never()).crawl(any());
    }

    @Test
    void should_returnNull_when_executeSingleCalledWhileAlreadyRunning() throws Exception {
        setRunningState(true);

        final CrawlResult result = crawlerService.executeSingle(TARGET_NAME_1, TriggerSource.MANUAL);

        assertThat(result).isNull();
        verify(crawlerEngine, never()).crawl(any());
        verify(crawlerConfig, never()).validTargets();
    }

    @Test
    void should_storeResultsInRecentResults_when_executeAllCompletes() {
        final CrawlTarget target1 = new CrawlTarget(TARGET_NAME_1, TARGET_URL_1);
        final List<CrawlTarget> targets = List.of(target1);

        when(crawlerConfig.validTargets()).thenReturn(targets);

        final CrawlResult result1 = createSuccessResult(target1);
        when(crawlerEngine.crawl(target1)).thenReturn(result1);

        crawlerService.executeAll(TriggerSource.SCHEDULED);

        final List<CrawlResult> recentResults = crawlerService.getRecentResults();
        assertThat(recentResults).hasSize(1);
        assertThat(recentResults.get(0).targetName()).isEqualTo(TARGET_NAME_1);
    }

    @Test
    void should_limitRecentResultsToTwenty_when_moreThanTwentyResultsAdded() {
        final int totalTargets = 25;
        final List<CrawlTarget> targets = createTargetList(totalTargets);

        when(crawlerConfig.validTargets()).thenReturn(targets);
        when(antiDetectionService.randomInterTargetDelay()).thenReturn(0L);

        for (final CrawlTarget target : targets) {
            when(crawlerEngine.crawl(target)).thenReturn(createSuccessResult(target));
        }

        crawlerService.executeAll(TriggerSource.SCHEDULED);

        final List<CrawlResult> recentResults = crawlerService.getRecentResults();
        assertThat(recentResults).hasSize(20);
    }

    @Test
    void should_storeResultsInNewestFirstOrder_when_multipleResultsAdded() {
        final CrawlTarget target1 = new CrawlTarget(TARGET_NAME_1, TARGET_URL_1);
        final CrawlTarget target2 = new CrawlTarget(TARGET_NAME_2, TARGET_URL_2);
        final List<CrawlTarget> targets = List.of(target1, target2);

        when(crawlerConfig.validTargets()).thenReturn(targets);
        when(antiDetectionService.randomInterTargetDelay()).thenReturn(0L);

        final LocalDateTime baseTime = LocalDateTime.of(2025, 1, 1, 10, 0);
        final CrawlResult result1 = new CrawlResult(
                TARGET_NAME_1, TARGET_URL_1, CrawlStatus.SUCCESS,
                TriggerSource.SCHEDULED, "content1", null,
                baseTime, baseTime.plusSeconds(5));
        final CrawlResult result2 = new CrawlResult(
                TARGET_NAME_2, TARGET_URL_2, CrawlStatus.SUCCESS,
                TriggerSource.SCHEDULED, "content2", null,
                baseTime.plusMinutes(1), baseTime.plusMinutes(1).plusSeconds(5));

        when(crawlerEngine.crawl(target1)).thenReturn(result1);
        when(crawlerEngine.crawl(target2)).thenReturn(result2);

        crawlerService.executeAll(TriggerSource.SCHEDULED);

        final List<CrawlResult> recentResults = crawlerService.getRecentResults();
        assertThat(recentResults).hasSize(2);
        assertThat(recentResults.get(0).targetName()).isEqualTo(TARGET_NAME_2);
        assertThat(recentResults.get(1).targetName()).isEqualTo(TARGET_NAME_1);
    }

    @Test
    void should_setRunningFalseAfterExecution_when_executeAllCompletes() {
        final List<CrawlTarget> targets = List.of(new CrawlTarget(TARGET_NAME_1, TARGET_URL_1));

        when(crawlerConfig.validTargets()).thenReturn(targets);
        when(crawlerEngine.crawl(any())).thenReturn(
                createSuccessResult(new CrawlTarget(TARGET_NAME_1, TARGET_URL_1)));

        crawlerService.executeAll(TriggerSource.SCHEDULED);

        assertThat(crawlerService.isRunning()).isFalse();
    }

    @Test
    void should_applyInterTargetDelayBetweenTargets_when_multipleTargetsExist() {
        final CrawlTarget target1 = new CrawlTarget(TARGET_NAME_1, TARGET_URL_1);
        final CrawlTarget target2 = new CrawlTarget(TARGET_NAME_2, TARGET_URL_2);
        final CrawlTarget target3 = new CrawlTarget(TARGET_NAME_3, TARGET_URL_3);
        final List<CrawlTarget> targets = List.of(target1, target2, target3);

        when(crawlerConfig.validTargets()).thenReturn(targets);
        when(antiDetectionService.randomInterTargetDelay()).thenReturn(0L);
        when(crawlerEngine.crawl(any())).thenReturn(
                createSuccessResult(new CrawlTarget(TARGET_NAME_1, TARGET_URL_1)));

        crawlerService.executeAll(TriggerSource.SCHEDULED);

        verify(antiDetectionService, times(2)).randomInterTargetDelay();
    }

    private CrawlResult createSuccessResult(final CrawlTarget target) {
        final LocalDateTime now = LocalDateTime.now();
        return new CrawlResult(
                target.name(),
                target.url(),
                CrawlStatus.SUCCESS,
                TriggerSource.SCHEDULED,
                "crawled content",
                null,
                now,
                now.plusSeconds(2)
        );
    }

    private List<CrawlTarget> createTargetList(final int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new CrawlTarget("target-" + i, "https://example.com/" + i))
                .toList();
    }

    private void setRunningState(final boolean state) throws Exception {
        final Field runningField = CrawlerService.class.getDeclaredField("running");
        runningField.setAccessible(true);
        final AtomicBoolean running = (AtomicBoolean) runningField.get(crawlerService);
        running.set(state);
    }
}
