package com.myapps.web.myrpg.domain.service;

import java.util.Random;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

import com.myapps.web.myrpg.domain.model.ResolvedTurn;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.TurnInput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 9칸 매트릭스 피해 산출의 정확성을 검증하는 프로퍼티 테스트.
 *
 * <p>모든 (플레이어 타입, 몬스터 타입) 9조합에 대해 {@link BattleResolver#resolve}가
 * 매트릭스 규칙을 따르는지 검증한다:
 * <ul>
 *   <li>공격 상성 승: 플레이어 &gt; 0, 몬스터 == 0</li>
 *   <li>방어 &gt; 일반 (방어 승): 플레이어 &gt; 0 (반격), 몬스터 &gt;= 0 (경감)</li>
 *   <li>공격 상성 패: 플레이어 == 0, 몬스터 &gt; 0</li>
 *   <li>일반 &lt; 방어 (일반 패): 플레이어 &gt;= 0 (경감), 몬스터 &gt; 0 (반격)</li>
 *   <li>공격 동일 무승부: 양쪽 &gt; 0</li>
 *   <li>방어 vs 방어: 양쪽 == 0</li>
 * </ul>
 *
 * <p>Feature: 008-battle-system, Property 6: 9칸 매트릭스 피해 산출
 *
 * <p><b>Validates: Requirements 3.2, 3.3, 3.4, 3.5, 3.6, 3.7</b>
 */
class BattleResolverMatrixPropertyTest {

    private static final int DEFAULT_ATTACK_POWER = 100;
    private static final int DEFAULT_DEFENSE = 10;
    private static final int DEFAULT_MULTIPLIER = 100;
    private static final int MONSTER_HEAVY_MULTIPLIER = 150;
    private static final int DEFAULT_BLOCK_RATE = 40;
    private static final int DEFAULT_COUNTER_PERCENT = 30;
    private static final int DEFAULT_CRITICAL = 0;

    /**
     * 일반 &gt; 강 (플레이어 승): 플레이어 피해 &gt; 0, 몬스터 피해 == 0.
     *
     * @param seed 고정 시드
     */
    @Property(tries = 100)
    void should_playerDamagePositiveMonsterZero_when_normalBeatsHeavy(
            @ForAll("seeds") final long seed) {

        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final TurnInput input = createInput(SkillType.NORMAL, SkillType.HEAVY);

        final ResolvedTurn result = resolver.resolve(input);

        assertThat(result.playerDamageToMonster()).isGreaterThan(0);
        assertThat(result.monsterDamageToPlayer()).isEqualTo(0);
    }

    /**
     * 강 &gt; 방어 (플레이어 승, 관통): 플레이어 피해 &gt; 0, 몬스터 피해 == 0, 반격 없음.
     *
     * @param seed 고정 시드
     */
    @Property(tries = 100)
    void should_playerDamagePositiveMonsterZero_when_heavyBeatsDefense(
            @ForAll("seeds") final long seed) {

        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final TurnInput input = createInput(SkillType.HEAVY, SkillType.DEFENSE);

        final ResolvedTurn result = resolver.resolve(input);

        assertThat(result.playerDamageToMonster()).isGreaterThan(0);
        assertThat(result.monsterDamageToPlayer()).isEqualTo(0);
        assertThat(result.countered()).isFalse();
    }

    /**
     * 방어 &gt; 일반 (플레이어 방어 승): 반격 피해 &gt; 0, 몬스터 경감 피해 &gt;= 0.
     *
     * @param seed 고정 시드
     */
    @Property(tries = 100)
    void should_playerCounterPositiveMonsterReduced_when_defenseBeatsNormal(
            @ForAll("seeds") final long seed) {

        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final TurnInput input = createInput(SkillType.DEFENSE, SkillType.NORMAL);

        final ResolvedTurn result = resolver.resolve(input);

        assertThat(result.playerDamageToMonster()).isGreaterThan(0);
        assertThat(result.monsterDamageToPlayer()).isGreaterThanOrEqualTo(0);
        assertThat(result.countered()).isTrue();
    }

    /**
     * 강 &lt; 일반 (플레이어 패): 플레이어 피해 == 0, 몬스터 피해 &gt; 0.
     *
     * @param seed 고정 시드
     */
    @Property(tries = 100)
    void should_playerZeroMonsterPositive_when_heavyLosesToNormal(
            @ForAll("seeds") final long seed) {

        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final TurnInput input = createInput(SkillType.HEAVY, SkillType.NORMAL);

        final ResolvedTurn result = resolver.resolve(input);

        assertThat(result.playerDamageToMonster()).isEqualTo(0);
        assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
    }

    /**
     * 방어 &lt; 강 (플레이어 패, 관통당함): 플레이어 피해 == 0, 몬스터 피해 &gt; 0.
     *
     * @param seed 고정 시드
     */
    @Property(tries = 100)
    void should_playerZeroMonsterPositive_when_defenseLosesToHeavy(
            @ForAll("seeds") final long seed) {

        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final TurnInput input = createInput(SkillType.DEFENSE, SkillType.HEAVY);

        final ResolvedTurn result = resolver.resolve(input);

        assertThat(result.playerDamageToMonster()).isEqualTo(0);
        assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
    }

    /**
     * 일반 &lt; 방어 (플레이어 패, 몬스터 방어 승): 플레이어 경감 피해 &gt;= 0, 몬스터 반격 &gt; 0.
     *
     * @param seed 고정 시드
     */
    @Property(tries = 100)
    void should_playerReducedMonsterCounter_when_normalLosesToDefense(
            @ForAll("seeds") final long seed) {

        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final TurnInput input = createInput(SkillType.NORMAL, SkillType.DEFENSE);

        final ResolvedTurn result = resolver.resolve(input);

        assertThat(result.playerDamageToMonster()).isGreaterThanOrEqualTo(0);
        assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
        assertThat(result.blocked()).isTrue();
        assertThat(result.countered()).isTrue();
    }

    /**
     * 일반 vs 일반 (무승부): 양쪽 피해 &gt; 0.
     *
     * @param seed 고정 시드
     */
    @Property(tries = 100)
    void should_bothPositive_when_normalVsNormalDraw(
            @ForAll("seeds") final long seed) {

        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final TurnInput input = createInput(SkillType.NORMAL, SkillType.NORMAL);

        final ResolvedTurn result = resolver.resolve(input);

        assertThat(result.playerDamageToMonster()).isGreaterThan(0);
        assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
    }

    /**
     * 강 vs 강 (무승부): 양쪽 피해 &gt; 0.
     *
     * @param seed 고정 시드
     */
    @Property(tries = 100)
    void should_bothPositive_when_heavyVsHeavyDraw(
            @ForAll("seeds") final long seed) {

        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final TurnInput input = createInput(SkillType.HEAVY, SkillType.HEAVY);

        final ResolvedTurn result = resolver.resolve(input);

        assertThat(result.playerDamageToMonster()).isGreaterThan(0);
        assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
    }

    /**
     * 방어 vs 방어 (교착): 양쪽 피해 == 0.
     *
     * @param seed 고정 시드
     */
    @Property(tries = 100)
    void should_bothZero_when_defenseVsDefenseDraw(
            @ForAll("seeds") final long seed) {

        final BattleResolver resolver = new BattleResolver(new Random(seed));
        final TurnInput input = createInput(SkillType.DEFENSE, SkillType.DEFENSE);

        final ResolvedTurn result = resolver.resolve(input);

        assertThat(result.playerDamageToMonster()).isEqualTo(0);
        assertThat(result.monsterDamageToPlayer()).isEqualTo(0);
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
     * 테스트용 TurnInput을 생성하는 헬퍼 메서드.
     *
     * @param playerType  플레이어 스킬 타입
     * @param monsterType 몬스터 스킬 타입
     * @return 표준 수치의 TurnInput
     */
    private TurnInput createInput(final SkillType playerType, final SkillType monsterType) {
        final int monsterMultiplier = monsterType == SkillType.HEAVY
                ? MONSTER_HEAVY_MULTIPLIER : DEFAULT_MULTIPLIER;
        return new TurnInput(
                playerType,
                monsterType,
                DEFAULT_ATTACK_POWER,
                DEFAULT_ATTACK_POWER,
                DEFAULT_DEFENSE,
                DEFAULT_DEFENSE,
                DEFAULT_MULTIPLIER,
                monsterMultiplier,
                DEFAULT_BLOCK_RATE,
                DEFAULT_BLOCK_RATE,
                DEFAULT_COUNTER_PERCENT,
                DEFAULT_COUNTER_PERCENT,
                DEFAULT_CRITICAL,
                DEFAULT_CRITICAL,
                1
        );
    }
}
