package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BattleStateTest {

    @Test
    void test_battleState_initialization_and_mutations() {
        final BattleState state = new BattleState(1L, "raccoon", 100, true);

        assertThat(state.getCharacterId()).isEqualTo(1L);
        assertThat(state.getMonsterId()).isEqualTo("raccoon");
        assertThat(state.getMonsterCurrentHp()).isEqualTo(100);
        assertThat(state.getTurnCount()).isEqualTo(1);
        assertThat(state.isAmbush()).isTrue();
        assertThat(state.isActive()).isTrue();
        assertThat(state.isStandby()).isTrue();
        assertThat(state.getPreemptiveParty()).isEqualTo(PreemptiveParty.NONE);

        state.setMonsterCurrentHp(50);
        assertThat(state.getMonsterCurrentHp()).isEqualTo(50);

        state.setTurnCount(2);
        assertThat(state.getTurnCount()).isEqualTo(2);

        state.setActive(false);
        assertThat(state.isActive()).isFalse();

        state.setStandby(false);
        assertThat(state.isStandby()).isFalse();

        state.setCurrentMonsterIntent(SkillType.HEAVY);
        assertThat(state.getCurrentMonsterIntent()).isEqualTo(SkillType.HEAVY);

        state.setDungeonMonsterDeducted(true);
        assertThat(state.isDungeonMonsterDeducted()).isTrue();

        state.setPreemptiveParty(PreemptiveParty.PLAYER);
        assertThat(state.getPreemptiveParty()).isEqualTo(PreemptiveParty.PLAYER);

        state.setNextAttackAmpPercent(50);
        assertThat(state.getNextAttackAmpPercent()).isEqualTo(50);

        state.setManaShieldTurnsLeft(3);
        state.setManaShieldAbsorbRate(80);
        assertThat(state.getManaShieldTurnsLeft()).isEqualTo(3);
        assertThat(state.getManaShieldAbsorbRate()).isEqualTo(80);
        assertThat(state.hasActiveManaShield()).isTrue();

        state.decrementManaShieldTurns();
        assertThat(state.getManaShieldTurnsLeft()).isEqualTo(2);

        state.setMonsterStunnedTurns(2);
        assertThat(state.getMonsterStunnedTurns()).isEqualTo(2);
        assertThat(state.isMonsterStunned()).isTrue();

        state.decrementMonsterStunnedTurns();
        assertThat(state.getMonsterStunnedTurns()).isEqualTo(1);

        state.setDotDamagePerTurn(10);
        state.setDotTurnsLeft(3);
        assertThat(state.getDotDamagePerTurn()).isEqualTo(10);
        assertThat(state.getDotTurnsLeft()).isEqualTo(3);
        assertThat(state.hasActiveDot()).isTrue();

        state.decrementDotTurns();
        assertThat(state.getDotTurnsLeft()).isEqualTo(2);
    }
}
