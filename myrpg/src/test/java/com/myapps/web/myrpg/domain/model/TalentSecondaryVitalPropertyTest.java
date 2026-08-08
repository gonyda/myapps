package com.myapps.web.myrpg.domain.model;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static net.jqwik.api.Arbitraries.integers;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재능별 보조 바이탈 성장을 검증하는 프로퍼티 테스트.
 *
 * <p>{@link StatProgression#vitalMaxFor(int, TalentType)}가 재능의 보조 바이탈 보너스를
 * 올바르게 적용하는지 확인한다.
 * <ul>
 *   <li>{@code MELEE}: HP만 공통 + 5×(L-1), MP·Stamina는 공통</li>
 *   <li>{@code MAGIC}: MP만 공통 + 5×(L-1), HP·Stamina는 공통</li>
 *   <li>{@code ARCHERY}: 세 바이탈 모두 공통값(보조 보너스가 CRITICAL → STAT 계열이므로 바이탈 무영향)</li>
 * </ul>
 *
 * <p><b>Validates: Requirements 7.1, 7.2, 7.4, 7.6, 8.1, 8.2</b>
 */
class TalentSecondaryVitalPropertyTest {

    // Feature: 004-talent-and-ability-points, Property 6: 재능별 보조 바이탈 성장 (근접/마법)

    private final StatProgression statProgression = new StatProgression();

    /**
     * MELEE 재능은 HP만 공통 + 5×(L-1)이고, MP와 Stamina는 공통값과 동일한지 검증한다.
     *
     * @param level 1~100 범위의 임의 레벨
     */
    @Property(tries = 100)
    void should_boostOnlyHp_when_meleeTalent(@ForAll("levels") final int level) {
        final int common = 100 + 10 * (level - 1);
        final int expectedHp = common + 5 * (level - 1);

        final VitalMax melee = statProgression.vitalMaxFor(level, TalentType.MELEE);

        assertThat(melee.hp()).isEqualTo(expectedHp);
        assertThat(melee.mp()).isEqualTo(common);
        assertThat(melee.stamina()).isEqualTo(common);
    }

    /**
     * MAGIC 재능은 MP만 공통 + 5×(L-1)이고, HP와 Stamina는 공통값과 동일한지 검증한다.
     *
     * @param level 1~100 범위의 임의 레벨
     */
    @Property(tries = 100)
    void should_boostOnlyMp_when_magicTalent(@ForAll("levels") final int level) {
        final int common = 100 + 10 * (level - 1);
        final int expectedMp = common + 5 * (level - 1);

        final VitalMax magic = statProgression.vitalMaxFor(level, TalentType.MAGIC);

        assertThat(magic.hp()).isEqualTo(common);
        assertThat(magic.mp()).isEqualTo(expectedMp);
        assertThat(magic.stamina()).isEqualTo(common);
    }

    /**
     * ARCHERY 재능은 세 바이탈 모두 공통값과 동일한지 검증한다.
     *
     * <p>ARCHERY의 보조 보너스 대상은 CRITICAL(STAT 계열)이므로 바이탈에는 가산되지 않는다.
     *
     * @param level 1~100 범위의 임의 레벨
     */
    @Property(tries = 100)
    void should_haveAllCommonVitals_when_archeryTalent(@ForAll("levels") final int level) {
        final int common = 100 + 10 * (level - 1);

        final VitalMax archery = statProgression.vitalMaxFor(level, TalentType.ARCHERY);

        assertThat(archery.hp()).isEqualTo(common);
        assertThat(archery.mp()).isEqualTo(common);
        assertThat(archery.stamina()).isEqualTo(common);
    }

    /**
     * 레벨 생성기 — 1~100 범위의 정수를 균등 생성한다.
     *
     * @return 1~100 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> levels() {
        return integers().between(1, 100);
    }
}
