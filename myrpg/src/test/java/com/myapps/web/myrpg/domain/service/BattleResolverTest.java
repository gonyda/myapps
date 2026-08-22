package com.myapps.web.myrpg.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.HitResult;
import com.myapps.web.myrpg.domain.model.ResolvedTurn;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.TurnInput;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link BattleResolver}의 구체적 예시를 검증하는 단위 테스트.
 *
 * <p>9칸 매트릭스 각 셀의 대표 예시, 감산 경계(방어 &gt;= 공격 산출 → baseDamage == 1), 크리티컬 on/off 예시를 고정 시드 {@link
 * Random}으로 검증한다.
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

        /** 일반 vs 강 (플레이어 승): 플레이어만 피해. */
        @Test
        void should_playerDealDamage_when_normalBeatsHeavy() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input =
                    createInput(SkillType.NORMAL, SkillType.HEAVY, MONSTER_NORMAL_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isGreaterThan(0);
            assertThat(result.monsterDamageToPlayer()).isEqualTo(0);
            assertThat(result.blocked()).isFalse();
            assertThat(result.countered()).isFalse();
        }

        /** 강 vs 방어 (플레이어 승, 관통): 플레이어만 피해, 반격 무효. */
        @Test
        void should_playerPenetrate_when_heavyBeatsDefense() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input =
                    createInput(SkillType.HEAVY, SkillType.DEFENSE, MONSTER_NORMAL_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isGreaterThan(0);
            assertThat(result.monsterDamageToPlayer()).isEqualTo(0);
            assertThat(result.countered()).isFalse();
        }

        /** 방어 vs 일반 (플레이어 방어 승): 반격 발생 + 경감 피해. */
        @Test
        void should_playerCounter_when_defenseBeatsNormal() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input =
                    createInput(SkillType.DEFENSE, SkillType.NORMAL, MONSTER_NORMAL_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isGreaterThan(0);
            assertThat(result.countered()).isTrue();
        }

        /** 강 vs 일반 (플레이어 패): 몬스터만 피해. */
        @Test
        void should_monsterDealDamage_when_heavyLosesToNormal() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input =
                    createInput(SkillType.HEAVY, SkillType.NORMAL, MONSTER_NORMAL_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isEqualTo(0);
            assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
        }

        /** 방어 vs 강 (플레이어 패, 관통당함): 몬스터만 피해. */
        @Test
        void should_monsterPenetrate_when_defenseLosesToHeavy() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input =
                    createInput(SkillType.DEFENSE, SkillType.HEAVY, MONSTER_HEAVY_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isEqualTo(0);
            assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
        }

        /** 일반 vs 방어 (플레이어 패, 몬스터 방어 승): 경감 + 반격. */
        @Test
        void should_monsterBlockAndCounter_when_normalLosesToDefense() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input =
                    createInput(SkillType.NORMAL, SkillType.DEFENSE, MONSTER_NORMAL_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
            assertThat(result.blocked()).isTrue();
            assertThat(result.countered()).isTrue();
        }

        /** 일반 vs 일반 (무승부): 양쪽 50% 피해. */
        @Test
        void should_bothDamage_when_normalVsNormal() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input =
                    createInput(SkillType.NORMAL, SkillType.NORMAL, MONSTER_NORMAL_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isGreaterThan(0);
            assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
        }

        /** 강 vs 강 (무승부): 양쪽 50% 피해. */
        @Test
        void should_bothDamage_when_heavyVsHeavy() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input =
                    createInput(SkillType.HEAVY, SkillType.HEAVY, MONSTER_HEAVY_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isGreaterThan(0);
            assertThat(result.monsterDamageToPlayer()).isGreaterThan(0);
        }

        /** 방어 vs 방어 (교착): 양쪽 피해 0. */
        @Test
        void should_bothZero_when_defenseVsDefense() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input =
                    createInput(SkillType.DEFENSE, SkillType.DEFENSE, MONSTER_NORMAL_MULTIPLIER);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isEqualTo(0);
            assertThat(result.monsterDamageToPlayer()).isEqualTo(0);
        }

        /** 디펜스 100% 완전 방어: 몬스터 일반 공격에 대해 플레이어 피격 0, 반격 0. */
        @Test
        void should_takeZeroDamageAndZeroCounter_when_defenseBlocks100Percent() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input =
                    new TurnInput(
                            SkillType.DEFENSE,
                            SkillType.NORMAL,
                            PLAYER_ATTACK,
                            MONSTER_ATTACK,
                            PLAYER_DEFENSE,
                            MONSTER_DEFENSE,
                            PLAYER_MULTIPLIER,
                            MONSTER_NORMAL_MULTIPLIER,
                            100,
                            100,
                            0,
                            0,
                            ZERO_CRITICAL,
                            ZERO_CRITICAL,
                            1,
                            false);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.monsterDamageToPlayer()).isEqualTo(0);
            assertThat(result.playerDamageToMonster()).isEqualTo(0);
            assertThat(result.countered()).isFalse();
        }

        /** 몬스터 100% 디펜스: 플레이어 일반 공격 0 피해(막힘), 몬스터 반격 0. */
        @Test
        void should_dealZeroDamage_when_playerNormalBlockedBy100PercentMonsterDefense() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input =
                    new TurnInput(
                            SkillType.NORMAL,
                            SkillType.DEFENSE,
                            PLAYER_ATTACK,
                            MONSTER_ATTACK,
                            PLAYER_DEFENSE,
                            MONSTER_DEFENSE,
                            PLAYER_MULTIPLIER,
                            MONSTER_NORMAL_MULTIPLIER,
                            100,
                            100,
                            0,
                            0,
                            ZERO_CRITICAL,
                            ZERO_CRITICAL,
                            1,
                            false);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isEqualTo(0);
            assertThat(result.monsterDamageToPlayer()).isEqualTo(0);
            assertThat(result.blocked()).isTrue();
            assertThat(result.countered()).isFalse();
        }

        /** 카운터 어택 vs 일반 공격: 상대 공격력 비례 반격, 플레이어 0 피격. */
        @Test
        void should_dealCounterDamageBasedOnMonsterAttack_when_counterAttackVsNormal() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input =
                    new TurnInput(
                            SkillType.DEFENSE,
                            SkillType.NORMAL,
                            PLAYER_ATTACK,
                            MONSTER_ATTACK,
                            PLAYER_DEFENSE,
                            MONSTER_DEFENSE,
                            PLAYER_MULTIPLIER,
                            MONSTER_NORMAL_MULTIPLIER,
                            100,
                            100,
                            100,
                            0,
                            ZERO_CRITICAL,
                            ZERO_CRITICAL,
                            1,
                            true);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.monsterDamageToPlayer()).isEqualTo(0);
            assertThat(result.playerDamageToMonster()).isGreaterThan(0);
            assertThat(result.countered()).isTrue();
        }

        /** 카운터 어택 vs 강공격: 강공격도 흘려내며 상대 공격력 비례 반격, 플레이어 0 피격. */
        @Test
        void should_dealCounterDamageBasedOnMonsterAttack_when_counterAttackVsHeavy() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input =
                    new TurnInput(
                            SkillType.DEFENSE,
                            SkillType.HEAVY,
                            PLAYER_ATTACK,
                            MONSTER_ATTACK,
                            PLAYER_DEFENSE,
                            MONSTER_DEFENSE,
                            PLAYER_MULTIPLIER,
                            MONSTER_HEAVY_MULTIPLIER,
                            100,
                            100,
                            150,
                            0,
                            ZERO_CRITICAL,
                            ZERO_CRITICAL,
                            1,
                            true);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.monsterDamageToPlayer()).isEqualTo(0);
            assertThat(result.playerDamageToMonster()).isGreaterThan(0);
            assertThat(result.countered()).isTrue();
        }

        /** 카운터 어택 vs 디펜스: 교착 (양측 0 피해). */
        @Test
        void should_dealZeroDamage_when_counterAttackVsDefense() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input =
                    new TurnInput(
                            SkillType.DEFENSE,
                            SkillType.DEFENSE,
                            PLAYER_ATTACK,
                            MONSTER_ATTACK,
                            PLAYER_DEFENSE,
                            MONSTER_DEFENSE,
                            PLAYER_MULTIPLIER,
                            MONSTER_NORMAL_MULTIPLIER,
                            100,
                            100,
                            100,
                            0,
                            ZERO_CRITICAL,
                            ZERO_CRITICAL,
                            1,
                            true);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerDamageToMonster()).isEqualTo(0);
            assertThat(result.monsterDamageToPlayer()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("감산 경계 테스트")
    class BaseDamageBoundary {

        /** 방어력이 산출 피해를 초과하면 baseDamage는 정확히 1이다. */
        @Test
        void should_returnOne_when_defenseExceedsRawDamage() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final int attackPower = 50;
            final int multiplier = 100;
            final int defense = 200;

            final int result = resolver.baseDamage(attackPower, multiplier, defense);

            assertThat(result).isEqualTo(1);
        }

        /** 방어력이 산출 피해와 정확히 같으면 baseDamage는 1이다 (0이 아님). */
        @Test
        void should_returnOne_when_defenseEqualsRawDamage() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final int attackPower = 100;
            final int multiplier = 100;
            final int defense = 100;

            final int result = resolver.baseDamage(attackPower, multiplier, defense);

            assertThat(result).isEqualTo(1);
        }

        /** 방어력 0이면 baseDamage는 floor(atk*mult/100)과 동일하다. */
        @Test
        void should_returnFullDamage_when_defenseIsZero() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final int attackPower = 100;
            final int multiplier = 150;
            final int defense = 0;

            final int result = resolver.baseDamage(attackPower, multiplier, defense);

            assertThat(result).isEqualTo(150);
        }

        /** floor 연산이 정확한지 확인 (소수점 버림). */
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

        /** critical=0이면 크리티컬이 절대 발동하지 않는다. */
        @Test
        void should_neverCritical_when_criticalIsZero() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final boolean result = resolver.rollCritical(0);

            assertThat(result).isFalse();
        }

        /** critical=1000이면 크리티컬이 항상 발동한다. */
        @Test
        void should_alwaysCritical_when_criticalIsMax() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final boolean result = resolver.rollCritical(MAX_CRITICAL);

            assertThat(result).isTrue();
        }

        /** 크리티컬 발동 시 resolve 결과에 playerCritical이 true로 표시된다. */
        @Test
        void should_markPlayerCritical_when_criticalTriggered() {
            final BattleResolver resolver = new BattleResolver(new Random(FIXED_SEED));
            final TurnInput input =
                    new TurnInput(
                            SkillType.NORMAL,
                            SkillType.HEAVY,
                            PLAYER_ATTACK,
                            MONSTER_ATTACK,
                            PLAYER_DEFENSE,
                            MONSTER_DEFENSE,
                            PLAYER_MULTIPLIER,
                            MONSTER_NORMAL_MULTIPLIER,
                            BLOCK_RATE,
                            BLOCK_RATE,
                            COUNTER_PERCENT,
                            COUNTER_PERCENT,
                            MAX_CRITICAL,
                            ZERO_CRITICAL,
                            1);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerCritical()).isTrue();
            assertThat(result.monsterCritical()).isFalse();
        }

        /** 고정 시드에서 finalDamage 크리티컬 vs 비크리티컬 비율이 약 1.5배인지 확인한다. */
        @Test
        void should_criticalDamageBeHigher_when_comparedToNonCritical() {
            final int baseDamage = 100;
            final double affinityCoefficient = 1.0;

            final BattleResolver resolverCrit = new BattleResolver(new Random(FIXED_SEED));
            final int critDamage = resolverCrit.finalDamage(baseDamage, affinityCoefficient, true);

            final BattleResolver resolverNoCrit = new BattleResolver(new Random(FIXED_SEED));
            final int noCritDamage =
                    resolverNoCrit.finalDamage(baseDamage, affinityCoefficient, false);

            // 동일 시드 → 동일 편차. 비율 차이는 정확히 1.5.
            assertThat(critDamage).isGreaterThan(noCritDamage);
            final double ratio = (double) critDamage / noCritDamage;
            assertThat(ratio).isBetween(1.4, 1.6);
        }
    }

    @Nested
    @DisplayName("멀티히트 예시")
    class MultiHitExamples {

        private static final long MULTI_HIT_SEED = 77777L;
        private static final int WINDMILL_PER_HIT_MULTIPLIER = 65;
        private static final int WINDMILL_HIT_COUNT = 3;
        private static final int ARROW_REVOLVER_PER_HIT_MULTIPLIER = 50;
        private static final int ARROW_REVOLVER_HIT_COUNT = 4;
        private static final int HIGH_DEFENSE = 80;

        /** 3타 스킬(windmill급): 정확히 3개의 히트 결과, 각 ≥1, 합계 ≥3. */
        @Test
        void should_returnThreeHits_when_threeHitSkill() {
            final BattleResolver resolver = new BattleResolver(new Random(MULTI_HIT_SEED));

            final List<HitResult> hits =
                    resolver.multiHitDamage(
                            PLAYER_ATTACK,
                            WINDMILL_PER_HIT_MULTIPLIER,
                            MONSTER_DEFENSE,
                            1.0,
                            ZERO_CRITICAL,
                            WINDMILL_HIT_COUNT);

            assertThat(hits).hasSize(WINDMILL_HIT_COUNT);
            final int total = hits.stream().mapToInt(HitResult::damage).sum();
            assertThat(total).isGreaterThanOrEqualTo(WINDMILL_HIT_COUNT);
            for (final HitResult hit : hits) {
                assertThat(hit.damage()).isGreaterThanOrEqualTo(1);
            }
        }

        /** 4타 스킬(arrow_revolver급): 정확히 4개의 히트 결과, 각 ≥1, 합계 ≥4. */
        @Test
        void should_returnFourHits_when_fourHitSkill() {
            final BattleResolver resolver = new BattleResolver(new Random(MULTI_HIT_SEED));

            final List<HitResult> hits =
                    resolver.multiHitDamage(
                            PLAYER_ATTACK,
                            ARROW_REVOLVER_PER_HIT_MULTIPLIER,
                            MONSTER_DEFENSE,
                            1.0,
                            ZERO_CRITICAL,
                            ARROW_REVOLVER_HIT_COUNT);

            assertThat(hits).hasSize(ARROW_REVOLVER_HIT_COUNT);
            final int total = hits.stream().mapToInt(HitResult::damage).sum();
            assertThat(total).isGreaterThanOrEqualTo(ARROW_REVOLVER_HIT_COUNT);
            for (final HitResult hit : hits) {
                assertThat(hit.damage()).isGreaterThanOrEqualTo(1);
            }
        }

        /**
         * 고방어 상대: 3타 멀티히트(히트당 배율 65%) 총 피해가 단일(배율 195%)보다 작다.
         *
         * <p>방어 80에서 히트당 기본피해 = max(1, floor(100×65/100)−80) = max(1,-15) = 1. 단일 기본피해 = max(1,
         * floor(100×195/100)−80) = max(1, 115) = 115. 다단은 3×1수준, 단일은 115수준으로 고방어에서 다단이 크게 불리.
         */
        @Test
        void should_multiHitMuchWeaker_when_highDefense() {
            final int totalMultiplier = WINDMILL_PER_HIT_MULTIPLIER * WINDMILL_HIT_COUNT;

            final BattleResolver multiResolver = new BattleResolver(new Random(MULTI_HIT_SEED));
            final List<HitResult> multiHits =
                    multiResolver.multiHitDamage(
                            PLAYER_ATTACK,
                            WINDMILL_PER_HIT_MULTIPLIER,
                            HIGH_DEFENSE,
                            1.0,
                            ZERO_CRITICAL,
                            WINDMILL_HIT_COUNT);
            final int multiTotal = multiHits.stream().mapToInt(HitResult::damage).sum();

            final BattleResolver singleResolver = new BattleResolver(new Random(MULTI_HIT_SEED));
            final List<HitResult> singleHits =
                    singleResolver.multiHitDamage(
                            PLAYER_ATTACK, totalMultiplier, HIGH_DEFENSE, 1.0, ZERO_CRITICAL, 1);
            final int singleTotal = singleHits.getFirst().damage();

            assertThat(multiTotal).isLessThan(singleTotal);
        }

        /** 단일 히트 동치: hitCount=1 multiHitDamage와 직접 rollCritical→finalDamage가 동일하다. */
        @Test
        void should_matchSingleFinalDamage_when_hitCountIsOne() {
            final long seed = 99999L;
            final int multiplier = 120;

            final BattleResolver multiResolver = new BattleResolver(new Random(seed));
            final List<HitResult> hits =
                    multiResolver.multiHitDamage(
                            PLAYER_ATTACK, multiplier, MONSTER_DEFENSE, 1.0, ZERO_CRITICAL, 1);

            final BattleResolver directResolver = new BattleResolver(new Random(seed));
            final int baseDmg =
                    directResolver.baseDamage(PLAYER_ATTACK, multiplier, MONSTER_DEFENSE);
            final boolean crit = directResolver.rollCritical(ZERO_CRITICAL);
            final int expectedDmg = directResolver.finalDamage(baseDmg, 1.0, crit);

            assertThat(hits).hasSize(1);
            assertThat(hits.getFirst().damage()).isEqualTo(expectedDmg);
            assertThat(hits.getFirst().critical()).isEqualTo(crit);
        }

        /** resolve에서 3타 스킬 사용 시 playerHits가 3개이고 합계가 playerDamageToMonster와 일치한다. */
        @Test
        void should_resolvePlayerHitsMatch_when_threeHitAttackWins() {
            final BattleResolver resolver = new BattleResolver(new Random(MULTI_HIT_SEED));
            final TurnInput input =
                    new TurnInput(
                            SkillType.NORMAL,
                            SkillType.HEAVY,
                            PLAYER_ATTACK,
                            MONSTER_ATTACK,
                            PLAYER_DEFENSE,
                            MONSTER_DEFENSE,
                            WINDMILL_PER_HIT_MULTIPLIER,
                            MONSTER_NORMAL_MULTIPLIER,
                            BLOCK_RATE,
                            BLOCK_RATE,
                            COUNTER_PERCENT,
                            COUNTER_PERCENT,
                            ZERO_CRITICAL,
                            ZERO_CRITICAL,
                            WINDMILL_HIT_COUNT);

            final ResolvedTurn result = resolver.resolve(input);

            assertThat(result.playerHits()).hasSize(WINDMILL_HIT_COUNT);
            final int hitsSum = result.playerHits().stream().mapToInt(HitResult::damage).sum();
            assertThat(result.playerDamageToMonster()).isEqualTo(hitsSum);
        }
    }

    /**
     * 테스트용 TurnInput을 생성하는 헬퍼 메서드.
     *
     * @param playerType 플레이어 스킬 타입
     * @param monsterType 몬스터 스킬 타입
     * @param monsterMultiplier 몬스터 스킬 배율
     * @return 표준 수치의 TurnInput
     */
    private TurnInput createInput(
            final SkillType playerType, final SkillType monsterType, final int monsterMultiplier) {
        return new TurnInput(
                playerType,
                monsterType,
                PLAYER_ATTACK,
                MONSTER_ATTACK,
                PLAYER_DEFENSE,
                MONSTER_DEFENSE,
                PLAYER_MULTIPLIER,
                monsterMultiplier,
                BLOCK_RATE,
                BLOCK_RATE,
                COUNTER_PERCENT,
                COUNTER_PERCENT,
                ZERO_CRITICAL,
                ZERO_CRITICAL,
                1);
    }
}
