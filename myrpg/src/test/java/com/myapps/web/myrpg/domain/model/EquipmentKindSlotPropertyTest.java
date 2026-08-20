package com.myapps.web.myrpg.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * {@link EquipmentKind}의 슬롯 정의가 정합한지 검증하는 프로퍼티 테스트.
 *
 * <p>Feature: 006-gold-item-inventory, Property 6: 장비 슬롯 정의 정합
 *
 * <p><b>Validates: Requirements 6.3</b>
 */
class EquipmentKindSlotPropertyTest {

    /**
     * 모든 {@link EquipmentKind}에 대해, {@code requiredSlots}는 {@code primarySlot}을 포함한다.
     *
     * @param kind 임의의 EquipmentKind 열거 상수
     */
    @Property(tries = 100)
    void should_containPrimarySlotInRequiredSlots_forAllKinds(
            @ForAll("equipmentKinds") final EquipmentKind kind) {
        final Set<EquipSlot> requiredSlots = kind.requiredSlots();
        final EquipSlot primarySlot = kind.primarySlot();

        assertThat(requiredSlots).contains(primarySlot);
    }

    /**
     * 양손 무기(양손검·활·스태프)만 {@code requiredSlots.size() == 2}(MAIN_HAND+OFF_HAND)이며, 나머지는 단일 슬롯(size ==
     * 1)이다.
     *
     * @param kind 임의의 EquipmentKind 열거 상수
     */
    @Property(tries = 100)
    void should_haveTwoSlotsForTwoHandedWeapons(
            @ForAll("equipmentKinds") final EquipmentKind kind) {
        final Set<EquipmentKind> twoHandedKinds =
                Set.of(EquipmentKind.TWO_HANDED_SWORD, EquipmentKind.BOW, EquipmentKind.STAFF);
        final int slotCount = kind.requiredSlots().size();

        if (twoHandedKinds.contains(kind)) {
            assertThat(slotCount).isEqualTo(2);
            assertThat(kind.requiredSlots())
                    .containsExactlyInAnyOrder(EquipSlot.MAIN_HAND, EquipSlot.OFF_HAND);
        } else {
            assertThat(slotCount).isEqualTo(1);
        }
    }

    /**
     * 모든 {@link EquipmentKind}에 대해, {@code primarySlot}이 정의대로인지 검증한다. 근접/원거리 무기=MAIN_HAND,
     * 방패=OFF_HAND, 갑옷=BODY, 투구=HEAD, 장갑=HANDS, 부츠=FEET.
     *
     * @param kind 임의의 EquipmentKind 열거 상수
     */
    @Property(tries = 100)
    void should_havePrimarySlotMatchingDefinition(
            @ForAll("equipmentKinds") final EquipmentKind kind) {
        switch (kind) {
            case ONE_HANDED_SWORD, TWO_HANDED_SWORD, BOW, WAND, STAFF ->
                    assertThat(kind.primarySlot()).isEqualTo(EquipSlot.MAIN_HAND);
            case SHIELD -> assertThat(kind.primarySlot()).isEqualTo(EquipSlot.OFF_HAND);
            case ARMOR_BODY -> assertThat(kind.primarySlot()).isEqualTo(EquipSlot.BODY);
            case HELMET -> assertThat(kind.primarySlot()).isEqualTo(EquipSlot.HEAD);
            case GLOVES -> assertThat(kind.primarySlot()).isEqualTo(EquipSlot.HANDS);
            case BOOTS -> assertThat(kind.primarySlot()).isEqualTo(EquipSlot.FEET);
        }
    }

    /**
     * 모든 {@link EquipmentKind} 상수를 균등하게 선택하는 Arbitrary 제공자.
     *
     * @return EquipmentKind 상수 중 하나를 선택하는 Arbitrary
     */
    @Provide
    Arbitrary<EquipmentKind> equipmentKinds() {
        return Arbitraries.of(EquipmentKind.values());
    }
}
