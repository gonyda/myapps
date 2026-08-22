package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 던전 보스 처치 및 던전 클리어 시 지급된 보상과 던전 정보를 담는 DTO 레코드.
 *
 * @param dungeonId 클리어한 던전 메타데이터 ID
 * @param dungeonName 클리어한 던전 이름
 * @param expGained 지급된 경험치량
 * @param goldGained 지급된 골드량
 * @param items 지급된 보상 아이템 목록
 */
public record DungeonClearResult(
        String dungeonId,
        String dungeonName,
        int expGained,
        int goldGained,
        List<DroppedItem> items) {

    /** 방어적 복사를 적용한 생성자. */
    public DungeonClearResult {
        items = items != null ? List.copyOf(items) : List.of();
    }
}
