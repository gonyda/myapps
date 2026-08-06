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

import com.myapps.web.myrpg.application.dto.InteractionItem;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.NpcLines;
import com.myapps.web.myrpg.domain.model.NpcType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TimeOfDay;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 상호작용 버튼 라벨 형식 프로퍼티 테스트.
 *
 * <p>임의 Npc에 대해 {@link PlayScreenViewHelper#buildInteractions(List)}가 생성하는
 * 라벨이 정확히 {@code name + " " + type.emoji()} 형식임을 검증한다.
 * 예: {@code "네리스 ⚒️"}.
 *
 * <p>Feature: 002-npc-system, Property 4: 상호작용 버튼 라벨 형식
 *
 * <p><b>Validates: Requirements 2.4</b>
 */
class InteractionLabelPropertyTest {

    private static final int MAX_LINE_COUNT = 5;
    private static final int MAX_LINE_LENGTH = 20;

    private final ExperiencePolicy experiencePolicy = new ExperiencePolicy();
    private final PlayScreenViewHelper helper = new PlayScreenViewHelper(experiencePolicy, new StatProgression());

    /**
     * 임의 Npc에 대해 {@code buildInteractions}로 생성된 라벨이
     * 정확히 {@code name + " (" + type.label() + ")"} 형식임을 검증한다.
     *
     * @param npc 임의 생성된 NPC
     */
    @Property(tries = 100)
    void should_formatInteractionLabel_asNameWithTypeLabel(
            @ForAll("npcs") final Npc npc) {
        // When
        final List<InteractionItem> interactions = helper.buildInteractions(List.of(npc));

        // Then
        assertThat(interactions).hasSize(1);

        final InteractionItem item = interactions.getFirst();
        final String expectedLabel = npc.name() + " " + npc.type().emoji();

        assertThat(item.name()).isEqualTo(expectedLabel);
        assertThat(item.id()).isEqualTo(npc.id());
        assertThat(item.npc()).isTrue();
    }

    /**
     * 임의 Npc 목록에 대해 모든 항목의 라벨이 올바른 형식이며
     * 입력 순서가 보존됨을 검증한다.
     *
     * @param npcList 임의 생성된 NPC 목록
     */
    @Property(tries = 100)
    void should_preserveOrderAndFormat_when_multipleNpcs(
            @ForAll("npcList") final List<Npc> npcList) {
        // When
        final List<InteractionItem> interactions = helper.buildInteractions(npcList);

        // Then
        assertThat(interactions).hasSameSizeAs(npcList);

        for (int i = 0; i < npcList.size(); i++) {
            final Npc npc = npcList.get(i);
            final InteractionItem item = interactions.get(i);
            final String expectedLabel = npc.name() + " " + npc.type().emoji();

            assertThat(item.name())
                    .as("NPC at index %d should have label '%s'", i, expectedLabel)
                    .isEqualTo(expectedLabel);
            assertThat(item.id()).isEqualTo(npc.id());
            assertThat(item.npc()).isTrue();
        }
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

    /**
     * 임의 Npc 목록을 생성하는 Arbitrary 제공자.
     *
     * @return Npc 목록 Arbitrary (0~5개)
     */
    @Provide
    Arbitrary<List<Npc>> npcList() {
        return npcArbitrary().list().ofMinSize(0).ofMaxSize(5);
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
