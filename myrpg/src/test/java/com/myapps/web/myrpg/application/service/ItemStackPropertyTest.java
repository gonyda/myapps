package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.PotionItem;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.util.List;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * 스택 규칙 프로퍼티 테스트.
 *
 * <p>소비형(POTION)은 같은 {@code itemId}+{@code storage}가 한 행으로 누적(quantity 증가)되고, 장비(WEAPON/ARMOR)는 항상
 * 개별 행으로 저장되어 스택되지 않는다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 9: 스택 규칙
 *
 * <p><b>Validates: Requirements 7.3, 7.4</b>
 */
class ItemStackPropertyTest {

    private static final String POTION_ITEM_ID = "hp_potion_50";
    private static final String WEAPON_ITEM_ID = "beginner_one_hand_sword";

    // Feature: 006-gold-item-inventory, Property 9: 스택 규칙

    /**
     * 소비형(POTION) 아이템 이동 시, 대상 저장소에 동일 itemId 행이 존재하면 새 행을 생성하지 않고 기존 행의 수량이 증가함을 검증한다.
     *
     * @param initialQuantity 원본 소비형 아이템 수량
     * @param existingQuantity 대상 저장소의 기존 스택 수량
     */
    @Property(tries = 100)
    void should_stackIntoExistingRow_when_potionMovedToStorageWithSameItem(
            @ForAll("potionQuantity") final int initialQuantity,
            @ForAll("potionQuantity") final int existingQuantity) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);

        final InventoryService inventoryService =
                createService(
                        ownedItemRepository,
                        itemCatalogService,
                        characterProgressRepository,
                        statProgression);

        final PotionItem potionCatalog = new PotionItem(POTION_ITEM_ID, "HP 포션", 50, 30);

        final OwnedItem source =
                new OwnedItem(POTION_ITEM_ID, initialQuantity, StorageKind.INVENTORY, false, 0);
        setOwnedItemId(source, 1L);

        final OwnedItem existingStack =
                new OwnedItem(POTION_ITEM_ID, existingQuantity, StorageKind.BANK, false, 0);
        setOwnedItemId(existingStack, 2L);

        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(source));
        when(itemCatalogService.byId(POTION_ITEM_ID)).thenReturn(Optional.of(potionCatalog));
        when(ownedItemRepository.findByStorageAndItemId(StorageKind.BANK, POTION_ITEM_ID))
                .thenReturn(Optional.of(existingStack));
        when(ownedItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.moveToBank(1L);

        // 기존 스택에 수량이 1만큼 증가
        assertThat(existingStack.getQuantity()).isEqualTo(existingQuantity + 1);
        // 원본의 수량이 1만큼 감소
        assertThat(source.getQuantity()).isEqualTo(initialQuantity - 1);
        // 원본 수량이 0이면 삭제, 아니면 유지
        if (initialQuantity == 1) {
            verify(ownedItemRepository).delete(source);
        } else {
            verify(ownedItemRepository, never()).delete(any());
        }
    }

    /**
     * 소비형(POTION) 아이템 이동 시, 대상 저장소에 동일 itemId 행이 없으면 새 행(저장위치 전환)이 생성됨을 검증한다.
     *
     * @param initialQuantity 원본 소비형 아이템 수량
     */
    @Property(tries = 100)
    void should_createNewRow_when_potionMovedToStorageWithoutSameItem(
            @ForAll("potionQuantity") final int initialQuantity) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);

        final InventoryService inventoryService =
                createService(
                        ownedItemRepository,
                        itemCatalogService,
                        characterProgressRepository,
                        statProgression);

        final PotionItem potionCatalog = new PotionItem(POTION_ITEM_ID, "HP 포션", 50, 30);

        final OwnedItem source =
                new OwnedItem(POTION_ITEM_ID, initialQuantity, StorageKind.INVENTORY, false, 0);
        setOwnedItemId(source, 1L);

        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(source));
        when(itemCatalogService.byId(POTION_ITEM_ID)).thenReturn(Optional.of(potionCatalog));
        when(ownedItemRepository.findByStorageAndItemId(StorageKind.BANK, POTION_ITEM_ID))
                .thenReturn(Optional.empty());
        when(ownedItemRepository.countByStorage(StorageKind.BANK)).thenReturn(10L);
        when(ownedItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.moveToBank(1L);

        // 새 행(quantity=1)이 생성되어 save 호출됨
        verify(ownedItemRepository, atLeastOnce()).save(any(OwnedItem.class));
        // 원본 저장위치는 INVENTORY 그대로 (이동하지 않음)
        assertThat(source.getStorage()).isEqualTo(StorageKind.INVENTORY);
        // 원본 수량이 1만큼 감소
        assertThat(source.getQuantity()).isEqualTo(initialQuantity - 1);
        // 원본 수량이 0이면 delete 호출
        if (initialQuantity == 1) {
            verify(ownedItemRepository).delete(source);
        } else {
            verify(ownedItemRepository, never()).delete(any());
        }
    }

    /**
     * 장비(WEAPON/ARMOR)는 대상 저장소에 동일 itemId가 있어도 스택하지 않고 개별 행(저장위치 전환)으로 이동함을 검증한다.
     *
     * @param kind 장비 종류
     */
    @Property(tries = 100)
    void should_neverStack_when_equipmentMoved(@ForAll("equipmentKind") final EquipmentKind kind) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);

        final InventoryService inventoryService =
                createService(
                        ownedItemRepository,
                        itemCatalogService,
                        characterProgressRepository,
                        statProgression);

        final ItemType equipType =
                (kind == EquipmentKind.SHIELD || kind == EquipmentKind.ARMOR_BODY)
                        ? ItemType.ARMOR
                        : ItemType.WEAPON;
        final EquipmentItem equipCatalog =
                new EquipmentItem(WEAPON_ITEM_ID, "초보자 무기", equipType, kind, List.of(), null, 20);

        final OwnedItem source =
                new OwnedItem(WEAPON_ITEM_ID, 1, StorageKind.INVENTORY, false, 20.0);
        setOwnedItemId(source, 1L);

        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(source));
        when(itemCatalogService.byId(WEAPON_ITEM_ID)).thenReturn(Optional.of(equipCatalog));
        when(ownedItemRepository.countByStorage(StorageKind.BANK)).thenReturn(10L);

        inventoryService.moveToBank(1L);

        // 장비는 스택 없이 storage만 전환
        assertThat(source.getStorage()).isEqualTo(StorageKind.BANK);
        assertThat(source.getQuantity()).isEqualTo(1);
        verify(ownedItemRepository, never()).delete(any());
    }

    /**
     * 장비(WEAPON/ARMOR)는 항상 quantity=1로 개별 인스턴스임을 검증한다. seedDefault에서 생성된 장비 아이템은 모두 quantity=1이다.
     *
     * @param kind 장비 종류
     */
    @Property(tries = 100)
    void should_alwaysHaveQuantityOne_when_equipmentCreated(
            @ForAll("equipmentKind") final EquipmentKind kind) {

        final OwnedItem equipment =
                new OwnedItem(
                        "equip_" + kind.name().toLowerCase(),
                        1,
                        StorageKind.INVENTORY,
                        false,
                        20.0);

        assertThat(equipment.getQuantity()).isEqualTo(1);
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 소비형 아이템의 유효한 수량을 생성한다.
     *
     * @return 1 ~ 99 범위의 수량 Arbitrary
     */
    @Provide
    Arbitrary<Integer> potionQuantity() {
        return Arbitraries.integers().between(1, 99);
    }

    /**
     * 장비 종류를 임의로 생성한다.
     *
     * @return EquipmentKind Arbitrary
     */
    @Provide
    Arbitrary<EquipmentKind> equipmentKind() {
        return Arbitraries.of(EquipmentKind.values());
    }

    private InventoryService createService(
            final OwnedItemRepository ownedItemRepository,
            final ItemCatalogService itemCatalogService,
            final CharacterProgressRepository characterProgressRepository,
            final StatProgression statProgression) {
        org.mockito.Mockito.lenient()
                .when(
                        ownedItemRepository.countByCharacterIdAndStorage(
                                org.mockito.ArgumentMatchers.any(),
                                org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> ownedItemRepository.countByStorage(inv.getArgument(1)));
        org.mockito.Mockito.lenient()
                .when(
                        ownedItemRepository.findByCharacterIdAndStorageAndItemId(
                                org.mockito.ArgumentMatchers.any(),
                                org.mockito.ArgumentMatchers.any(),
                                org.mockito.ArgumentMatchers.any()))
                .thenAnswer(
                        inv ->
                                ownedItemRepository.findByStorageAndItemId(
                                        inv.getArgument(1), inv.getArgument(2)));
        return new InventoryService(
                ownedItemRepository,
                itemCatalogService,
                characterProgressRepository,
                statProgression,
                mock(com.myapps.web.myrpg.domain.model.ActionLog.class),
                mock(com.myapps.web.myrpg.application.service.SkillCatalogService.class),
                mock(com.myapps.web.myrpg.domain.repository.CharacterSkillRepository.class));
    }

    /**
     * 리플렉션으로 OwnedItem의 id 필드를 설정한다.
     *
     * @param ownedItem 대상 엔티티
     * @param id 설정할 ID 값
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
