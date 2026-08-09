package com.myapps.web.myrpg.domain.model;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SkillDamagePolicy}의 랭크별 수치가 단조 증가하고 조회가 정확한지 검증하는 프로퍼티 테스트.
 *
 * <p>임의의 유효한 16-key 단조 맵을 가진 딜스킬/디펜스 스킬에 대해:
 * <ul>
 *   <li>딜스킬: {@code multiplier(skill, rank) == multiplierByRank.get(rank)}</li>
 *   <li>디펜스: {@code blockRate(skill, rank) == blockRateByRank.get(rank)},
 *       {@code counterMultiplier(skill, rank) == counterMultiplierByRank.get(rank)}</li>
 *   <li>모든 맵의 값은 F→MASTER 순서로 단조 비감소</li>
 * </ul>
 *
 * <p><b>Validates: Requirements 4.1, 4.2, 4.3, 4.4</b>
 */
class SkillDamagePolicyPropertyTest {

    // Feature: 005-skill-system, Property 6: 랭크별 수치 단조 + 조회 정확

    private static final int RANK_COUNT = 16;

    private final SkillDamagePolicy policy = new SkillDamagePolicy();

    /**
     * 딜스킬의 multiplier 조회가 맵의 값과 정확히 일치하는지 검증한다.
     *
     * @param skill 임의로 생성된 유효한 DamageSkill
     */
    @Property(tries = 100)
    void should_returnExactMultiplier_when_damageSkillQueried(
            @ForAll("damageSkills") final DamageSkill skill) {
        for (final SkillRank rank : SkillRank.values()) {
            final int expected = skill.multiplierByRank().get(rank);
            assertThat(policy.multiplier(skill, rank)).isEqualTo(expected);
        }
    }

    /**
     * 딜스킬의 multiplierByRank 값이 F→MASTER 순서로 단조 비감소인지 검증한다.
     *
     * @param skill 임의로 생성된 유효한 DamageSkill
     */
    @Property(tries = 100)
    void should_haveMonotonicMultiplier_when_damageSkill(
            @ForAll("damageSkills") final DamageSkill skill) {
        assertMonotonicNonDecreasing(skill.multiplierByRank());
    }

    /**
     * 디펜스 스킬의 blockRate 조회가 맵의 값과 정확히 일치하는지 검증한다.
     *
     * @param skill 임의로 생성된 유효한 DefenseSkill
     */
    @Property(tries = 100)
    void should_returnExactBlockRate_when_defenseSkillQueried(
            @ForAll("defenseSkills") final DefenseSkill skill) {
        for (final SkillRank rank : SkillRank.values()) {
            final int expected = skill.blockRateByRank().get(rank);
            assertThat(policy.blockRate(skill, rank)).isEqualTo(expected);
        }
    }

    /**
     * 디펜스 스킬의 counterMultiplier 조회가 맵의 값과 정확히 일치하는지 검증한다.
     *
     * @param skill 임의로 생성된 유효한 DefenseSkill
     */
    @Property(tries = 100)
    void should_returnExactCounterMultiplier_when_defenseSkillQueried(
            @ForAll("defenseSkills") final DefenseSkill skill) {
        for (final SkillRank rank : SkillRank.values()) {
            final int expected = skill.counterMultiplierByRank().get(rank);
            assertThat(policy.counterMultiplier(skill, rank)).isEqualTo(expected);
        }
    }

    /**
     * 디펜스 스킬의 blockRateByRank와 counterMultiplierByRank가
     * F→MASTER 순서로 단조 비감소인지 검증한다.
     *
     * @param skill 임의로 생성된 유효한 DefenseSkill
     */
    @Property(tries = 100)
    void should_haveMonotonicMaps_when_defenseSkill(
            @ForAll("defenseSkills") final DefenseSkill skill) {
        assertMonotonicNonDecreasing(skill.blockRateByRank());
        assertMonotonicNonDecreasing(skill.counterMultiplierByRank());
    }

    /**
     * 유효한 DamageSkill을 생성하는 Arbitrary 제공자.
     * multiplierByRank는 16개 랭크 키를 모두 보유하며 단조 비감소 값을 가진다.
     *
     * @return DamageSkill Arbitrary
     */
    @Provide
    Arbitrary<DamageSkill> damageSkills() {
        final Arbitrary<Map<SkillRank, Integer>> mapArb = monotoneRankMap();
        final Arbitrary<SkillType> typeArb = Arbitraries.of(SkillType.NORMAL, SkillType.HEAVY);
        final Arbitrary<SkillTalent> talentArb = Arbitraries.of(SkillTalent.MELEE, SkillTalent.ARCHERY, SkillTalent.MAGIC);

        return Combinators.combine(typeArb, talentArb, mapArb)
                .as((type, talent, multiplierMap) -> new DamageSkill(
                        "test-damage", "테스트딜", type, talent,
                        10, multiplierMap, "테스트 효과"));
    }

    /**
     * 유효한 DefenseSkill을 생성하는 Arbitrary 제공자.
     * blockRateByRank와 counterMultiplierByRank 모두 16개 키, 단조 비감소 값.
     *
     * @return DefenseSkill Arbitrary
     */
    @Provide
    Arbitrary<DefenseSkill> defenseSkills() {
        final Arbitrary<Map<SkillRank, Integer>> blockMapArb = monotoneRankMap();
        final Arbitrary<Map<SkillRank, Integer>> counterMapArb = monotoneRankMap();

        return Combinators.combine(blockMapArb, counterMapArb)
                .as((blockMap, counterMap) -> new DefenseSkill(
                        "test-defense", "테스트방어", SkillType.DEFENSE, SkillTalent.COMMON,
                        5, blockMap, counterMap, "테스트 방어 효과"));
    }

    /**
     * 16개 랭크 키에 대해 단조 비감소하는 양수 값을 가진 맵을 생성한다.
     * base(1~500)에서 시작하여 각 랭크마다 0~50의 증분을 누적한다.
     *
     * @return 단조 비감소 16-key EnumMap Arbitrary
     */
    private Arbitrary<Map<SkillRank, Integer>> monotoneRankMap() {
        final Arbitrary<Integer> baseArb = Arbitraries.integers().between(1, 500);
        final Arbitrary<int[]> incrementsArb = Arbitraries.integers().between(0, 50)
                .array(int[].class).ofSize(RANK_COUNT - 1);

        return Combinators.combine(baseArb, incrementsArb)
                .as((base, increments) -> {
                    final Map<SkillRank, Integer> map = new EnumMap<>(SkillRank.class);
                    final SkillRank[] ranks = SkillRank.values();
                    int current = base;
                    map.put(ranks[0], current);
                    for (int i = 1; i < RANK_COUNT; i++) {
                        current += increments[i - 1];
                        map.put(ranks[i], current);
                    }
                    return map;
                });
    }

    /**
     * 맵의 값이 F→MASTER 순서로 단조 비감소인지 검증한다.
     */
    private void assertMonotonicNonDecreasing(final Map<SkillRank, Integer> map) {
        final SkillRank[] ranks = SkillRank.values();
        for (int i = 1; i < ranks.length; i++) {
            assertThat(map.get(ranks[i]))
                    .as("rank %s should be >= rank %s", ranks[i].label(), ranks[i - 1].label())
                    .isGreaterThanOrEqualTo(map.get(ranks[i - 1]));
        }
    }
}
