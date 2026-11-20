package com.myapps.web.myshare.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myapps.web.myshare.domain.exception.InvalidMiniAppPayloadException;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import tools.jackson.databind.ObjectMapper;

/** {@link MiniAppJsonValidator}의 유효 JSON 보존 및 비정상 입력 차단 불변식을 검증하는 테스트. */
class MiniAppJsonValidatorPropertyTest {

    private static final int TEST_MAX_BYTES = 256;

    private final MiniAppJsonValidator validator =
            new MiniAppJsonValidator(TEST_MAX_BYTES, new ObjectMapper());

    @Property(tries = 100)
    void should_preserveRawJson_when_validJsonWithinLimitProvided(
            @ForAll("validJsonPayloads") final String json) {
        // given & when
        final String result = validator.validate(json);

        // then
        assertThat(result).isEqualTo(json);
    }

    @Property(tries = 50)
    void should_rejectPayload_when_malformedJsonProvided(
            @ForAll("invalidJsonPayloads") final String invalidJson) {
        // given & when & then
        assertThatThrownBy(() -> validator.validate(invalidJson))
                .isInstanceOf(InvalidMiniAppPayloadException.class);
    }

    @Example
    void should_rejectBoundaryPayloads_when_nullBlankOversizedOrTrailingToken() {
        // given
        final String oversized = "{\"data\":\"" + "a".repeat(TEST_MAX_BYTES) + "\"}";

        // when & then
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(InvalidMiniAppPayloadException.class);
        assertThatThrownBy(() -> validator.validate(""))
                .isInstanceOf(InvalidMiniAppPayloadException.class);
        assertThatThrownBy(() -> validator.validate("   \n\t "))
                .isInstanceOf(InvalidMiniAppPayloadException.class);
        assertThatThrownBy(() -> validator.validate(oversized))
                .isInstanceOf(InvalidMiniAppPayloadException.class);
        assertThatThrownBy(() -> validator.validate("{\"ok\":true} trailing"))
                .isInstanceOf(InvalidMiniAppPayloadException.class);
    }

    @Provide
    Arbitrary<String> validJsonPayloads() {
        final Arbitrary<String> keys = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(12);
        final Arbitrary<Integer> numbers = Arbitraries.integers().between(-1000, 1000);
        return keys.flatMap(
                key -> numbers.map(num -> "{\"key\":\"" + key + "\",\"count\":" + num + "}"));
    }

    @Provide
    Arbitrary<String> invalidJsonPayloads() {
        return Arbitraries.of(
                "{",
                "{\"a\":",
                "[1, 2,",
                "{\"a\": 1,}",
                "undefined",
                "{invalid: true}",
                "{\"a\":1} {\"b\":2}");
    }
}
