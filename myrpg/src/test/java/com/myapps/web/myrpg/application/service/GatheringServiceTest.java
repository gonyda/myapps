package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.WoodcutResult;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GatheringServiceTest {

    private InventoryService inventoryService;
    private CharacterProgressRepository characterProgressRepository;
    private StatProgression statProgression;
    private ActionLog actionLog;
    private Random random;
    private GatheringService gatheringService;

    @BeforeEach
    void setUp() {
        inventoryService = mock(InventoryService.class);
        characterProgressRepository = mock(CharacterProgressRepository.class);
        statProgression = mock(StatProgression.class);
        actionLog = mock(ActionLog.class);
        random = mock(Random.class);

        when(statProgression.vitalMaxFor(anyInt(), any(TalentType.class)))
                .thenReturn(new VitalMax(100, 100, 100));

        gatheringService =
                new GatheringService(
                        inventoryService,
                        characterProgressRepository,
                        statProgression,
                        actionLog,
                        random);
    }

    @Test
    @DisplayName("던전 노드에서는 나무가 절대 스폰되지 않는다")
    void should_notSpawnTree_inDungeonNode() {
        when(random.nextDouble()).thenReturn(0.1); // 0.1 < 0.50 이더라도

        final boolean spawned = gatheringService.rollTreeSpawn(1L, "alby-room-1", "dungeon");

        assertThat(spawned).isFalse();
        assertThat(gatheringService.isTreeAvailable(1L, "alby-room-1")).isFalse();
    }

    @Test
    @DisplayName("마을 노드에서 50% 확률 성공 시 나무가 스폰된다")
    void should_spawnTree_inTownNode_when_rollSucceeds() {
        when(random.nextDouble()).thenReturn(0.3); // 0.3 < 0.50

        final boolean spawned = gatheringService.rollTreeSpawn(1L, "tir-chonaill", "town");

        assertThat(spawned).isTrue();
        assertThat(gatheringService.isTreeAvailable(1L, "tir-chonaill")).isTrue();
    }

    @Test
    @DisplayName("스태미나가 5 미만이면 채집이 거부되고 스태미나가 소모되지 않는다")
    void should_rejectGathering_when_staminaIsLessThan5() {
        final CharacterProgress progress =
                new CharacterProgress(
                        "고니",
                        1,
                        1,
                        0L,
                        TalentType.MELEE,
                        null,
                        100,
                        100,
                        4,
                        "tir-chonaill",
                        0,
                        1000L);
        gatheringService.setTreeAvailable(1L, "tir-chonaill");

        final WoodcutResult result = gatheringService.gatherWood(progress);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("스태미나가 부족합니다");
        assertThat(progress.getStaminaCurrent()).isEqualTo(4);
        verify(inventoryService, never()).acquireItem(anyLong(), any(), anyInt());
    }

    @Test
    @DisplayName("스태미나 5 이상이고 채집 성공 시 장작이 지급되고 나무가 소멸된다")
    void should_acquireFirewood_and_depleteTree_when_gatheringSucceeds() {
        final CharacterProgress progress =
                new CharacterProgress(
                        "고니",
                        1,
                        1,
                        0L,
                        TalentType.MELEE,
                        null,
                        100,
                        100,
                        50,
                        "tir-chonaill",
                        0,
                        1000L);
        gatheringService.setTreeAvailable(1L, "tir-chonaill");
        when(random.nextDouble()).thenReturn(0.2); // 0.2 < 0.50 (성공)

        final WoodcutResult result = gatheringService.gatherWood(progress);

        assertThat(result.success()).isTrue();
        assertThat(result.itemId()).isEqualTo("firewood");
        assertThat(progress.getStaminaCurrent()).isEqualTo(45);
        assertThat(gatheringService.isTreeAvailable(1L, "tir-chonaill")).isFalse();
        verify(inventoryService).acquireItem(1L, "firewood", 1);
        verify(actionLog).add(eq("[채집] 🪵 단단한 장작을 1개 얻었습니다!"), eq("item"));
    }

    @Test
    @DisplayName("채집 실패 시 아이템이 지급되지 않고 스태미나와 나무는 소멸된다")
    void should_notAcquireFirewood_when_gatheringFails() {
        final CharacterProgress progress =
                new CharacterProgress(
                        "고니",
                        1,
                        1,
                        0L,
                        TalentType.MELEE,
                        null,
                        100,
                        100,
                        50,
                        "tir-chonaill",
                        0,
                        1000L);
        gatheringService.setTreeAvailable(1L, "tir-chonaill");
        when(random.nextDouble()).thenReturn(0.8); // 0.8 >= 0.50 (실패)

        final WoodcutResult result = gatheringService.gatherWood(progress);

        assertThat(result.success()).isFalse();
        assertThat(result.itemId()).isNull();
        assertThat(progress.getStaminaCurrent()).isEqualTo(45);
        assertThat(gatheringService.isTreeAvailable(1L, "tir-chonaill")).isFalse();
        verify(inventoryService, never()).acquireItem(anyLong(), any(), anyInt());
        verify(actionLog).add(eq("[채집] 💨 헛도끼질을 하여 장작을 얻지 못했습니다."), eq("system"));
    }
}
