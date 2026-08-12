package com.myapps.web.myrpg.application.dto;

import com.myapps.web.myrpg.domain.model.SkillType;

/**
 * 전투 한 턴의 활동 로그 생성을 위한 입력 값.
 *
 * <p>{@link com.myapps.web.myrpg.application.service.BattleLogFormatter}에 전달되어
 * 플레이어·몬스터 행동 로그 문자열을 산출하는 데 사용된다.
 * 데미지 수치와 특수 플래그(선제 사격·캐스팅 실패)를 담는다.
 *
 * @param skillLabel     플레이어가 사용한 스킬 이름
 * @param playerType     플레이어 스킬 타입 (일반/강/방어)
 * @param monsterName    몬스터 이름
 * @param monsterAction  몬스터 행동 타입 (일반/강/방어)
 * @param playerDamage   플레이어가 몬스터에게 가한 피해 (방어 승리 시 반격 피해)
 * @param monsterDamage  몬스터가 플레이어에게 가한 피해 (몬스터 방어 승리 시 반격 피해)
 * @param playerCritical 플레이어 크리티컬 발동 여부
 * @param firstStrike    활 1턴 선제 사격 발동 여부
 * @param castFailure    마법 캐스팅 실패 여부
 */
public record BattleLogInput(
        String skillLabel,
        SkillType playerType,
        String monsterName,
        SkillType monsterAction,
        int playerDamage,
        int monsterDamage,
        boolean playerCritical,
        boolean firstStrike,
        boolean castFailure) {
}
