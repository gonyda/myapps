package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.myapps.web.myrpg.application.dto.DungeonBossSpec;
import com.myapps.web.myrpg.application.dto.DungeonGenerationSpec;
import com.myapps.web.myrpg.application.dto.DungeonMonsterEntry;
import com.myapps.web.myrpg.application.dto.DungeonRewardSpec;
import com.myapps.web.myrpg.application.dto.DungeonSpec;
import com.myapps.web.myrpg.application.exception.BlockedMovementException;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.DungeonProgressEntity;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.DungeonProgressRepository;
import com.myapps.web.myrpg.domain.service.DungeonGenerator;
import java.util.List;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link DungeonService}의 백트래킹 이동 규칙 프로퍼티 기반 테스트.
 *
 * <p>미클리어 방에서는 이미 클리어된 방으로의 후퇴만 허용되고, 미클리어 방으로의 전진은 차단되는 불변성을 검증한다.
 *
 * <p>Feature: 011-dungeon-system, Property 3
 *
 * <p><b>Validates: Requirements 7.1, 7.2</b>
 */
class DungeonBacktrackingPropertyTest {

    private final DungeonGenerator generator = new DungeonGenerator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Property 3: 미클리어 방에서의 백트래킹 이동 규칙 불변 (클리어방 후퇴 허용 vs 미클리어방 전진 차단).
     *
     * @param spec 임의의 유효 던전 스펙
     * @param characterId 임의의 캐릭터 ID
     */
    @Property(tries = 50)
    void property3_backtrackingMovementRules_invariant(
            @ForAll("validDungeonSpec") final DungeonSpec spec, @ForAll final long characterId) {
        // given (준비: 프로시저럴 던전 생성 및 Mock 환경 구성)
        final Long safeCharId = Math.abs(characterId) + 1L;
        final DungeonInstance instance = generator.generate(spec, safeCharId);
        final MapGraph graph = instance.dungeonGraph();

        final DungeonProgressRepository mockDungeonRepo = mock(DungeonProgressRepository.class);
        final CharacterProgressRepository mockCharRepo = mock(CharacterProgressRepository.class);
        final DungeonSpecRepository mockSpecRepo = mock(DungeonSpecRepository.class);
        final ProgressionService mockProgression = mock(ProgressionService.class);
        final InventoryService mockInventory = mock(InventoryService.class);
        final ActionLog mockActionLog = mock(ActionLog.class);

        final DungeonService service =
                new DungeonService(
                        mockSpecRepo,
                        generator,
                        mockDungeonRepo,
                        mockCharRepo,
                        mockProgression,
                        mockInventory,
                        mock(MonsterRewardService.class),
                        mock(ItemCatalogService.class),
                        mockActionLog,
                        objectMapper);

        final CharacterProgress character = CharacterProgress.createDefault();
        given(mockCharRepo.findById(safeCharId)).willReturn(Optional.of(character));

        final String startRoomId = instance.startRoomId();
        final MapNode startNode = graph.byId(startRoomId).orElseThrow();
        assertThat(startNode.links()).isNotEmpty();

        // 시작방과 연결된 첫 번째 미클리어 방으로 이동
        final String firstUnclearedRoomId = startNode.links().get(0);
        instance.moveTo(firstUnclearedRoomId);

        final DungeonProgressEntity entity =
                new DungeonProgressEntity(
                        safeCharId,
                        spec.id(),
                        spec.entranceNodeId(),
                        instance.startRoomId(),
                        instance.bossRoomId(),
                        instance.currentRoomId(),
                        service.serializeDungeonGraph(instance.dungeonGraph()),
                        service.serializeRoomStates(instance.roomStates()));

        given(mockDungeonRepo.findByCharacterId(safeCharId)).willReturn(Optional.of(entity));

        // when & then (검증: 미클리어 방에서 각 이웃 방으로의 이동 판정 불변성)
        final MapNode currentUnclearedNode = graph.byId(firstUnclearedRoomId).orElseThrow();
        for (final String neighborId : currentUnclearedNode.links()) {
            final boolean neighborCleared = instance.isRoomCleared(neighborId);

            if (neighborCleared) {
                // 후퇴 허용: 이미 클리어된 방으로의 이동은 성공해야 함
                final DungeonInstance moved = service.moveToRoom(safeCharId, neighborId);
                assertThat(moved.currentRoomId()).isEqualTo(neighborId);
                assertThat(character.getCurrentNodeId()).isEqualTo(neighborId);

                // 원위치 복귀 (다음 검사를 위해)
                entity.setCurrentRoomId(firstUnclearedRoomId);
                instance.moveTo(firstUnclearedRoomId);
            } else {
                // 전진 차단: 아직 클리어되지 않은 방으로의 이동은 BlockedMovementException 발생해야 함
                assertThatThrownBy(() -> service.moveToRoom(safeCharId, neighborId))
                        .isInstanceOf(BlockedMovementException.class)
                        .hasMessageContaining("앞으로 나아가려면 이 방의 적들을 모두 처치해야 합니다");
            }
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
                                            new DungeonMonsterEntry("red-spider", 1, 1, 30));

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
