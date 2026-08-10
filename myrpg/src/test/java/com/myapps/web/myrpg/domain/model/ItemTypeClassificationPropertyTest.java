package com.myapps.web.myrpg.domain.model;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ItemType}의 분류 및 파싱이 정확한지 검증하는 프로퍼티 테스트.
 *
 * <p>Feature: 006-gold-item-inventory, Property 5: 아이템 타입 분류 및 파싱
 *
 * <p><b>Validates: Requirements 6.1</b>
 */
class ItemTypeClassificationPropertyTest {

    private static final Set<ItemType> EQUIPMENT_TYPES = Set.of(ItemType.WEAPON, ItemType.ARMOR);

    /**
     * 모든 {@link ItemType}에 대해, {@code fromString(code())}는 해당 enum 상수를 반환한다.
     *
     * @param type 임의의 ItemType 열거 상수
     */
    @Property(tries = 100)
    void should_returnMatchingEnum_when_fromStringWithValidCode(@ForAll("itemTypes") final ItemType type) {
        final Optional<ItemType> result = ItemType.fromString(type.code());

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(type);
    }

    /**
     * 미지 코드에 대해, {@code fromString}은 빈 {@link Optional}을 반환한다.
     *
     * @param unknownCode 유효하지 않은 임의의 문자열 코드
     */
    @Property(tries = 100)
    void should_returnEmpty_when_fromStringWithUnknownCode(@ForAll("unknownCodes") final String unknownCode) {
        final Optional<ItemType> result = ItemType.fromString(unknownCode);

        assertThat(result).isEmpty();
    }

    /**
     * 모든 {@link ItemType}에 대해, {@code isEquipment()}는 WEAPON 또는 ARMOR에서만 참이다.
     *
     * @param type 임의의 ItemType 열거 상수
     */
    @Property(tries = 100)
    void should_returnTrueOnlyForWeaponOrArmor_when_isEquipment(@ForAll("itemTypes") final ItemType type) {
        if (EQUIPMENT_TYPES.contains(type)) {
            assertThat(type.isEquipment()).isTrue();
        } else {
            assertThat(type.isEquipment()).isFalse();
        }
    }

    /**
     * 모든 {@link ItemType} 상수를 균등하게 선택하는 Arbitrary 제공자.
     *
     * @return ItemType 3종 중 하나를 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<ItemType> itemTypes() {
        return Arbitraries.of(ItemType.values());
    }

    /**
     * 유효한 ItemType 코드와 겹치지 않는 임의의 문자열을 생성하는 Arbitrary 제공자.
     *
     * @return 미지 코드 문자열을 생성하는 Arbitrary
     */
    @Provide
    Arbitrary<String> unknownCodes() {
        final Set<String> validCodes = Set.of("potion", "weapon", "armor");
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                .filter(code -> !validCodes.contains(code));
    }
}
