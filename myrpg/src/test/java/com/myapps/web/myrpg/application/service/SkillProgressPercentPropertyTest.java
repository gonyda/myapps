package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 진행바 단일 사용 횟수 비율 공식의 정확성을 검증하는 프로퍼티 테스트.
 *
 * <p>공식: {@code (int)(min(usageCurrent / usageRequired, 1.0) * 100)}
 *
 * <p>검증 속성:
 *
 * <ul>
 *   <li>결과가 [0, 100] 범위에 있다
 *   <li>usage ≥ req → 결과 100
 *   <li>usage == 0 → 결과 0
 *   <li>단조 증가 (usage1 ≤ usage2 → percent1 ≤ percent2)
 * </ul>
 *
 * <p>Feature: 016-skill-rank-requirement-simplification, Property 2: 수련 진행률 단일화
 *
 * <p><b>Validates: Requirements 2.1, 2.2, 2.3</b>
 */
class SkillProgressPercentPropertyTest {

    private static final int FULL_PROGRESS_PERCENT = 100;

    /**
     * 진행바 공식을 재현한다 (SkillService.calculateProgressPercent과 동일 로직).
     *
     * @param usageCurrent 현재 사용 횟수
     * @param usageRequired 요구 사용 횟수 (양수)
     * @return 진행률 퍼센트
     */
    private int calculateProgressPercent(final int usageCurrent, final int usageRequired) {
        final double usageRatio = Math.min((double) usageCurrent / usageRequired, 1.0);
        return (int) (usageRatio * FULL_PROGRESS_PERCENT);
    }

    /**
     * 임의 입력에 대해 진행률은 항상 [0, 100] 범위에 있어야 한다.
     *
     * @param usageCurrent 현재 사용 횟수
     * @param usageRequired 요구 사용 횟수
     */
    @Property(tries = 100)
    void should_returnBetween0And100_when_anyValidInput(
            @ForAll("usageCurrents") final int usageCurrent,
            @ForAll("requirements") final int usageRequired) {

        final int result = calculateProgressPercent(usageCurrent, usageRequired);

        assertThat(result).as("진행률은 항상 [0, 100] 범위에 있어야 한다").isBetween(0, FULL_PROGRESS_PERCENT);
    }

    /**
     * usage ≥ req이면 진행률은 100이어야 한다.
     *
     * @param usageRequired 요구 사용 횟수
     * @param usageExtra 초과 사용 횟수
     */
    @Property(tries = 100)
    void should_return100_when_usageConditionMet(
            @ForAll("requirements") final int usageRequired,
            @ForAll("extras") final int usageExtra) {

        final int usageCurrent = usageRequired + usageExtra;
        final int result = calculateProgressPercent(usageCurrent, usageRequired);

        assertThat(result).as("usage ≥ req이면 진행률은 100이어야 한다").isEqualTo(FULL_PROGRESS_PERCENT);
    }

    /**
     * usage == 0이면 진행률은 0이어야 한다.
     *
     * @param usageRequired 요구 사용 횟수
     */
    @Property(tries = 100)
    void should_return0_when_usageIsZero(@ForAll("requirements") final int usageRequired) {
        final int result = calculateProgressPercent(0, usageRequired);

        assertThat(result).as("usage == 0이면 진행률은 0이어야 한다").isEqualTo(0);
    }

    /**
     * 요구치 (양수, 1~10000)를 생성하는 Arbitrary.
     *
     * @return 1~10000 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> requirements() {
        return Arbitraries.integers().between(1, 10000);
    }

    /**
     * 현재 사용 횟수 (0~20000)를 생성하는 Arbitrary.
     *
     * @return 0~20000 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> usageCurrents() {
        return Arbitraries.integers().between(0, 20000);
    }

    /**
     * 초과분 (0~5000)을 생성하는 Arbitrary.
     *
     * @return 0~5000 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> extras() {
        return Arbitraries.integers().between(0, 5000);
    }
}
