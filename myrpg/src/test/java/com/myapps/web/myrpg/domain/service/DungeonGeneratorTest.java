package com.myapps.web.myrpg.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myapps.web.myrpg.application.dto.DungeonBossSpec;
import com.myapps.web.myrpg.application.dto.DungeonGenerationSpec;
import com.myapps.web.myrpg.application.dto.DungeonMonsterEntry;
import com.myapps.web.myrpg.application.dto.DungeonRewardSpec;
import com.myapps.web.myrpg.application.dto.DungeonSpec;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.DungeonRoomState;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link DungeonGenerator} 프로시저럴 던전 생성 엔진 단위 테스트. */
class DungeonGeneratorTest {

    private final DungeonGenerator generator = new DungeonGenerator();

    private DungeonSpec createAlbySpec() {
        final DungeonGenerationSpec genSpec = new DungeonGenerationSpec(10, 10, 20, 23, 0.40, 3);
        final List<DungeonMonsterEntry> monsterPool =
                List.of(
                        new DungeonMonsterEntry("spider", 1, 2, 40),
                        new DungeonMonsterEntry("red-spider", 1, 2, 25),
                        new DungeonMonsterEntry("goblin", 1, 2, 25),
                        new DungeonMonsterEntry("black-spider", 1, 1, 10));
        final DungeonBossSpec boss = new DungeonBossSpec("giant-spider", "거대거미", "쿠구구궁…!");
        final DungeonRewardSpec rewards = new DungeonRewardSpec(1000, 2000, List.of());

        return new DungeonSpec(
                "alby",
                "알비 던전",
                "alby-entrance",
                "dungeon-alby",
                true,
                genSpec,
                monsterPool,
                0.10,
                boss,
                rewards);
    }

    @Test
    @DisplayName("알비 던전 생성 시 보스방 최단거리 10, 총 방 20~23개 및 초기 안개/클리어 상태가 올바르게 설정된다")
    void should_generateAlbyDungeon_withExpectedInvariants() {
        // given
        final DungeonSpec spec = createAlbySpec();
        final Long characterId = 1L;

        // when
        final DungeonInstance instance = generator.generate(spec, characterId);

        // then
        assertThat(instance).isNotNull();
        assertThat(instance.characterId()).isEqualTo(characterId);
        assertThat(instance.dungeonId()).isEqualTo("alby");
        assertThat(instance.entranceNodeId()).isEqualTo("alby-entrance");
        assertThat(instance.startRoomId()).isEqualTo("room-0-0");
        assertThat(instance.currentRoomId()).isEqualTo("room-0-0");

        final MapGraph graph = instance.dungeonGraph();
        assertThat(graph.nodes().size()).isBetween(20, 23);
        assertThat(instance.roomStates()).hasSize(graph.nodes().size());

        // 시작방 검증
        final MapNode startNode = graph.byId("room-0-0").orElseThrow();
        assertThat(startNode.x()).isEqualTo(0);
        assertThat(startNode.y()).isEqualTo(0);
        assertThat(startNode.name()).isEqualTo("시작방");
        assertThat(instance.isRoomCleared("room-0-0")).isTrue();
        assertThat(instance.isRoomDiscovered("room-0-0")).isTrue();
        assertThat(instance.getRoomState("room-0-0").remainingMonsters()).isEmpty();

        // 보스방 검증
        final String bossRoomId = instance.bossRoomId();
        assertThat(bossRoomId).isNotEqualTo("room-0-0");
        final MapNode bossNode = graph.byId(bossRoomId).orElseThrow();
        assertThat(bossNode.name()).isEqualTo("거대거미의 방");
        assertThat(instance.isRoomCleared(bossRoomId)).isFalse();
        assertThat(instance.getRoomState(bossRoomId).remainingMonsters())
                .containsExactly("giant-spider");

        // BFS 최단거리 검증 (시작방 -> 보스방 최단거리 10)
        final int shortestDistance = computeBfsDistance(graph, "room-0-0", bossRoomId);
        assertThat(shortestDistance).isEqualTo(10);

        // 안개 발견 상태 검증 (시작방 및 그 직속 이웃만 discovered=true)
        final Set<String> startNeighbors = new HashSet<>(startNode.links());
        for (final MapNode node : graph.nodes()) {
            final DungeonRoomState state = instance.getRoomState(node.id());
            if (node.id().equals("room-0-0") || startNeighbors.contains(node.id())) {
                assertThat(state.discovered())
                        .as("Node %s should be discovered", node.id())
                        .isTrue();
            } else {
                assertThat(state.discovered()).as("Node %s should be hidden", node.id()).isFalse();
            }
        }
    }

    @Test
    @DisplayName("모든 통로는 양방향으로 연결되고 격자 인접성 및 좌표 고유성을 만족한다")
    void should_ensureBidirectionalLinks_and_uniqueCoordinates() {
        // given
        final DungeonSpec spec = createAlbySpec();
        final Long characterId = 1L;

        // when
        final DungeonInstance instance = generator.generate(spec, characterId);
        final MapGraph graph = instance.dungeonGraph();

        // then
        final Set<String> coords = new HashSet<>();
        for (final MapNode node : graph.nodes()) {
            // 좌표 고유성
            final String coordKey = node.x() + "," + node.y();
            assertThat(coords.add(coordKey)).as("Duplicate coordinate: %s", coordKey).isTrue();

            // 양방향 링크 및 격자 인접성
            for (final String neighborId : node.links()) {
                final MapNode neighbor = graph.byId(neighborId).orElseThrow();
                assertThat(neighbor.links())
                        .as("Link from %s to %s must be bidirectional", node.id(), neighborId)
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
    }

    @Test
    @DisplayName("동일한 시드로 난수 생성기를 주입하면 결정론적으로 동일한 맵이 생성된다")
    void should_generateDeterministically_when_sameSeedProvided() {
        // given
        final DungeonSpec spec = createAlbySpec();
        final Long characterId = 1L;
        final long seed = 42L;

        // when
        final DungeonInstance instance1 = generator.generate(spec, characterId, new Random(seed));
        final DungeonInstance instance2 = generator.generate(spec, characterId, new Random(seed));

        // then
        assertThat(instance1.bossRoomId()).isEqualTo(instance2.bossRoomId());
        assertThat(instance1.dungeonGraph().nodes().size())
                .isEqualTo(instance2.dungeonGraph().nodes().size());
        for (int i = 0; i < instance1.dungeonGraph().nodes().size(); i++) {
            final MapNode n1 = instance1.dungeonGraph().nodes().get(i);
            final MapNode n2 = instance2.dungeonGraph().nodes().get(i);
            assertThat(n1.id()).isEqualTo(n2.id());
            assertThat(n1.x()).isEqualTo(n2.x());
            assertThat(n1.y()).isEqualTo(n2.y());
            assertThat(n1.links()).isEqualTo(n2.links());
        }
    }

    @Test
    @DisplayName("몬스터 풀이 빈 던전 스펙에서도 방 생성 및 보스 배치가 정상 완료된다")
    void should_handleEmptyMonsterPool_gracefully() {
        // given
        final DungeonGenerationSpec genSpec = new DungeonGenerationSpec(3, 3, 5, 6, 0.40, 2);
        final DungeonBossSpec boss = new DungeonBossSpec("test-boss", "테스트보스", "대사");
        final DungeonSpec emptyPoolSpec =
                new DungeonSpec(
                        "test-dungeon",
                        "테스트 던전",
                        "test-entrance",
                        "dungeon-test",
                        true,
                        genSpec,
                        List.of(),
                        0.10,
                        boss,
                        new DungeonRewardSpec(100, 100, List.of()));

        // when
        final DungeonInstance instance = generator.generate(emptyPoolSpec, 1L);

        // then
        assertThat(instance.dungeonGraph().nodes().size()).isBetween(5, 6);
        for (final MapNode node : instance.dungeonGraph().nodes()) {
            final DungeonRoomState state = instance.getRoomState(node.id());
            if (node.id().equals(instance.bossRoomId())) {
                assertThat(state.remainingMonsters()).containsExactly("test-boss");
            } else {
                assertThat(state.remainingMonsters()).isEmpty();
            }
        }
    }

    @Test
    @DisplayName("필수 파라미터가 null이면 NullPointerException이 발생한다")
    void should_throwException_when_requiredParameterIsNull() {
        // given
        final DungeonSpec spec = createAlbySpec();

        // when & then
        assertThatThrownBy(() -> generator.generate(null, 1L))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> generator.generate(spec, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> generator.generate(spec, 1L, null))
                .isInstanceOf(NullPointerException.class);
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
}
