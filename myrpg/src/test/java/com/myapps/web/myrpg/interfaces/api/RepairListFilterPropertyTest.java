package com.myapps.web.myrpg.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.RepairItemView;
import com.myapps.web.myrpg.application.dto.RepairView;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.application.service.ItemCatalogService;
import com.myapps.web.myrpg.application.service.ShopService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.PotionItem;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.springframework.ui.ExtendedModelMap;

/**
 * 수리 목록 필터링 조건 일치 프로퍼티 테스트.
 *
 * <p>임의의 보유 아이템 목록에 대해, 수리 목록({@link RepairView#repairItems()})에 포함되는 아이템은 오직 {@link
 * EquipmentItem}이면서 {@code ceil(currentDurability) < maxDurability}인 아이템뿐이고, 포션 및 풀내구도 장비({@code
 * ceil >= max})는 항상 제외됨을 검증한다.
 *
 * <p>Feature: 010-npc-actions-shop-repair-heal, Property 6: 수리 목록 필터링 조건 일치 (ceil(current) < max)
 *
 * <p><b>Validates: Requirements 8.1, 8.2, 8.3, 8.5, 8.6</b>
 */
// Feature: 010-npc-actions-shop-repair-heal, Property 6: 수리 목록 필터링 조건 일치 (ceil(current) < max)
class RepairListFilterPropertyTest {

    /**
     * 아이템 카탈로그 항목과 그에 대응하는 보유 아이템의 현재 내구도 쌍.
     *
     * @param item 카탈로그 아이템 (EquipmentItem 또는 PotionItem)
     * @param durability 해당 보유 아이템의 현재 내구도
     */
    record OwnedSeed(Item item, double durability) {}

    /**
     * 임의의 보유 아이템 목록(0~8개)에 대해 수리 팝업 필터링 결과가 {@code EquipmentItem && ceil(current) < max} 조건과 정확히
     * 일치하는지 검증한다.
     *
     * @param seeds 임의 아이템·내구도 쌍 목록
     */
    @Property(tries = 100)
    void should_includeOnlyRepairableEquipment(@ForAll("ownedSeeds") final List<OwnedSeed> seeds) {
        final Fixture fixture = newFixture(seeds);
        final ExtendedModelMap model = new ExtendedModelMap();
        fixture.controller().repairPopup(model);

        final RepairView view = (RepairView) model.get("repair");
        final List<String> actualNames =
                view.repairItems().stream().map(RepairItemView::name).toList();

        final List<String> expectedNames =
                seeds.stream()
                        .filter(
                                seed ->
                                        seed.item() instanceof EquipmentItem equip
                                                && Math.ceil(seed.durability())
                                                        < equip.maxDurability())
                        .map(seed -> seed.item().name())
                        .toList();

        assertThat(actualNames).containsExactlyInAnyOrderElementsOf(expectedNames);
    }

    // ─── Arbitrary Providers ────────────────────────────────────────────────

    /**
     * 임의 아이템·내구도 쌍 목록(0~8개)을 생성한다.
     *
     * @return OwnedSeed 목록 Arbitrary
     */
    @Provide
    Arbitrary<List<OwnedSeed>> ownedSeeds() {
        return ownedSeed().list().ofMinSize(0).ofMaxSize(8);
    }

    /**
     * 임의 아이템(장비/포션)과 임의 내구도(0~200) 쌍을 생성한다.
     *
     * @return OwnedSeed Arbitrary
     */
    @Provide
    Arbitrary<OwnedSeed> ownedSeed() {
        final Arbitrary<Item> equipment =
                Arbitraries.integers()
                        .between(1, 100)
                        .map(
                                maxDurability ->
                                        new EquipmentItem(
                                                "eq-" + maxDurability,
                                                "장비" + maxDurability,
                                                ItemType.WEAPON,
                                                EquipmentKind.ONE_HANDED_SWORD,
                                                List.of(),
                                                null,
                                                maxDurability));
        final Arbitrary<Item> potion =
                Arbitraries.integers()
                        .between(1, 100)
                        .map(
                                healHp ->
                                        new PotionItem(
                                                "pot-" + healHp, "포션" + healHp, healHp, null));
        return Arbitraries.oneOf(equipment, potion)
                .flatMap(
                        item ->
                                Arbitraries.doubles()
                                        .between(0.0, 200.0)
                                        .map(durability -> new OwnedSeed(item, durability)));
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    /**
     * 테스트 픽스처: 수리 컨트롤러와 프록시 의존성 구성.
     *
     * @param controller 수리 컨트롤러 (실제 구현)
     */
    record Fixture(RepairController controller) {}

    private Fixture newFixture(final List<OwnedSeed> seeds) {
        final Map<String, Item> catalog = new HashMap<>();
        final List<OwnedItem> ownedItems = new ArrayList<>();
        long id = 1L;
        for (final OwnedSeed seed : seeds) {
            catalog.put(seed.item().id(), seed.item());
            final OwnedItem owned =
                    new OwnedItem(
                            seed.item().id(), 1, StorageKind.INVENTORY, false, seed.durability());
            IdTestHelper.setId(owned, id++);
            ownedItems.add(owned);
        }
        // 카탈로그에 존재하지 않는 보유 아이템(orphan)은 항상 수리 목록에서 제외되어야 한다.
        final OwnedItem orphan = new OwnedItem("orphan", 1, StorageKind.INVENTORY, false, 0.0);
        IdTestHelper.setId(orphan, id);
        ownedItems.add(orphan);

        final CharacterProgress progress = CharacterProgress.createDefault();
        progress.gainGold(1_000);

        final CharacterService characterService = mock(CharacterService.class);
        when(characterService.loadOrCreateDefault()).thenReturn(progress);

        final OwnedItemRepository repository = mock(OwnedItemRepository.class);
        when(repository.findByStorageOrderById(StorageKind.INVENTORY)).thenReturn(ownedItems);

        final ItemCatalogService catalogService = mock(ItemCatalogService.class);
        when(catalogService.byId(anyString()))
                .thenAnswer(
                        invocation -> {
                            final String itemId = invocation.getArgument(0);
                            return Optional.ofNullable(catalog.get(itemId));
                        });

        final RepairController controller =
                new RepairController(
                        characterService,
                        mock(ShopService.class),
                        mock(InventoryService.class),
                        catalogService,
                        repository,
                        new Random(0));

        return new Fixture(controller);
    }
}
