package com.myapps.web.mycrawler.application.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.myapps.web.mycrawler.domain.model.CrawlResult;
import com.myapps.web.mycrawler.domain.model.CrawlTarget;
import com.myapps.web.mycrawler.domain.model.TriggerSource;
import com.myapps.web.mycrawler.infrastructure.antidetect.AntiDetectionService;
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
    private final AntiDetectionService antiDetectionService;
    private final AtomicBoolean scheduledRunning = new AtomicBoolean(false);
    private final boolean enabled;

    /**
     * SchedulerService 인스턴스를 생성합니다.
     *
     * <p>생성 시점에 cron 표현식의 유효성을 검증하여
     * 스케줄링 활성화 여부를 결정합니다.
     *
     * @param crawlerService       크롤링 실행 서비스
     * @param crawlerConfig        크롤러 설정
     * @param antiDetectionService 봇 탐지 회피 서비스
     */
    public SchedulerService(final CrawlerService crawlerService,
                            final CrawlerConfig crawlerConfig,
                            final AntiDetectionService antiDetectionService) {
        this.crawlerService = crawlerService;
        this.crawlerConfig = crawlerConfig;
        this.antiDetectionService = antiDetectionService;
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
     * 스케줄러에 의한 크롤링이 현재 실행 중인지 반환합니다.
     *
     * <p>스케줄 실행 도중 수동 실행이 끼어드는 것을 방지하기 위해 사용합니다.
     *
     * @return 스케줄 크롤링이 실행 중이면 true, 아니면 false
     */
    public boolean isScheduledRunning() {
        return scheduledRunning.get();
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
        if (!scheduledRunning.compareAndSet(false, true)) {
            log.warn("스케줄 크롤링이 이미 실행 중입니다. 중복 실행 요청을 무시합니다.");
            return;
        }

        try {
            log.info("스케줄에 의한 크롤링 실행을 시작합니다. cron={}", crawlerConfig.cron());
            final List<CrawlTarget> targets = crawlerConfig.validTargets();

            for (int i = 0; i < targets.size(); i++) {
                final CrawlTarget target = targets.get(i);
                executeSingleTarget(target);
                applyInterTargetDelayIfNotLast(i, targets.size());
            }

            log.info("스케줄 크롤링이 완료되었습니다. 처리된 타겟 수={}", targets.size());
        } finally {
            scheduledRunning.set(false);
        }
    }

    private void executeSingleTarget(final CrawlTarget target) {
        try {
            final CrawlResult result = crawlerService.executeSingle(target.name(), TriggerSource.SCHEDULED);
            if (result == null) {
                log.error("스케줄 크롤링 결과가 null입니다. targetName={}", target.name());
            }
        } catch (final Exception exception) {
            log.error("스케줄 크롤링 중 예외 발생. targetName={}, error={}",
                    target.name(), exception.getMessage(), exception);
        }
    }

    private void applyInterTargetDelayIfNotLast(final int currentIndex, final int totalTargets) {
        final boolean isLastTarget = currentIndex >= totalTargets - 1;
        if (isLastTarget) {
            return;
        }

        final long delay = antiDetectionService.randomInterTargetDelay();
        log.info("  ⏳ 타겟 간 딜레이 적용: {}ms (다음 타겟까지 대기 중...)", delay);
        try {
            Thread.sleep(delay);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("스케줄 크롤링 타겟 간 딜레이가 인터럽트되었습니다.");
        }
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
