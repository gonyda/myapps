package com.myapps.web.myrpg.domain.random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FixedRandomSource}의 고정 값 반환 및 순환 동작을 검증하는 단위 테스트.
 */
class FixedRandomSourceTest {

    @Test
    void should_returnFixedDoubles_when_nextDoubleCalled() {
        final FixedRandomSource source = new FixedRandomSource(0.1, 0.5, 0.9);

        assertThat(source.nextDouble()).isEqualTo(0.1);
        assertThat(source.nextDouble()).isEqualTo(0.5);
        assertThat(source.nextDouble()).isEqualTo(0.9);
    }

    @Test
    void should_cycleValues_when_exhausted() {
        final FixedRandomSource source = new FixedRandomSource(0.2, 0.8);

        assertThat(source.nextDouble()).isEqualTo(0.2);
        assertThat(source.nextDouble()).isEqualTo(0.8);
        assertThat(source.nextDouble()).isEqualTo(0.2);
    }

    @Test
    void should_returnFixedInts_when_intArrayProvided() {
        final FixedRandomSource source = new FixedRandomSource(
                new double[]{0.5}, new int[]{3, 7}
        );

        assertThat(source.nextInt(10)).isEqualTo(3);
        assertThat(source.nextInt(10)).isEqualTo(7);
        assertThat(source.nextInt(10)).isEqualTo(3);
    }

    @Test
    void should_deriveIntFromDouble_when_noIntArrayProvided() {
        final FixedRandomSource source = new FixedRandomSource(0.5);

        final int result = source.nextInt(10);
        assertThat(result).isEqualTo(5);
    }

    @Test
    void should_computeIntInclusive_when_noIntArrayProvided() {
        final FixedRandomSource source = new FixedRandomSource(0.0);

        final int result = source.nextIntInclusive(3, 7);
        assertThat(result).isEqualTo(3);
    }

    @Test
    void should_returnFixedIntInclusive_when_intArrayProvided() {
        final FixedRandomSource source = new FixedRandomSource(
                new double[]{0.5}, new int[]{5}
        );

        final int result = source.nextIntInclusive(1, 10);
        assertThat(result).isEqualTo(5);
    }

    @Test
    void should_computeDoubleInRange_when_called() {
        final FixedRandomSource source = new FixedRandomSource(0.5);

        final double result = source.nextDoubleInRange(0.9, 1.1);
        assertThat(result).isEqualTo(1.0);
    }

    @Test
    void should_returnMinOfRange_when_doubleIsZero() {
        final FixedRandomSource source = new FixedRandomSource(0.0);

        final double result = source.nextDoubleInRange(0.9, 1.1);
        assertThat(result).isEqualTo(0.9);
    }
}
