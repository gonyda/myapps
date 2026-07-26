package com.myapps.web.myrpg.domain.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.ArmorSlot;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.StatType;
import com.myapps.web.myrpg.domain.model.WeaponType;
import com.myapps.web.myrpg.domain.model.vo.RolledArmor;
import com.myapps.web.myrpg.domain.model.vo.RolledWeapon;
import com.myapps.web.myrpg.domain.model.vo.StatRoll;
import com.myapps.web.myrpg.domain.random.FixedRandomSource;
import com.myapps.web.myrpg.domain.template.ArmorTemplate;
import com.myapps.web.myrpg.domain.template.WeaponTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DropService의 수치 스케일링 및 랜덤 능력치 롤 관련 속성 기반 테스트.
 *
 * <p>무기 기본공격력 스케일링 공식, 타입 고유 스탯 불변성,
 * 랜덤 능력치 롤의 범위·중복 없음·유효 타입 집합 불변식을 검증한다.
 *
 * <p><b>Validates: Requirements 14.4, 15.3, 15.4, 15.5, 16.1, 16.2, 16.4</b>
 */
class DropScaleStatsPropertyTest {

    private static final int ITEM_LEVEL_MIN = 1;
    private static final int ITEM_LEVEL_MAX = 100;
    private static final int BASE_ATTACK_MIN = 5;
    private static final int BASE_ATTACK_MAX = 100;
    private static final int BASE_SPEED_MIN = 1;
    private static final int BASE_SPEED_MAX = 20;
    private static final int BASE_CRITICAL_MIN = 0;
    private static final int BASE_CRITICAL_MAX = 15;
    private static final int BASE_VALUE = 100;
    private static final long TEMPLATE_ID = 1L;
    private static final double POWER_SCALING_COEFFICIENT = 0.15;
    private static final double STAT_LOW_COEFFICIENT = 0.4;
    private static final double STAT_HIGH_COEFFICIENT = 0.8;
    private static final int MIN_STAT_VALUE = 1;

    private static final Set<StatType> VALID_STAT_TYPES = Set.of(
            StatType.ATTACK, StatType.DEFENSE, StatType.HP, StatType.SPEED, StatType.CRITICAL
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
     * 유효 파워 레벨을 생성하는 Provider.
     *
     * <p>itemLevel(1~100) + grade.levelBonus(0~10) 범위를 커버한다.
     *
     * @return 파워 레벨 Arbitrary (1~110)
     */
    @Provide
    Arbitrary<Integer> powerLevels() {
        return Arbitraries.integers().between(1, 110);
    }

    /**
     * 템플릿 기본 공격력 값을 생성하는 Provider.
     *
     * @return 기본 공격력 Arbitrary (5~100)
     */
    @Provide
    Arbitrary<Integer> baseAttacks() {
        return Arbitraries.integers().between(BASE_ATTACK_MIN, BASE_ATTACK_MAX);
    }

    /**
     * 템플릿 기본 속도 값을 생성하는 Provider.
     *
     * @return 기본 속도 Arbitrary (1~20)
     */
    @Provide
    Arbitrary<Integer> baseSpeeds() {
        return Arbitraries.integers().between(BASE_SPEED_MIN, BASE_SPEED_MAX);
    }

    /**
     * 템플릿 기본 치명타 값을 생성하는 Provider.
     *
     * @return 기본 치명타 Arbitrary (0~15)
     */
    @Provide
    Arbitrary<Integer> baseCriticals() {
        return Arbitraries.integers().between(BASE_CRITICAL_MIN, BASE_CRITICAL_MAX);
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

    // --- Property 28: 무기 기본공격력 스케일 ---

    // Feature: myrpg-gen1-mvp, Property 28: 무기 기본공격력 스케일
    /**
     * rollBaseAttack(templateBaseAttack, P)가 Math.round(templateBaseAttack × (1 + 0.15 × P))를
     * 반환하는지 검증한다 (HALF_UP 반올림).
     *
     * @param templateBaseAttack 템플릿 기본 공격력
     * @param powerLevel         유효 파워 레벨
     */
    @Property(tries = 100)
    void baseAttackScalesWithPowerLevel(@ForAll("baseAttacks") final Integer templateBaseAttack,
                                        @ForAll("powerLevels") final Integer powerLevel) {
        final FixedRandomSource randomSource = new FixedRandomSource(0.5);
        final DropService dropService = new DropService(randomSource);

        final int actual = dropService.rollBaseAttack(templateBaseAttack, powerLevel);
        final int expected = (int) Math.round(
                templateBaseAttack * (1 + POWER_SCALING_COEFFICIENT * powerLevel));

        assertEquals(expected, actual,
                "rollBaseAttack(" + templateBaseAttack + ", " + powerLevel + ") = "
                        + "round(" + templateBaseAttack + " × (1 + 0.15 × " + powerLevel + ")) = " + expected);
    }

    // Feature: myrpg-gen1-mvp, Property 28: 무기 기본공격력 스케일
    /**
     * buildWeaponInstance로 생성된 무기의 baseAttack이 스케일링 공식과 일치하는지 검증한다.
     *
     * @param templateBaseAttack 템플릿 기본 공격력
     * @param itemLevel          아이템 레벨
     * @param grade              등급
     * @param weaponType         무기 타입
     */
    @Property(tries = 100)
    void weaponInstanceBaseAttackMatchesScaleFormula(
            @ForAll("baseAttacks") final Integer templateBaseAttack,
            @ForAll("itemLevels") final Integer itemLevel,
            @ForAll("grades") final Grade grade,
            @ForAll("weaponTypes") final WeaponType weaponType) {
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{0.5},
                new int[]{0}
        );
        final DropService dropService = new DropService(randomSource);
        final WeaponTemplate template = new WeaponTemplate(
                TEMPLATE_ID, "테스트검", weaponType, templateBaseAttack, 10, 5, BASE_VALUE);

        final RolledWeapon weapon = dropService.buildWeaponInstance(template, grade, itemLevel);

        final int powerLevel = itemLevel + grade.getLevelBonus();
        final int expected = (int) Math.round(
                templateBaseAttack * (1 + POWER_SCALING_COEFFICIENT * powerLevel));

        assertEquals(expected, weapon.baseAttack(),
                "무기 인스턴스 baseAttack은 스케일링 공식 결과(" + expected + ")와 일치해야 한다");
    }

    // --- Property 29: 타입 고유 스탯은 스케일에서 제외 ---

    // Feature: myrpg-gen1-mvp, Property 29: 타입 고유 스탯은 스케일에서 제외
    /**
     * buildWeaponInstance로 생성된 무기의 baseSpeed가 템플릿 고정값과 동일한지 검증한다.
     * 파워 레벨에 관계없이 스케일링되지 않아야 한다.
     *
     * @param baseSpeed  템플릿 기본 속도
     * @param itemLevel  아이템 레벨
     * @param grade      등급
     * @param weaponType 무기 타입
     */
    @Property(tries = 100)
    void weaponBaseSpeedIsNotScaled(@ForAll("baseSpeeds") final Integer baseSpeed,
                                    @ForAll("itemLevels") final Integer itemLevel,
                                    @ForAll("grades") final Grade grade,
                                    @ForAll("weaponTypes") final WeaponType weaponType) {
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{0.5},
                new int[]{0}
        );
        final DropService dropService = new DropService(randomSource);
        final WeaponTemplate template = new WeaponTemplate(
                TEMPLATE_ID, "테스트검", weaponType, 20, baseSpeed, 5, BASE_VALUE);

        final RolledWeapon weapon = dropService.buildWeaponInstance(template, grade, itemLevel);

        assertEquals(baseSpeed.intValue(), weapon.baseSpeed(),
                "무기 baseSpeed(" + weapon.baseSpeed() + ")는 템플릿 고정값(" + baseSpeed
                        + ")과 동일해야 한다 (P=" + (itemLevel + grade.getLevelBonus()) + ")");
    }

    // Feature: myrpg-gen1-mvp, Property 29: 타입 고유 스탯은 스케일에서 제외
    /**
     * buildWeaponInstance로 생성된 무기의 baseCritical이 템플릿 고정값과 동일한지 검증한다.
     * 파워 레벨에 관계없이 스케일링되지 않아야 한다.
     *
     * @param baseCritical 템플릿 기본 치명타
     * @param itemLevel    아이템 레벨
     * @param grade        등급
     * @param weaponType   무기 타입
     */
    @Property(tries = 100)
    void weaponBaseCriticalIsNotScaled(@ForAll("baseCriticals") final Integer baseCritical,
                                       @ForAll("itemLevels") final Integer itemLevel,
                                       @ForAll("grades") final Grade grade,
                                       @ForAll("weaponTypes") final WeaponType weaponType) {
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{0.5},
                new int[]{0}
        );
        final DropService dropService = new DropService(randomSource);
        final WeaponTemplate template = new WeaponTemplate(
                TEMPLATE_ID, "테스트검", weaponType, 20, 10, baseCritical, BASE_VALUE);

        final RolledWeapon weapon = dropService.buildWeaponInstance(template, grade, itemLevel);

        assertEquals(baseCritical.intValue(), weapon.baseCritical(),
                "무기 baseCritical(" + weapon.baseCritical() + ")은 템플릿 고정값(" + baseCritical
                        + ")과 동일해야 한다 (P=" + (itemLevel + grade.getLevelBonus()) + ")");
    }

    // --- Property 30: 랜덤 능력치 롤 불변식 ---

    // Feature: myrpg-gen1-mvp, Property 30: 랜덤 능력치 롤 불변식
    /**
     * rollStats 결과의 모든 statType이 {ATTACK, DEFENSE, HP, SPEED, CRITICAL} 집합 내에 있는지 검증한다.
     *
     * @param grade      등급
     * @param powerLevel 유효 파워 레벨
     */
    @Property(tries = 100)
    void allStatTypesAreWithinValidSet(@ForAll("grades") final Grade grade,
                                       @ForAll("powerLevels") final Integer powerLevel) {
        final int low = Math.max(MIN_STAT_VALUE,
                (int) Math.round(powerLevel * STAT_LOW_COEFFICIENT));
        final int[] intValues = buildIntValuesForRollStats(grade, low);
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{0.3},
                intValues
        );
        final DropService dropService = new DropService(randomSource);

        final List<StatRoll> stats = dropService.rollStats(grade, powerLevel);

        for (final StatRoll stat : stats) {
            assertTrue(VALID_STAT_TYPES.contains(stat.statType()),
                    "StatType " + stat.statType() + "은(는) 유효 집합 " + VALID_STAT_TYPES + " 내에 있어야 한다");
        }
    }

    // Feature: myrpg-gen1-mvp, Property 30: 랜덤 능력치 롤 불변식
    /**
     * rollStats 결과에 중복 StatType이 없는지 검증한다.
     *
     * @param grade      등급
     * @param powerLevel 유효 파워 레벨
     */
    @Property(tries = 100)
    void noStatTypeDuplicatesInRollStats(@ForAll("grades") final Grade grade,
                                          @ForAll("powerLevels") final Integer powerLevel) {
        final int low = Math.max(MIN_STAT_VALUE,
                (int) Math.round(powerLevel * STAT_LOW_COEFFICIENT));
        final int[] intValues = buildIntValuesForRollStats(grade, low);
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{0.3},
                intValues
        );
        final DropService dropService = new DropService(randomSource);

        final List<StatRoll> stats = dropService.rollStats(grade, powerLevel);

        final Set<StatType> uniqueTypes = new HashSet<>();
        for (final StatRoll stat : stats) {
            assertTrue(uniqueTypes.add(stat.statType()),
                    "StatType " + stat.statType() + "이(가) 중복되었다 (등급=" + grade + ")");
        }
    }

    // Feature: myrpg-gen1-mvp, Property 30: 랜덤 능력치 롤 불변식
    /**
     * rollStats 결과의 각 value가 [max(1, round(P×0.4)), max(1, round(P×0.8))] 범위 내이고
     * 항상 1 이상인지 검증한다.
     *
     * @param grade      등급
     * @param powerLevel 유효 파워 레벨
     */
    @Property(tries = 100)
    void statValuesAreWithinExpectedRange(@ForAll("grades") final Grade grade,
                                           @ForAll("powerLevels") final Integer powerLevel) {
        final int expectedLow = Math.max(MIN_STAT_VALUE,
                (int) Math.round(powerLevel * STAT_LOW_COEFFICIENT));
        final int expectedHigh = Math.max(MIN_STAT_VALUE,
                (int) Math.round(powerLevel * STAT_HIGH_COEFFICIENT));

        final int statCount = computeStatCount(grade);
        final int[] intValues = buildIntValuesForStatRange(statCount, expectedLow, expectedHigh);
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{0.3},
                intValues
        );
        final DropService dropService = new DropService(randomSource);

        final List<StatRoll> stats = dropService.rollStats(grade, powerLevel);

        for (final StatRoll stat : stats) {
            assertTrue(stat.value() >= MIN_STAT_VALUE,
                    "StatRoll value(" + stat.value() + ")는 항상 1 이상이어야 한다");
            assertTrue(stat.value() >= expectedLow && stat.value() <= expectedHigh,
                    "StatRoll value(" + stat.value() + ")는 [" + expectedLow + ", "
                            + expectedHigh + "] 범위 내여야 한다 (P=" + powerLevel + ")");
        }
    }

    // --- Helper methods ---

    /**
     * 주어진 등급과 double roll=0.3에 대응하는 statCount를 계산한다.
     *
     * @param grade 등급
     * @return 해당 등급에서 roll=0.3일 때의 능력치 개수
     */
    private int computeStatCount(final Grade grade) {
        final double roll = 0.3;
        return switch (grade) {
            case COMMON -> 1;
            case UNCOMMON -> roll < 0.6 ? 1 : 2;
            case RARE -> roll < 0.6 ? 2 : 3;
            case EPIC -> roll < 0.6 ? 3 : 4;
            case LEGENDARY -> roll < 0.5 ? 4 : 5;
        };
    }

    /**
     * rollStats에서 사용할 intValues 배열을 구성한다.
     *
     * <p>pickDistinctStatTypes용 인덱스(항상 0) + nextIntInclusive용 stat value(low)를 연결한다.
     *
     * @param grade    등급
     * @param statValue nextIntInclusive에서 반환할 stat value
     * @return 구성된 int 배열
     */
    private int[] buildIntValuesForRollStats(final Grade grade, final int statValue) {
        final int statCount = computeStatCount(grade);
        final int[] values = new int[statCount + statCount];
        // 첫 statCount개: pickDistinctStatTypes용 인덱스 (항상 0)
        for (int i = 0; i < statCount; i++) {
            values[i] = 0;
        }
        // 다음 statCount개: nextIntInclusive용 stat value
        for (int i = statCount; i < values.length; i++) {
            values[i] = statValue;
        }
        return values;
    }

    /**
     * statValues 범위 테스트를 위한 intValues 배열을 구성한다.
     *
     * <p>pickDistinctStatTypes용 인덱스(항상 0) + nextIntInclusive용으로
     * low와 high를 번갈아 반환하여 범위 내 값임을 검증한다.
     *
     * @param statCount    능력치 개수
     * @param expectedLow  최소 stat value
     * @param expectedHigh 최대 stat value
     * @return 구성된 int 배열
     */
    private int[] buildIntValuesForStatRange(final int statCount, final int expectedLow,
                                             final int expectedHigh) {
        final int[] values = new int[statCount + statCount];
        // 첫 statCount개: pickDistinctStatTypes용 인덱스 (항상 0)
        for (int i = 0; i < statCount; i++) {
            values[i] = 0;
        }
        // 다음 statCount개: nextIntInclusive용 — low와 high를 번갈아 사용
        for (int i = statCount; i < values.length; i++) {
            values[i] = (i - statCount) % 2 == 0 ? expectedLow : expectedHigh;
        }
        return values;
    }

    // Feature: myrpg-gen1-mvp, Property 30: 랜덤 능력치 롤 불변식
    /**
     * buildArmorInstance로 생성된 방어구 인스턴스에 skillSlots 필드가 없음을 검증한다.
     * RolledArmor record는 skillSlots를 포함하지 않으며, 이는 방어구에 스킬 슬롯이 없음을 의미한다.
     *
     * @param grade     등급
     * @param itemLevel 아이템 레벨
     * @param armorSlot 방어구 부위
     */
    @Property(tries = 100)
    void armorInstanceHasNoSkillSlots(@ForAll("grades") final Grade grade,
                                      @ForAll("itemLevels") final Integer itemLevel,
                                      @ForAll("armorSlots") final ArmorSlot armorSlot) {
        final FixedRandomSource randomSource = new FixedRandomSource(
                new double[]{0.5},
                new int[]{0}
        );
        final DropService dropService = new DropService(randomSource);
        final ArmorTemplate template = new ArmorTemplate(
                TEMPLATE_ID, "테스트갑옷", armorSlot, 15, BASE_VALUE);

        final RolledArmor armor = dropService.buildArmorInstance(template, grade, itemLevel);

        // RolledArmor record에 skillSlots 필드가 존재하지 않음을 컴파일 타임에 보장.
        // 런타임에는 record의 구성요소 목록에 "skillSlots"가 없음을 리플렉션으로 검증한다.
        final boolean hasSkillSlotsField = java.util.Arrays.stream(
                        RolledArmor.class.getRecordComponents())
                .anyMatch(component -> "skillSlots".equals(component.getName()));

        assertTrue(!hasSkillSlotsField,
                "RolledArmor는 skillSlots 필드를 가져서는 안 된다 (방어구에 스킬 슬롯 없음)");

        // 추가 검증: armor 인스턴스가 정상 생성되었는지 확인
        assertTrue(armor.stats() != null,
                "방어구 인스턴스의 stats는 null이 아니어야 한다");
        assertEquals(grade, armor.grade(),
                "방어구 인스턴스의 등급은 입력 등급과 일치해야 한다");
    }
}
