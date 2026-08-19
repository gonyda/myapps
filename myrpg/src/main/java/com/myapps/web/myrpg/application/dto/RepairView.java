package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 수리 팝업의 전체 뷰 모델.
 *
 * <p>내구도가 닳은 장비 목록(repairItems)과 보유 골드로 구성되며,
 * 하단에 보유 Gold를 표시한다(`.inventory-footer` 재사용).
 *
 * @param repairItems  수리 대상 목록 (내구도가 닳은 장비만)
 * @param currentGold  현재 보유 골드
 */
public record RepairView(
        List<RepairItemView> repairItems,
        long currentGold) {
}