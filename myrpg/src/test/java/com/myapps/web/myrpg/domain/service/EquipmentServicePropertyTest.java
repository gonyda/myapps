package com.myapps.web.myrpg.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.exception.IllegalEquipmentException;
import com.myapps.web.myrpg.domain.model.ArmorSlot;
import com.myapps.web.myrpg.domain.model.DamageType;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.PlayerArmor;
import com.myapps.web.myrpg.domain.model.PlayerInventory;
import com.myapps.web.myrpg.domain.model.PlayerWeapon;
import com.myapps.web.myrpg.domain.model.PlayerWeaponSkill;
import com.myapps.web.myrpg.domain.model.WeaponType;
import com.myapps.web.myrpg.domain.template.SkillTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EquipmentService 착용·스킬 장착 속성 기반 테스트.
 *
 * <p>jqwik을 사용하여 무기/방어구 착용 불변식과 스킬북 장착 규칙을 검증한다.
 *
 * <p><b>Validates: Requirements 26.1, 26.2, 26.3, 26.4, 26.5, 13.1, 13.2, 13.3, 13.4, 13.6</b>
 */
class EquipmentServicePropertyTest {

    private static final Long PLAYER_ID = 1L;
    private static final Long WEAPON_TEMPLATE_ID = 1L;
    private static final Long ARMOR_TEMPLATE_ID = 1L;
    private static final int BASE_ATTACK = 10;
    private static final int BASE_SPEED = 5;
    private static final int BASE_CRITICAL = 3;
    private static final int BASE_DEFENSE = 5;
    private static final int ITEM_LEVEL = 1;
    private static final int SKILL_SLOTS_MIN = 1;
    private static final int SKILL_SLOTS_MAX = 5;
    private static final int WEAPON_COUNT_MIN = 2;
    private static final int WEAPON_COUNT_MAX = 6;
    private static final int ARMOR_COUNT_MIN = 2;
    private static final int ARMOR_COUNT_MAX = 8;
    private static final int OPERATIONS_MIN = 3;
    private static final int OPERATIONS_MAX = 10;
    private static final int SKILL_ID_MIN = 1;
    private static final int SKILL_ID_MAX = 100;
    private static final int INVENTORY_QUANTITY_MIN = 1;
    private static final int INVENTORY_QUANTITY_MAX = 10;
    private static final double DAMAGE_MULTIPLIER = 1.5;
    private static final int MP_COST = 10;

    private final EquipmentService equipmentService = new EquipmentService();

    // --- Providers ---

    @Provide
    Arbitrary<WeaponType> weaponTypeProvider() {
        return Arbitraries.of(WeaponType.values());
    }

    @Provide
    Arbitrary<ArmorSlot> armorSlotProvider() {
        return Arbitraries.of(ArmorSlot.values());
    }

    @Provide
    Arbitrary<Grade> gradeProvider() {
        return Arbitraries.of(Grade.values());
    }

    @Provide
    Arbitrary<DamageType> damageTypeProvider() {
        return Arbitraries.of(DamageType.values());
    }

    @Provide
    Arbitrary<List<PlayerWeapon>> weaponListProvider() {
        final Arbitrary<PlayerWeapon> weaponArb = Combinators.combine(
                Arbitraries.of(WeaponType.values()),
                Arbitraries.of(Grade.values()),
                Arbitraries.integers().between(SKILL_SLOTS_MIN, SKILL_SLOTS_MAX)
        ).as((type, grade, slots) -> new PlayerWeapon(
                PLAYER_ID, WEAPON_TEMPLATE_ID, "[테스트] 무기", type,
                grade, ITEM_LEVEL, BASE_ATTACK, BASE_SPEED, BASE_CRITICAL,
                slots, false));
        return weaponArb.list().ofMinSize(WEAPON_COUNT_MIN).ofMaxSize(WEAPON_COUNT_MAX);
    }

    @Provide
    Arbitrary<List<PlayerArmor>> armorListProvider() {
        final Arbitrary<PlayerArmor> armorArb = Combinators.combine(
                Arbitraries.of(ArmorSlot.values()),
                Arbitraries.of(Grade.values())
        ).as((slot, grade) -> new PlayerArmor(
                PLAYER_ID, ARMOR_TEMPLATE_ID, "[테스트] 방어구", slot,
                grade, BASE_DEFENSE, ITEM_LEVEL, false));
        return armorArb.list().ofMinSize(ARMOR_COUNT_MIN).ofMaxSize(ARMOR_COUNT_MAX);
    }

    @Provide
    Arbitrary<Integer> operationCountProvider() {
        return Arbitraries.integers().between(OPERATIONS_MIN, OPERATIONS_MAX);
    }

    @Provide
    Arbitrary<Integer> skillIdProvider() {
        return Arbitraries.integers().between(SKILL_ID_MIN, SKILL_ID_MAX);
    }

    @Provide
    Arbitrary<Integer> inventoryQuantityProvider() {
        return Arbitraries.integers().between(INVENTORY_QUANTITY_MIN, INVENTORY_QUANTITY_MAX);
    }

    // =====================================================================
    // Property 48: 착용 불변식
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 48: 착용 불변식
    /**
     * 임의의 무기/방어구 착용·해제·교체 연산 시퀀스 후,
     * 무기는 최대 1개만 착용 상태이며 방어구는 부위별 최대 1개만 착용 상태이다.
     *
     * <p><b>Validates: Requirements 26.1, 26.2, 26.3, 26.4, 26.5</b>
     */
    @Property(tries = 100)
    void equipInvariantHoldsAfterRandomOperations(
            @ForAll("weaponListProvider") final List<PlayerWeapon> weapons,
            @ForAll("armorListProvider") final List<PlayerArmor> armors,
            @ForAll("operationCountProvider") final int operations) {

        final List<PlayerWeapon> mutableWeapons = new ArrayList<>(weapons);
        final List<PlayerArmor> mutableArmors = new ArrayList<>(armors);

        for (int i = 0; i < operations; i++) {
            performRandomEquipOperation(mutableWeapons, mutableArmors, i);
        }

        assertWeaponInvariant(mutableWeapons);
        assertArmorInvariant(mutableArmors);
    }

    // Feature: myrpg-gen1-mvp, Property 48: 착용 불변식
    /**
     * 무기 착용 시 기존 착용 무기를 해제한 뒤 새 무기를 착용하므로,
     * 연속 착용 후 항상 최대 1개만 착용 상태이다.
     *
     * <p><b>Validates: Requirements 26.1, 26.2</b>
     */
    @Property(tries = 100)
    void atMostOneWeaponEquippedAfterSequentialEquips(
            @ForAll("weaponListProvider") final List<PlayerWeapon> weapons) {

        final List<PlayerWeapon> mutableWeapons = new ArrayList<>(weapons);

        for (final PlayerWeapon weapon : mutableWeapons) {
            equipmentService.equipWeapon(mutableWeapons, weapon, false);
            assertWeaponInvariant(mutableWeapons);
        }
    }

    // Feature: myrpg-gen1-mvp, Property 48: 착용 불변식
    /**
     * 방어구 착용 시 같은 부위의 기존 방어구를 해제한 뒤 새 방어구를 착용하므로,
     * 연속 착용 후 각 부위별 최대 1개만 착용 상태이다.
     *
     * <p><b>Validates: Requirements 26.3, 26.4, 26.5</b>
     */
    @Property(tries = 100)
    void atMostOneArmorPerSlotAfterSequentialEquips(
            @ForAll("armorListProvider") final List<PlayerArmor> armors) {

        final List<PlayerArmor> mutableArmors = new ArrayList<>(armors);

        for (final PlayerArmor armor : mutableArmors) {
            equipmentService.equipArmor(mutableArmors, armor, false);
            assertArmorInvariant(mutableArmors);
        }
    }

    // =====================================================================
    // Property 49: 스킬북 장착 규칙
    // =====================================================================

    // Feature: myrpg-gen1-mvp, Property 49: 스킬북 장착 규칙
    /**
     * 무기 타입과 스킬의 weaponType이 다르면 IllegalEquipmentException이 발생하고
     * 인벤토리 수량은 변하지 않는다.
     *
     * <p><b>Validates: Requirements 13.1</b>
     */
    @Property(tries = 100)
    void rejectSkillBookWhenWeaponTypeMismatch(
            @ForAll("weaponTypeProvider") final WeaponType weaponType,
            @ForAll("damageTypeProvider") final DamageType damageType,
            @ForAll("inventoryQuantityProvider") final int quantity) {

        final WeaponType mismatchedType = pickDifferentWeaponType(weaponType);
        final PlayerWeapon weapon = createWeaponWithType(weaponType);
        final List<PlayerWeaponSkill> currentSkills = new ArrayList<>();
        final SkillTemplate skillTemplate = new SkillTemplate(
                1L, "테스트스킬", mismatchedType, damageType, DAMAGE_MULTIPLIER, MP_COST);
        final PlayerInventory inventory = new PlayerInventory(
                PLAYER_ID, ItemType.SKILL_BOOK, 1L, quantity);

        final int quantityBefore = inventory.getQuantity();

        assertThatThrownBy(() -> equipmentService.attachSkillBook(
                weapon, currentSkills, skillTemplate, inventory, Optional.empty()))
                .isInstanceOf(IllegalEquipmentException.class);

        assertThat(inventory.getQuantity()).isEqualTo(quantityBefore);
    }

    // Feature: myrpg-gen1-mvp, Property 49: 스킬북 장착 규칙
    /**
     * 동일 스킬이 이미 장착된 경우 IllegalEquipmentException이 발생하고
     * 인벤토리 수량은 변하지 않는다.
     *
     * <p><b>Validates: Requirements 13.2</b>
     */
    @Property(tries = 100)
    void rejectSkillBookWhenDuplicateSkillAttached(
            @ForAll("weaponTypeProvider") final WeaponType weaponType,
            @ForAll("skillIdProvider") final int skillId,
            @ForAll("inventoryQuantityProvider") final int quantity) {

        final PlayerWeapon weapon = createWeaponWithType(weaponType);
        final PlayerWeaponSkill existingSkill = new PlayerWeaponSkill(
                PLAYER_ID, (long) skillId, 0);
        final List<PlayerWeaponSkill> currentSkills = new ArrayList<>(List.of(existingSkill));
        final SkillTemplate skillTemplate = new SkillTemplate(
                skillId, "중복스킬", weaponType, DamageType.PHYSICAL, DAMAGE_MULTIPLIER, MP_COST);
        final PlayerInventory inventory = new PlayerInventory(
                PLAYER_ID, ItemType.SKILL_BOOK, (long) skillId, quantity);

        final int quantityBefore = inventory.getQuantity();

        assertThatThrownBy(() -> equipmentService.attachSkillBook(
                weapon, currentSkills, skillTemplate, inventory, Optional.empty()))
                .isInstanceOf(IllegalEquipmentException.class);

        assertThat(inventory.getQuantity()).isEqualTo(quantityBefore);
    }

    // Feature: myrpg-gen1-mvp, Property 49: 스킬북 장착 규칙
    /**
     * 빈 슬롯이 있을 때 스킬북 장착에 성공하면 인벤토리 수량이 1 감소하고
     * 새 스킬이 무기 슬롯에 귀속된다.
     *
     * <p><b>Validates: Requirements 13.3, 13.4</b>
     */
    @Property(tries = 100)
    void successfulAttachDecreasesQuantityAndBindsSkill(
            @ForAll("weaponTypeProvider") final WeaponType weaponType,
            @ForAll("skillIdProvider") final int skillId,
            @ForAll("inventoryQuantityProvider") final int quantity) {

        final PlayerWeapon weapon = createWeaponWithTypeAndSlots(weaponType, SKILL_SLOTS_MAX);
        final List<PlayerWeaponSkill> currentSkills = new ArrayList<>();
        final SkillTemplate skillTemplate = new SkillTemplate(
                skillId, "신규스킬", weaponType, DamageType.PHYSICAL, DAMAGE_MULTIPLIER, MP_COST);
        final PlayerInventory inventory = new PlayerInventory(
                PLAYER_ID, ItemType.SKILL_BOOK, (long) skillId, quantity);

        final int quantityBefore = inventory.getQuantity();

        final Optional<PlayerWeaponSkill> result = equipmentService.attachSkillBook(
                weapon, currentSkills, skillTemplate, inventory, Optional.empty());

        assertThat(result).isPresent();
        assertThat(result.get().getSkillId()).isEqualTo((long) skillId);
        assertThat(result.get().getSlotIndex()).isGreaterThanOrEqualTo(0);
        assertThat(inventory.getQuantity()).isEqualTo(quantityBefore - 1);
    }

    // Feature: myrpg-gen1-mvp, Property 49: 스킬북 장착 규칙
    /**
     * 슬롯이 모두 차 있을 때 덮어쓰기 슬롯 인덱스를 지정하면 기존 스킬이 새 스킬로
     * 교체되고 인벤토리 수량이 1 감소한다. 기존 스킬은 영구 소멸된다.
     *
     * <p><b>Validates: Requirements 13.6</b>
     */
    @Property(tries = 100)
    void overwriteReplacesOldSkillAndDecreasesQuantity(
            @ForAll("weaponTypeProvider") final WeaponType weaponType,
            @ForAll("inventoryQuantityProvider") final int quantity) {

        final int slotCount = SKILL_SLOTS_MIN;
        final PlayerWeapon weapon = createWeaponWithTypeAndSlots(weaponType, slotCount);
        final long oldSkillId = 1L;
        final long newSkillId = 2L;
        final PlayerWeaponSkill existingSkill = new PlayerWeaponSkill(PLAYER_ID, oldSkillId, 0);
        final List<PlayerWeaponSkill> currentSkills = new ArrayList<>(List.of(existingSkill));
        final SkillTemplate skillTemplate = new SkillTemplate(
                newSkillId, "교체스킬", weaponType, DamageType.PHYSICAL, DAMAGE_MULTIPLIER, MP_COST);
        final PlayerInventory inventory = new PlayerInventory(
                PLAYER_ID, ItemType.SKILL_BOOK, newSkillId, quantity);

        final int quantityBefore = inventory.getQuantity();

        final Optional<PlayerWeaponSkill> result = equipmentService.attachSkillBook(
                weapon, currentSkills, skillTemplate, inventory, Optional.of(0));

        assertThat(result).isEmpty();
        assertThat(existingSkill.getSkillId()).isEqualTo(newSkillId);
        assertThat(inventory.getQuantity()).isEqualTo(quantityBefore - 1);
    }

    // --- Helper Methods ---

    /**
     * 랜덤 착용 연산을 수행한다 (인덱스 기반으로 무기 또는 방어구 선택).
     */
    private void performRandomEquipOperation(final List<PlayerWeapon> weapons,
                                             final List<PlayerArmor> armors,
                                             final int operationIndex) {
        if (operationIndex % 2 == 0 && !weapons.isEmpty()) {
            final int weaponIndex = operationIndex % weapons.size();
            equipmentService.equipWeapon(weapons, weapons.get(weaponIndex), false);
        } else if (!armors.isEmpty()) {
            final int armorIndex = operationIndex % armors.size();
            equipmentService.equipArmor(armors, armors.get(armorIndex), false);
        }
    }

    /**
     * 무기 착용 불변식을 검증한다: 착용 상태인 무기가 최대 1개.
     */
    private void assertWeaponInvariant(final List<PlayerWeapon> weapons) {
        final long equippedCount = weapons.stream()
                .filter(PlayerWeapon::isEquipped)
                .count();
        assertThat(equippedCount).isLessThanOrEqualTo(1);
    }

    /**
     * 방어구 착용 불변식을 검증한다: 각 부위별 착용 상태인 방어구가 최대 1개.
     */
    private void assertArmorInvariant(final List<PlayerArmor> armors) {
        final Map<ArmorSlot, Long> equippedBySlot = armors.stream()
                .filter(PlayerArmor::isEquipped)
                .collect(Collectors.groupingBy(PlayerArmor::getArmorSlot, Collectors.counting()));

        for (final Map.Entry<ArmorSlot, Long> entry : equippedBySlot.entrySet()) {
            assertThat(entry.getValue())
                    .as("부위 %s에 착용된 방어구 수", entry.getKey())
                    .isLessThanOrEqualTo(1);
        }
    }

    /**
     * 주어진 무기 타입과 다른 타입을 반환한다.
     */
    private WeaponType pickDifferentWeaponType(final WeaponType original) {
        final WeaponType[] allTypes = WeaponType.values();
        for (final WeaponType type : allTypes) {
            if (type != original) {
                return type;
            }
        }
        return original;
    }

    /**
     * 지정된 무기 타입으로 테스트용 무기를 생성한다.
     */
    private PlayerWeapon createWeaponWithType(final WeaponType type) {
        return new PlayerWeapon(PLAYER_ID, WEAPON_TEMPLATE_ID, "[테스트] 무기", type,
                Grade.COMMON, ITEM_LEVEL, BASE_ATTACK, BASE_SPEED, BASE_CRITICAL,
                SKILL_SLOTS_MAX, true);
    }

    /**
     * 지정된 무기 타입과 슬롯 수로 테스트용 무기를 생성한다.
     */
    private PlayerWeapon createWeaponWithTypeAndSlots(final WeaponType type, final int slots) {
        return new PlayerWeapon(PLAYER_ID, WEAPON_TEMPLATE_ID, "[테스트] 무기", type,
                Grade.COMMON, ITEM_LEVEL, BASE_ATTACK, BASE_SPEED, BASE_CRITICAL,
                slots, true);
    }
}
