package com.myapps.web.myrpg.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 스킬 랭크 1단계 승급 시 발생하는 실제 영구 보너스 수치 변동량(Delta).
 *
 * @param strDelta STR 증가량
 * @param dexDelta DEX 증가량
 * @param intDelta INT 증가량
 * @param defDelta DEF 증가량
 * @param hpDelta HP 증가량
 * @param mpDelta MP 증가량
 * @param staminaDelta STAMINA 증가량
 * @param criticalDelta CRITICAL(0.1% 단위) 증가량
 * @param mpRegenDelta 턴당 MP 회복량 증가량
 */
public record SkillRankupBonusDelta(
        int strDelta,
        int dexDelta,
        int intDelta,
        int defDelta,
        int hpDelta,
        int mpDelta,
        int staminaDelta,
        int criticalDelta,
        int mpRegenDelta) {

    /** 보너스 변동 없음 상수. */
    public static final SkillRankupBonusDelta ZERO =
            new SkillRankupBonusDelta(0, 0, 0, 0, 0, 0, 0, 0, 0);

    /** STR 단일 증가. */
    public static SkillRankupBonusDelta str(final int delta) {
        return new SkillRankupBonusDelta(delta, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    /** DEX 단일 증가. */
    public static SkillRankupBonusDelta dex(final int delta) {
        return new SkillRankupBonusDelta(0, delta, 0, 0, 0, 0, 0, 0, 0);
    }

    /** INT 단일 증가. */
    public static SkillRankupBonusDelta intel(final int delta) {
        return new SkillRankupBonusDelta(0, 0, delta, 0, 0, 0, 0, 0, 0);
    }

    /** DEF 단일 증가. */
    public static SkillRankupBonusDelta def(final int delta) {
        return new SkillRankupBonusDelta(0, 0, 0, delta, 0, 0, 0, 0, 0);
    }

    /** DEF 및 HP 증가 (디펜스 전용). */
    public static SkillRankupBonusDelta defAndHp(final int def, final int hp) {
        return new SkillRankupBonusDelta(0, 0, 0, def, hp, 0, 0, 0, 0);
    }

    /** 모든 변동량이 0인지 여부를 반환한다. */
    public boolean isEmpty() {
        return strDelta == 0
                && dexDelta == 0
                && intDelta == 0
                && defDelta == 0
                && hpDelta == 0
                && mpDelta == 0
                && staminaDelta == 0
                && criticalDelta == 0
                && mpRegenDelta == 0;
    }

    /**
     * UI 모달 표시용 포맷팅 문자열을 생성한다.
     *
     * @return 포맷팅된 문자열 (변동 없으면 null)
     */
    public String toDisplayText() {
        if (isEmpty()) {
            return null;
        }
        final List<String> parts = new ArrayList<>();
        if (strDelta > 0) {
            parts.add("체력(STR) +" + strDelta);
        }
        if (dexDelta > 0) {
            parts.add("솜씨(DEX) +" + dexDelta);
        }
        if (intDelta > 0) {
            parts.add("지력(INT) +" + intDelta);
        }
        if (defDelta > 0) {
            parts.add("방어력(DEF) +" + defDelta);
        }
        if (hpDelta > 0) {
            parts.add("최대 HP +" + hpDelta);
        }
        if (mpDelta > 0) {
            parts.add("최대 MP +" + mpDelta);
        }
        if (staminaDelta > 0) {
            parts.add("최대 스태미나 +" + staminaDelta);
        }
        if (criticalDelta > 0) {
            final double critPercent = criticalDelta / 10.0;
            if (critPercent == (long) critPercent) {
                parts.add("치명타 확률 +" + (long) critPercent + "%p");
            } else {
                parts.add("치명타 확률 +" + String.format(Locale.US, "%.1f", critPercent) + "%p");
            }
        }
        if (mpRegenDelta > 0) {
            parts.add("턴당 MP 회복 +" + mpRegenDelta);
        }
        return String.join(", ", parts);
    }
}
