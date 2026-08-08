package com.myapps.web.myrpg.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link VitalMax} 델타 헬퍼의 불변성과 {@link TalentBonus} 생성/접근자를 검증하는 단위 테스트.
 *
 * <p>각 델타 헬퍼({@code withHpDelta}/{@code withMpDelta}/{@code withStaminaDelta})가
 * 원본을 변경하지 않고 대상 필드만 변경한 새 인스턴스를 반환하는지(음수 델타 포함) 확인하고,
 * {@code TalentBonus}의 생성자와 {@code target()}/{@code perLevel()} 접근자를 검증한다.
 *
 * <p><b>Validates: Requirements 8.1, 11.2</b>
 */
class VitalMaxTest {

    /**
     * withHpDelta는 HP만 증가시킨 새 인스턴스를 반환하고 원본은 불변임을 검증한다.
     */
    @Test
    void should_returnNewInstanceWithOnlyHpChanged_when_withHpDelta() {
        final VitalMax original = new VitalMax(100, 50, 30);

        final VitalMax result = original.withHpDelta(20);

        assertThat(result).isNotSameAs(original);
        assertThat(result.hp()).isEqualTo(120);
        assertThat(result.mp()).isEqualTo(50);
        assertThat(result.stamina()).isEqualTo(30);
        assertThat(original).isEqualTo(new VitalMax(100, 50, 30));
    }

    /**
     * withMpDelta는 MP만 증가시킨 새 인스턴스를 반환하고 원본은 불변임을 검증한다.
     */
    @Test
    void should_returnNewInstanceWithOnlyMpChanged_when_withMpDelta() {
        final VitalMax original = new VitalMax(100, 50, 30);

        final VitalMax result = original.withMpDelta(15);

        assertThat(result).isNotSameAs(original);
        assertThat(result.hp()).isEqualTo(100);
        assertThat(result.mp()).isEqualTo(65);
        assertThat(result.stamina()).isEqualTo(30);
        assertThat(original).isEqualTo(new VitalMax(100, 50, 30));
    }

    /**
     * withStaminaDelta는 Stamina만 증가시킨 새 인스턴스를 반환하고 원본은 불변임을 검증한다.
     */
    @Test
    void should_returnNewInstanceWithOnlyStaminaChanged_when_withStaminaDelta() {
        final VitalMax original = new VitalMax(100, 50, 30);

        final VitalMax result = original.withStaminaDelta(25);

        assertThat(result).isNotSameAs(original);
        assertThat(result.hp()).isEqualTo(100);
        assertThat(result.mp()).isEqualTo(50);
        assertThat(result.stamina()).isEqualTo(55);
        assertThat(original).isEqualTo(new VitalMax(100, 50, 30));
    }

    /**
     * 음수 델타에 대해서도 대상 필드만 감소한 새 인스턴스를 반환하고 원본은 불변임을 검증한다.
     */
    @Test
    void should_returnNewInstanceWithTargetDecreased_when_negativeDelta() {
        final VitalMax original = new VitalMax(100, 50, 30);

        final VitalMax result = original.withHpDelta(-40);

        assertThat(result).isNotSameAs(original);
        assertThat(result.hp()).isEqualTo(60);
        assertThat(result.mp()).isEqualTo(50);
        assertThat(result.stamina()).isEqualTo(30);
        assertThat(original).isEqualTo(new VitalMax(100, 50, 30));
    }

    /**
     * TalentBonus는 STAT 계열 대상(STR)과 레벨당 증가치를 접근자로 그대로 반환함을 검증한다.
     */
    @Test
    void should_exposeTargetAndPerLevel_when_statTalentBonus() {
        final TalentBonus bonus = new TalentBonus(BonusTarget.STR, 2);

        assertThat(bonus.target()).isEqualTo(BonusTarget.STR);
        assertThat(bonus.perLevel()).isEqualTo(2);
    }

    /**
     * TalentBonus는 VITAL 계열 대상(HP)과 레벨당 증가치를 접근자로 그대로 반환함을 검증한다.
     */
    @Test
    void should_exposeTargetAndPerLevel_when_vitalTalentBonus() {
        final TalentBonus bonus = new TalentBonus(BonusTarget.HP, 5);

        assertThat(bonus.target()).isEqualTo(BonusTarget.HP);
        assertThat(bonus.perLevel()).isEqualTo(5);
    }
}
