package com.myapps.web.mycrawler.interfaces.api;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.myapps.web.mycrawler.application.service.CrawlerService;
import com.myapps.web.mycrawler.application.service.SchedulerService;
import com.myapps.web.mycrawler.domain.model.CrawlResult;
import com.myapps.web.mycrawler.domain.model.TriggerSource;
import com.myapps.web.mycrawler.infrastructure.config.CrawlerConfig;

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
    private final CrawlerConfig crawlerConfig;

    /**
     * AdminController 인스턴스를 생성합니다.
     *
     * @param crawlerService   크롤링 실행 서비스
     * @param schedulerService 스케줄러 서비스
     * @param crawlerConfig    크롤러 설정
     */
    public AdminController(final CrawlerService crawlerService,
                           final SchedulerService schedulerService,
                           final CrawlerConfig crawlerConfig) {
        this.crawlerService = crawlerService;
        this.schedulerService = schedulerService;
        this.crawlerConfig = crawlerConfig;
    }

    /**
     * 관리 대시보드 화면을 표시합니다.
     *
     * <p>최근 크롤링 결과 목록, 스케줄러 상태, 등록된 크롤러 목록을 포함합니다.
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
     * 특정 타겟에 대해 수동 크롤링을 실행합니다.
     *
     * <p>크롤링이 이미 실행 중이면 경고 메시지와 함께 대시보드로 리다이렉트합니다.
     * 미등록 타겟인 경우 오류 메시지를, 정상 완료 시 성공 메시지를 flash 속성으로 전달합니다.
     *
     * @param targetName         크롤링할 타겟 이름
     * @param redirectAttributes 리다이렉트 시 flash 속성 전달용
     * @return 대시보드 리다이렉트 경로
     */
    @PostMapping("/crawl/{targetName}")
    public String triggerSingleCrawl(@PathVariable final String targetName,
                                     final RedirectAttributes redirectAttributes) {
        if (crawlerService.isRunning() || schedulerService.isScheduledRunning()) {
            redirectAttributes.addFlashAttribute("warningMessage",
                    "크롤링이 이미 실행 중입니다. 완료 후 다시 시도해주세요.");
            return "redirect:/admin";
        }

        final CrawlResult result = crawlerService.executeSingle(targetName, TriggerSource.MANUAL);

        if (result == null) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "크롤링 대상을 찾을 수 없습니다: " + targetName);
            return "redirect:/admin";
        }

        redirectAttributes.addFlashAttribute("successMessage",
                targetName + " 크롤링이 완료되었습니다");
        return "redirect:/admin";
    }

    private void populateDashboardModel(final Model model) {
        model.addAttribute("results", crawlerService.getRecentResults());
        model.addAttribute("isRunning", crawlerService.isRunning());
        model.addAttribute("schedulerEnabled", schedulerService.isEnabled());
        model.addAttribute("nextExecutionTime", schedulerService.getNextExecutionTime().orElse(null));
        model.addAttribute("cronExpression", schedulerService.getCronExpression());
        model.addAttribute("contentMaxLength", CONTENT_SUMMARY_MAX_LENGTH);
        model.addAttribute("targets", crawlerConfig.validTargets());
    }
}
