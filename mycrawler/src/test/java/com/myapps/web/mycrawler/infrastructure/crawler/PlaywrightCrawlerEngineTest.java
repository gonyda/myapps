package com.myapps.web.mycrawler.infrastructure.crawler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.ViewportSize;
import com.myapps.web.mycrawler.domain.model.CrawlResult;
import com.myapps.web.mycrawler.domain.model.CrawlStatus;
import com.myapps.web.mycrawler.domain.model.CrawlTarget;
import com.myapps.web.mycrawler.domain.model.TriggerSource;
import com.myapps.web.mycrawler.infrastructure.antidetect.AntiDetectionService;
import com.myapps.web.mycrawler.infrastructure.config.CrawlerConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PlaywrightCrawlerEngine 단위 테스트.
 *
 * <p>Playwright 클래스들을 Mock하여 크롤링 엔진의 성공/실패 시나리오 및 BrowserContext 정리 동작을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class PlaywrightCrawlerEngineTest {

    @Mock private AntiDetectionService antiDetectionService;

    @Mock private Browser browser;

    @Mock private BrowserContext browserContext;

    @Mock private Page page;

    private PlaywrightCrawlerEngine engine;

    @BeforeEach
    void setUp() {
        final CrawlerConfig crawlerConfig =
                new CrawlerConfig(
                        "0 0 */6 * * *",
                        30L,
                        null,
                        List.of(new CrawlerConfig.TargetConfig("test", "https://example.com")));
        engine = new PlaywrightCrawlerEngine(antiDetectionService, crawlerConfig);
    }

    @Test
    void should_returnSuccessResult_when_crawlSucceeds() {
        // given
        final CrawlTarget target = new CrawlTarget("test-site", "https://example.com");
        final String expectedContent = "<html><body>Hello World</body></html>";

        when(antiDetectionService.randomUserAgent()).thenReturn("Mozilla/5.0 Test");
        when(antiDetectionService.randomViewport()).thenReturn(new ViewportSize(1920, 1080));
        when(browser.newContext(any(Browser.NewContextOptions.class))).thenReturn(browserContext);
        doNothing().when(antiDetectionService).applyStealthSettings(browserContext);
        when(browserContext.newPage()).thenReturn(page);
        when(page.content()).thenReturn(expectedContent);

        // when
        final CrawlResult result = engine.crawlWithBrowser(target, TriggerSource.MANUAL, browser);

        // then
        assertThat(result.status()).isEqualTo(CrawlStatus.SUCCESS);
        assertThat(result.targetName()).isEqualTo("test-site");
        assertThat(result.targetUrl()).isEqualTo("https://example.com");
        assertThat(result.content()).isEqualTo(expectedContent);
        assertThat(result.errorMessage()).isNull();
        assertThat(result.triggerSource()).isEqualTo(TriggerSource.MANUAL);
        assertThat(result.startTime()).isNotNull();
        assertThat(result.endTime()).isNotNull();
        assertThat(result.endTime()).isAfterOrEqualTo(result.startTime());
    }

    @Test
    void should_returnFailureResult_when_timeoutOccurs() {
        // given
        final CrawlTarget target = new CrawlTarget("timeout-site", "https://slow.example.com");

        when(antiDetectionService.randomUserAgent()).thenReturn("Mozilla/5.0 Test");
        when(antiDetectionService.randomViewport()).thenReturn(new ViewportSize(1280, 720));
        when(browser.newContext(any(Browser.NewContextOptions.class))).thenReturn(browserContext);
        doNothing().when(antiDetectionService).applyStealthSettings(browserContext);
        when(browserContext.newPage()).thenReturn(page);
        when(page.navigate(anyString(), any(Page.NavigateOptions.class)))
                .thenThrow(new PlaywrightException("Timeout 30000ms exceeded"));

        // when
        final CrawlResult result =
                engine.crawlWithBrowser(target, TriggerSource.SCHEDULED, browser);

        // then
        assertThat(result.status()).isEqualTo(CrawlStatus.FAILURE);
        assertThat(result.targetName()).isEqualTo("timeout-site");
        assertThat(result.targetUrl()).isEqualTo("https://slow.example.com");
        assertThat(result.content()).isNull();
        assertThat(result.errorMessage()).contains("Timeout");
        assertThat(result.triggerSource()).isEqualTo(TriggerSource.SCHEDULED);
        assertThat(result.startTime()).isNotNull();
        assertThat(result.endTime()).isAfterOrEqualTo(result.startTime());
    }

    @Test
    void should_returnFailureResult_when_networkErrorOccurs() {
        // given
        final CrawlTarget target = new CrawlTarget("error-site", "https://nonexistent.example.com");

        when(antiDetectionService.randomUserAgent()).thenReturn("Mozilla/5.0 Test");
        when(antiDetectionService.randomViewport()).thenReturn(new ViewportSize(1440, 900));
        when(browser.newContext(any(Browser.NewContextOptions.class))).thenReturn(browserContext);
        doNothing().when(antiDetectionService).applyStealthSettings(browserContext);
        when(browserContext.newPage()).thenReturn(page);
        when(page.navigate(anyString(), any(Page.NavigateOptions.class)))
                .thenThrow(new PlaywrightException("net::ERR_NAME_NOT_RESOLVED"));

        // when
        final CrawlResult result = engine.crawlWithBrowser(target, TriggerSource.MANUAL, browser);

        // then
        assertThat(result.status()).isEqualTo(CrawlStatus.FAILURE);
        assertThat(result.errorMessage()).contains("net::ERR_NAME_NOT_RESOLVED");
    }

    @Test
    void should_closeBrowserContext_when_crawlSucceeds() {
        // given
        final CrawlTarget target = new CrawlTarget("test-site", "https://example.com");

        when(antiDetectionService.randomUserAgent()).thenReturn("Mozilla/5.0 Test");
        when(antiDetectionService.randomViewport()).thenReturn(new ViewportSize(1920, 1080));
        when(browser.newContext(any(Browser.NewContextOptions.class))).thenReturn(browserContext);
        doNothing().when(antiDetectionService).applyStealthSettings(browserContext);
        when(browserContext.newPage()).thenReturn(page);
        when(page.content()).thenReturn("<html></html>");

        // when
        engine.crawlWithBrowser(target, TriggerSource.MANUAL, browser);

        // then
        verify(browserContext).close();
    }

    @Test
    void should_closeBrowserContext_when_crawlFails() {
        // given
        final CrawlTarget target = new CrawlTarget("fail-site", "https://fail.example.com");

        when(antiDetectionService.randomUserAgent()).thenReturn("Mozilla/5.0 Test");
        when(antiDetectionService.randomViewport()).thenReturn(new ViewportSize(1280, 720));
        when(browser.newContext(any(Browser.NewContextOptions.class))).thenReturn(browserContext);
        doNothing().when(antiDetectionService).applyStealthSettings(browserContext);
        when(browserContext.newPage()).thenReturn(page);
        when(page.navigate(anyString(), any(Page.NavigateOptions.class)))
                .thenThrow(new PlaywrightException("Connection refused"));

        // when
        engine.crawlWithBrowser(target, TriggerSource.MANUAL, browser);

        // then
        verify(browserContext).close();
    }

    @Test
    void should_applyAntiDetectionSettings_when_crawling() {
        // given
        final CrawlTarget target = new CrawlTarget("stealth-site", "https://protected.example.com");

        when(antiDetectionService.randomUserAgent()).thenReturn("Mozilla/5.0 Stealth");
        when(antiDetectionService.randomViewport()).thenReturn(new ViewportSize(1600, 900));
        when(browser.newContext(any(Browser.NewContextOptions.class))).thenReturn(browserContext);
        doNothing().when(antiDetectionService).applyStealthSettings(browserContext);
        when(browserContext.newPage()).thenReturn(page);
        when(page.content()).thenReturn("<html>protected</html>");

        // when
        engine.crawlWithBrowser(target, TriggerSource.MANUAL, browser);

        // then
        verify(antiDetectionService).randomUserAgent();
        verify(antiDetectionService).randomViewport();
        verify(antiDetectionService).applyStealthSettings(browserContext);
        verify(antiDetectionService).simulateHumanBehavior(page);
    }

    @Test
    void should_notPropagateException_when_crawlFails() {
        // given
        final CrawlTarget target = new CrawlTarget("crash-site", "https://crash.example.com");

        when(antiDetectionService.randomUserAgent()).thenReturn("Mozilla/5.0 Test");
        when(antiDetectionService.randomViewport()).thenReturn(new ViewportSize(1280, 720));
        when(browser.newContext(any(Browser.NewContextOptions.class))).thenReturn(browserContext);
        doNothing().when(antiDetectionService).applyStealthSettings(browserContext);
        when(browserContext.newPage()).thenReturn(page);
        when(page.navigate(anyString(), any(Page.NavigateOptions.class)))
                .thenThrow(new RuntimeException("Browser crashed"));

        // when
        final CrawlResult result = engine.crawlWithBrowser(target, TriggerSource.MANUAL, browser);

        // then — no exception propagated, FAILURE result returned
        assertThat(result.status()).isEqualTo(CrawlStatus.FAILURE);
        assertThat(result.errorMessage()).isEqualTo("Browser crashed");
    }
}
