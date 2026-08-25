package com.myapps.web.myrpg.domain.model;

import java.util.Map;

/**
 * 방어스킬(DEFENSE) 카탈로그 항목을 나타내는 불변 record.
 *
 * <p>각 랭크({@link SkillRank})에 대한 피해 경감률(%)과 반격 배율(%)을 각각 {@link #blockRateByRank()}와 {@link
 * #counterMultiplierByRank()}로 보유한다. 랭크가 오를수록 단조 비감소하며, 맵에는 반드시 16개 랭크 키가 모두 포함되어야 한다.
 *
 * <p>{@link #resourceCostByRank()}는 랭크별 자원 소모량 맵(선택적), {@link #critBonusByRank()}는 랭크별 크리티컬 가산 보너스
 * 맵(선택적)이다.
 *
 * @param id 스킬 고유 식별자
 * @param label 표시용 라벨
 * @param type 스킬 타입 (DEFENSE)
 * @param talent 재능 분류
 * @param resourceCost 기본 자원 소모량
 * @param blockRateByRank 랭크별 피해 경감률(%) 맵 (16키)
 * @param counterMultiplierByRank 랭크별 반격 배율(%) 맵 (16키)
 * @param description 스킬 설명 문자열
 * @param resourceCostByRank 랭크별 자원 소모량 맵 (부재 시 빈 맵)
 * @param critBonusByRank 랭크별 크리티컬 보너스 맵 (부재 시 빈 맵)
 */
public record DefenseSkill(
        String id,
        String label,
        SkillType type,
        SkillTalent talent,
        int resourceCost,
        Map<SkillRank, Integer> blockRateByRank,
        Map<SkillRank, Integer> counterMultiplierByRank,
        String description,
        Map<SkillRank, Integer> resourceCostByRank,
        Map<SkillRank, Integer> critBonusByRank)
        implements Skill {

    /**
     * 하위호환 보조 생성자: resourceCostByRank=Map.of(), critBonusByRank=Map.of().
     *
     * @param id 스킬 고유 식별자
     * @param label 표시용 라벨
     * @param type 스킬 타입
     * @param talent 재능 분류
     * @param resourceCost 자원 소모량
     * @param blockRateByRank 랭크별 피해 경감률(%) 맵
     * @param counterMultiplierByRank 랭크별 반격 배율(%) 맵
     * @param description 스킬 설명 문자열
     */
    public DefenseSkill(
            final String id,
            final String label,
            final SkillType type,
            final SkillTalent talent,
            final int resourceCost,
            final Map<SkillRank, Integer> blockRateByRank,
            final Map<SkillRank, Integer> counterMultiplierByRank,
            final String description) {
        this(
                id,
                label,
                type,
                talent,
                resourceCost,
                blockRateByRank,
                counterMultiplierByRank,
                description,
                Map.of(),
                Map.of());
    }

    /**
     * 지정된 랭크에서의 자원 소모량을 반환한다.
     *
     * <p>랭크별 자원 소모 맵({@link #resourceCostByRank()})에 해당 랭크 키가 존재하면 그 값을 반환하고, 그렇지 않으면 기본 자원
     * 소모량({@link #resourceCost()})을 반환한다.
     *
     * @param rank 스킬 랭크
     * @return 해당 랭크의 자원 소모량
     */
    public int resourceCostAt(final SkillRank rank) {
        if (resourceCostByRank != null && rank != null && resourceCostByRank.containsKey(rank)) {
            return resourceCostByRank.get(rank);
        }
        return resourceCost;
    }

    /**
     * 지정된 랭크에서의 크리티컬 보너스(0.1% 단위)를 반환한다.
     *
     * <p>랭크별 크리티컬 보너스 맵({@link #critBonusByRank()})에 해당 랭크 키가 존재하면 그 값을 반환하고, 그렇지 않으면 0을 반환한다.
     *
     * @param rank 스킬 랭크
     * @return 해당 랭크의 크리티컬 보너스 (0.1% 단위)
     */
    public int critBonusAt(final SkillRank rank) {
        if (critBonusByRank != null && rank != null && critBonusByRank.containsKey(rank)) {
            return critBonusByRank.get(rank);
        }
        return 0;
    }

    @Override
    public java.util.List<SkillEffectRowView> effectRowsAt(
            final SkillRank currentRank, final SkillRank nextRank) {
        final java.util.List<SkillEffectRowView> rows = new java.util.ArrayList<>();
        final int curBlock = blockRateByRank.getOrDefault(currentRank, 0);
        final String nextBlock =
                nextRank != null ? blockRateByRank.getOrDefault(nextRank, 0) + "%" : null;
        rows.add(new SkillEffectRowView("피해 경감률", curBlock + "%", nextBlock));

        final int curCounter = counterMultiplierByRank.getOrDefault(currentRank, 0);
        if (curCounter > 0
                || (nextRank != null && counterMultiplierByRank.getOrDefault(nextRank, 0) > 0)) {
            final String nextCounter =
                    nextRank != null
                            ? counterMultiplierByRank.getOrDefault(nextRank, 0) + "%"
                            : null;
            rows.add(new SkillEffectRowView("반격 피해 배율", curCounter + "%", nextCounter));
        }

        if (critBonusByRank != null && !critBonusByRank.isEmpty()) {
            final int curCrit = critBonusAt(currentRank);
            final String nextCrit =
                    nextRank != null ? "+" + (critBonusAt(nextRank) / 10.0) + "%p" : null;
            rows.add(new SkillEffectRowView("크리티컬 보너스", "+" + (curCrit / 10.0) + "%p", nextCrit));
        }

        return java.util.List.copyOf(rows);
    }

    @Override
    public SkillRankupBonusDelta rankupBonusDelta(
            final SkillRank currentRank, final SkillRank nextRank) {
        if (nextRank == null) {
            return SkillRankupBonusDelta.ZERO;
        }
        if ("defense".equals(id)) {
            return SkillRankupBonusDelta.defAndHp(1, 5);
        }
        return SkillRankupBonusDelta.ZERO;
    }
}
