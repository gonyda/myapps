package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 나무 스폰 격리 및 1회 채집 소멸 불변식 프로퍼티 테스트.
 *
 * <p>던전 노드는 어떤 난수에서도 항상 {@code false}이며, 유효한 채집 후에는 성공/실패 여부와 무관하게 나무가 즉시 소멸된다.
 *
 * <p>Feature: 017-firewood-gathering, Property 3: 50% 스폰 및 던전 격리 불변식, Property 4: 1회 채집 후 소멸 불변식
 *
 * <p><b>Validates: Requirements 2.1, 2.2, 2.5</b>
 */
class GatheringSpawnPropertyTest {

    @Property(tries = 100)
    void should_neverSpawnTree_inDungeonNode_regardlessOfRandomSeed(
            @ForAll("randomSeed") final double randomSeed,
            @ForAll("characterId") final long characterId) {

        final InventoryService inventoryService = mock(InventoryService.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);
        final ActionLog actionLog = mock(ActionLog.class);
        final Random random = mock(Random.class);

        when(random.nextDouble()).thenReturn(randomSeed);

        final GatheringService service =
                new GatheringService(
                        inventoryService,
                        characterProgressRepository,
                        statProgression,
                        actionLog,
                        random);

        final boolean spawned = service.rollTreeSpawn(characterId, "alby-dungeon-1", "dungeon");

        assertThat(spawned).isFalse();
        assertThat(service.isTreeAvailable(characterId, "alby-dungeon-1")).isFalse();
    }

    @Property(tries = 100)
    void should_depleteTree_afterAnyGatheringAttempt_when_staminaIsSufficient(
            @ForAll("randomSeed") final double randomSeed,
            @ForAll("characterId") final long characterId) {

        final InventoryService inventoryService = mock(InventoryService.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);
        final ActionLog actionLog = mock(ActionLog.class);
        final Random random = mock(Random.class);

        when(statProgression.vitalMaxFor(anyInt(), any(TalentType.class)))
                .thenReturn(new VitalMax(100, 100, 100));
        when(random.nextDouble()).thenReturn(randomSeed);

        final GatheringService service =
                new GatheringService(
                        inventoryService,
                        characterProgressRepository,
                        statProgression,
                        actionLog,
                        random);

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
        service.setTreeAvailable(characterId, "tir-chonaill");

        service.gatherWood(characterId, progress);

        assertThat(service.isTreeAvailable(characterId, "tir-chonaill")).isFalse();
    }

    @Provide
    Arbitrary<Double> randomSeed() {
        return Arbitraries.doubles().between(0.0, 0.99);
    }

    @Provide
    Arbitrary<Long> characterId() {
        return Arbitraries.longs().between(1L, 1000L);
    }
}
