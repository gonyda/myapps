package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 수리 목록 한 행에 대응하는 뷰 모델.
 *
 * <p>내구도가 닳은 장비(`ceil(currentDurability) < maxDurability`)만 조립되며, 장착 중인 장비도 수리 대상에 포함된다. 내구도 표시값은
 * 올림 정수(Durability_Display)다.
 *
 * @param ownedItemId 보유 아이템 엔티티 PK
 * @param name 아이템 표시명
 * @param typeLabel 아이템 타입의 한글 라벨 (예: "무기", "방어구")
 * @param currentDurabilityCeil 현재 내구도 올림 정수 표시값 (예: 실제 12.4 → 13)
 * @param maxDurability 최대 내구도
 * @param repairCost 1포인트 수리비 (= Sell_Value)
 * @param equipped 장착 여부
 * @param detailLines 상세 설명 줄 목록 (임베드용)
 */
public record RepairItemView(
        long ownedItemId,
        String name,
        String typeLabel,
        int currentDurabilityCeil,
        int maxDurability,
        long repairCost,
        boolean equipped,
        List<String> detailLines) {}
