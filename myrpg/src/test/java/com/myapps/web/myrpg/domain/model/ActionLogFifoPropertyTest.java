package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 행동 로그 최대 10개 유지(FIFO) 프로퍼티를 검증하는 프로퍼티 테스트.
 *
 * <p>N개 항목을 추가한 후 로그 크기가 {@code min(N, 10)}이며, 보존되는 항목이 가장 최근에 추가된 10개(FIFO 방식으로 오래된 항목 제거)인지 검증한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 20: 행동 로그 최대 10개 유지(FIFO)
 *
 * <p><b>Validates: Requirements 9.2</b>
 */
class ActionLogFifoPropertyTest {

    private static final int MAX_ENTRIES = 10;
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final long BASE_EPOCH_SECOND =
            Instant.parse("2025-01-01T00:00:00Z").getEpochSecond();

    /**
     * N개 항목 추가 후 로그 크기는 {@code min(N, 10)}이고, 보존된 항목은 마지막 10개(가장 최근 추가된 항목)인지 검증한다.
     *
     * @param entryCount 추가할 항목 수 (1~50)
     */
    @Property(tries = 100)
    void should_maintainMaxTenEntries_and_preserveMostRecent_when_nEntriesAdded(
            @ForAll("entryCounts") final int entryCount) {
        // Given: 고정 Clock으로 ActionLog 생성
        final Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(BASE_EPOCH_SECOND), ZONE_ID);
        final ActionLog actionLog = new ActionLog(fixedClock);

        // When: N개 항목 추가 (각 항목에 고유 메시지 부여)
        for (int i = 1; i <= entryCount; i++) {
            actionLog.add("메시지-" + i, "move");
        }

        // Then: 크기는 min(N, 10)
        final int expectedSize = Math.min(entryCount, MAX_ENTRIES);
        assertThat(actionLog.size()).isEqualTo(expectedSize);

        // Then: 보존된 항목은 가장 최근 추가된 min(N, 10)개
        final List<ActionLogEntry> entries = actionLog.getEntries();
        assertThat(entries).hasSize(expectedSize);

        // 마지막 min(N, 10)개의 메시지를 검증 (FIFO: 오래된 것 제거, 최근 것 보존)
        final int firstPreservedIndex = entryCount - expectedSize + 1;
        for (int i = 0; i < expectedSize; i++) {
            final int expectedMessageIndex = firstPreservedIndex + i;
            assertThat(entries.get(i).message()).isEqualTo("메시지-" + expectedMessageIndex);
        }
    }

    /**
     * 정확히 10개 추가 시 모든 항목이 보존되는지 검증한다.
     *
     * @param dummy 테스트 반복을 위한 임의 값 (사용하지 않음)
     */
    @Property(tries = 100)
    void should_preserveAllEntries_when_exactlyTenAdded(@ForAll("epochSeconds") final long dummy) {
        // Given: 고정 Clock으로 ActionLog 생성
        final Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(dummy), ZONE_ID);
        final ActionLog actionLog = new ActionLog(fixedClock);

        // When: 정확히 10개 추가
        for (int i = 1; i <= MAX_ENTRIES; i++) {
            actionLog.add("항목-" + i, "move");
        }

        // Then: 크기는 10, 모든 항목 보존
        assertThat(actionLog.size()).isEqualTo(MAX_ENTRIES);

        final List<ActionLogEntry> entries = actionLog.getEntries();
        for (int i = 0; i < MAX_ENTRIES; i++) {
            assertThat(entries.get(i).message()).isEqualTo("항목-" + (i + 1));
        }
    }

    /**
     * 항목이 삽입 순서(오름차순)대로 정렬되어 있는지 검증한다. FIFO 제거 후에도 남은 항목의 순서가 추가 순서와 일치해야 한다.
     *
     * @param entryCount 추가할 항목 수 (1~50)
     */
    @Property(tries = 100)
    void should_preserveInsertionOrder_when_entriesAreRetained(
            @ForAll("entryCounts") final int entryCount) {
        // Given: 각 항목마다 1초씩 증가하는 Clock 시뮬레이션을 위해 고정 Clock 사용
        final Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(BASE_EPOCH_SECOND), ZONE_ID);
        final ActionLog actionLog = new ActionLog(fixedClock);

        // When: N개 항목 추가
        for (int i = 1; i <= entryCount; i++) {
            actionLog.add("순서-" + i, "move");
        }

        // Then: 반환된 목록의 메시지가 오름차순(추가 순서)으로 정렬
        final List<ActionLogEntry> entries = actionLog.getEntries();
        final int expectedSize = Math.min(entryCount, MAX_ENTRIES);
        assertThat(entries).hasSize(expectedSize);

        for (int i = 1; i < entries.size(); i++) {
            final String previousMessage = entries.get(i - 1).message();
            final String currentMessage = entries.get(i).message();
            final int previousIndex = extractIndex(previousMessage);
            final int currentIndex = extractIndex(currentMessage);
            assertThat(currentIndex).isGreaterThan(previousIndex);
        }
    }

    /**
     * "순서-{N}" 형식의 메시지에서 숫자 인덱스를 추출한다.
     *
     * @param message 메시지 문자열
     * @return 추출된 숫자 인덱스
     */
    private int extractIndex(final String message) {
        final String numberPart = message.substring(message.lastIndexOf('-') + 1);
        return Integer.parseInt(numberPart);
    }

    /**
     * 추가할 항목 수를 생성하는 Arbitrary 제공자.
     *
     * @return 1~50 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> entryCounts() {
        return Arbitraries.integers().between(1, 50);
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
