package com.myapps.web.myrpg.domain.model;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SkillRankPolicy}의 AP 소모 곡선이 양수·단조 증가이며
 * F→Master 합계가 200인지 검증하는 프로퍼티 테스트.
 *
 * <p>모든 비-MASTER 랭크에 대해 {@code apCost(rank) > 0}이고,
 * 랭크 순서 증가 시 apCost가 단조 증가(비감소)하며,
 * F부터 R1까지의 apCost 합계가 정확히 200이다. MASTER는 빈 값을 반환한다.
 *
 * <p><b>Validates: Requirements 6.1, 6.2</b>
 */
class SkillRankApCostPropertyTest {

    // Feature: 005-skill-system, Property 3: AP 소모 곡선 양수·단조·합 200

    private final SkillRankPolicy policy = new SkillRankPolicy();

    /**
     * 비-MASTER 랭크의 apCost가 양수인지 검증한다.
     *
     * @param rank MASTER가 아닌 임의의 SkillRank
     */
    @Property(tries = 100)
    void should_havePositiveApCost_when_notMaster(@ForAll("nonMasterRanks") final SkillRank rank) {
        final OptionalInt apCost = policy.apCost(rank);

        assertThat(apCost).isPresent();
        assertThat(apCost.getAsInt()).isGreaterThan(0);
    }

    /**
     * MASTER 랭크는 apCost가 빈 값인지 검증한다.
     */
    @Property(tries = 100)
    void should_returnEmpty_when_master() {
        final OptionalInt apCost = policy.apCost(SkillRank.MASTER);

        assertThat(apCost).isEmpty();
    }

    /**
     * 랭크 순서가 증가하면 apCost가 단조 증가(비감소)하는지 검증한다.
     *
     * @param rank next()가 존재하고 next()도 MASTER가 아닌 랭크
     */
    @Property(tries = 100)
    void should_increaseMonotonically_when_orderIncreases(
            @ForAll("ranksWithNextNonMaster") final SkillRank rank) {
        final SkillRank next = rank.next().orElseThrow();
        final int currentCost = policy.apCost(rank).orElseThrow();
        final int nextCost = policy.apCost(next).orElseThrow();

        assertThat(nextCost).isGreaterThanOrEqualTo(currentCost);
    }

    /**
     * F→R1(15개 전이)의 apCost 합계가 정확히 200인지 검증한다.
     */
    @Property(tries = 100)
    void should_sumTo200_when_allNonMasterRanksAggregated() {
        int totalAp = 0;
        for (final SkillRank rank : SkillRank.values()) {
            final OptionalInt apCost = policy.apCost(rank);
            if (apCost.isPresent()) {
                totalAp += apCost.getAsInt();
            }
        }
        assertThat(totalAp).isEqualTo(200);
    }

    /**
     * MASTER를 제외한 SkillRank 상수를 생성하는 Arbitrary 제공자.
     *
     * @return 비-MASTER SkillRank 중 하나를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<SkillRank> nonMasterRanks() {
        return Arbitraries.of(SkillRank.values())
                .filter(rank -> !rank.isMax());
    }

    /**
     * next()가 존재하고 next()도 MASTER가 아닌 SkillRank를 생성하는 Arbitrary 제공자.
     *
     * @return 적합한 SkillRank 중 하나를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<SkillRank> ranksWithNextNonMaster() {
        return Arbitraries.of(SkillRank.values())
                .filter(rank -> rank.next().isPresent() && !rank.next().get().isMax());
    }
}
