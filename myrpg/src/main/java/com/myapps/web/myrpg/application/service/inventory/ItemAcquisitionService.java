package com.myapps.web.myrpg.application.service.inventory;

import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DroppedItem;
import com.myapps.web.myrpg.application.exception.InventoryFullException;
import com.myapps.web.myrpg.application.service.ItemCatalogService;
import com.myapps.web.myrpg.config.GameProperties;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.Optional;

/** 드랍/구매 아이템 획득 처리 및 신규 캐릭터 기본 장비/소비품 시딩을 담당하는 협력 객체. */
public final class ItemAcquisitionService {

    private static final String SEED_HP_POTION_ID = "hp_potion_30";
    private static final String SEED_MP_POTION_ID = "mp_potion_30";
    private static final String SEED_STAMINA_POTION_ID = "stamina_potion_30";
    private static final String SEED_ONE_HAND_SWORD_ID = "beginner_one_hand_sword";
    private static final String SEED_TWO_HAND_SWORD_ID = "beginner_two_hand_sword";
    private static final String SEED_BOW_ID = "beginner_bow";
    private static final String SEED_WAND_ID = "beginner_wand";
    private static final String SEED_STAFF_ID = "beginner_staff";
    private static final String SEED_SHIELD_ID = "beginner_shield";
    private static final String SEED_ARMOR_ID = "beginner_armor";
    private static final String SEED_HELMET_ID = "beginner_helmet";
    private static final String SEED_GLOVES_ID = "beginner_gloves";
    private static final String SEED_BOOTS_ID = "beginner_boots";

    private static final String LOG_TYPE_ITEM = "item";

    private final InventoryQuerySupport query;
    private final OwnedItemRepository ownedItemRepository;
    private final ItemCatalogService itemCatalogService;
    private final ActionLog actionLog;
    private final GameMessageService gameMessageService;
    private final GameProperties gameProperties;

    /**
     * ItemAcquisitionService를 생성한다.
     *
     * @param query 공유 조회 지원 컴포넌트
     * @param ownedItemRepository 보유 아이템 저장소
     * @param itemCatalogService 아이템 카탈로그 서비스
     * @param actionLog 활동 로그 컴포넌트
     * @param gameMessageService 게임 메시지 다국어 서비스
     * @param gameProperties 게임 프로퍼티 설정
     */
    public ItemAcquisitionService(
            final InventoryQuerySupport query,
            final OwnedItemRepository ownedItemRepository,
            final ItemCatalogService itemCatalogService,
            final ActionLog actionLog,
            final GameMessageService gameMessageService,
            final GameProperties gameProperties) {
        this.query = query;
        this.ownedItemRepository = ownedItemRepository;
        this.itemCatalogService = itemCatalogService;
        this.actionLog = actionLog;
        this.gameMessageService = gameMessageService;
        this.gameProperties = gameProperties;
    }

    /** 기본 캐릭터(1L)에 모든 초보자용 장비와 기본 소비품을 지급하고 기본 장비를 장착한다. */
    public void seedDefault() {
        seedDefault(1L);
    }

    /**
     * 특정 캐릭터에 모든 초보자용 장비와 기본 소비품을 지급하고 기본 장비를 장착한다.
     *
     * @param characterId 아이템을 지급할 대상 캐릭터 ID
     */
    public void seedDefault(final Long characterId) {
        final Long targetCharId = query.normalizeCharacterId(characterId);
        final int potionQty = defaultPotionQuantity();
        ownedItemRepository.save(
                new OwnedItem(
                        targetCharId,
                        SEED_HP_POTION_ID,
                        potionQty,
                        StorageKind.INVENTORY,
                        false,
                        0));
        ownedItemRepository.save(
                new OwnedItem(
                        targetCharId,
                        SEED_MP_POTION_ID,
                        potionQty,
                        StorageKind.INVENTORY,
                        false,
                        0));
        ownedItemRepository.save(
                new OwnedItem(
                        targetCharId,
                        SEED_STAMINA_POTION_ID,
                        potionQty,
                        StorageKind.INVENTORY,
                        false,
                        0));

        // 무기 5종: 한손검만 기본 장착, 나머지는 인벤토리 보유
        seedEquipment(targetCharId, SEED_ONE_HAND_SWORD_ID, true);
        seedEquipment(targetCharId, SEED_TWO_HAND_SWORD_ID, false);
        seedEquipment(targetCharId, SEED_BOW_ID, false);
        seedEquipment(targetCharId, SEED_WAND_ID, false);
        seedEquipment(targetCharId, SEED_STAFF_ID, false);

        // 방어구 5종: 방패·갑옷·투구·장갑·부츠 모두 기본 장착
        seedEquipment(targetCharId, SEED_SHIELD_ID, true);
        seedEquipment(targetCharId, SEED_ARMOR_ID, true);
        seedEquipment(targetCharId, SEED_HELMET_ID, true);
        seedEquipment(targetCharId, SEED_GLOVES_ID, true);
        seedEquipment(targetCharId, SEED_BOOTS_ID, true);
    }

    /**
     * 몬스터 처치 드랍 결과를 인벤토리에 적재한다.
     *
     * @param progress 캐릭터 진행상황 (골드 가산 대상)
     * @param drop 드랍 결과 (골드 + 아이템 목록)
     */
    public void acquire(final CharacterProgress progress, final DropResult drop) {
        if (drop.gold() > 0) {
            progress.gainGold(drop.gold());
        }

        for (final DroppedItem droppedItem : drop.items()) {
            acquireSingleItem(progress, droppedItem);
        }
    }

    /**
     * 지정된 아이템을 기본 캐릭터 인벤토리에 획득 처리한다.
     *
     * @param itemId 획득할 아이템 카탈로그 ID
     * @param quantity 획득 수량 (1 이상)
     * @throws InventoryFullException 인벤토리 용량 초과 시
     */
    public void acquireItem(final String itemId, final int quantity) {
        acquireItem(null, itemId, quantity);
    }

    /**
     * 특정 캐릭터의 인벤토리에 아이템을 획득 처리한다.
     *
     * @param characterId 대상 캐릭터 식별자
     * @param itemId 획득할 아이템 카탈로그 ID
     * @param quantity 획득 수량
     * @throws InventoryFullException 인벤토리 용량 초과 시
     */
    public void acquireItem(final Long characterId, final String itemId, final int quantity) {
        final Optional<Item> catalogOpt = itemCatalogService.byId(itemId);
        if (catalogOpt.isEmpty()) {
            return;
        }
        final Item catalogItem = catalogOpt.get();
        final Long targetCharId = query.normalizeCharacterId(characterId);

        if (catalogItem.type().isStackable()) {
            acquirePotionItem(targetCharId, itemId, quantity);
        } else {
            query.checkCapacity(targetCharId, StorageKind.INVENTORY);
            final int maxDurability = query.resolveMaxDurability(itemId);
            ownedItemRepository.save(
                    new OwnedItem(
                            targetCharId, itemId, 1, StorageKind.INVENTORY, false, maxDurability));
        }
    }

    private void seedEquipment(
            final Long characterId, final String itemId, final boolean equipped) {
        ownedItemRepository.save(
                new OwnedItem(
                        characterId,
                        itemId,
                        1,
                        StorageKind.INVENTORY,
                        equipped,
                        query.resolveMaxDurability(itemId)));
    }

    private void acquireSingleItem(
            final CharacterProgress progress, final DroppedItem droppedItem) {
        final Optional<Item> catalogOpt = itemCatalogService.byId(droppedItem.itemId());
        if (catalogOpt.isEmpty()) {
            return;
        }
        final Item catalogItem = catalogOpt.get();
        final String itemName = catalogItem.name();
        final Long charId = query.normalizeCharacterId(progress != null ? progress.getId() : null);

        if (catalogItem.type().isStackable()) {
            acquirePotion(charId, droppedItem);
        } else {
            acquireEquipment(charId, droppedItem, itemName);
        }
    }

    private void acquirePotion(final Long characterId, final DroppedItem droppedItem) {
        final Long targetCharId = query.normalizeCharacterId(characterId);
        final Optional<OwnedItem> existingStack =
                query.findStorageItem(targetCharId, StorageKind.INVENTORY, droppedItem.itemId());

        if (existingStack.isPresent()) {
            existingStack.get().increaseQuantity(droppedItem.quantity());
        } else {
            if (query.isInventoryFull(targetCharId)) {
                logItemAcquireFailure(droppedItem.itemId());
                return;
            }
            ownedItemRepository.save(
                    new OwnedItem(
                            targetCharId,
                            droppedItem.itemId(),
                            droppedItem.quantity(),
                            StorageKind.INVENTORY,
                            false,
                            0));
        }
    }

    private void acquireEquipment(
            final Long characterId, final DroppedItem droppedItem, final String itemName) {
        final Long targetCharId = query.normalizeCharacterId(characterId);
        if (query.isInventoryFull(targetCharId)) {
            final String failMsg = gameMessageService.get("log.item.acquire_fail", itemName);
            actionLog.add(failMsg, LOG_TYPE_ITEM);
            return;
        }
        final int maxDurability = query.resolveMaxDurability(droppedItem.itemId());
        ownedItemRepository.save(
                new OwnedItem(
                        targetCharId,
                        droppedItem.itemId(),
                        1,
                        StorageKind.INVENTORY,
                        false,
                        maxDurability));
    }

    private void acquirePotionItem(
            final Long characterId, final String itemId, final int quantity) {
        final Long targetCharId = query.normalizeCharacterId(characterId);
        final Optional<OwnedItem> existingStack =
                query.findStorageItem(targetCharId, StorageKind.INVENTORY, itemId);

        if (existingStack.isPresent()) {
            existingStack.get().increaseQuantity(quantity);
        } else {
            query.checkCapacity(targetCharId, StorageKind.INVENTORY);
            ownedItemRepository.save(
                    new OwnedItem(targetCharId, itemId, quantity, StorageKind.INVENTORY, false, 0));
        }
    }

    private void logItemAcquireFailure(final String itemId) {
        final String itemName = itemCatalogService.byId(itemId).map(Item::name).orElse(itemId);
        final String failMsg = gameMessageService.get("log.item.acquire_fail", itemName);
        actionLog.add(failMsg, LOG_TYPE_ITEM);
    }

    private int defaultPotionQuantity() {
        return gameProperties.inventory().defaultPotionQty();
    }
}
