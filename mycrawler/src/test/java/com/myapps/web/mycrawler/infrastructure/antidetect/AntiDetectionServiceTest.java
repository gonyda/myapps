package com.myapps.web.mycrawler.infrastructure.antidetect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Mouse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ViewportSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AntiDetectionService의 단위 테스트.
 *
 * <p>Mockito를 사용하여 Playwright 객체(Page, BrowserContext)를 mock하고, 서비스의 핵심 동작을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class AntiDetectionServiceTest {

    @Mock private Page page;

    @Mock private Mouse mouse;

    @Mock private BrowserContext browserContext;

    private AntiDetectionService antiDetectionService;

    @BeforeEach
    void setUp() {
        antiDetectionService = new AntiDetectionService();
    }

    /** randomUserAgent()가 사전 정의된 목록의 원소를 반환하는지 검증합니다. */
    @Test
    void should_returnValidUserAgent_when_randomUserAgentCalled() {
        final String userAgent = antiDetectionService.randomUserAgent();

        assertThat(userAgent).isIn(antiDetectionService.getUserAgentList());
    }

    /** randomViewport()가 유효한 범위 내의 값을 반환하는지 검증합니다. */
    @Test
    void should_returnViewportInRange_when_randomViewportCalled() {
        final ViewportSize viewport = antiDetectionService.randomViewport();

        assertThat(viewport.width).isBetween(1280, 1920);
        assertThat(viewport.height).isBetween(720, 1080);
    }

    /** randomPageDelay()가 [1000, 5000]ms 범위 내의 값을 반환하는지 검증합니다. */
    @Test
    void should_returnDelayInRange_when_randomPageDelayCalled() {
        final long delay = antiDetectionService.randomPageDelay();

        assertThat(delay).isBetween(1000L, 5000L);
    }

    /** randomInterTargetDelay()가 [3000, 10000]ms 범위 내의 값을 반환하는지 검증합니다. */
    @Test
    void should_returnDelayInRange_when_randomInterTargetDelayCalled() {
        final long delay = antiDetectionService.randomInterTargetDelay();

        assertThat(delay).isBetween(3000L, 10000L);
    }

    /** simulateHumanBehavior()가 page의 마우스 이동과 스크롤을 호출하는지 검증합니다. */
    @Test
    void should_callMouseMoveAndWheel_when_simulateHumanBehaviorCalled() {
        when(page.mouse()).thenReturn(mouse);

        antiDetectionService.simulateHumanBehavior(page);

        verify(mouse, atLeastOnce()).move(anyDouble(), anyDouble());
        verify(mouse, atLeastOnce()).wheel(anyDouble(), anyDouble());
    }

    /** applyStealthSettings()가 context의 addInitScript를 호출하는지 검증합니다. */
    @Test
    void should_callAddInitScript_when_applyStealthSettingsCalled() {
        antiDetectionService.applyStealthSettings(browserContext);

        verify(browserContext).addInitScript(anyString());
    }
}
