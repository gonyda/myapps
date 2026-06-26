package com.myapps.web.mycrawler.infrastructure.crawler;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ViewportSize;
import com.myapps.web.mycrawler.domain.model.CrawlResult;
import com.myapps.web.mycrawler.domain.model.CrawlStatus;
import com.myapps.web.mycrawler.domain.model.CrawlTarget;
import com.myapps.web.mycrawler.domain.model.TriggerSource;
import com.myapps.web.mycrawler.infrastructure.antidetect.AntiDetectionService;
import com.myapps.web.mycrawler.infrastructure.config.CrawlerConfig;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Playwright Java를 사용한 크롤링 실행 엔진 구현체.
 *
 * <p>애플리케이션 시작 시 Playwright 인스턴스를 초기화하고 headless Chromium 브라우저를
 * 싱글톤으로 유지합니다. 요청마다 새로운 BrowserContext를 생성하여 격리된 세션으로 크롤링을 수행합니다.
 */
@Component
public class PlaywrightCrawlerEngine implements CrawlerEngine {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightCrawlerEngine.class);

    private static final double PAGE_LOAD_TIMEOUT_MS = 30_000.0;
    private static final int CONTENT_SUMMARY_LENGTH = 200;

    private final AntiDetectionService antiDetectionService;
    private final CrawlerConfig crawlerConfig;

    private Playwright playwright;
    private Browser browser;

    /**
     * PlaywrightCrawlerEngine 인스턴스를 생성합니다.
     *
     * @param antiDetectionService 안티 디텍션 설정 및 행동 시뮬레이션 서비스
     * @param crawlerConfig        크롤러 설정 정보
     */
    public PlaywrightCrawlerEngine(final AntiDetectionService antiDetectionService,
                                   final CrawlerConfig crawlerConfig) {
        this.antiDetectionService = antiDetectionService;
        this.crawlerConfig = crawlerConfig;
    }

    /**
     * 애플리케이션 시작 시 Playwright 인스턴스와 headless Chromium 브라우저를 초기화합니다.
     *
     * <p>CrawlerConfig의 browsersPath가 설정되어 있으면 환경변수 PLAYWRIGHT_BROWSERS_PATH를
     * 해당 값으로 설정하여 환경별 브라우저 바이너리 경로를 해소합니다.
     */
    @PostConstruct
    public void initialize() {
        configureBrowsersPath();
        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions().setHeadless(true)
        );
        log.info("Playwright Chromium 브라우저가 초기화되었습니다.");
    }

    /**
     * 애플리케이션 종료 시 브라우저와 Playwright 리소스를 정리합니다.
     */
    @PreDestroy
    public void shutdown() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        log.info("Playwright 리소스가 정리되었습니다.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>지정된 대상 URL에 대해 Playwright 크롤링을 수행합니다.
     * 성공 시 SUCCESS CrawlResult를, 실패 시 FAILURE CrawlResult를 반환합니다.
     * 어떠한 예외도 외부로 전파하지 않습니다.
     */
    @Override
    public CrawlResult crawl(final CrawlTarget target) {
        return crawlWithBrowser(target, this.browser);
    }

    /**
     * 지정된 브라우저 인스턴스를 사용하여 크롤링을 수행합니다.
     *
     * @param target  크롤링 대상
     * @param targetBrowser 사용할 브라우저 인스턴스
     * @return 크롤링 결과
     */
    CrawlResult crawlWithBrowser(final CrawlTarget target, final Browser targetBrowser) {
        final LocalDateTime startTime = LocalDateTime.now();
        BrowserContext context = null;

        try {
            context = createBrowserContext(targetBrowser);
            final String content = navigateAndExtract(context, target.url());
            final LocalDateTime endTime = LocalDateTime.now();
            final CrawlResult result = buildSuccessResult(target, content, startTime, endTime);
            logCrawlResult(result);
            return result;
        } catch (final Exception exception) {
            final LocalDateTime endTime = LocalDateTime.now();
            final CrawlResult result = buildFailureResult(target, exception, startTime, endTime);
            logCrawlResult(result);
            return result;
        } finally {
            closeBrowserContext(context);
        }
    }

    private void configureBrowsersPath() {
        final String browsersPath = crawlerConfig.browsersPath();
        if (browsersPath != null && !browsersPath.isBlank()) {
            System.setProperty("PLAYWRIGHT_BROWSERS_PATH", browsersPath);
            log.info("Playwright 브라우저 경로가 설정되었습니다: {}", browsersPath);
        }
    }

    private BrowserContext createBrowserContext(final Browser targetBrowser) {
        final String userAgent = antiDetectionService.randomUserAgent();
        final ViewportSize viewport = antiDetectionService.randomViewport();

        final BrowserContext context = targetBrowser.newContext(
            new Browser.NewContextOptions()
                .setUserAgent(userAgent)
                .setViewportSize(viewport.width, viewport.height)
        );

        antiDetectionService.applyStealthSettings(context);
        return context;
    }

    private String navigateAndExtract(final BrowserContext context, final String url) {
        final Page page = context.newPage();
        page.navigate(url, new Page.NavigateOptions().setTimeout(PAGE_LOAD_TIMEOUT_MS));
        antiDetectionService.simulateHumanBehavior(page);
        return page.content();
    }

    private CrawlResult buildSuccessResult(final CrawlTarget target,
                                           final String content,
                                           final LocalDateTime startTime,
                                           final LocalDateTime endTime) {
        return new CrawlResult(
            target.name(),
            target.url(),
            CrawlStatus.SUCCESS,
            TriggerSource.SCHEDULED,
            content,
            null,
            startTime,
            endTime
        );
    }

    private CrawlResult buildFailureResult(final CrawlTarget target,
                                           final Exception exception,
                                           final LocalDateTime startTime,
                                           final LocalDateTime endTime) {
        return new CrawlResult(
            target.name(),
            target.url(),
            CrawlStatus.FAILURE,
            TriggerSource.SCHEDULED,
            null,
            exception.getMessage(),
            startTime,
            endTime
        );
    }

    private void logCrawlResult(final CrawlResult result) {
        log.info("크롤링 완료 - 상태: {}, URL: {}, 응답 요약: {}, 소요 시간: {}ms",
            result.status(),
            result.targetUrl(),
            result.contentSummary(CONTENT_SUMMARY_LENGTH),
            result.durationMillis());
    }

    private void closeBrowserContext(final BrowserContext context) {
        if (context != null) {
            try {
                context.close();
            } catch (final Exception exception) {
                log.warn("BrowserContext 정리 중 오류 발생: {}", exception.getMessage());
            }
        }
    }
}
