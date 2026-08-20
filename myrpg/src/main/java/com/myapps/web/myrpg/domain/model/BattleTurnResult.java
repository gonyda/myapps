package com.myapps.web.myrpg.domain.model;

import com.myapps.web.myrpg.application.dto.DropResult;
import java.util.List;

/**
 * 한 턴의 전투 결과를 나타내는 불변 레코드.
 *
 * <p>플레이어와 몬스터 양측의 행동·피해, 각종 판정 플래그(크리티컬/방어/반격/캐스팅 실패/ 선제/자원 부족), 전투 종료 여부 및 결과, 보상 정보, 플레이어 히트 상세,
 * 전투 액션 로그를 포함한다. {@link BattleTurnResult.Outcome}으로 전투 종료 유형을 구분한다.
 *
 * @param playerAction 플레이어가 선택한 스킬 타입
 * @param playerDamage 플레이어가 몬스터에게 가한 피해량
 * @param monsterAction 몬스터가 선택한 스킬 타입
 * @param monsterDamage 몬스터가 플레이어에게 가한 피해량
 * @param playerCritical 플레이어 크리티컬 발동 여부
 * @param monsterCritical 몬스터 크리티컬 발동 여부
 * @param blocked 방어로 경감된 턴 여부
 * @param countered 반격이 발생한 턴 여부
 * @param castFailure 마법 캐스팅 실패 여부
 * @param firstStrike 선제 공격(활 1턴) 발동 여부
 * @param resourceInsufficient 자원 부족으로 턴 미진행 여부
 * @param insufficientKind 부족한 자원 종류 (자원 부족 시에만 유효)
 * @param battleEnded 전투 종료 여부
 * @param outcome 전투 결과 유형 (진행 중/승/패/도망)
 * @param reward 처치 보상 (승리 시에만 유효, 그 외 {@code null})
 * @param experienceGained 획득 경험치 (승리 시에만 유효, 그 외 0)
 * @param playerHits 플레이어 딜 스킬 히트별 상세 결과 (공격 경로 시 hitCount개, 그 외 빈 리스트)
 * @param combatLines 이번 턴의 전투 액션 로그 라인 목록 (중앙 표시용)
 */
public record BattleTurnResult(
        SkillType playerAction,
        int playerDamage,
        SkillType monsterAction,
        int monsterDamage,
        boolean playerCritical,
        boolean monsterCritical,
        boolean blocked,
        boolean countered,
        boolean castFailure,
        boolean firstStrike,
        boolean resourceInsufficient,
        ResourceKind insufficientKind,
        boolean battleEnded,
        Outcome outcome,
        DropResult reward,
        long experienceGained,
        List<HitResult> playerHits,
        List<String> combatLines) {

    /** 전투 결과 유형을 정의하는 열거형. */
    public enum Outcome {

        /** 전투 진행 중 (종료되지 않음). */
        NONE,

        /** 플레이어 승리 (몬스터 처치). */
        WIN,

        /** 플레이어 패배 (HP 0). */
        LOSE,

        /** 도망 성공. */
        FLED
    }
}
