package com.myapps.web.myrpg.application.service;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.AmbienceData;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Theme 결정 규칙 프로퍼티 테스트.
 *
 * <p>노드에 {@code theme} 값이 있으면 그 값을, 없으면 노드의 {@code type} 값을
 * 상황 멘트 후보 검색 키로 사용하는지 검증한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 18: Theme 결정 규칙
 *
 * <p><b>Validates: Requirements 7.5</b>
 */
class AmbienceServiceThemeDeterminationPropertyTest {

    private static final List<String> THEME_CANDIDATES = List.of("테마 멘트 A", "테마 멘트 B", "테마 멘트 C");
    private static final List<String> TYPE_CANDIDATES = List.of("타입 멘트 X", "타입 멘트 Y", "타입 멘트 Z");
    private static final String THEME_KEY = "custom-theme";
    private static final String TYPE_KEY = "custom-type";
    private static final String FIXED_SEASON = "spring";
    private static final String FIXED_TOD = "morning";
    private static final int FIXED_MONTH = 3;
    private static final int FIXED_HOUR = 9;

    private final AmbienceData realAmbienceData;

    AmbienceServiceThemeDeterminationPropertyTest() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final Clock clock = Clock.systemDefaultZone();
        final Random random = new Random(0L);
        final AmbienceService initService = new AmbienceService(objectMapper, clock, random);
        initService.init();
        this.realAmbienceData = initService.ambienceData();
    }

    /**
     * 노드에 비어 있지 않은 {@code theme} 값이 설정되어 있으면,
     * 서비스는 해당 theme 키의 후보 목록에서 멘트를 선택한다.
     *
     * <p>커스텀 AmbienceData에 theme 키와 type 키를 서로 다른 후보 목록으로 배치하고,
     * 노드의 theme이 설정된 경우 theme 키의 후보가 반환되는지 검증한다.
     *
     * @param themeSuffix theme 값의 접미사 (비어 있지 않은 문자열)
     */
    @Property(tries = 100)
    void should_useThemeValue_when_nodeHasNonBlankTheme(
            @ForAll("nonBlankSuffixes") final String themeSuffix) {

        final String themeValue = THEME_KEY + themeSuffix;
        final AmbienceData customData = createCustomDataWithDistinctPools(themeValue, TYPE_KEY);

        final Clock fixedClock = createFixedClock(FIXED_MONTH, FIXED_HOUR);
        final Random fixedRandom = new Random(42L);
        final AmbienceService service = createServiceWithCustomData(fixedClock, fixedRandom, customData);

        final MapNode node = new MapNode(
                "test-node", "테스트마을", TYPE_KEY, NodeType.TOWN,
                0, 0, null, themeValue, List.of());

        final String result = service.ambience(node);

        assertThat(result)
                .as("theme이 '%s'이면 theme 키의 후보에서 선택해야 함", themeValue)
                .isIn(THEME_CANDIDATES);
    }

    /**
     * 노드의 {@code theme} 값이 null이면,
     * 서비스는 노드의 {@code type} 키의 후보 목록에서 멘트를 선택한다.
     *
     * @param month 임의의 월 값 (1~12, 시드 다양성 확보용)
     * @param hour  임의의 시각 값 (0~23, 시드 다양성 확보용)
     */
    @Property(tries = 100)
    void should_useTypeValue_when_themeIsNull(
            @ForAll("validMonths") final int month,
            @ForAll("validHours") final int hour) {

        final AmbienceData customData = createAllSeasonTodData(THEME_KEY, TYPE_KEY);

        final Clock fixedClock = createFixedClock(month, hour);
        final Random fixedRandom = new Random(42L);
        final AmbienceService service = createServiceWithCustomData(fixedClock, fixedRandom, customData);

        final MapNode node = new MapNode(
                "test-node", "테스트마을", TYPE_KEY, NodeType.TOWN,
                0, 0, null, null, List.of());

        final String result = service.ambience(node);

        assertThat(result)
                .as("theme이 null이면 type 키('%s')의 후보에서 선택해야 함", TYPE_KEY)
                .isIn(TYPE_CANDIDATES);
    }

    /**
     * 노드의 {@code theme} 값이 공백 문자열이면,
     * 서비스는 노드의 {@code type} 키의 후보 목록에서 멘트를 선택한다.
     *
     * @param blankTheme 공백 문자열 변형
     */
    @Property(tries = 100)
    void should_useTypeValue_when_themeIsBlank(
            @ForAll("blankStrings") final String blankTheme) {

        final AmbienceData customData = createAllSeasonTodData(THEME_KEY, TYPE_KEY);

        final Clock fixedClock = createFixedClock(FIXED_MONTH, FIXED_HOUR);
        final Random fixedRandom = new Random(42L);
        final AmbienceService service = createServiceWithCustomData(fixedClock, fixedRandom, customData);

        final MapNode node = new MapNode(
                "test-node", "테스트마을", TYPE_KEY, NodeType.TOWN,
                0, 0, null, blankTheme, List.of());

        final String result = service.ambience(node);

        assertThat(result)
                .as("theme이 '%s'(공백)이면 type 키('%s')의 후보에서 선택해야 함", blankTheme, TYPE_KEY)
                .isIn(TYPE_CANDIDATES);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Arbitrary 제공자
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * 비어 있지 않은 접미사 문자열을 생성하는 Arbitrary 제공자.
     *
     * @return 빈 문자열이 아닌 접미사 Arbitrary
     */
    @Provide
    Arbitrary<String> nonBlankSuffixes() {
        return Arbitraries.of("-alpha", "-beta", "-gamma", "-delta", "-epsilon");
    }

    /**
     * 공백 문자열 변형을 생성하는 Arbitrary 제공자.
     *
     * @return 공백만 포함하는 문자열 Arbitrary
     */
    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.of("", " ", "  ", "\t", " \t ");
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
     * theme 키와 type 키에 서로 다른 후보 목록을 배치한 커스텀 AmbienceData를 생성한다.
     *
     * <p>FIXED_SEASON + FIXED_TOD 조합에만 후보를 배치한다.
     *
     * @param themeKey theme 키
     * @param typeKey  type 키
     * @return 커스텀 AmbienceData
     */
    private AmbienceData createCustomDataWithDistinctPools(
            final String themeKey, final String typeKey) {

        final Map<String, List<String>> themeSeasonMap = new HashMap<>();
        themeSeasonMap.put(FIXED_TOD, THEME_CANDIDATES);

        final Map<String, Map<String, List<String>>> themeMap = new HashMap<>();
        themeMap.put(FIXED_SEASON, themeSeasonMap);

        final Map<String, List<String>> typeSeasonMap = new HashMap<>();
        typeSeasonMap.put(FIXED_TOD, TYPE_CANDIDATES);

        final Map<String, Map<String, List<String>>> typeMap = new HashMap<>();
        typeMap.put(FIXED_SEASON, typeSeasonMap);

        final Map<String, Map<String, Map<String, List<String>>>> themes = new HashMap<>();
        themes.put(themeKey, themeMap);
        themes.put(typeKey, typeMap);

        return new AmbienceData(
                realAmbienceData.season(),
                realAmbienceData.timeOfDay(),
                themes
        );
    }

    /**
     * 모든 season × timeOfDay 조합에 후보를 배치한 커스텀 AmbienceData를 생성한다.
     *
     * <p>theme 키에는 THEME_CANDIDATES, type 키에는 TYPE_CANDIDATES를 배치하여
     * 어떤 시각이든 올바른 풀에서 선택되는지 검증할 수 있다.
     *
     * @param themeKey theme 키
     * @param typeKey  type 키
     * @return 커스텀 AmbienceData
     */
    private AmbienceData createAllSeasonTodData(final String themeKey, final String typeKey) {
        final List<String> seasons = List.of("spring", "summer", "autumn", "winter");
        final List<String> timeOfDays = List.of(
                "dawn", "morning", "afternoon", "late-afternoon", "night", "late-night");

        final Map<String, Map<String, List<String>>> themeMap = new HashMap<>();
        for (final String season : seasons) {
            final Map<String, List<String>> todMap = new HashMap<>();
            for (final String tod : timeOfDays) {
                todMap.put(tod, THEME_CANDIDATES);
            }
            themeMap.put(season, todMap);
        }

        final Map<String, Map<String, List<String>>> typeMap = new HashMap<>();
        for (final String season : seasons) {
            final Map<String, List<String>> todMap = new HashMap<>();
            for (final String tod : timeOfDays) {
                todMap.put(tod, TYPE_CANDIDATES);
            }
            typeMap.put(season, todMap);
        }

        final Map<String, Map<String, Map<String, List<String>>>> themes = new HashMap<>();
        themes.put(themeKey, themeMap);
        themes.put(typeKey, typeMap);

        return new AmbienceData(
                realAmbienceData.season(),
                realAmbienceData.timeOfDay(),
                themes
        );
    }
}
