package com.myapps.web.myrpg.domain.service;

import java.util.List;

import com.myapps.web.myrpg.domain.model.DropCategory;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.StageEventType;
import com.myapps.web.myrpg.domain.model.vo.DropResult;
import com.myapps.web.myrpg.domain.model.vo.RolledArmor;
import com.myapps.web.myrpg.domain.model.vo.RolledWeapon;
import com.myapps.web.myrpg.domain.model.vo.TreasureKind;
import com.myapps.web.myrpg.domain.model.vo.TreasureReward;
import com.myapps.web.myrpg.domain.random.RandomSource;
import com.myapps.web.myrpg.domain.template.ArmorTemplate;
import com.myapps.web.myrpg.domain.template.DungeonSpawn;
import com.myapps.web.myrpg.domain.template.DungeonTemplate;
import com.myapps.web.myrpg.domain.template.WeaponTemplate;

/**
 * 던전 탐색 관련 순수 도메인 규칙 서비스.
 *
 * <p>스테이지 이벤트 결정, 몬스터 선택, 휴식/함정 효과, 보물상자 보상 등
 * 던전 내 규칙 로직을 캡슐화한다. 리포지토리 의존 없이 {@link RandomSource}만
 * 주입받는 순수 서비스이다.
 */
public class DungeonService {

    private static final int BOSS_STAGE = 5;

    private static final double BATTLE_THRESHOLD = 0.75;
    private static final double REST_THRESHOLD = 0.83;
    private static final double MERCHANT_THRESHOLD = 0.90;
    private static final double TRAP_THRESHOLD = 0.95;

    private static final double REST_RECOVERY_RATE = 0.10;
    private static final double TRAP_DAMAGE_RATE = 0.10;
    private static final int MINIMUM_HP = 1;

    private static final double TREASURE_GOLD_THRESHOLD = 0.50;
    private static final double TREASURE_POTION_THRESHOLD = 0.90;
    private static final double GOLD_LEVEL_SCALING = 0.05;

    private static final long HP_POTION_ITEM_ID = 1L;
    private static final long MP_POTION_ITEM_ID = 2L;

    private static final double EQUIPMENT_WEAPON_CHANCE = 0.5;

    private final RandomSource randomSource;

    /**
     * DungeonService를 생성한다.
     *
     * @param randomSource 난수 생성 인터페이스
     */
    public DungeonService(final RandomSource randomSource) {
        this.randomSource = randomSource;
    }

    /**
     * 스테이지 이벤트를 결정한다.
     *
     * <p>스테이지 5는 항상 보스 전투(BATTLE)를 반환한다.
     * 스테이지 1~4는 누적 분포에 따라 BATTLE(75%), REST(8%), MERCHANT(7%),
     * TRAP(5%), TREASURE(5%) 중 하나를 반환한다.
     *
     * @param stage 현재 스테이지 번호 (1~5)
     * @return 결정된 스테이지 이벤트 종류
     */
    public StageEventType rollStageEvent(final int stage) {
        if (stage == BOSS_STAGE) {
            return StageEventType.BATTLE;
        }

        final double roll = randomSource.nextDouble();

        if (roll < BATTLE_THRESHOLD) {
            return StageEventType.BATTLE;
        }
        if (roll < REST_THRESHOLD) {
            return StageEventType.REST;
        }
        if (roll < MERCHANT_THRESHOLD) {
            return StageEventType.MERCHANT;
        }
        if (roll < TRAP_THRESHOLD) {
            return StageEventType.TRAP;
        }
        return StageEventType.TREASURE;
    }

    /**
     * 던전 스테이지에 출현할 몬스터를 선택한다.
     *
     * <p>스테이지 5(보스)는 던전 템플릿의 bossId를 반환한다.
     * 스테이지 1~4는 해당 스테이지 범위에 속하는 몬스터 중
     * spawnWeight 가중치 기반 랜덤 선택을 수행한다.
     *
     * @param dungeon 던전 템플릿
     * @param stage   현재 스테이지 번호 (1~5)
     * @return 선택된 몬스터 ID
     */
    public long pickMonster(final DungeonTemplate dungeon, final int stage) {
        if (stage == BOSS_STAGE) {
            return dungeon.bossId();
        }

        final List<DungeonSpawn> eligible = dungeon.monsters().stream()
                .filter(spawn -> spawn.minFloor() <= stage && stage <= spawn.maxFloor())
                .toList();

        final int totalWeight = eligible.stream()
                .mapToInt(DungeonSpawn::spawnWeight)
                .sum();

        final int roll = randomSource.nextInt(totalWeight);
        int cumulative = 0;

        for (final DungeonSpawn spawn : eligible) {
            cumulative += spawn.spawnWeight();
            if (roll < cumulative) {
                return spawn.monsterId();
            }
        }

        return eligible.getLast().monsterId();
    }

    /**
     * 휴식 이벤트의 HP 회복량을 적용한다.
     *
     * <p>maxHp의 10%를 회복하며(반올림), maxHp를 초과하지 않는다.
     *
     * @param currentHp 현재 HP
     * @param maxHp     최대 HP
     * @return 회복 후 HP
     */
    public int applyRest(final int currentHp, final int maxHp) {
        final long recovery = Math.round(maxHp * REST_RECOVERY_RATE);
        final long healed = currentHp + recovery;
        return (int) Math.min(healed, maxHp);
    }

    /**
     * 함정 이벤트의 HP 감소량을 적용한다.
     *
     * <p>현재 HP의 10%를 감소시키며(반올림), HP가 1 미만으로 떨어지지 않는다.
     * 함정으로 인한 사망은 발생하지 않는다.
     *
     * @param currentHp 현재 HP
     * @return 감소 후 HP (최소 1)
     */
    public int applyTrap(final int currentHp) {
        final long damage = Math.round(currentHp * TRAP_DAMAGE_RATE);
        final long result = currentHp - damage;
        return (int) Math.max(result, MINIMUM_HP);
    }

    /**
     * 보물상자 이벤트의 보상을 결정한다.
     *
     * <p>보상 종류 분포: GOLD 50% / POTION 40% / EQUIPMENT 10%.
     * GOLD: treasureBaseGold × (1 + 0.05 × requiredLevel) 반올림.
     * POTION: HP 포션(id=1) 또는 MP 포션(id=2) 동일 확률.
     * EQUIPMENT: 무기(50%) 또는 방어구(50%)를 DropService로 생성.
     *
     * @param dungeon         던전 템플릿
     * @param dropService     드랍 서비스 (장비 생성용)
     * @param weaponTemplates 사용 가능한 무기 템플릿 목록
     * @param armorTemplates  사용 가능한 방어구 템플릿 목록
     * @return 보물상자 보상 결과
     */
    public TreasureReward rollTreasure(final DungeonTemplate dungeon,
                                       final DropService dropService,
                                       final List<WeaponTemplate> weaponTemplates,
                                       final List<ArmorTemplate> armorTemplates) {
        final double roll = randomSource.nextDouble();

        if (roll < TREASURE_GOLD_THRESHOLD) {
            return rollGoldTreasure(dungeon);
        }
        if (roll < TREASURE_POTION_THRESHOLD) {
            return rollPotionTreasure();
        }
        return rollEquipmentTreasure(dungeon, dropService, weaponTemplates, armorTemplates);
    }

    /**
     * 골드 보물을 생성한다.
     *
     * @param dungeon 던전 템플릿
     * @return 골드 보상 결과
     */
    private TreasureReward rollGoldTreasure(final DungeonTemplate dungeon) {
        final int itemLevel = dungeon.requiredLevel();
        final int gold = (int) Math.round(
                dungeon.treasureBaseGold() * (1 + GOLD_LEVEL_SCALING * itemLevel));
        return new TreasureReward(TreasureKind.GOLD, gold, null, null);
    }

    /**
     * 포션 보물을 생성한다.
     *
     * <p>HP 포션(id=1)과 MP 포션(id=2) 중 동일 확률로 선택한다.
     *
     * @return 포션 보상 결과
     */
    private TreasureReward rollPotionTreasure() {
        final int choice = randomSource.nextInt(2);
        final long potionId = (choice == 0) ? HP_POTION_ITEM_ID : MP_POTION_ITEM_ID;
        return new TreasureReward(TreasureKind.POTION, 0, potionId, null);
    }

    /**
     * 장비 보물을 생성한다.
     *
     * <p>무기(50%) 또는 방어구(50%)를 선택하고 DropService를 통해 인스턴스를 생성한다.
     *
     * @param dungeon         던전 템플릿
     * @param dropService     드랍 서비스
     * @param weaponTemplates 사용 가능한 무기 템플릿 목록
     * @param armorTemplates  사용 가능한 방어구 템플릿 목록
     * @return 장비 보상 결과
     */
    private TreasureReward rollEquipmentTreasure(final DungeonTemplate dungeon,
                                                 final DropService dropService,
                                                 final List<WeaponTemplate> weaponTemplates,
                                                 final List<ArmorTemplate> armorTemplates) {
        final int itemLevel = dungeon.requiredLevel();
        final double typeRoll = randomSource.nextDouble();

        if (typeRoll < EQUIPMENT_WEAPON_CHANCE) {
            return rollWeaponTreasure(dungeon, dropService, weaponTemplates, itemLevel);
        }
        return rollArmorTreasure(dungeon, dropService, armorTemplates, itemLevel);
    }

    /**
     * 무기 보물을 생성한다.
     *
     * @param dungeon         던전 템플릿
     * @param dropService     드랍 서비스
     * @param weaponTemplates 사용 가능한 무기 템플릿 목록
     * @param itemLevel       아이템 레벨
     * @return 무기 장비 보상 결과
     */
    private TreasureReward rollWeaponTreasure(final DungeonTemplate dungeon,
                                              final DropService dropService,
                                              final List<WeaponTemplate> weaponTemplates,
                                              final int itemLevel) {
        final WeaponTemplate template = weaponTemplates.get(
                randomSource.nextInt(weaponTemplates.size()));
        final Grade grade = dropService.rollGrade(dungeon);
        final RolledWeapon weapon = dropService.buildWeaponInstance(template, grade, itemLevel);
        final DropResult equipment = new DropResult(DropCategory.WEAPON, weapon, null, null);
        return new TreasureReward(TreasureKind.EQUIPMENT, 0, null, equipment);
    }

    /**
     * 방어구 보물을 생성한다.
     *
     * @param dungeon        던전 템플릿
     * @param dropService    드랍 서비스
     * @param armorTemplates 사용 가능한 방어구 템플릿 목록
     * @param itemLevel      아이템 레벨
     * @return 방어구 장비 보상 결과
     */
    private TreasureReward rollArmorTreasure(final DungeonTemplate dungeon,
                                             final DropService dropService,
                                             final List<ArmorTemplate> armorTemplates,
                                             final int itemLevel) {
        final ArmorTemplate template = armorTemplates.get(
                randomSource.nextInt(armorTemplates.size()));
        final Grade grade = dropService.rollGrade(dungeon);
        final RolledArmor armor = dropService.buildArmorInstance(template, grade, itemLevel);
        final DropResult equipment = new DropResult(DropCategory.ARMOR, null, armor, null);
        return new TreasureReward(TreasureKind.EQUIPMENT, 0, null, equipment);
    }
}
