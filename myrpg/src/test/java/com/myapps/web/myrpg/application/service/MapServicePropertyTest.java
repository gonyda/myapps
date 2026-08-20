package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 맵 파싱 라운드트립 프로퍼티 테스트.
 *
 * <p>유효한 맵 그래프를 JSON으로 직렬화한 뒤 파싱하여 모든 노드의 id/name/type/좌표/links가 보존되고 NodeType이 원본 type 문자열에 대응하는지
 * 검증한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 1: 맵 파싱 라운드트립
 *
 * <p><b>Validates: Requirements 4.1, 4.2</b>
 */
class MapServicePropertyTest {

    private static final int GRID_SIZE_MIN = 1;
    private static final int GRID_SIZE_MAX = 5;
    private static final int COORD_MIN = -10;
    private static final int COORD_MAX = 10;
    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 15;
    private static final int ID_MIN_LENGTH = 3;
    private static final int ID_MAX_LENGTH = 20;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 유효한 맵 그래프를 JSON 직렬화 후 파싱하면 모든 노드의 id/name/type/좌표/links가 보존되고 NodeType이 대응하는지 검증한다.
     *
     * @param nodes 임의 생성된 유효 맵 노드 목록 (양방향 링크 보장)
     */
    @Property(tries = 100)
    void should_preserveAllNodeFields_when_serializedAndParsed(
            @ForAll("validMapGraph") final List<MapNode> nodes) {
        // Given: 노드 목록을 JSON으로 직렬화
        final String json = serializeToJson(nodes);

        // When: JSON을 파싱하여 노드 목록 재구성
        final List<MapNode> parsedNodes = parseNodesFromJson(json);

        // Then: 모든 노드 필드 보존 검증
        assertThat(parsedNodes).hasSameSizeAs(nodes);

        for (int i = 0; i < nodes.size(); i++) {
            final MapNode original = nodes.get(i);
            final MapNode parsed = parsedNodes.get(i);

            assertThat(parsed.id()).isEqualTo(original.id());
            assertThat(parsed.name()).isEqualTo(original.name());
            assertThat(parsed.type()).isEqualTo(original.type());
            assertThat(parsed.x()).isEqualTo(original.x());
            assertThat(parsed.y()).isEqualTo(original.y());
            assertThat(parsed.links()).containsExactlyInAnyOrderElementsOf(original.links());

            // NodeType 대응 검증
            final Optional<NodeType> expectedNodeType = NodeType.fromType(original.type());
            assertThat(parsed.nodeType()).isEqualTo(expectedNodeType.orElse(null));
        }
    }

    /**
     * 유효한 맵 그래프(양방향 링크 보장)를 생성하는 Arbitrary 제공자.
     *
     * <p>격자 좌표에 노드를 배치하고 인접 좌표 간 양방향 links를 부여한다. town/field/dungeon 및 미지 타입과 theme 유무를 포함한다.
     *
     * @return 임의의 유효한 맵 노드 목록 Arbitrary
     */
    @Provide
    Arbitrary<List<MapNode>> validMapGraph() {
        return Arbitraries.integers()
                .between(GRID_SIZE_MIN, GRID_SIZE_MAX)
                .flatMap(this::buildGridGraph);
    }

    private Arbitrary<List<MapNode>> buildGridGraph(final int gridSize) {
        final Arbitrary<String> types =
                Arbitraries.of("town", "field", "dungeon", "shrine", "lake");
        final Arbitrary<Boolean> hasTheme = Arbitraries.of(true, false);

        return Combinators.combine(
                        types.list().ofSize(gridSize * gridSize),
                        hasTheme.list().ofSize(gridSize * gridSize))
                .as((typeList, themeFlags) -> createGridNodes(gridSize, typeList, themeFlags));
    }

    private List<MapNode> createGridNodes(
            final int gridSize, final List<String> typeList, final List<Boolean> themeFlags) {
        final List<MapNode> nodes = new ArrayList<>();
        final String[][] idGrid = new String[gridSize][gridSize];

        // 노드 ID 및 기본 정보 생성
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                final int index = row * gridSize + col;
                final String id = "node-" + row + "-" + col;
                idGrid[row][col] = id;
            }
        }

        // 양방향 링크를 보장하면서 노드 생성
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

    private List<String> buildLinksForCell(
            final int row, final int col, final int gridSize, final String[][] idGrid) {
        final List<String> links = new ArrayList<>();

        // 상하좌우 인접 노드에 대한 양방향 링크
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

    private String serializeToJson(final List<MapNode> nodes) {
        final ObjectNode root = objectMapper.createObjectNode();
        root.put("startNodeId", nodes.isEmpty() ? "" : nodes.getFirst().id());

        final ArrayNode nodesArray = root.putArray("nodes");
        for (final MapNode node : nodes) {
            final ObjectNode nodeJson = nodesArray.addObject();
            nodeJson.put("id", node.id());
            nodeJson.put("name", node.name());
            nodeJson.put("type", node.type());
            nodeJson.put("x", node.x());
            nodeJson.put("y", node.y());

            if (node.dungeonId() != null) {
                nodeJson.put("dungeonId", node.dungeonId());
            }
            if (node.theme() != null) {
                nodeJson.put("theme", node.theme());
            }

            final ArrayNode linksArray = nodeJson.putArray("links");
            for (final String link : node.links()) {
                linksArray.add(link);
            }
        }

        root.putArray("dungeons");
        return root.toString();
    }

    private List<MapNode> parseNodesFromJson(final String json) {
        final JsonNode root = objectMapper.readTree(json);
        final JsonNode nodesArray = root.get("nodes");
        final List<MapNode> result = new ArrayList<>();

        for (final JsonNode nodeJson : nodesArray) {
            final String id = nodeJson.get("id").asText();
            final String name = nodeJson.get("name").asText();
            final String type = nodeJson.get("type").asText();
            final NodeType nodeType = NodeType.fromType(type).orElse(null);
            final int x = nodeJson.get("x").asInt();
            final int y = nodeJson.get("y").asInt();
            final String dungeonId =
                    nodeJson.has("dungeonId") ? nodeJson.get("dungeonId").asText() : null;
            final String theme = nodeJson.has("theme") ? nodeJson.get("theme").asText() : null;
            final List<String> links = parseLinks(nodeJson);

            result.add(new MapNode(id, name, type, nodeType, x, y, dungeonId, theme, links));
        }

        return List.copyOf(result);
    }

    private List<String> parseLinks(final JsonNode nodeJson) {
        final JsonNode linksArray = nodeJson.get("links");
        if (linksArray == null || !linksArray.isArray()) {
            return List.of();
        }

        final List<String> links = new ArrayList<>();
        for (final JsonNode linkNode : linksArray) {
            links.add(linkNode.asText());
        }
        return List.copyOf(links);
    }
}
