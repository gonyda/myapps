package com.myapps.web.myrpg.domain.service;

import com.myapps.web.myrpg.application.dto.FullMapCell;
import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.MinimapCell;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.exception.MapViewGenerationException;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.DungeonRoomState;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 미니맵 및 전체지도 격자 데이터를 생성하는 순수 로직 팩토리.
 *
 * <p>외부 의존성 없이 {@link MapGraph}와 현재 노드 정보, 또는 {@link DungeonInstance}의 전장의 안개(Fog of War) 및 방 클리어
 * 상태를 바탕으로 뷰 렌더링에 필요한 격자 셀과 간선 정보를 산출한다. 현재 노드 id가 그래프에 존재하지 않으면 {@link
 * MapViewGenerationException}을 던진다.
 */
public class MapViewFactory {

    /** 미클리어 던전 방 CSS 타입 문자열. */
    public static final String TYPE_DUNGEON_UNCLEARED = "dungeon-uncleared";

    /** 클리어된 던전 방 CSS 타입 문자열. */
    public static final String TYPE_DUNGEON_CLEARED = "dungeon-cleared";

    /** 던전 시작방 CSS 타입 문자열. */
    public static final String TYPE_DUNGEON_START = "dungeon-start";

    /** 던전 보스방 CSS 타입 문자열. */
    public static final String TYPE_DUNGEON_BOSS = "dungeon-boss";

    /** 미진입 미클리어 방 마스킹 표시명. */
    private static final String DEFAULT_DUNGEON_ROOM_NAME = "던전 방";

    /** 미니맵 중심 노드의 grid-column 값. */
    private static final int MINIMAP_CENTER_COLUMN = 5;

    /** 미니맵 중심 노드의 grid-row 값. */
    private static final int MINIMAP_CENTER_ROW = 3;

    /** 미니맵 X 오프셋 최소값 (왼쪽 방향). */
    private static final int MINIMAP_DX_MIN = -4;

    /** 미니맵 X 오프셋 최대값 (오른쪽 방향). */
    private static final int MINIMAP_DX_MAX = 4;

    /** 미니맵 Y 오프셋 최소값 (위쪽 방향). */
    private static final int MINIMAP_DY_MIN = -2;

    /** 미니맵 Y 오프셋 최대값 (아래쪽 방향). */
    private static final int MINIMAP_DY_MAX = 2;

    /**
     * 월드맵 미니맵 뷰를 생성한다.
     *
     * <p>현재 노드를 중심(grid-column=5, grid-row=3)으로 dx∈[-4,4], dy∈[-2,2] 범위의 노드를 격자에 배치한다. 간선은 두 노드가 모두
     * 창 안에 있고 links로 실제 연결된 경우에만 linkRight/linkDown 플래그로 표현한다.
     *
     * @param graph 맵 그래프
     * @param currentNodeId 현재 노드 ID
     * @return 미니맵 뷰 모델
     * @throws MapViewGenerationException 현재 노드가 그래프에 존재하지 않을 때
     */
    public MinimapView createMinimap(final MapGraph graph, final String currentNodeId) {
        if (graph == null) {
            throw new MapViewGenerationException("graph must not be null");
        }
        final MapNode currentNode = resolveCurrentNode(graph, currentNodeId);
        final List<MinimapCell> cells = buildMinimapCells(graph, currentNode);
        return new MinimapView(currentNode.name(), cells);
    }

    /**
     * 던전 인스턴스 전용 미니맵 뷰를 생성한다.
     *
     * <p>전장의 안개(Fog of War)를 적용하여 {@code discovered == false}인 방은 미니맵에서 제외(투명 처리)하고, 발견된 방은
     * 시작방/보스방/클리어 여부에 따라 타입을 부여한다. 연결 간선은 두 방이 모두 발견 상태일 때만 렌더링한다.
     *
     * @param dungeonInstance 활성화된 던전 인스턴스
     * @return 미니맵 뷰 모델
     * @throws MapViewGenerationException 현재 노드가 그래프에 존재하지 않거나 인스턴스가 null일 때
     */
    public MinimapView createMinimap(final DungeonInstance dungeonInstance) {
        if (dungeonInstance == null) {
            throw new MapViewGenerationException("dungeonInstance must not be null");
        }
        return createMinimap(
                dungeonInstance.dungeonGraph(),
                dungeonInstance.currentRoomId(),
                dungeonInstance.roomStates(),
                dungeonInstance.startRoomId(),
                dungeonInstance.bossRoomId());
    }

    /**
     * 방 상태 맵이 주어진 던전 맵 그래프 기준 미니맵 뷰를 생성한다.
     *
     * @param graph 맵 그래프
     * @param currentNodeId 현재 노드 ID
     * @param roomStates 방별 상태 맵
     * @return 미니맵 뷰 모델
     * @throws MapViewGenerationException 현재 노드가 그래프에 존재하지 않을 때
     */
    public MinimapView createMinimap(
            final MapGraph graph,
            final String currentNodeId,
            final Map<String, DungeonRoomState> roomStates) {
        return createMinimap(graph, currentNodeId, roomStates, null, null);
    }

    /**
     * 방 상태 맵 및 시작/보스방 정보가 주어진 던전 맵 그래프 기준 미니맵 뷰를 생성한다.
     *
     * @param graph 맵 그래프
     * @param currentNodeId 현재 노드 ID
     * @param roomStates 방별 상태 맵
     * @param startRoomId 시작방 ID
     * @param bossRoomId 보스방 ID
     * @return 미니맵 뷰 모델
     * @throws MapViewGenerationException 현재 노드가 그래프에 존재하지 않을 때
     */
    public MinimapView createMinimap(
            final MapGraph graph,
            final String currentNodeId,
            final Map<String, DungeonRoomState> roomStates,
            final String startRoomId,
            final String bossRoomId) {
        if (graph == null) {
            throw new MapViewGenerationException("graph must not be null");
        }
        final MapNode currentNode = resolveCurrentNode(graph, currentNodeId);
        final List<MinimapCell> cells =
                buildDungeonMinimapCells(graph, currentNode, roomStates, startRoomId, bossRoomId);
        return new MinimapView(currentNode.name(), cells);
    }

    /**
     * 월드맵 전체지도 뷰를 생성한다.
     *
     * <p>모든 노드를 바운딩박스 기준 gridColumn=x-minX+1, gridRow=y-minY+1에 배치한다. 간선은 좌표상 오른쪽/아래 이웃이면서 links로
     * 실제 연결된 경우에만 linkRight/linkDown 플래그로 표현한다.
     *
     * @param graph 맵 그래프
     * @param currentNodeId 현재 노드 ID
     * @return 전체지도 뷰 모델
     * @throws MapViewGenerationException 현재 노드가 그래프에 존재하지 않을 때
     */
    public FullMapView createFullMap(final MapGraph graph, final String currentNodeId) {
        if (graph == null) {
            throw new MapViewGenerationException("graph must not be null");
        }
        final MapNode currentNode = resolveCurrentNode(graph, currentNodeId);
        final MapBounds bounds = computeBoundingBox(graph.nodes());
        final List<FullMapCell> cells = buildFullMapCells(graph, currentNode, bounds);

        return new FullMapView(cells, bounds.columns(), bounds.rows());
    }

    /**
     * 던전 인스턴스 전용 전체지도 뷰를 생성한다.
     *
     * <p>전장의 안개(Fog of War)를 적용하여 {@code discovered == false}인 방은 격자에서 제외(투명 처리)하고, 발견된 방은
     * 시작방/보스방/클리어 여부에 따라 타입을 부여한다. 보스방에 직접 진입하지 않고 인접 노출만 된 경우 방 이름을 마스킹한다.
     *
     * @param dungeonInstance 활성화된 던전 인스턴스
     * @return 전체지도 뷰 모델
     * @throws MapViewGenerationException 현재 노드가 그래프에 존재하지 않거나 인스턴스가 null일 때
     */
    public FullMapView createFullMap(final DungeonInstance dungeonInstance) {
        if (dungeonInstance == null) {
            throw new MapViewGenerationException("dungeonInstance must not be null");
        }
        return createFullMap(
                dungeonInstance.dungeonGraph(),
                dungeonInstance.currentRoomId(),
                dungeonInstance.roomStates(),
                dungeonInstance.bossRoomId(),
                dungeonInstance.startRoomId());
    }

    /**
     * 방 상태 맵과 보스방 정보가 주어진 던전 맵 그래프 기준 전체지도 뷰를 생성한다.
     *
     * @param graph 맵 그래프
     * @param currentNodeId 현재 노드 ID
     * @param roomStates 방별 상태 맵
     * @param bossRoomId 보스방 ID
     * @return 전체지도 뷰 모델
     * @throws MapViewGenerationException 현재 노드가 그래프에 존재하지 않을 때
     */
    public FullMapView createFullMap(
            final MapGraph graph,
            final String currentNodeId,
            final Map<String, DungeonRoomState> roomStates,
            final String bossRoomId) {
        return createFullMap(graph, currentNodeId, roomStates, bossRoomId, null);
    }

    /**
     * 방 상태 맵과 보스방/시작방 정보가 주어진 던전 맵 그래프 기준 전체지도 뷰를 생성한다.
     *
     * @param graph 맵 그래프
     * @param currentNodeId 현재 노드 ID
     * @param roomStates 방별 상태 맵
     * @param bossRoomId 보스방 ID
     * @param startRoomId 시작방 ID
     * @return 전체지도 뷰 모델
     * @throws MapViewGenerationException 현재 노드가 그래프에 존재하지 않을 때
     */
    public FullMapView createFullMap(
            final MapGraph graph,
            final String currentNodeId,
            final Map<String, DungeonRoomState> roomStates,
            final String bossRoomId,
            final String startRoomId) {
        if (graph == null) {
            throw new MapViewGenerationException("graph must not be null");
        }
        final MapNode currentNode = resolveCurrentNode(graph, currentNodeId);
        final MapBounds bounds = computeBoundingBox(graph.nodes());
        final List<FullMapCell> cells =
                buildDungeonFullMapCells(
                        graph, currentNode, bounds, roomStates, bossRoomId, startRoomId);

        return new FullMapView(cells, bounds.columns(), bounds.rows());
    }

    private MapNode resolveCurrentNode(final MapGraph graph, final String currentNodeId) {
        final Optional<MapNode> nodeOpt = graph.byId(currentNodeId);
        if (nodeOpt.isEmpty()) {
            throw new MapViewGenerationException("현재 노드를 확인할 수 없습니다: " + currentNodeId);
        }
        return nodeOpt.get();
    }

    private List<MinimapCell> buildMinimapCells(final MapGraph graph, final MapNode currentNode) {
        final List<MinimapCell> cells = new ArrayList<>();

        for (int dx = MINIMAP_DX_MIN; dx <= MINIMAP_DX_MAX; dx++) {
            for (int dy = MINIMAP_DY_MIN; dy <= MINIMAP_DY_MAX; dy++) {
                final Optional<MapNode> neighborOpt = graph.neighborByOffset(currentNode, dx, dy);
                if (neighborOpt.isPresent()) {
                    final MapNode node = neighborOpt.get();
                    final int gridColumn = MINIMAP_CENTER_COLUMN + dx;
                    final int gridRow = MINIMAP_CENTER_ROW + dy;
                    final boolean isCurrent = node.id().equals(currentNode.id());
                    final boolean linkRight = hasMinimapLink(graph, currentNode, node, 1, 0);
                    final boolean linkDown = hasMinimapLink(graph, currentNode, node, 0, 1);

                    cells.add(
                            new MinimapCell(
                                    node.id(),
                                    gridColumn,
                                    gridRow,
                                    node.type(),
                                    isCurrent,
                                    linkRight,
                                    linkDown));
                }
            }
        }
        return List.copyOf(cells);
    }

    private List<MinimapCell> buildDungeonMinimapCells(
            final MapGraph graph,
            final MapNode currentNode,
            final Map<String, DungeonRoomState> roomStates,
            final String startRoomId,
            final String bossRoomId) {
        final List<MinimapCell> cells = new ArrayList<>();

        for (int dx = MINIMAP_DX_MIN; dx <= MINIMAP_DX_MAX; dx++) {
            for (int dy = MINIMAP_DY_MIN; dy <= MINIMAP_DY_MAX; dy++) {
                final Optional<MapNode> neighborOpt = graph.neighborByOffset(currentNode, dx, dy);
                if (neighborOpt.isPresent()) {
                    final MapNode node = neighborOpt.get();
                    final DungeonRoomState state =
                            roomStates != null ? roomStates.get(node.id()) : null;
                    final boolean isDiscovered = state != null && state.discovered();
                    if (!isDiscovered) {
                        continue;
                    }

                    final int gridColumn = MINIMAP_CENTER_COLUMN + dx;
                    final int gridRow = MINIMAP_CENTER_ROW + dy;
                    final boolean isCurrent = node.id().equals(currentNode.id());
                    final boolean isCleared = state.cleared();
                    final String type =
                            resolveDungeonCellType(node.id(), isCleared, startRoomId, bossRoomId);
                    final boolean linkRight =
                            hasDungeonMinimapLink(graph, currentNode, node, 1, 0, roomStates);
                    final boolean linkDown =
                            hasDungeonMinimapLink(graph, currentNode, node, 0, 1, roomStates);

                    cells.add(
                            new MinimapCell(
                                    node.id(),
                                    gridColumn,
                                    gridRow,
                                    type,
                                    isCurrent,
                                    linkRight,
                                    linkDown));
                }
            }
        }
        return List.copyOf(cells);
    }

    private Optional<MapNode> findMinimapNeighbor(
            final MapGraph graph,
            final MapNode center,
            final MapNode node,
            final int edgeDx,
            final int edgeDy) {
        final int neighborX = node.x() + edgeDx;
        final int neighborY = node.y() + edgeDy;
        final int offsetFromCenter = neighborX - center.x();
        final int offsetYFromCenter = neighborY - center.y();

        if (offsetFromCenter < MINIMAP_DX_MIN
                || offsetFromCenter > MINIMAP_DX_MAX
                || offsetYFromCenter < MINIMAP_DY_MIN
                || offsetYFromCenter > MINIMAP_DY_MAX) {
            return Optional.empty();
        }

        return graph.byCoord(MapGraph.coordKey(neighborX, neighborY));
    }

    private boolean hasMinimapLink(
            final MapGraph graph,
            final MapNode center,
            final MapNode node,
            final int edgeDx,
            final int edgeDy) {
        final Optional<MapNode> neighborOpt =
                findMinimapNeighbor(graph, center, node, edgeDx, edgeDy);
        return neighborOpt.isPresent() && node.links().contains(neighborOpt.get().id());
    }

    private boolean hasDungeonMinimapLink(
            final MapGraph graph,
            final MapNode center,
            final MapNode node,
            final int edgeDx,
            final int edgeDy,
            final Map<String, DungeonRoomState> roomStates) {
        final Optional<MapNode> neighborOpt =
                findMinimapNeighbor(graph, center, node, edgeDx, edgeDy);
        if (neighborOpt.isEmpty()) {
            return false;
        }

        final MapNode neighborNode = neighborOpt.get();
        final DungeonRoomState neighborState =
                roomStates != null ? roomStates.get(neighborNode.id()) : null;
        if (neighborState == null || !neighborState.discovered()) {
            return false;
        }

        return node.links().contains(neighborNode.id());
    }

    private MapBounds computeBoundingBox(final List<MapNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return new MapBounds(0, 0, 0, 0);
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (final MapNode node : nodes) {
            minX = Math.min(minX, node.x());
            minY = Math.min(minY, node.y());
            maxX = Math.max(maxX, node.x());
            maxY = Math.max(maxY, node.y());
        }
        return new MapBounds(minX, minY, maxX, maxY);
    }

    private List<FullMapCell> buildFullMapCells(
            final MapGraph graph, final MapNode currentNode, final MapBounds bounds) {
        final List<FullMapCell> cells = new ArrayList<>();

        for (final MapNode node : graph.nodes()) {
            final int gridColumn = node.x() - bounds.minX() + 1;
            final int gridRow = node.y() - bounds.minY() + 1;
            final boolean isCurrent = node.id().equals(currentNode.id());
            final boolean linkRight = hasFullMapLink(graph, node, 1, 0);
            final boolean linkDown = hasFullMapLink(graph, node, 0, 1);

            cells.add(
                    new FullMapCell(
                            node.id(),
                            node.name(),
                            gridColumn,
                            gridRow,
                            node.type(),
                            isCurrent,
                            linkRight,
                            linkDown,
                            node.links()));
        }
        return List.copyOf(cells);
    }

    private List<FullMapCell> buildDungeonFullMapCells(
            final MapGraph graph,
            final MapNode currentNode,
            final MapBounds bounds,
            final Map<String, DungeonRoomState> roomStates,
            final String bossRoomId,
            final String startRoomId) {
        final List<FullMapCell> cells = new ArrayList<>();

        for (final MapNode node : graph.nodes()) {
            final DungeonRoomState state = roomStates != null ? roomStates.get(node.id()) : null;
            final boolean isDiscovered = state != null && state.discovered();
            if (!isDiscovered) {
                continue;
            }

            final int gridColumn = node.x() - bounds.minX() + 1;
            final int gridRow = node.y() - bounds.minY() + 1;
            final boolean isCurrent = node.id().equals(currentNode.id());
            final boolean isCleared = state.cleared();
            final String type =
                    resolveDungeonCellType(node.id(), isCleared, startRoomId, bossRoomId);
            final boolean linkRight = hasDungeonFullMapLink(graph, node, 1, 0, roomStates);
            final boolean linkDown = hasDungeonFullMapLink(graph, node, 0, 1, roomStates);

            final String displayName =
                    resolveDungeonNodeDisplayName(node, isCurrent, isCleared, bossRoomId);
            final List<String> visibleLinks = filterVisibleLinks(node.links(), roomStates);

            cells.add(
                    new FullMapCell(
                            node.id(),
                            displayName,
                            gridColumn,
                            gridRow,
                            type,
                            isCurrent,
                            linkRight,
                            linkDown,
                            visibleLinks));
        }
        return List.copyOf(cells);
    }

    private String resolveDungeonCellType(
            final String roomId,
            final boolean isCleared,
            final String startRoomId,
            final String bossRoomId) {
        if (startRoomId != null && roomId.equals(startRoomId)) {
            return TYPE_DUNGEON_START;
        }
        if (bossRoomId != null && roomId.equals(bossRoomId)) {
            return TYPE_DUNGEON_BOSS;
        }
        return isCleared ? TYPE_DUNGEON_CLEARED : TYPE_DUNGEON_UNCLEARED;
    }

    private String resolveDungeonNodeDisplayName(
            final MapNode node,
            final boolean isCurrent,
            final boolean isCleared,
            final String bossRoomId) {
        if (bossRoomId != null && node.id().equals(bossRoomId) && !isCurrent && !isCleared) {
            return DEFAULT_DUNGEON_ROOM_NAME;
        }
        return node.name();
    }

    private List<String> filterVisibleLinks(
            final List<String> links, final Map<String, DungeonRoomState> roomStates) {
        if (links == null || links.isEmpty() || roomStates == null) {
            return List.of();
        }
        final List<String> visible = new ArrayList<>();
        for (final String targetId : links) {
            final DungeonRoomState targetState = roomStates.get(targetId);
            if (targetState != null && targetState.discovered()) {
                visible.add(targetId);
            }
        }
        return List.copyOf(visible);
    }

    private boolean hasFullMapLink(
            final MapGraph graph, final MapNode node, final int edgeDx, final int edgeDy) {
        final Optional<MapNode> neighborOpt = graph.neighborByOffset(node, edgeDx, edgeDy);
        if (neighborOpt.isEmpty()) {
            return false;
        }
        return node.links().contains(neighborOpt.get().id());
    }

    private boolean hasDungeonFullMapLink(
            final MapGraph graph,
            final MapNode node,
            final int edgeDx,
            final int edgeDy,
            final Map<String, DungeonRoomState> roomStates) {
        final Optional<MapNode> neighborOpt = graph.neighborByOffset(node, edgeDx, edgeDy);
        if (neighborOpt.isEmpty()) {
            return false;
        }
        final MapNode neighbor = neighborOpt.get();
        final DungeonRoomState neighborState =
                roomStates != null ? roomStates.get(neighbor.id()) : null;
        final boolean neighborDiscovered = neighborState != null && neighborState.discovered();
        if (!neighborDiscovered) {
            return false;
        }
        return node.links().contains(neighbor.id());
    }

    private record MapBounds(int minX, int minY, int maxX, int maxY) {
        int columns() {
            return maxX - minX + 1;
        }

        int rows() {
            return maxY - minY + 1;
        }
    }
}
