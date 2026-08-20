package com.myapps.web.myrpg.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 감산형 기본피해 공식의 정확성을 검증하는 프로퍼티 테스트.
 *
 * <p>임의의 공격력·스킬배율·방어력 조합에 대해 {@link BattleResolver#baseDamage}가 {@code max(1, floor(attackPower ×
 * multiplier / 100) − defense)}와 동치이며, 방어력이 산출 피해를 초과해도 항상 최소 1을 반환하는지 검증한다.
 *
 * <p>Feature: 008-battle-system, Property 2: 감산형 기본피해·최소 1
 *
 * <p><b>Validates: Requirements 4.2, 4.4</b>
 */
class BattleResolverBaseDamagePropertyTest {

    private static final long FIXED_SEED = 42L;

    private final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));

    /**
     * baseDamage는 공식 max(1, floor(atk*mult/100) - def)과 동치인지 검증한다.
     *
     * @param attackPower 공격력 (1~500)
     * @param skillMultiplierPercent 스킬 배율% (50~300)
     * @param targetDefense 대상 방어력 (0~300)
     */
    @Property(tries = 100)
    void should_matchFormula_when_anyValidInputs(
            @ForAll("attackPowers") final int attackPower,
            @ForAll("multipliers") final int skillMultiplierPercent,
            @ForAll("defenses") final int targetDefense) {

        final int actual = resolver.baseDamage(attackPower, skillMultiplierPercent, targetDefense);
        final int expected =
                Math.max(
                        1,
                        Math.floorDiv(attackPower * skillMultiplierPercent, 100) - targetDefense);

        assertThat(actual).isEqualTo(expected);
    }

    /**
     * baseDamage는 항상 1 이상을 반환하는지 검증한다.
     *
     * @param attackPower 공격력 (1~500)
     * @param skillMultiplierPercent 스킬 배율% (50~300)
     * @param targetDefense 대상 방어력 (0~1000)
     */
    @Property(tries = 100)
    void should_returnAtLeastOne_when_defenseExceedsDamage(
            @ForAll("attackPowers") final int attackPower,
            @ForAll("multipliers") final int skillMultiplierPercent,
            @ForAll("highDefenses") final int targetDefense) {

        final int actual = resolver.baseDamage(attackPower, skillMultiplierPercent, targetDefense);

        assertThat(actual).isGreaterThanOrEqualTo(1);
    }

    /**
     * 방어력이 원시 피해를 초과하면 정확히 1을 반환하는지 검증한다.
     *
     * @param attackPower 공격력 (1~100)
     * @param skillMultiplierPercent 스킬 배율% (50~150)
     */
    @Property(tries = 100)
    void should_returnExactlyOne_when_defenseExceedsRawDamage(
            @ForAll("smallAttackPowers") final int attackPower,
            @ForAll("smallMultipliers") final int skillMultiplierPercent) {

        final int rawDamage = Math.floorDiv(attackPower * skillMultiplierPercent, 100);
        final int overDefense = rawDamage + 100;

        final int actual = resolver.baseDamage(attackPower, skillMultiplierPercent, overDefense);

        assertThat(actual).isEqualTo(1);
    }

    /**
     * 공격력 생성기 (1~500).
     *
     * @return 공격력 Arbitrary
     */
    @Provide
    Arbitrary<Integer> attackPowers() {
        return Arbitraries.integers().between(1, 500);
    }

    /**
     * 스킬 배율 생성기 (50~300%).
     *
     * @return 스킬 배율 Arbitrary
     */
    @Provide
    Arbitrary<Integer> multipliers() {
        return Arbitraries.integers().between(50, 300);
    }

    /**
     * 방어력 생성기 (0~300).
     *
     * @return 방어력 Arbitrary
     */
    @Provide
    Arbitrary<Integer> defenses() {
        return Arbitraries.integers().between(0, 300);
    }

    /**
     * 높은 방어력 생성기 (0~1000).
     *
     * @return 높은 방어력 Arbitrary
     */
    @Provide
    Arbitrary<Integer> highDefenses() {
        return Arbitraries.integers().between(0, 1000);
    }

    /**
     * 소규모 공격력 생성기 (1~100).
     *
     * @return 소규모 공격력 Arbitrary
     */
    @Provide
    Arbitrary<Integer> smallAttackPowers() {
        return Arbitraries.integers().between(1, 100);
    }

    /**
     * 소규모 스킬 배율 생성기 (50~150%).
     *
     * @return 소규모 스킬 배율 Arbitrary
     */
    @Provide
    Arbitrary<Integer> smallMultipliers() {
        return Arbitraries.integers().between(50, 150);
    }
}
