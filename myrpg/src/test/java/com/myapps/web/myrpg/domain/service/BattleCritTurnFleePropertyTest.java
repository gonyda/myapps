package com.myapps.web.myrpg.domain.service;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.DamageType;
import com.myapps.web.myrpg.domain.model.vo.DamageResult;
import com.myapps.web.myrpg.domain.model.vo.EffectiveStats;
import com.myapps.web.myrpg.domain.model.vo.TurnOrder;
import com.myapps.web.myrpg.domain.random.FixedRandomSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BattleService 치명타 확률·선후공·도망 속성 기반 테스트.
 *
 * <p>jqwik을 사용하여 치명타 확률 클램프, 치명타 판정 규칙,
 * 선후공 판정, 도망 성공 판정의 불변식을 검증한다.
 *
 * <p><b>Validates: Requirements 7.1, 7.2, 8.1, 8.2, 8.3, 9.1</b>
 */
class BattleCritTurnFleePropertyTest {

    private static final int SPEED_MIN = 0;
    private static final int SPEED_MAX = 1000;
    private static final int CRITICAL_MIN = -100;
    private static final int CRITICAL_MAX = 1000;

    private static final int CRIT_CHANCE_FLOOR = 5;
    private static final int CRIT_CHANCE_CEILING = 100;
    private static final double SPEED_CRIT_COEFFICIENT = 0.2;

    private static final int DEFAULT_ATTACK = 50;
    private static final int DEFAULT_DEFENSE = 10;
    private static final int DEFAULT_HP = 100;

    private static final double CRITICAL_MULTIPLIER = 1.5;
    private static final int MIN_DAMAGE = 1;
    private static final double DEVIATION_MIN = 0.9;
    private static final double DEVIATION_MAX = 1.1;

    // --- Providers ---

    @Provide
    Arbitrary<Integer> speedProvider() {
        return Arbitraries.integers().between(SPEED_MIN, SPEED_MAX);
    }

    @Provide
    Arbitrary<Integer> criticalStatProvider() {
        return Arbitraries.integers().between(CRITICAL_MIN, CRITICAL_MAX);
    }

    @Provide
    Arbitrary<Double> rollProvider() {
        return Arbitraries.doubles().between(0.0, 0.99);
    }

    @Provide
    Arbitrary<Integer> positiveSpeedProvider() {
        return Arbitraries.integers().between(1, 500);
    }

    // =====================================================================
    // Property 15: 치명타 확률 클램프
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 15: 치명타 확률 클램프
    /**
     * 임의의 speed ∈ [0, 1000]과 critical ∈ [-100, 1000]에 대해
     * criticalChance 결과는 항상 [5, 100] 범위에 속한다.
     *
     * <p><b>Validates: Requirements 7.1</b>
     */
    @Property(tries = 100)
    void criticalChanceIsAlwaysClampedBetween5And100(
            @ForAll("speedProvider") final int speed,
            @ForAll("criticalStatProvider") final int critical) {

        final EffectiveStats stats = new EffectiveStats(
                DEFAULT_ATTACK, DEFAULT_DEFENSE, speed, critical, DEFAULT_HP, DamageType.PHYSICAL);

        final FixedRandomSource random = new FixedRandomSource(0.5);
        final BattleService service = new BattleService(random);

        final int result = service.criticalChance(stats);

        assertTrue(result >= CRIT_CHANCE_FLOOR,
                "치명타 확률은 최소 5여야 한다. actual=" + result);
        assertTrue(result <= CRIT_CHANCE_CEILING,
                "치명타 확률은 최대 100이어야 한다. actual=" + result);
    }

    // Feature: myrpg-gen1-mvp, Property 15: 치명타 확률 클램프
    /**
     * 임의의 speed와 critical에 대해 criticalChance 결과는
     * max(5, min(100, (int)(5 + speed*0.2 + critical)))와 정확히 일치한다.
     *
     * <p><b>Validates: Requirements 7.1</b>
     */
    @Property(tries = 100)
    void criticalChanceMatchesFormula(
            @ForAll("speedProvider") final int speed,
            @ForAll("criticalStatProvider") final int critical) {

        final EffectiveStats stats = new EffectiveStats(
                DEFAULT_ATTACK, DEFAULT_DEFENSE, speed, critical, DEFAULT_HP, DamageType.PHYSICAL);

        final FixedRandomSource random = new FixedRandomSource(0.5);
        final BattleService service = new BattleService(random);

        final int result = service.criticalChance(stats);

        final int expected = Math.max(CRIT_CHANCE_FLOOR,
                Math.min(CRIT_CHANCE_CEILING,
                        (int) (CRIT_CHANCE_FLOOR + speed * SPEED_CRIT_COEFFICIENT + critical)));

        assertEquals(expected, result,
                "치명타 확률은 공식 max(5, min(100, (int)(5 + speed*0.2 + critical)))과 일치해야 한다");
    }

    // =====================================================================
    // Property 16: 치명타 판정 규칙
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 16: 치명타 판정 규칙
    /**
     * 임의의 critChance p ∈ [5, 100]과 roll x ∈ [0, 1)에 대해:
     * roll * 100 < p 이면 치명타 발생, 그 외에는 비치명타이다.
     *
     * <p>computeDamage는 내부적으로 nextDoubleInRange(편차) → nextDouble(치명타 판정) 순서로
     * 난수를 소비한다. 치명타 확률을 정확히 제어하기 위해 speed/critical을 조합하여
     * 알려진 critChance를 생성하고, 두 번째 double 값을 roll로 설정한다.
     *
     * <p><b>Validates: Requirements 7.2</b>
     */
    @Property(tries = 100)
    void criticalHitOccursWhenRollBelowChance(
            @ForAll("rollProvider") final double critRoll) {

        // speed=0, critical=0 → critChance = 5
        final int knownCritChance = CRIT_CHANCE_FLOOR;
        final EffectiveStats stats = new EffectiveStats(
                DEFAULT_ATTACK, DEFAULT_DEFENSE, 0, 0, DEFAULT_HP, DamageType.PHYSICAL);

        // 첫 번째 double: 편차용(0.5 → 1.0 편차), 두 번째: 치명타 판정
        final FixedRandomSource random = new FixedRandomSource(0.5, critRoll);
        final BattleService service = new BattleService(random);

        final DamageResult result = service.computeDamage(
                DEFAULT_ATTACK, 1.0, DamageType.PHYSICAL, DEFAULT_DEFENSE, stats);

        final boolean expectedCrit = critRoll * CRIT_CHANCE_CEILING < knownCritChance;

        assertEquals(expectedCrit, result.critical(),
                "roll=" + critRoll + " * 100=" + (critRoll * CRIT_CHANCE_CEILING)
                        + " < critChance=" + knownCritChance + " → 치명타=" + expectedCrit);
    }

    // Feature: myrpg-gen1-mvp, Property 16: 치명타 판정 규칙
    /**
     * 높은 치명타 확률(speed=475, critical=0 → critChance=100)일 때
     * 모든 roll에 대해 항상 치명타가 발생한다.
     *
     * <p><b>Validates: Requirements 7.2</b>
     */
    @Property(tries = 100)
    void alwaysCriticalWhenChanceIs100(
            @ForAll("rollProvider") final double critRoll) {

        // speed=475 → 5 + 475*0.2 + 0 = 100 → clamped to 100
        final EffectiveStats stats = new EffectiveStats(
                DEFAULT_ATTACK, DEFAULT_DEFENSE, 475, 0, DEFAULT_HP, DamageType.PHYSICAL);

        final FixedRandomSource random = new FixedRandomSource(0.5, critRoll);
        final BattleService service = new BattleService(random);

        final DamageResult result = service.computeDamage(
                DEFAULT_ATTACK, 1.0, DamageType.PHYSICAL, DEFAULT_DEFENSE, stats);

        assertTrue(result.critical(),
                "치명타 확률이 100일 때 모든 roll에서 치명타가 발생해야 한다. roll=" + critRoll);
    }

    // =====================================================================
    // Property 17: 선후공 판정
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 17: 선후공 판정
    /**
     * 플레이어 속도가 몬스터 속도보다 크면 항상 PLAYER_FIRST이다.
     * 난수 값과 무관하게 결정되어야 한다.
     *
     * <p><b>Validates: Requirements 8.1</b>
     */
    @Property(tries = 100)
    void playerFirstWhenPlayerSpeedGreater(
            @ForAll("positiveSpeedProvider") final int baseSpeed,
            @ForAll("rollProvider") final double anyRandom) {

        final int playerSpeed = baseSpeed + 1;
        final int monsterSpeed = baseSpeed;

        final FixedRandomSource random = new FixedRandomSource(anyRandom);
        final BattleService service = new BattleService(random);

        final TurnOrder result = service.decideTurnOrder(playerSpeed, monsterSpeed);

        assertEquals(TurnOrder.PLAYER_FIRST, result,
                "플레이어 속도 > 몬스터 속도 → 항상 PLAYER_FIRST");
    }

    // Feature: myrpg-gen1-mvp, Property 17: 선후공 판정
    /**
     * 플레이어 속도가 몬스터 속도보다 작으면 항상 MONSTER_FIRST이다.
     * 난수 값과 무관하게 결정되어야 한다.
     *
     * <p><b>Validates: Requirements 8.2</b>
     */
    @Property(tries = 100)
    void monsterFirstWhenMonsterSpeedGreater(
            @ForAll("positiveSpeedProvider") final int baseSpeed,
            @ForAll("rollProvider") final double anyRandom) {

        final int playerSpeed = baseSpeed;
        final int monsterSpeed = baseSpeed + 1;

        final FixedRandomSource random = new FixedRandomSource(anyRandom);
        final BattleService service = new BattleService(random);

        final TurnOrder result = service.decideTurnOrder(playerSpeed, monsterSpeed);

        assertEquals(TurnOrder.MONSTER_FIRST, result,
                "플레이어 속도 < 몬스터 속도 → 항상 MONSTER_FIRST");
    }

    // Feature: myrpg-gen1-mvp, Property 17: 선후공 판정
    /**
     * 속도가 동일할 때 random < 0.5이면 PLAYER_FIRST, >= 0.5이면 MONSTER_FIRST이다.
     *
     * <p><b>Validates: Requirements 8.3</b>
     */
    @Property(tries = 100)
    void tieBreakByRandomWhenSpeedEqual(
            @ForAll("positiveSpeedProvider") final int speed,
            @ForAll("rollProvider") final double tieRandom) {

        final FixedRandomSource random = new FixedRandomSource(tieRandom);
        final BattleService service = new BattleService(random);

        final TurnOrder result = service.decideTurnOrder(speed, speed);

        final TurnOrder expected = tieRandom < 0.5
                ? TurnOrder.PLAYER_FIRST
                : TurnOrder.MONSTER_FIRST;

        assertEquals(expected, result,
                "속도 동일 시 random=" + tieRandom + " < 0.5 → PLAYER_FIRST, >= 0.5 → MONSTER_FIRST");
    }

    // =====================================================================
    // Property 19: 도망 성공 판정
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 19: 도망 성공 판정
    /**
     * 임의의 random 값 x ∈ [0, 1)에 대해:
     * x < 0.5이면 도망 성공(true), x >= 0.5이면 도망 실패(false)이다.
     *
     * <p><b>Validates: Requirements 9.1</b>
     */
    @Property(tries = 100)
    void fleeSucceedsWhenRandomBelowHalf(
            @ForAll("rollProvider") final double fleeRoll) {

        final FixedRandomSource random = new FixedRandomSource(fleeRoll);
        final BattleService service = new BattleService(random);

        final boolean result = service.attemptFlee();

        if (fleeRoll < 0.5) {
            assertTrue(result,
                    "random=" + fleeRoll + " < 0.5 → 도망 성공이어야 한다");
        } else {
            assertFalse(result,
                    "random=" + fleeRoll + " >= 0.5 → 도망 실패여야 한다");
        }
    }
}
