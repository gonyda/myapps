package com.myapps.web.myrpg.domain.model;

import java.util.Optional;

/**
 * 스킬 랭크를 정의하는 열거형(16단계).
 *
 * <p>랭크 사다리는 {@code F → E → D → C → B → A → 9 → 8 → 7 → 6 → 5 → 4 → 3 → 2 → 1 → Master}
 * 순서이며, {@code F}(order 0)에서 시작하여 {@code MASTER}(order 15)까지 승급할 수 있다.
 * 각 상수는 표시용 라벨({@link #label()}), 순서 번호({@link #order()}),
 * 다음 랭크 조회({@link #next()}), 최고 랭크 판정({@link #isMax()})을 제공한다.
 */
public enum SkillRank {

    /** F 랭크 (order 0). */
    F("F", 0),

    /** E 랭크 (order 1). */
    E("E", 1),

    /** D 랭크 (order 2). */
    D("D", 2),

    /** C 랭크 (order 3). */
    C("C", 3),

    /** B 랭크 (order 4). */
    B("B", 4),

    /** A 랭크 (order 5). */
    A("A", 5),

    /** 9 랭크 (order 6). */
    R9("9", 6),

    /** 8 랭크 (order 7). */
    R8("8", 7),

    /** 7 랭크 (order 8). */
    R7("7", 8),

    /** 6 랭크 (order 9). */
    R6("6", 9),

    /** 5 랭크 (order 10). */
    R5("5", 10),

    /** 4 랭크 (order 11). */
    R4("4", 11),

    /** 3 랭크 (order 12). */
    R3("3", 12),

    /** 2 랭크 (order 13). */
    R2("2", 13),

    /** 1 랭크 (order 14). */
    R1("1", 14),

    /** 마스터 랭크 (order 15, 최고 랭크). */
    MASTER("Master", 15);

    private static final SkillRank[] VALUES = values();

    private final String label;
    private final int order;

    SkillRank(final String label, final int order) {
        this.label = label;
        this.order = order;
    }

    /**
     * 랭크의 표시용 라벨을 반환한다.
     *
     * <p>{@code F}~{@code A}는 알파벳 그대로, {@code R9}~{@code R1}은 숫자만("9"~"1"),
     * {@code MASTER}는 "Master"를 반환한다.
     *
     * @return 표시용 라벨 문자열
     */
    public String label() {
        return label;
    }

    /**
     * 랭크의 순서 번호를 반환한다.
     *
     * <p>{@code F}=0, {@code E}=1, ... , {@code MASTER}=15.
     *
     * @return 0부터 15까지의 순서 번호
     */
    public int order() {
        return order;
    }

    /**
     * 다음 랭크를 반환한다.
     *
     * <p>{@code MASTER}인 경우 빈 값을 반환한다.
     *
     * @return 다음 랭크의 {@link Optional}, 최고 랭크이면 {@link Optional#empty()}
     */
    public Optional<SkillRank> next() {
        if (isMax()) {
            return Optional.empty();
        }
        return Optional.of(VALUES[ordinal() + 1]);
    }

    /**
     * 현재 랭크가 최고 랭크({@code MASTER})인지 판정한다.
     *
     * @return 최고 랭크이면 {@code true}
     */
    public boolean isMax() {
        return this == MASTER;
    }

    /**
     * 신규 스킬의 초기 랭크({@code F})를 반환한다.
     *
     * @return {@code F} 랭크
     */
    public static SkillRank first() {
        return F;
    }
}
