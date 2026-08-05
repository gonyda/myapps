package com.myapps.web.myrpg.domain.model;

import java.util.List;

/**
 * 맵 그래프의 단일 노드를 나타내는 불변 레코드.
 *
 * <p>원본 {@code type} 문자열을 보존하여 렌더링 시 {@code type-{type}} CSS 클래스를 생성할 수 있으며,
 * 파싱된 {@link NodeType} 열거 값으로 로직에서 타입별 처리를 수행한다.
 * 알 수 없는 타입은 {@code nodeType}이 {@code null}이 되며 일반 통행 노드로 취급한다.
 *
 * @param id        노드 고유 식별자
 * @param name      노드 이름 (표시용)
 * @param type      원본 타입 문자열 (CSS 클래스 생성에 사용)
 * @param nodeType  파싱된 노드 타입 열거 값 (미지 타입이면 {@code null})
 * @param x         맵 격자 X 좌표
 * @param y         맵 격자 Y 좌표
 * @param dungeonId 이 노드가 입구인 던전의 ID (던전 입구가 아니면 {@code null})
 * @param theme     상황 멘트 선택에 사용할 테마 (없으면 {@code null}, 이 경우 {@code type} 사용)
 * @param links     이 노드와 연결된 다른 노드 ID 목록 (양방향)
 */
public record MapNode(
        String id,
        String name,
        String type,
        NodeType nodeType,
        int x,
        int y,
        String dungeonId,
        String theme,
        List<String> links
) {
}
