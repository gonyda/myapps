package com.myapps.web.mycrawler.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.mycrawler.domain.model.TriggerSource;
import com.myapps.web.mycrawler.infrastructure.antidetect.AntiDetectionService;
import com.myapps.web.mycrawler.infrastructure.config.CrawlerConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link SchedulerService}에 대한 단위 테스트. */
class SchedulerServiceTest {

    private final CrawlerService crawlerService = mock(CrawlerService.class);
    private final AntiDetectionService antiDetectionService = mock(AntiDetectionService.class);

    @Test
    @DisplayName("유효한 cron 표현식이면 스케줄링이 활성화된다")
    void should_enableScheduling_when_cronExpressionIsValid() {
        // given
        final CrawlerConfig config = new CrawlerConfig("0 0 */6 * * *", 30L, "", List.of());

        // when
        final SchedulerService schedulerService =
                new SchedulerService(crawlerService, config, antiDetectionService);

        // then
        assertThat(schedulerService.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("null cron 표현식이면 스케줄링이 비활성화된다")
    void should_disableScheduling_when_cronExpressionIsNull() {
        // given
        final CrawlerConfig config = new CrawlerConfig(null, 30L, "", List.of());

        // when
        final SchedulerService schedulerService =
                new SchedulerService(crawlerService, config, antiDetectionService);

        // then
        assertThat(schedulerService.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("빈 cron 표현식이면 스케줄링이 비활성화된다")
    void should_disableScheduling_when_cronExpressionIsBlank() {
        // given
        final CrawlerConfig config = new CrawlerConfig("  ", 30L, "", List.of());

        // when
        final SchedulerService schedulerService =
                new SchedulerService(crawlerService, config, antiDetectionService);

        // then
        assertThat(schedulerService.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("유효하지 않은 cron 표현식이면 스케줄링이 비활성화된다")
    void should_disableScheduling_when_cronExpressionIsInvalid() {
        // given
        final CrawlerConfig config = new CrawlerConfig("invalid-cron", 30L, "", List.of());

        // when
        final SchedulerService schedulerService =
                new SchedulerService(crawlerService, config, antiDetectionService);

        // then
        assertThat(schedulerService.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("활성화된 스케줄러는 다음 실행 시각을 KST 포맷 문자열로 반환한다")
    void should_returnNextExecutionTime_when_schedulerIsEnabled() {
        // given
        final CrawlerConfig config = new CrawlerConfig("0 0 */6 * * *", 30L, "", List.of());
        final SchedulerService schedulerService =
                new SchedulerService(crawlerService, config, antiDetectionService);

        // when
        final Optional<String> nextExecution = schedulerService.getNextExecutionTime();

        // then
        assertThat(nextExecution).isPresent();
        assertThat(nextExecution.get()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    @DisplayName("비활성화된 스케줄러는 빈 Optional을 반환한다")
    void should_returnEmpty_when_schedulerIsDisabled() {
        // given
        final CrawlerConfig config = new CrawlerConfig(null, 30L, "", List.of());
        final SchedulerService schedulerService =
                new SchedulerService(crawlerService, config, antiDetectionService);

        // when
        final Optional<String> nextExecution = schedulerService.getNextExecutionTime();

        // then
        assertThat(nextExecution).isEmpty();
    }

    @Test
    @DisplayName("getCronExpression은 설정된 cron 표현식을 반환한다")
    void should_returnCronExpression_when_called() {
        // given
        final String expectedCron = "0 0 */6 * * *";
        final CrawlerConfig config = new CrawlerConfig(expectedCron, 30L, "", List.of());
        final SchedulerService schedulerService =
                new SchedulerService(crawlerService, config, antiDetectionService);

        // when
        final String result = schedulerService.getCronExpression();

        // then
        assertThat(result).isEqualTo(expectedCron);
    }

    @Test
    @DisplayName("executeCrawl은 각 타겟별로 executeSingle을 호출한다")
    void should_callExecuteSingleForEachTarget_when_executeCrawlIsTriggered() throws Exception {
        // given
        final List<CrawlerConfig.TargetConfig> targets =
                List.of(
                        new CrawlerConfig.TargetConfig("target-a", "https://example.com/a"),
                        new CrawlerConfig.TargetConfig("target-b", "https://example.com/b"),
                        new CrawlerConfig.TargetConfig("target-c", "https://example.com/c"));
        final CrawlerConfig config = new CrawlerConfig("0 0 */6 * * *", 30L, "", targets);
        final SchedulerService schedulerService =
                new SchedulerService(crawlerService, config, antiDetectionService);

        when(antiDetectionService.randomInterTargetDelay()).thenReturn(0L);

        // when
        invokeExecuteCrawl(schedulerService);

        // then
        verify(crawlerService, times(1)).executeSingle("target-a", TriggerSource.SCHEDULED);
        verify(crawlerService, times(1)).executeSingle("target-b", TriggerSource.SCHEDULED);
        verify(crawlerService, times(1)).executeSingle("target-c", TriggerSource.SCHEDULED);
    }

    @Test
    @DisplayName("하나의 타겟이 실패해도 나머지 타겟은 계속 처리된다")
    void should_continueWithRemainingTargets_when_oneTargetFails() throws Exception {
        // given
        final List<CrawlerConfig.TargetConfig> targets =
                List.of(
                        new CrawlerConfig.TargetConfig("target-a", "https://example.com/a"),
                        new CrawlerConfig.TargetConfig("target-b", "https://example.com/b"),
                        new CrawlerConfig.TargetConfig("target-c", "https://example.com/c"));
        final CrawlerConfig config = new CrawlerConfig("0 0 */6 * * *", 30L, "", targets);
        final SchedulerService schedulerService =
                new SchedulerService(crawlerService, config, antiDetectionService);

        when(antiDetectionService.randomInterTargetDelay()).thenReturn(0L);
        when(crawlerService.executeSingle("target-b", TriggerSource.SCHEDULED))
                .thenThrow(new RuntimeException("크롤링 실패"));

        // when
        invokeExecuteCrawl(schedulerService);

        // then
        verify(crawlerService, times(1)).executeSingle("target-a", TriggerSource.SCHEDULED);
        verify(crawlerService, times(1)).executeSingle("target-b", TriggerSource.SCHEDULED);
        verify(crawlerService, times(1)).executeSingle("target-c", TriggerSource.SCHEDULED);
    }

    @Test
    @DisplayName("N개 타겟 실행 시 randomInterTargetDelay가 N-1번 호출된다")
    void should_applyRandomInterTargetDelay_when_multipleTargetsExist() throws Exception {
        // given
        final List<CrawlerConfig.TargetConfig> targets =
                List.of(
                        new CrawlerConfig.TargetConfig("target-a", "https://example.com/a"),
                        new CrawlerConfig.TargetConfig("target-b", "https://example.com/b"),
                        new CrawlerConfig.TargetConfig("target-c", "https://example.com/c"));
        final CrawlerConfig config = new CrawlerConfig("0 0 */6 * * *", 30L, "", targets);
        final SchedulerService schedulerService =
                new SchedulerService(crawlerService, config, antiDetectionService);

        when(antiDetectionService.randomInterTargetDelay()).thenReturn(0L);

        // when
        invokeExecuteCrawl(schedulerService);

        // then
        verify(antiDetectionService, times(2)).randomInterTargetDelay();
    }

    @Test
    @DisplayName("scheduledRunning이 이미 true이면 executeSingle이 호출되지 않는다")
    void should_skipExecution_when_scheduledRunningIsAlreadyTrue() throws Exception {
        // given
        final List<CrawlerConfig.TargetConfig> targets =
                List.of(new CrawlerConfig.TargetConfig("target-a", "https://example.com/a"));
        final CrawlerConfig config = new CrawlerConfig("0 0 */6 * * *", 30L, "", targets);
        final SchedulerService schedulerService =
                new SchedulerService(crawlerService, config, antiDetectionService);

        final Field scheduledRunningField =
                SchedulerService.class.getDeclaredField("scheduledRunning");
        scheduledRunningField.setAccessible(true);
        final AtomicBoolean scheduledRunning =
                (AtomicBoolean) scheduledRunningField.get(schedulerService);
        scheduledRunning.set(true);

        // when
        invokeExecuteCrawl(schedulerService);

        // then
        verify(crawlerService, never()).executeSingle(any(), any());
    }

    private void invokeExecuteCrawl(final SchedulerService schedulerService) throws Exception {
        final Method executeCrawlMethod = SchedulerService.class.getDeclaredMethod("executeCrawl");
        executeCrawlMethod.setAccessible(true);
        executeCrawlMethod.invoke(schedulerService);
    }
}
