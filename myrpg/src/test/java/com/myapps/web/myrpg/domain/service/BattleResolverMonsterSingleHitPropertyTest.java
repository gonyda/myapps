package com.myapps.web.myrpg.domain.service;

import java.util.Random;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.ResolvedTurn;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.TurnInput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 몬스터·반격 단일 히트 불변 성질을 검증하는 프로퍼티 테스트.
 *
 * <p>몬스터 피해·방어 반격 피해는 {@code hitCount}와 무관하게 단일 값으로 산출되고,
 * 해당 경로(반격/0/교착)의 {@code playerHits}는 비어 있다.
 *
 * <p>Feature: 009-skill-differentiation-and-battle-log, Property 9: 몬스터·반격 단일 히트 불변
 *
 * <p><b>Validates: Requirements 4.6, 6.4</b>
 */
class BattleResolverMonsterSingleHitPropertyTest {

    private static final int DEFAULT_ATTACK_POWER = 100;
    private static final int DEFAULT_DEFENSE = 10;
    private static final int DEFAULT_MULTIPLIER = 100;
    private static final int MONSTER_HEAVY_MULTIPLIER = 150;
    private static final int DEFAULT_BLOCK_RATE = 40;
    private static final int DEFAULT_COUNTER_PERCENT = 30;
    private static final int DEFAULT_CRITICAL = 50;

    /**
     * 공격 상성 패배(강&lt;일반) 시, playerHits는 비어 있고 몬스터 피해는 hitCount에 무관하게 단일이다.
     *
     * @param seed     난수 시드
     * @param hitCount 플레이어 히트 수 (무관해야 함)
     */
    @Property(tries = 100)
    void should_playerHitsEmpty_when_attackLoses(
            @ForAll("seeds") final long seed,
            @ForAll("hitCounts") final int hitCount) {

        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final TurnInput input = new TurnInput(
                SkillType.HEAVY, SkillType.NORMAL,
                DEFAULT_ATTACK_POWER, DEFAULT_ATTACK_POWER,
                DEFAULT_DEFENSE, DEFAULT_DEFENSE,
                DEFAULT_MULTIPLIER, DEFAULT_MULTIPLIER,
                DEFAULT_BLOCK_RATE, DEFAULT_BLOCK_RATE,
                DEFAULT_COUNTER_PERCENT, DEFAULT_COUNTER_PERCENT,
                DEFAULT_CRITICAL, DEFAULT_CRITICAL,
                hitCount);

        final ResolvedTurn result = resolver.resolve(input);

        assertThat(result.playerDamageToMonster()).isEqualTo(0);
        assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
        assertThat(result.playerHits()).isEmpty();
    }

    /**
     * 방어&lt;강(관통당함) 시, playerHits는 비어 있고 몬스터 피해는 hitCount에 무관하게 단일이다.
     *
     * @param seed     난수 시드
     * @param hitCount 플레이어 히트 수 (무관해야 함)
     */
    @Property(tries = 100)
    void should_playerHitsEmpty_when_defensePenetrated(
            @ForAll("seeds") final long seed,
            @ForAll("hitCounts") final int hitCount) {

        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final TurnInput input = new TurnInput(
                SkillType.DEFENSE, SkillType.HEAVY,
                DEFAULT_ATTACK_POWER, DEFAULT_ATTACK_POWER,
                DEFAULT_DEFENSE, DEFAULT_DEFENSE,
                DEFAULT_MULTIPLIER, MONSTER_HEAVY_MULTIPLIER,
                DEFAULT_BLOCK_RATE, DEFAULT_BLOCK_RATE,
                DEFAULT_COUNTER_PERCENT, DEFAULT_COUNTER_PERCENT,
                DEFAULT_CRITICAL, DEFAULT_CRITICAL,
                hitCount);

        final ResolvedTurn result = resolver.resolve(input);

        assertThat(result.playerDamageToMonster()).isEqualTo(0);
        assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
        assertThat(result.playerHits()).isEmpty();
    }

    /**
     * 방어 vs 방어(교착) 시, playerHits는 비어 있고 양쪽 피해 0이다.
     *
     * @param seed     난수 시드
     * @param hitCount 플레이어 히트 수 (무관해야 함)
     */
    @Property(tries = 100)
    void should_playerHitsEmpty_when_defenseVsDefenseStalemate(
            @ForAll("seeds") final long seed,
            @ForAll("hitCounts") final int hitCount) {

        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final TurnInput input = new TurnInput(
                SkillType.DEFENSE, SkillType.DEFENSE,
                DEFAULT_ATTACK_POWER, DEFAULT_ATTACK_POWER,
                DEFAULT_DEFENSE, DEFAULT_DEFENSE,
                DEFAULT_MULTIPLIER, DEFAULT_MULTIPLIER,
                DEFAULT_BLOCK_RATE, DEFAULT_BLOCK_RATE,
                DEFAULT_COUNTER_PERCENT, DEFAULT_COUNTER_PERCENT,
                DEFAULT_CRITICAL, DEFAULT_CRITICAL,
                hitCount);

        final ResolvedTurn result = resolver.resolve(input);

        assertThat(result.playerDamageToMonster()).isEqualTo(0);
        assertThat(result.monsterDamageToPlayer()).isEqualTo(0);
        assertThat(result.playerHits()).isEmpty();
    }

    /**
     * 방어&gt;일반(방어 승, 반격) 시, playerHits는 비어 있고 반격 피해는 hitCount에 무관하다.
     *
     * <p>반격 경로에서는 multiHitDamage가 호출되지 않으며 단일 반격 피해만 산출된다.
     *
     * @param seed     난수 시드
     * @param hitCount 플레이어 히트 수 (무관해야 함)
     */
    @Property(tries = 100)
    void should_playerHitsEmpty_when_defenseWinsWithCounter(
            @ForAll("seeds") final long seed,
            @ForAll("hitCounts") final int hitCount) {

        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final TurnInput input = new TurnInput(
                SkillType.DEFENSE, SkillType.NORMAL,
                DEFAULT_ATTACK_POWER, DEFAULT_ATTACK_POWER,
                DEFAULT_DEFENSE, DEFAULT_DEFENSE,
                DEFAULT_MULTIPLIER, DEFAULT_MULTIPLIER,
                DEFAULT_BLOCK_RATE, DEFAULT_BLOCK_RATE,
                DEFAULT_COUNTER_PERCENT, DEFAULT_COUNTER_PERCENT,
                DEFAULT_CRITICAL, DEFAULT_CRITICAL,
                hitCount);

        final ResolvedTurn result = resolver.resolve(input);

        assertThat(result.playerDamageToMonster()).isGreaterThan(0);
        assertThat(result.countered()).isTrue();
        assertThat(result.playerHits()).isEmpty();
    }

    /**
     * 몬스터 피해가 hitCount 변경에 의해 영향받지 않는지 검증한다.
     *
     * <p>동일 시드·동일 입력에서 hitCount만 달리한 두 결과의 몬스터 피해가 동일한지 확인한다.
     * 공격 상성 패배(강&lt;일반) 경로에서 몬스터 피해만 산출되며 playerHitCount는 무관하다.
     *
     * @param seed 난수 시드
     */
    @Property(tries = 100)
    void should_monsterDamageUnchanged_when_hitCountVaries(
            @ForAll("seeds") final long seed) {

        final BattleResolver resolver1 = new BattleResolver(new Random(seed));
        final TurnInput input1 = new TurnInput(
                SkillType.HEAVY, SkillType.NORMAL,
                DEFAULT_ATTACK_POWER, DEFAULT_ATTACK_POWER,
                DEFAULT_DEFENSE, DEFAULT_DEFENSE,
                DEFAULT_MULTIPLIER, DEFAULT_MULTIPLIER,
                DEFAULT_BLOCK_RATE, DEFAULT_BLOCK_RATE,
                DEFAULT_COUNTER_PERCENT, DEFAULT_COUNTER_PERCENT,
                DEFAULT_CRITICAL, DEFAULT_CRITICAL,
                1);
        final ResolvedTurn result1 = resolver1.resolve(input1);

        final BattleResolver resolver2 = new BattleResolver(new Random(seed));
        final TurnInput input2 = new TurnInput(
                SkillType.HEAVY, SkillType.NORMAL,
                DEFAULT_ATTACK_POWER, DEFAULT_ATTACK_POWER,
                DEFAULT_DEFENSE, DEFAULT_DEFENSE,
                DEFAULT_MULTIPLIER, DEFAULT_MULTIPLIER,
                DEFAULT_BLOCK_RATE, DEFAULT_BLOCK_RATE,
                DEFAULT_COUNTER_PERCENT, DEFAULT_COUNTER_PERCENT,
                DEFAULT_CRITICAL, DEFAULT_CRITICAL,
                5);
        final ResolvedTurn result2 = resolver2.resolve(input2);

        assertThat(result1.monsterDamageToPlayer()).isEqualTo(result2.monsterDamageToPlayer());
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
     * 히트 수 생성기 (1~8).
     *
     * @return 히트 수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> hitCounts() {
        return Arbitraries.integers().between(1, 8);
    }
}
