package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * {@link SkillRankPolicy}의 랭크업 요구치가 양수이고 단조 증가하는지 검증하는 프로퍼티 테스트.
 *
 * <p>모든 비-MASTER 랭크에 대해 {@code requirement(rank)}가 존재하며 {@code requiredUsage > 0}, {@code
 * requiredKills > 0}이고, 랭크 순서가 증가하면 두 요구치가 단조 증가한다. MASTER는 빈 값을 반환한다.
 *
 * <p><b>Validates: Requirements 5.1, 5.2</b>
 */
class SkillRankRequirementPropertyTest {

    // Feature: 005-skill-system, Property 2: 요구치 양수·단조 증가

    private final SkillRankPolicy policy = new SkillRankPolicy();

    /**
     * 비-MASTER 랭크의 요구치가 존재하며 양수인지 검증한다.
     *
     * @param rank MASTER가 아닌 임의의 SkillRank
     */
    @Property(tries = 100)
    void should_havePositiveRequirements_when_notMaster(
            @ForAll("nonMasterRanks") final SkillRank rank) {
        final Optional<RankUpRequirement> requirement = policy.requirement(rank);

        assertThat(requirement).isPresent();
        assertThat(requirement.get().requiredUsage()).isGreaterThan(0);
        assertThat(requirement.get().requiredKills()).isGreaterThan(0);
    }

    /** MASTER 랭크는 요구치가 빈 값인지 검증한다. */
    @Property(tries = 100)
    void should_returnEmpty_when_master() {
        final Optional<RankUpRequirement> requirement = policy.requirement(SkillRank.MASTER);

        assertThat(requirement).isEmpty();
    }

    /**
     * 랭크 순서가 증가하면 요구치(사용/막타)가 단조 증가하는지 검증한다.
     *
     * @param rank next()가 존재하고 next()도 MASTER가 아닌 랭크
     */
    @Property(tries = 100)
    void should_increaseMonotonically_when_orderIncreases(
            @ForAll("ranksWithNextNonMaster") final SkillRank rank) {
        final SkillRank next = rank.next().orElseThrow();
        final RankUpRequirement current = policy.requirement(rank).orElseThrow();
        final RankUpRequirement nextReq = policy.requirement(next).orElseThrow();

        assertThat(nextReq.requiredUsage()).isGreaterThan(current.requiredUsage());
        assertThat(nextReq.requiredKills()).isGreaterThan(current.requiredKills());
    }

    /**
     * MASTER를 제외한 SkillRank 상수를 생성하는 Arbitrary 제공자.
     *
     * @return 비-MASTER SkillRank 중 하나를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<SkillRank> nonMasterRanks() {
        return Arbitraries.of(SkillRank.values()).filter(rank -> !rank.isMax());
    }

    /**
     * next()가 존재하고 next()도 MASTER가 아닌 SkillRank를 생성하는 Arbitrary 제공자. (F ~ R2까지: 두 연속 랭크의 요구치를 비교하기
     * 위함)
     *
     * @return 적합한 SkillRank 중 하나를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<SkillRank> ranksWithNextNonMaster() {
        return Arbitraries.of(SkillRank.values())
                .filter(rank -> rank.next().isPresent() && !rank.next().get().isMax());
    }
}
