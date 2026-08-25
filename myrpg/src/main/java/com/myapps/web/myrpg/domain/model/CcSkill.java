package com.myapps.web.myrpg.domain.model;

import java.util.Map;

/**
 * 군중 제어 스킬(CC - 스파이더 샷) 카탈로그 항목을 나타내는 불변 record.
 *
 * <p>데미지 0이며, 시전 턴 피격 후 성공 시 다음 턴 1턴간 몬스터를 속박한다. 랭크별 성공률(%) 맵({@link #successRateByRank()})을 보유한다.
 *
 * @param id 스킬 고유 식별자
 * @param label 표시용 라벨
 * @param type 스킬 타입 (CC)
 * @param talent 재능 분류
 * @param resourceCost 자원 소모량
 * @param successRateByRank 랭크별 성공률(%) 맵 (16키)
 * @param description 스킬 설명 문자열
 */
public record CcSkill(
        String id,
        String label,
        SkillType type,
        SkillTalent talent,
        int resourceCost,
        Map<SkillRank, Integer> successRateByRank,
        String description)
        implements Skill {

    /**
     * 지정된 랭크에서의 성공률(%)을 반환한다.
     *
     * @param rank 스킬 랭크
     * @return 해당 랭크의 성공률(%)
     */
    public int successRateAt(final SkillRank rank) {
        if (successRateByRank != null && rank != null && successRateByRank.containsKey(rank)) {
            return successRateByRank.get(rank);
        }
        return 0;
    }

    @Override
    public java.util.List<SkillEffectRowView> effectRowsAt(
            final SkillRank currentRank, final SkillRank nextRank) {
        final java.util.List<SkillEffectRowView> rows = new java.util.ArrayList<>();
        final int curRate = successRateAt(currentRank);
        final String nextRate = nextRank != null ? successRateAt(nextRank) + "%" : null;
        rows.add(new SkillEffectRowView("속박 성공률", curRate + "%", nextRate));
        rows.add(new SkillEffectRowView("행동 불능 지속", "1턴", nextRank != null ? "1턴" : null));

        return java.util.List.copyOf(rows);
    }

    @Override
    public SkillRankupBonusDelta rankupBonusDelta(
            final SkillRank currentRank, final SkillRank nextRank) {
        if (nextRank == null) {
            return SkillRankupBonusDelta.ZERO;
        }
        return switch (talent) {
            case MELEE -> SkillRankupBonusDelta.str(1);
            case ARCHERY -> SkillRankupBonusDelta.dex(1);
            case MAGIC -> SkillRankupBonusDelta.intel(1);
            case COMMON -> SkillRankupBonusDelta.def(1);
        };
    }
}
