package com.myapps.web.myrpg.domain.model;

import java.util.Map;

/**
 * 영구 패시브(PASSIVE) 카탈로그 항목을 나타내는 불변 record.
 *
 * <p>전투 슬롯에 등록되지 않고 스킬 팝업 공용 탭에서 관리되며, MASTER 랭크 기준 최종 누적 스탯 맵({@link #totalStatBonus()})을 보유한다.
 *
 * @param id 스킬 고유 식별자
 * @param label 표시용 라벨
 * @param type 스킬 타입 (PASSIVE)
 * @param talent 재능 분류
 * @param resourceCost 자원 소모량 (0)
 * @param totalStatBonus MASTER 랭크 기준 최종 누적 스탯 맵
 * @param description 스킬 설명 문자열
 */
public record PassiveSkill(
        String id,
        String label,
        SkillType type,
        SkillTalent talent,
        int resourceCost,
        Map<BonusTarget, Integer> totalStatBonus,
        String description)
        implements Skill {}
