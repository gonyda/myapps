package com.myapps.web.myrpg.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EquipmentService 단위 테스트.
 *
 * <p>무기/방어구 착용 불변식 및 스킬북 장착 규칙을 검증한다.
 */
class EquipmentServiceTest {

    private EquipmentService equipmentService;

    @BeforeEach
    void setUp() {
        equipmentService = new EquipmentService();
    }

    @Test
    void should_equipWeapon_when_noCurrentlyEquipped() {
        final PlayerWeapon weapon = createWeapon(1L, WeaponType.SWORD, false);
        final List<PlayerWeapon> weapons = new ArrayList<>(List.of(weapon));

        equipmentService.equipWeapon(weapons, weapon, false);

        assertThat(weapon.isEquipped()).isTrue();
    }

    @Test
    void should_unequipPrevious_when_equipNewWeapon() {
        final PlayerWeapon oldWeapon = createWeapon(1L, WeaponType.SWORD, true);
        final PlayerWeapon newWeapon = createWeapon(2L, WeaponType.AXE, false);
        final List<PlayerWeapon> weapons = new ArrayList<>(List.of(oldWeapon, newWeapon));

        equipmentService.equipWeapon(weapons, newWeapon, false);

        assertThat(oldWeapon.isEquipped()).isFalse();
        assertThat(newWeapon.isEquipped()).isTrue();
    }

    @Test
    void should_rejectEquipWeapon_when_inDungeon() {
        final PlayerWeapon weapon = createWeapon(1L, WeaponType.SWORD, false);
        final List<PlayerWeapon> weapons = new ArrayList<>(List.of(weapon));

        assertThatThrownBy(() -> equipmentService.equipWeapon(weapons, weapon, true))
                .isInstanceOf(IllegalEquipmentException.class)
                .hasMessageContaining("던전 진행 중");
    }

    @Test
    void should_equipArmor_when_noSameSlotEquipped() {
        final PlayerArmor armor = createArmor(1L, ArmorSlot.HELMET, false);
        final List<PlayerArmor> armors = new ArrayList<>(List.of(armor));

        equipmentService.equipArmor(armors, armor, false);

        assertThat(armor.isEquipped()).isTrue();
    }

    @Test
    void should_unequipSameSlot_when_equipNewArmor() {
        final PlayerArmor oldArmor = createArmor(1L, ArmorSlot.CHEST, true);
        final PlayerArmor newArmor = createArmor(2L, ArmorSlot.CHEST, false);
        final List<PlayerArmor> armors = new ArrayList<>(List.of(oldArmor, newArmor));

        equipmentService.equipArmor(armors, newArmor, false);

        assertThat(oldArmor.isEquipped()).isFalse();
        assertThat(newArmor.isEquipped()).isTrue();
    }

    @Test
    void should_notUnequipDifferentSlot_when_equipArmor() {
        final PlayerArmor helmetArmor = createArmor(1L, ArmorSlot.HELMET, true);
        final PlayerArmor chestArmor = createArmor(2L, ArmorSlot.CHEST, false);
        final List<PlayerArmor> armors = new ArrayList<>(List.of(helmetArmor, chestArmor));

        equipmentService.equipArmor(armors, chestArmor, false);

        assertThat(helmetArmor.isEquipped()).isTrue();
        assertThat(chestArmor.isEquipped()).isTrue();
    }

    @Test
    void should_rejectEquipArmor_when_inDungeon() {
        final PlayerArmor armor = createArmor(1L, ArmorSlot.BOOTS, false);
        final List<PlayerArmor> armors = new ArrayList<>(List.of(armor));

        assertThatThrownBy(() -> equipmentService.equipArmor(armors, armor, true))
                .isInstanceOf(IllegalEquipmentException.class)
                .hasMessageContaining("던전 진행 중");
    }

    @Test
    void should_attachSkillBook_when_emptySlotAvailable() {
        final PlayerWeapon weapon = createWeapon(10L, WeaponType.SWORD, true);
        final List<PlayerWeaponSkill> currentSkills = new ArrayList<>();
        final SkillTemplate skill = new SkillTemplate(1L, "베기", WeaponType.SWORD,
                DamageType.PHYSICAL, 1.5, 10);
        final PlayerInventory inventory = new PlayerInventory(1L, ItemType.SKILL_BOOK, 1L, 3);

        final Optional<PlayerWeaponSkill> result = equipmentService.attachSkillBook(
                weapon, currentSkills, skill, inventory, Optional.empty());

        assertThat(result).isPresent();
        assertThat(result.get().getSkillId()).isEqualTo(1L);
        assertThat(result.get().getSlotIndex()).isEqualTo(0);
        assertThat(inventory.getQuantity()).isEqualTo(2);
    }

    @Test
    void should_rejectAttach_when_weaponTypeIncompatible() {
        final PlayerWeapon weapon = createWeapon(10L, WeaponType.SWORD, true);
        final List<PlayerWeaponSkill> currentSkills = new ArrayList<>();
        final SkillTemplate skill = new SkillTemplate(2L, "파이어볼", WeaponType.STAFF,
                DamageType.MAGICAL, 2.0, 20);
        final PlayerInventory inventory = new PlayerInventory(1L, ItemType.SKILL_BOOK, 2L, 1);

        assertThatThrownBy(() -> equipmentService.attachSkillBook(
                weapon, currentSkills, skill, inventory, Optional.empty()))
                .isInstanceOf(IllegalEquipmentException.class)
                .hasMessageContaining("호환되지 않습니다");
    }

    @Test
    void should_rejectAttach_when_duplicateSkill() {
        final PlayerWeapon weapon = createWeapon(10L, WeaponType.SWORD, true);
        final PlayerWeaponSkill existingSkill = new PlayerWeaponSkill(10L, 1L, 0);
        final List<PlayerWeaponSkill> currentSkills = new ArrayList<>(List.of(existingSkill));
        final SkillTemplate skill = new SkillTemplate(1L, "베기", WeaponType.SWORD,
                DamageType.PHYSICAL, 1.5, 10);
        final PlayerInventory inventory = new PlayerInventory(1L, ItemType.SKILL_BOOK, 1L, 2);

        assertThatThrownBy(() -> equipmentService.attachSkillBook(
                weapon, currentSkills, skill, inventory, Optional.empty()))
                .isInstanceOf(IllegalEquipmentException.class)
                .hasMessageContaining("동일한 스킬");
    }

    @Test
    void should_requireSlotSelection_when_noEmptySlotAndNoOverwrite() {
        final PlayerWeapon weapon = createWeaponWithSlots(10L, WeaponType.SWORD, 1);
        final PlayerWeaponSkill existingSkill = new PlayerWeaponSkill(10L, 1L, 0);
        final List<PlayerWeaponSkill> currentSkills = new ArrayList<>(List.of(existingSkill));
        final SkillTemplate newSkill = new SkillTemplate(2L, "찌르기", WeaponType.SWORD,
                DamageType.PHYSICAL, 1.8, 12);
        final PlayerInventory inventory = new PlayerInventory(1L, ItemType.SKILL_BOOK, 2L, 1);

        assertThatThrownBy(() -> equipmentService.attachSkillBook(
                weapon, currentSkills, newSkill, inventory, Optional.empty()))
                .isInstanceOf(IllegalEquipmentException.class)
                .hasMessageContaining("빈 스킬슬롯이 없습니다");
    }

    @Test
    void should_overwriteSkill_when_slotIndexProvided() {
        final PlayerWeapon weapon = createWeaponWithSlots(10L, WeaponType.SWORD, 1);
        final PlayerWeaponSkill existingSkill = new PlayerWeaponSkill(10L, 1L, 0);
        final List<PlayerWeaponSkill> currentSkills = new ArrayList<>(List.of(existingSkill));
        final SkillTemplate newSkill = new SkillTemplate(2L, "찌르기", WeaponType.SWORD,
                DamageType.PHYSICAL, 1.8, 12);
        final PlayerInventory inventory = new PlayerInventory(1L, ItemType.SKILL_BOOK, 2L, 2);

        final Optional<PlayerWeaponSkill> result = equipmentService.attachSkillBook(
                weapon, currentSkills, newSkill, inventory, Optional.of(0));

        assertThat(result).isEmpty();
        assertThat(existingSkill.getSkillId()).isEqualTo(2L);
        assertThat(inventory.getQuantity()).isEqualTo(1);
    }

    @Test
    void should_consumeSkillBook_when_attachSucceeds() {
        final PlayerWeapon weapon = createWeaponWithSlots(10L, WeaponType.BOW, 3);
        final List<PlayerWeaponSkill> currentSkills = new ArrayList<>();
        final SkillTemplate skill = new SkillTemplate(5L, "관통사격", WeaponType.BOW,
                DamageType.PHYSICAL, 1.6, 8);
        final PlayerInventory inventory = new PlayerInventory(1L, ItemType.SKILL_BOOK, 5L, 5);

        equipmentService.attachSkillBook(weapon, currentSkills, skill, inventory, Optional.empty());

        assertThat(inventory.getQuantity()).isEqualTo(4);
    }

    private PlayerWeapon createWeapon(final Long playerId, final WeaponType type,
                                      final boolean equipped) {
        return new PlayerWeapon(playerId, 1L, "[일반] 테스트 무기", type,
                Grade.COMMON, 1, 10, 5, 0, 2, equipped);
    }

    private PlayerWeapon createWeaponWithSlots(final Long playerId, final WeaponType type,
                                               final int skillSlots) {
        return new PlayerWeapon(playerId, 1L, "[일반] 테스트 무기", type,
                Grade.COMMON, 1, 10, 5, 0, skillSlots, true);
    }

    private PlayerArmor createArmor(final Long playerId, final ArmorSlot slot,
                                    final boolean equipped) {
        return new PlayerArmor(playerId, 1L, "[일반] 테스트 방어구", slot,
                Grade.COMMON, 5, 1, equipped);
    }
}
