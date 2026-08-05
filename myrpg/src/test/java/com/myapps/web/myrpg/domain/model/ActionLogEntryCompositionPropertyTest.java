package com.myapps.web.myrpg.domain.model;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 행동 로그 항목 구성과 기본 타입을 검증하는 프로퍼티 테스트.
 *
 * <p>항목은 {@code yyyy-MM-dd HH:mm:ss} 형식의 타임스탬프, 메시지, 타입으로 구성되며,
 * 타입이 {@code null}이면 기본값 {@code move}가 설정되는지 검증한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 19: 행동 로그 항목 구성과 기본 타입
 *
 * <p><b>Validates: Requirements 9.1, 9.3</b>
 */
class ActionLogEntryCompositionPropertyTest {

    private static final String DEFAULT_TYPE = "move";
    private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN);
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    /**
     * 임의의 메시지와 non-null 타입으로 로그 항목을 추가하면,
     * 결과 항목의 타임스탬프가 {@code yyyy-MM-dd HH:mm:ss} 형식이고,
     * 메시지와 타입이 입력값과 동일한지 검증한다.
     *
     * @param message 임의의 로그 메시지
     * @param type    임의의 non-null 로그 타입
     * @param epochSecond 임의의 에포크 초 (결정적 Clock 생성용)
     */
    @Property(tries = 100)
    void should_haveValidTimestampAndPreserveMessageAndType_when_typeIsNotNull(
            @ForAll("messages") final String message,
            @ForAll("nonNullTypes") final String type,
            @ForAll("epochSeconds") final long epochSecond) {
        // Given: 결정적 Clock으로 ActionLog 생성
        final Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(epochSecond), ZONE_ID);
        final ActionLog actionLog = new ActionLog(fixedClock);

        // When: 메시지와 타입으로 항목 추가
        final ActionLogEntry entry = actionLog.add(message, type);

        // Then: 타임스탬프가 yyyy-MM-dd HH:mm:ss 형식
        assertThat(entry.timestamp()).isNotNull();
        assertTimestampFormat(entry.timestamp());

        // Then: 타임스탬프가 고정 Clock의 시각과 일치
        final String expectedTimestamp = LocalDateTime.now(fixedClock).format(TIMESTAMP_FORMATTER);
        assertThat(entry.timestamp()).isEqualTo(expectedTimestamp);

        // Then: 메시지 보존
        assertThat(entry.message()).isEqualTo(message);

        // Then: 타입 보존
        assertThat(entry.type()).isEqualTo(type);
    }

    /**
     * 임의의 메시지와 {@code null} 타입으로 로그 항목을 추가하면,
     * 결과 항목의 타입이 기본값 {@code move}로 설정되는지 검증한다.
     *
     * @param message 임의의 로그 메시지
     * @param epochSecond 임의의 에포크 초 (결정적 Clock 생성용)
     */
    @Property(tries = 100)
    void should_useDefaultTypeMove_when_typeIsNull(
            @ForAll("messages") final String message,
            @ForAll("epochSeconds") final long epochSecond) {
        // Given: 결정적 Clock으로 ActionLog 생성
        final Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(epochSecond), ZONE_ID);
        final ActionLog actionLog = new ActionLog(fixedClock);

        // When: 메시지와 null 타입으로 항목 추가
        final ActionLogEntry entry = actionLog.add(message, null);

        // Then: 타임스탬프가 yyyy-MM-dd HH:mm:ss 형식
        assertThat(entry.timestamp()).isNotNull();
        assertTimestampFormat(entry.timestamp());

        // Then: 메시지 보존
        assertThat(entry.message()).isEqualTo(message);

        // Then: null 타입은 기본값 "move"로 대체
        assertThat(entry.type()).isEqualTo(DEFAULT_TYPE);
    }

    /**
     * 타임스탬프 문자열이 {@code yyyy-MM-dd HH:mm:ss} 형식인지 검증한다.
     *
     * @param timestamp 검증할 타임스탬프 문자열
     */
    private void assertTimestampFormat(final String timestamp) {
        try {
            LocalDateTime.parse(timestamp, TIMESTAMP_FORMATTER);
        } catch (final DateTimeParseException e) {
            throw new AssertionError(
                    "타임스탬프가 yyyy-MM-dd HH:mm:ss 형식이 아닙니다: " + timestamp, e);
        }
    }

    /**
     * 임의의 로그 메시지를 생성하는 Arbitrary 제공자.
     *
     * @return 1~50자 범위의 문자열 Arbitrary
     */
    @Provide
    Arbitrary<String> messages() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    /**
     * non-null 로그 타입을 생성하는 Arbitrary 제공자.
     *
     * @return 1~20자 범위의 알파벳 문자열 Arbitrary
     */
    @Provide
    Arbitrary<String> nonNullTypes() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20);
    }

    /**
     * 결정적 Clock 생성을 위한 에포크 초 Arbitrary 제공자.
     *
     * @return 2020-01-01 ~ 2030-12-31 범위의 에포크 초 Arbitrary
     */
    @Provide
    Arbitrary<Long> epochSeconds() {
        final long from = Instant.parse("2020-01-01T00:00:00Z").getEpochSecond();
        final long to = Instant.parse("2030-12-31T23:59:59Z").getEpochSecond();
        return Arbitraries.longs().between(from, to);
    }
}
