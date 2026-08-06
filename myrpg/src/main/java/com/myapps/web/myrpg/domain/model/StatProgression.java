package com.myapps.web.myrpg.domain.model;

/**
 * 레벨로부터 스탯과 최대 바이탈을 계산하는 순수 정책.
 *
 * <p>스킬 랭크업 보너스를 포함하지 않으며, 기본값과 레벨 파생분만 산출한다.
 * Critical은 0.1% 단위 정수로 계산한다(예: 50 = 5.0%).
 */
public class StatProgression {

    private static final int BASE_STR = 10;
    private static final int BASE_DEX = 10;
    private static final int BASE_INT = 10;
    private static final int BASE_CRITICAL = 50;
    private static final int BASE_DEF = 5;

    private static final int STR_PER_LEVEL = 3;
    private static final int DEX_PER_LEVEL = 3;
    private static final int INT_PER_LEVEL = 3;
    private static final int CRITICAL_PER_LEVEL = 3;
    private static final int DEF_PER_LEVEL = 1;

    private static final int BASE_VITAL = 100;
    private static final int VITAL_PER_LEVEL = 10;

    /**
     * 주어진 레벨의 Level_Stat(기본값 + 레벨 파생분)을 계산한다.
     *
     * <p>스킬 랭크업 보너스는 포함하지 않는다. Critical은 0.1% 단위 정수이다.
     *
     * @param level 현재 레벨 (1 이상)
     * @return 해당 레벨의 스탯 (STR, DEX, INT, Critical(0.1%단위), DEF)
     */
    public Stats levelStatsFor(final int level) {
        final int levelDerived = level - 1;
        return new Stats(
                BASE_STR + STR_PER_LEVEL * levelDerived,
                BASE_DEX + DEX_PER_LEVEL * levelDerived,
                BASE_INT + INT_PER_LEVEL * levelDerived,
                BASE_CRITICAL + CRITICAL_PER_LEVEL * levelDerived,
                BASE_DEF + DEF_PER_LEVEL * levelDerived
        );
    }

    /**
     * 주어진 레벨의 HP/MP/Stamina 최대치를 계산한다.
     *
     * <p>스킬 바이탈 보너스는 포함하지 않는다(현 시점 0).
     *
     * @param level 현재 레벨 (1 이상)
     * @return 해당 레벨의 바이탈 최대치
     */
    public int vitalMaxFor(final int level) {
        return BASE_VITAL + VITAL_PER_LEVEL * (level - 1);
    }
}
