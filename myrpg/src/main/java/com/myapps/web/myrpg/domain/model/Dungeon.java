package com.myapps.web.myrpg.domain.model;

/**
 * 던전 정의를 나타내는 불변 레코드.
 *
 * <p>{@code implemented:false}, {@code map:null}을 그대로 노출하며, {@code entranceNodeId}와 {@link
 * MapNode#dungeonId()}로 입구↔던전 참조를 유지한다. 향후 자체 맵({@code nodes} 구조 동일)을 채워 넣을 수 있도록 {@code map} 필드를
 * {@code Object}로 보관한다.
 *
 * @param id 던전 고유 식별자
 * @param name 던전 이름
 * @param entranceNodeId 이 던전의 입구 노드 ID
 * @param implemented 구현 완료 여부 (현재는 항상 {@code false})
 * @param map 던전 내부 맵 데이터 (현재는 항상 {@code null}, 향후 확장용)
 */
public record Dungeon(
        String id, String name, String entranceNodeId, boolean implemented, Object map) {}
