package com.myapps.web.myrpg.application.dto;

import com.myapps.web.myrpg.domain.model.ResourceKind;
import com.myapps.web.myrpg.domain.model.SkillType;

/**
 * 전투 화면에서 스킬 버튼으로 표시할 정보를 담는 불변 레코드.
 *
 * <p>착용 무기 재능에 해당하는 스킬과 공통(방어) 스킬만 노출되며, 무기 교체 시 목록이 실시간으로 갱신된다.
 *
 * @param id 스킬 카탈로그 ID (예: "windmill", "firebolt")
 * @param label 스킬 표시명 (예: "윈드밀", "파이어볼트")
 * @param type 스킬 공격/방어 유형 (NORMAL/HEAVY/DEFENSE)
 * @param resourceKind 소모 자원 종류 (STAMINA 또는 MP)
 * @param resourceCost 소모 자원량
 */
public record BattleSkillButton(
        String id, String label, SkillType type, ResourceKind resourceKind, int resourceCost) {}
