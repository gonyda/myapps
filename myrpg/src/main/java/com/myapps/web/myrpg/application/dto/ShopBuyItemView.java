package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 상점 구매 목록 한 행에 대응하는 뷰 모델.
 *
 * <p>NPC의 {@code shopItems}에서 카탈로그 Buy_Price가 있는 아이템만 조립되며, {@code detailLines}는 렌더 시점에 임베드되어 🔍 클릭
 * 시 별도 서버 요청 없이 표시된다. 착용 중인 동일 부위 장비가 있는 경우 {@code equippedItemName} 및 {@code equippedDetailLines}를
 * 통해 비교 정보를 제공한다.
 *
 * @param id 아이템 카탈로그 키 (예: "short_sword")
 * @param name 아이템 표시명
 * @param typeLabel 아이템 타입의 한글 라벨 (예: "무기", "방어구")
 * @param buyPrice 상점 구매가 (골드)
 * @param detailLines 상세 설명 줄 목록 (임베드용)
 * @param equippedItemName 현재 착용 중인 동일 부위 장비명 (미착용 시 null)
 * @param equippedDetailLines 현재 착용 중인 동일 부위 장비의 상세 설명 줄 목록 (미착용 시 빈 리스트)
 */
public record ShopBuyItemView(
        String id,
        String name,
        String typeLabel,
        long buyPrice,
        List<String> detailLines,
        String equippedItemName,
        List<String> equippedDetailLines) {

    /** 착용 장비 비교 정보가 없는 단독 상점 아이템 뷰를 생성한다. */
    public ShopBuyItemView(
            final String id,
            final String name,
            final String typeLabel,
            final long buyPrice,
            final List<String> detailLines) {
        this(id, name, typeLabel, buyPrice, detailLines, null, List.of());
    }

    /** 착용 장비 비교 대상이 존재하는지 여부를 반환한다. */
    public boolean hasEquippedComparison() {
        return equippedDetailLines != null && !equippedDetailLines.isEmpty();
    }
}
