package com.myapps.web.myrpg.domain.random;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

/**
 * {@link ThreadLocalRandom}에 위임하는 운영용 난수 생성 구현.
 *
 * <p>스레드별 독립 인스턴스를 사용하므로 동시성 문제가 없다.
 */
@Service
public class ThreadLocalRandomSource implements RandomSource {

    /**
     * {@inheritDoc}
     */
    @Override
    public double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int nextInt(final int boundExclusive) {
        return ThreadLocalRandom.current().nextInt(boundExclusive);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int nextIntInclusive(final int minInclusive, final int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double nextDoubleInRange(final double minInclusive, final double maxInclusive) {
        return ThreadLocalRandom.current().nextDouble(minInclusive, maxInclusive);
    }
}
