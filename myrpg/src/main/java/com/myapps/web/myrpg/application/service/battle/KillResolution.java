package com.myapps.web.myrpg.application.service.battle;

import com.myapps.web.myrpg.application.dto.DropResult;
import com.myapps.web.myrpg.application.dto.DungeonClearResult;
import com.myapps.web.myrpg.domain.model.BattleTurnResult.Outcome;

/**
 * 몬스터 처치 판정 및 결산 결과(전투 결과, 드랍 보상, 던전 클리어 결과)를 담는 불변 레코드.
 *
 * @param outcome 전투 결과 (WIN, NONE 등)
 * @param reward 드랍 아이템 및 골드 보상
 * @param dungeonClearResult 던전 클리어 시의 보상 결과
 */
public record KillResolution(
        Outcome outcome, DropResult reward, DungeonClearResult dungeonClearResult) {}
