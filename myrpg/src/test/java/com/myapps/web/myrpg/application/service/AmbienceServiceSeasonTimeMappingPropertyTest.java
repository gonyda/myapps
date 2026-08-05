package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.AmbienceData;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 계절/시간대 매핑 프로퍼티 테스트.
 *
 * <p>월(1~12)에 대해 {@code resolveSeason}이 ambience.json의 season 정의에 부합하는
 * 계절 키를 산출하고, 시(0~23)에 대해 {@code resolveTimeOfDay}가 자정을 넘는
 * 구간(late-night)을 포함하여 timeOfDay 정의에 부합하는 시간대 키를 산출하는지 검증한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 16: 계절/시간대 매핑
 *
 * <p><b>Validates: Requirements 7.1</b>
 */
class AmbienceServiceSeasonTimeMappingPropertyTest {

    private final AmbienceService ambienceService;
    private final AmbienceData ambienceData;

    AmbienceServiceSeasonTimeMappingPropertyTest() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final Clock clock = Clock.systemDefaultZone();
        final Random random = new Random(0L);
        this.ambienceService = new AmbienceService(objectMapper, clock, random);
        this.ambienceService.init();
        this.ambienceData = this.ambienceService.ambienceData();
    }

    /**
     * 월(1~12)에 대해 resolveSeason이 반환하는 계절 키가
     * ambience.json의 season 매핑에서 해당 월을 포함하는 키와 일치하는지 검증한다.
     *
     * @param month 임의의 월 값 (1~12)
     */
    @Property(tries = 100)
    void should_returnMatchingSeason_when_monthIsValid(
            @ForAll("validMonths") final int month) {
        // When
        final String resolvedSeason = ambienceService.resolveSeason(month);

        // Then: 반환된 season 키가 ambience.json에 정의되어 있음
        assertThat(ambienceData.season()).containsKey(resolvedSeason);

        // Then: 해당 season 키의 월 목록에 입력 월이 포함됨
        final List<Integer> monthsInSeason = ambienceData.season().get(resolvedSeason);
        assertThat(monthsInSeason).contains(month);
    }

    /**
     * 시(0~23)에 대해 resolveTimeOfDay가 반환하는 시간대 키가
     * ambience.json의 timeOfDay 매핑에서 해당 시각이 [from, to) 범위에 속하는
     * 키와 일치하는지 검증한다. late-night(0~4시) 구간을 포함한다.
     *
     * @param hour 임의의 시각 값 (0~23)
     */
    @Property(tries = 100)
    void should_returnMatchingTimeOfDay_when_hourIsValid(
            @ForAll("validHours") final int hour) {
        // When
        final String resolvedTimeOfDay = ambienceService.resolveTimeOfDay(hour);

        // Then: 반환된 timeOfDay 키가 ambience.json에 정의되어 있음
        assertThat(ambienceData.timeOfDay()).containsKey(resolvedTimeOfDay);

        // Then: 해당 timeOfDay 키의 범위에 입력 시각이 포함됨 (from <= hour < to)
        final AmbienceData.TimeBucket bucket = ambienceData.timeOfDay().get(resolvedTimeOfDay);
        assertThat(hour)
                .isGreaterThanOrEqualTo(bucket.from())
                .isLessThan(bucket.to());
    }

    /**
     * late-night 구간(시각 0~4)에 대해 resolveTimeOfDay가
     * 정확히 "late-night" 키를 반환하는지 검증한다.
     *
     * @param hour late-night 구간의 시각 값 (0~4)
     */
    @Property(tries = 100)
    void should_returnLateNight_when_hourIsInLateNightRange(
            @ForAll("lateNightHours") final int hour) {
        // When
        final String resolvedTimeOfDay = ambienceService.resolveTimeOfDay(hour);

        // Then: late-night 구간의 시각은 반드시 "late-night"으로 매핑됨
        assertThat(resolvedTimeOfDay).isEqualTo("late-night");

        // Then: ambience.json의 late-night 정의에 부합
        final AmbienceData.TimeBucket lateNightBucket = ambienceData.timeOfDay().get("late-night");
        assertThat(hour)
                .isGreaterThanOrEqualTo(lateNightBucket.from())
                .isLessThan(lateNightBucket.to());
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
     * late-night 구간의 시각 값(0~4)을 생성하는 Arbitrary 제공자.
     *
     * @return 0~4 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> lateNightHours() {
        return Arbitraries.integers().between(0, 4);
    }
}
