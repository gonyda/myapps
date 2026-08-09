package com.myapps.web.myrpg.domain.model;

import java.util.Map;

/**
 * 방어스킬(DEFENSE) 카탈로그 항목을 나타내는 불변 record.
 *
 * <p>각 랭크({@link SkillRank})에 대한 피해 경감률(%)과 반격 배율(%)을
 * 각각 {@link #blockRateByRank()}와 {@link #counterMultiplierByRank()}로 보유한다.
 * 랭크가 오를수록 두 값 모두 단조 증가하며, 맵에는 반드시 16개 랭크 키가 모두 포함되어야 한다.
 *
 * @param id                       스킬 고유 식별자
 * @param label                    표시용 라벨
 * @param type                     스킬 타입 (DEFENSE)
 * @param talent                   재능 분류
 * @param resourceCost             자원 소모량 (랭크 무관 고정)
 * @param blockRateByRank          랭크별 피해 경감률(%) 맵 (16키)
 * @param counterMultiplierByRank  랭크별 반격 배율(%) 맵 (16키)
 * @param effectSummary            효과 요약 문자열
 */
public record DefenseSkill(
        String id,
        String label,
        SkillType type,
        SkillTalent talent,
        int resourceCost,
        Map<SkillRank, Integer> blockRateByRank,
        Map<SkillRank, Integer> counterMultiplierByRank,
        String effectSummary
) implements Skill {
}
