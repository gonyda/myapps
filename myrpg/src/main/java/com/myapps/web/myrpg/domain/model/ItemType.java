package com.myapps.web.myrpg.domain.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * 아이템의 유형을 정의하는 열거형.
 *
 * <p>포션(소비형), 무기, 방어구로 분류하며 장비 여부 판별과 문자열 코드↔enum 변환을 제공한다.
 */
public enum ItemType {

    /** 소비형 포션 아이템. */
    POTION("potion", "포션"),

    /** 무기 장비 아이템. */
    WEAPON("weapon", "무기"),

    /** 방어구 장비 아이템. */
    ARMOR("armor", "방어구"),

    /** 생활 채집 및 제작용 재료 아이템. */
    MATERIAL("material", "재료");

    private final String code;
    private final String label;

    ItemType(final String code, final String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * JSON/카탈로그에서 사용하는 소문자 코드를 반환한다.
     *
     * @return 아이템 타입의 코드 문자열
     */
    public String code() {
        return code;
    }

    /**
     * UI에 표시할 한글 라벨을 반환한다.
     *
     * @return 아이템 타입의 라벨 문자열
     */
    public String label() {
        return label;
    }

    /**
     * 이 타입이 장비(착용 가능)인지 여부를 반환한다.
     *
     * <p>무기({@code WEAPON}) 또는 방어구({@code ARMOR})이면 {@code true}.
     *
     * @return 장비 여부
     */
    public boolean isEquipment() {
        return this == WEAPON || this == ARMOR;
    }

    /**
     * 이 타입이 인벤토리/은행의 동일 슬롯에 수량 스택 누적이 가능한지 여부를 반환한다.
     *
     * <p>포션({@code POTION}) 또는 재료({@code MATERIAL})이면 {@code true}.
     *
     * @return 스택 누적 가능 여부
     */
    public boolean isStackable() {
        return this == POTION || this == MATERIAL;
    }

    /**
     * 문자열 코드로부터 대응하는 {@code ItemType}을 찾는다.
     *
     * <p>정의되지 않은 코드에 대해서는 빈 {@code Optional}을 반환한다.
     *
     * @param code 변환할 문자열 코드
     * @return 대응하는 {@code ItemType}, 또는 미지 코드이면 {@code Optional.empty()}
     */
    public static Optional<ItemType> fromString(final String code) {
        return Arrays.stream(values()).filter(type -> type.code.equals(code)).findFirst();
    }
}
