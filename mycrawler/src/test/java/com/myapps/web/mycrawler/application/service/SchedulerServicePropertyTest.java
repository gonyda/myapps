package com.myapps.web.mycrawler.application.service;

import java.util.List;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitraries;

import com.myapps.web.mycrawler.infrastructure.config.CrawlerConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * SchedulerService의 유효하지 않은 cron 표현식 처리에 대한 Property-Based 테스트.
 *
 * <p>jqwik을 사용하여 다양한 유효하지 않은 cron 문자열에 대해
 * 스케줄러가 비활성화됨을 검증합니다.
 *
 * <p><b>Validates: Requirements 3.5</b>
 */
class SchedulerServicePropertyTest {

    /**
     * 유효하지 않은 cron 표현식으로 SchedulerService를 생성하면
     * isEnabled()가 false를 반환함을 검증합니다.
     *
     * <p>null, 빈 문자열, 공백만 포함된 문자열, 파싱 불가능한 문자열 등
     * 다양한 유효하지 않은 cron 입력에 대해 스케줄링이 비활성화됨을 확인합니다.
     *
     * <p><b>Validates: Requirements 3.5</b>
     *
     * @param invalidCron 유효하지 않은 cron 표현식
     */
    @Property(tries = 100)
    void schedulerShouldBeDisabledForInvalidCronExpression(
            @ForAll("invalidCronExpressions") final String invalidCron) {

        final CrawlerService mockCrawlerService = mock(CrawlerService.class);
        final CrawlerConfig crawlerConfig = new CrawlerConfig(invalidCron, 30L, "", List.of());

        final SchedulerService schedulerService = new SchedulerService(mockCrawlerService, crawlerConfig);

        assertThat(schedulerService.isEnabled()).isFalse();
    }

    /**
     * null cron 표현식으로 SchedulerService를 생성하면
     * isEnabled()가 false를 반환함을 검증합니다.
     *
     * <p><b>Validates: Requirements 3.5</b>
     */
    @Property(tries = 10)
    void schedulerShouldBeDisabledForNullCron() {
        final CrawlerService mockCrawlerService = mock(CrawlerService.class);
        final CrawlerConfig crawlerConfig = new CrawlerConfig(null, 30L, "", List.of());

        final SchedulerService schedulerService = new SchedulerService(mockCrawlerService, crawlerConfig);

        assertThat(schedulerService.isEnabled()).isFalse();
    }

    /**
     * 유효하지 않은 cron 표현식을 생성하는 Arbitrary provider.
     *
     * <p>빈 문자열, 공백 문자열, 파싱 불가능한 문자열, 필드 수가 부족한 문자열 등
     * 다양한 유형의 무효한 cron 표현식을 생성합니다.
     *
     * @return 유효하지 않은 cron 문자열의 Arbitrary
     */
    @Provide
    Arbitrary<String> invalidCronExpressions() {
        final Arbitrary<String> emptyStrings = Arbitraries.of("", "   ", "\t", "\n", "  \t\n  ");

        final Arbitrary<String> unparseableStrings = Arbitraries.of(
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
                "@ @ @ @ @ @"
        );

        final Arbitrary<String> randomGarbage = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20);

        return Arbitraries.oneOf(emptyStrings, unparseableStrings, randomGarbage);
    }
}
