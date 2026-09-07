package com.myapps.web.myrpg.application.service.battle;

import com.myapps.web.myrpg.domain.model.HitResult;
import java.util.List;

/**
 * 턴 전투 해석 결과(데미지, 크리티컬, 블록/카운터, 히트 목록)를 담는 불변 레코드.
 *
 * @param playerDamage 플레이어가 몬스터에 입힌 피해량
 * @param monsterDamage 몬스터가 플레이어에 입힌 피해량
 * @param playerCritical 플레이어 크리티컬 발동 여부
 * @param monsterCritical 몬스터 크리티컬 발동 여부
 * @param blocked 플레이어 방어 성공 여부
 * @param countered 플레이어 카운터 성공 여부
 * @param firstStrike 선제공격 발동 여부
 * @param playerHits 멀티히트 공격인 경우 각 타수별 상세 결과 목록
 */
public record TurnCombatResult(
        int playerDamage,
        int monsterDamage,
        boolean playerCritical,
        boolean monsterCritical,
        boolean blocked,
        boolean countered,
        boolean firstStrike,
        List<HitResult> playerHits) {

    /** 방어적 복사를 적용한 생성자. */
    public TurnCombatResult {
        playerHits = playerHits != null ? List.copyOf(playerHits) : List.of();
    }

    /** 몬스터 선제 피해 결과 생성. */
    public static TurnCombatResult monsterPreemptive(final int monsterDamage) {
        return new TurnCombatResult(0, monsterDamage, false, false, false, false, true, List.of());
    }

    /** 몬스터 단독 피해 결과 생성 (캐스팅 실패 등). */
    public static TurnCombatResult monsterOnly(final int monsterDamage) {
        return new TurnCombatResult(0, monsterDamage, false, false, false, false, false, List.of());
    }

    /** 플레이어 피해량 증폭 적용 복사본 반환. */
    public TurnCombatResult withPlayerDamage(final int newPlayerDamage) {
        return new TurnCombatResult(
                newPlayerDamage,
                monsterDamage,
                playerCritical,
                monsterCritical,
                blocked,
                countered,
                firstStrike,
                playerHits);
    }
}
