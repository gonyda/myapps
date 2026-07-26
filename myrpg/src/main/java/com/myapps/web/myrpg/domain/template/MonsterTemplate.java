package com.myapps.web.myrpg.domain.template;

import com.myapps.web.myrpg.domain.model.DamageType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 몬스터 마스터 데이터 템플릿.
 *
 * <p>JSON 데이터 파일에서 로딩되는 불변 몬스터 정의 레코드이다.
 * 전투 시 몬스터 스탯 및 보상 수치 참조에 사용된다.
 * JSON의 drops 필드는 코드 로직으로 대체되므로 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MonsterTemplate(long id, String name, int hp, int attack, int defense,
                              int speed, DamageType damageType, int expReward, int goldReward,
                              boolean boss) {
}
