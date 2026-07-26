package com.myapps.web.myrpg.domain.service;

import java.util.List;
import java.util.Optional;

import com.myapps.web.myrpg.domain.exception.IllegalEquipmentException;
import com.myapps.web.myrpg.domain.model.PlayerArmor;
import com.myapps.web.myrpg.domain.model.PlayerInventory;
import com.myapps.web.myrpg.domain.model.PlayerWeapon;
import com.myapps.web.myrpg.domain.model.PlayerWeaponSkill;
import com.myapps.web.myrpg.domain.template.SkillTemplate;

/**
 * 장비 착용·스킬 장착 관련 순수 도메인 규칙 서비스.
 *
 * <p>무기/방어구 착용 불변식(무기 최대 1개, 방어구 부위별 최대 1개)과
 * 스킬북 장착 규칙(타입 호환·중복 방지·슬롯 관리·소모·덮어쓰기)을 캡슐화한다.
 * 리포지토리 의존 없는 순수 서비스이며, 엔티티 영속화는 호출자(application 계층)의 책임이다.
 */
public class EquipmentService {

    /**
     * 무기를 착용한다.
     *
     * <p>한 플레이어가 착용한 무기를 최대 1개로 유지한다.
     * 기존 착용 무기가 있으면 해제한 뒤 새 무기를 착용한다.
     * 던전 진입 이후에는 무기 변경을 허용하지 않는다.
     *
     * @param playerWeapons 플레이어의 전체 무기 목록
     * @param weaponToEquip 착용할 무기 인스턴스
     * @param isInDungeon   던전 진행 중 여부
     * @throws IllegalEquipmentException 던전 진행 중 장비 변경을 시도하는 경우
     */
    public void equipWeapon(final List<PlayerWeapon> playerWeapons,
                            final PlayerWeapon weaponToEquip,
                            final boolean isInDungeon) {
        validateNotInDungeon(isInDungeon);
        unequipCurrentWeapon(playerWeapons);
        weaponToEquip.changeEquipped(true);
    }

    /**
     * 방어구를 착용한다.
     *
     * <p>한 플레이어가 각 방어구 부위별로 착용한 방어구를 최대 1개로 유지한다.
     * 같은 부위에 기존 착용 방어구가 있으면 자동 해제한 뒤 새 방어구를 착용한다.
     * 던전 진입 이후에는 방어구 변경을 허용하지 않는다.
     *
     * @param playerArmors 플레이어의 전체 방어구 목록
     * @param armorToEquip 착용할 방어구 인스턴스
     * @param isInDungeon  던전 진행 중 여부
     * @throws IllegalEquipmentException 던전 진행 중 장비 변경을 시도하는 경우
     */
    public void equipArmor(final List<PlayerArmor> playerArmors,
                           final PlayerArmor armorToEquip,
                           final boolean isInDungeon) {
        validateNotInDungeon(isInDungeon);
        unequipSameSlotArmor(playerArmors, armorToEquip);
        armorToEquip.changeEquipped(true);
    }

    /**
     * 스킬북을 무기에 장착한다.
     *
     * <p>스킬북 장착 시 아래 규칙을 적용한다:
     * <ul>
     *   <li>무기 타입이 스킬의 weaponType과 일치하지 않으면 장착 거부</li>
     *   <li>동일 스킬이 이미 장착되어 있으면 중복 장착 거부</li>
     *   <li>빈 슬롯이 없고 덮어쓸 슬롯이 지정되지 않으면 장착 중단(슬롯 선택 요구)</li>
     *   <li>장착 성공 시 인벤토리에서 스킬북 수량 1 감소</li>
     *   <li>덮어쓰기 시 기존 스킬 영구 소멸(skillId 변경)</li>
     * </ul>
     *
     * @param weapon         대상 무기 인스턴스
     * @param currentSkills  해당 무기에 현재 장착된 스킬 목록
     * @param skillTemplate  장착하려는 스킬 템플릿
     * @param inventoryItem  스킬북 인벤토리 항목
     * @param overwriteSlot  덮어쓸 슬롯 인덱스 (빈 슬롯이 없을 때 사용, 없으면 empty)
     * @return 새로 생성된 PlayerWeaponSkill (빈 슬롯에 장착된 경우), 또는 empty (덮어쓰기로 기존 엔티티를 변경한 경우)
     * @throws IllegalEquipmentException 타입 불일치, 중복 장착, 슬롯 부족 시
     */
    public Optional<PlayerWeaponSkill> attachSkillBook(final PlayerWeapon weapon,
                                                       final List<PlayerWeaponSkill> currentSkills,
                                                       final SkillTemplate skillTemplate,
                                                       final PlayerInventory inventoryItem,
                                                       final Optional<Integer> overwriteSlot) {
        validateWeaponTypeCompatible(weapon, skillTemplate);
        validateNoDuplicate(currentSkills, skillTemplate);

        final int emptySlotIndex = findEmptySlotIndex(weapon, currentSkills);

        if (emptySlotIndex >= 0) {
            consumeSkillBook(inventoryItem);
            final PlayerWeaponSkill newSkill = new PlayerWeaponSkill(
                    weapon.getId(), skillTemplate.id(), emptySlotIndex);
            return Optional.of(newSkill);
        }

        final int slotIndex = overwriteSlot.orElseThrow(() ->
                new IllegalEquipmentException("빈 스킬슬롯이 없습니다. 덮어쓸 슬롯을 선택해주세요."));

        validateSlotIndexInRange(weapon, slotIndex);
        overwriteSkillAtSlot(currentSkills, slotIndex, skillTemplate);
        consumeSkillBook(inventoryItem);
        return Optional.empty();
    }

    /**
     * 던전 진행 중 장비 변경을 방지한다.
     *
     * @param isInDungeon 던전 진행 중 여부
     * @throws IllegalEquipmentException 던전 진행 중인 경우
     */
    private void validateNotInDungeon(final boolean isInDungeon) {
        if (isInDungeon) {
            throw new IllegalEquipmentException("던전 진행 중에는 장비를 변경할 수 없습니다.");
        }
    }

    /**
     * 현재 착용 중인 무기를 해제한다.
     *
     * @param playerWeapons 플레이어의 전체 무기 목록
     */
    private void unequipCurrentWeapon(final List<PlayerWeapon> playerWeapons) {
        for (final PlayerWeapon weapon : playerWeapons) {
            if (weapon.isEquipped()) {
                weapon.changeEquipped(false);
            }
        }
    }

    /**
     * 같은 부위에 착용 중인 방어구를 해제한다.
     *
     * @param playerArmors 플레이어의 전체 방어구 목록
     * @param armorToEquip 새로 착용할 방어구 (부위 비교 기준)
     */
    private void unequipSameSlotArmor(final List<PlayerArmor> playerArmors,
                                      final PlayerArmor armorToEquip) {
        for (final PlayerArmor armor : playerArmors) {
            if (armor.isEquipped() && armor.getArmorSlot() == armorToEquip.getArmorSlot()) {
                armor.changeEquipped(false);
            }
        }
    }

    /**
     * 무기 타입과 스킬의 weaponType 호환성을 검증한다.
     *
     * @param weapon        대상 무기
     * @param skillTemplate 장착하려는 스킬 템플릿
     * @throws IllegalEquipmentException 타입 불일치 시
     */
    private void validateWeaponTypeCompatible(final PlayerWeapon weapon,
                                              final SkillTemplate skillTemplate) {
        if (weapon.getWeaponType() != skillTemplate.weaponType()) {
            throw new IllegalEquipmentException(
                    "무기 타입이 스킬과 호환되지 않습니다. 무기: " + weapon.getWeaponType()
                            + ", 스킬 요구: " + skillTemplate.weaponType());
        }
    }

    /**
     * 동일 스킬 중복 장착을 검증한다.
     *
     * @param currentSkills 현재 장착된 스킬 목록
     * @param skillTemplate 장착하려는 스킬 템플릿
     * @throws IllegalEquipmentException 동일 스킬이 이미 장착된 경우
     */
    private void validateNoDuplicate(final List<PlayerWeaponSkill> currentSkills,
                                     final SkillTemplate skillTemplate) {
        for (final PlayerWeaponSkill skill : currentSkills) {
            if (skill.getSkillId() == skillTemplate.id()) {
                throw new IllegalEquipmentException(
                        "동일한 스킬이 이미 장착되어 있습니다. 스킬 ID: " + skillTemplate.id());
            }
        }
    }

    /**
     * 무기의 빈 스킬슬롯 인덱스를 찾는다.
     *
     * @param weapon        대상 무기
     * @param currentSkills 현재 장착된 스킬 목록
     * @return 빈 슬롯 인덱스 (0부터 시작), 빈 슬롯이 없으면 -1
     */
    private int findEmptySlotIndex(final PlayerWeapon weapon,
                                   final List<PlayerWeaponSkill> currentSkills) {
        final int totalSlots = weapon.getSkillSlots();
        final boolean[] occupied = new boolean[totalSlots];

        for (final PlayerWeaponSkill skill : currentSkills) {
            final int index = skill.getSlotIndex();
            if (index >= 0 && index < totalSlots) {
                occupied[index] = true;
            }
        }

        for (int i = 0; i < totalSlots; i++) {
            if (!occupied[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 슬롯 인덱스가 무기의 스킬슬롯 범위 내인지 검증한다.
     *
     * @param weapon    대상 무기
     * @param slotIndex 슬롯 인덱스
     * @throws IllegalEquipmentException 범위를 벗어난 경우
     */
    private void validateSlotIndexInRange(final PlayerWeapon weapon, final int slotIndex) {
        if (slotIndex < 0 || slotIndex >= weapon.getSkillSlots()) {
            throw new IllegalEquipmentException(
                    "유효하지 않은 슬롯 인덱스입니다. 인덱스: " + slotIndex
                            + ", 최대: " + (weapon.getSkillSlots() - 1));
        }
    }

    /**
     * 지정된 슬롯의 스킬을 새 스킬로 덮어쓴다.
     *
     * <p>기존 스킬은 영구 소멸된다 (skillId가 변경됨).
     *
     * @param currentSkills 현재 장착된 스킬 목록
     * @param slotIndex     덮어쓸 슬롯 인덱스
     * @param skillTemplate 새로 장착할 스킬 템플릿
     */
    private void overwriteSkillAtSlot(final List<PlayerWeaponSkill> currentSkills,
                                      final int slotIndex,
                                      final SkillTemplate skillTemplate) {
        for (final PlayerWeaponSkill skill : currentSkills) {
            if (skill.getSlotIndex() == slotIndex) {
                skill.changeSkillId(skillTemplate.id());
                return;
            }
        }
    }

    /**
     * 스킬북을 인벤토리에서 소모한다 (수량 1 감소).
     *
     * @param inventoryItem 스킬북 인벤토리 항목
     */
    private void consumeSkillBook(final PlayerInventory inventoryItem) {
        inventoryItem.changeQuantity(inventoryItem.getQuantity() - 1);
    }
}
