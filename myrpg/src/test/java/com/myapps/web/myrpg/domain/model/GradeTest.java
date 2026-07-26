package com.myapps.web.myrpg.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Grade 열거형의 등급 레벨 보너스, 판매가 배수, 스킬슬롯 수 값을 검증하는 단위 테스트.
 */
class GradeTest {

    @Test
    void should_haveCorrectLevelBonus_when_eachGrade() {
        assertEquals(0, Grade.COMMON.getLevelBonus());
        assertEquals(2, Grade.UNCOMMON.getLevelBonus());
        assertEquals(5, Grade.RARE.getLevelBonus());
        assertEquals(8, Grade.EPIC.getLevelBonus());
        assertEquals(10, Grade.LEGENDARY.getLevelBonus());
    }

    @Test
    void should_haveCorrectSellMultiplier_when_eachGrade() {
        assertEquals(1.0, Grade.COMMON.getSellMultiplier());
        assertEquals(1.6, Grade.UNCOMMON.getSellMultiplier());
        assertEquals(3.0, Grade.RARE.getSellMultiplier());
        assertEquals(6.0, Grade.EPIC.getSellMultiplier());
        assertEquals(12.0, Grade.LEGENDARY.getSellMultiplier());
    }

    @Test
    void should_haveCorrectSkillSlots_when_eachGrade() {
        assertEquals(1, Grade.COMMON.getSkillSlots());
        assertEquals(2, Grade.UNCOMMON.getSkillSlots());
        assertEquals(3, Grade.RARE.getSkillSlots());
        assertEquals(4, Grade.EPIC.getSkillSlots());
        assertEquals(5, Grade.LEGENDARY.getSkillSlots());
    }

    @Test
    void should_haveFiveGrades() {
        assertEquals(5, Grade.values().length);
    }
}
