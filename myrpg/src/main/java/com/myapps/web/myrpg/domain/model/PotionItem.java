package com.myapps.web.myrpg.domain.model;

/**
 * 소비형 포션 아이템을 나타내는 레코드.
 *
 * <p>사용 시 캐릭터의 HP, MP, Stamina를 각각 {@code healHp}, {@code healMp}, {@code healStamina}만큼 회복하며, 타입은
 * 항상 {@link ItemType#POTION}이다.
 *
 * @param id 아이템 고유 식별자
 * @param name 아이템 표시명
 * @param healHp 사용 시 HP 회복량 (0 이상)
 * @param healMp 사용 시 MP 회복량 (0 이상)
 * @param healStamina 사용 시 스태미나 회복량 (0 이상)
 * @param buyPrice 상점 구매가(nullable, 상점 미판매이면 null)
 */
public record PotionItem(
        String id, String name, int healHp, int healMp, int healStamina, Integer buyPrice)
        implements Item {

    /**
     * HP 회복 전용 포션 생성을 위한 편의 생성자.
     *
     * @param id 아이템 고유 식별자
     * @param name 아이템 표시명
     * @param healHp 사용 시 HP 회복량
     * @param buyPrice 상점 구매가
     */
    public PotionItem(
            final String id, final String name, final int healHp, final Integer buyPrice) {
        this(id, name, healHp, 0, 0, buyPrice);
    }

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
