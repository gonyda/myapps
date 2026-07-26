package com.myapps.web.myrpg.domain.model.vo;

import com.myapps.web.myrpg.domain.model.StatType;

/**
 * 장비에 랜덤 부여된 단일 능력치 롤 결과를 나타내는 값 객체.
 *
 * <p>능력치 종류와 수치 값으로 구성된다.
 */
public record StatRoll(StatType statType, int value) {
}
