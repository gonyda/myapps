package com.myapps.web.myrpg.application.dto;

import com.myapps.web.myrpg.domain.model.Stats;
import com.myapps.web.myrpg.domain.model.VitalMax;

/**
 * 장착 중인 장비들의 합산 보너스를 반환하는 DTO.
 *
 * <p>STAT 계열(STR/DEX/INT/CRITICAL/DEF)은 {@link Stats}로,
 * VITAL 계열(HP/MP/STAMINA)은 {@link VitalMax}로 분기하여 합산한 결과이다.
 *
 * @param statBonus  장비 STAT 계열 보너스 합산 (0 기준에서의 델타)
 * @param vitalBonus 장비 VITAL 계열 보너스 합산 (0 기준에서의 델타)
 */
public record EquippedBonusResult(Stats statBonus, VitalMax vitalBonus) {

    /** 보너스가 없는 기본값 (모든 필드 0). */
    public static final EquippedBonusResult ZERO = new EquippedBonusResult(
            Stats.ZERO, new VitalMax(0, 0, 0));
}
