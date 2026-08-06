package com.myapps.web.myrpg.interfaces.api;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Critical 표시 포맷 프로퍼티 테스트.
 *
 * <p>임의의 0.1% 단위 정수 {@code t ≥ 0}에서
 * {@code formatCritical(t)} = {@code "{t/10}.{t%10}%"} 공식을 검증한다.
 *
 * <p>Feature: 003-character-progression-and-rebirth, Property 11: Critical 표시 포맷
 *
 * <p><b>Validates: Requirements 5.4</b>
 */
class CriticalFormatPropertyTest {

    private final PlayScreenViewHelper helper = new PlayScreenViewHelper(
            new ExperiencePolicy(), new StatProgression());

    /**
     * 임의의 t(0~9999)에서 formatCritical(t)이 "{t/10}.{t%10}%" 형식임을 검증한다.
     *
     * @param tenths 0.1% 단위 정수 (0 이상)
     */
    @Property(tries = 100)
    void should_formatCritical_when_givenTenths(
            @ForAll @IntRange(min = 0, max = 9999) final int tenths) {

        final String result = helper.formatCritical(tenths);

        final int expectedWhole = tenths / 10;
        final int expectedFraction = tenths % 10;
        final String expected = expectedWhole + "." + expectedFraction + "%";

        assertThat(result).isEqualTo(expected);
    }

    /**
     * 임의의 t(0~9999)에서 formatCriticalDelta(t)이 "+{t/10}.{t%10}%" 형식임을 검증한다.
     *
     * @param tenths 0.1% 단위 보너스 정수 (0 이상)
     */
    @Property(tries = 100)
    void should_formatCriticalDelta_when_givenTenths(
            @ForAll @IntRange(min = 0, max = 9999) final int tenths) {

        final String result = helper.formatCriticalDelta(tenths);

        final int expectedWhole = tenths / 10;
        final int expectedFraction = tenths % 10;
        final String expected = "+" + expectedWhole + "." + expectedFraction + "%";

        assertThat(result).isEqualTo(expected);
    }
}
