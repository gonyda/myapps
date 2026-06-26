package com.myapps.web.mycrawler.domain.model;

/**
 * 크롤링 대상을 나타내는 불변 값 객체.
 *
 * <p>크롤링할 대상의 이름과 URL을 포함합니다.
 *
 * @param name 크롤링 대상의 식별 이름
 * @param url  크롤링할 대상 URL
 */
public record CrawlTarget(
    String name,
    String url
) {
}
