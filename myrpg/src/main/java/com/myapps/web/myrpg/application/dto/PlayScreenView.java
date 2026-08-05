package com.myapps.web.myrpg.application.dto;

import java.util.List;

import com.myapps.web.myrpg.domain.model.ActionLogEntry;

/**
 * 플레이 화면 전체 뷰를 집계하는 뷰 모델 레코드.
 *
 * <p>상단바, 미니맵, 전체지도, 상황 멘트, NPC 대화,
 * 상호작용 목록, NPC 행동 버튼, 행동 로그 등 화면 렌더링에 필요한 모든 데이터를 하나로 묶어 제공한다.
 *
 * @param topBar       상단바 뷰 모델
 * @param minimap      미니맵 뷰 모델
 * @param fullMap      전체지도 뷰 모델
 * @param ambience     상황 멘트 텍스트
 * @param npcName      현재 노드 NPC 이름 (없으면 null)
 * @param npcDialogue  NPC 대사 텍스트 (없으면 null)
 * @param interactions 상호작용 대상 목록 (없으면 null)
 * @param npcActions   NPC 행동 버튼 목록 (대화 중이 아니면 null)
 * @param logs         행동 로그 항목 목록 (오름차순)
 */
public record PlayScreenView(
        TopBarView topBar,
        MinimapView minimap,
        FullMapView fullMap,
        String ambience,
        String npcName,
        String npcDialogue,
        List<InteractionItem> interactions,
        List<NpcActionButton> npcActions,
        List<ActionLogEntry> logs
) {
}
