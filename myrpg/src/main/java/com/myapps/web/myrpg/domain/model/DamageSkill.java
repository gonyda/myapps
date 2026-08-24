package com.myapps.web.myrpg.domain.model;

import java.util.Map;

/**
 * 딜스킬(NORMAL/HEAVY/DEBUFF) 카탈로그 항목을 나타내는 불변 record.
 *
 * <p>각 랭크({@link SkillRank})에 대한 1히트당 데미지 배율(%)을 {@link #multiplierByRank()}로 보유하며, 랭크가 오를수록 배율은 단조
 * 증가한다. 맵에는 반드시 16개 랭크 키가 모두 포함되어야 한다.
 *
 * <p>{@link #hitCount()}는 스킬 사용 시 기본 타격 횟수를 나타내며, {@link #minHits()}와 {@link #maxHits()}가 지정된 경우 랜덤
 * 범위 내에서 타수가 결정된다. {@link #defensePierce()}는 방어력 100% 관통 여부, {@link #freezeRateByRank()}는 랭크별 빙결 CC
 * 확률을 나타낸다.
 *
 * @param id 스킬 고유 식별자
 * @param label 표시용 라벨
 * @param type 스킬 타입 (NORMAL, HEAVY, DEBUFF)
 * @param talent 재능 분류
 * @param resourceCost 자원 소모량 (랭크 무관 고정)
 * @param multiplierByRank 랭크별 1히트당 데미지 배율(%) 맵 (16키, 단조 비감소)
 * @param description 스킬 설명 문자열
 * @param hitCount 스킬 사용 시 타격 횟수 (1 이상, 기본 1)
 * @param minHits 랜덤 타수 최소값 (랜덤 타수가 아니면 0)
 * @param maxHits 랜덤 타수 최대값 (랜덤 타수가 아니면 0)
 * @param critBonus 크리티컬 확률 가산 보너스 (0~100, 기본 0)
 * @param defensePierce 방어력 100% 관통 여부 (라이트닝 로드 등)
 * @param freezeRateByRank 랭크별 빙결 CC 확률(%) 맵 (아이스 스피어 등, 없으면 빈 맵)
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
        int minHits,
        int maxHits,
        int critBonus,
        boolean defensePierce,
        Map<SkillRank, Integer> freezeRateByRank)
        implements Skill {

    /**
     * 하위호환 생성자: minHits=0, maxHits=0, defensePierce=false, freezeRateByRank=Map.of().
     *
     * @param id 스킬 고유 식별자
     * @param label 표시용 라벨
     * @param type 스킬 타입
     * @param talent 재능 분류
     * @param resourceCost 자원 소모량
     * @param multiplierByRank 랭크별 1히트당 데미지 배율(%) 맵
     * @param description 스킬 설명 문자열
     * @param hitCount 스킬 사용 시 타격 횟수
     * @param critBonus 크리티컬 보너스
     */
    public DamageSkill(
            final String id,
            final String label,
            final SkillType type,
            final SkillTalent talent,
            final int resourceCost,
            final Map<SkillRank, Integer> multiplierByRank,
            final String description,
            final int hitCount,
            final int critBonus) {
        this(
                id,
                label,
                type,
                talent,
                resourceCost,
                multiplierByRank,
                description,
                hitCount,
                0,
                0,
                critBonus,
                false,
                Map.of());
    }

    /**
     * 기존 7-인자 하위호환 생성자: hitCount=1, critBonus=0.
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

    /**
     * 지정 랭크에서의 빙결 확률(%)을 반환한다.
     *
     * @param rank 스킬 랭크
     * @return 빙결 확률 (부재 시 0)
     */
    public int freezeRateAt(final SkillRank rank) {
        if (freezeRateByRank != null && rank != null && freezeRateByRank.containsKey(rank)) {
            return freezeRateByRank.get(rank);
        }
        return 0;
    }

    /**
     * 랜덤 다단히트 스킬인지 여부를 반환한다.
     *
     * @return 랜덤 타수 스킬이면 {@code true}
     */
    public boolean isRandomHit() {
        return minHits > 0 && maxHits >= minHits;
    }
}
