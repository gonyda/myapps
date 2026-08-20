package com.myapps.web.myrpg.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.RepairView;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.application.service.ItemCatalogService;
import com.myapps.web.myrpg.application.service.NpcService;
import com.myapps.web.myrpg.application.service.ShopService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
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
 * 수리 비용과 1포인트 판매가 동치성 프로퍼티 테스트.
 *
 * <p>수리 가능한 장비(내구도가 닳은 장비)에 대해, 수리 목록에 노출되는 {@code repairCost}는 해당 장비의 {@code
 * ShopService.sellValueOf}와 정확히 일치함을 검증한다.
 *
 * <p>Feature: 010-npc-actions-shop-repair-heal, Property 7: 수리 비용과 1포인트 판매가 동치성
 *
 * <p><b>Validates: Requirements 9.3</b>
 */
// Feature: 010-npc-actions-shop-repair-heal, Property 7: 수리 비용과 1포인트 판매가 동치성
class RepairCostEquivalencePropertyTest {

    private static final int MAX_DURABILITY = 20;

    /**
     * 임의의 판매가 계산이 가능한 장비(가중치 합산 또는 buyPrice 경로)에 대해, 수리 목록의 {@code repairCost}가 실제 {@code
     * sellValueOf(ownedItem)}와 일치하는지 검증한다.
     *
     * @param equip 임의 장비 아이템
     */
    @Property(tries = 100)
    void should_makeRepairCostEqualToSellValue(
            @ForAll("repairableEquip") final EquipmentItem equip) {
        final OwnedItem owned =
                new OwnedItem(equip.id(), 1, StorageKind.INVENTORY, false, MAX_DURABILITY - 5);
        final Fixture fixture = newFixture(equip, owned);

        final ExtendedModelMap model = new ExtendedModelMap();
        fixture.controller().repairPopup(model);

        final RepairView view = (RepairView) model.get("repair");
        assertThat(view.repairItems()).hasSize(1);

        final long expectedSellValue = fixture.shopService().sellValueOf(owned);
        final long actualRepairCost = view.repairItems().get(0).repairCost();
        assertThat(actualRepairCost).isEqualTo(expectedSellValue);
    }

    /**
     * 풀내구도 장비는 수리 목록에서 제외되므로 repairCost 동치성 대상이 아님을 검증한다.
     *
     * @param equip 임의 장비 아이템
     */
    @Property(tries = 100)
    void should_excludeFullDurabilityEquip(@ForAll("repairableEquip") final EquipmentItem equip) {
        final OwnedItem ownedFull =
                new OwnedItem(equip.id(), 1, StorageKind.INVENTORY, false, equip.maxDurability());
        final Fixture fixture = newFixture(equip, ownedFull);

        final ExtendedModelMap model = new ExtendedModelMap();
        fixture.controller().repairPopup(model);

        final RepairView view = (RepairView) model.get("repair");
        assertThat(view.repairItems()).isEmpty();
    }

    // ─── Arbitrary Providers ────────────────────────────────────────────────

    /**
     * 수리 가능한 장비(보너스 1개 또는 buyPrice 존재, 내구도 닳음)를 생성한다.
     *
     * @return EquipmentItem Arbitrary
     */
    @Provide
    Arbitrary<EquipmentItem> repairableEquip() {
        final Arbitrary<EquipmentItem> bonusEquip =
                Arbitraries.integers()
                        .between(1, 100)
                        .map(
                                amount ->
                                        new EquipmentItem(
                                                "bonus-sword-" + amount,
                                                "보너스검" + amount,
                                                ItemType.WEAPON,
                                                EquipmentKind.ONE_HANDED_SWORD,
                                                List.of(new EquipBonus(BonusTarget.STR, amount)),
                                                null,
                                                MAX_DURABILITY));
        final Arbitrary<EquipmentItem> buyPriceEquip =
                Arbitraries.integers()
                        .between(1, 100_000)
                        .map(
                                price ->
                                        new EquipmentItem(
                                                "shop-sword-" + price,
                                                "상점검" + price,
                                                ItemType.WEAPON,
                                                EquipmentKind.ONE_HANDED_SWORD,
                                                List.of(),
                                                price,
                                                MAX_DURABILITY));
        return Arbitraries.oneOf(bonusEquip, buyPriceEquip);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    /**
     * 테스트 픽스처: 수리 컨트롤러, 상점 서비스, 의존성 구성.
     *
     * @param controller 수리 컨트롤러
     * @param shopService 상점 서비스 (실제 판매가 계산)
     */
    record Fixture(RepairController controller, ShopService shopService) {}

    private Fixture newFixture(final Item catalogItem, final OwnedItem owned) {
        final List<OwnedItem> ownedItems = new ArrayList<>();
        IdTestHelper.setId(owned, 1L);
        ownedItems.add(owned);

        final CharacterProgress progress = CharacterProgress.createDefault();
        progress.gainGold(10_000);

        final CharacterService characterService = mock(CharacterService.class);
        when(characterService.loadOrCreateDefault()).thenReturn(progress);

        final OwnedItemRepository repository = mock(OwnedItemRepository.class);
        when(repository.findByStorageOrderById(StorageKind.INVENTORY)).thenReturn(ownedItems);

        final ItemCatalogService catalogService = mock(ItemCatalogService.class);
        when(catalogService.byId(anyString())).thenAnswer(invocation -> Optional.of(catalogItem));

        final Map<String, Item> itemCatalog = new HashMap<>();
        itemCatalog.put(catalogItem.id(), catalogItem);
        when(catalogService.byId(catalogItem.id())).thenReturn(Optional.of(catalogItem));

        final ShopService shopService =
                new ShopService(
                        catalogService,
                        mock(NpcService.class),
                        repository,
                        mock(InventoryService.class),
                        characterService,
                        mock(ActionLog.class));

        final RepairController controller =
                new RepairController(
                        characterService,
                        shopService,
                        mock(InventoryService.class),
                        catalogService,
                        repository,
                        mock(ActionLog.class),
                        new Random(0));

        return new Fixture(controller, shopService);
    }
}
