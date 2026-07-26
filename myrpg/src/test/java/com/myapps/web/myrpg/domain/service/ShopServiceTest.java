package com.myapps.web.myrpg.domain.service;

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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ShopService의 단위 테스트.
 *
 * <p>판매가 산출, 무기/방어구 판매 검증, 포션 구매 로직을 검증한다.
 */
class ShopServiceTest {

    private final ShopService shopService = new ShopService();

    // --- sellPrice ---

    @Test
    void should_calculate_sell_price_for_common_weapon() {
        // baseValue=10, COMMON(1.0), itemLevel=1
        // 반올림(10 × 1.0 × (1 + 0.05 × 1)) = 반올림(10 × 1.05) = 11 (10.5 → 11)
        final int price = shopService.sellPrice(10, Grade.COMMON, 1);

        assertEquals(11, price);
    }

    @Test
    void should_calculate_sell_price_for_uncommon_weapon() {
        // baseValue=10, UNCOMMON(1.6), itemLevel=5
        // 반올림(10 × 1.6 × (1 + 0.05 × 5)) = 반올림(10 × 1.6 × 1.25) = 반올림(20.0) = 20
        final int price = shopService.sellPrice(10, Grade.UNCOMMON, 5);

        assertEquals(20, price);
    }

    @Test
    void should_calculate_sell_price_for_rare_weapon() {
        // baseValue=15, RARE(3.0), itemLevel=3
        // 반올림(15 × 3.0 × (1 + 0.05 × 3)) = 반올림(15 × 3.0 × 1.15) = 반올림(51.75) = 52
        final int price = shopService.sellPrice(15, Grade.RARE, 3);

        assertEquals(52, price);
    }

    @Test
    void should_calculate_sell_price_for_epic_weapon() {
        // baseValue=20, EPIC(6.0), itemLevel=7
        // 반올림(20 × 6.0 × (1 + 0.05 × 7)) = 반올림(20 × 6.0 × 1.35) = 반올림(162.0) = 162
        final int price = shopService.sellPrice(20, Grade.EPIC, 7);

        assertEquals(162, price);
    }

    @Test
    void should_calculate_sell_price_for_legendary_weapon() {
        // baseValue=25, LEGENDARY(12.0), itemLevel=10
        // 반올림(25 × 12.0 × (1 + 0.05 × 10)) = 반올림(25 × 12.0 × 1.5) = 반올림(450.0) = 450
        final int price = shopService.sellPrice(25, Grade.LEGENDARY, 10);

        assertEquals(450, price);
    }

    @Test
    void should_calculate_sell_price_with_zero_item_level() {
        // baseValue=10, COMMON(1.0), itemLevel=0
        // 반올림(10 × 1.0 × (1 + 0.0)) = 10
        final int price = shopService.sellPrice(10, Grade.COMMON, 0);

        assertEquals(10, price);
    }

    // --- sellWeapon ---

    @Test
    void should_sell_unequipped_weapon_and_grant_gold() {
        final Player player = new Player("테스트", 1, 0, 100, 100, 50, 50, 10, 5, 5, 0, 100);
        final PlayerWeapon weapon = new PlayerWeapon(
                1L, 1L, "[일반] 낡은 검", WeaponType.SWORD,
                Grade.COMMON, 1, 10, 5, 0, 1, false);

        final int price = shopService.sellWeapon(player, weapon, 10);

        // sellPrice(10, COMMON, 1) = 11
        assertEquals(11, price);
        assertEquals(111, player.getGold());
    }

    @Test
    void should_reject_selling_equipped_weapon() {
        final Player player = new Player("테스트", 1, 0, 100, 100, 50, 50, 10, 5, 5, 0, 100);
        final PlayerWeapon weapon = new PlayerWeapon(
                1L, 1L, "[일반] 낡은 검", WeaponType.SWORD,
                Grade.COMMON, 1, 10, 5, 0, 1, true);

        assertThrows(IllegalEquipmentException.class,
                () -> shopService.sellWeapon(player, weapon, 10));
    }

    // --- sellArmor ---

    @Test
    void should_sell_unequipped_armor_and_grant_gold() {
        final Player player = new Player("테스트", 1, 0, 100, 100, 50, 50, 10, 5, 5, 0, 50);
        final PlayerArmor armor = new PlayerArmor(
                1L, 1L, "[희귀] 강철 투구", ArmorSlot.HELMET,
                Grade.RARE, 15, 3, false);

        final int price = shopService.sellArmor(player, armor, 12);

        // sellPrice(12, RARE, 3) = 반올림(12 × 3.0 × 1.15) = 반올림(41.4) = 41
        assertEquals(41, price);
        assertEquals(91, player.getGold());
    }

    @Test
    void should_reject_selling_equipped_armor() {
        final Player player = new Player("테스트", 1, 0, 100, 100, 50, 50, 10, 5, 5, 0, 50);
        final PlayerArmor armor = new PlayerArmor(
                1L, 1L, "[일반] 가죽 투구", ArmorSlot.HELMET,
                Grade.COMMON, 10, 1, true);

        assertThrows(IllegalEquipmentException.class,
                () -> shopService.sellArmor(player, armor, 10));
    }

    // --- buyPotion ---

    @Test
    void should_buy_potion_and_deduct_gold_and_increment_quantity() {
        final Player player = new Player("테스트", 1, 0, 100, 100, 50, 50, 10, 5, 5, 0, 100);
        final PlayerInventory inventory = new PlayerInventory(1L, ItemType.POTION, 1L, 3);

        shopService.buyPotion(player, inventory, 30);

        assertEquals(70, player.getGold());
        assertEquals(4, inventory.getQuantity());
    }

    @Test
    void should_reject_buying_potion_when_gold_insufficient() {
        final Player player = new Player("테스트", 1, 0, 100, 100, 50, 50, 10, 5, 5, 0, 20);
        final PlayerInventory inventory = new PlayerInventory(1L, ItemType.POTION, 1L, 0);

        assertThrows(InsufficientGoldException.class,
                () -> shopService.buyPotion(player, inventory, 30));
    }

    @Test
    void should_buy_potion_when_gold_equals_price() {
        final Player player = new Player("테스트", 1, 0, 100, 100, 50, 50, 10, 5, 5, 0, 30);
        final PlayerInventory inventory = new PlayerInventory(1L, ItemType.POTION, 1L, 0);

        shopService.buyPotion(player, inventory, 30);

        assertEquals(0, player.getGold());
        assertEquals(1, inventory.getQuantity());
    }

    @Test
    void should_buy_multiple_potions_without_quantity_limit() {
        final Player player = new Player("테스트", 1, 0, 100, 100, 50, 50, 10, 5, 5, 0, 90);
        final PlayerInventory inventory = new PlayerInventory(1L, ItemType.POTION, 1L, 10);

        shopService.buyPotion(player, inventory, 30);
        shopService.buyPotion(player, inventory, 30);
        shopService.buyPotion(player, inventory, 30);

        assertEquals(0, player.getGold());
        assertEquals(13, inventory.getQuantity());
    }
}
