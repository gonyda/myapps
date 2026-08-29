package com.myapps.web.myrpg.application.dto;

/**
 * 스킬 목록 팝업의 한 행을 표현하는 뷰 모델.
 *
 * <p>스킬명, 재능 분류 라벨, 랭크 라벨, 진행률(사용 횟수 달성율), 승급 가능 여부, 최고 랭크 여부, 필드 사용 가능 여부, 쿨타임 뱃지 텍스트, 스킬 유형
 * 식별자/라벨을 포함한다.
 *
 * @param id 스킬 카탈로그 ID
 * @param label 스킬 표시명
 * @param talentLabel 재능 분류 라벨 (예: "MELEE", "MAGIC")
 * @param rankLabel 현재 랭크 라벨 (예: "F", "A", "Master")
 * @param progressPercent 진행률(0~100)
 * @param rankable 승급 가능 여부 (조건+AP 충족 + MASTER 아님)
 * @param maxed 최고 랭크(MASTER) 여부
 * @param fieldUsable 필드 사용 가능 여부 (힐링 등)
 * @param cooldownBadgeText 쿨타임 뱃지 텍스트 (궁극기 대기 상태 등, 없으면 null)
 * @param slotIndex 배정된 슬롯 번호 (0~9, 미배정 시 null)
 * @param isPassive 패시브 스킬 여부
 * @param icon 스킬 표시 이모지 (예: "👑", "⚔️", "🏹", "🔮")
 * @param typeName 스킬 유형 식별자 (예: "NORMAL", "HEAVY", "CC", "BUFF", "DEBUFF", "DEFENSE", "ULTIMATE",
 *     "RECOVERY", "PASSIVE", "DOT")
 * @param typeLabel 스킬 유형 한글 라벨 (예: "일반", "강", "제어", "버프", "디버프", "방어", "궁극기", "회복", "패시브", "지속피해")
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
        boolean isPassive,
        String icon,
        String typeName,
        String typeLabel) {

    /** 12인자 하위호환 생성자. */
    public SkillRowView(
            final String id,
            final String label,
            final String talentLabel,
            final String rankLabel,
            final int progressPercent,
            final boolean rankable,
            final boolean maxed,
            final boolean fieldUsable,
            final String cooldownBadgeText,
            final Integer slotIndex,
            final boolean isPassive,
            final String icon) {
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
                slotIndex,
                isPassive,
                icon,
                isPassive ? "PASSIVE" : "NORMAL",
                isPassive ? "패시브" : "일반");
    }

    /** 11인자 하위호환 생성자 (icon="⚔️" 기본값). */
    public SkillRowView(
            final String id,
            final String label,
            final String talentLabel,
            final String rankLabel,
            final int progressPercent,
            final boolean rankable,
            final boolean maxed,
            final boolean fieldUsable,
            final String cooldownBadgeText,
            final Integer slotIndex,
            final boolean isPassive) {
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
                slotIndex,
                isPassive,
                "⚔️");
    }

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
                false,
                "⚔️");
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
                false,
                "⚔️");
    }
}
