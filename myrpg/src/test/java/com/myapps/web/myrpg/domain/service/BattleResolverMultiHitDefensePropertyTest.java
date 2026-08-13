package com.myapps.web.myrpg.domain.service;

import java.util.Random;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 히트별 방어 차감 성질을 검증하는 프로퍼티 테스트.
 *
 * <p>방어 > 0인 대상에 대해, 각 히트의 기본피해는
 * {@code max(1, floor(공격력×per-hit배율/100) − 방어)}로 방어가 히트마다 차감되며,
 * 같은 총 배율을 단일로 때린 경우보다 다단 총 피해가 크지 않다(고방어일수록 다단 불리).
 *
 * <p>Feature: 009-skill-differentiation-and-battle-log, Property 3: 히트별 방어 차감
 *
 * <p><b>Validates: Requirements 4.2, 4.7</b>
 */
class BattleResolverMultiHitDefensePropertyTest {

    /**
     * 동일 총 배율에서 다단히트(방어 N회 차감)의 기본피해 합이 단일 히트(방어 1회 차감) 기본피해 이하인지 검증한다.
     *
     * <p>편차에 의한 차이를 배제하기 위해 baseDamage 레벨에서 비교한다.
     * 다단은 히트마다 방어를 빼므로 방어가 높을수록 불리하다.
     *
     * <p>비교가 유효한 조건: 1히트당 원시 피해(floor(atk×perHitMult/100)−defense)가 양수여야 함.
     * min-floor(max(1,...))가 개입하면 다단 합이 단일을 초과할 수 있으므로(min 보장 × hitCount),
     * 1히트당 원시가 양수인 경우에만 비교한다.
     *
     * @param attackPower       공격력
     * @param perHitMultiplier  다단 1히트당 배율(%)
     * @param defense           대상 방어력 (1 이상)
     * @param hitCount          히트 수 (2~8)
     */
    @Property(tries = 100)
    void should_multiHitBaseDamageSumNotExceedSingleHitBaseDamage_when_defensePositive(
            @ForAll("attackPowers") final int attackPower,
            @ForAll("perHitMultipliers") final int perHitMultiplier,
            @ForAll("positiveDefenses") final int defense,
            @ForAll("multiHitCounts") final int hitCount) {

        final int perHitRaw = Math.floorDiv(attackPower * perHitMultiplier, 100) - defense;

        // 1히트당 원시 피해가 양수가 아니면 min 보장이 개입하므로 비교 제외
        if (perHitRaw <= 0) {
            return;
        }

        final int totalMultiplier = perHitMultiplier * hitCount;
        final BattleResolver resolver = new BattleResolver(new Random(42L));

        final int singleBaseDmg = resolver.baseDamage(attackPower, totalMultiplier, defense);
        final int multiBaseDmgSum = hitCount * resolver.baseDamage(attackPower, perHitMultiplier, defense);

        assertThat(multiBaseDmgSum).isLessThanOrEqualTo(singleBaseDmg);
    }

    /**
     * 각 히트의 기본피해가 공식 max(1, floor(atk×perHitMult/100)−defense)와 일치하는지 검증한다.
     *
     * @param attackPower       공격력
     * @param perHitMultiplier  1히트당 배율(%)
     * @param defense           대상 방어력
     */
    @Property(tries = 100)
    void should_eachHitBaseDamageMatchFormula_when_anyInput(
            @ForAll("attackPowers") final int attackPower,
            @ForAll("perHitMultipliers") final int perHitMultiplier,
            @ForAll("allDefenses") final int defense) {

        final BattleResolver resolver = new BattleResolver(new Random(42L));
        final int baseDmg = resolver.baseDamage(attackPower, perHitMultiplier, defense);

        final int expected = Math.max(1, Math.floorDiv(attackPower * perHitMultiplier, 100) - defense);

        assertThat(baseDmg).isEqualTo(expected);
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
     * 1히트당 배율 생성기 (20~100).
     *
     * @return 배율 Arbitrary
     */
    @Provide
    Arbitrary<Integer> perHitMultipliers() {
        return Arbitraries.integers().between(20, 100);
    }

    /**
     * 양수 방어력 생성기 (1~100).
     *
     * @return 방어력 Arbitrary
     */
    @Provide
    Arbitrary<Integer> positiveDefenses() {
        return Arbitraries.integers().between(1, 100);
    }

    /**
     * 전체 방어력 생성기 (0~200).
     *
     * @return 방어력 Arbitrary
     */
    @Provide
    Arbitrary<Integer> allDefenses() {
        return Arbitraries.integers().between(0, 200);
    }

    /**
     * 다단 히트 수 생성기 (2~8, 다단이므로 최소 2).
     *
     * @return 히트 수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> multiHitCounts() {
        return Arbitraries.integers().between(2, 8);
    }
}
