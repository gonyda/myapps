package com.myapps.web.myrpg.domain.model;

/**
 * 가위바위보 상성 판정 결과를 정의하는 열거형.
 *
 * <p>두 {@link SkillType} 간 상성 비교 결과를 나타내며, 전투 데미지 산출 시
 * 상성계수(affinity coefficient) 결정의 기반이 된다.
 */
public enum AffinityResult {

    /** 상성 승리 (공격력 100% 적용). */
    WIN,

    /** 상성 패배 (공격 무효 또는 경감). */
    LOSE,

    /** 동일 타입 — 무승부 (공격력 50% 적용). */
    DRAW
}
