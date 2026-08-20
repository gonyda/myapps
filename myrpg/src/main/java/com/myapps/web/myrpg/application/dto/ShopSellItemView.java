package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 상점 판매 목록 한 행에 대응하는 뷰 모델.
 *
 * <p>인벤토리(`storage=INVENTORY`) 아이템만 판매 대상이며, 장착 중이면 {@code equipped=true}로 표시되어 클라이언트에서 [장착중] 배지로
 * 렌더된다.
 *
 * @param ownedItemId 보유 아이템 엔티티 PK
 * @param name 아이템 표시명
 * @param typeLabel 아이템 타입의 한글 라벨 (예: "포션", "무기")
 * @param quantity 보유 수량 (소비형 스택, 장비는 1)
 * @param sellValue 판매가 (Sell_Value, 계산값)
 * @param equipped 장착 여부 (true면 판매 거부)
 * @param detailLines 상세 설명 줄 목록 (임베드용)
 */
public record ShopSellItemView(
        long ownedItemId,
        String name,
        String typeLabel,
        int quantity,
        long sellValue,
        boolean equipped,
        List<String> detailLines) {}
