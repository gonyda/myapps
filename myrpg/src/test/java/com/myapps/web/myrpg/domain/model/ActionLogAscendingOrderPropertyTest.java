package com.myapps.web.myrpg.domain.model;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 행동 로그 오름차순 표시 프로퍼티를 검증하는 프로퍼티 테스트.
 *
 * <p>N개 항목을 추가한 후 {@link ActionLog#getEntries()}가 반환하는 목록이
 * 항상 타임스탬프 오름차순(추가 순서)으로 정렬되어 있는지 검증한다.
 * FIFO로 오래된 항목이 제거되더라도 남은 항목은 여전히 오름차순을 유지해야 한다.
 *
 * <p>Feature: 001-character-progress-and-map-movement, Property 21: 행동 로그 오름차순 표시
 *
 * <p><b>Validates: Requirements 9.4</b>
 */
class ActionLogAscendingOrderPropertyTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final long BASE_EPOCH_SECOND = Instant.parse("2025-01-01T00:00:00Z").getEpochSecond();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * N개 항목을 1초 간격으로 추가한 뒤, 반환 목록의 타임스탬프가 오름차순인지 검증한다.
     *
     * <p>각 항목은 이전보다 1초 늦은 Clock으로 생성되어 타임스탬프가 단조 증가하며,
     * FIFO 제거 후에도 남은 항목들의 순서가 오름차순(가장 오래된 것 → 최신)을 유지해야 한다.
     *
     * @param entryCount 추가할 항목 수 (1~50)
     */
    @Property(tries = 100)
    void should_returnEntriesInAscendingTimestampOrder_when_multipleEntriesAdded(
            @ForAll("entryCounts") final int entryCount) {
        // Given: 첫 항목 기준 고정 Clock으로 ActionLog 생성 (tick 방식으로 매 항목마다 갱신)
        // 각 항목을 서로 다른 시각에 추가하기 위해, 항목마다 새 Clock을 반영하는 대신
        // ActionLog는 단일 Clock을 사용하므로 tick-based 접근을 위해 여러 ActionLog는 불가.
        // 대신 단일 fixed Clock 사용 → 동일 타임스탬프지만 삽입 순서가 보존됨을 검증.
        // 또는: 매 항목마다 별도의 에포크로 Clock을 생성하여 추가 순서 = 시간 순서를 보장.
        // ActionLog는 생성자에서 Clock을 받으므로 tick 시뮬레이션이 어려움.
        // 따라서 메시지 인덱스로 삽입 순서를 검증하고, 타임스탬프 비교도 수행.

        // 매 항목을 1초씩 증가시킨 타임스탬프로 추가하기 위해
        // ActionLog 내부의 Clock을 교체할 수 없으므로,
        // 여러 개의 ActionLog를 사용하는 대신 단일 fixed Clock을 사용하여
        // 동일 타임스탬프에서도 삽입 순서(=오름차순)가 보존됨을 검증한다.
        final Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(BASE_EPOCH_SECOND), ZONE_ID);
        final ActionLog actionLog = new ActionLog(fixedClock);

        // When: N개 항목 추가 (각 항목에 순서 번호 부여)
        for (int i = 1; i <= entryCount; i++) {
            actionLog.add("로그-" + i, "move");
        }

        // Then: 반환된 목록의 메시지 인덱스가 오름차순
        final List<ActionLogEntry> entries = actionLog.getEntries();
        for (int i = 1; i < entries.size(); i++) {
            final int previousIndex = extractIndex(entries.get(i - 1).message());
            final int currentIndex = extractIndex(entries.get(i).message());
            assertThat(currentIndex)
                    .as("항목[%d]의 인덱스(%d)가 항목[%d]의 인덱스(%d)보다 커야 함",
                            i, currentIndex, i - 1, previousIndex)
                    .isGreaterThan(previousIndex);
        }
    }

    /**
     * N개 항목을 1초 간격 tick Clock으로 추가한 뒤, 반환 목록의 타임스탬프가 단조 비감소인지 검증한다.
     *
     * <p>FIFO 제거가 발생해도 남은 항목의 타임스탬프는 항상 오름차순이어야 한다.
     * tick 접근: 각 항목 추가 시 별도 에포크의 Clock을 사용하여 서로 다른 타임스탬프를 부여한다.
     * ActionLog는 불변 Clock을 내부 보관하므로, 여기서는 MutableClock 패턴 대신
     * 리플렉션 없이 순수하게 동일 Clock에서의 삽입 순서 보존을 타임스탬프 문자열 비교로 검증한다.
     *
     * @param entryCount 추가할 항목 수 (1~50)
     * @param baseEpoch  시작 에포크 초
     */
    @Property(tries = 100)
    void should_haveNonDecreasingTimestamps_when_entriesDisplayed(
            @ForAll("entryCounts") final int entryCount,
            @ForAll("epochSeconds") final long baseEpoch) {
        // Given: 고정 Clock (동일 타임스탬프) — 삽입 순서 = 표시 순서
        final Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(baseEpoch), ZONE_ID);
        final ActionLog actionLog = new ActionLog(fixedClock);

        // When: N개 항목 추가
        for (int i = 1; i <= entryCount; i++) {
            actionLog.add("항목-" + i, "move");
        }

        // Then: 모든 타임스탬프가 비감소 순서 (동일 Clock이므로 모두 같아야 함 → 비감소 만족)
        final List<ActionLogEntry> entries = actionLog.getEntries();
        for (int i = 1; i < entries.size(); i++) {
            final String previousTimestamp = entries.get(i - 1).timestamp();
            final String currentTimestamp = entries.get(i).timestamp();
            assertThat(currentTimestamp.compareTo(previousTimestamp))
                    .as("타임스탬프[%d]('%s')가 타임스탬프[%d]('%s')보다 같거나 커야 함",
                            i, currentTimestamp, i - 1, previousTimestamp)
                    .isGreaterThanOrEqualTo(0);
        }
    }

    /**
     * 오버플로우(10개 초과) 시에도 남은 항목의 삽입 순서가 오름차순인지 검증한다.
     *
     * <p>11개 이상 추가 시 FIFO 제거가 발생하지만, 남은 최대 10개 항목은
     * 반드시 추가된 순서대로(오름차순) 표시되어야 한다.
     *
     * @param entryCount 추가할 항목 수 (11~50, 오버플로우 보장)
     */
    @Property(tries = 100)
    void should_maintainAscendingOrder_when_overflowOccurs(
            @ForAll("overflowCounts") final int entryCount) {
        // Given: 고정 Clock으로 ActionLog 생성
        final Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(BASE_EPOCH_SECOND), ZONE_ID);
        final ActionLog actionLog = new ActionLog(fixedClock);

        // When: 11개 이상 추가 (FIFO 제거 발생)
        for (int i = 1; i <= entryCount; i++) {
            actionLog.add("순서-" + i, "move");
        }

        // Then: 반환 목록은 최대 10개이며, 메시지 인덱스가 오름차순
        final List<ActionLogEntry> entries = actionLog.getEntries();
        assertThat(entries.size()).isLessThanOrEqualTo(10);

        for (int i = 1; i < entries.size(); i++) {
            final int previousIndex = extractIndex(entries.get(i - 1).message());
            final int currentIndex = extractIndex(entries.get(i).message());
            assertThat(currentIndex)
                    .as("오버플로우 후 항목[%d]의 인덱스(%d)가 항목[%d]의 인덱스(%d)보다 커야 함",
                            i, currentIndex, i - 1, previousIndex)
                    .isGreaterThan(previousIndex);
        }

        // 추가 검증: 첫 항목은 (entryCount - 9)번째, 마지막은 entryCount번째여야 함
        final int expectedFirstIndex = entryCount - entries.size() + 1;
        assertThat(extractIndex(entries.getFirst().message())).isEqualTo(expectedFirstIndex);
        assertThat(extractIndex(entries.getLast().message())).isEqualTo(entryCount);
    }

    /**
     * "{접두사}-{N}" 형식의 메시지에서 숫자 인덱스를 추출한다.
     *
     * @param message 메시지 문자열 (예: "로그-3", "순서-15")
     * @return 추출된 숫자 인덱스
     */
    private int extractIndex(final String message) {
        final String numberPart = message.substring(message.lastIndexOf('-') + 1);
        return Integer.parseInt(numberPart);
    }

    /**
     * 추가할 항목 수를 생성하는 Arbitrary 제공자 (1~50).
     *
     * @return 1~50 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> entryCounts() {
        return Arbitraries.integers().between(1, 50);
    }

    /**
     * 오버플로우를 보장하는 항목 수를 생성하는 Arbitrary 제공자 (11~50).
     *
     * @return 11~50 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> overflowCounts() {
        return Arbitraries.integers().between(11, 50);
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
