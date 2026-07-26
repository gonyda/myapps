package com.myapps.web.myrpg.domain.random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ThreadLocalRandomSource}의 범위 계약을 검증하는 단위 테스트.
 */
class ThreadLocalRandomSourceTest {

    private ThreadLocalRandomSource randomSource;

    @BeforeEach
    void setUp() {
        randomSource = new ThreadLocalRandomSource();
    }

    @Test
    void should_returnDoubleInRange_when_nextDoubleCalled() {
        for (int i = 0; i < 100; i++) {
            final double value = randomSource.nextDouble();
            assertThat(value).isGreaterThanOrEqualTo(0.0).isLessThan(1.0);
        }
    }

    @Test
    void should_returnIntInRange_when_nextIntCalled() {
        final int bound = 10;
        for (int i = 0; i < 100; i++) {
            final int value = randomSource.nextInt(bound);
            assertThat(value).isGreaterThanOrEqualTo(0).isLessThan(bound);
        }
    }

    @Test
    void should_returnIntInInclusiveRange_when_nextIntInclusiveCalled() {
        final int min = 3;
        final int max = 7;
        for (int i = 0; i < 100; i++) {
            final int value = randomSource.nextIntInclusive(min, max);
            assertThat(value).isGreaterThanOrEqualTo(min).isLessThanOrEqualTo(max);
        }
    }

    @Test
    void should_returnDoubleInInclusiveRange_when_nextDoubleInRangeCalled() {
        final double min = 0.9;
        final double max = 1.1;
        for (int i = 0; i < 100; i++) {
            final double value = randomSource.nextDoubleInRange(min, max);
            assertThat(value).isGreaterThanOrEqualTo(min).isLessThan(max);
        }
    }
}
