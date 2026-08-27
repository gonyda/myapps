package com.myapps.web.myrpg.application.dto;

/**
 * 스킬 목록 팝업의 한 행을 표현하는 뷰 모델.
 *
 * <p>스킬명, 재능 분류 라벨, 랭크 라벨, 진행률(사용+막타 동일가중 평균), 승급 가능 여부, 최고 랭크 여부, 필드 사용 가능 여부, 쿨타임 뱃지 텍스트를 포함한다.
 *
 * @param id 스킬 카탈로그 ID
 * @param label 스킬 표시명
 * @param talentLabel 재능 분류 라벨 (예: "MELEE", "MAGIC")
 * @param rankLabel 현재 랭크 라벨 (예: "F", "A", "Master")
 * @param progressPercent 진행률(0~100, 사용+막타 동일가중 평균)
 * @param rankable 승급 가능 여부 (조건+AP 충족 + MASTER 아님)
 * @param maxed 최고 랭크(MASTER) 여부
 * @param fieldUsable 필드 사용 가능 여부 (힐링 등)
 * @param cooldownBadgeText 쿨타임 뱃지 텍스트 (궁극기 대기 상태 등, 없으면 null)
 */
public record SkillRowView(
        String id,
        String label,
        String talentLabel,
        String rankLabel,
        int progressPercent,
        boolean rankable,
        boolean maxed,
        boolean fieldUsable,
        String cooldownBadgeText,
        Integer slotIndex,
        boolean isPassive) {

    /** 하위호환 생성자 (9인자). */
    public SkillRowView(
            final String id,
            final String label,
            final String talentLabel,
            final String rankLabel,
            final int progressPercent,
            final boolean rankable,
            final boolean maxed,
            final boolean fieldUsable,
            final String cooldownBadgeText) {
        this(
                id,
                label,
                talentLabel,
                rankLabel,
                progressPercent,
                rankable,
                maxed,
                fieldUsable,
                cooldownBadgeText,
                null,
                false);
    }

    /** 하위호환 생성자: fieldUsable=false, cooldownBadgeText=null. */
    public SkillRowView(
            final String id,
            final String label,
            final String talentLabel,
            final String rankLabel,
            final int progressPercent,
            final boolean rankable,
            final boolean maxed) {
        this(
                id,
                label,
                talentLabel,
                rankLabel,
                progressPercent,
                rankable,
                maxed,
                false,
                null,
                null,
                false);
    }
}
