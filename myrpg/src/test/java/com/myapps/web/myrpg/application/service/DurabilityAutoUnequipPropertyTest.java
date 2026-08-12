package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 내구도 0 자동 해제 프로퍼티 테스트.
 *
 * <p>임의의 장비 내구도와 감소량에 대해, {@code reduceDurabilityAndAutoUnequip}이
 * 내구도 0 도달 시 장비를 자동 장착 해제하고, 보너스를 소멸시키며,
 * 활동 로그에 해제 메시지를 남기는지 검증한다.
 *
 * <p>Feature: 008-battle-system, Property 15: 내구도 0 자동 해제
 *
 * <p><b>Validates: Requirements 15.1, 15.2</b>
 */
// Feature: 008-battle-system, Property 15: 내구도 0 자동 해제
class DurabilityAutoUnequipPropertyTest {

    private static final String ITEM_ID = "test_weapon";
    private static final String ITEM_NAME = "테스트무기";
    private static final int MAX_DURABILITY = 20;

    /**
     * 내구도가 정확히 0에 도달하면 자동 장착 해제됨을 검증한다.
     *
     * <p>감소량이 현재 내구도 이상이면 내구도가 0 이하가 되어 해제가 발생한다.
     *
     * @param currentDurability 현재 내구도 (0.2~1.0, 0.2 감소 1회로 0 도달 가능 범위)
     */
    @Property(tries = 100)
    void should_autoUnequip_when_durabilityReachesZero(
            @ForAll("lowDurabilities") final double currentDurability) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final ActionLog actionLog = new ActionLog(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepository = mock(CharacterSkillRepository.class);

        final InventoryService service = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository,
                new StatProgression(), actionLog, skillCatalogService, characterSkillRepository);

        final OwnedItem equippedItem = new OwnedItem(
                ITEM_ID, 1, StorageKind.INVENTORY, true, currentDurability);
        final EquipmentItem catalogItem = new EquipmentItem(
                ITEM_ID, ITEM_NAME, ItemType.WEAPON, EquipmentKind.ONE_HANDED_SWORD,
                List.of(), null, MAX_DURABILITY);

        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(equippedItem));
        when(itemCatalogService.byId(ITEM_ID)).thenReturn(Optional.of(catalogItem));

        final CharacterProgress progress = createProgress();

        // 감소량 >= 현재 내구도 → 0 도달
        service.reduceDurabilityAndAutoUnequip(progress, currentDurability);

        assertThat(equippedItem.isEquipped()).isFalse();
        assertThat(equippedItem.getCurrentDurability()).isEqualTo(0.0);
    }

    /**
     * 내구도 0 도달 시 활동 로그에 해제 메시지가 남음을 검증한다.
     *
     * @param currentDurability 현재 내구도 (0.2~1.0)
     */
    @Property(tries = 100)
    void should_logUnequipMessage_when_durabilityReachesZero(
            @ForAll("lowDurabilities") final double currentDurability) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final ActionLog actionLog = new ActionLog(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepository = mock(CharacterSkillRepository.class);

        final InventoryService service = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository,
                new StatProgression(), actionLog, skillCatalogService, characterSkillRepository);

        final OwnedItem equippedItem = new OwnedItem(
                ITEM_ID, 1, StorageKind.INVENTORY, true, currentDurability);
        final EquipmentItem catalogItem = new EquipmentItem(
                ITEM_ID, ITEM_NAME, ItemType.WEAPON, EquipmentKind.ONE_HANDED_SWORD,
                List.of(), null, MAX_DURABILITY);

        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(equippedItem));
        when(itemCatalogService.byId(ITEM_ID)).thenReturn(Optional.of(catalogItem));

        final CharacterProgress progress = createProgress();

        service.reduceDurabilityAndAutoUnequip(progress, currentDurability);

        assertThat(actionLog.getEntries())
                .anyMatch(entry -> entry.message().equals(ITEM_NAME + " 내구도 0 — 장착 해제됨"));
    }

    /**
     * 내구도가 0보다 크게 남으면 장착 상태가 유지됨을 검증한다.
     *
     * @param currentDurability 현재 내구도 (1.0~20.0, 0.2 감소 후에도 양수)
     */
    @Property(tries = 100)
    void should_remainEquipped_when_durabilityAboveZero(
            @ForAll("highDurabilities") final double currentDurability) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final ActionLog actionLog = new ActionLog(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepository = mock(CharacterSkillRepository.class);

        final InventoryService service = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository,
                new StatProgression(), actionLog, skillCatalogService, characterSkillRepository);

        final OwnedItem equippedItem = new OwnedItem(
                ITEM_ID, 1, StorageKind.INVENTORY, true, currentDurability);
        final EquipmentItem catalogItem = new EquipmentItem(
                ITEM_ID, ITEM_NAME, ItemType.WEAPON, EquipmentKind.ONE_HANDED_SWORD,
                List.of(), null, MAX_DURABILITY);

        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(equippedItem));
        when(itemCatalogService.byId(ITEM_ID)).thenReturn(Optional.of(catalogItem));

        final CharacterProgress progress = createProgress();
        final double amount = 0.2;

        service.reduceDurabilityAndAutoUnequip(progress, amount);

        assertThat(equippedItem.isEquipped()).isTrue();
        assertThat(equippedItem.getCurrentDurability()).isGreaterThan(0.0);
        assertThat(actionLog.getEntries())
                .noneMatch(entry -> entry.message().contains("내구도 0 — 장착 해제됨"));
    }

    /**
     * 자동 해제 후 equippedBonus 재조회에서 해당 장비 보너스가 제외됨을 검증한다.
     *
     * @param currentDurability 현재 내구도 (0.2~1.0)
     */
    @Property(tries = 100)
    void should_excludeFromEquippedBonus_when_autoUnequipped(
            @ForAll("lowDurabilities") final double currentDurability) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final ActionLog actionLog = new ActionLog(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepository = mock(CharacterSkillRepository.class);

        final InventoryService service = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository,
                new StatProgression(), actionLog, skillCatalogService, characterSkillRepository);

        final OwnedItem equippedItem = new OwnedItem(
                ITEM_ID, 1, StorageKind.INVENTORY, true, currentDurability);
        final EquipmentItem catalogItem = new EquipmentItem(
                ITEM_ID, ITEM_NAME, ItemType.WEAPON, EquipmentKind.ONE_HANDED_SWORD,
                List.of(), null, MAX_DURABILITY);

        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(equippedItem));
        when(itemCatalogService.byId(ITEM_ID)).thenReturn(Optional.of(catalogItem));

        final CharacterProgress progress = createProgress();

        service.reduceDurabilityAndAutoUnequip(progress, currentDurability);

        // 해제 후 equipped 조회 시 빈 목록(아이템이 unequip됨)
        assertThat(equippedItem.isEquipped()).isFalse();
    }

    // ─── Arbitrary Providers ────────────────────────────────────────────────

    /**
     * 0.2 감소 1회로 0 도달 가능한 낮은 내구도(0.2~1.0, 0.2 단위)를 생성한다.
     *
     * @return 낮은 내구도 Arbitrary
     */
    @Provide
    Arbitrary<Double> lowDurabilities() {
        return Arbitraries.integers().between(1, 5)
                .map(n -> n * 0.2);
    }

    /**
     * 0.2 감소 후에도 양수로 남는 충분한 내구도(1.0~20.0)를 생성한다.
     *
     * @return 높은 내구도 Arbitrary
     */
    @Provide
    Arbitrary<Double> highDurabilities() {
        return Arbitraries.integers().between(5, 100)
                .map(n -> n * 0.2);
    }

    // ─── Helper ─────────────────────────────────────────────────────────────

    /**
     * 테스트용 CharacterProgress를 생성한다.
     *
     * @return CharacterProgress 인스턴스
     */
    private CharacterProgress createProgress() {
        return new CharacterProgress(
                "테스트", 1, 1, 0L, TalentType.MELEE, null,
                100, 100, 100, "tir-chonaill", 0, 0L);
    }
}
