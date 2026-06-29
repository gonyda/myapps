package com.myapps.web.mycrawler.application.service;

import java.time.LocalDateTime;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.myapps.web.mycrawler.domain.model.CrawlResult;
import com.myapps.web.mycrawler.domain.model.CrawlStatus;
import com.myapps.web.mycrawler.domain.model.CrawlTarget;
import com.myapps.web.mycrawler.domain.model.TriggerSource;
import com.myapps.web.mycrawler.infrastructure.config.CrawlerConfig;
import com.myapps.web.mycrawler.infrastructure.crawler.CrawlerEngine;

/**
 * 크롤링 실행의 오케스트레이션 서비스.
 *
 * <p>지정된 타겟에 대해 개별 크롤링을 수행하고,
 * 최근 결과를 메모리에 보관합니다. 중복 실행을 방지하며,
 * 타겟 실패를 안전하게 처리합니다.
 */
@Service
public class CrawlerService {

    private static final Logger log = LoggerFactory.getLogger(CrawlerService.class);

    private static final int MAX_RECENT_RESULTS = 20;

    private final CrawlerEngine crawlerEngine;
    private final CrawlerConfig crawlerConfig;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Deque<CrawlResult> recentResults = new ConcurrentLinkedDeque<>();

    /**
     * CrawlerService 인스턴스를 생성합니다.
     *
     * @param crawlerEngine 크롤링 실행 엔진
     * @param crawlerConfig 크롤러 설정
     */
    public CrawlerService(final CrawlerEngine crawlerEngine,
                          final CrawlerConfig crawlerConfig) {
        this.crawlerEngine = crawlerEngine;
        this.crawlerConfig = crawlerConfig;
    }

    /**
     * 특정 타겟에 대해 크롤링을 실행합니다.
     *
     * <p>지정된 이름의 타겟을 유효 타겟 목록에서 찾아 크롤링합니다.
     * 이미 실행 중이거나 타겟을 찾을 수 없는 경우 null을 반환합니다.
     *
     * @param targetName    크롤링할 타겟의 이름
     * @param triggerSource 크롤링 트리거 출처
     * @return 크롤링 결과, 실행 불가 시 null
     */
    public CrawlResult executeSingle(final String targetName, final TriggerSource triggerSource) {
        if (!running.compareAndSet(false, true)) {
            log.warn("크롤링이 이미 실행 중입니다. 단일 실행 요청을 무시합니다. targetName={}, triggerSource={}",
                    targetName, triggerSource);
            return null;
        }

        try {
            return performSingleCrawl(targetName, triggerSource);
        } finally {
            running.set(false);
        }
    }

    /**
     * 최근 크롤링 결과 목록을 시간 역순으로 반환합니다.
     *
     * <p>최대 20건까지 유지됩니다.
     *
     * @return 최근 크롤링 결과 목록 (시간 역순)
     */
    public List<CrawlResult> getRecentResults() {
        return List.copyOf(recentResults);
    }

    /**
     * 현재 크롤링이 실행 중인지 반환합니다.
     *
     * @return 실행 중이면 true, 아니면 false
     */
    public boolean isRunning() {
        return running.get();
    }

    private CrawlResult performSingleCrawl(final String targetName, final TriggerSource triggerSource) {
        final List<CrawlTarget> targets = crawlerConfig.validTargets();
        final CrawlTarget target = findTargetByName(targets, targetName);

        if (target == null) {
            log.warn("크롤링 대상을 찾을 수 없습니다. targetName={}", targetName);
            return null;
        }

        log.info("단일 크롤링 시작. targetName={}, triggerSource={}", targetName, triggerSource);
        final CrawlResult result = crawlTargetSafely(target, triggerSource);
        addToRecentResults(result);
        return result;
    }

    private CrawlResult crawlTargetSafely(final CrawlTarget target, final TriggerSource triggerSource) {
        try {
            return crawlerEngine.crawl(target, triggerSource);
        } catch (final Exception exception) {
            log.error("크롤링 중 예기치 않은 오류 발생. targetName={}, error={}",
                    target.name(), exception.getMessage(), exception);
            return createFailureResult(target, triggerSource, exception);
        }
    }

    private CrawlResult createFailureResult(final CrawlTarget target, final TriggerSource triggerSource, final Exception exception) {
        final LocalDateTime now = LocalDateTime.now();
        return new CrawlResult(
                target.name(),
                target.url(),
                CrawlStatus.FAILURE,
                triggerSource,
                null,
                exception.getMessage(),
                now,
                now
        );
    }

    private void addToRecentResults(final CrawlResult result) {
        recentResults.addFirst(result);
        while (recentResults.size() > MAX_RECENT_RESULTS) {
            recentResults.removeLast();
        }
    }

    private CrawlTarget findTargetByName(final List<CrawlTarget> targets, final String targetName) {
        for (final CrawlTarget target : targets) {
            if (target.name().equals(targetName)) {
                return target;
            }
        }
        return null;
    }
}
