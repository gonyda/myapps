package com.myapps.web.myrpg.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 데미지 편차 범위의 정확성을 검증하는 프로퍼티 테스트.
 *
 * <p>고정 시드 {@link Random}에 대해 {@link BattleResolver#finalDamage}가 {@code [round(base × coeff × crit
 * × 0.90), round(base × coeff × crit × 1.10)]} 범위 내에 있고, 항상 최소 1이며, 동일 시드에서 결정적(동일 결과)인지 검증한다.
 *
 * <p>Feature: 008-battle-system, Property 5: 데미지 편차 범위
 *
 * <p><b>Validates: Requirements 4.6, 4.8</b>
 */
class BattleResolverVariancePropertyTest {

    private static final double VARIANCE_MIN = 0.90;
    private static final double VARIANCE_MAX = 1.10;
    private static final double CRITICAL_MULTIPLIER = 1.5;

    /**
     * finalDamage 결과가 보정피해의 ±10% 범위 내에 있는지 검증한다.
     *
     * @param seed 고정 시드 (0~9999)
     * @param baseDamage 기본피해 (1~500)
     * @param affinityCoefficient 상성계수 (50~100, ÷100으로 사용)
     * @param critical 크리티컬 여부 (true/false)
     */
    @Property(tries = 100)
    void should_beWithinVarianceRange_when_anyInput(
            @ForAll("seeds") final long seed,
            @ForAll("baseDamages") final int baseDamage,
            @ForAll("coefficientPercents") final int affinityCoeffPercent,
            @ForAll("booleans") final boolean critical) {

        final double affinityCoefficient = affinityCoeffPercent / 100.0;
        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final int actualDamage = resolver.finalDamage(baseDamage, affinityCoefficient, critical);

        final double critMultiplier = critical ? CRITICAL_MULTIPLIER : 1.0;
        final double preDamage = baseDamage * affinityCoefficient * critMultiplier;
        final long lowerBound = Math.max(1, Math.round(preDamage * VARIANCE_MIN));
        final long upperBound = Math.max(1, Math.round(preDamage * VARIANCE_MAX));

        assertThat((long) actualDamage).isBetween(lowerBound, upperBound);
    }

    /**
     * 상성계수가 0보다 클 때 finalDamage 결과가 항상 최소 1인지 검증한다.
     *
     * @param seed 고정 시드 (0~9999)
     * @param baseDamage 기본피해 (1~500)
     * @param affinityCoeffPercent 상성계수 (1~100, ÷100으로 사용)
     * @param critical 크리티컬 여부
     */
    @Property(tries = 100)
    void should_returnAtLeastOne_when_positiveCoefficient(
            @ForAll("seeds") final long seed,
            @ForAll("baseDamages") final int baseDamage,
            @ForAll("positiveCoeffPercents") final int affinityCoeffPercent,
            @ForAll("booleans") final boolean critical) {

        final double affinityCoefficient = affinityCoeffPercent / 100.0;
        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final int actualDamage = resolver.finalDamage(baseDamage, affinityCoefficient, critical);

        assertThat(actualDamage).isGreaterThanOrEqualTo(1);
    }

    /** 상성계수가 0.0일 때 finalDamage 결과가 정확히 0인지 검증한다. */
    @Property(tries = 100)
    void should_returnZero_when_zeroCoefficient(
            @ForAll("seeds") final long seed,
            @ForAll("baseDamages") final int baseDamage,
            @ForAll("booleans") final boolean critical) {

        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final int actualDamage = resolver.finalDamage(baseDamage, 0.0, critical);

        assertThat(actualDamage).isEqualTo(0);
    }

    /**
     * 동일 시드에서 동일 입력이면 동일 결과를 반환하는지(결정성) 검증한다.
     *
     * @param seed 고정 시드 (0~9999)
     * @param baseDamage 기본피해 (1~500)
     * @param critical 크리티컬 여부
     */
    @Property(tries = 100)
    void should_beDeterministic_when_sameSeed(
            @ForAll("seeds") final long seed,
            @ForAll("baseDamages") final int baseDamage,
            @ForAll("booleans") final boolean critical) {

        final double affinityCoefficient = 1.0;

        final BattleResolver resolver1 = new BattleResolver(new Random(seed));
        final int result1 = resolver1.finalDamage(baseDamage, affinityCoefficient, critical);

        final BattleResolver resolver2 = new BattleResolver(new Random(seed));
        final int result2 = resolver2.finalDamage(baseDamage, affinityCoefficient, critical);

        assertThat(result1).isEqualTo(result2);
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
     * 기본피해 생성기 (1~500).
     *
     * @return 기본피해 Arbitrary
     */
    @Provide
    Arbitrary<Integer> baseDamages() {
        return Arbitraries.integers().between(1, 500);
    }

    /**
     * 상성계수% 생성기 (50~100, 나누기 100.0으로 0.5~1.0).
     *
     * @return 상성계수% Arbitrary
     */
    @Provide
    Arbitrary<Integer> coefficientPercents() {
        return Arbitraries.integers().between(50, 100);
    }

    /**
     * 양의 상성계수% 생성기 (1~100, 나누기 100.0으로 0.01~1.0).
     *
     * @return 상성계수% Arbitrary
     */
    @Provide
    Arbitrary<Integer> positiveCoeffPercents() {
        return Arbitraries.integers().between(1, 100);
    }

    /**
     * boolean 생성기.
     *
     * @return boolean Arbitrary
     */
    @Provide
    Arbitrary<Boolean> booleans() {
        return Arbitraries.of(true, false);
    }
}
