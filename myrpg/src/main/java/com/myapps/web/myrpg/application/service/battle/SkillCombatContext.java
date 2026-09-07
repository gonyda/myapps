package com.myapps.web.myrpg.application.service.battle;

import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import java.util.List;
import java.util.Optional;

/**
 * 스킬별 전투 처리 실행에 필요한 입력 컨텍스트를 담는 불변 레코드.
 *
 * @param skill 사용할 스킬
 * @param progress 플레이어 진행 상태
 * @param monster 전투 상대 몬스터
 * @param state 현재 전투 상태
 * @param ownedSkillOpt 플레이어가 습득한 스킬 정보
 * @param combatLines 중앙 전투 로그 라인 리스트 (순서 보존 및 동일 인스턴스 누적용)
 * @param equippedTalent 착용 중인 무기 계열 재능
 * @param isPlayerPreemptive 플레이어 선제공격 여부
 * @param monsterAction 몬스터의 이번 턴 행동
 */
public record SkillCombatContext(
        Skill skill,
        CharacterProgress progress,
        Monster monster,
        BattleState state,
        Optional<CharacterSkill> ownedSkillOpt,
        List<String> combatLines,
        SkillTalent equippedTalent,
        boolean isPlayerPreemptive,
        SkillType monsterAction) {}
