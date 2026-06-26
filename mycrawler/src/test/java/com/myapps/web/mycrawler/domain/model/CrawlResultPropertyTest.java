package com.myapps.web.mycrawler.domain.model;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CrawlResult 도메인 모델의 구조적 무결성을 검증하는 Property-Based 테스트.
 *
 * <p>jqwik을 사용하여 임의의 CrawlResult 인스턴스를 생성하고,
 * 도메인 불변식이 모든 유효한 입력에 대해 성립함을 검증합니다.
 *
 * <p><b>Validates: Requirements 2.3, 2.4</b>
 */
class CrawlResultPropertyTest {

    /**
     * status, triggerSource, startTime, endTime 필드가 non-null인 CrawlResult에 대해
     * 해당 필드들이 항상 non-null임을 검증합니다.
     *
     * @param result 임의로 생성된 CrawlResult 인스턴스
     */
    @Property(tries = 100)
    void crawlResultFieldsMustBeNonNull(@ForAll("validCrawlResults") final CrawlResult result) {
        assertThat(result.status()).isNotNull();
        assertThat(result.triggerSource()).isNotNull();
        assertThat(result.startTime()).isNotNull();
        assertThat(result.endTime()).isNotNull();
    }

    /**
     * endTime이 startTime 이후이므로 durationMillis()가 항상 0 이상임을 검증합니다.
     *
     * @param result 임의로 생성된 CrawlResult 인스턴스
     */
    @Property(tries = 100)
    void endTimeMustBeGreaterThanOrEqualToStartTime(@ForAll("validCrawlResults") final CrawlResult result) {
        assertThat(result.durationMillis()).isGreaterThanOrEqualTo(0L);
    }

    /**
     * status가 FAILURE인 경우 errorMessage가 non-null이고 비어있지 않음을 검증합니다.
     *
     * @param result status=FAILURE인 임의의 CrawlResult 인스턴스
     */
    @Property(tries = 100)
    void failureStatusMustHaveNonEmptyErrorMessage(@ForAll("failureCrawlResults") final CrawlResult result) {
        assertThat(result.status()).isEqualTo(CrawlStatus.FAILURE);
        assertThat(result.errorMessage()).isNotNull();
        assertThat(result.errorMessage().isBlank()).isFalse();
    }

    /**
     * endTime >= startTime 불변식을 만족하는 유효한 CrawlResult 인스턴스를 생성하는 제공자.
     *
     * @return CrawlResult Arbitrary
     */
    @Provide
    Arbitrary<CrawlResult> validCrawlResults() {
        final Arbitrary<String> targetNames = Arbitraries.strings()
                .alpha().ofMinLength(1).ofMaxLength(30);
        final Arbitrary<String> targetUrls = Arbitraries.strings()
                .alpha().ofMinLength(5).ofMaxLength(50)
                .map(s -> "https://" + s + ".com");
        final Arbitrary<CrawlStatus> statuses = Arbitraries.of(CrawlStatus.values());
        final Arbitrary<TriggerSource> triggerSources = Arbitraries.of(TriggerSource.values());
        final Arbitrary<String> contents = Arbitraries.strings().ofMaxLength(200).injectNull(0.3);
        final Arbitrary<String> errorMessages = Arbitraries.strings().ofMaxLength(100).injectNull(0.5);
        final Arbitrary<LocalDateTime> startTimes = Arbitraries.integers()
                .between(2020, 2025)
                .flatMap(year -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .flatMap(day -> Arbitraries.integers().between(0, 23)
                                        .flatMap(hour -> Arbitraries.integers().between(0, 59)
                                                .map(minute -> LocalDateTime.of(year, month, day, hour, minute, 0))))));
        final Arbitrary<Integer> durationSeconds = Arbitraries.integers().between(0, 3600);

        return Combinators.combine(targetNames, targetUrls, statuses, triggerSources, contents, errorMessages, startTimes, durationSeconds)
                .as((targetName, targetUrl, status, triggerSource, content, errorMessage, startTime, durationSec) ->
                        new CrawlResult(
                                targetName,
                                targetUrl,
                                status,
                                triggerSource,
                                content,
                                errorMessage,
                                startTime,
                                startTime.plusSeconds(durationSec)
                        ));
    }

    /**
     * status=FAILURE이고 errorMessage가 non-empty인 CrawlResult 인스턴스를 생성하는 제공자.
     *
     * @return FAILURE 상태의 CrawlResult Arbitrary
     */
    @Provide
    Arbitrary<CrawlResult> failureCrawlResults() {
        final Arbitrary<String> targetNames = Arbitraries.strings()
                .alpha().ofMinLength(1).ofMaxLength(30);
        final Arbitrary<String> targetUrls = Arbitraries.strings()
                .alpha().ofMinLength(5).ofMaxLength(50)
                .map(s -> "https://" + s + ".com");
        final Arbitrary<TriggerSource> triggerSources = Arbitraries.of(TriggerSource.values());
        final Arbitrary<String> errorMessages = Arbitraries.strings()
                .alpha().ofMinLength(1).ofMaxLength(100);
        final Arbitrary<LocalDateTime> startTimes = Arbitraries.integers()
                .between(2020, 2025)
                .flatMap(year -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .flatMap(day -> Arbitraries.integers().between(0, 23)
                                        .flatMap(hour -> Arbitraries.integers().between(0, 59)
                                                .map(minute -> LocalDateTime.of(year, month, day, hour, minute, 0))))));
        final Arbitrary<Integer> durationSeconds = Arbitraries.integers().between(0, 3600);

        return Combinators.combine(targetNames, targetUrls, triggerSources, errorMessages, startTimes, durationSeconds)
                .as((targetName, targetUrl, triggerSource, errorMessage, startTime, durationSec) ->
                        new CrawlResult(
                                targetName,
                                targetUrl,
                                CrawlStatus.FAILURE,
                                triggerSource,
                                null,
                                errorMessage,
                                startTime,
                                startTime.plusSeconds(durationSec)
                        ));
    }
}
