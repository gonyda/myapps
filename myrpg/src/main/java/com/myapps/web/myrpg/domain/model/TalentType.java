package com.myapps.web.myrpg.domain.model;

/**
 * 캐릭터의 재능 유형을 정의하는 열거형.
 *
 * <p>각 상수는 한글 라벨, 주 스탯 보너스({@link #primary()}), 보조 성장 보너스({@link #secondary()}), 데미지 보너스 퍼센트({@link
 * #damageBonusPercent()}), 재능 효과 요약({@link #effectSummary()})을 자체 보유하여 재능 데이터의 단일 소스 역할을 한다. {@code
 * MELEE}/{@code ARCHERY}/{@code MAGIC} 3종은 모두 실사용 재능이며, 정의 누락 시 컴파일 오류가 발생한다.
 */
public enum TalentType {

    /** 근접전투 재능 (주 STR, 보조 HP). */
    MELEE(
            "근접전투",
            new TalentBonus(BonusTarget.STR, 2),
            new TalentBonus(BonusTarget.HP, 5),
            10,
            "근접 데미지 +10%, STR +2/Lv, HP +5/Lv"),

    /** 활 재능 (주 DEX, 보조 Critical). */
    ARCHERY(
            "활",
            new TalentBonus(BonusTarget.DEX, 2),
            new TalentBonus(BonusTarget.CRITICAL, 1),
            10,
            "원거리 데미지 +10%, DEX +2/Lv, 치명 +0.1%/Lv"),

    /** 마법 재능 (주 INT, 보조 MP). */
    MAGIC(
            "마법",
            new TalentBonus(BonusTarget.INT, 2),
            new TalentBonus(BonusTarget.MP, 5),
            10,
            "마법 데미지 +10%, INT +2/Lv, MP +5/Lv");

    private final String label;
    private final TalentBonus primary;
    private final TalentBonus secondary;
    private final int damageBonusPercent;
    private final String effectSummary;

    TalentType(
            final String label,
            final TalentBonus primary,
            final TalentBonus secondary,
            final int damageBonusPercent,
            final String effectSummary) {
        this.label = label;
        this.primary = primary;
        this.secondary = secondary;
        this.damageBonusPercent = damageBonusPercent;
        this.effectSummary = effectSummary;
    }

    /**
     * 재능의 한글 라벨을 반환한다.
     *
     * @return 재능 라벨 문자열 (예: "근접전투", "활", "마법")
     */
    public String label() {
        return label;
    }

    /**
     * 재능의 주 스탯 보너스를 반환한다.
     *
     * @return 레벨업당 적용되는 주 스탯 {@link TalentBonus}
     */
    public TalentBonus primary() {
        return primary;
    }

    /**
     * 재능의 보조 성장 보너스를 반환한다.
     *
     * @return 레벨업당 적용되는 보조 성장 {@link TalentBonus}
     */
    public TalentBonus secondary() {
        return secondary;
    }

    /**
     * 재능과 일치하는 공격 타입에 부여되는 데미지 보너스 퍼센트를 반환한다.
     *
     * @return 데미지 보너스 퍼센트 정수 (예: 10 = +10%)
     */
    public int damageBonusPercent() {
        return damageBonusPercent;
    }

    /**
     * 재능 효과를 한 줄로 요약한 문자열을 반환한다.
     *
     * @return 재능 효과 요약 문자열
     */
    public String effectSummary() {
        return effectSummary;
    }

    /**
     * 상수명으로 {@code TalentType}을 조회하되, 유효하지 않으면 폴백 값을 반환한다.
     *
     * <p>{@code name}이 {@code null}이거나 공백이거나 알려진 상수명과 일치하지 않으면 {@code fallback}을 반환한다. 상수명 비교는
     * 대소문자를 구분한다(예: "ARCHERY").
     *
     * @param name 조회할 재능 상수명 (예: "ARCHERY")
     * @param fallback 유효하지 않은 입력일 때 반환할 폴백 재능
     * @return 유효한 상수명이면 해당 {@code TalentType}, 그 외에는 {@code fallback}
     */
    public static TalentType fromNameOrFallback(final String name, final TalentType fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            return TalentType.valueOf(name);
        } catch (final IllegalArgumentException ex) {
            return fallback;
        }
    }
}
