package com.myapps.web.myshare.application.service;

import com.myapps.web.myshare.domain.model.SharePage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 짧은 TTL과 최대 엔트리 수 제한을 갖춘 스레드 안전 인메모리 HTML 캐시.
 *
 * <p>GitHub API 호출 부하를 줄이면서 HTML 수정 시 TTL 내에 빠르게 반영되도록 설계되었습니다.
 */
public class InMemoryShareCache {

    private final ConcurrentHashMap<String, CacheEntry> entries;
    private final Duration ttl;
    private final int maxEntries;
    private final Clock clock;

    /**
     * 캐시 인스턴스를 생성합니다.
     *
     * @param ttlSeconds 캐시 유효 시간(초), 0 이하이면 캐싱 비활성화
     * @param maxEntries 최대 저장 슬러그 수, 0 이하이면 캐싱 비활성화
     * @param clock 현재 시각 조회용 시계
     */
    public InMemoryShareCache(final long ttlSeconds, final int maxEntries, final Clock clock) {
        this.entries = new ConcurrentHashMap<>();
        this.ttl = ttlSeconds > 0 ? Duration.ofSeconds(ttlSeconds) : Duration.ZERO;
        this.maxEntries = Math.max(0, maxEntries);
        this.clock = clock;
    }

    /**
     * 유효 기간(TTL)이 지나지 않은 캐시 페이지를 조회합니다.
     *
     * @param slug 검증된 공유 슬러그
     * @return 유효한 캐시 페이지(미스 또는 만료 시 빈 {@link Optional})
     */
    public Optional<SharePage> findFresh(final String slug) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        final CacheEntry entry = entries.get(slug);
        if (entry == null) {
            return Optional.empty();
        }
        if (clock.instant().isBefore(entry.expiresAt())) {
            return Optional.of(entry.page());
        }
        return Optional.empty();
    }

    /**
     * 만료 여부를 불문하고 기존 캐시 엔트리를 조회합니다({@code If-None-Match} 조건부 요청용).
     *
     * @param slug 검증된 공유 슬러그
     * @return 기존 엔트리(없으면 빈 {@link Optional})
     */
    public Optional<CacheEntry> findEntry(final String slug) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        return Optional.ofNullable(entries.get(slug));
    }

    /**
     * 페이지를 캐시에 저장하고 최대 엔트리 수를 초과하지 않도록 오래된 항목을 축출합니다.
     *
     * @param page 저장할 공유 페이지
     */
    public void put(final SharePage page) {
        if (!isEnabled()) {
            return;
        }
        final Instant now = clock.instant();
        synchronized (entries) {
            if (!entries.containsKey(page.slug()) && entries.size() >= maxEntries) {
                evictToFitCapacity(now);
            }
            entries.put(page.slug(), new CacheEntry(page, now, now.plus(ttl)));
        }
    }

    /**
     * 기존 캐시 엔트리의 TTL을 현재 시각 기준으로 연장합니다.
     *
     * @param slug 검증된 공유 슬러그
     * @return 연장된 공유 페이지(이미 축출된 경우 빈 {@link Optional})
     */
    public Optional<SharePage> refreshTtl(final String slug) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        final Instant now = clock.instant();
        final CacheEntry updated =
                entries.computeIfPresent(
                        slug,
                        (key, existing) -> new CacheEntry(existing.page(), now, now.plus(ttl)));
        return Optional.ofNullable(updated).map(CacheEntry::page);
    }

    /**
     * 지정된 슬러그의 캐시 엔트리를 제거합니다.
     *
     * @param slug 제거할 슬러그
     */
    public void remove(final String slug) {
        entries.remove(slug);
    }

    /**
     * 현재 캐시에 보관 중인 엔트리 수를 반환합니다.
     *
     * @return 캐시 엔트리 개수
     */
    public int size() {
        return entries.size();
    }

    private boolean isEnabled() {
        return !ttl.isZero() && maxEntries > 0;
    }

    private void evictToFitCapacity(final Instant now) {
        entries.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
        while (entries.size() >= maxEntries) {
            final Optional<String> oldestKey =
                    entries.entrySet().stream()
                            .min(Comparator.comparing(entry -> entry.getValue().fetchedAt()))
                            .map(Map.Entry::getKey);
            if (oldestKey.isEmpty()) {
                break;
            }
            entries.remove(oldestKey.get());
        }
    }

    /**
     * 캐시에 저장되는 페이지 엔트리.
     *
     * @param page 공유 페이지 본문 및 SHA
     * @param fetchedAt 조회 시각
     * @param expiresAt 만료 시각
     */
    public record CacheEntry(SharePage page, Instant fetchedAt, Instant expiresAt) {}
}
