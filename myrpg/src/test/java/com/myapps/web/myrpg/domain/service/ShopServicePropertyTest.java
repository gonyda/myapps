package com.myapps.web.myrpg.domain.service;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.exception.IllegalEquipmentException;
import com.myapps.web.myrpg.domain.exception.InsufficientGoldException;
import com.myapps.web.myrpg.domain.model.ArmorSlot;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerArmor;
import com.myapps.web.myrpg.domain.model.PlayerInventory;
import com.myapps.web.myrpg.domain.model.PlayerWeapon;
import com.myapps.web.myrpg.domain.model.WeaponType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ShopService 판매·구매 속성 기반 테스트.
 *
 * <p>jqwik을 사용하여 판매가 공식, 판매 거부 규칙, 포션 구매 로직을 검증한다.
 *
 * <p><b>Validates: Requirements 23.1, 23.2, 23.4, 23.5, 24.2, 24.3</b>
 */
class ShopServicePropertyTest {

    private static final int BASE_VALUE_MIN = 1;
    private static final int BASE_VALUE_MAX = 100;
    private static final int ITEM_LEVEL_MIN = 0;
    private static final int ITEM_LEVEL_MAX = 20;
    private static final int GOLD_MIN = 0;
    private static final int GOLD_MAX = 10000;
    private static final int BUY_PRICE_MIN = 1;
    private static final int BUY_PRICE_MAX = 500;
    private static final int QUANTITY_MIN = 1;
    private static final int QUANTITY_MAX = 10;
    private static final double ITEM_LEVEL_SCALING = 0.05;

    private final ShopService shopService = new ShopService();

    // --- Providers ---

    @Provide
    Arbitrary<Integer> baseValueProvider() {
        return Arbitraries.integers().between(BASE_VALUE_MIN, BASE_VALUE_MAX);
    }

    @Provide
    Arbitrary<Grade> gradeProvider() {
        return Arbitraries.of(Grade.values());
    }

    @Provide
    Arbitrary<Integer> itemLevelProvider() {
        return Arbitraries.integers().between(ITEM_LEVEL_MIN, ITEM_LEVEL_MAX);
    }

    @Provide
    Arbitrary<Integer> goldProvider() {
        return Arbitraries.integers().between(GOLD_MIN, GOLD_MAX);
    }

    @Provide
    Arbitrary<Integer> buyPriceProvider() {
        return Arbitraries.integers().between(BUY_PRICE_MIN, BUY_PRICE_MAX);
    }

    @Provide
    Arbitrary<Integer> quantityProvider() {
        return Arbitraries.integers().between(QUANTITY_MIN, QUANTITY_MAX);
    }

    @Provide
    Arbitrary<WeaponType> weaponTypeProvider() {
        return Arbitraries.of(WeaponType.values());
    }

    @Provide
    Arbitrary<ArmorSlot> armorSlotProvider() {
        return Arbitraries.of(ArmorSlot.values());
    }

    // =====================================================================
    // Property 44: 판매가 공식
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 44: 판매가 공식
    /**
     * 임의의 baseValue, 등급, itemLevel에 대해 판매가는
     * {@code 반올림(baseValue × 등급배수 × (1 + 0.05 × itemLevel))}(HALF_UP)이다.
     *
     * <p><b>Validates: Requirements 23.1, 23.2</b>
     */
    @Property(tries = 100)
    void sellPriceMatchesFormula(
            @ForAll("baseValueProvider") final int baseValue,
            @ForAll("gradeProvider") final Grade grade,
            @ForAll("itemLevelProvider") final int itemLevel) {

        final int actual = shopService.sellPrice(baseValue, grade, itemLevel);

        final double multiplier = grade.getSellMultiplier();
        final double levelFactor = 1 + ITEM_LEVEL_SCALING * itemLevel;
        final int expected = (int) Math.round(baseValue * multiplier * levelFactor);

        assertEquals(expected, actual,
                "판매가는 반올림(baseValue × 등급배수 × (1 + 0.05 × itemLevel))이어야 한다");
    }

    // =====================================================================
    // Property 45: 판매 거부 규칙
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 45: 판매 거부 규칙
    /**
     * 착용 중인 무기를 판매하려 하면 IllegalEquipmentException이 발생하고
     * 플레이어 골드는 변하지 않는다.
     *
     * <p><b>Validates: Requirements 23.4, 23.5</b>
     */
    @Property(tries = 100)
    void sellEquippedWeaponIsRejectedAndStateUnchanged(
            @ForAll("goldProvider") final int gold,
            @ForAll("baseValueProvider") final int baseValue,
            @ForAll("gradeProvider") final Grade grade,
            @ForAll("itemLevelProvider") final int itemLevel,
            @ForAll("weaponTypeProvider") final WeaponType weaponType) {

        final Player player = new Player(
                "TestHero", 1, 0, 100, 100, 50, 50, 10, 5, 5, 5, gold);
        final PlayerWeapon equippedWeapon = new PlayerWeapon(
                1L, 1L, "[테스트] 무기", weaponType, grade, itemLevel,
                10, 5, 3, 2, true);

        final int goldBefore = player.getGold();

        assertThrows(IllegalEquipmentException.class,
                () -> shopService.sellWeapon(player, equippedWeapon, baseValue));

        assertEquals(goldBefore, player.getGold(),
                "착용 중 무기 판매 거부 시 골드가 변하지 않아야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 45: 판매 거부 규칙
    /**
     * 착용 중인 방어구를 판매하려 하면 IllegalEquipmentException이 발생하고
     * 플레이어 골드는 변하지 않는다.
     *
     * <p><b>Validates: Requirements 23.4, 23.5</b>
     */
    @Property(tries = 100)
    void sellEquippedArmorIsRejectedAndStateUnchanged(
            @ForAll("goldProvider") final int gold,
            @ForAll("baseValueProvider") final int baseValue,
            @ForAll("gradeProvider") final Grade grade,
            @ForAll("itemLevelProvider") final int itemLevel,
            @ForAll("armorSlotProvider") final ArmorSlot armorSlot) {

        final Player player = new Player(
                "TestHero", 1, 0, 100, 100, 50, 50, 10, 5, 5, 5, gold);
        final PlayerArmor equippedArmor = new PlayerArmor(
                1L, 1L, "[테스트] 방어구", armorSlot, grade, 10, itemLevel, true);

        final int goldBefore = player.getGold();

        assertThrows(IllegalEquipmentException.class,
                () -> shopService.sellArmor(player, equippedArmor, baseValue));

        assertEquals(goldBefore, player.getGold(),
                "착용 중 방어구 판매 거부 시 골드가 변하지 않아야 한다");
    }

    // =====================================================================
    // Property 46: 포션 구매
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 46: 포션 구매
    /**
     * 골드가 구매가 × 수량 이상이면, 순차적 buyPotion 호출로 골드는 구매가 × q만큼 감소하고
     * 인벤토리 수량은 q만큼 증가한다.
     *
     * <p><b>Validates: Requirements 24.2, 24.3</b>
     */
    @Property(tries = 100)
    void buyPotionSucceedsWhenGoldSufficient(
            @ForAll("buyPriceProvider") final int buyPrice,
            @ForAll("quantityProvider") final int quantity) {

        final int requiredGold = buyPrice * quantity;
        final Player player = new Player(
                "TestHero", 1, 0, 100, 100, 50, 50, 10, 5, 5, 5, requiredGold);
        final PlayerInventory inventory = new PlayerInventory(
                1L, ItemType.POTION, 1L, 0);

        for (int i = 0; i < quantity; i++) {
            shopService.buyPotion(player, inventory, buyPrice);
        }

        assertEquals(0, player.getGold(),
                "구매가 × q만큼 골드가 감소해야 한다");
        assertEquals(quantity, inventory.getQuantity(),
                "인벤토리 수량이 q만큼 증가해야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 46: 포션 구매
    /**
     * 골드가 구매가 미만이면 InsufficientGoldException이 발생하고
     * 플레이어 골드·인벤토리 수량 모두 변하지 않는다.
     *
     * <p><b>Validates: Requirements 24.2, 24.3</b>
     */
    @Property(tries = 100)
    void buyPotionRejectedWhenGoldInsufficient(
            @ForAll("buyPriceProvider") final int buyPrice) {

        final int insufficientGold = buyPrice - 1;
        final Player player = new Player(
                "TestHero", 1, 0, 100, 100, 50, 50, 10, 5, 5, 5, insufficientGold);
        final PlayerInventory inventory = new PlayerInventory(
                1L, ItemType.POTION, 1L, 5);

        final int goldBefore = player.getGold();
        final int quantityBefore = inventory.getQuantity();

        assertThrows(InsufficientGoldException.class,
                () -> shopService.buyPotion(player, inventory, buyPrice));

        assertEquals(goldBefore, player.getGold(),
                "골드 부족 시 골드가 변하지 않아야 한다");
        assertEquals(quantityBefore, inventory.getQuantity(),
                "골드 부족 시 인벤토리 수량이 변하지 않아야 한다");
    }
}
