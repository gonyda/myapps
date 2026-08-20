package com.myapps.web.myrpg.domain.model;

import java.util.Optional;

/**
 * 맵 노드의 유형을 나타내는 열거형.
 *
 * <p>알려진 값은 {@code TOWN}, {@code FIELD}, {@code DUNGEON}이며, 알 수 없는 타입 문자열은 일반 통행 노드로 취급한다. 새로운 유형
 * 추가 시 기존 로직을 깨지 않도록 확장 가능한 형태로 설계되었다.
 */
public enum NodeType {

    /** 마을 노드. */
    TOWN("town"),

    /** 자유필드 노드. */
    FIELD("field"),

    /** 던전 입구 노드. */
    DUNGEON("dungeon");

    private final String typeString;

    NodeType(final String typeString) {
        this.typeString = typeString;
    }

    /**
     * 원본 타입 문자열을 반환한다.
     *
     * @return 이 열거 값에 대응하는 소문자 타입 문자열
     */
    public String getTypeString() {
        return typeString;
    }

    /**
     * 원본 타입 문자열에 대응하는 {@code NodeType}을 반환한다.
     *
     * <p>알 수 없는 타입 문자열은 빈 {@code Optional}을 반환하며, 이 경우 해당 노드는 일반 통행 노드로 취급한다.
     *
     * @param type 원본 타입 문자열 (예: "town", "field", "dungeon")
     * @return 대응하는 {@code NodeType}을 감싼 {@code Optional}, 미지 타입이면 빈 {@code Optional}
     */
    public static Optional<NodeType> fromType(final String type) {
        if (type == null) {
            return Optional.empty();
        }
        for (final NodeType nodeType : values()) {
            if (nodeType.typeString.equals(type)) {
                return Optional.of(nodeType);
            }
        }
        return Optional.empty();
    }
}
