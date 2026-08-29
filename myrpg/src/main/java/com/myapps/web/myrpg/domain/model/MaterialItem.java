package com.myapps.web.myrpg.domain.model;

/**
 * 생활 채집 및 제작 등에 사용되는 재료 아이템 불변 레코드.
 *
 * <p>고유 식별자({@code id}), 표시명({@code name}), 상점 구매가({@code buyPrice}), 설명글({@code description})을
 * 제공하며 항상 {@link ItemType#MATERIAL} 유형을 가집니다.
 *
 * @param id 아이템 고유 식별자
 * @param name 아이템 표시명
 * @param buyPrice 상점 구매가 (null일 경우 상점 미판매)
 * @param description 아이템 설명글 (null일 경우 설명 없음)
 */
public record MaterialItem(String id, String name, Integer buyPrice, String description)
        implements Item {

    /**
     * 설명글이 없는 기본 재료 아이템 생성을 위한 편의 생성자.
     *
     * @param id 아이템 고유 식별자
     * @param name 아이템 표시명
     * @param buyPrice 상점 구매가
     */
    public MaterialItem(final String id, final String name, final Integer buyPrice) {
        this(id, name, buyPrice, null);
    }

    @Override
    public ItemType type() {
        return ItemType.MATERIAL;
    }
}
