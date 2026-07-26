package com.myapps.web.myrpg.domain.service;

import java.util.List;
import java.util.Map;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import org.mockito.Mockito;

import com.myapps.web.myrpg.domain.model.ArmorSlot;
import com.myapps.web.myrpg.domain.model.DropCategory;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.WeaponType;
import com.myapps.web.myrpg.domain.model.vo.RolledArmor;
import com.myapps.web.myrpg.domain.model.vo.RolledWeapon;
import com.myapps.web.myrpg.domain.model.vo.TreasureKind;
import com.myapps.web.myrpg.domain.model.vo.TreasureReward;
import com.myapps.web.myrpg.domain.random.FixedRandomSource;
import com.myapps.web.myrpg.domain.template.ArmorTemplate;
import com.myapps.web.myrpg.domain.template.DungeonTemplate;
import com.myapps.web.myrpg.domain.template.WeaponTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DungeonService 휴식·함정·보물상자 메커니즘 속성 기반 테스트.
 *
 * <p>jqwik을 사용하여 휴식 회복 상한, 함정 감소와 최소 HP 보장,
 * 보물상자 보상 종류 분포, 보물상자 골드 공식의 불변식을 검증한다.
 *
 * <p><b>Validates: Requirements 20.2, 20.3, 20.5, 20.6, 20.7, 20.8</b>
 */
class DungeonRestTrapTreasurePropertyTest {

    private static final double TREASURE_GOLD_THRESHOLD = 0.50;
    private static final double TREASURE_POTION_THRESHOLD = 0.90;
    private static final double GOLD_LEVEL_SCALING = 0.05;

    // --- Providers ---

    /**
     * 현재 HP를 생성한다 (1 ~ 9999).
     *
     * @return 현재 HP arbitrary
     */
    @Provide
    Arbitrary<Integer> currentHpProvider() {
        return Arbitraries.integers().between(1, 9999);
    }

    /**
     * 최대 HP를 생성한다 (1 ~ 9999).
     *
     * @return 최대 HP arbitrary
     */
    @Provide
    Arbitrary<Integer> maxHpProvider() {
        return Arbitraries.integers().between(1, 9999);
    }

    /**
     * 보물상자 종류 결정용 roll 값을 생성한다.
     *
     * @return roll arbitrary [0.0, 1.0)
     */
    @Provide
    Arbitrary<Double> treasureRollProvider() {
        return Arbitraries.doubles().between(0.0, true, 1.0, false);
    }

    /**
     * treasureBaseGold를 생성한다 (10 ~ 10000).
     *
     * @return treasureBaseGold arbitrary
     */
    @Provide
    Arbitrary<Integer> baseGoldProvider() {
        return Arbitraries.integers().between(10, 10000);
    }

    /**
     * requiredLevel(itemLevel)을 생성한다 (1 ~ 100).
     *
     * @return requiredLevel arbitrary
     */
    @Provide
    Arbitrary<Integer> itemLevelProvider() {
        return Arbitraries.integers().between(1, 100);
    }

    // =====================================================================
    // Property 38: 휴식 회복 상한
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 38: 휴식 회복 상한
    /**
     * 임의의 현재 HP와 최대 HP에 대해, 휴식 이벤트 처리 후 HP는
     * {@code min(현재 + 반올림(최대×0.1), 최대)}와 같고 최대치를 초과하지 않는다.
     *
     * <p><b>Validates: Requirements 20.2</b>
     */
    @Property(tries = 100)
    void restRecoveryIsCappedAtMaxHp(
            @ForAll("currentHpProvider") final int currentHp,
            @ForAll("maxHpProvider") final int maxHp) {

        final FixedRandomSource random = new FixedRandomSource(0.5);
        final DungeonService service = new DungeonService(random);

        final int result = service.applyRest(currentHp, maxHp);

        final long expectedRecovery = Math.round(maxHp * 0.10);
        final int expected = (int) Math.min(currentHp + expectedRecovery, maxHp);

        assertEquals(expected, result,
                "applyRest(" + currentHp + ", " + maxHp + ") = " + result
                        + ", 기대값 = " + expected);
        assertTrue(result <= maxHp,
                "회복 후 HP " + result + "가 최대 HP " + maxHp + "를 초과해서는 안 된다");
    }

    // =====================================================================
    // Property 39: 함정 감소와 최소 HP 보장
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 39: 함정 감소와 최소 HP 보장
    /**
     * 임의의 1 이상의 현재 HP에 대해, 함정 이벤트 처리 후 HP는 1 이상이며
     * {@code 현재 - 반올림(현재×0.1)}을 1로 하한 처리한 값과 같다(함정으로 사망하지 않음).
     *
     * <p><b>Validates: Requirements 20.3</b>
     */
    @Property(tries = 100)
    void trapDamageNeverKills(
            @ForAll("currentHpProvider") final int currentHp) {

        final FixedRandomSource random = new FixedRandomSource(0.5);
        final DungeonService service = new DungeonService(random);

        final int result = service.applyTrap(currentHp);

        final long expectedDamage = Math.round(currentHp * 0.10);
        final int expected = (int) Math.max(currentHp - expectedDamage, 1);

        assertEquals(expected, result,
                "applyTrap(" + currentHp + ") = " + result
                        + ", 기대값 = " + expected);
        assertTrue(result >= 1,
                "함정 후 HP " + result + "는 1 이상이어야 한다 (함정으로 사망 불가)");
    }

    // =====================================================================
    // Property 40: 보물상자 보상 종류
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 40: 보물상자 보상 종류
    /**
     * 임의의 난수에 대해, 보물상자 보상 종류는 {GOLD, POTION, EQUIPMENT} 중 하나이며
     * 누적 분포(0.5/0.9/1.0)에 따라 매핑된다. 포션 보상은 HP 포션 또는 MP 포션 중 하나이고,
     * 장비 보상은 던전 gradeChance와 일반 드랍 능력치 롤 규칙을 따른다.
     *
     * <p><b>Validates: Requirements 20.5, 20.7, 20.8</b>
     */
    @Property(tries = 100)
    void treasureKindFollowsCumulativeDistribution(
            @ForAll("treasureRollProvider") final double roll) {

        final DungeonTemplate dungeon = createDungeonTemplate(100, 10);
        final DropService dropService = createMockedDropService();
        final List<WeaponTemplate> weapons = createWeaponTemplates();
        final List<ArmorTemplate> armors = createArmorTemplates();

        final FixedRandomSource random = new FixedRandomSource(
                new double[]{roll, 0.3, 0.1, 0.5},
                new int[]{0, 0}
        );
        final DungeonService service = new DungeonService(random);

        final TreasureReward reward = service.rollTreasure(dungeon, dropService, weapons, armors);

        final TreasureKind expectedKind = expectedTreasureKind(roll);
        assertEquals(expectedKind, reward.kind(),
                "roll=" + roll + " → 기대 보상 종류=" + expectedKind
                        + ", 실제=" + reward.kind());

        switch (reward.kind()) {
            case GOLD -> {
                assertTrue(reward.gold() > 0,
                        "GOLD 보상은 양수 골드를 가져야 한다");
            }
            case POTION -> {
                assertTrue(reward.itemId() == 1L || reward.itemId() == 2L,
                        "POTION 보상은 HP 포션(1) 또는 MP 포션(2)이어야 한다, 실제=" + reward.itemId());
            }
            case EQUIPMENT -> {
                assertTrue(reward.equipment() != null,
                        "EQUIPMENT 보상은 equipment가 null이 아니어야 한다");
                assertTrue(
                        reward.equipment().category() == DropCategory.WEAPON
                                || reward.equipment().category() == DropCategory.ARMOR,
                        "EQUIPMENT 보상 카테고리는 WEAPON 또는 ARMOR이어야 한다");
            }
        }
    }

    // =====================================================================
    // Property 41: 보물상자 골드 공식
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 41: 보물상자 골드 공식
    /**
     * 임의의 던전 treasureBaseGold와 itemLevel에 대해, 보물상자 골드 보상은
     * {@code 반올림(treasureBaseGold × (1 + 0.05 × itemLevel))}(HALF_UP)과 같다.
     *
     * <p><b>Validates: Requirements 20.6</b>
     */
    @Property(tries = 100)
    void treasureGoldFormula(
            @ForAll("baseGoldProvider") final int baseGold,
            @ForAll("itemLevelProvider") final int itemLevel) {

        final DungeonTemplate dungeon = createDungeonTemplate(baseGold, itemLevel);

        final FixedRandomSource random = new FixedRandomSource(0.0);
        final DungeonService service = new DungeonService(random);

        final DropService dropService = createMockedDropService();
        final List<WeaponTemplate> weapons = createWeaponTemplates();
        final List<ArmorTemplate> armors = createArmorTemplates();

        final TreasureReward reward = service.rollTreasure(
                dungeon, dropService, weapons, armors);

        assertEquals(TreasureKind.GOLD, reward.kind(),
                "roll=0.0이면 GOLD 보상이어야 한다");

        final int expectedGold = (int) Math.round(
                baseGold * (1 + GOLD_LEVEL_SCALING * itemLevel));
        assertEquals(expectedGold, reward.gold(),
                "baseGold=" + baseGold + ", itemLevel=" + itemLevel
                        + " → 기대 골드=" + expectedGold + ", 실제=" + reward.gold());
    }

    // --- Helper methods ---

    /**
     * roll 값에 기반하여 기대되는 보물상자 보상 종류를 계산한다.
     *
     * @param roll 난수 값 [0.0, 1.0)
     * @return 기대되는 보물 종류
     */
    private TreasureKind expectedTreasureKind(final double roll) {
        if (roll < TREASURE_GOLD_THRESHOLD) {
            return TreasureKind.GOLD;
        }
        if (roll < TREASURE_POTION_THRESHOLD) {
            return TreasureKind.POTION;
        }
        return TreasureKind.EQUIPMENT;
    }

    /**
     * 테스트용 DungeonTemplate을 생성한다.
     *
     * @param treasureBaseGold 보물 기본 골드
     * @param requiredLevel    던전 요구 레벨
     * @return 던전 템플릿
     */
    private DungeonTemplate createDungeonTemplate(final int treasureBaseGold,
                                                  final int requiredLevel) {
        final Map<Grade, Double> gradeChance = Map.of(
                Grade.COMMON, 0.60,
                Grade.UNCOMMON, 0.25,
                Grade.RARE, 0.10,
                Grade.EPIC, 0.04,
                Grade.LEGENDARY, 0.01
        );

        return new DungeonTemplate(
                1L,
                "테스트 던전",
                1,
                5,
                requiredLevel,
                999L,
                1,
                List.of(WeaponType.SWORD),
                List.of(ArmorSlot.CHEST),
                gradeChance,
                treasureBaseGold,
                List.of()
        );
    }

    /**
     * 테스트용 무기 템플릿 목록을 생성한다.
     *
     * @return 무기 템플릿 목록
     */
    private List<WeaponTemplate> createWeaponTemplates() {
        return List.of(
                new WeaponTemplate(1L, "테스트 검", WeaponType.SWORD, 10, 5, 3, 100)
        );
    }

    /**
     * 테스트용 방어구 템플릿 목록을 생성한다.
     *
     * @return 방어구 템플릿 목록
     */
    private List<ArmorTemplate> createArmorTemplates() {
        return List.of(
                new ArmorTemplate(1L, "테스트 갑옷", ArmorSlot.CHEST, 8, 80)
        );
    }

    /**
     * 장비 보상 테스트를 위한 mocked DropService를 생성한다.
     *
     * @return mocked DropService
     */
    private DropService createMockedDropService() {
        final DropService dropService = Mockito.mock(DropService.class);

        Mockito.when(dropService.rollGrade(Mockito.any(DungeonTemplate.class)))
                .thenReturn(Grade.COMMON);

        Mockito.when(dropService.buildWeaponInstance(
                        Mockito.any(WeaponTemplate.class),
                        Mockito.any(Grade.class),
                        Mockito.anyInt()))
                .thenAnswer(invocation -> {
                    final WeaponTemplate template = invocation.getArgument(0);
                    final Grade grade = invocation.getArgument(1);
                    final int level = invocation.getArgument(2);
                    return new RolledWeapon(
                            template.id(), template.weaponType(), grade, level,
                            template.baseAttack(), template.baseSpeed(),
                            template.baseCritical(), 1, List.of(), "[일반] " + template.name());
                });

        Mockito.when(dropService.buildArmorInstance(
                        Mockito.any(ArmorTemplate.class),
                        Mockito.any(Grade.class),
                        Mockito.anyInt()))
                .thenAnswer(invocation -> {
                    final ArmorTemplate template = invocation.getArgument(0);
                    final Grade grade = invocation.getArgument(1);
                    final int level = invocation.getArgument(2);
                    return new RolledArmor(
                            template.id(), template.slot(), grade, level,
                            List.of(), "[일반] " + template.name());
                });

        return dropService;
    }
}
