package com.myapps.web.myrpg.domain.service;

import java.util.List;
import java.util.Random;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.HitResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 멀티히트 합산·최소 보장을 검증하는 프로퍼티 테스트.
 *
 * <p>임의의 공격력·per-hit 배율·방어·상성계수·{@code hitCount ≥ 1}과 고정 시드에 대해,
 * {@link BattleResolver#multiHitDamage}는 정확히 {@code hitCount}개의 {@link HitResult}를 만들고,
 * 각 히트 피해는 ≥1이며, 총 피해는 각 히트 합과 같고 ≥ {@code hitCount}이다.
 *
 * <p>Feature: 009-skill-differentiation-and-battle-log, Property 2: 멀티히트 합산·최소 보장
 *
 * <p><b>Validates: Requirements 4.1, 4.4</b>
 */
class BattleResolverMultiHitSumPropertyTest {

    /**
     * multiHitDamage가 정확히 hitCount개의 결과를 반환하고, 각 히트 ≥1, 합계 == Σ 히트이며 ≥ hitCount인지 검증한다.
     *
     * @param seed                    난수 시드
     * @param attackPower             공격력
     * @param multiplierPercent       1히트당 배율(%)
     * @param defense                 대상 방어력
     * @param critChance              크리티컬 수치
     * @param hitCount                히트 수
     */
    @Property(tries = 100)
    void should_returnExactHitCountResults_when_anyValidInput(
            @ForAll("seeds") final long seed,
            @ForAll("attackPowers") final int attackPower,
            @ForAll("multipliers") final int multiplierPercent,
            @ForAll("defenses") final int defense,
            @ForAll("critChances") final int critChance,
            @ForAll("hitCounts") final int hitCount) {

        final double affinityCoefficient = 1.0;
        final BattleResolver resolver = new BattleResolver(new Random(seed));

        final List<HitResult> hits = resolver.multiHitDamage(
                attackPower, multiplierPercent, defense, affinityCoefficient, critChance, hitCount);

        assertThat(hits).hasSize(hitCount);

        final int sum = hits.stream().mapToInt(HitResult::damage).sum();

        for (final HitResult hit : hits) {
            assertThat(hit.damage()).isGreaterThanOrEqualTo(1);
        }

        assertThat(sum).isGreaterThanOrEqualTo(hitCount);
    }

    /**
     * 상성계수 0.5(무승부)에서도 각 히트 ≥1이고 합산이 일관되는지 검증한다.
     *
     * @param seed                    난수 시드
     * @param attackPower             공격력
     * @param multiplierPercent       1히트당 배율(%)
     * @param defense                 대상 방어력
     * @param critChance              크리티컬 수치
     * @param hitCount                히트 수
     */
    @Property(tries = 100)
    void should_maintainMinimumGuarantee_when_drawCoefficient(
            @ForAll("seeds") final long seed,
            @ForAll("attackPowers") final int attackPower,
            @ForAll("multipliers") final int multiplierPercent,
            @ForAll("defenses") final int defense,
            @ForAll("critChances") final int critChance,
            @ForAll("hitCounts") final int hitCount) {

        final double drawCoefficient = 0.5;
        final BattleResolver resolver = new BattleResolver(new Random(seed));

        final List<HitResult> hits = resolver.multiHitDamage(
                attackPower, multiplierPercent, defense, drawCoefficient, critChance, hitCount);

        assertThat(hits).hasSize(hitCount);

        int totalDamage = 0;
        for (final HitResult hit : hits) {
            assertThat(hit.damage()).isGreaterThanOrEqualTo(1);
            totalDamage += hit.damage();
        }

        assertThat(totalDamage).isGreaterThanOrEqualTo(hitCount);
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
     * 공격력 생성기 (10~500).
     *
     * @return 공격력 Arbitrary
     */
    @Provide
    Arbitrary<Integer> attackPowers() {
        return Arbitraries.integers().between(10, 500);
    }

    /**
     * 배율 생성기 (20~300).
     *
     * @return 배율 Arbitrary
     */
    @Provide
    Arbitrary<Integer> multipliers() {
        return Arbitraries.integers().between(20, 300);
    }

    /**
     * 방어력 생성기 (0~100).
     *
     * @return 방어력 Arbitrary
     */
    @Provide
    Arbitrary<Integer> defenses() {
        return Arbitraries.integers().between(0, 100);
    }

    /**
     * 크리티컬 수치 생성기 (0~1000).
     *
     * @return 크리티컬 수치 Arbitrary
     */
    @Provide
    Arbitrary<Integer> critChances() {
        return Arbitraries.integers().between(0, 1000);
    }

    /**
     * 히트 수 생성기 (1~8).
     *
     * @return 히트 수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> hitCounts() {
        return Arbitraries.integers().between(1, 8);
    }
}
