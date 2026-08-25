package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 장비 팝업 전체 렌더링을 위한 뷰 DTO.
 *
 * @param slots 3x3 그리드 순서대로 정렬된 9개 슬롯 뷰 목록
 * @param bonusResult 장착 장비로 인한 종합 스탯 및 바이탈 보너스
 * @param equippedCount 현재 장착된 부위 수 (최대 6부위)
 * @param averageDurabilityPercent 장착 장비들의 평균 내구도 백분율
 * @param weaponTalentLabel 현재 장착 중인 무기의 재능 분류 라벨
 */
public record EquipmentView(
        List<EquipmentSlotView> slots,
        EquippedBonusResult bonusResult,
        int equippedCount,
        int averageDurabilityPercent,
        String weaponTalentLabel) {}
