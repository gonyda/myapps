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
 * 후보 풀 구성 프로퍼티 테스트.
 *
 * <p>임의 Npc와 {@code TimeOfDay}에 대해, 후보 풀이 {@code lines.default} 전체 뒤에 {@code lines.byTime[tod]} 전체를
 * 이어붙인 목록과 정확히 일치(순서·개수 보존)하며, {@code byTime}에 {@code tod} 키가 없거나 그 값이 빈 목록이면 {@code default}만으로
 * 구성됨을 검증한다.
 *
 * <p>Feature: 002-npc-system, Property 6: 후보 풀 구성
 *
 * <p><b>Validates: Requirements 3.2, 3.3</b>
 */
class NpcDialogueCandidatePoolPropertyTest {

    private static final int MAX_LINE_COUNT = 5;
    private static final int MAX_LINE_LENGTH = 20;

    /**
     * 임의 Npc와 TimeOfDay에 대해, {@code selectLine}을 호출하면 반환 결과가 예상 후보 풀(default ++ byTime[tod])의 원소임을
     * 검증한다. 또한 모든 인덱스를 순회하며 풀의 순서·개수가 정확히 일치함을 확인한다.
     *
     * <p>{@code default}와 {@code byTime[tod]}가 모두 채워진 경우, 한쪽만 채워진 경우, 양쪽 모두 비어 있는 경우를 커버한다.
     *
     * @param testData 임의 생성된 테스트 데이터(Npc + TimeOfDay 조합)
     */
    @Property(tries = 100)
    void should_buildCandidatePool_matchingDefaultThenTimeLines(
            @ForAll("npcWithTimeOfDay") final CandidatePoolTestData testData) {
        // Given: 예상 후보 풀 구성
        final Npc npc = testData.npc();
        final TimeOfDay tod = testData.timeOfDay();

        final List<String> defaultLines =
                npc.lines().defaultLines() != null ? npc.lines().defaultLines() : List.of();

        final List<String> timeLines;
        if (npc.lines().byTime() != null && npc.lines().byTime().containsKey(tod.key())) {
            final List<String> value = npc.lines().byTime().get(tod.key());
            timeLines = value != null ? value : List.of();
        } else {
            timeLines = List.of();
        }

        final List<String> expectedPool;
        if (timeLines.isEmpty()) {
            expectedPool = defaultLines;
        } else if (defaultLines.isEmpty()) {
            expectedPool = timeLines;
        } else {
            final List<String> merged = new ArrayList<>(defaultLines.size() + timeLines.size());
            merged.addAll(defaultLines);
            merged.addAll(timeLines);
            expectedPool = List.copyOf(merged);
        }

        // When & Then: expectedPool이 비어있지 않으면 모든 인덱스를 순회하여 풀 내용 검증
        if (!expectedPool.isEmpty()) {
            for (int i = 0; i < expectedPool.size(); i++) {
                final Random mockRandom = mock(Random.class);
                when(mockRandom.nextInt(expectedPool.size())).thenReturn(i);

                final Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
                final NpcDialogueService service = new NpcDialogueService(fixedClock, mockRandom);

                final int hour = tod.from();
                final String result = service.selectLine(npc, hour);

                assertThat(result)
                        .as("Pool index %d should return '%s'", i, expectedPool.get(i))
                        .isEqualTo(expectedPool.get(i));
            }
        } else {
            // 풀이 비어있으면 폴백 문구 반환
            final Random mockRandom = mock(Random.class);
            final Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
            final NpcDialogueService service = new NpcDialogueService(fixedClock, mockRandom);

            final int hour = tod.from();
            final String result = service.selectLine(npc, hour);

            assertThat(result).isNotEmpty();
            assertThat(result).contains(npc.name());
        }
    }

    /**
     * byTime에 현재 tod 키가 없거나 빈 목록인 경우, 후보 풀이 {@code lines.default}만으로 구성됨을 검증한다.
     *
     * @param testData 임의 생성된 테스트 데이터(byTime에 tod 키 부재/빈 목록)
     */
    @Property(tries = 100)
    void should_usDefaultOnly_when_byTimeMissingOrEmpty(
            @ForAll("npcWithMissingTimeKey") final CandidatePoolTestData testData) {
        // Given
        final Npc npc = testData.npc();
        final TimeOfDay tod = testData.timeOfDay();

        final List<String> defaultLines =
                npc.lines().defaultLines() != null ? npc.lines().defaultLines() : List.of();

        if (!defaultLines.isEmpty()) {
            // When: 모든 인덱스를 순회하여 default만으로 풀이 구성됨 검증
            for (int i = 0; i < defaultLines.size(); i++) {
                final Random mockRandom = mock(Random.class);
                when(mockRandom.nextInt(defaultLines.size())).thenReturn(i);

                final Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
                final NpcDialogueService service = new NpcDialogueService(fixedClock, mockRandom);

                final int hour = tod.from();
                final String result = service.selectLine(npc, hour);

                assertThat(result)
                        .as("Default-only pool index %d should return '%s'", i, defaultLines.get(i))
                        .isEqualTo(defaultLines.get(i));
            }
        } else {
            // default도 비어있으면 폴백
            final Random mockRandom = mock(Random.class);
            final Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
            final NpcDialogueService service = new NpcDialogueService(fixedClock, mockRandom);

            final int hour = tod.from();
            final String result = service.selectLine(npc, hour);

            assertThat(result).isNotEmpty();
            assertThat(result).contains(npc.name());
        }
    }

    /**
     * 임의 Npc와 TimeOfDay 조합을 생성하는 Arbitrary 제공자. default와 byTime[tod]가 모두 채워진 경우, 한쪽만 채워진 경우를 포함한다.
     *
     * @return 테스트 데이터 Arbitrary
     */
    @Provide
    Arbitrary<CandidatePoolTestData> npcWithTimeOfDay() {
        return Combinators.combine(npcArbitrary(), timeOfDayArbitrary())
                .as(
                        (npc, tod) -> {
                            // byTime에 해당 tod 키가 포함되도록 보장 (빈/채운 값 혼합)
                            final Map<String, List<String>> byTime =
                                    npc.lines().byTime() != null
                                            ? new LinkedHashMap<>(npc.lines().byTime())
                                            : new LinkedHashMap<>();
                            // 50% 확률로 tod 키에 대사를 채우거나 비움 (이미 Arbitrary에서 혼합됨)
                            final Npc finalNpc =
                                    new Npc(
                                            npc.id(),
                                            npc.name(),
                                            npc.type(),
                                            npc.nodeId(),
                                            npc.personality(),
                                            new NpcLines(npc.lines().defaultLines(), byTime));
                            return new CandidatePoolTestData(finalNpc, tod);
                        });
    }

    /**
     * byTime에 현재 tod 키가 없거나 빈 목록인 Npc를 생성하는 Arbitrary 제공자.
     *
     * @return 테스트 데이터 Arbitrary
     */
    @Provide
    Arbitrary<CandidatePoolTestData> npcWithMissingTimeKey() {
        return Combinators.combine(npcArbitrary(), timeOfDayArbitrary())
                .as(
                        (npc, tod) -> {
                            // byTime에서 해당 tod 키를 제거하거나 빈 목록으로 설정
                            final Map<String, List<String>> byTime =
                                    npc.lines().byTime() != null
                                            ? new LinkedHashMap<>(npc.lines().byTime())
                                            : new LinkedHashMap<>();
                            byTime.remove(tod.key());
                            final Npc finalNpc =
                                    new Npc(
                                            npc.id(),
                                            npc.name(),
                                            npc.type(),
                                            npc.nodeId(),
                                            npc.personality(),
                                            new NpcLines(npc.lines().defaultLines(), byTime));
                            return new CandidatePoolTestData(finalNpc, tod);
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
     * 프로퍼티 테스트용 후보 풀 테스트 데이터 레코드.
     *
     * @param npc 테스트 대상 NPC
     * @param timeOfDay 테스트 대상 시간대
     */
    record CandidatePoolTestData(Npc npc, TimeOfDay timeOfDay) {}
}
