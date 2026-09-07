package com.myapps.web.myrpg.application.service.battle;

import com.myapps.web.myrpg.domain.model.PreemptiveParty;

/**
 * 스킬 전투 처리 결과와 다음 턴 선제 파티 정보를 담는 불변 레코드.
 *
 * @param combat 이번 턴 전투 결과
 * @param nextPreemptive 다음 턴의 선제 파티
 */
public record SkillCombatOutcome(TurnCombatResult combat, PreemptiveParty nextPreemptive) {}
