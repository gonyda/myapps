package com.myapps.web.myrpg.domain.model.vo;

/**
 * 보물상자 이벤트의 보상 결과를 나타내는 값 객체.
 *
 * <p>보상 종류에 따라 골드, 아이템 ID, 장비 드랍 중 해당 필드만 유효하다.
 */
public record TreasureReward(TreasureKind kind, int gold, Long itemId, DropResult equipment) {
}
