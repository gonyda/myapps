package com.myapps.web.myrpg.application.service;

import java.util.ArrayList;
import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.Dungeon;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 링크 양방향 불변식 프로퍼티 테스트.
 *
 * <p>로드 성공한 맵 그래프에서 임의의 두 노드 A, B에 대해
 * A의 {@code links}가 B를 포함하면 B의 {@code links}도 A를 포함하는지 검증한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 3: 링크 양방향 불변식
 *
 * <p><b>Validates: Requirements 4.5</b>
 */
class LinkBidirectionalPropertyTest {

    private static final int GRID_SIZE_MIN = 2;
    private static final int GRID_SIZE_MAX = 5;

    /**
     * 로드 성공한 맵 그래프에서 A.links⊇{B} ⇒ B.links⊇{A} 양방향 불변식을 검증한다.
     *
     * @param mapGraph 임의 생성된 유효 맵 그래프
     */
    @Property(tries = 100)
    void should_haveBidirectionalLinks_forAllNodesInLoadedGraph(
            @ForAll("validMapGraph") final MapGraph mapGraph) {
        for (final MapNode nodeA : mapGraph.nodes()) {
            for (final String linkedId : nodeA.links()) {
                final MapNode nodeB = mapGraph.byId(linkedId).orElse(null);

                assertThat(nodeB)
                        .as("노드 '%s'의 링크 대상 '%s'가 그래프에 존재해야 한다",
                                nodeA.id(), linkedId)
                        .isNotNull();

                assertThat(nodeB.links())
                        .as("노드 '%s' → '%s' 링크 존재 시 역방향 '%s' → '%s'도 존재해야 한다",
                                nodeA.id(), nodeB.id(), nodeB.id(), nodeA.id())
                        .contains(nodeA.id());
            }
        }
    }

    /**
     * 유효한 맵 그래프(양방향 링크 보장)를 {@link MapGraph}로 생성하는 Arbitrary 제공자.
     *
     * <p>격자 좌표에 노드를 배치하고 인접 좌표 간 양방향 links를 부여한 뒤
     * {@link MapGraph} 인스턴스로 래핑한다.
     *
     * @return 임의의 유효한 {@link MapGraph} Arbitrary
     */
    @Provide
    Arbitrary<MapGraph> validMapGraph() {
        return Arbitraries.integers().between(GRID_SIZE_MIN, GRID_SIZE_MAX)
                .flatMap(this::buildGridMapGraph);
    }

    private Arbitrary<MapGraph> buildGridMapGraph(final int gridSize) {
        final Arbitrary<String> types = Arbitraries.of("town", "field", "dungeon", "shrine", "lake");
        final Arbitrary<Boolean> hasTheme = Arbitraries.of(true, false);

        return Combinators.combine(
                types.list().ofSize(gridSize * gridSize),
                hasTheme.list().ofSize(gridSize * gridSize)
        ).as((typeList, themeFlags) -> createMapGraph(gridSize, typeList, themeFlags));
    }

    private MapGraph createMapGraph(final int gridSize,
                                    final List<String> typeList,
                                    final List<Boolean> themeFlags) {
        final List<MapNode> nodes = createGridNodes(gridSize, typeList, themeFlags);
        final String startNodeId = nodes.getFirst().id();
        return new MapGraph(nodes, List.of(), startNodeId);
    }

    private List<MapNode> createGridNodes(final int gridSize,
                                          final List<String> typeList,
                                          final List<Boolean> themeFlags) {
        final List<MapNode> nodes = new ArrayList<>();
        final String[][] idGrid = new String[gridSize][gridSize];

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                idGrid[row][col] = "node-" + row + "-" + col;
            }
        }

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                final int index = row * gridSize + col;
                final String id = idGrid[row][col];
                final String type = typeList.get(index);
                final String name = "Node " + row + "," + col;
                final NodeType nodeType = NodeType.fromType(type).orElse(null);
                final String dungeonId = "dungeon".equals(type) ? "dungeon-" + id : null;
                final String theme = themeFlags.get(index) ? "theme-" + type : null;
                final List<String> links = buildLinksForCell(row, col, gridSize, idGrid);

                nodes.add(new MapNode(id, name, type, nodeType, col, row, dungeonId, theme, links));
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
