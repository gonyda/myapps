package com.myapps.web.myrpg.domain.service;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.exception.InsufficientMpException;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerInventory;
import com.myapps.web.myrpg.domain.random.FixedRandomSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BattleService 전투 자원 관리 속성 기반 테스트.
 *
 * <p>jqwik을 사용하여 MP 회복, 스킬 MP 소비/거부, 포션 상한, 소모품 수량 감소를 검증한다.
 *
 * <p><b>Validates: Requirements 11.1, 11.2, 11.3, 11.4, 11.5, 11.6</b>
 */
class BattleResourcePropertyTest {

    private static final int MP_MIN = 0;
    private static final int MP_MAX = 500;
    private static final int MAX_MP_MIN = 1;
    private static final int MAX_MP_MAX = 500;
    private static final int HP_MIN = 1;
    private static final int HP_MAX = 1000;
    private static final int EFFECT_AMOUNT_MIN = 1;
    private static final int EFFECT_AMOUNT_MAX = 200;
    private static final int QUANTITY_MIN = 1;
    private static final int QUANTITY_MAX = 99;
    private static final int MP_COST_MIN = 1;
    private static final int MP_COST_MAX = 200;

    private final BattleService battleService = new BattleService(new FixedRandomSource(0.5));

    // --- Providers ---

    @Provide
    Arbitrary<Integer> maxMpProvider() {
        return Arbitraries.integers().between(MAX_MP_MIN, MAX_MP_MAX);
    }

    @Provide
    Arbitrary<Integer> effectAmountProvider() {
        return Arbitraries.integers().between(EFFECT_AMOUNT_MIN, EFFECT_AMOUNT_MAX);
    }

    @Provide
    Arbitrary<Integer> quantityProvider() {
        return Arbitraries.integers().between(QUANTITY_MIN, QUANTITY_MAX);
    }

    // =====================================================================
    // Property 20: 전투 종료 시 MP 완전 회복
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 20: 전투 종료 시 MP 완전 회복
    /**
     * 전투 종료 후 restoreMpAfterBattle을 호출하면 MP가 maxMp로 완전 회복된다.
     *
     * <p>임의의 currentMp(0~maxMp)와 maxMp에 대해, 호출 후 player.getMp() == maxMp이다.
     *
     * <p><b>Validates: Requirements 11.1</b>
     */
    @Property(tries = 100)
    void restoreMpAfterBattleSetsToMaxMp(
            @ForAll("maxMpProvider") final int maxMp) {

        final int currentMp = maxMp / 2;
        final Player player = new Player(
                "TestHero", 1, 0, 100, 100, currentMp, maxMp, 10, 5, 5, 5, 0);

        battleService.restoreMpAfterBattle(player);

        assertEquals(maxMp, player.getMp(),
                "전투 종료 후 MP는 maxMp와 동일해야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 20: 전투 종료 시 MP 완전 회복
    /**
     * MP가 0인 상태에서도 전투 종료 시 maxMp로 완전 회복된다.
     *
     * <p><b>Validates: Requirements 11.1</b>
     */
    @Property(tries = 100)
    void restoreMpAfterBattleFromZero(
            @ForAll("maxMpProvider") final int maxMp) {

        final Player player = new Player(
                "TestHero", 1, 0, 100, 100, 0, maxMp, 10, 5, 5, 5, 0);

        battleService.restoreMpAfterBattle(player);

        assertEquals(maxMp, player.getMp(),
                "MP가 0이어도 전투 종료 후 maxMp로 회복해야 한다");
    }

    // =====================================================================
    // Property 21: 스킬 MP 소비와 부족 시 거부
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 21: 스킬 MP 소비와 부족 시 거부
    /**
     * 현재 MP가 mpCost 이상이면 validateAndConsumeMp는 MP를 mpCost만큼 차감한다.
     *
     * <p><b>Validates: Requirements 11.2</b>
     */
    @Property(tries = 100)
    void validateAndConsumeMpReducesMpByCost(
            @ForAll("maxMpProvider") final int maxMp) {

        final int mpCost = Math.max(1, maxMp / 3);
        final int currentMp = maxMp;
        final Player player = new Player(
                "TestHero", 1, 0, 100, 100, currentMp, maxMp, 10, 5, 5, 5, 0);

        battleService.validateAndConsumeMp(player, mpCost);

        assertEquals(currentMp - mpCost, player.getMp(),
                "MP는 mpCost만큼 정확히 차감되어야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 21: 스킬 MP 소비와 부족 시 거부
    /**
     * 현재 MP가 mpCost 미만이면 InsufficientMpException이 발생하고 MP는 변하지 않는다.
     *
     * <p><b>Validates: Requirements 11.3</b>
     */
    @Property(tries = 100)
    void validateAndConsumeMpThrowsWhenInsufficient(
            @ForAll("maxMpProvider") final int maxMp) {

        final int currentMp = Math.max(0, maxMp / 4);
        final int mpCost = currentMp + 1;
        final Player player = new Player(
                "TestHero", 1, 0, 100, 100, currentMp, maxMp, 10, 5, 5, 5, 0);

        assertThrows(InsufficientMpException.class,
                () -> battleService.validateAndConsumeMp(player, mpCost));
        assertEquals(currentMp, player.getMp(),
                "MP 부족 시 MP는 변경되지 않아야 한다");
    }

    // =====================================================================
    // Property 22: 포션 회복 상한
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 22: 포션 회복 상한
    /**
     * HP 포션 적용 결과는 min(current + effectAmount, maxHp)이며 maxHp를 초과하지 않는다.
     *
     * <p><b>Validates: Requirements 11.4</b>
     */
    @Property(tries = 100)
    void applyHpPotionNeverExceedsMaxHp(
            @ForAll("effectAmountProvider") final int effectAmount) {

        final int maxHp = 200;
        final int currentHp = maxHp / 2;
        final Player player = new Player(
                "TestHero", 1, 0, currentHp, maxHp, 50, 100, 10, 5, 5, 5, 0);

        final int result = battleService.applyPotion(player, effectAmount, maxHp, true);

        final int expected = Math.min(currentHp + effectAmount, maxHp);
        assertEquals(expected, result, "HP 포션 결과는 min(current+effect, maxHp)이어야 한다");
        assertTrue(result <= maxHp, "HP 포션 결과는 maxHp를 초과할 수 없다");
        assertEquals(result, player.getHp(), "플레이어 HP가 반환값과 동일해야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 22: 포션 회복 상한
    /**
     * MP 포션 적용 결과는 min(current + effectAmount, maxMp)이며 maxMp를 초과하지 않는다.
     *
     * <p><b>Validates: Requirements 11.5</b>
     */
    @Property(tries = 100)
    void applyMpPotionNeverExceedsMaxMp(
            @ForAll("effectAmountProvider") final int effectAmount) {

        final int maxMp = 150;
        final int currentMp = maxMp / 3;
        final Player player = new Player(
                "TestHero", 1, 0, 100, 200, currentMp, maxMp, 10, 5, 5, 5, 0);

        final int result = battleService.applyPotion(player, effectAmount, maxMp, false);

        final int expected = Math.min(currentMp + effectAmount, maxMp);
        assertEquals(expected, result, "MP 포션 결과는 min(current+effect, maxMp)이어야 한다");
        assertTrue(result <= maxMp, "MP 포션 결과는 maxMp를 초과할 수 없다");
        assertEquals(result, player.getMp(), "플레이어 MP가 반환값과 동일해야 한다");
    }

    // =====================================================================
    // Property 23: 소모품 사용 시 수량 감소
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 23: 소모품 사용 시 수량 감소
    /**
     * consumeItem 호출 시 인벤토리 수량이 정확히 1 감소한다.
     *
     * <p><b>Validates: Requirements 11.6</b>
     */
    @Property(tries = 100)
    void consumeItemDecreasesQuantityByOne(
            @ForAll("quantityProvider") final int quantity) {

        final PlayerInventory inventory = new PlayerInventory(1L, ItemType.POTION, 1L, quantity);

        battleService.consumeItem(inventory);

        assertEquals(quantity - 1, inventory.getQuantity(),
                "소모품 사용 후 수량은 정확히 1 감소해야 한다");
    }
}
