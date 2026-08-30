package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.ShopBuyItemView;
import com.myapps.web.myrpg.application.dto.ShopSellItemView;
import com.myapps.web.myrpg.application.dto.ShopView;
import com.myapps.web.myrpg.application.exception.EquipConflictException;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipSlot;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상점(구매/판매) 핵심 비즈니스 로직을 담당하는 서비스.
 *
 * <p>아이템 판매가 계산, NPC별 구매 목록 조립, 구매/판매 오케스트레이션을 처리한다. 판매가는 저장되지 않고 조회 시점에 매번 계산되며, 기본가(buyPrice) 존재
 * 시 {@code round(buyPrice * 0.5)}, 부재 시 카탈로그 보너스 합산({@code Σ amount * weightOf(target)})으로 배타 적용된다.
 */
@Service
public class ShopService {

    /** 판매가 비율 (구매가의 50%). */
    public static final double SELL_RATIO = 0.5;

    /** 일반 보너스 대상 가중치. */
    public static final int WEIGHT = 10;

    /** 치명타 보너스 대상 가중치. */
    public static final int CRITICAL_WEIGHT = 1;

    private static final String LOG_TYPE_ITEM = "item";

    private final ItemCatalogService itemCatalogService;
    private final NpcService npcService;
    private final OwnedItemRepository ownedItemRepository;
    private final InventoryService inventoryService;
    private final CharacterService characterService;
    private final ActionLog actionLog;
    private final GameMessageService gameMessageService;

    /** ShopService를 생성한다 (Spring 주입용). */
    @org.springframework.beans.factory.annotation.Autowired
    public ShopService(
            final ItemCatalogService itemCatalogService,
            final NpcService npcService,
            final OwnedItemRepository ownedItemRepository,
            final InventoryService inventoryService,
            final CharacterService characterService,
            final ActionLog actionLog,
            final GameMessageService gameMessageService) {
        this.itemCatalogService = itemCatalogService;
        this.npcService = npcService;
        this.ownedItemRepository = ownedItemRepository;
        this.inventoryService = inventoryService;
        this.characterService = characterService;
        this.actionLog = actionLog;
        this.gameMessageService =
                gameMessageService != null ? gameMessageService : new GameMessageService(null);
    }

    /** 이전 호환용 생성자. */
    public ShopService(
            final ItemCatalogService itemCatalogService,
            final NpcService npcService,
            final OwnedItemRepository ownedItemRepository,
            final InventoryService inventoryService,
            final CharacterService characterService,
            final ActionLog actionLog) {
        this(
                itemCatalogService,
                npcService,
                ownedItemRepository,
                inventoryService,
                characterService,
                actionLog,
                new GameMessageService(null));
    }

    /**
     * 보유 아이템의 판매가(1개당)를 계산한다.
     *
     * <p>공식: 기본가 + 인스턴스 보너스 (현재 인스턴스 보너스는 0). 기본가는 buyPrice 존재 시 {@code round(buyPrice * 0.5)}, 부재
     * 시 카탈로그 보너스 합산으로 배타 적용된다.
     *
     * @param ownedItem 판매가를 계산할 보유 아이템
     * @return 판매가 (골드)
     */
    public long sellValueOf(final OwnedItem ownedItem) {
        final Item item =
                itemCatalogService
                        .byId(ownedItem.getItemId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "카탈로그에 아이템이 없습니다: " + ownedItem.getItemId()));
        return calculateSellValue(item);
    }

    /**
     * 카탈로그 아이템의 판매가를 계산한다.
     *
     * <p>buyPrice가 존재하면 {@code round(buyPrice * 0.5)}를 반환하고, 없으면 장비의 카탈로그 보너스 합산을 반환한다. 포션은
     * buyPrice가 없으면 0이다.
     *
     * @param item 판매가를 계산할 카탈로그 아이템
     * @return 판매가 (골드)
     */
    public long calculateSellValue(final Item item) {
        if (item.buyPrice() != null) {
            return Math.round(item.buyPrice() * SELL_RATIO);
        }
        if (item instanceof EquipmentItem equip) {
            long total = 0;
            for (final EquipBonus bonus : equip.bonuses()) {
                total += (long) bonus.amount() * weightOf(bonus.target());
            }
            return total;
        }
        return 0L;
    }

    /**
     * 보너스 대상별 판매가 가중치를 반환한다.
     *
     * <p>치명타({@link BonusTarget#CRITICAL})는 1, 그 외 대상은 10이다.
     *
     * @param target 보너스 대상
     * @return 가중치
     */
    public int weightOf(final BonusTarget target) {
        return (target == BonusTarget.CRITICAL) ? CRITICAL_WEIGHT : WEIGHT;
    }

    /**
     * 상점 팝업 뷰를 조립한다.
     *
     * <p>NPC의 {@code shopItems} 중 buyPrice가 있는 아이템만 구매 목록으로, 인벤토리 아이템 전체를 판매 목록으로 구성한다.
     *
     * @param npcId 대화 중인 NPC id
     * @param currentGold 현재 보유 골드
     * @return 상점 팝업 뷰
     */
    public ShopView buildShopView(
            final Long characterId, final String npcId, final long currentGold) {
        final List<ShopBuyItemView> buyItems = shopBuyList(characterId, npcId);
        final List<ShopSellItemView> sellItems = buildSellList(characterId);
        return new ShopView(buyItems, sellItems, currentGold, npcId);
    }

    public ShopView buildShopView(final String npcId, final long currentGold) {
        return buildShopView(null, npcId, currentGold);
    }

    /**
     * NPC가 판매하는 구매 목록을 조립한다.
     *
     * <p>NPC의 {@code shopItems} 중 카탈로그에 존재하고 buyPrice가 있는 아이템만 포함한다. NPC가 없거나 shopItems가 비어 있으면 빈
     * 목록을 반환한다.
     *
     * @param npcId NPC id
     * @return 구매 목록 (정의 순서 보존)
     */
    public List<ShopBuyItemView> shopBuyList(final String npcId) {
        return shopBuyList(null, npcId);
    }

    /**
     * 특정 캐릭터의 착용 장비 비교 정보가 포함된 NPC 구매 목록을 조립한다.
     *
     * @param characterId 캐릭터 식별자 (미지정 시 null)
     * @param npcId NPC id
     * @return 구매 목록 (정의 순서 보존)
     */
    public List<ShopBuyItemView> shopBuyList(final Long characterId, final String npcId) {
        final Optional<Npc> npcOpt = npcService.byId(npcId);
        if (npcOpt.isEmpty()) {
            return List.of();
        }

        final Map<EquipSlot, OwnedItem> equippedSlotMap = resolveEquippedSlotMap(characterId);

        final List<ShopBuyItemView> result = new ArrayList<>();
        for (final String itemId : npcOpt.get().shopItems()) {
            final Optional<Item> itemOpt = itemCatalogService.byId(itemId);
            if (itemOpt.isEmpty() || itemOpt.get().buyPrice() == null) {
                continue;
            }
            final Item item = itemOpt.get();
            final int initialDurability =
                    item instanceof EquipmentItem equipItem ? equipItem.maxDurability() : 0;
            final List<String> detailLines =
                    inventoryService.describe(
                            item,
                            new OwnedItem(
                                    item.id(), 1, StorageKind.INVENTORY, false, initialDurability));

            String equippedItemName = null;
            List<String> equippedDetailLines = List.of();

            if (item instanceof EquipmentItem equipItem) {
                final EquipSlot primarySlot = equipItem.kind().primarySlot();
                final OwnedItem currentEquipped = equippedSlotMap.get(primarySlot);
                if (currentEquipped != null) {
                    final Optional<Item> equippedCatalogOpt =
                            itemCatalogService.byId(currentEquipped.getItemId());
                    if (equippedCatalogOpt.isPresent()) {
                        final Item equippedCatalog = equippedCatalogOpt.get();
                        equippedItemName = equippedCatalog.name();
                        equippedDetailLines =
                                inventoryService.describe(equippedCatalog, currentEquipped);
                    }
                }
            }

            result.add(
                    new ShopBuyItemView(
                            item.id(),
                            item.name(),
                            item.type().label(),
                            item.buyPrice(),
                            detailLines,
                            equippedItemName,
                            equippedDetailLines));
        }
        return List.copyOf(result);
    }

    private Map<EquipSlot, OwnedItem> resolveEquippedSlotMap(final Long characterId) {
        final Map<EquipSlot, OwnedItem> slotMap = new EnumMap<>(EquipSlot.class);
        if (characterId == null) {
            return slotMap;
        }

        final List<OwnedItem> inventoryItems =
                ownedItemRepository.findByCharacterIdAndStorageOrderById(
                        characterId, StorageKind.INVENTORY);
        for (final OwnedItem owned : inventoryItems) {
            if (owned.isEquipped()) {
                final Optional<Item> catalogOpt = itemCatalogService.byId(owned.getItemId());
                if (catalogOpt.isPresent() && catalogOpt.get() instanceof EquipmentItem equip) {
                    slotMap.put(equip.kind().primarySlot(), owned);
                }
            }
        }
        return slotMap;
    }

    /**
     * 인벤토리 아이템의 판매 목록을 조립한다.
     *
     * <p>인벤토리({@code storage=INVENTORY}) 아이템 전체를 획득순으로 조회하고 각 행의 판매가를 계산하여 뷰로 변환한다.
     *
     * @return 판매 목록
     */
    public List<ShopSellItemView> buildSellList() {
        return buildSellList(null);
    }

    /**
     * 특정 캐릭터의 인벤토리 아이템 판매 목록을 조립한다.
     *
     * @param characterId 캐릭터 식별자
     * @return 판매 목록
     */
    public List<ShopSellItemView> buildSellList(final Long characterId) {
        final CharacterProgress progress =
                characterId != null ? characterService.loadByCharacterId(characterId) : null;
        final List<OwnedItem> inventoryItems =
                characterId != null
                        ? ownedItemRepository.findByCharacterIdAndStorageOrderById(
                                characterId, StorageKind.INVENTORY)
                        : ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY);

        final List<ShopSellItemView> result = new ArrayList<>();
        for (final OwnedItem owned : inventoryItems) {
            final Item catalogItem =
                    itemCatalogService
                            .byId(owned.getItemId())
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "카탈로그에 아이템이 없습니다: " + owned.getItemId()));
            final boolean isEquipped = isItemEquippedOrAssigned(progress, owned);
            result.add(
                    new ShopSellItemView(
                            owned.getId(),
                            catalogItem.name(),
                            catalogItem.type().label(),
                            owned.getQuantity(),
                            sellValueOf(owned),
                            isEquipped,
                            inventoryService.describe(catalogItem, owned)));
        }
        return List.copyOf(result);
    }

    /**
     * 상점에서 아이템을 1개 구매한다.
     *
     * <p>NPC의 {@code shopItems}에 포함되고 buyPrice가 있는 아이템만 구매 가능하다. 골드를 차감한 뒤 인벤토리에 아이템을 획득하고 행동 로그를
     * 남긴다.
     *
     * @param progress 캐릭터 진행상황 (골드 차감 대상)
     * @param npcId 대화 중인 NPC id
     * @param itemId 구매할 아이템 카탈로그 ID
     * @throws IllegalArgumentException NPC 판매 목록에 없는 아이템 구매 시
     */
    @Transactional
    public void buy(final CharacterProgress progress, final String npcId, final String itemId) {
        final Npc npc =
                npcService
                        .byId(npcId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("NPC를 찾을 수 없습니다: " + npcId));

        if (!npc.shopItems().contains(itemId)) {
            throw new IllegalArgumentException("해당 NPC가 판매하지 않는 아이템입니다: " + itemId);
        }

        final Item item =
                itemCatalogService
                        .byId(itemId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("카탈로그에 아이템이 없습니다: " + itemId));

        if (item.buyPrice() == null) {
            throw new IllegalArgumentException("상점에서 판매하지 않는 아이템입니다: " + itemId);
        }

        if (progress != null) {
            progress.spendGold(item.buyPrice());
        }

        if (progress == null || progress.getId() == null) {
            inventoryService.acquireItem(itemId, 1);
        } else {
            inventoryService.acquireItem(progress.getId(), itemId, 1);
        }
        final String buyMsg = gameMessageService.get("log.shop.buy", item.name());
        actionLog.add(buyMsg, LOG_TYPE_ITEM);
    }

    /**
     * 인벤토리 아이템을 1개 판매한다.
     *
     * <p>장착 중인 장비는 판매를 거부한다. 판매가는 {@code sellValueOf}로 계산하고, 수량을 1 감소시킨 뒤(0이면 행 삭제) 골드를 지급하고 행동 로그를
     * 남긴다.
     *
     * @param progress 캐릭터 진행상황 (골드 지급 대상)
     * @param ownedItemId 판매할 보유 아이템 PK
     * @throws EquipConflictException 장착 중인 장비 판매 시도 시
     */
    @Transactional
    public void sell(final CharacterProgress progress, final long ownedItemId) {
        final OwnedItem owned =
                ownedItemRepository
                        .findById(ownedItemId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "보유 아이템을 찾을 수 없습니다: " + ownedItemId));

        if (isItemEquippedOrAssigned(progress, owned)) {
            final String conflictMsg =
                    gameMessageService.get("exception.equip.unequip_before_sell");
            throw new EquipConflictException(conflictMsg);
        }

        final Item catalogItem =
                itemCatalogService
                        .byId(owned.getItemId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "카탈로그에 아이템이 없습니다: " + owned.getItemId()));

        final long sellValue = sellValueOf(owned);

        owned.decreaseQuantity(1);
        if (owned.getQuantity() == 0) {
            ownedItemRepository.delete(owned);
        }

        progress.gainGold(sellValue);
        final String sellMsg = gameMessageService.get("log.shop.sell", catalogItem.name());
        actionLog.add(sellMsg, LOG_TYPE_ITEM);
    }

    private boolean isItemEquippedOrAssigned(
            final CharacterProgress progress, final OwnedItem owned) {
        if (owned.isEquipped()) {
            return true;
        }
        if (progress == null) {
            return false;
        }
        final Long id = owned.getId();
        return id.equals(progress.getWeapon1MainId())
                || id.equals(progress.getWeapon1OffId())
                || id.equals(progress.getWeapon2MainId())
                || id.equals(progress.getWeapon2OffId());
    }
}
