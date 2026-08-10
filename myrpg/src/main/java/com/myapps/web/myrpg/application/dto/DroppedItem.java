package com.myapps.web.myrpg.application.dto;

/**
 * 몬스터 처치 시 드랍된 개별 아이템을 나타내는 불변 레코드.
 *
 * <p>{@link com.myapps.web.myrpg.application.service.MonsterRewardService}의
 * 드랍 계산 결과로 생성되며, 아이템 ID와 확정된 수량을 담는다.
 *
 * @param itemId   드랍된 아이템 ID (Item_Catalog_Service에 존재하는 값)
 * @param quantity 드랍 수량 (1 이상)
 */
public record DroppedItem(String itemId, int quantity) {
}
