package com.myapps.web.myrpg.domain.model;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TalentType 재능 데이터 완비를 검증하는 프로퍼티 테스트.
 *
 * <p>모든 {@link TalentType} 상수(MELEE/ARCHERY/MAGIC)가 비어 있지 않은 {@code label()}과
 * {@code effectSummary()}를 보유하고, {@code primary()}/{@code secondary()}가 각각 유효한
 * {@link BonusTarget}과 비음수 {@code perLevel()}을 가지며, {@code damageBonusPercent()}가
 * 0 이상임을 검증한다.
 *
 * <p><b>Validates: Requirements 9.1, 9.2, 9.3, 10.1, 10.3, 11.1, 11.3, 11.5</b>
 */
class TalentTypeCompletenessPropertyTest {

    // Feature: 004-talent-and-ability-points, Property 13: 재능 데이터 완비
    /**
     * 모든 TalentType 상수가 비어 있지 않은 라벨/효과 요약, 유효한 주·보조 보너스,
     * 0 이상의 데미지 보너스 퍼센트를 보유하는지 검증한다.
     *
     * @param talent 임의의 TalentType 상수
     */
    @Property(tries = 100)
    void should_haveCompleteTalentData_when_anyTalentType(@ForAll("talents") final TalentType talent) {
        // Then: label()은 비어 있지 않은 문자열
        assertThat(talent.label()).isNotNull().isNotBlank();

        // Then: effectSummary()는 비어 있지 않은 문자열
        assertThat(talent.effectSummary()).isNotNull().isNotBlank();

        // Then: primary()는 유효한 BonusTarget과 비음수 perLevel을 보유
        assertBonusIsValid(talent.primary());

        // Then: secondary()는 유효한 BonusTarget과 비음수 perLevel을 보유
        assertBonusIsValid(talent.secondary());

        // Then: damageBonusPercent()는 0 이상
        assertThat(talent.damageBonusPercent()).isGreaterThanOrEqualTo(0);
    }

    /**
     * 주어진 재능 보너스가 유효한 대상과 비음수 레벨당 증가치를 갖는지 검증한다.
     *
     * @param bonus 검증할 재능 보너스
     */
    private void assertBonusIsValid(final TalentBonus bonus) {
        assertThat(bonus).isNotNull();
        assertThat(bonus.target()).isNotNull();
        assertThat(bonus.perLevel()).isGreaterThanOrEqualTo(0);
    }

    /**
     * TalentType 상수를 생성하는 Arbitrary 제공자.
     *
     * @return 전체 TalentType 상수 중 하나를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<TalentType> talents() {
        return Arbitraries.of(TalentType.values());
    }
}
