package com.myapps.web.myrpg.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.springframework.ui.ExtendedModelMap;

/**
 * 수리 시도 시 골드 소모 및 실패 비환불 불변 프로퍼티 테스트.
 *
 * <p>임의의 수리 시도에 대해, 수리 성공(95%) 시 내구도가 +1 되고 골드가 차감되며, 수리 실패(5%) 시에도 차감된 수리비는 환불되지 않고 내구도는 유지됨을
 * 검증한다.
 *
 * <p>Feature: 010-npc-actions-shop-repair-heal, Property 8: 수리 시도 시 골드 소모 및 실패 비환불 불변
 *
 * <p><b>Validates: Requirements 9.4, 9.5, 9.6</b>
 */
// Feature: 010-npc-actions-shop-repair-heal, Property 8: 수리 시도 시 골드 소모 및 실패 비환불 불변
class RepairExecutionPropertyTest {

    private static final int MAX_DURABILITY = 20;
    private static final long FIXED_EPOCH_SECOND = 1_700_000_000L;
    private static final long OWNED_ITEM_ID = 1L;

    /**
     * 수리 성공(95% 판정 성공) 시 내구도가 정확히 +1 되고 골드가 차감되는지 검증한다.
     *
     * @param bonusAmount 임의 보너스 금액 (판매가 결정)
     */
    @Property(tries = 100)
    void should_repairSuccess_increaseDurabilityAndSpendGold(
            @ForAll("bonusAmount") final int bonusAmount) {
        final EquipmentItem equip = newEquipment(bonusAmount);
        final double initialDurability = 5.0;
        final OwnedItem owned =
                new OwnedItem(equip.id(), 1, StorageKind.INVENTORY, false, initialDurability);
        final long initialGold = 10_000L;

        final Fixture fixture = newFixture(equip, owned, initialGold, new AlwaysSuccessRandom());
        final long sellValue = fixture.shopService().sellValueOf(owned);

        final ExtendedModelMap model = new ExtendedModelMap();
        fixture.controller().repair(OWNED_ITEM_ID, model);

        final double expectedDurability = Math.min(equip.maxDurability(), initialDurability + 1.0);
        assertThat(owned.getCurrentDurability()).isEqualTo(expectedDurability);
        assertThat(fixture.progress().getGold()).isEqualTo(initialGold - sellValue);
    }

    /**
     * 수리 실패(5% 판정 실패) 시에도 차감된 수리비는 환불되지 않고 내구도는 유지되는지 검증한다.
     *
     * @param bonusAmount 임의 보너스 금액 (판매가 결정)
     */
    @Property(tries = 100)
    void should_repairFailure_keepDurabilityAndNoRefund(
            @ForAll("bonusAmount") final int bonusAmount) {
        final EquipmentItem equip = newEquipment(bonusAmount);
        final double initialDurability = 5.0;
        final OwnedItem owned =
                new OwnedItem(equip.id(), 1, StorageKind.INVENTORY, false, initialDurability);
        final long initialGold = 10_000L;

        final Fixture fixture = newFixture(equip, owned, initialGold, new AlwaysFailureRandom());
        final long sellValue = fixture.shopService().sellValueOf(owned);

        final ExtendedModelMap model = new ExtendedModelMap();
        fixture.controller().repair(OWNED_ITEM_ID, model);

        assertThat(owned.getCurrentDurability()).isEqualTo(initialDurability);
        assertThat(fixture.progress().getGold()).isEqualTo(initialGold - sellValue);
    }

    // ─── Arbitrary Providers ────────────────────────────────────────────────

    /**
     * 임의의 보너스 금액(1~100)을 생성한다.
     *
     * @return 보너스 금액 Arbitrary
     */
    @Provide
    Arbitrary<Integer> bonusAmount() {
        return Arbitraries.integers().between(1, 100);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    /** 항상 수리 성공(95% 판정 미만)을 반환하는 Random. */
    private static class AlwaysSuccessRandom extends Random {
        @Override
        public int nextInt(final int bound) {
            return 0;
        }
    }

    /** 항상 수리 실패(95% 판정 이상)를 반환하는 Random. */
    private static class AlwaysFailureRandom extends Random {
        @Override
        public int nextInt(final int bound) {
            return bound - 1;
        }
    }

    /**
     * 테스트 픽스처: 수리 컨트롤러, 상점 서비스, 캐릭터 진행상황.
     *
     * @param controller 수리 컨트롤러
     * @param shopService 상점 서비스 (판매가 계산)
     * @param progress 캐릭터 진행상황 (골드 검증용)
     */
    record Fixture(
            RepairController controller, ShopService shopService, CharacterProgress progress) {}

    private EquipmentItem newEquipment(final int bonusAmount) {
        return new EquipmentItem(
                "test-sword",
                "테스트검",
                ItemType.WEAPON,
                EquipmentKind.ONE_HANDED_SWORD,
                List.of(new EquipBonus(BonusTarget.STR, bonusAmount)),
                null,
                MAX_DURABILITY);
    }

    private Fixture newFixture(
            final EquipmentItem equip,
            final OwnedItem owned,
            final long initialGold,
            final Random random) {
        IdTestHelper.setId(owned, OWNED_ITEM_ID);
        final CharacterProgress progress = CharacterProgress.createDefault();
        progress.gainGold(initialGold);

        final CharacterService characterService = mock(CharacterService.class);
        when(characterService.loadOrCreateDefault()).thenReturn(progress);

        final OwnedItemRepository repository = mock(OwnedItemRepository.class);
        when(repository.findById(OWNED_ITEM_ID)).thenReturn(Optional.of(owned));

        final ItemCatalogService catalogService = mock(ItemCatalogService.class);
        when(catalogService.byId(anyString())).thenReturn(Optional.of(equip));

        final ShopService shopService =
                new ShopService(
                        catalogService,
                        mock(NpcService.class),
                        repository,
                        mock(InventoryService.class),
                        characterService,
                        fixedAction());

        final RepairController controller =
                new RepairController(
                        characterService,
                        shopService,
                        mock(InventoryService.class),
                        catalogService,
                        repository,
                        random);

        return new Fixture(controller, shopService, progress);
    }

    private ActionLog fixedAction() {
        return new ActionLog(
                Clock.fixed(Instant.ofEpochSecond(FIXED_EPOCH_SECOND), ZoneId.systemDefault()));
    }
}
