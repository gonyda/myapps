package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * {@link SkillRank} 랭크 사다리의 정합성을 검증하는 프로퍼티 테스트.
 *
 * <p>order 범위(0~15), next 체인 연속성, MASTER만 isMax/next==empty 임을 검증한다.
 *
 * <p><b>Validates: Requirements 2.3</b>
 */
class SkillRankLadderPropertyTest {

    // Feature: 005-skill-system, Property 1: 랭크 사다리 정합

    /**
     * 모든 SkillRank의 order()가 0~15 범위이며 ordinal()과 일치하는지 검증한다.
     *
     * @param rank 임의의 SkillRank 상수
     */
    @Property(tries = 100)
    void should_haveOrderBetween0And15MatchingOrdinal_when_anyRank(
            @ForAll("ranks") final SkillRank rank) {
        assertThat(rank.order()).isBetween(0, 15);
        assertThat(rank.order()).isEqualTo(rank.ordinal());
    }

    /**
     * MASTER만 isMax()==true이고 next()==empty인지 검증한다. 비-MASTER 랭크는 isMax()==false이고 next()가 존재하며
     * order()+1인지 검증한다.
     *
     * @param rank 임의의 SkillRank 상수
     */
    @Property(tries = 100)
    void should_onlyMasterBeMax_when_anyRank(@ForAll("ranks") final SkillRank rank) {
        if (rank == SkillRank.MASTER) {
            assertThat(rank.isMax()).isTrue();
            assertThat(rank.next()).isEmpty();
        } else {
            assertThat(rank.isMax()).isFalse();
            assertThat(rank.next()).isPresent();
            assertThat(rank.next().get().order()).isEqualTo(rank.order() + 1);
        }
    }

    /** F에서 next() 체인을 따라가면 정확히 15홉 후 MASTER에 도달하는지 검증한다. */
    @Property(tries = 100)
    void should_reachMasterIn15Hops_when_startingFromF() {
        SkillRank current = SkillRank.F;
        int hops = 0;
        while (current.next().isPresent()) {
            current = current.next().get();
            hops++;
        }
        assertThat(hops).isEqualTo(15);
        assertThat(current).isEqualTo(SkillRank.MASTER);
    }

    /**
     * SkillRank 상수를 생성하는 Arbitrary 제공자.
     *
     * @return 전체 SkillRank 상수 중 하나를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<SkillRank> ranks() {
        return Arbitraries.of(SkillRank.values());
    }
}
