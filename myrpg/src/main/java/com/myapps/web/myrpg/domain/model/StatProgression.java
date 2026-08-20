package com.myapps.web.myrpg.domain.model;

/**
 * 레벨과 재능으로부터 스탯과 최대 바이탈을 계산하는 순수 정책.
 *
 * <p>스킬 랭크업 보너스를 포함하지 않으며, 기본값·레벨 파생분·재능 보너스만 산출한다. Critical은 0.1% 단위 정수로 계산한다(예: 50 = 5.0%).
 *
 * <p>재능 보너스는 {@link TalentType}이 보유한 주/보조 {@link TalentBonus}의 {@code perLevel × (level - 1)}을 대상
 * 필드에만 가산하며, {@link BonusKind}로 스탯/바이탈 적용을 분기한다.
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
     * <p>재능 보너스·스킬 랭크업 보너스는 포함하지 않는다. Critical은 0.1% 단위 정수이다.
     *
     * @param level 현재 레벨 (1 이상)
     * @return 해당 레벨의 공통 스탯 (STR, DEX, INT, Critical(0.1%단위), DEF)
     */
    public Stats levelStatsFor(final int level) {
        final int levelDerived = level - 1;
        return new Stats(
                BASE_STR + STR_PER_LEVEL * levelDerived,
                BASE_DEX + DEX_PER_LEVEL * levelDerived,
                BASE_INT + INT_PER_LEVEL * levelDerived,
                BASE_CRITICAL + CRITICAL_PER_LEVEL * levelDerived,
                BASE_DEF + DEF_PER_LEVEL * levelDerived);
    }

    /**
     * 주어진 레벨과 재능의 Level_Stat(공통 + 재능 스탯 계열 보너스)을 계산한다.
     *
     * <p>공통 스탯에 재능의 주/보조 보너스 중 {@link BonusKind#STAT} 대상만 가산한다. 바이탈 계열 보너스는 스탯에 반영하지 않는다.
     *
     * @param level 현재 레벨 (1 이상)
     * @param talent 선택 재능
     * @return 해당 레벨·재능의 스탯 (STR, DEX, INT, Critical(0.1%단위), DEF)
     */
    public Stats levelStatsFor(final int level, final TalentType talent) {
        Stats result = levelStatsFor(level);
        result = applyStatBonus(result, talent.primary(), level);
        result = applyStatBonus(result, talent.secondary(), level);
        return result;
    }

    /**
     * 주어진 레벨의 HP/MP/Stamina 공통 최대치를 계산한다.
     *
     * <p>재능 보너스·스킬 바이탈 보너스는 포함하지 않는다.
     *
     * @param level 현재 레벨 (1 이상)
     * @return 해당 레벨의 바이탈 최대치 (HP=MP=Stamina 동일)
     * @deprecated 재능별 바이탈 최대치를 반환하는 {@link #vitalMaxFor(int, TalentType)} 사용 권장. 호출부 교체 완료 후 제거 예정.
     */
    @Deprecated
    public int vitalMaxFor(final int level) {
        return BASE_VITAL + VITAL_PER_LEVEL * (level - 1);
    }

    /**
     * 주어진 레벨과 재능의 바이탈별 최대치(공통 + 재능 바이탈 계열 보너스)를 계산한다.
     *
     * <p>공통값 {@code 100 + 10×(level-1)}을 세 바이탈에 채운 뒤 재능의 주/보조 보너스 중 {@link BonusKind#VITAL} 대상만
     * 가산한다. 스탯 계열 보너스는 바이탈에 반영하지 않는다.
     *
     * @param level 현재 레벨 (1 이상)
     * @param talent 선택 재능
     * @return 해당 레벨·재능의 바이탈별 최대치
     */
    public VitalMax vitalMaxFor(final int level, final TalentType talent) {
        final int commonVital = BASE_VITAL + VITAL_PER_LEVEL * (level - 1);
        VitalMax result = new VitalMax(commonVital, commonVital, commonVital);
        result = applyVitalBonus(result, talent.primary(), level);
        result = applyVitalBonus(result, talent.secondary(), level);
        return result;
    }

    /**
     * 보너스 대상이 스탯 계열이면 해당 스탯에 {@code perLevel × (level - 1)}을 가산한다.
     *
     * <p>대상이 바이탈 계열이면 스탯을 변경하지 않고 그대로 반환한다.
     *
     * @param stats 현재 스탯
     * @param bonus 적용할 재능 보너스
     * @param level 현재 레벨
     * @return 보너스 적용 후 스탯
     */
    private Stats applyStatBonus(final Stats stats, final TalentBonus bonus, final int level) {
        if (bonus.target().kind() != BonusKind.STAT) {
            return stats;
        }
        final int delta = bonus.perLevel() * (level - 1);
        return switch (bonus.target()) {
            case STR -> stats.withStrDelta(delta);
            case DEX -> stats.withDexDelta(delta);
            case INT -> stats.withIntDelta(delta);
            case CRITICAL -> stats.withCriticalDelta(delta);
            default -> stats;
        };
    }

    /**
     * 보너스 대상이 바이탈 계열이면 해당 바이탈에 {@code perLevel × (level - 1)}을 가산한다.
     *
     * <p>대상이 스탯 계열이면 바이탈을 변경하지 않고 그대로 반환한다.
     *
     * @param vitalMax 현재 바이탈 최대치
     * @param bonus 적용할 재능 보너스
     * @param level 현재 레벨
     * @return 보너스 적용 후 바이탈 최대치
     */
    private VitalMax applyVitalBonus(
            final VitalMax vitalMax, final TalentBonus bonus, final int level) {
        if (bonus.target().kind() != BonusKind.VITAL) {
            return vitalMax;
        }
        final int delta = bonus.perLevel() * (level - 1);
        return switch (bonus.target()) {
            case HP -> vitalMax.withHpDelta(delta);
            case MP -> vitalMax.withMpDelta(delta);
            case STAMINA -> vitalMax.withStaminaDelta(delta);
            default -> vitalMax;
        };
    }
}
