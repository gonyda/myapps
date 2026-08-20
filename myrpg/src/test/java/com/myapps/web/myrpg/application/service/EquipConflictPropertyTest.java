package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.exception.EquipConflictException;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 장비 착용 충돌/스왑 규칙 프로퍼티 테스트.
 *
 * <p>착용 대상의 {@code requiredSlots}의 어느 슬롯을 primary가 다른 장착 장비가 점유하면 {@link EquipConflictException}으로
 * 거부되고(상태 불변), 그렇지 않으면 같은 primary 장비를 해제한 뒤 대상을 착용한다(스왑). 한손검+방패는 병용, 양손검↔방패는 상호 배타, 같은 슬롯 무기·갑옷은
 * 스왑된다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 7: 장비 착용 충돌/스왑 규칙
 *
 * <p><b>Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7</b>
 */
class EquipConflictPropertyTest {

    private static final int MAX_DURABILITY = 20;

    // Feature: 006-gold-item-inventory, Property 7: 장비 착용 충돌/스왑 규칙

    /**
     * 한손검과 방패는 슬롯이 겹치지 않으므로 병용 착용이 허용됨을 검증한다.
     *
     * @param ignored 프로퍼티 실행을 100회 반복하기 위한 임의 값
     */
    @Property(tries = 100)
    void should_allowEquip_when_oneHandedSwordAndShieldCombined(
            @ForAll("smallPositive") final long ignored) {

        final OwnedItemRepository mockRepo = mock(OwnedItemRepository.class);
        final ItemCatalogService mockCatalog = mock(ItemCatalogService.class);
        final CharacterProgressRepository mockProgressRepo =
                mock(CharacterProgressRepository.class);
        final InventoryService service = createService(mockRepo, mockCatalog, mockProgressRepo);

        final OwnedItem sword = createOwnedItem(1L, "one_hand_sword", true);
        final OwnedItem shield = createOwnedItem(2L, "shield", false);

        final EquipmentItem swordItem =
                createEquipmentItem(
                        "one_hand_sword", "한손검", ItemType.WEAPON, EquipmentKind.ONE_HANDED_SWORD);
        final EquipmentItem shieldItem =
                createEquipmentItem("shield", "방패", ItemType.ARMOR, EquipmentKind.SHIELD);

        when(mockRepo.findById(2L)).thenReturn(Optional.of(shield));
        when(mockCatalog.byId("shield")).thenReturn(Optional.of(shieldItem));
        when(mockCatalog.byId("one_hand_sword")).thenReturn(Optional.of(swordItem));
        when(mockRepo.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(sword));

        service.equip(2L);

        assertThat(shield.isEquipped()).isTrue();
        assertThat(sword.isEquipped()).isTrue();
    }

    /**
     * 양손검이 장착된 상태에서 방패를 착용하면 {@link EquipConflictException}으로 거부되고 장착 상태가 불변임을 검증한다.
     *
     * @param ignored 프로퍼티 실행을 100회 반복하기 위한 임의 값
     */
    @Property(tries = 100)
    void should_throwConflict_when_twoHandedSwordEquippedAndShieldAttempted(
            @ForAll("smallPositive") final long ignored) {

        final OwnedItemRepository mockRepo = mock(OwnedItemRepository.class);
        final ItemCatalogService mockCatalog = mock(ItemCatalogService.class);
        final CharacterProgressRepository mockProgressRepo =
                mock(CharacterProgressRepository.class);
        final InventoryService service = createService(mockRepo, mockCatalog, mockProgressRepo);

        final OwnedItem twoHandSword = createOwnedItem(1L, "two_hand_sword", true);
        final OwnedItem shield = createOwnedItem(2L, "shield", false);

        final EquipmentItem twoHandItem =
                createEquipmentItem(
                        "two_hand_sword", "양손검", ItemType.WEAPON, EquipmentKind.TWO_HANDED_SWORD);
        final EquipmentItem shieldItem =
                createEquipmentItem("shield", "방패", ItemType.ARMOR, EquipmentKind.SHIELD);

        when(mockRepo.findById(2L)).thenReturn(Optional.of(shield));
        when(mockCatalog.byId("shield")).thenReturn(Optional.of(shieldItem));
        when(mockCatalog.byId("two_hand_sword")).thenReturn(Optional.of(twoHandItem));
        when(mockRepo.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(twoHandSword));

        assertThatThrownBy(() -> service.equip(2L)).isInstanceOf(EquipConflictException.class);

        assertThat(twoHandSword.isEquipped()).isTrue();
        assertThat(shield.isEquipped()).isFalse();
    }

    /**
     * 방패가 장착된 상태에서 양손검을 착용하면 {@link EquipConflictException}으로 거부되고 장착 상태가 불변임을 검증한다.
     *
     * @param ignored 프로퍼티 실행을 100회 반복하기 위한 임의 값
     */
    @Property(tries = 100)
    void should_throwConflict_when_shieldEquippedAndTwoHandedSwordAttempted(
            @ForAll("smallPositive") final long ignored) {

        final OwnedItemRepository mockRepo = mock(OwnedItemRepository.class);
        final ItemCatalogService mockCatalog = mock(ItemCatalogService.class);
        final CharacterProgressRepository mockProgressRepo =
                mock(CharacterProgressRepository.class);
        final InventoryService service = createService(mockRepo, mockCatalog, mockProgressRepo);

        final OwnedItem shield = createOwnedItem(1L, "shield", true);
        final OwnedItem twoHandSword = createOwnedItem(2L, "two_hand_sword", false);

        final EquipmentItem shieldItem =
                createEquipmentItem("shield", "방패", ItemType.ARMOR, EquipmentKind.SHIELD);
        final EquipmentItem twoHandItem =
                createEquipmentItem(
                        "two_hand_sword", "양손검", ItemType.WEAPON, EquipmentKind.TWO_HANDED_SWORD);

        when(mockRepo.findById(2L)).thenReturn(Optional.of(twoHandSword));
        when(mockCatalog.byId("two_hand_sword")).thenReturn(Optional.of(twoHandItem));
        when(mockCatalog.byId("shield")).thenReturn(Optional.of(shieldItem));
        when(mockRepo.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(shield));

        assertThatThrownBy(() -> service.equip(2L)).isInstanceOf(EquipConflictException.class);

        assertThat(shield.isEquipped()).isTrue();
        assertThat(twoHandSword.isEquipped()).isFalse();
    }

    /**
     * 같은 primary 슬롯 장비끼리(한손검→한손검) 착용 시 기존 장비가 해제되고 새 장비가 장착됨(스왑)을 검증한다.
     *
     * @param ignored 프로퍼티 실행을 100회 반복하기 위한 임의 값
     */
    @Property(tries = 100)
    void should_swapEquipment_when_samePrimarySlotWeapon(
            @ForAll("smallPositive") final long ignored) {

        final OwnedItemRepository mockRepo = mock(OwnedItemRepository.class);
        final ItemCatalogService mockCatalog = mock(ItemCatalogService.class);
        final CharacterProgressRepository mockProgressRepo =
                mock(CharacterProgressRepository.class);
        final InventoryService service = createService(mockRepo, mockCatalog, mockProgressRepo);

        final OwnedItem swordA = createOwnedItem(1L, "sword_a", true);
        final OwnedItem swordB = createOwnedItem(2L, "sword_b", false);

        final EquipmentItem swordAItem =
                createEquipmentItem(
                        "sword_a", "한손검A", ItemType.WEAPON, EquipmentKind.ONE_HANDED_SWORD);
        final EquipmentItem swordBItem =
                createEquipmentItem(
                        "sword_b", "한손검B", ItemType.WEAPON, EquipmentKind.ONE_HANDED_SWORD);

        when(mockRepo.findById(2L)).thenReturn(Optional.of(swordB));
        when(mockCatalog.byId("sword_b")).thenReturn(Optional.of(swordBItem));
        when(mockCatalog.byId("sword_a")).thenReturn(Optional.of(swordAItem));
        when(mockRepo.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(swordA));

        service.equip(2L);

        assertThat(swordA.isEquipped()).isFalse();
        assertThat(swordB.isEquipped()).isTrue();
    }

    /**
     * 같은 primary 슬롯 장비끼리(갑옷→갑옷) 착용 시 기존 갑옷이 해제되고 새 갑옷이 장착됨(스왑)을 검증한다.
     *
     * @param ignored 프로퍼티 실행을 100회 반복하기 위한 임의 값
     */
    @Property(tries = 100)
    void should_swapEquipment_when_samePrimarySlotArmor(
            @ForAll("smallPositive") final long ignored) {

        final OwnedItemRepository mockRepo = mock(OwnedItemRepository.class);
        final ItemCatalogService mockCatalog = mock(ItemCatalogService.class);
        final CharacterProgressRepository mockProgressRepo =
                mock(CharacterProgressRepository.class);
        final InventoryService service = createService(mockRepo, mockCatalog, mockProgressRepo);

        final OwnedItem armorA = createOwnedItem(1L, "armor_a", true);
        final OwnedItem armorB = createOwnedItem(2L, "armor_b", false);

        final EquipmentItem armorAItem =
                createEquipmentItem("armor_a", "갑옷A", ItemType.ARMOR, EquipmentKind.ARMOR_BODY);
        final EquipmentItem armorBItem =
                createEquipmentItem("armor_b", "갑옷B", ItemType.ARMOR, EquipmentKind.ARMOR_BODY);

        when(mockRepo.findById(2L)).thenReturn(Optional.of(armorB));
        when(mockCatalog.byId("armor_b")).thenReturn(Optional.of(armorBItem));
        when(mockCatalog.byId("armor_a")).thenReturn(Optional.of(armorAItem));
        when(mockRepo.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(armorA));

        service.equip(2L);

        assertThat(armorA.isEquipped()).isFalse();
        assertThat(armorB.isEquipped()).isTrue();
    }

    /**
     * 갑옷은 무기/방패와 슬롯이 겹치지 않으므로 독립적으로 착용됨을 검증한다.
     *
     * @param ignored 프로퍼티 실행을 100회 반복하기 위한 임의 값
     */
    @Property(tries = 100)
    void should_allowArmorIndependently_when_weaponAndShieldEquipped(
            @ForAll("smallPositive") final long ignored) {

        final OwnedItemRepository mockRepo = mock(OwnedItemRepository.class);
        final ItemCatalogService mockCatalog = mock(ItemCatalogService.class);
        final CharacterProgressRepository mockProgressRepo =
                mock(CharacterProgressRepository.class);
        final InventoryService service = createService(mockRepo, mockCatalog, mockProgressRepo);

        final OwnedItem sword = createOwnedItem(1L, "sword", true);
        final OwnedItem shield = createOwnedItem(2L, "shield", true);
        final OwnedItem armor = createOwnedItem(3L, "armor", false);

        final EquipmentItem swordItem =
                createEquipmentItem(
                        "sword", "한손검", ItemType.WEAPON, EquipmentKind.ONE_HANDED_SWORD);
        final EquipmentItem shieldItem =
                createEquipmentItem("shield", "방패", ItemType.ARMOR, EquipmentKind.SHIELD);
        final EquipmentItem armorItem =
                createEquipmentItem("armor", "갑옷", ItemType.ARMOR, EquipmentKind.ARMOR_BODY);

        when(mockRepo.findById(3L)).thenReturn(Optional.of(armor));
        when(mockCatalog.byId("armor")).thenReturn(Optional.of(armorItem));
        when(mockCatalog.byId("sword")).thenReturn(Optional.of(swordItem));
        when(mockCatalog.byId("shield")).thenReturn(Optional.of(shieldItem));
        when(mockRepo.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(sword, shield));

        service.equip(3L);

        assertThat(sword.isEquipped()).isTrue();
        assertThat(shield.isEquipped()).isTrue();
        assertThat(armor.isEquipped()).isTrue();
    }

    /**
     * primary가 다른 장비가 슬롯을 점유하여 충돌이 발생하면 장착 상태가 일체 불변임을 검증한다. 다양한 충돌 시나리오(양손검 장착 후 방패, 방패 장착 후 양손검)를
     * 임의 조합으로 실행한다.
     *
     * @param scenario 충돌 시나리오 인덱스 (0=양손검→방패, 1=방패→양손검)
     */
    @Property(tries = 100)
    void should_keepStateUnchanged_when_equipConflictOccurs(
            @ForAll("conflictScenario") final int scenario) {

        final OwnedItemRepository mockRepo = mock(OwnedItemRepository.class);
        final ItemCatalogService mockCatalog = mock(ItemCatalogService.class);
        final CharacterProgressRepository mockProgressRepo =
                mock(CharacterProgressRepository.class);
        final InventoryService service = createService(mockRepo, mockCatalog, mockProgressRepo);

        final OwnedItem equippedItem;
        final OwnedItem targetItem;
        final EquipmentItem equippedCatalog;
        final EquipmentItem targetCatalog;

        if (scenario == 0) {
            equippedItem = createOwnedItem(1L, "two_hand_sword", true);
            targetItem = createOwnedItem(2L, "shield", false);
            equippedCatalog =
                    createEquipmentItem(
                            "two_hand_sword",
                            "양손검",
                            ItemType.WEAPON,
                            EquipmentKind.TWO_HANDED_SWORD);
            targetCatalog =
                    createEquipmentItem("shield", "방패", ItemType.ARMOR, EquipmentKind.SHIELD);
        } else {
            equippedItem = createOwnedItem(1L, "shield", true);
            targetItem = createOwnedItem(2L, "two_hand_sword", false);
            equippedCatalog =
                    createEquipmentItem("shield", "방패", ItemType.ARMOR, EquipmentKind.SHIELD);
            targetCatalog =
                    createEquipmentItem(
                            "two_hand_sword",
                            "양손검",
                            ItemType.WEAPON,
                            EquipmentKind.TWO_HANDED_SWORD);
        }

        when(mockRepo.findById(2L)).thenReturn(Optional.of(targetItem));
        when(mockCatalog.byId(targetItem.getItemId())).thenReturn(Optional.of(targetCatalog));
        when(mockCatalog.byId(equippedItem.getItemId())).thenReturn(Optional.of(equippedCatalog));
        when(mockRepo.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(equippedItem));

        final boolean equippedBefore = equippedItem.isEquipped();
        final boolean targetBefore = targetItem.isEquipped();

        assertThatThrownBy(() -> service.equip(2L)).isInstanceOf(EquipConflictException.class);

        assertThat(equippedItem.isEquipped()).isEqualTo(equippedBefore);
        assertThat(targetItem.isEquipped()).isEqualTo(targetBefore);
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 양의 정수를 생성하는 Arbitrary 제공자 (프로퍼티 반복용).
     *
     * @return 1~1000 범위의 long Arbitrary
     */
    @Provide
    Arbitrary<Long> smallPositive() {
        return Arbitraries.longs().between(1L, 1000L);
    }

    /**
     * 충돌 시나리오 인덱스(0 또는 1)를 생성하는 Arbitrary 제공자.
     *
     * @return 0~1 범위의 int Arbitrary
     */
    @Provide
    Arbitrary<Integer> conflictScenario() {
        return Arbitraries.integers().between(0, 1);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /**
     * {@link InventoryService}를 모의 의존성으로 생성한다.
     *
     * @param repo 모의 OwnedItemRepository
     * @param catalog 모의 ItemCatalogService
     * @param progressRepo 모의 CharacterProgressRepository
     * @return InventoryService 인스턴스
     */
    private InventoryService createService(
            final OwnedItemRepository repo,
            final ItemCatalogService catalog,
            final CharacterProgressRepository progressRepo) {
        return new InventoryService(
                repo,
                catalog,
                progressRepo,
                mock(com.myapps.web.myrpg.domain.model.StatProgression.class),
                mock(com.myapps.web.myrpg.domain.model.ActionLog.class),
                mock(com.myapps.web.myrpg.application.service.SkillCatalogService.class),
                mock(com.myapps.web.myrpg.domain.repository.CharacterSkillRepository.class));
    }

    /**
     * 지정된 ID와 장착 상태를 가진 {@link OwnedItem}을 생성한다.
     *
     * @param id 엔티티 ID
     * @param itemId 아이템 카탈로그 ID
     * @param equipped 장착 여부
     * @return 설정된 OwnedItem 인스턴스
     */
    private OwnedItem createOwnedItem(final long id, final String itemId, final boolean equipped) {
        final OwnedItem item =
                new OwnedItem(itemId, 1, StorageKind.INVENTORY, equipped, MAX_DURABILITY);
        setId(item, id);
        return item;
    }

    /**
     * 지정된 속성을 가진 {@link EquipmentItem}을 생성한다.
     *
     * @param id 아이템 ID
     * @param name 아이템 이름
     * @param type 아이템 유형
     * @param kind 장비 종류
     * @return EquipmentItem 인스턴스
     */
    private EquipmentItem createEquipmentItem(
            final String id, final String name, final ItemType type, final EquipmentKind kind) {
        return new EquipmentItem(
                id,
                name,
                type,
                kind,
                List.of(new EquipBonus(BonusTarget.STR, 5)),
                null,
                MAX_DURABILITY);
    }

    /**
     * 리플렉션을 이용하여 OwnedItem의 ID를 설정한다.
     *
     * @param item 대상 엔티티
     * @param id 설정할 ID 값
     */
    private void setId(final OwnedItem item, final Long id) {
        try {
            final Field idField = OwnedItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(item, id);
        } catch (final NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException("OwnedItem ID 설정 실패", exception);
        }
    }
}
