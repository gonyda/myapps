package com.myapps.web.myrpg.domain.service;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.DamageType;
import com.myapps.web.myrpg.domain.model.vo.DamageResult;
import com.myapps.web.myrpg.domain.model.vo.EffectiveStats;
import com.myapps.web.myrpg.domain.random.FixedRandomSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BattleService 랜덤 편차 범위 및 치명타 배율 속성 기반 테스트.
 *
 * <p>jqwik을 사용하여 랜덤 편차가 항상 [0.9, 1.1] 범위에 있으며,
 * 치명타 발생 시 편차 적용 데미지에 1.5 배율이 정확히 적용됨을 검증한다.
 *
 * <p><b>Validates: Requirements 6.3, 7.3, 10.5</b>
 */
class BattleDeviationCritMultiplierPropertyTest {

    private static final int ATTACK_MIN = 1;
    private static final int ATTACK_MAX = 500;
    private static final int DEFENSE_MIN = 0;
    private static final int DEFENSE_MAX = 500;
    private static final int SPEED_MIN = 0;
    private static final int SPEED_MAX = 100;
    private static final int CRITICAL_MIN = 0;
    private static final int CRITICAL_MAX = 100;
    private static final int HP_MIN = 1;
    private static final int HP_MAX = 1000;

    private static final double DEVIATION_MIN = 0.9;
    private static final double DEVIATION_MAX = 1.1;
    private static final double DEVIATION_RANGE = DEVIATION_MAX - DEVIATION_MIN;
    private static final double CRITICAL_MULTIPLIER = 1.5;
    private static final int MIN_DAMAGE = 1;

    private static final double PHYSICAL_DEFENSE_COEFFICIENT = 0.5;
    private static final double MAGICAL_DEFENSE_COEFFICIENT = 0.2;
    private static final double MONSTER_SKILL_MULTIPLIER = 1.0;

    private static final double CRIT_ROLL_NO_CRIT = 0.99;
    private static final double CRIT_ROLL_FORCE_CRIT = 0.01;

    // --- Providers ---

    @Provide
    Arbitrary<Integer> attackPowerProvider() {
        return Arbitraries.integers().between(ATTACK_MIN, ATTACK_MAX);
    }

    @Provide
    Arbitrary<Integer> defenseProvider() {
        return Arbitraries.integers().between(DEFENSE_MIN, DEFENSE_MAX);
    }

    @Provide
    Arbitrary<Double> skillMultiplierProvider() {
        return Arbitraries.doubles().between(0.8, 2.0);
    }

    @Provide
    Arbitrary<Double> deviationRatioProvider() {
        return Arbitraries.doubles().between(0.0, 1.0);
    }

    @Provide
    Arbitrary<DamageType> damageTypeProvider() {
        return Arbitraries.of(DamageType.values());
    }

    @Provide
    Arbitrary<EffectiveStats> attackerStatsProvider() {
        return Combinators.combine(
                Arbitraries.integers().between(ATTACK_MIN, ATTACK_MAX),
                Arbitraries.integers().between(DEFENSE_MIN, DEFENSE_MAX),
                Arbitraries.integers().between(SPEED_MIN, SPEED_MAX),
                Arbitraries.integers().between(CRITICAL_MIN, CRITICAL_MAX),
                Arbitraries.integers().between(HP_MIN, HP_MAX),
                Arbitraries.of(DamageType.values())
        ).as(EffectiveStats::new);
    }

    // =====================================================================
    // Property 13: 랜덤 편차 범위
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 13: 랜덤 편차 범위
    /**
     * 플레이어 데미지의 랜덤 편차 적용 값은 항상 [baseDamage × 0.9, baseDamage × 1.1] 범위에 있다.
     *
     * <p>임의의 공격력, 스킬배율, 방어력, 편차 비율(0~1)에 대해
     * 실제 적용되는 편차 계수는 0.9 + ratio × 0.2 이며 항상 [0.9, 1.1] 범위에 속한다.
     * 최종 결과는 baseDamage × 편차를 int 캐스트 후 최소 1 보장한 값과 일치한다.
     *
     * <p><b>Validates: Requirements 6.3</b>
     */
    @Property(tries = 100)
    void playerDeviationIsWithinBounds(
            @ForAll("attackPowerProvider") final int attackPower,
            @ForAll("skillMultiplierProvider") final double skillMultiplier,
            @ForAll("damageTypeProvider") final DamageType damageType,
            @ForAll("defenseProvider") final int targetDefense,
            @ForAll("deviationRatioProvider") final double deviationRatio) {

        final EffectiveStats stats = new EffectiveStats(
                attackPower, targetDefense, 0, 0, 100, damageType);

        final FixedRandomSource random = new FixedRandomSource(deviationRatio, CRIT_ROLL_NO_CRIT);
        final BattleService service = new BattleService(random);

        final DamageResult result = service.computeDamage(
                attackPower, skillMultiplier, damageType, targetDefense, stats);

        final double defCoeff = defenseCoefficient(damageType);
        final double baseDamage = attackPower * skillMultiplier - targetDefense * defCoeff;

        final double actualDeviation = DEVIATION_MIN + deviationRatio * DEVIATION_RANGE;

        assertTrue(actualDeviation >= DEVIATION_MIN && actualDeviation <= DEVIATION_MAX,
                "편차 계수는 [0.9, 1.1] 범위여야 한다. actual=" + actualDeviation);

        final double afterDeviation = baseDamage * actualDeviation;
        final int expectedDamage = Math.max(MIN_DAMAGE, (int) afterDeviation);

        assertEquals(expectedDamage, result.damage(),
                "플레이어 데미지는 baseDamage × 편차(int) 또는 최소 1이어야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 13: 랜덤 편차 범위
    /**
     * 몬스터 데미지의 랜덤 편차 적용 값은 항상 [baseDamage × 0.9, baseDamage × 1.1] 범위에 있다.
     *
     * <p>임의의 몬스터 공격력, 데미지 타입, 플레이어 방어력, 편차 비율(0~1)에 대해
     * 실제 적용되는 편차 계수는 0.9 + ratio × 0.2 이며 항상 [0.9, 1.1] 범위에 속한다.
     * 최종 결과는 baseDamage × 편차를 int 캐스트 후 최소 1 보장한 값과 일치한다.
     *
     * <p><b>Validates: Requirements 10.5</b>
     */
    @Property(tries = 100)
    void monsterDeviationIsWithinBounds(
            @ForAll("attackPowerProvider") final int monsterAttack,
            @ForAll("damageTypeProvider") final DamageType monsterType,
            @ForAll("defenseProvider") final int playerDefense,
            @ForAll("deviationRatioProvider") final double deviationRatio) {

        final FixedRandomSource random = new FixedRandomSource(deviationRatio);
        final BattleService service = new BattleService(random);

        final DamageResult result = service.monsterDamage(monsterAttack, monsterType, playerDefense);

        final double defCoeff = defenseCoefficient(monsterType);
        final double baseDamage = monsterAttack * MONSTER_SKILL_MULTIPLIER
                - playerDefense * defCoeff;

        final double actualDeviation = DEVIATION_MIN + deviationRatio * DEVIATION_RANGE;

        assertTrue(actualDeviation >= DEVIATION_MIN && actualDeviation <= DEVIATION_MAX,
                "편차 계수는 [0.9, 1.1] 범위여야 한다. actual=" + actualDeviation);

        final double afterDeviation = baseDamage * actualDeviation;
        final int expectedDamage = Math.max(MIN_DAMAGE, (int) afterDeviation);

        assertEquals(expectedDamage, result.damage(),
                "몬스터 데미지는 baseDamage × 편차(int) 또는 최소 1이어야 한다");
    }

    // =====================================================================
    // Property 14: 치명타 배율
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 14: 치명타 배율
    /**
     * 치명타가 발생하면 최종 데미지는 편차 적용 값 × 1.5에 최소값 보정을 적용한 값과 같다.
     *
     * <p>임의의 공격력, 스킬배율, 방어력, 편차 비율에 대해 치명타를 강제 발생시키면
     * 결과는 (baseDamage × 편차 × 1.5)를 int 캐스트 후 최소 1 보장한 값과 일치한다.
     * 치명타 확률을 정확히 제어하기 위해 speed=0, critical=0(확률 5%)인 스탯을 사용하고
     * critRoll=0.01(1 < 5 → 치명타 발생)로 강제한다.
     *
     * <p><b>Validates: Requirements 7.3</b>
     */
    @Property(tries = 100)
    void criticalHitMultipliesByOnePointFive(
            @ForAll("attackPowerProvider") final int attackPower,
            @ForAll("skillMultiplierProvider") final double skillMultiplier,
            @ForAll("damageTypeProvider") final DamageType damageType,
            @ForAll("defenseProvider") final int targetDefense,
            @ForAll("deviationRatioProvider") final double deviationRatio) {

        final EffectiveStats stats = new EffectiveStats(
                attackPower, targetDefense, 0, 0, 100, damageType);

        final FixedRandomSource random = new FixedRandomSource(deviationRatio, CRIT_ROLL_FORCE_CRIT);
        final BattleService service = new BattleService(random);

        final DamageResult result = service.computeDamage(
                attackPower, skillMultiplier, damageType, targetDefense, stats);

        assertTrue(result.critical(), "치명타가 반드시 발생해야 한다");

        final double defCoeff = defenseCoefficient(damageType);
        final double baseDamage = attackPower * skillMultiplier - targetDefense * defCoeff;
        final double actualDeviation = DEVIATION_MIN + deviationRatio * DEVIATION_RANGE;
        final double afterDeviation = baseDamage * actualDeviation;
        final double afterCrit = afterDeviation * CRITICAL_MULTIPLIER;
        final int expectedDamage = Math.max(MIN_DAMAGE, (int) afterCrit);

        assertEquals(expectedDamage, result.damage(),
                "치명타 시 데미지는 편차값 × 1.5에 최소값 보정을 적용한 값이어야 한다");
    }

    // --- Helper ---

    /**
     * 데미지 타입에 따른 방어 계수를 반환한다.
     *
     * @param damageType 데미지 타입
     * @return 방어 계수
     */
    private double defenseCoefficient(final DamageType damageType) {
        return damageType == DamageType.PHYSICAL
                ? PHYSICAL_DEFENSE_COEFFICIENT
                : MAGICAL_DEFENSE_COEFFICIENT;
    }
}
