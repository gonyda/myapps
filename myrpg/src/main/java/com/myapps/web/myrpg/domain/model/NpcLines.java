package com.myapps.web.myrpg.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * NPC의 대사 풀을 나타내는 불변 레코드.
 *
 * <p>기본 대사 목록과 시간대별 추가 대사 맵을 보관한다.
 * 대사 선택 시 {@code defaultLines}와 현재 시간대에 해당하는
 * {@code byTime} 항목을 병합하여 후보 풀을 구성한다.
 *
 * @param defaultLines 시간대에 무관하게 항상 사용 가능한 기본 대사 목록 (JSON {@code lines.default} 매핑)
 * @param byTime       시간대 키({@link TimeOfDay#key()})를 키로, 해당 시간대 추가 대사 목록을 값으로 하는 맵
 */
public record NpcLines(
        @JsonProperty("default") List<String> defaultLines,
        Map<String, List<String>> byTime
) {
}
