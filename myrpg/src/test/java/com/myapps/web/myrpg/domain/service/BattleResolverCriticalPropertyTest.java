package com.myapps.web.myrpg.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 크리티컬 판정 및 배율의 정확성을 검증하는 프로퍼티 테스트.
 *
 * <p>고정 시드 {@link Random}에 대해 {@link BattleResolver#rollCritical}이 {@code random.nextInt(1000) <
 * critical}과 정확히 동치이며, {@link BattleResolver#finalDamage}에서 크리티컬 발동 시 비크리티컬 대비 1.5배(편차 전)를 반영하는지
 * 검증한다.
 *
 * <p>Feature: 008-battle-system, Property 4: 크리티컬 판정·배율
 *
 * <p><b>Validates: Requirements 5.1, 5.2, 5.3, 5.4</b>
 */
class BattleResolverCriticalPropertyTest {

    private static final int CRITICAL_ROLL_MAX = 1000;
    private static final double CRITICAL_MULTIPLIER = 1.5;

    /**
     * rollCritical이 동일 시드의 random.nextInt(1000) &lt; critical과 정확히 일치하는지 검증한다.
     *
     * @param seed 고정 시드 (0~9999)
     * @param critical 크리티컬 수치 (0~1000)
     */
    @Property(tries = 100)
    void should_matchRandomRoll_when_fixedSeed(
            @ForAll("seeds") final long seed, @ForAll("criticals") final int critical) {

        final Random expectedRandom = new Random(seed);
        final boolean expectedResult = expectedRandom.nextInt(CRITICAL_ROLL_MAX) < critical;

        final Random resolverRandom = new Random(seed);
        final BattleResolver resolver = new BattleResolver(resolverRandom);
        final boolean actualResult = resolver.rollCritical(critical);

        assertThat(actualResult).isEqualTo(expectedResult);
    }

    /**
     * critical=0이면 절대 크리티컬이 발동하지 않는지 검증한다.
     *
     * @param seed 고정 시드
     */
    @Property(tries = 100)
    void should_neverCritical_when_criticalIsZero(@ForAll("seeds") final long seed) {
        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final boolean result = resolver.rollCritical(0);

        assertThat(result).isFalse();
    }

    /**
     * critical=1000이면 항상 크리티컬이 발동하는지 검증한다.
     *
     * @param seed 고정 시드
     */
    @Property(tries = 100)
    void should_alwaysCritical_when_criticalIsMax(@ForAll("seeds") final long seed) {
        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final boolean result = resolver.rollCritical(CRITICAL_ROLL_MAX);

        assertThat(result).isTrue();
    }

    /**
     * 크리티컬 발동 시 finalDamage가 비크리티컬 대비 1.5배 (편차 동일 시드)인지 검증한다.
     *
     * <p>동일 시드에서 크리티컬/비크리티컬을 각각 산출하여, 편차 전 비율이 1.5배인지 확인한다. 편차(variance)는 동일 시드에서 동일 roll을 생산하므로
     * 비율이 정확히 1.5가 된다.
     *
     * @param seed 고정 시드 (0~9999)
     * @param baseDamage 기본피해 (10~200)
     * @param affinityCoefficient 상성계수 (사용: 1.0)
     */
    @Property(tries = 100)
    void should_applyOnePointFiveMultiplier_when_critical(
            @ForAll("seeds") final long seed, @ForAll("baseDamages") final int baseDamage) {

        final double affinityCoefficient = 1.0;

        final BattleResolver resolverCritical = new BattleResolver(new Random(seed));
        final int criticalDamage =
                resolverCritical.finalDamage(baseDamage, affinityCoefficient, true);

        final BattleResolver resolverNonCritical = new BattleResolver(new Random(seed));
        final int nonCriticalDamage =
                resolverNonCritical.finalDamage(baseDamage, affinityCoefficient, false);

        // 동일 시드이므로 편차(variance)가 같다. 크리티컬 비율은 정확히 1.5.
        // round 차이로 인한 ±1 허용
        final double expectedCritDamage = nonCriticalDamage * CRITICAL_MULTIPLIER;
        assertThat((double) criticalDamage)
                .isCloseTo(expectedCritDamage, org.assertj.core.data.Offset.offset(1.0));
    }

    /**
     * 시드 생성기 (0~9999).
     *
     * @return 시드 Arbitrary
     */
    @Provide
    Arbitrary<Long> seeds() {
        return Arbitraries.longs().between(0, 9999);
    }

    /**
     * 크리티컬 수치 생성기 (0~1000).
     *
     * @return 크리티컬 수치 Arbitrary
     */
    @Provide
    Arbitrary<Integer> criticals() {
        return Arbitraries.integers().between(0, 1000);
    }

    /**
     * 기본피해 생성기 (10~200).
     *
     * @return 기본피해 Arbitrary
     */
    @Provide
    Arbitrary<Integer> baseDamages() {
        return Arbitraries.integers().between(10, 200);
    }
}
