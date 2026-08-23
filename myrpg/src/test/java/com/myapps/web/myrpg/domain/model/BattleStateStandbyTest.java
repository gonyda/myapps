package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

/**
 * {@link BattleState} 엔티티의 대치(standby) 상태 및 몬스터 의도(currentMonsterIntent) 관리 단위 테스트.
 *
 * <p>Requirements: 2.1 — 2단계 턴 사이클 대치 페이즈 및 몬스터 전조 의도 도메인 상태 검증
 */
class BattleStateStandbyTest {

    @Test
    @DisplayName("전투 생성 시 대치 상태(standby)는 true이고 몬스터 의도(currentMonsterIntent)는 null로 초기화된다")
    void should_initializeWithStandbyTrueAndNullIntent_when_created() {
        // given & when
        final BattleState state = new BattleState(1L, "goblin", 50, false);

        // then
        assertThat(state.isStandby()).isTrue();
        assertThat(state.getCurrentMonsterIntent()).isNull();
        assertThat(state.isActive()).isTrue();
        assertThat(state.getTurnCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("setStandby로 대치 상태와 공방 상태를 전환할 수 있다")
    void should_toggleStandby_when_setStandbyCalled() {
        // given
        final BattleState state = new BattleState(1L, "goblin", 50, false);

        // when - 공방 페이즈 전환
        state.setStandby(false);

        // then
        assertThat(state.isStandby()).isFalse();

        // when - 대치 페이즈 복귀
        state.setStandby(true);

        // then
        assertThat(state.isStandby()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(SkillType.class)
    @NullSource
    @DisplayName("setCurrentMonsterIntent로 몬스터 의도(NORMAL, HEAVY, DEFENSE, null)를 설정 및 조회할 수 있다")
    void should_setAndGetMonsterIntent_when_intentUpdated(final SkillType intent) {
        // given
        final BattleState state = new BattleState(1L, "goblin", 50, false);

        // when
        state.setCurrentMonsterIntent(intent);

        // then
        assertThat(state.getCurrentMonsterIntent()).isEqualTo(intent);
    }
}
