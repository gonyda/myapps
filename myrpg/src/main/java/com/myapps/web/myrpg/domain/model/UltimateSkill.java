package com.myapps.web.myrpg.domain.model;

import java.util.Map;

/**
 * 결전 궁극기(ULTIMATE) 카탈로그 항목을 나타내는 불변 record.
 *
 * <p>절대 우위(Super-Priority)로 몬스터의 행동을 무시하고 100% 관통 피해를 입히며 적 공격을 차단한다. 랭크별 데미지 배율 맵({@link
 * #multiplierByRank()}), 타수 맵({@link #hitCountByRank()}), 치명타 보너스({@link #critBonus()}), 랭크별 필요 승리
 * 횟수 쿨타임 맵({@link #coolWinsByRank()})을 보유한다.
 *
 * @param id 스킬 고유 식별자
 * @param label 표시용 라벨
 * @param type 스킬 타입 (ULTIMATE)
 * @param talent 재능 분류
 * @param resourceCost 자원 소모량
 * @param multiplierByRank 랭크별 1히트당 데미지 배율(%) 맵 (16키)
 * @param hitCountByRank 랭크별 타격 횟수 맵 (16키)
 * @param critBonus 크리티컬 확률 가산 보너스 (0~100, 기본 0)
 * @param coolWinsByRank 랭크별 필요 승리 횟수 쿨타임 맵 (16키)
 * @param description 스킬 설명 문자열
 */
public record UltimateSkill(
        String id,
        String label,
        SkillType type,
        SkillTalent talent,
        int resourceCost,
        Map<SkillRank, Integer> multiplierByRank,
        Map<SkillRank, Integer> hitCountByRank,
        int critBonus,
        Map<SkillRank, Integer> coolWinsByRank,
        String description)
        implements Skill {

    /**
     * 지정된 랭크에서의 타격 횟수를 반환한다.
     *
     * @param rank 스킬 랭크
     * @return 해당 랭크의 타격 횟수 (기본 1)
     */
    public int hitCountAt(final SkillRank rank) {
        if (hitCountByRank != null && rank != null && hitCountByRank.containsKey(rank)) {
            return hitCountByRank.get(rank);
        }
        return 1;
    }

    /**
     * 지정된 랭크에서의 배율을 반환한다.
     *
     * @param rank 스킬 랭크
     * @return 해당 랭크의 배율
     */
    public int multiplierAt(final SkillRank rank) {
        if (multiplierByRank != null && rank != null && multiplierByRank.containsKey(rank)) {
            return multiplierByRank.get(rank);
        }
        return 100;
    }

    /**
     * 지정된 랭크에서의 쿨타임(필요 승리 횟수)을 반환한다.
     *
     * @param rank 스킬 랭크
     * @return 해당 랭크의 필요 승리 횟수
     */
    public int coolWinsAt(final SkillRank rank) {
        if (coolWinsByRank != null && rank != null && coolWinsByRank.containsKey(rank)) {
            return coolWinsByRank.get(rank);
        }
        return 30;
    }

    @Override
    public java.util.List<SkillEffectRowView> effectRowsAt(
            final SkillRank currentRank, final SkillRank nextRank) {
        final java.util.List<SkillEffectRowView> rows = new java.util.ArrayList<>();
        final int curMult = multiplierAt(currentRank);
        final String nextMult = nextRank != null ? multiplierAt(nextRank) + "%" : null;
        rows.add(new SkillEffectRowView("결전 피해 배율", curMult + "%", nextMult));

        final int curHits = hitCountAt(currentRank);
        final String nextHits = nextRank != null ? hitCountAt(nextRank) + "회" : null;
        rows.add(new SkillEffectRowView("타격 횟수", curHits + "회", nextHits));

        if (critBonus > 0) {
            final String critText = "+" + (critBonus / 10.0) + "%p";
            rows.add(
                    new SkillEffectRowView(
                            "크리티컬 보너스", critText, nextRank != null ? critText : null));
        }

        final int curCool = coolWinsAt(currentRank);
        final String nextCool = nextRank != null ? coolWinsAt(nextRank) + "승" : null;
        rows.add(new SkillEffectRowView("필요 승리 수 (쿨다운)", curCool + "승", nextCool));

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
