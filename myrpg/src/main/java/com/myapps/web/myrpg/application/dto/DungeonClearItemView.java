package com.myapps.web.myrpg.application.dto;

/**
 * 던전 클리어 보상 팝업에 표시할 개별 아이템 뷰 레코드.
 *
 * @param itemId 아이템 식별자
 * @param itemName 화면에 표시할 아이템 한글 명칭
 * @param quantity 지급된 수량
 * @param icon 아이템 표시용 아이콘 이모지
 */
public record DungeonClearItemView(String itemId, String itemName, int quantity, String icon) {

    /** 하위 호환용 편의 생성자. */
    public DungeonClearItemView(final String itemId, final String itemName, final int quantity) {
        this(itemId, itemName, quantity, "📦");
    }
}
