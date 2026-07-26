package com.myapps.web.myrpg.domain.template;

import com.myapps.web.myrpg.domain.model.DamageType;
import com.myapps.web.myrpg.domain.model.WeaponType;

/**
 * 스킬 마스터 데이터 템플릿.
 *
 * <p>JSON 데이터 파일에서 로딩되는 불변 스킬 정의 레코드이다.
 * 스킬북 장착 시 무기 타입 호환성 검증 및 전투 데미지 산출에 사용된다.
 */
public record SkillTemplate(long id, String name, WeaponType weaponType,
                            DamageType damageType, double damageMultiplier, int mpCost) {
}
