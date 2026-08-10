package com.myapps.web.myrpg.domain.model;

/**
 * 몬스터 처치 시 드랍될 수 있는 아이템 항목을 나타내는 불변 레코드.
 *
 * <p>각 항목은 드랍 대상 아이템 ID, 드랍 확률(1~100%), 수량 범위를 포함한다.
 * 확률 판정을 통과한 경우에만 {@code [minQuantity, maxQuantity]} 범위에서 수량이 결정된다.
 *
 * @param itemId       드랍 대상 아이템 ID (Item_Catalog_Service에 존재해야 함)
 * @param chancePercent 드랍 확률 (1~100, 백분율)
 * @param minQuantity  최소 드랍 수량 (1 이상)
 * @param maxQuantity  최대 드랍 수량 (minQuantity 이상)
 */
public record ItemDrop(String itemId, int chancePercent, int minQuantity, int maxQuantity) {
}
