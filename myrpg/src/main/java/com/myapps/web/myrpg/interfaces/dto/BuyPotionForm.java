package com.myapps.web.myrpg.interfaces.dto;

/**
 * 포션 구매 폼 DTO.
 *
 * <p>구매할 포션의 아이템 식별자를 전달받는다.
 */
public record BuyPotionForm(Long itemId) {
}
