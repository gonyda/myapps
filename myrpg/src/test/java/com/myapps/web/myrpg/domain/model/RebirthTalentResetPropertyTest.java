package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 환생 시 재능 보너스 초기화를 검증하는 프로퍼티 테스트.
 *
 * <p>임의의 재능 T에 대해, 레벨 1에서 {@code levelStatsFor(1, T)}는 공통 기본값과 같고 {@code vitalMaxFor(1, T)}는 세 바이탈
 * 모두 100인지 검증한다. 이는 재능 보너스 공식이 {@code perLevel × (level - 1)}이므로 level=1이면 보너스가 0이 되어 환생(레벨 1 복귀) 시
 * 자동 초기화가 보장됨을 확인한다.
 *
 * <p>Feature: 004-talent-and-ability-points, Property 10: 환생 시 재능 보너스 초기화
 *
 * <p><b>Validates: Requirements 12.4</b>
 */
class RebirthTalentResetPropertyTest {

    private static final int COMMON_BASE_VITAL = 100;

    private final StatProgression statProgression = new StatProgression();

    /**
     * 레벨 1에서 모든 재능의 스탯이 공통 기본값과 동일한지 검증한다.
     *
     * <p>{@code perLevel × (1 - 1) = 0}이므로 재능 보너스가 가산되지 않아 재능과 무관하게 공통 기본값만 반환되어야 한다.
     *
     * @param talent 임의의 재능 타입 (3종)
     */
    @Property(tries = 100)
    void should_returnCommonBaseStats_when_levelIsOne(@ForAll("talents") final TalentType talent) {
        final Stats result = statProgression.levelStatsFor(1, talent);
        final Stats common = statProgression.levelStatsFor(1);

        assertThat(result).isEqualTo(common);
    }

    /**
     * 레벨 1에서 모든 재능의 바이탈 최대치가 HP=MP=Stamina=100인지 검증한다.
     *
     * <p>공통 바이탈 = {@code 100 + 10×(1-1) = 100}이고, 재능 보너스 = {@code perLevel × 0 = 0}이므로 세 바이탈 모두
     * 100이어야 한다.
     *
     * @param talent 임의의 재능 타입 (3종)
     */
    @Property(tries = 100)
    void should_returnAllVitalsAt100_when_levelIsOne(@ForAll("talents") final TalentType talent) {
        final VitalMax vitalResult = statProgression.vitalMaxFor(1, talent);

        assertThat(vitalResult.hp()).isEqualTo(COMMON_BASE_VITAL);
        assertThat(vitalResult.mp()).isEqualTo(COMMON_BASE_VITAL);
        assertThat(vitalResult.stamina()).isEqualTo(COMMON_BASE_VITAL);
    }

    /**
     * 모든 재능 타입(3종)을 생성하는 Arbitrary 제공자.
     *
     * @return MELEE, ARCHERY, MAGIC 중 하나를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<TalentType> talents() {
        return Arbitraries.of(TalentType.values());
    }
}
