package com.myapps.web.myrpg.domain.model;

import jakarta.persistence.Embeddable;

/**
 * HP, MP, Stamina 등 현재값/최대값 쌍을 나타내는 불변 임베더블 값 객체.
 *
 * <p>표시·저장 모두 정수이며, 현재값이 0이어도 보정하지 않는다.
 * {@code CharacterProgress}에서 {@code @AttributeOverrides}를 통해
 * hp, mp, stamina 각각의 컬럼으로 매핑된다.
 *
 * @param current 현재값
 * @param max     최대값
 */
@Embeddable
public record Vital(
        int current,
        int max
) {

    /** 신규 캐릭터 기본 Vital 값 (100/100). */
    private static final int DEFAULT_VALUE = 100;

    /**
     * 신규 캐릭터용 기본 Vital을 생성한다 (100/100).
     *
     * @return 기본값이 설정된 Vital 인스턴스
     */
    public static Vital createDefault() {
        return new Vital(DEFAULT_VALUE, DEFAULT_VALUE);
    }
}
