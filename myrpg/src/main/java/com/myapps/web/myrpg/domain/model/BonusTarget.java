package com.myapps.web.myrpg.domain.model;

/**
 * 재능 보너스가 적용되는 구체적인 대상을 정의하는 열거형.
 *
 * <p>스탯 계열({@code STR}/{@code DEX}/{@code INT}/{@code CRITICAL})과 바이탈 계열({@code HP}/{@code
 * MP}/{@code STAMINA})을 하나의 어휘로 통합하며, 각 상수는 자신의 {@link BonusKind} 분류를 보유한다.
 */
public enum BonusTarget {

    /** 힘 (스탯 계열). */
    STR(BonusKind.STAT),

    /** 민첩 (스탯 계열). */
    DEX(BonusKind.STAT),

    /** 지능 (스탯 계열). */
    INT(BonusKind.STAT),

    /** 치명타 (스탯 계열). */
    CRITICAL(BonusKind.STAT),

    /** 체력 최대치 (바이탈 계열). */
    HP(BonusKind.VITAL),

    /** 마나 최대치 (바이탈 계열). */
    MP(BonusKind.VITAL),

    /** 기력 최대치 (바이탈 계열). */
    STAMINA(BonusKind.VITAL),

    /** 방어력 (스탯 계열). */
    DEF(BonusKind.STAT),

    /** 턴당 마나 자연 재생 (스탯 계열). */
    MP_REGEN(BonusKind.STAT);

    private final BonusKind kind;

    BonusTarget(final BonusKind kind) {
        this.kind = kind;
    }

    /**
     * 이 대상의 분류(스탯 계열/바이탈 계열)를 반환한다.
     *
     * @return 대상의 {@link BonusKind} 분류
     */
    public BonusKind kind() {
        return kind;
    }

    /**
     * UI 표시용 한글 라벨을 반환한다.
     *
     * @return 한글 표시명
     */
    public String label() {
        return switch (this) {
            case STR -> "체력 (STR)";
            case DEX -> "솜씨 (DEX)";
            case INT -> "지력 (INT)";
            case DEF -> "방어력 (DEF)";
            case HP -> "최대 생명력 (HP)";
            case MP -> "최대 마나 (MP)";
            case STAMINA -> "최대 스태미나";
            case CRITICAL -> "치명타 확률";
            case MP_REGEN -> "턴당 마나 회복";
        };
    }
}
