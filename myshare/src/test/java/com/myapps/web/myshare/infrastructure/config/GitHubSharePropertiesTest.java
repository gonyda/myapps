package com.myapps.web.myshare.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link GitHubShareProperties} 및 {@link ShareCacheProperties} 기본값과 토큰 마스킹 검증 테스트. */
class GitHubSharePropertiesTest {

    @Test
    @DisplayName("누락된 프로퍼티 필드는 기본값으로 보완되고 toString에서 토큰이 마스킹된다")
    void should_applyDefaultsAndMaskToken_when_fieldsMissing() {
        // given
        final GitHubShareProperties props =
                new GitHubShareProperties(true, "secret-pat-token", null, " ", "", null);

        // when
        final String summary = props.toString();

        // then
        assertThat(props.isActive()).isTrue();
        assertThat(props.owner()).isEqualTo("gonyda");
        assertThat(props.repo()).isEqualTo("mike");
        assertThat(props.branch()).isEqualTo("main");
        assertThat(props.basePath()).isEqualTo("share");
        assertThat(summary).doesNotContain("secret-pat-token");
        assertThat(summary).contains("tokenConfigured=true");
    }

    @Test
    @DisplayName("비활성화 상태이거나 토큰이 비어 있으면 isActive가 false를 반환한다")
    void should_returnInactive_when_disabledOrBlankToken() {
        // given
        final GitHubShareProperties disabled =
                new GitHubShareProperties(false, "tok", "o", "r", "b", "p");
        final GitHubShareProperties blankToken =
                new GitHubShareProperties(true, "  ", "o", "r", "b", "p");
        final GitHubShareProperties nullToken =
                new GitHubShareProperties(true, null, "o", "r", "b", "p");

        // when & then
        assertThat(disabled.isActive()).isFalse();
        assertThat(blankToken.isActive()).isFalse();
        assertThat(nullToken.isActive()).isFalse();
        assertThat(nullToken.toString()).contains("tokenConfigured=false");
    }

    @Test
    @DisplayName("캐시 프로퍼티에 음수가 전달되면 안전한 기본값으로 대체된다")
    void should_useDefaults_when_cachePropertiesNegative() {
        // given & when
        final ShareCacheProperties props = new ShareCacheProperties(-1, -5);

        // then
        assertThat(props.ttlSeconds()).isEqualTo(20L);
        assertThat(props.maxEntries()).isEqualTo(200);
    }
}
