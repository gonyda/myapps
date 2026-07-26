package com.myapps.web.myrpg.domain.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.myapps.web.myrpg.domain.model.ArmorSlot;
import com.myapps.web.myrpg.domain.model.DropCategory;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.StatType;
import com.myapps.web.myrpg.domain.model.WeaponType;
import com.myapps.web.myrpg.domain.model.vo.DropResult;
import com.myapps.web.myrpg.domain.model.vo.RolledArmor;
import com.myapps.web.myrpg.domain.model.vo.RolledWeapon;
import com.myapps.web.myrpg.domain.model.vo.StatRoll;
import com.myapps.web.myrpg.domain.random.RandomSource;
import com.myapps.web.myrpg.domain.template.ArmorTemplate;
import com.myapps.web.myrpg.domain.template.DungeonTemplate;
import com.myapps.web.myrpg.domain.template.MonsterTemplate;
import com.myapps.web.myrpg.domain.template.SkillTemplate;
import com.myapps.web.myrpg.domain.template.WeaponTemplate;

/**
 * 드랍 관련 순수 도메인 규칙 서비스.
 *
 * <p>몬스터 처치 시 드랍 카테고리 결정, 등급 롤, 무기/방어구 인스턴스 생성,
 * 랜덤 능력치 롤 등 드랍 시스템의 모든 계산 로직을 캡슐화한다.
 * 리포지토리 의존 없이 {@link RandomSource}만 주입받는 순수 서비스이다.
 */
@Service
public class DropService {

    private static final double NORMAL_ARMOR_CHANCE = 0.05;
    private static final double BOSS_WEAPON_CHANCE = 0.15;
    private static final double BOSS_SKILL_BOOK_CHANCE = 0.15;

    private static final double POWER_SCALING_COEFFICIENT = 0.15;
    private static final double STAT_LOW_COEFFICIENT = 0.4;
    private static final double STAT_HIGH_COEFFICIENT = 0.8;
    private static final int MIN_STAT_VALUE = 1;

    private static final Map<Grade, String> GRADE_DISPLAY_LABELS = Map.of(
            Grade.COMMON, "일반",
            Grade.UNCOMMON, "고급",
            Grade.RARE, "희귀",
            Grade.EPIC, "영웅",
            Grade.LEGENDARY, "전설"
    );

    private final RandomSource randomSource;

    /**
     * DropService를 생성한다.
     *
     * @param randomSource 난수 생성 인터페이스
     */
    public DropService(final RandomSource randomSource) {
        this.randomSource = randomSource;
    }

    /**
     * 등급에 따른 스킬 슬롯 수를 반환한다.
     *
     * @param grade 장비 등급
     * @return 스킬 슬롯 수 (COMMON=1, UNCOMMON=2, RARE=3, EPIC=4, LEGENDARY=5)
     */
    public int slotCount(final Grade grade) {
        return grade.getSkillSlots();
    }

    /**
     * 등급에 따른 랜덤 능력치 개수를 결정한다.
     *
     * <p>등급별 확률에 따라 능력치 부여 개수가 결정된다.
     *
     * @param grade 장비 등급
     * @return 능력치 개수
     */
    public int rollStatCount(final Grade grade) {
        final double roll = randomSource.nextDouble();
        return switch (grade) {
            case COMMON -> 1;
            case UNCOMMON -> roll < 0.6 ? 1 : 2;
            case RARE -> roll < 0.6 ? 2 : 3;
            case EPIC -> roll < 0.6 ? 3 : 4;
            case LEGENDARY -> roll < 0.5 ? 4 : 5;
        };
    }

    /**
     * 유효 파워 레벨을 산출한다.
     *
     * <p>공식: {@code itemLevel + grade.getLevelBonus()}
     *
     * @param itemLevel 아이템 레벨
     * @param grade     장비 등급
     * @return 유효 파워 레벨
     */
    public int effectivePowerLevel(final int itemLevel, final Grade grade) {
        return itemLevel + grade.getLevelBonus();
    }

    /**
     * 무기 기본 공격력을 파워 레벨 기반으로 스케일링한다.
     *
     * <p>공식: {@code Math.round(templateBaseAttack * (1 + 0.15 * P))}
     * HALF_UP 반올림을 사용한다.
     *
     * @param templateBaseAttack 템플릿 기본 공격력
     * @param powerLevel         유효 파워 레벨
     * @return 스케일링된 기본 공격력
     */
    public int rollBaseAttack(final int templateBaseAttack, final int powerLevel) {
        return (int) Math.round(templateBaseAttack * (1 + POWER_SCALING_COEFFICIENT * powerLevel));
    }

    /**
     * 랜덤 능력치를 롤한다.
     *
     * <p>등급에 따른 개수만큼 중복 없이 StatType 5종에서 선택하고,
     * 각 값은 {@code [max(1, round(P*0.4)), round(P*0.8)]} 범위에서 결정된다.
     *
     * @param grade      장비 등급
     * @param powerLevel 유효 파워 레벨
     * @return 롤된 능력치 목록 (불변)
     */
    public List<StatRoll> rollStats(final Grade grade, final int powerLevel) {
        final int statCount = rollStatCount(grade);
        final List<StatType> selectedTypes = pickDistinctStatTypes(statCount);

        final int low = Math.max(MIN_STAT_VALUE, (int) Math.round(powerLevel * STAT_LOW_COEFFICIENT));
        final int high = Math.max(MIN_STAT_VALUE, (int) Math.round(powerLevel * STAT_HIGH_COEFFICIENT));

        final List<StatRoll> stats = new ArrayList<>(statCount);
        for (final StatType type : selectedTypes) {
            final int value = randomSource.nextIntInclusive(low, high);
            stats.add(new StatRoll(type, value));
        }

        return Collections.unmodifiableList(stats);
    }

    /**
     * 던전의 등급 확률 분포에 따라 등급을 결정한다.
     *
     * <p>누적 분포 함수(CDF)를 사용하여 nextDouble() 결과에 대응하는 등급을 반환한다.
     *
     * @param dungeon 던전 템플릿
     * @return 결정된 등급
     */
    public Grade rollGrade(final DungeonTemplate dungeon) {
        final double roll = randomSource.nextDouble();
        double cumulative = 0.0;

        for (final Grade grade : Grade.values()) {
            final Double chance = dungeon.gradeChance().get(grade);
            if (chance != null) {
                cumulative += chance;
                if (roll < cumulative) {
                    return grade;
                }
            }
        }

        return Grade.LEGENDARY;
    }

    /**
     * 몬스터 처치 시 드랍을 결정한다.
     *
     * <p>일반 몬스터는 방어구 5% / 없음 95%,
     * 보스 몬스터는 무기 15% / 스킬북 15% / 없음 70% 확률로 드랍한다.
     *
     * @param monster         처치된 몬스터 템플릿
     * @param dungeon         현재 던전 템플릿
     * @param availableSkills 던전 무기 타입과 매칭되는 사용 가능 스킬 목록
     * @param weaponTemplates 던전에서 드랍 가능한 무기 템플릿 목록
     * @param armorTemplates  던전에서 드랍 가능한 방어구 템플릿 목록
     * @param itemLevel       드랍 아이템 레벨
     * @return 드랍 결과
     */
    public DropResult rollDrop(final MonsterTemplate monster,
                               final DungeonTemplate dungeon,
                               final List<SkillTemplate> availableSkills,
                               final List<WeaponTemplate> weaponTemplates,
                               final List<ArmorTemplate> armorTemplates,
                               final int itemLevel) {
        final DropCategory category = rollDropCategory(monster);

        return switch (category) {
            case NONE -> new DropResult(DropCategory.NONE, null, null, null);
            case WEAPON -> rollWeaponDrop(dungeon, weaponTemplates, itemLevel);
            case ARMOR -> rollArmorDrop(dungeon, armorTemplates, itemLevel);
            case SKILL_BOOK -> rollSkillBookDrop(availableSkills);
        };
    }

    /**
     * 무기 인스턴스를 생성한다.
     *
     * <p>유효 파워 레벨을 산출하고 공격력 스케일링, 스킬 슬롯, 랜덤 능력치를 결정한다.
     * baseSpeed와 baseCritical은 템플릿 고정값을 그대로 사용한다.
     *
     * @param template  무기 템플릿
     * @param grade     결정된 등급
     * @param itemLevel 아이템 레벨
     * @return 생성된 무기 인스턴스
     */
    public RolledWeapon buildWeaponInstance(final WeaponTemplate template,
                                            final Grade grade,
                                            final int itemLevel) {
        final int powerLevel = effectivePowerLevel(itemLevel, grade);
        final int scaledAttack = rollBaseAttack(template.baseAttack(), powerLevel);
        final int skillSlots = slotCount(grade);
        final List<StatRoll> stats = rollStats(grade, powerLevel);
        final String displayName = buildDisplayName(grade, template.name());

        return new RolledWeapon(
                template.id(),
                template.weaponType(),
                grade,
                itemLevel,
                scaledAttack,
                template.baseSpeed(),
                template.baseCritical(),
                skillSlots,
                stats,
                displayName
        );
    }

    /**
     * 방어구 인스턴스를 생성한다.
     *
     * <p>유효 파워 레벨을 산출하고 랜덤 능력치를 결정한다.
     * 방어구는 스킬 슬롯이 없다.
     *
     * @param template  방어구 템플릿
     * @param grade     결정된 등급
     * @param itemLevel 아이템 레벨
     * @return 생성된 방어구 인스턴스
     */
    public RolledArmor buildArmorInstance(final ArmorTemplate template,
                                          final Grade grade,
                                          final int itemLevel) {
        final int powerLevel = effectivePowerLevel(itemLevel, grade);
        final List<StatRoll> stats = rollStats(grade, powerLevel);
        final String displayName = buildDisplayName(grade, template.name());

        return new RolledArmor(
                template.id(),
                template.slot(),
                grade,
                itemLevel,
                stats,
                displayName
        );
    }

    /**
     * 몬스터 종류에 따른 드랍 카테고리를 결정한다.
     *
     * @param monster 몬스터 템플릿
     * @return 드랍 카테고리
     */
    private DropCategory rollDropCategory(final MonsterTemplate monster) {
        final double roll = randomSource.nextDouble();

        if (monster.boss()) {
            if (roll < BOSS_WEAPON_CHANCE) {
                return DropCategory.WEAPON;
            }
            if (roll < BOSS_WEAPON_CHANCE + BOSS_SKILL_BOOK_CHANCE) {
                return DropCategory.SKILL_BOOK;
            }
            return DropCategory.NONE;
        }

        if (roll < NORMAL_ARMOR_CHANCE) {
            return DropCategory.ARMOR;
        }
        return DropCategory.NONE;
    }

    /**
     * 무기 드랍을 처리한다.
     *
     * @param dungeon         던전 템플릿
     * @param weaponTemplates 드랍 가능한 무기 템플릿 목록
     * @param itemLevel       아이템 레벨
     * @return 무기 드랍 결과
     */
    private DropResult rollWeaponDrop(final DungeonTemplate dungeon,
                                      final List<WeaponTemplate> weaponTemplates,
                                      final int itemLevel) {
        final List<WeaponType> weaponTypes = dungeon.weaponTypes();
        final WeaponType selectedType = weaponTypes.get(randomSource.nextInt(weaponTypes.size()));

        final List<WeaponTemplate> matching = weaponTemplates.stream()
                .filter(w -> w.weaponType() == selectedType)
                .toList();

        if (matching.isEmpty()) {
            return new DropResult(DropCategory.NONE, null, null, null);
        }

        final WeaponTemplate template = matching.get(randomSource.nextInt(matching.size()));
        final Grade grade = rollGrade(dungeon);
        final RolledWeapon weapon = buildWeaponInstance(template, grade, itemLevel);

        return new DropResult(DropCategory.WEAPON, weapon, null, null);
    }

    /**
     * 방어구 드랍을 처리한다.
     *
     * @param dungeon        던전 템플릿
     * @param armorTemplates 드랍 가능한 방어구 템플릿 목록
     * @param itemLevel      아이템 레벨
     * @return 방어구 드랍 결과
     */
    private DropResult rollArmorDrop(final DungeonTemplate dungeon,
                                     final List<ArmorTemplate> armorTemplates,
                                     final int itemLevel) {
        final List<ArmorSlot> armorSlots = dungeon.armorSlots();
        final ArmorSlot selectedSlot = armorSlots.get(randomSource.nextInt(armorSlots.size()));

        final List<ArmorTemplate> matching = armorTemplates.stream()
                .filter(a -> a.slot() == selectedSlot)
                .toList();

        if (matching.isEmpty()) {
            return new DropResult(DropCategory.NONE, null, null, null);
        }

        final ArmorTemplate template = matching.get(randomSource.nextInt(matching.size()));
        final Grade grade = rollGrade(dungeon);
        final RolledArmor armor = buildArmorInstance(template, grade, itemLevel);

        return new DropResult(DropCategory.ARMOR, null, armor, null);
    }

    /**
     * 스킬북 드랍을 처리한다.
     *
     * @param availableSkills 사용 가능한 스킬 목록
     * @return 스킬북 드랍 결과
     */
    private DropResult rollSkillBookDrop(final List<SkillTemplate> availableSkills) {
        if (availableSkills.isEmpty()) {
            return new DropResult(DropCategory.NONE, null, null, null);
        }

        final SkillTemplate selected = availableSkills.get(
                randomSource.nextInt(availableSkills.size()));
        return new DropResult(DropCategory.SKILL_BOOK, null, null, selected.id());
    }

    /**
     * 중복 없이 지정 개수의 StatType을 선택한다.
     *
     * @param count 선택할 개수
     * @return 선택된 StatType 목록
     */
    private List<StatType> pickDistinctStatTypes(final int count) {
        final StatType[] allTypes = StatType.values();
        final List<StatType> pool = new ArrayList<>(List.of(allTypes));
        final List<StatType> selected = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            final int index = randomSource.nextInt(pool.size());
            selected.add(pool.remove(index));
        }

        return selected;
    }

    /**
     * 등급 라벨과 템플릿명을 조합하여 표시명을 생성한다.
     *
     * @param grade        장비 등급
     * @param templateName 템플릿 이름
     * @return 형식: "[등급라벨] 템플릿명"
     */
    private String buildDisplayName(final Grade grade, final String templateName) {
        final String label = GRADE_DISPLAY_LABELS.get(grade);
        return "[" + label + "] " + templateName;
    }
}
