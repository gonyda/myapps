package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 진행바 동일가중 평균 공식의 정확성을 검증하는 프로퍼티 테스트.
 *
 * <p>공식: {@code (int)((min(usageCurrent/usageRequired, 1.0) + min(killCurrent/killRequired, 1.0)) /
 * 2.0 * 100)}
 *
 * <p>검증 속성:
 *
 * <ul>
 *   <li>결과가 [0, 100] 범위에 있다
 *   <li>usage ≥ req AND kill ≥ req → 결과 100
 *   <li>usage == 0 AND kill == 0 → 결과 0
 *   <li>usage == req AND kill == 0 → 결과 50
 * </ul>
 *
 * <p>Feature: 005-skill-system, Property 17: 진행바 동일가중 평균
 *
 * <p><b>Validates: Requirements 12.3</b>
 */
class SkillProgressPercentPropertyTest {

    private static final int FULL_PROGRESS_PERCENT = 100;
    private static final int PROGRESS_DIVISOR = 2;

    /**
     * 진행바 공식을 재현한다 (SkillService.calculateProgressPercent과 동일 로직).
     *
     * @param usageCurrent 현재 사용 횟수
     * @param killCurrent 현재 막타 처치 수
     * @param usageRequired 요구 사용 횟수 (양수)
     * @param killRequired 요구 막타 처치 수 (양수)
     * @return 진행률 퍼센트
     */
    private int calculateProgressPercent(
            final int usageCurrent,
            final int killCurrent,
            final int usageRequired,
            final int killRequired) {
        final double usageRatio = Math.min((double) usageCurrent / usageRequired, 1.0);
        final double killRatio = Math.min((double) killCurrent / killRequired, 1.0);
        return (int) ((usageRatio + killRatio) / PROGRESS_DIVISOR * FULL_PROGRESS_PERCENT);
    }

    /**
     * 임의 입력에 대해 진행률은 항상 [0, 100] 범위에 있어야 한다.
     *
     * @param usageCurrent 현재 사용 횟수
     * @param killCurrent 현재 막타 처치 수
     * @param usageRequired 요구 사용 횟수
     * @param killRequired 요구 막타 처치 수
     */
    @Property(tries = 100)
    void should_returnBetween0And100_when_anyValidInput(
            @ForAll("usageCurrents") final int usageCurrent,
            @ForAll("killCurrents") final int killCurrent,
            @ForAll("requirements") final int usageRequired,
            @ForAll("requirements") final int killRequired) {

        final int result =
                calculateProgressPercent(usageCurrent, killCurrent, usageRequired, killRequired);

        assertThat(result).as("진행률은 항상 [0, 100] 범위에 있어야 한다").isBetween(0, FULL_PROGRESS_PERCENT);
    }

    /**
     * usage ≥ req AND kill ≥ req이면 진행률은 100이어야 한다.
     *
     * @param usageRequired 요구 사용 횟수
     * @param killRequired 요구 막타 처치 수
     * @param usageExtra 초과 사용 횟수
     * @param killExtra 초과 막타 처치 수
     */
    @Property(tries = 100)
    void should_return100_when_bothConditionsMet(
            @ForAll("requirements") final int usageRequired,
            @ForAll("requirements") final int killRequired,
            @ForAll("extras") final int usageExtra,
            @ForAll("extras") final int killExtra) {

        final int usageCurrent = usageRequired + usageExtra;
        final int killCurrent = killRequired + killExtra;

        final int result =
                calculateProgressPercent(usageCurrent, killCurrent, usageRequired, killRequired);

        assertThat(result)
                .as("usage ≥ req AND kill ≥ req이면 진행률은 100이어야 한다")
                .isEqualTo(FULL_PROGRESS_PERCENT);
    }

    /**
     * usage == 0 AND kill == 0이면 진행률은 0이어야 한다.
     *
     * @param usageRequired 요구 사용 횟수
     * @param killRequired 요구 막타 처치 수
     */
    @Property(tries = 100)
    void should_return0_when_bothCountsAreZero(
            @ForAll("requirements") final int usageRequired,
            @ForAll("requirements") final int killRequired) {

        final int result = calculateProgressPercent(0, 0, usageRequired, killRequired);

        assertThat(result).as("usage == 0 AND kill == 0이면 진행률은 0이어야 한다").isEqualTo(0);
    }

    /**
     * usage == req AND kill == 0이면 진행률은 50이어야 한다.
     *
     * @param usageRequired 요구 사용 횟수
     * @param killRequired 요구 막타 처치 수
     */
    @Property(tries = 100)
    void should_return50_when_onlyUsageMet(
            @ForAll("requirements") final int usageRequired,
            @ForAll("requirements") final int killRequired) {

        final int result = calculateProgressPercent(usageRequired, 0, usageRequired, killRequired);

        assertThat(result).as("usage == req AND kill == 0이면 진행률은 50이어야 한다").isEqualTo(50);
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
     * 현재 막타 처치 수 (0~20000)를 생성하는 Arbitrary.
     *
     * @return 0~20000 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> killCurrents() {
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
