package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** {@link SkillRank}의 라벨, 순서, next 체인, isMax, first()를 검증하는 단위 테스트. */
class SkillRankTest {

    @Test
    void should_return_correct_labels() {
        assertThat(SkillRank.F.label()).isEqualTo("F");
        assertThat(SkillRank.E.label()).isEqualTo("E");
        assertThat(SkillRank.D.label()).isEqualTo("D");
        assertThat(SkillRank.C.label()).isEqualTo("C");
        assertThat(SkillRank.B.label()).isEqualTo("B");
        assertThat(SkillRank.A.label()).isEqualTo("A");
        assertThat(SkillRank.R9.label()).isEqualTo("9");
        assertThat(SkillRank.R8.label()).isEqualTo("8");
        assertThat(SkillRank.R7.label()).isEqualTo("7");
        assertThat(SkillRank.R6.label()).isEqualTo("6");
        assertThat(SkillRank.R5.label()).isEqualTo("5");
        assertThat(SkillRank.R4.label()).isEqualTo("4");
        assertThat(SkillRank.R3.label()).isEqualTo("3");
        assertThat(SkillRank.R2.label()).isEqualTo("2");
        assertThat(SkillRank.R1.label()).isEqualTo("1");
        assertThat(SkillRank.MASTER.label()).isEqualTo("Master");
    }

    @Test
    void should_return_correct_order_values() {
        assertThat(SkillRank.F.order()).isEqualTo(0);
        assertThat(SkillRank.E.order()).isEqualTo(1);
        assertThat(SkillRank.A.order()).isEqualTo(5);
        assertThat(SkillRank.R9.order()).isEqualTo(6);
        assertThat(SkillRank.R1.order()).isEqualTo(14);
        assertThat(SkillRank.MASTER.order()).isEqualTo(15);
    }

    @Test
    void should_chain_next_from_F_to_E() {
        assertThat(SkillRank.F.next()).isPresent();
        assertThat(SkillRank.F.next().get()).isEqualTo(SkillRank.E);
    }

    @Test
    void should_chain_next_from_A_to_R9() {
        assertThat(SkillRank.A.next()).isPresent();
        assertThat(SkillRank.A.next().get()).isEqualTo(SkillRank.R9);
    }

    @Test
    void should_chain_next_from_R1_to_MASTER() {
        assertThat(SkillRank.R1.next()).isPresent();
        assertThat(SkillRank.R1.next().get()).isEqualTo(SkillRank.MASTER);
    }

    @Test
    void should_return_empty_next_for_MASTER() {
        assertThat(SkillRank.MASTER.next()).isEmpty();
    }

    @Test
    void should_return_isMax_only_for_MASTER() {
        assertThat(SkillRank.MASTER.isMax()).isTrue();
        assertThat(SkillRank.F.isMax()).isFalse();
        assertThat(SkillRank.R1.isMax()).isFalse();
    }

    @Test
    void should_return_F_as_first() {
        assertThat(SkillRank.first()).isEqualTo(SkillRank.F);
    }

    @Test
    void should_have_exactly_sixteen_constants() {
        assertThat(SkillRank.values()).hasSize(16);
    }
}
