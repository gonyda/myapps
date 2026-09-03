package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.application.dto.BankView;
import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.EquipmentView;
import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.application.dto.InventoryView;
import com.myapps.web.myrpg.application.dto.OwnedItemView;
import com.myapps.web.myrpg.application.exception.EquipConflictException;
import com.myapps.web.myrpg.application.exception.InventoryFullException;
import com.myapps.web.myrpg.application.service.inventory.CombatSkillLoadoutService;
import com.myapps.web.myrpg.application.service.inventory.EquipmentBonusService;
import com.myapps.web.myrpg.application.service.inventory.EquipmentService;
import com.myapps.web.myrpg.application.service.inventory.InventoryQuerySupport;
import com.myapps.web.myrpg.application.service.inventory.InventoryViewAssembler;
import com.myapps.web.myrpg.application.service.inventory.ItemAcquisitionService;
import com.myapps.web.myrpg.application.service.inventory.ItemStorageService;
import com.myapps.web.myrpg.application.service.inventory.PotionConsumptionService;
import com.myapps.web.myrpg.config.GameProperties;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.PotionItem;
import com.myapps.web.myrpg.domain.model.SkillRankupBonus;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인벤토리 핵심 비즈니스 로직을 담당하는 파사드(Facade) 서비스.
 *
 * <p>장비 착용/해제, 포션 사용, 은행 ↔ 인벤토리 아이템 이동, 장착 장비 보너스 합산, 신규 캐릭터 기본 지급, 아이템 상세 생성을 처리한다. 실제 비즈니스 로직은
 * 관심사별 협력 객체({@link EquipmentService}, {@link ItemStorageService}, {@link ItemAcquisitionService},
 * {@link PotionConsumptionService}, {@link EquipmentBonusService}, {@link InventoryViewAssembler},
 * {@link CombatSkillLoadoutService})로 위임한다.
 */
@Service
public class InventoryService {

    private final EquipmentService equipmentService;
    private final ItemStorageService itemStorageService;
    private final ItemAcquisitionService itemAcquisitionService;
    private final PotionConsumptionService potionConsumptionService;
    private final EquipmentBonusService equipmentBonusService;
    private final InventoryViewAssembler inventoryViewAssembler;
    private final CombatSkillLoadoutService combatSkillLoadoutService;

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
        final SkillRankupBonus skillRankupBonus = new SkillRankupBonus();
        final GameMessageService resolvedGameMessageService =
                gameMessageService != null ? gameMessageService : new GameMessageService(null);
        final GameProperties resolvedGameProperties =
                gameProperties != null
                        ? gameProperties
                        : new GameProperties(null, null, null, null, null, null);
        final InventoryQuerySupport inventoryQuerySupport =
                new InventoryQuerySupport(
                        ownedItemRepository,
                        itemCatalogService,
                        characterProgressRepository,
                        resolvedGameProperties);
        this.equipmentBonusService =
                new EquipmentBonusService(
                        inventoryQuerySupport, actionLog, resolvedGameMessageService);
        this.inventoryViewAssembler =
                new InventoryViewAssembler(
                        inventoryQuerySupport,
                        itemCatalogService,
                        ownedItemRepository,
                        resolvedGameMessageService,
                        this.equipmentBonusService);
        this.equipmentService =
                new EquipmentService(
                        inventoryQuerySupport,
                        ownedItemRepository,
                        characterProgressRepository,
                        this.inventoryViewAssembler::toOwnedItemView);
        this.itemStorageService =
                new ItemStorageService(
                        inventoryQuerySupport, ownedItemRepository, itemCatalogService);
        this.itemAcquisitionService =
                new ItemAcquisitionService(
                        inventoryQuerySupport,
                        ownedItemRepository,
                        itemCatalogService,
                        actionLog,
                        resolvedGameMessageService,
                        resolvedGameProperties);
        this.potionConsumptionService =
                new PotionConsumptionService(
                        inventoryQuerySupport,
                        ownedItemRepository,
                        itemCatalogService,
                        statProgression,
                        this.equipmentBonusService,
                        characterSkillRepository,
                        skillCatalogService,
                        skillRankupBonus);
        this.combatSkillLoadoutService =
                new CombatSkillLoadoutService(
                        inventoryQuerySupport, characterSkillRepository, skillCatalogService);
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
        equipmentService.equip(ownedItemId);
    }

    /**
     * 장비를 장착 해제한다.
     *
     * @param ownedItemId 해제할 보유 아이템 PK
     */
    @Transactional
    public void unequip(final long ownedItemId) {
        equipmentService.unequip(ownedItemId);
    }

    /**
     * 포션을 사용하여 HP를 회복한다.
     *
     * <p>HP, MP, Stamina를 각각 {@code min(current + heal, max)}로 회복하고 수량을 1 감소시킨다. 수량이 0이 되면 해당 행을
     * 삭제한다.
     *
     * @param ownedItemId 사용할 포션 보유 아이템 PK
     * @return 사용된 포션 아이템
     */
    @Transactional
    public PotionItem usePotion(final long ownedItemId) {
        return potionConsumptionService.usePotion(ownedItemId);
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
        itemStorageService.moveToBank(ownedItemId);
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
        itemStorageService.moveToInventory(ownedItemId);
    }

    /**
     * 기본 캐릭터의 장착 장비 보너스 합산을 계산한다.
     *
     * @return 장비 STAT 보너스와 VITAL 보너스를 담은 결과
     */
    public EquippedBonusResult equippedBonus() {
        return equipmentBonusService.equippedBonus();
    }

    /**
     * 특정 캐릭터의 장착 장비 보너스 합산을 계산한다.
     *
     * <p>인벤토리({@code storage=INVENTORY})에서 장착 중인({@code equipped=true}) 장비의 {@link
     * com.myapps.web.myrpg.domain.model.EquipBonus} 목록을 순회하여 STAT 계열은 Stats로, VITAL 계열은 VitalMax로
     * 합산한다.
     *
     * @param characterId 캐릭터 식별자 (null일 경우 전체/기본)
     * @return 장비 STAT 보너스와 VITAL 보너스를 담은 결과
     */
    public EquippedBonusResult equippedBonus(final Long characterId) {
        return equipmentBonusService.equippedBonus(characterId);
    }

    /**
     * 기본 캐릭터 인벤토리 팝업 뷰를 조립한다.
     *
     * @param gold 캐릭터 보유 골드
     * @return 인벤토리 뷰 (보유 골드 + 아이템 목록)
     */
    public InventoryView buildInventoryView(final long gold) {
        return inventoryViewAssembler.buildInventoryView(gold);
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
        return inventoryViewAssembler.buildInventoryView(characterId, gold);
    }

    /**
     * 기본 캐릭터 은행 팝업 뷰를 조립한다.
     *
     * @param gold 캐릭터 보유 골드
     * @param bankGold 은행 보관 골드
     * @return 은행 뷰 (은행·보유 골드 + 은행/소지품 목록)
     */
    public BankView buildBankView(final long gold, final long bankGold) {
        return inventoryViewAssembler.buildBankView(gold, bankGold);
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
        return inventoryViewAssembler.buildBankView(characterId, gold, bankGold);
    }

    /**
     * 기본 캐릭터의 장비 팝업 뷰를 조립한다.
     *
     * @return 장비 팝업 뷰
     */
    public EquipmentView buildEquipmentView() {
        return inventoryViewAssembler.buildEquipmentView();
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
        return inventoryViewAssembler.buildEquipmentView(characterId);
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
        equipmentService.smartEquip(ownedItemId);
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
        return equipmentService.findEquippableForSlot(characterId, slotCode);
    }

    /** 기본 캐릭터(1L)에 모든 초보자용 장비와 기본 소비품을 지급하고 기본 장비를 장착한다. */
    @Transactional
    public void seedDefault() {
        itemAcquisitionService.seedDefault();
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
        itemAcquisitionService.seedDefault(characterId);
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
        itemAcquisitionService.acquire(progress, drop);
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
        itemAcquisitionService.acquireItem(itemId, quantity);
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
        itemAcquisitionService.acquireItem(characterId, itemId, quantity);
    }

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
        return combatSkillLoadoutService.combatSkills(progress);
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
        return equipmentService.swapWeapon(progressParam);
    }

    /**
     * 장착 장비의 내구도를 감소시키고, 0에 도달하면 자동 장착 해제한다.
     *
     * <p>전투에서 공격 턴당 호출되며, 내구도가 0에 도달한 장비는 자동으로 장착 해제되어 보너스가 소멸된다. 자동 해제 시 활동 로그에 해제 메시지를 남긴다.
     *
     * @param progress 캐릭터 진행상황 (로그 기록용)
     * @param amount 감소시킬 내구도량 (양수, 전투에서는 0.05)
     */
    @Transactional
    public void reduceDurabilityAndAutoUnequip(
            final CharacterProgress progress, final double amount) {
        equipmentBonusService.reduceDurabilityAndAutoUnequip(progress, amount);
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
        return inventoryViewAssembler.describe(item, owned);
    }

    /**
     * 기본 캐릭터의 현재 장착 중인 무기의 재능 분류를 반환한다.
     *
     * <p>무기를 장착하지 않았거나 알 수 없는 경우 {@code null}을 반환한다.
     *
     * @return 장착 중인 무기의 재능 분류 (MELEE, ARCHERY, MAGIC, 또는 null)
     */
    public SkillTalent equippedWeaponTalent() {
        return equipmentService.equippedWeaponTalent();
    }

    /**
     * 특정 캐릭터의 현재 장착 중인 무기의 재능 분류를 반환한다.
     *
     * @param characterId 캐릭터 식별자
     * @return 장착 중인 무기의 재능 분류 (MELEE, ARCHERY, MAGIC, 또는 null)
     */
    public SkillTalent equippedWeaponTalent(final Long characterId) {
        return equipmentService.equippedWeaponTalent(characterId);
    }

    /**
     * 기본 캐릭터의 현재 활(BOW) 계열 무기 장착 여부를 반환한다.
     *
     * @return 활을 장착 중이면 {@code true}, 아니면 {@code false}
     */
    public boolean isBowEquipped() {
        return equipmentService.isBowEquipped();
    }

    /**
     * 특정 캐릭터의 현재 활(BOW) 계열 무기 장착 여부를 반환한다.
     *
     * @param characterId 캐릭터 식별자
     * @return 활을 장착 중이면 {@code true}, 아니면 {@code false}
     */
    public boolean isBowEquipped(final Long characterId) {
        return equipmentService.isBowEquipped(characterId);
    }
}
