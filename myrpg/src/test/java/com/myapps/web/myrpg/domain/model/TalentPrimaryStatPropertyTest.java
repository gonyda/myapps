package com.myapps.web.myrpg.domain.model;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재능별 주 스탯 성장을 검증하는 프로퍼티 테스트.
 *
 * <p>임의의 레벨 L ∈ [1,100]과 재능 3종에 대해, {@code levelStatsFor(L, talent)}의
 * 주 스탯이 {@code 공통값(L) + 2×(L-1)}이고, 주 스탯/보조 스탯이 아닌 스탯은 공통값과 동일한지 검증한다.
 *
 * <p>Feature: 004-talent-and-ability-points, Property 5: 재능별 주 스탯 성장
 *
 * <p><b>Validates: Requirements 6.1, 6.2, 6.3, 6.4, 11.2</b>
 */
class TalentPrimaryStatPropertyTest {

    private static final int PRIMARY_PER_LEVEL = 2;

    private final StatProgression statProgression = new StatProgression();

    /**
     * MELEE 재능의 주 스탯(STR)이 공통값 + 2×(L-1)이고,
     * 나머지 스탯(DEX, INT, CRITICAL, DEF)은 공통값과 동일한지 검증한다.
     *
     * <p>MELEE의 보조 보너스(HP)는 바이탈 계열이므로 스탯에 영향을 주지 않는다.
     *
     * @param level 1 이상 100 이하의 임의 레벨
     */
    @Property(tries = 100)
    void should_boostOnlyStr_when_meleeTalent(@ForAll("levels") final int level) {
        final Stats result = statProgression.levelStatsFor(level, TalentType.MELEE);
        final Stats common = statProgression.levelStatsFor(level);
        final int primaryBonus = PRIMARY_PER_LEVEL * (level - 1);

        // 주 스탯 STR = 공통값 + 2×(L-1)
        assertThat(result.str()).isEqualTo(common.str() + primaryBonus);

        // 비주력 스탯은 공통값과 동일
        assertThat(result.dex()).isEqualTo(common.dex());
        assertThat(result.intelligence()).isEqualTo(common.intelligence());
        assertThat(result.critical()).isEqualTo(common.critical());
        assertThat(result.defense()).isEqualTo(common.defense());
    }

    /**
     * ARCHERY 재능의 주 스탯(DEX)이 공통값 + 2×(L-1)이고,
     * 주 스탯/보조 스탯(CRITICAL)이 아닌 스탯(STR, INT, DEF)은 공통값과 동일한지 검증한다.
     *
     * <p>ARCHERY의 보조 보너스(CRITICAL, +1)는 스탯 계열이므로 Critical은 별도 증가한다.
     * 이 프로퍼티에서는 주 스탯 검증에 집중하고, 보조 CRITICAL은 Property 7에서 검증한다.
     *
     * @param level 1 이상 100 이하의 임의 레벨
     */
    @Property(tries = 100)
    void should_boostOnlyDex_when_archeryTalent(@ForAll("levels") final int level) {
        final Stats result = statProgression.levelStatsFor(level, TalentType.ARCHERY);
        final Stats common = statProgression.levelStatsFor(level);
        final int primaryBonus = PRIMARY_PER_LEVEL * (level - 1);

        // 주 스탯 DEX = 공통값 + 2×(L-1)
        assertThat(result.dex()).isEqualTo(common.dex() + primaryBonus);

        // 주/보조(CRITICAL)가 아닌 스탯은 공통값과 동일
        assertThat(result.str()).isEqualTo(common.str());
        assertThat(result.intelligence()).isEqualTo(common.intelligence());
        assertThat(result.defense()).isEqualTo(common.defense());
    }

    /**
     * MAGIC 재능의 주 스탯(INT)이 공통값 + 2×(L-1)이고,
     * 나머지 스탯(STR, DEX, CRITICAL, DEF)은 공통값과 동일한지 검증한다.
     *
     * <p>MAGIC의 보조 보너스(MP)는 바이탈 계열이므로 스탯에 영향을 주지 않는다.
     *
     * @param level 1 이상 100 이하의 임의 레벨
     */
    @Property(tries = 100)
    void should_boostOnlyInt_when_magicTalent(@ForAll("levels") final int level) {
        final Stats result = statProgression.levelStatsFor(level, TalentType.MAGIC);
        final Stats common = statProgression.levelStatsFor(level);
        final int primaryBonus = PRIMARY_PER_LEVEL * (level - 1);

        // 주 스탯 INT = 공통값 + 2×(L-1)
        assertThat(result.intelligence()).isEqualTo(common.intelligence() + primaryBonus);

        // 비주력 스탯은 공통값과 동일
        assertThat(result.str()).isEqualTo(common.str());
        assertThat(result.dex()).isEqualTo(common.dex());
        assertThat(result.critical()).isEqualTo(common.critical());
        assertThat(result.defense()).isEqualTo(common.defense());
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
