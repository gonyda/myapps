package com.myapps.web.mycrawler.infrastructure.config;

import java.util.ArrayList;
import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.mycrawler.domain.model.CrawlTarget;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CrawlerConfig의 크롤 타겟 유효성 검증 로직을 Property-Based 테스트로 검증합니다.
 *
 * <p>jqwik을 사용하여 임의의 타겟 설정 목록을 생성하고,
 * 무효한 항목이 validTargets() 결과에서 항상 제외됨을 검증합니다.
 *
 * <p><b>Validates: Requirements 5.2, 5.5</b>
 */
class CrawlerConfigPropertyTest {

    private static final String VALID_CRON = "0 0 */6 * * *";
    private static final long VALID_TIMEOUT = 30L;
    private static final String VALID_BROWSERS_PATH = "";

    /**
     * 이름이 null 또는 blank인 타겟은 validTargets() 결과에서 항상 제외됨을 검증합니다.
     *
     * @param invalidName null 또는 blank 이름
     * @param validUrl    유효한 URL 문자열
     */
    @Property(tries = 100)
    void targetsWithNullOrBlankNameAreExcluded(
            @ForAll("nullOrBlankStrings") final String invalidName,
            @ForAll("validUrls") final String validUrl) {

        final List<CrawlerConfig.TargetConfig> targets = List.of(
                new CrawlerConfig.TargetConfig(invalidName, validUrl)
        );
        final CrawlerConfig config = new CrawlerConfig(VALID_CRON, VALID_TIMEOUT, VALID_BROWSERS_PATH, targets);

        final List<CrawlTarget> result = config.validTargets();

        assertThat(result).isEmpty();
    }

    /**
     * URL이 null 또는 blank인 타겟은 validTargets() 결과에서 항상 제외됨을 검증합니다.
     *
     * @param validName  유효한 이름 문자열
     * @param invalidUrl null 또는 blank URL
     */
    @Property(tries = 100)
    void targetsWithNullOrBlankUrlAreExcluded(
            @ForAll("validNames") final String validName,
            @ForAll("nullOrBlankStrings") final String invalidUrl) {

        final List<CrawlerConfig.TargetConfig> targets = List.of(
                new CrawlerConfig.TargetConfig(validName, invalidUrl)
        );
        final CrawlerConfig config = new CrawlerConfig(VALID_CRON, VALID_TIMEOUT, VALID_BROWSERS_PATH, targets);

        final List<CrawlTarget> result = config.validTargets();

        assertThat(result).isEmpty();
    }

    /**
     * URL 형식이 유효하지 않은 타겟은 validTargets() 결과에서 항상 제외됨을 검증합니다.
     *
     * @param validName  유효한 이름 문자열
     * @param invalidUrl 무효한 URL 형식 문자열
     */
    @Property(tries = 100)
    void targetsWithInvalidUrlFormatAreExcluded(
            @ForAll("validNames") final String validName,
            @ForAll("invalidUrls") final String invalidUrl) {

        final List<CrawlerConfig.TargetConfig> targets = List.of(
                new CrawlerConfig.TargetConfig(validName, invalidUrl)
        );
        final CrawlerConfig config = new CrawlerConfig(VALID_CRON, VALID_TIMEOUT, VALID_BROWSERS_PATH, targets);

        final List<CrawlTarget> result = config.validTargets();

        assertThat(result).isEmpty();
    }

    /**
     * 이름이 중복된 타겟은 첫 번째 항목만 유지되고 나머지는 제외됨을 검증합니다.
     *
     * @param duplicateName 중복될 이름 문자열
     */
    @Property(tries = 100)
    void duplicateNameTargetsAreExcluded(@ForAll("validNames") final String duplicateName) {
        final String firstUrl = "https://first.example.com";
        final String secondUrl = "https://second.example.com";

        final List<CrawlerConfig.TargetConfig> targets = List.of(
                new CrawlerConfig.TargetConfig(duplicateName, firstUrl),
                new CrawlerConfig.TargetConfig(duplicateName, secondUrl)
        );
        final CrawlerConfig config = new CrawlerConfig(VALID_CRON, VALID_TIMEOUT, VALID_BROWSERS_PATH, targets);

        final List<CrawlTarget> result = config.validTargets();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo(duplicateName);
        assertThat(result.get(0).url()).isEqualTo(firstUrl);
    }

    /**
     * null 또는 blank 문자열(공백, 탭, 개행 포함)을 생성하는 제공자.
     *
     * @return null 또는 blank 문자열 Arbitrary
     */
    @Provide
    Arbitrary<String> nullOrBlankStrings() {
        return Arbitraries.of(
                null,
                "",
                " ",
                "  ",
                "\t",
                "\n",
                "   \t  "
        );
    }

    /**
     * 유효한 이름 문자열을 생성하는 제공자.
     *
     * @return 유효한 이름 Arbitrary
     */
    @Provide
    Arbitrary<String> validNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(30);
    }

    /**
     * 유효한 URL 문자열을 생성하는 제공자.
     *
     * @return 유효한 URL Arbitrary
     */
    @Provide
    Arbitrary<String> validUrls() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(20)
                .map(s -> "https://" + s.toLowerCase() + ".com");
    }

    /**
     * 유효하지 않은 URL 형식 문자열을 생성하는 제공자.
     *
     * <p>스킴이 없거나, 호스트가 없거나, 구문적으로 무효한 문자열을 생성합니다.
     *
     * @return 무효한 URL Arbitrary
     */
    @Provide
    Arbitrary<String> invalidUrls() {
        final Arbitrary<String> plainText = Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(20);
        final Arbitrary<String> noHost = Arbitraries.of(
                "http://",
                "https://",
                "://missing-scheme"
        );
        final Arbitrary<String> malformed = Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(15)
                .map(s -> "not-a-url-" + s);

        return Arbitraries.oneOf(plainText, noHost, malformed);
    }
}
