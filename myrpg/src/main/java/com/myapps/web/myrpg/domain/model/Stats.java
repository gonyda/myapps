package com.myapps.web.myrpg.domain.model;

/**
 * 캐릭터의 기본 스탯을 나타내는 순수 표시 VO (Value Object).
 *
 * <p>STR, DEX, INT, Critical, DEF 다섯 가지 능력치를 정수로 보관한다.
 * 이 record는 JPA 엔티티에 직접 매핑되지 않으며, 레벨·장비·스킬 등으로부터
 * 계산되어 조립된 결과를 표현하는 용도로만 사용한다.
 *
 * <p>신규 캐릭터 기본값: STR 10, DEX 10, INT 10, Critical 5, DEF 5.
 *
 * @param str          힘 (Strength)
 * @param dex          민첩 (Dexterity)
 * @param intelligence 지능 (Intelligence)
 * @param critical     치명타 (Critical) — 0.1% 단위 (예: 50 = 5.0%)
 * @param defense      방어력 (Defense)
 */
public record Stats(
        int str,
        int dex,
        int intelligence,
        int critical,
        int defense
) {

    /** 모든 값이 0인 스탯 (스킬 보너스 기본값 등). */
    public static final Stats ZERO = new Stats(0, 0, 0, 0, 0);

    /** 신규 캐릭터 기본 스탯 (STR 10, DEX 10, INT 10, Critical 5, DEF 5). */
    private static final int DEFAULT_STR = 10;
    private static final int DEFAULT_DEX = 10;
    private static final int DEFAULT_INTELLIGENCE = 10;
    private static final int DEFAULT_CRITICAL = 5;
    private static final int DEFAULT_DEFENSE = 5;

    /**
     * 신규 캐릭터용 기본 스탯을 생성한다.
     *
     * @return 기본값이 설정된 Stats 인스턴스
     */
    public static Stats createDefault() {
        return new Stats(DEFAULT_STR, DEFAULT_DEX, DEFAULT_INTELLIGENCE, DEFAULT_CRITICAL, DEFAULT_DEFENSE);
    }

    /**
     * STR에 델타를 더한 새 인스턴스를 반환한다.
     *
     * @param delta STR에 가산할 값 (음수 가능)
     * @return STR만 변경된 새 Stats 인스턴스
     */
    public Stats withStrDelta(final int delta) {
        return new Stats(str + delta, dex, intelligence, critical, defense);
    }

    /**
     * DEX에 델타를 더한 새 인스턴스를 반환한다.
     *
     * @param delta DEX에 가산할 값 (음수 가능)
     * @return DEX만 변경된 새 Stats 인스턴스
     */
    public Stats withDexDelta(final int delta) {
        return new Stats(str, dex + delta, intelligence, critical, defense);
    }

    /**
     * INT(지능)에 델타를 더한 새 인스턴스를 반환한다.
     *
     * @param delta INT에 가산할 값 (음수 가능)
     * @return INT만 변경된 새 Stats 인스턴스
     */
    public Stats withIntDelta(final int delta) {
        return new Stats(str, dex, intelligence + delta, critical, defense);
    }

    /**
     * Critical(0.1% 단위)에 델타를 더한 새 인스턴스를 반환한다.
     *
     * @param delta Critical에 가산할 값 (0.1% 단위, 음수 가능)
     * @return Critical만 변경된 새 Stats 인스턴스
     */
    public Stats withCriticalDelta(final int delta) {
        return new Stats(str, dex, intelligence, critical + delta, defense);
    }
}
