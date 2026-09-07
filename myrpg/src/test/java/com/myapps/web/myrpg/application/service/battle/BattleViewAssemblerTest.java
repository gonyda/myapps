package com.myapps.web.myrpg.application.service.battle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.application.dto.BattleView;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.PreemptiveParty;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BattleViewAssemblerTest {

    private GameMessageService gameMessageService;
    private BattleViewAssembler assembler;

    @BeforeEach
    void setUp() {
        gameMessageService = mock(GameMessageService.class);
        assembler = new BattleViewAssembler(gameMessageService);
    }

    @Test
    @DisplayName("스탠스에 따른 클래시 지속시간을 올바르게 반환한다")
    void should_resolveCorrectDuration_when_intentProvided() {
        assertThat(assembler.resolveClashDuration(SkillType.NORMAL)).isEqualTo(1000);
        assertThat(assembler.resolveClashDuration(SkillType.HEAVY)).isEqualTo(1500);
        assertThat(assembler.resolveClashDuration(SkillType.DEFENSE)).isEqualTo(1500);
        assertThat(assembler.resolveClashDuration(null)).isEqualTo(1500);
    }

    @Test
    @DisplayName("활 선제, 선제권, 몬스터 의도 우선순위에 따라 스탠스 뱃지 라벨을 반환한다")
    void should_resolveStanceBadgeLabel_when_variousConditions() {
        when(gameMessageService.get(eq("badge.stance.bow_first_strike"))).thenReturn("활 선제");
        when(gameMessageService.get(eq("badge.stance.preemptive_player"))).thenReturn("선제 공격");
        when(gameMessageService.get(eq("badge.stance.preemptive_monster"))).thenReturn("몬스터 선제");
        when(gameMessageService.get(eq("badge.stance.normal"))).thenReturn("일반");
        when(gameMessageService.get(eq("badge.stance.heavy"))).thenReturn("강공격");

        // 1. 활 선제 최우선
        assertThat(assembler.resolveStanceBadgeLabel(SkillType.NORMAL, true, PreemptiveParty.NONE))
                .isEqualTo("활 선제");

        // 2. 플레이어 선제
        assertThat(
                        assembler.resolveStanceBadgeLabel(
                                SkillType.NORMAL, false, PreemptiveParty.PLAYER))
                .isEqualTo("선제 공격");

        // 3. 몬스터 선제
        assertThat(
                        assembler.resolveStanceBadgeLabel(
                                SkillType.NORMAL, false, PreemptiveParty.MONSTER))
                .isEqualTo("몬스터 선제");

        // 4. 의도 기본 매핑
        assertThat(assembler.resolveStanceBadgeLabel(SkillType.HEAVY, false, PreemptiveParty.NONE))
                .isEqualTo("강공격");
        assertThat(assembler.resolveStanceBadgeLabel(null, false, PreemptiveParty.NONE)).isNull();
    }

    @Test
    @DisplayName("선제권 및 몬스터 의도에 따른 스탠스 뱃지 CSS 클래스를 반환한다")
    void should_resolveStanceBadgeClass_when_variousConditions() {
        assertThat(assembler.resolveStanceBadgeClass(SkillType.NORMAL, PreemptiveParty.PLAYER))
                .isEqualTo("badge-stance-preemptive-player");
        assertThat(assembler.resolveStanceBadgeClass(SkillType.NORMAL, PreemptiveParty.MONSTER))
                .isEqualTo("badge-stance-preemptive-monster");
        assertThat(assembler.resolveStanceBadgeClass(SkillType.HEAVY, PreemptiveParty.NONE))
                .isEqualTo("badge-stance-heavy");
        assertThat(assembler.resolveStanceBadgeClass(SkillType.DEFENSE, PreemptiveParty.NONE))
                .isEqualTo("badge-stance-defense");
        assertThat(assembler.resolveStanceBadgeClass(null, PreemptiveParty.NONE)).isNull();
    }

    @Test
    @DisplayName("대치 뷰 모델을 올바르게 조립하여 반환한다")
    void should_buildClashBattleView_when_validInputs() {
        // Given
        final BattleState state = new BattleState(1L, "goblin", 80, false);
        final Monster monster =
                new Monster(
                        "goblin",
                        "고블린",
                        MonsterType.NORMAL,
                        5,
                        100,
                        25,
                        10,
                        5,
                        20L,
                        new GoldDrop(5, 20),
                        List.of(),
                        List.of());
        final List<BattleSkillButton> skills = List.of();
        when(gameMessageService.get(eq("badge.stance.normal"))).thenReturn("일반");

        // When
        final BattleView view =
                assembler.buildClashBattleView(
                        state, monster, skills, SkillType.NORMAL, false, PreemptiveParty.NONE);

        // Then
        assertThat(view.monsterName()).isEqualTo("고블린");
        assertThat(view.monsterCurrentHp()).isEqualTo(80);
        assertThat(view.monsterMaxHp()).isEqualTo(100);
        assertThat(view.clashDurationMs()).isEqualTo(1000);
        assertThat(view.monsterStanceBadgeLabel()).isEqualTo("일반");
        assertThat(view.monsterStanceBadgeClass()).isEqualTo("badge-stance-normal");
        assertThat(view.bowFirstStrike()).isFalse();
    }
}
