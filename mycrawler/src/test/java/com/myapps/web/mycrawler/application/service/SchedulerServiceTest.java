package com.myapps.web.mycrawler.application.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.myapps.web.mycrawler.infrastructure.config.CrawlerConfig;

/**
 * {@link SchedulerService}에 대한 단위 테스트.
 */
class SchedulerServiceTest {

    private final CrawlerService crawlerService = mock(CrawlerService.class);

    @Test
    @DisplayName("유효한 cron 표현식이면 스케줄링이 활성화된다")
    void should_enableScheduling_when_cronExpressionIsValid() {
        // given
        final CrawlerConfig config = new CrawlerConfig(
                "0 0 */6 * * *", 30L, "", List.of());

        // when
        final SchedulerService schedulerService = new SchedulerService(crawlerService, config);

        // then
        assertThat(schedulerService.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("null cron 표현식이면 스케줄링이 비활성화된다")
    void should_disableScheduling_when_cronExpressionIsNull() {
        // given
        final CrawlerConfig config = new CrawlerConfig(
                null, 30L, "", List.of());

        // when
        final SchedulerService schedulerService = new SchedulerService(crawlerService, config);

        // then
        assertThat(schedulerService.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("빈 cron 표현식이면 스케줄링이 비활성화된다")
    void should_disableScheduling_when_cronExpressionIsBlank() {
        // given
        final CrawlerConfig config = new CrawlerConfig(
                "  ", 30L, "", List.of());

        // when
        final SchedulerService schedulerService = new SchedulerService(crawlerService, config);

        // then
        assertThat(schedulerService.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("유효하지 않은 cron 표현식이면 스케줄링이 비활성화된다")
    void should_disableScheduling_when_cronExpressionIsInvalid() {
        // given
        final CrawlerConfig config = new CrawlerConfig(
                "invalid-cron", 30L, "", List.of());

        // when
        final SchedulerService schedulerService = new SchedulerService(crawlerService, config);

        // then
        assertThat(schedulerService.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("활성화된 스케줄러는 다음 실행 시각을 KST 포맷 문자열로 반환한다")
    void should_returnNextExecutionTime_when_schedulerIsEnabled() {
        // given
        final CrawlerConfig config = new CrawlerConfig(
                "0 0 */6 * * *", 30L, "", List.of());
        final SchedulerService schedulerService = new SchedulerService(crawlerService, config);

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
        final CrawlerConfig config = new CrawlerConfig(
                null, 30L, "", List.of());
        final SchedulerService schedulerService = new SchedulerService(crawlerService, config);

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
        final CrawlerConfig config = new CrawlerConfig(
                expectedCron, 30L, "", List.of());
        final SchedulerService schedulerService = new SchedulerService(crawlerService, config);

        // when
        final String result = schedulerService.getCronExpression();

        // then
        assertThat(result).isEqualTo(expectedCron);
    }
}
