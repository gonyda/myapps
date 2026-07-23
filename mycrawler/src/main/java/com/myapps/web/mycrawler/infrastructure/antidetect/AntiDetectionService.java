package com.myapps.web.mycrawler.infrastructure.antidetect;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Mouse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ViewportSize;

/**
 * 봇 탐지 회피를 위한 설정 및 행동 시뮬레이션 서비스.
 *
 * <p>User-Agent 랜덤화, 랜덤 딜레이, 마우스/스크롤 시뮬레이션,
 * viewport 랜덤화, navigator.webdriver 제거 등 다층 방어 전략을 제공합니다.
 */
@Service
public class AntiDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AntiDetectionService.class);

    private static final int VIEWPORT_MIN_WIDTH = 1280;
    private static final int VIEWPORT_MAX_WIDTH = 1920;
    private static final int VIEWPORT_MIN_HEIGHT = 720;
    private static final int VIEWPORT_MAX_HEIGHT = 1080;

    private static final long PAGE_DELAY_MIN_MS = 1000L;
    private static final long PAGE_DELAY_MAX_MS = 5000L;

    private static final long INTER_TARGET_DELAY_MIN_MS = 3000L;
    private static final long INTER_TARGET_DELAY_MAX_MS = 10000L;

    private static final int MOUSE_MOVE_COUNT = 3;
    private static final int MOUSE_MOVE_MAX_X = 800;
    private static final int MOUSE_MOVE_MAX_Y = 600;
    private static final int SCROLL_MIN_PIXELS = 100;
    private static final int SCROLL_MAX_PIXELS = 500;
    private static final int SCROLL_COUNT = 2;

    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:133.0) Gecko/20100101 Firefox/133.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"
    );

    private static final String STEALTH_SCRIPT = """
            Object.defineProperty(navigator, 'webdriver', {
                get: () => undefined
            });
            Object.defineProperty(navigator, 'plugins', {
                get: () => [1, 2, 3, 4, 5]
            });
            Object.defineProperty(navigator, 'languages', {
                get: () => ['ko-KR', 'ko', 'en-US', 'en']
            });
            window.chrome = { runtime: {} };
            """;

    /**
     * 사전 정의된 User-Agent 목록에서 랜덤으로 하나를 선택하여 반환합니다.
     *
     * <p>Chrome, Firefox, Edge 최신 버전의 실제 브라우저 UA 문자열 목록에서 선택합니다.
     *
     * @return 랜덤 선택된 User-Agent 문자열
     */
    public String randomUserAgent() {
        final int index = ThreadLocalRandom.current().nextInt(USER_AGENTS.size());
        final String userAgent = USER_AGENTS.get(index);
        log.debug("선택된 User-Agent: {}", userAgent);
        return userAgent;
    }

    /**
     * 일반적인 데스크톱 해상도 범위 내에서 랜덤 viewport 크기를 생성합니다.
     *
     * <p>width: [1280, 1920], height: [720, 1080] 범위 내에서 랜덤 생성합니다.
     *
     * @return 랜덤 생성된 {@link ViewportSize}
     */
    public ViewportSize randomViewport() {
        final int width = randomIntInRange(VIEWPORT_MIN_WIDTH, VIEWPORT_MAX_WIDTH);
        final int height = randomIntInRange(VIEWPORT_MIN_HEIGHT, VIEWPORT_MAX_HEIGHT);
        log.debug("선택된 Viewport: {}x{}", width, height);
        return new ViewportSize(width, height);
    }

    /**
     * 페이지 로드 후 적용할 랜덤 딜레이를 밀리초 단위로 반환합니다.
     *
     * <p>[1000, 5000]ms 범위 내에서 랜덤 딜레이를 생성합니다.
     *
     * @return 랜덤 페이지 딜레이 (밀리초)
     */
    public long randomPageDelay() {
        final long delay = randomLongInRange(PAGE_DELAY_MIN_MS, PAGE_DELAY_MAX_MS);
        log.debug("페이지 딜레이: {}ms", delay);
        return delay;
    }

    /**
     * 다수 타겟 간 요청 간격에 적용할 랜덤 딜레이를 밀리초 단위로 반환합니다.
     *
     * <p>[3000, 10000]ms 범위 내에서 랜덤 딜레이를 생성합니다.
     *
     * @return 랜덤 타겟 간 딜레이 (밀리초)
     */
    public long randomInterTargetDelay() {
        final long delay = randomLongInRange(INTER_TARGET_DELAY_MIN_MS, INTER_TARGET_DELAY_MAX_MS);
        log.debug("타겟 간 딜레이: {}ms", delay);
        return delay;
    }

    /**
     * 사람의 브라우징 패턴을 시뮬레이션하기 위해 랜덤 마우스 이동과 스크롤을 수행합니다.
     *
     * <p>페이지 내에서 랜덤한 좌표로 마우스를 이동하고,
     * 랜덤한 양만큼 페이지를 스크롤합니다.
     *
     * @param page 동작을 수행할 Playwright {@link Page} 인스턴스
     */
    public void simulateHumanBehavior(final Page page) {
        log.debug("인간 행동 시뮬레이션 시작 — 마우스 이동 {}회, 스크롤 {}회 수행",
                MOUSE_MOVE_COUNT, SCROLL_COUNT);
        performRandomMouseMovements(page);
        performRandomScrolls(page);
        log.debug("인간 행동 시뮬레이션 완료");
    }

    /**
     * 브라우저 컨텍스트에 스텔스 설정을 적용합니다.
     *
     * <p>navigator.webdriver 속성을 숨기고, Playwright 자동화 탐지 시그니처를 제거하는
     * 초기화 스크립트를 주입합니다.
     *
     * @param context 스텔스 설정을 적용할 Playwright {@link BrowserContext} 인스턴스
     */
    public void applyStealthSettings(final BrowserContext context) {
        context.addInitScript(STEALTH_SCRIPT);
        log.debug("스텔스 설정 적용 — webdriver 은닉, plugins 위장, languages(ko-KR) 설정, chrome.runtime 주입");
    }

    /**
     * 사전 정의된 User-Agent 목록을 반환합니다.
     *
     * <p>Property 테스트 등에서 멤버십 검증에 활용할 수 있습니다.
     *
     * @return 불변 User-Agent 문자열 목록
     */
    public List<String> getUserAgentList() {
        return USER_AGENTS;
    }

    private void performRandomMouseMovements(final Page page) {
        final Mouse mouse = page.mouse();
        for (int i = 0; i < MOUSE_MOVE_COUNT; i++) {
            final double x = ThreadLocalRandom.current().nextDouble(MOUSE_MOVE_MAX_X);
            final double y = ThreadLocalRandom.current().nextDouble(MOUSE_MOVE_MAX_Y);
            mouse.move(x, y);
        }
    }

    private void performRandomScrolls(final Page page) {
        for (int i = 0; i < SCROLL_COUNT; i++) {
            final int scrollAmount = randomIntInRange(SCROLL_MIN_PIXELS, SCROLL_MAX_PIXELS);
            page.mouse().wheel(0, scrollAmount);
        }
    }

    private int randomIntInRange(final int min, final int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private long randomLongInRange(final long min, final long max) {
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }
}
