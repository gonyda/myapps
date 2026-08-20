package com.myapps.web.mycrawler.infrastructure.antidetect;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.options.ViewportSize;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

/**
 * AntiDetectionService의 랜덤 선택, 딜레이 범위, Viewport 범위 동작에 대한 Property-Based 테스트.
 *
 * <p>jqwik을 사용하여 randomUserAgent() 호출 결과가 항상 사전 정의된 User-Agent 목록의 원소임을 검증하고, 랜덤 딜레이 메서드들이 정해진 범위
 * 내의 값만 반환하며, randomViewport()가 허용된 해상도 범위 내의 값만 반환함을 검증합니다.
 *
 * <p><b>Validates: Requirements 6.1, 6.2, 6.4, 6.6</b>
 */
class AntiDetectionServicePropertyTest {

    private final AntiDetectionService antiDetectionService = new AntiDetectionService();

    /**
     * randomUserAgent() 반환값이 사전 정의된 User-Agent 목록의 원소임을 검증합니다.
     *
     * <p>여러 번의 호출에 대해 반환값이 항상 허용된 목록에 포함되어 있는지 확인하여, 목록 외부의 값이 반환되는 경우가 없음을 보장합니다.
     *
     * @param seed 랜덤 시드로 활용되는 임의의 정수 (jqwik이 다양한 실행 경로를 탐색하도록 유도)
     */
    @Property(tries = 100)
    void randomUserAgentMustBeInPredefinedList(
            @ForAll @IntRange(min = 0, max = 1000) final int seed) {
        final String userAgent = antiDetectionService.randomUserAgent();
        final List<String> allowedUserAgents = antiDetectionService.getUserAgentList();

        assertThat(allowedUserAgents).contains(userAgent);
    }

    /**
     * randomPageDelay() 반환값이 [1000, 5000]ms 범위 내임을 검증합니다.
     *
     * <p>페이지 로드 후 적용되는 랜덤 딜레이가 항상 허용된 범위를 벗어나지 않음을 보장합니다.
     *
     * <p><b>Validates: Requirements 6.2</b>
     *
     * @param seed 랜덤 시드로 활용되는 임의의 정수 (jqwik이 다양한 실행 경로를 탐색하도록 유도)
     */
    @Property(tries = 100)
    void randomPageDelayMustBeWithinRange(@ForAll @IntRange(min = 0, max = 1000) final int seed) {
        final long delay = antiDetectionService.randomPageDelay();

        assertThat(delay).isBetween(1000L, 5000L);
    }

    /**
     * randomInterTargetDelay() 반환값이 [3000, 10000]ms 범위 내임을 검증합니다.
     *
     * <p>타겟 간 요청 간격에 적용되는 랜덤 딜레이가 항상 허용된 범위를 벗어나지 않음을 보장합니다.
     *
     * <p><b>Validates: Requirements 6.6</b>
     *
     * @param seed 랜덤 시드로 활용되는 임의의 정수 (jqwik이 다양한 실행 경로를 탐색하도록 유도)
     */
    @Property(tries = 100)
    void randomInterTargetDelayMustBeWithinRange(
            @ForAll @IntRange(min = 0, max = 1000) final int seed) {
        final long delay = antiDetectionService.randomInterTargetDelay();

        assertThat(delay).isBetween(3000L, 10000L);
    }

    /**
     * randomViewport() 반환값의 width가 [1280, 1920], height가 [720, 1080] 범위 내임을 검증합니다.
     *
     * <p>데스크톱 해상도 범위 내에서 생성되는 랜덤 viewport가 항상 허용된 범위를 벗어나지 않음을 보장합니다.
     *
     * <p><b>Validates: Requirements 6.4</b>
     *
     * @param seed 랜덤 시드로 활용되는 임의의 정수 (jqwik이 다양한 실행 경로를 탐색하도록 유도)
     */
    @Property(tries = 100)
    void randomViewportMustBeWithinRange(@ForAll @IntRange(min = 0, max = 1000) final int seed) {
        final ViewportSize viewport = antiDetectionService.randomViewport();

        assertThat(viewport.width).isBetween(1280, 1920);
        assertThat(viewport.height).isBetween(720, 1080);
    }
}
