package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;
import com.myapps.web.myrpg.domain.model.TalentType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * MovementService 단위 테스트.
 *
 * <p>인접 이동 성공, 비인접 이동 거부, 던전 내부 진입 거부 시나리오를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2025-06-15T10:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock private MapService mapService;
    @Mock private DungeonService dungeonService;

    private ActionLog actionLog;
    private MovementService movementService;

    @BeforeEach
    void setUp() {
        actionLog = new ActionLog(FIXED_CLOCK);
        movementService = new MovementService(mapService, actionLog, dungeonService);
    }

    @Test
    void should_returnMoved_when_neighborIsConnected() {
        // given
        final MapNode townNode =
                new MapNode(
                        "town-a",
                        "마을A",
                        "town",
                        NodeType.TOWN,
                        0,
                        0,
                        null,
                        null,
                        List.of("field-b"));
        final MapNode fieldNode =
                new MapNode(
                        "field-b",
                        "들판B",
                        "field",
                        NodeType.FIELD,
                        1,
                        0,
                        null,
                        null,
                        List.of("town-a"));

        final MapGraph graph = new MapGraph(List.of(townNode, fieldNode), List.of(), "town-a");

        when(mapService.node("town-a")).thenReturn(townNode);
        when(mapService.graph()).thenReturn(graph);

        final CharacterProgress progress =
                new CharacterProgress(
                        "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "town-a", 0, 0L);

        // when
        final MovementResult result = movementService.move(progress, 1, 0);

        // then
        assertThat(result).isInstanceOf(MovementResult.Moved.class);
        final MovementResult.Moved moved = (MovementResult.Moved) result;
        assertThat(moved.node().id()).isEqualTo("field-b");
        assertThat(moved.log()).isNull();
        assertThat(progress.getCurrentNodeId()).isEqualTo("field-b");
    }

    @Test
    void should_returnMoved_when_targetIsDungeonEntrance() {
        // given
        final MapNode fieldNode =
                new MapNode(
                        "field-a",
                        "들판A",
                        "field",
                        NodeType.FIELD,
                        0,
                        0,
                        null,
                        null,
                        List.of("dungeon-entrance"));
        final MapNode dungeonEntranceNode =
                new MapNode(
                        "dungeon-entrance",
                        "던전입구",
                        "dungeon",
                        NodeType.DUNGEON,
                        1,
                        0,
                        "d1",
                        null,
                        List.of("field-a"));

        final MapGraph graph =
                new MapGraph(List.of(fieldNode, dungeonEntranceNode), List.of(), "field-a");

        when(mapService.node("field-a")).thenReturn(fieldNode);
        when(mapService.graph()).thenReturn(graph);

        final CharacterProgress progress =
                new CharacterProgress(
                        "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "field-a", 0, 0L);

        // when
        final MovementResult result = movementService.move(progress, 1, 0);

        // then
        assertThat(result).isInstanceOf(MovementResult.Moved.class);
        final MovementResult.Moved moved = (MovementResult.Moved) result;
        assertThat(moved.node().id()).isEqualTo("dungeon-entrance");
        assertThat(moved.node().nodeType()).isEqualTo(NodeType.DUNGEON);
        assertThat(progress.getCurrentNodeId()).isEqualTo("dungeon-entrance");
    }

    @Test
    void should_returnBlocked_when_noNeighborAtOffset() {
        // given
        final MapNode townNode =
                new MapNode(
                        "town-a",
                        "마을A",
                        "town",
                        NodeType.TOWN,
                        0,
                        0,
                        null,
                        null,
                        List.of("field-b"));
        final MapNode fieldNode =
                new MapNode(
                        "field-b",
                        "들판B",
                        "field",
                        NodeType.FIELD,
                        1,
                        0,
                        null,
                        null,
                        List.of("town-a"));

        final MapGraph graph = new MapGraph(List.of(townNode, fieldNode), List.of(), "town-a");

        when(mapService.node("town-a")).thenReturn(townNode);
        when(mapService.graph()).thenReturn(graph);

        final CharacterProgress progress =
                new CharacterProgress(
                        "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "town-a", 0, 0L);

        // when — no neighbor at (0, 1)
        final MovementResult result = movementService.move(progress, 0, 1);

        // then
        assertThat(result).isInstanceOf(MovementResult.Blocked.class);
        assertThat(progress.getCurrentNodeId()).isEqualTo("town-a");
    }

    @Test
    void should_returnBlocked_when_neighborExistsButNotLinked() {
        // given
        final MapNode townNode =
                new MapNode("town-a", "마을A", "town", NodeType.TOWN, 0, 0, null, null, List.of());
        final MapNode fieldNode =
                new MapNode("field-b", "들판B", "field", NodeType.FIELD, 1, 0, null, null, List.of());

        final MapGraph graph = new MapGraph(List.of(townNode, fieldNode), List.of(), "town-a");

        when(mapService.node("town-a")).thenReturn(townNode);
        when(mapService.graph()).thenReturn(graph);

        final CharacterProgress progress =
                new CharacterProgress(
                        "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "town-a", 0, 0L);

        // when — neighbor exists at (1,0) but not in links
        final MovementResult result = movementService.move(progress, 1, 0);

        // then
        assertThat(result).isInstanceOf(MovementResult.Blocked.class);
        assertThat(progress.getCurrentNodeId()).isEqualTo("town-a");
    }

    @Test
    void should_returnDungeonLocked_when_enterDungeonCalled() {
        // given
        final CharacterProgress progress =
                new CharacterProgress(
                        "고니",
                        1,
                        1,
                        0L,
                        TalentType.MELEE,
                        null,
                        100,
                        100,
                        100,
                        "dungeon-entrance",
                        0,
                        0L);

        // when
        final MovementResult result = movementService.enterDungeon(progress, "d1");

        // then
        assertThat(result).isInstanceOf(MovementResult.DungeonLocked.class);
        final MovementResult.DungeonLocked locked = (MovementResult.DungeonLocked) result;
        assertThat(locked.message()).isEqualTo("아직 준비 중입니다.");
        assertThat(progress.getCurrentNodeId()).isEqualTo("dungeon-entrance");
    }

    @Test
    void should_returnDungeonLocked_regardlessOfDungeonId() {
        // given
        final CharacterProgress progress =
                new CharacterProgress(
                        "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "town-a", 0, 0L);

        // when — even for non-existent dungeon
        final MovementResult result =
                movementService.enterDungeon(progress, "non-existent-dungeon");

        // then
        assertThat(result).isInstanceOf(MovementResult.DungeonLocked.class);
        assertThat(progress.getCurrentNodeId()).isEqualTo("town-a");
    }

    @Test
    void should_notAddMoveLogEntry_when_movementSucceeds() {
        // given
        final MapNode townNode =
                new MapNode(
                        "town-a",
                        "마을A",
                        "town",
                        NodeType.TOWN,
                        0,
                        0,
                        null,
                        null,
                        List.of("field-b"));
        final MapNode fieldNode =
                new MapNode(
                        "field-b",
                        "들판B",
                        "field",
                        NodeType.FIELD,
                        1,
                        0,
                        null,
                        null,
                        List.of("town-a"));

        final MapGraph graph = new MapGraph(List.of(townNode, fieldNode), List.of(), "town-a");

        when(mapService.node("town-a")).thenReturn(townNode);
        when(mapService.graph()).thenReturn(graph);

        final CharacterProgress progress =
                new CharacterProgress(
                        "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "town-a", 0, 0L);

        // when
        movementService.move(progress, 1, 0);

        // then
        assertThat(actionLog.size()).isEqualTo(0);
    }

    @Test
    void should_returnMoved_when_inDungeonAndNeighborConnected() {
        // given
        final MapNode room0 =
                new MapNode(
                        "room-0",
                        "시작방",
                        "dungeon",
                        NodeType.DUNGEON,
                        0,
                        0,
                        null,
                        null,
                        List.of("room-1"));
        new MapNode(
                "room-0", "시작방", "dungeon", NodeType.DUNGEON, 0, 0, null, null, List.of("room-1"));
        final MapNode room1 =
                new MapNode(
                        "room-1",
                        "던전방",
                        "dungeon",
                        NodeType.DUNGEON,
                        0,
                        1,
                        null,
                        null,
                        List.of("room-0"));
        final MapGraph dungeonGraph = new MapGraph(List.of(room0, room1), List.of(), "room-0");

        final com.myapps.web.myrpg.domain.model.DungeonInstance dungeon =
                new com.myapps.web.myrpg.domain.model.DungeonInstance(
                        1L,
                        "alby",
                        "alby-entrance",
                        "room-0",
                        "room-1",
                        "room-0",
                        dungeonGraph,
                        java.util.Map.of());

        when(dungeonService.getActiveDungeon(1L)).thenReturn(java.util.Optional.of(dungeon));

        final CharacterProgress progress =
                new CharacterProgress(
                        "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "room-0", 0, 0L);
        setId(progress, 1L);

        // when
        final MovementResult result = movementService.move(progress, 0, 1);

        // then
        assertThat(result).isInstanceOf(MovementResult.Moved.class);
        final MovementResult.Moved moved = (MovementResult.Moved) result;
        assertThat(moved.node().id()).isEqualTo("room-1");
    }

    @Test
    void should_returnBlocked_when_inDungeonAndBlockedMovementExceptionThrown() {
        // given
        final MapNode room0 =
                new MapNode(
                        "room-0",
                        "시작방",
                        "dungeon",
                        NodeType.DUNGEON,
                        0,
                        0,
                        null,
                        null,
                        List.of("room-1"));
        final MapNode room1 =
                new MapNode(
                        "room-1",
                        "던전방",
                        "dungeon",
                        NodeType.DUNGEON,
                        0,
                        1,
                        null,
                        null,
                        List.of("room-0"));
        final MapGraph dungeonGraph = new MapGraph(List.of(room0, room1), List.of(), "room-0");

        final com.myapps.web.myrpg.domain.model.DungeonInstance dungeon =
                new com.myapps.web.myrpg.domain.model.DungeonInstance(
                        1L,
                        "alby",
                        "alby-entrance",
                        "room-0",
                        "room-1",
                        "room-0",
                        dungeonGraph,
                        java.util.Map.of());

        when(dungeonService.getActiveDungeon(1L)).thenReturn(java.util.Optional.of(dungeon));
        when(dungeonService.moveToRoom(1L, "room-1"))
                .thenThrow(
                        new com.myapps.web.myrpg.application.exception.BlockedMovementException(
                                "몬스터를 처치해야 합니다."));

        final CharacterProgress progress =
                new CharacterProgress(
                        "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "room-0", 0, 0L);
        setId(progress, 1L);

        // when
        final MovementResult result = movementService.move(progress, 0, 1);

        // then
        assertThat(result).isInstanceOf(MovementResult.Blocked.class);
        final MovementResult.Blocked blocked = (MovementResult.Blocked) result;
        assertThat(blocked.message()).isEqualTo("몬스터를 처치해야 합니다.");
    }

    private void setId(final CharacterProgress progress, final Long id) {
        try {
            final java.lang.reflect.Field field = CharacterProgress.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(progress, id);
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
