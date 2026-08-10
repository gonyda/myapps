package com.myapps.web.myrpg.domain.model;

/**
 * 장비 아이템이 제공하는 단일 보너스를 나타내는 레코드.
 *
 * <p>{@link BonusTarget}으로 보너스 대상(STR, DEF, HP 등)을 지정하고,
 * {@code amount}로 수치를 나타낸다. {@code target.kind()}로
 * STAT 계열/VITAL 계열을 분기할 수 있다.
 *
 * @param target 보너스가 적용되는 대상
 * @param amount 보너스 수치
 */
public record EquipBonus(BonusTarget target, int amount) {
}
