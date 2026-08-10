package com.myapps.web.myrpg.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ItemType}의 코드·라벨·상수값과 {@code fromString}/{@code isEquipment}를
 * 검증하는 단위 테스트.
 *
 * <p><b>Validates: Requirements 6.1</b>
 */
class ItemTypeTest {

    /**
     * POTION의 code는 "potion", label은 "포션"임을 검증한다.
     */
    @Test
    void should_returnCorrectCodeAndLabel_when_potion() {
        assertThat(ItemType.POTION.code()).isEqualTo("potion");
        assertThat(ItemType.POTION.label()).isEqualTo("포션");
    }

    /**
     * WEAPON의 code는 "weapon", label은 "무기"임을 검증한다.
     */
    @Test
    void should_returnCorrectCodeAndLabel_when_weapon() {
        assertThat(ItemType.WEAPON.code()).isEqualTo("weapon");
        assertThat(ItemType.WEAPON.label()).isEqualTo("무기");
    }

    /**
     * ARMOR의 code는 "armor", label은 "방어구"임을 검증한다.
     */
    @Test
    void should_returnCorrectCodeAndLabel_when_armor() {
        assertThat(ItemType.ARMOR.code()).isEqualTo("armor");
        assertThat(ItemType.ARMOR.label()).isEqualTo("방어구");
    }

    /**
     * POTION은 장비가 아님을 검증한다.
     */
    @Test
    void should_returnFalse_when_isEquipmentForPotion() {
        assertThat(ItemType.POTION.isEquipment()).isFalse();
    }

    /**
     * WEAPON은 장비임을 검증한다.
     */
    @Test
    void should_returnTrue_when_isEquipmentForWeapon() {
        assertThat(ItemType.WEAPON.isEquipment()).isTrue();
    }

    /**
     * ARMOR는 장비임을 검증한다.
     */
    @Test
    void should_returnTrue_when_isEquipmentForArmor() {
        assertThat(ItemType.ARMOR.isEquipment()).isTrue();
    }

    /**
     * 유효한 코드 "potion"으로 fromString 시 POTION을 반환함을 검증한다.
     */
    @Test
    void should_returnPotion_when_fromStringWithPotion() {
        final Optional<ItemType> result = ItemType.fromString("potion");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(ItemType.POTION);
    }

    /**
     * 유효한 코드 "weapon"으로 fromString 시 WEAPON을 반환함을 검증한다.
     */
    @Test
    void should_returnWeapon_when_fromStringWithWeapon() {
        final Optional<ItemType> result = ItemType.fromString("weapon");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(ItemType.WEAPON);
    }

    /**
     * 유효한 코드 "armor"로 fromString 시 ARMOR를 반환함을 검증한다.
     */
    @Test
    void should_returnArmor_when_fromStringWithArmor() {
        final Optional<ItemType> result = ItemType.fromString("armor");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(ItemType.ARMOR);
    }

    /**
     * 미지 코드에 대해 fromString은 빈 Optional을 반환함을 검증한다.
     */
    @Test
    void should_returnEmpty_when_fromStringWithUnknownCode() {
        assertThat(ItemType.fromString("unknown")).isEmpty();
        assertThat(ItemType.fromString("sword")).isEmpty();
        assertThat(ItemType.fromString("")).isEmpty();
    }

    /**
     * ItemType 상수가 정확히 3개임을 검증한다.
     */
    @Test
    void should_haveExactlyThreeConstants() {
        assertThat(ItemType.values()).hasSize(3);
    }
}
