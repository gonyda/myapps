package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.WoodcutResult;
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
 * 스태미나 차감 불변식 프로퍼티 테스트.
 *
 * <p>임의의 스태미나 {@code SP >= 5}에 대해 채집 시 정확히 {@code SP - 5}로 차감되며, {@code SP < 5}인 경우 채집이 거부되고 스태미나는
 * 전혀 변경되지 않는다.
 *
 * <p>Feature: 017-firewood-gathering, Property 1: 스태미나 차감 불변식
 *
 * <p><b>Validates: Requirements 3.2, 3.5</b>
 */
class GatheringStaminaPropertyTest {

    @Property(tries = 100)
    void should_decreaseExactly5Stamina_when_staminaIsAtLeast5(
            @ForAll("sufficientStamina") final int initialStamina,
            @ForAll("randomRate") final double randomRoll) {

        final InventoryService inventoryService = mock(InventoryService.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);
        final ActionLog actionLog = mock(ActionLog.class);
        final Random random = mock(Random.class);

        when(statProgression.vitalMaxFor(anyInt(), any(TalentType.class)))
                .thenReturn(new VitalMax(100, 100, 100));
        when(random.nextDouble()).thenReturn(randomRoll);

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
                        initialStamina,
                        "tir-chonaill",
                        0,
                        1000L);
        service.setTreeAvailable(1L, "tir-chonaill");

        final WoodcutResult result = service.gatherWood(progress);

        assertThat(progress.getStaminaCurrent()).isEqualTo(initialStamina - 5);
        assertThat(result.currentStamina()).isEqualTo(initialStamina - 5);
    }

    @Property(tries = 100)
    void should_notChangeStamina_when_staminaIsLessThan5(
            @ForAll("insufficientStamina") final int initialStamina) {

        final InventoryService inventoryService = mock(InventoryService.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);
        final ActionLog actionLog = mock(ActionLog.class);
        final Random random = mock(Random.class);

        when(statProgression.vitalMaxFor(anyInt(), any(TalentType.class)))
                .thenReturn(new VitalMax(100, 100, 100));

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
                        initialStamina,
                        "tir-chonaill",
                        0,
                        1000L);
        service.setTreeAvailable(1L, "tir-chonaill");

        final WoodcutResult result = service.gatherWood(progress);

        assertThat(result.success()).isFalse();
        assertThat(progress.getStaminaCurrent()).isEqualTo(initialStamina);
    }

    @Provide
    Arbitrary<Integer> sufficientStamina() {
        return Arbitraries.integers().between(5, 100);
    }

    @Provide
    Arbitrary<Integer> insufficientStamina() {
        return Arbitraries.integers().between(0, 4);
    }

    @Provide
    Arbitrary<Double> randomRate() {
        return Arbitraries.doubles().between(0.0, 0.99);
    }
}
