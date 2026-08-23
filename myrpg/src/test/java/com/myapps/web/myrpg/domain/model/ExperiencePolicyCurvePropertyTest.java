package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 경험치 곡선의 정확성을 검증하는 프로퍼티 테스트.
 *
 * <p>{@code L ∈ [1,100]} 전 구간에서 {@link ExperiencePolicy#requiredForNext(int)}가 {@code 50 × L + 15 ×
 * L²} 공식을 정확히 만족하며, 레벨이 증가할수록 필요 경험치가 단조 증가함을 검증한다.
 *
 * <p>Feature: 003-character-progression-and-rebirth, Property 1: 경험치 곡선
 *
 * <p><b>Validates: Requirements 2.1, 11.5</b>
 */
class ExperiencePolicyCurvePropertyTest {

    private final ExperiencePolicy policy = new ExperiencePolicy();

    /**
     * 임의의 레벨 L(1~100)에 대해 {@code requiredForNext(L) == 50 × L + 15 × L × L}임을 검증한다.
     *
     * @param level 1 이상 100 이하의 임의 레벨
     */
    @Property(tries = 100)
    void should_returnFiftyLevelPlusFifteenLevelSquared_when_anyValidLevel(
            @ForAll("levels") final int level) {
        final long expected = (50L * level) + (15L * level * level);
        final long actual = policy.requiredForNext(level);

        assertThat(actual).isEqualTo(expected);
    }

    /**
     * 임의의 레벨 L(1~99)에 대해 {@code requiredForNext(L) < requiredForNext(L+1)}임을 검증한다. 경험치 곡선이 단조 증가하는지
     * 확인한다.
     *
     * @param level 1 이상 99 이하의 임의 레벨
     */
    @Property(tries = 100)
    void should_beMonotonicallyIncreasing_when_levelIncreases(
            @ForAll("levelsForMonotonic") final int level) {
        final long current = policy.requiredForNext(level);
        final long next = policy.requiredForNext(level + 1);

        assertThat(current).isLessThan(next);
    }

    /**
     * 유효한 레벨(1~100)을 생성하는 Arbitrary 제공자.
     *
     * @return 1 이상 100 이하의 정수를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<Integer> levels() {
        return Arbitraries.integers().between(1, 100);
    }

    /**
     * 단조 증가 검증에 사용할 레벨(1~99)을 생성하는 Arbitrary 제공자. {@code L+1}이 100을 초과하지 않도록 상한을 99로 제한한다.
     *
     * @return 1 이상 99 이하의 정수를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<Integer> levelsForMonotonic() {
        return Arbitraries.integers().between(1, 99);
    }
}
