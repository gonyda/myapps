package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 상점 구매 목록 한 행에 대응하는 뷰 모델.
 *
 * <p>NPC의 {@code shopItems}에서 카탈로그 Buy_Price가 있는 아이템만 조립되며,
 * {@code detailLines}는 렌더 시점에 임베드되어 🔍 클릭 시 별도 서버 요청 없이 표시된다.
 *
 * @param id          아이템 카탈로그 키 (예: "short_sword")
 * @param name        아이템 표시명
 * @param typeLabel   아이템 타입의 한글 라벨 (예: "무기", "방어구")
 * @param buyPrice    상점 구매가 (골드)
 * @param detailLines 상세 설명 줄 목록 (임베드용)
 */
public record ShopBuyItemView(
        String id,
        String name,
        String typeLabel,
        long buyPrice,
        List<String> detailLines) {
}