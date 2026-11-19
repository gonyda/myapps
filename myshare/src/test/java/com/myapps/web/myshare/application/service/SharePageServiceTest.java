package com.myapps.web.myshare.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.myapps.web.myshare.application.dto.ShareFetchResult;
import com.myapps.web.myshare.application.exception.GitHubShareException;
import com.myapps.web.myshare.application.exception.ShareNotFoundException;
import com.myapps.web.myshare.application.port.GitHubShareClient;
import com.myapps.web.myshare.domain.exception.InvalidShareSlugException;
import com.myapps.web.myshare.domain.model.SharePage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link SharePageService}의 캐시 히트, 만료 후 ETag 재검증, 미존재(404) 및 슬러그 검증 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class SharePageServiceTest {

    @Mock private GitHubShareClient gitHubShareClient;

    private MutableClock clock;
    private InMemoryShareCache cache;
    private SharePageService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-19T10:00:00Z"));
        cache = new InMemoryShareCache(20, 100, clock);
        service = new SharePageService(gitHubShareClient, cache);
    }

    @Test
    @DisplayName("최초 조회 시 GitHub에서 HTML을 가져와 캐시하고 TTL 내 재요청은 캐시에서 반환한다")
    void should_fetchOnceAndServeFromCacheWithinTtl_when_validSlugRequestedTwice() {
        // given
        given(gitHubShareClient.fetchHtml("mike-note", null))
                .willReturn(new ShareFetchResult.Found("<h1>안녕</h1>", "sha-1"));

        // when
        final SharePage first = service.loadPage("mike-note");
        clock.advanceSeconds(10);
        final SharePage second = service.loadPage("mike-note");

        // then
        assertThat(first.html()).isEqualTo("<h1>안녕</h1>");
        assertThat(second).isEqualTo(first);
        then(gitHubShareClient).should(times(1)).fetchHtml("mike-note", null);
    }

    @Test
    @DisplayName("TTL 만료 후 GitHub이 304 Not Modified를 반환하면 기존 캐시 본문을 유지하고 TTL을 연장한다")
    void should_reuseCachedHtmlAndExtendTtl_when_gitHubReturnsNotModified() {
        // given
        given(gitHubShareClient.fetchHtml("mike-note", null))
                .willReturn(new ShareFetchResult.Found("<h1>원본</h1>", "sha-1"));
        given(gitHubShareClient.fetchHtml("mike-note", "sha-1"))
                .willReturn(new ShareFetchResult.NotModified());

        service.loadPage("mike-note");
        clock.advanceSeconds(25);

        // when
        final SharePage refreshed = service.loadPage("mike-note");
        clock.advanceSeconds(10);
        final SharePage cachedAgain = service.loadPage("mike-note");

        // then
        assertThat(refreshed.html()).isEqualTo("<h1>원본</h1>");
        assertThat(cachedAgain.sha()).isEqualTo("sha-1");
        then(gitHubShareClient).should(times(1)).fetchHtml("mike-note", "sha-1");
    }

    @Test
    @DisplayName("GitHub이 NotFound를 반환하면 기존 캐시를 비우고 ShareNotFoundException을 던진다")
    void should_evictCacheAndThrowNotFound_when_gitHubReturnsNotFound() {
        // given
        cache.put(new SharePage("deleted-note", "<h1>old</h1>", "sha-old"));
        clock.advanceSeconds(25);
        given(gitHubShareClient.fetchHtml("deleted-note", "sha-old"))
                .willReturn(new ShareFetchResult.NotFound());

        // when & then
        assertThatThrownBy(() -> service.loadPage("deleted-note"))
                .isInstanceOf(ShareNotFoundException.class);
        assertThat(cache.findEntry("deleted-note")).isEmpty();
    }

    @Test
    @DisplayName("만료 엔트리가 304 처리 중 동시 축출된 경우 ETag 없이 재조회해 복구한다")
    void should_refetchWithoutEtag_when_cacheEvictedDuringNotModified() {
        // given
        final InMemoryShareCache disabledCache = new InMemoryShareCache(0, 0, clock);
        final SharePageService noCacheService =
                new SharePageService(gitHubShareClient, disabledCache);
        given(gitHubShareClient.fetchHtml("race", null))
                .willReturn(new ShareFetchResult.NotModified())
                .willReturn(new ShareFetchResult.Found("<p>recovered</p>", "sha-new"));

        // when
        final SharePage page = noCacheService.loadPage("race");

        // then
        assertThat(page.html()).isEqualTo("<p>recovered</p>");
        then(gitHubShareClient).should(times(2)).fetchHtml("race", null);
    }

    @Test
    @DisplayName("ETag 없이 재조회 시에도 304가 반환되면 GitHubShareException을 던진다")
    void should_throwGitHubShareException_when_refetchWithoutEtagReturnsNotModified() {
        // given
        final InMemoryShareCache disabledCache = new InMemoryShareCache(0, 0, clock);
        final SharePageService noCacheService =
                new SharePageService(gitHubShareClient, disabledCache);
        given(gitHubShareClient.fetchHtml("broken", null))
                .willReturn(new ShareFetchResult.NotModified());

        // when & then
        assertThatThrownBy(() -> noCacheService.loadPage("broken"))
                .isInstanceOf(GitHubShareException.class);
    }

    @Test
    @DisplayName("허용되지 않은 슬러그 요청은 외부 GitHub 호출 전에 즉시 차단된다")
    void should_rejectBeforeCallingGitHub_when_slugInvalid() {
        // given & when & then
        assertThatThrownBy(() -> service.loadPage("../secret"))
                .isInstanceOf(InvalidShareSlugException.class);
        then(gitHubShareClient).should(never()).fetchHtml(any(), any());
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
