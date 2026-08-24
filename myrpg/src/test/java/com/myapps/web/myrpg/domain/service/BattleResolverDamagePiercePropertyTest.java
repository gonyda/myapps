package com.myapps.web.myrpg.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * {@link BattleResolver}의 랜덤 타수 범위 및 방어 관통 불변식을 검증하는 jqwik 프로퍼티 테스트.
 *
 * <p><b>Validates: Requirements 2.1, 2.2 (Property 2, 3)</b>
 */
class BattleResolverDamagePiercePropertyTest {

    private final BattleResolver resolver = new BattleResolver(new Random(42L));

    /**
     * Property 2: 임의의 minHits, maxHits에 대해 rollHitCount 결과 N은 항상 minHits <= N <= maxHits 범위를 만족한다.
     *
     * <p><b>Validates: Requirement 2.1</b>
     */
    @Property(tries = 100)
    void property2_should_constrainHitCountWithinMinAndMax(
            @ForAll("hitCountRange") final int[] range) {
        // given
        final int minHits = range[0];
        final int maxHits = range[1];

        // when
        final int rolledHits = resolver.rollHitCount(minHits, maxHits);

        // then
        assertThat(rolledHits).isBetween(minHits, maxHits);
    }

    /**
     * Property 3: defensePierce = true일 때 baseDamage는 대상의 방어력(DEF)과 무관하게 floor(ATK * mult / 100)와
     * 동일하다.
     *
     * <p><b>Validates: Requirement 2.2</b>
     */
    @Property(tries = 100)
    void property3_should_ignoreDefense_when_defensePierceIsTrue(
            @ForAll("positiveAttack") final int attackPower,
            @ForAll("positiveMultiplier") final int multiplier,
            @ForAll("positiveDefense") final int targetDefense) {
        // given
        final int expectedPierceDamage = Math.max(1, Math.floorDiv(attackPower * multiplier, 100));

        // when
        final int actualBaseDamage =
                resolver.baseDamage(attackPower, multiplier, targetDefense, true);

        // then
        assertThat(actualBaseDamage).isEqualTo(expectedPierceDamage);
    }

    /** defensePierce = false일 때 baseDamage는 targetDefense에 의해 정상 감산된다. */
    @Property(tries = 100)
    void should_subtractDefense_when_defensePierceIsFalse(
            @ForAll("positiveAttack") final int attackPower,
            @ForAll("positiveMultiplier") final int multiplier,
            @ForAll("positiveDefense") final int targetDefense) {
        // given
        final int expectedDamage =
                Math.max(1, Math.floorDiv(attackPower * multiplier, 100) - targetDefense);

        // when
        final int actualBaseDamage =
                resolver.baseDamage(attackPower, multiplier, targetDefense, false);

        // then
        assertThat(actualBaseDamage).isEqualTo(expectedDamage);
    }

    @Provide
    Arbitrary<int[]> hitCountRange() {
        return Arbitraries.integers()
                .between(1, 10)
                .flatMap(
                        min ->
                                Arbitraries.integers()
                                        .between(min, min + 5)
                                        .map(max -> new int[] {min, max}));
    }

    @Provide
    Arbitrary<Integer> positiveAttack() {
        return Arbitraries.integers().between(10, 1000);
    }

    @Provide
    Arbitrary<Integer> positiveMultiplier() {
        return Arbitraries.integers().between(50, 500);
    }

    @Provide
    Arbitrary<Integer> positiveDefense() {
        return Arbitraries.integers().between(1, 500);
    }
}
