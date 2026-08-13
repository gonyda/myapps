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
 * 히트별 독립 크리·편차의 결정성을 검증하는 프로퍼티 테스트.
 *
 * <p>고정 시드 {@link Random}과 동일 입력에 대해, 각 히트는 {@code rollCritical}(크리) →
 * {@code finalDamage}(편차) 순으로 난수를 소비하며, 동일 시드에서 히트별 크리 여부·피해가
 * 결정적으로 재현된다.
 *
 * <p>Feature: 009-skill-differentiation-and-battle-log, Property 4: 히트별 독립 크리·편차(결정성)
 *
 * <p><b>Validates: Requirements 4.3, 4.8</b>
 */
class BattleResolverMultiHitDeterminismPropertyTest {

    /**
     * 동일 시드·동일 입력으로 두 번 호출하면 히트별 결과가 동일한지 검증한다.
     *
     * @param seed              난수 시드
     * @param attackPower       공격력
     * @param multiplierPercent 1히트당 배율(%)
     * @param defense           대상 방어력
     * @param critChance        크리티컬 수치
     * @param hitCount          히트 수
     */
    @Property(tries = 100)
    void should_reproduceSameResults_when_sameSeedAndInput(
            @ForAll("seeds") final long seed,
            @ForAll("attackPowers") final int attackPower,
            @ForAll("multipliers") final int multiplierPercent,
            @ForAll("defenses") final int defense,
            @ForAll("critChances") final int critChance,
            @ForAll("hitCounts") final int hitCount) {

        final double affinityCoefficient = 1.0;

        final BattleResolver resolver1 = new BattleResolver(new Random(seed));
        final List<HitResult> result1 = resolver1.multiHitDamage(
                attackPower, multiplierPercent, defense, affinityCoefficient, critChance, hitCount);

        final BattleResolver resolver2 = new BattleResolver(new Random(seed));
        final List<HitResult> result2 = resolver2.multiHitDamage(
                attackPower, multiplierPercent, defense, affinityCoefficient, critChance, hitCount);

        assertThat(result1).hasSize(hitCount);
        assertThat(result2).hasSize(hitCount);

        for (int i = 0; i < hitCount; i++) {
            assertThat(result1.get(i).damage()).isEqualTo(result2.get(i).damage());
            assertThat(result1.get(i).critical()).isEqualTo(result2.get(i).critical());
        }
    }

    /**
     * 각 히트의 난수 소비 순서가 rollCritical→finalDamage(편차)임을 검증한다.
     *
     * <p>multiHitDamage의 각 히트 결과를 수동으로 baseDamage→rollCritical→finalDamage
     * 순서로 재현하여 히트별 일치를 확인한다.
     *
     * @param seed              난수 시드
     * @param attackPower       공격력
     * @param multiplierPercent 1히트당 배율(%)
     * @param defense           대상 방어력
     * @param critChance        크리티컬 수치
     * @param hitCount          히트 수
     */
    @Property(tries = 100)
    void should_consumeRandomInCritThenVarianceOrder_when_multiHit(
            @ForAll("seeds") final long seed,
            @ForAll("attackPowers") final int attackPower,
            @ForAll("multipliers") final int multiplierPercent,
            @ForAll("defenses") final int defense,
            @ForAll("critChances") final int critChance,
            @ForAll("hitCounts") final int hitCount) {

        final double affinityCoefficient = 1.0;

        // Get actual multiHitDamage results
        final BattleResolver multiResolver = new BattleResolver(new Random(seed));
        final List<HitResult> actualHits = multiResolver.multiHitDamage(
                attackPower, multiplierPercent, defense, affinityCoefficient, critChance, hitCount);

        // Manually reproduce hit-by-hit using same random sequence
        final BattleResolver manualResolver = new BattleResolver(new Random(seed));
        for (int i = 0; i < hitCount; i++) {
            final int baseDmg = manualResolver.baseDamage(attackPower, multiplierPercent, defense);
            final boolean crit = manualResolver.rollCritical(critChance);
            final int dmg = manualResolver.finalDamage(baseDmg, affinityCoefficient, crit);

            assertThat(actualHits.get(i).damage()).isEqualTo(dmg);
            assertThat(actualHits.get(i).critical()).isEqualTo(crit);
        }
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
