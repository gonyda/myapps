package com.myapps.web.myrpg.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * 몬스터의 유형을 나타내는 열거형.
 *
 * <p>각 상수는 타입 문자열, 한글 라벨, 버튼 배지, 행동 버튼 라벨 목록을 내장하여 몬스터 타입 정보의 단일 소스 역할을 한다. 새로운 몬스터 유형 추가 시 상수 하나만
 * 추가하면 되며, 라벨이나 행동 목록 누락 시 컴파일 오류가 발생한다.
 */
public enum MonsterType {

    /** 일반 몬스터. */
    NORMAL("normal", "일반", "", List.of("전투")),

    /** 보스 몬스터. */
    BOSS("boss", "보스", "👑", List.of("전투"));

    private final String typeString;
    private final String label;
    private final String badge;
    private final List<String> actionLabels;

    MonsterType(
            final String typeString,
            final String label,
            final String badge,
            final List<String> actionLabels) {
        this.typeString = typeString;
        this.label = label;
        this.badge = badge;
        this.actionLabels = actionLabels;
    }

    /**
     * 원본 타입 문자열을 반환한다.
     *
     * @return 이 열거 값에 대응하는 타입 문자열 (예: "normal", "boss")
     */
    public String typeString() {
        return typeString;
    }

    /**
     * 몬스터 유형의 한글 라벨을 반환한다.
     *
     * @return 화면에 표시할 한글 라벨 (예: "일반", "보스")
     */
    public String label() {
        return label;
    }

    /**
     * 몬스터 유형의 버튼 배지를 반환한다.
     *
     * <p>일반 몬스터는 빈 문자열, 보스 몬스터는 "👑"을 반환한다. 상호작용 버튼 라벨에서 이름 뒤에 붙는 접미사로 사용된다.
     *
     * @return 버튼 배지 문자열 (일반="", 보스="👑")
     */
    public String badge() {
        return badge;
    }

    /**
     * 몬스터 유형에 정의된 행동 버튼 라벨 목록을 반환한다.
     *
     * <p>반환되는 목록은 불변이며 정의 순서를 보존한다.
     *
     * @return 행동 버튼 라벨의 불변 목록 (예: ["전투"])
     */
    public List<String> actionLabels() {
        return actionLabels;
    }

    /**
     * 타입 문자열에 대응하는 {@code MonsterType}을 반환한다.
     *
     * <p>알 수 없는 타입 문자열이나 {@code null}은 빈 {@code Optional}을 반환한다.
     *
     * @param type 원본 타입 문자열 (예: "normal", "boss")
     * @return 대응하는 {@code MonsterType}을 감싼 {@code Optional}, 미지 타입이면 빈 {@code Optional}
     */
    public static Optional<MonsterType> fromType(final String type) {
        if (type == null) {
            return Optional.empty();
        }
        for (final MonsterType monsterType : values()) {
            if (monsterType.typeString.equals(type)) {
                return Optional.of(monsterType);
            }
        }
        return Optional.empty();
    }
}
