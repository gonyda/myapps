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

import com.myapps.web.myrpg.application.exception.InsufficientGoldException;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.NpcLines;
import com.myapps.web.myrpg.domain.model.NpcType;
import com.myapps.web.myrpg.domain.model.TalentType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 상점 구매 유효성 검증 및 골드/인벤토리 상태 보존 프로퍼티 테스트.
 *
 * <p>NPC shopItems에 없는 아이템, buyPrice가 없는 아이템, 골드 부족 상태의
 * 구매 시도는 모두 거부되고 골드와 인벤토리 상태가 불변임을 검증한다.
 * 유효한 구매는 buyPrice만큼 골드가 차감되고 인벤토리 획득이 1회 호출됨을 검증한다.
 *
 * <p>Feature: 010-npc-actions-shop-repair-heal, Property 3: 상점 구매 유효성 검증 및 골드/인벤토리 상태 보존
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.8, 4.1, 4.2, 4.4, 5.1, 5.3, 5.5, 14.5, 15.2</b>
 */
// Feature: 010-npc-actions-shop-repair-heal, Property 3: 상점 구매 유효성 검증 및 골드/인벤토리 상태 보존
class ShopServiceBuyValidationPropertyTest {

    private static final long FIXED_EPOCH_SECOND = 1_700_000_000L;
    private static final String NPC_ID = "shopkeeper";
    private static final String SELLING_ITEM_ID = "sell_item";
    private static final String NON_SELLING_ITEM_ID = "non_sell_item";
    private static final int MAX_DURABILITY = 20;

    /**
     * NPC shopItems에 포함되지 않은 아이템 구매 시도는 항상 거부되고,
     * 골드와 인벤토리 상태가 불변임을 검증한다.
     *
     * @param gold  보유 골드 (0~10000)
     * @param price 구매 시도 아이템의 buyPrice (1~1000)
     */
    @Property(tries = 100)
    void should_rejectBuy_whenNpcDoesNotSellItem(
            @ForAll @LongRange(min = 0, max = 10_000) final long gold,
            @ForAll @IntRange(min = 1, max = 1_000) final int price) {
        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final InventoryService inventoryService = mock(InventoryService.class);
        when(catalog.byId(NON_SELLING_ITEM_ID)).thenReturn(Optional.of(
                createEquip(NON_SELLING_ITEM_ID, "판매품 아님", price)));

        final NpcService npcService = mock(NpcService.class);
        when(npcService.byId(NPC_ID)).thenReturn(Optional.of(npc(List.of(SELLING_ITEM_ID))));

        final ShopService service = newService(catalog, npcService, inventoryService);
        final CharacterProgress progress = createProgress(gold);

        assertThatThrownBy(() -> service.buy(progress, NPC_ID, NON_SELLING_ITEM_ID))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(progress.getGold()).isEqualTo(gold);
        verify(inventoryService, never()).acquireItem(any(String.class), anyInt());
    }

    /**
     * buyPrice가 없는 아이템 구매 시도는 항상 거부되고,
     * 골드와 인벤토리 상태가 불변임을 검증한다.
     *
     * @param gold 보유 골드 (0~10000)
     */
    @Property(tries = 100)
    void should_rejectBuy_whenItemHasNoBuyPrice(
            @ForAll @LongRange(min = 0, max = 10_000) final long gold) {
        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final InventoryService inventoryService = mock(InventoryService.class);
        when(catalog.byId(SELLING_ITEM_ID)).thenReturn(Optional.of(
                createEquip(SELLING_ITEM_ID, "드랍 전용", null)));

        final NpcService npcService = mock(NpcService.class);
        when(npcService.byId(NPC_ID)).thenReturn(Optional.of(npc(List.of(SELLING_ITEM_ID))));

        final ShopService service = newService(catalog, npcService, inventoryService);
        final CharacterProgress progress = createProgress(gold);

        assertThatThrownBy(() -> service.buy(progress, NPC_ID, SELLING_ITEM_ID))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(progress.getGold()).isEqualTo(gold);
        verify(inventoryService, never()).acquireItem(any(String.class), anyInt());
    }

    /**
     * 골드 부족(gold < price) 상태의 구매 시도는 항상 거부되고,
     * 골드와 인벤토리 상태가 불변임을 검증한다.
     *
     * @param tuple (보유 골드, buyPrice) 쌍 (gold < price 보장)
     */
    @Property(tries = 100)
    void should_rejectBuy_whenGoldInsufficient(
            @ForAll("goldLessThanPrice") final Tuple.Tuple2<Long, Integer> tuple) {
        final long gold = tuple.get1();
        final int price = tuple.get2();

        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final InventoryService inventoryService = mock(InventoryService.class);
        when(catalog.byId(SELLING_ITEM_ID)).thenReturn(Optional.of(
                createEquip(SELLING_ITEM_ID, "장비", price)));

        final NpcService npcService = mock(NpcService.class);
        when(npcService.byId(NPC_ID)).thenReturn(Optional.of(npc(List.of(SELLING_ITEM_ID))));

        final ShopService service = newService(catalog, npcService, inventoryService);
        final CharacterProgress progress = createProgress(gold);

        assertThatThrownBy(() -> service.buy(progress, NPC_ID, SELLING_ITEM_ID))
                .isInstanceOf(InsufficientGoldException.class);

        assertThat(progress.getGold()).isEqualTo(gold);
        verify(inventoryService, never()).acquireItem(any(String.class), anyInt());
    }

    /**
     * 골드가 충분한 유효한 구매는 buyPrice만큼 골드가 차감되고
     * 인벤토리 획득이 정확히 1회 호출됨을 검증한다.
     *
     * @param tuple (보유 골드, buyPrice) 쌍 (골드 ≥ price 보장)
     */
    @Property(tries = 100)
    void should_buySuccessfully_whenGoldEnough(
            @ForAll("goldAtLeastPrice") final Tuple.Tuple2<Long, Integer> tuple) {
        final long gold = tuple.get1();
        final int price = tuple.get2();

        final ItemCatalogService catalog = mock(ItemCatalogService.class);
        final InventoryService inventoryService = mock(InventoryService.class);
        when(catalog.byId(SELLING_ITEM_ID)).thenReturn(Optional.of(
                createEquip(SELLING_ITEM_ID, "장비", price)));

        final NpcService npcService = mock(NpcService.class);
        when(npcService.byId(NPC_ID)).thenReturn(Optional.of(npc(List.of(SELLING_ITEM_ID))));

        final ShopService service = newService(catalog, npcService, inventoryService);
        final CharacterProgress progress = createProgress(gold);

        service.buy(progress, NPC_ID, SELLING_ITEM_ID);

        assertThat(progress.getGold()).isEqualTo(gold - price);
        verify(inventoryService).acquireItem(SELLING_ITEM_ID, 1);
    }

    // ─── Arbitrary Providers ────────────────────────────────────────────────

    /**
     * gold < price를 보장하는 (gold, price) 쌍을 생성한다.
     *
     * @return (gold, price) Tuple Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Long, Integer>> goldLessThanPrice() {
        return Arbitraries.integers().between(1, 1_000)
                .flatMap(price -> Arbitraries.longs().between(0L, price - 1L)
                        .map(gold -> Tuple.of(gold, price)));
    }

    /**
     * gold ≥ price를 보장하는 (gold, price) 쌍을 생성한다.
     *
     * @return (gold, price) Tuple Arbitrary
     */
    @Provide
    Arbitrary<Tuple.Tuple2<Long, Integer>> goldAtLeastPrice() {
        return Arbitraries.integers().between(1, 1_000)
                .flatMap(price -> Arbitraries.longs().between(price, price + 10_000L)
                        .map(gold -> Tuple.of(gold, price)));
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    /**
     * NPC를 생성한다.
     *
     * @param shopItems 판매 품목 목록
     * @return Npc 인스턴스
     */
    private Npc npc(final List<String> shopItems) {
        return new Npc(NPC_ID, "상점 주인", NpcType.BLACKSMITH, "tir-chonaill",
                "상점 주인", new NpcLines(List.of("어서 오세요."), null), shopItems);
    }

    /**
     * 모의 의존성으로 ShopService를 생성한다.
     *
     * @param catalog         모의 ItemCatalogService
     * @param npcService      모의 NpcService
     * @param inventoryService 모의 InventoryService
     * @return ShopService 인스턴스
     */
    private ShopService newService(final ItemCatalogService catalog,
                                   final NpcService npcService,
                                   final InventoryService inventoryService) {
        return new ShopService(catalog, npcService,
                mock(com.myapps.web.myrpg.domain.repository.OwnedItemRepository.class),
                inventoryService, mock(CharacterService.class), fixedActionLog());
    }

    /**
     * 지정된 속성의 장비 아이템을 생성한다.
     *
     * @param id       아이템 ID
     * @param name     아이템 이름
     * @param buyPrice 구매가 (null 가능)
     * @return EquipmentItem 인스턴스
     */
    private Item createEquip(final String id, final String name, final Integer buyPrice) {
        return new EquipmentItem(id, name, ItemType.WEAPON, EquipmentKind.ONE_HANDED_SWORD,
                List.of(new EquipBonus(BonusTarget.STR, 8)), buyPrice, MAX_DURABILITY);
    }

    /**
     * 지정 골드를 가진 CharacterProgress를 생성한다.
     *
     * @param gold 초기 골드
     * @return CharacterProgress 인스턴스
     */
    private CharacterProgress createProgress(final long gold) {
        return new CharacterProgress(
                "테스트", 1, 1, 0L, TalentType.MELEE, null,
                100, 100, 100, "tir-chonaill", 0, gold);
    }

    private ActionLog fixedActionLog() {
        return new ActionLog(Clock.fixed(Instant.ofEpochSecond(FIXED_EPOCH_SECOND), ZoneId.systemDefault()));
    }
}