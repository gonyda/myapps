package com.myapps.web.myrpg.domain.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

import com.myapps.web.myrpg.application.dto.MinimapCell;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.domain.model.Dungeon;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 미니맵 셀 구성 프로퍼티 테스트.
 *
 * <p>임의의 유효 맵 그래프와 현재 노드에 대해 {@link MapViewFactory#createMinimap}이
 * 생성하는 미니맵 셀이 윈도우 범위(dx∈[-4,4], dy∈[-2,2])와 정확히 일치하고,
 * 그리드 좌표·타입 문자열·현재 노드 플래그가 올바른지 검증한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 4: 미니맵 셀 구성
 *
 * <p><b>Validates: Requirements 8.1, 8.2, 8.4, 6.4, 1.7</b>
 */
class MapViewFactoryMinimapPropertyTest {

    private static final int GRID_SIZE_MIN = 1;
    private static final int GRID_SIZE_MAX = 5;
    private static final int MINIMAP_CENTER_COLUMN = 5;
    private static final int MINIMAP_CENTER_ROW = 3;
    private static final int MINIMAP_DX_MIN = -4;
    private static final int MINIMAP_DX_MAX = 4;
    private static final int MINIMAP_DY_MIN = -2;
    private static final int MINIMAP_DY_MAX = 2;
    private static final int MAX_MINIMAP_CELLS = 45;

    private final MapViewFactory mapViewFactory = new MapViewFactory();

    /**
     * 미니맵 셀이 창 범위 내 노드와 정확히 일치하고(최대 45셀),
     * 현재 노드가 항상 포함되며, 그리드 좌표·타입 문자열·current 플래그가 올바른지 검증한다.
     *
     * @param graphAndCurrent 임의 생성된 맵 그래프와 현재 노드 ID 튜플
     */
    @Property(tries = 100)
    void should_matchWindowRangeNodes_when_minimapCreated(
            @ForAll("validGraphWithCurrentNode") final Tuple.Tuple2<MapGraph, String> graphAndCurrent) {

        final MapGraph graph = graphAndCurrent.get1();
        final String currentNodeId = graphAndCurrent.get2();
        final MapNode currentNode = graph.byId(currentNodeId).orElseThrow();

        // When: 미니맵 생성
        final MinimapView minimap = mapViewFactory.createMinimap(graph, currentNodeId);
        final List<MinimapCell> cells = minimap.cells();

        // Then 1: 셀 집합이 창 범위 내 노드와 정확히 일치
        final Set<String> expectedNodeIds = computeExpectedNodeIds(graph, currentNode);
        final Set<String> actualNodeIds = cells.stream()
                .map(MinimapCell::nodeId)
                .collect(Collectors.toSet());
        assertThat(actualNodeIds).isEqualTo(expectedNodeIds);

        // Then 2: 최대 45셀
        assertThat(cells).hasSizeLessThanOrEqualTo(MAX_MINIMAP_CELLS);

        // Then 3: 현재 노드 항상 포함
        assertThat(actualNodeIds).contains(currentNodeId);

        // Then 4 & 5: 각 셀의 그리드 좌표 및 타입 문자열 검증
        for (final MinimapCell cell : cells) {
            final MapNode node = graph.byId(cell.nodeId()).orElseThrow();
            final int expectedGridColumn = MINIMAP_CENTER_COLUMN + (node.x() - currentNode.x());
            final int expectedGridRow = MINIMAP_CENTER_ROW + (node.y() - currentNode.y());

            assertThat(cell.gridColumn())
                    .as("gridColumn for node %s", cell.nodeId())
                    .isEqualTo(expectedGridColumn);
            assertThat(cell.gridRow())
                    .as("gridRow for node %s", cell.nodeId())
                    .isEqualTo(expectedGridRow);
            assertThat(cell.type())
                    .as("type for node %s", cell.nodeId())
                    .isEqualTo(node.type());
        }

        // Then 6: 정확히 하나의 셀만 current=true이며, 그것이 현재 노드
        final List<MinimapCell> currentCells = cells.stream()
                .filter(MinimapCell::current)
                .toList();
        assertThat(currentCells).hasSize(1);
        assertThat(currentCells.getFirst().nodeId()).isEqualTo(currentNodeId);
    }

    /**
     * 유효한 맵 그래프와 그 안에서 무작위로 선택된 현재 노드 ID의 튜플을 생성하는 Arbitrary 제공자.
     *
     * @return 맵 그래프와 현재 노드 ID 튜플 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<MapGraph, String>> validGraphWithCurrentNode() {
        return Arbitraries.integers().between(GRID_SIZE_MIN, GRID_SIZE_MAX)
                .flatMap(this::buildGraphWithCurrentNode);
    }

    private Arbitrary<Tuple.Tuple2<MapGraph, String>> buildGraphWithCurrentNode(final int gridSize) {
        final Arbitrary<String> types = Arbitraries.of("town", "field", "dungeon", "shrine", "lake");
        final int nodeCount = gridSize * gridSize;

        return types.list().ofSize(nodeCount)
                .flatMap(typeList -> {
                    final List<MapNode> nodes = createGridNodes(gridSize, typeList);
                    final MapGraph graph = new MapGraph(nodes, List.of(), nodes.getFirst().id());
                    final List<String> nodeIds = nodes.stream()
                            .map(MapNode::id)
                            .toList();
                    return Arbitraries.of(nodeIds)
                            .map(currentId -> Tuple.of(graph, currentId));
                });
    }

    private List<MapNode> createGridNodes(final int gridSize, final List<String> typeList) {
        final String[][] idGrid = new String[gridSize][gridSize];

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                idGrid[row][col] = "node-" + row + "-" + col;
            }
        }

        final List<MapNode> nodes = new ArrayList<>();
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                final int index = row * gridSize + col;
                final String id = idGrid[row][col];
                final String type = typeList.get(index);
                final String name = "Node " + row + "," + col;
                final NodeType nodeType = NodeType.fromType(type).orElse(null);
                final String dungeonId = "dungeon".equals(type) ? "dungeon-" + id : null;
                final List<String> links = buildLinksForCell(row, col, gridSize, idGrid);

                nodes.add(new MapNode(id, name, type, nodeType, col, row, dungeonId, null, links));
            }
        }
        return List.copyOf(nodes);
    }

    private List<String> buildLinksForCell(final int row, final int col,
                                           final int gridSize, final String[][] idGrid) {
        final List<String> links = new ArrayList<>();

        if (row > 0) {
            links.add(idGrid[row - 1][col]);
        }
        if (row < gridSize - 1) {
            links.add(idGrid[row + 1][col]);
        }
        if (col > 0) {
            links.add(idGrid[row][col - 1]);
        }
        if (col < gridSize - 1) {
            links.add(idGrid[row][col + 1]);
        }
        return List.copyOf(links);
    }

    private Set<String> computeExpectedNodeIds(final MapGraph graph, final MapNode currentNode) {
        final Set<String> expected = new HashSet<>();
        for (int dx = MINIMAP_DX_MIN; dx <= MINIMAP_DX_MAX; dx++) {
            for (int dy = MINIMAP_DY_MIN; dy <= MINIMAP_DY_MAX; dy++) {
                graph.neighborByOffset(currentNode, dx, dy)
                        .ifPresent(node -> expected.add(node.id()));
            }
        }
        return expected;
    }
}
