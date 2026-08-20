package com.myapps.web.myrpg.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link ActionLog} 및 {@link ActionLogEntry}의 기본 동작을 검증하는 단위 테스트.
 *
 * <p>프로퍼티 기반 테스트(Property 19~21)는 별도 Task(9.2~9.4)에서 구현한다.
 */
class ActionLogTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2025-03-15T14:30:45Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void should_createEntry_withTimestampAndMessageAndType_when_addWithType() {
        final ActionLog actionLog = new ActionLog(FIXED_CLOCK);

        final ActionLogEntry entry = actionLog.add("티르코네일로 이동했습니다.", "move");

        assertNotNull(entry);
        assertEquals("2025-03-15 23:30:45", entry.timestamp());
        assertEquals("티르코네일로 이동했습니다.", entry.message());
        assertEquals("move", entry.type());
    }

    @Test
    void should_useDefaultTypeMoveWhen_typeIsNull() {
        final ActionLog actionLog = new ActionLog(FIXED_CLOCK);

        final ActionLogEntry entry = actionLog.add("필드로 이동했습니다.", null);

        assertEquals("move", entry.type());
    }

    @Test
    void should_useDefaultTypeMoveWhen_addWithMessageOnly() {
        final ActionLog actionLog = new ActionLog(FIXED_CLOCK);

        final ActionLogEntry entry = actionLog.add("던전 입구로 이동했습니다.");

        assertEquals("move", entry.type());
    }

    @Test
    void should_maintainMaxTenEntries_when_moreThanTenAdded() {
        final ActionLog actionLog = new ActionLog(FIXED_CLOCK);

        for (int i = 1; i <= 12; i++) {
            actionLog.add("메시지 " + i, "move");
        }

        assertEquals(10, actionLog.size());
        final List<ActionLogEntry> entries = actionLog.getEntries();
        assertEquals("메시지 3", entries.getFirst().message());
        assertEquals("메시지 12", entries.getLast().message());
    }

    @Test
    void should_returnEntriesInAscendingOrder_when_getEntries() {
        final ActionLog actionLog = new ActionLog(FIXED_CLOCK);

        actionLog.add("첫 번째", "move");
        actionLog.add("두 번째", "move");
        actionLog.add("세 번째", "move");

        final List<ActionLogEntry> entries = actionLog.getEntries();
        assertEquals(3, entries.size());
        assertEquals("첫 번째", entries.get(0).message());
        assertEquals("두 번째", entries.get(1).message());
        assertEquals("세 번째", entries.get(2).message());
    }

    @Test
    void should_returnEmptyList_when_noEntriesAdded() {
        final ActionLog actionLog = new ActionLog(FIXED_CLOCK);

        final List<ActionLogEntry> entries = actionLog.getEntries();

        assertNotNull(entries);
        assertEquals(0, entries.size());
    }
}
