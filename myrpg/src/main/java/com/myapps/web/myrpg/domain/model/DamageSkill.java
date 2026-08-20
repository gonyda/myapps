package com.myapps.web.myrpg.domain.model;

import java.util.Map;

/**
 * 딜스킬(NORMAL/HEAVY) 카탈로그 항목을 나타내는 불변 record.
 *
 * <p>각 랭크({@link SkillRank})에 대한 1히트당 데미지 배율(%)을 {@link #multiplierByRank()}로 보유하며, 랭크가 오를수록 배율은 단조
 * 증가한다. 맵에는 반드시 16개 랭크 키가 모두 포함되어야 한다.
 *
 * <p>{@link #hitCount()}는 스킬 사용 시 반복 타격 횟수(기본 1)를 나타내며, {@link #critBonus()}는 이 스킬 사용 시 크리티컬 확률에
 * 가산되는 보너스(기본 0, 상한 100)를 나타낸다.
 *
 * @param id 스킬 고유 식별자
 * @param label 표시용 라벨
 * @param type 스킬 타입 (NORMAL 또는 HEAVY)
 * @param talent 재능 분류
 * @param resourceCost 자원 소모량 (랭크 무관 고정)
 * @param multiplierByRank 랭크별 1히트당 데미지 배율(%) 맵 (16키, 단조 비감소)
 * @param description 스킬 설명 문자열
 * @param hitCount 스킬 사용 시 타격 횟수 (1 이상, 기본 1)
 * @param critBonus 크리티컬 확률 가산 보너스 (0~100, 기본 0)
 */
public record DamageSkill(
        String id,
        String label,
        SkillType type,
        SkillTalent talent,
        int resourceCost,
        Map<SkillRank, Integer> multiplierByRank,
        String description,
        int hitCount,
        int critBonus)
        implements Skill {

    /**
     * 하위호환 보조 생성자: hitCount=1, critBonus=0.
     *
     * <p>기존 7-인자 호출부 및 테스트를 변경 없이 유지하기 위한 생성자이다.
     *
     * @param id 스킬 고유 식별자
     * @param label 표시용 라벨
     * @param type 스킬 타입
     * @param talent 재능 분류
     * @param resourceCost 자원 소모량
     * @param multiplierByRank 랭크별 1히트당 데미지 배율(%) 맵
     * @param description 스킬 설명 문자열
     */
    public DamageSkill(
            final String id,
            final String label,
            final SkillType type,
            final SkillTalent talent,
            final int resourceCost,
            final Map<SkillRank, Integer> multiplierByRank,
            final String description) {
        this(id, label, type, talent, resourceCost, multiplierByRank, description, 1, 0);
    }
}
