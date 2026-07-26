package com.myapps.web.myrpg.domain.model;

/**
 * 장비 등급을 정의하는 열거형.
 *
 * <p>COMMON부터 LEGENDARY까지 5단계로 구성되며,
 * 각 등급은 레벨 보너스, 판매가 배수, 스킬슬롯 수를 결정한다.
 */
public enum Grade {

    COMMON(0, 1.0, 1),
    UNCOMMON(2, 1.6, 2),
    RARE(5, 3.0, 3),
    EPIC(8, 6.0, 4),
    LEGENDARY(10, 12.0, 5);

    private final int levelBonus;
    private final double sellMultiplier;
    private final int skillSlots;

    Grade(final int levelBonus, final double sellMultiplier, final int skillSlots) {
        this.levelBonus = levelBonus;
        this.sellMultiplier = sellMultiplier;
        this.skillSlots = skillSlots;
    }

    /**
     * 등급 레벨 보너스를 반환한다.
     *
     * <p>유효 파워 레벨 산출 시 itemLevel에 합산되는 값이다.
     *
     * @return 등급 레벨 보너스 (COMMON 0, UNCOMMON 2, RARE 5, EPIC 8, LEGENDARY 10)
     */
    public int getLevelBonus() {
        return levelBonus;
    }

    /**
     * 판매가 배수를 반환한다.
     *
     * <p>상점 판매 시 baseValue에 곱해지는 등급별 배수이다.
     *
     * @return 판매가 배수 (COMMON 1.0, UNCOMMON 1.6, RARE 3.0, EPIC 6.0, LEGENDARY 12.0)
     */
    public double getSellMultiplier() {
        return sellMultiplier;
    }

    /**
     * 스킬슬롯 수를 반환한다.
     *
     * <p>무기 인스턴스가 보유하는 스킬 장착 가능 슬롯 수이다.
     *
     * @return 스킬슬롯 수 (COMMON 1, UNCOMMON 2, RARE 3, EPIC 4, LEGENDARY 5)
     */
    public int getSkillSlots() {
        return skillSlots;
    }
}
