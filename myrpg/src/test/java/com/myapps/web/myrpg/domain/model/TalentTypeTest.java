package com.myapps.web.myrpg.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TalentType} 열거형의 라벨 매핑과 재능 데이터 구체값을 검증하는 단위 테스트.
 *
 * <p>3개 상수({@code MELEE}, {@code ARCHERY}, {@code MAGIC})의 {@code label()},
 * {@code primary()}/{@code secondary()}/{@code damageBonusPercent()}/{@code effectSummary()}
 * 구체값과 {@link TalentType#fromNameOrFallback(String, TalentType)} 폴백 동작을 예시로 확인한다.
 *
 * <p><b>Validates: Requirements 9.1, 10.1, 10.3, 11.1</b>
 */
class TalentTypeTest {

    /**
     * MELEE의 라벨은 "근접전투"임을 검증한다.
     */
    @Test
    void should_returnMeleeLabel_when_melee() {
        assertThat(TalentType.MELEE.label()).isEqualTo("근접전투");
    }

    /**
     * ARCHERY의 라벨은 "활"임을 검증한다.
     */
    @Test
    void should_returnArcheryLabel_when_archery() {
        assertThat(TalentType.ARCHERY.label()).isEqualTo("활");
    }

    /**
     * MAGIC의 라벨은 "마법"임을 검증한다.
     */
    @Test
    void should_returnMagicLabel_when_magic() {
        assertThat(TalentType.MAGIC.label()).isEqualTo("마법");
    }

    /**
     * MELEE의 주 스탯 보너스는 STR +2/Lv임을 검증한다.
     */
    @Test
    void should_returnStrPrimary_when_melee() {
        assertThat(TalentType.MELEE.primary()).isEqualTo(new TalentBonus(BonusTarget.STR, 2));
    }

    /**
     * MELEE의 보조 성장 보너스는 HP +5/Lv임을 검증한다.
     */
    @Test
    void should_returnHpSecondary_when_melee() {
        assertThat(TalentType.MELEE.secondary()).isEqualTo(new TalentBonus(BonusTarget.HP, 5));
    }

    /**
     * MELEE의 데미지 보너스 퍼센트는 10임을 검증한다.
     */
    @Test
    void should_return10DamageBonus_when_melee() {
        assertThat(TalentType.MELEE.damageBonusPercent()).isEqualTo(10);
    }

    /**
     * MELEE의 효과 요약 문자열을 검증한다.
     */
    @Test
    void should_returnMeleeEffectSummary_when_melee() {
        assertThat(TalentType.MELEE.effectSummary()).isEqualTo("근접 데미지 +10%, STR +2/Lv, HP +5/Lv");
    }

    /**
     * ARCHERY의 주 스탯 보너스는 DEX +2/Lv임을 검증한다.
     */
    @Test
    void should_returnDexPrimary_when_archery() {
        assertThat(TalentType.ARCHERY.primary()).isEqualTo(new TalentBonus(BonusTarget.DEX, 2));
    }

    /**
     * ARCHERY의 보조 성장 보너스는 Critical +1/Lv임을 검증한다.
     */
    @Test
    void should_returnCriticalSecondary_when_archery() {
        assertThat(TalentType.ARCHERY.secondary()).isEqualTo(new TalentBonus(BonusTarget.CRITICAL, 1));
    }

    /**
     * ARCHERY의 데미지 보너스 퍼센트는 10임을 검증한다.
     */
    @Test
    void should_return10DamageBonus_when_archery() {
        assertThat(TalentType.ARCHERY.damageBonusPercent()).isEqualTo(10);
    }

    /**
     * ARCHERY의 효과 요약 문자열을 검증한다.
     */
    @Test
    void should_returnArcheryEffectSummary_when_archery() {
        assertThat(TalentType.ARCHERY.effectSummary()).isEqualTo("원거리 데미지 +10%, DEX +2/Lv, 치명 +0.1%/Lv");
    }

    /**
     * MAGIC의 주 스탯 보너스는 INT +2/Lv임을 검증한다.
     */
    @Test
    void should_returnIntPrimary_when_magic() {
        assertThat(TalentType.MAGIC.primary()).isEqualTo(new TalentBonus(BonusTarget.INT, 2));
    }

    /**
     * MAGIC의 보조 성장 보너스는 MP +5/Lv임을 검증한다.
     */
    @Test
    void should_returnMpSecondary_when_magic() {
        assertThat(TalentType.MAGIC.secondary()).isEqualTo(new TalentBonus(BonusTarget.MP, 5));
    }

    /**
     * MAGIC의 데미지 보너스 퍼센트는 10임을 검증한다.
     */
    @Test
    void should_return10DamageBonus_when_magic() {
        assertThat(TalentType.MAGIC.damageBonusPercent()).isEqualTo(10);
    }

    /**
     * MAGIC의 효과 요약 문자열을 검증한다.
     */
    @Test
    void should_returnMagicEffectSummary_when_magic() {
        assertThat(TalentType.MAGIC.effectSummary()).isEqualTo("마법 데미지 +10%, INT +2/Lv, MP +5/Lv");
    }

    /**
     * 유효한 상수명("ARCHERY")이면 해당 재능을 반환함을 검증한다.
     */
    @Test
    void should_returnArchery_when_validName() {
        assertThat(TalentType.fromNameOrFallback("ARCHERY", TalentType.MELEE)).isEqualTo(TalentType.ARCHERY);
    }

    /**
     * 이름이 null이면 폴백 재능을 반환함을 검증한다.
     */
    @Test
    void should_returnFallback_when_nullName() {
        assertThat(TalentType.fromNameOrFallback(null, TalentType.MELEE)).isEqualTo(TalentType.MELEE);
    }

    /**
     * 이름이 빈 문자열이면 폴백 재능을 반환함을 검증한다.
     */
    @Test
    void should_returnFallback_when_blankName() {
        assertThat(TalentType.fromNameOrFallback("", TalentType.MELEE)).isEqualTo(TalentType.MELEE);
    }

    /**
     * 알려지지 않은 상수명이면 폴백 재능을 반환함을 검증한다.
     */
    @Test
    void should_returnFallback_when_unknownName() {
        assertThat(TalentType.fromNameOrFallback("XXX", TalentType.MELEE)).isEqualTo(TalentType.MELEE);
    }
}
