package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 미니맵 전체 뷰를 나타내는 뷰 모델 레코드.
 *
 * <p>현재 노드를 중심으로 가로 9칸(dx∈[-4,4]) × 세로 5칸(dy∈[-2,2]) 범위의
 * 노드 셀 목록을 담는다. 최대 45개의 셀을 포함할 수 있다.
 *
 * @param mapName 현재 맵 노드의 이름 (표시용)
 * @param cells   미니맵 격자 내 셀 목록
 */
public record MinimapView(
        String mapName,
        List<MinimapCell> cells
) {
}
