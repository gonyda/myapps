package com.myapps.web.myrpg.application.service.inventory;

import com.myapps.web.myrpg.application.dto.BankView;
import com.myapps.web.myrpg.application.dto.EquipmentSlotView;
import com.myapps.web.myrpg.application.dto.EquipmentView;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.application.dto.InventoryView;
import com.myapps.web.myrpg.application.dto.OwnedItemView;
import com.myapps.web.myrpg.application.service.ItemCatalogService;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipSlot;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 인벤토리, 은행, 장비 팝업 뷰 조립과 아이템 상세 설명(describe) 생성을 전담하는 협력 객체. */
public final class InventoryViewAssembler {

    private final InventoryQuerySupport query;
    private final ItemCatalogService itemCatalogService;
    private final OwnedItemRepository ownedItemRepository;
    private final GameMessageService gameMessageService;
    private final EquipmentBonusService equipmentBonusService;

    /**
     * InventoryViewAssembler를 생성한다.
     *
     * @param query 공유 조회 지원 컴포넌트
     * @param itemCatalogService 아이템 카탈로그 서비스
     * @param ownedItemRepository 보유 아이템 저장소
     * @param gameMessageService 게임 메시지 다국어 서비스
     * @param equipmentBonusService 장비 보너스 서비스
     */
    public InventoryViewAssembler(
            final InventoryQuerySupport query,
            final ItemCatalogService itemCatalogService,
            final OwnedItemRepository ownedItemRepository,
            final GameMessageService gameMessageService,
            final EquipmentBonusService equipmentBonusService) {
        this.query = query;
        this.itemCatalogService = itemCatalogService;
        this.ownedItemRepository = ownedItemRepository;
        this.gameMessageService = gameMessageService;
        this.equipmentBonusService = equipmentBonusService;
    }

    /**
     * 기본 캐릭터 인벤토리 팝업 뷰를 조립한다.
     *
     * @param gold 캐릭터 보유 골드
     * @return 인벤토리 뷰 (보유 골드 + 아이템 목록)
     */
    public InventoryView buildInventoryView(final long gold) {
        return buildInventoryView(null, gold);
    }

    /**
     * 특정 캐릭터의 인벤토리 팝업 뷰를 조립한다.
     *
     * <p>인벤토리({@code storage=INVENTORY}) 아이템을 획득순(id 오름차순)으로 조회하고 각 행을 {@link OwnedItemView}로 변환하여
     * 보유 골드와 함께 반환한다.
     *
     * @param characterId 캐릭터 식별자 (null일 경우 전체/기본)
     * @param gold 캐릭터 보유 골드
     * @return 인벤토리 뷰 (보유 골드 + 아이템 목록)
     */
    public InventoryView buildInventoryView(final Long characterId, final long gold) {
        final List<OwnedItem> inventoryItems =
                characterId != null
                        ? ownedItemRepository.findByCharacterIdAndStorageOrderById(
                                characterId, StorageKind.INVENTORY)
                        : ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY);
        final Set<Long> otherSetAssignedItemIds = query.resolveOtherSetAssignedItemIds(characterId);
        final List<OwnedItemView> views =
                inventoryItems.stream()
                        .map(owned -> toOwnedItemView(owned, otherSetAssignedItemIds))
                        .toList();
        return new InventoryView(gold, views);
    }

    /**
     * 기본 캐릭터 은행 팝업 뷰를 조립한다.
     *
     * @param gold 캐릭터 보유 골드
     * @param bankGold 은행 보관 골드
     * @return 은행 뷰 (은행·보유 골드 + 은행/소지품 목록)
     */
    public BankView buildBankView(final long gold, final long bankGold) {
        return buildBankView(null, gold, bankGold);
    }

    /**
     * 특정 캐릭터의 은행 팝업 뷰를 조립한다.
     *
     * <p>은행({@code storage=BANK})과 소지품({@code storage=INVENTORY}) 아이템을 각각 획득순(id 오름차순)으로 조회하고 행 뷰로
     * 변환하여 은행/보유 골드와 함께 반환한다.
     *
     * @param characterId 캐릭터 식별자 (null일 경우 전체/기본)
     * @param gold 캐릭터 보유 골드
     * @param bankGold 은행 보관 골드
     * @return 은행 뷰 (은행·보유 골드 + 은행/소지품 목록)
     */
    public BankView buildBankView(final Long characterId, final long gold, final long bankGold) {
        final List<OwnedItem> bankItems =
                characterId != null
                        ? ownedItemRepository.findByCharacterIdAndStorageOrderById(
                                characterId, StorageKind.BANK)
                        : ownedItemRepository.findByStorageOrderById(StorageKind.BANK);
        final List<OwnedItem> inventoryItems =
                characterId != null
                        ? ownedItemRepository.findByCharacterIdAndStorageOrderById(
                                characterId, StorageKind.INVENTORY)
                        : ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY);

        final Set<Long> otherSetAssignedItemIds = query.resolveOtherSetAssignedItemIds(characterId);
        final List<OwnedItemView> bankViews =
                bankItems.stream().map(this::toOwnedItemView).toList();
        final List<OwnedItemView> inventoryViews =
                inventoryItems.stream()
                        .map(owned -> toOwnedItemView(owned, otherSetAssignedItemIds))
                        .toList();

        return new BankView(bankGold, gold, bankViews, inventoryViews);
    }

    /**
     * 기본 캐릭터의 장비 팝업 뷰를 조립한다.
     *
     * @return 장비 팝업 뷰
     */
    public EquipmentView buildEquipmentView() {
        return buildEquipmentView(null);
    }

    /**
     * 특정 캐릭터의 장비 팝업 뷰를 조립한다.
     *
     * <p>3x3 매트릭스 순서(ACC1, HEAD, ACC2, MAIN_HAND, BODY, OFF_HAND, HANDS, FEET, ROBE)로 슬롯을 구성하고, 장착
     * 장비의 보너스 합산과 평균 내구도를 산출한다.
     *
     * @param characterId 캐릭터 식별자
     * @return 장비 팝업 뷰
     */
    public EquipmentView buildEquipmentView(final Long characterId) {
        final List<OwnedItem> equippedItems = query.findEquippedInventoryItems(characterId);
        final Map<EquipSlot, OwnedItem> slotMap = query.resolveEquippedSlotMap(equippedItems);
        final boolean mainHandIsTwoHanded = isMainHandTwoHanded(slotMap.get(EquipSlot.MAIN_HAND));
        final List<EquipmentSlotView> slots = assembleEquipmentSlots(slotMap, mainHandIsTwoHanded);

        final EquippedBonusResult bonusResult = equipmentBonusService.equippedBonus(characterId);
        final int equippedCount = (int) slots.stream().filter(EquipmentSlotView::equipped).count();
        final int averageDurabilityPercent = calculateAverageDurability(slots);
        final String weaponTalentLabel = resolveWeaponTalentLabel(characterId);
        final CharacterProgress progress = query.findCharacterSafe(characterId);
        final int activeWeaponSet = progress != null ? progress.getActiveWeaponSet() : 1;

        return new EquipmentView(
                slots,
                bonusResult,
                equippedCount,
                averageDurabilityPercent,
                weaponTalentLabel,
                activeWeaponSet);
    }

    private boolean isMainHandTwoHanded(final OwnedItem mainHandOwned) {
        final Optional<EquipmentItem> mainOpt = query.resolveEquipment(mainHandOwned);
        return mainOpt.isPresent()
                && mainOpt.get().kind().requiredSlots().contains(EquipSlot.OFF_HAND);
    }

    private List<EquipmentSlotView> assembleEquipmentSlots(
            final Map<EquipSlot, OwnedItem> slotMap, final boolean mainHandIsTwoHanded) {
        final List<EquipmentSlotView> slots = new ArrayList<>(9);
        slots.add(EquipmentSlotView.locked("ACC1", "악세사리 1", "💍"));
        slots.add(toSlotView(EquipSlot.HEAD, "머리", "🪖", slotMap.get(EquipSlot.HEAD), false));
        slots.add(EquipmentSlotView.locked("ACC2", "악세사리 2", "💍"));

        slots.add(
                toSlotView(
                        EquipSlot.MAIN_HAND,
                        "주무기",
                        "🗡️",
                        slotMap.get(EquipSlot.MAIN_HAND),
                        false));
        slots.add(toSlotView(EquipSlot.BODY, "갑옷", "🥋", slotMap.get(EquipSlot.BODY), false));
        if (slotMap.containsKey(EquipSlot.OFF_HAND)) {
            slots.add(
                    toSlotView(
                            EquipSlot.OFF_HAND,
                            "보조손",
                            "🛡️",
                            slotMap.get(EquipSlot.OFF_HAND),
                            false));
        } else if (mainHandIsTwoHanded) {
            slots.add(EquipmentSlotView.blockedByTwoHanded("OFF_HAND", "보조손", "🛡️"));
        } else {
            slots.add(EquipmentSlotView.empty("OFF_HAND", "보조손", "🛡️"));
        }

        slots.add(toSlotView(EquipSlot.HANDS, "손", "🧤", slotMap.get(EquipSlot.HANDS), false));
        slots.add(toSlotView(EquipSlot.FEET, "발", "👢", slotMap.get(EquipSlot.FEET), false));
        slots.add(EquipmentSlotView.locked("ROBE", "로브", "🧥"));
        return List.copyOf(slots);
    }

    private static int calculateAverageDurability(final List<EquipmentSlotView> slots) {
        int totalDurabilityPercent = 0;
        int equippedWithDurability = 0;
        for (final EquipmentSlotView slot : slots) {
            if (slot.equipped() && slot.maxDurability() != null && slot.maxDurability() > 0) {
                totalDurabilityPercent += slot.durabilityPercent();
                equippedWithDurability++;
            }
        }
        return equippedWithDurability > 0 ? totalDurabilityPercent / equippedWithDurability : 100;
    }

    private String resolveWeaponTalentLabel(final Long characterId) {
        final SkillTalent weaponTalent = query.resolveEquippedWeaponTalent(characterId);
        if (weaponTalent != null && weaponTalent.matchingTalent().isPresent()) {
            return weaponTalent.matchingTalent().get().label();
        }
        return (weaponTalent == SkillTalent.COMMON) ? "공용" : "맨손";
    }

    private EquipmentSlotView toSlotView(
            final EquipSlot slot,
            final String label,
            final String defaultIcon,
            final OwnedItem owned,
            final boolean blocked) {
        if (blocked) {
            return EquipmentSlotView.blockedByTwoHanded(slot.name(), label, defaultIcon);
        }
        if (owned == null) {
            return EquipmentSlotView.empty(slot.name(), label, defaultIcon);
        }

        final Optional<EquipmentItem> equipOpt = query.resolveEquipment(owned);
        if (equipOpt.isEmpty()) {
            return EquipmentSlotView.empty(slot.name(), label, defaultIcon);
        }
        final EquipmentItem equipItem = equipOpt.get();

        final int maxDura = owned.effectiveMaxDurability(equipItem.maxDurability());
        final int currentDura = (int) Math.ceil(owned.getCurrentDurability());
        final int duraPercent =
                maxDura > 0
                        ? Math.min(
                                100,
                                Math.max(
                                        0,
                                        (int)
                                                Math.round(
                                                        (owned.getCurrentDurability() / maxDura)
                                                                * 100.0)))
                        : 0;
        final String duraStatus =
                duraPercent <= 20 ? "danger" : (duraPercent <= 50 ? "warning" : "normal");

        final String icon = equipItem.icon();
        final String bonusSummary = formatBonusesSummary(equipItem.bonuses());
        final String detail = String.join("||", describe(equipItem, owned));

        return new EquipmentSlotView(
                slot.name(),
                label,
                defaultIcon,
                true,
                false,
                false,
                owned.getId(),
                equipItem.name(),
                icon,
                equipItem.kind().label(),
                currentDura,
                maxDura,
                duraPercent,
                duraStatus,
                bonusSummary,
                detail);
    }

    private String formatBonusesSummary(final List<EquipBonus> bonuses) {
        if (bonuses == null || bonuses.isEmpty()) {
            return "보너스 없음";
        }
        final List<String> parts = new ArrayList<>();
        for (final EquipBonus b : bonuses) {
            parts.add(formatBonus(b));
        }
        return String.join(", ", parts);
    }

    /**
     * 아이템 상세 설명을 자동 생성한다.
     *
     * <p>포션이면 회복량 문구를 포함하고, 장비이면 보너스 한 줄씩·장비 종류· 내구도·양손 무기 배타 안내를 포함한다.
     *
     * @param item 카탈로그 아이템
     * @param owned 보유 아이템 인스턴스
     * @return 상세 설명 줄 목록
     */
    public List<String> describe(final Item item, final OwnedItem owned) {
        final List<String> lines = new ArrayList<>();

        if (item.description() != null && !item.description().isBlank()) {
            lines.add(item.description());
        }

        if (item instanceof EquipmentItem equipItem) {
            appendEquipmentDescription(equipItem, owned, lines);
        }

        return List.copyOf(lines);
    }

    /**
     * 보유 아이템을 {@link OwnedItemView}로 변환한다.
     *
     * @param owned 보유 아이템 인스턴스
     * @return 변환된 아이템 뷰
     */
    public OwnedItemView toOwnedItemView(final OwnedItem owned) {
        return toOwnedItemView(owned, java.util.Set.of());
    }

    /**
     * 보유 아이템을 다른 무기 세트에 할당된 아이템 ID 세트를 반영하여 {@link OwnedItemView}로 변환한다.
     *
     * @param owned 보유 아이템 인스턴스
     * @param otherSetAssignedItemIds 다른 무기세트 등록 ID 세트
     * @return 변환된 아이템 뷰
     */
    public OwnedItemView toOwnedItemView(
            final OwnedItem owned, final Set<Long> otherSetAssignedItemIds) {
        final Item catalogItem =
                itemCatalogService
                        .byId(owned.getItemId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "카탈로그에 아이템이 없습니다: " + targetItemId(owned)));

        final boolean usable = catalogItem.type() == ItemType.POTION;
        final boolean equippable = catalogItem.type().isEquipment();
        final Double currentDurability = equippable ? owned.getCurrentDurability() : null;
        final Integer maxDurability =
                (catalogItem instanceof EquipmentItem equipItem)
                        ? owned.effectiveMaxDurability(equipItem.maxDurability())
                        : null;
        final List<String> detailLines = describe(catalogItem, owned);

        final boolean isEquipped =
                owned.isEquipped()
                        || (otherSetAssignedItemIds != null
                                && otherSetAssignedItemIds.contains(owned.getId()));

        return new OwnedItemView(
                owned.getId(),
                catalogItem.name(),
                catalogItem.type().label(),
                catalogItem.type(),
                owned.getQuantity(),
                isEquipped,
                usable,
                equippable,
                currentDurability,
                maxDurability,
                detailLines,
                catalogItem.icon());
    }

    private String targetItemId(final OwnedItem owned) {
        return owned != null ? owned.getItemId() : "null";
    }

    private void appendEquipmentDescription(
            final EquipmentItem equipItem, final OwnedItem owned, final List<String> lines) {
        final String kindType =
                gameMessageService.get(
                        "describe.equip.kind_type",
                        equipItem.kind().label(),
                        equipItem.type().label());
        lines.add(kindType);

        for (final EquipBonus bonus : equipItem.bonuses()) {
            lines.add(formatBonus(bonus));
        }

        if (equipItem.kind().primarySlot() == EquipSlot.MAIN_HAND
                && equipItem.kind().requiredSlots().contains(EquipSlot.OFF_HAND)) {
            final String shieldConflict = gameMessageService.get("exception.equip.shield_conflict");
            lines.add(shieldConflict);
        }

        lines.add(buildDurabilityText(equipItem, owned));
    }

    private String buildDurabilityText(final EquipmentItem equipItem, final OwnedItem owned) {
        final int maxDura =
                owned != null
                        ? owned.effectiveMaxDurability(equipItem.maxDurability())
                        : equipItem.maxDurability();
        final double currentDura =
                owned != null ? owned.getCurrentDurability() : equipItem.maxDurability();
        final String currentDuraStr = formatDurability(currentDura);

        return gameMessageService.get("describe.equip.durability", currentDuraStr, maxDura);
    }

    /**
     * 장비 보너스를 설명 문자열로 서식화한다.
     *
     * @param bonus 장비 보너스
     * @return 서식화된 문자열
     */
    public String formatBonus(final EquipBonus bonus) {
        final String targetLabel = bonusTargetLabel(bonus.target());
        if (bonus.target() == BonusTarget.CRITICAL) {
            final double percent = bonus.amount() * 0.1;
            return targetLabel + " +" + percent + "%";
        }
        return targetLabel + " +" + bonus.amount();
    }

    private String bonusTargetLabel(final BonusTarget target) {
        return switch (target) {
            case STR -> "STR";
            case DEX -> "DEX";
            case INT -> "INT";
            case CRITICAL -> "CRIT";
            case DEF -> "DEF";
            case HP -> "HP";
            case MP -> "MP";
            case STAMINA -> "Stamina";
            case MP_REGEN -> "MP Regen";
        };
    }

    private String formatDurability(final double currentDurability) {
        return String.valueOf((int) Math.ceil(currentDurability));
    }
}
