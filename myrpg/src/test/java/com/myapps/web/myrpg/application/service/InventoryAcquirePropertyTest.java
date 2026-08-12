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
import net.jqwik.api.Tuple;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;

import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DroppedItem;
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * 드랍 적재·용량 초과 프로퍼티 테스트.
 *
 * <p>임의의 {@link DropResult}와 인벤토리 상태에 대해,
 * {@code acquire}가 골드를 항상 가산하고, 용량 30 이내면 아이템을 적재하며,
 * 초과 시 해당 아이템을 소실(로그)시키되 나머지 아이템·골드 처리는 계속함을 검증한다.
 *
 * <p>Feature: 008-battle-system, Property 14: 드랍 적재·용량 초과
 *
 * <p><b>Validates: Requirements 13.2, 13.3</b>
 */
// Feature: 008-battle-system, Property 14: 드랍 적재·용량 초과
class InventoryAcquirePropertyTest {

    private static final int MAX_CAPACITY = 30;
    private static final String TEST_ITEM_ID = "test_sword";
    private static final String TEST_ITEM_NAME = "테스트 검";
    private static final int EQUIPMENT_MAX_DURABILITY = 20;

    /**
     * 드랍 골드는 인벤토리 상태와 무관하게 항상 캐릭터에 가산됨을 검증한다.
     *
     * @param gold 드랍 골드량 (1~10000)
     */
    @Property(tries = 100)
    void should_alwaysAddGold_when_anyInventoryState(
            @ForAll @LongRange(min = 1, max = 10_000) final long gold) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final ActionLog actionLog = new ActionLog(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepository = mock(CharacterSkillRepository.class);

        final InventoryService service = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository,
                new StatProgression(), actionLog, skillCatalogService, characterSkillRepository);

        final CharacterProgress progress = createProgress(0L);
        final DropResult drop = new DropResult(gold, List.of());

        service.acquire(progress, drop);

        assertThat(progress.getGold()).isEqualTo(gold);
    }

    /**
     * 용량 미달 시 장비 아이템이 인벤토리에 적재됨을 검증한다.
     *
     * @param currentCount 현재 인벤토리 아이템 수 (0~29)
     */
    @Property(tries = 100)
    void should_addItem_when_inventoryHasCapacity(
            @ForAll @IntRange(min = 0, max = 29) final int currentCount) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final ActionLog actionLog = new ActionLog(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepository = mock(CharacterSkillRepository.class);

        final InventoryService service = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository,
                new StatProgression(), actionLog, skillCatalogService, characterSkillRepository);

        when(ownedItemRepository.countByStorage(StorageKind.INVENTORY)).thenReturn((long) currentCount);
        when(itemCatalogService.byId(TEST_ITEM_ID)).thenReturn(Optional.of(
                new EquipmentItem(TEST_ITEM_ID, TEST_ITEM_NAME, ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD, List.of(), null, EQUIPMENT_MAX_DURABILITY)));

        final CharacterProgress progress = createProgress(100L);
        final DropResult drop = new DropResult(0L, List.of(new DroppedItem(TEST_ITEM_ID, 1)));

        service.acquire(progress, drop);

        verify(ownedItemRepository).save(any(OwnedItem.class));
    }

    /**
     * 용량 초과(30 이상) 시 장비 아이템이 소실되고 로그에 실패 메시지가 남음을 검증한다.
     *
     * @param currentCount 현재 인벤토리 아이템 수 (30~50)
     */
    @Property(tries = 100)
    void should_loseItem_when_inventoryAtCapacity(
            @ForAll @IntRange(min = 30, max = 50) final int currentCount) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final ActionLog actionLog = new ActionLog(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepository = mock(CharacterSkillRepository.class);

        final InventoryService service = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository,
                new StatProgression(), actionLog, skillCatalogService, characterSkillRepository);

        when(ownedItemRepository.countByStorage(StorageKind.INVENTORY)).thenReturn((long) currentCount);
        when(itemCatalogService.byId(TEST_ITEM_ID)).thenReturn(Optional.of(
                new EquipmentItem(TEST_ITEM_ID, TEST_ITEM_NAME, ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD, List.of(), null, EQUIPMENT_MAX_DURABILITY)));

        final CharacterProgress progress = createProgress(100L);
        final DropResult drop = new DropResult(0L, List.of(new DroppedItem(TEST_ITEM_ID, 1)));

        service.acquire(progress, drop);

        verify(ownedItemRepository, never()).save(any(OwnedItem.class));
        assertThat(actionLog.getEntries())
                .anyMatch(entry -> entry.message().equals(TEST_ITEM_NAME + " 획득 실패!"));
    }

    /**
     * 용량 초과로 아이템이 소실되어도 동일 드랍의 골드는 정상 처리됨을 검증한다.
     *
     * @param tuple (골드량, 현재 인벤토리 수) 쌍
     */
    @Property(tries = 100)
    void should_addGoldEvenWhenItemLost_when_capacityExceeded(
            @ForAll("goldAndFullInventory") final Tuple.Tuple2<Long, Integer> tuple) {

        final long gold = tuple.get1();
        final int currentCount = tuple.get2();

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final ActionLog actionLog = new ActionLog(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepository = mock(CharacterSkillRepository.class);

        final InventoryService service = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository,
                new StatProgression(), actionLog, skillCatalogService, characterSkillRepository);

        when(ownedItemRepository.countByStorage(StorageKind.INVENTORY)).thenReturn((long) currentCount);
        when(itemCatalogService.byId(TEST_ITEM_ID)).thenReturn(Optional.of(
                new EquipmentItem(TEST_ITEM_ID, TEST_ITEM_NAME, ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD, List.of(), null, EQUIPMENT_MAX_DURABILITY)));

        final CharacterProgress progress = createProgress(0L);
        final DropResult drop = new DropResult(gold, List.of(new DroppedItem(TEST_ITEM_ID, 1)));

        service.acquire(progress, drop);

        assertThat(progress.getGold()).isEqualTo(gold);
    }

    /**
     * 여러 아이템 드랍 시 하나가 소실되어도 나머지 아이템 처리는 계속됨을 검증한다.
     *
     * @param seed 임의 시드 (테스트 변동용)
     */
    @Property(tries = 100)
    void should_continueProcessing_when_oneItemLostDueToCapacity(
            @ForAll @IntRange(min = 0, max = 100) final int seed) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final ActionLog actionLog = new ActionLog(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepository = mock(CharacterSkillRepository.class);

        final InventoryService service = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository,
                new StatProgression(), actionLog, skillCatalogService, characterSkillRepository);

        final String firstItemId = "item_a";
        final String secondItemId = "item_b";
        final String firstItemName = "아이템A";
        final String secondItemName = "아이템B";

        // 첫 아이템 적재 시 용량 가득 → 실패, 두 번째 아이템도 용량 가득
        when(ownedItemRepository.countByStorage(StorageKind.INVENTORY)).thenReturn(30L);
        when(itemCatalogService.byId(firstItemId)).thenReturn(Optional.of(
                new EquipmentItem(firstItemId, firstItemName, ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD, List.of(), null, EQUIPMENT_MAX_DURABILITY)));
        when(itemCatalogService.byId(secondItemId)).thenReturn(Optional.of(
                new EquipmentItem(secondItemId, secondItemName, ItemType.ARMOR,
                        EquipmentKind.ARMOR_BODY, List.of(), null, EQUIPMENT_MAX_DURABILITY)));

        final CharacterProgress progress = createProgress(0L);
        final long dropGold = 50L + seed;
        final DropResult drop = new DropResult(dropGold, List.of(
                new DroppedItem(firstItemId, 1),
                new DroppedItem(secondItemId, 1)));

        service.acquire(progress, drop);

        // 골드는 정상 처리
        assertThat(progress.getGold()).isEqualTo(dropGold);
        // 두 아이템 모두 소실 로그
        assertThat(actionLog.getEntries())
                .anyMatch(entry -> entry.message().equals(firstItemName + " 획득 실패!"));
        assertThat(actionLog.getEntries())
                .anyMatch(entry -> entry.message().equals(secondItemName + " 획득 실패!"));
    }

    // ─── Arbitrary Providers ────────────────────────────────────────────────

    /**
     * 골드량(1~10000)과 용량 초과 인벤토리 수(30~50)의 조합을 생성한다.
     *
     * @return (gold, inventoryCount) 튜플의 Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Long, Integer>> goldAndFullInventory() {
        return Arbitraries.longs().between(1L, 10_000L)
                .flatMap(gold -> Arbitraries.integers().between(30, 50)
                        .map(count -> Tuple.of(gold, count)));
    }

    // ─── Helper ─────────────────────────────────────────────────────────────

    /**
     * 지정 골드를 가진 테스트용 CharacterProgress를 생성한다.
     *
     * @param gold 초기 골드
     * @return CharacterProgress 인스턴스
     */
    private CharacterProgress createProgress(final long gold) {
        return new CharacterProgress(
                "테스트", 1, 1, 0L, TalentType.MELEE, null,
                100, 100, 100, "tir-chonaill", 0, gold);
    }
}
