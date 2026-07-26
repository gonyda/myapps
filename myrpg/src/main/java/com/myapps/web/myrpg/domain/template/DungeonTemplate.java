package com.myapps.web.myrpg.domain.template;

import java.util.List;
import java.util.Map;

import com.myapps.web.myrpg.domain.model.ArmorSlot;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.WeaponType;

/**
 * 던전 마스터 데이터 템플릿.
 *
 * <p>JSON 데이터 파일에서 로딩되는 불변 던전 정의 레코드이다.
 * 던전의 난이도, 층 수, 입장 레벨, 드랍 가능 장비 타입,
 * 등급 확률 분포, 출현 몬스터 목록 등 던전 전체 설정을 정의한다.
 */
public record DungeonTemplate(long id, String name, int difficulty, int floorCount,
                              int requiredLevel, long bossId, int generation,
                              List<WeaponType> weaponTypes, List<ArmorSlot> armorSlots,
                              Map<Grade, Double> gradeChance, int treasureBaseGold,
                              List<DungeonSpawn> monsters) {
}
