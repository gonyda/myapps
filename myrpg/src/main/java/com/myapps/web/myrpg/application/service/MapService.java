package com.myapps.web.myrpg.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.myapps.web.myrpg.application.dto.FullMapView;
import com.myapps.web.myrpg.application.dto.MinimapView;
import com.myapps.web.myrpg.application.exception.MapDataException;
import com.myapps.web.myrpg.application.exception.NodeNotFoundException;
import com.myapps.web.myrpg.domain.model.Dungeon;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;
import com.myapps.web.myrpg.domain.service.MapViewFactory;

import jakarta.annotation.PostConstruct;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 맵 데이터 로딩 및 조회 서비스.
 *
 * <p>애플리케이션 기동 시 {@code classpath:data/map.json}을 1회 파싱하여
 * 불변 {@link MapGraph}를 구성하고, 노드·던전 조회 기능을 제공합니다.
 * 로딩 시 양방향 링크 무결성을 검증하며, 위반 시 {@link MapDataException}을
 * 발생시켜 기동을 실패시킵니다.
 */
@Service
public class MapService {

    private static final String MAP_JSON_PATH = "data/map.json";

    private final ObjectMapper objectMapper;
    private final MapViewFactory mapViewFactory;
    private MapGraph mapGraph;

    /**
     * MapService를 생성합니다.
     *
     * @param objectMapper   Jackson 3 ObjectMapper
     * @param mapViewFactory 미니맵/전체지도 격자 생성 팩토리
     */
    public MapService(final ObjectMapper objectMapper, final MapViewFactory mapViewFactory) {
        this.objectMapper = objectMapper;
        this.mapViewFactory = mapViewFactory;
    }

    /**
     * 애플리케이션 기동 시 맵 JSON을 로드하고 검증합니다.
     *
     * @throws MapDataException JSON 파싱 실패 또는 양방향 링크 위반 시
     */
    @PostConstruct
    void init() {
        final JsonNode root = loadJsonFromClasspath();
        final String startNodeId = root.get("startNodeId").asText();
        final List<MapNode> nodes = parseNodes(root);
        final List<Dungeon> dungeons = parseDungeons(root);
        validateBidirectionalLinks(nodes);
        this.mapGraph = new MapGraph(nodes, dungeons, startNodeId);
    }

    /**
     * 노드 ID로 맵 노드를 조회합니다.
     *
     * @param id 조회할 노드 ID
     * @return 대응하는 {@link MapNode}
     * @throws NodeNotFoundException 해당 ID의 노드가 존재하지 않는 경우
     */
    public MapNode node(final String id) {
        return mapGraph.byId(id)
                .orElseThrow(() -> new NodeNotFoundException(
                        "노드를 찾을 수 없습니다: " + id));
    }

    /**
     * 전체 맵 그래프를 반환합니다.
     *
     * @return 불변 {@link MapGraph} 인스턴스
     */
    public MapGraph graph() {
        return mapGraph;
    }

    /**
     * 던전 목록을 반환합니다.
     *
     * <p>{@code implemented:false}, {@code map:null}을 그대로 노출합니다.
     *
     * @return 불변 던전 목록
     */
    public List<Dungeon> dungeons() {
        return mapGraph.dungeons();
    }

    /**
     * 현재 노드를 중심으로 미니맵 뷰를 생성합니다.
     *
     * <p>{@link MapViewFactory}에 위임하여 격자 셀과 간선 정보를 산출합니다.
     *
     * @param currentNodeId 현재 노드 ID
     * @return 미니맵 뷰 모델
     * @throws com.myapps.web.myrpg.application.exception.MapViewGenerationException
     *         현재 노드가 그래프에 존재하지 않을 때
     */
    public MinimapView minimap(final String currentNodeId) {
        return mapViewFactory.createMinimap(mapGraph, currentNodeId);
    }

    /**
     * 전체지도 뷰를 생성합니다.
     *
     * <p>{@link MapViewFactory}에 위임하여 바운딩박스 기준 격자 배치를 산출합니다.
     *
     * @param currentNodeId 현재 노드 ID
     * @return 전체지도 뷰 모델
     * @throws com.myapps.web.myrpg.application.exception.MapViewGenerationException
     *         현재 노드가 그래프에 존재하지 않을 때
     */
    public FullMapView fullMap(final String currentNodeId) {
        return mapViewFactory.createFullMap(mapGraph, currentNodeId);
    }

    private JsonNode loadJsonFromClasspath() {
        final ClassPathResource resource = new ClassPathResource(MAP_JSON_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readTree(inputStream);
        } catch (final IOException exception) {
            throw new MapDataException(
                    "맵 JSON 파일 로딩 실패: " + MAP_JSON_PATH, exception);
        }
    }

    private List<MapNode> parseNodes(final JsonNode root) {
        final JsonNode nodesArray = root.get("nodes");
        if (nodesArray == null || !nodesArray.isArray()) {
            throw new MapDataException("맵 JSON에 'nodes' 배열이 없습니다.");
        }

        final List<MapNode> nodes = new ArrayList<>();
        for (final JsonNode nodeJson : nodesArray) {
            final MapNode mapNode = parseNode(nodeJson);
            nodes.add(mapNode);
        }
        return List.copyOf(nodes);
    }

    private MapNode parseNode(final JsonNode nodeJson) {
        final String id = nodeJson.get("id").asText();
        final String name = nodeJson.get("name").asText();
        final String type = nodeJson.get("type").asText();
        final NodeType nodeType = NodeType.fromType(type).orElse(null);
        final int x = nodeJson.get("x").asInt();
        final int y = nodeJson.get("y").asInt();
        final String dungeonId = nodeJson.has("dungeonId")
                ? nodeJson.get("dungeonId").asText() : null;
        final String theme = nodeJson.has("theme")
                ? nodeJson.get("theme").asText() : null;
        final List<String> links = parseLinks(nodeJson);

        return new MapNode(id, name, type, nodeType, x, y, dungeonId, theme, links);
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

    private List<Dungeon> parseDungeons(final JsonNode root) {
        final JsonNode dungeonsArray = root.get("dungeons");
        if (dungeonsArray == null || !dungeonsArray.isArray()) {
            return List.of();
        }

        final List<Dungeon> dungeons = new ArrayList<>();
        for (final JsonNode dungeonJson : dungeonsArray) {
            final Dungeon dungeon = parseDungeon(dungeonJson);
            dungeons.add(dungeon);
        }
        return List.copyOf(dungeons);
    }

    private Dungeon parseDungeon(final JsonNode dungeonJson) {
        final String id = dungeonJson.get("id").asText();
        final String name = dungeonJson.get("name").asText();
        final String entranceNodeId = dungeonJson.get("entranceNodeId").asText();
        final boolean implemented = dungeonJson.get("implemented").asBoolean();
        final Object map = dungeonJson.get("map").isNull() ? null : dungeonJson.get("map");
        return new Dungeon(id, name, entranceNodeId, implemented, map);
    }

    private void validateBidirectionalLinks(final List<MapNode> nodes) {
        final Map<String, MapNode> nodeMap = nodes.stream()
                .collect(Collectors.toUnmodifiableMap(
                        MapNode::id, node -> node));

        for (final MapNode node : nodes) {
            for (final String linkedId : node.links()) {
                final MapNode linkedNode = nodeMap.get(linkedId);
                if (linkedNode == null) {
                    throw new MapDataException(
                            "노드 '" + node.id() + "'의 링크 대상 '" + linkedId
                                    + "'가 존재하지 않습니다.");
                }
                if (!linkedNode.links().contains(node.id())) {
                    throw new MapDataException(
                            "양방향 링크 위반: 노드 '" + node.id() + "' → '"
                                    + linkedId + "' 링크 존재, 그러나 '"
                                    + linkedId + "' → '" + node.id()
                                    + "' 역방향 링크 부재");
                }
            }
        }
    }
}
