package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.BankView;
import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DroppedItem;
import com.myapps.web.myrpg.application.dto.EquipmentSlotView;
import com.myapps.web.myrpg.application.dto.EquipmentView;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.application.dto.InventoryView;
import com.myapps.web.myrpg.application.dto.OwnedItemView;
import com.myapps.web.myrpg.application.exception.EquipConflictException;
import com.myapps.web.myrpg.application.exception.InventoryFullException;
import com.myapps.web.myrpg.config.GameProperties;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BonusKind;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipSlot;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.PassiveSkill;
import com.myapps.web.myrpg.domain.model.PotionItem;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRankupBonus;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인벤토리 핵심 비즈니스 로직을 담당하는 서비스.
 *
 * <p>장비 착용/해제, 포션 사용, 은행 ↔ 인벤토리 아이템 이동, 장착 장비 보너스 합산, 신규 캐릭터 기본 지급, 아이템 상세 생성을 처리한다. 슬롯 점유 기반 충돌
 * 검사로 착용 규칙을 구현하며, 소비형(POTION)은 스택 누적, 장비(WEAPON/ARMOR)는 개별 인스턴스로 관리한다.
 */
@Service
@SuppressWarnings("PMD.CyclomaticComplexity")
public class InventoryService {

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

    private final OwnedItemRepository ownedItemRepository;
    private final ItemCatalogService itemCatalogService;
    private final CharacterProgressRepository characterProgressRepository;
    private final StatProgression statProgression;
    private final ActionLog actionLog;
    private final SkillCatalogService skillCatalogService;
    private final CharacterSkillRepository characterSkillRepository;
    private final SkillRankupBonus skillRankupBonus;
    private final GameMessageService gameMessageService;
    private final GameProperties gameProperties;

    /** InventoryService를 생성한다 (Spring 주입용). */
    @org.springframework.beans.factory.annotation.Autowired
    public InventoryService(
            final OwnedItemRepository ownedItemRepository,
            final ItemCatalogService itemCatalogService,
            final CharacterProgressRepository characterProgressRepository,
            final StatProgression statProgression,
            final ActionLog actionLog,
            final SkillCatalogService skillCatalogService,
            final CharacterSkillRepository characterSkillRepository,
            final GameMessageService gameMessageService,
            final GameProperties gameProperties) {
        this.ownedItemRepository = ownedItemRepository;
        this.itemCatalogService = itemCatalogService;
        this.characterProgressRepository = characterProgressRepository;
        this.statProgression = statProgression;
        this.actionLog = actionLog;
        this.skillCatalogService = skillCatalogService;
        this.characterSkillRepository = characterSkillRepository;
        this.skillRankupBonus = new SkillRankupBonus();
        this.gameMessageService =
                gameMessageService != null ? gameMessageService : new GameMessageService(null);
        this.gameProperties =
                gameProperties != null
                        ? gameProperties
                        : new GameProperties(null, null, null, null, null, null);
    }

    /** 이전 호환용 생성자. */
    public InventoryService(
            final OwnedItemRepository ownedItemRepository,
            final ItemCatalogService itemCatalogService,
            final CharacterProgressRepository characterProgressRepository,
            final StatProgression statProgression,
            final ActionLog actionLog,
            final SkillCatalogService skillCatalogService,
            final CharacterSkillRepository characterSkillRepository) {
        this(
                ownedItemRepository,
                itemCatalogService,
                characterProgressRepository,
                statProgression,
                actionLog,
                skillCatalogService,
                characterSkillRepository,
                new GameMessageService(null),
                new GameProperties(null, null, null, null, null, null));
    }

    /**
     * 장비를 착용한다.
     *
     * <p>착용 규칙: 대상 장비의 {@code requiredSlots} 각 슬롯을 점유한 장착 장비를 조사한다. 점유 장비의 {@code primarySlot}이 대상과
     * 다르면 충돌({@link EquipConflictException}), 같으면 해제 후 착용(스왑)한다.
     *
     * @param ownedItemId 착용할 보유 아이템 PK
     * @throws EquipConflictException 착용 슬롯 충돌 시
     */
    @Transactional
    public void equip(final long ownedItemId) {
        final OwnedItem target = findOwnedItemOrThrow(ownedItemId);
        final EquipmentItem equipmentItem = resolveEquipmentItem(target);
        final EquipmentKind targetKind = equipmentItem.kind();
        final Set<EquipSlot> requiredSlots = targetKind.requiredSlots();

        final List<OwnedItem> equippedItems = findEquippedInventoryItems(target.getCharacterId());

        for (final EquipSlot slot : requiredSlots) {
            final Optional<OwnedItem> occupier = findSlotOccupier(equippedItems, slot, target);
            if (occupier.isPresent()) {
                final EquipmentItem occupierEquip = resolveEquipmentItem(occupier.get());
                if (occupierEquip.kind().primarySlot() != targetKind.primarySlot()) {
                    throw new EquipConflictException("착용 할 수 없습니다.");
                }
            }
        }

        unequipSamePrimarySlot(equippedItems, targetKind.primarySlot(), target);
        target.equip();
    }

    /**
     * 장비를 장착 해제한다.
     *
     * @param ownedItemId 해제할 보유 아이템 PK
     */
    @Transactional
    public void unequip(final long ownedItemId) {
        final OwnedItem target = findOwnedItemOrThrow(ownedItemId);
        target.unequip();

        final CharacterProgress progress = findCharacterSafe(target.getCharacterId());
        if (progress != null) {
            boolean modified = false;
            if (Long.valueOf(ownedItemId).equals(progress.getWeapon1MainId())) {
                progress.setWeapon1MainId(null);
                modified = true;
            }
            if (Long.valueOf(ownedItemId).equals(progress.getWeapon1OffId())) {
                progress.setWeapon1OffId(null);
                modified = true;
            }
            if (Long.valueOf(ownedItemId).equals(progress.getWeapon2MainId())) {
                progress.setWeapon2MainId(null);
                modified = true;
            }
            if (Long.valueOf(ownedItemId).equals(progress.getWeapon2OffId())) {
                progress.setWeapon2OffId(null);
                modified = true;
            }
            if (modified) {
                characterProgressRepository.save(progress);
            }
        }
    }

    /**
     * 포션을 사용하여 HP를 회복한다.
     *
     * <p>HP, MP, Stamina를 각각 {@code min(current + heal, max)}로 회복하고 수량을 1 감소시킨다. 수량이 0이 되면 해당 행을
     * 삭제한다.
     *
     * @param ownedItemId 사용할 포션 보유 아이템 PK
     */
    @Transactional
    public PotionItem usePotion(final long ownedItemId) {
        final OwnedItem target = findOwnedItemOrThrow(ownedItemId);
        final Item catalogItem =
                itemCatalogService
                        .byId(target.getItemId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "카탈로그에 아이템이 없습니다: " + target.getItemId()));

        if (!(catalogItem instanceof PotionItem potionItem)) {
            throw new IllegalStateException("포션이 아닌 아이템은 사용할 수 없습니다: " + target.getItemId());
        }

        final CharacterProgress character = loadCharacter(target.getCharacterId());
        final VitalMax vitalMax =
                statProgression.vitalMaxFor(character.getCurrentLevel(), character.getTalent());
        final VitalMax equipVitalBonus = equippedBonus(character.getId()).vitalBonus();
        final List<CharacterSkill> ownedSkills =
                characterSkillRepository.findByCharacterId(character.getId());
        final VitalMax skillVitalBonus =
                skillRankupBonus.sumVital(ownedSkills, skillCatalogService::byId);
        final int hpMax = vitalMax.hp() + equipVitalBonus.hp() + skillVitalBonus.hp();
        final int mpMax = vitalMax.mp() + equipVitalBonus.mp() + skillVitalBonus.mp();
        final int staminaMax =
                vitalMax.stamina() + equipVitalBonus.stamina() + skillVitalBonus.stamina();

        final int healedHp = Math.min(character.getHpCurrent() + potionItem.healHp(), hpMax);
        final int healedMp = Math.min(character.getMpCurrent() + potionItem.healMp(), mpMax);
        final int healedStamina =
                Math.min(character.getStaminaCurrent() + potionItem.healStamina(), staminaMax);

        character.fullRecover(new VitalMax(healedHp, healedMp, healedStamina));

        target.decreaseQuantity(1);
        if (target.getQuantity() == 0) {
            ownedItemRepository.delete(target);
        }
        return potionItem;
    }

    /**
     * 인벤토리에서 은행으로 아이템을 맡긴다.
     *
     * <p>장착 중인 장비는 맡기기를 거부한다. 소비형(POTION)은 은행의 동일 itemId 행에 스택 누적하며, 장비는 저장위치를 전환한다. 은행 용량(30)을
     * 초과하면 거부한다.
     *
     * @param ownedItemId 맡길 보유 아이템 PK
     * @throws EquipConflictException 장착 중 장비 맡기기 시도 시
     * @throws InventoryFullException 은행 용량 초과 시
     */
    @Transactional
    public void moveToBank(final long ownedItemId) {
        final OwnedItem target = findOwnedItemOrThrow(ownedItemId);

        final Set<Long> otherSetIds = resolveOtherSetAssignedItemIds(target.getCharacterId());
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
            checkCapacity(target.getCharacterId(), StorageKind.BANK);
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
    @Transactional
    public void moveToInventory(final long ownedItemId) {
        final OwnedItem target = findOwnedItemOrThrow(ownedItemId);

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
            checkCapacity(target.getCharacterId(), StorageKind.INVENTORY);
            target.moveTo(StorageKind.INVENTORY);
        }
    }

    /**
     * 장착 중인 장비의 보너스를 합산하여 STAT/VITAL로 분기한 결과를 반환한다.
     *
     * <p>{@code storage=INVENTORY && equipped=true}인 장비의 {@link EquipBonus}를 {@link
     * BonusTarget#kind()}로 분기하여 STAT 계열은 {@link Stats}로, VITAL 계열은 {@link VitalMax}로 합산한다.
     *
     * @return 장비 STAT 보너스와 VITAL 보너스를 담은 결과
     */
    /**
     * 기본 캐릭터의 장착 장비 보너스 합산을 계산한다.
     *
     * @return 장비 STAT 보너스와 VITAL 보너스를 담은 결과
     */
    public EquippedBonusResult equippedBonus() {
        return equippedBonus(null);
    }

    /**
     * 특정 캐릭터의 장착 장비 보너스 합산을 계산한다.
     *
     * <p>인벤토리({@code storage=INVENTORY})에서 장착 중인({@code equipped=true}) 장비의 {@link EquipBonus} 목록을
     * 순회하여 {@link BonusTarget#kind()}로 분기하여 STAT 계열은 {@link Stats}로, VITAL 계열은 {@link VitalMax}로
     * 합산한다.
     *
     * @param characterId 캐릭터 식별자 (null일 경우 전체/기본)
     * @return 장비 STAT 보너스와 VITAL 보너스를 담은 결과
     */
    public EquippedBonusResult equippedBonus(final Long characterId) {
        final List<OwnedItem> equippedItems = findEquippedInventoryItems(characterId);

        Stats statBonus = Stats.ZERO;
        VitalMax vitalBonus = new VitalMax(0, 0, 0);

        for (final OwnedItem owned : equippedItems) {
            final Optional<Item> catalogOpt = itemCatalogService.byId(owned.getItemId());
            if (catalogOpt.isEmpty()) {
                continue;
            }
            final Item catalogItem = catalogOpt.get();
            if (!(catalogItem instanceof EquipmentItem equipItem)) {
                continue;
            }
            for (final EquipBonus bonus : equipItem.bonuses()) {
                if (bonus.target().kind() == BonusKind.STAT) {
                    statBonus = applyStatDelta(statBonus, bonus.target(), bonus.amount());
                } else {
                    vitalBonus = applyVitalDelta(vitalBonus, bonus.target(), bonus.amount());
                }
            }
        }

        return new EquippedBonusResult(statBonus, vitalBonus);
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
        final Set<Long> otherSetAssignedItemIds = resolveOtherSetAssignedItemIds(characterId);
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

        final Set<Long> otherSetAssignedItemIds = resolveOtherSetAssignedItemIds(characterId);
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
        final List<OwnedItem> equippedItems = findEquippedInventoryItems(characterId);
        final Map<EquipSlot, OwnedItem> slotMap = resolveEquippedSlotMap(equippedItems);
        final boolean mainHandIsTwoHanded = isMainHandTwoHanded(slotMap.get(EquipSlot.MAIN_HAND));
        final List<EquipmentSlotView> slots = assembleEquipmentSlots(slotMap, mainHandIsTwoHanded);

        final EquippedBonusResult bonusResult = equippedBonus(characterId);
        final int equippedCount = (int) slots.stream().filter(EquipmentSlotView::equipped).count();
        final int averageDurabilityPercent = calculateAverageDurability(slots);
        final String weaponTalentLabel = resolveWeaponTalentLabel(characterId);
        final CharacterProgress progress = findCharacterSafe(characterId);
        final int activeWeaponSet = progress != null ? progress.getActiveWeaponSet() : 1;

        return new EquipmentView(
                slots,
                bonusResult,
                equippedCount,
                averageDurabilityPercent,
                weaponTalentLabel,
                activeWeaponSet);
    }

    private List<OwnedItem> findEquippedInventoryItems(final Long characterId) {
        List<OwnedItem> items = null;
        if (characterId != null) {
            items =
                    ownedItemRepository.findByCharacterIdAndStorageAndEquippedTrue(
                            characterId, StorageKind.INVENTORY);
        }
        if (items == null) {
            items = ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY);
        }
        return items != null ? items : List.of();
    }

    private Map<EquipSlot, OwnedItem> resolveEquippedSlotMap(final List<OwnedItem> equippedItems) {
        final Map<EquipSlot, OwnedItem> slotMap = new EnumMap<>(EquipSlot.class);
        for (final OwnedItem owned : equippedItems) {
            final Optional<Item> catalogOpt = itemCatalogService.byId(owned.getItemId());
            if (catalogOpt.isPresent() && catalogOpt.get() instanceof EquipmentItem equipItem) {
                slotMap.put(equipItem.kind().primarySlot(), owned);
            }
        }
        return slotMap;
    }

    private boolean isMainHandTwoHanded(final OwnedItem mainHandOwned) {
        if (mainHandOwned == null) {
            return false;
        }
        final Optional<Item> mainOpt = itemCatalogService.byId(mainHandOwned.getItemId());
        return mainOpt.isPresent()
                && mainOpt.get() instanceof EquipmentItem equipItem
                && equipItem.kind().requiredSlots().contains(EquipSlot.OFF_HAND);
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
        final SkillTalent weaponTalent = resolveEquippedWeaponTalent(characterId);
        if (weaponTalent != null && weaponTalent.matchingTalent().isPresent()) {
            return weaponTalent.matchingTalent().get().label();
        }
        return (weaponTalent == SkillTalent.COMMON) ? "공용" : "맨손";
    }

    /**
     * 슬롯 충돌을 자동으로 해결하며 장비를 착용한다.
     *
     * <p>양손무기 착용 시 보조손 장비(방패)를 자동 해제하고, 방패 착용 시 주무기의 양손무기를 자동 해제한 후 착용한다.
     *
     * @param ownedItemId 착용할 보유 아이템 PK
     */
    @Transactional
    public void smartEquip(final long ownedItemId) {
        final OwnedItem target = findOwnedItemOrThrow(ownedItemId);
        final EquipmentItem equipmentItem = resolveEquipmentItem(target);
        final EquipmentKind targetKind = equipmentItem.kind();
        final List<OwnedItem> equippedItems = findEquippedInventoryItems(target.getCharacterId());

        autoUnequipShieldForTwoHanded(target, targetKind, equippedItems);
        autoUnequipTwoHandedForShield(target, targetKind, equippedItems);

        equip(ownedItemId);

        final CharacterProgress progress = findCharacterSafe(target.getCharacterId());
        if (progress != null) {
            clearFromOtherWeaponSet(progress, target.getId());
            if (targetKind.primarySlot() == EquipSlot.MAIN_HAND) {
                progress.setActiveMainWeaponId(target.getId());
                if (targetKind.requiredSlots().contains(EquipSlot.OFF_HAND)) {
                    progress.setActiveOffWeaponId(null);
                }
                characterProgressRepository.save(progress);
            } else if (targetKind.primarySlot() == EquipSlot.OFF_HAND) {
                progress.setActiveOffWeaponId(target.getId());
                characterProgressRepository.save(progress);
            }
        }
    }

    private void clearFromOtherWeaponSet(final CharacterProgress progress, final Long ownedItemId) {
        if (progress == null || ownedItemId == null) {
            return;
        }
        final int activeSet = progress.getActiveWeaponSet();
        if (activeSet == 1) {
            if (Long.valueOf(ownedItemId).equals(progress.getWeapon2MainId())) {
                progress.setWeapon2MainId(null);
            }
            if (Long.valueOf(ownedItemId).equals(progress.getWeapon2OffId())) {
                progress.setWeapon2OffId(null);
            }
        } else {
            if (Long.valueOf(ownedItemId).equals(progress.getWeapon1MainId())) {
                progress.setWeapon1MainId(null);
            }
            if (Long.valueOf(ownedItemId).equals(progress.getWeapon1OffId())) {
                progress.setWeapon1OffId(null);
            }
        }
    }

    private void autoUnequipShieldForTwoHanded(
            final OwnedItem target,
            final EquipmentKind targetKind,
            final List<OwnedItem> equippedItems) {
        if (!targetKind.requiredSlots().contains(EquipSlot.OFF_HAND)
                || targetKind.primarySlot() != EquipSlot.MAIN_HAND) {
            return;
        }
        for (final OwnedItem equipped : equippedItems) {
            if (equipped.getId().equals(target.getId())) {
                continue;
            }
            final Optional<Item> catalogOpt = itemCatalogService.byId(equipped.getItemId());
            if (catalogOpt.isPresent()
                    && catalogOpt.get() instanceof EquipmentItem equipItem
                    && equipItem.kind().primarySlot() == EquipSlot.OFF_HAND) {
                equipped.unequip();
            }
        }
    }

    private void autoUnequipTwoHandedForShield(
            final OwnedItem target,
            final EquipmentKind targetKind,
            final List<OwnedItem> equippedItems) {
        if (targetKind.primarySlot() != EquipSlot.OFF_HAND) {
            return;
        }
        for (final OwnedItem equipped : equippedItems) {
            if (equipped.getId().equals(target.getId())) {
                continue;
            }
            final Optional<Item> catalogOpt = itemCatalogService.byId(equipped.getItemId());
            if (catalogOpt.isPresent()
                    && catalogOpt.get() instanceof EquipmentItem equipItem
                    && equipItem.kind().primarySlot() == EquipSlot.MAIN_HAND
                    && equipItem.kind().requiredSlots().contains(EquipSlot.OFF_HAND)) {
                equipped.unequip();
            }
        }
    }

    /**
     * 특정 슬롯에 착용 가능한 인벤토리 아이템 목록을 조회한다.
     *
     * @param characterId 캐릭터 PK
     * @param slotCode 슬롯 코드 (예: "HEAD", "BODY", "MAIN_HAND", "OFF_HAND", "HANDS", "FEET")
     * @return 착용 가능한 미장착 아이템 뷰 목록
     */
    public List<OwnedItemView> findEquippableForSlot(
            final Long characterId, final String slotCode) {
        final EquipSlot targetSlot;
        try {
            targetSlot = EquipSlot.valueOf(slotCode.toUpperCase());
        } catch (final IllegalArgumentException e) {
            return List.of();
        }

        final List<OwnedItem> inventoryItems =
                characterId != null
                        ? ownedItemRepository.findByCharacterIdAndStorageOrderById(
                                characterId, StorageKind.INVENTORY)
                        : ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY);

        final Set<Long> otherSetAssignedItemIds = resolveOtherSetAssignedItemIds(characterId);

        return inventoryItems.stream()
                .filter(owned -> !owned.isEquipped())
                .filter(owned -> !otherSetAssignedItemIds.contains(owned.getId()))
                .filter(
                        owned -> {
                            final Optional<Item> catalogOpt =
                                    itemCatalogService.byId(owned.getItemId());
                            if (catalogOpt.isEmpty()
                                    || !(catalogOpt.get() instanceof EquipmentItem equipItem)) {
                                return false;
                            }
                            return equipItem.kind().primarySlot() == targetSlot;
                        })
                .map(this::toOwnedItemView)
                .toList();
    }

    private Set<Long> resolveOtherSetAssignedItemIds(final Long characterId) {
        final CharacterProgress progress = findCharacterSafe(characterId);
        if (progress == null) {
            return Set.of();
        }
        final Set<Long> ids = new HashSet<>();
        final int activeSet = progress.getActiveWeaponSet();
        if (activeSet == 1) {
            if (progress.getWeapon2MainId() != null) {
                ids.add(progress.getWeapon2MainId());
            }
            if (progress.getWeapon2OffId() != null) {
                ids.add(progress.getWeapon2OffId());
            }
        } else {
            if (progress.getWeapon1MainId() != null) {
                ids.add(progress.getWeapon1MainId());
            }
            if (progress.getWeapon1OffId() != null) {
                ids.add(progress.getWeapon1OffId());
            }
        }
        return Set.copyOf(ids);
    }

    /** 기본 캐릭터(1L)에 모든 초보자용 장비와 기본 소비품을 지급하고 기본 장비를 장착한다. */
    @Transactional
    public void seedDefault() {
        seedDefault(1L);
    }

    /**
     * 특정 캐릭터에 모든 초보자용 장비와 기본 소비품을 지급하고 기본 장비를 장착한다.
     *
     * <p>초보자 무기 5종(한손검·양손검·활·완드·스태프)과 방어구 5종 (방패·갑옷·투구·장갑·부츠) 각 1개(내구도 20), 생명력 30 포션 5개, 마나 30 포션
     * 5개, 스태미나 30 포션 5개를 인벤토리에 생성한다. 이 중 한손검·방패·갑옷·투구·장갑·부츠를 기본 장착하고 나머지 무기는 미장착 상태로 지급한다.
     *
     * @param characterId 아이템을 지급할 대상 캐릭터 ID
     */
    @Transactional
    public void seedDefault(final Long characterId) {
        final Long targetCharId = characterId != null ? characterId : 1L;
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
     * 초보자용 장비 1개를 최대 내구도로 인벤토리에 지급한다.
     *
     * @param characterId 대상 캐릭터 ID
     * @param itemId 지급할 장비 아이템 id
     * @param equipped 기본 장착 여부
     */
    private void seedEquipment(
            final Long characterId, final String itemId, final boolean equipped) {
        ownedItemRepository.save(
                new OwnedItem(
                        characterId,
                        itemId,
                        1,
                        StorageKind.INVENTORY,
                        equipped,
                        resolveMaxDurability(itemId)));
    }

    /**
     * 몬스터 처치 드랍 결과를 인벤토리에 적재한다.
     *
     * <p>골드는 항상 {@code gainGold}로 가산하고, 아이템은 각각 인벤토리에 추가한다. 인벤토리 용량(30)을 초과하는 아이템은 소실시키고 활동 로그에 실패
     * 메시지를 남기며, 나머지 아이템과 골드 처리는 계속한다.
     *
     * @param progress 캐릭터 진행상황 (골드 가산 대상)
     * @param drop 드랍 결과 (골드 + 아이템 목록)
     */
    @Transactional
    public void acquire(final CharacterProgress progress, final DropResult drop) {
        if (drop.gold() > 0) {
            progress.gainGold(drop.gold());
        }

        for (final DroppedItem droppedItem : drop.items()) {
            acquireSingleItem(progress, droppedItem);
        }
    }

    /**
     * 지정된 아이템을 인벤토리에 1개 획득 처리한다.
     *
     * <p>상점 구매 등에서 사용되며, 포션은 기존 스택에 누적되고 장비는 개별 인스턴스로 저장된다. 인벤토리가 가득 찬 경우 {@link
     * InventoryFullException}이 발생한다.
     *
     * @param itemId 획득할 아이템 카탈로그 ID
     * @param quantity 획득 수량 (1 이상)
     * @throws InventoryFullException 인벤토리 용량 초과 시
     */
    @Transactional
    public void acquireItem(final String itemId, final int quantity) {
        acquireItem(null, itemId, quantity);
    }

    /**
     * 특정 캐릭터의 인벤토리에 아이템을 획득 처리한다.
     *
     * @param characterId 대상 캐릭터 식별자
     * @param itemId 획득할 아이템 카탈로그 ID
     * @param quantity 획득 수량
     */
    @Transactional
    public void acquireItem(final Long characterId, final String itemId, final int quantity) {
        final Optional<Item> catalogOpt = itemCatalogService.byId(itemId);
        if (catalogOpt.isEmpty()) {
            return;
        }
        final Item catalogItem = catalogOpt.get();
        final Long targetCharId = characterId != null ? characterId : 1L;

        if (catalogItem.type().isStackable()) {
            acquirePotionItem(targetCharId, itemId, quantity);
        } else {
            checkCapacity(targetCharId, StorageKind.INVENTORY);
            final int maxDurability = resolveMaxDurability(itemId);
            ownedItemRepository.save(
                    new OwnedItem(
                            targetCharId, itemId, 1, StorageKind.INVENTORY, false, maxDurability));
        }
    }

    private void acquirePotionItem(final String itemId, final int quantity) {
        acquirePotionItem(1L, itemId, quantity);
    }

    private void acquirePotionItem(
            final Long characterId, final String itemId, final int quantity) {
        final Long targetCharId = characterId != null ? characterId : 1L;
        final Optional<OwnedItem> existingStack =
                ownedItemRepository.findByCharacterIdAndStorageAndItemId(
                        targetCharId, StorageKind.INVENTORY, itemId);

        if (existingStack.isPresent()) {
            existingStack.get().increaseQuantity(quantity);
        } else {
            checkCapacity(targetCharId, StorageKind.INVENTORY);
            ownedItemRepository.save(
                    new OwnedItem(targetCharId, itemId, quantity, StorageKind.INVENTORY, false, 0));
        }
    }

    /**
     * 현재 착용 무기의 재능에 해당하는 전투 스킬 목록을 조회한다.
     *
     * <p>착용 무기 재능에 해당하는 스킬과 공통(COMMON) 스킬 중 캐릭터가 습득(보유)한 스킬만 {@link BattleSkillButton}으로 반환한다. 무기를
     * 장착하지 않은 경우 공통 스킬(방어)만 반환한다.
     *
     * @param progress 캐릭터 진행상황 (보유 스킬·착용 장비 조회용)
     * @return 전투에서 사용 가능한 스킬 버튼 목록
     */
    /**
     * 전투에서 사용할 10개 스킬 슬롯 버튼 목록을 반환한다.
     *
     * <p>0~9번 10개 슬롯을 순서대로 생성하며, 착용 중인 무기 계열({@code weaponTalent})과 일치하거나 공통(COMMON) 스킬만 {@code
     * enabled=true}로 활성화된다. 착용 무기와 다른 계열의 스킬은 슬롯 자리는 유지하되 {@code enabled=false}로 비활성화된다.
     *
     * @param progress 캐릭터 진행상황 (보유 스킬·착용 장비 조회용)
     * @return 10개 슬롯의 BattleSkillButton 목록
     */
    public List<BattleSkillButton> combatSkills(final CharacterProgress progress) {
        final Long charId = progress != null ? progress.getId() : null;
        final SkillTalent weaponTalent = resolveEquippedWeaponTalent(charId);
        final List<CharacterSkill> ownedSkills = resolveOwnedSkills(charId);

        final BattleSkillButton[] slotArray = new BattleSkillButton[10];
        for (int i = 0; i < 10; i++) {
            slotArray[i] = BattleSkillButton.emptySlot(i);
        }

        for (final CharacterSkill characterSkill : ownedSkills) {
            final Integer slotIdx = characterSkill.getSlotIndex();
            if (slotIdx != null && slotIdx >= 0 && slotIdx < 10) {
                final BattleSkillButton button =
                        buildSlotButton(characterSkill, slotIdx, weaponTalent);
                if (button != null) {
                    slotArray[slotIdx] = button;
                }
            }
        }
        return List.of(slotArray);
    }

    private List<CharacterSkill> resolveOwnedSkills(final Long charId) {
        List<CharacterSkill> ownedSkills = List.of();
        if (charId != null) {
            ownedSkills = characterSkillRepository.findByCharacterId(charId);
        }
        if (ownedSkills == null || ownedSkills.isEmpty()) {
            ownedSkills = characterSkillRepository.findByCharacterId(null);
        }
        return ownedSkills != null ? ownedSkills : List.of();
    }

    private BattleSkillButton buildSlotButton(
            final CharacterSkill characterSkill,
            final int slotIdx,
            final SkillTalent weaponTalent) {
        final Optional<Skill> catalogOpt = skillCatalogService.byId(characterSkill.getSkillId());
        if (catalogOpt.isEmpty() || catalogOpt.get() instanceof PassiveSkill) {
            return null;
        }
        final Skill catalog = catalogOpt.get();
        final boolean isCommon = catalog.talent() == SkillTalent.COMMON;
        final boolean isMatched = weaponTalent != null && catalog.talent() == weaponTalent;
        final boolean enabled = isCommon || isMatched;
        final String disabledReason =
                enabled ? null : (weaponTalent == null ? "무기 장착 필요" : "장착 무기 불일치");
        final int cooldown = characterSkill.getUltimateCooldown();
        final boolean ready = catalog.type() == SkillType.ULTIMATE && cooldown == 0;
        final String icon = resolveSkillIcon(catalog);

        return new BattleSkillButton(
                catalog.id(),
                catalog.label(),
                catalog.type(),
                catalog.talent().resourceKind(),
                catalog.resourceCost(),
                cooldown,
                ready,
                slotIdx,
                enabled,
                disabledReason,
                false,
                icon);
    }

    /**
     * 대치 페이즈 또는 장비창에서 무기 세트(I / II)를 즉시 스왑한다.
     *
     * <p>주무기(MAIN_HAND)뿐만 아니라 보조손 장비(방패 등 OFF_HAND)까지 각 무기 세트별로 저장 및 복원하여 완벽하게 페어로 전환한다.
     *
     * @param progressParam 캐릭터 진행 상태
     * @return 무기 스왑 성공 시 true, 스왑할 무기 세트가 없으면 false
     */
    @Transactional
    public boolean swapWeapon(final CharacterProgress progressParam) {
        final CharacterProgress progress = resolveProgressForSwap(progressParam);
        if (progress == null) {
            return false;
        }

        final Long charId = progress.getId();
        final int currentSet = progress.getActiveWeaponSet();
        final int targetSet = currentSet == 1 ? 2 : 1;

        final List<OwnedItem> currentlyEquipped = findEquippedInventoryItems(charId);
        final Map<EquipSlot, OwnedItem> currentSlotMap = resolveEquippedSlotMap(currentlyEquipped);
        syncCurrentWeaponSetSlots(progress, currentSet, currentSlotMap);

        final Long targetMainId =
                resolveExistingOwnedId(
                        targetSet == 1 ? progress.getWeapon1MainId() : progress.getWeapon2MainId());
        final Long targetOffId =
                resolveExistingOwnedId(
                        targetSet == 1 ? progress.getWeapon1OffId() : progress.getWeapon2OffId());

        final boolean currentHasAny =
                currentSlotMap.containsKey(EquipSlot.MAIN_HAND)
                        || currentSlotMap.containsKey(EquipSlot.OFF_HAND);
        final boolean targetHasAny = targetMainId != null || targetOffId != null;

        if (!currentHasAny && !targetHasAny) {
            return false;
        }

        unequipWeaponSlots(currentSlotMap);
        equipTargetWeaponSet(targetMainId, targetOffId);

        progress.setActiveWeaponSet(targetSet);
        characterProgressRepository.save(progress);
        return true;
    }

    private CharacterProgress resolveProgressForSwap(final CharacterProgress progressParam) {
        if (progressParam != null) {
            return characterProgressRepository
                    .findById(progressParam.getId())
                    .orElse(progressParam);
        }
        return characterProgressRepository.findById(1L).orElse(null);
    }

    private void syncCurrentWeaponSetSlots(
            final CharacterProgress progress,
            final int currentSet,
            final Map<EquipSlot, OwnedItem> currentSlotMap) {
        final Long mainId =
                currentSlotMap.containsKey(EquipSlot.MAIN_HAND)
                        ? currentSlotMap.get(EquipSlot.MAIN_HAND).getId()
                        : null;
        final Long offId =
                currentSlotMap.containsKey(EquipSlot.OFF_HAND)
                        ? currentSlotMap.get(EquipSlot.OFF_HAND).getId()
                        : null;
        if (currentSet == 1) {
            progress.setWeapon1MainId(mainId);
            progress.setWeapon1OffId(offId);
        } else {
            progress.setWeapon2MainId(mainId);
            progress.setWeapon2OffId(offId);
        }
    }

    private Long resolveExistingOwnedId(final Long ownedItemId) {
        if (ownedItemId == null) {
            return null;
        }
        return ownedItemRepository
                .findById(ownedItemId)
                .filter(it -> it.getStorage() == StorageKind.INVENTORY)
                .map(OwnedItem::getId)
                .orElse(null);
    }

    private void unequipWeaponSlots(final Map<EquipSlot, OwnedItem> currentSlotMap) {
        if (currentSlotMap.containsKey(EquipSlot.MAIN_HAND)) {
            currentSlotMap.get(EquipSlot.MAIN_HAND).unequip();
        }
        if (currentSlotMap.containsKey(EquipSlot.OFF_HAND)) {
            currentSlotMap.get(EquipSlot.OFF_HAND).unequip();
        }
    }

    private void equipTargetWeaponSet(final Long targetMainId, final Long targetOffId) {
        boolean mainIsTwoHanded = false;
        if (targetMainId != null) {
            final Optional<OwnedItem> mainOpt = ownedItemRepository.findById(targetMainId);
            if (mainOpt.isPresent() && mainOpt.get().getStorage() == StorageKind.INVENTORY) {
                mainOpt.get().equip();
                final Optional<Item> catOpt = itemCatalogService.byId(mainOpt.get().getItemId());
                if (catOpt.isPresent() && catOpt.get() instanceof EquipmentItem equipItem) {
                    mainIsTwoHanded = equipItem.kind().requiredSlots().contains(EquipSlot.OFF_HAND);
                }
            }
        }

        if (targetOffId != null && !mainIsTwoHanded) {
            final Optional<OwnedItem> offOpt = ownedItemRepository.findById(targetOffId);
            if (offOpt.isPresent() && offOpt.get().getStorage() == StorageKind.INVENTORY) {
                offOpt.get().equip();
            }
        }
    }

    /**
     * 장착 장비의 내구도를 감소시키고, 0에 도달하면 자동 장착 해제한다.
     *
     * <p>6순위 전투에서 공격 턴당 호출되며, 내구도가 0에 도달한 장비는 자동으로 장착 해제되어 보너스가 소멸된다. 자동 해제 시 활동 로그에 해제 메시지를 남긴다.
     *
     * <p>내구도 수리(대장간)는 7순위 스펙에서 구현되며, 본 스펙에서는 파손 시 자동 장착 해제까지만 처리한다.
     *
     * @param progress 캐릭터 진행상황 (로그 기록용)
     * @param amount 감소시킬 내구도량 (양수, 전투에서는 0.05)
     */
    @Transactional
    public void reduceDurabilityAndAutoUnequip(
            final CharacterProgress progress, final double amount) {
        final Long charId = progress != null ? progress.getId() : null;
        final List<OwnedItem> equippedItems = findEquippedInventoryItems(charId);

        for (final OwnedItem equipped : equippedItems) {
            final Optional<Item> catalogOpt = itemCatalogService.byId(equipped.getItemId());
            if (catalogOpt.isEmpty() || !(catalogOpt.get() instanceof EquipmentItem)) {
                continue;
            }
            equipped.reduceDurability(amount);
            if (equipped.getCurrentDurability() <= 0.0) {
                autoUnequipBroken(equipped, catalogOpt.get().name());
            }
        }
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

    // ─── Private helpers ────────────────────────────────────────────────────

    private OwnedItemView toOwnedItemView(final OwnedItem owned) {
        return toOwnedItemView(owned, java.util.Set.of());
    }

    private OwnedItemView toOwnedItemView(
            final OwnedItem owned, final Set<Long> otherSetAssignedItemIds) {
        final Item catalogItem =
                itemCatalogService
                        .byId(owned.getItemId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "카탈로그에 아이템이 없습니다: " + owned.getItemId()));

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
                detailLines);
    }

    private OwnedItem findOwnedItemOrThrow(final long ownedItemId) {
        return ownedItemRepository
                .findById(ownedItemId)
                .orElseThrow(() -> new IllegalStateException("보유 아이템을 찾을 수 없습니다: " + ownedItemId));
    }

    private EquipmentItem resolveEquipmentItem(final OwnedItem owned) {
        final Item catalogItem =
                itemCatalogService
                        .byId(owned.getItemId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "카탈로그에 아이템이 없습니다: " + owned.getItemId()));
        if (!(catalogItem instanceof EquipmentItem equipItem)) {
            throw new IllegalStateException("장비가 아닌 아이템입니다: " + owned.getItemId());
        }
        return equipItem;
    }

    private Optional<OwnedItem> findSlotOccupier(
            final List<OwnedItem> equippedItems,
            final EquipSlot slot,
            final OwnedItem excludeTarget) {
        for (final OwnedItem equipped : equippedItems) {
            if (equipped.getId().equals(excludeTarget.getId()) || !equipped.isEquipped()) {
                continue;
            }
            final Optional<Item> catalogOpt = itemCatalogService.byId(equipped.getItemId());
            if (catalogOpt.isEmpty()) {
                continue;
            }
            final Item catalogItem = catalogOpt.get();
            if (catalogItem instanceof EquipmentItem equipItem
                    && equipItem.kind().requiredSlots().contains(slot)) {
                return Optional.of(equipped);
            }
        }
        return Optional.empty();
    }

    private void unequipSamePrimarySlot(
            final List<OwnedItem> equippedItems,
            final EquipSlot primarySlot,
            final OwnedItem excludeTarget) {
        for (final OwnedItem equipped : equippedItems) {
            if (equipped.getId().equals(excludeTarget.getId()) || !equipped.isEquipped()) {
                continue;
            }
            final Optional<Item> catalogOpt = itemCatalogService.byId(equipped.getItemId());
            if (catalogOpt.isEmpty()) {
                continue;
            }
            final Item catalogItem = catalogOpt.get();
            if (catalogItem instanceof EquipmentItem equipItem
                    && equipItem.kind().primarySlot() == primarySlot) {
                equipped.unequip();
            }
        }
    }

    private void movePotionToStorage(final OwnedItem source, final StorageKind destination) {
        final Long targetCharId = source.getCharacterId() != null ? source.getCharacterId() : 1L;
        final Optional<OwnedItem> existingStack =
                findStorageItem(targetCharId, destination, source.getItemId());

        if (existingStack.isPresent()) {
            existingStack.get().increaseQuantity(1);
            ownedItemRepository.save(existingStack.get());
        } else {
            checkCapacity(targetCharId, destination);
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

    private Optional<OwnedItem> findStorageItem(
            final Long characterId, final StorageKind storage, final String itemId) {
        if (characterId != null) {
            final Optional<OwnedItem> item =
                    ownedItemRepository.findByCharacterIdAndStorageAndItemId(
                            characterId, storage, itemId);
            if (item != null) {
                return item;
            }
        }
        final Optional<OwnedItem> fallback =
                ownedItemRepository.findByStorageAndItemId(storage, itemId);
        return fallback != null ? fallback : Optional.empty();
    }

    private long countStorage(final Long characterId, final StorageKind storage) {
        if (characterId != null) {
            return ownedItemRepository.countByCharacterIdAndStorage(characterId, storage);
        }
        return ownedItemRepository.countByStorage(storage);
    }

    private void checkCapacity(final StorageKind storage) {
        checkCapacity(1L, storage);
    }

    private void checkCapacity(final Long characterId, final StorageKind storage) {
        final long currentCount = countStorage(characterId, storage);
        if (currentCount >= maxCapacity()) {
            throw new InventoryFullException(
                    storage.name() + " 용량(" + maxCapacity() + ")을 초과했습니다.");
        }
    }

    private boolean hasCapacity(final StorageKind storage) {
        return hasCapacity(1L, storage);
    }

    private boolean hasCapacity(final Long characterId, final StorageKind storage) {
        return countStorage(characterId, storage) < maxCapacity();
    }

    private CharacterProgress findCharacterSafe(final Long characterId) {
        if (characterId != null) {
            return characterProgressRepository
                    .findById(characterId)
                    .or(() -> characterProgressRepository.findFirstByOrderByIdAsc())
                    .orElse(null);
        }
        return characterProgressRepository.findFirstByOrderByIdAsc().orElse(null);
    }

    private CharacterProgress loadCharacter(final Long characterId) {
        if (characterId != null) {
            return characterProgressRepository.findById(characterId).orElseGet(this::loadCharacter);
        }
        return loadCharacter();
    }

    private CharacterProgress loadCharacter() {
        return characterProgressRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("캐릭터 진행상황을 찾을 수 없습니다."));
    }

    private Stats applyStatDelta(final Stats stats, final BonusTarget target, final int amount) {
        return switch (target) {
            case STR -> stats.withStrDelta(amount);
            case DEX -> stats.withDexDelta(amount);
            case INT -> stats.withIntDelta(amount);
            case CRITICAL -> stats.withCriticalDelta(amount);
            case DEF -> stats.withDefenseDelta(amount);
            default -> stats;
        };
    }

    private VitalMax applyVitalDelta(
            final VitalMax vitalMax, final BonusTarget target, final int amount) {
        return switch (target) {
            case HP -> vitalMax.withHpDelta(amount);
            case MP -> vitalMax.withMpDelta(amount);
            case STAMINA -> vitalMax.withStaminaDelta(amount);
            default -> vitalMax;
        };
    }

    private String formatBonus(final EquipBonus bonus) {
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

    // ─── acquire helpers ────────────────────────────────────────────────────

    private void acquireSingleItem(
            final CharacterProgress progress, final DroppedItem droppedItem) {
        final Optional<Item> catalogOpt = itemCatalogService.byId(droppedItem.itemId());
        if (catalogOpt.isEmpty()) {
            return;
        }
        final Item catalogItem = catalogOpt.get();
        final String itemName = catalogItem.name();
        final Long charId = progress != null && progress.getId() != null ? progress.getId() : 1L;

        if (catalogItem.type().isStackable()) {
            acquirePotion(charId, droppedItem);
        } else {
            acquireEquipment(charId, droppedItem, itemName);
        }
    }

    private void acquirePotion(final Long characterId, final DroppedItem droppedItem) {
        final Long targetCharId = characterId != null ? characterId : 1L;
        final Optional<OwnedItem> existingStack =
                findStorageItem(targetCharId, StorageKind.INVENTORY, droppedItem.itemId());

        if (existingStack.isPresent()) {
            existingStack.get().increaseQuantity(droppedItem.quantity());
        } else {
            if (isInventoryFull(targetCharId)) {
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

    private int maxCapacity() {
        return gameProperties.inventory().maxSlots();
    }

    private int defaultPotionQuantity() {
        return gameProperties.inventory().defaultPotionQty();
    }

    private void acquireEquipment(
            final Long characterId, final DroppedItem droppedItem, final String itemName) {
        final Long targetCharId = characterId != null ? characterId : 1L;
        if (isInventoryFull(targetCharId)) {
            final String failMsg = gameMessageService.get("log.item.acquire_fail", itemName);
            actionLog.add(failMsg, LOG_TYPE_ITEM);
            return;
        }
        final int maxDurability = resolveMaxDurability(droppedItem.itemId());
        ownedItemRepository.save(
                new OwnedItem(
                        targetCharId,
                        droppedItem.itemId(),
                        1,
                        StorageKind.INVENTORY,
                        false,
                        maxDurability));
    }

    private boolean isInventoryFull(final Long characterId) {
        return countStorage(characterId, StorageKind.INVENTORY) >= maxCapacity();
    }

    private boolean isInventoryFull() {
        return isInventoryFull(1L);
    }

    private void logItemAcquireFailure(final String itemId) {
        final String itemName = itemCatalogService.byId(itemId).map(Item::name).orElse(itemId);
        final String failMsg = gameMessageService.get("log.item.acquire_fail", itemName);
        actionLog.add(failMsg, LOG_TYPE_ITEM);
    }

    private int resolveMaxDurability(final String itemId) {
        final Optional<Item> catalogOpt = itemCatalogService.byId(itemId);
        if (catalogOpt.isPresent() && catalogOpt.get() instanceof EquipmentItem equipItem) {
            return equipItem.maxDurability();
        }
        return gameProperties.inventory().equipmentMaxDurability();
    }

    // ─── combatSkills helpers ───────────────────────────────────────────────

    /**
     * 현재 장착 중인 무기의 재능 분류를 반환한다.
     *
     * <p>무기를 장착하지 않았거나 알 수 없는 경우 {@code null}을 반환한다.
     *
     * @return 장착 중인 무기의 재능 분류 (MELEE, ARCHERY, MAGIC, 또는 null)
     */
    /**
     * 기본 캐릭터의 현재 장착 중인 무기의 재능 분류를 반환한다.
     *
     * <p>무기를 장착하지 않았거나 알 수 없는 경우 {@code null}을 반환한다.
     *
     * @return 장착 중인 무기의 재능 분류 (MELEE, ARCHERY, MAGIC, 또는 null)
     */
    public SkillTalent equippedWeaponTalent() {
        return resolveEquippedWeaponTalent(null);
    }

    /**
     * 특정 캐릭터의 현재 장착 중인 무기의 재능 분류를 반환한다.
     *
     * @param characterId 캐릭터 식별자
     * @return 장착 중인 무기의 재능 분류 (MELEE, ARCHERY, MAGIC, 또는 null)
     */
    public SkillTalent equippedWeaponTalent(final Long characterId) {
        return resolveEquippedWeaponTalent(characterId);
    }

    /**
     * 기본 캐릭터의 현재 활(BOW) 계열 무기 장착 여부를 반환한다.
     *
     * @return 활을 장착 중이면 {@code true}, 아니면 {@code false}
     */
    public boolean isBowEquipped() {
        return isBowEquipped(null);
    }

    /**
     * 특정 캐릭터의 현재 활(BOW) 계열 무기 장착 여부를 반환한다.
     *
     * @param characterId 캐릭터 식별자
     * @return 활을 장착 중이면 {@code true}, 아니면 {@code false}
     */
    public boolean isBowEquipped(final Long characterId) {
        return resolveEquippedWeaponTalent(characterId) == SkillTalent.ARCHERY;
    }

    private SkillTalent resolveEquippedWeaponTalent(final Long characterId) {
        final List<OwnedItem> equippedItems = findEquippedInventoryItems(characterId);

        for (final OwnedItem equipped : equippedItems) {
            final Optional<Item> catalogOpt = itemCatalogService.byId(equipped.getItemId());
            if (catalogOpt.isEmpty()) {
                continue;
            }
            final Item catalogItem = catalogOpt.get();
            if (catalogItem instanceof EquipmentItem equipItem
                    && catalogItem.type() == ItemType.WEAPON) {
                return mapKindToSkillTalent(equipItem.kind());
            }
        }
        return null;
    }

    private SkillTalent mapKindToSkillTalent(final EquipmentKind kind) {
        return switch (kind) {
            case ONE_HANDED_SWORD, TWO_HANDED_SWORD -> SkillTalent.MELEE;
            case BOW -> SkillTalent.ARCHERY;
            case WAND, STAFF -> SkillTalent.MAGIC;
            default -> SkillTalent.MELEE;
        };
    }

    private String resolveSkillIcon(final Skill catalog) {
        if (catalog == null) {
            return "⚔️";
        }
        if (catalog.talent() == SkillTalent.COMMON || catalog instanceof PassiveSkill) {
            return "👑";
        }
        return switch (catalog.talent()) {
            case MELEE -> "⚔️";
            case ARCHERY -> "🏹";
            case MAGIC -> "🔮";
            case COMMON -> "👑";
        };
    }

    // ─── durability helpers ─────────────────────────────────────────────────

    private void autoUnequipBroken(final OwnedItem equipped, final String itemName) {
        equipped.unequip();
        final String brokenMsg = gameMessageService.get("log.item.durability_broken", itemName);
        actionLog.add(brokenMsg, LOG_TYPE_ITEM);
    }

    private static int skillTypePriority(final SkillType type) {
        if (type == null) {
            return 99;
        }
        return switch (type) {
            case NORMAL -> 1;
            case HEAVY -> 2;
            case DEFENSE -> 3;
            case RECOVERY -> 4;
            case BUFF -> 5;
            case DEBUFF -> 6;
            case CC -> 7;
            case DOT -> 8;
            case ULTIMATE -> 9;
            case PASSIVE -> 10;
        };
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

        final Item item = itemCatalogService.byId(owned.getItemId()).orElse(null);
        if (!(item instanceof EquipmentItem equipItem)) {
            return EquipmentSlotView.empty(slot.name(), label, defaultIcon);
        }

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

        final String icon = resolveEquipIcon(equipItem);
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

    private static String resolveEquipIcon(final EquipmentItem equipItem) {
        return switch (equipItem.kind()) {
            case ONE_HANDED_SWORD, TWO_HANDED_SWORD -> "🗡️";
            case BOW -> "🏹";
            case WAND, STAFF -> "🔮";
            case SHIELD -> "🛡️";
            case ARMOR_BODY -> "🥋";
            case HELMET -> "🪖";
            case GLOVES -> "🧤";
            case BOOTS -> "👢";
        };
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
}
