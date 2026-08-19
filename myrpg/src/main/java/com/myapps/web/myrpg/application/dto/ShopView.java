package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 상점 팝업의 전체 뷰 모델.
 *
 * <p>NPC별 구매 목록(buyItems) + 내 인벤토리 판매 목록(sellItems) + 보유 골드로 구성되며,
 * 모바일 세로 배치(상점 물건 위 / 내 소지품 아래 / 골드 하단)로 렌더된다.
 *
 * @param buyItems    구매 목록 (NPC Shop_Items 중 Buy_Price 보유 품목)
 * @param sellItems   판매 목록 (인벤토리 아이템)
 * @param currentGold 현재 보유 골드
 * @param npcId       대화 중인 NPC id (구매 목록 정체성, 빈 목록이면 빈 문자열 허용)
 */
public record ShopView(
        List<ShopBuyItemView> buyItems,
        List<ShopSellItemView> sellItems,
        long currentGold,
        String npcId) {
}