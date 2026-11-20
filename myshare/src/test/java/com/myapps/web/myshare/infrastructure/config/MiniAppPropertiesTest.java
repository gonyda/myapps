package com.myapps.web.myshare.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link MiniAppProperties} 기본값 및 PIN 마스킹 검증 테스트. */
class MiniAppPropertiesTest {

    @Test
    @DisplayName("누락되거나 유효하지 않은 필드는 안전한 기본값으로 대체되고 toString은 PIN을 마스킹한다")
    void should_applyDefaultsAndMaskPin_when_fieldsMissingOrInvalid() {
        // given
        final MiniAppProperties props = new MiniAppProperties("  secret-pin-9999  ", 0, null);

        // when
        final String rendered = props.toString();

        // then
        assertThat(props.pin()).isEqualTo("secret-pin-9999");
        assertThat(props.isPinConfigured()).isTrue();
        assertThat(props.maxPayloadBytes()).isEqualTo(1_048_576);
        assertThat(props.isGitHubSyncEnabled()).isTrue();
        assertThat(props.github().basePath()).isEqualTo("mini");
        assertThat(props.github().commitMessagePrefix()).isEqualTo("chore(mini): sync");
        assertThat(rendered).doesNotContain("secret-pin-9999");
        assertThat(rendered).contains("pinConfigured=true");
    }

    @Test
    @DisplayName("PIN이 null이거나 공백이면 isPinConfigured가 false를 반환한다")
    void should_returnUnconfigured_when_pinNullOrBlank() {
        // given
        final MiniAppProperties nullPin =
                new MiniAppProperties(null, 512, new MiniAppProperties.GitHub(false, " ", ""));
        final MiniAppProperties blankPin =
                new MiniAppProperties(
                        "   ", 512, new MiniAppProperties.GitHub(true, "apps", "sync"));

        // when & then
        assertThat(nullPin.isPinConfigured()).isFalse();
        assertThat(nullPin.isGitHubSyncEnabled()).isFalse();
        assertThat(nullPin.github().basePath()).isEqualTo("mini");
        assertThat(nullPin.github().commitMessagePrefix()).isEqualTo("chore(mini): sync");
        assertThat(blankPin.isPinConfigured()).isFalse();
        assertThat(blankPin.github().basePath()).isEqualTo("apps");
    }
}
