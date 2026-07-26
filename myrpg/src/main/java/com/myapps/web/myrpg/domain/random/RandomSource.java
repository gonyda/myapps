package com.myapps.web.myrpg.domain.random;

/**
 * 게임 내 난수 생성 추상화 인터페이스.
 *
 * <p>결정론적 테스트를 위해 난수를 인터페이스로 분리한다.
 * 운영 환경에서는 {@code ThreadLocalRandomSource}를 사용하고,
 * 테스트에서는 고정 값을 주입하는 {@code FixedRandomSource}를 사용한다.
 */
public interface RandomSource {

    /**
     * [0.0, 1.0) 범위의 균등 분포 실수를 반환한다.
     *
     * @return 0.0 이상 1.0 미만의 실수
     */
    double nextDouble();

    /**
     * [0, boundExclusive) 범위의 균등 분포 정수를 반환한다.
     *
     * @param boundExclusive 상한값 (미포함, 양수여야 함)
     * @return 0 이상 boundExclusive 미만의 정수
     */
    int nextInt(int boundExclusive);

    /**
     * [minInclusive, maxInclusive] 범위의 균등 분포 정수를 반환한다.
     *
     * @param minInclusive 하한값 (포함)
     * @param maxInclusive 상한값 (포함)
     * @return minInclusive 이상 maxInclusive 이하의 정수
     */
    int nextIntInclusive(int minInclusive, int maxInclusive);

    /**
     * [minInclusive, maxInclusive] 범위의 균등 분포 실수를 반환한다.
     *
     * <p>랜덤 편차(예: 0.9~1.1 배율) 등에 사용한다.
     *
     * @param minInclusive 하한값 (포함)
     * @param maxInclusive 상한값 (포함)
     * @return minInclusive 이상 maxInclusive 이하의 실수
     */
    double nextDoubleInRange(double minInclusive, double maxInclusive);
}
