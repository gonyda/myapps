package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.NpcLines;
import com.myapps.web.myrpg.domain.model.NpcType;
import com.myapps.web.myrpg.domain.model.TimeOfDay;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 대사 선택 유효성 프로퍼티 테스트.
 *
 * <p>시드 고정 {@code Random} 주입으로 (a) 풀이 비어있지 않으면 결과가 풀의 원소, (b) 풀이 비어있으면 비어있지 않은 단일 {@code
 * Personality_Fallback_Line}을 반환함을 검증한다.
 *
 * <p>Feature: 002-npc-system, Property 7: 대사 선택은 항상 유효 결과
 *
 * <p><b>Validates: Requirements 3.4, 3.5</b>
 */
class NpcDialogueSelectionPropertyTest {

    private static final int MAX_LINE_COUNT = 5;
    private static final int MAX_LINE_LENGTH = 20;

    /**
     * (a) 후보 풀이 비어있지 않을 때, {@code selectLine}의 결과는 반드시 후보 풀의 원소이다.
     *
     * <p>시드 고정 {@code Random}(mock)을 사용하여 {@code nextInt(size)}가 유효 인덱스를 반환하도록 스텁하고, 결과가 해당 인덱스의 풀
     * 원소와 정확히 일치함을 검증한다.
     *
     * @param testData 임의 생성된 테스트 데이터(비어있지 않은 후보 풀을 가진 Npc + TimeOfDay)
     */
    @Property(tries = 100)
    void should_returnElementFromPool_when_poolIsNotEmpty(
            @ForAll("npcWithNonEmptyPool") final SelectionTestData testData) {
        // Given
        final Npc npc = testData.npc();
        final TimeOfDay tod = testData.timeOfDay();
        final List<String> expectedPool = testData.expectedPool();
        final int index = testData.index();

        final Random mockRandom = mock(Random.class);
        when(mockRandom.nextInt(expectedPool.size())).thenReturn(index);

        final Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        final NpcDialogueService service = new NpcDialogueService(fixedClock, mockRandom);

        // When
        final int hour = tod.from();
        final String result = service.selectLine(npc, hour);

        // Then
        assertThat(result)
                .as("Result should be the pool element at index %d", index)
                .isEqualTo(expectedPool.get(index));
        assertThat(expectedPool).contains(result);
    }

    /**
     * (b) 후보 풀이 비어있을 때, {@code selectLine}은 비어있지 않은 단일 {@code Personality_Fallback_Line}을 반환한다.
     *
     * <p>결과가 비어있지 않고, NPC 이름을 포함하며, 동일 NPC에 대해 반복 호출 시 같은 결과(결정적)를 반환함을 검증한다.
     *
     * @param testData 임의 생성된 테스트 데이터(빈 후보 풀을 가진 Npc + TimeOfDay)
     */
    @Property(tries = 100)
    void should_returnNonEmptyFallback_when_poolIsEmpty(
            @ForAll("npcWithEmptyPool") final SelectionTestData testData) {
        // Given
        final Npc npc = testData.npc();
        final TimeOfDay tod = testData.timeOfDay();

        final Random mockRandom = mock(Random.class);
        final Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        final NpcDialogueService service = new NpcDialogueService(fixedClock, mockRandom);

        final int hour = tod.from();

        // When
        final String result = service.selectLine(npc, hour);

        // Then: 비어있지 않음
        assertThat(result).isNotEmpty();

        // Then: NPC 이름을 포함
        assertThat(result).contains(npc.name());

        // Then: 결정적 — 동일 NPC로 재호출 시 동일 결과
        final Random mockRandom2 = mock(Random.class);
        final NpcDialogueService service2 = new NpcDialogueService(fixedClock, mockRandom2);
        final String result2 = service2.selectLine(npc, hour);
        assertThat(result).isEqualTo(result2);
    }

    /**
     * 비어있지 않은 후보 풀을 가진 Npc + TimeOfDay + 유효 인덱스를 생성하는 Arbitrary 제공자.
     *
     * @return 테스트 데이터 Arbitrary
     */
    @Provide
    Arbitrary<SelectionTestData> npcWithNonEmptyPool() {
        return Combinators.combine(npcArbitrary(), timeOfDayArbitrary())
                .flatAs(
                        (npc, tod) -> {
                            // 후보 풀 계산
                            final List<String> defaultLines =
                                    npc.lines().defaultLines() != null
                                            ? npc.lines().defaultLines()
                                            : List.of();

                            final List<String> timeLines;
                            if (npc.lines().byTime() != null
                                    && npc.lines().byTime().containsKey(tod.key())) {
                                final List<String> value = npc.lines().byTime().get(tod.key());
                                timeLines = value != null ? value : List.of();
                            } else {
                                timeLines = List.of();
                            }

                            final List<String> pool;
                            if (timeLines.isEmpty()) {
                                pool = defaultLines;
                            } else if (defaultLines.isEmpty()) {
                                pool = timeLines;
                            } else {
                                final List<String> merged =
                                        new ArrayList<>(defaultLines.size() + timeLines.size());
                                merged.addAll(defaultLines);
                                merged.addAll(timeLines);
                                pool = List.copyOf(merged);
                            }

                            if (pool.isEmpty()) {
                                // 풀이 비어있으면 default에 최소 1개 대사 추가
                                return nonEmptyLineListArbitrary()
                                        .flatMap(
                                                forcedDefault -> {
                                                    final Npc fixedNpc =
                                                            new Npc(
                                                                    npc.id(),
                                                                    npc.name(),
                                                                    npc.type(),
                                                                    npc.nodeId(),
                                                                    npc.personality(),
                                                                    new NpcLines(
                                                                            forcedDefault,
                                                                            npc.lines().byTime()));
                                                    final List<String> fixedPool =
                                                            new ArrayList<>(forcedDefault);
                                                    if (npc.lines().byTime() != null
                                                            && npc.lines()
                                                                    .byTime()
                                                                    .containsKey(tod.key())) {
                                                        final List<String> tv =
                                                                npc.lines().byTime().get(tod.key());
                                                        if (tv != null && !tv.isEmpty()) {
                                                            fixedPool.addAll(tv);
                                                        }
                                                    }
                                                    return Arbitraries.integers()
                                                            .between(0, fixedPool.size() - 1)
                                                            .map(
                                                                    idx ->
                                                                            new SelectionTestData(
                                                                                    fixedNpc,
                                                                                    tod,
                                                                                    List.copyOf(
                                                                                            fixedPool),
                                                                                    idx));
                                                });
                            }

                            return Arbitraries.integers()
                                    .between(0, pool.size() - 1)
                                    .map(idx -> new SelectionTestData(npc, tod, pool, idx));
                        });
    }

    /**
     * 빈 후보 풀을 가진 Npc + TimeOfDay를 생성하는 Arbitrary 제공자.
     *
     * <p>{@code lines.default}와 {@code lines.byTime[tod]}가 모두 비어있는 경우를 보장한다.
     *
     * @return 테스트 데이터 Arbitrary
     */
    @Provide
    Arbitrary<SelectionTestData> npcWithEmptyPool() {
        return Combinators.combine(npcArbitrary(), timeOfDayArbitrary())
                .as(
                        (npc, tod) -> {
                            // byTime에서 해당 tod 키를 제거하고, defaultLines를 빈 목록으로 설정
                            final Map<String, List<String>> byTime =
                                    npc.lines().byTime() != null
                                            ? new LinkedHashMap<>(npc.lines().byTime())
                                            : new LinkedHashMap<>();
                            byTime.remove(tod.key());

                            final Npc emptyPoolNpc =
                                    new Npc(
                                            npc.id(),
                                            npc.name(),
                                            npc.type(),
                                            npc.nodeId(),
                                            npc.personality(),
                                            new NpcLines(List.of(), byTime));

                            return new SelectionTestData(emptyPoolNpc, tod, List.of(), 0);
                        });
    }

    private Arbitrary<TimeOfDay> timeOfDayArbitrary() {
        return Arbitraries.of(TimeOfDay.values());
    }

    private Arbitrary<Npc> npcArbitrary() {
        final Arbitrary<String> ids = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10);
        final Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(8);
        final Arbitrary<NpcType> types = Arbitraries.of(NpcType.values());
        final Arbitrary<String> nodeIds = Arbitraries.of("tir-chonaill", "dunbarton", "bangor");
        final Arbitrary<String> personalities =
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(15);
        final Arbitrary<NpcLines> lines = npcLinesArbitrary();

        return Combinators.combine(ids, names, types, nodeIds, personalities, lines).as(Npc::new);
    }

    private Arbitrary<NpcLines> npcLinesArbitrary() {
        final Arbitrary<List<String>> defaultLines = lineListArbitrary();
        final Arbitrary<Map<String, List<String>>> byTime = byTimeMapArbitrary();

        return Combinators.combine(defaultLines, byTime).as(NpcLines::new);
    }

    private Arbitrary<List<String>> lineListArbitrary() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(MAX_LINE_LENGTH)
                .list()
                .ofMinSize(0)
                .ofMaxSize(MAX_LINE_COUNT);
    }

    private Arbitrary<List<String>> nonEmptyLineListArbitrary() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(MAX_LINE_LENGTH)
                .list()
                .ofMinSize(1)
                .ofMaxSize(MAX_LINE_COUNT);
    }

    private Arbitrary<Map<String, List<String>>> byTimeMapArbitrary() {
        final String[] timeKeys = new String[TimeOfDay.values().length];
        for (int i = 0; i < TimeOfDay.values().length; i++) {
            timeKeys[i] = TimeOfDay.values()[i].key();
        }

        return Arbitraries.of(timeKeys)
                .set()
                .ofMinSize(0)
                .ofMaxSize(timeKeys.length)
                .flatMap(
                        keys -> {
                            if (keys.isEmpty()) {
                                return Arbitraries.just(Map.of());
                            }
                            final List<String> keyList = new ArrayList<>(keys);
                            return lineListArbitrary()
                                    .list()
                                    .ofSize(keyList.size())
                                    .map(
                                            valueLists -> {
                                                final Map<String, List<String>> map =
                                                        new LinkedHashMap<>();
                                                for (int i = 0; i < keyList.size(); i++) {
                                                    map.put(keyList.get(i), valueLists.get(i));
                                                }
                                                return Map.copyOf(map);
                                            });
                        });
    }

    /**
     * 프로퍼티 테스트용 대사 선택 테스트 데이터 레코드.
     *
     * @param npc 테스트 대상 NPC
     * @param timeOfDay 테스트 대상 시간대
     * @param expectedPool 예상 후보 풀
     * @param index 선택할 인덱스
     */
    record SelectionTestData(Npc npc, TimeOfDay timeOfDay, List<String> expectedPool, int index) {}
}
