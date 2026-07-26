package com.myapps.web.myrpg.application.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.myapps.web.myrpg.domain.model.ArmorSlot;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.WeaponType;
import com.myapps.web.myrpg.domain.template.DungeonTemplate;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MasterDataLoader 세대1 콘텐츠 볼륨 및 gradeChance 분포 검증 테스트.
 *
 * <p>실제 JSON 파일을 로딩하여 세대1 콘텐츠 볼륨(던전 3·무기 6·방어구 4·스킬 6·몬스터 9)과
 * 각 던전별 gradeChance 분포값 및 합 = 1.0을 검증한다.
 */
class MasterDataLoaderContentVolumeTest {

    private static final double GRADE_CHANCE_TOLERANCE = 1e-6;

    private static MasterDataLoader loader;

    @BeforeAll
    static void setUp() {
        loader = new MasterDataLoader(new ObjectMapper());
        loader.load();
    }

    // ─── 콘텐츠 볼륨 검증 ───────────────────────────────────────────────────

    @Test
    void should_have3Dungeons_when_gen1DataLoaded() {
        final List<DungeonTemplate> dungeons = loader.allDungeons();

        assertEquals(3, dungeons.size());
    }

    @Test
    void should_have6Weapons_when_gen1DataLoaded() {
        final Set<WeaponType> coveredTypes = Arrays.stream(WeaponType.values())
                .filter(type -> {
                    try {
                        loader.findWeaponTemplate(type.ordinal() + 1L);
                        return true;
                    } catch (final Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toSet());

        assertEquals(6, coveredTypes.size(), "세대1은 무기 6종(WeaponType별 1개)을 포함해야 한다");
    }

    @Test
    void should_have4Armors_when_gen1DataLoaded() {
        final Set<ArmorSlot> coveredSlots = Arrays.stream(ArmorSlot.values())
                .filter(slot -> {
                    try {
                        loader.findArmorTemplate(slot.ordinal() + 1L);
                        return true;
                    } catch (final Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toSet());

        assertEquals(4, coveredSlots.size(), "세대1은 방어구 4종(ArmorSlot별 1개)을 포함해야 한다");
    }

    @Test
    void should_have6Skills_when_gen1DataLoaded() {
        int skillCount = 0;
        for (final WeaponType type : WeaponType.values()) {
            skillCount += loader.skillsForWeaponType(type).size();
        }

        assertEquals(6, skillCount, "세대1은 스킬 6종(WeaponType별 1개)을 포함해야 한다");
    }

    @Test
    void should_have9Monsters_when_gen1DataLoaded() {
        int monsterCount = 0;
        for (long id = 1; id <= 9; id++) {
            try {
                loader.findMonster(id);
                monsterCount++;
            } catch (final Exception e) {
                // id가 존재하지 않으면 카운트하지 않음
            }
        }

        assertEquals(9, monsterCount, "세대1은 몬스터 9종(일반 6 + 보스 3)을 포함해야 한다");
    }

    // ─── 던전별 gradeChance 분포값 검증 ─────────────────────────────────────

    @Test
    void should_haveCorrectGradeChance_when_forestDungeon() {
        final DungeonTemplate forest = loader.findDungeon(1L);
        final Map<Grade, Double> chance = forest.gradeChance();

        assertEquals(0.700, chance.get(Grade.COMMON), GRADE_CHANCE_TOLERANCE);
        assertEquals(0.220, chance.get(Grade.UNCOMMON), GRADE_CHANCE_TOLERANCE);
        assertEquals(0.060, chance.get(Grade.RARE), GRADE_CHANCE_TOLERANCE);
        assertEquals(0.018, chance.get(Grade.EPIC), GRADE_CHANCE_TOLERANCE);
        assertEquals(0.002, chance.get(Grade.LEGENDARY), GRADE_CHANCE_TOLERANCE);
    }

    @Test
    void should_haveCorrectGradeChance_when_mineDungeon() {
        final DungeonTemplate mine = loader.findDungeon(2L);
        final Map<Grade, Double> chance = mine.gradeChance();

        assertEquals(0.550, chance.get(Grade.COMMON), GRADE_CHANCE_TOLERANCE);
        assertEquals(0.280, chance.get(Grade.UNCOMMON), GRADE_CHANCE_TOLERANCE);
        assertEquals(0.120, chance.get(Grade.RARE), GRADE_CHANCE_TOLERANCE);
        assertEquals(0.040, chance.get(Grade.EPIC), GRADE_CHANCE_TOLERANCE);
        assertEquals(0.010, chance.get(Grade.LEGENDARY), GRADE_CHANCE_TOLERANCE);
    }

    @Test
    void should_haveCorrectGradeChance_when_towerDungeon() {
        final DungeonTemplate tower = loader.findDungeon(3L);
        final Map<Grade, Double> chance = tower.gradeChance();

        assertEquals(0.400, chance.get(Grade.COMMON), GRADE_CHANCE_TOLERANCE);
        assertEquals(0.300, chance.get(Grade.UNCOMMON), GRADE_CHANCE_TOLERANCE);
        assertEquals(0.180, chance.get(Grade.RARE), GRADE_CHANCE_TOLERANCE);
        assertEquals(0.090, chance.get(Grade.EPIC), GRADE_CHANCE_TOLERANCE);
        assertEquals(0.030, chance.get(Grade.LEGENDARY), GRADE_CHANCE_TOLERANCE);
    }

    @Test
    void should_haveGradeChanceSumEqualOne_when_eachDungeon() {
        for (final DungeonTemplate dungeon : loader.allDungeons()) {
            final double sum = dungeon.gradeChance().values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();

            assertTrue(Math.abs(sum - 1.0) < GRADE_CHANCE_TOLERANCE,
                    "던전 '" + dungeon.name() + "'의 gradeChance 합은 1.0이어야 한다 (실제: " + sum + ")");
        }
    }
}
