package com.myapps.web.myrpg.application.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.ArmorSlot;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.WeaponType;
import com.myapps.web.myrpg.domain.template.DungeonTemplate;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MasterDataLoader 속성 기반 테스트.
 *
 * <p>jqwik을 사용하여 세대1 마스터 데이터의 불변식을 검증한다.
 * Property 34(부분): 등급 분포 정합성, Property 50: 전체 장비 드랍을 검증한다.
 *
 * <p><b>Validates: Requirements 18.1, 18.2, 18.3, 18.4, 27.1, 27.2, 27.3, 27.4, 27.5</b>
 */
class MasterDataLoaderPropertyTest {

    private static final double GRADE_CHANCE_TOLERANCE = 1e-6;
    private static final Set<WeaponType> ALL_WEAPON_TYPES =
            Arrays.stream(WeaponType.values()).collect(Collectors.toUnmodifiableSet());
    private static final Set<ArmorSlot> ALL_ARMOR_SLOTS =
            Arrays.stream(ArmorSlot.values()).collect(Collectors.toUnmodifiableSet());

    private static final MasterDataLoader LOADER;

    static {
        LOADER = new MasterDataLoader(new ObjectMapper());
        LOADER.load();
    }

    @Provide
    Arbitrary<DungeonTemplate> dungeonProvider() {
        final List<DungeonTemplate> dungeons = LOADER.allDungeons();
        return Arbitraries.of(dungeons);
    }

    // Feature: myrpg-gen1-mvp, Property 34: 등급 분포 정합성
    /**
     * 모든 던전의 gradeChance 합은 허용오차(1e-6) 내에서 1.0이어야 한다.
     *
     * <p>gradeChance 맵은 5개 등급(COMMON~LEGENDARY)을 모두 포함하고,
     * 각 값은 0 이상이며, 합은 1.0이어야 한다.
     *
     * <p><b>Validates: Requirements 18.4</b>
     */
    @Property(tries = 100)
    void gradeChanceSumEqualsOne(@ForAll("dungeonProvider") final DungeonTemplate dungeon) {
        final double sum = dungeon.gradeChance().values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        assertTrue(Math.abs(sum - 1.0) < GRADE_CHANCE_TOLERANCE,
                "던전 '" + dungeon.name() + "'의 gradeChance 합은 1.0이어야 한다 (실제: " + sum + ")");

        assertEquals(Grade.values().length, dungeon.gradeChance().size(),
                "던전 '" + dungeon.name() + "'의 gradeChance는 모든 등급을 포함해야 한다");

        for (final Double chance : dungeon.gradeChance().values()) {
            assertTrue(chance >= 0.0,
                    "던전 '" + dungeon.name() + "'의 각 gradeChance 값은 0 이상이어야 한다");
        }
    }

    // Feature: myrpg-gen1-mvp, Property 50: 모든 세대1 던전은 전체 장비를 드랍
    /**
     * 모든 세대1 던전은 무기 6종 전체와 방어구 4부위 전체를 드랍해야 한다.
     *
     * <p>세대1 던전의 weaponTypes는 6종(SWORD, AXE, SPEAR, DAGGER, STAFF, BOW) 전체를,
     * armorSlots는 4부위(HELMET, CHEST, GLOVES, BOOTS) 전체를 포함한다.
     *
     * <p><b>Validates: Requirements 27.1, 27.2, 27.3, 27.4, 27.5</b>
     */
    @Property(tries = 100)
    void allGen1DungeonsDropFullEquipment(@ForAll("dungeonProvider") final DungeonTemplate dungeon) {
        final Set<WeaponType> dungeonWeaponTypes =
                Set.copyOf(dungeon.weaponTypes());
        final Set<ArmorSlot> dungeonArmorSlots =
                Set.copyOf(dungeon.armorSlots());

        assertEquals(ALL_WEAPON_TYPES, dungeonWeaponTypes,
                "던전 '" + dungeon.name() + "'은 무기 6종 전체를 포함해야 한다");
        assertEquals(ALL_ARMOR_SLOTS, dungeonArmorSlots,
                "던전 '" + dungeon.name() + "'은 방어구 4부위 전체를 포함해야 한다");
    }
}
