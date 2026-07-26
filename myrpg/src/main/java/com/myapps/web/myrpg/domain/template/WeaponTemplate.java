package com.myapps.web.myrpg.domain.template;

import com.myapps.web.myrpg.domain.model.WeaponType;

/**
 * 무기 마스터 데이터 템플릿.
 *
 * <p>JSON 데이터 파일에서 로딩되는 불변 무기 정의 레코드이다.
 * 인스턴스 생성 시 기본 수치 참조용으로 사용된다.
 */
public record WeaponTemplate(long id, String name, WeaponType weaponType,
                             int baseAttack, int baseSpeed, int baseCritical, int baseValue) {
}
