package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link DungeonInstance} 도메인 집계 단위 테스트. */
class DungeonInstanceTest {

    private MapGraph createSampleGraph() {
        final MapNode startNode =
                new MapNode(
                        "room-0-0",
                        "시작방",
                        "dungeon",
                        NodeType.DUNGEON,
                        0,
                        0,
                        null,
                        null,
                        List.of("room-1-0"));
        final MapNode midNode =
                new MapNode(
                        "room-1-0",
                        "통로방",
                        "dungeon",
                        NodeType.DUNGEON,
                        1,
                        0,
                        null,
                        null,
                        List.of("room-0-0", "room-2-0", "room-1-1"));
        final MapNode subNode =
                new MapNode(
                        "room-1-1",
                        "갈림길방",
                        "dungeon",
                        NodeType.DUNGEON,
                        1,
                        1,
                        null,
                        null,
                        List.of("room-1-0"));
        final MapNode bossNode =
                new MapNode(
                        "room-2-0",
                        "보스방",
                        "dungeon",
                        NodeType.DUNGEON,
                        2,
                        0,
                        null,
                        null,
                        List.of("room-1-0"));

        return new MapGraph(List.of(startNode, midNode, subNode, bossNode), List.of(), "room-0-0");
    }

    @Test
    @DisplayName("던전 인스턴스 생성 시 필드가 올바르게 초기화된다")
    void should_initializeFields_when_created() {
        // given
        final MapGraph graph = createSampleGraph();
        final Map<String, DungeonRoomState> roomStates =
                Map.of(
                        "room-0-0", new DungeonRoomState("room-0-0", true, true, List.of()),
                        "room-1-0",
                                new DungeonRoomState("room-1-0", false, true, List.of("spider")),
                        "room-1-1",
                                new DungeonRoomState(
                                        "room-1-1", false, false, List.of("red-spider")),
                        "room-2-0",
                                new DungeonRoomState(
                                        "room-2-0", false, false, List.of("giant-spider")));

        // when
        final DungeonInstance instance =
                new DungeonInstance(
                        1L,
                        "alby",
                        "alby-entrance",
                        "room-0-0",
                        "room-2-0",
                        "room-0-0",
                        graph,
                        roomStates);

        // then
        assertThat(instance.characterId()).isEqualTo(1L);
        assertThat(instance.dungeonId()).isEqualTo("alby");
        assertThat(instance.entranceNodeId()).isEqualTo("alby-entrance");
        assertThat(instance.startRoomId()).isEqualTo("room-0-0");
        assertThat(instance.bossRoomId()).isEqualTo("room-2-0");
        assertThat(instance.currentRoomId()).isEqualTo("room-0-0");
        assertThat(instance.dungeonGraph()).isNotNull();
        assertThat(instance.roomStates()).hasSize(4);
    }

    @Test
    @DisplayName("isRoomCleared 및 isRoomDiscovered 메서드가 방 상태를 정확히 조회한다")
    void should_checkRoomStatus_correctly() {
        // given
        final MapGraph graph = createSampleGraph();
        final Map<String, DungeonRoomState> roomStates =
                Map.of(
                        "room-0-0", new DungeonRoomState("room-0-0", true, true, List.of()),
                        "room-1-0",
                                new DungeonRoomState("room-1-0", false, true, List.of("spider")),
                        "room-1-1",
                                new DungeonRoomState(
                                        "room-1-1", false, false, List.of("red-spider")));
        final DungeonInstance instance =
                new DungeonInstance(
                        1L,
                        "alby",
                        "alby-entrance",
                        "room-0-0",
                        "room-2-0",
                        "room-0-0",
                        graph,
                        roomStates);

        // when & then
        assertThat(instance.isRoomCleared("room-0-0")).isTrue();
        assertThat(instance.isRoomDiscovered("room-0-0")).isTrue();

        assertThat(instance.isRoomCleared("room-1-0")).isFalse();
        assertThat(instance.isRoomDiscovered("room-1-0")).isTrue();

        assertThat(instance.isRoomCleared("room-1-1")).isFalse();
        assertThat(instance.isRoomDiscovered("room-1-1")).isFalse();

        assertThat(instance.isRoomCleared("non-existent")).isFalse();
        assertThat(instance.isRoomDiscovered("non-existent")).isFalse();
    }

    @Test
    @DisplayName("isAdjacentToBoss는 보스방과 직접 연결된 방에서만 true를 반환한다")
    void should_returnTrue_when_roomIsAdjacentToBoss() {
        // given
        final MapGraph graph = createSampleGraph();
        final Map<String, DungeonRoomState> roomStates = new HashMap<>();
        final DungeonInstance instance =
                new DungeonInstance(
                        1L,
                        "alby",
                        "alby-entrance",
                        "room-0-0",
                        "room-2-0",
                        "room-0-0",
                        graph,
                        roomStates);

        // when & then
        assertThat(instance.isAdjacentToBoss("room-1-0")).isTrue();
        assertThat(instance.isAdjacentToBoss("room-0-0")).isFalse();
        assertThat(instance.isAdjacentToBoss("room-1-1")).isFalse();
        assertThat(instance.isAdjacentToBoss("room-2-0")).isFalse();
    }

    @Test
    @DisplayName("moveTo 호출 시 현재 방이 갱신되고 대상 방 및 인접 방들이 발견 상태로 전이된다")
    void should_updateCurrentRoomAndRevealAdjacent_when_moveToCalled() {
        // given
        final MapGraph graph = createSampleGraph();
        final Map<String, DungeonRoomState> roomStates = new HashMap<>();
        roomStates.put("room-0-0", new DungeonRoomState("room-0-0", true, true, List.of()));
        roomStates.put(
                "room-1-0", new DungeonRoomState("room-1-0", false, true, List.of("spider")));
        roomStates.put(
                "room-1-1", new DungeonRoomState("room-1-1", false, false, List.of("red-spider")));
        roomStates.put(
                "room-2-0",
                new DungeonRoomState("room-2-0", false, false, List.of("giant-spider")));

        final DungeonInstance instance =
                new DungeonInstance(
                        1L,
                        "alby",
                        "alby-entrance",
                        "room-0-0",
                        "room-2-0",
                        "room-0-0",
                        graph,
                        roomStates);

        // when
        instance.moveTo("room-1-0");

        // then
        assertThat(instance.currentRoomId()).isEqualTo("room-1-0");
        assertThat(instance.isRoomDiscovered("room-1-0")).isTrue();
        assertThat(instance.isRoomDiscovered("room-1-1")).isTrue();
        assertThat(instance.isRoomDiscovered("room-2-0")).isTrue();
    }

    @Test
    @DisplayName("markCleared 호출 시 해당 방이 클리어 상태가 되고 몬스터가 비워진다")
    void should_setClearedTrueAndEmptyMonsters_when_markClearedCalled() {
        // given
        final MapGraph graph = createSampleGraph();
        final Map<String, DungeonRoomState> roomStates = new HashMap<>();
        roomStates.put(
                "room-1-0",
                new DungeonRoomState("room-1-0", false, true, List.of("spider", "goblin")));

        final DungeonInstance instance =
                new DungeonInstance(
                        1L,
                        "alby",
                        "alby-entrance",
                        "room-0-0",
                        "room-2-0",
                        "room-0-0",
                        graph,
                        roomStates);

        // when
        instance.markCleared("room-1-0");

        // then
        assertThat(instance.isRoomCleared("room-1-0")).isTrue();
        assertThat(instance.getRoomState("room-1-0").remainingMonsters()).isEmpty();
    }

    @Test
    @DisplayName("removeMonster 호출 시 단일 몬스터 제거 및 마지막 몬스터 처치 시 자동 클리어 전이된다")
    void should_removeMonsterAndAutoClear_when_lastMonsterDefeated() {
        // given
        final MapGraph graph = createSampleGraph();
        final Map<String, DungeonRoomState> roomStates = new HashMap<>();
        roomStates.put(
                "room-1-0",
                new DungeonRoomState("room-1-0", false, true, List.of("spider", "goblin")));

        final DungeonInstance instance =
                new DungeonInstance(
                        1L,
                        "alby",
                        "alby-entrance",
                        "room-0-0",
                        "room-2-0",
                        "room-0-0",
                        graph,
                        roomStates);

        // when (첫 번째 몬스터 처치)
        instance.removeMonster("room-1-0", "spider");

        // then (아직 고블린이 남아있어 미클리어)
        assertThat(instance.isRoomCleared("room-1-0")).isFalse();
        assertThat(instance.getRoomState("room-1-0").remainingMonsters()).containsExactly("goblin");

        // when (마지막 몬스터 처치)
        instance.removeMonster("room-1-0", "goblin");

        // then (모든 몬스터 처치되어 클리어 전이)
        assertThat(instance.isRoomCleared("room-1-0")).isTrue();
        assertThat(instance.getRoomState("room-1-0").remainingMonsters()).isEmpty();
    }

    @Test
    @DisplayName("currentRoomNode는 현재 플레이어가 위치한 방의 MapNode를 반환한다")
    void should_returnCurrentRoomNode() {
        // given
        final MapGraph graph = createSampleGraph();
        final Map<String, DungeonRoomState> roomStates = new HashMap<>();
        final DungeonInstance instance =
                new DungeonInstance(
                        1L,
                        "alby",
                        "alby-entrance",
                        "room-0-0",
                        "room-2-0",
                        "room-1-0",
                        graph,
                        roomStates);

        // when
        final Optional<MapNode> nodeOpt = instance.currentRoomNode();

        // then
        assertThat(nodeOpt).isPresent();
        assertThat(nodeOpt.get().id()).isEqualTo("room-1-0");
        assertThat(nodeOpt.get().name()).isEqualTo("통로방");
    }
}
