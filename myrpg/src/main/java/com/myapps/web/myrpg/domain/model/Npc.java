package com.myapps.web.myrpg.domain.model;

/**
 * NPC 도메인 모델을 나타내는 불변 레코드.
 *
 * <p>고정 데이터({@code classpath:data/npc.json})에서 로드되어 메모리에 보관되며,
 * NPC의 식별자, 이름, 유형, 배치 노드, 성격, 대사 풀 정보를 포함한다.
 *
 * @param id          NPC 고유 식별자 (예: "duncan", "neris")
 * @param name        NPC 표시 이름 (예: "던컨", "네리스")
 * @param type        NPC 유형 ({@link NpcType} 열거 상수)
 * @param nodeId      NPC가 배치된 맵 노드 ID (예: "tir-chonaill", "dunbarton")
 * @param personality NPC 성격 설명 (폴백 대사 생성 시 참조)
 * @param lines       NPC 대사 풀 ({@link NpcLines})
 */
public record Npc(
        String id,
        String name,
        NpcType type,
        String nodeId,
        String personality,
        NpcLines lines
) {
}
