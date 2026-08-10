package com.myapps.web.myrpg.application.service;

import java.util.List;
import java.util.Optional;

import com.myapps.web.myrpg.application.exception.InventoryFullException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 저장소 용량 가드 프로퍼티 테스트.
 *
 * <p>신규 스택이 추가되어 항목 수가 30을 초과하면 {@link InventoryFullException}으로
 * 거부되고(상태 불변), 기존 스택에 누적되는 소비형 이동은 용량 검사를 통과한다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 10: 저장소 용량 가드
 *
 * <p><b>Validates: Requirements 8.1, 8.2, 8.3, 8.4</b>
 */
class InventoryCapacityPropertyTest {

    private static final int MAX_CAPACITY = 30;
    private static final String POTION_ITEM_ID = "hp_potion_50";
    private static final String WEAPON_ITEM_ID = "beginner_one_hand_sword";

    // Feature: 006-gold-item-inventory, Property 10: 저장소 용량 가드

    /**
     * 대상 저장소가 가득 찬 상태(30항목)에서 신규 스택이 추가되면
     * {@link InventoryFullException}으로 거부되고 상태가 불변임을 검증한다.
     *
     * @param currentCount 현재 항목 수 (30 이상)
     */
    @Property(tries = 100)
    void should_throwInventoryFull_when_newStackExceedsCapacity(
            @ForAll("fullOrOverCapacity") final long currentCount) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);

        final InventoryService inventoryService = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository, statProgression);

        // 장비를 인벤토리→은행 이동 시 은행이 가득 참
        final EquipmentItem equipCatalog = new EquipmentItem(
                WEAPON_ITEM_ID, "초보자 한손검", ItemType.WEAPON,
                EquipmentKind.ONE_HANDED_SWORD, List.of(), null, 20);

        final OwnedItem source = new OwnedItem(
                WEAPON_ITEM_ID, 1, StorageKind.INVENTORY, false, 20.0);
        setOwnedItemId(source, 1L);

        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(source));
        when(itemCatalogService.byId(WEAPON_ITEM_ID)).thenReturn(Optional.of(equipCatalog));
        when(ownedItemRepository.countByStorage(StorageKind.BANK)).thenReturn(currentCount);

        assertThatThrownBy(() -> inventoryService.moveToBank(1L))
                .isInstanceOf(InventoryFullException.class);

        // 상태 불변: storage가 INVENTORY로 유지됨
        assertThat(source.getStorage()).isEqualTo(StorageKind.INVENTORY);
    }

    /**
     * 대상 저장소가 가득 찬 상태(30항목)에서도 소비형이 기존 스택에 누적되면
     * 용량 검사를 통과하고 이동이 성공함을 검증한다.
     *
     * @param currentCount 현재 항목 수 (30 이상)
     */
    @Property(tries = 100)
    void should_passCapacityCheck_when_potionStacksIntoExisting(
            @ForAll("fullOrOverCapacity") final long currentCount) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);

        final InventoryService inventoryService = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository, statProgression);

        final PotionItem potionCatalog = new PotionItem(POTION_ITEM_ID, "HP 포션", 50, 30);

        final OwnedItem source = new OwnedItem(
                POTION_ITEM_ID, 3, StorageKind.INVENTORY, false, 0);
        setOwnedItemId(source, 1L);

        final OwnedItem existingStack = new OwnedItem(
                POTION_ITEM_ID, 5, StorageKind.BANK, false, 0);
        setOwnedItemId(existingStack, 2L);

        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(source));
        when(itemCatalogService.byId(POTION_ITEM_ID)).thenReturn(Optional.of(potionCatalog));
        when(ownedItemRepository.findByStorageAndItemId(StorageKind.BANK, POTION_ITEM_ID))
                .thenReturn(Optional.of(existingStack));
        when(ownedItemRepository.countByStorage(StorageKind.BANK)).thenReturn(currentCount);

        // 기존 스택 누적이므로 예외 없이 성공
        inventoryService.moveToBank(1L);

        // 수량이 누적됨
        assertThat(existingStack.getQuantity()).isEqualTo(5 + 3);
        // 원본은 삭제됨
        verify(ownedItemRepository).delete(source);
    }

    /**
     * 대상 저장소에 여유가 있으면 (항목 수 < 30) 신규 스택 추가가 허용됨을 검증한다.
     *
     * @param currentCount 현재 항목 수 (0 ~ 29)
     */
    @Property(tries = 100)
    void should_allowNewStack_when_capacityNotExceeded(
            @ForAll("underCapacity") final long currentCount) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);

        final InventoryService inventoryService = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository, statProgression);

        final EquipmentItem equipCatalog = new EquipmentItem(
                WEAPON_ITEM_ID, "초보자 한손검", ItemType.WEAPON,
                EquipmentKind.ONE_HANDED_SWORD, List.of(), null, 20);

        final OwnedItem source = new OwnedItem(
                WEAPON_ITEM_ID, 1, StorageKind.INVENTORY, false, 20.0);
        setOwnedItemId(source, 1L);

        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(source));
        when(itemCatalogService.byId(WEAPON_ITEM_ID)).thenReturn(Optional.of(equipCatalog));
        when(ownedItemRepository.countByStorage(StorageKind.BANK)).thenReturn(currentCount);

        inventoryService.moveToBank(1L);

        // storage가 BANK로 전환됨
        assertThat(source.getStorage()).isEqualTo(StorageKind.BANK);
    }

    /**
     * 인벤토리 방향(찾기)에서도 대상 저장소가 가득 차면 거부됨을 검증한다.
     *
     * @param currentCount 인벤토리 현재 항목 수 (30 이상)
     */
    @Property(tries = 100)
    void should_throwInventoryFull_when_moveToInventoryExceedsCapacity(
            @ForAll("fullOrOverCapacity") final long currentCount) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);

        final InventoryService inventoryService = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository, statProgression);

        final EquipmentItem equipCatalog = new EquipmentItem(
                WEAPON_ITEM_ID, "초보자 한손검", ItemType.WEAPON,
                EquipmentKind.ONE_HANDED_SWORD, List.of(), null, 20);

        final OwnedItem source = new OwnedItem(
                WEAPON_ITEM_ID, 1, StorageKind.BANK, false, 20.0);
        setOwnedItemId(source, 1L);

        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(source));
        when(itemCatalogService.byId(WEAPON_ITEM_ID)).thenReturn(Optional.of(equipCatalog));
        when(ownedItemRepository.countByStorage(StorageKind.INVENTORY)).thenReturn(currentCount);

        assertThatThrownBy(() -> inventoryService.moveToInventory(1L))
                .isInstanceOf(InventoryFullException.class);

        // 상태 불변: storage가 BANK로 유지됨
        assertThat(source.getStorage()).isEqualTo(StorageKind.BANK);
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 용량이 가득 차거나 초과한 항목 수를 생성한다.
     *
     * @return 30 ~ 50 범위의 항목 수 Arbitrary
     */
    @Provide
    Arbitrary<Long> fullOrOverCapacity() {
        return Arbitraries.longs().between(MAX_CAPACITY, 50L);
    }

    /**
     * 용량 미만인 항목 수를 생성한다.
     *
     * @return 0 ~ 29 범위의 항목 수 Arbitrary
     */
    @Provide
    Arbitrary<Long> underCapacity() {
        return Arbitraries.longs().between(0L, (long) (MAX_CAPACITY - 1));
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
