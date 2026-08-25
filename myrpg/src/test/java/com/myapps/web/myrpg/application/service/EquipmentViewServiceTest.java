package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.EquipmentSlotView;
import com.myapps.web.myrpg.application.dto.EquipmentView;
import com.myapps.web.myrpg.application.dto.OwnedItemView;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.lang.reflect.Field;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 장비 팝업 뷰 조립 및 스마트 장착 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class EquipmentViewServiceTest {

    @Mock private OwnedItemRepository ownedItemRepository;
    @Mock private ItemCatalogService itemCatalogService;
    @Mock private CharacterProgressRepository characterProgressRepository;
    @Mock private CharacterSkillRepository characterSkillRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        final StatProgression statProgression = new StatProgression();
        final ActionLog actionLog = new ActionLog(Clock.systemDefaultZone());
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        inventoryService =
                new InventoryService(
                        ownedItemRepository,
                        itemCatalogService,
                        characterProgressRepository,
                        statProgression,
                        actionLog,
                        skillCatalogService,
                        characterSkillRepository);
    }

    /** 3x3 장비 팝업 뷰가 9개 슬롯 및 보너스 합산과 함께 정상 조립되는지 검증한다. */
    @Test
    void should_buildNineSlotsEquipmentView_withEquippedItems() throws Exception {
        final OwnedItem sword = createOwnedItem(1L, "sword", true, 20.0);
        final EquipmentItem swordItem =
                new EquipmentItem(
                        "sword",
                        "롱소드",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 5)),
                        100,
                        20);

        final OwnedItem shield = createOwnedItem(2L, "shield", true, 15.0);
        final EquipmentItem shieldItem =
                new EquipmentItem(
                        "shield",
                        "라운드실드",
                        ItemType.ARMOR,
                        EquipmentKind.SHIELD,
                        List.of(new EquipBonus(BonusTarget.DEF, 4)),
                        80,
                        15);

        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(sword, shield));
        when(itemCatalogService.byId("sword")).thenReturn(Optional.of(swordItem));
        when(itemCatalogService.byId("shield")).thenReturn(Optional.of(shieldItem));

        final EquipmentView view = inventoryService.buildEquipmentView(1L);

        assertThat(view.slots()).hasSize(9);
        assertThat(view.equippedCount()).isEqualTo(2);

        // Row 1: ACC1 (locked), HEAD (empty), ACC2 (locked)
        assertThat(view.slots().get(0).slotId()).isEqualTo("ACC1");
        assertThat(view.slots().get(0).locked()).isTrue();
        assertThat(view.slots().get(1).slotId()).isEqualTo("HEAD");
        assertThat(view.slots().get(1).equipped()).isFalse();
        assertThat(view.slots().get(2).slotId()).isEqualTo("ACC2");
        assertThat(view.slots().get(2).locked()).isTrue();

        // Row 2: MAIN_HAND (sword), BODY (empty), OFF_HAND (shield)
        final EquipmentSlotView mainHand = view.slots().get(3);
        assertThat(mainHand.slotId()).isEqualTo("MAIN_HAND");
        assertThat(mainHand.equipped()).isTrue();
        assertThat(mainHand.itemName()).isEqualTo("롱소드");
        assertThat(mainHand.durabilityPercent()).isEqualTo(100);

        final EquipmentSlotView offHand = view.slots().get(5);
        assertThat(offHand.slotId()).isEqualTo("OFF_HAND");
        assertThat(offHand.equipped()).isTrue();
        assertThat(offHand.itemName()).isEqualTo("라운드실드");

        // Row 3: HANDS (empty), FEET (empty), ROBE (locked)
        assertThat(view.slots().get(8).slotId()).isEqualTo("ROBE");
        assertThat(view.slots().get(8).locked()).isTrue();

        // 스탯 보너스 합산 확인
        assertThat(view.bonusResult().statBonus().str()).isEqualTo(5);
        assertThat(view.bonusResult().statBonus().defense()).isEqualTo(4);
    }

    /** 양손무기 착용 시 보조손 슬롯이 blockedByTwoHanded로 표시되는지 검증한다. */
    @Test
    void should_markOffHandBlocked_when_twoHandedWeaponEquipped() throws Exception {
        final OwnedItem bow = createOwnedItem(1L, "bow", true, 20.0);
        final EquipmentItem bowItem =
                new EquipmentItem(
                        "bow",
                        "롱보우",
                        ItemType.WEAPON,
                        EquipmentKind.BOW,
                        List.of(new EquipBonus(BonusTarget.DEX, 7)),
                        150,
                        20);

        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(bow));
        when(itemCatalogService.byId("bow")).thenReturn(Optional.of(bowItem));

        final EquipmentView view = inventoryService.buildEquipmentView(1L);

        final EquipmentSlotView offHand = view.slots().get(5);
        assertThat(offHand.slotId()).isEqualTo("OFF_HAND");
        assertThat(offHand.blockedByTwoHanded()).isTrue();
        assertThat(offHand.equipped()).isFalse();
    }

    /** 한손검+방패 상태에서 활(양손무기) 스마트 착용 시 방패가 자동 해제되는지 검증한다. */
    @Test
    void should_autoUnequipShield_when_smartEquipTwoHandedBow() throws Exception {
        final OwnedItem sword = createOwnedItem(1L, "sword", true, 20.0);
        final EquipmentItem swordItem =
                new EquipmentItem(
                        "sword",
                        "롱소드",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(),
                        100,
                        20);

        final OwnedItem shield = createOwnedItem(2L, "shield", true, 15.0);
        final EquipmentItem shieldItem =
                new EquipmentItem(
                        "shield", "라운드실드", ItemType.ARMOR, EquipmentKind.SHIELD, List.of(), 80, 15);

        final OwnedItem bow = createOwnedItem(3L, "bow", false, 20.0);
        final EquipmentItem bowItem =
                new EquipmentItem(
                        "bow", "롱보우", ItemType.WEAPON, EquipmentKind.BOW, List.of(), 150, 20);

        when(ownedItemRepository.findById(3L)).thenReturn(Optional.of(bow));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(sword, shield));
        when(itemCatalogService.byId("sword")).thenReturn(Optional.of(swordItem));
        when(itemCatalogService.byId("shield")).thenReturn(Optional.of(shieldItem));
        when(itemCatalogService.byId("bow")).thenReturn(Optional.of(bowItem));

        inventoryService.smartEquip(3L);

        assertThat(shield.isEquipped()).isFalse();
        assertThat(sword.isEquipped()).isFalse();
        assertThat(bow.isEquipped()).isTrue();
    }

    /** 양손무기 상태에서 방패 스마트 착용 시 양손무기가 자동 해제되는지 검증한다. */
    @Test
    void should_autoUnequipTwoHandedWeapon_when_smartEquipShield() throws Exception {
        final OwnedItem bow = createOwnedItem(1L, "bow", true, 20.0);
        final EquipmentItem bowItem =
                new EquipmentItem(
                        "bow", "롱보우", ItemType.WEAPON, EquipmentKind.BOW, List.of(), 150, 20);

        final OwnedItem shield = createOwnedItem(2L, "shield", false, 15.0);
        final EquipmentItem shieldItem =
                new EquipmentItem(
                        "shield", "라운드실드", ItemType.ARMOR, EquipmentKind.SHIELD, List.of(), 80, 15);

        when(ownedItemRepository.findById(2L)).thenReturn(Optional.of(shield));
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(bow));
        when(itemCatalogService.byId("bow")).thenReturn(Optional.of(bowItem));
        when(itemCatalogService.byId("shield")).thenReturn(Optional.of(shieldItem));

        inventoryService.smartEquip(2L);

        assertThat(bow.isEquipped()).isFalse();
        assertThat(shield.isEquipped()).isTrue();
    }

    /** 특정 슬롯에 장착 가능한 미장착 후보 아이템만 필터링되는지 검증한다. */
    @Test
    void should_findEquippableCandidates_forSpecificSlot() throws Exception {
        final OwnedItem helmet1 = createOwnedItem(1L, "helmet1", false, 20.0);
        final EquipmentItem helmetItem1 =
                new EquipmentItem(
                        "helmet1", "철투구", ItemType.ARMOR, EquipmentKind.HELMET, List.of(), 100, 20);

        final OwnedItem helmet2Equipped = createOwnedItem(2L, "helmet2", true, 20.0);
        final EquipmentItem helmetItem2 =
                new EquipmentItem(
                        "helmet2", "가죽투구", ItemType.ARMOR, EquipmentKind.HELMET, List.of(), 50, 20);

        final OwnedItem sword = createOwnedItem(3L, "sword", false, 20.0);
        final EquipmentItem swordItem =
                new EquipmentItem(
                        "sword",
                        "롱소드",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(),
                        100,
                        20);

        when(ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY))
                .thenReturn(List.of(helmet1, helmet2Equipped, sword));
        when(itemCatalogService.byId("helmet1")).thenReturn(Optional.of(helmetItem1));
        when(itemCatalogService.byId("sword")).thenReturn(Optional.of(swordItem));

        final List<OwnedItemView> candidates = inventoryService.findEquippableForSlot(null, "HEAD");

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).name()).isEqualTo("철투구");
    }

    private OwnedItem createOwnedItem(
            final Long id,
            final String itemId,
            final boolean equipped,
            final double currentDurability)
            throws Exception {
        final OwnedItem item =
                new OwnedItem(1L, itemId, 1, StorageKind.INVENTORY, equipped, currentDurability);
        final Field idField = OwnedItem.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(item, id);
        return item;
    }
}
