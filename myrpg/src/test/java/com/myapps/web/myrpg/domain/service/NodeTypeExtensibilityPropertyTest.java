package com.myapps.web.myrpg.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.myapps.web.myrpg.application.dto.FullMapCell;
import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.MinimapCell;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

/**
 * NodeType 확장성 프로퍼티 테스트.
 *
 * <p>미지 type 문자열을 포함하는 그래프에서 노드 조회, 미니맵 생성, 전체지도 생성이 예외 없이 동작하고 원본 {@code type} 문자열이 보존되는지 검증한다. 알
 * 수 없는 타입의 노드는 {@code nodeType == null}로 일반 통행 노드로 취급된다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 22: NodeType 확장성
 *
 * <p><b>Validates: Requirements 10.4</b>
 */
class NodeTypeExtensibilityPropertyTest {

    private static final int GRID_SIZE_MIN = 2;
    private static final int GRID_SIZE_MAX = 5;

    private static final List<String> UNKNOWN_TYPES =
            List.of("shrine", "lake", "cave", "portal", "ruins", "oasis", "bridge");

    private static final List<String> ALL_TYPES =
            List.of("town", "field", "dungeon", "shrine", "lake", "cave", "portal");

    private final MapViewFactory mapViewFactory = new MapViewFactory();

    /**
     * 미지 type 노드를 포함한 그래프에서 {@link MapGraph#byId}로 조회 시 원본 type 문자열이 보존되고, 미지 타입은 {@code nodeType
     * == null}임을 검증한다.
     *
     * @param graphAndUnknown 그래프와 미지 타입 노드 ID 튜플
     */
    @Property(tries = 100)
    void should_preserveUnknownType_when_lookupById(
            @ForAll("graphWithUnknownTypeNode")
                    final Tuple.Tuple3<MapGraph, String, String> graphAndUnknown) {

        final MapGraph graph = graphAndUnknown.get1();
        final String unknownNodeId = graphAndUnknown.get2();
        final String expectedType = graphAndUnknown.get3();

        // When: ID로 노드 조회
        final Optional<MapNode> nodeOpt = graph.byId(unknownNodeId);

        // Then: 노드가 존재하고, 원본 type 문자열 보존, nodeType은 null
        assertThat(nodeOpt).isPresent();
        final MapNode node = nodeOpt.get();
        assertThat(node.type()).isEqualTo(expectedType);
        assertThat(node.nodeType()).isNull();
        assertThat(NodeType.fromType(expectedType)).isEmpty();
    }

    /**
     * 미지 type 노드를 포함한 그래프에서 미니맵 생성이 예외 없이 동작하고, 미지 타입 노드의 원본 type 문자열이 셀에 보존되는지 검증한다.
     *
     * @param graphAndCurrent 그래프와 현재 노드 ID 튜플 (미지 타입 노드가 현재 노드)
     */
    @Property(tries = 100)
    void should_createMinimapWithoutException_when_unknownTypeNodesExist(
            @ForAll("graphWithUnknownTypeAsCurrent")
                    final Tuple.Tuple2<MapGraph, String> graphAndCurrent) {

        final MapGraph graph = graphAndCurrent.get1();
        final String currentNodeId = graphAndCurrent.get2();

        // When & Then: 미니맵 생성이 예외 없이 완료
        assertThatCode(() -> mapViewFactory.createMinimap(graph, currentNodeId))
                .doesNotThrowAnyException();

        // Then: 미니맵 셀에서 미지 타입 노드의 type 문자열 보존 확인
        final MinimapView minimap = mapViewFactory.createMinimap(graph, currentNodeId);
        for (final MinimapCell cell : minimap.cells()) {
            final MapNode node = graph.byId(cell.nodeId()).orElseThrow();
            assertThat(cell.type())
                    .as("minimap cell type for node %s", cell.nodeId())
                    .isEqualTo(node.type());
        }
    }

    /**
     * 미지 type 노드를 포함한 그래프에서 전체지도 생성이 예외 없이 동작하고, 미지 타입 노드의 원본 type 문자열이 셀에 보존되는지 검증한다.
     *
     * @param graphAndCurrent 그래프와 현재 노드 ID 튜플 (미지 타입 노드가 현재 노드)
     */
    @Property(tries = 100)
    void should_createFullMapWithoutException_when_unknownTypeNodesExist(
            @ForAll("graphWithUnknownTypeAsCurrent")
                    final Tuple.Tuple2<MapGraph, String> graphAndCurrent) {

        final MapGraph graph = graphAndCurrent.get1();
        final String currentNodeId = graphAndCurrent.get2();

        // When & Then: 전체지도 생성이 예외 없이 완료
        assertThatCode(() -> mapViewFactory.createFullMap(graph, currentNodeId))
                .doesNotThrowAnyException();

        // Then: 전체지도 셀에서 미지 타입 노드의 type 문자열 보존 확인
        final FullMapView fullMap = mapViewFactory.createFullMap(graph, currentNodeId);
        for (final FullMapCell cell : fullMap.cells()) {
            final MapNode node = graph.byId(cell.nodeId()).orElseThrow();
            assertThat(cell.type())
                    .as("fullmap cell type for node %s", cell.nodeId())
                    .isEqualTo(node.type());
        }
    }

    /**
     * 미지 type 노드를 포함한 그래프에서 모든 미지 타입 노드는 {@code nodeType == null}이며 일반 통행 노드로 취급됨을 검증한다.
     *
     * @param graphAndCurrent 그래프와 현재 노드 ID 튜플
     */
    @Property(tries = 100)
    void should_treatUnknownTypeAsTraversable_when_nodeTypeIsNull(
            @ForAll("graphWithUnknownTypeAsCurrent")
                    final Tuple.Tuple2<MapGraph, String> graphAndCurrent) {

        final MapGraph graph = graphAndCurrent.get1();

        // Then: 모든 노드에서 미지 타입은 nodeType == null
        for (final MapNode node : graph.nodes()) {
            final boolean isKnownType = NodeType.fromType(node.type()).isPresent();
            if (!isKnownType) {
                assertThat(node.nodeType())
                        .as("nodeType for unknown type '%s' on node %s", node.type(), node.id())
                        .isNull();
            }
        }
    }

    /**
     * 미지 타입 노드를 하나 이상 포함하는 그래프와 해당 미지 타입 노드 ID, 그리고 그 노드의 원본 type 문자열을 반환하는 Arbitrary.
     *
     * @return 그래프, 미지 타입 노드 ID, 원본 type 문자열 튜플 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple3<MapGraph, String, String>> graphWithUnknownTypeNode() {
        return Arbitraries.integers()
                .between(GRID_SIZE_MIN, GRID_SIZE_MAX)
                .flatMap(
                        gridSize -> {
                            final int nodeCount = gridSize * gridSize;
                            return Arbitraries.of(ALL_TYPES)
                                    .list()
                                    .ofSize(nodeCount)
                                    .filter(
                                            types ->
                                                    types.stream()
                                                            .anyMatch(UNKNOWN_TYPES::contains))
                                    .flatMap(
                                            typeList -> {
                                                final List<MapNode> nodes =
                                                        createGridNodes(gridSize, typeList);
                                                final MapGraph graph =
                                                        new MapGraph(
                                                                nodes,
                                                                List.of(),
                                                                nodes.getFirst().id());
                                                final List<String> unknownNodeIds =
                                                        nodes.stream()
                                                                .filter(
                                                                        n ->
                                                                                UNKNOWN_TYPES
                                                                                        .contains(
                                                                                                n
                                                                                                        .type()))
                                                                .map(MapNode::id)
                                                                .toList();
                                                return Arbitraries.of(unknownNodeIds)
                                                        .map(
                                                                id -> {
                                                                    final MapNode node =
                                                                            graph.byId(id)
                                                                                    .orElseThrow();
                                                                    return Tuple.of(
                                                                            graph, id, node.type());
                                                                });
                                            });
                        });
    }

    /**
     * 미지 타입 노드를 하나 이상 포함하는 그래프와 미지 타입 노드를 현재 노드로 사용하는 Arbitrary. 미니맵/전체지도 생성 시 현재 노드가 미지 타입인 경우를
     * 테스트한다.
     *
     * @return 그래프와 현재 노드 ID(미지 타입) 튜플 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<MapGraph, String>> graphWithUnknownTypeAsCurrent() {
        return Arbitraries.integers()
                .between(GRID_SIZE_MIN, GRID_SIZE_MAX)
                .flatMap(
                        gridSize -> {
                            final int nodeCount = gridSize * gridSize;
                            return Arbitraries.of(ALL_TYPES)
                                    .list()
                                    .ofSize(nodeCount)
                                    .filter(
                                            types ->
                                                    types.stream()
                                                            .anyMatch(UNKNOWN_TYPES::contains))
                                    .flatMap(
                                            typeList -> {
                                                final List<MapNode> nodes =
                                                        createGridNodes(gridSize, typeList);
                                                final MapGraph graph =
                                                        new MapGraph(
                                                                nodes,
                                                                List.of(),
                                                                nodes.getFirst().id());
                                                final List<String> unknownNodeIds =
                                                        nodes.stream()
                                                                .filter(
                                                                        n ->
                                                                                UNKNOWN_TYPES
                                                                                        .contains(
                                                                                                n
                                                                                                        .type()))
                                                                .map(MapNode::id)
                                                                .toList();
                                                return Arbitraries.of(unknownNodeIds)
                                                        .map(
                                                                currentId ->
                                                                        Tuple.of(graph, currentId));
                                            });
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
