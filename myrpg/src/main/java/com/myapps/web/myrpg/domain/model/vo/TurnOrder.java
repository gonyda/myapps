package com.myapps.web.myrpg.domain.model.vo;

/**
 * 전투 선후공 순서를 정의하는 열거형.
 *
 * <p>속도 비교에 따라 플레이어 선공 또는 몬스터 선공으로 결정된다.
 */
public enum TurnOrder {

    PLAYER_FIRST,
    MONSTER_FIRST
}
