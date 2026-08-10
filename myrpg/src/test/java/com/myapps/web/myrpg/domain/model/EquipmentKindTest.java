package com.myapps.web.myrpg.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link EquipmentKind}의 라벨·슬롯·상수값과 {@code fromString}을 검증하는 단위 테스트.
 *
 * <p><b>Validates: Requirements 6.3</b>
 */
class EquipmentKindTest {

    /**
     * ONE_HANDED_SWORD의 라벨은 "한손검", primarySlot은 MAIN_HAND,
     * requiredSlots는 {MAIN_HAND}임을 검증한다.
     */
    @Test
    void should_returnCorrectProperties_when_oneHandedSword() {
        final EquipmentKind kind = EquipmentKind.ONE_HANDED_SWORD;

        assertThat(kind.label()).isEqualTo("한손검");
        assertThat(kind.primarySlot()).isEqualTo(EquipSlot.MAIN_HAND);
        assertThat(kind.requiredSlots()).isEqualTo(Set.of(EquipSlot.MAIN_HAND));
    }

    /**
     * TWO_HANDED_SWORD의 라벨은 "양손검", primarySlot은 MAIN_HAND,
     * requiredSlots는 {MAIN_HAND, OFF_HAND}임을 검증한다.
     */
    @Test
    void should_returnCorrectProperties_when_twoHandedSword() {
        final EquipmentKind kind = EquipmentKind.TWO_HANDED_SWORD;

        assertThat(kind.label()).isEqualTo("양손검");
        assertThat(kind.primarySlot()).isEqualTo(EquipSlot.MAIN_HAND);
        assertThat(kind.requiredSlots()).isEqualTo(Set.of(EquipSlot.MAIN_HAND, EquipSlot.OFF_HAND));
    }

    /**
     * SHIELD의 라벨은 "방패", primarySlot은 OFF_HAND,
     * requiredSlots는 {OFF_HAND}임을 검증한다.
     */
    @Test
    void should_returnCorrectProperties_when_shield() {
        final EquipmentKind kind = EquipmentKind.SHIELD;

        assertThat(kind.label()).isEqualTo("방패");
        assertThat(kind.primarySlot()).isEqualTo(EquipSlot.OFF_HAND);
        assertThat(kind.requiredSlots()).isEqualTo(Set.of(EquipSlot.OFF_HAND));
    }

    /**
     * ARMOR_BODY의 라벨은 "갑옷", primarySlot은 BODY,
     * requiredSlots는 {BODY}임을 검증한다.
     */
    @Test
    void should_returnCorrectProperties_when_armorBody() {
        final EquipmentKind kind = EquipmentKind.ARMOR_BODY;

        assertThat(kind.label()).isEqualTo("갑옷");
        assertThat(kind.primarySlot()).isEqualTo(EquipSlot.BODY);
        assertThat(kind.requiredSlots()).isEqualTo(Set.of(EquipSlot.BODY));
    }

    /**
     * 유효한 코드 "ONE_HANDED_SWORD"(대소문자 무시)로 fromString 시 해당 상수를 반환함을 검증한다.
     */
    @Test
    void should_returnOneHandedSword_when_fromStringWithValidCode() {
        assertThat(EquipmentKind.fromString("ONE_HANDED_SWORD")).isPresent()
                .contains(EquipmentKind.ONE_HANDED_SWORD);
        assertThat(EquipmentKind.fromString("one_handed_sword")).isPresent()
                .contains(EquipmentKind.ONE_HANDED_SWORD);
    }

    /**
     * 유효한 코드 "TWO_HANDED_SWORD"로 fromString 시 해당 상수를 반환함을 검증한다.
     */
    @Test
    void should_returnTwoHandedSword_when_fromStringWithValidCode() {
        assertThat(EquipmentKind.fromString("TWO_HANDED_SWORD")).isPresent()
                .contains(EquipmentKind.TWO_HANDED_SWORD);
        assertThat(EquipmentKind.fromString("two_handed_sword")).isPresent()
                .contains(EquipmentKind.TWO_HANDED_SWORD);
    }

    /**
     * 유효한 코드 "SHIELD"로 fromString 시 해당 상수를 반환함을 검증한다.
     */
    @Test
    void should_returnShield_when_fromStringWithValidCode() {
        assertThat(EquipmentKind.fromString("SHIELD")).isPresent()
                .contains(EquipmentKind.SHIELD);
        assertThat(EquipmentKind.fromString("shield")).isPresent()
                .contains(EquipmentKind.SHIELD);
    }

    /**
     * 유효한 코드 "ARMOR_BODY"로 fromString 시 해당 상수를 반환함을 검증한다.
     */
    @Test
    void should_returnArmorBody_when_fromStringWithValidCode() {
        assertThat(EquipmentKind.fromString("ARMOR_BODY")).isPresent()
                .contains(EquipmentKind.ARMOR_BODY);
        assertThat(EquipmentKind.fromString("armor_body")).isPresent()
                .contains(EquipmentKind.ARMOR_BODY);
    }

    /**
     * 미지 코드에 대해 fromString은 빈 Optional을 반환함을 검증한다.
     */
    @Test
    void should_returnEmpty_when_fromStringWithUnknownCode() {
        assertThat(EquipmentKind.fromString("unknown")).isEmpty();
        assertThat(EquipmentKind.fromString("longsword")).isEmpty();
        assertThat(EquipmentKind.fromString("")).isEmpty();
    }

    /**
     * null 입력에 대해 fromString은 빈 Optional을 반환함을 검증한다.
     */
    @Test
    void should_returnEmpty_when_fromStringWithNull() {
        assertThat(EquipmentKind.fromString(null)).isEmpty();
    }

    /**
     * EquipmentKind 상수가 정확히 4개임을 검증한다.
     */
    @Test
    void should_haveExactlyFourConstants() {
        assertThat(EquipmentKind.values()).hasSize(4);
    }
}
