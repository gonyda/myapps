package com.myapps.web.myrpg.domain.model.vo;

import java.util.List;

import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.WeaponType;

/**
 * 드랍 롤로 생성된 무기 인스턴스를 나타내는 값 객체.
 *
 * <p>템플릿 기반으로 등급·능력치가 랜덤 결정된 개별 무기이다.
 */
public record RolledWeapon(long templateId, WeaponType weaponType, Grade grade, int itemLevel,
                           int baseAttack, int baseSpeed, int baseCritical, int skillSlots,
                           List<StatRoll> stats, String displayName) {
}
