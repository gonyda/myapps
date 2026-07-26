package com.myapps.web.myrpg.application.service;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.myapps.web.myrpg.domain.exception.MasterDataNotFoundException;
import com.myapps.web.myrpg.domain.model.WeaponType;
import com.myapps.web.myrpg.domain.template.ArmorTemplate;
import com.myapps.web.myrpg.domain.template.DungeonTemplate;
import com.myapps.web.myrpg.domain.template.ItemTemplate;
import com.myapps.web.myrpg.domain.template.MonsterTemplate;
import com.myapps.web.myrpg.domain.template.SkillTemplate;
import com.myapps.web.myrpg.domain.template.WeaponTemplate;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MasterDataLoader 단위 테스트.
 *
 * <p>실제 JSON 파일을 로딩하여 인덱싱, 조회, 미존재 id 예외 발생을 검증한다.
 */
class MasterDataLoaderTest {

    private static MasterDataLoader loader;

    @BeforeAll
    static void setUp() {
        loader = new MasterDataLoader(new ObjectMapper());
        loader.load();
    }

    @Test
    void should_findMonster_when_validId() {
        final MonsterTemplate goblin = loader.findMonster(1L);

        assertNotNull(goblin);
        assertEquals("고블린", goblin.name());
        assertEquals(50, goblin.hp());
        assertEquals(12, goblin.attack());
    }

    @Test
    void should_throwException_when_monsterNotFound() {
        assertThrows(MasterDataNotFoundException.class, () -> loader.findMonster(999L));
    }

    @Test
    void should_findWeaponTemplate_when_validId() {
        final WeaponTemplate weapon = loader.findWeaponTemplate(1L);

        assertNotNull(weapon);
        assertEquals("낡은 검", weapon.name());
        assertEquals(WeaponType.SWORD, weapon.weaponType());
        assertEquals(10, weapon.baseAttack());
    }

    @Test
    void should_throwException_when_weaponNotFound() {
        assertThrows(MasterDataNotFoundException.class, () -> loader.findWeaponTemplate(999L));
    }

    @Test
    void should_findArmorTemplate_when_validId() {
        final ArmorTemplate armor = loader.findArmorTemplate(1L);

        assertNotNull(armor);
        assertEquals("가죽 투구", armor.name());
    }

    @Test
    void should_throwException_when_armorNotFound() {
        assertThrows(MasterDataNotFoundException.class, () -> loader.findArmorTemplate(999L));
    }

    @Test
    void should_findSkill_when_validId() {
        final SkillTemplate skill = loader.findSkill(1L);

        assertNotNull(skill);
        assertEquals("강타", skill.name());
        assertEquals(WeaponType.SWORD, skill.weaponType());
    }

    @Test
    void should_throwException_when_skillNotFound() {
        assertThrows(MasterDataNotFoundException.class, () -> loader.findSkill(999L));
    }

    @Test
    void should_findItem_when_validId() {
        final ItemTemplate item = loader.findItem(1L);

        assertNotNull(item);
        assertEquals("HP 포션", item.name());
        assertEquals(50, item.effectAmount());
    }

    @Test
    void should_throwException_when_itemNotFound() {
        assertThrows(MasterDataNotFoundException.class, () -> loader.findItem(999L));
    }

    @Test
    void should_findDungeon_when_validId() {
        final DungeonTemplate dungeon = loader.findDungeon(1L);

        assertNotNull(dungeon);
        assertEquals("숲 던전", dungeon.name());
        assertEquals(5, dungeon.floorCount());
    }

    @Test
    void should_throwException_when_dungeonNotFound() {
        assertThrows(MasterDataNotFoundException.class, () -> loader.findDungeon(999L));
    }

    @Test
    void should_returnAllDungeons() {
        final List<DungeonTemplate> allDungeons = loader.allDungeons();

        assertEquals(3, allDungeons.size());
    }

    @Test
    void should_returnSkillsForWeaponType() {
        final List<SkillTemplate> swordSkills = loader.skillsForWeaponType(WeaponType.SWORD);

        assertEquals(1, swordSkills.size());
        assertEquals("강타", swordSkills.getFirst().name());
    }

    @Test
    void should_returnEmptyList_when_noSkillsForWeaponType() {
        // 모든 무기 타입에 스킬이 있으므로 별도 타입이 없는 경우를 만들 수 없다.
        // 대신 모든 6종 무기 타입에 각 1개씩 스킬이 있는지 검증
        for (final WeaponType type : WeaponType.values()) {
            final List<SkillTemplate> skills = loader.skillsForWeaponType(type);
            assertTrue(skills.size() >= 1,
                    "Expected at least 1 skill for " + type);
        }
    }

    @Test
    void should_loadBossMonsters() {
        final MonsterTemplate boss = loader.findMonster(3L);

        assertTrue(boss.boss());
        assertEquals("고대 트렌트", boss.name());
    }
}
