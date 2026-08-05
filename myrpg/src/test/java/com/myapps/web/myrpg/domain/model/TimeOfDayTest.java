package com.myapps.web.myrpg.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link TimeOfDay} 열거형의 기본 동작을 검증하는 단위 테스트.
 */
class TimeOfDayTest {

    @ParameterizedTest
    @CsvSource({
            "0, LATE_NIGHT",
            "4, LATE_NIGHT",
            "5, DAWN",
            "7, DAWN",
            "8, MORNING",
            "11, MORNING",
            "12, AFTERNOON",
            "15, AFTERNOON",
            "16, LATE_AFTERNOON",
            "18, LATE_AFTERNOON",
            "19, NIGHT",
            "23, NIGHT"
    })
    void should_returnCorrectTimeOfDay_when_fromHourCalled(final int hour, final String expected) {
        final TimeOfDay result = TimeOfDay.fromHour(hour);

        assertEquals(TimeOfDay.valueOf(expected), result);
    }

    @ParameterizedTest
    @CsvSource({
            "LATE_NIGHT, late-night, 0, 5",
            "DAWN, dawn, 5, 8",
            "MORNING, morning, 8, 12",
            "AFTERNOON, afternoon, 12, 16",
            "LATE_AFTERNOON, late-afternoon, 16, 19",
            "NIGHT, night, 19, 24"
    })
    void should_haveCorrectKeyAndBounds_when_enumConstantAccessed(
            final String name, final String expectedKey, final int expectedFrom, final int expectedTo) {
        final TimeOfDay timeOfDay = TimeOfDay.valueOf(name);

        assertEquals(expectedKey, timeOfDay.key());
        assertEquals(expectedFrom, timeOfDay.from());
        assertEquals(expectedTo, timeOfDay.to());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100, 24, 25, 100})
    void should_throwIllegalArgumentException_when_hourOutOfRange(final int invalidHour) {
        assertThrows(IllegalArgumentException.class, () -> TimeOfDay.fromHour(invalidHour));
    }

    @Test
    void should_coverAllHours_when_iteratingZeroToTwentyThree() {
        for (int hour = 0; hour < 24; hour++) {
            final TimeOfDay result = TimeOfDay.fromHour(hour);
            assertEquals(true, result.from() <= hour && hour < result.to());
        }
    }

    @Test
    void should_haveSixConstants_when_valuesChecked() {
        assertEquals(6, TimeOfDay.values().length);
    }
}
