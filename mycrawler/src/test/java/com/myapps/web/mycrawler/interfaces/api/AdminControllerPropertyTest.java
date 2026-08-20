package com.myapps.web.mycrawler.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.mycrawler.application.service.CrawlerService;
import com.myapps.web.mycrawler.application.service.SchedulerService;
import com.myapps.web.mycrawler.domain.model.CrawlResult;
import com.myapps.web.mycrawler.domain.model.CrawlStatus;
import com.myapps.web.mycrawler.domain.model.CrawlTarget;
import com.myapps.web.mycrawler.domain.model.TriggerSource;
import com.myapps.web.mycrawler.infrastructure.config.CrawlerConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AdminController에 대한 Property-Based 테스트.
 *
 * <p>jqwik을 사용하여 다양한 타겟 이름 문자열과 타겟 목록에 대해 AdminController의 개별 실행 위임, 실행 중 가드, 오류 처리, 대시보드 모델 속성 포함
 * 동작을 검증합니다.
 *
 * <p><b>Validates: Requirements 1.2, 1.3, 1.4, 1.5, 3.1, 4.1, 4.2</b>
 */
class AdminControllerPropertyTest {

    // Feature: 002-individual-execution, Property 1: 개별 실행 위임 및 성공 응답

    /**
     * isRunning false, isScheduledRunning false, executeSingle이 non-null CrawlResult를 반환할 때
     * executeSingle(targetName, TriggerSource.MANUAL)이 호출되고 성공 flash가 설정됨을 검증합니다.
     *
     * <p><b>Validates: Requirements 1.2, 1.3</b>
     *
     * @param targetName 임의의 타겟 이름 문자열
     */
    @Property(tries = 100)
    void shouldDelegateExecutionAndSetSuccessFlash(@ForAll("targetNames") final String targetName) {

        final CrawlerService mockCrawlerService = mock(CrawlerService.class);
        final SchedulerService mockSchedulerService = mock(SchedulerService.class);
        final CrawlerConfig mockCrawlerConfig = mock(CrawlerConfig.class);
        final RedirectAttributes mockRedirectAttributes = mock(RedirectAttributes.class);

        when(mockCrawlerService.isRunning()).thenReturn(false);
        when(mockSchedulerService.isScheduledRunning()).thenReturn(false);

        final LocalDateTime now = LocalDateTime.now();
        final CrawlResult crawlResult =
                new CrawlResult(
                        targetName,
                        "https://example.com/" + targetName,
                        CrawlStatus.SUCCESS,
                        TriggerSource.MANUAL,
                        "<html>content</html>",
                        null,
                        now,
                        now.plusSeconds(2));
        when(mockCrawlerService.executeSingle(targetName, TriggerSource.MANUAL))
                .thenReturn(crawlResult);

        final AdminController controller =
                new AdminController(mockCrawlerService, mockSchedulerService, mockCrawlerConfig);

        final String result = controller.triggerSingleCrawl(targetName, mockRedirectAttributes);

        verify(mockCrawlerService).executeSingle(targetName, TriggerSource.MANUAL);
        verify(mockRedirectAttributes)
                .addFlashAttribute("successMessage", targetName + " 크롤링이 완료되었습니다");
        assertThat(result).isEqualTo("redirect:/admin");
    }

    // Feature: 002-individual-execution, Property 2: 실행 중 가드

    /**
     * isRunning true 또는 isScheduledRunning true일 때 executeSingle이 호출되지 않고 경고 flash가 설정됨을 검증합니다.
     *
     * <p><b>Validates: Requirements 1.4</b>
     *
     * @param targetName 임의의 타겟 이름 문자열
     * @param runningScenario 실행 중 시나리오 (0: crawler만, 1: scheduler만, 2: 둘 다)
     */
    @Property(tries = 100)
    void shouldNotCallExecuteSingleWhenRunning(
            @ForAll("targetNames") final String targetName,
            @ForAll("runningScenarios") final int runningScenario) {

        final CrawlerService mockCrawlerService = mock(CrawlerService.class);
        final SchedulerService mockSchedulerService = mock(SchedulerService.class);
        final CrawlerConfig mockCrawlerConfig = mock(CrawlerConfig.class);
        final RedirectAttributes mockRedirectAttributes = mock(RedirectAttributes.class);

        final boolean crawlerRunning = runningScenario == 0 || runningScenario == 2;
        final boolean schedulerRunning = runningScenario == 1 || runningScenario == 2;

        when(mockCrawlerService.isRunning()).thenReturn(crawlerRunning);
        when(mockSchedulerService.isScheduledRunning()).thenReturn(schedulerRunning);

        final AdminController controller =
                new AdminController(mockCrawlerService, mockSchedulerService, mockCrawlerConfig);

        final String result = controller.triggerSingleCrawl(targetName, mockRedirectAttributes);

        verify(mockCrawlerService, never()).executeSingle(anyString(), any());
        verify(mockRedirectAttributes)
                .addFlashAttribute("warningMessage", "크롤링이 이미 실행 중입니다. 완료 후 다시 시도해주세요.");
        assertThat(result).isEqualTo("redirect:/admin");
    }

    // Feature: 002-individual-execution, Property 3: 미등록 타겟 오류 처리

    /**
     * isRunning false, isScheduledRunning false, executeSingle이 null을 반환할 때 오류 flash가 targetName을
     * 포함하여 설정됨을 검증합니다.
     *
     * <p><b>Validates: Requirements 1.5</b>
     *
     * @param targetName 임의의 타겟 이름 문자열
     */
    @Property(tries = 100)
    void shouldSetErrorFlashWhenExecuteSingleReturnsNull(
            @ForAll("targetNames") final String targetName) {

        final CrawlerService mockCrawlerService = mock(CrawlerService.class);
        final SchedulerService mockSchedulerService = mock(SchedulerService.class);
        final CrawlerConfig mockCrawlerConfig = mock(CrawlerConfig.class);
        final RedirectAttributes mockRedirectAttributes = mock(RedirectAttributes.class);

        when(mockCrawlerService.isRunning()).thenReturn(false);
        when(mockSchedulerService.isScheduledRunning()).thenReturn(false);
        when(mockCrawlerService.executeSingle(targetName, TriggerSource.MANUAL)).thenReturn(null);

        final AdminController controller =
                new AdminController(mockCrawlerService, mockSchedulerService, mockCrawlerConfig);

        final String result = controller.triggerSingleCrawl(targetName, mockRedirectAttributes);

        verify(mockRedirectAttributes)
                .addFlashAttribute("errorMessage", "크롤링 대상을 찾을 수 없습니다: " + targetName);
        assertThat(result).isEqualTo("redirect:/admin");
    }

    // Feature: 002-individual-execution, Property 4: 대시보드 모델 타겟 목록 포함

    /**
     * validTargets() 반환값이 모델의 "targets" 속성에 그대로 포함됨을 검증합니다.
     *
     * <p><b>Validates: Requirements 3.1, 4.1, 4.2</b>
     *
     * @param targetCount 생성할 타겟 수 (0~10)
     */
    @Property(tries = 100)
    void shouldIncludeValidTargetsInDashboardModel(@ForAll("targetCounts") final int targetCount) {

        final CrawlerService mockCrawlerService = mock(CrawlerService.class);
        final SchedulerService mockSchedulerService = mock(SchedulerService.class);
        final CrawlerConfig mockCrawlerConfig = mock(CrawlerConfig.class);
        final Model mockModel = mock(Model.class);

        final List<CrawlTarget> targets = generateCrawlTargets(targetCount);

        when(mockCrawlerService.getRecentResults()).thenReturn(List.of());
        when(mockCrawlerService.isRunning()).thenReturn(false);
        when(mockSchedulerService.isEnabled()).thenReturn(true);
        when(mockSchedulerService.getNextExecutionTime()).thenReturn(java.util.Optional.empty());
        when(mockSchedulerService.getCronExpression()).thenReturn("0 0 */6 * * *");
        when(mockCrawlerConfig.validTargets()).thenReturn(targets);

        final AdminController controller =
                new AdminController(mockCrawlerService, mockSchedulerService, mockCrawlerConfig);

        final String viewName = controller.dashboard(mockModel);

        verify(mockModel).addAttribute("targets", targets);
        assertThat(viewName).isEqualTo("admin");
    }

    /**
     * 1~30자 영문 알파벳 타겟 이름을 생성하는 Arbitrary provider.
     *
     * @return 1~30자 영문 알파벳 문자열의 Arbitrary
     */
    @Provide
    Arbitrary<String> targetNames() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30);
    }

    /**
     * 실행 중 시나리오를 생성하는 Arbitrary provider.
     *
     * <p>0: crawlerService만 실행 중, 1: schedulerService만 실행 중, 2: 둘 다 실행 중
     *
     * @return 0~2 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> runningScenarios() {
        return Arbitraries.integers().between(0, 2);
    }

    /**
     * 0~10 범위의 타겟 수를 생성하는 Arbitrary provider.
     *
     * @return 0~10 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> targetCounts() {
        return Arbitraries.integers().between(0, 10);
    }

    private List<CrawlTarget> generateCrawlTargets(final int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new CrawlTarget("target-" + i, "https://example.com/target" + i))
                .collect(Collectors.toList());
    }
}
