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
    DEFENSE("방어"),

    /** 회복 스킬 (힐링). */
    RECOVERY("회복"),

    /** 결전 궁극기 스킬 (메테오, 파이널 히트/샷). */
    ULTIMATE("궁극기"),

    /** 상시 영구 패시브 스킬 (마스터리, 메디테이션 등). */
    PASSIVE("패시브"),

    /** 버프 스킬 (마나 실드). */
    BUFF("버프"),

    /** 디버프 스킬 (레이지 임팩트). */
    DEBUFF("디버프"),

    /** 군중 제어 스킬 (스파이더 샷). */
    CC("제어"),

    /** 지속 피해 스킬 (미라지 미사일). */
    DOT("지속피해");

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
     * @param value 조회할 문자열 (null 허용)
     * @return 일치하는 SkillType을 감싼 {@code Optional}, 불일치 또는 null이면 빈 {@code Optional}
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
