package com.myapps.web.myrpg.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.application.dto.DungeonBossSpec;
import com.myapps.web.myrpg.application.dto.DungeonGenerationSpec;
import com.myapps.web.myrpg.application.dto.DungeonMonsterEntry;
import com.myapps.web.myrpg.application.dto.DungeonRewardSpec;
import com.myapps.web.myrpg.application.dto.DungeonSpec;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * {@link DungeonGenerator} 프로시저럴 던전 생성기 프로퍼티 기반 테스트.
 *
 * <p>임의의 유효 던전 스펙에 대해 생성된 던전의 보스방 최단거리, 방 개수 범위, 양방향 연결성, 좌표 고유성 및 그래프 연결성 불변을 검증한다.
 *
 * <p>Feature: 011-dungeon-system, Property 1 & Property 2
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6</b>
 */
class DungeonGeneratorPropertyTest {

    private final DungeonGenerator generator = new DungeonGenerator();

    /**
     * Property 1: 생성된 던전의 보스방 최단 거리 및 전체 방 개수 범위 불변.
     *
     * @param spec 임의의 유효 던전 스펙
     * @param characterId 임의의 캐릭터 ID
     */
    @Property(tries = 100)
    void property1_shortestDistanceToBoss_and_totalRoomsCount_invariant(
            @ForAll("validDungeonSpec") final DungeonSpec spec, @ForAll final long characterId) {
        // given (준비: 유효 던전 스펙과 캐릭터 ID)
        final Long safeCharId = Math.abs(characterId) + 1L;

        // when (실행: 던전 생성 엔진 구동)
        final DungeonInstance instance = generator.generate(spec, safeCharId);

        // then (검증: 방 개수 범위 및 보스방 최단거리 불변 검증)
        final MapGraph graph = instance.dungeonGraph();
        final int nodeCount = graph.nodes().size();
        final DungeonGenerationSpec gen = spec.generation();

        assertThat(nodeCount)
                .as(
                        "Total rooms count %d must be within [%d, %d]",
                        nodeCount, gen.minTotalRooms(), gen.maxTotalRooms())
                .isBetween(gen.minTotalRooms(), gen.maxTotalRooms());

        final int shortestDistance =
                computeBfsDistance(graph, instance.startRoomId(), instance.bossRoomId());
        assertThat(shortestDistance)
                .as(
                        "Shortest path distance %d must be within [%d, %d]",
                        shortestDistance, gen.minDistanceToBoss(), gen.maxDistanceToBoss())
                .isBetween(gen.minDistanceToBoss(), gen.maxDistanceToBoss());
    }

    /**
     * Property 2: 모든 통로의 양방향 연결성 및 좌표 고유성, 전체 연결성 불변.
     *
     * @param spec 임의의 유효 던전 스펙
     * @param characterId 임의의 캐릭터 ID
     */
    @Property(tries = 100)
    void property2_bidirectionalLinks_and_uniqueCoordinates_invariant(
            @ForAll("validDungeonSpec") final DungeonSpec spec, @ForAll final long characterId) {
        // given (준비: 유효 던전 스펙과 캐릭터 ID)
        final Long safeCharId = Math.abs(characterId) + 1L;

        // when (실행: 던전 생성 엔진 구동)
        final DungeonInstance instance = generator.generate(spec, safeCharId);
        final MapGraph graph = instance.dungeonGraph();

        // then (검증: 좌표 고유성, 양방향성, 인접 맨해튼 거리, 전체 연결성)
        final Set<String> coords = new HashSet<>();
        for (final MapNode node : graph.nodes()) {
            final String coordKey = node.x() + "," + node.y();
            assertThat(coords.add(coordKey)).as("Coordinate %s must be unique", coordKey).isTrue();

            for (final String neighborId : node.links()) {
                final MapNode neighbor = graph.byId(neighborId).orElseThrow();
                assertThat(neighbor.links())
                        .as("Link between %s and %s must be bidirectional", node.id(), neighborId)
                        .contains(node.id());

                final int manhattanDistance =
                        Math.abs(node.x() - neighbor.x()) + Math.abs(node.y() - neighbor.y());
                assertThat(manhattanDistance)
                        .as(
                                "Neighbor %s must be Manhattan distance 1 from %s",
                                neighborId, node.id())
                        .isEqualTo(1);
            }
        }

        // 전체 연결성 (Connected graph)
        final Set<String> reachable = computeReachableNodes(graph, instance.startRoomId());
        assertThat(reachable).hasSameSizeAs(graph.nodes());

        // 시작방 특성 불변성
        final MapNode startNode = graph.byId(instance.startRoomId()).orElseThrow();
        assertThat(startNode.x()).isEqualTo(0);
        assertThat(startNode.y()).isEqualTo(0);
        assertThat(instance.isRoomCleared(instance.startRoomId())).isTrue();
        assertThat(instance.isRoomDiscovered(instance.startRoomId())).isTrue();
        assertThat(instance.getRoomState(instance.startRoomId()).remainingMonsters()).isEmpty();
    }

    @Provide
    Arbitrary<DungeonSpec> validDungeonSpec() {
        final Arbitrary<Integer> distanceArbitrary = Arbitraries.integers().between(3, 11);
        final Arbitrary<Integer> branchDepthArbitrary = Arbitraries.integers().between(1, 3);

        return Combinators.combine(distanceArbitrary, branchDepthArbitrary)
                .as(
                        (distance, branchDepth) -> {
                            final int minDistance = distance;
                            final int maxDistance = distance;
                            final int minRooms = distance + 3;
                            final int maxRooms = distance + 7;
                            final DungeonGenerationSpec genSpec =
                                    new DungeonGenerationSpec(
                                            minDistance,
                                            maxDistance,
                                            minRooms,
                                            maxRooms,
                                            0.40,
                                            branchDepth);

                            final List<DungeonMonsterEntry> monsterPool =
                                    List.of(
                                            new DungeonMonsterEntry("spider", 1, 2, 40),
                                            new DungeonMonsterEntry("red-spider", 1, 2, 30),
                                            new DungeonMonsterEntry("goblin", 1, 1, 30));

                            final DungeonBossSpec boss =
                                    new DungeonBossSpec("giant-spider", "거대거미", "대사");
                            final DungeonRewardSpec rewards =
                                    new DungeonRewardSpec(1000, 2000, List.of());

                            return new DungeonSpec(
                                    "test-dungeon",
                                    "테스트 던전",
                                    "test-entrance",
                                    "dungeon-test",
                                    true,
                                    genSpec,
                                    monsterPool,
                                    0.10,
                                    boss,
                                    rewards);
                        });
    }

    private int computeBfsDistance(
            final MapGraph graph, final String startRoomId, final String targetRoomId) {
        final Queue<String> queue = new ArrayDeque<>();
        final Map<String, Integer> distanceMap = new HashMap<>();

        queue.add(startRoomId);
        distanceMap.put(startRoomId, 0);

        while (!queue.isEmpty()) {
            final String current = queue.poll();
            final int currentDist = distanceMap.get(current);

            if (current.equals(targetRoomId)) {
                return currentDist;
            }

            final MapNode node = graph.byId(current).orElseThrow();
            for (final String neighbor : node.links()) {
                if (!distanceMap.containsKey(neighbor)) {
                    distanceMap.put(neighbor, currentDist + 1);
                    queue.add(neighbor);
                }
            }
        }

        return -1;
    }

    private Set<String> computeReachableNodes(final MapGraph graph, final String startRoomId) {
        final Set<String> visited = new HashSet<>();
        final Queue<String> queue = new ArrayDeque<>();

        queue.add(startRoomId);
        visited.add(startRoomId);

        while (!queue.isEmpty()) {
            final String current = queue.poll();
            final MapNode node = graph.byId(current).orElseThrow();
            for (final String neighbor : node.links()) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return visited;
    }
}
