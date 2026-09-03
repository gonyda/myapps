package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.OwnedItemView;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
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
 * InventoryService 분해 리팩토링 전 현행 동작을 고정하기 위한 Characterization 테스트.
 *
 * <p>미커버 분기(findEquippableForSlot 잘못된 slotCode, findCharacterSafe/loadCharacter 폴백, 무기 미착용 시 null
 * 폴백, 비포션 아이템 포션 사용 예외 등)를 사전에 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class InventoryCharacterizationTest {

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
                        mock(SkillCatalogService.class),
                        mock(CharacterSkillRepository.class));
    }

    @Test
    void should_returnEmptyList_when_invalidSlotCodeInFindEquippableForSlot() {
        // Given
        final Long characterId = 1L;
        final String invalidSlotCode = "INVALID_SLOT_NAME";

        // When
        final List<OwnedItemView> result =
                inventoryService.findEquippableForSlot(characterId, invalidSlotCode);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void should_fallbackToFirstCharacter_when_characterIdNotFoundInFindEquippableForSlot() {
        // Given: characterId 999L is not found by ID, but first character exists with id 1L
        final Long characterId = 999L;
        final CharacterProgress fallbackCharacter = createProgress(1L, 100, 100);
        when(characterProgressRepository.findById(characterId)).thenReturn(Optional.empty());
        when(characterProgressRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(fallbackCharacter));

        final OwnedItem helmet = createOwnedItem(characterId, 10L, "test_helmet", false);
        when(ownedItemRepository.findByCharacterIdAndStorageOrderById(
                        characterId, StorageKind.INVENTORY))
                .thenReturn(List.of(helmet));

        final EquipmentItem helmetItem =
                new EquipmentItem(
                        "test_helmet",
                        "투구",
                        ItemType.ARMOR,
                        EquipmentKind.HELMET,
                        List.of(),
                        null,
                        10);
        when(itemCatalogService.byId("test_helmet")).thenReturn(Optional.of(helmetItem));

        // When
        final List<OwnedItemView> result =
                inventoryService.findEquippableForSlot(characterId, "HEAD");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("투구");
    }

    @Test
    void should_returnEmptyList_when_noCharacterExistsAtAllInFindEquippableForSlot() {
        // Given: null characterId and no character in repository
        when(characterProgressRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY))
                .thenReturn(List.of());

        // When
        final List<OwnedItemView> result = inventoryService.findEquippableForSlot(null, "HEAD");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnNullTalent_when_noWeaponEquipped() {
        // Given: no equipped items
        when(ownedItemRepository.findByCharacterIdAndStorageAndEquippedTrue(
                        1L, StorageKind.INVENTORY))
                .thenReturn(List.of());

        // When
        final SkillTalent talent = inventoryService.equippedWeaponTalent(1L);

        // Then
        assertThat(talent).isNull();
    }

    @Test
    void should_returnFalse_when_isBowEquippedCalledWithNoItems() {
        // Given: no equipped items
        when(ownedItemRepository.findByCharacterIdAndStorageAndEquippedTrue(
                        1L, StorageKind.INVENTORY))
                .thenReturn(List.of());

        // When
        final boolean isBow = inventoryService.isBowEquipped(1L);

        // Then
        assertThat(isBow).isFalse();
    }

    @Test
    void should_throwIllegalStateException_when_usePotionOnNonPotionItem() {
        // Given: item in repo is armor/equipment, not potion
        final OwnedItem ownedSword = createOwnedItem(1L, 10L, "beginner_one_hand_sword", false);
        when(ownedItemRepository.findById(10L)).thenReturn(Optional.of(ownedSword));

        final EquipmentItem swordItem =
                new EquipmentItem(
                        "beginner_one_hand_sword",
                        "초보자용 한손검",
                        ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(),
                        null,
                        10);
        when(itemCatalogService.byId("beginner_one_hand_sword")).thenReturn(Optional.of(swordItem));

        // When / Then
        assertThatThrownBy(() -> inventoryService.usePotion(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("포션이 아닌 아이템은 사용할 수 없습니다");
    }

    private CharacterProgress createProgress(final Long id, final int currentHp, final int maxHp) {
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
            idField.set(progress, id);
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException("CharacterProgress id 설정 실패", exception);
        }
        return progress;
    }

    private OwnedItem createOwnedItem(
            final Long characterId, final Long id, final String itemId, final boolean equipped) {
        final OwnedItem owned =
                new OwnedItem(characterId, itemId, 1, StorageKind.INVENTORY, equipped, 10);
        try {
            final Field idField = OwnedItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(owned, id);
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException("OwnedItem id 설정 실패", exception);
        }
        return owned;
    }
}
