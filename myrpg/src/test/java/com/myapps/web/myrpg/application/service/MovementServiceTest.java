package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myapps.web.myrpg.application.dto.MovementResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;
import com.myapps.web.myrpg.domain.model.TalentType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * MovementService 단위 테스트.
 *
 * <p>인접 이동 성공, 비인접 이동 거부, 던전 내부 진입 거부 시나리오를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2025-06-15T10:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private MapService mapService;

    private ActionLog actionLog;
    private MovementService movementService;

    @BeforeEach
    void setUp() {
        actionLog = new ActionLog(FIXED_CLOCK);
        movementService = new MovementService(mapService, actionLog);
    }

    @Test
    void should_returnMoved_when_neighborIsConnected() {
        // given
        final MapNode townNode = new MapNode("town-a", "마을A", "town", NodeType.TOWN,
                0, 0, null, null, List.of("field-b"));
        final MapNode fieldNode = new MapNode("field-b", "들판B", "field", NodeType.FIELD,
                1, 0, null, null, List.of("town-a"));

        final MapGraph graph = new MapGraph(List.of(townNode, fieldNode), List.of(), "town-a");

        when(mapService.node("town-a")).thenReturn(townNode);
        when(mapService.graph()).thenReturn(graph);

        final CharacterProgress progress = new CharacterProgress(
                "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "town-a");

        // when
        final MovementResult result = movementService.move(progress, 1, 0);

        // then
        assertThat(result).isInstanceOf(MovementResult.Moved.class);
        final MovementResult.Moved moved = (MovementResult.Moved) result;
        assertThat(moved.node().id()).isEqualTo("field-b");
        assertThat(moved.log().type()).isEqualTo("move");
        assertThat(moved.log().message()).contains("들판B");
        assertThat(progress.getCurrentNodeId()).isEqualTo("field-b");
    }

    @Test
    void should_returnMoved_when_targetIsDungeonEntrance() {
        // given
        final MapNode fieldNode = new MapNode("field-a", "들판A", "field", NodeType.FIELD,
                0, 0, null, null, List.of("dungeon-entrance"));
        final MapNode dungeonEntranceNode = new MapNode("dungeon-entrance", "던전입구", "dungeon",
                NodeType.DUNGEON, 1, 0, "d1", null, List.of("field-a"));

        final MapGraph graph = new MapGraph(List.of(fieldNode, dungeonEntranceNode), List.of(), "field-a");

        when(mapService.node("field-a")).thenReturn(fieldNode);
        when(mapService.graph()).thenReturn(graph);

        final CharacterProgress progress = new CharacterProgress(
                "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "field-a");

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
        final MapNode townNode = new MapNode("town-a", "마을A", "town", NodeType.TOWN,
                0, 0, null, null, List.of("field-b"));
        final MapNode fieldNode = new MapNode("field-b", "들판B", "field", NodeType.FIELD,
                1, 0, null, null, List.of("town-a"));

        final MapGraph graph = new MapGraph(List.of(townNode, fieldNode), List.of(), "town-a");

        when(mapService.node("town-a")).thenReturn(townNode);
        when(mapService.graph()).thenReturn(graph);

        final CharacterProgress progress = new CharacterProgress(
                "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "town-a");

        // when — no neighbor at (0, 1)
        final MovementResult result = movementService.move(progress, 0, 1);

        // then
        assertThat(result).isInstanceOf(MovementResult.Blocked.class);
        assertThat(progress.getCurrentNodeId()).isEqualTo("town-a");
    }

    @Test
    void should_returnBlocked_when_neighborExistsButNotLinked() {
        // given
        final MapNode townNode = new MapNode("town-a", "마을A", "town", NodeType.TOWN,
                0, 0, null, null, List.of());
        final MapNode fieldNode = new MapNode("field-b", "들판B", "field", NodeType.FIELD,
                1, 0, null, null, List.of());

        final MapGraph graph = new MapGraph(List.of(townNode, fieldNode), List.of(), "town-a");

        when(mapService.node("town-a")).thenReturn(townNode);
        when(mapService.graph()).thenReturn(graph);

        final CharacterProgress progress = new CharacterProgress(
                "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "town-a");

        // when — neighbor exists at (1,0) but not in links
        final MovementResult result = movementService.move(progress, 1, 0);

        // then
        assertThat(result).isInstanceOf(MovementResult.Blocked.class);
        assertThat(progress.getCurrentNodeId()).isEqualTo("town-a");
    }

    @Test
    void should_returnDungeonLocked_when_enterDungeonCalled() {
        // given
        final CharacterProgress progress = new CharacterProgress(
                "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "dungeon-entrance");

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
        final CharacterProgress progress = new CharacterProgress(
                "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "town-a");

        // when — even for non-existent dungeon
        final MovementResult result = movementService.enterDungeon(progress, "non-existent-dungeon");

        // then
        assertThat(result).isInstanceOf(MovementResult.DungeonLocked.class);
        assertThat(progress.getCurrentNodeId()).isEqualTo("town-a");
    }

    @Test
    void should_addMoveLogEntry_when_movementSucceeds() {
        // given
        final MapNode townNode = new MapNode("town-a", "마을A", "town", NodeType.TOWN,
                0, 0, null, null, List.of("field-b"));
        final MapNode fieldNode = new MapNode("field-b", "들판B", "field", NodeType.FIELD,
                1, 0, null, null, List.of("town-a"));

        final MapGraph graph = new MapGraph(List.of(townNode, fieldNode), List.of(), "town-a");

        when(mapService.node("town-a")).thenReturn(townNode);
        when(mapService.graph()).thenReturn(graph);

        final CharacterProgress progress = new CharacterProgress(
                "고니", 1, 1, 0L, TalentType.MELEE, null, 100, 100, 100, "town-a");

        // when
        movementService.move(progress, 1, 0);

        // then
        assertThat(actionLog.size()).isEqualTo(1);
        assertThat(actionLog.getEntries().getFirst().type()).isEqualTo("move");
        assertThat(actionLog.getEntries().getFirst().message()).contains("들판B");
    }
}
