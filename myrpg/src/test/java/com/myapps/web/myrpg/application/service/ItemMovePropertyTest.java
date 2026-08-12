package com.myapps.web.myrpg.application.service;

import java.util.List;
import java.util.Optional;

import com.myapps.web.myrpg.application.exception.EquipConflictException;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.PotionItem;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 맡기기/찾기 저장위치 전환 프로퍼티 테스트.
 *
 * <p>{@code moveToBank}는 {@code storage}를 INVENTORY→BANK로,
 * {@code moveToInventory}는 BANK→INVENTORY로 전환하고,
 * 장착 중({@code equipped=true}) 장비의 맡기기는 거부되어 상태가 불변이다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 11: 맡기기/찾기 저장위치 전환
 *
 * <p><b>Validates: Requirements 15.5, 15.6, 15.7</b>
 */
class ItemMovePropertyTest {

    private static final String WEAPON_ITEM_ID = "beginner_one_hand_sword";
    private static final String POTION_ITEM_ID = "hp_potion_50";

    // Feature: 006-gold-item-inventory, Property 11: 맡기기/찾기 저장위치 전환

    /**
     * moveToBank 호출 시 장비의 storage가 INVENTORY→BANK로 전환됨을 검증한다.
     *
     * @param kind 장비 종류
     */
    @Property(tries = 100)
    void should_changeStorageToBank_when_moveToBankCalledOnEquipment(
            @ForAll("equipmentKind") final EquipmentKind kind) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);

        final InventoryService inventoryService = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository, statProgression,
                mock(com.myapps.web.myrpg.domain.model.ActionLog.class),
                mock(com.myapps.web.myrpg.application.service.SkillCatalogService.class),
                mock(com.myapps.web.myrpg.domain.repository.CharacterSkillRepository.class));

        final ItemType equipType = (kind == EquipmentKind.SHIELD || kind == EquipmentKind.ARMOR_BODY)
                ? ItemType.ARMOR : ItemType.WEAPON;
        final EquipmentItem equipCatalog = new EquipmentItem(
                WEAPON_ITEM_ID, "초보자 장비", equipType, kind, List.of(), null, 20);

        final OwnedItem source = new OwnedItem(
                WEAPON_ITEM_ID, 1, StorageKind.INVENTORY, false, 20.0);
        setOwnedItemId(source, 1L);

        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(source));
        when(itemCatalogService.byId(WEAPON_ITEM_ID)).thenReturn(Optional.of(equipCatalog));
        when(ownedItemRepository.countByStorage(StorageKind.BANK)).thenReturn(10L);

        inventoryService.moveToBank(1L);

        assertThat(source.getStorage()).isEqualTo(StorageKind.BANK);
    }

    /**
     * moveToInventory 호출 시 장비의 storage가 BANK→INVENTORY로 전환됨을 검증한다.
     *
     * @param kind 장비 종류
     */
    @Property(tries = 100)
    void should_changeStorageToInventory_when_moveToInventoryCalledOnEquipment(
            @ForAll("equipmentKind") final EquipmentKind kind) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);

        final InventoryService inventoryService = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository, statProgression,
                mock(com.myapps.web.myrpg.domain.model.ActionLog.class),
                mock(com.myapps.web.myrpg.application.service.SkillCatalogService.class),
                mock(com.myapps.web.myrpg.domain.repository.CharacterSkillRepository.class));

        final ItemType equipType = (kind == EquipmentKind.SHIELD || kind == EquipmentKind.ARMOR_BODY)
                ? ItemType.ARMOR : ItemType.WEAPON;
        final EquipmentItem equipCatalog = new EquipmentItem(
                WEAPON_ITEM_ID, "초보자 장비", equipType, kind, List.of(), null, 20);

        final OwnedItem source = new OwnedItem(
                WEAPON_ITEM_ID, 1, StorageKind.BANK, false, 20.0);
        setOwnedItemId(source, 1L);

        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(source));
        when(itemCatalogService.byId(WEAPON_ITEM_ID)).thenReturn(Optional.of(equipCatalog));
        when(ownedItemRepository.countByStorage(StorageKind.INVENTORY)).thenReturn(10L);

        inventoryService.moveToInventory(1L);

        assertThat(source.getStorage()).isEqualTo(StorageKind.INVENTORY);
    }

    /**
     * 장착 중(equipped=true) 장비의 moveToBank 호출 시
     * {@link EquipConflictException}으로 거부되고 storage가 불변임을 검증한다.
     *
     * @param kind 장비 종류
     */
    @Property(tries = 100)
    void should_throwAndKeepStateUnchanged_when_moveToBankOnEquippedItem(
            @ForAll("equipmentKind") final EquipmentKind kind) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);

        final InventoryService inventoryService = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository, statProgression,
                mock(com.myapps.web.myrpg.domain.model.ActionLog.class),
                mock(com.myapps.web.myrpg.application.service.SkillCatalogService.class),
                mock(com.myapps.web.myrpg.domain.repository.CharacterSkillRepository.class));

        final ItemType equipType = (kind == EquipmentKind.SHIELD || kind == EquipmentKind.ARMOR_BODY)
                ? ItemType.ARMOR : ItemType.WEAPON;
        final EquipmentItem equipCatalog = new EquipmentItem(
                WEAPON_ITEM_ID, "초보자 장비", equipType, kind, List.of(), null, 20);

        // equipped = true → 장착 중
        final OwnedItem equippedItem = new OwnedItem(
                WEAPON_ITEM_ID, 1, StorageKind.INVENTORY, true, 20.0);
        setOwnedItemId(equippedItem, 1L);

        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(equippedItem));
        when(itemCatalogService.byId(WEAPON_ITEM_ID)).thenReturn(Optional.of(equipCatalog));

        assertThatThrownBy(() -> inventoryService.moveToBank(1L))
                .isInstanceOf(EquipConflictException.class);

        // 상태 불변
        assertThat(equippedItem.getStorage()).isEqualTo(StorageKind.INVENTORY);
        assertThat(equippedItem.isEquipped()).isTrue();
    }

    /**
     * 소비형(POTION) 아이템의 moveToBank 호출 시 storage가 전환됨을 검증한다.
     * 대상에 기존 스택이 없는 경우 저장위치가 BANK로 변경된다.
     *
     * @param quantity 소비형 아이템 수량
     */
    @Property(tries = 100)
    void should_changeStorageToBank_when_potionMovedWithoutExistingStack(
            @ForAll("potionQuantity") final int quantity) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);

        final InventoryService inventoryService = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository, statProgression,
                mock(com.myapps.web.myrpg.domain.model.ActionLog.class),
                mock(com.myapps.web.myrpg.application.service.SkillCatalogService.class),
                mock(com.myapps.web.myrpg.domain.repository.CharacterSkillRepository.class));

        final PotionItem potionCatalog = new PotionItem(POTION_ITEM_ID, "HP 포션", 50, 30);

        final OwnedItem source = new OwnedItem(
                POTION_ITEM_ID, quantity, StorageKind.INVENTORY, false, 0);
        setOwnedItemId(source, 1L);

        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(source));
        when(itemCatalogService.byId(POTION_ITEM_ID)).thenReturn(Optional.of(potionCatalog));
        when(ownedItemRepository.findByStorageAndItemId(StorageKind.BANK, POTION_ITEM_ID))
                .thenReturn(Optional.empty());
        when(ownedItemRepository.countByStorage(StorageKind.BANK)).thenReturn(10L);

        inventoryService.moveToBank(1L);

        assertThat(source.getStorage()).isEqualTo(StorageKind.BANK);
        assertThat(source.getQuantity()).isEqualTo(quantity);
    }

    /**
     * 소비형(POTION) 아이템의 moveToInventory 호출 시
     * 기존 스택이 없으면 storage가 INVENTORY로 전환됨을 검증한다.
     *
     * @param quantity 소비형 아이템 수량
     */
    @Property(tries = 100)
    void should_changeStorageToInventory_when_potionMovedFromBankWithoutExistingStack(
            @ForAll("potionQuantity") final int quantity) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);

        final InventoryService inventoryService = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository, statProgression,
                mock(com.myapps.web.myrpg.domain.model.ActionLog.class),
                mock(com.myapps.web.myrpg.application.service.SkillCatalogService.class),
                mock(com.myapps.web.myrpg.domain.repository.CharacterSkillRepository.class));

        final PotionItem potionCatalog = new PotionItem(POTION_ITEM_ID, "HP 포션", 50, 30);

        final OwnedItem source = new OwnedItem(
                POTION_ITEM_ID, quantity, StorageKind.BANK, false, 0);
        setOwnedItemId(source, 1L);

        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(source));
        when(itemCatalogService.byId(POTION_ITEM_ID)).thenReturn(Optional.of(potionCatalog));
        when(ownedItemRepository.findByStorageAndItemId(StorageKind.INVENTORY, POTION_ITEM_ID))
                .thenReturn(Optional.empty());
        when(ownedItemRepository.countByStorage(StorageKind.INVENTORY)).thenReturn(10L);

        inventoryService.moveToInventory(1L);

        assertThat(source.getStorage()).isEqualTo(StorageKind.INVENTORY);
        assertThat(source.getQuantity()).isEqualTo(quantity);
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 장비 종류를 임의로 생성한다.
     *
     * @return EquipmentKind Arbitrary
     */
    @Provide
    Arbitrary<EquipmentKind> equipmentKind() {
        return Arbitraries.of(EquipmentKind.values());
    }

    /**
     * 소비형 아이템의 유효한 수량을 생성한다.
     *
     * @return 1 ~ 99 범위의 수량 Arbitrary
     */
    @Provide
    Arbitrary<Integer> potionQuantity() {
        return Arbitraries.integers().between(1, 99);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /**
     * 리플렉션으로 OwnedItem의 id 필드를 설정한다.
     *
     * @param ownedItem 대상 엔티티
     * @param id        설정할 ID 값
     */
    private void setOwnedItemId(final OwnedItem ownedItem, final Long id) {
        try {
            final java.lang.reflect.Field idField = OwnedItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(ownedItem, id);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to set OwnedItem id", e);
        }
    }
}
