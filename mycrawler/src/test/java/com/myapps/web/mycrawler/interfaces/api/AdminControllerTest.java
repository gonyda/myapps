package com.myapps.web.mycrawler.interfaces.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.myapps.web.mycrawler.application.service.CrawlerService;
import com.myapps.web.mycrawler.application.service.SchedulerService;
import com.myapps.web.mycrawler.domain.model.CrawlResult;
import com.myapps.web.mycrawler.domain.model.CrawlStatus;
import com.myapps.web.mycrawler.domain.model.CrawlTarget;
import com.myapps.web.mycrawler.domain.model.TriggerSource;
import com.myapps.web.mycrawler.infrastructure.config.CrawlerConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AdminController의 웹 슬라이스 테스트.
 *
 * <p>MockMvc를 활용하여 대시보드 GET 요청 및 개별 크롤링 실행 POST 요청을 검증합니다.
 */
@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CrawlerService crawlerService;

    @MockitoBean private SchedulerService schedulerService;

    @MockitoBean private CrawlerConfig crawlerConfig;

    /** GET /admin 요청 시 200 응답과 대시보드 뷰, 모델 속성을 검증합니다. */
    @Test
    void should_returnDashboardView_when_getAdminRequested() throws Exception {
        final String nextExecution = "2026-06-27 06:00:00";

        when(crawlerService.getRecentResults()).thenReturn(List.of());
        when(crawlerService.isRunning()).thenReturn(false);
        when(schedulerService.isEnabled()).thenReturn(true);
        when(schedulerService.getNextExecutionTime()).thenReturn(Optional.of(nextExecution));
        when(schedulerService.getCronExpression()).thenReturn("0 0 */6 * * *");

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(
                        model().attributeExists(
                                        "results",
                                        "isRunning",
                                        "schedulerEnabled",
                                        "nextExecutionTime",
                                        "cronExpression",
                                        "contentMaxLength"))
                .andExpect(model().attribute("isRunning", false))
                .andExpect(model().attribute("schedulerEnabled", true))
                .andExpect(model().attribute("cronExpression", "0 0 */6 * * *"))
                .andExpect(model().attribute("contentMaxLength", 500));
    }

    /** GET /admin 요청 시 모델에 targets 속성이 포함되는지 검증합니다. */
    @Test
    void should_includeTargetsInModel_when_getAdminRequested() throws Exception {
        final List<CrawlTarget> targets =
                List.of(
                        new CrawlTarget("fmkorea-stock", "https://fmkorea.com/stock"),
                        new CrawlTarget(
                                "wepoll-stock",
                                "https://wepoll.kr/g2/bbs/board.php?bo_table=stock"));

        when(crawlerService.getRecentResults()).thenReturn(List.of());
        when(crawlerService.isRunning()).thenReturn(false);
        when(schedulerService.isEnabled()).thenReturn(true);
        when(schedulerService.getNextExecutionTime()).thenReturn(Optional.empty());
        when(schedulerService.getCronExpression()).thenReturn("0 0 */6 * * *");
        when(crawlerConfig.validTargets()).thenReturn(targets);

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("targets"))
                .andExpect(model().attribute("targets", targets));
    }

    /** POST /admin/crawl/{targetName} 성공 시 successMessage flash 속성과 리다이렉트를 검증합니다. */
    @Test
    void should_setSuccessFlash_when_executeSingleReturnsResult() throws Exception {
        final String targetName = "fmkorea-stock";
        final LocalDateTime now = LocalDateTime.now();
        final CrawlResult result =
                new CrawlResult(
                        targetName,
                        "https://fmkorea.com/stock",
                        CrawlStatus.SUCCESS,
                        TriggerSource.MANUAL,
                        "<html>content</html>",
                        null,
                        now,
                        now.plusSeconds(3));

        when(crawlerService.isRunning()).thenReturn(false);
        when(schedulerService.isScheduledRunning()).thenReturn(false);
        when(crawlerService.executeSingle(targetName, TriggerSource.MANUAL)).thenReturn(result);

        mockMvc.perform(post("/admin/crawl/{targetName}", targetName))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("successMessage", targetName + " 크롤링이 완료되었습니다"));
    }

    /** POST /admin/crawl/{targetName} 실행 중일 때 warningMessage flash 속성과 리다이렉트를 검증합니다. */
    @Test
    void should_setWarningFlash_when_crawlerIsRunning() throws Exception {
        final String targetName = "fmkorea-stock";

        when(crawlerService.isRunning()).thenReturn(true);
        when(schedulerService.isScheduledRunning()).thenReturn(false);

        mockMvc.perform(post("/admin/crawl/{targetName}", targetName))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("warningMessage", "크롤링이 이미 실행 중입니다. 완료 후 다시 시도해주세요."));
    }

    /** POST /admin/crawl/{targetName} 스케줄러 실행 중일 때 warningMessage flash 속성과 리다이렉트를 검증합니다. */
    @Test
    void should_setWarningFlash_when_schedulerIsRunning() throws Exception {
        final String targetName = "wepoll-stock";

        when(crawlerService.isRunning()).thenReturn(false);
        when(schedulerService.isScheduledRunning()).thenReturn(true);

        mockMvc.perform(post("/admin/crawl/{targetName}", targetName))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("warningMessage", "크롤링이 이미 실행 중입니다. 완료 후 다시 시도해주세요."));
    }

    /** POST /admin/crawl/{targetName} 미등록 타겟일 때 errorMessage flash 속성과 리다이렉트를 검증합니다. */
    @Test
    void should_setErrorFlash_when_executeSingleReturnsNull() throws Exception {
        final String targetName = "unknown-target";

        when(crawlerService.isRunning()).thenReturn(false);
        when(schedulerService.isScheduledRunning()).thenReturn(false);
        when(crawlerService.executeSingle(targetName, TriggerSource.MANUAL)).thenReturn(null);

        mockMvc.perform(post("/admin/crawl/{targetName}", targetName))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("errorMessage", "크롤링 대상을 찾을 수 없습니다: " + targetName));
    }

    /**
     * POST /admin/crawl (경로 변수 없는 기존 전체 실행 경로) 요청 시 유효한 엔드포인트가 아님을 검증합니다.
     *
     * <p>GlobalExceptionHandler가 NoResourceFoundException을 처리하여 에러 뷰를 반환합니다.
     */
    @Test
    void should_returnErrorView_when_postCrawlWithoutTargetName() throws Exception {
        mockMvc.perform(post("/admin/crawl"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));
    }
}
