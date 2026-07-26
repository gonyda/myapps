package com.myapps.web.myrpg.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Feature: myrpg-gen1-mvp, Property 3: 필요 경험치 공식
/**
 * CharacterService.requiredExp 메서드의 속성 기반 테스트.
 *
 * <p>레벨 N에서 N+1로 가기 위한 필요 경험치 공식 {@code round(100 × N^1.5)}(HALF_UP)이
 * 모든 유효 레벨(1 이상)에 대해 올바르게 동작하는지 검증한다.
 *
 * <p><b>Validates: Requirements 3.1</b>
 */
class CharacterServiceRequiredExpPropertyTest {

    private static final int LEVEL_MIN = 1;
    private static final int LEVEL_MAX = 1000;

    private final CharacterService characterService = new CharacterService();

    /**
     * 1 이상 1000 이하의 임의 레벨을 생성하는 Provider.
     *
     * @return 레벨 Arbitrary
     */
    @Provide
    Arbitrary<Integer> validLevels() {
        return Arbitraries.integers().between(LEVEL_MIN, LEVEL_MAX);
    }

    /**
     * 필요 경험치가 {@code round(100 × level^1.5)} (HALF_UP) 공식과 일치하는지 검증한다.
     *
     * <p>BigDecimal을 사용하여 독립적으로 기대값을 계산하고 구현 결과와 비교한다.
     *
     * @param level 검증할 레벨 (1 이상)
     */
    @Property(tries = 100)
    void requiredExpMatchesFormula(@ForAll("validLevels") final Integer level) {
        final int actual = characterService.requiredExp(level);

        final BigDecimal base = BigDecimal.valueOf(100);
        final BigDecimal power = BigDecimal.valueOf(Math.pow(level, 1.5));
        final BigDecimal raw = base.multiply(power);
        final int expected = raw.setScale(0, RoundingMode.HALF_UP).intValue();

        assertEquals(expected, actual,
                "레벨 " + level + "의 필요 경험치는 round(100 × " + level + "^1.5) = " + expected);
    }

    /**
     * 필요 경험치는 모든 유효 레벨에 대해 항상 0 이상의 정수 값이다.
     *
     * @param level 검증할 레벨 (1 이상)
     */
    @Property(tries = 100)
    void requiredExpIsNonNegative(@ForAll("validLevels") final Integer level) {
        final int actual = characterService.requiredExp(level);

        assertTrue(actual >= 0,
                "레벨 " + level + "의 필요 경험치는 0 이상이어야 한다: " + actual);
    }

    /**
     * 연속된 두 레벨의 필요 경험치를 비교하여 레벨이 높을수록 필요 경험치가 순증가(strictly increasing)함을 검증한다.
     *
     * @param level 검증할 레벨 (1 이상, level+1도 유효 범위 내)
     */
    @Property(tries = 100)
    void requiredExpIsStrictlyMonotonicallyIncreasing(@ForAll("validLevels") final Integer level) {
        final int expAtLevel = characterService.requiredExp(level);
        final int expAtNextLevel = characterService.requiredExp(level + 1);

        assertTrue(expAtNextLevel > expAtLevel,
                "레벨 " + (level + 1) + "의 필요 경험치(" + expAtNextLevel
                        + ")는 레벨 " + level + "의 필요 경험치(" + expAtLevel + ")보다 커야 한다");
    }
}
