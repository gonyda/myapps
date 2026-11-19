package com.myapps.web.myshare.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myshare.domain.model.SharePage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link InMemoryShareCache}의 TTL 만료, 재검증 연장 및 최대 용량 축출 정책 단위 테스트. */
class InMemoryShareCacheTest {

    @Test
    @DisplayName("TTL 이내 조회 시 캐시 적중하고 만료 시각 도달 시 미스를 반환한다")
    void should_hitWithinTtlAndMissAfterExpiry_when_clockAdvances() {
        // given
        final MutableClock clock = new MutableClock(Instant.parse("2026-11-19T10:00:00Z"));
        final InMemoryShareCache cache = new InMemoryShareCache(20, 10, clock);
        final SharePage page = new SharePage("daily-brief", "<h1>Hi</h1>", "sha-1");
        cache.put(page);

        // when
        clock.advanceSeconds(19);
        final Optional<SharePage> hitBeforeExpiry = cache.findFresh("daily-brief");
        clock.advanceSeconds(1);
        final Optional<SharePage> missAtExpiry = cache.findFresh("daily-brief");

        // then
        assertThat(hitBeforeExpiry).contains(page);
        assertThat(missAtExpiry).isEmpty();
        assertThat(cache.findEntry("daily-brief")).isPresent();
    }

    @Test
    @DisplayName("refreshTtl 호출 시 기존 캐시 본문의 만료 시각이 현재 기준으로 연장된다")
    void should_extendTtl_when_refreshTtlCalled() {
        // given
        final MutableClock clock = new MutableClock(Instant.parse("2026-11-19T10:00:00Z"));
        final InMemoryShareCache cache = new InMemoryShareCache(20, 10, clock);
        cache.put(new SharePage("report", "<h1>V1</h1>", "sha-v1"));
        clock.advanceSeconds(25);
        assertThat(cache.findFresh("report")).isEmpty();

        // when
        final Optional<SharePage> refreshed = cache.refreshTtl("report");
        clock.advanceSeconds(10);

        // then
        assertThat(refreshed).isPresent();
        assertThat(cache.findFresh("report")).isPresent();
    }

    @Test
    @DisplayName("최대 용량 초과 시 만료된 항목과 가장 오래된 항목을 축출하여 용량을 유지한다")
    void should_evictExpiredAndOldestEntries_when_capacityExceeded() {
        // given
        final MutableClock clock = new MutableClock(Instant.parse("2026-11-19T10:00:00Z"));
        final InMemoryShareCache cache = new InMemoryShareCache(20, 2, clock);
        cache.put(new SharePage("slug-1", "<p>1</p>", "sha-1"));
        clock.advanceSeconds(5);
        cache.put(new SharePage("slug-2", "<p>2</p>", "sha-2"));

        // when
        clock.advanceSeconds(5);
        cache.put(new SharePage("slug-3", "<p>3</p>", "sha-3"));

        // then
        assertThat(cache.size()).isEqualTo(2);
        assertThat(cache.findFresh("slug-1")).isEmpty();
        assertThat(cache.findFresh("slug-2")).isPresent();
        assertThat(cache.findFresh("slug-3")).isPresent();
    }

    @Test
    @DisplayName("TTL 또는 maxEntries가 0 이하이면 캐싱이 비활성화된다")
    void should_disableCaching_when_ttlOrMaxEntriesIsZero() {
        // given
        final MutableClock clock = new MutableClock(Instant.parse("2026-11-19T10:00:00Z"));
        final InMemoryShareCache zeroTtlCache = new InMemoryShareCache(0, 10, clock);
        final InMemoryShareCache zeroMaxCache = new InMemoryShareCache(20, 0, clock);
        final SharePage page = new SharePage("a", "<p>a</p>", "sha");

        // when
        zeroTtlCache.put(page);
        zeroMaxCache.put(page);

        // then
        assertThat(zeroTtlCache.findFresh("a")).isEmpty();
        assertThat(zeroTtlCache.refreshTtl("a")).isEmpty();
        assertThat(zeroMaxCache.findEntry("a")).isEmpty();
    }

    private static final class MutableClock extends Clock {

        private Instant current;

        private MutableClock(final Instant initial) {
            this.current = initial;
        }

        private void advanceSeconds(final long seconds) {
            this.current = this.current.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
