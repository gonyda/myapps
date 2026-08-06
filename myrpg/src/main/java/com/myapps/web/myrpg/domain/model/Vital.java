package com.myapps.web.myrpg.domain.model;

/**
 * HP, MP, Stamina 등 현재값/최대값 쌍을 나타내는 순수 표시 VO (Value Object).
 *
 * <p>이 record는 JPA 엔티티에 직접 매핑되지 않는다.
 * 저장되는 값은 현재값({@code current})뿐이며, 최대값({@code max})은
 * 레벨·장비·스킬 등으로부터 계산되어 조립된다.
 * 표시·로직 모두 정수이며, 현재값이 0이어도 보정하지 않는다.
 *
 * @param current 현재값
 * @param max     최대값
 */
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
