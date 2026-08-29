package com.myapps.web.myrpg.application.dto;

/**
 * 장작 채집 시도 결과를 담는 불변 DTO 레코드.
 *
 * @param success 채집 성공 여부 (50% 확률)
 * @param message 사용자 안내용 결과 메시지
 * @param itemId 획득한 아이템 ID (실패 시 null)
 * @param currentStamina 채집 후 잔여 스태미나
 * @param maxStamina 최대 스태미나
 */
public record WoodcutResult(
        boolean success, String message, String itemId, int currentStamina, int maxStamina) {}
