package com.myapps.web.myrpg.domain.service;

import com.myapps.web.myrpg.domain.exception.InsufficientMpException;
import com.myapps.web.myrpg.domain.model.DamageType;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerInventory;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.vo.DamageResult;
import com.myapps.web.myrpg.domain.model.vo.EffectiveStats;
import com.myapps.web.myrpg.domain.model.vo.TurnOrder;
import com.myapps.web.myrpg.domain.random.FixedRandomSource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BattleService의 단위 테스트.
 *
 * <p>데미지 산출, 치명타, 선후공, 도주, MP 관리, 포션, 소모품 로직을 검증한다.
 */
class BattleServiceTest {

    // --- criticalChance ---

    @Test
    void should_return_base_critical_chance_when_speed_and_critical_are_zero() {
        final BattleService service = new BattleService(new FixedRandomSource(0.5));
        final EffectiveStats stats = new EffectiveStats(10, 5, 0, 0, 100, DamageType.PHYSICAL);

        final int chance = service.criticalChance(stats);

        assertEquals(5, chance, "speed=0, critical=0이면 기본 5%");
    }

    @Test
    void should_calculate_critical_chance_with_speed_and_critical() {
        final BattleService service = new BattleService(new FixedRandomSource(0.5));
        // 5 + (20 * 0.2) + 10 = 5 + 4 + 10 = 19
        final EffectiveStats stats = new EffectiveStats(10, 5, 20, 10, 100, DamageType.PHYSICAL);

        final int chance = service.criticalChance(stats);

        assertEquals(19, chance);
    }

    @Test
    void should_clamp_critical_chance_to_max_100() {
        final BattleService service = new BattleService(new FixedRandomSource(0.5));
        // 5 + (100 * 0.2) + 200 = 5 + 20 + 200 = 225 → clamp to 100
        final EffectiveStats stats = new EffectiveStats(10, 5, 100, 200, 100, DamageType.PHYSICAL);

        final int chance = service.criticalChance(stats);

        assertEquals(100, chance);
    }

    // --- computeDamage ---

    @Test
    void should_compute_physical_damage_without_critical() {
        // deviation=1.0 (ratio 0.5 → 0.9 + 0.5*0.2 = 1.0), crit check fail (0.99 → 0.99*100=99 >= 5)
        final FixedRandomSource random = new FixedRandomSource(0.5, 0.99);
        final BattleService service = new BattleService(random);
        final EffectiveStats stats = new EffectiveStats(50, 10, 0, 0, 100, DamageType.PHYSICAL);

        // base = 50 * 1.5 - 10 * 0.5 = 75 - 5 = 70
        // deviation = 0.9 + 0.5 * 0.2 = 1.0
        // damage = 70 * 1.0 = 70 (no crit)
        final DamageResult result = service.computeDamage(50, 1.5, DamageType.PHYSICAL, 10, stats);

        assertEquals(70, result.damage());
        assertFalse(result.critical());
    }

    @Test
    void should_compute_damage_with_critical_hit() {
        // deviation=1.0 (ratio 0.5), crit check pass (0.01 → 0.01*100=1 < 5)
        final FixedRandomSource random = new FixedRandomSource(0.5, 0.01);
        final BattleService service = new BattleService(random);
        final EffectiveStats stats = new EffectiveStats(50, 10, 0, 0, 100, DamageType.PHYSICAL);

        // base = 50 * 1.5 - 10 * 0.5 = 70
        // deviation = 1.0, crit → 70 * 1.5 = 105
        final DamageResult result = service.computeDamage(50, 1.5, DamageType.PHYSICAL, 10, stats);

        assertEquals(105, result.damage());
        assertTrue(result.critical());
    }

    @Test
    void should_enforce_minimum_damage_of_1() {
        // deviation=0.9 (ratio 0.0 → 0.9 + 0.0 * 0.2 = 0.9), no crit (0.99)
        final FixedRandomSource random = new FixedRandomSource(0.0, 0.99);
        final BattleService service = new BattleService(random);
        final EffectiveStats stats = new EffectiveStats(1, 10, 0, 0, 100, DamageType.PHYSICAL);

        // base = 1 * 1.0 - 100 * 0.5 = 1 - 50 = -49
        // deviation = 0.9, damage = -49 * 0.9 = -44.1 → min 1
        final DamageResult result = service.computeDamage(1, 1.0, DamageType.PHYSICAL, 100, stats);

        assertEquals(1, result.damage());
        assertFalse(result.critical());
    }

    @Test
    void should_compute_magical_damage_with_lower_defense_coefficient() {
        // deviation=1.0 (ratio 0.5), no crit (0.99)
        final FixedRandomSource random = new FixedRandomSource(0.5, 0.99);
        final BattleService service = new BattleService(random);
        final EffectiveStats stats = new EffectiveStats(50, 10, 0, 0, 100, DamageType.MAGICAL);

        // base = 50 * 1.5 - 10 * 0.2 = 75 - 2 = 73
        // deviation = 1.0, no crit → 73
        final DamageResult result = service.computeDamage(50, 1.5, DamageType.MAGICAL, 10, stats);

        assertEquals(73, result.damage());
        assertFalse(result.critical());
    }

    // --- decideTurnOrder ---

    @Test
    void should_return_player_first_when_player_is_faster() {
        final BattleService service = new BattleService(new FixedRandomSource(0.5));

        final TurnOrder order = service.decideTurnOrder(10, 5);

        assertEquals(TurnOrder.PLAYER_FIRST, order);
    }

    @Test
    void should_return_monster_first_when_monster_is_faster() {
        final BattleService service = new BattleService(new FixedRandomSource(0.5));

        final TurnOrder order = service.decideTurnOrder(5, 10);

        assertEquals(TurnOrder.MONSTER_FIRST, order);
    }

    @Test
    void should_return_player_first_when_speeds_equal_and_random_below_half() {
        final BattleService service = new BattleService(new FixedRandomSource(0.3));

        final TurnOrder order = service.decideTurnOrder(10, 10);

        assertEquals(TurnOrder.PLAYER_FIRST, order);
    }

    @Test
    void should_return_monster_first_when_speeds_equal_and_random_above_half() {
        final BattleService service = new BattleService(new FixedRandomSource(0.7));

        final TurnOrder order = service.decideTurnOrder(10, 10);

        assertEquals(TurnOrder.MONSTER_FIRST, order);
    }

    // --- monsterDamage ---

    @Test
    void should_compute_monster_damage_without_critical() {
        // deviation=1.0 (ratio 0.5)
        final FixedRandomSource random = new FixedRandomSource(0.5);
        final BattleService service = new BattleService(random);

        // base = 30 * 1.0 - 10 * 0.5 = 30 - 5 = 25
        // deviation = 1.0 → 25
        final DamageResult result = service.monsterDamage(30, DamageType.PHYSICAL, 10);

        assertEquals(25, result.damage());
        assertFalse(result.critical());
    }

    @Test
    void should_enforce_minimum_monster_damage_of_1() {
        // deviation=0.9 (ratio 0.0)
        final FixedRandomSource random = new FixedRandomSource(0.0);
        final BattleService service = new BattleService(random);

        // base = 5 * 1.0 - 100 * 0.5 = 5 - 50 = -45
        // deviation = 0.9, damage = -45 * 0.9 = -40.5 → min 1
        final DamageResult result = service.monsterDamage(5, DamageType.PHYSICAL, 100);

        assertEquals(1, result.damage());
        assertFalse(result.critical());
    }

    // --- attemptFlee ---

    @Test
    void should_succeed_flee_when_random_below_half() {
        final BattleService service = new BattleService(new FixedRandomSource(0.3));

        assertTrue(service.attemptFlee());
    }

    @Test
    void should_fail_flee_when_random_above_half() {
        final BattleService service = new BattleService(new FixedRandomSource(0.7));

        assertFalse(service.attemptFlee());
    }

    // --- validateAndConsumeMp ---

    @Test
    void should_consume_mp_when_sufficient() {
        final BattleService service = new BattleService(new FixedRandomSource(0.5));
        final Player player = new Player("테스트", 1, 0, 100, 100, 30, 50, 10, 5, 5, 0, 0);

        service.validateAndConsumeMp(player, 20);

        assertEquals(10, player.getMp());
    }

    @Test
    void should_throw_when_mp_insufficient() {
        final BattleService service = new BattleService(new FixedRandomSource(0.5));
        final Player player = new Player("테스트", 1, 0, 100, 100, 5, 50, 10, 5, 5, 0, 0);

        assertThrows(InsufficientMpException.class,
                () -> service.validateAndConsumeMp(player, 20));
    }

    // --- restoreMpAfterBattle ---

    @Test
    void should_restore_mp_to_max_after_battle() {
        final BattleService service = new BattleService(new FixedRandomSource(0.5));
        final Player player = new Player("테스트", 1, 0, 100, 100, 10, 50, 10, 5, 5, 0, 0);

        service.restoreMpAfterBattle(player);

        assertEquals(50, player.getMp());
    }

    // --- applyPotion ---

    @Test
    void should_heal_hp_up_to_max() {
        final BattleService service = new BattleService(new FixedRandomSource(0.5));
        final Player player = new Player("테스트", 1, 0, 50, 100, 50, 50, 10, 5, 5, 0, 0);

        final int newHp = service.applyPotion(player, 80, 100, true);

        assertEquals(100, newHp, "HP는 최대치를 초과하지 않아야 한다");
        assertEquals(100, player.getHp());
    }

    @Test
    void should_heal_hp_partially() {
        final BattleService service = new BattleService(new FixedRandomSource(0.5));
        final Player player = new Player("테스트", 1, 0, 50, 100, 50, 50, 10, 5, 5, 0, 0);

        final int newHp = service.applyPotion(player, 30, 100, true);

        assertEquals(80, newHp);
        assertEquals(80, player.getHp());
    }

    @Test
    void should_heal_mp_with_potion() {
        final BattleService service = new BattleService(new FixedRandomSource(0.5));
        final Player player = new Player("테스트", 1, 0, 100, 100, 10, 50, 10, 5, 5, 0, 0);

        final int newMp = service.applyPotion(player, 25, 50, false);

        assertEquals(35, newMp);
        assertEquals(35, player.getMp());
    }

    // --- consumeItem ---

    @Test
    void should_decrease_item_quantity_by_one() {
        final BattleService service = new BattleService(new FixedRandomSource(0.5));
        final PlayerInventory inventory = new PlayerInventory(1L, ItemType.POTION, 1L, 5);

        service.consumeItem(inventory);

        assertEquals(4, inventory.getQuantity());
    }
}
