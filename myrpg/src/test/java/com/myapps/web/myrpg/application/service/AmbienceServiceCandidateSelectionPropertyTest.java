package com.myapps.web.myrpg.application.service;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.AmbienceData;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 상황 멘트 유효 후보 선택 프로퍼티 테스트.
 *
 * <p>세 가지 경우에 대해 {@code ambience()} 메서드가 항상 유효한 후보를 반환하는지 검증한다:
 * <ul>
 *   <li>(a) 후보 존재 시 해당 목록({@code themes[theme][season][tod]})의 원소</li>
 *   <li>(b) 정확한 매치가 없을 때 동일 theme 내 폴백 목록의 원소</li>
 *   <li>(c) theme 후보가 전무할 때 정확히 기본 문구 {@code "{맵이름} 주변을 둘러봅니다."}</li>
 * </ul>
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 17: 상황 멘트 선택은 항상 유효 후보
 *
 * <p><b>Validates: Requirements 7.2, 7.3, 7.4</b>
 */
class AmbienceServiceCandidateSelectionPropertyTest {

    private static final List<String> KNOWN_THEMES = List.of("town", "field", "dungeon");
    private static final List<String> SEASONS = List.of("spring", "summer", "autumn", "winter");
    private static final List<String> TIME_OF_DAYS = List.of(
            "dawn", "morning", "afternoon", "late-afternoon", "night", "late-night");

    private final AmbienceData realAmbienceData;

    AmbienceServiceCandidateSelectionPropertyTest() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final Clock clock = Clock.systemDefaultZone();
        final Random random = new Random(0L);
        final AmbienceService initService = new AmbienceService(objectMapper, clock, random);
        initService.init();
        this.realAmbienceData = initService.ambienceData();
    }

    /**
     * (a) 후보 존재 시 결과는 반드시 {@code themes[theme][season][tod]} 목록의 원소이다.
     *
     * <p>실제 ambience.json의 모든 theme × season × timeOfDay 조합은 비어 있지 않으므로,
     * 해당 조합에 대응하는 시각을 고정하고 결과가 정확한 매치 목록에 포함되는지 검증한다.
     *
     * @param theme 임의의 알려진 테마 ("town", "field", "dungeon")
     * @param month 임의의 월 값 (1~12)
     * @param hour  임의의 시각 값 (0~23)
     */
    @Property(tries = 100)
    void should_returnExactMatchCandidate_when_candidatesExist(
            @ForAll("knownThemes") final String theme,
            @ForAll("validMonths") final int month,
            @ForAll("validHours") final int hour) {

        final Clock fixedClock = createFixedClock(month, hour);
        final Random fixedRandom = new Random(42L);
        final AmbienceService service = createServiceWithRealData(fixedClock, fixedRandom);

        final MapNode node = createNodeWithTheme("테스트마을", theme);
        final String result = service.ambience(node);

        final String season = service.resolveSeason(month);
        final String timeOfDay = service.resolveTimeOfDay(hour);
        final List<String> exactCandidates = realAmbienceData.themes()
                .get(theme).get(season).get(timeOfDay);

        assertThat(exactCandidates)
                .as("정확한 매치 후보 목록이 비어 있지 않아야 함 (theme=%s, season=%s, tod=%s)",
                        theme, season, timeOfDay)
                .isNotEmpty();
        assertThat(result)
                .as("결과는 themes[%s][%s][%s] 목록의 원소여야 함", theme, season, timeOfDay)
                .isIn(exactCandidates);
    }

    /**
     * (b) 정확한 매치가 비어 있지만 동일 theme 내 다른 season/tod에 후보가 존재하면,
     * 결과는 폴백 풀(동일 theme의 모든 멘트)의 원소이다.
     *
     * <p>커스텀 AmbienceData를 주입하여 특정 season+tod 조합을 비워두고,
     * 동일 theme의 다른 조합에만 후보를 배치하여 폴백 동작을 검증한다.
     *
     * @param targetSeason 정확한 매치를 비울 시즌
     * @param targetTod    정확한 매치를 비울 시간대
     */
    @Property(tries = 100)
    void should_returnFallbackCandidate_when_exactMatchIsEmpty(
            @ForAll("seasons") final String targetSeason,
            @ForAll("timeOfDays") final String targetTod) {

        final List<String> fallbackMessages = List.of("폴백 멘트 A", "폴백 멘트 B", "폴백 멘트 C");
        final String otherSeason = SEASONS.stream()
                .filter(s -> !s.equals(targetSeason))
                .findFirst()
                .orElse("summer");
        final String otherTod = TIME_OF_DAYS.stream()
                .filter(t -> !t.equals(targetTod))
                .findFirst()
                .orElse("morning");

        final AmbienceData customData = createCustomAmbienceDataWithGap(
                "custom-theme", targetSeason, targetTod, otherSeason, otherTod, fallbackMessages);

        final int month = resolveMonthForSeason(targetSeason, customData);
        final int hour = resolveHourForTimeOfDay(targetTod, customData);
        final Clock fixedClock = createFixedClock(month, hour);
        final Random fixedRandom = new Random(42L);
        final AmbienceService service = createServiceWithCustomData(fixedClock, fixedRandom, customData);

        final MapNode node = createNodeWithTheme("갭테스트", "custom-theme");
        final String result = service.ambience(node);

        assertThat(result)
                .as("폴백 풀의 원소여야 함 (targetSeason=%s, targetTod=%s)", targetSeason, targetTod)
                .isIn(fallbackMessages);
    }

    /**
     * (c) theme에 해당하는 후보가 전혀 없으면 정확히 기본 문구를 반환한다.
     *
     * <p>알려지지 않은 theme을 가진 노드를 전달하여 기본 문구가 반환되는지 검증한다.
     *
     * @param mapName 임의의 맵 이름
     * @param month   임의의 월 값 (1~12)
     * @param hour    임의의 시각 값 (0~23)
     */
    @Property(tries = 100)
    void should_returnDefaultMessage_when_themeHasNoCandidates(
            @ForAll("mapNames") final String mapName,
            @ForAll("validMonths") final int month,
            @ForAll("validHours") final int hour) {

        final Clock fixedClock = createFixedClock(month, hour);
        final Random fixedRandom = new Random(42L);
        final AmbienceService service = createServiceWithRealData(fixedClock, fixedRandom);

        final MapNode node = createNodeWithTheme(mapName, "unknown-theme-xyz");
        final String result = service.ambience(node);

        final String expectedDefault = mapName + " 주변을 둘러봅니다.";
        assertThat(result)
                .as("theme 후보 전무 시 기본 문구를 반환해야 함")
                .isEqualTo(expectedDefault);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Arbitrary 제공자
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * 알려진 테마 값을 생성하는 Arbitrary 제공자.
     *
     * @return "town", "field", "dungeon" 중 하나
     */
    @Provide
    Arbitrary<String> knownThemes() {
        return Arbitraries.of(KNOWN_THEMES);
    }

    /**
     * 유효한 월 값(1~12)을 생성하는 Arbitrary 제공자.
     *
     * @return 1~12 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> validMonths() {
        return Arbitraries.integers().between(1, 12);
    }

    /**
     * 유효한 시각 값(0~23)을 생성하는 Arbitrary 제공자.
     *
     * @return 0~23 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> validHours() {
        return Arbitraries.integers().between(0, 23);
    }

    /**
     * 시즌 값을 생성하는 Arbitrary 제공자.
     *
     * @return SEASONS 목록 중 하나
     */
    @Provide
    Arbitrary<String> seasons() {
        return Arbitraries.of(SEASONS);
    }

    /**
     * 시간대 값을 생성하는 Arbitrary 제공자.
     *
     * @return TIME_OF_DAYS 목록 중 하나
     */
    @Provide
    Arbitrary<String> timeOfDays() {
        return Arbitraries.of(TIME_OF_DAYS);
    }

    /**
     * 맵 이름을 생성하는 Arbitrary 제공자.
     *
     * @return 다양한 맵 이름 문자열
     */
    @Provide
    Arbitrary<String> mapNames() {
        return Arbitraries.of("초원마을", "바람의 언덕", "던전 입구", "숲속 오두막", "항구도시");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 헬퍼 메서드
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * 지정된 월과 시각으로 고정된 Clock을 생성한다.
     *
     * @param month 월 (1~12)
     * @param hour  시각 (0~23)
     * @return 고정된 Clock 인스턴스
     */
    private Clock createFixedClock(final int month, final int hour) {
        final LocalDateTime fixedTime = LocalDateTime.of(2025, month, 15, hour, 30, 0);
        final Instant instant = fixedTime.toInstant(ZoneOffset.UTC);
        return Clock.fixed(instant, ZoneId.of("UTC"));
    }

    /**
     * 실제 ambience.json 데이터를 사용하는 AmbienceService를 생성한다.
     *
     * @param clock  고정된 Clock
     * @param random 시드 고정 Random
     * @return AmbienceService 인스턴스
     */
    private AmbienceService createServiceWithRealData(final Clock clock, final Random random) {
        final ObjectMapper objectMapper = new ObjectMapper();
        final AmbienceService service = new AmbienceService(objectMapper, clock, random);
        service.init();
        return service;
    }

    /**
     * 커스텀 AmbienceData를 주입한 AmbienceService를 생성한다.
     *
     * @param clock      고정된 Clock
     * @param random     시드 고정 Random
     * @param customData 주입할 커스텀 AmbienceData
     * @return AmbienceService 인스턴스
     */
    private AmbienceService createServiceWithCustomData(
            final Clock clock, final Random random, final AmbienceData customData) {
        final ObjectMapper objectMapper = new ObjectMapper();
        final AmbienceService service = new AmbienceService(objectMapper, clock, random);
        service.init();
        setAmbienceDataViaReflection(service, customData);
        return service;
    }

    /**
     * 리플렉션을 통해 AmbienceService의 ambienceData 필드를 교체한다.
     *
     * @param service    대상 서비스
     * @param customData 주입할 데이터
     */
    private void setAmbienceDataViaReflection(
            final AmbienceService service, final AmbienceData customData) {
        try {
            final Field field = AmbienceService.class.getDeclaredField("ambienceData");
            field.setAccessible(true);
            field.set(service, customData);
        } catch (final NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException("AmbienceData 리플렉션 주입 실패", exception);
        }
    }

    /**
     * 특정 theme에서 targetSeason+targetTod 조합이 비어 있는 커스텀 AmbienceData를 생성한다.
     *
     * <p>otherSeason+otherTod 조합에만 fallbackMessages를 배치하여 폴백 경로를 테스트한다.
     *
     * @param theme            테마 키
     * @param targetSeason     비워둘 시즌
     * @param targetTod        비워둘 시간대
     * @param otherSeason      후보를 배치할 시즌
     * @param otherTod         후보를 배치할 시간대
     * @param fallbackMessages 폴백 후보 목록
     * @return 커스텀 AmbienceData
     */
    private AmbienceData createCustomAmbienceDataWithGap(
            final String theme,
            final String targetSeason,
            final String targetTod,
            final String otherSeason,
            final String otherTod,
            final List<String> fallbackMessages) {

        final Map<String, List<String>> targetSeasonMap = new HashMap<>();
        targetSeasonMap.put(targetTod, List.of());

        final Map<String, List<String>> otherSeasonMap = new HashMap<>();
        otherSeasonMap.put(otherTod, fallbackMessages);

        final Map<String, Map<String, List<String>>> themeMap = new HashMap<>();
        themeMap.put(targetSeason, targetSeasonMap);
        if (!targetSeason.equals(otherSeason)) {
            themeMap.put(otherSeason, otherSeasonMap);
        } else {
            targetSeasonMap.put(otherTod, fallbackMessages);
        }

        final Map<String, Map<String, Map<String, List<String>>>> themes = new HashMap<>();
        themes.put(theme, themeMap);

        return new AmbienceData(
                realAmbienceData.season(),
                realAmbienceData.timeOfDay(),
                themes
        );
    }

    /**
     * 지정된 시즌에 해당하는 월 값을 반환한다.
     *
     * @param season 시즌 키
     * @param data   AmbienceData
     * @return 해당 시즌의 첫 번째 월 값
     */
    private int resolveMonthForSeason(final String season, final AmbienceData data) {
        final List<Integer> months = data.season().get(season);
        if (months != null && !months.isEmpty()) {
            return months.get(0);
        }
        return 1;
    }

    /**
     * 지정된 시간대에 해당하는 시각 값을 반환한다.
     *
     * @param timeOfDay 시간대 키
     * @param data      AmbienceData
     * @return 해당 시간대의 from 값
     */
    private int resolveHourForTimeOfDay(final String timeOfDay, final AmbienceData data) {
        final AmbienceData.TimeBucket bucket = data.timeOfDay().get(timeOfDay);
        if (bucket != null) {
            return bucket.from();
        }
        return 12;
    }

    /**
     * 지정된 이름과 테마를 가진 MapNode를 생성한다.
     *
     * @param name  노드 이름
     * @param theme 노드 테마
     * @return MapNode 인스턴스
     */
    private MapNode createNodeWithTheme(final String name, final String theme) {
        return new MapNode("test-node", name, "town", NodeType.TOWN, 0, 0, null, theme, List.of());
    }
}
