package com.myapps.web.myrpg.domain.model.vo;

import com.myapps.web.myrpg.domain.model.DropCategory;

/**
 * 몬스터 처치 시 드랍 판정 결과를 나타내는 값 객체.
 *
 * <p>카테고리에 따라 weapon, armor, skillId 중 하나만 유효하며 나머지는 null이다.
 */
public record DropResult(DropCategory category, RolledWeapon weapon,
                         RolledArmor armor, Long skillId) {
}
