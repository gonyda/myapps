package com.myapps.web.myrpg.application.service.inventory;

import com.myapps.web.myrpg.application.exception.EquipConflictException;
import com.myapps.web.myrpg.application.exception.InventoryFullException;
import com.myapps.web.myrpg.application.service.ItemCatalogService;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.util.Optional;
import java.util.Set;

/** 인벤토리와 은행 간 아이템 이동(맡기기/찾기) 및 저장소 이동을 담당하는 협력 객체. */
public final class ItemStorageService {

    private final InventoryQuerySupport query;
    private final OwnedItemRepository ownedItemRepository;
    private final ItemCatalogService itemCatalogService;

    /**
     * ItemStorageService를 생성한다.
     *
     * @param query 공유 조회 지원 컴포넌트
     * @param ownedItemRepository 보유 아이템 저장소
     * @param itemCatalogService 아이템 카탈로그 서비스
     */
    public ItemStorageService(
            final InventoryQuerySupport query,
            final OwnedItemRepository ownedItemRepository,
            final ItemCatalogService itemCatalogService) {
        this.query = query;
        this.ownedItemRepository = ownedItemRepository;
        this.itemCatalogService = itemCatalogService;
    }

    /**
     * 인벤토리에서 은행으로 아이템을 맡긴다.
     *
     * <p>장착 중인 장비는 맡기기를 거부한다. 소비형(POTION, MATERIAL)은 은행의 동일 itemId 행에 스택 누적하며, 장비는 저장위치를 전환한다. 은행
     * 용량(30)을 초과하면 거부한다.
     *
     * @param ownedItemId 맡길 보유 아이템 PK
     * @throws EquipConflictException 장착 중 장비 맡기기 시도 시
     * @throws InventoryFullException 은행 용량 초과 시
     */
    public void moveToBank(final long ownedItemId) {
        final OwnedItem target = query.findOwnedItemOrThrow(ownedItemId);

        final Set<Long> otherSetIds = query.resolveOtherSetAssignedItemIds(target.getCharacterId());
        if (target.isEquipped() || otherSetIds.contains(target.getId())) {
            throw new EquipConflictException("장착을 해제한 후 맡길 수 있습니다.");
        }

        final Item catalogItem =
                itemCatalogService
                        .byId(target.getItemId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "카탈로그에 아이템이 없습니다: " + target.getItemId()));

        if (catalogItem.type().isStackable()) {
            movePotionToStorage(target, StorageKind.BANK);
        } else {
            query.checkCapacity(target.getCharacterId(), StorageKind.BANK);
            target.moveTo(StorageKind.BANK);
        }
    }

    /**
     * 은행에서 인벤토리로 아이템을 찾는다.
     *
     * <p>스택형 아이템(POTION, MATERIAL)은 인벤토리의 동일 itemId 행에 스택 누적하며, 장비는 저장위치를 전환한다. 인벤토리 용량(30)을 초과하면
     * 거부한다.
     *
     * @param ownedItemId 찾을 보유 아이템 PK
     * @throws InventoryFullException 인벤토리 용량 초과 시
     */
    public void moveToInventory(final long ownedItemId) {
        final OwnedItem target = query.findOwnedItemOrThrow(ownedItemId);

        final Item catalogItem =
                itemCatalogService
                        .byId(target.getItemId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "카탈로그에 아이템이 없습니다: " + target.getItemId()));

        if (catalogItem.type().isStackable()) {
            movePotionToStorage(target, StorageKind.INVENTORY);
        } else {
            query.checkCapacity(target.getCharacterId(), StorageKind.INVENTORY);
            target.moveTo(StorageKind.INVENTORY);
        }
    }

    private void movePotionToStorage(final OwnedItem source, final StorageKind destination) {
        final Long targetCharId = query.normalizeCharacterId(source.getCharacterId());
        final Optional<OwnedItem> existingStack =
                query.findStorageItem(targetCharId, destination, source.getItemId());

        if (existingStack.isPresent()) {
            existingStack.get().increaseQuantity(1);
            ownedItemRepository.save(existingStack.get());
        } else {
            query.checkCapacity(targetCharId, destination);
            final OwnedItem newItem =
                    new OwnedItem(targetCharId, source.getItemId(), 1, destination, false, 0);
            ownedItemRepository.save(newItem);
        }

        source.decreaseQuantity(1);
        if (source.getQuantity() <= 0) {
            ownedItemRepository.delete(source);
        } else {
            ownedItemRepository.save(source);
        }
    }
}
