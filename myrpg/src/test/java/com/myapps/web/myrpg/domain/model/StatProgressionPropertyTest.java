package com.myapps.web.myrpg.domain.model;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link StatProgression}의 레벨 파생 스탯 계산 공식을 검증하는 프로퍼티 테스트.
 *
 * <p>임의의 레벨 L ∈ [1,100]에 대해 {@code levelStatsFor(L)}와 {@code vitalMaxFor(L)}가
 * 설계서에 정의된 공식과 일치하는지 검증한다.
 *
 * <p>Feature: 003-character-progression-and-rebirth, Property 5: 레벨 파생 스탯 계산
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3</b>
 */
class StatProgressionPropertyTest {

    private final StatProgression statProgression = new StatProgression();

    /**
     * 임의의 레벨 L에 대해 STR, DEX, INT가 공식 {@code 10 + 3×(L-1)}을 만족하는지 검증한다.
     *
     * @param level 1 이상 100 이하의 임의 레벨
     */
    @Property(tries = 100)
    void should_calculateCorrectStrDexInt_when_anyValidLevel(@ForAll("levels") final int level) {
        final Stats stats = statProgression.levelStatsFor(level);
        final int expected = 10 + 3 * (level - 1);

        assertThat(stats.str()).isEqualTo(expected);
        assertThat(stats.dex()).isEqualTo(expected);
        assertThat(stats.intelligence()).isEqualTo(expected);
    }

    /**
     * 임의의 레벨 L에 대해 Critical이 공식 {@code 50 + 3×(L-1)} (0.1% 단위)을 만족하는지 검증한다.
     *
     * @param level 1 이상 100 이하의 임의 레벨
     */
    @Property(tries = 100)
    void should_calculateCorrectCritical_when_anyValidLevel(@ForAll("levels") final int level) {
        final Stats stats = statProgression.levelStatsFor(level);
        final int expected = 50 + 3 * (level - 1);

        assertThat(stats.critical()).isEqualTo(expected);
    }

    /**
     * 임의의 레벨 L에 대해 DEF가 공식 {@code 5 + 1×(L-1)}을 만족하는지 검증한다.
     *
     * @param level 1 이상 100 이하의 임의 레벨
     */
    @Property(tries = 100)
    void should_calculateCorrectDefense_when_anyValidLevel(@ForAll("levels") final int level) {
        final Stats stats = statProgression.levelStatsFor(level);
        final int expected = 5 + (level - 1);

        assertThat(stats.defense()).isEqualTo(expected);
    }

    /**
     * 임의의 레벨 L에 대해 바이탈 최대치가 공식 {@code 100 + 10×(L-1)}을 만족하는지 검증한다.
     *
     * @param level 1 이상 100 이하의 임의 레벨
     */
    @Property(tries = 100)
    void should_calculateCorrectVitalMax_when_anyValidLevel(@ForAll("levels") final int level) {
        final int vitalMax = statProgression.vitalMaxFor(level);
        final int expected = 100 + 10 * (level - 1);

        assertThat(vitalMax).isEqualTo(expected);
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
}
