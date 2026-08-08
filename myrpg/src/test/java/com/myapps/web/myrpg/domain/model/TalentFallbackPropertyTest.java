package com.myapps.web.myrpg.domain.model;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TalentType#fromNameOrFallback(String, TalentType)}의 폴백 동작을 검증하는 프로퍼티 테스트.
 *
 * <p>유효한 상수명("MELEE"/"ARCHERY"/"MAGIC")이면 정확히 대응하는 재능을 반환하고,
 * null·공백·미지값이면 폴백({@code MELEE})을 반환하는지 검증한다. 상수명 비교는 대소문자를
 * 구분하므로 "melee"/"Archery" 등 대소문자 불일치 문자열도 폴백 대상이다.
 *
 * <p>Feature: 004-talent-and-ability-points, Property 14: 재능 파라미터 폴백
 *
 * <p><b>Validates: Requirements 5.8</b>
 */
class TalentFallbackPropertyTest {

    private static final TalentType FALLBACK = TalentType.MELEE;

    /**
     * 임의의 유효한 재능 상수명에 대해 정확히 대응하는 {@link TalentType}을 반환하는지 검증한다.
     *
     * @param talent 임의의 유효한 재능 상수
     */
    // Feature: 004-talent-and-ability-points, Property 14: 재능 파라미터 폴백
    @Property(tries = 100)
    void should_returnMatchingTalent_when_validConstantName(@ForAll("talentTypes") final TalentType talent) {
        assertThat(TalentType.fromNameOrFallback(talent.name(), FALLBACK)).isSameAs(talent);
    }

    /**
     * null·공백·미지값 등 유효하지 않은 문자열에 대해 폴백({@code MELEE})을 반환하는지 검증한다.
     *
     * @param invalidName 유효한 상수명이 아닌 임의의 문자열(null·공백·미지값 포함)
     */
    // Feature: 004-talent-and-ability-points, Property 14: 재능 파라미터 폴백
    @Property(tries = 100)
    void should_returnFallback_when_invalidName(@ForAll("invalidNames") final String invalidName) {
        assertThat(TalentType.fromNameOrFallback(invalidName, FALLBACK)).isSameAs(FALLBACK);
    }

    /**
     * 모든 유효한 재능 상수를 생성하는 Arbitrary 제공자.
     *
     * @return 전체 {@link TalentType} 상수 중 하나를 균등하게 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<TalentType> talentTypes() {
        return Arbitraries.of(TalentType.values());
    }

    /**
     * 유효한 상수명이 아닌 문자열을 생성하는 Arbitrary 제공자.
     *
     * <p>명시적 엣지 케이스(null, 빈 문자열, 공백 전용, 대소문자 불일치, 미지값)와,
     * 유효 상수명을 필터로 제외한 임의 알파벳 문자열을 함께 제공한다.
     *
     * @return 폴백을 유발해야 하는 문자열을 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<String> invalidNames() {
        final Arbitrary<String> nullValue = Arbitraries.just((String) null);
        final Arbitrary<String> blanks = Arbitraries.of("", " ", "   ", "\t\n");
        final Arbitrary<String> unknowns =
                Arbitraries.of("XXX", "melee", "Archery", "magic", "WARRIOR", "MELEE ");
        final Arbitrary<String> randomAlpha = Arbitraries.strings().alpha().ofMaxLength(8)
                .filter(candidate -> !isValidName(candidate));
        return Arbitraries.oneOf(nullValue, blanks, unknowns, randomAlpha);
    }

    /**
     * 주어진 문자열이 유효한 {@link TalentType} 상수명인지 판별한다.
     *
     * @param name 검사할 문자열
     * @return 유효한 상수명이면 {@code true}, 그 외에는 {@code false}
     */
    private static boolean isValidName(final String name) {
        for (final TalentType type : TalentType.values()) {
            if (type.name().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
