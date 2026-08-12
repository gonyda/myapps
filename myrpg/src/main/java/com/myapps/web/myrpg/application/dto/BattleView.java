package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 전투 화면의 뷰 모델 레코드.
 *
 * <p>전투 진행 중 몬스터 정보(이름·레벨·현재/최대 HP),
 * 사용 가능한 전투 스킬 버튼 목록, 도망 가능 여부를 제공한다.
 * Thymeleaf 프래그먼트({@code battle-view.html})에서 렌더링에 사용된다.
 *
 * @param monsterName      전투 대상 몬스터 표시명
 * @param monsterLevel     전투 대상 몬스터 레벨
 * @param monsterCurrentHp 몬스터 현재 HP (매 턴 갱신)
 * @param monsterMaxHp     몬스터 최대 HP
 * @param skills           전투 스킬 버튼 목록 (착용 무기 재능 + 공통)
 * @param fleeAvailable    도망 버튼 활성 여부 (항상 true)
 */
public record BattleView(String monsterName, int monsterLevel,
                         int monsterCurrentHp, int monsterMaxHp,
                         List<BattleSkillButton> skills, boolean fleeAvailable) {
}
