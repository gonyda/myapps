package com.myapps.web.myrpg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.exception.BlockedMovementException;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.DungeonService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.DungeonInstance;
import com.myapps.web.myrpg.domain.model.DungeonRoomState;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.DungeonProgressRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import com.myapps.web.myrpg.interfaces.api.NodeViewAssembler;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 던전 전체 생명주기 및 사용자 흐름에 대한 E2E 통합 테스트.
 *
 * <p>입장 -> 탐색/안개 해제 -> 몬스터 처치 및 백트래킹 제약 -> 보스 격퇴/보상 획득/자동 퇴장, 자발적 퇴장, 사망 시 리스폰 흐름을 검증한다.
 *
 * <p>Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 3.1, 3.2, 5.1, 5.2, 6.1, 6.2, 6.3, 7.1
 */
@SpringBootTest
@Transactional
class DungeonLifecycleIntegrationTest {

    @Autowired private DungeonService dungeonService;

    @Autowired private CharacterService characterService;

    @Autowired private NodeViewAssembler nodeViewAssembler;

    @Autowired private DungeonProgressRepository dungeonProgressRepository;

    @Autowired private OwnedItemRepository ownedItemRepository;

    @Autowired private com.myapps.web.myrpg.application.service.InventoryService inventoryService;

    private CharacterProgress character;

    @BeforeEach
    void setUp() {
        character = characterService.loadOrCreateDefault();
        character.updateCurrentNodeId("alby-entrance");
        characterService.saveTurn(character);
        dungeonProgressRepository.deleteByCharacterId(character.getId());
        if (ownedItemRepository
                .findByCharacterIdAndStorageAndItemId(
                        character.getId(), StorageKind.INVENTORY, "hp_potion_30")
                .isEmpty()) {
            inventoryService.seedDefault(character.getId());
        }
    }

    @Test
    @DisplayName("던전 입장부터 보스 격퇴 및 보상 수령, 자동 퇴장까지 전체 라이프사이클이 정상 작동한다")
    void should_completeFullDungeonLifecycle_successfully() {
        final Long charId = character.getId();
        final long initialExp = character.getExperience();
        final long initialGold = character.getGold();

        // 1. 던전 입장 (Req 1.1, 1.2, 2.1)
        final DungeonInstance dungeon = dungeonService.enterDungeon(charId, "alby");
        assertThat(dungeon).isNotNull();
        assertThat(dungeon.dungeonId()).isEqualTo("alby");
        assertThat(dungeon.entranceNodeId()).isEqualTo("alby-entrance");
        assertThat(dungeon.currentRoomId()).isEqualTo("room-0-0");
        assertThat(dungeon.roomStates().get("room-0-0").cleared()).isTrue();
        assertThat(dungeon.roomStates().get("room-0-0").discovered()).isTrue();

        // 캐릭터 노드가 시작방으로 변경되었는지 확인
        final CharacterProgress afterEnter = characterService.loadOrCreateDefault();
        assertThat(afterEnter.getCurrentNodeId()).isEqualTo("room-0-0");

        // 2. 시작방 뷰 모델 검증 (Req 1.3, 4.1, 4.2)
        final PlayScreenView startView = nodeViewAssembler.fromProgress(afterEnter);
        assertThat(startView.minimap().mapName()).isEqualTo("시작방");
        assertThat(startView.interactions())
                .anyMatch(item -> "dungeon-leave".equals(item.actionType()));

        // 3. 인접 방으로 이동 (Req 3.1, 7.1)
        final List<String> startLinks =
                dungeon.dungeonGraph().nodes().stream()
                        .filter(n -> n.id().equals("room-0-0"))
                        .findFirst()
                        .orElseThrow()
                        .links();
        final String nextRoomId = startLinks.getFirst();

        dungeonService.moveToRoom(charId, nextRoomId);
        final DungeonInstance dungeonAfterMove =
                dungeonService.getActiveDungeon(charId).orElseThrow();
        assertThat(dungeonAfterMove.currentRoomId()).isEqualTo(nextRoomId);
        assertThat(dungeonAfterMove.roomStates().get(nextRoomId).discovered()).isTrue();

        // 4. 미클리어 방에서 전진 시도 시 차단 및 후퇴 허용 검증 (Req 3.1, 3.2)
        final DungeonRoomState roomState = dungeonAfterMove.roomStates().get(nextRoomId);
        if (!roomState.cleared() && !roomState.remainingMonsters().isEmpty()) {
            final List<String> forwardLinks =
                    dungeon.dungeonGraph().nodes().stream()
                            .filter(n -> n.id().equals(nextRoomId))
                            .findFirst()
                            .orElseThrow()
                            .links()
                            .stream()
                            .filter(id -> !id.equals("room-0-0"))
                            .toList();

            if (!forwardLinks.isEmpty()) {
                final String forwardRoomId = forwardLinks.getFirst();
                assertThatThrownBy(() -> dungeonService.moveToRoom(charId, forwardRoomId))
                        .isInstanceOf(BlockedMovementException.class);
            }

            // 시작방으로의 후퇴는 허용되어야 함
            dungeonService.moveToRoom(charId, "room-0-0");
            assertThat(dungeonService.getActiveDungeon(charId).orElseThrow().currentRoomId())
                    .isEqualTo("room-0-0");

            // 다시 몬스터 방으로 복귀
            dungeonService.moveToRoom(charId, nextRoomId);

            // 몬스터 순차 격퇴 (Req 5.1, 5.2)
            final List<String> monstersToKill = List.copyOf(roomState.remainingMonsters());
            for (final String monsterId : monstersToKill) {
                dungeonService.onMonsterDefeated(charId, monsterId);
            }

            final DungeonInstance dungeonAfterClear =
                    dungeonService.getActiveDungeon(charId).orElseThrow();
            assertThat(dungeonAfterClear.roomStates().get(nextRoomId).cleared()).isTrue();
            assertThat(dungeonAfterClear.roomStates().get(nextRoomId).remainingMonsters())
                    .isEmpty();
        }

        // 5. 보스방 격퇴 (Req 6.1, 6.2, 6.3)
        dungeonService.onBossDefeated(charId);

        // 던전 종료 후 확인: 던전 인스턴스 삭제, 캐릭터 입구 노드로 귀환, 보상 지급 (EXP 1000, 골드 2000)
        final Optional<DungeonInstance> activeDungeon = dungeonService.getActiveDungeon(charId);
        assertThat(activeDungeon).isEmpty();

        final CharacterProgress completedChar = characterService.loadOrCreateDefault();
        assertThat(completedChar.getCurrentNodeId()).isEqualTo("alby-entrance");
        assertThat(completedChar.getCurrentLevel()).isGreaterThan(1);
        assertThat(completedChar.getGold()).isEqualTo(initialGold + 2000);

        final Optional<OwnedItem> potionOpt =
                ownedItemRepository.findByCharacterIdAndStorageAndItemId(
                        charId, StorageKind.INVENTORY, "hp_potion_30");
        assertThat(potionOpt).isPresent();
        assertThat(potionOpt.get().getQuantity()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("시작방에서 자발적으로 퇴장 시 던전 인스턴스가 삭제되고 던전 입구로 복귀한다")
    void should_leaveDungeon_fromStartRoom() {
        final Long charId = character.getId();

        dungeonService.enterDungeon(charId, "alby");
        assertThat(dungeonService.getActiveDungeon(charId)).isPresent();

        dungeonService.leaveDungeon(charId);

        assertThat(dungeonService.getActiveDungeon(charId)).isEmpty();
        final CharacterProgress afterLeave = characterService.loadOrCreateDefault();
        assertThat(afterLeave.getCurrentNodeId()).isEqualTo("alby-entrance");
    }

    @Test
    @DisplayName("던전 내에서 플레이어 사망 시 던전 인스턴스가 정리되고 부활 노드로 이동한다")
    void should_handlePlayerDeath_inDungeon() {
        final Long charId = character.getId();

        dungeonService.enterDungeon(charId, "alby");
        assertThat(dungeonService.getActiveDungeon(charId)).isPresent();

        dungeonService.handlePlayerDeath(charId);

        assertThat(dungeonService.getActiveDungeon(charId)).isEmpty();
        final CharacterProgress afterDeath = characterService.loadOrCreateDefault();
        assertThat(afterDeath.getCurrentNodeId()).isEqualTo("tir-chonaill");
    }
}
