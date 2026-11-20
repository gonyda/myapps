package com.myapps.web.myshare.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myapps.web.myshare.domain.exception.InvalidMiniAppKeyException;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/** {@link MiniAppKey}의 정규식 검증 불변식을 검증하는 jqwik 프로퍼티 테스트. */
class MiniAppKeyPropertyTest {

    private static final int MAX_KEY_LENGTH = 64;

    @Property(tries = 100)
    void should_preserveValue_when_validKeyGenerated(@ForAll("validKeys") final String validKey) {
        // given & when
        final MiniAppKey appKey = MiniAppKey.of(validKey);

        // then
        assertThat(appKey.value()).isEqualTo(validKey);
    }

    @Property(tries = 100)
    void should_rejectKey_when_disallowedCharactersOrLengthPresent(
            @ForAll("invalidKeys") final String invalidKey) {
        // given & when & then
        assertThatThrownBy(() -> MiniAppKey.of(invalidKey))
                .isInstanceOf(InvalidMiniAppKeyException.class);
    }

    @Example
    void should_rejectBoundaryKeys_when_nullEmptyUppercaseOrTooLong() {
        // given
        final String exactly64 = "a".repeat(MAX_KEY_LENGTH);
        final String tooLong65 = "a".repeat(MAX_KEY_LENGTH + 1);

        // when & then
        assertThat(MiniAppKey.of(exactly64).value()).isEqualTo(exactly64);
        assertThatThrownBy(() -> MiniAppKey.of(null))
                .isInstanceOf(InvalidMiniAppKeyException.class);
        assertThatThrownBy(() -> MiniAppKey.of("")).isInstanceOf(InvalidMiniAppKeyException.class);
        assertThatThrownBy(() -> MiniAppKey.of(" ")).isInstanceOf(InvalidMiniAppKeyException.class);
        assertThatThrownBy(() -> MiniAppKey.of("MyApp"))
                .isInstanceOf(InvalidMiniAppKeyException.class);
        assertThatThrownBy(() -> MiniAppKey.of("app.json"))
                .isInstanceOf(InvalidMiniAppKeyException.class);
        assertThatThrownBy(() -> MiniAppKey.of("../secret"))
                .isInstanceOf(InvalidMiniAppKeyException.class);
        assertThatThrownBy(() -> MiniAppKey.of(tooLong65))
                .isInstanceOf(InvalidMiniAppKeyException.class);
    }

    @Provide
    Arbitrary<String> validKeys() {
        return Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyz0123456789_-")
                .ofMinLength(1)
                .ofMaxLength(MAX_KEY_LENGTH);
    }

    @Provide
    Arbitrary<String> invalidKeys() {
        final Arbitrary<String> withDisallowedChar =
                Arbitraries.strings()
                        .withChars("ABCDEFGHIJKLMNOPQRSTUVWXYZ./\\ %!@#")
                        .ofMinLength(1)
                        .ofMaxLength(16)
                        .map(bad -> "app" + bad);
        final Arbitrary<String> tooLong =
                Arbitraries.strings()
                        .withChars("abcdefghijklmnopqrstuvwxyz0123456789")
                        .ofMinLength(MAX_KEY_LENGTH + 1)
                        .ofMaxLength(80);
        return Arbitraries.oneOf(withDisallowedChar, tooLong);
    }
}
