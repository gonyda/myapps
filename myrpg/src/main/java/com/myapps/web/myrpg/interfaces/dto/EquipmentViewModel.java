package com.myapps.web.myrpg.interfaces.dto;

import java.util.List;

import com.myapps.web.myrpg.domain.model.PlayerArmor;
import com.myapps.web.myrpg.domain.model.PlayerWeapon;
import com.myapps.web.myrpg.domain.model.PlayerWeaponSkill;
import com.myapps.web.myrpg.domain.template.SkillTemplate;

/**
 * 장비 관리 화면에 전달할 뷰 모델.
 *
 * <p>무기/방어구 목록과 착용 상태, 스킬북 목록, 장착 가능한 스킬을 표현한다.
 */
public record EquipmentViewModel(List<PlayerWeapon> weapons, List<PlayerArmor> armors,
                                 PlayerWeapon equippedWeapon, List<PlayerArmor> equippedArmors,
                                 List<PlayerWeaponSkill> weaponSkills,
                                 List<SkillTemplate> availableSkills,
                                 List<SkillBookInfo> skillBooks) {

    /**
     * 보유 스킬북 정보를 표현하는 레코드.
     *
     * @param skillId  스킬 ID
     * @param name     스킬 이름
     * @param quantity 보유 수량
     */
    public record SkillBookInfo(long skillId, String name, int quantity) {
    }
}
