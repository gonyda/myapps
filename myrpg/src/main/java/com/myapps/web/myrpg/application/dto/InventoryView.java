package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 인벤토리 팝업의 전체 뷰 모델.
 *
 * <p>보유 골드와 인벤토리(INVENTORY) 아이템 목록을 담으며, 목록은 획득순(id 오름차순)으로 정렬된다.
 *
 * @param gold 캐릭터 보유 골드
 * @param items 인벤토리 아이템 목록 (획득순)
 */
public record InventoryView(long gold, List<OwnedItemView> items) {}
