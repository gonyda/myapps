package com.myapps.web.myrpg.application.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.myapps.web.myrpg.domain.model.Dungeon;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;
import com.myapps.web.myrpg.domain.service.MapViewFactory;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MapService#dungeons()} 메서드의 단위 테스트.
 *
 * <p>실제 {@code classpath:data/map.json}을 로드한 {@link MapService}로부터
 * 던전 목록을 조회하여 다음을 검증한다:
 * <ul>
 *   <li>모든 던전의 {@code implemented}가 {@code false}인지</li>
 *   <li>모든 던전의 {@code map}이 {@code null}인지</li>
 *   <li>{@code entranceNodeId}가 실제 노드 그래프에서 {@code type="dungeon"} 노드를 참조하는지</li>
 *   <li>던전 입구 노드의 {@code dungeonId}가 해당 던전의 {@code id}와 일치하는지</li>
 * </ul>
 *
 * <p><b>Validates: Requirements 6.2, 10.2</b>
 */
class MapServiceDungeonTest {

    private static final int EXPECTED_DUNGEON_COUNT = 3;

    private MapService mapService;

    /**
     * 실제 맵 JSON을 로드하여 MapService를 초기화한다.
     */
    @BeforeEach
    void setUp() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final MapViewFactory mapViewFactory = new MapViewFactory();
        mapService = new MapService(objectMapper, mapViewFactory);
        mapService.init();
    }

    /**
     * 던전 목록이 정확히 3개(alby, ciar, rabbie)인지 검증한다.
     */
    @Test
    void should_returnThreeDungeons_when_loadedFromMapJson() {
        // When
        final List<Dungeon> dungeons = mapService.dungeons();

        // Then
        assertThat(dungeons).hasSize(EXPECTED_DUNGEON_COUNT);
    }

    /**
     * 모든 던전의 {@code implemented} 필드가 {@code false}인지 검증한다.
     */
    @Test
    void should_haveImplementedFalse_when_allDungeonsExposed() {
        // When
        final List<Dungeon> dungeons = mapService.dungeons();

        // Then
        assertThat(dungeons)
                .allSatisfy(dungeon ->
                        assertThat(dungeon.implemented()).isFalse());
    }

    /**
     * 모든 던전의 {@code map} 필드가 {@code null}인지 검증한다.
     */
    @Test
    void should_haveMapNull_when_allDungeonsExposed() {
        // When
        final List<Dungeon> dungeons = mapService.dungeons();

        // Then
        assertThat(dungeons)
                .allSatisfy(dungeon ->
                        assertThat(dungeon.map()).isNull());
    }

    /**
     * 각 던전의 {@code entranceNodeId}가 노드 그래프에 존재하며
     * 해당 노드의 타입이 {@code dungeon}인지 검증한다.
     */
    @Test
    void should_referenceExistingDungeonNode_when_entranceNodeIdResolved() {
        // Given
        final List<Dungeon> dungeons = mapService.dungeons();
        final MapGraph graph = mapService.graph();

        // When & Then
        for (final Dungeon dungeon : dungeons) {
            final Optional<MapNode> entranceNode = graph.byId(dungeon.entranceNodeId());

            assertThat(entranceNode)
                    .as("던전 '%s'의 entranceNodeId '%s'가 노드 그래프에 존재해야 함",
                            dungeon.id(), dungeon.entranceNodeId())
                    .isPresent();

            assertThat(entranceNode.get().nodeType())
                    .as("던전 '%s'의 입구 노드 타입이 DUNGEON이어야 함", dungeon.id())
                    .isEqualTo(NodeType.DUNGEON);

            assertThat(entranceNode.get().type())
                    .as("던전 '%s'의 입구 노드 원본 타입 문자열이 'dungeon'이어야 함", dungeon.id())
                    .isEqualTo("dungeon");
        }
    }

    /**
     * 각 던전 입구 노드의 {@code dungeonId}가 해당 던전의 {@code id}와 일치하는지 검증한다.
     * (던전 → 입구 노드 → dungeonId → 던전 id 양방향 참조 무결성)
     */
    @Test
    void should_matchDungeonId_when_entranceNodeDungeonIdResolved() {
        // Given
        final List<Dungeon> dungeons = mapService.dungeons();
        final MapGraph graph = mapService.graph();

        // When & Then
        for (final Dungeon dungeon : dungeons) {
            final MapNode entranceNode = graph.byId(dungeon.entranceNodeId()).orElseThrow();

            assertThat(entranceNode.dungeonId())
                    .as("입구 노드 '%s'의 dungeonId가 던전 '%s'의 id와 일치해야 함",
                            entranceNode.id(), dungeon.id())
                    .isEqualTo(dungeon.id());
        }
    }

    /**
     * 모든 {@code type="dungeon"} 노드가 던전 정의에 의해 참조되는지 검증한다.
     * (고아 던전 입구 노드가 없음을 보장)
     */
    @Test
    void should_coverAllDungeonNodes_when_dungeonEntranceReferencesChecked() {
        // Given
        final List<Dungeon> dungeons = mapService.dungeons();
        final MapGraph graph = mapService.graph();

        final Map<String, String> entranceNodeIdToDungeonId = dungeons.stream()
                .collect(Collectors.toUnmodifiableMap(
                        Dungeon::entranceNodeId, Dungeon::id));

        final List<MapNode> dungeonTypeNodes = graph.nodes().stream()
                .filter(node -> NodeType.DUNGEON == node.nodeType())
                .toList();

        // When & Then
        assertThat(dungeonTypeNodes)
                .allSatisfy(node ->
                        assertThat(entranceNodeIdToDungeonId)
                                .as("던전 타입 노드 '%s'가 던전 정의에서 참조되어야 함", node.id())
                                .containsKey(node.id()));
    }
}
