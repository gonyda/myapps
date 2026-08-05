package com.myapps.web.myrpg.domain.model;

/**
 * 행동 로그의 단일 항목을 나타내는 불변 레코드.
 *
 * <p>각 항목은 타임스탬프, 메시지, 타입으로 구성된다.
 * 타임스탬프는 {@code yyyy-MM-dd HH:mm:ss} 형식이며, 타입 미지정 시 {@code move}로 설정된다.
 *
 * @param timestamp 항목 생성 시각 ({@code yyyy-MM-dd HH:mm:ss} 형식)
 * @param message   로그 메시지 텍스트
 * @param type      로그 타입 (예: {@code move})
 */
public record ActionLogEntry(
        String timestamp,
        String message,
        String type
) {
}
