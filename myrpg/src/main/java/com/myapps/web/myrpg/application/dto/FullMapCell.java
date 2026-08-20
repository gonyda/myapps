package com.myapps.web.myrpg.application.dto;

import java.util.List;

/**
 * 전체지도의 단일 셀을 나타내는 뷰 모델 레코드.
 *
 * <p>바운딩박스 기준으로 배치된 하나의 맵 노드와 그 연결 정보를 담는다. {@code gridColumn=x-minX+1}, {@code gridRow=y-minY+1}로
 * 계산된다.
 *
 * @param nodeId 노드 고유 식별자
 * @param name 노드 이름 (라벨 표시용)
 * @param gridColumn CSS grid-column 값 (바운딩박스 기준, 1부터 시작)
 * @param gridRow CSS grid-row 값 (바운딩박스 기준, 1부터 시작)
 * @param type 원본 노드 타입 문자열 (CSS 클래스 생성용)
 * @param current 현재 노드 여부
 * @param linkRight 오른쪽 방향 연결 존재 여부 (좌표 이웃이 존재하고 실제 연결일 때)
 * @param linkDown 아래쪽 방향 연결 존재 여부 (좌표 이웃이 존재하고 실제 연결일 때)
 * @param links 이 노드와 연결된 다른 노드 ID 목록
 */
public record FullMapCell(
        String nodeId,
        String name,
        int gridColumn,
        int gridRow,
        String type,
        boolean current,
        boolean linkRight,
        boolean linkDown,
        List<String> links) {}
