package com.myapps.web.mycalendar.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** GitHubSyncProperties 단위 테스트. */
class GitHubSyncPropertiesTest {

    @Test
    @DisplayName("기본값이 누락되거나 공백일 때 기본 저장소 설정으로 채워진다")
    void should_applyDefaults_when_fieldsAreNullOrBlank() {
        // given
        final GitHubSyncProperties properties =
                new GitHubSyncProperties(false, null, " ", "", null, " ", null);

        // when & then
        assertThat(properties.enabled()).isFalse();
        assertThat(properties.token()).isEmpty();
        assertThat(properties.owner()).isEqualTo("gonyda");
        assertThat(properties.repo()).isEqualTo("mike");
        assertThat(properties.branch()).isEqualTo("main");
        assertThat(properties.path()).isEqualTo("calendar/mycalendar.json");
        assertThat(properties.commitMessage()).isEqualTo("chore(calendar): sync mycalendar events");
        assertThat(properties.isActive()).isFalse();
    }

    @Test
    @DisplayName("enabled가 true이고 유효한 토큰이 있으면 isActive가 true를 반환하고 toString은 토큰을 마스킹한다")
    void should_beActiveAndMaskToken_when_enabledAndTokenProvided() {
        // given
        final String secretToken = "ghp_super_secret_token_12345";
        final GitHubSyncProperties properties =
                new GitHubSyncProperties(
                        true,
                        secretToken,
                        "gonyda",
                        "mike",
                        "main",
                        "calendar/mycalendar.json",
                        "msg");

        // when
        final boolean active = properties.isActive();
        final String rendered = properties.toString();

        // then
        assertThat(active).isTrue();
        assertThat(rendered).contains("tokenConfigured=true");
        assertThat(rendered).doesNotContain(secretToken);
    }

    @Test
    @DisplayName("enabled가 true여도 토큰이 공백이면 isActive가 false를 반환한다")
    void should_notBeActive_when_tokenIsBlank() {
        // given
        final GitHubSyncProperties properties =
                new GitHubSyncProperties(
                        true, "   ", "gonyda", "mike", "main", "calendar/mycalendar.json", "msg");

        // when
        final boolean active = properties.isActive();

        // then
        assertThat(active).isFalse();
    }
}
