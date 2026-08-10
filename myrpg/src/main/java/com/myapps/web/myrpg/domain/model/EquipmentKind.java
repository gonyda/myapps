package com.myapps.web.myrpg.domain.model;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * 장비의 세부 종류를 정의하는 열거형.
 *
 * <p>각 종류는 한글 라벨, 기본 착용 슬롯({@code primarySlot}),
 * 착용 시 점유하는 슬롯 집합({@code requiredSlots})을 갖는다.
 * 양손검은 주무기+보조손 두 슬롯을 동시에 점유하여 방패와 상호 배타적이다.
 */
public enum EquipmentKind {

    /** 한손검 — 주무기 슬롯 점유. */
    ONE_HANDED_SWORD("한손검", EquipSlot.MAIN_HAND, Set.of(EquipSlot.MAIN_HAND)),

    /** 양손검 — 주무기+보조손 슬롯 동시 점유. */
    TWO_HANDED_SWORD("양손검", EquipSlot.MAIN_HAND, Set.of(EquipSlot.MAIN_HAND, EquipSlot.OFF_HAND)),

    /** 방패 — 보조손 슬롯 점유. */
    SHIELD("방패", EquipSlot.OFF_HAND, Set.of(EquipSlot.OFF_HAND)),

    /** 갑옷 — 몸통 슬롯 점유. */
    ARMOR_BODY("갑옷", EquipSlot.BODY, Set.of(EquipSlot.BODY));

    private final String label;
    private final EquipSlot primarySlot;
    private final Set<EquipSlot> requiredSlots;

    EquipmentKind(final String label, final EquipSlot primarySlot, final Set<EquipSlot> requiredSlots) {
        this.label = label;
        this.primarySlot = primarySlot;
        this.requiredSlots = requiredSlots;
    }

    /**
     * UI에 표시할 한글 라벨을 반환한다.
     *
     * @return 장비 종류의 라벨 문자열
     */
    public String label() {
        return label;
    }

    /**
     * 장비의 기본 착용 슬롯을 반환한다.
     *
     * <p>같은 {@code primarySlot}을 가진 장비끼리 스왑이 발생한다.
     *
     * @return 기본 착용 {@link EquipSlot}
     */
    public EquipSlot primarySlot() {
        return primarySlot;
    }

    /**
     * 착용 시 점유하는 슬롯 집합(불변)을 반환한다.
     *
     * <p>양손검은 {@code {MAIN_HAND, OFF_HAND}}를 점유하여 방패와 배타적이다.
     *
     * @return 점유 슬롯의 불변 집합
     */
    public Set<EquipSlot> requiredSlots() {
        return requiredSlots;
    }

    /**
     * 문자열 코드(enum 이름의 소문자 변환)로부터 대응하는 {@code EquipmentKind}를 찾는다.
     *
     * <p>비교는 대소문자를 무시한다. 정의되지 않은 코드에 대해서는 빈 {@code Optional}을 반환한다.
     *
     * @param code 변환할 문자열 코드
     * @return 대응하는 {@code EquipmentKind}, 또는 미지 코드이면 {@code Optional.empty()}
     */
    public static Optional<EquipmentKind> fromString(final String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(kind -> kind.name().equalsIgnoreCase(code))
                .findFirst();
    }
}
