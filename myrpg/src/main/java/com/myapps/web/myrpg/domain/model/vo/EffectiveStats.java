package com.myapps.web.myrpg.domain.model.vo;

import com.myapps.web.myrpg.domain.model.DamageType;

/**
 * 전투 시 사용되는 캐릭터의 유효 스탯을 나타내는 값 객체.
 *
 * <p>기본 스탯 + 무기 base값 + 장비 랜덤 스탯을 합산한 최종 전투 수치이다.
 */
public record EffectiveStats(int attack, int defense, int speed, int critical,
                             int maxHp, DamageType damageType) {
}
