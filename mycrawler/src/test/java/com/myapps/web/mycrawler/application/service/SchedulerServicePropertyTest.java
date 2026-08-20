package com.myapps.web.mycrawler.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * SchedulerService에 대한 Property-Based 테스트.
 *
 * <p>jqwik을 사용하여 다양한 유효하지 않은 cron 문자열에 대해 스케줄러가 비활성화됨을 검증하고, 개별 순회 실행 방식의 정확성을 다양한 타겟 목록에 대해 검증합니다.
 *
 * <p><b>Validates: Requirements 3.5, 6.1, 6.2, 6.3</b>
 */
class SchedulerServicePropertyTest {

    /**
     * 유효하지 않은 cron 표현식으로 SchedulerService를 생성하면 isEnabled()가 false를 반환함을 검증합니다.
     *
     * <p>null, 빈 문자열, 공백만 포함된 문자열, 파싱 불가능한 문자열 등 다양한 유효하지 않은 cron 입력에 대해 스케줄링이 비활성화됨을 확인합니다.
     *
     * <p><b>Validates: Requirements 3.5</b>
     *
     * @param invalidCron 유효하지 않은 cron 표현식
     */
    @Property(tries = 100)
    void schedulerShouldBeDisabledForInvalidCronExpression(
            @ForAll("invalidCronExpressions") final String invalidCron) {

        final CrawlerService mockCrawlerService = mock(CrawlerService.class);
        final AntiDetectionService mockAntiDetectionService = mock(AntiDetectionService.class);
        final CrawlerConfig crawlerConfig = new CrawlerConfig(invalidCron, 30L, "", List.of());

        final SchedulerService schedulerService =
                new SchedulerService(mockCrawlerService, crawlerConfig, mockAntiDetectionService);

        assertThat(schedulerService.isEnabled()).isFalse();
    }

    /**
     * null cron 표현식으로 SchedulerService를 생성하면 isEnabled()가 false를 반환함을 검증합니다.
     *
     * <p><b>Validates: Requirements 3.5</b>
     */
    @Property(tries = 10)
    void schedulerShouldBeDisabledForNullCron() {
        final CrawlerService mockCrawlerService = mock(CrawlerService.class);
        final AntiDetectionService mockAntiDetectionService = mock(AntiDetectionService.class);
        final CrawlerConfig crawlerConfig = new CrawlerConfig(null, 30L, "", List.of());

        final SchedulerService schedulerService =
                new SchedulerService(mockCrawlerService, crawlerConfig, mockAntiDetectionService);

        assertThat(schedulerService.isEnabled()).isFalse();
    }

    /**
     * 유효하지 않은 cron 표현식을 생성하는 Arbitrary provider.
     *
     * <p>빈 문자열, 공백 문자열, 파싱 불가능한 문자열, 필드 수가 부족한 문자열 등 다양한 유형의 무효한 cron 표현식을 생성합니다.
     *
     * @return 유효하지 않은 cron 문자열의 Arbitrary
     */
    @Provide
    Arbitrary<String> invalidCronExpressions() {
        final Arbitrary<String> emptyStrings = Arbitraries.of("", "   ", "\t", "\n", "  \t\n  ");

        final Arbitrary<String> unparseableStrings =
                Arbitraries.of(
                        "invalid",
                        "not-a-cron",
                        "* * *",
                        "abc def ghi",
                        "1 2 3",
                        "0 0 0",
                        "hello world",
                        "12345",
                        "*/invalid * * * * *",
                        "0 0 32 13 8 *",
                        "random-text-here",
                        "@ @ @ @ @ @");

        final Arbitrary<String> randomGarbage =
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);

        return Arbitraries.oneOf(emptyStrings, unparseableStrings, randomGarbage);
    }

    // ========================================================================
    // Property 5-8: 스케줄러 개별 실행 방식 검증
    // ========================================================================

    /**
     * N개 유효 타겟에 대해 executeSingle이 정확히 N번 호출됨을 검증합니다.
     *
     * <p>SchedulerService의 executeCrawl 메서드가 validTargets 목록의 각 타겟에 대해
     * CrawlerService.executeSingle을 정확히 한 번씩 호출하는지 확인합니다.
     *
     * <p><b>Validates: Requirements 6.1</b>
     *
     * @param targetCount 생성할 타겟 수 (1~10)
     */
    @Property(tries = 100)
    void schedulerShouldCallExecuteSingleForEachTarget(
            @ForAll("targetCounts") final int targetCount) throws Exception {

        final CrawlerService mockCrawlerService = mock(CrawlerService.class);
        final AntiDetectionService mockAntiDetectionService = mock(AntiDetectionService.class);
        final List<CrawlerConfig.TargetConfig> targets = generateValidTargets(targetCount);
        final CrawlerConfig config = new CrawlerConfig("0 0 */6 * * *", 30L, "", targets);
        final SchedulerService schedulerService =
                new SchedulerService(mockCrawlerService, config, mockAntiDetectionService);

        when(mockAntiDetectionService.randomInterTargetDelay()).thenReturn(0L);

        invokeExecuteCrawl(schedulerService);

        verify(mockCrawlerService, times(targetCount))
                .executeSingle(anyString(), eq(TriggerSource.SCHEDULED));
        for (final CrawlerConfig.TargetConfig target : targets) {
            verify(mockCrawlerService, times(1))
                    .executeSingle(target.name(), TriggerSource.SCHEDULED);
        }
    }

    /**
     * 일부 타겟이 실패(예외 또는 null 반환)해도 나머지 타겟이 모두 처리됨을 검증합니다.
     *
     * <p>SchedulerService는 개별 타겟 실행 실패를 격리하여 나머지 타겟의 크롤링을 계속 진행해야 합니다.
     *
     * <p><b>Validates: Requirements 6.2</b>
     *
     * @param targetCount 생성할 타겟 수 (2~10)
     */
    @Property(tries = 100)
    void schedulerShouldContinueProcessingWhenSomeTargetsFail(
            @ForAll("targetCountsForFailure") final int targetCount) throws Exception {

        final CrawlerService mockCrawlerService = mock(CrawlerService.class);
        final AntiDetectionService mockAntiDetectionService = mock(AntiDetectionService.class);
        final List<CrawlerConfig.TargetConfig> targets = generateValidTargets(targetCount);
        final CrawlerConfig config = new CrawlerConfig("0 0 */6 * * *", 30L, "", targets);
        final SchedulerService schedulerService =
                new SchedulerService(mockCrawlerService, config, mockAntiDetectionService);

        when(mockAntiDetectionService.randomInterTargetDelay()).thenReturn(0L);

        // 첫 번째 타겟은 예외 발생
        when(mockCrawlerService.executeSingle(targets.get(0).name(), TriggerSource.SCHEDULED))
                .thenThrow(new RuntimeException("테스트 실패"));

        // 두 번째 타겟(존재 시)은 null 반환
        if (targetCount > 1) {
            when(mockCrawlerService.executeSingle(targets.get(1).name(), TriggerSource.SCHEDULED))
                    .thenReturn(null);
        }

        invokeExecuteCrawl(schedulerService);

        // 모든 타겟에 대해 executeSingle이 호출되어야 함
        verify(mockCrawlerService, times(targetCount))
                .executeSingle(anyString(), eq(TriggerSource.SCHEDULED));
    }

    /**
     * N개 타겟 실행 시 randomInterTargetDelay가 정확히 N-1번 호출됨을 검증합니다.
     *
     * <p>타겟 간 딜레이는 마지막 타겟 이후에는 적용되지 않으므로, N개의 타겟에 대해 N-1번 호출되어야 합니다.
     *
     * <p><b>Validates: Requirements 6.3</b>
     *
     * @param targetCount 생성할 타겟 수 (1~10)
     */
    @Property(tries = 100)
    void schedulerShouldApplyInterTargetDelayNMinusOneTimes(
            @ForAll("targetCounts") final int targetCount) throws Exception {

        final CrawlerService mockCrawlerService = mock(CrawlerService.class);
        final AntiDetectionService mockAntiDetectionService = mock(AntiDetectionService.class);
        final List<CrawlerConfig.TargetConfig> targets = generateValidTargets(targetCount);
        final CrawlerConfig config = new CrawlerConfig("0 0 */6 * * *", 30L, "", targets);
        final SchedulerService schedulerService =
                new SchedulerService(mockCrawlerService, config, mockAntiDetectionService);

        when(mockAntiDetectionService.randomInterTargetDelay()).thenReturn(0L);

        invokeExecuteCrawl(schedulerService);

        final int expectedDelayCalls = Math.max(0, targetCount - 1);
        verify(mockAntiDetectionService, times(expectedDelayCalls)).randomInterTargetDelay();
    }

    /**
     * scheduledRunning이 true일 때 후속 executeCrawl 호출이 타겟을 처리하지 않음을 검증합니다.
     *
     * <p>스케줄러 중복 실행을 방지하기 위해, scheduledRunning이 이미 true인 상태에서는 executeSingle이 한 번도 호출되지 않아야 합니다.
     *
     * <p><b>Validates: Requirements 6.1</b>
     *
     * @param targetCount 생성할 타겟 수 (1~10)
     */
    @Property(tries = 100)
    void schedulerShouldNotProcessTargetsWhenAlreadyRunning(
            @ForAll("targetCounts") final int targetCount) throws Exception {

        final CrawlerService mockCrawlerService = mock(CrawlerService.class);
        final AntiDetectionService mockAntiDetectionService = mock(AntiDetectionService.class);
        final List<CrawlerConfig.TargetConfig> targets = generateValidTargets(targetCount);
        final CrawlerConfig config = new CrawlerConfig("0 0 */6 * * *", 30L, "", targets);
        final SchedulerService schedulerService =
                new SchedulerService(mockCrawlerService, config, mockAntiDetectionService);

        setScheduledRunning(schedulerService, true);

        invokeExecuteCrawl(schedulerService);

        verify(mockCrawlerService, never()).executeSingle(anyString(), any());
        verify(mockAntiDetectionService, never()).randomInterTargetDelay();
    }

    /**
     * 1~10 범위의 타겟 수를 생성하는 Arbitrary provider.
     *
     * @return 1~10 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> targetCounts() {
        return Arbitraries.integers().between(1, 10);
    }

    /**
     * 2~10 범위의 타겟 수를 생성하는 Arbitrary provider.
     *
     * <p>장애 격리 테스트에서는 최소 2개 타겟이 필요합니다.
     *
     * @return 2~10 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> targetCountsForFailure() {
        return Arbitraries.integers().between(2, 10);
    }

    private List<CrawlerConfig.TargetConfig> generateValidTargets(final int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(
                        i ->
                                new CrawlerConfig.TargetConfig(
                                        "target-" + i, "https://example.com/target" + i))
                .collect(Collectors.toList());
    }

    private void invokeExecuteCrawl(final SchedulerService schedulerService) throws Exception {
        final Method executeCrawlMethod = SchedulerService.class.getDeclaredMethod("executeCrawl");
        executeCrawlMethod.setAccessible(true);
        executeCrawlMethod.invoke(schedulerService);
    }

    private void setScheduledRunning(final SchedulerService schedulerService, final boolean value)
            throws Exception {
        final Field scheduledRunningField =
                SchedulerService.class.getDeclaredField("scheduledRunning");
        scheduledRunningField.setAccessible(true);
        final AtomicBoolean scheduledRunning =
                (AtomicBoolean) scheduledRunningField.get(schedulerService);
        scheduledRunning.set(value);
    }
}
