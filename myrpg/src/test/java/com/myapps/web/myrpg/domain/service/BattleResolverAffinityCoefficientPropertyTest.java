package com.myapps.web.myrpg.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.myapps.web.myrpg.domain.model.AffinityResult;
import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 상성계수 매핑의 정확성을 검증하는 프로퍼티 테스트.
 *
 * <p>임의의 {@link AffinityResult}와 경감률에 대해 {@link BattleResolver#affinityCoefficient}가 승 1.0, 무승부
 * 0.5, 관통패 0.0, 방어당함 {@code (1 − blockRate/100)}을 반환하며, blockRate가 [0,100] 범위일 때 계수가 [0,1] 범위인지
 * 검증한다.
 *
 * <p>Feature: 008-battle-system, Property 3: 상성계수 매핑
 *
 * <p><b>Validates: Requirements 3.4, 3.5, 3.6, 3.7, 3.8, 3.9</b>
 */
class BattleResolverAffinityCoefficientPropertyTest {

    private static final long FIXED_SEED = 42L;
    private static final double TOLERANCE = 0.0001;

    private final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));

    /**
     * WIN 결과는 항상 계수 1.0을 반환하는지 검증한다.
     *
     * @param blockRate 임의 경감률 (0~100)
     */
    @Property(tries = 100)
    void should_returnOne_when_win(@ForAll("blockRates") final int blockRate) {
        final double coefficient =
                resolver.affinityCoefficient(AffinityResult.WIN, false, blockRate);

        assertThat(coefficient).isCloseTo(1.0, within(TOLERANCE));
    }

    /**
     * DRAW 결과는 항상 계수 0.5를 반환하는지 검증한다.
     *
     * @param blockRate 임의 경감률 (0~100)
     */
    @Property(tries = 100)
    void should_returnHalf_when_draw(@ForAll("blockRates") final int blockRate) {
        final double coefficient =
                resolver.affinityCoefficient(AffinityResult.DRAW, false, blockRate);

        assertThat(coefficient).isCloseTo(0.5, within(TOLERANCE));
    }

    /**
     * LOSE + penetrated(관통)는 계수 0.0을 반환하는지 검증한다.
     *
     * @param blockRate 임의 경감률 (0~100)
     */
    @Property(tries = 100)
    void should_returnZero_when_loseAndPenetrated(@ForAll("blockRates") final int blockRate) {
        final double coefficient =
                resolver.affinityCoefficient(AffinityResult.LOSE, true, blockRate);

        assertThat(coefficient).isCloseTo(0.0, within(TOLERANCE));
    }

    /**
     * LOSE + not penetrated(방어당함)는 (1 - blockRate/100)을 반환하는지 검증한다.
     *
     * @param blockRate 경감률 (0~100)
     */
    @Property(tries = 100)
    void should_returnBlockReduction_when_loseAndNotPenetrated(
            @ForAll("blockRates") final int blockRate) {

        final double coefficient =
                resolver.affinityCoefficient(AffinityResult.LOSE, false, blockRate);
        final double expected = 1.0 - blockRate / 100.0;

        assertThat(coefficient).isCloseTo(expected, within(TOLERANCE));
    }

    /**
     * blockRate가 [0,100] 범위일 때 LOSE+!penetrated 계수가 [0,1] 범위인지 검증한다.
     *
     * @param blockRate 경감률 (0~100)
     */
    @Property(tries = 100)
    void should_returnCoefficientInZeroOneRange_when_validBlockRate(
            @ForAll("blockRates") final int blockRate) {

        final double coefficient =
                resolver.affinityCoefficient(AffinityResult.LOSE, false, blockRate);

        assertThat(coefficient).isBetween(0.0, 1.0);
    }

    /**
     * 경감률 생성기 (0~100).
     *
     * @return 경감률 Arbitrary
     */
    @Provide
    Arbitrary<Integer> blockRates() {
        return Arbitraries.integers().between(0, 100);
    }
}
