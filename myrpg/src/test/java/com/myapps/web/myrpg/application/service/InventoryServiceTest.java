package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.BankView;
import com.myapps.web.myrpg.application.dto.InventoryView;
import com.myapps.web.myrpg.application.dto.OwnedItemView;
import com.myapps.web.myrpg.application.exception.EquipConflictException;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.PotionItem;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * InventoryService 핵심 메서드의 예시 단위 테스트.
 *
 * <p>착용 규칙(한손검+방패 병용, 양손검+방패 충돌, 스왑), 포션 사용(HP 상한 클램프), 아이템 상세 생성(포션/장비 문구)을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    private static final int MAX_DURABILITY = 20;

    @Mock private OwnedItemRepository ownedItemRepository;

    @Mock private ItemCatalogService itemCatalogService;

    @Mock private CharacterProgressRepository characterProgressRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        final StatProgression statProgression = new StatProgression();
        inventoryService =
                new InventoryService(
                        ownedItemRepository,
                        itemCatalogService,
                        characterProgressRepository,
                        statProgression,
                        mock(com.myapps.web.myrpg.domain.model.ActionLog.class),
                        mock(com.myapps.web.myrpg.application.service.SkillCatalogService.class),
                        mock(
                                com.myapps.web.myrpg.domain.repository.CharacterSkillRepository
                                        .class));
    }

    // ─── 착용 규칙 테스트 ───────────────────────────────────────────────────

    /** 한손검과 방패는 슬롯이 겹치지 않으므로 병용 착용이 허용됨을 검증한다. */
    @Test
    void should_allowEquip_when_oneHandSwordAndShield() {
        final OwnedItem shield = createOwnedItem(2L, "beginner_shield", false);
        final OwnedItem oneHandSword = createOwnedItem(1L, "beginner_one_hand_sword", true);

        final EquipmentItem shieldCatalog =
                new EquipmentItem(
                        "beginner_shield",
                        "초보자용 방패",
                        ItemType.ARMOR,
                        EquipmentKind.SHIELD,
                        List.of(new EquipBonus(BonusTarget.DEF, 5)),
                        null,
                        MAX_DURABILITY);

        final EquipmentItem swordCatalog =
                new EquipmentItem(
                        "beginner_one_hand_sword",
                        "초보자용 한손검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 5)),
                        null,
                        MAX_DURABILITY);

        when(ownedItemRepository.findById(2L)).thenReturn(Optional.of(shield));
        when(itemCatalogService.byId("beginner_shield")).thenReturn(Optional.of(shieldCatalog));
        when(itemCatalogService.byId("beginner_one_hand_sword"))
                .thenReturn(Optional.of(swordCatalog));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(oneHandSword));

        inventoryService.equip(2L);

        assertThat(shield.isEquipped()).isTrue();
    }

    /** 양손검 장착 중 방패 착용을 시도하면 EquipConflictException이 발생함을 검증한다. */
    @Test
    void should_throwConflict_when_twoHandSwordWithShield() {
        final OwnedItem shield = createOwnedItem(2L, "beginner_shield", false);
        final OwnedItem twoHandSword = createOwnedItem(1L, "beginner_two_hand_sword", true);

        final EquipmentItem shieldCatalog =
                new EquipmentItem(
                        "beginner_shield",
                        "초보자용 방패",
                        ItemType.ARMOR,
                        EquipmentKind.SHIELD,
                        List.of(new EquipBonus(BonusTarget.DEF, 5)),
                        null,
                        MAX_DURABILITY);

        final EquipmentItem twoHandCatalog =
                new EquipmentItem(
                        "beginner_two_hand_sword",
                        "초보자용 양손검",
                        ItemType.WEAPON,
                        EquipmentKind.TWO_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 10)),
                        null,
                        MAX_DURABILITY);

        when(ownedItemRepository.findById(2L)).thenReturn(Optional.of(shield));
        when(itemCatalogService.byId("beginner_shield")).thenReturn(Optional.of(shieldCatalog));
        when(itemCatalogService.byId("beginner_two_hand_sword"))
                .thenReturn(Optional.of(twoHandCatalog));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(twoHandSword));

        assertThatThrownBy(() -> inventoryService.equip(2L))
                .isInstanceOf(EquipConflictException.class);

        assertThat(shield.isEquipped()).isFalse();
    }

    /** 방패 장착 중 양손검 착용을 시도하면 EquipConflictException이 발생함을 검증한다. */
    @Test
    void should_throwConflict_when_shieldWithTwoHandSword() {
        final OwnedItem twoHandSword = createOwnedItem(2L, "beginner_two_hand_sword", false);
        final OwnedItem shield = createOwnedItem(1L, "beginner_shield", true);

        final EquipmentItem twoHandCatalog =
                new EquipmentItem(
                        "beginner_two_hand_sword",
                        "초보자용 양손검",
                        ItemType.WEAPON,
                        EquipmentKind.TWO_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 10)),
                        null,
                        MAX_DURABILITY);

        final EquipmentItem shieldCatalog =
                new EquipmentItem(
                        "beginner_shield",
                        "초보자용 방패",
                        ItemType.ARMOR,
                        EquipmentKind.SHIELD,
                        List.of(new EquipBonus(BonusTarget.DEF, 5)),
                        null,
                        MAX_DURABILITY);

        when(ownedItemRepository.findById(2L)).thenReturn(Optional.of(twoHandSword));
        when(itemCatalogService.byId("beginner_two_hand_sword"))
                .thenReturn(Optional.of(twoHandCatalog));
        when(itemCatalogService.byId("beginner_shield")).thenReturn(Optional.of(shieldCatalog));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(shield));

        assertThatThrownBy(() -> inventoryService.equip(2L))
                .isInstanceOf(EquipConflictException.class);

        assertThat(twoHandSword.isEquipped()).isFalse();
    }

    /** 한손검 장착 중 새 한손검을 착용하면 기존 한손검이 해제(스왑)됨을 검증한다. */
    @Test
    void should_swapWeapon_when_equipNewOneHandSword() {
        final OwnedItem newSword = createOwnedItem(2L, "new_one_hand_sword", false);
        final OwnedItem oldSword = createOwnedItem(1L, "beginner_one_hand_sword", true);

        final EquipmentItem newSwordCatalog =
                new EquipmentItem(
                        "new_one_hand_sword",
                        "새 한손검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 8)),
                        null,
                        MAX_DURABILITY);

        final EquipmentItem oldSwordCatalog =
                new EquipmentItem(
                        "beginner_one_hand_sword",
                        "초보자용 한손검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 5)),
                        null,
                        MAX_DURABILITY);

        when(ownedItemRepository.findById(2L)).thenReturn(Optional.of(newSword));
        when(itemCatalogService.byId("new_one_hand_sword"))
                .thenReturn(Optional.of(newSwordCatalog));
        when(itemCatalogService.byId("beginner_one_hand_sword"))
                .thenReturn(Optional.of(oldSwordCatalog));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(oldSword));

        inventoryService.equip(2L);

        assertThat(newSword.isEquipped()).isTrue();
        assertThat(oldSword.isEquipped()).isFalse();
    }

    // ─── 포션 사용 테스트 ───────────────────────────────────────────────────

    /** HP가 거의 만 상태에서 포션을 사용하면 hpMax를 초과하지 않음을 검증한다. */
    @Test
    void should_clampHp_when_usePotionAtNearMax() {
        final OwnedItem potion = createOwnedItem(1L, "hp_potion_50", false);
        setQuantity(potion, 3);

        final PotionItem potionCatalog = new PotionItem("hp_potion_50", "생명력 50 포션", 50, 30);

        final CharacterProgress character =
                new CharacterProgress(
                        "테스트",
                        1,
                        1,
                        0L,
                        TalentType.MELEE,
                        null,
                        90,
                        100,
                        100,
                        "tir-chonaill",
                        0,
                        0L);

        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(potion));
        when(itemCatalogService.byId("hp_potion_50")).thenReturn(Optional.of(potionCatalog));
        when(characterProgressRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(character));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of());

        inventoryService.usePotion(1L);

        // HP는 100(max)을 초과하면 안 됨: min(90+50, 100) = 100
        assertThat(character.getHpCurrent()).isEqualTo(100);
        assertThat(potion.getQuantity()).isEqualTo(2);
    }

    // ─── 상세 생성 테스트 ───────────────────────────────────────────────────

    /** 포션 아이템의 상세 설명에 "생명력을 50 회복한다."가 포함됨을 검증한다. */
    @Test
    void should_describePotion_when_potionItem() {
        final PotionItem potionCatalog = new PotionItem("hp_potion_50", "생명력 50 포션", 50, 30);
        final OwnedItem potion = createOwnedItem(1L, "hp_potion_50", false);

        final List<String> lines = inventoryService.describe(potionCatalog, potion);

        assertThat(lines).contains("생명력을 50 회복한다.");
    }

    /** 장비 아이템의 상세 설명에 보너스 라인과 내구도가 포함됨을 검증한다. */
    @Test
    void should_describeEquipment_when_equipmentItem() {
        final EquipmentItem swordCatalog =
                new EquipmentItem(
                        "beginner_one_hand_sword",
                        "초보자용 한손검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 5)),
                        null,
                        MAX_DURABILITY);

        final OwnedItem sword =
                new OwnedItem("beginner_one_hand_sword", 1, StorageKind.INVENTORY, true, 18.0);

        final List<String> lines = inventoryService.describe(swordCatalog, sword);

        assertThat(lines).anyMatch(line -> line.contains("STR") && line.contains("+5"));
        assertThat(lines)
                .anyMatch(line -> line.contains("18") && line.contains("/" + MAX_DURABILITY));
    }

    // ─── 뷰 조립 테스트 ────────────────────────────────────────────────────

    /**
     * buildInventoryView가 INVENTORY 아이템을 OwnedItemView 목록으로 변환하고 보유 골드를 포함하는 InventoryView를 반환함을
     * 검증한다.
     */
    @Test
    void should_buildInventoryView_when_inventoryHasItems() {
        final OwnedItem potion = createOwnedItem(1L, "hp_potion_50", false);
        setQuantity(potion, 5);
        final OwnedItem sword = createOwnedItem(2L, "beginner_one_hand_sword", true);

        final PotionItem potionCatalog = new PotionItem("hp_potion_50", "생명력 50 포션", 50, 30);
        final EquipmentItem swordCatalog =
                new EquipmentItem(
                        "beginner_one_hand_sword",
                        "초보자용 한손검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 5)),
                        null,
                        MAX_DURABILITY);

        when(ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY))
                .thenReturn(List.of(potion, sword));
        when(itemCatalogService.byId("hp_potion_50")).thenReturn(Optional.of(potionCatalog));
        when(itemCatalogService.byId("beginner_one_hand_sword"))
                .thenReturn(Optional.of(swordCatalog));

        final InventoryView view = inventoryService.buildInventoryView(500L);

        assertThat(view.gold()).isEqualTo(500L);
        assertThat(view.items()).hasSize(2);

        final OwnedItemView potionView = view.items().get(0);
        assertThat(potionView.ownedItemId()).isEqualTo(1L);
        assertThat(potionView.name()).isEqualTo("생명력 50 포션");
        assertThat(potionView.typeLabel()).isEqualTo("포션");
        assertThat(potionView.type()).isEqualTo(ItemType.POTION);
        assertThat(potionView.quantity()).isEqualTo(5);
        assertThat(potionView.equipped()).isFalse();
        assertThat(potionView.usable()).isTrue();
        assertThat(potionView.equippable()).isFalse();
        assertThat(potionView.currentDurability()).isNull();
        assertThat(potionView.maxDurability()).isNull();
        assertThat(potionView.detailLines()).contains("생명력을 50 회복한다.");

        final OwnedItemView swordView = view.items().get(1);
        assertThat(swordView.ownedItemId()).isEqualTo(2L);
        assertThat(swordView.name()).isEqualTo("초보자용 한손검");
        assertThat(swordView.typeLabel()).isEqualTo("무기");
        assertThat(swordView.type()).isEqualTo(ItemType.WEAPON);
        assertThat(swordView.quantity()).isEqualTo(1);
        assertThat(swordView.equipped()).isTrue();
        assertThat(swordView.usable()).isFalse();
        assertThat(swordView.equippable()).isTrue();
        assertThat(swordView.currentDurability()).isEqualTo((double) MAX_DURABILITY);
        assertThat(swordView.maxDurability()).isEqualTo(MAX_DURABILITY);
        assertThat(swordView.detailLines()).isNotEmpty();
    }

    /** buildInventoryView가 빈 인벤토리일 때 빈 목록을 반환함을 검증한다. */
    @Test
    void should_returnEmptyItems_when_inventoryEmpty() {
        when(ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY))
                .thenReturn(List.of());

        final InventoryView view = inventoryService.buildInventoryView(0L);

        assertThat(view.gold()).isEqualTo(0L);
        assertThat(view.items()).isEmpty();
    }

    /** buildBankView가 은행·소지품 양쪽 목록과 골드를 포함하는 BankView를 반환함을 검증한다. */
    @Test
    void should_buildBankView_when_bankAndInventoryHaveItems() {
        final OwnedItem bankPotion =
                createOwnedItemWithStorage(1L, "hp_potion_50", false, StorageKind.BANK);
        setQuantity(bankPotion, 10);
        final OwnedItem invSword = createOwnedItem(2L, "beginner_one_hand_sword", true);

        final PotionItem potionCatalog = new PotionItem("hp_potion_50", "생명력 50 포션", 50, 30);
        final EquipmentItem swordCatalog =
                new EquipmentItem(
                        "beginner_one_hand_sword",
                        "초보자용 한손검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 5)),
                        null,
                        MAX_DURABILITY);

        when(ownedItemRepository.findByStorageOrderById(StorageKind.BANK))
                .thenReturn(List.of(bankPotion));
        when(ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY))
                .thenReturn(List.of(invSword));
        when(itemCatalogService.byId("hp_potion_50")).thenReturn(Optional.of(potionCatalog));
        when(itemCatalogService.byId("beginner_one_hand_sword"))
                .thenReturn(Optional.of(swordCatalog));

        final BankView view = inventoryService.buildBankView(300L, 1000L);

        assertThat(view.bankGold()).isEqualTo(1000L);
        assertThat(view.playerGold()).isEqualTo(300L);
        assertThat(view.bankItems()).hasSize(1);
        assertThat(view.inventoryItems()).hasSize(1);

        final OwnedItemView bankPotionView = view.bankItems().get(0);
        assertThat(bankPotionView.name()).isEqualTo("생명력 50 포션");
        assertThat(bankPotionView.quantity()).isEqualTo(10);
        assertThat(bankPotionView.usable()).isTrue();

        final OwnedItemView invSwordView = view.inventoryItems().get(0);
        assertThat(invSwordView.name()).isEqualTo("초보자용 한손검");
        assertThat(invSwordView.equipped()).isTrue();
        assertThat(invSwordView.equippable()).isTrue();
    }

    // ─── Helper 메서드 ──────────────────────────────────────────────────────

    /**
     * 테스트용 OwnedItem 인스턴스를 생성하고 리플렉션으로 ID를 설정한다.
     *
     * @param id 설정할 엔티티 ID
     * @param itemId 아이템 카탈로그 ID
     * @param equipped 장착 여부
     * @return 설정된 OwnedItem 인스턴스
     */
    private OwnedItem createOwnedItem(final long id, final String itemId, final boolean equipped) {
        return createOwnedItem(1L, id, itemId, equipped);
    }

    private OwnedItem createOwnedItem(
            final Long characterId, final long id, final String itemId, final boolean equipped) {
        final OwnedItem item =
                new OwnedItem(
                        characterId, itemId, 1, StorageKind.INVENTORY, equipped, MAX_DURABILITY);
        setId(item, id);
        return item;
    }

    /**
     * 리플렉션으로 OwnedItem의 id 필드를 설정한다.
     *
     * @param item 대상 OwnedItem
     * @param id 설정할 ID 값
     */
    private void setId(final OwnedItem item, final long id) {
        try {
            final Field idField = OwnedItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(item, id);
        } catch (final NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException("OwnedItem id 설정 실패", exception);
        }
    }

    /**
     * 리플렉션으로 OwnedItem의 quantity 필드를 설정한다.
     *
     * @param item 대상 OwnedItem
     * @param quantity 설정할 수량
     */
    private void setQuantity(final OwnedItem item, final int quantity) {
        try {
            final Field quantityField = OwnedItem.class.getDeclaredField("quantity");
            quantityField.setAccessible(true);
            quantityField.set(item, quantity);
        } catch (final NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException("OwnedItem quantity 설정 실패", exception);
        }
    }

    /**
     * 테스트용 OwnedItem 인스턴스를 지정된 저장 위치로 생성하고 리플렉션으로 ID를 설정한다.
     *
     * @param id 설정할 엔티티 ID
     * @param itemId 아이템 카탈로그 ID
     * @param equipped 장착 여부
     * @param storage 저장 위치
     * @return 설정된 OwnedItem 인스턴스
     */
    private OwnedItem createOwnedItemWithStorage(
            final long id, final String itemId, final boolean equipped, final StorageKind storage) {
        final OwnedItem item = new OwnedItem(itemId, 1, storage, equipped, MAX_DURABILITY);
        setId(item, id);
        return item;
    }

    private CharacterProgress createProgress(final int currentHp, final int maxHp) {
        final CharacterProgress progress =
                new CharacterProgress(
                        "테스트",
                        1,
                        1,
                        0L,
                        TalentType.MELEE,
                        null,
                        maxHp,
                        100,
                        100,
                        "tir-chonaill",
                        0,
                        0L);
        try {
            final Field idField = CharacterProgress.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(progress, 1L);
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException("CharacterProgress id 설정 실패", exception);
        }
        return progress;
    }

    @Test
    void should_swapWeapon_when_different_weapon_exists() {
        final CharacterProgress progress = createProgress(100, 100);
        progress.setWeapon2MainId(2L);
        final OwnedItem currentSword = createOwnedItem(1L, "sword", true);
        final OwnedItem bow = createOwnedItem(2L, "bow", false);

        final EquipmentItem swordCat =
                new EquipmentItem(
                        "sword",
                        "검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(),
                        null,
                        MAX_DURABILITY);
        final EquipmentItem bowCat =
                new EquipmentItem(
                        "bow",
                        "활",
                        ItemType.WEAPON,
                        EquipmentKind.BOW,
                        List.of(),
                        null,
                        MAX_DURABILITY);

        when(characterProgressRepository.findById(progress.getId()))
                .thenReturn(Optional.of(progress));
        when(itemCatalogService.byId("sword")).thenReturn(Optional.of(swordCat));
        when(itemCatalogService.byId("bow")).thenReturn(Optional.of(bowCat));
        when(ownedItemRepository.findById(2L)).thenReturn(Optional.of(bow));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(currentSword));

        final boolean swapped = inventoryService.swapWeapon(progress);

        assertThat(swapped).isTrue();
        assertThat(progress.getActiveWeaponSet()).isEqualTo(2);
        assertThat(bow.isEquipped()).isTrue();
        assertThat(currentSword.isEquipped()).isFalse();
    }

    @Test
    void should_return_false_when_no_other_weapon_to_swap() {
        final CharacterProgress progress = createProgress(100, 100);
        final OwnedItem currentSword = createOwnedItem(1L, "sword", false);

        when(characterProgressRepository.findById(progress.getId()))
                .thenReturn(Optional.of(progress));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of());

        final boolean swapped = inventoryService.swapWeapon(progress);

        assertThat(swapped).isFalse();
    }

    @Test
    void should_restore_shield_when_swapping_back_to_one_handed_weapon_set() {
        final CharacterProgress progress = createProgress(100, 100);
        progress.setWeapon2MainId(3L);
        final OwnedItem sword = createOwnedItem(1L, "sword", true);
        final OwnedItem shield = createOwnedItem(2L, "shield", true);
        final OwnedItem bow = createOwnedItem(3L, "bow", false);

        final EquipmentItem swordCat =
                new EquipmentItem(
                        "sword",
                        "한손검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(),
                        null,
                        MAX_DURABILITY);
        final EquipmentItem shieldCat =
                new EquipmentItem(
                        "shield",
                        "방패",
                        ItemType.ARMOR,
                        EquipmentKind.SHIELD,
                        List.of(),
                        null,
                        MAX_DURABILITY);
        final EquipmentItem bowCat =
                new EquipmentItem(
                        "bow",
                        "활",
                        ItemType.WEAPON,
                        EquipmentKind.BOW,
                        List.of(),
                        null,
                        MAX_DURABILITY);

        when(characterProgressRepository.findById(progress.getId()))
                .thenReturn(Optional.of(progress));
        when(itemCatalogService.byId("sword")).thenReturn(Optional.of(swordCat));
        when(itemCatalogService.byId("shield")).thenReturn(Optional.of(shieldCat));
        when(itemCatalogService.byId("bow")).thenReturn(Optional.of(bowCat));
        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(sword));
        when(ownedItemRepository.findById(2L)).thenReturn(Optional.of(shield));
        when(ownedItemRepository.findById(3L)).thenReturn(Optional.of(bow));

        // 1. 처음에는 한손검 + 방패 착용 상태에서 세트 2(활)로 스왑
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(sword, shield));

        final boolean swapToSet2 = inventoryService.swapWeapon(progress);
        assertThat(swapToSet2).isTrue();
        assertThat(progress.getActiveWeaponSet()).isEqualTo(2);
        assertThat(sword.isEquipped()).isFalse();
        assertThat(shield.isEquipped()).isFalse();
        assertThat(bow.isEquipped()).isTrue();

        // 2. 세트 2(활)에서 세트 1(한손검+방패)로 다시 스왑
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(bow));

        final boolean swapToSet1 = inventoryService.swapWeapon(progress);
        assertThat(swapToSet1).isTrue();
        assertThat(progress.getActiveWeaponSet()).isEqualTo(1);
        assertThat(sword.isEquipped()).isTrue();
        assertThat(shield.isEquipped()).isTrue();
        assertThat(bow.isEquipped()).isFalse();
    }

    @Test
    void should_allow_bare_hands_swap_when_weapon_set_is_empty() {
        final CharacterProgress progress = createProgress(100, 100);
        // 세트 1: 한손검(1L)+방패(2L), 세트 2: 빈손(null)
        final OwnedItem sword = createOwnedItem(1L, "sword", true);
        final OwnedItem shield = createOwnedItem(2L, "shield", true);

        final EquipmentItem swordCat =
                new EquipmentItem(
                        "sword",
                        "한손검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(),
                        null,
                        MAX_DURABILITY);
        final EquipmentItem shieldCat =
                new EquipmentItem(
                        "shield",
                        "방패",
                        ItemType.ARMOR,
                        EquipmentKind.SHIELD,
                        List.of(),
                        null,
                        MAX_DURABILITY);

        when(characterProgressRepository.findById(progress.getId()))
                .thenReturn(Optional.of(progress));
        when(itemCatalogService.byId("sword")).thenReturn(Optional.of(swordCat));
        when(itemCatalogService.byId("shield")).thenReturn(Optional.of(shieldCat));
        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(sword));
        when(ownedItemRepository.findById(2L)).thenReturn(Optional.of(shield));

        // 1. 한손검+방패 착용 상태에서 빈손 세트(2번 세트)로 스왑
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(sword, shield));

        final boolean swapToBareHands = inventoryService.swapWeapon(progress);
        assertThat(swapToBareHands).isTrue();
        assertThat(progress.getActiveWeaponSet()).isEqualTo(2);
        assertThat(sword.isEquipped()).isFalse();
        assertThat(shield.isEquipped()).isFalse();

        // 2. 빈손 세트(2번)에서 다시 1번 세트(한손검+방패)로 스왑
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of());

        final boolean swapBackToOneHand = inventoryService.swapWeapon(progress);
        assertThat(swapBackToOneHand).isTrue();
        assertThat(progress.getActiveWeaponSet()).isEqualTo(1);
        assertThat(sword.isEquipped()).isTrue();
        assertThat(shield.isEquipped()).isTrue();
    }

    @Test
    void should_exclude_other_weapon_set_items_from_equippable_candidates() {
        final CharacterProgress progress = createProgress(100, 100);
        progress.setActiveWeaponSet(2); // 현재 2번 세트 활성화 중
        progress.setWeapon1MainId(1L); // 1번 세트에 1L 한손검 등록됨
        progress.setWeapon1OffId(2L); // 1번 세트에 2L 방패 등록됨

        final OwnedItem set1Sword = createOwnedItem(1L, "sword", false);
        final OwnedItem set1Shield = createOwnedItem(2L, "shield", false);
        final OwnedItem freeDagger = createOwnedItem(3L, "dagger", false);

        final EquipmentItem swordCat =
                new EquipmentItem(
                        "sword",
                        "검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(),
                        null,
                        20);
        final EquipmentItem daggerCat =
                new EquipmentItem(
                        "dagger",
                        "단검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(),
                        null,
                        15);

        when(characterProgressRepository.findById(progress.getId()))
                .thenReturn(Optional.of(progress));
        when(ownedItemRepository.findByCharacterIdAndStorageOrderById(
                        progress.getId(), StorageKind.INVENTORY))
                .thenReturn(List.of(set1Sword, set1Shield, freeDagger));
        when(itemCatalogService.byId("dagger")).thenReturn(Optional.of(daggerCat));

        final List<com.myapps.web.myrpg.application.dto.OwnedItemView> candidates =
                inventoryService.findEquippableForSlot(progress.getId(), "MAIN_HAND");

        // 1번 세트에 등록된 set1Sword(1L)는 제외되고, 여유 장비인 freeDagger(3L)만 나와야 함
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).ownedItemId()).isEqualTo(3L);
    }

    @Test
    void should_mark_other_weapon_set_items_as_equipped_in_buildInventoryView() {
        final CharacterProgress progress = createProgress(100, 100);
        progress.setActiveWeaponSet(2); // 현재 2번 세트 활성화 (활 착용 중)
        progress.setWeapon1MainId(1L); // 1번 세트에 1L 한손검 배정
        progress.setWeapon1OffId(2L); // 1번 세트에 2L 방패 배정
        progress.setWeapon2MainId(3L); // 2번 세트에 3L 활 배정

        final OwnedItem set1Sword =
                createOwnedItem(1L, "sword", false); // DB상 비활성이므로 isEquipped=false
        final OwnedItem set1Shield = createOwnedItem(2L, "shield", false);
        final OwnedItem set2Bow = createOwnedItem(3L, "bow", true); // 현재 활성이므로 isEquipped=true
        final OwnedItem freePotion = createOwnedItem(4L, "hp_potion", false);

        final EquipmentItem swordCat =
                new EquipmentItem(
                        "sword",
                        "검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(),
                        null,
                        20);
        final EquipmentItem shieldCat =
                new EquipmentItem(
                        "shield", "방패", ItemType.ARMOR, EquipmentKind.SHIELD, List.of(), null, 20);
        final EquipmentItem bowCat =
                new EquipmentItem(
                        "bow", "활", ItemType.WEAPON, EquipmentKind.BOW, List.of(), null, 20);
        final PotionItem potionCat = new PotionItem("hp_potion", "포션", 30, 0, 0, 50);

        when(characterProgressRepository.findById(progress.getId()))
                .thenReturn(Optional.of(progress));
        when(ownedItemRepository.findByCharacterIdAndStorageOrderById(
                        progress.getId(), StorageKind.INVENTORY))
                .thenReturn(List.of(set1Sword, set1Shield, set2Bow, freePotion));
        when(itemCatalogService.byId("sword")).thenReturn(Optional.of(swordCat));
        when(itemCatalogService.byId("shield")).thenReturn(Optional.of(shieldCat));
        when(itemCatalogService.byId("bow")).thenReturn(Optional.of(bowCat));
        when(itemCatalogService.byId("hp_potion")).thenReturn(Optional.of(potionCat));

        final var inventoryView = inventoryService.buildInventoryView(progress.getId(), 500L);

        // set1Sword(1L), set1Shield(2L), set2Bow(3L) 모두 equipped=true여야 함
        assertThat(inventoryView.items().get(0).equipped()).isTrue();
        assertThat(inventoryView.items().get(1).equipped()).isTrue();
        assertThat(inventoryView.items().get(2).equipped()).isTrue();
        assertThat(inventoryView.items().get(3).equipped()).isFalse();
    }

    @Test
    void should_clear_inactive_weapon_set_slot_when_unequipped() {
        final CharacterProgress progress = createProgress(100, 100);
        progress.setActiveWeaponSet(2); // 2번 세트 활성
        progress.setWeapon1MainId(10L); // 1번 세트에 10L 한손검 배정

        final OwnedItem set1Sword = createOwnedItem(progress.getId(), 10L, "sword", false);

        when(ownedItemRepository.findById(10L)).thenReturn(Optional.of(set1Sword));
        when(characterProgressRepository.findById(progress.getId()))
                .thenReturn(Optional.of(progress));

        inventoryService.unequip(10L);

        assertThat(progress.getWeapon1MainId()).isNull();
        verify(characterProgressRepository).save(progress);
    }

    @Test
    void should_prevent_moving_other_weapon_set_item_to_bank() {
        final CharacterProgress progress = createProgress(100, 100);
        progress.setActiveWeaponSet(2);
        progress.setWeapon1MainId(10L); // 1번 세트에 10L 배정

        final OwnedItem set1Sword = createOwnedItem(progress.getId(), 10L, "sword", false);

        when(ownedItemRepository.findById(10L)).thenReturn(Optional.of(set1Sword));
        when(characterProgressRepository.findById(progress.getId()))
                .thenReturn(Optional.of(progress));

        assertThatThrownBy(() -> inventoryService.moveToBank(10L))
                .isInstanceOf(
                        com.myapps.web.myrpg.application.exception.EquipConflictException.class)
                .hasMessage("장착을 해제한 후 맡길 수 있습니다.");
    }

    @Test
    void should_clear_from_other_weapon_set_when_smart_equipped() {
        final CharacterProgress progress = createProgress(100, 100);
        progress.setActiveWeaponSet(2); // 2번 세트 활성
        progress.setWeapon1MainId(10L); // 1번 세트에 10L 배정

        final OwnedItem sword = createOwnedItem(progress.getId(), 10L, "sword", false);

        final EquipmentItem swordCat =
                new EquipmentItem(
                        "sword",
                        "검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(),
                        null,
                        20);

        when(ownedItemRepository.findById(10L)).thenReturn(Optional.of(sword));
        when(itemCatalogService.byId("sword")).thenReturn(Optional.of(swordCat));
        when(characterProgressRepository.findById(progress.getId()))
                .thenReturn(Optional.of(progress));
        when(ownedItemRepository.findByCharacterIdAndStorageAndEquippedTrue(
                        progress.getId(), StorageKind.INVENTORY))
                .thenReturn(List.of());

        inventoryService.smartEquip(10L);

        // 1번 세트에서는 해제되고 2번 세트의 주무기로 설정되어야 함
        assertThat(progress.getWeapon1MainId()).isNull();
        assertThat(progress.getWeapon2MainId()).isEqualTo(10L);
    }
}
