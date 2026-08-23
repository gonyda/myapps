package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 던전 클리어 보상 결과 모달에 바인딩할 프론트엔드 뷰 모델.
 *
 * @param dungeonId 클리어한 던전 메타데이터 ID
 * @param dungeonName 클리어한 던전 한글 명칭 (예: "알비 던전")
 * @param expGained 획득 경험치
 * @param goldGained 획득 골드
 * @param items 획득 아이템 목록
 */
public record DungeonClearView(
        String dungeonId,
        String dungeonName,
        int expGained,
        int goldGained,
        List<DungeonClearItemView> items) {

    /** 방어적 복사를 적용한 생성자. */
    public DungeonClearView {
        items = items != null ? List.copyOf(items) : List.of();
    }
}
