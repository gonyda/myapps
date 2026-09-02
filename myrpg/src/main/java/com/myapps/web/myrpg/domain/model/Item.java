package com.myapps.web.myrpg.domain.model;

/**
 * 아이템 카탈로그의 공통 계약을 정의하는 봉인 인터페이스.
 *
 * <p>포션({@link PotionItem}), 장비({@link EquipmentItem}), 재료({@link MaterialItem})를 허용하며, 모든 아이템은 고유
 * 식별자·이름·타입·구매가를 제공한다.
 */
public sealed interface Item permits PotionItem, EquipmentItem, MaterialItem {

    /**
     * 아이템 고유 식별자를 반환한다.
     *
     * @return 아이템 ID 문자열
     */
    String id();

    /**
     * 아이템 이름을 반환한다.
     *
     * @return 아이템 표시명
     */
    String name();

    /**
     * 아이템 유형을 반환한다.
     *
     * @return {@link ItemType} 열거값
     */
    ItemType type();

    /**
     * 상점 구매가(optional).
     *
     * <p>7순위 상점 스펙에서 실제 구매 처리가 확정된다. 없으면({@code null}) 상점 미판매(드랍 전용)로 취급.
     *
     * @return 구매 가격, 또는 상점 미판매이면 {@code null}
     */
    Integer buyPrice();

    /**
     * 아이템 기본 설명/플레이버 텍스트(optional).
     *
     * @return 설명글 문자열, 없으면 {@code null}
     */
    String description();

    /**
     * 아이템의 표시용 아이콘(이모지)을 반환한다.
     *
     * @return 아이콘 이모지 문자열
     */
    String icon();
}
