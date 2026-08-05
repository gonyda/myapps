package com.myapps.web.myrpg.domain.model;

import jakarta.persistence.Embeddable;

/**
 * 캐릭터의 기본 스탯을 나타내는 불변 임베더블 값 객체.
 *
 * <p>STR, DEX, INT, Critical, DEF 다섯 가지 능력치를 정수로 보관한다.
 * 신규 캐릭터 기본값: STR 10, DEX 10, INT 10, Critical 5, DEF 5.
 *
 * @param str          힘 (Strength)
 * @param dex          민첩 (Dexterity)
 * @param intelligence 지능 (Intelligence)
 * @param critical     치명타 (Critical)
 * @param defense      방어력 (Defense)
 */
@Embeddable
public record Stats(
        int str,
        int dex,
        int intelligence,
        int critical,
        int defense
) {

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
}
