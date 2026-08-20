package com.myapps.web.myrpg.application.dto;

/**
 * 스킬 목록 팝업의 한 행을 표현하는 뷰 모델.
 *
 * <p>스킬명, 재능 분류 라벨, 랭크 라벨, 진행률(사용+막타 동일가중 평균), 승급 가능 여부, 최고 랭크 여부를 포함한다.
 *
 * @param id 스킬 카탈로그 ID
 * @param label 스킬 표시명
 * @param talentLabel 재능 분류 라벨 (예: "MELEE", "MAGIC")
 * @param rankLabel 현재 랭크 라벨 (예: "F", "A", "Master")
 * @param progressPercent 진행률(0~100, 사용+막타 동일가중 평균)
 * @param rankable 승급 가능 여부 (조건+AP 충족 + MASTER 아님)
 * @param maxed 최고 랭크(MASTER) 여부
 */
public record SkillRowView(
        String id,
        String label,
        String talentLabel,
        String rankLabel,
        int progressPercent,
        boolean rankable,
        boolean maxed) {}
