package com.myapps.web.myrpg.application.service.battle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.DeathResult;
import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DungeonClearResult;
import com.myapps.web.myrpg.application.dto.LevelUpResult;
import com.myapps.web.myrpg.application.service.DungeonService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.application.service.ItemCatalogService;
import com.myapps.web.myrpg.application.service.MonsterRewardService;
import com.myapps.web.myrpg.application.service.ProgressionService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.BattleTurnResult.Outcome;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BattleRewardServiceTest {

    private MonsterRewardService monsterRewardService;
    private InventoryService inventoryService;
    private ProgressionService progressionService;
    private DungeonService dungeonService;
    private ItemCatalogService itemCatalogService;
    private GameMessageService gameMessageService;
    private ActionLog actionLog;
    private Random random;
    private BattleRewardService battleRewardService;

    private Monster monster;
    private CharacterProgress progress;
    private BattleState state;

    @BeforeEach
    void setUp() {
        monsterRewardService = mock(MonsterRewardService.class);
        inventoryService = mock(InventoryService.class);
        progressionService = mock(ProgressionService.class);
        dungeonService = mock(DungeonService.class);
        itemCatalogService = mock(ItemCatalogService.class);
        gameMessageService = mock(GameMessageService.class);
        actionLog = mock(ActionLog.class);
        random = new Random(42L);

        battleRewardService =
                new BattleRewardService(
                        monsterRewardService,
                        inventoryService,
                        progressionService,
                        dungeonService,
                        itemCatalogService,
                        gameMessageService,
                        actionLog,
                        random);

        monster =
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
        progress = CharacterProgress.createDefault();
        setId(progress, 1L);
        state = new BattleState(1L, "goblin", 0, false);
    }

    @Test
    @DisplayName("일반 몬스터 처치 시 드랍 보상을 획득하고 WIN 결과를 반환한다")
    void should_resolveWinAndDrop_when_normalMonsterKilled() {
        // Given
        final List<String> combatLines = new ArrayList<>();
        final List<String> settlementLines = new ArrayList<>();
        final DropResult dropResult = new DropResult(30, List.of());
        when(monsterRewardService.rollDrop(monster)).thenReturn(dropResult);
        when(progressionService.gainExperience(progress, monster.experience()))
                .thenReturn(new LevelUpResult(2, 1));
        when(gameMessageService.get(eq("battle.victory_reward"), any(), any()))
                .thenReturn("20 경험치, 30 골드 획득");
        when(gameMessageService.get(eq("battle.monster_slain"), eq("고블린")))
                .thenReturn("고블린을 처치했습니다!");

        // When
        final KillResolution resolution =
                battleRewardService.handleMonsterKilled(
                        progress, state, monster, combatLines, settlementLines);

        // Then
        assertThat(resolution.outcome()).isEqualTo(Outcome.WIN);
        assertThat(resolution.reward()).isEqualTo(dropResult);
        assertThat(state.isActive()).isFalse();
        assertThat(combatLines).contains("고블린을 처치했습니다!");
        verify(inventoryService).acquire(progress, dropResult);
    }

    @Test
    @DisplayName("던전 보스 처치 시 onBossDefeated를 호출하고 클리어 결과를 반환한다")
    void should_clearDungeon_when_dungeonBossKilled() {
        // Given
        final List<String> combatLines = new ArrayList<>();
        final List<String> settlementLines = new ArrayList<>();
        final DungeonInstance dungeon = mock(DungeonInstance.class);
        when(dungeon.bossRoomId()).thenReturn("boss_room");
        when(dungeon.currentRoomId()).thenReturn("boss_room");
        when(dungeonService.getActiveDungeon(1L)).thenReturn(Optional.of(dungeon));
        final DungeonClearResult clearResult =
                new DungeonClearResult("alby-normal", "알비 던전", 100, 200, List.of());
        when(dungeonService.onBossDefeated(1L)).thenReturn(clearResult);

        // When
        final KillResolution resolution =
                battleRewardService.handleMonsterKilled(
                        progress, state, monster, combatLines, settlementLines);

        // Then
        assertThat(resolution.outcome()).isEqualTo(Outcome.WIN);
        assertThat(resolution.dungeonClearResult()).isEqualTo(clearResult);
        assertThat(state.isActive()).isFalse();
        verify(dungeonService).onBossDefeated(1L);
    }

    @Test
    @DisplayName("플레이어 사망 시 handleDeath가 LOSE를 반환하고 사망 메시지를 추가한다")
    void should_returnLose_when_playerDeathHandled() {
        // Given
        final List<String> logLines = new ArrayList<>();
        final DeathResult deathResult = new DeathResult(10L);
        when(progressionService.die(progress)).thenReturn(deathResult);
        when(gameMessageService.get(eq("battle.death"), eq(10L))).thenReturn("사망하여 10 경험치를 잃었습니다.");

        // When
        final Outcome outcome = battleRewardService.handleDeath(progress, state, logLines);

        // Then
        assertThat(outcome).isEqualTo(Outcome.LOSE);
        assertThat(state.isActive()).isFalse();
        assertThat(logLines).contains("사망하여 10 경험치를 잃었습니다.");
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
