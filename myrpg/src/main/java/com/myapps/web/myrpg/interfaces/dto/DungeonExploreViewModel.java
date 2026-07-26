package com.myapps.web.myrpg.interfaces.dto;

import com.myapps.web.myrpg.domain.model.StageEventType;

/**
 * 던전 탐색 화면에 전달할 뷰 모델.
 *
 * <p>현재 스테이지 정보, 이벤트 타입, 이벤트 결과, 보스 여부를 표현한다.
 */
public record DungeonExploreViewModel(String dungeonName, int currentStage, int totalStages,
                                      StageEventType eventType, String eventResult,
                                      boolean isBossStage, boolean bossDefeated) {
}
