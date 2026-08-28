package com.myapps.web.myrpg.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** {@link CharacterProgress}의 인게임 시간 시스템(0~1439분 순환) 동작을 검증하는 단위 테스트. */
class CharacterProgressInGameTimeTest {

    @Test
    @DisplayName("기본 생성된 캐릭터는 인게임 시간 08:00 (480분)을 가진다")
    void should_haveDefaultInGameTimeOf0800_when_created() {
        final CharacterProgress progress = CharacterProgress.createDefault();

        assertEquals(480, progress.getInGameMinutes());
        assertEquals(8, progress.getInGameHour());
        assertEquals(0, progress.getInGameMinute());
        assertEquals("08:00", progress.getInGameTimeFormatted());
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0, '00:00'",
        "480, 8, 0, '08:00'",
        "515, 8, 35, '08:35'",
        "720, 12, 0, '12:00'",
        "1439, 23, 59, '23:59'"
    })
    @DisplayName("분 단위 시간에 따라 시, 분, 포맷 문자열이 정확하게 계산된다")
    void should_calculateHourMinuteAndFormatted_when_setInGameMinutes(
            final int minutes,
            final int expectedHour,
            final int expectedMinute,
            final String expectedFormatted) {
        final CharacterProgress progress = CharacterProgress.createDefault();
        progress.setInGameMinutes(minutes);

        assertEquals(minutes, progress.getInGameMinutes());
        assertEquals(expectedHour, progress.getInGameHour());
        assertEquals(expectedMinute, progress.getInGameMinute());
        assertEquals(expectedFormatted, progress.getInGameTimeFormatted());
    }

    @Test
    @DisplayName("시간 경과 시 분이 누적되며 1440분을 넘어가면 24시간 주기로 순환(wrap-around)한다")
    void should_wrapAround24Hours_when_advancingTimePastMidnight() {
        final CharacterProgress progress = CharacterProgress.createDefault();
        progress.setInGameMinutes(1435); // 23:55
        assertEquals("23:55", progress.getInGameTimeFormatted());

        // 15분 경과 -> 23:55 + 15분 = 00:10 (10분)
        progress.advanceInGameTime(15);
        assertEquals(10, progress.getInGameMinutes());
        assertEquals(0, progress.getInGameHour());
        assertEquals(10, progress.getInGameMinute());
        assertEquals("00:10", progress.getInGameTimeFormatted());
    }

    @Test
    @DisplayName("0 이하의 분으로 시간 경과 시도 시 시간은 변하지 않는다")
    void should_notChangeTime_when_advancingNegativeOrZeroMinutes() {
        final CharacterProgress progress = CharacterProgress.createDefault();
        progress.setInGameMinutes(480);

        progress.advanceInGameTime(0);
        assertEquals(480, progress.getInGameMinutes());

        progress.advanceInGameTime(-15);
        assertEquals(480, progress.getInGameMinutes());
    }
}
