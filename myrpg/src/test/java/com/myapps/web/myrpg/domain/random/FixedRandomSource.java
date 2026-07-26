package com.myapps.web.myrpg.domain.random;

import java.util.Arrays;

/**
 * 결정론적 테스트를 위한 고정 값 난수 소스.
 *
 * <p>미리 설정된 값 목록을 순서대로 반환하며, 끝에 도달하면 처음부터 순환한다.
 * 테스트에서 특정 경로(치명타 발생/미발생, 특정 등급 롤 등)를 재현할 때 사용한다.
 */
public class FixedRandomSource implements RandomSource {

    private final double[] doubleValues;
    private final int[] intValues;
    private int doubleIndex;
    private int intIndex;

    /**
     * double 값과 int 값을 별도로 지정하는 생성자.
     *
     * @param doubleValues nextDouble, nextDoubleInRange 호출 시 순서대로 반환할 값 배열
     * @param intValues    nextInt, nextIntInclusive 호출 시 순서대로 반환할 값 배열
     */
    public FixedRandomSource(final double[] doubleValues, final int[] intValues) {
        this.doubleValues = Arrays.copyOf(doubleValues, doubleValues.length);
        this.intValues = Arrays.copyOf(intValues, intValues.length);
        this.doubleIndex = 0;
        this.intIndex = 0;
    }

    /**
     * 모든 메서드가 동일한 double 값을 기반으로 동작하는 간편 생성자.
     *
     * <p>int 메서드 호출 시에는 double 값을 정수 변환하여 반환한다.
     *
     * @param doubleValues nextDouble 호출 시 순서대로 반환할 값 배열
     */
    public FixedRandomSource(final double... doubleValues) {
        this.doubleValues = Arrays.copyOf(doubleValues, doubleValues.length);
        this.intValues = new int[0];
        this.doubleIndex = 0;
        this.intIndex = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double nextDouble() {
        final double value = doubleValues[doubleIndex % doubleValues.length];
        doubleIndex++;
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int nextInt(final int boundExclusive) {
        if (intValues.length > 0) {
            final int value = intValues[intIndex % intValues.length];
            intIndex++;
            return value;
        }
        final double ratio = nextDouble();
        return (int) (ratio * boundExclusive);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int nextIntInclusive(final int minInclusive, final int maxInclusive) {
        if (intValues.length > 0) {
            final int value = intValues[intIndex % intValues.length];
            intIndex++;
            return value;
        }
        final double ratio = nextDouble();
        return minInclusive + (int) (ratio * (maxInclusive - minInclusive + 1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double nextDoubleInRange(final double minInclusive, final double maxInclusive) {
        final double ratio = nextDouble();
        return minInclusive + ratio * (maxInclusive - minInclusive);
    }
}
