package com.myapps.web.myrpg.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * NPC의 유형을 나타내는 열거형.
 *
 * <p>각 상수는 타입 문자열, 한글 라벨, 행동 버튼 라벨 목록을 내장하여
 * NPC 타입 정보의 단일 소스 역할을 한다.
 * 새로운 NPC 유형 추가 시 상수 하나만 추가하면 되며,
 * 라벨이나 행동 목록 누락 시 컴파일 오류가 발생한다.
 */
public enum NpcType {

    /** 촌장 NPC. */
    CHIEF("chief", "촌장", "🏡", List.of("퀘스트")),

    /** 대장간 NPC. */
    BLACKSMITH("blacksmith", "대장간", "⚒️", List.of("상점", "수리")),

    /** 마법학교 NPC. */
    MAGIC_SCHOOL("magic-school", "마법학교", "🕍", List.of("상점", "인챈트")),

    /** 학교 NPC. */
    SCHOOL("school", "학교", "🏫", List.of("상점")),

    /** 힐러집 NPC. */
    HEALER("healer", "힐러집", "🏥", List.of("상점", "치료받기")),

    /** 은행 NPC. */
    BANK("bank", "은행", "🏦", List.of("은행"));

    private final String typeString;
    private final String label;
    private final String emoji;
    private final List<String> actionLabels;

    NpcType(final String typeString, final String label, final String emoji, final List<String> actionLabels) {
        this.typeString = typeString;
        this.label = label;
        this.emoji = emoji;
        this.actionLabels = actionLabels;
    }

    /**
     * 원본 타입 문자열을 반환한다.
     *
     * @return 이 열거 값에 대응하는 타입 문자열 (예: "chief", "blacksmith")
     */
    public String typeString() {
        return typeString;
    }

    /**
     * NPC 유형의 한글 라벨을 반환한다.
     *
     * @return 화면에 표시할 한글 라벨 (예: "촌장", "대장간")
     */
    public String label() {
        return label;
    }

    /**
     * NPC 유형의 이모지를 반환한다.
     *
     * @return 화면에 표시할 이모지 (예: "⚒️", "🧙")
     */
    public String emoji() {
        return emoji;
    }

    /**
     * NPC 유형에 정의된 행동 버튼 라벨 목록을 반환한다.
     *
     * <p>반환되는 목록은 불변이며 정의 순서를 보존한다.
     *
     * @return 행동 버튼 라벨의 불변 목록 (예: ["상점", "수리"])
     */
    public List<String> actionLabels() {
        return actionLabels;
    }

    /**
     * 타입 문자열에 대응하는 {@code NpcType}을 반환한다.
     *
     * <p>알 수 없는 타입 문자열이나 {@code null}은 빈 {@code Optional}을 반환한다.
     *
     * @param type 원본 타입 문자열 (예: "chief", "healer")
     * @return 대응하는 {@code NpcType}을 감싼 {@code Optional}, 미지 타입이면 빈 {@code Optional}
     */
    public static Optional<NpcType> fromType(final String type) {
        if (type == null) {
            return Optional.empty();
        }
        for (final NpcType npcType : values()) {
            if (npcType.typeString.equals(type)) {
                return Optional.of(npcType);
            }
        }
        return Optional.empty();
    }
}
