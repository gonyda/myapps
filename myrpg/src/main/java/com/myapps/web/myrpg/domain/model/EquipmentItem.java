package com.myapps.web.myrpg.domain.model;

import java.util.List;

/**
 * 착용 가능한 장비 아이템을 나타내는 레코드.
 *
 * <p>무기({@link ItemType#WEAPON}) 또는 방어구({@link ItemType#ARMOR})이며, 장비 종류({@link EquipmentKind})에 따라
 * 착용 슬롯이 결정된다. 보너스({@link EquipBonus})는 STAT/VITAL 계열로 분기되어 캐릭터 스탯에 합산된다.
 *
 * <p>{@code buyPrice}는 7순위 상점 스펙에서 실제 구매 처리가 확정된다. 없으면({@code null}) 상점 미판매(드랍 전용)로 취급.
 *
 * @param id 아이템 고유 식별자
 * @param name 아이템 표시명
 * @param type 아이템 유형 (WEAPON 또는 ARMOR)
 * @param kind 장비 세부 종류
 * @param bonuses 장비가 제공하는 보너스 목록
 * @param buyPrice 상점 구매가(nullable, 상점 미판매이면 null)
 * @param maxDurability 최대 내구도
 * @param description 아이템 설명글(nullable)
 */
public record EquipmentItem(
        String id,
        String name,
        ItemType type,
        EquipmentKind kind,
        List<EquipBonus> bonuses,
        Integer buyPrice,
        int maxDurability,
        String description)
        implements Item {

    /**
     * 설명글이 없는 장비 생성을 위한 편의 생성자.
     *
     * @param id 아이템 고유 식별자
     * @param name 아이템 표시명
     * @param type 아이템 유형
     * @param kind 장비 세부 종류
     * @param bonuses 장비가 제공하는 보너스 목록
     * @param buyPrice 상점 구매가
     * @param maxDurability 최대 내구도
     */
    public EquipmentItem(
            final String id,
            final String name,
            final ItemType type,
            final EquipmentKind kind,
            final List<EquipBonus> bonuses,
            final Integer buyPrice,
            final int maxDurability) {
        this(id, name, type, kind, bonuses, buyPrice, maxDurability, null);
    }
}
