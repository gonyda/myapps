package com.myapps.web.myrpg.domain.model;

import java.util.Map;

/**
 * 영구 패시브(PASSIVE) 카탈로그 항목을 나타내는 불변 record.
 *
 * <p>전투 슬롯에 등록되지 않고 스킬 팝업 공용 탭에서 관리되며, MASTER 랭크 기준 최종 누적 스탯 맵({@link #totalStatBonus()})을 보유한다.
 *
 * @param id 스킬 고유 식별자
 * @param label 표시용 라벨
 * @param type 스킬 타입 (PASSIVE)
 * @param talent 재능 분류
 * @param resourceCost 자원 소모량 (0)
 * @param totalStatBonus MASTER 랭크 기준 최종 누적 스탯 맵
 * @param description 스킬 설명 문자열
 */
public record PassiveSkill(
        String id,
        String label,
        SkillType type,
        SkillTalent talent,
        int resourceCost,
        Map<BonusTarget, Integer> totalStatBonus,
        String description)
        implements Skill {

    @Override
    public java.util.List<SkillEffectRowView> effectRowsAt(
            final SkillRank currentRank, final SkillRank nextRank) {
        if (totalStatBonus == null || totalStatBonus.isEmpty()) {
            return java.util.List.of();
        }
        final java.util.List<SkillEffectRowView> rows = new java.util.ArrayList<>();
        for (final var entry : totalStatBonus.entrySet()) {
            rows.add(createEffectRow(entry.getKey(), entry.getValue(), currentRank, nextRank));
        }
        return java.util.List.copyOf(rows);
    }

    private SkillEffectRowView createEffectRow(
            final BonusTarget target,
            final int totalVal,
            final SkillRank currentRank,
            final SkillRank nextRank) {
        if (target == BonusTarget.MP_REGEN) {
            final int curRegen = Math.min(5, (currentRank.order() / 3) + 1);
            final String nextRegen =
                    nextRank != null ? "+" + Math.min(5, (nextRank.order() / 3) + 1) : null;
            return new SkillEffectRowView("턴당 마나 회복", "+" + curRegen, nextRegen);
        }
        if (target == BonusTarget.CRITICAL) {
            final int curVal = Math.round((float) totalVal * currentRank.order() / 15.0f);
            final String nextVal =
                    nextRank != null
                            ? "+"
                                    + (Math.round((float) totalVal * nextRank.order() / 15.0f)
                                            / 10.0)
                                    + "%p"
                            : null;
            return new SkillEffectRowView(target.label(), "+" + (curVal / 10.0) + "%p", nextVal);
        }
        final int curVal = Math.round((float) totalVal * currentRank.order() / 15.0f);
        final String nextVal =
                nextRank != null
                        ? "+" + Math.round((float) totalVal * nextRank.order() / 15.0f)
                        : null;
        return new SkillEffectRowView(target.label(), "+" + curVal, nextVal);
    }

    @Override
    public SkillRankupBonusDelta rankupBonusDelta(
            final SkillRank currentRank, final SkillRank nextRank) {
        if (nextRank == null || totalStatBonus == null) {
            return SkillRankupBonusDelta.ZERO;
        }
        final int curOrder = currentRank.order();
        final int nextOrder = nextRank.order();

        int strDelta = 0;
        int dexDelta = 0;
        int intDelta = 0;
        int defDelta = 0;
        int hpDelta = 0;
        int mpDelta = 0;
        int staminaDelta = 0;
        int critDelta = 0;
        int mpRegenDelta = 0;

        for (final var entry : totalStatBonus.entrySet()) {
            final BonusTarget target = entry.getKey();
            final int totalVal = entry.getValue();
            if (target == BonusTarget.MP_REGEN) {
                mpRegenDelta = Math.min(5, (nextOrder / 3) + 1) - Math.min(5, (curOrder / 3) + 1);
            } else {
                final int curVal = Math.round((float) totalVal * curOrder / 15.0f);
                final int nxtVal = Math.round((float) totalVal * nextOrder / 15.0f);
                final int delta = nxtVal - curVal;
                switch (target) {
                    case STR -> strDelta = delta;
                    case DEX -> dexDelta = delta;
                    case INT -> intDelta = delta;
                    case DEF -> defDelta = delta;
                    case HP -> hpDelta = delta;
                    case MP -> mpDelta = delta;
                    case STAMINA -> staminaDelta = delta;
                    case CRITICAL -> critDelta = delta;
                    case MP_REGEN -> {}
                }
            }
        }

        return new SkillRankupBonusDelta(
                strDelta,
                dexDelta,
                intDelta,
                defDelta,
                hpDelta,
                mpDelta,
                staminaDelta,
                critDelta,
                mpRegenDelta);
    }
}
