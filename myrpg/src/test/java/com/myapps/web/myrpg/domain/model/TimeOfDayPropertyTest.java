package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 시각→TimeOfDay 매핑의 정확성을 검증하는 프로퍼티 테스트.
 *
 * <p>{@code hour ∈ [0,23]} 전 구간에서 {@link TimeOfDay#fromHour(int)}가 {@code from <= hour < to}를 만족하는
 * 상수를 정확히 하나 반환하며, 6개 구간이 {@code [0,24)}를 빈틈·중복 없이 분할함을 검증한다.
 *
 * <p>Feature: 002-npc-system, Property 5: 시각→Time_Of_Day 매핑
 *
 * <p><b>Validates: Requirements 3.1</b>
 */
class TimeOfDayPropertyTest {

    /**
     * 임의의 시(hour)에 대해 {@code fromHour(hour)}가 반환하는 {@link TimeOfDay}의 구간 {@code [from, to)}이 해당
     * hour를 포함하는지 검증한다.
     *
     * @param hour 0 이상 24 미만의 임의 시각
     */
    @Property(tries = 100)
    void should_returnTimeOfDayContainingHour_when_anyValidHour(@ForAll("hours") final int hour) {
        final TimeOfDay result = TimeOfDay.fromHour(hour);

        assertThat(result.from()).isLessThanOrEqualTo(hour);
        assertThat(result.to()).isGreaterThan(hour);
    }

    /**
     * 임의의 시(hour)에 대해 정확히 하나의 {@link TimeOfDay} 상수만 해당 hour를 구간에 포함하는지 검증한다.
     *
     * @param hour 0 이상 24 미만의 임의 시각
     */
    @Property(tries = 100)
    void should_matchExactlyOneConstant_when_anyValidHour(@ForAll("hours") final int hour) {
        final long matchCount =
                Arrays.stream(TimeOfDay.values())
                        .filter(tod -> tod.from() <= hour && hour < tod.to())
                        .count();

        assertThat(matchCount).isEqualTo(1L);
    }

    /**
     * 6개의 {@link TimeOfDay} 구간이 {@code [0, 24)}를 빈틈·중복 없이 분할하는지 검증한다. 구간을 {@code from} 기준 정렬 후, 첫
     * 구간은 0에서 시작하고 마지막 구간은 24에서 끝나며, 인접 구간의 {@code to}와 다음 구간의 {@code from}이 정확히 일치함을 확인한다.
     */
    @Property(tries = 100)
    void should_partitionFullDay_when_allConstantsExamined() {
        final List<TimeOfDay> sorted =
                Arrays.stream(TimeOfDay.values())
                        .sorted((a, b) -> Integer.compare(a.from(), b.from()))
                        .toList();

        assertThat(sorted).hasSize(6);
        assertThat(sorted.getFirst().from()).isEqualTo(0);
        assertThat(sorted.getLast().to()).isEqualTo(24);

        for (int i = 0; i < sorted.size() - 1; i++) {
            final TimeOfDay current = sorted.get(i);
            final TimeOfDay next = sorted.get(i + 1);
            assertThat(current.to()).isEqualTo(next.from());
        }
    }

    /** 모든 {@link TimeOfDay} 상수가 비어있지 않은 이모지를 제공하는지 검증한다. */
    @Property(tries = 10)
    void should_provideNonBlankEmoji_when_allConstantsChecked() {
        for (final TimeOfDay tod : TimeOfDay.values()) {
            assertThat(tod.emoji()).isNotBlank();
        }
    }

    /**
     * 유효한 시각(0~23)을 생성하는 Arbitrary 제공자.
     *
     * @return 0 이상 23 이하의 정수를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<Integer> hours() {
        return Arbitraries.integers().between(0, 23);
    }
}
