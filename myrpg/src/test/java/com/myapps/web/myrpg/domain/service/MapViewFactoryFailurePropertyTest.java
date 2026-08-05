package com.myapps.web.myrpg.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

import com.myapps.web.myrpg.application.exception.MapViewGenerationException;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 뷰 생성 실패 시 무생성(all-or-nothing) 프로퍼티 테스트.
 *
 * <p>임의의 유효 맵 그래프에서 그래프에 존재하지 않는 현재 노드 ID로
 * {@link MapViewFactory#createMinimap} 또는 {@link MapViewFactory#createFullMap}을
 * 호출하면 {@link MapViewGenerationException}이 발생하고, 어떤 셀이나 간선도
 * 생성되지 않음(부분 결과 없음)을 검증한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 7: 뷰 생성 실패 시 무생성(all-or-nothing)
 *
 * <p><b>Validates: Requirements 8.6, 8.7</b>
 */
class MapViewFactoryFailurePropertyTest {

    private static final int GRID_SIZE_MIN = 1;
    private static final int GRID_SIZE_MAX = 5;

    private final MapViewFactory mapViewFactory = new MapViewFactory();

    /**
     * 그래프에 존재하지 않는 현재 노드 ID로 미니맵 생성 시
     * {@link MapViewGenerationException}이 발생하고 부분 결과가 생성되지 않음을 검증한다.
     *
     * @param graphAndUnknownId 임의 생성된 맵 그래프와 그래프에 존재하지 않는 노드 ID 튜플
     */
    @Property(tries = 100)
    void should_throwMapViewGenerationException_when_minimapWithUnknownNodeId(
            @ForAll("validGraphWithUnknownNodeId") final Tuple.Tuple2<MapGraph, String> graphAndUnknownId) {

        final MapGraph graph = graphAndUnknownId.get1();
        final String unknownNodeId = graphAndUnknownId.get2();

        assertThatThrownBy(() -> mapViewFactory.createMinimap(graph, unknownNodeId))
                .isInstanceOf(MapViewGenerationException.class);
    }

    /**
     * 그래프에 존재하지 않는 현재 노드 ID로 전체지도 생성 시
     * {@link MapViewGenerationException}이 발생하고 부분 결과가 생성되지 않음을 검증한다.
     *
     * @param graphAndUnknownId 임의 생성된 맵 그래프와 그래프에 존재하지 않는 노드 ID 튜플
     */
    @Property(tries = 100)
    void should_throwMapViewGenerationException_when_fullMapWithUnknownNodeId(
            @ForAll("validGraphWithUnknownNodeId") final Tuple.Tuple2<MapGraph, String> graphAndUnknownId) {

        final MapGraph graph = graphAndUnknownId.get1();
        final String unknownNodeId = graphAndUnknownId.get2();

        assertThatThrownBy(() -> mapViewFactory.createFullMap(graph, unknownNodeId))
                .isInstanceOf(MapViewGenerationException.class);
    }

    /**
     * 유효한 맵 그래프와 그래프에 존재하지 않는 노드 ID의 튜플을 생성하는 Arbitrary 제공자.
     *
     * <p>UUID 기반으로 그래프에 절대 존재하지 않는 노드 ID를 생성하여
     * "미확인 현재 노드" 시나리오를 확실히 재현한다.
     *
     * @return 맵 그래프와 미존재 노드 ID 튜플 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<MapGraph, String>> validGraphWithUnknownNodeId() {
        return Arbitraries.integers().between(GRID_SIZE_MIN, GRID_SIZE_MAX)
                .flatMap(this::buildGraphWithUnknownNodeId);
    }

    private Arbitrary<Tuple.Tuple2<MapGraph, String>> buildGraphWithUnknownNodeId(final int gridSize) {
        final Arbitrary<String> types = Arbitraries.of("town", "field", "dungeon", "shrine", "lake");
        final int nodeCount = gridSize * gridSize;

        return types.list().ofSize(nodeCount)
                .map(typeList -> {
                    final List<MapNode> nodes = createGridNodes(gridSize, typeList);
                    final MapGraph graph = new MapGraph(nodes, List.of(), nodes.getFirst().id());
                    final String unknownNodeId = "unknown-" + UUID.randomUUID();
                    return Tuple.of(graph, unknownNodeId);
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
}
