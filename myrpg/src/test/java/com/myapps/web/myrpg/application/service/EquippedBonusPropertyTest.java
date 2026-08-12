package com.myapps.web.myrpg.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.domain.model.BonusKind;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 장비 보너스 합산 STAT/VITAL 분기 프로퍼티 테스트.
 *
 * <p>임의의 보유 아이템 집합에 대해, {@code equippedBonus}는
 * {@code storage=INVENTORY && equipped} 장비의 {@link EquipBonus}만 합산하며,
 * STAT 계열(STR/DEX/INT/CRITICAL/DEF)은 {@link Stats}에,
 * VITAL 계열(HP/MP/STAMINA)은 {@link VitalMax}에 가산하고 서로 섞지 않는다.
 * 미장착·은행·포션은 기여하지 않는다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 12: 장비 보너스 합산 STAT/VITAL 분기
 *
 * <p><b>Validates: Requirements 10.1, 10.2, 10.4</b>
 */
class EquippedBonusPropertyTest {

    private static final int MAX_BONUS_AMOUNT = 50;

    // Feature: 006-gold-item-inventory, Property 12: 장비 보너스 합산 STAT/VITAL 분기

    /**
     * INVENTORY+equipped 장비의 STAT 보너스만 Stats에 합산됨을 검증한다.
     *
     * @param scenario 임의 생성된 장비 시나리오
     */
    @Property(tries = 100)
    void should_sumStatBonusesFromEquippedInventoryOnly(
            @ForAll("equipScenario") final EquipScenario scenario) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final StatProgression statProgression = new StatProgression();

        final InventoryService inventoryService = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository, statProgression,
                mock(com.myapps.web.myrpg.domain.model.ActionLog.class),
                mock(com.myapps.web.myrpg.application.service.SkillCatalogService.class),
                mock(com.myapps.web.myrpg.domain.repository.CharacterSkillRepository.class));

        // 장착 중 장비만 리포지토리가 반환
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(scenario.equippedInventoryItems());

        // 카탈로그 조회 설정
        for (final ItemEntry entry : scenario.allEntries()) {
            when(itemCatalogService.byId(entry.ownedItem().getItemId()))
                    .thenReturn(Optional.ofNullable(entry.catalogItem()));
        }

        final EquippedBonusResult result = inventoryService.equippedBonus();

        // 기대 STAT 합산: equipped + INVENTORY 장비의 STAT 보너스만
        Stats expectedStats = Stats.ZERO;
        for (final ItemEntry entry : scenario.equippedInventoryEntries()) {
            if (entry.catalogItem() instanceof EquipmentItem equipItem) {
                for (final EquipBonus bonus : equipItem.bonuses()) {
                    if (bonus.target().kind() == BonusKind.STAT) {
                        expectedStats = applyStatDelta(expectedStats, bonus.target(), bonus.amount());
                    }
                }
            }
        }

        assertThat(result.statBonus()).isEqualTo(expectedStats);
    }

    /**
     * INVENTORY+equipped 장비의 VITAL 보너스만 VitalMax에 합산됨을 검증한다.
     *
     * @param scenario 임의 생성된 장비 시나리오
     */
    @Property(tries = 100)
    void should_sumVitalBonusesFromEquippedInventoryOnly(
            @ForAll("equipScenario") final EquipScenario scenario) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final StatProgression statProgression = new StatProgression();

        final InventoryService inventoryService = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository, statProgression,
                mock(com.myapps.web.myrpg.domain.model.ActionLog.class),
                mock(com.myapps.web.myrpg.application.service.SkillCatalogService.class),
                mock(com.myapps.web.myrpg.domain.repository.CharacterSkillRepository.class));

        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(scenario.equippedInventoryItems());

        for (final ItemEntry entry : scenario.allEntries()) {
            when(itemCatalogService.byId(entry.ownedItem().getItemId()))
                    .thenReturn(Optional.ofNullable(entry.catalogItem()));
        }

        final EquippedBonusResult result = inventoryService.equippedBonus();

        // 기대 VITAL 합산
        VitalMax expectedVital = new VitalMax(0, 0, 0);
        for (final ItemEntry entry : scenario.equippedInventoryEntries()) {
            if (entry.catalogItem() instanceof EquipmentItem equipItem) {
                for (final EquipBonus bonus : equipItem.bonuses()) {
                    if (bonus.target().kind() == BonusKind.VITAL) {
                        expectedVital = applyVitalDelta(expectedVital, bonus.target(), bonus.amount());
                    }
                }
            }
        }

        assertThat(result.vitalBonus()).isEqualTo(expectedVital);
    }

    /**
     * 미장착/은행/포션 아이템은 보너스에 기여하지 않음을 검증한다.
     *
     * @param scenario 임의 생성된 장비 시나리오
     */
    @Property(tries = 100)
    void should_excludeUnequippedBankAndPotionItems(
            @ForAll("nonContributingScenario") final EquipScenario scenario) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final StatProgression statProgression = new StatProgression();

        final InventoryService inventoryService = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository, statProgression,
                mock(com.myapps.web.myrpg.domain.model.ActionLog.class),
                mock(com.myapps.web.myrpg.application.service.SkillCatalogService.class),
                mock(com.myapps.web.myrpg.domain.repository.CharacterSkillRepository.class));

        // 장착 장비 없음(미장착·은행·포션만 존재)
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of());

        final EquippedBonusResult result = inventoryService.equippedBonus();

        assertThat(result.statBonus()).isEqualTo(Stats.ZERO);
        assertThat(result.vitalBonus()).isEqualTo(new VitalMax(0, 0, 0));
    }

    // ─── Providers ──────────────────────────────────────────────────────────

    /**
     * 장착 INVENTORY 장비와 비기여 아이템을 혼합한 시나리오를 생성한다.
     *
     * @return EquipScenario Arbitrary
     */
    @Provide
    Arbitrary<EquipScenario> equipScenario() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 4),
                bonusListArbitrary().list().ofMinSize(1).ofMaxSize(4),
                equipmentKindArbitrary().list().ofMinSize(1).ofMaxSize(4)
        ).as((count, bonusLists, kinds) -> {
            final int size = Math.min(count, Math.min(bonusLists.size(), kinds.size()));
            final List<ItemEntry> equipped = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                final String itemId = "eq_" + i;
                final EquipmentKind kind = kinds.get(i);
                final List<EquipBonus> bonuses = bonusLists.get(i);
                final ItemType itemType = (kind == EquipmentKind.SHIELD || kind == EquipmentKind.ARMOR_BODY)
                        ? ItemType.ARMOR : ItemType.WEAPON;
                final EquipmentItem catalogItem = new EquipmentItem(
                        itemId, "장비_" + itemId, itemType, kind, bonuses, null, 20);
                final OwnedItem ownedItem = new OwnedItem(itemId, 1, StorageKind.INVENTORY, true, 20.0);
                equipped.add(new ItemEntry(ownedItem, catalogItem));
            }
            return new EquipScenario(equipped, List.of());
        });
    }

    /**
     * 비기여 아이템(미장착·은행·포션)만으로 구성된 시나리오를 생성한다.
     *
     * @return EquipScenario Arbitrary
     */
    @Provide
    Arbitrary<EquipScenario> nonContributingScenario() {
        return Arbitraries.integers().between(1, 5).map(count -> {
            final List<ItemEntry> entries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                final String itemId = "nc_" + i;
                final EquipmentItem catalogItem = new EquipmentItem(
                        itemId, "비기여_" + itemId, ItemType.WEAPON,
                        EquipmentKind.ONE_HANDED_SWORD,
                        List.of(new EquipBonus(BonusTarget.STR, 5 + i)),
                        null, 20);
                final OwnedItem ownedItem = new OwnedItem(
                        itemId, 1, StorageKind.BANK, false, 20.0);
                entries.add(new ItemEntry(ownedItem, catalogItem));
            }
            return new EquipScenario(List.of(), entries);
        });
    }

    private Arbitrary<EquipmentKind> equipmentKindArbitrary() {
        return Arbitraries.of(EquipmentKind.values());
    }

    private Arbitrary<List<EquipBonus>> bonusListArbitrary() {
        return bonusArbitrary().list().ofMinSize(1).ofMaxSize(3);
    }

    private Arbitrary<EquipBonus> bonusArbitrary() {
        return Combinators.combine(
                Arbitraries.of(BonusTarget.values()),
                Arbitraries.integers().between(1, MAX_BONUS_AMOUNT)
        ).as(EquipBonus::new);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

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

    // ─── Inner types ────────────────────────────────────────────────────────

    /**
     * 보유 아이템과 카탈로그 매핑 쌍.
     */
    private record ItemEntry(OwnedItem ownedItem, Item catalogItem) {
    }

    /**
     * 장착+INVENTORY 아이템과 비기여 아이템을 조합한 시나리오.
     *
     * <p>ID 충돌을 방지하기 위해 생성 후 모든 아이템의 ID에 인덱스 접두사를 부여한다.
     */
    private record EquipScenario(
            List<ItemEntry> equippedInventoryEntries,
            List<ItemEntry> nonContributingEntries) {

        /**
         * 장착 INVENTORY 아이템의 OwnedItem 목록을 반환한다.
         *
         * @return 장착 아이템 리스트
         */
        List<OwnedItem> equippedInventoryItems() {
            return equippedInventoryEntries.stream()
                    .map(ItemEntry::ownedItem)
                    .toList();
        }

        /**
         * 전체 엔트리 목록을 반환한다.
         *
         * @return 모든 ItemEntry 합산 리스트
         */
        List<ItemEntry> allEntries() {
            final List<ItemEntry> all = new ArrayList<>(equippedInventoryEntries);
            all.addAll(nonContributingEntries);
            return all;
        }
    }
}
