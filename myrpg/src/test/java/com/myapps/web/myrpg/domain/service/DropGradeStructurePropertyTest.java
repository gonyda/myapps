package com.myapps.web.myrpg.domain.service;

import java.util.Map;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.ArmorSlot;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.WeaponType;
import com.myapps.web.myrpg.domain.model.vo.RolledArmor;
import com.myapps.web.myrpg.domain.model.vo.RolledWeapon;
import com.myapps.web.myrpg.domain.random.FixedRandomSource;
import com.myapps.web.myrpg.domain.template.ArmorTemplate;
import com.myapps.web.myrpg.domain.template.WeaponTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DropService의 등급 구조·표시명·파워 레벨 관련 속성 기반 테스트.
 *
 * <p>등급별 스킬슬롯 수, 능력치 개수 범위, 인스턴스 표시명 형식,
 * 유효 파워 레벨 산출 공식을 검증한다.
 *
 * <p><b>Validates: Requirements 14.2, 14.3, 14.5, 15.2, 16.3</b>
 */
class DropGradeStructurePropertyTest {

    private static final int ITEM_LEVEL_MIN = 1;
    private static final int ITEM_LEVEL_MAX = 100;
    private static final int BASE_ATTACK_MIN = 5;
    private static final int BASE_ATTACK_MAX = 100;
    private static final int BASE_DEFENSE_MIN = 5;
    private static final int BASE_DEFENSE_MAX = 80;
    private static final int BASE_SPEED_MIN = 1;
    private static final int BASE_SPEED_MAX = 20;
    private static final int BASE_CRITICAL_MIN = 0;
    private static final int BASE_CRITICAL_MAX = 15;
    private static final int BASE_VALUE_MIN = 10;
    private static final int BASE_VALUE_MAX = 500;
    private static final long TEMPLATE_ID = 1L;

    private static final Map<Grade, Integer> EXPECTED_SKILL_SLOTS = Map.of(
            Grade.COMMON, 1,
            Grade.UNCOMMON, 2,
            Grade.RARE, 3,
            Grade.EPIC, 4,
            Grade.LEGENDARY, 5
    );

    private static final Map<Grade, String> GRADE_DISPLAY_LABELS = Map.of(
            Grade.COMMON, "일반",
            Grade.UNCOMMON, "고급",
            Grade.RARE, "희귀",
            Grade.EPIC, "영웅",
            Grade.LEGENDARY, "전설"
    );

    private static final Map<Grade, Integer> EXPECTED_LEVEL_BONUS = Map.of(
            Grade.COMMON, 0,
            Grade.UNCOMMON, 2,
            Grade.RARE, 5,
            Grade.EPIC, 8,
            Grade.LEGENDARY, 10
    );

    // --- Providers ---

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
     * 유효 아이템 레벨을 생성하는 Provider.
     *
     * @return 아이템 레벨 Arbitrary (1~100)
     */
    @Provide
    Arbitrary<Integer> itemLevels() {
        return Arbitraries.integers().between(ITEM_LEVEL_MIN, ITEM_LEVEL_MAX);
    }

    /**
     * [0.0, 1.0) 범위의 임의 난수 값을 생성하는 Provider.
     *
     * @return double Arbitrary
     */
    @Provide
    Arbitrary<Double> rollValues() {
        return Arbitraries.doubles().between(0.0, true, 1.0, false);
    }

    /**
     * 임의 무기 타입을 생성하는 Provider.
     *
     * @return WeaponType Arbitrary
     */
    @Provide
    Arbitrary<WeaponType> weaponTypes() {
        return Arbitraries.of(WeaponType.values());
    }

    /**
     * 임의 방어구 부위를 생성하는 Provider.
     *
     * @return ArmorSlot Arbitrary
     */
    @Provide
    Arbitrary<ArmorSlot> armorSlots() {
        return Arbitraries.of(ArmorSlot.values());
    }

    /**
     * 임의 템플릿 이름 문자열을 생성하는 Provider.
     *
     * @return 템플릿 이름 Arbitrary
     */
    @Provide
    Arbitrary<String> templateNames() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
    }

    // --- Property 24: 등급별 무기 스킬슬롯 수 ---

    // Feature: myrpg-gen1-mvp, Property 24: 등급별 무기 스킬슬롯 수
    /**
     * slotCount(grade)가 등급에 따라 COMMON=1, UNCOMMON=2, RARE=3, EPIC=4, LEGENDARY=5를 반환하는지 검증한다.
     *
     * @param grade 검증할 등급
     */
    @Property(tries = 100)
    void slotCountMatchesGradeDefinition(@ForAll("grades") final Grade grade) {
        final FixedRandomSource randomSource = new FixedRandomSource(0.5);
        final DropService dropService = new DropService(randomSource);

        final int actual = dropService.slotCount(grade);
        final int expected = EXPECTED_SKILL_SLOTS.get(grade);

        assertEquals(expected, actual,
                grade + " 등급의 스킬슬롯 수는 " + expected + "이어야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 24: 등급별 무기 스킬슬롯 수
    /**
     * buildWeaponInstance로 생성된 무기의 skillSlots가 등급에 따른 slotCount와 일치하는지 검증한다.
     *
     * @param grade      검증할 등급
     * @param weaponType 무기 타입
     */
    @Property(tries = 100)
    void weaponInstanceSkillSlotsMatchesGrade(@ForAll("grades") final Grade grade,
                                              @ForAll("weaponTypes") final WeaponType weaponType) {
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{0.5},
                new int[]{0}
        );
        final DropService dropService = new DropService(randomSource);
        final WeaponTemplate template = new WeaponTemplate(
                TEMPLATE_ID, "테스트검", weaponType, 20, 10, 5, 100);

        final RolledWeapon weapon = dropService.buildWeaponInstance(template, grade, 5);

        assertEquals(EXPECTED_SKILL_SLOTS.get(grade), weapon.skillSlots(),
                grade + " 등급 무기의 skillSlots는 " + EXPECTED_SKILL_SLOTS.get(grade) + "이어야 한다");
    }

    // --- Property 25: 등급별 능력치 개수 범위 ---

    // Feature: myrpg-gen1-mvp, Property 25: 등급별 능력치 개수 범위
    /**
     * COMMON 등급의 rollStatCount는 랜덤 값에 관계없이 항상 1을 반환하는지 검증한다.
     *
     * @param roll 난수 값 (0.0~1.0 미만)
     */
    @Property(tries = 100)
    void commonGradeAlwaysReturnsOneStatCount(@ForAll("rollValues") final Double roll) {
        final FixedRandomSource randomSource = new FixedRandomSource(roll);
        final DropService dropService = new DropService(randomSource);

        final int actual = dropService.rollStatCount(Grade.COMMON);

        assertEquals(1, actual, "COMMON 등급은 항상 능력치 1개여야 한다 (roll=" + roll + ")");
    }

    // Feature: myrpg-gen1-mvp, Property 25: 등급별 능력치 개수 범위
    /**
     * UNCOMMON 등급의 rollStatCount가 roll < 0.6이면 1, roll >= 0.6이면 2를 반환하는지 검증한다.
     *
     * @param roll 난수 값
     */
    @Property(tries = 100)
    void uncommonGradeStatCountFollowsProbability(@ForAll("rollValues") final Double roll) {
        final FixedRandomSource randomSource = new FixedRandomSource(roll);
        final DropService dropService = new DropService(randomSource);

        final int actual = dropService.rollStatCount(Grade.UNCOMMON);
        final int expected = roll < 0.6 ? 1 : 2;

        assertEquals(expected, actual,
                "UNCOMMON roll=" + roll + " → 능력치 " + expected + "개 기대, 실제: " + actual);
    }

    // Feature: myrpg-gen1-mvp, Property 25: 등급별 능력치 개수 범위
    /**
     * RARE 등급의 rollStatCount가 roll < 0.6이면 2, roll >= 0.6이면 3을 반환하는지 검증한다.
     *
     * @param roll 난수 값
     */
    @Property(tries = 100)
    void rareGradeStatCountFollowsProbability(@ForAll("rollValues") final Double roll) {
        final FixedRandomSource randomSource = new FixedRandomSource(roll);
        final DropService dropService = new DropService(randomSource);

        final int actual = dropService.rollStatCount(Grade.RARE);
        final int expected = roll < 0.6 ? 2 : 3;

        assertEquals(expected, actual,
                "RARE roll=" + roll + " → 능력치 " + expected + "개 기대, 실제: " + actual);
    }

    // Feature: myrpg-gen1-mvp, Property 25: 등급별 능력치 개수 범위
    /**
     * EPIC 등급의 rollStatCount가 roll < 0.6이면 3, roll >= 0.6이면 4를 반환하는지 검증한다.
     *
     * @param roll 난수 값
     */
    @Property(tries = 100)
    void epicGradeStatCountFollowsProbability(@ForAll("rollValues") final Double roll) {
        final FixedRandomSource randomSource = new FixedRandomSource(roll);
        final DropService dropService = new DropService(randomSource);

        final int actual = dropService.rollStatCount(Grade.EPIC);
        final int expected = roll < 0.6 ? 3 : 4;

        assertEquals(expected, actual,
                "EPIC roll=" + roll + " → 능력치 " + expected + "개 기대, 실제: " + actual);
    }

    // Feature: myrpg-gen1-mvp, Property 25: 등급별 능력치 개수 범위
    /**
     * LEGENDARY 등급의 rollStatCount가 roll < 0.5이면 4, roll >= 0.5이면 5를 반환하는지 검증한다.
     *
     * @param roll 난수 값
     */
    @Property(tries = 100)
    void legendaryGradeStatCountFollowsProbability(@ForAll("rollValues") final Double roll) {
        final FixedRandomSource randomSource = new FixedRandomSource(roll);
        final DropService dropService = new DropService(randomSource);

        final int actual = dropService.rollStatCount(Grade.LEGENDARY);
        final int expected = roll < 0.5 ? 4 : 5;

        assertEquals(expected, actual,
                "LEGENDARY roll=" + roll + " → 능력치 " + expected + "개 기대, 실제: " + actual);
    }

    // Feature: myrpg-gen1-mvp, Property 25: 등급별 능력치 개수 범위
    /**
     * 모든 등급에 대해 rollStatCount가 유효 범위 내(1~5)에 있는지 검증한다.
     *
     * @param grade 검증할 등급
     * @param roll  난수 값
     */
    @Property(tries = 100)
    void statCountIsAlwaysWithinValidRange(@ForAll("grades") final Grade grade,
                                           @ForAll("rollValues") final Double roll) {
        final FixedRandomSource randomSource = new FixedRandomSource(roll);
        final DropService dropService = new DropService(randomSource);

        final int actual = dropService.rollStatCount(grade);

        assertTrue(actual >= 1 && actual <= 5,
                grade + " 등급의 능력치 개수(" + actual + ")는 1~5 범위여야 한다");
    }

    // --- Property 26: 인스턴스 표시명 형식 ---

    // Feature: myrpg-gen1-mvp, Property 26: 인스턴스 표시명 형식
    /**
     * buildWeaponInstance로 생성된 무기의 표시명이 "[등급라벨] 템플릿명" 형식인지 검증한다.
     *
     * @param grade        검증할 등급
     * @param templateName 임의 템플릿 이름
     * @param weaponType   무기 타입
     */
    @Property(tries = 100)
    void weaponDisplayNameFollowsFormat(@ForAll("grades") final Grade grade,
                                        @ForAll("templateNames") final String templateName,
                                        @ForAll("weaponTypes") final WeaponType weaponType) {
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{0.5},
                new int[]{0}
        );
        final DropService dropService = new DropService(randomSource);
        final WeaponTemplate template = new WeaponTemplate(
                TEMPLATE_ID, templateName, weaponType, 20, 10, 5, 100);

        final RolledWeapon weapon = dropService.buildWeaponInstance(template, grade, 5);

        final String expectedLabel = GRADE_DISPLAY_LABELS.get(grade);
        final String expectedName = "[" + expectedLabel + "] " + templateName;

        assertEquals(expectedName, weapon.displayName(),
                grade + " 등급 무기 표시명은 '[" + expectedLabel + "] " + templateName + "' 형식이어야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 26: 인스턴스 표시명 형식
    /**
     * buildArmorInstance로 생성된 방어구의 표시명이 "[등급라벨] 템플릿명" 형식인지 검증한다.
     *
     * @param grade        검증할 등급
     * @param templateName 임의 템플릿 이름
     * @param armorSlot    방어구 부위
     */
    @Property(tries = 100)
    void armorDisplayNameFollowsFormat(@ForAll("grades") final Grade grade,
                                       @ForAll("templateNames") final String templateName,
                                       @ForAll("armorSlots") final ArmorSlot armorSlot) {
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{0.5},
                new int[]{0}
        );
        final DropService dropService = new DropService(randomSource);
        final ArmorTemplate template = new ArmorTemplate(
                TEMPLATE_ID, templateName, armorSlot, 15, 80);

        final RolledArmor armor = dropService.buildArmorInstance(template, grade, 5);

        final String expectedLabel = GRADE_DISPLAY_LABELS.get(grade);
        final String expectedName = "[" + expectedLabel + "] " + templateName;

        assertEquals(expectedName, armor.displayName(),
                grade + " 등급 방어구 표시명은 '[" + expectedLabel + "] " + templateName + "' 형식이어야 한다");
    }

    // --- Property 27: 유효 파워 레벨 산출 ---

    // Feature: myrpg-gen1-mvp, Property 27: 유효 파워 레벨 산출
    /**
     * effectivePowerLevel(itemLevel, grade)가 itemLevel + grade.getLevelBonus()를 반환하는지 검증한다.
     *
     * @param grade     검증할 등급
     * @param itemLevel 아이템 레벨
     */
    @Property(tries = 100)
    void effectivePowerLevelMatchesFormula(@ForAll("grades") final Grade grade,
                                           @ForAll("itemLevels") final Integer itemLevel) {
        final FixedRandomSource randomSource = new FixedRandomSource(0.5);
        final DropService dropService = new DropService(randomSource);

        final int actual = dropService.effectivePowerLevel(itemLevel, grade);
        final int expected = itemLevel + grade.getLevelBonus();

        assertEquals(expected, actual,
                "effectivePowerLevel(" + itemLevel + ", " + grade + ") = "
                        + itemLevel + " + " + grade.getLevelBonus() + " = " + expected);
    }

    // Feature: myrpg-gen1-mvp, Property 27: 유효 파워 레벨 산출
    /**
     * 각 등급의 레벨 보너스가 사양과 일치하는지 검증한다.
     * COMMON +0, UNCOMMON +2, RARE +5, EPIC +8, LEGENDARY +10.
     *
     * @param grade     검증할 등급
     * @param itemLevel 아이템 레벨
     */
    @Property(tries = 100)
    void effectivePowerLevelAppliesCorrectBonus(@ForAll("grades") final Grade grade,
                                                @ForAll("itemLevels") final Integer itemLevel) {
        final FixedRandomSource randomSource = new FixedRandomSource(0.5);
        final DropService dropService = new DropService(randomSource);

        final int actual = dropService.effectivePowerLevel(itemLevel, grade);
        final int expectedBonus = EXPECTED_LEVEL_BONUS.get(grade);

        assertEquals(itemLevel + expectedBonus, actual,
                grade + " 등급의 레벨 보너스는 +" + expectedBonus + "이어야 한다 (itemLevel=" + itemLevel + ")");
    }
}
