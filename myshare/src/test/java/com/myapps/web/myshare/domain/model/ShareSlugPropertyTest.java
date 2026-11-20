package com.myapps.web.myshare.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myapps.web.myshare.domain.exception.InvalidShareSlugException;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link ShareSlug}의 허용 패턴 검증 및 경로 탐색 차단 불변식 테스트. */
class ShareSlugPropertyTest {

    @Property(tries = 100)
    void should_preserveValue_when_validSlugGenerated(@ForAll("validSlugs") final String rawSlug) {
        // given & when
        final ShareSlug slug = ShareSlug.of(rawSlug);

        // then
        assertThat(slug.value()).isEqualTo(rawSlug);
    }

    @Property(tries = 100)
    void should_rejectSlug_when_disallowedCharactersPresent(
            @ForAll("invalidSlugs") final String invalidSlug) {
        // given & when & then
        assertThatThrownBy(() -> ShareSlug.of(invalidSlug))
                .isInstanceOf(InvalidShareSlugException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                " ",
                "..",
                ".",
                "a/b",
                "a\\b",
                "../secret",
                "report.html",
                "hello%20world",
                "한글슬러그",
                "UpperCase",
                "My_App"
            })
    @DisplayName("경로 탐색 문자나 대문자·허용되지 않은 특수문자가 포함된 슬러그는 차단된다")
    void should_throwInvalidShareSlugException_when_malformedInput(final String malformed) {
        // given & when & then
        assertThatThrownBy(() -> ShareSlug.of(malformed))
                .isInstanceOf(InvalidShareSlugException.class);
    }

    @Test
    @DisplayName("최대 길이(200자)를 초과한 슬러그는 차단되고 200자 이하는 허용된다")
    void should_enforceMaxLength_when_slugLengthAtBoundary() {
        // given
        final String maxAllowed = "a".repeat(200);
        final String tooLong = "a".repeat(201);

        // when
        final ShareSlug accepted = ShareSlug.of(maxAllowed);

        // then
        assertThat(accepted.value()).hasSize(200);
        assertThatThrownBy(() -> ShareSlug.of(tooLong))
                .isInstanceOf(InvalidShareSlugException.class);
    }

    @Provide
    Arbitrary<String> validSlugs() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .withChars('-', '_')
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    @Provide
    Arbitrary<String> invalidSlugs() {
        final Arbitrary<String> prefix = validSlugs();
        final Arbitrary<String> badChar =
                Arbitraries.of(".", "..", "/", "\\", " ", "%", "?", "&", "#", "A", "Z");
        return prefix.flatMap(p -> badChar.map(bad -> p + bad));
    }
}
