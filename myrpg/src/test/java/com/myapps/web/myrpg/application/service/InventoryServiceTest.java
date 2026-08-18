package com.myapps.web.myrpg.application.service;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * InventoryService 핵심 메서드의 예시 단위 테스트.
 *
 * <p>착용 규칙(한손검+방패 병용, 양손검+방패 충돌, 스왑),
 * 포션 사용(HP 상한 클램프), 아이템 상세 생성(포션/장비 문구)을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    private static final int MAX_DURABILITY = 20;

    @Mock
    private OwnedItemRepository ownedItemRepository;

    @Mock
    private ItemCatalogService itemCatalogService;

    @Mock
    private CharacterProgressRepository characterProgressRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        final StatProgression statProgression = new StatProgression();
        inventoryService = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository, statProgression,
                mock(com.myapps.web.myrpg.domain.model.ActionLog.class),
                mock(com.myapps.web.myrpg.application.service.SkillCatalogService.class),
                mock(com.myapps.web.myrpg.domain.repository.CharacterSkillRepository.class));
    }

    // ─── 착용 규칙 테스트 ───────────────────────────────────────────────────

    /**
     * 한손검과 방패는 슬롯이 겹치지 않으므로 병용 착용이 허용됨을 검증한다.
     */
    @Test
    void should_allowEquip_when_oneHandSwordAndShield() {
        final OwnedItem shield = createOwnedItem(2L, "beginner_shield", false);
        final OwnedItem oneHandSword = createOwnedItem(1L, "beginner_one_hand_sword", true);

        final EquipmentItem shieldCatalog = new EquipmentItem(
                "beginner_shield", "초보자용 방패", ItemType.ARMOR,
                EquipmentKind.SHIELD, List.of(new EquipBonus(BonusTarget.DEF, 5)),
                null, MAX_DURABILITY);

        final EquipmentItem swordCatalog = new EquipmentItem(
                "beginner_one_hand_sword", "초보자용 한손검", ItemType.WEAPON,
                EquipmentKind.ONE_HANDED_SWORD, List.of(new EquipBonus(BonusTarget.STR, 5)),
                null, MAX_DURABILITY);

        when(ownedItemRepository.findById(2L)).thenReturn(Optional.of(shield));
        when(itemCatalogService.byId("beginner_shield")).thenReturn(Optional.of(shieldCatalog));
        when(itemCatalogService.byId("beginner_one_hand_sword")).thenReturn(Optional.of(swordCatalog));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(oneHandSword));

        inventoryService.equip(2L);

        assertThat(shield.isEquipped()).isTrue();
    }

    /**
     * 양손검 장착 중 방패 착용을 시도하면 EquipConflictException이 발생함을 검증한다.
     */
    @Test
    void should_throwConflict_when_twoHandSwordWithShield() {
        final OwnedItem shield = createOwnedItem(2L, "beginner_shield", false);
        final OwnedItem twoHandSword = createOwnedItem(1L, "beginner_two_hand_sword", true);

        final EquipmentItem shieldCatalog = new EquipmentItem(
                "beginner_shield", "초보자용 방패", ItemType.ARMOR,
                EquipmentKind.SHIELD, List.of(new EquipBonus(BonusTarget.DEF, 5)),
                null, MAX_DURABILITY);

        final EquipmentItem twoHandCatalog = new EquipmentItem(
                "beginner_two_hand_sword", "초보자용 양손검", ItemType.WEAPON,
                EquipmentKind.TWO_HANDED_SWORD, List.of(new EquipBonus(BonusTarget.STR, 10)),
                null, MAX_DURABILITY);

        when(ownedItemRepository.findById(2L)).thenReturn(Optional.of(shield));
        when(itemCatalogService.byId("beginner_shield")).thenReturn(Optional.of(shieldCatalog));
        when(itemCatalogService.byId("beginner_two_hand_sword")).thenReturn(Optional.of(twoHandCatalog));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(twoHandSword));

        assertThatThrownBy(() -> inventoryService.equip(2L))
                .isInstanceOf(EquipConflictException.class);

        assertThat(shield.isEquipped()).isFalse();
    }

    /**
     * 방패 장착 중 양손검 착용을 시도하면 EquipConflictException이 발생함을 검증한다.
     */
    @Test
    void should_throwConflict_when_shieldWithTwoHandSword() {
        final OwnedItem twoHandSword = createOwnedItem(2L, "beginner_two_hand_sword", false);
        final OwnedItem shield = createOwnedItem(1L, "beginner_shield", true);

        final EquipmentItem twoHandCatalog = new EquipmentItem(
                "beginner_two_hand_sword", "초보자용 양손검", ItemType.WEAPON,
                EquipmentKind.TWO_HANDED_SWORD, List.of(new EquipBonus(BonusTarget.STR, 10)),
                null, MAX_DURABILITY);

        final EquipmentItem shieldCatalog = new EquipmentItem(
                "beginner_shield", "초보자용 방패", ItemType.ARMOR,
                EquipmentKind.SHIELD, List.of(new EquipBonus(BonusTarget.DEF, 5)),
                null, MAX_DURABILITY);

        when(ownedItemRepository.findById(2L)).thenReturn(Optional.of(twoHandSword));
        when(itemCatalogService.byId("beginner_two_hand_sword")).thenReturn(Optional.of(twoHandCatalog));
        when(itemCatalogService.byId("beginner_shield")).thenReturn(Optional.of(shieldCatalog));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(shield));

        assertThatThrownBy(() -> inventoryService.equip(2L))
                .isInstanceOf(EquipConflictException.class);

        assertThat(twoHandSword.isEquipped()).isFalse();
    }

    /**
     * 한손검 장착 중 새 한손검을 착용하면 기존 한손검이 해제(스왑)됨을 검증한다.
     */
    @Test
    void should_swapWeapon_when_equipNewOneHandSword() {
        final OwnedItem newSword = createOwnedItem(2L, "new_one_hand_sword", false);
        final OwnedItem oldSword = createOwnedItem(1L, "beginner_one_hand_sword", true);

        final EquipmentItem newSwordCatalog = new EquipmentItem(
                "new_one_hand_sword", "새 한손검", ItemType.WEAPON,
                EquipmentKind.ONE_HANDED_SWORD, List.of(new EquipBonus(BonusTarget.STR, 8)),
                null, MAX_DURABILITY);

        final EquipmentItem oldSwordCatalog = new EquipmentItem(
                "beginner_one_hand_sword", "초보자용 한손검", ItemType.WEAPON,
                EquipmentKind.ONE_HANDED_SWORD, List.of(new EquipBonus(BonusTarget.STR, 5)),
                null, MAX_DURABILITY);

        when(ownedItemRepository.findById(2L)).thenReturn(Optional.of(newSword));
        when(itemCatalogService.byId("new_one_hand_sword")).thenReturn(Optional.of(newSwordCatalog));
        when(itemCatalogService.byId("beginner_one_hand_sword")).thenReturn(Optional.of(oldSwordCatalog));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(oldSword));

        inventoryService.equip(2L);

        assertThat(newSword.isEquipped()).isTrue();
        assertThat(oldSword.isEquipped()).isFalse();
    }

    // ─── 포션 사용 테스트 ───────────────────────────────────────────────────

    /**
     * HP가 거의 만 상태에서 포션을 사용하면 hpMax를 초과하지 않음을 검증한다.
     */
    @Test
    void should_clampHp_when_usePotionAtNearMax() {
        final OwnedItem potion = createOwnedItem(1L, "hp_potion_50", false);
        setQuantity(potion, 3);

        final PotionItem potionCatalog = new PotionItem("hp_potion_50", "생명력 50 포션", 50, 30);

        final CharacterProgress character = new CharacterProgress(
                "테스트", 1, 1, 0L, TalentType.MELEE, null,
                90, 100, 100, "tir-chonaill", 0, 0L);

        when(ownedItemRepository.findById(1L)).thenReturn(Optional.of(potion));
        when(itemCatalogService.byId("hp_potion_50")).thenReturn(Optional.of(potionCatalog));
        when(characterProgressRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(character));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of());

        inventoryService.usePotion(1L);

        // HP는 100(max)을 초과하면 안 됨: min(90+50, 100) = 100
        assertThat(character.getHpCurrent()).isEqualTo(100);
        assertThat(potion.getQuantity()).isEqualTo(2);
    }

    // ─── 상세 생성 테스트 ───────────────────────────────────────────────────

    /**
     * 포션 아이템의 상세 설명에 "생명력을 50 회복한다."가 포함됨을 검증한다.
     */
    @Test
    void should_describePotion_when_potionItem() {
        final PotionItem potionCatalog = new PotionItem("hp_potion_50", "생명력 50 포션", 50, 30);
        final OwnedItem potion = createOwnedItem(1L, "hp_potion_50", false);

        final List<String> lines = inventoryService.describe(potionCatalog, potion);

        assertThat(lines).contains("생명력을 50 회복한다.");
    }

    /**
     * 장비 아이템의 상세 설명에 보너스 라인과 내구도가 포함됨을 검증한다.
     */
    @Test
    void should_describeEquipment_when_equipmentItem() {
        final EquipmentItem swordCatalog = new EquipmentItem(
                "beginner_one_hand_sword", "초보자용 한손검", ItemType.WEAPON,
                EquipmentKind.ONE_HANDED_SWORD, List.of(new EquipBonus(BonusTarget.STR, 5)),
                null, MAX_DURABILITY);

        final OwnedItem sword = new OwnedItem(
                "beginner_one_hand_sword", 1, StorageKind.INVENTORY, true, 18.0);

        final List<String> lines = inventoryService.describe(swordCatalog, sword);

        assertThat(lines).anyMatch(line -> line.contains("STR") && line.contains("+5"));
        assertThat(lines).anyMatch(line -> line.contains("18") && line.contains("/" + MAX_DURABILITY));
    }

    // ─── 뷰 조립 테스트 ────────────────────────────────────────────────────

    /**
     * buildInventoryView가 INVENTORY 아이템을 OwnedItemView 목록으로 변환하고
     * 보유 골드를 포함하는 InventoryView를 반환함을 검증한다.
     */
    @Test
    void should_buildInventoryView_when_inventoryHasItems() {
        final OwnedItem potion = createOwnedItem(1L, "hp_potion_50", false);
        setQuantity(potion, 5);
        final OwnedItem sword = createOwnedItem(2L, "beginner_one_hand_sword", true);

        final PotionItem potionCatalog = new PotionItem("hp_potion_50", "생명력 50 포션", 50, 30);
        final EquipmentItem swordCatalog = new EquipmentItem(
                "beginner_one_hand_sword", "초보자용 한손검", ItemType.WEAPON,
                EquipmentKind.ONE_HANDED_SWORD, List.of(new EquipBonus(BonusTarget.STR, 5)),
                null, MAX_DURABILITY);

        when(ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY))
                .thenReturn(List.of(potion, sword));
        when(itemCatalogService.byId("hp_potion_50")).thenReturn(Optional.of(potionCatalog));
        when(itemCatalogService.byId("beginner_one_hand_sword")).thenReturn(Optional.of(swordCatalog));

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

    /**
     * buildInventoryView가 빈 인벤토리일 때 빈 목록을 반환함을 검증한다.
     */
    @Test
    void should_returnEmptyItems_when_inventoryEmpty() {
        when(ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY))
                .thenReturn(List.of());

        final InventoryView view = inventoryService.buildInventoryView(0L);

        assertThat(view.gold()).isEqualTo(0L);
        assertThat(view.items()).isEmpty();
    }

    /**
     * buildBankView가 은행·소지품 양쪽 목록과 골드를 포함하는 BankView를 반환함을 검증한다.
     */
    @Test
    void should_buildBankView_when_bankAndInventoryHaveItems() {
        final OwnedItem bankPotion = createOwnedItemWithStorage(1L, "hp_potion_50", false, StorageKind.BANK);
        setQuantity(bankPotion, 10);
        final OwnedItem invSword = createOwnedItem(2L, "beginner_one_hand_sword", true);

        final PotionItem potionCatalog = new PotionItem("hp_potion_50", "생명력 50 포션", 50, 30);
        final EquipmentItem swordCatalog = new EquipmentItem(
                "beginner_one_hand_sword", "초보자용 한손검", ItemType.WEAPON,
                EquipmentKind.ONE_HANDED_SWORD, List.of(new EquipBonus(BonusTarget.STR, 5)),
                null, MAX_DURABILITY);

        when(ownedItemRepository.findByStorageOrderById(StorageKind.BANK))
                .thenReturn(List.of(bankPotion));
        when(ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY))
                .thenReturn(List.of(invSword));
        when(itemCatalogService.byId("hp_potion_50")).thenReturn(Optional.of(potionCatalog));
        when(itemCatalogService.byId("beginner_one_hand_sword")).thenReturn(Optional.of(swordCatalog));

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
     * @param id       설정할 엔티티 ID
     * @param itemId   아이템 카탈로그 ID
     * @param equipped 장착 여부
     * @return 설정된 OwnedItem 인스턴스
     */
    private OwnedItem createOwnedItem(final long id, final String itemId, final boolean equipped) {
        final OwnedItem item = new OwnedItem(itemId, 1, StorageKind.INVENTORY, equipped, MAX_DURABILITY);
        setId(item, id);
        return item;
    }

    /**
     * 리플렉션으로 OwnedItem의 id 필드를 설정한다.
     *
     * @param item 대상 OwnedItem
     * @param id   설정할 ID 값
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
     * @param item     대상 OwnedItem
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
     * @param id       설정할 엔티티 ID
     * @param itemId   아이템 카탈로그 ID
     * @param equipped 장착 여부
     * @param storage  저장 위치
     * @return 설정된 OwnedItem 인스턴스
     */
    private OwnedItem createOwnedItemWithStorage(final long id, final String itemId,
                                                  final boolean equipped, final StorageKind storage) {
        final OwnedItem item = new OwnedItem(itemId, 1, storage, equipped, MAX_DURABILITY);
        setId(item, id);
        return item;
    }
}
