package com.myapps.web.myrpg.domain.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.ArmorSlot;
import com.myapps.web.myrpg.domain.model.DamageType;
import com.myapps.web.myrpg.domain.model.DropCategory;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.WeaponType;
import com.myapps.web.myrpg.domain.model.vo.DropResult;
import com.myapps.web.myrpg.domain.model.vo.RolledArmor;
import com.myapps.web.myrpg.domain.model.vo.RolledWeapon;
import com.myapps.web.myrpg.domain.random.FixedRandomSource;
import com.myapps.web.myrpg.domain.template.ArmorTemplate;
import com.myapps.web.myrpg.domain.template.DungeonSpawn;
import com.myapps.web.myrpg.domain.template.DungeonTemplate;
import com.myapps.web.myrpg.domain.template.MonsterTemplate;
import com.myapps.web.myrpg.domain.template.SkillTemplate;
import com.myapps.web.myrpg.domain.template.WeaponTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DropService의 드랍 카테고리, 던전 풀 제약, 독립 인스턴스 관련 속성 기반 테스트.
 *
 * <p>상호 배타적 단일 롤, 몬스터 종류별 카테고리 제약, 던전 풀 내 선택,
 * 등급 분포 정합성, 드랍마다 독립 인스턴스를 검증한다.
 *
 * <p><b>Validates: Requirements 17.1, 17.2, 17.3, 17.4, 17.5, 17.6, 17.7,
 * 18.4, 25.4</b>
 */
class DropCategoryPoolPropertyTest {

    private static final long TEMPLATE_ID = 1L;
    private static final int DEFAULT_ITEM_LEVEL = 10;
    private static final int DEFAULT_BASE_ATTACK = 20;
    private static final int DEFAULT_BASE_SPEED = 10;
    private static final int DEFAULT_BASE_CRITICAL = 5;
    private static final int DEFAULT_BASE_VALUE = 100;
    private static final int DEFAULT_BASE_DEFENSE = 15;

    // --- Providers ---

    /**
     * [0.0, 1.0) 범위의 카테고리 롤 값을 생성하는 Provider.
     *
     * @return double Arbitrary
     */
    @Provide
    Arbitrary<Double> categoryRolls() {
        return Arbitraries.doubles().between(0.0, true, 1.0, false);
    }

    /**
     * 등급 롤 값을 생성하는 Provider.
     *
     * @return double Arbitrary
     */
    @Provide
    Arbitrary<Double> gradeRolls() {
        return Arbitraries.doubles().between(0.0, true, 1.0, false);
    }

    /**
     * 모든 등급 값을 생성하는 Provider.
     *
     * @return Grade Arbitrary
     */
    @Provide
    Arbitrary<Grade> grades() {
        return Arbitraries.of(Grade.values());
    }

    /**
     * 1~3개의 무기 타입 부분집합을 생성하는 Provider.
     *
     * @return WeaponType 리스트 Arbitrary
     */
    @Provide
    Arbitrary<List<WeaponType>> weaponTypeSubsets() {
        return Arbitraries.of(WeaponType.values())
                .list().ofMinSize(1).ofMaxSize(3).uniqueElements();
    }

    /**
     * 1~4개의 방어구 부위 부분집합을 생성하는 Provider.
     *
     * @return ArmorSlot 리스트 Arbitrary
     */
    @Provide
    Arbitrary<List<ArmorSlot>> armorSlotSubsets() {
        return Arbitraries.of(ArmorSlot.values())
                .list().ofMinSize(1).ofMaxSize(4).uniqueElements();
    }

    /**
     * 유효한 gradeChance 맵(합 1.0)을 생성하는 Provider.
     *
     * @return gradeChance 맵 Arbitrary
     */
    @Provide
    Arbitrary<Map<Grade, Double>> validGradeChances() {
        return Arbitraries.of(
                Map.of(Grade.COMMON, 0.5, Grade.UNCOMMON, 0.3,
                        Grade.RARE, 0.15, Grade.EPIC, 0.04, Grade.LEGENDARY, 0.01),
                Map.of(Grade.COMMON, 0.4, Grade.UNCOMMON, 0.35,
                        Grade.RARE, 0.15, Grade.EPIC, 0.07, Grade.LEGENDARY, 0.03),
                Map.of(Grade.COMMON, 0.6, Grade.UNCOMMON, 0.25,
                        Grade.RARE, 0.1, Grade.EPIC, 0.04, Grade.LEGENDARY, 0.01)
        );
    }

    // --- Helper Methods ---

    /**
     * 테스트용 일반 몬스터 템플릿을 생성한다.
     *
     * @return 일반 몬스터 템플릿
     */
    private MonsterTemplate normalMonster() {
        return new MonsterTemplate(1L, "슬라임", 50, 10, 5, 3,
                DamageType.PHYSICAL, 10, 5, false);
    }

    /**
     * 테스트용 보스 몬스터 템플릿을 생성한다.
     *
     * @return 보스 몬스터 템플릿
     */
    private MonsterTemplate bossMonster() {
        return new MonsterTemplate(2L, "드래곤", 500, 50, 30, 10,
                DamageType.MAGICAL, 100, 50, true);
    }

    /**
     * 지정된 무기 타입과 방어구 부위로 던전 템플릿을 생성한다.
     *
     * @param weaponTypes 무기 타입 목록
     * @param armorSlots  방어구 부위 목록
     * @param gradeChance 등급 확률 분포
     * @return 던전 템플릿
     */
    private DungeonTemplate buildDungeon(final List<WeaponType> weaponTypes,
                                         final List<ArmorSlot> armorSlots,
                                         final Map<Grade, Double> gradeChance) {
        return new DungeonTemplate(1L, "테스트던전", 1, 5, 1, 2L, 1,
                weaponTypes, armorSlots, gradeChance, 100,
                List.of(new DungeonSpawn(1L, 1, 5, 10)));
    }

    /**
     * 지정된 무기 타입 목록에 대응하는 무기 템플릿을 생성한다.
     *
     * @param weaponTypes 무기 타입 목록
     * @return 무기 템플릿 리스트
     */
    private List<WeaponTemplate> buildWeaponTemplates(
            final List<WeaponType> weaponTypes) {
        return weaponTypes.stream()
                .map(type -> new WeaponTemplate(TEMPLATE_ID, "무기_" + type.name(),
                        type, DEFAULT_BASE_ATTACK, DEFAULT_BASE_SPEED,
                        DEFAULT_BASE_CRITICAL, DEFAULT_BASE_VALUE))
                .toList();
    }

    /**
     * 지정된 방어구 부위 목록에 대응하는 방어구 템플릿을 생성한다.
     *
     * @param armorSlots 방어구 부위 목록
     * @return 방어구 템플릿 리스트
     */
    private List<ArmorTemplate> buildArmorTemplates(
            final List<ArmorSlot> armorSlots) {
        return armorSlots.stream()
                .map(slot -> new ArmorTemplate(TEMPLATE_ID, "방어구_" + slot.name(),
                        slot, DEFAULT_BASE_DEFENSE, DEFAULT_BASE_VALUE))
                .toList();
    }

    /**
     * 지정된 무기 타입 목록에 대응하는 스킬 템플릿을 생성한다.
     *
     * @param weaponTypes 무기 타입 목록
     * @return 스킬 템플릿 리스트
     */
    private List<SkillTemplate> buildSkillTemplates(
            final List<WeaponType> weaponTypes) {
        long id = 1L;
        return weaponTypes.stream()
                .map(type -> new SkillTemplate(id, "스킬_" + type.name(),
                        type, DamageType.PHYSICAL, 1.5, 10))
                .toList();
    }

    // --- Property 31: 드랍은 상호 배타적 단일 롤 ---

    // Feature: myrpg-gen1-mvp, Property 31: 드랍은 상호 배타적 단일 롤
    /**
     * rollDrop 결과에서 카테고리에 따라 정확히 하나의 필드만 유효하고
     * 나머지는 null인지 검증한다. 두 개 이상 동시 드랍은 불가.
     *
     * <p><b>Validates: Requirements 17.1, 17.7</b>
     *
     * @param categoryRoll 카테고리 결정 난수
     */
    @Property(tries = 100)
    void dropIsExclusiveSingleRoll(
            @ForAll("categoryRolls") final Double categoryRoll) {
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{categoryRoll, 0.5, 0.5, 0.5, 0.5, 0.5},
                new int[]{0, 0, 0, 0, 0}
        );
        final DropService dropService = new DropService(randomSource);
        final List<WeaponType> weaponTypes = List.of(WeaponType.SWORD);
        final List<ArmorSlot> armorSlots = List.of(ArmorSlot.HELMET);
        final Map<Grade, Double> gradeChance = Map.of(Grade.COMMON, 1.0);
        final DungeonTemplate dungeon = buildDungeon(
                weaponTypes, armorSlots, gradeChance);
        final MonsterTemplate monster = bossMonster();
        final List<WeaponTemplate> weapons = buildWeaponTemplates(weaponTypes);
        final List<ArmorTemplate> armors = buildArmorTemplates(armorSlots);
        final List<SkillTemplate> skills = buildSkillTemplates(weaponTypes);

        final DropResult result = dropService.rollDrop(
                monster, dungeon, skills, weapons, armors, DEFAULT_ITEM_LEVEL);

        assertNotNull(result.category(), "카테고리는 null이 아니어야 한다");
        switch (result.category()) {
            case NONE -> {
                assertNull(result.weapon(), "NONE일 때 weapon은 null");
                assertNull(result.armor(), "NONE일 때 armor는 null");
                assertNull(result.skillId(), "NONE일 때 skillId는 null");
            }
            case WEAPON -> {
                assertNotNull(result.weapon(), "WEAPON일 때 weapon은 non-null");
                assertNull(result.armor(), "WEAPON일 때 armor는 null");
                assertNull(result.skillId(), "WEAPON일 때 skillId는 null");
            }
            case ARMOR -> {
                assertNull(result.weapon(), "ARMOR일 때 weapon은 null");
                assertNotNull(result.armor(), "ARMOR일 때 armor는 non-null");
                assertNull(result.skillId(), "ARMOR일 때 skillId는 null");
            }
            case SKILL_BOOK -> {
                assertNull(result.weapon(), "SKILL_BOOK일 때 weapon은 null");
                assertNull(result.armor(), "SKILL_BOOK일 때 armor는 null");
                assertNotNull(result.skillId(), "SKILL_BOOK일 때 skillId는 non-null");
            }
        }
    }

    // --- Property 32: 몬스터 종류별 카테고리 제약 ---

    // Feature: myrpg-gen1-mvp, Property 32: 몬스터 종류별 카테고리 제약
    /**
     * 일반 몬스터(boss=false)는 ARMOR 또는 NONE만 드랍하고,
     * 보스 몬스터(boss=true)는 WEAPON, SKILL_BOOK, NONE만 드랍하는지 검증한다.
     *
     * <p><b>Validates: Requirements 17.2, 17.3</b>
     *
     * @param categoryRoll 카테고리 결정 난수
     */
    @Property(tries = 100)
    void normalMonsterCanOnlyDropArmorOrNone(
            @ForAll("categoryRolls") final Double categoryRoll) {
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{categoryRoll, 0.5, 0.5, 0.5, 0.5, 0.5},
                new int[]{0, 0, 0, 0, 0}
        );
        final DropService dropService = new DropService(randomSource);
        final List<WeaponType> weaponTypes = List.of(WeaponType.SWORD);
        final List<ArmorSlot> armorSlots = List.of(ArmorSlot.HELMET);
        final Map<Grade, Double> gradeChance = Map.of(Grade.COMMON, 1.0);
        final DungeonTemplate dungeon = buildDungeon(
                weaponTypes, armorSlots, gradeChance);
        final List<WeaponTemplate> weapons = buildWeaponTemplates(weaponTypes);
        final List<ArmorTemplate> armors = buildArmorTemplates(armorSlots);
        final List<SkillTemplate> skills = buildSkillTemplates(weaponTypes);

        final DropResult result = dropService.rollDrop(
                normalMonster(), dungeon, skills, weapons, armors,
                DEFAULT_ITEM_LEVEL);

        assertTrue(
                result.category() == DropCategory.ARMOR
                        || result.category() == DropCategory.NONE,
                "일반 몬스터는 ARMOR 또는 NONE만 가능, 실제: "
                        + result.category());
    }

    // Feature: myrpg-gen1-mvp, Property 32: 몬스터 종류별 카테고리 제약
    /**
     * 보스 몬스터(boss=true)는 WEAPON, SKILL_BOOK, NONE만 드랍하는지 검증한다.
     *
     * <p><b>Validates: Requirements 17.2, 17.3</b>
     *
     * @param categoryRoll 카테고리 결정 난수
     */
    @Property(tries = 100)
    void bossMonsterCanOnlyDropWeaponSkillBookOrNone(
            @ForAll("categoryRolls") final Double categoryRoll) {
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{categoryRoll, 0.5, 0.5, 0.5, 0.5, 0.5},
                new int[]{0, 0, 0, 0, 0}
        );
        final DropService dropService = new DropService(randomSource);
        final List<WeaponType> weaponTypes = List.of(WeaponType.SWORD);
        final List<ArmorSlot> armorSlots = List.of(ArmorSlot.HELMET);
        final Map<Grade, Double> gradeChance = Map.of(Grade.COMMON, 1.0);
        final DungeonTemplate dungeon = buildDungeon(
                weaponTypes, armorSlots, gradeChance);
        final List<WeaponTemplate> weapons = buildWeaponTemplates(weaponTypes);
        final List<ArmorTemplate> armors = buildArmorTemplates(armorSlots);
        final List<SkillTemplate> skills = buildSkillTemplates(weaponTypes);

        final DropResult result = dropService.rollDrop(
                bossMonster(), dungeon, skills, weapons, armors,
                DEFAULT_ITEM_LEVEL);

        assertTrue(
                result.category() == DropCategory.WEAPON
                        || result.category() == DropCategory.SKILL_BOOK
                        || result.category() == DropCategory.NONE,
                "보스 몬스터는 WEAPON, SKILL_BOOK, NONE만 가능, 실제: "
                        + result.category());
    }

    // --- Property 33: 드랍 세부 롤은 던전 풀 내에서 선택 ---

    // Feature: myrpg-gen1-mvp, Property 33: 드랍 세부 롤은 던전 풀 내에서 선택
    /**
     * WEAPON 드랍 시 무기의 weaponType이 던전 weaponTypes에 포함되는지 검증한다.
     *
     * <p><b>Validates: Requirements 17.4, 17.5, 17.6</b>
     *
     * @param weaponTypes 던전 무기 타입 목록
     */
    @Property(tries = 100)
    void weaponDropTypeIsWithinDungeonPool(
            @ForAll("weaponTypeSubsets") final List<WeaponType> weaponTypes) {
        final double weaponCategoryRoll = 0.01;
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{weaponCategoryRoll, 0.5, 0.5, 0.5, 0.5, 0.5},
                new int[]{0, 0, 0, 0, 0}
        );
        final DropService dropService = new DropService(randomSource);
        final List<ArmorSlot> armorSlots = List.of(ArmorSlot.HELMET);
        final Map<Grade, Double> gradeChance = Map.of(Grade.COMMON, 1.0);
        final DungeonTemplate dungeon = buildDungeon(
                weaponTypes, armorSlots, gradeChance);
        final List<WeaponTemplate> weapons = buildWeaponTemplates(weaponTypes);
        final List<ArmorTemplate> armors = buildArmorTemplates(armorSlots);
        final List<SkillTemplate> skills = buildSkillTemplates(weaponTypes);

        final DropResult result = dropService.rollDrop(
                bossMonster(), dungeon, skills, weapons, armors,
                DEFAULT_ITEM_LEVEL);

        assertEquals(DropCategory.WEAPON, result.category());
        assertNotNull(result.weapon());
        assertTrue(weaponTypes.contains(result.weapon().weaponType()),
                "드랍된 무기 타입 " + result.weapon().weaponType()
                        + "이 던전 풀 " + weaponTypes + "에 포함되어야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 33: 드랍 세부 롤은 던전 풀 내에서 선택
    /**
     * ARMOR 드랍 시 방어구의 slot이 던전 armorSlots에 포함되는지 검증한다.
     *
     * <p><b>Validates: Requirements 17.4, 17.5, 17.6</b>
     *
     * @param armorSlots 던전 방어구 부위 목록
     */
    @Property(tries = 100)
    void armorDropSlotIsWithinDungeonPool(
            @ForAll("armorSlotSubsets") final List<ArmorSlot> armorSlots) {
        final double armorCategoryRoll = 0.01;
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{armorCategoryRoll, 0.5, 0.5, 0.5, 0.5, 0.5},
                new int[]{0, 0, 0, 0, 0}
        );
        final DropService dropService = new DropService(randomSource);
        final List<WeaponType> weaponTypes = List.of(WeaponType.SWORD);
        final Map<Grade, Double> gradeChance = Map.of(Grade.COMMON, 1.0);
        final DungeonTemplate dungeon = buildDungeon(
                weaponTypes, armorSlots, gradeChance);
        final List<WeaponTemplate> weapons = buildWeaponTemplates(weaponTypes);
        final List<ArmorTemplate> armors = buildArmorTemplates(armorSlots);
        final List<SkillTemplate> skills = buildSkillTemplates(weaponTypes);

        final DropResult result = dropService.rollDrop(
                normalMonster(), dungeon, skills, weapons, armors,
                DEFAULT_ITEM_LEVEL);

        assertEquals(DropCategory.ARMOR, result.category());
        assertNotNull(result.armor());
        assertTrue(armorSlots.contains(result.armor().slot()),
                "드랍된 방어구 부위 " + result.armor().slot()
                        + "이 던전 풀 " + armorSlots + "에 포함되어야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 33: 드랍 세부 롤은 던전 풀 내에서 선택
    /**
     * SKILL_BOOK 드랍 시 skillId가 availableSkills 목록에 포함되는지 검증한다.
     *
     * <p><b>Validates: Requirements 17.4, 17.5, 17.6</b>
     *
     * @param weaponTypes 던전 무기 타입 목록 (스킬 풀 결정)
     */
    @Property(tries = 100)
    void skillBookDropIsWithinAvailableSkills(
            @ForAll("weaponTypeSubsets") final List<WeaponType> weaponTypes) {
        final double skillBookCategoryRoll = 0.20;
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{skillBookCategoryRoll, 0.5, 0.5, 0.5},
                new int[]{0, 0, 0, 0}
        );
        final DropService dropService = new DropService(randomSource);
        final List<ArmorSlot> armorSlots = List.of(ArmorSlot.HELMET);
        final Map<Grade, Double> gradeChance = Map.of(Grade.COMMON, 1.0);
        final DungeonTemplate dungeon = buildDungeon(
                weaponTypes, armorSlots, gradeChance);
        final List<WeaponTemplate> weapons = buildWeaponTemplates(weaponTypes);
        final List<ArmorTemplate> armors = buildArmorTemplates(armorSlots);
        final List<SkillTemplate> skills = buildSkillTemplates(weaponTypes);
        final List<Long> skillIds = skills.stream()
                .map(SkillTemplate::id).toList();

        final DropResult result = dropService.rollDrop(
                bossMonster(), dungeon, skills, weapons, armors,
                DEFAULT_ITEM_LEVEL);

        assertEquals(DropCategory.SKILL_BOOK, result.category());
        assertNotNull(result.skillId());
        assertTrue(skillIds.contains(result.skillId()),
                "드랍된 스킬 ID " + result.skillId()
                        + "이 사용 가능 스킬 " + skillIds + "에 포함되어야 한다");
    }

    // --- Property 34: 등급 분포 정합성 ---

    // Feature: myrpg-gen1-mvp, Property 34: 등급 분포 정합성(rollGrade 유효 등급 반환)
    /**
     * rollGrade가 항상 유효한 Grade 열거값을 반환하며,
     * 반환된 등급이 dungeon gradeChance에 존재하거나 LEGENDARY 폴백인지 검증한다.
     *
     * <p><b>Validates: Requirements 18.4</b>
     *
     * @param gradeRoll   등급 결정 난수
     * @param gradeChance 유효한 등급 확률 분포
     */
    @Property(tries = 100)
    void rollGradeReturnsValidGrade(
            @ForAll("gradeRolls") final Double gradeRoll,
            @ForAll("validGradeChances") final Map<Grade, Double> gradeChance) {
        final FixedRandomSource randomSource = new FixedRandomSource(gradeRoll);
        final DropService dropService = new DropService(randomSource);
        final DungeonTemplate dungeon = buildDungeon(
                List.of(WeaponType.SWORD), List.of(ArmorSlot.HELMET),
                gradeChance);

        final Grade result = dropService.rollGrade(dungeon);

        assertNotNull(result, "rollGrade 결과는 null이 아니어야 한다");
        assertTrue(
                Arrays.asList(Grade.values()).contains(result),
                "rollGrade 결과가 유효한 Grade 열거값이어야 한다: " + result);
        assertTrue(
                gradeChance.containsKey(result)
                        || result == Grade.LEGENDARY,
                "등급 " + result + "은 gradeChance에 존재하거나 LEGENDARY여야 한다");
    }

    // --- Property 47: 드랍마다 독립 인스턴스 ---

    // Feature: myrpg-gen1-mvp, Property 47: 드랍마다 독립 인스턴스
    /**
     * 동일 무기 템플릿을 다른 난수 상태로 두 번 빌드하면
     * 각각 독립적인 능력치를 가진 별개 인스턴스가 생성되는지 검증한다.
     *
     * <p><b>Validates: Requirements 25.4</b>
     *
     * @param grade 장비 등급
     */
    @Property(tries = 100)
    void weaponInstancesAreIndependent(
            @ForAll("grades") final Grade grade) {
        final FixedRandomSource randomSource1 = new FixedRandomSource(
                0.3, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1
        );
        final FixedRandomSource randomSource2 = new FixedRandomSource(
                0.3, 0.1, 0.9, 0.1, 0.9, 0.1, 0.9, 0.1, 0.9, 0.1, 0.9
        );
        final DropService dropService1 = new DropService(randomSource1);
        final DropService dropService2 = new DropService(randomSource2);
        final WeaponTemplate template = new WeaponTemplate(
                TEMPLATE_ID, "테스트검", WeaponType.SWORD,
                DEFAULT_BASE_ATTACK, DEFAULT_BASE_SPEED,
                DEFAULT_BASE_CRITICAL, DEFAULT_BASE_VALUE);

        final RolledWeapon weapon1 = dropService1.buildWeaponInstance(
                template, grade, DEFAULT_ITEM_LEVEL);
        final RolledWeapon weapon2 = dropService2.buildWeaponInstance(
                template, grade, DEFAULT_ITEM_LEVEL);

        assertEquals(weapon1.templateId(), weapon2.templateId(),
                "동일 템플릿 ID여야 한다");
        assertEquals(weapon1.weaponType(), weapon2.weaponType(),
                "동일 무기 타입이어야 한다");
        assertTrue(
                !weapon1.stats().equals(weapon2.stats()),
                "서로 다른 난수로 생성된 인스턴스는 능력치가 달라야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 47: 드랍마다 독립 인스턴스
    /**
     * 동일 방어구 템플릿을 다른 난수 상태로 두 번 빌드하면
     * 각각 독립적인 능력치를 가진 별개 인스턴스가 생성되는지 검증한다.
     *
     * <p><b>Validates: Requirements 25.4</b>
     *
     * @param grade 장비 등급
     */
    @Property(tries = 100)
    void armorInstancesAreIndependent(
            @ForAll("grades") final Grade grade) {
        final FixedRandomSource randomSource1 = new FixedRandomSource(
                0.3, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1
        );
        final FixedRandomSource randomSource2 = new FixedRandomSource(
                0.3, 0.1, 0.9, 0.1, 0.9, 0.1, 0.9, 0.1, 0.9, 0.1, 0.9
        );
        final DropService dropService1 = new DropService(randomSource1);
        final DropService dropService2 = new DropService(randomSource2);
        final ArmorTemplate template = new ArmorTemplate(
                TEMPLATE_ID, "테스트투구", ArmorSlot.HELMET,
                DEFAULT_BASE_DEFENSE, DEFAULT_BASE_VALUE);

        final RolledArmor armor1 = dropService1.buildArmorInstance(
                template, grade, DEFAULT_ITEM_LEVEL);
        final RolledArmor armor2 = dropService2.buildArmorInstance(
                template, grade, DEFAULT_ITEM_LEVEL);

        assertEquals(armor1.templateId(), armor2.templateId(),
                "동일 템플릿 ID여야 한다");
        assertEquals(armor1.slot(), armor2.slot(),
                "동일 방어구 부위여야 한다");
        assertTrue(
                !armor1.stats().equals(armor2.stats()),
                "서로 다른 난수로 생성된 인스턴스는 능력치가 달라야 한다");
    }
}
