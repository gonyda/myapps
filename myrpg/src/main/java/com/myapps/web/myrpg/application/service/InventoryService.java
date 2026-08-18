package com.myapps.web.myrpg.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myapps.web.myrpg.application.dto.BankView;
import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DroppedItem;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.application.dto.InventoryView;
import com.myapps.web.myrpg.application.dto.OwnedItemView;
import com.myapps.web.myrpg.application.exception.EquipConflictException;
import com.myapps.web.myrpg.application.exception.InventoryFullException;
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
import com.myapps.web.myrpg.domain.model.PotionItem;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;

/**
 * 인벤토리 핵심 비즈니스 로직을 담당하는 서비스.
 *
 * <p>장비 착용/해제, 포션 사용, 은행 ↔ 인벤토리 아이템 이동,
 * 장착 장비 보너스 합산, 신규 캐릭터 기본 지급, 아이템 상세 생성을 처리한다.
 * 슬롯 점유 기반 충돌 검사로 착용 규칙을 구현하며,
 * 소비형(POTION)은 스택 누적, 장비(WEAPON/ARMOR)는 개별 인스턴스로 관리한다.
 */
@Service
public class InventoryService {

    private static final int MAX_CAPACITY = 30;
    private static final int DEFAULT_POTION_QUANTITY = 5;
    private static final int EQUIPMENT_MAX_DURABILITY = 20;

    private static final String SEED_POTION_ID = "hp_potion_30";
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

    /**
     * InventoryService를 생성한다.
     *
     * @param ownedItemRepository         보유 아이템 리포지토리
     * @param itemCatalogService          아이템 카탈로그 서비스
     * @param characterProgressRepository 캐릭터 진행상황 리포지토리
     * @param statProgression             스탯/바이탈 계산 정책
     * @param actionLog                   활동 로그 (세션 스코프)
     * @param skillCatalogService         스킬 카탈로그 서비스
     * @param characterSkillRepository    캐릭터 보유 스킬 리포지토리
     */
    public InventoryService(final OwnedItemRepository ownedItemRepository,
                            final ItemCatalogService itemCatalogService,
                            final CharacterProgressRepository characterProgressRepository,
                            final StatProgression statProgression,
                            final ActionLog actionLog,
                            final SkillCatalogService skillCatalogService,
                            final CharacterSkillRepository characterSkillRepository) {
        this.ownedItemRepository = ownedItemRepository;
        this.itemCatalogService = itemCatalogService;
        this.characterProgressRepository = characterProgressRepository;
        this.statProgression = statProgression;
        this.actionLog = actionLog;
        this.skillCatalogService = skillCatalogService;
        this.characterSkillRepository = characterSkillRepository;
    }

    /**
     * 장비를 착용한다.
     *
     * <p>착용 규칙: 대상 장비의 {@code requiredSlots} 각 슬롯을 점유한 장착 장비를 조사한다.
     * 점유 장비의 {@code primarySlot}이 대상과 다르면 충돌({@link EquipConflictException}),
     * 같으면 해제 후 착용(스왑)한다.
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

        final List<OwnedItem> equippedItems =
                ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY);

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
    }

    /**
     * 포션을 사용하여 HP를 회복한다.
     *
     * <p>HP를 {@code min(hpCurrent + healHp, hpMax)}로 회복하고
     * 수량을 1 감소시킨다. 수량이 0이 되면 해당 행을 삭제한다.
     *
     * @param ownedItemId 사용할 포션 보유 아이템 PK
     */
    @Transactional
    public void usePotion(final long ownedItemId) {
        final OwnedItem target = findOwnedItemOrThrow(ownedItemId);
        final Item catalogItem = itemCatalogService.byId(target.getItemId())
                .orElseThrow(() -> new IllegalStateException(
                        "카탈로그에 아이템이 없습니다: " + target.getItemId()));

        if (!(catalogItem instanceof PotionItem potionItem)) {
            throw new IllegalStateException("포션이 아닌 아이템은 사용할 수 없습니다: " + target.getItemId());
        }

        final CharacterProgress character = loadCharacter();
        final VitalMax vitalMax = statProgression.vitalMaxFor(
                character.getCurrentLevel(), character.getTalent());
        final VitalMax equipVitalBonus = equippedBonus().vitalBonus();
        final int hpMax = vitalMax.hp() + equipVitalBonus.hp();

        final int healed = Math.min(character.getHpCurrent() + potionItem.healHp(), hpMax);
        character.fullRecover(new VitalMax(healed, character.getMpCurrent(), character.getStaminaCurrent()));

        target.decreaseQuantity(1);
        if (target.getQuantity() == 0) {
            ownedItemRepository.delete(target);
        }
    }

    /**
     * 인벤토리에서 은행으로 아이템을 맡긴다.
     *
     * <p>장착 중인 장비는 맡기기를 거부한다. 소비형(POTION)은 은행의 동일 itemId 행에
     * 스택 누적하며, 장비는 저장위치를 전환한다. 은행 용량(30)을 초과하면 거부한다.
     *
     * @param ownedItemId 맡길 보유 아이템 PK
     * @throws EquipConflictException  장착 중 장비 맡기기 시도 시
     * @throws InventoryFullException  은행 용량 초과 시
     */
    @Transactional
    public void moveToBank(final long ownedItemId) {
        final OwnedItem target = findOwnedItemOrThrow(ownedItemId);

        if (target.isEquipped()) {
            throw new EquipConflictException("장착을 해제한 후 맡길 수 있습니다.");
        }

        final Item catalogItem = itemCatalogService.byId(target.getItemId())
                .orElseThrow(() -> new IllegalStateException(
                        "카탈로그에 아이템이 없습니다: " + target.getItemId()));

        if (catalogItem.type() == ItemType.POTION) {
            movePotionToStorage(target, StorageKind.BANK);
        } else {
            checkCapacity(StorageKind.BANK);
            target.moveTo(StorageKind.BANK);
        }
    }

    /**
     * 은행에서 인벤토리로 아이템을 찾는다.
     *
     * <p>소비형(POTION)은 인벤토리의 동일 itemId 행에 스택 누적하며,
     * 장비는 저장위치를 전환한다. 인벤토리 용량(30)을 초과하면 거부한다.
     *
     * @param ownedItemId 찾을 보유 아이템 PK
     * @throws InventoryFullException 인벤토리 용량 초과 시
     */
    @Transactional
    public void moveToInventory(final long ownedItemId) {
        final OwnedItem target = findOwnedItemOrThrow(ownedItemId);

        final Item catalogItem = itemCatalogService.byId(target.getItemId())
                .orElseThrow(() -> new IllegalStateException(
                        "카탈로그에 아이템이 없습니다: " + target.getItemId()));

        if (catalogItem.type() == ItemType.POTION) {
            movePotionToStorage(target, StorageKind.INVENTORY);
        } else {
            checkCapacity(StorageKind.INVENTORY);
            target.moveTo(StorageKind.INVENTORY);
        }
    }

    /**
     * 장착 중인 장비의 보너스를 합산하여 STAT/VITAL로 분기한 결과를 반환한다.
     *
     * <p>{@code storage=INVENTORY && equipped=true}인 장비의 {@link EquipBonus}를
     * {@link BonusTarget#kind()}로 분기하여 STAT 계열은 {@link Stats}로,
     * VITAL 계열은 {@link VitalMax}로 합산한다.
     *
     * @return 장비 STAT 보너스와 VITAL 보너스를 담은 결과
     */
    public EquippedBonusResult equippedBonus() {
        final List<OwnedItem> equippedItems =
                ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY);

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
     * 인벤토리 팝업 뷰를 조립한다.
     *
     * <p>인벤토리({@code storage=INVENTORY}) 아이템을 획득순(id 오름차순)으로 조회하고
     * 각 행을 {@link OwnedItemView}로 변환하여 보유 골드와 함께 반환한다.
     * 상세 설명({@code detailLines})은 렌더 시점에 임베드되어 별도 서버 요청 없이 표시된다.
     *
     * @param gold 캐릭터 보유 골드
     * @return 인벤토리 뷰 (보유 골드 + 아이템 목록)
     */
    public InventoryView buildInventoryView(final long gold) {
        final List<OwnedItem> inventoryItems =
                ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY);
        final List<OwnedItemView> views = inventoryItems.stream()
                .map(this::toOwnedItemView)
                .toList();
        return new InventoryView(gold, views);
    }

    /**
     * 은행 팝업 뷰를 조립한다.
     *
     * <p>은행({@code storage=BANK})과 소지품({@code storage=INVENTORY}) 아이템을
     * 각각 획득순(id 오름차순)으로 조회하고 행 뷰로 변환하여
     * 은행/보유 골드와 함께 반환한다.
     *
     * @param gold     캐릭터 보유 골드
     * @param bankGold 은행 보관 골드
     * @return 은행 뷰 (은행·보유 골드 + 은행/소지품 목록)
     */
    public BankView buildBankView(final long gold, final long bankGold) {
        final List<OwnedItem> bankItems =
                ownedItemRepository.findByStorageOrderById(StorageKind.BANK);
        final List<OwnedItem> inventoryItems =
                ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY);

        final List<OwnedItemView> bankViews = bankItems.stream()
                .map(this::toOwnedItemView)
                .toList();
        final List<OwnedItemView> inventoryViews = inventoryItems.stream()
                .map(this::toOwnedItemView)
                .toList();

        return new BankView(bankGold, gold, bankViews, inventoryViews);
    }

    /**
     * 신규 캐릭터에 모든 초보자용 장비와 기본 소비품을 지급하고 기본 장비를 장착한다.
     *
     * <p>초보자 무기 5종(한손검·양손검·활·완드·스태프)과 방어구 5종
     * (방패·갑옷·투구·장갑·부츠) 각 1개(내구도 20), 생명력 30 포션 5개를 인벤토리에 생성한다.
     * 이 중 한손검·방패·갑옷·투구·장갑·부츠를 기본 장착하고 나머지 무기는 미장착 상태로 지급한다.
     */
    @Transactional
    public void seedDefault() {
        ownedItemRepository.save(new OwnedItem(
                SEED_POTION_ID, DEFAULT_POTION_QUANTITY, StorageKind.INVENTORY, false, 0));

        // 무기 5종: 한손검만 기본 장착, 나머지는 인벤토리 보유
        seedEquipment(SEED_ONE_HAND_SWORD_ID, true);
        seedEquipment(SEED_TWO_HAND_SWORD_ID, false);
        seedEquipment(SEED_BOW_ID, false);
        seedEquipment(SEED_WAND_ID, false);
        seedEquipment(SEED_STAFF_ID, false);

        // 방어구 5종: 방패·갑옷·투구·장갑·부츠 모두 기본 장착
        seedEquipment(SEED_SHIELD_ID, true);
        seedEquipment(SEED_ARMOR_ID, true);
        seedEquipment(SEED_HELMET_ID, true);
        seedEquipment(SEED_GLOVES_ID, true);
        seedEquipment(SEED_BOOTS_ID, true);
    }

    /**
     * 초보자용 장비 1개를 최대 내구도로 인벤토리에 지급한다.
     *
     * @param itemId   지급할 장비 아이템 id
     * @param equipped 기본 장착 여부
     */
    private void seedEquipment(final String itemId, final boolean equipped) {
        ownedItemRepository.save(new OwnedItem(
                itemId, 1, StorageKind.INVENTORY, equipped, EQUIPMENT_MAX_DURABILITY));
    }

    /**
     * 몬스터 처치 드랍 결과를 인벤토리에 적재한다.
     *
     * <p>골드는 항상 {@code gainGold}로 가산하고, 아이템은 각각 인벤토리에 추가한다.
     * 인벤토리 용량(30)을 초과하는 아이템은 소실시키고 활동 로그에 실패 메시지를 남기며,
     * 나머지 아이템과 골드 처리는 계속한다.
     *
     * @param progress 캐릭터 진행상황 (골드 가산 대상)
     * @param drop     드랍 결과 (골드 + 아이템 목록)
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
     * 현재 착용 무기의 재능에 해당하는 전투 스킬 목록을 조회한다.
     *
     * <p>착용 무기 재능에 해당하는 스킬과 공통(COMMON) 스킬 중
     * 캐릭터가 습득(보유)한 스킬만 {@link BattleSkillButton}으로 반환한다.
     * 무기를 장착하지 않은 경우 공통 스킬(방어)만 반환한다.
     *
     * @param progress 캐릭터 진행상황 (보유 스킬·착용 장비 조회용)
     * @return 전투에서 사용 가능한 스킬 버튼 목록
     */
    public List<BattleSkillButton> combatSkills(final CharacterProgress progress) {
        final SkillTalent weaponTalent = resolveEquippedWeaponTalent();
        final List<CharacterSkill> ownedSkills =
                characterSkillRepository.findByCharacterId(progress.getId());

        final List<BattleSkillButton> buttons = new ArrayList<>();
        for (final CharacterSkill characterSkill : ownedSkills) {
            buildCombatButton(characterSkill, weaponTalent).ifPresent(buttons::add);
        }
        return List.copyOf(buttons);
    }

    /**
     * 장착 장비의 내구도를 감소시키고, 0에 도달하면 자동 장착 해제한다.
     *
     * <p>6순위 전투에서 공격 턴당 호출되며, 내구도가 0에 도달한 장비는
     * 자동으로 장착 해제되어 보너스가 소멸된다. 자동 해제 시 활동 로그에
     * 해제 메시지를 남긴다.
     *
     * <p>내구도 수리(대장간)는 7순위 스펙에서 구현되며, 본 스펙에서는
     * 파손 시 자동 장착 해제까지만 처리한다.
     *
     * @param progress 캐릭터 진행상황 (로그 기록용)
     * @param amount   감소시킬 내구도량 (양수, 전투에서는 0.05)
     */
    @Transactional
    public void reduceDurabilityAndAutoUnequip(final CharacterProgress progress, final double amount) {
        final List<OwnedItem> equippedItems =
                ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY);

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
     * <p>포션이면 회복량 문구를 포함하고, 장비이면 보너스 한 줄씩·장비 종류·
     * 내구도·양손 무기 배타 안내를 포함한다.
     *
     * @param item  카탈로그 아이템
     * @param owned 보유 아이템 인스턴스
     * @return 상세 설명 줄 목록
     */
    public List<String> describe(final Item item, final OwnedItem owned) {
        final List<String> lines = new ArrayList<>();

        if (item instanceof PotionItem potionItem) {
            lines.add("생명력을 " + potionItem.healHp() + " 회복한다.");
        } else if (item instanceof EquipmentItem equipItem) {
            lines.add(equipItem.kind().label() + " (" + equipItem.type().label() + ")");

            for (final EquipBonus bonus : equipItem.bonuses()) {
                lines.add(formatBonus(bonus));
            }

            if (equipItem.kind().primarySlot() == EquipSlot.MAIN_HAND
                    && equipItem.kind().requiredSlots().contains(EquipSlot.OFF_HAND)) {
                lines.add("방패와 함께 착용할 수 없습니다.");
            }

            lines.add("내구도: " + formatDurability(owned.getCurrentDurability())
                    + "/" + equipItem.maxDurability());
        }

        return List.copyOf(lines);
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private OwnedItemView toOwnedItemView(final OwnedItem owned) {
        final Item catalogItem = itemCatalogService.byId(owned.getItemId())
                .orElseThrow(() -> new IllegalStateException(
                        "카탈로그에 아이템이 없습니다: " + owned.getItemId()));

        final boolean usable = catalogItem.type() == ItemType.POTION;
        final boolean equippable = catalogItem.type().isEquipment();
        final Double currentDurability = equippable ? owned.getCurrentDurability() : null;
        final Integer maxDurability = (catalogItem instanceof EquipmentItem equipItem)
                ? equipItem.maxDurability() : null;
        final List<String> detailLines = describe(catalogItem, owned);

        return new OwnedItemView(
                owned.getId(),
                catalogItem.name(),
                catalogItem.type().label(),
                catalogItem.type(),
                owned.getQuantity(),
                owned.isEquipped(),
                usable,
                equippable,
                currentDurability,
                maxDurability,
                detailLines);
    }

    private OwnedItem findOwnedItemOrThrow(final long ownedItemId) {
        return ownedItemRepository.findById(ownedItemId)
                .orElseThrow(() -> new IllegalStateException(
                        "보유 아이템을 찾을 수 없습니다: " + ownedItemId));
    }

    private EquipmentItem resolveEquipmentItem(final OwnedItem owned) {
        final Item catalogItem = itemCatalogService.byId(owned.getItemId())
                .orElseThrow(() -> new IllegalStateException(
                        "카탈로그에 아이템이 없습니다: " + owned.getItemId()));
        if (!(catalogItem instanceof EquipmentItem equipItem)) {
            throw new IllegalStateException(
                    "장비가 아닌 아이템입니다: " + owned.getItemId());
        }
        return equipItem;
    }

    private Optional<OwnedItem> findSlotOccupier(final List<OwnedItem> equippedItems,
                                                  final EquipSlot slot,
                                                  final OwnedItem excludeTarget) {
        for (final OwnedItem equipped : equippedItems) {
            if (equipped.getId().equals(excludeTarget.getId())) {
                continue;
            }
            final Optional<Item> catalogOpt = itemCatalogService.byId(equipped.getItemId());
            if (catalogOpt.isEmpty()) {
                continue;
            }
            final Item catalogItem = catalogOpt.get();
            if (catalogItem instanceof EquipmentItem equipItem) {
                if (equipItem.kind().requiredSlots().contains(slot)) {
                    return Optional.of(equipped);
                }
            }
        }
        return Optional.empty();
    }

    private void unequipSamePrimarySlot(final List<OwnedItem> equippedItems,
                                         final EquipSlot primarySlot,
                                         final OwnedItem excludeTarget) {
        for (final OwnedItem equipped : equippedItems) {
            if (equipped.getId().equals(excludeTarget.getId())) {
                continue;
            }
            final Optional<Item> catalogOpt = itemCatalogService.byId(equipped.getItemId());
            if (catalogOpt.isEmpty()) {
                continue;
            }
            final Item catalogItem = catalogOpt.get();
            if (catalogItem instanceof EquipmentItem equipItem) {
                if (equipItem.kind().primarySlot() == primarySlot) {
                    equipped.unequip();
                }
            }
        }
    }

    private void movePotionToStorage(final OwnedItem source, final StorageKind destination) {
        final Optional<OwnedItem> existingStack =
                ownedItemRepository.findByStorageAndItemId(destination, source.getItemId());

        if (existingStack.isPresent()) {
            existingStack.get().increaseQuantity(1);
            ownedItemRepository.save(existingStack.get());
        } else {
            checkCapacity(destination);
            final OwnedItem newItem = new OwnedItem(source.getItemId(), 1, destination, false, 0);
            ownedItemRepository.save(newItem);
        }

        source.decreaseQuantity(1);
        if (source.getQuantity() <= 0) {
            ownedItemRepository.delete(source);
        } else {
            ownedItemRepository.save(source);
        }
    }

    private void checkCapacity(final StorageKind storage) {
        final long currentCount = ownedItemRepository.countByStorage(storage);
        if (currentCount >= MAX_CAPACITY) {
            final String storageName = storage == StorageKind.INVENTORY ? "인벤토리" : "은행";
            throw new InventoryFullException(storageName + "가 가득 찼습니다.");
        }
    }

    private CharacterProgress loadCharacter() {
        return characterProgressRepository.findFirstByOrderByIdAsc()
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

    private VitalMax applyVitalDelta(final VitalMax vitalMax, final BonusTarget target, final int amount) {
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
        };
    }

    private String formatDurability(final double currentDurability) {
        if (currentDurability == Math.floor(currentDurability)) {
            return String.valueOf((int) currentDurability);
        }
        return String.valueOf(currentDurability);
    }

    // ─── acquire helpers ────────────────────────────────────────────────────

    private void acquireSingleItem(final CharacterProgress progress, final DroppedItem droppedItem) {
        final Optional<Item> catalogOpt = itemCatalogService.byId(droppedItem.itemId());
        if (catalogOpt.isEmpty()) {
            return;
        }
        final Item catalogItem = catalogOpt.get();
        final String itemName = catalogItem.name();

        if (catalogItem.type() == ItemType.POTION) {
            acquirePotion(droppedItem);
        } else {
            acquireEquipment(droppedItem, itemName);
        }
    }

    private void acquirePotion(final DroppedItem droppedItem) {
        final Optional<OwnedItem> existingStack =
                ownedItemRepository.findByStorageAndItemId(StorageKind.INVENTORY, droppedItem.itemId());

        if (existingStack.isPresent()) {
            existingStack.get().increaseQuantity(droppedItem.quantity());
        } else {
            if (isInventoryFull()) {
                logItemAcquireFailure(droppedItem.itemId());
                return;
            }
            ownedItemRepository.save(new OwnedItem(
                    droppedItem.itemId(), droppedItem.quantity(), StorageKind.INVENTORY, false, 0));
        }
    }

    private void acquireEquipment(final DroppedItem droppedItem, final String itemName) {
        if (isInventoryFull()) {
            actionLog.add(itemName + " 획득 실패!", LOG_TYPE_ITEM);
            return;
        }
        final int maxDurability = resolveMaxDurability(droppedItem.itemId());
        ownedItemRepository.save(new OwnedItem(
                droppedItem.itemId(), 1, StorageKind.INVENTORY, false, maxDurability));
    }

    private boolean isInventoryFull() {
        return ownedItemRepository.countByStorage(StorageKind.INVENTORY) >= MAX_CAPACITY;
    }

    private void logItemAcquireFailure(final String itemId) {
        final String itemName = itemCatalogService.byId(itemId)
                .map(Item::name)
                .orElse(itemId);
        actionLog.add(itemName + " 획득 실패!", LOG_TYPE_ITEM);
    }

    private int resolveMaxDurability(final String itemId) {
        final Optional<Item> catalogOpt = itemCatalogService.byId(itemId);
        if (catalogOpt.isPresent() && catalogOpt.get() instanceof EquipmentItem equipItem) {
            return equipItem.maxDurability();
        }
        return EQUIPMENT_MAX_DURABILITY;
    }

    // ─── combatSkills helpers ───────────────────────────────────────────────

    private SkillTalent resolveEquippedWeaponTalent() {
        final List<OwnedItem> equippedItems =
                ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY);

        for (final OwnedItem equipped : equippedItems) {
            final Optional<Item> catalogOpt = itemCatalogService.byId(equipped.getItemId());
            if (catalogOpt.isEmpty()) {
                continue;
            }
            final Item catalogItem = catalogOpt.get();
            if (catalogItem instanceof EquipmentItem equipItem && catalogItem.type() == ItemType.WEAPON) {
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

    private Optional<BattleSkillButton> buildCombatButton(final CharacterSkill characterSkill,
                                                           final SkillTalent weaponTalent) {
        final Optional<Skill> catalogOpt = skillCatalogService.byId(characterSkill.getSkillId());
        if (catalogOpt.isEmpty()) {
            return Optional.empty();
        }
        final Skill catalog = catalogOpt.get();
        final SkillTalent skillTalent = catalog.talent();

        if (skillTalent == SkillTalent.COMMON) {
            return Optional.of(toBattleSkillButton(catalog));
        }
        if (weaponTalent != null && skillTalent == weaponTalent) {
            return Optional.of(toBattleSkillButton(catalog));
        }
        return Optional.empty();
    }

    private BattleSkillButton toBattleSkillButton(final Skill catalog) {
        return new BattleSkillButton(
                catalog.id(),
                catalog.label(),
                catalog.type(),
                catalog.talent().resourceKind(),
                catalog.resourceCost());
    }

    // ─── durability helpers ─────────────────────────────────────────────────

    private void autoUnequipBroken(final OwnedItem equipped, final String itemName) {
        equipped.unequip();
        actionLog.add(itemName + " 내구도 0 — 장착 해제됨", LOG_TYPE_ITEM);
    }
}
