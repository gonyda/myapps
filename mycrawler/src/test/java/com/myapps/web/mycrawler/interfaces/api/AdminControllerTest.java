package com.myapps.web.mycrawler.interfaces.api;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.myapps.web.mycrawler.application.service.CrawlerService;
import com.myapps.web.mycrawler.application.service.SchedulerService;
import com.myapps.web.mycrawler.domain.model.TriggerSource;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * AdminController의 웹 슬라이스 테스트.
 *
 * <p>MockMvc를 활용하여 대시보드 GET 요청, 수동 크롤링 실행 POST 요청,
 * 중복 실행 방지 시나리오를 검증합니다.
 */
@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrawlerService crawlerService;

    @MockitoBean
    private SchedulerService schedulerService;

    /**
     * GET /admin 요청 시 200 응답과 대시보드 뷰, 모델 속성을 검증합니다.
     */
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
                .andExpect(model().attributeExists(
                        "results", "isRunning", "schedulerEnabled",
                        "nextExecutionTime", "cronExpression", "contentMaxLength"))
                .andExpect(model().attribute("isRunning", false))
                .andExpect(model().attribute("schedulerEnabled", true))
                .andExpect(model().attribute("cronExpression", "0 0 */6 * * *"))
                .andExpect(model().attribute("contentMaxLength", 500));
    }

    /**
     * POST /admin/crawl 요청 시 크롤링을 실행하고 성공 메시지와 함께 리다이렉트합니다.
     */
    @Test
    void should_redirectWithSuccessMessage_when_crawlTriggeredWhileNotRunning() throws Exception {
        when(crawlerService.isRunning()).thenReturn(false);
        when(crawlerService.executeAll(TriggerSource.MANUAL)).thenReturn(List.of());

        mockMvc.perform(post("/admin/crawl"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    /**
     * 크롤링 실행 중 POST /admin/crawl 요청 시 경고 메시지와 함께 리다이렉트합니다.
     */
    @Test
    void should_redirectWithWarningMessage_when_crawlTriggeredWhileAlreadyRunning() throws Exception {
        when(crawlerService.isRunning()).thenReturn(true);

        mockMvc.perform(post("/admin/crawl"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("warningMessage", "현재 크롤링이 진행 중입니다"));
    }
}
