package com.myapps.web.myrpg.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.dto.FullMapCell;
import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

/**
 * 전체지도 완전성 프로퍼티 테스트.
 *
 * <p>임의의 유효 맵 그래프와 현재 노드에 대해 {@link MapViewFactory#createFullMap}이 생성하는 전체지도 셀이 모든 노드를 빠짐없이 포함하고, 각
 * 셀의 이름/타입/links가 원본과 동일하며, 격자 좌표가 바운딩박스 기준으로 정확히 계산되는지 검증한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 6: 전체지도 완전성
 *
 * <p><b>Validates: Requirements 8.5</b>
 */
class MapViewFactoryFullMapPropertyTest {

    private static final int GRID_SIZE_MIN = 1;
    private static final int GRID_SIZE_MAX = 5;

    private final MapViewFactory mapViewFactory = new MapViewFactory();

    /**
     * 전체지도 셀의 nodeId 집합이 그래프의 전체 노드 집합과 정확히 일치하고, 각 셀이 원본 노드의 name/type/links를 보존하며,
     * gridColumn=x-minX+1, gridRow=y-minY+1 공식이 올바른지 검증한다.
     *
     * @param graphAndCurrent 임의 생성된 맵 그래프와 현재 노드 ID 튜플
     */
    @Property(tries = 100)
    void should_containAllNodes_and_preserveAttributes_and_correctGridCoords(
            @ForAll("validGraphWithCurrentNode")
                    final Tuple.Tuple2<MapGraph, String> graphAndCurrent) {

        final MapGraph graph = graphAndCurrent.get1();
        final String currentNodeId = graphAndCurrent.get2();

        // When: 전체지도 생성
        final FullMapView fullMap = mapViewFactory.createFullMap(graph, currentNodeId);
        final List<FullMapCell> cells = fullMap.cells();

        // Then 1: 셀 nodeId 집합 = 전체 노드 집합 (완전성)
        final Set<String> expectedNodeIds =
                graph.nodes().stream().map(MapNode::id).collect(Collectors.toSet());
        final Set<String> actualNodeIds =
                cells.stream().map(FullMapCell::nodeId).collect(Collectors.toSet());
        assertThat(actualNodeIds).isEqualTo(expectedNodeIds);

        // 바운딩박스 minX, minY 계산
        final int minX = graph.nodes().stream().mapToInt(MapNode::x).min().orElseThrow();
        final int minY = graph.nodes().stream().mapToInt(MapNode::y).min().orElseThrow();

        // Then 2 & 3: 각 셀의 name/type/links 보존 및 gridColumn/gridRow 검증
        for (final FullMapCell cell : cells) {
            final MapNode node = graph.byId(cell.nodeId()).orElseThrow();

            assertThat(cell.name()).as("name for node %s", cell.nodeId()).isEqualTo(node.name());
            assertThat(cell.type()).as("type for node %s", cell.nodeId()).isEqualTo(node.type());
            assertThat(cell.links()).as("links for node %s", cell.nodeId()).isEqualTo(node.links());

            final int expectedGridColumn = node.x() - minX + 1;
            final int expectedGridRow = node.y() - minY + 1;

            assertThat(cell.gridColumn())
                    .as("gridColumn for node %s", cell.nodeId())
                    .isEqualTo(expectedGridColumn);
            assertThat(cell.gridRow())
                    .as("gridRow for node %s", cell.nodeId())
                    .isEqualTo(expectedGridRow);
        }
    }

    /**
     * 유효한 맵 그래프와 그 안에서 무작위로 선택된 현재 노드 ID의 튜플을 생성하는 Arbitrary 제공자.
     *
     * @return 맵 그래프와 현재 노드 ID 튜플 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<MapGraph, String>> validGraphWithCurrentNode() {
        return Arbitraries.integers()
                .between(GRID_SIZE_MIN, GRID_SIZE_MAX)
                .flatMap(this::buildGraphWithCurrentNode);
    }

    private Arbitrary<Tuple.Tuple2<MapGraph, String>> buildGraphWithCurrentNode(
            final int gridSize) {
        final Arbitrary<String> types =
                Arbitraries.of("town", "field", "dungeon", "shrine", "lake");
        final int nodeCount = gridSize * gridSize;

        return types.list()
                .ofSize(nodeCount)
                .flatMap(
                        typeList -> {
                            final List<MapNode> nodes = createGridNodes(gridSize, typeList);
                            final MapGraph graph =
                                    new MapGraph(nodes, List.of(), nodes.getFirst().id());
                            final List<String> nodeIds = nodes.stream().map(MapNode::id).toList();
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

    private List<String> buildLinksForCell(
            final int row, final int col, final int gridSize, final String[][] idGrid) {
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
}
