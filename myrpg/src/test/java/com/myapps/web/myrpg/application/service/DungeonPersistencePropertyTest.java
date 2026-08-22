package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.myapps.web.myrpg.application.dto.DungeonBossSpec;
import com.myapps.web.myrpg.application.dto.DungeonGenerationSpec;
import com.myapps.web.myrpg.application.dto.DungeonMonsterEntry;
import com.myapps.web.myrpg.application.dto.DungeonRewardSpec;
import com.myapps.web.myrpg.application.dto.DungeonSpec;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.DungeonProgressEntity;
import com.myapps.web.myrpg.domain.model.DungeonRoomState;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.DungeonProgressRepository;
import com.myapps.web.myrpg.domain.service.DungeonGenerator;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link DungeonService}의 던전 인스턴스 직렬화/역직렬화 무손실 복원 프로퍼티 기반 테스트.
 *
 * <p>임의로 생성 및 상태 전이된 던전 인스턴스가 DB 직렬화(JSON) 및 역직렬화를 거쳐도 모든 그래프 구조, 노드 정보, 방 상태가 완벽히 보존되는 불변성을 검증한다.
 *
 * <p>Feature: 011-dungeon-system, Property 4
 *
 * <p><b>Validates: Requirements 9.1, 9.2</b>
 */
class DungeonPersistencePropertyTest {

    private final DungeonGenerator generator = new DungeonGenerator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Property 4: DB 직렬화/역직렬화 후 던전 그래프 및 룸 상태 무손실 복원.
     *
     * @param spec 임의의 유효 던전 스펙
     * @param characterId 임의의 캐릭터 ID
     */
    @Property(tries = 100)
    void property4_serialization_and_deserialization_lossless_invariant(
            @ForAll("validDungeonSpec") final DungeonSpec spec, @ForAll final long characterId) {
        // given (준비: 임의 던전 인스턴스 생성 및 상태 변형)
        final Long safeCharId = Math.abs(characterId) + 1L;
        final DungeonInstance original = generator.generate(spec, safeCharId);

        // 일부 방 상태 임의 전이 시뮬레이션
        final String startRoomId = original.startRoomId();
        final MapNode startNode = original.dungeonGraph().byId(startRoomId).orElseThrow();
        if (!startNode.links().isEmpty()) {
            final String nextRoomId = startNode.links().get(0);
            original.moveTo(nextRoomId);
            original.markDiscovered(nextRoomId);
            original.markCleared(nextRoomId);
        }

        final DungeonService service =
                new DungeonService(
                        mock(DungeonSpecRepository.class),
                        generator,
                        mock(DungeonProgressRepository.class),
                        mock(CharacterProgressRepository.class),
                        mock(ProgressionService.class),
                        mock(InventoryService.class),
                        mock(ActionLog.class),
                        objectMapper);

        // when (실행: 던전 인스턴스를 Entity(JSON)로 직렬화 후 다시 역직렬화 복원)
        final String graphJson = service.serializeDungeonGraph(original.dungeonGraph());
        final String roomStatesJson = service.serializeRoomStates(original.roomStates());

        final DungeonProgressEntity entity =
                new DungeonProgressEntity(
                        original.characterId(),
                        original.dungeonId(),
                        original.entranceNodeId(),
                        original.startRoomId(),
                        original.bossRoomId(),
                        original.currentRoomId(),
                        graphJson,
                        roomStatesJson);

        final DungeonInstance restored = service.restoreInstance(entity);

        // then (검증: 메타데이터, 현재 위치, 맵 그래프, 방 상태의 완전 일치 검증)
        assertThat(restored.characterId()).isEqualTo(original.characterId());
        assertThat(restored.dungeonId()).isEqualTo(original.dungeonId());
        assertThat(restored.entranceNodeId()).isEqualTo(original.entranceNodeId());
        assertThat(restored.startRoomId()).isEqualTo(original.startRoomId());
        assertThat(restored.bossRoomId()).isEqualTo(original.bossRoomId());
        assertThat(restored.currentRoomId()).isEqualTo(original.currentRoomId());

        // 맵 그래프 노드 검증
        final MapGraph origGraph = original.dungeonGraph();
        final MapGraph restGraph = restored.dungeonGraph();
        assertThat(restGraph.nodes()).hasSameSizeAs(origGraph.nodes());

        for (final MapNode origNode : origGraph.nodes()) {
            final MapNode restNode = restGraph.byId(origNode.id()).orElse(null);
            assertThat(restNode).isNotNull();
            assertThat(restNode.name()).isEqualTo(origNode.name());
            assertThat(restNode.type()).isEqualTo(origNode.type());
            assertThat(restNode.nodeType()).isEqualTo(origNode.nodeType());
            assertThat(restNode.x()).isEqualTo(origNode.x());
            assertThat(restNode.y()).isEqualTo(origNode.y());
            assertThat(restNode.dungeonId()).isEqualTo(origNode.dungeonId());
            assertThat(restNode.theme()).isEqualTo(origNode.theme());
            assertThat(restNode.links()).containsExactlyInAnyOrderElementsOf(origNode.links());
            assertThat(restNode.monsters()).containsExactlyElementsOf(origNode.monsters());
        }

        // 방 상태 맵 검증
        final Map<String, DungeonRoomState> origStates = original.roomStates();
        final Map<String, DungeonRoomState> restStates = restored.roomStates();
        assertThat(restStates).hasSameSizeAs(origStates);

        for (final Map.Entry<String, DungeonRoomState> entry : origStates.entrySet()) {
            final String roomId = entry.getKey();
            final DungeonRoomState origState = entry.getValue();
            final DungeonRoomState restState = restStates.get(roomId);

            assertThat(restState).isNotNull();
            assertThat(restState.roomId()).isEqualTo(origState.roomId());
            assertThat(restState.cleared()).isEqualTo(origState.cleared());
            assertThat(restState.discovered()).isEqualTo(origState.discovered());
            assertThat(restState.remainingMonsters())
                    .containsExactlyElementsOf(origState.remainingMonsters());
        }
    }

    @Provide
    Arbitrary<DungeonSpec> validDungeonSpec() {
        final Arbitrary<Integer> distanceArbitrary = Arbitraries.integers().between(3, 10);
        final Arbitrary<Integer> branchDepthArbitrary = Arbitraries.integers().between(1, 3);

        return Combinators.combine(distanceArbitrary, branchDepthArbitrary)
                .as(
                        (distance, branchDepth) -> {
                            final DungeonGenerationSpec genSpec =
                                    new DungeonGenerationSpec(
                                            distance,
                                            distance,
                                            distance + 3,
                                            distance + 6,
                                            0.40,
                                            branchDepth);

                            final List<DungeonMonsterEntry> monsterPool =
                                    List.of(
                                            new DungeonMonsterEntry("spider", 1, 2, 40),
                                            new DungeonMonsterEntry("goblin", 1, 2, 30),
                                            new DungeonMonsterEntry("black-spider", 1, 1, 30));

                            final DungeonBossSpec boss =
                                    new DungeonBossSpec("giant-spider", "거대거미", "샤아악");
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
}
