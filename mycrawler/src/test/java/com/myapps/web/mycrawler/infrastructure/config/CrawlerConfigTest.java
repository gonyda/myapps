package com.myapps.web.mycrawler.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.myapps.web.mycrawler.domain.model.CrawlTarget;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@link CrawlerConfig}의 타겟 유효성 검증 로직을 테스트합니다. */
class CrawlerConfigTest {

    private static final String VALID_CRON = "0 0 */6 * * *";
    private static final long VALID_TIMEOUT = 30L;
    private static final String VALID_BROWSERS_PATH = "";

    @Test
    void should_returnValidTargets_when_allTargetsAreValid() {
        final List<CrawlerConfig.TargetConfig> targets =
                List.of(
                        new CrawlerConfig.TargetConfig("site-a", "https://example.com"),
                        new CrawlerConfig.TargetConfig("site-b", "https://example.org"));
        final CrawlerConfig config =
                new CrawlerConfig(VALID_CRON, VALID_TIMEOUT, VALID_BROWSERS_PATH, targets);

        final List<CrawlTarget> result = config.validTargets();

        assertEquals(2, result.size());
        assertEquals("site-a", result.get(0).name());
        assertEquals("https://example.com", result.get(0).url());
        assertEquals("site-b", result.get(1).name());
        assertEquals("https://example.org", result.get(1).url());
    }

    @Test
    void should_filterTarget_when_nameIsNull() {
        final List<CrawlerConfig.TargetConfig> targets =
                List.of(
                        new CrawlerConfig.TargetConfig(null, "https://example.com"),
                        new CrawlerConfig.TargetConfig("valid", "https://example.org"));
        final CrawlerConfig config =
                new CrawlerConfig(VALID_CRON, VALID_TIMEOUT, VALID_BROWSERS_PATH, targets);

        final List<CrawlTarget> result = config.validTargets();

        assertEquals(1, result.size());
        assertEquals("valid", result.get(0).name());
    }

    @Test
    void should_filterTarget_when_nameIsBlank() {
        final List<CrawlerConfig.TargetConfig> targets =
                List.of(
                        new CrawlerConfig.TargetConfig("  ", "https://example.com"),
                        new CrawlerConfig.TargetConfig("valid", "https://example.org"));
        final CrawlerConfig config =
                new CrawlerConfig(VALID_CRON, VALID_TIMEOUT, VALID_BROWSERS_PATH, targets);

        final List<CrawlTarget> result = config.validTargets();

        assertEquals(1, result.size());
        assertEquals("valid", result.get(0).name());
    }

    @Test
    void should_filterTarget_when_urlIsNull() {
        final List<CrawlerConfig.TargetConfig> targets =
                List.of(
                        new CrawlerConfig.TargetConfig("site-a", null),
                        new CrawlerConfig.TargetConfig("site-b", "https://example.org"));
        final CrawlerConfig config =
                new CrawlerConfig(VALID_CRON, VALID_TIMEOUT, VALID_BROWSERS_PATH, targets);

        final List<CrawlTarget> result = config.validTargets();

        assertEquals(1, result.size());
        assertEquals("site-b", result.get(0).name());
    }

    @Test
    void should_filterTarget_when_urlIsBlank() {
        final List<CrawlerConfig.TargetConfig> targets =
                List.of(
                        new CrawlerConfig.TargetConfig("site-a", "   "),
                        new CrawlerConfig.TargetConfig("site-b", "https://example.org"));
        final CrawlerConfig config =
                new CrawlerConfig(VALID_CRON, VALID_TIMEOUT, VALID_BROWSERS_PATH, targets);

        final List<CrawlTarget> result = config.validTargets();

        assertEquals(1, result.size());
        assertEquals("site-b", result.get(0).name());
    }

    @Test
    void should_filterTarget_when_urlFormatIsInvalid() {
        final List<CrawlerConfig.TargetConfig> targets =
                List.of(
                        new CrawlerConfig.TargetConfig("site-a", "not-a-valid-url"),
                        new CrawlerConfig.TargetConfig("site-b", "https://example.org"));
        final CrawlerConfig config =
                new CrawlerConfig(VALID_CRON, VALID_TIMEOUT, VALID_BROWSERS_PATH, targets);

        final List<CrawlTarget> result = config.validTargets();

        assertEquals(1, result.size());
        assertEquals("site-b", result.get(0).name());
    }

    @Test
    void should_filterDuplicateNames_keepingFirstOccurrence() {
        final List<CrawlerConfig.TargetConfig> targets =
                List.of(
                        new CrawlerConfig.TargetConfig("site-a", "https://first.example.com"),
                        new CrawlerConfig.TargetConfig("site-a", "https://second.example.com"),
                        new CrawlerConfig.TargetConfig("site-b", "https://example.org"));
        final CrawlerConfig config =
                new CrawlerConfig(VALID_CRON, VALID_TIMEOUT, VALID_BROWSERS_PATH, targets);

        final List<CrawlTarget> result = config.validTargets();

        assertEquals(2, result.size());
        assertEquals("site-a", result.get(0).name());
        assertEquals("https://first.example.com", result.get(0).url());
        assertEquals("site-b", result.get(1).name());
    }

    @Test
    void should_returnEmptyList_when_targetsIsNull() {
        final CrawlerConfig config =
                new CrawlerConfig(VALID_CRON, VALID_TIMEOUT, VALID_BROWSERS_PATH, null);

        final List<CrawlTarget> result = config.validTargets();

        assertTrue(result.isEmpty());
    }

    @Test
    void should_returnEmptyList_when_targetsIsEmpty() {
        final CrawlerConfig config =
                new CrawlerConfig(VALID_CRON, VALID_TIMEOUT, VALID_BROWSERS_PATH, List.of());

        final List<CrawlTarget> result = config.validTargets();

        assertTrue(result.isEmpty());
    }

    @Test
    void should_returnEmptyList_when_allTargetsAreInvalid() {
        final List<CrawlerConfig.TargetConfig> targets =
                List.of(
                        new CrawlerConfig.TargetConfig(null, "https://example.com"),
                        new CrawlerConfig.TargetConfig("site-a", "invalid-url"),
                        new CrawlerConfig.TargetConfig("", "https://example.org"));
        final CrawlerConfig config =
                new CrawlerConfig(VALID_CRON, VALID_TIMEOUT, VALID_BROWSERS_PATH, targets);

        final List<CrawlTarget> result = config.validTargets();

        assertTrue(result.isEmpty());
    }
}
