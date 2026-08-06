package com.myapps.web.myrpg.domain.model;

/**
 * 캐릭터의 재능 유형을 정의하는 열거형.
 *
 * <p>현재 스펙에서 실제 사용 값은 {@link #MELEE}이며,
 * {@link #ARCHERY}와 {@link #MAGIC}은 재능 시스템(2순위)을 위해 정의만 유지한다.
 * 라벨은 정보 팝업의 재능 표시에 사용된다.
 */
public enum TalentType {

    /** 근접전투 재능. */
    MELEE("근접전투"),

    /** 활 재능. */
    ARCHERY("활"),

    /** 마법 재능. */
    MAGIC("마법");

    private final String label;

    TalentType(final String label) {
        this.label = label;
    }

    /**
     * 재능의 한글 라벨을 반환한다.
     *
     * @return 재능 라벨 문자열
     */
    public String label() {
        return label;
    }
}
