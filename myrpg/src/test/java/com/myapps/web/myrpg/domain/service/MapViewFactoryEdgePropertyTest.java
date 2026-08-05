package com.myapps.web.myrpg.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

import com.myapps.web.myrpg.application.dto.FullMapCell;
import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.MinimapCell;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.domain.model.Dungeon;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 뷰 간선 정합성 프로퍼티 테스트.
 *
 * <p>임의의 유효 맵 그래프(완전 연결 및 부분 연결 격자)에 대해
 * {@link MapViewFactory#createMinimap}과 {@link MapViewFactory#createFullMap}이
 * 생성하는 뷰 셀의 {@code linkRight}/{@code linkDown} 플래그가 다음 쌍조건과
 * 정확히 일치하는지 검증한다:
 *
 * <pre>
 *   linkRight ⟺ (오른쪽 이웃이 표시 범위 내 ∧ links에 실제 연결)
 *   linkDown  ⟺ (아래쪽 이웃이 표시 범위 내 ∧ links에 실제 연결)
 * </pre>
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 5: 뷰 간선 정합성
 *
 * <p><b>Validates: Requirements 8.3, 8.7</b>
 */
class MapViewFactoryEdgePropertyTest {

    private static final int GRID_SIZE_MIN = 2;
    private static final int GRID_SIZE_MAX = 6;
    private static final int MINIMAP_CENTER_COLUMN = 5;
    private static final int MINIMAP_CENTER_ROW = 3;
    private static final int MINIMAP_DX_MIN = -4;
    private static final int MINIMAP_DX_MAX = 4;
    private static final int MINIMAP_DY_MIN = -2;
    private static final int MINIMAP_DY_MAX = 2;

    private final MapViewFactory mapViewFactory = new MapViewFactory();

    /**
     * 미니맵에서 각 셀의 linkRight/linkDown 플래그가 쌍조건을 만족하는지 검증한다.
     *
     * <p>linkRight=true ⟺ (오른쪽 이웃이 미니맵 표시 범위 내 ∧ links에 실제 연결)
     * <p>linkDown=true  ⟺ (아래쪽 이웃이 미니맵 표시 범위 내 ∧ links에 실제 연결)
     *
     * @param graphAndCurrent 임의 생성된 맵 그래프(부분 연결 포함)와 현재 노드 ID 튜플
     */
    @Property(tries = 100)
    void should_satisfyEdgeBiconditional_when_minimapCreated(
            @ForAll("graphWithPartialConnectivity") final Tuple.Tuple2<MapGraph, String> graphAndCurrent) {

        final MapGraph graph = graphAndCurrent.get1();
        final String currentNodeId = graphAndCurrent.get2();
        final MapNode currentNode = graph.byId(currentNodeId).orElseThrow();

        final MinimapView minimap = mapViewFactory.createMinimap(graph, currentNodeId);
        final List<MinimapCell> cells = minimap.cells();

        final Set<String> cellNodeIds = cells.stream()
                .map(MinimapCell::nodeId)
                .collect(Collectors.toSet());

        for (final MinimapCell cell : cells) {
            final MapNode node = graph.byId(cell.nodeId()).orElseThrow();

            // linkRight 쌍조건 검증
            final boolean expectedLinkRight = isMinimapEdgeExpected(
                    graph, currentNode, node, 1, 0, cellNodeIds);
            assertThat(cell.linkRight())
                    .as("linkRight for node %s at (%d,%d)", node.id(), node.x(), node.y())
                    .isEqualTo(expectedLinkRight);

            // linkDown 쌍조건 검증
            final boolean expectedLinkDown = isMinimapEdgeExpected(
                    graph, currentNode, node, 0, 1, cellNodeIds);
            assertThat(cell.linkDown())
                    .as("linkDown for node %s at (%d,%d)", node.id(), node.x(), node.y())
                    .isEqualTo(expectedLinkDown);
        }
    }

    /**
     * 전체지도에서 각 셀의 linkRight/linkDown 플래그가 쌍조건을 만족하는지 검증한다.
     *
     * <p>전체지도는 모든 노드가 항상 표시 범위 내이므로, 조건은 다음과 같다:
     * <p>linkRight=true ⟺ (좌표상 오른쪽 이웃이 존재 ∧ links에 실제 연결)
     * <p>linkDown=true  ⟺ (좌표상 아래쪽 이웃이 존재 ∧ links에 실제 연결)
     *
     * @param graphAndCurrent 임의 생성된 맵 그래프(부분 연결 포함)와 현재 노드 ID 튜플
     */
    @Property(tries = 100)
    void should_satisfyEdgeBiconditional_when_fullMapCreated(
            @ForAll("graphWithPartialConnectivity") final Tuple.Tuple2<MapGraph, String> graphAndCurrent) {

        final MapGraph graph = graphAndCurrent.get1();
        final String currentNodeId = graphAndCurrent.get2();

        final FullMapView fullMap = mapViewFactory.createFullMap(graph, currentNodeId);
        final List<FullMapCell> cells = fullMap.cells();

        for (final FullMapCell cell : cells) {
            final MapNode node = graph.byId(cell.nodeId()).orElseThrow();

            // linkRight 쌍조건 검증
            final boolean expectedLinkRight = isFullMapEdgeExpected(graph, node, 1, 0);
            assertThat(cell.linkRight())
                    .as("linkRight for node %s at (%d,%d)", node.id(), node.x(), node.y())
                    .isEqualTo(expectedLinkRight);

            // linkDown 쌍조건 검증
            final boolean expectedLinkDown = isFullMapEdgeExpected(graph, node, 0, 1);
            assertThat(cell.linkDown())
                    .as("linkDown for node %s at (%d,%d)", node.id(), node.x(), node.y())
                    .isEqualTo(expectedLinkDown);
        }
    }

    /**
     * 부분 연결 격자 그래프와 무작위 현재 노드 ID 튜플을 생성하는 Arbitrary 제공자.
     *
     * <p>완전 연결 격자를 생성한 뒤, 일부 간선을 무작위로 제거하여
     * 좌표상 인접하지만 links로 연결되지 않은 경우를 포함시킨다.
     *
     * @return 맵 그래프와 현재 노드 ID 튜플 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<MapGraph, String>> graphWithPartialConnectivity() {
        return Arbitraries.integers().between(GRID_SIZE_MIN, GRID_SIZE_MAX)
                .flatMap(gridSize -> {
                    final int nodeCount = gridSize * gridSize;
                    final Arbitrary<String> types = Arbitraries.of(
                            "town", "field", "dungeon", "shrine", "lake");
                    final Arbitrary<List<Boolean>> removals = Arbitraries.of(true, false)
                            .list().ofSize(computeMaxEdgeCount(gridSize));

                    return types.list().ofSize(nodeCount)
                            .flatMap(typeList -> removals.flatMap(removalFlags -> {
                                final List<MapNode> nodes = createPartialGridNodes(
                                        gridSize, typeList, removalFlags);
                                final MapGraph graph = new MapGraph(
                                        nodes, List.of(), nodes.getFirst().id());
                                final List<String> nodeIds = nodes.stream()
                                        .map(MapNode::id)
                                        .toList();
                                return Arbitraries.of(nodeIds)
                                        .map(currentId -> Tuple.of(graph, currentId));
                            }));
                });
    }

    private int computeMaxEdgeCount(final int gridSize) {
        // 격자에서 가능한 양방향 간선 수: 수평 (gridSize-1)*gridSize + 수직 gridSize*(gridSize-1)
        return 2 * gridSize * (gridSize - 1);
    }

    private List<MapNode> createPartialGridNodes(final int gridSize,
                                                  final List<String> typeList,
                                                  final List<Boolean> removalFlags) {
        final String[][] idGrid = new String[gridSize][gridSize];
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                idGrid[row][col] = "node-" + row + "-" + col;
            }
        }

        // 간선 제거 여부 결정: 각 간선(양방향 쌍)에 대해 removal flag가 true이면 제거
        final Set<String> removedEdges = computeRemovedEdges(gridSize, idGrid, removalFlags);

        final List<MapNode> nodes = new ArrayList<>();
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                final int index = row * gridSize + col;
                final String id = idGrid[row][col];
                final String type = typeList.get(index);
                final String name = "Node " + row + "," + col;
                final NodeType nodeType = NodeType.fromType(type).orElse(null);
                final String dungeonId = "dungeon".equals(type) ? "dungeon-" + id : null;
                final List<String> links = buildPartialLinks(
                        row, col, gridSize, idGrid, removedEdges);

                nodes.add(new MapNode(id, name, type, nodeType, col, row, dungeonId, null, links));
            }
        }
        return List.copyOf(nodes);
    }

    private Set<String> computeRemovedEdges(final int gridSize,
                                             final String[][] idGrid,
                                             final List<Boolean> removalFlags) {
        final List<String> allEdgeKeys = new ArrayList<>();

        // 수평 간선 (row, col) → (row, col+1)
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize - 1; col++) {
                allEdgeKeys.add(edgeKey(idGrid[row][col], idGrid[row][col + 1]));
            }
        }
        // 수직 간선 (row, col) → (row+1, col)
        for (int row = 0; row < gridSize - 1; row++) {
            for (int col = 0; col < gridSize; col++) {
                allEdgeKeys.add(edgeKey(idGrid[row][col], idGrid[row + 1][col]));
            }
        }

        final Set<String> removed = new java.util.HashSet<>();
        for (int i = 0; i < allEdgeKeys.size() && i < removalFlags.size(); i++) {
            if (removalFlags.get(i)) {
                removed.add(allEdgeKeys.get(i));
            }
        }
        return removed;
    }

    private String edgeKey(final String nodeA, final String nodeB) {
        // 정렬된 키를 사용하여 양방향 간선을 단일 키로 표현
        if (nodeA.compareTo(nodeB) < 0) {
            return nodeA + "<->" + nodeB;
        }
        return nodeB + "<->" + nodeA;
    }

    private List<String> buildPartialLinks(final int row, final int col,
                                            final int gridSize,
                                            final String[][] idGrid,
                                            final Set<String> removedEdges) {
        final List<String> links = new ArrayList<>();
        final String currentId = idGrid[row][col];

        if (row > 0) {
            final String neighborId = idGrid[row - 1][col];
            if (!removedEdges.contains(edgeKey(currentId, neighborId))) {
                links.add(neighborId);
            }
        }
        if (row < gridSize - 1) {
            final String neighborId = idGrid[row + 1][col];
            if (!removedEdges.contains(edgeKey(currentId, neighborId))) {
                links.add(neighborId);
            }
        }
        if (col > 0) {
            final String neighborId = idGrid[row][col - 1];
            if (!removedEdges.contains(edgeKey(currentId, neighborId))) {
                links.add(neighborId);
            }
        }
        if (col < gridSize - 1) {
            final String neighborId = idGrid[row][col + 1];
            if (!removedEdges.contains(edgeKey(currentId, neighborId))) {
                links.add(neighborId);
            }
        }
        return List.copyOf(links);
    }

    private boolean isMinimapEdgeExpected(final MapGraph graph,
                                          final MapNode centerNode,
                                          final MapNode node,
                                          final int edgeDx,
                                          final int edgeDy,
                                          final Set<String> cellNodeIds) {
        final int neighborX = node.x() + edgeDx;
        final int neighborY = node.y() + edgeDy;

        // 조건 1: 이웃이 미니맵 표시 범위 내인지 확인
        final int offsetX = neighborX - centerNode.x();
        final int offsetY = neighborY - centerNode.y();
        if (offsetX < MINIMAP_DX_MIN || offsetX > MINIMAP_DX_MAX
                || offsetY < MINIMAP_DY_MIN || offsetY > MINIMAP_DY_MAX) {
            return false;
        }

        // 조건 2: 해당 좌표에 노드가 존재하는지 확인
        final String coordKey = MapGraph.coordKey(neighborX, neighborY);
        final MapNode neighbor = graph.byCoord(coordKey).orElse(null);
        if (neighbor == null) {
            return false;
        }

        // 조건 3: 이웃이 실제로 뷰 셀에 포함되어 있는지 확인
        if (!cellNodeIds.contains(neighbor.id())) {
            return false;
        }

        // 조건 4: links에 실제 연결이 있는지 확인
        return node.links().contains(neighbor.id());
    }

    private boolean isFullMapEdgeExpected(final MapGraph graph,
                                          final MapNode node,
                                          final int edgeDx,
                                          final int edgeDy) {
        final int neighborX = node.x() + edgeDx;
        final int neighborY = node.y() + edgeDy;

        // 조건 1: 해당 좌표에 노드가 존재하는지 확인
        final String coordKey = MapGraph.coordKey(neighborX, neighborY);
        final MapNode neighbor = graph.byCoord(coordKey).orElse(null);
        if (neighbor == null) {
            return false;
        }

        // 조건 2: links에 실제 연결이 있는지 확인 (전체지도는 모든 노드가 범위 내)
        return node.links().contains(neighbor.id());
    }
}
