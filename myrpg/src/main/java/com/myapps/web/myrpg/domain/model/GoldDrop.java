package com.myapps.web.myrpg.domain.model;

/**
 * 몬스터 처치 시 드랍되는 골드 범위를 나타내는 불변 레코드.
 *
 * <p>모든 몬스터는 필수로 골드를 드랍하며, 실제 드랍 금액은 {@code [min, max]} 범위에서
 * 결정된다. 컴팩트 생성자가 {@code 0 ≤ min ≤ max} 제약을 검증한다.
 *
 * @param min 최소 드랍 골드 (0 이상)
 * @param max 최대 드랍 골드 (min 이상)
 */
public record GoldDrop(int min, int max) {

    /**
     * 골드 드랍 범위를 검증하는 컴팩트 생성자.
     *
     * @throws IllegalArgumentException {@code min < 0} 이거나 {@code min > max}인 경우
     */
    public GoldDrop {
        if (min < 0) {
            throw new IllegalArgumentException(
                    "GoldDrop min은 0 이상이어야 합니다: min=" + min);
        }
        if (min > max) {
            throw new IllegalArgumentException(
                    "GoldDrop min은 max 이하여야 합니다: min=" + min + ", max=" + max);
        }
    }
}
