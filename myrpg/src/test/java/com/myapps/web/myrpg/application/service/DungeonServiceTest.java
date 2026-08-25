package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.myapps.web.myrpg.application.dto.DroppedItem;
import com.myapps.web.myrpg.application.dto.DungeonBossSpec;
import com.myapps.web.myrpg.application.dto.DungeonClearResult;
import com.myapps.web.myrpg.application.dto.DungeonGenerationSpec;
import com.myapps.web.myrpg.application.dto.DungeonMonsterEntry;
import com.myapps.web.myrpg.application.dto.DungeonRewardSpec;
import com.myapps.web.myrpg.application.dto.DungeonSpec;
import com.myapps.web.myrpg.application.exception.BlockedMovementException;
import com.myapps.web.myrpg.application.exception.DungeonNotImplementedException;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.DungeonProgressEntity;
import com.myapps.web.myrpg.domain.model.DungeonRoomState;
import com.myapps.web.myrpg.domain.model.ItemDrop;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.DungeonProgressRepository;
import com.myapps.web.myrpg.domain.service.DungeonGenerator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link DungeonService} 단위 테스트.
 *
 * <p>던전 입장, 퇴장, 백트래킹 이동 제어, 몬스터 처치 동기화, 보스 처치 보상/복귀 및 사망 처리를 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class DungeonServiceTest {

    @Mock private DungeonSpecRepository dungeonSpecRepository;

    @Mock private DungeonGenerator dungeonGenerator;

    @Mock private DungeonProgressRepository dungeonProgressRepository;

    @Mock private CharacterProgressRepository characterProgressRepository;

    @Mock private ProgressionService progressionService;

    @Mock private InventoryService inventoryService;

    @Mock private MonsterRewardService monsterRewardService;

    @Mock private ItemCatalogService itemCatalogService;

    @Mock private ActionLog actionLog;

    private ObjectMapper objectMapper;
    private DungeonService dungeonService;

    private DungeonSpec albySpec;
    private DungeonSpec ciarSpec;
    private CharacterProgress character;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        dungeonService =
                new DungeonService(
                        dungeonSpecRepository,
                        dungeonGenerator,
                        dungeonProgressRepository,
                        characterProgressRepository,
                        progressionService,
                        inventoryService,
                        monsterRewardService,
                        itemCatalogService,
                        actionLog,
                        objectMapper);

        final DungeonGenerationSpec genSpec = new DungeonGenerationSpec(10, 10, 20, 23, 0.40, 3);
        final List<DungeonMonsterEntry> monsterPool =
                List.of(
                        new DungeonMonsterEntry("spider", 1, 2, 40),
                        new DungeonMonsterEntry("red-spider", 1, 2, 25));
        final DungeonBossSpec bossSpec = new DungeonBossSpec("giant-spider", "거대거미", "샤아악!");
        final DungeonRewardSpec rewardSpec =
                new DungeonRewardSpec(1000, 2000, List.of(new ItemDrop("hp_potion_30", 50, 1, 3)));

        albySpec =
                new DungeonSpec(
                        "alby",
                        "알비 던전",
                        "alby-entrance",
                        "dungeon-alby",
                        true,
                        genSpec,
                        monsterPool,
                        0.10,
                        bossSpec,
                        rewardSpec);

        ciarSpec =
                new DungeonSpec(
                        "ciar",
                        "키아 던전",
                        "ciar-entrance",
                        "dungeon-ciar",
                        false,
                        genSpec,
                        monsterPool,
                        0.10,
                        bossSpec,
                        rewardSpec);

        character = CharacterProgress.createDefault();
    }

    @Test
    @DisplayName("구현된 던전에 입장하면 프로시저럴 맵이 생성되고 DB에 저장되며 캐릭터가 시작방에 배치된다")
    void should_enterDungeon_when_validImplementedDungeon() {
        // given
        final Long charId = 1L;
        final DungeonInstance mockInstance = createSimpleInstance(charId, "alby");

        given(dungeonSpecRepository.findById("alby")).willReturn(Optional.of(albySpec));
        given(characterProgressRepository.findById(charId)).willReturn(Optional.of(character));
        given(dungeonGenerator.generate(albySpec, charId)).willReturn(mockInstance);

        // when
        final DungeonInstance result = dungeonService.enterDungeon(charId, "alby");

        // then
        assertThat(result).isNotNull();
        assertThat(character.getCurrentNodeId()).isEqualTo("room-0-0");
        then(dungeonProgressRepository).should().deleteByCharacterId(charId);
        then(dungeonProgressRepository).should().save(any(DungeonProgressEntity.class));
        then(characterProgressRepository).should().save(character);
        then(actionLog).should().add("알비 던전에 입장했습니다.", "dungeon");
    }

    @Test
    @DisplayName("미구현 던전에 입장 시도시 DungeonNotImplementedException이 발생한다")
    void should_throwDungeonNotImplemented_when_unimplementedDungeon() {
        // given
        final Long charId = 1L;
        given(dungeonSpecRepository.findById("ciar")).willReturn(Optional.of(ciarSpec));

        // when & then
        assertThatThrownBy(() -> dungeonService.enterDungeon(charId, "ciar"))
                .isInstanceOf(DungeonNotImplementedException.class)
                .hasMessageContaining("해당 던전은 아직 준비 중입니다");

        then(dungeonGenerator).shouldHaveNoInteractions();
        then(dungeonProgressRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("던전 나가기 호출 시 진행 엔티티가 삭제되고 캐릭터가 입구 노드로 복귀한다")
    void should_leaveDungeon_when_inDungeon() {
        // given
        final Long charId = 1L;
        final DungeonInstance instance = createSimpleInstance(charId, "alby");
        final DungeonProgressEntity entity = createEntity(instance);

        given(dungeonProgressRepository.findByCharacterId(charId)).willReturn(Optional.of(entity));
        given(characterProgressRepository.findById(charId)).willReturn(Optional.of(character));

        // when
        dungeonService.leaveDungeon(charId);

        // then
        then(dungeonProgressRepository).should().delete(entity);
        assertThat(character.getCurrentNodeId()).isEqualTo("alby-entrance");
        then(characterProgressRepository).should().save(character);
        then(actionLog).should().add("던전에서 나왔습니다.", "dungeon");
    }

    @Test
    @DisplayName("현재 방이 클리어된 상태에서는 연결된 임의의 이웃 방으로 자유롭게 이동할 수 있다")
    void should_moveToRoom_when_currentRoomCleared() {
        // given
        final Long charId = 1L;
        final DungeonInstance instance = createSimpleInstance(charId, "alby");
        // start room-0-0 is cleared
        final DungeonProgressEntity entity = createEntity(instance);

        given(dungeonProgressRepository.findByCharacterId(charId)).willReturn(Optional.of(entity));
        given(characterProgressRepository.findById(charId)).willReturn(Optional.of(character));

        // when
        final DungeonInstance updated = dungeonService.moveToRoom(charId, "room-1-0");

        // then
        assertThat(updated.currentRoomId()).isEqualTo("room-1-0");
        assertThat(character.getCurrentNodeId()).isEqualTo("room-1-0");
        then(dungeonProgressRepository).should().save(entity);
        then(characterProgressRepository).should().save(character);
    }

    @Test
    @DisplayName("현재 방이 미클리어 상태일 때 이미 클리어된 이전 방으로의 후퇴 이동은 허용된다")
    void should_allowRetreat_when_currentRoomUnclearedAndTargetCleared() {
        // given
        final Long charId = 1L;
        final DungeonInstance instance = createSimpleInstance(charId, "alby");
        // Move to room-1-0 (uncleared)
        instance.moveTo("room-1-0");
        final DungeonProgressEntity entity = createEntity(instance);

        given(dungeonProgressRepository.findByCharacterId(charId)).willReturn(Optional.of(entity));
        given(characterProgressRepository.findById(charId)).willReturn(Optional.of(character));

        // when: retreat back to room-0-0 (cleared)
        final DungeonInstance updated = dungeonService.moveToRoom(charId, "room-0-0");

        // then
        assertThat(updated.currentRoomId()).isEqualTo("room-0-0");
        assertThat(character.getCurrentNodeId()).isEqualTo("room-0-0");
        then(dungeonProgressRepository).should().save(entity);
    }

    @Test
    @DisplayName("현재 방이 미클리어 상태일 때 또 다른 미클리어 방으로의 전진은 BlockedMovementException으로 차단된다")
    void should_throwBlockedMovement_when_currentRoomUnclearedAndTargetUncleared() {
        // given
        final Long charId = 1L;
        final DungeonInstance instance = createLinearThreeRoomInstance(charId, "alby");
        // room-0-0 (cleared) -> room-1-0 (uncleared) -> room-2-0 (uncleared)
        instance.moveTo("room-1-0");
        final DungeonProgressEntity entity = createEntity(instance);

        given(dungeonProgressRepository.findByCharacterId(charId)).willReturn(Optional.of(entity));

        // when & then: try forward to room-2-0 (uncleared) while in room-1-0 (uncleared)
        assertThatThrownBy(() -> dungeonService.moveToRoom(charId, "room-2-0"))
                .isInstanceOf(BlockedMovementException.class)
                .hasMessageContaining("앞으로 나아가려면 이 방의 적들을 모두 처치해야 합니다");

        then(dungeonProgressRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("연결되지 않은 방으로 이동을 시도하면 BlockedMovementException이 발생한다")
    void should_throwBlockedMovement_when_roomsNotConnected() {
        // given
        final Long charId = 1L;
        final DungeonInstance instance = createSimpleInstance(charId, "alby");
        final DungeonProgressEntity entity = createEntity(instance);

        given(dungeonProgressRepository.findByCharacterId(charId)).willReturn(Optional.of(entity));

        // when & then: try to move to non-adjacent room
        assertThatThrownBy(() -> dungeonService.moveToRoom(charId, "room-9-9"))
                .isInstanceOf(BlockedMovementException.class)
                .hasMessageContaining("연결되지 않은 방입니다");
    }

    @Test
    @DisplayName("보스방과 인접한 방에 진입하면 불길한 기운 경고 힌트가 액션로그에 출력된다")
    void should_triggerBossWarning_when_movingToBossAdjacentRoom() {
        // given
        final Long charId = 1L;
        final DungeonInstance instance = createSimpleInstance(charId, "alby");
        // In simple instance, room-1-0 is adjacent to boss room-2-0
        final DungeonProgressEntity entity = createEntity(instance);

        given(dungeonProgressRepository.findByCharacterId(charId)).willReturn(Optional.of(entity));
        given(characterProgressRepository.findById(charId)).willReturn(Optional.of(character));

        // when
        dungeonService.moveToRoom(charId, "room-1-0");

        // then
        then(actionLog).should().add("어두운 통로 너머 깊은 곳에서 불길하고 강력한 기운이 느껴집니다...", "dungeon");
    }

    @Test
    @DisplayName("방 안의 몬스터를 격파하여 잔여 몬스터가 0이 되면 방이 클리어 상태로 전이되고 액션로그가 기록된다")
    void should_removeMonster_and_markCleared_when_allMonstersDefeated() {
        // given
        final Long charId = 1L;
        final DungeonInstance instance = createSimpleInstance(charId, "alby");
        instance.moveTo("room-1-0");
        final DungeonProgressEntity entity = createEntity(instance);

        given(dungeonProgressRepository.findByCharacterId(charId)).willReturn(Optional.of(entity));

        // when: defeat "spider" in room-1-0 (only 1 monster)
        dungeonService.onMonsterDefeated(charId, "spider");

        // then
        final DungeonInstance restored = dungeonService.restoreInstance(entity);
        assertThat(restored.isRoomCleared("room-1-0")).isTrue();
        assertThat(restored.getRoomState("room-1-0").remainingMonsters()).isEmpty();
        then(actionLog).should().add("방 안의 모든 적을 소탕했습니다! 전진 통로가 열립니다.", "combat");
        then(dungeonProgressRepository).should().save(entity);
    }

    @Test
    @DisplayName("보스 몬스터 격파 시 확정 보상이 지급되고 진행 엔티티가 삭제되며 캐릭터가 입구 노드로 복귀한다")
    void should_grantRewards_and_deleteEntity_and_returnToEntrance_when_bossDefeated() {
        // given
        final Long charId = 1L;
        final DungeonInstance instance = createSimpleInstance(charId, "alby");
        instance.moveTo("room-2-0");
        final DungeonProgressEntity entity = createEntity(instance);

        given(dungeonProgressRepository.findByCharacterId(charId)).willReturn(Optional.of(entity));
        given(dungeonSpecRepository.getById("alby")).willReturn(albySpec);
        given(characterProgressRepository.findById(charId)).willReturn(Optional.of(character));
        given(monsterRewardService.rollItemDrops(anyList()))
                .willReturn(List.of(new DroppedItem("hp_potion_30", 3)));

        // when
        final DungeonClearResult result = dungeonService.onBossDefeated(charId);

        // then
        assertThat(result.dungeonId()).isEqualTo("alby");
        assertThat(result.expGained()).isEqualTo(1000);
        assertThat(result.goldGained()).isEqualTo(2000);
        assertThat(result.items()).hasSize(1);

        then(progressionService).should().gainExperience(character, 1000);
        then(inventoryService).should().acquireItem("hp_potion_30", 3);
        then(dungeonProgressRepository).should().delete(entity);
        assertThat(character.getCurrentNodeId()).isEqualTo("alby-entrance");
        then(characterProgressRepository).should().save(character);
        then(actionLog).should().add("알비 던전을(를) 완전히 정복했습니다!", "dungeon");
    }

    @Test
    @DisplayName("사망 처리 시 활성 던전 진행 엔티티가 소멸되고 마을로 리스폰된다")
    void should_handlePlayerDeath_when_playerDiesInDungeon() {
        // given
        final Long charId = 1L;
        given(characterProgressRepository.findById(charId)).willReturn(Optional.of(character));

        // when
        dungeonService.handlePlayerDeath(charId);

        // then
        then(dungeonProgressRepository).should().deleteByCharacterId(charId);
        then(progressionService).should().die(character);
        then(characterProgressRepository).should().save(character);
    }

    private DungeonInstance createSimpleInstance(final Long characterId, final String dungeonId) {
        final MapNode startNode =
                new MapNode(
                        "room-0-0",
                        "시작방",
                        "dungeon",
                        NodeType.DUNGEON,
                        0,
                        0,
                        dungeonId,
                        "dungeon-alby",
                        List.of("room-1-0"),
                        List.of());
        final MapNode room1 =
                new MapNode(
                        "room-1-0",
                        "던전 방",
                        "dungeon",
                        NodeType.DUNGEON,
                        1,
                        0,
                        dungeonId,
                        "dungeon-alby",
                        List.of("room-0-0", "room-2-0"),
                        List.of("spider"));
        final MapNode bossRoom =
                new MapNode(
                        "room-2-0",
                        "거대거미의 방",
                        "dungeon",
                        NodeType.DUNGEON,
                        2,
                        0,
                        dungeonId,
                        "dungeon-alby",
                        List.of("room-1-0"),
                        List.of("giant-spider"));

        final MapGraph graph =
                new MapGraph(List.of(startNode, room1, bossRoom), List.of(), "room-0-0");

        final Map<String, DungeonRoomState> roomStates =
                Map.of(
                        "room-0-0", new DungeonRoomState("room-0-0", true, true, List.of()),
                        "room-1-0",
                                new DungeonRoomState("room-1-0", false, true, List.of("spider")),
                        "room-2-0",
                                new DungeonRoomState(
                                        "room-2-0", false, false, List.of("giant-spider")));

        return new DungeonInstance(
                characterId,
                dungeonId,
                "alby-entrance",
                "room-0-0",
                "room-2-0",
                "room-0-0",
                graph,
                roomStates);
    }

    private DungeonInstance createLinearThreeRoomInstance(
            final Long characterId, final String dungeonId) {
        final MapNode startNode =
                new MapNode(
                        "room-0-0",
                        "시작방",
                        "dungeon",
                        NodeType.DUNGEON,
                        0,
                        0,
                        dungeonId,
                        "dungeon-alby",
                        List.of("room-1-0"),
                        List.of());
        final MapNode room1 =
                new MapNode(
                        "room-1-0",
                        "던전 방 1",
                        "dungeon",
                        NodeType.DUNGEON,
                        1,
                        0,
                        dungeonId,
                        "dungeon-alby",
                        List.of("room-0-0", "room-2-0"),
                        List.of("spider"));
        final MapNode room2 =
                new MapNode(
                        "room-2-0",
                        "던전 방 2",
                        "dungeon",
                        NodeType.DUNGEON,
                        2,
                        0,
                        dungeonId,
                        "dungeon-alby",
                        List.of("room-1-0", "room-3-0"),
                        List.of("red-spider"));
        final MapNode bossRoom =
                new MapNode(
                        "room-3-0",
                        "거대거미의 방",
                        "dungeon",
                        NodeType.DUNGEON,
                        3,
                        0,
                        dungeonId,
                        "dungeon-alby",
                        List.of("room-2-0"),
                        List.of("giant-spider"));

        final MapGraph graph =
                new MapGraph(List.of(startNode, room1, room2, bossRoom), List.of(), "room-0-0");

        final Map<String, DungeonRoomState> roomStates =
                Map.of(
                        "room-0-0", new DungeonRoomState("room-0-0", true, true, List.of()),
                        "room-1-0",
                                new DungeonRoomState("room-1-0", false, true, List.of("spider")),
                        "room-2-0",
                                new DungeonRoomState(
                                        "room-2-0", false, false, List.of("red-spider")),
                        "room-3-0",
                                new DungeonRoomState(
                                        "room-3-0", false, false, List.of("giant-spider")));

        return new DungeonInstance(
                characterId,
                dungeonId,
                "alby-entrance",
                "room-0-0",
                "room-3-0",
                "room-0-0",
                graph,
                roomStates);
    }

    private DungeonProgressEntity createEntity(final DungeonInstance instance) {
        return new DungeonProgressEntity(
                instance.characterId(),
                instance.dungeonId(),
                instance.entranceNodeId(),
                instance.startRoomId(),
                instance.bossRoomId(),
                instance.currentRoomId(),
                dungeonService.serializeDungeonGraph(instance.dungeonGraph()),
                dungeonService.serializeRoomStates(instance.roomStates()));
    }
}
