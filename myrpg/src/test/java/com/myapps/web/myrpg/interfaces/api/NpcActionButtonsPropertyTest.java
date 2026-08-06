package com.myapps.web.myrpg.interfaces.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import org.mockito.Mockito;

import com.myapps.web.myrpg.application.dto.NpcActionButton;
import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.NpcLines;
import com.myapps.web.myrpg.domain.model.NpcType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TimeOfDay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 행동 버튼 목록 일치 프로퍼티 테스트.
 *
 * <p>임의 Npc에 대해 {@link PlayScreenViewHelper#buildPlayScreen}이 생성하는
 * {@code npcActions} 라벨 목록이 {@code NpcType.actionLabels()}와
 * 개수·순서·라벨이 정확히 동일함을 검증한다.
 *
 * <p>Feature: 002-npc-system, Property 8: 행동 버튼 목록은 타입 정의와 일치
 *
 * <p><b>Validates: Requirements 4.1, 4.2, 4.3</b>
 */
class NpcActionButtonsPropertyTest {

    private static final int MAX_LINE_COUNT = 5;
    private static final int MAX_LINE_LENGTH = 20;

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final PlayScreenViewHelper helper = new PlayScreenViewHelper(experiencePolicy, new StatProgression());

    /**
     * 임의 Npc를 talkingNpc로 전달했을 때, 반환되는 {@code npcActions} 라벨 목록이
     * 해당 NPC 타입의 {@code actionLabels()}와 개수·순서·라벨이 정확히 동일함을 검증한다.
     *
     * @param npc 임의 생성된 NPC
     */
    @Property(tries = 100)
    void should_matchNpcActionLabels_withTypeDefinition(
            @ForAll("npcs") final Npc npc) {
        // Given
        final CharacterProgress progress = mockCharacterProgress();
        final List<String> expectedLabels = npc.type().actionLabels();

        // When
        final PlayScreenView view = helper.buildPlayScreen(
                progress, null, null, null, null, npc, null, List.of());

        // Then
        final List<NpcActionButton> npcActions = view.npcActions();
        assertThat(npcActions).isNotNull();
        assertThat(npcActions).hasSize(expectedLabels.size());

        final List<String> actualLabels = npcActions.stream()
                .map(NpcActionButton::label)
                .toList();
        assertThat(actualLabels).isEqualTo(expectedLabels);
    }

    /**
     * 임의 Npc를 생성하는 Arbitrary 제공자.
     *
     * @return Npc Arbitrary
     */
    @Provide
    Arbitrary<Npc> npcs() {
        return npcArbitrary();
    }

    private CharacterProgress mockCharacterProgress() {
        final CharacterProgress progress = Mockito.mock(CharacterProgress.class);
        when(progress.getNickname()).thenReturn("테스트");
        when(progress.getCurrentLevel()).thenReturn(1);
        when(progress.getExperience()).thenReturn(0L);
        when(progress.getHpCurrent()).thenReturn(100);
        when(progress.getMpCurrent()).thenReturn(100);
        when(progress.getStaminaCurrent()).thenReturn(100);
        return progress;
    }

    private Arbitrary<Npc> npcArbitrary() {
        final Arbitrary<String> ids = Arbitraries.strings()
                .alpha().ofMinLength(3).ofMaxLength(10);
        final Arbitrary<String> names = Arbitraries.strings()
                .alpha().ofMinLength(2).ofMaxLength(8);
        final Arbitrary<NpcType> types = Arbitraries.of(NpcType.values());
        final Arbitrary<String> nodeIds = Arbitraries.of(
                "tir-chonaill", "dunbarton", "bangor");
        final Arbitrary<String> personalities = Arbitraries.strings()
                .alpha().ofMinLength(3).ofMaxLength(15);
        final Arbitrary<NpcLines> lines = npcLinesArbitrary();

        return Combinators.combine(ids, names, types, nodeIds, personalities, lines)
                .as(Npc::new);
    }

    private Arbitrary<NpcLines> npcLinesArbitrary() {
        final Arbitrary<List<String>> defaultLines = lineListArbitrary();
        final Arbitrary<Map<String, List<String>>> byTime = byTimeMapArbitrary();

        return Combinators.combine(defaultLines, byTime).as(NpcLines::new);
    }

    private Arbitrary<List<String>> lineListArbitrary() {
        return Arbitraries.strings()
                .alpha().ofMinLength(1).ofMaxLength(MAX_LINE_LENGTH)
                .list().ofMinSize(0).ofMaxSize(MAX_LINE_COUNT);
    }

    private Arbitrary<Map<String, List<String>>> byTimeMapArbitrary() {
        final String[] timeKeys = new String[TimeOfDay.values().length];
        for (int i = 0; i < TimeOfDay.values().length; i++) {
            timeKeys[i] = TimeOfDay.values()[i].key();
        }

        return Arbitraries.of(timeKeys)
                .set().ofMinSize(0).ofMaxSize(timeKeys.length)
                .flatMap(keys -> {
                    if (keys.isEmpty()) {
                        return Arbitraries.just(Map.of());
                    }
                    final List<String> keyList = new ArrayList<>(keys);
                    return lineListArbitrary()
                            .list().ofSize(keyList.size())
                            .map(valueLists -> {
                                final Map<String, List<String>> map = new LinkedHashMap<>();
                                for (int i = 0; i < keyList.size(); i++) {
                                    map.put(keyList.get(i), valueLists.get(i));
                                }
                                return Map.copyOf(map);
                            });
                });
    }
}
