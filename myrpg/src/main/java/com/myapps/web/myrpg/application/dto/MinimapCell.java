package com.myapps.web.myrpg.application.dto;

/**
 * 미니맵의 단일 셀을 나타내는 뷰 모델 레코드.
 *
 * <p>미니맵 격자에서 하나의 맵 노드 위치와 연결 정보를 담는다.
 * {@code gridColumn}과 {@code gridRow}는 CSS {@code grid-column}/{@code grid-row}에 직접 매핑된다.
 *
 * @param nodeId     노드 고유 식별자
 * @param gridColumn CSS grid-column 값 (중심 노드 = 5, 범위 1~9)
 * @param gridRow    CSS grid-row 값 (중심 노드 = 3, 범위 1~5)
 * @param type       원본 노드 타입 문자열 (CSS 클래스 생성용)
 * @param current    현재 노드 여부
 * @param linkRight  오른쪽 방향 연결 존재 여부 (오른쪽 이웃이 창 안에 있고 실제 연결일 때)
 * @param linkDown   아래쪽 방향 연결 존재 여부 (아래 이웃이 창 안에 있고 실제 연결일 때)
 */
public record MinimapCell(
        String nodeId,
        int gridColumn,
        int gridRow,
        String type,
        boolean current,
        boolean linkRight,
        boolean linkDown
) {
}
