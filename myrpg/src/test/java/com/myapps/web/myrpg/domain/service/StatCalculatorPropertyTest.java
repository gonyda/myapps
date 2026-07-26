package com.myapps.web.myrpg.domain.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.ArmorSlot;
import com.myapps.web.myrpg.domain.model.DamageType;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerArmor;
import com.myapps.web.myrpg.domain.model.PlayerArmorStat;
import com.myapps.web.myrpg.domain.model.PlayerWeapon;
import com.myapps.web.myrpg.domain.model.PlayerWeaponStat;
import com.myapps.web.myrpg.domain.model.StatType;
import com.myapps.web.myrpg.domain.model.WeaponType;
import com.myapps.web.myrpg.domain.model.vo.EffectiveStats;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * StatCalculator 속성 기반 테스트.
 *
 * <p>jqwik을 사용하여 유효 스탯 합산 공식 및 무기 타입별 데미지 타입 결정 로직을 검증한다.
 *
 * <p><b>Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 12.2, 12.3</b>
 */
class StatCalculatorPropertyTest {

    private static final int STAT_MIN = 1;
    private static final int STAT_MAX = 500;
    private static final int RANDOM_STAT_MIN = 0;
    private static final int RANDOM_STAT_MAX = 100;
    private static final int MAX_WEAPON_STATS = 4;
    private static final int MAX_ARMOR_STATS = 8;

    private final StatCalculator statCalculator = new StatCalculator();

    @Provide
    Arbitrary<Player> playerProvider() {
        final Arbitrary<Integer> stat = Arbitraries.integers().between(STAT_MIN, STAT_MAX);
        return Combinators.combine(stat, stat, stat, stat, stat)
                .as((attack, defense, speed, critical, maxHp) ->
                        new Player("TestHero", 1, 0, maxHp, maxHp, 50, 50,
                                attack, defense, speed, critical, 0));
    }

    @Provide
    Arbitrary<PlayerWeapon> weaponProvider() {
        final Arbitrary<Integer> baseAttack = Arbitraries.integers().between(STAT_MIN, STAT_MAX);
        final Arbitrary<Integer> baseSpeed = Arbitraries.integers().between(0, 50);
        final Arbitrary<Integer> baseCritical = Arbitraries.integers().between(0, 50);
        final Arbitrary<WeaponType> weaponType = Arbitraries.of(WeaponType.values());

        return Combinators.combine(baseAttack, baseSpeed, baseCritical, weaponType)
                .as((atk, spd, crit, type) ->
                        new PlayerWeapon(1L, 1L, "[COMMON] TestWeapon", type,
                                Grade.COMMON, 1, atk, spd, crit, 1, true));
    }

    @Provide
    Arbitrary<List<PlayerWeaponStat>> weaponStatsProvider() {
        final Arbitrary<PlayerWeaponStat> singleStat =
                Combinators.combine(
                        Arbitraries.of(StatType.values()),
                        Arbitraries.integers().between(RANDOM_STAT_MIN, RANDOM_STAT_MAX)
                ).as((type, value) -> new PlayerWeaponStat(1L, type, value));

        return singleStat.list().ofMinSize(0).ofMaxSize(MAX_WEAPON_STATS);
    }

    @Provide
    Arbitrary<List<PlayerArmorStat>> armorStatsProvider() {
        final Arbitrary<PlayerArmorStat> singleStat =
                Combinators.combine(
                        Arbitraries.of(StatType.values()),
                        Arbitraries.integers().between(RANDOM_STAT_MIN, RANDOM_STAT_MAX)
                ).as((type, value) -> new PlayerArmorStat(1L, type, value));

        return singleStat.list().ofMinSize(0).ofMaxSize(MAX_ARMOR_STATS);
    }

    // Feature: myrpg-gen1-mvp, Property 9: 유효 스탯 합산
    /**
     * 유효 스탯은 캐릭터 기본 스탯 + 무기 base값 + 무기 랜덤 스탯 + 방어구 랜덤 스탯의 합으로 산출된다.
     *
     * <p>공격력 = player.attack + weapon.baseAttack + sum(weaponStats[ATTACK]) + sum(armorStats[ATTACK])
     * <br>방어력 = player.defense + sum(weaponStats[DEFENSE]) + sum(armorStats[DEFENSE])
     * <br>속도 = player.speed + weapon.baseSpeed + sum(weaponStats[SPEED]) + sum(armorStats[SPEED])
     * <br>치명타 = player.critical + weapon.baseCritical + sum(weaponStats[CRITICAL]) + sum(armorStats[CRITICAL])
     * <br>최대HP = player.maxHp + sum(weaponStats[HP]) + sum(armorStats[HP])
     *
     * <p><b>Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 12.2</b>
     */
    @Property(tries = 100)
    void effectiveStatsSumFormula(
            @ForAll("playerProvider") final Player player,
            @ForAll("weaponProvider") final PlayerWeapon weapon,
            @ForAll("weaponStatsProvider") final List<PlayerWeaponStat> weaponStats,
            @ForAll("armorStatsProvider") final List<PlayerArmorStat> armorStats) {

        final List<PlayerArmor> equippedArmors = Collections.emptyList();

        final EffectiveStats result = statCalculator.compute(
                player, weapon, weaponStats, equippedArmors, armorStats);

        final int expectedAttack = player.getAttack() + weapon.getBaseAttack()
                + sumWeaponStatByType(weaponStats, StatType.ATTACK)
                + sumArmorStatByType(armorStats, StatType.ATTACK);
        final int expectedDefense = player.getDefense()
                + sumWeaponStatByType(weaponStats, StatType.DEFENSE)
                + sumArmorStatByType(armorStats, StatType.DEFENSE);
        final int expectedSpeed = player.getSpeed() + weapon.getBaseSpeed()
                + sumWeaponStatByType(weaponStats, StatType.SPEED)
                + sumArmorStatByType(armorStats, StatType.SPEED);
        final int expectedCritical = player.getCritical() + weapon.getBaseCritical()
                + sumWeaponStatByType(weaponStats, StatType.CRITICAL)
                + sumArmorStatByType(armorStats, StatType.CRITICAL);
        final int expectedMaxHp = player.getMaxHp()
                + sumWeaponStatByType(weaponStats, StatType.HP)
                + sumArmorStatByType(armorStats, StatType.HP);

        assertEquals(expectedAttack, result.attack(),
                "유효 공격력 = 캐릭터 공격력 + 무기 baseAttack + 랜덤 ATTACK 합");
        assertEquals(expectedDefense, result.defense(),
                "유효 방어력 = 캐릭터 방어력 + 랜덤 DEFENSE 합");
        assertEquals(expectedSpeed, result.speed(),
                "유효 속도 = 캐릭터 속도 + 무기 baseSpeed + 랜덤 SPEED 합");
        assertEquals(expectedCritical, result.critical(),
                "유효 치명타 = 캐릭터 치명타 + 무기 baseCritical + 랜덤 CRITICAL 합");
        assertEquals(expectedMaxHp, result.maxHp(),
                "유효 최대HP = 캐릭터 최대HP + 랜덤 HP 합");
    }

    // Feature: myrpg-gen1-mvp, Property 10: 무기 타입에 따른 데미지 타입
    /**
     * 무기 타입이 STAFF이면 데미지 타입은 MAGICAL, 그 외 모든 타입은 PHYSICAL이다.
     * 무기가 없으면(null) 데미지 타입은 PHYSICAL이다.
     *
     * <p><b>Validates: Requirements 5.6, 5.7, 12.3</b>
     */
    @Property(tries = 100)
    void damageTypeDeterminedByWeaponType(
            @ForAll("playerProvider") final Player player,
            @ForAll("weaponProvider") final PlayerWeapon weapon) {

        final List<PlayerWeaponStat> weaponStats = Collections.emptyList();
        final List<PlayerArmor> equippedArmors = Collections.emptyList();
        final List<PlayerArmorStat> armorStats = Collections.emptyList();

        final EffectiveStats result = statCalculator.compute(
                player, weapon, weaponStats, equippedArmors, armorStats);

        final DamageType expected = weapon.getWeaponType() == WeaponType.STAFF
                ? DamageType.MAGICAL
                : DamageType.PHYSICAL;

        assertEquals(expected, result.damageType(),
                "STAFF → MAGICAL, 그 외 → PHYSICAL");
    }

    // Feature: myrpg-gen1-mvp, Property 10: 무기 미착용 시 데미지 타입
    /**
     * 무기가 null인 경우 데미지 타입은 PHYSICAL이다.
     *
     * <p><b>Validates: Requirements 5.7</b>
     */
    @Property(tries = 100)
    void damageTypeIsPhysicalWhenNoWeapon(
            @ForAll("playerProvider") final Player player,
            @ForAll("armorStatsProvider") final List<PlayerArmorStat> armorStats) {

        final List<PlayerWeaponStat> weaponStats = Collections.emptyList();
        final List<PlayerArmor> equippedArmors = Collections.emptyList();

        final EffectiveStats result = statCalculator.compute(
                player, null, weaponStats, equippedArmors, armorStats);

        assertEquals(DamageType.PHYSICAL, result.damageType(),
                "무기 미착용 시 데미지 타입은 PHYSICAL");
    }

    /**
     * 무기 랜덤 스탯 목록에서 지정 타입의 수치 합계를 구한다.
     *
     * @param stats    무기 랜덤 스탯 목록
     * @param statType 합산할 능력치 종류
     * @return 해당 종류의 수치 합계
     */
    private int sumWeaponStatByType(final List<PlayerWeaponStat> stats,
                                    final StatType statType) {
        return stats.stream()
                .filter(s -> s.getStatType() == statType)
                .mapToInt(PlayerWeaponStat::getStatValue)
                .sum();
    }

    /**
     * 방어구 랜덤 스탯 목록에서 지정 타입의 수치 합계를 구한다.
     *
     * @param stats    방어구 랜덤 스탯 목록
     * @param statType 합산할 능력치 종류
     * @return 해당 종류의 수치 합계
     */
    private int sumArmorStatByType(final List<PlayerArmorStat> stats,
                                   final StatType statType) {
        return stats.stream()
                .filter(s -> s.getStatType() == statType)
                .mapToInt(PlayerArmorStat::getStatValue)
                .sum();
    }
}
