package com.myapps.web.myrpg.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myapps.web.myrpg.application.dto.FullMapCell;
import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.MinimapCell;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.exception.MapViewGenerationException;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.DungeonRoomState;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 던전 맵 뷰 팩토리 단위 테스트.
 *
 * <p>전장의 안개(Fog of War) 마스킹, 클리어/미클리어 노드 CSS 클래스 부여, 보스방 이름 마스킹(Requirement 4.1~4.6)을 검증한다.
 */
class MapViewFactoryDungeonTest {

    private final MapViewFactory mapViewFactory = new MapViewFactory();

    @Test
    @DisplayName("미발견 방은 미니맵 격자에서 완전히 제외(투명 처리)된다")
    void should_renderOnlyDiscoveredRooms_when_minimapCreatedForDungeon() {
        // given
        final MapNode r0 = createNode("room-0-0", "시작방", 0, 0, List.of("room-1-0"));
        final MapNode r1 = createNode("room-1-0", "던전 방", 1, 0, List.of("room-0-0", "room-2-0"));
        final MapNode r2 = createNode("room-2-0", "던전 방", 2, 0, List.of("room-1-0"));
        final MapGraph graph = new MapGraph(List.of(r0, r1, r2), List.of(), "room-0-0");

        final Map<String, DungeonRoomState> roomStates = new LinkedHashMap<>();
        roomStates.put("room-0-0", new DungeonRoomState("room-0-0", true, true, List.of()));
        roomStates.put(
                "room-1-0", new DungeonRoomState("room-1-0", false, true, List.of("spider")));
        roomStates.put(
                "room-2-0", new DungeonRoomState("room-2-0", false, false, List.of("spider")));

        final DungeonInstance dungeon =
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
        final MinimapView minimap = mapViewFactory.createMinimap(dungeon);

        // then
        final List<MinimapCell> cells = minimap.cells();
        assertThat(cells).hasSize(2);
        assertThat(cells.stream().map(MinimapCell::nodeId))
                .containsExactlyInAnyOrder("room-0-0", "room-1-0");
        assertThat(cells.stream().map(MinimapCell::nodeId)).doesNotContain("room-2-0");
    }

    @Test
    @DisplayName(
            "시작방은 dungeon-start, 보스방은 dungeon-boss, 일반 클리어 방은 dungeon-cleared, 일반 미클리어 방은 dungeon-uncleared 타입이 부여된다")
    void should_applyCorrectTypes_when_minimapCreatedForDungeon() {
        // given
        final MapNode r0 = createNode("room-0-0", "시작방", 0, 0, List.of("room-1-0"));
        final MapNode r1 = createNode("room-1-0", "던전 방", 1, 0, List.of("room-0-0", "room-2-0"));
        final MapNode r2 = createNode("room-2-0", "던전 방", 2, 0, List.of("room-1-0", "room-3-0"));
        final MapNode r3 = createNode("room-3-0", "보스방", 3, 0, List.of("room-2-0"));
        final MapGraph graph = new MapGraph(List.of(r0, r1, r2, r3), List.of(), "room-0-0");

        final Map<String, DungeonRoomState> roomStates =
                Map.of(
                        "room-0-0", new DungeonRoomState("room-0-0", true, true, List.of()),
                        "room-1-0", new DungeonRoomState("room-1-0", true, true, List.of()),
                        "room-2-0",
                                new DungeonRoomState("room-2-0", false, true, List.of("spider")),
                        "room-3-0",
                                new DungeonRoomState(
                                        "room-3-0", false, true, List.of("giant-spider")));

        final DungeonInstance dungeon =
                new DungeonInstance(
                        1L,
                        "alby",
                        "alby-entrance",
                        "room-0-0",
                        "room-3-0",
                        "room-0-0",
                        graph,
                        roomStates);

        // when
        final MinimapView minimap = mapViewFactory.createMinimap(dungeon);

        // then
        final MinimapCell cell0 =
                minimap.cells().stream()
                        .filter(c -> c.nodeId().equals("room-0-0"))
                        .findFirst()
                        .orElseThrow();
        final MinimapCell cell1 =
                minimap.cells().stream()
                        .filter(c -> c.nodeId().equals("room-1-0"))
                        .findFirst()
                        .orElseThrow();
        final MinimapCell cell2 =
                minimap.cells().stream()
                        .filter(c -> c.nodeId().equals("room-2-0"))
                        .findFirst()
                        .orElseThrow();
        final MinimapCell cell3 =
                minimap.cells().stream()
                        .filter(c -> c.nodeId().equals("room-3-0"))
                        .findFirst()
                        .orElseThrow();

        assertThat(cell0.type()).isEqualTo(MapViewFactory.TYPE_DUNGEON_START);
        assertThat(cell0.current()).isTrue();

        assertThat(cell1.type()).isEqualTo(MapViewFactory.TYPE_DUNGEON_CLEARED);
        assertThat(cell1.current()).isFalse();

        assertThat(cell2.type()).isEqualTo(MapViewFactory.TYPE_DUNGEON_UNCLEARED);
        assertThat(cell2.current()).isFalse();

        assertThat(cell3.type()).isEqualTo(MapViewFactory.TYPE_DUNGEON_BOSS);
        assertThat(cell3.current()).isFalse();
    }

    @Test
    @DisplayName("미니맵 통로는 두 방이 모두 발견 상태일 때만 linkRight/linkDown 플래그가 활성화된다")
    void should_onlyConnectEdgesToDiscoveredRooms_when_minimapCreatedForDungeon() {
        // given
        final MapNode r0 = createNode("room-0-0", "시작방", 0, 0, List.of("room-1-0"));
        final MapNode r1 = createNode("room-1-0", "던전 방", 1, 0, List.of("room-0-0", "room-2-0"));
        final MapNode r2 = createNode("room-2-0", "던전 방", 2, 0, List.of("room-1-0"));
        final MapGraph graph = new MapGraph(List.of(r0, r1, r2), List.of(), "room-0-0");

        // r0, r1 발견 / r2 미발견
        final Map<String, DungeonRoomState> roomStates =
                Map.of(
                        "room-0-0", new DungeonRoomState("room-0-0", true, true, List.of()),
                        "room-1-0",
                                new DungeonRoomState("room-1-0", false, true, List.of("spider")),
                        "room-2-0",
                                new DungeonRoomState("room-2-0", false, false, List.of("spider")));

        final DungeonInstance dungeon =
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
        final MinimapView minimap = mapViewFactory.createMinimap(dungeon);

        // then
        final MinimapCell cell0 =
                minimap.cells().stream()
                        .filter(c -> c.nodeId().equals("room-0-0"))
                        .findFirst()
                        .orElseThrow();
        final MinimapCell cell1 =
                minimap.cells().stream()
                        .filter(c -> c.nodeId().equals("room-1-0"))
                        .findFirst()
                        .orElseThrow();

        // r0 -> r1 (둘 다 발견) -> linkRight = true
        assertThat(cell0.linkRight()).isTrue();
        // r1 -> r2 (r2는 미발견) -> linkRight = false
        assertThat(cell1.linkRight()).isFalse();
    }

    @Test
    @DisplayName("전체지도에서 미발견 방은 제외되고 전체 바운딩 박스는 전체 그래프 기준으로 계산된다")
    void should_renderOnlyDiscoveredRooms_when_fullMapCreatedForDungeon() {
        // given
        final MapNode r0 = createNode("room-0-0", "시작방", 0, 0, List.of("room-1-0"));
        final MapNode r1 = createNode("room-1-0", "던전 방", 1, 0, List.of("room-0-0", "room-2-0"));
        final MapNode r2 = createNode("room-2-0", "던전 방", 2, 1, List.of("room-1-0"));
        final MapGraph graph = new MapGraph(List.of(r0, r1, r2), List.of(), "room-0-0");

        final Map<String, DungeonRoomState> roomStates =
                Map.of(
                        "room-0-0", new DungeonRoomState("room-0-0", true, true, List.of()),
                        "room-1-0",
                                new DungeonRoomState("room-1-0", false, true, List.of("spider")),
                        "room-2-0",
                                new DungeonRoomState("room-2-0", false, false, List.of("spider")));

        final DungeonInstance dungeon =
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
        final FullMapView fullMap = mapViewFactory.createFullMap(dungeon);

        // then
        // 전체 바운딩 박스: x [0, 2] -> columns = 3, y [0, 1] -> rows = 2
        assertThat(fullMap.columns()).isEqualTo(3);
        assertThat(fullMap.rows()).isEqualTo(2);

        // 셀 목록에는 발견된 r0, r1만 포함
        assertThat(fullMap.cells()).hasSize(2);
        assertThat(fullMap.cells().stream().map(FullMapCell::nodeId))
                .containsExactlyInAnyOrder("room-0-0", "room-1-0");
    }

    @Test
    @DisplayName("보스방은 플레이어가 직접 진입하거나 클리어하기 전까지 '던전 방'으로 이름이 마스킹된다")
    void should_maskBossRoomNameUntilEnteredOrCleared_when_fullMapCreatedForDungeon() {
        // given
        final MapNode r0 = createNode("room-0-0", "시작방", 0, 0, List.of("room-1-0"));
        final MapNode bossNode = createNode("room-1-0", "거대거미의 방", 1, 0, List.of("room-0-0"));
        final MapGraph graph = new MapGraph(List.of(r0, bossNode), List.of(), "room-0-0");

        // 1. 시작방 위치, 보스방 발견되었으나 미클리어 & 미진입
        final Map<String, DungeonRoomState> roomStates1 =
                Map.of(
                        "room-0-0", new DungeonRoomState("room-0-0", true, true, List.of()),
                        "room-1-0",
                                new DungeonRoomState(
                                        "room-1-0", false, true, List.of("giant-spider")));

        final DungeonInstance dungeonNotEntered =
                new DungeonInstance(
                        1L,
                        "alby",
                        "alby-entrance",
                        "room-0-0",
                        "room-1-0",
                        "room-0-0",
                        graph,
                        roomStates1);

        // when 1: 시작방에서 본 전체지도
        final FullMapView fullMap1 = mapViewFactory.createFullMap(dungeonNotEntered);
        final FullMapCell bossCell1 =
                fullMap1.cells().stream()
                        .filter(c -> c.nodeId().equals("room-1-0"))
                        .findFirst()
                        .orElseThrow();

        // then 1: 이름이 '던전 방'으로 마스킹
        assertThat(bossCell1.name()).isEqualTo("던전 방");

        // 2. 보스방으로 플레이어가 진입한 경우 (current = room-1-0)
        final DungeonInstance dungeonEntered =
                new DungeonInstance(
                        1L,
                        "alby",
                        "alby-entrance",
                        "room-0-0",
                        "room-1-0",
                        "room-1-0",
                        graph,
                        roomStates1);

        // when 2: 보스방 진입 시 전체지도
        final FullMapView fullMap2 = mapViewFactory.createFullMap(dungeonEntered);
        final FullMapCell bossCell2 =
                fullMap2.cells().stream()
                        .filter(c -> c.nodeId().equals("room-1-0"))
                        .findFirst()
                        .orElseThrow();

        // then 2: 원본 이름 노출
        assertThat(bossCell2.name()).isEqualTo("거대거미의 방");

        // 3. 보스방 클리어 후 시작방으로 돌아왔을 때
        final Map<String, DungeonRoomState> roomStatesCleared =
                Map.of(
                        "room-0-0", new DungeonRoomState("room-0-0", true, true, List.of()),
                        "room-1-0", new DungeonRoomState("room-1-0", true, true, List.of()));

        final DungeonInstance dungeonCleared =
                new DungeonInstance(
                        1L,
                        "alby",
                        "alby-entrance",
                        "room-0-0",
                        "room-1-0",
                        "room-0-0",
                        graph,
                        roomStatesCleared);

        // when 3: 클리어 후 전체지도
        final FullMapView fullMap3 = mapViewFactory.createFullMap(dungeonCleared);
        final FullMapCell bossCell3 =
                fullMap3.cells().stream()
                        .filter(c -> c.nodeId().equals("room-1-0"))
                        .findFirst()
                        .orElseThrow();

        // then 3: 클리어 상태이므로 원본 이름 노출
        assertThat(bossCell3.name()).isEqualTo("거대거미의 방");
    }

    @Test
    @DisplayName("전체지도 셀의 visibleLinks 목록은 발견된 방만 필터링하여 포함한다")
    void should_filterVisibleLinks_when_fullMapCreatedForDungeon() {
        // given
        final MapNode r0 = createNode("room-0-0", "시작방", 0, 0, List.of("room-1-0"));
        final MapNode r1 = createNode("room-1-0", "던전 방", 1, 0, List.of("room-0-0", "room-2-0"));
        final MapNode r2 = createNode("room-2-0", "던전 방", 2, 0, List.of("room-1-0"));
        final MapGraph graph = new MapGraph(List.of(r0, r1, r2), List.of(), "room-0-0");

        final Map<String, DungeonRoomState> roomStates =
                Map.of(
                        "room-0-0", new DungeonRoomState("room-0-0", true, true, List.of()),
                        "room-1-0",
                                new DungeonRoomState("room-1-0", false, true, List.of("spider")),
                        "room-2-0",
                                new DungeonRoomState("room-2-0", false, false, List.of("spider")));

        final DungeonInstance dungeon =
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
        final FullMapView fullMap = mapViewFactory.createFullMap(dungeon);

        // then
        final FullMapCell cell1 =
                fullMap.cells().stream()
                        .filter(c -> c.nodeId().equals("room-1-0"))
                        .findFirst()
                        .orElseThrow();

        // room-1-0의 전체 links는 room-0-0, room-2-0이지만, room-2-0이 미발견이므로 visibleLinks에는 room-0-0만 포함
        assertThat(cell1.links()).containsExactly("room-0-0");
    }

    @Test
    @DisplayName("유효하지 않은 인자가 전달되면 MapViewGenerationException 예외가 발생한다")
    void should_throwException_when_invalidArgumentsProvided() {
        // given
        final MapNode r0 = createNode("room-0-0", "시작방", 0, 0, List.of());
        final MapGraph graph = new MapGraph(List.of(r0), List.of(), "room-0-0");

        // when & then
        assertThatThrownBy(() -> mapViewFactory.createMinimap((DungeonInstance) null))
                .isInstanceOf(MapViewGenerationException.class);

        assertThatThrownBy(() -> mapViewFactory.createFullMap((DungeonInstance) null))
                .isInstanceOf(MapViewGenerationException.class);

        assertThatThrownBy(() -> mapViewFactory.createMinimap((MapGraph) null, "room-0-0"))
                .isInstanceOf(MapViewGenerationException.class);

        assertThatThrownBy(() -> mapViewFactory.createFullMap((MapGraph) null, "room-0-0"))
                .isInstanceOf(MapViewGenerationException.class);

        assertThatThrownBy(() -> mapViewFactory.createMinimap(graph, "non-existent-room", Map.of()))
                .isInstanceOf(MapViewGenerationException.class);

        assertThatThrownBy(
                        () ->
                                mapViewFactory.createFullMap(
                                        graph, "non-existent-room", Map.of(), null))
                .isInstanceOf(MapViewGenerationException.class);
    }

    private static MapNode createNode(
            final String id,
            final String name,
            final int x,
            final int y,
            final List<String> links) {
        return new MapNode(
                id,
                name,
                "dungeon",
                NodeType.DUNGEON,
                x,
                y,
                "alby",
                "dungeon-alby",
                links,
                List.of());
    }
}
