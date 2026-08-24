package com.myapps.web.myrpg.domain.model;

import java.util.Map;

/**
 * 버프 스킬(BUFF - 마나 실드) 카탈로그 항목을 나타내는 불변 record.
 *
 * <p>지속 턴 수({@link #durationTurns()})와 랭크별 피해 감쇄율(%) 맵({@link #absorbRateByRank()})을 보유한다.
 *
 * @param id 스킬 고유 식별자
 * @param label 표시용 라벨
 * @param type 스킬 타입 (BUFF)
 * @param talent 재능 분류
 * @param resourceCost 자원 소모량
 * @param durationTurns 지속 턴 수
 * @param absorbRateByRank 랭크별 피해 감쇄율(%) 맵 (16키)
 * @param description 스킬 설명 문자열
 */
public record BuffSkill(
        String id,
        String label,
        SkillType type,
        SkillTalent talent,
        int resourceCost,
        int durationTurns,
        Map<SkillRank, Integer> absorbRateByRank,
        String description)
        implements Skill {

    /**
     * 지정된 랭크에서의 피해 감쇄율(%)을 반환한다.
     *
     * @param rank 스킬 랭크
     * @return 해당 랭크의 감쇄율(%)
     */
    public int absorbRateAt(final SkillRank rank) {
        if (absorbRateByRank != null && rank != null && absorbRateByRank.containsKey(rank)) {
            return absorbRateByRank.get(rank);
        }
        return 0;
    }
}
