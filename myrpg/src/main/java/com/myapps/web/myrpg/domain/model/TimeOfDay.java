package com.myapps.web.myrpg.domain.model;

/**
 * 게임 내 시간대를 나타내는 열거형.
 *
 * <p>각 상수는 key 문자열과 반열린 구간 {@code [from, to)}을 내장하여 시간대 정보의 단일 소스 역할을 한다. 6개의 구간이 {@code [0, 24)}을
 * 빈틈 없이 분할하므로, 0~23 범위의 모든 시(hour)는 정확히 하나의 시간대에 대응한다.
 */
public enum TimeOfDay {

    /** 심야 시간대. */
    LATE_NIGHT("late-night", 0, 5, "🌌"),

    /** 새벽 시간대. */
    DAWN("dawn", 5, 8, "🌅"),

    /** 오전 시간대. */
    MORNING("morning", 8, 12, "🌄"),

    /** 오후 시간대. */
    AFTERNOON("afternoon", 12, 16, "☀️"),

    /** 늦은 오후 시간대. */
    LATE_AFTERNOON("late-afternoon", 16, 19, "🌇"),

    /** 밤 시간대. */
    NIGHT("night", 19, 24, "🌙");

    private final String key;
    private final int from;
    private final int to;
    private final String emoji;

    TimeOfDay(final String key, final int from, final int to, final String emoji) {
        this.key = key;
        this.from = from;
        this.to = to;
        this.emoji = emoji;
    }

    /**
     * 이 시간대의 식별 키를 반환한다.
     *
     * @return 소문자 케밥 케이스 키 문자열 (예: "late-night", "dawn")
     */
    public String key() {
        return key;
    }

    /**
     * 이 시간대의 시작 시(포함)를 반환한다.
     *
     * @return 구간 시작 시각 (0 이상)
     */
    public int from() {
        return from;
    }

    /**
     * 이 시간대의 종료 시(미포함)를 반환한다.
     *
     * @return 구간 종료 시각 (시작 시보다 큼)
     */
    public int to() {
        return to;
    }

    /**
     * 이 시간대의 대표 이모지를 반환한다.
     *
     * @return 시간대 이모지 문자열 (예: "🌅", "☀️", "🌙")
     */
    public String emoji() {
        return emoji;
    }

    /**
     * 주어진 시(hour)에 해당하는 {@code TimeOfDay}를 반환한다.
     *
     * <p>6개의 구간이 {@code [0, 24)}을 빈틈 없이 분할하므로, 0~23 범위의 시는 항상 정확히 하나의 상수에 대응한다.
     *
     * @param hour 0 이상 24 미만의 시각
     * @return 해당 시각이 속하는 {@code TimeOfDay}
     * @throws IllegalArgumentException hour가 0 미만이거나 24 이상인 경우
     */
    public static TimeOfDay fromHour(final int hour) {
        if (hour < 0 || hour >= 24) {
            throw new IllegalArgumentException("hour must be in [0, 24): " + hour);
        }
        for (final TimeOfDay timeOfDay : values()) {
            if (timeOfDay.from <= hour && hour < timeOfDay.to) {
                return timeOfDay;
            }
        }
        throw new IllegalArgumentException("No TimeOfDay found for hour: " + hour);
    }
}
