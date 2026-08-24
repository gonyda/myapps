package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link SkillRankPolicy}의 요구치·AP 경계 예시 단위 테스트. */
class SkillRankPolicyTest {

    private final SkillRankPolicy policy = new SkillRankPolicy();

    @Test
    @DisplayName("F→E 승급 요구치: usage=5, kills=1, apCost=1")
    void should_returnFirstRankRequirement_when_rankIsF() {
        final Optional<RankUpRequirement> requirement = policy.requirement(SkillRank.F);
        final OptionalInt apCost = policy.apCost(SkillRank.F);

        assertThat(requirement).isPresent();
        assertThat(requirement.get().requiredUsage()).isEqualTo(5);
        assertThat(requirement.get().requiredKills()).isEqualTo(1);
        assertThat(apCost).isPresent();
        assertThat(apCost.getAsInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("1→Master 승급 요구치: usage=5000, kills=1500, apCost=34")
    void should_returnLastRankRequirement_when_rankIsR1() {
        final Optional<RankUpRequirement> requirement = policy.requirement(SkillRank.R1);
        final OptionalInt apCost = policy.apCost(SkillRank.R1);

        assertThat(requirement).isPresent();
        assertThat(requirement.get().requiredUsage()).isEqualTo(5000);
        assertThat(requirement.get().requiredKills()).isEqualTo(1500);
        assertThat(apCost).isPresent();
        assertThat(apCost.getAsInt()).isEqualTo(34);
    }

    @Test
    @DisplayName("MASTER는 승급 불가: requirement·apCost 모두 empty")
    void should_returnEmpty_when_rankIsMaster() {
        final Optional<RankUpRequirement> requirement = policy.requirement(SkillRank.MASTER);
        final OptionalInt apCost = policy.apCost(SkillRank.MASTER);

        assertThat(requirement).isEmpty();
        assertThat(apCost).isEmpty();
    }

    @Test
    @DisplayName("F→Master 전체 AP 소모 합계는 200")
    void should_sumApCostTo200_when_allRanksAggregated() {
        // given & when
        int totalAp = 0;
        for (final SkillRank rank : SkillRank.values()) {
            final OptionalInt apCost = policy.apCost(rank);
            if (apCost.isPresent()) {
                totalAp += apCost.getAsInt();
            }
        }

        // then
        assertThat(totalAp).isEqualTo(200);
    }

    @Test
    @DisplayName("궁극기 승급 요구치: F랭크 1회, 1랭크 20회, 막타 처치는 항상 0")
    void should_returnUltimateRequirements_when_ultimateType() {
        // given & when
        final Optional<RankUpRequirement> fReq = policy.ultimateRequirement(SkillRank.F);
        final Optional<RankUpRequirement> r1Req = policy.ultimateRequirement(SkillRank.R1);
        final Optional<RankUpRequirement> masterReq = policy.ultimateRequirement(SkillRank.MASTER);

        // then
        assertThat(fReq).isPresent();
        assertThat(fReq.get().requiredUsage()).isEqualTo(1);
        assertThat(fReq.get().requiredKills()).isZero();

        assertThat(r1Req).isPresent();
        assertThat(r1Req.get().requiredUsage()).isEqualTo(20);
        assertThat(r1Req.get().requiredKills()).isZero();

        assertThat(masterReq).isEmpty();
    }

    @Test
    @DisplayName("requirementFor: PASSIVE는 usage=0, kills=0 즉시 승급 반환")
    void should_returnZeroUsageRequirement_when_passiveType() {
        // given & when
        final Optional<RankUpRequirement> passiveReq =
                policy.requirementFor(SkillRank.F, SkillType.PASSIVE);

        // then
        assertThat(passiveReq).isPresent();
        assertThat(passiveReq.get().requiredUsage()).isZero();
        assertThat(passiveReq.get().requiredKills()).isZero();
    }
}
