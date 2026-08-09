package com.myapps.web.myrpg.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BonusTarget} 8개 상수의 {@link BonusKind} 분류를 검증하는 단위 테스트.
 *
 * <p>스탯 계열 5종({@code STR}/{@code DEX}/{@code INT}/{@code CRITICAL}/{@code DEF})은 {@code STAT}으로,
 * 바이탈 계열 3종({@code HP}/{@code MP}/{@code STAMINA})은 {@code VITAL}로 분류되는지 확인한다.
 *
 * <p><b>Validates: Requirements 11.2</b>
 */
class BonusTargetTest {

    /**
     * STR의 kind()는 STAT임을 검증한다.
     */
    @Test
    void should_classifyAsStat_when_str() {
        assertThat(BonusTarget.STR.kind()).isEqualTo(BonusKind.STAT);
    }

    /**
     * DEX의 kind()는 STAT임을 검증한다.
     */
    @Test
    void should_classifyAsStat_when_dex() {
        assertThat(BonusTarget.DEX.kind()).isEqualTo(BonusKind.STAT);
    }

    /**
     * INT의 kind()는 STAT임을 검증한다.
     */
    @Test
    void should_classifyAsStat_when_int() {
        assertThat(BonusTarget.INT.kind()).isEqualTo(BonusKind.STAT);
    }

    /**
     * CRITICAL의 kind()는 STAT임을 검증한다.
     */
    @Test
    void should_classifyAsStat_when_critical() {
        assertThat(BonusTarget.CRITICAL.kind()).isEqualTo(BonusKind.STAT);
    }

    /**
     * HP의 kind()는 VITAL임을 검증한다.
     */
    @Test
    void should_classifyAsVital_when_hp() {
        assertThat(BonusTarget.HP.kind()).isEqualTo(BonusKind.VITAL);
    }

    /**
     * MP의 kind()는 VITAL임을 검증한다.
     */
    @Test
    void should_classifyAsVital_when_mp() {
        assertThat(BonusTarget.MP.kind()).isEqualTo(BonusKind.VITAL);
    }

    /**
     * STAMINA의 kind()는 VITAL임을 검증한다.
     */
    @Test
    void should_classifyAsVital_when_stamina() {
        assertThat(BonusTarget.STAMINA.kind()).isEqualTo(BonusKind.VITAL);
    }

    /**
     * DEF의 kind()는 STAT임을 검증한다.
     */
    @Test
    void should_classifyAsStat_when_def() {
        assertThat(BonusTarget.DEF.kind()).isEqualTo(BonusKind.STAT);
    }
}
