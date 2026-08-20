package com.myapps.web.myrpg.domain.model;

import java.util.List;

/**
 * NPC 도메인 모델을 나타내는 불변 레코드.
 *
 * <p>고정 데이터({@code classpath:data/npc.json})에서 로드되어 메모리에 보관되며, NPC의 식별자, 이름, 유형, 배치 노드, 성격, 대사 풀,
 * 상점 판매 품목 정보를 포함한다.
 *
 * @param id NPC 고유 식별자 (예: "duncan", "neris")
 * @param name NPC 표시 이름 (예: "던컨", "네리스")
 * @param type NPC 유형 ({@link NpcType} 열거 상수)
 * @param nodeId NPC가 배치된 맵 노드 ID (예: "tir-chonaill", "dunbarton")
 * @param personality NPC 성격 설명 (폴백 대사 생성 시 참조)
 * @param lines NPC 대사 풀 ({@link NpcLines})
 * @param shopItems NPC 상점에서 판매하는 아이템 카탈로그 ID 목록 (불변, 정의 순서 보존)
 */
public record Npc(
        String id,
        String name,
        NpcType type,
        String nodeId,
        String personality,
        NpcLines lines,
        List<String> shopItems) {

    /**
     * 상점 판매 품목이 없는 NPC를 생성하는 보조 생성자.
     *
     * <p>기존 6-인자 호출부와의 하위 호환을 유지하기 위해 {@code shopItems}를 빈 불변 목록으로 초기화한다.
     *
     * @param id NPC 고유 식별자
     * @param name NPC 표시 이름
     * @param type NPC 유형
     * @param nodeId NPC가 배치된 맵 노드 ID
     * @param personality NPC 성격 설명
     * @param lines NPC 대사 풀
     */
    public Npc(
            final String id,
            final String name,
            final NpcType type,
            final String nodeId,
            final String personality,
            final NpcLines lines) {
        this(id, name, type, nodeId, personality, lines, List.of());
    }
}
