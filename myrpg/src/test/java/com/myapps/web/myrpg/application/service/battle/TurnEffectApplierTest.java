package com.myapps.web.myrpg.application.service.battle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.service.SkillService;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TurnEffectApplierTest {

    private SkillService skillService;
    private StatProgression statProgression;
    private GameMessageService gameMessageService;
    private TurnEffectApplier turnEffectApplier;

    @BeforeEach
    void setUp() {
        skillService = mock(SkillService.class);
        statProgression = mock(StatProgression.class);
        gameMessageService = mock(GameMessageService.class);
        turnEffectApplier =
                new TurnEffectApplier(skillService, statProgression, gameMessageService);
    }

    @Test
    @DisplayName("마나실드 활성 시 몬스터 피해를 MP로 흡수하고 남은 피해만 HP에 적용한다")
    void should_absorbDamageWithMp_when_manaShieldActive() {
        // Given
        final CharacterProgress progress = CharacterProgress.createDefault();
        // createDefault: hpCurrent=100, mpCurrent=100
        progress.spendMp(70); // mpCurrent = 30

        final BattleState state = new BattleState(1L, "goblin", 100, false);
        state.setManaShieldTurnsLeft(2);
        state.setManaShieldAbsorbRate(50); // 50% 흡수
        final List<String> combatLines = new ArrayList<>();

        // When (rawMonsterDamage = 40 -> 50% = 20 흡수, MP 30에서 20 소비 -> MP 10, monsterDamage 20 ->
        // HP 100-20=80)
        turnEffectApplier.applyManaShieldAndDamage(progress, state, 15, 40, combatLines);

        // Then
        assertThat(progress.getMpCurrent()).isEqualTo(10);
        assertThat(progress.getHpCurrent()).isEqualTo(80);
        assertThat(state.getMonsterCurrentHp()).isEqualTo(85);
        assertThat(state.getManaShieldTurnsLeft()).isEqualTo(1);
        assertThat(combatLines)
                .anyMatch(line -> line.contains("마나 실드") && line.contains("20 피해를 MP로 흡수"));
    }

    @Test
    @DisplayName("DoT 턴이 남아있으면 몬스터에게 틱 피해를 적용하고 턴을 1 감소시킨다")
    void should_applyDoTDamage_when_dotTurnsLeft() {
        // Given
        final BattleState state = new BattleState(1L, "goblin", 100, false);
        state.setDotTurnsLeft(2);
        state.setDotDamagePerTurn(15);
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
        final List<String> combatLines = new ArrayList<>();
        when(gameMessageService.get(eq("battle.dot"), any(), any())).thenReturn("고블린에게 15의 지속 피해!");

        // When
        turnEffectApplier.applyDoTTick(state, monster, combatLines);

        // Then
        assertThat(state.getMonsterCurrentHp()).isEqualTo(85);
        assertThat(state.getDotTurnsLeft()).isEqualTo(1);
        assertThat(combatLines).contains("고블린에게 15의 지속 피해!");
    }

    @Test
    @DisplayName("명상 턴 재생이 존재할 때 최대 MP를 초과하지 않고 MP를 회복한다")
    void should_recoverMp_when_meditationRegenExists() {
        // Given
        final CharacterProgress progress = CharacterProgress.createDefault();
        setId(progress, 1L);
        progress.spendMp(60); // mpCurrent = 40
        final List<String> combatLines = new ArrayList<>();

        when(skillService.meditationTurnRegen(1L)).thenReturn(10);
        when(statProgression.vitalMaxFor(anyInt(), any(TalentType.class)))
                .thenReturn(new VitalMax(100, 50, 100));
        when(skillService.rankupVitalBonus(1L)).thenReturn(new VitalMax(0, 0, 0));
        when(gameMessageService.get(eq("battle.meditation"), any())).thenReturn("명상으로 10 MP 회복");

        // When
        turnEffectApplier.applyMeditationRegen(progress, combatLines);

        // Then
        assertThat(progress.getMpCurrent()).isEqualTo(50);
        assertThat(combatLines).contains("명상으로 10 MP 회복");
    }

    private void setId(final CharacterProgress progress, final long id) {
        try {
            final java.lang.reflect.Field idField = CharacterProgress.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(progress, id);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
