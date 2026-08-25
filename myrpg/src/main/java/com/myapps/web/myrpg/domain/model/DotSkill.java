package com.myapps.web.myrpg.domain.model;

import java.util.Map;

/**
 * 지속 피해 스킬(DOT - 미라지 미사일) 카탈로그 항목을 나타내는 불변 record.
 *
 * <p>랭크별 즉발 배율(%) 맵({@link #initialMultiplierByRank()}), 랭크별 턴당 독 피해 맵({@link
 * #dotPerTurnByRank()}), 랭크별 지속 턴수 맵({@link #dotTurnsByRank()})을 보유한다.
 *
 * @param id 스킬 고유 식별자
 * @param label 표시용 라벨
 * @param type 스킬 타입 (DOT)
 * @param talent 재능 분류
 * @param resourceCost 자원 소모량
 * @param initialMultiplierByRank 랭크별 즉발 배율(%) 맵 (16키)
 * @param dotPerTurnByRank 랭크별 턴당 독 피해 맵 (16키)
 * @param dotTurnsByRank 랭크별 지속 턴수 맵 (16키)
 * @param description 스킬 설명 문자열
 */
public record DotSkill(
        String id,
        String label,
        SkillType type,
        SkillTalent talent,
        int resourceCost,
        Map<SkillRank, Integer> initialMultiplierByRank,
        Map<SkillRank, Integer> dotPerTurnByRank,
        Map<SkillRank, Integer> dotTurnsByRank,
        String description)
        implements Skill {

    /**
     * 지정된 랭크에서의 즉발 데미지 배율(%)을 반환한다.
     *
     * @param rank 스킬 랭크
     * @return 해당 랭크의 즉발 배율(%)
     */
    public int initialMultiplierAt(final SkillRank rank) {
        if (initialMultiplierByRank != null
                && rank != null
                && initialMultiplierByRank.containsKey(rank)) {
            return initialMultiplierByRank.get(rank);
        }
        return 0;
    }

    /**
     * 지정된 랭크에서의 턴당 독 피해량을 반환한다.
     *
     * @param rank 스킬 랭크
     * @return 해당 랭크의 턴당 독 피해량
     */
    public int dotPerTurnAt(final SkillRank rank) {
        if (dotPerTurnByRank != null && rank != null && dotPerTurnByRank.containsKey(rank)) {
            return dotPerTurnByRank.get(rank);
        }
        return 0;
    }

    /**
     * 지정된 랭크에서의 지속 턴 수를 반환한다.
     *
     * @param rank 스킬 랭크
     * @return 해당 랭크의 지속 턴 수
     */
    public int dotTurnsAt(final SkillRank rank) {
        if (dotTurnsByRank != null && rank != null && dotTurnsByRank.containsKey(rank)) {
            return dotTurnsByRank.get(rank);
        }
        return 0;
    }

    @Override
    public java.util.List<SkillEffectRowView> effectRowsAt(
            final SkillRank currentRank, final SkillRank nextRank) {
        final java.util.List<SkillEffectRowView> rows = new java.util.ArrayList<>();
        final int curInit = initialMultiplierAt(currentRank);
        final String nextInit = nextRank != null ? initialMultiplierAt(nextRank) + "%" : null;
        rows.add(new SkillEffectRowView("초기 직격 피해", curInit + "%", nextInit));

        final int curTurns = dotTurnsAt(currentRank);
        final String nextTurns = nextRank != null ? dotTurnsAt(nextRank) + "턴" : null;
        rows.add(new SkillEffectRowView("독 지속 시간", curTurns + "턴", nextTurns));

        final int curDot = dotPerTurnAt(currentRank);
        final String nextDot = nextRank != null ? dotPerTurnAt(nextRank) + "%" : null;
        rows.add(new SkillEffectRowView("턴당 독 피해", curDot + "%", nextDot));

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
