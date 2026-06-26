package com.myapps.web.mycrawler.application.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.myapps.web.mycrawler.domain.model.TriggerSource;
import com.myapps.web.mycrawler.infrastructure.config.CrawlerConfig;

/**
 * 크롤링 스케줄링을 관리하는 서비스.
 *
 * <p>{@link SchedulingConfigurer}를 구현하여 매 트리거 시점마다
 * cron 표현식을 재평가합니다. 이를 통해 애플리케이션 재시작 없이
 * 설정 변경을 반영할 수 있습니다.
 *
 * <p>시작 시점에 cron 표현식의 유효성을 검증하며,
 * 유효하지 않은 경우 에러 로그를 출력하고 스케줄링을 비활성화한 채로
 * 애플리케이션을 정상 기동합니다.
 */
@Service
public class SchedulerService implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);
    private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter KST_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CrawlerService crawlerService;
    private final CrawlerConfig crawlerConfig;
    private final boolean enabled;

    /**
     * SchedulerService 인스턴스를 생성합니다.
     *
     * <p>생성 시점에 cron 표현식의 유효성을 검증하여
     * 스케줄링 활성화 여부를 결정합니다.
     *
     * @param crawlerService 크롤링 실행 서비스
     * @param crawlerConfig  크롤러 설정
     */
    public SchedulerService(final CrawlerService crawlerService,
                            final CrawlerConfig crawlerConfig) {
        this.crawlerService = crawlerService;
        this.crawlerConfig = crawlerConfig;
        this.enabled = validateCronExpression(crawlerConfig.cron());
    }

    /**
     * 스케줄링 태스크를 등록합니다.
     *
     * <p>매 트리거 시점마다 cron 표현식을 재평가하는 트리거 태스크를 등록합니다.
     * 스케줄링이 비활성화된 경우 태스크를 등록하지 않습니다.
     *
     * @param taskRegistrar 스케줄 태스크 등록기
     */
    @Override
    public void configureTasks(final ScheduledTaskRegistrar taskRegistrar) {
        if (!enabled) {
            log.info("스케줄링이 비활성화되어 태스크를 등록하지 않습니다.");
            return;
        }

        taskRegistrar.addTriggerTask(
                this::executeCrawl,
                triggerContext -> {
                    final String cronExpression = crawlerConfig.cron();
                    final CronTrigger cronTrigger = new CronTrigger(cronExpression);
                    return cronTrigger.nextExecution(triggerContext);
                }
        );
    }

    /**
     * 스케줄링이 활성화되어 있는지 반환합니다.
     *
     * @return 스케줄링이 활성화되어 있으면 true, 아니면 false
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 다음 실행 예정 시각을 한국 시간(KST) 포맷 문자열로 반환합니다.
     *
     * <p>현재 설정된 cron 표현식 기준으로 다음 실행 시각을 계산하여
     * "yyyy-MM-dd HH:mm:ss" 형식의 KST 문자열로 반환합니다.
     * 스케줄링이 비활성화되었거나 cron이 유효하지 않으면 빈 Optional을 반환합니다.
     *
     * @return 다음 실행 예정 시각(KST 포맷 문자열), 계산 불가 시 빈 Optional
     */
    public Optional<String> getNextExecutionTime() {
        if (!enabled) {
            return Optional.empty();
        }

        final String cronExpression = crawlerConfig.cron();
        if (!isValidCron(cronExpression)) {
            return Optional.empty();
        }

        final CronExpression parsed = CronExpression.parse(cronExpression);
        final ZonedDateTime now = ZonedDateTime.now(ZONE_KST);
        final ZonedDateTime next = parsed.next(now);
        if (next == null) {
            return Optional.empty();
        }
        return Optional.of(next.format(KST_FORMATTER));
    }

    /**
     * 현재 설정된 cron 표현식을 반환합니다.
     *
     * @return cron 표현식 문자열, null일 수 있음
     */
    public String getCronExpression() {
        return crawlerConfig.cron();
    }

    private void executeCrawl() {
        log.info("스케줄에 의한 크롤링 실행을 시작합니다. cron={}", crawlerConfig.cron());
        crawlerService.executeAll(TriggerSource.SCHEDULED);
    }

    private boolean validateCronExpression(final String cron) {
        if (!isValidCron(cron)) {
            log.error("cron 표현식이 누락되었거나 유효하지 않습니다. cron={} — 스케줄링을 비활성화합니다.", cron);
            return false;
        }

        log.info("스케줄링이 활성화되었습니다. cron={}", cron);
        return true;
    }

    private boolean isValidCron(final String cron) {
        if (cron == null || cron.isBlank()) {
            return false;
        }
        return CronExpression.isValidExpression(cron);
    }
}
