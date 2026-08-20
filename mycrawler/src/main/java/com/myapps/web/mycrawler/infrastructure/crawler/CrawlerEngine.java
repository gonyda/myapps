package com.myapps.web.mycrawler.infrastructure.crawler;

import com.myapps.web.mycrawler.domain.model.CrawlResult;
import com.myapps.web.mycrawler.domain.model.CrawlTarget;
import com.myapps.web.mycrawler.domain.model.TriggerSource;

/**
 * 웹 크롤링 실행 엔진 인터페이스.
 *
 * <p>주어진 크롤링 대상에 대해 페이지를 로드하고 콘텐츠를 수집하여 크롤링 결과를 반환하는 책임을 가집니다.
 */
public interface CrawlerEngine {

    /**
     * 지정된 크롤링 대상에 대해 크롤링을 수행합니다.
     *
     * @param target 크롤링할 대상 정보
     * @param triggerSource 크롤링 트리거 출처
     * @return 크롤링 실행 결과
     */
    CrawlResult crawl(CrawlTarget target, TriggerSource triggerSource);
}
