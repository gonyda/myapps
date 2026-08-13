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
 * 단일 히트 하위호환 동치를 검증하는 프로퍼티 테스트.
 *
 * <p>{@code hitCount == 1}이고 {@code critBonus == 0}인 딜 스킬에 대해,
 * {@link BattleResolver#multiHitDamage}의 단일 결과와 총 피해가 009 이전
 * 단일 {@link BattleResolver#finalDamage} 산출과 동일한 값(동일 난수 시퀀스)을 내는지 검증한다.
 *
 * <p>Feature: 009-skill-differentiation-and-battle-log, Property 1: 단일 히트 하위호환 동치
 *
 * <p><b>Validates: Requirements 4.5, 11.1</b>
 */
class BattleResolverSingleHitBackwardCompatPropertyTest {

    private static final int SINGLE_HIT = 1;

    /**
     * hitCount==1일 때, multiHitDamage의 결과가 기존 rollCritical→finalDamage 시퀀스와 동일한지 검증한다.
     *
     * <p>동일 시드의 두 BattleResolver를 만들어, 하나는 multiHitDamage를 호출하고,
     * 다른 하나는 직접 rollCritical→finalDamage를 호출하여 결과를 비교한다.
     *
     * @param seed                    난수 시드
     * @param attackPower             공격력
     * @param multiplierPercent       스킬 배율(%)
     * @param defense                 대상 방어력
     * @param critChance              크리티컬 수치
     */
    @Property(tries = 100)
    void should_produceIdenticalResult_when_hitCountIsOne(
            @ForAll("seeds") final long seed,
            @ForAll("attackPowers") final int attackPower,
            @ForAll("multipliers") final int multiplierPercent,
            @ForAll("defenses") final int defense,
            @ForAll("critChances") final int critChance) {

        final double affinityCoefficient = 1.0;

        // multiHitDamage with hitCount=1
        final BattleResolver multiResolver = new BattleResolver(new Random(seed));
        final List<HitResult> hits = multiResolver.multiHitDamage(
                attackPower, multiplierPercent, defense, affinityCoefficient, critChance, SINGLE_HIT);

        // Old-style: baseDamage → rollCritical → finalDamage (same seed)
        final BattleResolver singleResolver = new BattleResolver(new Random(seed));
        final int baseDmg = singleResolver.baseDamage(attackPower, multiplierPercent, defense);
        final boolean crit = singleResolver.rollCritical(critChance);
        final int expectedDamage = singleResolver.finalDamage(baseDmg, affinityCoefficient, crit);

        assertThat(hits).hasSize(1);
        assertThat(hits.getFirst().damage()).isEqualTo(expectedDamage);
        assertThat(hits.getFirst().critical()).isEqualTo(crit);
    }

    /**
     * hitCount==1일 때, 상성계수 0.5(무승부)에서도 동일한 결과를 내는지 검증한다.
     *
     * @param seed                    난수 시드
     * @param attackPower             공격력
     * @param multiplierPercent       스킬 배율(%)
     * @param defense                 대상 방어력
     * @param critChance              크리티컬 수치
     */
    @Property(tries = 100)
    void should_produceIdenticalResult_when_hitCountIsOneWithDrawCoefficient(
            @ForAll("seeds") final long seed,
            @ForAll("attackPowers") final int attackPower,
            @ForAll("multipliers") final int multiplierPercent,
            @ForAll("defenses") final int defense,
            @ForAll("critChances") final int critChance) {

        final double drawCoefficient = 0.5;

        final BattleResolver multiResolver = new BattleResolver(new Random(seed));
        final List<HitResult> hits = multiResolver.multiHitDamage(
                attackPower, multiplierPercent, defense, drawCoefficient, critChance, SINGLE_HIT);

        final BattleResolver singleResolver = new BattleResolver(new Random(seed));
        final int baseDmg = singleResolver.baseDamage(attackPower, multiplierPercent, defense);
        final boolean crit = singleResolver.rollCritical(critChance);
        final int expectedDamage = singleResolver.finalDamage(baseDmg, drawCoefficient, crit);

        assertThat(hits).hasSize(1);
        assertThat(hits.getFirst().damage()).isEqualTo(expectedDamage);
        assertThat(hits.getFirst().critical()).isEqualTo(crit);
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
}
