package com.myapps.web.myrpg.domain.model;

/**
 * 스킬 사용 시 소모되는 자원의 종류를 정의하는 열거형.
 *
 * <p>{@code MAGIC} 재능 스킬은 {@code MP}를, 그 외 재능 스킬은 {@code STAMINA}를 소모한다.
 * 자원 종류는 {@code SkillTalent}에서 파생되며 {@code skill.json}에 별도 저장하지 않는다.
 */
public enum ResourceKind {

    /** 스태미나 자원 (근접전투/활/공용 스킬 소모). */
    STAMINA("스태미나"),

    /** MP 자원 (마법 스킬 소모). */
    MP("MP");

    private final String label;

    ResourceKind(final String label) {
        this.label = label;
    }

    /**
     * 자원 종류의 한글 라벨을 반환한다.
     *
     * @return 자원 라벨 문자열 (예: "스태미나", "MP")
     */
    public String label() {
        return label;
    }
}
