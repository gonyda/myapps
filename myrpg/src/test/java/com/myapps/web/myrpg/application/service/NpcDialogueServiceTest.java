package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.NpcLines;
import com.myapps.web.myrpg.domain.model.NpcType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * NpcDialogueService 단위 테스트.
 *
 * <p>시간대 기반 후보 풀 구성, 무작위 선택, 폴백 동작을 검증합니다.
 */
class NpcDialogueServiceTest {

    @Test
    void should_selectFromDefaultAndTimeLines_when_bothExist() {
        final Random seededRandom = new Random(42);
        final Clock fixedClock =
                Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));
        final NpcDialogueService service = new NpcDialogueService(fixedClock, seededRandom);

        final NpcLines lines =
                new NpcLines(List.of("기본1", "기본2"), Map.of("morning", List.of("오전1", "오전2")));
        final Npc npc = new Npc("test", "테스트", NpcType.CHIEF, "node1", "성격", lines);

        final String result = service.selectLine(npc, 10);

        assertThat(result).isIn("기본1", "기본2", "오전1", "오전2");
    }

    @Test
    void should_selectFromDefaultOnly_when_byTimeKeyMissing() {
        final Random seededRandom = new Random(42);
        final Clock fixedClock =
                Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));
        final NpcDialogueService service = new NpcDialogueService(fixedClock, seededRandom);

        final NpcLines lines = new NpcLines(List.of("기본1", "기본2"), Map.of("night", List.of("밤대사")));
        final Npc npc = new Npc("test", "테스트", NpcType.CHIEF, "node1", "성격", lines);

        final String result = service.selectLine(npc, 10);

        assertThat(result).isIn("기본1", "기본2");
    }

    @Test
    void should_selectFromDefaultOnly_when_byTimeIsEmpty() {
        final Random seededRandom = new Random(42);
        final Clock fixedClock =
                Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));
        final NpcDialogueService service = new NpcDialogueService(fixedClock, seededRandom);

        final NpcLines lines = new NpcLines(List.of("기본1"), Map.of("morning", List.of()));
        final Npc npc = new Npc("test", "테스트", NpcType.CHIEF, "node1", "성격", lines);

        final String result = service.selectLine(npc, 10);

        assertThat(result).isEqualTo("기본1");
    }

    @Test
    void should_returnFallback_when_poolIsEmpty() {
        final Random seededRandom = new Random(42);
        final Clock fixedClock =
                Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));
        final NpcDialogueService service = new NpcDialogueService(fixedClock, seededRandom);

        final NpcLines lines = new NpcLines(List.of(), Map.of());
        final Npc npc = new Npc("test", "던컨", NpcType.CHIEF, "node1", "성격", lines);

        final String result = service.selectLine(npc, 10);

        assertThat(result).isEqualTo("던컨은(는) 말없이 고개를 끄덕인다.");
    }

    @Test
    void should_returnFallback_when_defaultIsNullAndByTimeKeyMissing() {
        final Random seededRandom = new Random(42);
        final Clock fixedClock =
                Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));
        final NpcDialogueService service = new NpcDialogueService(fixedClock, seededRandom);

        final NpcLines lines = new NpcLines(null, null);
        final Npc npc = new Npc("test", "네리스", NpcType.BLACKSMITH, "node1", "성격", lines);

        final String result = service.selectLine(npc, 10);

        assertThat(result).isEqualTo("네리스은(는) 말없이 고개를 끄덕인다.");
    }

    @Test
    void should_useClockHour_when_selectLineWithoutHour() {
        final Clock fixedClock =
                Clock.fixed(Instant.parse("2025-06-15T22:30:00Z"), ZoneId.of("UTC"));
        final Random seededRandom = new Random(0);
        final NpcDialogueService service = new NpcDialogueService(fixedClock, seededRandom);

        final NpcLines lines = new NpcLines(List.of("기본"), Map.of("night", List.of("밤대사")));
        final Npc npc = new Npc("test", "테스트", NpcType.CHIEF, "node1", "성격", lines);

        final String result = service.selectLine(npc);

        assertThat(result).isIn("기본", "밤대사");
    }
}
