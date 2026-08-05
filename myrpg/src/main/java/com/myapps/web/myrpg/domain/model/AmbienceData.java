package com.myapps.web.myrpg.domain.model;

import java.util.List;
import java.util.Map;

/**
 * 상황 멘트 데이터를 나타내는 불변 레코드 계층.
 *
 * <p>{@code classpath:data/ambience.json}에서 파싱되어 메모리에 보관된다.
 * 계절·시간대 매핑과 테마별 멘트 풀을 제공한다.
 *
 * @param season    계절 매핑 (계절 키 → 해당 월 목록, 예: "spring" → [3,4,5])
 * @param timeOfDay 시간대 매핑 (시간대 키 → 시간 범위, 예: "dawn" → {from:5, to:8})
 * @param themes    테마별 상황 멘트 풀 (themes[theme][season][timeOfDay] → 멘트 목록)
 */
public record AmbienceData(
        Map<String, List<Integer>> season,
        Map<String, TimeBucket> timeOfDay,
        Map<String, Map<String, Map<String, List<String>>>> themes
) {

    /**
     * 시간대의 시작·종료 시각(시 단위)을 나타내는 불변 레코드.
     *
     * <p>범위는 {@code [from, to)} 형태이며, 자정을 넘는 구간(예: late-night: 0~5)도 지원한다.
     *
     * @param from 시작 시각(포함, 0~23)
     * @param to   종료 시각(미포함, 1~24)
     */
    public record TimeBucket(int from, int to) {
    }
}
