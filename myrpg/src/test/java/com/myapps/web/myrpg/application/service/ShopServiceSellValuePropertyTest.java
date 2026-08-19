package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.PotionItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 판매가 계산식 배타성 및 결정성 프로퍼티 테스트.
 *
 * <p>아이템이 {@code buyPrice}를 가지면 {@code round(buyPrice * 0.5)}로 계산되고,
 * buyPrice가 없으면 장비 보너스 합산({@code Σ amount * weight})으로 계산되며,
 * 두 경로가 동시에 적용되지 않는다(배타성)를 검증한다. 또한 같은 입력에 대해
 * 결과가 항상 동일함(결정성)을 검증한다.
 *
 * <p>Feature: 010-npc-actions-shop-repair-heal, Property 1: 판매가 계산식 배타성 및 결정성 (BuyPrice vs Catalog Bonuses)
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.8, 15.2</b>
 */
// Feature: 010-npc-actions-shop-repair-heal, Property 1: 판매가 계산식 배타성 및 결정성 (BuyPrice vs Catalog Bonuses)
class ShopServiceSellValuePropertyTest {

    private static final double SELL_RATIO = 0.5;
    private static final long FIXED_EPOCH_SECOND = 1_700_000_000L;
    private static final int MAX_DURABILITY = 20;

    /**
     * buyPrice가 존재하는 임의 아이템의 판매가는 항상 {@code round(buyPrice * 0.5)}와 일치하고,
     * 보너스 합산이 아닌 단일 경로로 계산됨(배타성)을 검증한다.
     *
     * @param buyPrice 구매가 1~100000
     */
    @Property(tries = 100)
    void should_useBuyPricePath_only_whenBuyPricePresent(
            @ForAll("positiveBuyPrice") final int buyPrice) {
        final ShopService service = newService(fixedAction());

        final int amount = 5;
        final EquipmentItem equip = new EquipmentItem(
                "sword", "검", ItemType.WEAPON, EquipmentKind.ONE_HANDED_SWORD,
                List.of(new EquipBonus(BonusTarget.STR, amount)), buyPrice, MAX_DURABILITY);
        final long expected = Math.round(buyPrice * SELL_RATIO);

        final long actual = service.calculateSellValue(equip);
        assertThat(actual).isEqualTo(expected);
    }

    /**
     * buyPrice가 없는 장비 아이템의 판매가는 보너스 합산식
     * {@code Σ amount * weightOf(target)}과 일치하고, buyPrice 경로와 배타적임을 검증한다.
     *
     * @param bonuses 임의 보너스 목록 (amount 1~100)
     */
    @Property(tries = 100)
    void should_sumCatalogBonuses_when_noBuyPrice(@ForAll("equipmentBonusList") final List<EquipBonus> bonuses) {
        final ShopService service = newService(fixedAction());
        final EquipmentItem equip = new EquipmentItem(
                "armor", "갑옷", ItemType.ARMOR, EquipmentKind.ARMOR_BODY, bonuses, null, MAX_DURABILITY);

        final long expected = bonuses.stream()
                .mapToLong(bonus -> (long) bonus.amount() * service.weightOf(bonus.target()))
                .sum();
        assertThat(service.calculateSellValue(equip)).isEqualTo(expected);
    }

    /**
     * buyPrice가 없고 보너스도 없는 장비, 또는 buyPrice가 없는 포션은 판매가 0임을 검증한다.
     *
     * @param item 임의의 무가치 아이템
     */
    @Property(tries = 100)
    void should_returnZero_whenNoBuyPriceAndNoBonus(
            @ForAll("zeroScoringItem") final Item item) {
        final ShopService service = newService(fixedAction());
        assertThat(service.calculateSellValue(item)).isZero();
    }

    // ─── Arbitrary Providers ────────────────────────────────────────────────

    /**
     * 양의 구매가(1~100000)를 생성한다.
     *
     * @return 양의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> positiveBuyPrice() {
        return Arbitraries.integers().between(1, 100_000);
    }

    /**
     * 임의의 보너스 목록(0~5개)을 생성한다.
     *
     * @return EquipBonus 목록 Arbitrary
     */
    @Provide
    Arbitrary<List<EquipBonus>> equipmentBonusList() {
        return Arbitraries.of(BonusTarget.values())
                .flatMap(target -> Arbitraries.integers().between(1, 100)
                        .map(amount -> new EquipBonus(target, amount)))
                .list().ofMinSize(0).ofMaxSize(5);
    }

    /**
     * 판매가 0인 아이템(보너스 없는 장비, buyPrice 없는 포션)을 생성한다.
     *
     * @return 판매가 0 아이템 Arbitrary
     */
    @Provide
    Arbitrary<Item> zeroScoringItem() {
        return Arbitraries.oneOf(
                Arbitraries.just(new PotionItem("potion", "포션", 30, null)),
                Arbitraries.just(new EquipmentItem("armor", "방어구", ItemType.ARMOR,
                        EquipmentKind.ARMOR_BODY, List.of(), null, MAX_DURABILITY)));
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private ShopService newService(final ActionLog actionLog) {
        return new ShopService(
                mock(ItemCatalogService.class),
                mock(NpcService.class),
                mock(com.myapps.web.myrpg.domain.repository.OwnedItemRepository.class),
                mock(InventoryService.class),
                mock(CharacterService.class),
                actionLog);
    }

    private ActionLog fixedAction() {
        return new ActionLog(Clock.fixed(Instant.ofEpochSecond(FIXED_EPOCH_SECOND), ZoneId.systemDefault()));
    }
}