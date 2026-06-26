package com.myapps.web.mycrawler.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 크롤링 실행 결과를 나타내는 불변 값 객체.
 *
 * <p>크롤링 대상 정보, 실행 상태, 콘텐츠, 에러 메시지, 실행 시간 등을 포함합니다.
 *
 * @param targetName    크롤링 대상의 식별 이름
 * @param targetUrl     크롤링한 대상 URL
 * @param status        크롤링 실행 결과 상태
 * @param triggerSource 크롤링 트리거 출처
 * @param content       크롤링된 페이지 콘텐츠 (실패 시 null 가능)
 * @param errorMessage  에러 메시지 (성공 시 null 가능)
 * @param startTime     크롤링 시작 시각
 * @param endTime       크롤링 종료 시각
 */
public record CrawlResult(
    String targetName,
    String targetUrl,
    CrawlStatus status,
    TriggerSource triggerSource,
    String content,
    String errorMessage,
    LocalDateTime startTime,
    LocalDateTime endTime
) {

    /**
     * 크롤링 소요 시간을 밀리초 단위로 반환합니다.
     *
     * @return 시작 시각부터 종료 시각까지의 소요 시간 (밀리초)
     */
    public long durationMillis() {
        return Duration.between(startTime, endTime).toMillis();
    }

    /**
     * 크롤링된 콘텐츠의 요약을 반환합니다.
     *
     * <p>콘텐츠가 null이거나 지정된 최대 길이 이하이면 원본을 그대로 반환하고,
     * 초과하면 최대 길이까지 잘라서 반환합니다.
     *
     * @param maxLength 요약 최대 문자 길이
     * @return 최대 길이 이하로 잘린 콘텐츠 요약, 또는 null
     */
    public String contentSummary(final int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength);
    }
}
