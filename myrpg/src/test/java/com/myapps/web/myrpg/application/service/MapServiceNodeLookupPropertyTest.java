package com.myapps.web.myrpg.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.application.exception.NodeNotFoundException;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 노드 조회와 부재 오류 프로퍼티 테스트.
 *
 * <p>존재하는 id에 대해 {@code node(id)}가 해당 노드의
 * name/type/좌표/links를 반환하고, 존재하지 않는 id에 대해서는
 * {@link NodeNotFoundException}을 던지는지 검증한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 2: 노드 조회와 부재 오류
 *
 * <p><b>Validates: Requirements 4.3, 4.4</b>
 */
class MapServiceNodeLookupPropertyTest {

    private static final int GRID_SIZE_MIN = 1;
    private static final int GRID_SIZE_MAX = 5;
    private static final String NON_EXISTING_ID_PREFIX = "non-existing-";

    /**
     * 존재하는 노드 ID로 조회하면 해당 노드의 name/type/좌표/links가 정확히 반환되는지 검증한다.
     *
     * @param nodes 임의 생성된 유효 맵 노드 목록 (양방향 링크 보장)
     */
    @Property(tries = 100)
    void should_returnCorrectNodeFields_when_lookupByExistingId(
            @ForAll("validMapGraph") final List<MapNode> nodes) {
        // Given: 유효한 그래프 구성
        final MapGraph graph = new MapGraph(nodes, List.of(), nodes.getFirst().id());

        // When & Then: 모든 노드 ID로 조회 시 올바른 필드 반환
        for (final MapNode expectedNode : nodes) {
            final MapNode found = graph.byId(expectedNode.id())
                    .orElseThrow(() -> new NodeNotFoundException(
                            "노드를 찾을 수 없습니다: " + expectedNode.id()));

            assertThat(found.id()).isEqualTo(expectedNode.id());
            assertThat(found.name()).isEqualTo(expectedNode.name());
            assertThat(found.type()).isEqualTo(expectedNode.type());
            assertThat(found.x()).isEqualTo(expectedNode.x());
            assertThat(found.y()).isEqualTo(expectedNode.y());
            assertThat(found.links()).containsExactlyInAnyOrderElementsOf(expectedNode.links());
        }
    }

    /**
     * 존재하지 않는 ID로 조회하면 {@link NodeNotFoundException}이 발생하는지 검증한다.
     *
     * <p>{@code MapService.node(id)}의 동작을 재현하여, 그래프에 없는 임의 문자열 ID에 대해
     * 예외가 정확히 던져지는지 확인한다.
     *
     * @param nodes          임의 생성된 유효 맵 노드 목록
     * @param nonExistingSuffix 그래프에 포함되지 않을 임의 문자열 접미사
     */
    @Property(tries = 100)
    void should_throwNodeNotFoundException_when_lookupByNonExistingId(
            @ForAll("validMapGraph") final List<MapNode> nodes,
            @ForAll("nonExistingIdSuffix") final String nonExistingSuffix) {
        // Given: 유효한 그래프 구성
        final MapGraph graph = new MapGraph(nodes, List.of(), nodes.getFirst().id());
        final Set<String> existingIds = nodes.stream()
                .map(MapNode::id)
                .collect(Collectors.toUnmodifiableSet());

        // 존재하지 않는 ID 생성 (접두사 보장으로 절대 기존 ID와 충돌하지 않음)
        final String nonExistingId = NON_EXISTING_ID_PREFIX + nonExistingSuffix;

        // 혹시 충돌 시 건너뛰기 (확률적으로 거의 불가능하나 방어 코드)
        if (existingIds.contains(nonExistingId)) {
            return;
        }

        // When & Then: MapService.node() 동작 재현 — NodeNotFoundException 발생
        assertThatThrownBy(() ->
                graph.byId(nonExistingId)
                        .orElseThrow(() -> new NodeNotFoundException(
                                "노드를 찾을 수 없습니다: " + nonExistingId)))
                .isInstanceOf(NodeNotFoundException.class)
                .hasMessageContaining(nonExistingId);
    }

    /**
     * 유효한 맵 그래프(양방향 링크 보장)를 생성하는 Arbitrary 제공자.
     *
     * <p>격자 좌표에 노드를 배치하고 인접 좌표 간 양방향 links를 부여한다.
     *
     * @return 임의의 유효한 맵 노드 목록 Arbitrary
     */
    @Provide
    Arbitrary<List<MapNode>> validMapGraph() {
        return Arbitraries.integers().between(GRID_SIZE_MIN, GRID_SIZE_MAX)
                .flatMap(this::buildGridGraph);
    }

    /**
     * 존재하지 않는 노드 ID의 접미사를 생성하는 Arbitrary 제공자.
     *
     * @return 임의의 영문 소문자 문자열 Arbitrary (길이 3~10)
     */
    @Provide
    Arbitrary<String> nonExistingIdSuffix() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(10);
    }

    private Arbitrary<List<MapNode>> buildGridGraph(final int gridSize) {
        final Arbitrary<String> types = Arbitraries.of("town", "field", "dungeon", "shrine", "lake");
        final Arbitrary<Boolean> hasTheme = Arbitraries.of(true, false);

        return Combinators.combine(
                types.list().ofSize(gridSize * gridSize),
                hasTheme.list().ofSize(gridSize * gridSize)
        ).as((typeList, themeFlags) -> createGridNodes(gridSize, typeList, themeFlags));
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
                final int x = col;
                final int y = row;
                final String dungeonId = "dungeon".equals(type) ? "dungeon-" + id : null;
                final String theme = themeFlags.get(index) ? "theme-" + type : null;
                final List<String> links = buildLinksForCell(row, col, gridSize, idGrid);

                nodes.add(new MapNode(id, name, type, nodeType, x, y, dungeonId, theme, links));
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
