package com.myapps.web.mycrawler.interfaces.api;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.myapps.web.mycrawler.application.service.CrawlerService;
import com.myapps.web.mycrawler.application.service.SchedulerService;
import com.myapps.web.mycrawler.domain.model.CrawlResult;
import com.myapps.web.mycrawler.domain.model.TriggerSource;

/**
 * 크롤러 관리 웹 UI 컨트롤러.
 *
 * <p>대시보드 화면을 제공하며, 수동 크롤링 실행 및 결과 조회,
 * 스케줄러 상태 확인 기능을 담당합니다.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final int CONTENT_SUMMARY_MAX_LENGTH = 500;

    private final CrawlerService crawlerService;
    private final SchedulerService schedulerService;

    /**
     * AdminController 인스턴스를 생성합니다.
     *
     * @param crawlerService   크롤링 실행 서비스
     * @param schedulerService 스케줄러 서비스
     */
    public AdminController(final CrawlerService crawlerService,
                           final SchedulerService schedulerService) {
        this.crawlerService = crawlerService;
        this.schedulerService = schedulerService;
    }

    /**
     * 관리 대시보드 화면을 표시합니다.
     *
     * <p>최근 크롤링 결과 목록, 스케줄러 상태, 수동 실행 버튼을 포함합니다.
     *
     * @param model Thymeleaf 모델
     * @return 대시보드 뷰 이름
     */
    @GetMapping
    public String dashboard(final Model model) {
        populateDashboardModel(model);
        return "admin";
    }

    /**
     * 크롤링을 수동으로 실행합니다.
     *
     * <p>PRG(Post-Redirect-Get) 패턴을 적용하여 새로고침 시 중복 실행을 방지합니다.
     * 실행 중인 경우 경고 메시지를, 실행 완료 시 성공 메시지를 flash attribute로 전달합니다.
     *
     * @param redirectAttributes 리다이렉트 시 전달할 flash attributes
     * @return 대시보드로의 리다이렉트 경로
     */
    @PostMapping("/crawl")
    public String triggerCrawl(final RedirectAttributes redirectAttributes) {
        if (crawlerService.isRunning()) {
            redirectAttributes.addFlashAttribute("warningMessage", "현재 크롤링이 진행 중입니다");
            return "redirect:/admin";
        }

        final List<CrawlResult> results = crawlerService.executeAll(TriggerSource.MANUAL);
        redirectAttributes.addFlashAttribute("successMessage",
                "수동 크롤링이 완료되었습니다. 처리된 타겟: " + results.size() + "건");
        return "redirect:/admin";
    }

    private void populateDashboardModel(final Model model) {
        model.addAttribute("results", crawlerService.getRecentResults());
        model.addAttribute("isRunning", crawlerService.isRunning());
        model.addAttribute("schedulerEnabled", schedulerService.isEnabled());
        model.addAttribute("nextExecutionTime", schedulerService.getNextExecutionTime().orElse(null));
        model.addAttribute("cronExpression", schedulerService.getCronExpression());
        model.addAttribute("contentMaxLength", CONTENT_SUMMARY_MAX_LENGTH);
    }
}
