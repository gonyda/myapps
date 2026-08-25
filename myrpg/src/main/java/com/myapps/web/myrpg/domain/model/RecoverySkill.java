package com.myapps.web.myrpg.domain.model;

import java.util.Map;

/**
 * 즉시 회복 스킬(RECOVERY - 힐링) 카탈로그 항목을 나타내는 불변 record.
 *
 * <p>랭크별 HP 회복량 맵({@link #healAmountByRank()})과 랭크별 마나 소모량 맵({@link #resourceCostByRank()})을 보유한다.
 *
 * @param id 스킬 고유 식별자
 * @param label 표시용 라벨
 * @param type 스킬 타입 (RECOVERY)
 * @param talent 재능 분류
 * @param resourceCost 기본 자원 소모량
 * @param healAmountByRank 랭크별 HP 회복량 맵 (16키)
 * @param resourceCostByRank 랭크별 마나 소모량 맵 (16키)
 * @param description 스킬 설명 문자열
 */
public record RecoverySkill(
        String id,
        String label,
        SkillType type,
        SkillTalent talent,
        int resourceCost,
        Map<SkillRank, Integer> healAmountByRank,
        Map<SkillRank, Integer> resourceCostByRank,
        String description)
        implements Skill {

    /**
     * 지정된 랭크에서의 마나 소모량을 반환한다.
     *
     * @param rank 스킬 랭크
     * @return 해당 랭크의 마나 소모량
     */
    public int resourceCostAt(final SkillRank rank) {
        if (resourceCostByRank != null && rank != null && resourceCostByRank.containsKey(rank)) {
            return resourceCostByRank.get(rank);
        }
        return resourceCost;
    }

    /**
     * 지정된 랭크에서의 HP 회복량을 반환한다.
     *
     * @param rank 스킬 랭크
     * @return 해당 랭크의 HP 회복량
     */
    public int healAmountAt(final SkillRank rank) {
        if (healAmountByRank != null && rank != null && healAmountByRank.containsKey(rank)) {
            return healAmountByRank.get(rank);
        }
        return 0;
    }

    @Override
    public java.util.List<SkillEffectRowView> effectRowsAt(
            final SkillRank currentRank, final SkillRank nextRank) {
        final java.util.List<SkillEffectRowView> rows = new java.util.ArrayList<>();
        final int curHeal = healAmountAt(currentRank);
        final String nextHeal = nextRank != null ? healAmountAt(nextRank) + " HP" : null;
        rows.add(new SkillEffectRowView("생명력 회복량", curHeal + " HP", nextHeal));

        final int curCost = resourceCostAt(currentRank);
        final String nextCost = nextRank != null ? resourceCostAt(nextRank) + " MP" : null;
        rows.add(new SkillEffectRowView("소모 마나", curCost + " MP", nextCost));

        return java.util.List.copyOf(rows);
    }

    @Override
    public SkillRankupBonusDelta rankupBonusDelta(
            final SkillRank currentRank, final SkillRank nextRank) {
        if (nextRank == null) {
            return SkillRankupBonusDelta.ZERO;
        }
        return SkillRankupBonusDelta.intel(1);
    }
}
