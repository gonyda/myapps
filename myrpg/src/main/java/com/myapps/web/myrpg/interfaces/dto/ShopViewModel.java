package com.myapps.web.myrpg.interfaces.dto;

import java.util.List;

import com.myapps.web.myrpg.domain.model.PlayerArmor;
import com.myapps.web.myrpg.domain.model.PlayerWeapon;

/**
 * 상점 화면에 전달할 뷰 모델.
 *
 * <p>플레이어 골드, 판매 가능 무기/방어구, 구매 가능 포션 목록을 표현한다.
 */
public record ShopViewModel(int playerGold, List<SellableWeapon> sellableWeapons,
                            List<SellableArmor> sellableArmors,
                            List<BuyablePotion> buyablePotions) {

    /**
     * 판매 가능한 무기 정보를 나타내는 레코드.
     *
     * @param weapon    무기 엔티티
     * @param sellPrice 판매가
     */
    public record SellableWeapon(PlayerWeapon weapon, int sellPrice) {
    }

    /**
     * 판매 가능한 방어구 정보를 나타내는 레코드.
     *
     * @param armor     방어구 엔티티
     * @param sellPrice 판매가
     */
    public record SellableArmor(PlayerArmor armor, int sellPrice) {
    }

    /**
     * 구매 가능한 포션 정보를 나타내는 레코드.
     *
     * @param itemId   아이템 ID
     * @param name     포션 이름
     * @param buyPrice 구매가
     * @param owned    현재 보유 수량
     */
    public record BuyablePotion(long itemId, String name, int buyPrice, int owned) {
    }
}
