package com.myapps.web.myrpg.domain.model;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link StatProgression}의 재능별 보조 치명 성장을 검증하는 프로퍼티 테스트.
 *
 * <p>{@code ARCHERY}의 보조 보너스는 {@link BonusTarget#CRITICAL}(+1/Lv)이므로,
 * {@code levelStatsFor(L, ARCHERY)}의 Critical은 공통값 {@code 50 + 3×(L-1)}에
 * {@code 1×(L-1)}을 가산한 {@code 50 + 4×(L-1)}이어야 한다.
 * {@code MELEE}/{@code MAGIC}의 보조 보너스는 바이탈 계열이므로 Critical은 공통값과 동일하다.
 *
 * <p>Feature: 004-talent-and-ability-points, Property 7: 재능별 보조 치명 성장 (활)
 *
 * <p><b>Validates: Requirements 7.3, 7.5</b>
 */
class TalentSecondaryCriticalPropertyTest {

    private final StatProgression statProgression = new StatProgression();

    /**
     * 임의의 레벨 L에 대해 {@code ARCHERY}의 Critical이
     * {@code 50 + 4×(L-1)} (공통 + 보조 보너스)을 만족하는지 검증한다.
     *
     * @param level 1 이상 100 이하의 임의 레벨
     */
    @Property(tries = 100)
    void should_addSecondaryCriticalBonus_when_archery(@ForAll("levels") final int level) {
        final Stats archery = statProgression.levelStatsFor(level, TalentType.ARCHERY);
        final int expected = 50 + 4 * (level - 1);

        assertThat(archery.critical()).isEqualTo(expected);
    }

    /**
     * 임의의 레벨 L에 대해 {@code MELEE}의 Critical이
     * 공통값 {@code 50 + 3×(L-1)}과 동일한지 검증한다.
     *
     * <p>{@code MELEE}의 보조 보너스는 HP(바이탈 계열)이므로 Critical에 영향 없다.
     *
     * @param level 1 이상 100 이하의 임의 레벨
     */
    @Property(tries = 100)
    void should_keepCommonCritical_when_melee(@ForAll("levels") final int level) {
        final Stats melee = statProgression.levelStatsFor(level, TalentType.MELEE);
        final int expected = 50 + 3 * (level - 1);

        assertThat(melee.critical()).isEqualTo(expected);
    }

    /**
     * 임의의 레벨 L에 대해 {@code MAGIC}의 Critical이
     * 공통값 {@code 50 + 3×(L-1)}과 동일한지 검증한다.
     *
     * <p>{@code MAGIC}의 보조 보너스는 MP(바이탈 계열)이므로 Critical에 영향 없다.
     *
     * @param level 1 이상 100 이하의 임의 레벨
     */
    @Property(tries = 100)
    void should_keepCommonCritical_when_magic(@ForAll("levels") final int level) {
        final Stats magic = statProgression.levelStatsFor(level, TalentType.MAGIC);
        final int expected = 50 + 3 * (level - 1);

        assertThat(magic.critical()).isEqualTo(expected);
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
