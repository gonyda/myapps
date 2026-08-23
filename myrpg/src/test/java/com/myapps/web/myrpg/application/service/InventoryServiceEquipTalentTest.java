package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * {@link InventoryService}의 착용 무기 재능 조회({@code equippedWeaponTalent}) 및 활 착용 판정({@code
 * isBowEquipped}) 단위 테스트.
 *
 * <p>Requirements: 2.4 (3) — 착용 무기 기준 재능 판정 및 활 1턴 선제 사격 무기 판정 검증
 */
class InventoryServiceEquipTalentTest {

    private OwnedItemRepository ownedItemRepository;
    private ItemCatalogService itemCatalogService;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        ownedItemRepository = mock(OwnedItemRepository.class);
        itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final StatProgression statProgression = mock(StatProgression.class);
        final ActionLog actionLog = mock(ActionLog.class);
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepository =
                mock(CharacterSkillRepository.class);

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

    @ParameterizedTest
    @CsvSource({
        "ONE_HANDED_SWORD, MELEE, false",
        "TWO_HANDED_SWORD, MELEE, false",
        "BOW, ARCHERY, true",
        "WAND, MAGIC, false",
        "STAFF, MAGIC, false"
    })
    @DisplayName("각 무기 종류 착용 시 올바른 재능과 활 여부를 반환한다")
    void should_returnCorrectTalentAndBowFlag_when_weaponEquipped(
            final String kindStr, final String expectedTalentStr, final boolean expectedIsBow) {
        // given
        final EquipmentKind kind = EquipmentKind.valueOf(kindStr);
        final SkillTalent expectedTalent = SkillTalent.valueOf(expectedTalentStr);
        final String itemId = "test_weapon_" + kindStr.toLowerCase();

        final OwnedItem equippedItem = new OwnedItem(itemId, 1, StorageKind.INVENTORY, true, 20.0);
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(equippedItem));

        final EquipmentItem catalogItem =
                new EquipmentItem(itemId, "테스트 무기", ItemType.WEAPON, kind, List.of(), 100, 20);
        when(itemCatalogService.byId(itemId)).thenReturn(Optional.of(catalogItem));

        // when
        final SkillTalent talent = inventoryService.equippedWeaponTalent();
        final boolean isBow = inventoryService.isBowEquipped();

        // then
        assertThat(talent).isEqualTo(expectedTalent);
        assertThat(isBow).isEqualTo(expectedIsBow);
    }

    @Test
    @DisplayName("무기를 착용하지 않은 경우 equippedWeaponTalent는 null, isBowEquipped는 false를 반환한다")
    void should_returnNullAndFalse_when_noWeaponEquipped() {
        // given
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(Collections.emptyList());

        // when
        final SkillTalent talent = inventoryService.equippedWeaponTalent();
        final boolean isBow = inventoryService.isBowEquipped();

        // then
        assertThat(talent).isNull();
        assertThat(isBow).isFalse();
    }

    @Test
    @DisplayName("방어구만 착용한 경우 equippedWeaponTalent는 null, isBowEquipped는 false를 반환한다")
    void should_returnNullAndFalse_when_onlyArmorEquipped() {
        // given
        final String itemId = "test_armor";
        final OwnedItem equippedItem = new OwnedItem(itemId, 1, StorageKind.INVENTORY, true, 20.0);
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(equippedItem));

        final EquipmentItem armorItem =
                new EquipmentItem(
                        itemId,
                        "테스트 갑옷",
                        ItemType.ARMOR,
                        EquipmentKind.ARMOR_BODY,
                        List.of(),
                        100,
                        20);
        when(itemCatalogService.byId(itemId)).thenReturn(Optional.of(armorItem));

        // when
        final SkillTalent talent = inventoryService.equippedWeaponTalent();
        final boolean isBow = inventoryService.isBowEquipped();

        // then
        assertThat(talent).isNull();
        assertThat(isBow).isFalse();
    }
}
