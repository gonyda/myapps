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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BattleService 데미지 파이프라인 속성 기반 테스트.
 *
 * <p>jqwik을 사용하여 데미지 최소값 보장, 산출 순서, 방어 계수, 배율 단조성을 검증한다.
 *
 * <p><b>Validates: Requirements 6.1, 6.2, 6.4, 6.6, 7.4, 10.2, 10.3, 10.4, 10.6</b>
 */
class BattleDamagePipelinePropertyTest {

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

    private static final double PHYSICAL_DEFENSE_COEFFICIENT = 0.5;
    private static final double MAGICAL_DEFENSE_COEFFICIENT = 0.2;
    private static final double DEVIATION_MIN = 0.9;
    private static final double DEVIATION_MAX = 1.1;
    private static final double CRITICAL_MULTIPLIER = 1.5;
    private static final int MIN_DAMAGE = 1;
    private static final double MONSTER_SKILL_MULTIPLIER = 1.0;

    private static final double DEVIATION_RATIO_FOR_1_0 = 0.5;
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
    // Property 11: 데미지 최소값과 산출 순서 보장
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 11: 데미지 최소값과 산출 순서 보장
    /**
     * computeDamage의 결과는 항상 1 이상이다 (최소 데미지 보장).
     *
     * <p>임의의 공격력, 스킬배율, 데미지 타입, 대상 방어력, 공격자 스탯, 난수 조합에 대해
     * 최종 데미지는 반드시 1 이상의 정수이다.
     *
     * <p><b>Validates: Requirements 6.4</b>
     */
    @Property(tries = 100)
    void computeDamageAlwaysReturnsAtLeastOne(
            @ForAll("attackPowerProvider") final int attackPower,
            @ForAll("skillMultiplierProvider") final double skillMultiplier,
            @ForAll("damageTypeProvider") final DamageType damageType,
            @ForAll("defenseProvider") final int targetDefense,
            @ForAll("deviationRatioProvider") final double deviationRatio,
            @ForAll("attackerStatsProvider") final EffectiveStats attackerStats) {

        final FixedRandomSource random = new FixedRandomSource(deviationRatio, CRIT_ROLL_NO_CRIT);
        final BattleService service = new BattleService(random);

        final DamageResult result = service.computeDamage(
                attackPower, skillMultiplier, damageType, targetDefense, attackerStats);

        assertTrue(result.damage() >= MIN_DAMAGE,
                "computeDamage 결과는 항상 >= 1이어야 한다. actual=" + result.damage());
    }

    // Feature: myrpg-gen1-mvp, Property 11: 데미지 최소값과 산출 순서 보장
    /**
     * monsterDamage의 결과는 항상 1 이상이다 (몬스터 최소 데미지 보장).
     *
     * <p>임의의 몬스터 공격력, 데미지 타입, 플레이어 방어력, 난수에 대해
     * 최종 데미지는 반드시 1 이상의 정수이다.
     *
     * <p><b>Validates: Requirements 10.6</b>
     */
    @Property(tries = 100)
    void monsterDamageAlwaysReturnsAtLeastOne(
            @ForAll("attackPowerProvider") final int monsterAttack,
            @ForAll("damageTypeProvider") final DamageType monsterType,
            @ForAll("defenseProvider") final int playerDefense,
            @ForAll("deviationRatioProvider") final double deviationRatio) {

        final FixedRandomSource random = new FixedRandomSource(deviationRatio);
        final BattleService service = new BattleService(random);

        final DamageResult result = service.monsterDamage(monsterAttack, monsterType, playerDefense);

        assertTrue(result.damage() >= MIN_DAMAGE,
                "monsterDamage 결과는 항상 >= 1이어야 한다. actual=" + result.damage());
    }

    // Feature: myrpg-gen1-mvp, Property 11: 데미지 최소값과 산출 순서 보장
    /**
     * 데미지 산출 순서 검증: 기본데미지 → 편차 → 치명타 → 최소 1 보장.
     *
     * <p>편차를 고정하고 치명타 미발생 시, 결과는 기본데미지×편차의 int 캐스트이거나 최소 1이다.
     * 편차를 고정하고 치명타 발생 시, 결과는 기본데미지×편차×1.5의 int 캐스트이거나 최소 1이다.
     * 치명타 확률을 정확히 제어하기 위해 speed=0, critical=0(확률 5%)인 스탯을 사용한다.
     *
     * <p><b>Validates: Requirements 7.4</b>
     */
    @Property(tries = 100)
    void computeDamageFollowsPipelineOrder(
            @ForAll("attackPowerProvider") final int attackPower,
            @ForAll("skillMultiplierProvider") final double skillMultiplier,
            @ForAll("damageTypeProvider") final DamageType damageType,
            @ForAll("defenseProvider") final int targetDefense) {

        // speed=0, critical=0 → critChance=5, so 0.99*100=99 >= 5 → no crit
        // and 0.01*100=1 < 5 → crit
        final EffectiveStats lowCritStats = new EffectiveStats(
                attackPower, targetDefense, 0, 0, 100, damageType);

        final double defCoeff = damageType == DamageType.PHYSICAL
                ? PHYSICAL_DEFENSE_COEFFICIENT
                : MAGICAL_DEFENSE_COEFFICIENT;
        final double baseDamage = attackPower * skillMultiplier - targetDefense * defCoeff;

        // Replicate exact deviation value from FixedRandomSource.nextDoubleInRange
        final double deviation = DEVIATION_MIN
                + DEVIATION_RATIO_FOR_1_0 * (DEVIATION_MAX - DEVIATION_MIN);

        // Case 1: deviation fixed, no crit (0.99*100=99 >= 5)
        final FixedRandomSource noCritRandom = new FixedRandomSource(
                DEVIATION_RATIO_FOR_1_0, CRIT_ROLL_NO_CRIT);
        final BattleService noCritService = new BattleService(noCritRandom);
        final DamageResult noCritResult = noCritService.computeDamage(
                attackPower, skillMultiplier, damageType, targetDefense, lowCritStats);

        final double afterDeviation = baseDamage * deviation;
        final int expectedNoCritDmg = Math.max(MIN_DAMAGE, (int) afterDeviation);
        assertEquals(expectedNoCritDmg, noCritResult.damage(),
                "편차 적용 후, 치명타 미발생 시 기본데미지×편차(int) 또는 최소 1");
        assertFalse(noCritResult.critical(), "치명타가 발생하지 않아야 한다");

        // Case 2: deviation fixed, force crit (0.01*100=1 < 5)
        final FixedRandomSource critRandom = new FixedRandomSource(
                DEVIATION_RATIO_FOR_1_0, CRIT_ROLL_FORCE_CRIT);
        final BattleService critService = new BattleService(critRandom);
        final DamageResult critResult = critService.computeDamage(
                attackPower, skillMultiplier, damageType, targetDefense, lowCritStats);

        final double afterCrit = afterDeviation * CRITICAL_MULTIPLIER;
        final int expectedCritDmg = Math.max(MIN_DAMAGE, (int) afterCrit);
        assertEquals(expectedCritDmg, critResult.damage(),
                "편차 적용 후, 치명타 발생 시 기본데미지×편차×1.5(int) 또는 최소 1");
        assertTrue(critResult.critical(), "치명타가 발생해야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 11: 데미지 최소값과 산출 순서 보장
    /**
     * 몬스터 데미지는 플레이어 공식과 동일하되 스킬배율 1.0, 치명타 없음으로 동작한다.
     *
     * <p>편차를 1.0으로 고정 시 결과는 (공격력×1.0 - 방어력×계수)의 int 캐스트이거나 최소 1이며,
     * 치명타는 항상 false이다.
     *
     * <p><b>Validates: Requirements 10.2</b>
     */
    @Property(tries = 100)
    void monsterDamageMatchesPlayerFormulaWithoutCrit(
            @ForAll("attackPowerProvider") final int monsterAttack,
            @ForAll("damageTypeProvider") final DamageType monsterType,
            @ForAll("defenseProvider") final int playerDefense) {

        final FixedRandomSource random = new FixedRandomSource(DEVIATION_RATIO_FOR_1_0);
        final BattleService service = new BattleService(random);

        final DamageResult result = service.monsterDamage(monsterAttack, monsterType, playerDefense);

        final double defCoeff = monsterType == DamageType.PHYSICAL
                ? PHYSICAL_DEFENSE_COEFFICIENT
                : MAGICAL_DEFENSE_COEFFICIENT;
        final double baseDamage = monsterAttack * MONSTER_SKILL_MULTIPLIER
                - playerDefense * defCoeff;
        final int expectedDmg = Math.max(MIN_DAMAGE, (int) (baseDamage * 1.0));

        assertEquals(expectedDmg, result.damage(),
                "몬스터 데미지는 공격력×1.0 - 방어력×계수, 최소 1");
        assertFalse(result.critical(), "몬스터 데미지에 치명타는 없다");
    }

    // =====================================================================
    // Property 12: 데미지 방어 계수와 배율 단조성
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 12: 데미지 방어 계수와 배율 단조성
    /**
     * 동일 공격력·배율에서 방어력이 높을수록 물리 데미지가 낮아진다(단조 비증가).
     *
     * <p>물리 방어 계수 0.5를 검증한다.
     *
     * <p><b>Validates: Requirements 6.1</b>
     */
    @Property(tries = 100)
    void higherDefenseReducesPhysicalDamage(
            @ForAll("attackPowerProvider") final int attackPower,
            @ForAll("skillMultiplierProvider") final double skillMultiplier) {

        final int lowDefense = 10;
        final int highDefense = 100;
        final EffectiveStats stats = new EffectiveStats(
                attackPower, 5, 0, 0, 100, DamageType.PHYSICAL);

        final FixedRandomSource random1 = new FixedRandomSource(
                DEVIATION_RATIO_FOR_1_0, CRIT_ROLL_NO_CRIT);
        final BattleService service1 = new BattleService(random1);
        final DamageResult lowDefResult = service1.computeDamage(
                attackPower, skillMultiplier, DamageType.PHYSICAL, lowDefense, stats);

        final FixedRandomSource random2 = new FixedRandomSource(
                DEVIATION_RATIO_FOR_1_0, CRIT_ROLL_NO_CRIT);
        final BattleService service2 = new BattleService(random2);
        final DamageResult highDefResult = service2.computeDamage(
                attackPower, skillMultiplier, DamageType.PHYSICAL, highDefense, stats);

        assertTrue(lowDefResult.damage() >= highDefResult.damage(),
                "방어력이 높을수록 물리 데미지는 낮아야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 12: 데미지 방어 계수와 배율 단조성
    /**
     * 동일 공격력·방어력에서 MAGICAL이 PHYSICAL보다 데미지가 같거나 높다.
     *
     * <p>마법 방어 계수(0.2) &lt; 물리 방어 계수(0.5)이므로,
     * 동일 조건에서 MAGICAL 데미지 &gt;= PHYSICAL 데미지이다.
     *
     * <p><b>Validates: Requirements 6.1, 6.2</b>
     */
    @Property(tries = 100)
    void magicalDamageIsGreaterOrEqualToPhysical(
            @ForAll("attackPowerProvider") final int attackPower,
            @ForAll("skillMultiplierProvider") final double skillMultiplier,
            @ForAll("defenseProvider") final int targetDefense) {

        final EffectiveStats stats = new EffectiveStats(
                attackPower, 5, 0, 0, 100, DamageType.PHYSICAL);

        final FixedRandomSource randomPhys = new FixedRandomSource(
                DEVIATION_RATIO_FOR_1_0, CRIT_ROLL_NO_CRIT);
        final BattleService servicePhys = new BattleService(randomPhys);
        final DamageResult physResult = servicePhys.computeDamage(
                attackPower, skillMultiplier, DamageType.PHYSICAL, targetDefense, stats);

        final FixedRandomSource randomMag = new FixedRandomSource(
                DEVIATION_RATIO_FOR_1_0, CRIT_ROLL_NO_CRIT);
        final BattleService serviceMag = new BattleService(randomMag);
        final DamageResult magResult = serviceMag.computeDamage(
                attackPower, skillMultiplier, DamageType.MAGICAL, targetDefense, stats);

        assertTrue(magResult.damage() >= physResult.damage(),
                "마법 데미지는 물리 데미지 이상이어야 한다 (방어 계수 0.2 < 0.5)");
    }

    // Feature: myrpg-gen1-mvp, Property 12: 데미지 방어 계수와 배율 단조성
    /**
     * 스킬 배율이 클수록 데미지가 감소하지 않는다 (단조 비감소).
     *
     * <p>편차·치명타를 고정한 상태에서 스킬 배율을 높이면 데미지는 같거나 증가한다.
     *
     * <p><b>Validates: Requirements 6.6</b>
     */
    @Property(tries = 100)
    void higherSkillMultiplierIncreasesOrMaintainsDamage(
            @ForAll("attackPowerProvider") final int attackPower,
            @ForAll("defenseProvider") final int targetDefense,
            @ForAll("damageTypeProvider") final DamageType damageType) {

        final double lowMultiplier = 0.8;
        final double highMultiplier = 2.0;
        final EffectiveStats stats = new EffectiveStats(
                attackPower, 5, 0, 0, 100, damageType);

        final FixedRandomSource random1 = new FixedRandomSource(
                DEVIATION_RATIO_FOR_1_0, CRIT_ROLL_NO_CRIT);
        final BattleService service1 = new BattleService(random1);
        final DamageResult lowResult = service1.computeDamage(
                attackPower, lowMultiplier, damageType, targetDefense, stats);

        final FixedRandomSource random2 = new FixedRandomSource(
                DEVIATION_RATIO_FOR_1_0, CRIT_ROLL_NO_CRIT);
        final BattleService service2 = new BattleService(random2);
        final DamageResult highResult = service2.computeDamage(
                attackPower, highMultiplier, damageType, targetDefense, stats);

        assertTrue(highResult.damage() >= lowResult.damage(),
                "스킬 배율이 높을수록 데미지는 같거나 높아야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 12: 데미지 방어 계수와 배율 단조성
    /**
     * 몬스터 물리 공격의 방어 계수는 0.5이다.
     *
     * <p>편차 1.0 고정 시 결과는 (공격력×1.0 - 방어력×0.5)의 int 캐스트이거나 최소 1이다.
     *
     * <p><b>Validates: Requirements 10.4</b>
     */
    @Property(tries = 100)
    void monsterPhysicalDefenseCoefficientIsHalf(
            @ForAll("attackPowerProvider") final int monsterAttack,
            @ForAll("defenseProvider") final int playerDefense) {

        final FixedRandomSource random = new FixedRandomSource(DEVIATION_RATIO_FOR_1_0);
        final BattleService service = new BattleService(random);

        final DamageResult result = service.monsterDamage(
                monsterAttack, DamageType.PHYSICAL, playerDefense);

        final double expected = monsterAttack * MONSTER_SKILL_MULTIPLIER
                - playerDefense * PHYSICAL_DEFENSE_COEFFICIENT;
        final int expectedDmg = Math.max(MIN_DAMAGE, (int) (expected * 1.0));

        assertEquals(expectedDmg, result.damage(),
                "몬스터 물리 방어 계수는 0.5");
    }

    // Feature: myrpg-gen1-mvp, Property 12: 데미지 방어 계수와 배율 단조성
    /**
     * 몬스터 마법 공격의 방어 계수는 0.2이다.
     *
     * <p>편차 1.0 고정 시 결과는 (공격력×1.0 - 방어력×0.2)의 int 캐스트이거나 최소 1이다.
     *
     * <p><b>Validates: Requirements 10.3</b>
     */
    @Property(tries = 100)
    void monsterMagicalDefenseCoefficientIsPointTwo(
            @ForAll("attackPowerProvider") final int monsterAttack,
            @ForAll("defenseProvider") final int playerDefense) {

        final FixedRandomSource random = new FixedRandomSource(DEVIATION_RATIO_FOR_1_0);
        final BattleService service = new BattleService(random);

        final DamageResult result = service.monsterDamage(
                monsterAttack, DamageType.MAGICAL, playerDefense);

        final double expected = monsterAttack * MONSTER_SKILL_MULTIPLIER
                - playerDefense * MAGICAL_DEFENSE_COEFFICIENT;
        final int expectedDmg = Math.max(MIN_DAMAGE, (int) (expected * 1.0));

        assertEquals(expectedDmg, result.damage(),
                "몬스터 마법 방어 계수는 0.2");
    }
}
