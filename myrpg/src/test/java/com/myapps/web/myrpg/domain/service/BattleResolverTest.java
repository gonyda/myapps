package com.myapps.web.myrpg.domain.service;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.myapps.web.myrpg.domain.model.ResolvedTurn;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.TurnInput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BattleResolver}의 구체적 예시를 검증하는 단위 테스트.
 *
 * <p>9칸 매트릭스 각 셀의 대표 예시, 감산 경계(방어 &gt;= 공격 산출 → baseDamage == 1),
 * 크리티컬 on/off 예시를 고정 시드 {@link Random}으로 검증한다.
 */
class BattleResolverTest {

    private static final long FIXED_SEED = 12345L;
    private static final int PLAYER_ATTACK = 100;
    private static final int MONSTER_ATTACK = 80;
    private static final int PLAYER_DEFENSE = 10;
    private static final int MONSTER_DEFENSE = 15;
    private static final int PLAYER_MULTIPLIER = 120;
    private static final int MONSTER_NORMAL_MULTIPLIER = 100;
    private static final int MONSTER_HEAVY_MULTIPLIER = 150;
    private static final int BLOCK_RATE = 40;
    private static final int COUNTER_PERCENT = 30;
    private static final int ZERO_CRITICAL = 0;
    private static final int MAX_CRITICAL = 1000;

    @Nested
    @DisplayName("9칸 매트릭스 예시")
    class MatrixExamples {

        /**
         * 일반 vs 강 (플레이어 승): 플레이어만 피해.
         */
        @Test
        void should_playerDealDamage_when_normalBeatsHeavy() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input = createInput(SkillType.NORMAL, SkillType.HEAVY,
                    MONSTER_NORMAL_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isGreaterThan(0);
            assertThat(result.monsterDamageToPlayer()).isEqualTo(0);
            assertThat(result.blocked()).isFalse();
            assertThat(result.countered()).isFalse();
        }

        /**
         * 강 vs 방어 (플레이어 승, 관통): 플레이어만 피해, 반격 무효.
         */
        @Test
        void should_playerPenetrate_when_heavyBeatsDefense() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input = createInput(SkillType.HEAVY, SkillType.DEFENSE,
                    MONSTER_NORMAL_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isGreaterThan(0);
            assertThat(result.monsterDamageToPlayer()).isEqualTo(0);
            assertThat(result.countered()).isFalse();
        }

        /**
         * 방어 vs 일반 (플레이어 방어 승): 반격 발생 + 경감 피해.
         */
        @Test
        void should_playerCounter_when_defenseBeatsNormal() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input = createInput(SkillType.DEFENSE, SkillType.NORMAL,
                    MONSTER_NORMAL_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isGreaterThan(0);
            assertThat(result.countered()).isTrue();
        }

        /**
         * 강 vs 일반 (플레이어 패): 몬스터만 피해.
         */
        @Test
        void should_monsterDealDamage_when_heavyLosesToNormal() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input = createInput(SkillType.HEAVY, SkillType.NORMAL,
                    MONSTER_NORMAL_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isEqualTo(0);
            assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
        }

        /**
         * 방어 vs 강 (플레이어 패, 관통당함): 몬스터만 피해.
         */
        @Test
        void should_monsterPenetrate_when_defenseLosesToHeavy() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input = createInput(SkillType.DEFENSE, SkillType.HEAVY,
                    MONSTER_HEAVY_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isEqualTo(0);
            assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
        }

        /**
         * 일반 vs 방어 (플레이어 패, 몬스터 방어 승): 경감 + 반격.
         */
        @Test
        void should_monsterBlockAndCounter_when_normalLosesToDefense() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input = createInput(SkillType.NORMAL, SkillType.DEFENSE,
                    MONSTER_NORMAL_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
            assertThat(result.blocked()).isTrue();
            assertThat(result.countered()).isTrue();
        }

        /**
         * 일반 vs 일반 (무승부): 양쪽 50% 피해.
         */
        @Test
        void should_bothDamage_when_normalVsNormal() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input = createInput(SkillType.NORMAL, SkillType.NORMAL,
                    MONSTER_NORMAL_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isGreaterThan(0);
            assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
        }

        /**
         * 강 vs 강 (무승부): 양쪽 50% 피해.
         */
        @Test
        void should_bothDamage_when_heavyVsHeavy() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input = createInput(SkillType.HEAVY, SkillType.HEAVY,
                    MONSTER_HEAVY_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isGreaterThan(0);
            assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
        }

        /**
         * 방어 vs 방어 (교착): 양쪽 피해 0.
         */
        @Test
        void should_bothZero_when_defenseVsDefense() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input = createInput(SkillType.DEFENSE, SkillType.DEFENSE,
                    MONSTER_NORMAL_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isEqualTo(0);
            assertThat(result.monsterDamageToPlayer()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("감산 경계 테스트")
    class BaseDamageBoundary {

        /**
         * 방어력이 산출 피해를 초과하면 baseDamage는 정확히 1이다.
         */
        @Test
        void should_returnOne_when_defenseExceedsRawDamage() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final int attackPower = 50;
            final int multiplier = 100;
            final int defense = 200;

            final int result = resolver.baseDamage(attackPower, multiplier, defense);

            assertThat(result).isEqualTo(1);
        }

        /**
         * 방어력이 산출 피해와 정확히 같으면 baseDamage는 1이다 (0이 아님).
         */
        @Test
        void should_returnOne_when_defenseEqualsRawDamage() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final int attackPower = 100;
            final int multiplier = 100;
            final int defense = 100;

            final int result = resolver.baseDamage(attackPower, multiplier, defense);

            assertThat(result).isEqualTo(1);
        }

        /**
         * 방어력 0이면 baseDamage는 floor(atk*mult/100)과 동일하다.
         */
        @Test
        void should_returnFullDamage_when_defenseIsZero() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final int attackPower = 100;
            final int multiplier = 150;
            final int defense = 0;

            final int result = resolver.baseDamage(attackPower, multiplier, defense);

            assertThat(result).isEqualTo(150);
        }

        /**
         * floor 연산이 정확한지 확인 (소수점 버림).
         */
        @Test
        void should_floorCorrectly_when_multiplierCausesDecimal() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final int attackPower = 33;
            final int multiplier = 110;
            final int defense = 5;

            // floor(33 * 110 / 100) = floor(36.3) = 36, 36 - 5 = 31
            final int result = resolver.baseDamage(attackPower, multiplier, defense);

            assertThat(result).isEqualTo(31);
        }
    }

    @Nested
    @DisplayName("크리티컬 on/off 예시")
    class CriticalExamples {

        /**
         * critical=0이면 크리티컬이 절대 발동하지 않는다.
         */
        @Test
        void should_neverCritical_when_criticalIsZero() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final boolean result = resolver.rollCritical(0);

            assertThat(result).isFalse();
        }

        /**
         * critical=1000이면 크리티컬이 항상 발동한다.
         */
        @Test
        void should_alwaysCritical_when_criticalIsMax() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final boolean result = resolver.rollCritical(MAX_CRITICAL);

            assertThat(result).isTrue();
        }

        /**
         * 크리티컬 발동 시 resolve 결과에 playerCritical이 true로 표시된다.
         */
        @Test
        void should_markPlayerCritical_when_criticalTriggered() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input = new TurnInput(
                    SkillType.NORMAL, SkillType.HEAVY,
                    PLAYER_ATTACK, MONSTER_ATTACK,
                    PLAYER_DEFENSE, MONSTER_DEFENSE,
                    PLAYER_MULTIPLIER, MONSTER_NORMAL_MULTIPLIER,
                    BLOCK_RATE, BLOCK_RATE,
                    COUNTER_PERCENT, COUNTER_PERCENT,
                    MAX_CRITICAL, ZERO_CRITICAL
            );

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerCritical()).isTrue();
            assertThat(result.monsterCritical()).isFalse();
        }

        /**
         * 고정 시드에서 finalDamage 크리티컬 vs 비크리티컬 비율이 약 1.5배인지 확인한다.
         */
        @Test
        void should_criticalDamageBeHigher_when_comparedToNonCritical() {
            final int baseDamage = 100;
            final double affinityCoefficient = 1.0;

            final BattleResolver resolverCrit = new BattleResolver(new Random(FIXED_SEED));
            final int critDamage = resolverCrit.finalDamage(baseDamage, affinityCoefficient, true);

            final BattleResolver resolverNoCrit = new BattleResolver(new Random(FIXED_SEED));
            final int noCritDamage = resolverNoCrit.finalDamage(baseDamage, affinityCoefficient, false);

            // 동일 시드 → 동일 편차. 비율 차이는 정확히 1.5.
            assertThat(critDamage).isGreaterThan(noCritDamage);
            final double ratio = (double) critDamage / noCritDamage;
            assertThat(ratio).isBetween(1.4, 1.6);
        }
    }

    /**
     * 테스트용 TurnInput을 생성하는 헬퍼 메서드.
     *
     * @param playerType        플레이어 스킬 타입
     * @param monsterType       몬스터 스킬 타입
     * @param monsterMultiplier 몬스터 스킬 배율
     * @return 표준 수치의 TurnInput
     */
    private TurnInput createInput(final SkillType playerType, final SkillType monsterType,
                                  final int monsterMultiplier) {
        return new TurnInput(
                playerType, monsterType,
                PLAYER_ATTACK, MONSTER_ATTACK,
                PLAYER_DEFENSE, MONSTER_DEFENSE,
                PLAYER_MULTIPLIER, monsterMultiplier,
                BLOCK_RATE, BLOCK_RATE,
                COUNTER_PERCENT, COUNTER_PERCENT,
                ZERO_CRITICAL, ZERO_CRITICAL
        );
    }
}
