package com.myapps.web.myrpg.application.dto;

/**
 * 맵 뷰에서 두 노드 간의 간선을 나타내는 뷰 모델 레코드.
 *
 * <p>범위(미니맵 창 또는 전체지도) 안에 두 노드가 모두 존재하고
 * {@code links}로 실제 연결된 경우에만 생성된다.
 *
 * @param fromNodeId 간선 시작 노드 ID
 * @param toNodeId   간선 끝 노드 ID
 */
public record MapEdge(
        String fromNodeId,
        String toNodeId
) {
}
