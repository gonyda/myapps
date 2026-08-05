package com.myapps.web.myrpg.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 맵 노드 그래프의 도메인 집계(Aggregate).
 *
 * <p>애플리케이션 기동 시 {@code classpath:data/map.json}에서 1회 파싱되어 불변으로 메모리에 보관된다.
 * 영속 대상이 아니며, 노드 조회·좌표 이웃 탐색 등의 헬퍼를 제공한다.
 */
public final class MapGraph {

    private final List<MapNode> nodes;
    private final Map<String, MapNode> byId;
    private final Map<String, MapNode> byCoord;
    private final List<Dungeon> dungeons;
    private final String startNodeId;

    /**
     * 맵 그래프를 구성한다.
     *
     * @param nodes       모든 맵 노드 목록
     * @param dungeons    던전 목록
     * @param startNodeId 시작 노드 ID (예: "tir-chonaill")
     */
    public MapGraph(final List<MapNode> nodes,
                    final List<Dungeon> dungeons,
                    final String startNodeId) {
        this.nodes = List.copyOf(nodes);
        this.dungeons = List.copyOf(dungeons);
        this.startNodeId = startNodeId;
        this.byId = this.nodes.stream()
                .collect(Collectors.toUnmodifiableMap(MapNode::id, node -> node));
        this.byCoord = this.nodes.stream()
                .collect(Collectors.toUnmodifiableMap(
                        node -> coordKey(node.x(), node.y()),
                        node -> node));
    }

    /**
     * 모든 노드를 반환한다.
     *
     * @return 불변 노드 목록
     */
    public List<MapNode> nodes() {
        return nodes;
    }

    /**
     * 노드 ID로 노드를 조회한다.
     *
     * @param id 노드 ID
     * @return 대응하는 노드를 감싼 {@code Optional}, 미존재 시 빈 {@code Optional}
     */
    public Optional<MapNode> byId(final String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * 좌표 키 문자열("x,y")로 노드를 조회한다.
     *
     * @param coordKey 좌표 키 문자열 (예: "3,5")
     * @return 대응하는 노드를 감싼 {@code Optional}, 미존재 시 빈 {@code Optional}
     */
    public Optional<MapNode> byCoord(final String coordKey) {
        return Optional.ofNullable(byCoord.get(coordKey));
    }

    /**
     * 던전 목록을 반환한다.
     *
     * @return 불변 던전 목록
     */
    public List<Dungeon> dungeons() {
        return dungeons;
    }

    /**
     * 시작 노드 ID를 반환한다.
     *
     * @return 시작 노드 ID
     */
    public String startNodeId() {
        return startNodeId;
    }

    /**
     * ID별 조회 맵을 반환한다.
     *
     * @return 노드 ID → {@link MapNode} 불변 맵
     */
    public Map<String, MapNode> byIdMap() {
        return byId;
    }

    /**
     * 좌표별 조회 맵을 반환한다.
     *
     * @return 좌표 키("x,y") → {@link MapNode} 불변 맵
     */
    public Map<String, MapNode> byCoordMap() {
        return byCoord;
    }

    /**
     * 기준 노드에서 좌표 오프셋(dx, dy)만큼 떨어진 이웃 노드를 탐색한다.
     *
     * @param baseNode 기준 노드
     * @param dx       X 좌표 오프셋
     * @param dy       Y 좌표 오프셋
     * @return 해당 좌표에 존재하는 노드를 감싼 {@code Optional}, 미존재 시 빈 {@code Optional}
     */
    public Optional<MapNode> neighborByOffset(final MapNode baseNode,
                                              final int dx,
                                              final int dy) {
        final String key = coordKey(baseNode.x() + dx, baseNode.y() + dy);
        return Optional.ofNullable(byCoord.get(key));
    }

    /**
     * 좌표 값으로부터 조회 키 문자열을 생성한다.
     *
     * @param x X 좌표
     * @param y Y 좌표
     * @return "x,y" 형식의 키 문자열
     */
    public static String coordKey(final int x, final int y) {
        return x + "," + y;
    }
}
