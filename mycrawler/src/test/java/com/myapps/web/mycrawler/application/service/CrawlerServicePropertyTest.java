package com.myapps.web.mycrawler.application.service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import com.myapps.web.mycrawler.domain.model.CrawlResult;
import com.myapps.web.mycrawler.domain.model.CrawlStatus;
import com.myapps.web.mycrawler.domain.model.TriggerSource;
import com.myapps.web.mycrawler.infrastructure.antidetect.AntiDetectionService;
import com.myapps.web.mycrawler.infrastructure.config.CrawlerConfig;
import com.myapps.web.mycrawler.infrastructure.crawler.CrawlerEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

/**
 * CrawlerService의 중복 실행 방지 및 최근 결과 관리 동작에 대한 Property-Based 테스트.
 *
 * <p>jqwik을 사용하여 다양한 입력에 대해 서비스의 불변식을 검증합니다.
 *
 * <p><b>Validates: Requirements 3.2, 4.4</b>
 */
class CrawlerServicePropertyTest {

    /**
     * isRunning이 true일 때 executeAll() 호출이 새 크롤링을 시작하지 않고 빈 결과를 반환함을 검증합니다.
     *
     * <p>리플렉션을 사용하여 running AtomicBoolean을 true로 설정한 뒤,
     * executeAll() 호출 결과가 빈 리스트이고 crawlerEngine.crawl()이 호출되지 않음을 확인합니다.
     *
     * <p><b>Validates: Requirements 3.2</b>
     *
     * @param seed 랜덤 시드로 활용되는 임의의 정수 (jqwik이 다양한 실행 경로를 탐색하도록 유도)
     */
    @Property(tries = 100)
    void executeAllShouldReturnEmptyWhenAlreadyRunning(@ForAll @IntRange(min = 0, max = 1000) final int seed) throws Exception {
        final CrawlerEngine mockCrawlerEngine = mock(CrawlerEngine.class);
        final AntiDetectionService mockAntiDetectionService = mock(AntiDetectionService.class);
        final CrawlerConfig mockCrawlerConfig = mock(CrawlerConfig.class);

        final CrawlerService crawlerService = new CrawlerService(
                mockCrawlerEngine, mockAntiDetectionService, mockCrawlerConfig);

        final Field runningField = CrawlerService.class.getDeclaredField("running");
        runningField.setAccessible(true);
        final AtomicBoolean running = (AtomicBoolean) runningField.get(crawlerService);
        running.set(true);

        final TriggerSource triggerSource = seed % 2 == 0 ? TriggerSource.SCHEDULED : TriggerSource.MANUAL;
        final List<CrawlResult> result = crawlerService.executeAll(triggerSource);

        assertThat(result).isEmpty();
        verify(mockCrawlerEngine, never()).crawl(any());
    }

    /**
     * N개 결과를 추가한 후 getRecentResults() 크기가 min(N, 20) 이하이며,
     * 목록이 시간 역순(최신 우선)으로 정렬됨을 검증합니다.
     *
     * <p>리플렉션을 사용하여 private addToRecentResults 메서드를 호출하고,
     * 각 결과에 1분씩 증가하는 고유 타임스탬프를 부여하여 정렬 순서를 확인합니다.
     *
     * <p><b>Validates: Requirements 4.4</b>
     *
     * @param count 추가할 결과 개수 (0~50 범위)
     */
    @Property(tries = 100)
    void recentResultsShouldBeLimitedAndSortedInReverseChronologicalOrder(
            @ForAll @IntRange(min = 0, max = 50) final int count) throws Exception {

        final CrawlerEngine mockCrawlerEngine = mock(CrawlerEngine.class);
        final AntiDetectionService mockAntiDetectionService = mock(AntiDetectionService.class);
        final CrawlerConfig mockCrawlerConfig = mock(CrawlerConfig.class);

        final CrawlerService crawlerService = new CrawlerService(
                mockCrawlerEngine, mockAntiDetectionService, mockCrawlerConfig);

        final Method addToRecentResultsMethod = CrawlerService.class.getDeclaredMethod(
                "addToRecentResults", CrawlResult.class);
        addToRecentResultsMethod.setAccessible(true);

        final LocalDateTime baseTime = LocalDateTime.of(2025, 1, 1, 0, 0);

        for (int i = 0; i < count; i++) {
            final LocalDateTime startTime = baseTime.plusMinutes(i);
            final LocalDateTime endTime = startTime.plusSeconds(30);
            final CrawlResult crawlResult = new CrawlResult(
                    "target-" + i,
                    "https://example.com/" + i,
                    CrawlStatus.SUCCESS,
                    TriggerSource.SCHEDULED,
                    "content-" + i,
                    null,
                    startTime,
                    endTime
            );
            addToRecentResultsMethod.invoke(crawlerService, crawlResult);
        }

        final List<CrawlResult> recentResults = crawlerService.getRecentResults();
        final int expectedMaxSize = Math.min(count, 20);

        assertThat(recentResults).hasSizeLessThanOrEqualTo(expectedMaxSize);
        assertThat(recentResults).hasSize(expectedMaxSize);

        for (int i = 0; i < recentResults.size() - 1; i++) {
            final LocalDateTime currentStartTime = recentResults.get(i).startTime();
            final LocalDateTime nextStartTime = recentResults.get(i + 1).startTime();
            assertThat(currentStartTime).isAfter(nextStartTime);
        }
    }
}
