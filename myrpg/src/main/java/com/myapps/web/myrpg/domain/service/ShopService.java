package com.myapps.web.myrpg.domain.service;

import com.myapps.web.myrpg.domain.exception.IllegalEquipmentException;
import com.myapps.web.myrpg.domain.exception.InsufficientGoldException;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerArmor;
import com.myapps.web.myrpg.domain.model.PlayerInventory;
import com.myapps.web.myrpg.domain.model.PlayerWeapon;

/**
 * 상점 판매·구매 관련 순수 도메인 규칙 서비스.
 *
 * <p>전리품 판매가 산출, 무기/방어구 판매 검증 및 골드 지급,
 * 포션 구매 검증 및 인벤토리 누적 로직을 캡슐화한다.
 * 리포지토리 의존 없는 순수 서비스이며, 엔티티 삭제는 호출자(application 계층)의 책임이다.
 */
public class ShopService {

    private static final double ITEM_LEVEL_SCALING = 0.05;

    /**
     * 판매가를 산출한다.
     *
     * <p>공식: {@code 반올림(baseValue × 등급배수 × (1 + 0.05 × itemLevel))}
     *
     * @param baseValue 템플릿 기본 판매가
     * @param grade     아이템 등급
     * @param itemLevel 아이템 레벨 (드랍 던전 권장레벨)
     * @return 산출된 판매가 (HALF_UP 반올림)
     */
    public int sellPrice(final int baseValue, final Grade grade, final int itemLevel) {
        final double multiplier = grade.getSellMultiplier();
        final double levelFactor = 1 + ITEM_LEVEL_SCALING * itemLevel;
        return (int) Math.round(baseValue * multiplier * levelFactor);
    }

    /**
     * 무기를 판매한다.
     *
     * <p>착용 중인 무기는 판매를 거부한다. 정상 판매 시 판매가만큼 골드를 지급한다.
     * 엔티티 삭제는 호출자의 책임이다.
     *
     * @param player    플레이어 엔티티
     * @param weapon    판매할 무기 인스턴스
     * @param baseValue 무기 템플릿 기본 판매가
     * @return 산출된 판매가
     * @throws IllegalEquipmentException 착용 중인 무기를 판매하려는 경우
     */
    public int sellWeapon(final Player player, final PlayerWeapon weapon, final int baseValue) {
        validateWeaponSellable(weapon);
        final int price = sellPrice(baseValue, weapon.getGrade(), weapon.getItemLevel());
        player.changeGold(player.getGold() + price);
        return price;
    }

    /**
     * 방어구를 판매한다.
     *
     * <p>착용 중인 방어구는 판매를 거부한다. 정상 판매 시 판매가만큼 골드를 지급한다.
     * 엔티티 삭제는 호출자의 책임이다.
     *
     * @param player    플레이어 엔티티
     * @param armor     판매할 방어구 인스턴스
     * @param baseValue 방어구 템플릿 기본 판매가
     * @return 산출된 판매가
     * @throws IllegalEquipmentException 착용 중인 방어구를 판매하려는 경우
     */
    public int sellArmor(final Player player, final PlayerArmor armor, final int baseValue) {
        validateArmorSellable(armor);
        final int price = sellPrice(baseValue, armor.getGrade(), armor.getItemLevel());
        player.changeGold(player.getGold() + price);
        return price;
    }

    /**
     * 포션을 구매한다.
     *
     * <p>골드 부족 시 구매를 거부한다. 정상 구매 시 골드를 차감하고
     * 인벤토리 수량을 1 누적한다.
     *
     * @param player    플레이어 엔티티
     * @param inventory 포션 인벤토리 항목
     * @param buyPrice  포션 구매가
     * @throws InsufficientGoldException 보유 골드가 구매가 미만인 경우
     */
    public void buyPotion(final Player player, final PlayerInventory inventory,
                          final int buyPrice) {
        validateGoldSufficient(player, buyPrice);
        player.changeGold(player.getGold() - buyPrice);
        inventory.changeQuantity(inventory.getQuantity() + 1);
    }

    /**
     * 무기가 판매 가능한 상태인지 검증한다.
     *
     * @param weapon 판매 대상 무기
     * @throws IllegalEquipmentException 착용 중인 경우
     */
    private void validateWeaponSellable(final PlayerWeapon weapon) {
        if (weapon.isEquipped()) {
            throw new IllegalEquipmentException("착용 중인 무기는 판매할 수 없습니다.");
        }
    }

    /**
     * 방어구가 판매 가능한 상태인지 검증한다.
     *
     * @param armor 판매 대상 방어구
     * @throws IllegalEquipmentException 착용 중인 경우
     */
    private void validateArmorSellable(final PlayerArmor armor) {
        if (armor.isEquipped()) {
            throw new IllegalEquipmentException("착용 중인 방어구는 판매할 수 없습니다.");
        }
    }

    /**
     * 구매에 필요한 골드가 충분한지 검증한다.
     *
     * @param player   플레이어 엔티티
     * @param buyPrice 구매가
     * @throws InsufficientGoldException 골드 부족 시
     */
    private void validateGoldSufficient(final Player player, final int buyPrice) {
        if (player.getGold() < buyPrice) {
            throw new InsufficientGoldException(
                    "골드가 부족합니다. 필요: " + buyPrice + ", 보유: " + player.getGold());
        }
    }
}
