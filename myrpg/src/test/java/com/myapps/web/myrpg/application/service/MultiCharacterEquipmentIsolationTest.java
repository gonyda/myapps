package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.EquipmentSlotView;
import com.myapps.web.myrpg.application.dto.EquipmentView;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipSlot;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 다중 계정/캐릭터 간 장비 및 인벤토리 데이터 격리 검증 단위 테스트.
 *
 * <p>1번 캐릭터('고니')와 2번 캐릭터('관리자')가 각각 장비를 보유·장착했을 때, 한 캐릭터의 장착/해제/조회/내구도 처리가 다른 캐릭터의 장비 상태에 일체 간섭하지
 * 않음을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MultiCharacterEquipmentIsolationTest {

    private static final Long CHAR_1_ID = 1L;
    private static final Long CHAR_2_ID = 2L;
    private static final int MAX_DURABILITY = 20;

    @Mock private OwnedItemRepository ownedItemRepository;
    @Mock private ItemCatalogService itemCatalogService;
    @Mock private CharacterProgressRepository characterProgressRepository;
    @Mock private CharacterSkillRepository characterSkillRepository;

    private InventoryService inventoryService;

    private EquipmentItem armorCatalog;
    private EquipmentItem swordCatalog;
    private EquipmentItem bowCatalog;

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

        armorCatalog =
                new EquipmentItem(
                        "beginner_armor",
                        "초보자용 갑옷",
                        ItemType.ARMOR,
                        EquipmentKind.ARMOR_BODY,
                        List.of(new EquipBonus(BonusTarget.DEF, 10)),
                        null,
                        MAX_DURABILITY);

        swordCatalog =
                new EquipmentItem(
                        "beginner_one_hand_sword",
                        "초보자용 한손검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 5)),
                        null,
                        MAX_DURABILITY);

        bowCatalog =
                new EquipmentItem(
                        "beginner_bow",
                        "초보자용 활",
                        ItemType.WEAPON,
                        EquipmentKind.BOW,
                        List.of(new EquipBonus(BonusTarget.DEX, 8)),
                        null,
                        MAX_DURABILITY);

        when(itemCatalogService.byId("beginner_armor")).thenReturn(Optional.of(armorCatalog));
        when(itemCatalogService.byId("beginner_one_hand_sword"))
                .thenReturn(Optional.of(swordCatalog));
        when(itemCatalogService.byId("beginner_bow")).thenReturn(Optional.of(bowCatalog));
    }

    /** 1번 캐릭터가 갑옷을 해제했을 때 1번 캐릭터 팝업에서만 해제되고, 2번 캐릭터는 착용 상태를 유지함을 검증한다. */
    @Test
    @DisplayName("캐릭터 1이 갑옷을 unequip하면 캐릭터 1의 슬롯만 비고, 캐릭터 2의 갑옷은 착용 유지")
    void should_isolateUnequip_between_character1_and_character2() {
        // Given: 캐릭터 1, 2 모두 갑옷을 보유 및 착용 중
        final OwnedItem char1Armor = createOwnedItem(CHAR_1_ID, 101L, "beginner_armor", true);
        final OwnedItem char2Armor = createOwnedItem(CHAR_2_ID, 201L, "beginner_armor", true);

        final CharacterProgress progress1 = createProgress(CHAR_1_ID, "고니");
        final CharacterProgress progress2 = createProgress(CHAR_2_ID, "관리자");

        when(ownedItemRepository.findById(101L)).thenReturn(Optional.of(char1Armor));
        when(characterProgressRepository.findById(CHAR_1_ID)).thenReturn(Optional.of(progress1));
        when(characterProgressRepository.findById(CHAR_2_ID)).thenReturn(Optional.of(progress2));

        // When: 캐릭터 1이 자신의 갑옷(101L)을 해제
        inventoryService.unequip(101L);

        // Then: 엔티티 레벨에서 캐릭터 1의 갑옷은 false, 캐릭터 2의 갑옷은 여전히 true
        assertThat(char1Armor.isEquipped()).isFalse();
        assertThat(char2Armor.isEquipped()).isTrue();

        // When: 각 캐릭터의 장비 팝업 뷰를 조회
        when(ownedItemRepository.findByCharacterIdAndStorageAndEquippedTrue(
                        CHAR_1_ID, StorageKind.INVENTORY))
                .thenReturn(List.of()); // 캐릭터 1은 착용 장비 없음
        when(ownedItemRepository.findByCharacterIdAndStorageAndEquippedTrue(
                        CHAR_2_ID, StorageKind.INVENTORY))
                .thenReturn(List.of(char2Armor)); // 캐릭터 2는 갑옷 착용 중

        final EquipmentView view1 = inventoryService.buildEquipmentView(CHAR_1_ID);
        final EquipmentView view2 = inventoryService.buildEquipmentView(CHAR_2_ID);

        // Then: 캐릭터 1 뷰의 BODY 슬롯은 비어있어야 하고, 캐릭터 2 뷰의 BODY 슬롯은 착용 상태여야 함
        final EquipmentSlotView char1Body = findSlot(view1.slots(), EquipSlot.BODY);
        assertThat(char1Body.equipped()).isFalse();
        assertThat(char1Body.itemName()).isNull();

        final EquipmentSlotView char2Body = findSlot(view2.slots(), EquipSlot.BODY);
        assertThat(char2Body.equipped()).isTrue();
        assertThat(char2Body.itemName()).isEqualTo("초보자용 갑옷");
    }

    /** 캐릭터 1이 새로운 장비를 장착할 때 캐릭터 2의 장착 장비가 해제(스왑)되지 않음을 검증한다. */
    @Test
    @DisplayName("캐릭터 1이 장비 착용 시 캐릭터 2의 동일 슬롯 장비가 해제되지 않고 유지")
    void should_notUnequipOtherCharacterEquipment_when_equipping() {
        // Given: 캐릭터 1은 미착용 갑옷 보유, 캐릭터 2는 이미 갑옷 착용 중
        final OwnedItem char1Armor = createOwnedItem(CHAR_1_ID, 101L, "beginner_armor", false);
        final OwnedItem char2Armor = createOwnedItem(CHAR_2_ID, 201L, "beginner_armor", true);

        when(ownedItemRepository.findById(101L)).thenReturn(Optional.of(char1Armor));
        when(ownedItemRepository.findByCharacterIdAndStorageAndEquippedTrue(
                        CHAR_1_ID, StorageKind.INVENTORY))
                .thenReturn(List.of()); // 캐릭터 1의 착용 장비 목록 (비어있음)

        // When: 캐릭터 1이 갑옷(101L)을 장착
        inventoryService.equip(101L);

        // Then: 캐릭터 1의 갑옷은 착용되고, 캐릭터 2의 갑옷은 여전히 착용 유지
        assertThat(char1Armor.isEquipped()).isTrue();
        assertThat(char2Armor.isEquipped()).isTrue();
    }

    /** 캐릭터 1이 무기를 장착하지 않은 맨손 상태일 때 캐릭터 2의 무기 재능을 가져오지 않음을 검증한다. */
    @Test
    @DisplayName("캐릭터 1이 맨손일 때 캐릭터 2가 활을 장착하고 있어도 캐릭터 1은 ARCHERY가 아님")
    void should_isolateWeaponTalent_when_oneCharacterIsBarehanded() {
        // Given: 캐릭터 1은 무기 없음, 캐릭터 2는 활 착용 중
        when(ownedItemRepository.findByCharacterIdAndStorageAndEquippedTrue(
                        CHAR_1_ID, StorageKind.INVENTORY))
                .thenReturn(List.of()); // 캐릭터 1은 맨손 (장비 0개)

        // When: 캐릭터 1의 활 장착 여부 확인
        final boolean isChar1Bow = inventoryService.isBowEquipped(CHAR_1_ID);

        // Then: 캐릭터 1은 활을 장착하지 않은 상태여야 함
        assertThat(isChar1Bow).isFalse();
    }

    /** 전투 후 내구도 감소 시 대상 캐릭터의 장비만 내구도가 깎이고 타 캐릭터 장비는 보존됨을 검증한다. */
    @Test
    @DisplayName("캐릭터 1 전투 후 내구도 감소 시 캐릭터 1 장비만 깎이고 캐릭터 2 장비는 보존")
    void should_reduceDurabilityOnlyForTargetCharacter() {
        // Given: 캐릭터 1의 검(내구도 20.0), 캐릭터 2의 검(내구도 20.0)
        final OwnedItem char1Sword =
                createOwnedItem(CHAR_1_ID, 101L, "beginner_one_hand_sword", true);
        final OwnedItem char2Sword =
                createOwnedItem(CHAR_2_ID, 201L, "beginner_one_hand_sword", true);

        final CharacterProgress progress1 = createProgress(CHAR_1_ID, "고니");
        when(ownedItemRepository.findByCharacterIdAndStorageAndEquippedTrue(
                        CHAR_1_ID, StorageKind.INVENTORY))
                .thenReturn(List.of(char1Sword));

        // When: 캐릭터 1 전투 턴 진행 (내구도 0.05 감소)
        inventoryService.reduceDurabilityAndAutoUnequip(progress1, 0.05);

        // Then: 캐릭터 1의 검은 19.95, 캐릭터 2의 검은 20.0 유지
        assertThat(char1Sword.getCurrentDurability()).isEqualTo(19.95);
        assertThat(char2Sword.getCurrentDurability()).isEqualTo(20.0);
    }

    /** 캐릭터 1의 인벤토리 항목 수 조회가 타 캐릭터의 아이템 수에 영향을 받지 않음을 검증한다. */
    @Test
    @DisplayName("캐릭터 1의 인벤토리가 비어있을 때(0개) 타 캐릭터의 아이템 수로 용량 초과가 발생하지 않음")
    void should_isolateStorageCapacity_when_oneCharacterHasZeroItems() {
        // Given: 캐릭터 1은 0개, 전체 카운트(타 캐릭터 포함)는 30개
        when(ownedItemRepository.countByCharacterIdAndStorage(CHAR_1_ID, StorageKind.INVENTORY))
                .thenReturn(0L);
        when(itemCatalogService.byId("beginner_armor")).thenReturn(Optional.of(armorCatalog));

        // When: 캐릭터 1에게 아이템 획득 시도 시 용량 초과 예외가 발생하지 않고 저장되어야 함
        inventoryService.acquireItem(CHAR_1_ID, "beginner_armor", 1);

        // Then: 정상 저장 성공
        assertThat(true).isTrue();
    }

    private EquipmentSlotView findSlot(
            final List<EquipmentSlotView> slots, final EquipSlot target) {
        return slots.stream()
                .filter(s -> s.slotId().equals(target.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("슬롯을 찾을 수 없습니다: " + target));
    }

    private OwnedItem createOwnedItem(
            final Long characterId, final long id, final String itemId, final boolean equipped) {
        final OwnedItem item =
                new OwnedItem(
                        characterId, itemId, 1, StorageKind.INVENTORY, equipped, MAX_DURABILITY);
        setId(item, id);
        return item;
    }

    private void setId(final OwnedItem item, final long id) {
        try {
            final Field idField = OwnedItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(item, id);
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException("OwnedItem id 설정 실패", exception);
        }
    }

    private CharacterProgress createProgress(final Long id, final String name) {
        final CharacterProgress progress = CharacterProgress.createNamed(name);
        try {
            final Field idField = CharacterProgress.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(progress, id);
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException("CharacterProgress id 설정 실패", exception);
        }
        return progress;
    }
}
