package com.myapps.web.myrpg.domain.model;

/**
 * 소비형 포션 아이템을 나타내는 레코드.
 *
 * <p>사용 시 캐릭터의 HP를 {@code healHp}만큼 회복하며,
 * 타입은 항상 {@link ItemType#POTION}이다.
 *
 * @param id       아이템 고유 식별자
 * @param name     아이템 표시명
 * @param healHp   사용 시 HP 회복량
 * @param buyPrice 상점 구매가(nullable, 상점 미판매이면 null)
 */
public record PotionItem(String id, String name, int healHp, Integer buyPrice) implements Item {

    /**
     * 포션 아이템의 타입을 반환한다.
     *
     * @return 항상 {@link ItemType#POTION}
     */
    @Override
    public ItemType type() {
        return ItemType.POTION;
    }
}
