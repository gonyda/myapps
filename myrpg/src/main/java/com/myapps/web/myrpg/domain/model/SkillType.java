package com.myapps.web.myrpg.domain.model;

import java.util.Optional;

/**
 * 스킬의 공격/방어 유형을 정의하는 열거형.
 *
 * <p>각 상수는 한글 라벨을 보유하며, 카탈로그 파싱 시 문자열에서 안전하게 변환할 수 있도록 {@link #fromString(String)} 팩토리를 제공한다. 미지
 * 문자열은 빈 {@code Optional}을 반환한다.
 */
public enum SkillType {

    /** 일반 스킬. */
    NORMAL("일반"),

    /** 강 스킬. */
    HEAVY("강"),

    /** 방어 스킬. */
    DEFENSE("방어");

    private final String label;

    SkillType(final String label) {
        this.label = label;
    }

    /**
     * 스킬 타입의 한글 라벨을 반환한다.
     *
     * @return 타입 라벨 문자열 (예: "일반", "강", "방어")
     */
    public String label() {
        return label;
    }

    /**
     * 문자열로부터 {@code SkillType}을 안전하게 조회한다.
     *
     * <p>상수명 비교는 대소문자를 구분한다(예: "NORMAL"). {@code null}, 공백, 알려지지 않은 문자열이면 빈 {@code Optional}을 반환한다.
     *
     * @param value 조회할 스킬 타입 상수명
     * @return 유효한 상수명이면 해당 {@code SkillType}을 담은 {@code Optional}, 그 외 빈 값
     */
    public static Optional<SkillType> fromString(final String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(SkillType.valueOf(value));
        } catch (final IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
