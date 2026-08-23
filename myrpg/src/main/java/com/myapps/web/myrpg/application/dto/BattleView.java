package com.myapps.web.myrpg.application.dto;

import com.myapps.web.myrpg.domain.model.SkillType;
import java.util.List;

/**
 * 전투 화면의 뷰 모델 레코드.
 *
 * <p>전투 진행 중 몬스터 정보(이름·레벨·현재/최대 HP), 사용 가능한 전투 스킬 버튼 목록, 도망 가능 여부, 대치/공방 상태, 몬스터 의도 전조 뱃지, 실시간 타이머
 * 지속시간 및 선제 사격 여부를 제공한다. Thymeleaf 프래그먼트({@code battle-view.html})에서 렌더링에 사용된다.
 *
 * @param monsterName 전투 대상 몬스터 표시명
 * @param monsterLevel 전투 대상 몬스터 레벨
 * @param monsterCurrentHp 몬스터 현재 HP (매 턴 갱신)
 * @param monsterMaxHp 몬스터 최대 HP
 * @param skills 전투 스킬 버튼 목록 (착용 무기 재능 + 공통)
 * @param fleeAvailable 도망 버튼 활성 여부 (대치 상태에서만 true)
 * @param standby 대치 페이즈 여부 (true: 시간 정지/정비, false: 액티브 공방)
 * @param monsterIntent 몬스터가 준비한 의도 (NORMAL, HEAVY, DEFENSE, null)
 * @param clashDurationMs 공방 타이머 지속시간 (ms단위: 1000, 1500, 대치 시 0)
 * @param monsterStanceBadgeLabel B안 전조 뱃지 텍스트 라벨
 * @param monsterStanceBadgeClass B안 전조 뱃지 CSS 클래스
 * @param bowFirstStrike 활 1턴 선제 사격 발동 여부
 */
public record BattleView(
        String monsterName,
        int monsterLevel,
        int monsterCurrentHp,
        int monsterMaxHp,
        List<BattleSkillButton> skills,
        boolean fleeAvailable,
        boolean standby,
        SkillType monsterIntent,
        int clashDurationMs,
        String monsterStanceBadgeLabel,
        String monsterStanceBadgeClass,
        boolean bowFirstStrike) {}
