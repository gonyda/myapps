package com.myapps.web.myrpg.domain.model;

import java.util.Map;

/**
 * 딜스킬(NORMAL/HEAVY) 카탈로그 항목을 나타내는 불변 record.
 *
 * <p>각 랭크({@link SkillRank})에 대한 데미지 배율(%)을 {@link #multiplierByRank()}로 보유하며,
 * 랭크가 오를수록 배율은 단조 증가한다. 맵에는 반드시 16개 랭크 키가 모두 포함되어야 한다.
 *
 * @param id              스킬 고유 식별자
 * @param label           표시용 라벨
 * @param type            스킬 타입 (NORMAL 또는 HEAVY)
 * @param talent          재능 분류
 * @param resourceCost    자원 소모량 (랭크 무관 고정)
 * @param multiplierByRank 랭크별 데미지 배율(%) 맵 (16키)
 * @param description     스킬 설명 문자열
 */
public record DamageSkill(
        String id,
        String label,
        SkillType type,
        SkillTalent talent,
        int resourceCost,
        Map<SkillRank, Integer> multiplierByRank,
        String description
) implements Skill {
}
