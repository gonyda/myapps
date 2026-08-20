package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.NodeType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link AmbienceService} 단위 테스트.
 *
 * <p>실제 {@code classpath:data/ambience.json}을 로드하여 계절/시간대 매핑, 테마 결정, 폴백 규칙을 검증한다.
 */
class AmbienceServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final long SEED = 42L;

    private AmbienceService ambienceService;

    /** 고정 시각(2024-04-15 10:30 KST = spring, morning)과 고정 시드로 서비스를 초기화한다. */
    @BeforeEach
    void setUp() {
        final Instant fixedInstant = Instant.parse("2024-04-15T01:30:00Z");
        final Clock fixedClock = Clock.fixed(fixedInstant, ZONE);
        final Random seededRandom = new Random(SEED);
        final ObjectMapper objectMapper = new ObjectMapper();

        ambienceService = new AmbienceService(objectMapper, fixedClock, seededRandom);
        ambienceService.init();
    }

    /** 4월은 spring으로 매핑됨을 검증한다. */
    @Test
    void should_returnSpring_when_monthIsApril() {
        final int april = 4;
        final String season = ambienceService.resolveSeason(april);
        assertThat(season).isEqualTo("spring");
    }

    /** 7월은 summer로 매핑됨을 검증한다. */
    @Test
    void should_returnSummer_when_monthIsJuly() {
        final int july = 7;
        final String season = ambienceService.resolveSeason(july);
        assertThat(season).isEqualTo("summer");
    }

    /** 12월은 winter로 매핑됨을 검증한다. */
    @Test
    void should_returnWinter_when_monthIsDecember() {
        final int december = 12;
        final String season = ambienceService.resolveSeason(december);
        assertThat(season).isEqualTo("winter");
    }

    /** 1월은 winter로 매핑됨을 검증한다 (자정 넘김 확인). */
    @Test
    void should_returnWinter_when_monthIsJanuary() {
        final int january = 1;
        final String season = ambienceService.resolveSeason(january);
        assertThat(season).isEqualTo("winter");
    }

    /** 시각 10시는 morning으로 매핑됨을 검증한다. */
    @Test
    void should_returnMorning_when_hourIsTen() {
        final int hour = 10;
        final String timeOfDay = ambienceService.resolveTimeOfDay(hour);
        assertThat(timeOfDay).isEqualTo("morning");
    }

    /** 시각 2시는 late-night으로 매핑됨을 검증한다. */
    @Test
    void should_returnLateNight_when_hourIsTwo() {
        final int hour = 2;
        final String timeOfDay = ambienceService.resolveTimeOfDay(hour);
        assertThat(timeOfDay).isEqualTo("late-night");
    }

    /** 시각 20시는 night으로 매핑됨을 검증한다. */
    @Test
    void should_returnNight_when_hourIsTwenty() {
        final int hour = 20;
        final String timeOfDay = ambienceService.resolveTimeOfDay(hour);
        assertThat(timeOfDay).isEqualTo("night");
    }

    /** node.theme이 있으면 해당 값을 테마로 사용함을 검증한다. */
    @Test
    void should_useNodeTheme_when_themeIsPresent() {
        final MapNode node =
                new MapNode(
                        "test-node",
                        "테스트 마을",
                        "field",
                        NodeType.FIELD,
                        0,
                        0,
                        null,
                        "town",
                        List.of());

        final String result = ambienceService.ambience(node);
        final List<String> townSpringMorning =
                ambienceService.ambienceData().themes().get("town").get("spring").get("morning");

        assertThat(townSpringMorning).contains(result);
    }

    /** node.theme이 null이면 node.type을 테마로 사용함을 검증한다. */
    @Test
    void should_useNodeType_when_themeIsNull() {
        final MapNode node =
                new MapNode(
                        "test-node",
                        "테스트 필드",
                        "field",
                        NodeType.FIELD,
                        0,
                        0,
                        null,
                        null,
                        List.of());

        final String result = ambienceService.ambience(node);
        final List<String> fieldSpringMorning =
                ambienceService.ambienceData().themes().get("field").get("spring").get("morning");

        assertThat(fieldSpringMorning).contains(result);
    }

    /** 알 수 없는 테마일 때 기본 문구를 반환함을 검증한다. */
    @Test
    void should_returnDefaultMessage_when_themeIsUnknown() {
        final MapNode node =
                new MapNode(
                        "test-node", "미지의 땅", "unknown-type", null, 0, 0, null, null, List.of());

        final String result = ambienceService.ambience(node);

        assertThat(result).isEqualTo("미지의 땅 주변을 둘러봅니다.");
    }
}
