package com.myapps.web.mycalendar.interfaces.api;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import com.myapps.web.mycalendar.application.dto.ScheduleResponse;
import com.myapps.web.mycalendar.application.service.ScheduleService;
import com.myapps.web.mycalendar.domain.model.Anniversary;
import com.myapps.web.mycalendar.domain.service.AnniversaryCalculator;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 월별 캘린더 뷰를 렌더링하는 컨트롤러.
 *
 * <p>Thymeleaf 템플릿에 일정, 기념일, D-Day 카운터 데이터를 전달하여
 * 캘린더 뷰를 구성합니다. 루트 경로 접근 시 현재 월 캘린더로 리다이렉트합니다.
 */
@Controller
public class CalendarController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ScheduleService scheduleService;
    private final AnniversaryCalculator anniversaryCalculator;
    private final CalendarViewHelper calendarViewHelper;

    /**
     * CalendarController를 생성합니다.
     *
     * @param scheduleService       일정 조회 서비스
     * @param anniversaryCalculator 기념일 및 D-Day 계산 도메인 서비스
     * @param calendarViewHelper    캘린더 뷰 헬퍼
     */
    public CalendarController(final ScheduleService scheduleService,
                              final AnniversaryCalculator anniversaryCalculator,
                              final CalendarViewHelper calendarViewHelper) {
        this.scheduleService = scheduleService;
        this.anniversaryCalculator = anniversaryCalculator;
        this.calendarViewHelper = calendarViewHelper;
    }

    /**
     * 루트 경로 접근 시 현재 월 캘린더 뷰로 리다이렉트합니다.
     *
     * @return 현재 연도/월 캘린더 경로로의 리다이렉트 문자열
     */
    @GetMapping("/")
    public String redirectToCurrentMonth() {
        final LocalDate today = LocalDate.now(KST);
        return "redirect:/calendar/" + today.getYear() + "/" + today.getMonthValue();
    }

    /**
     * 특정 연도/월의 캘린더 뷰를 렌더링합니다.
     *
     * <p>해당 월의 일정, 기념일, D-Day 카운터, 이전/다음 월 네비게이션 정보를
     * 모델에 추가하여 Thymeleaf 템플릿에 전달합니다.
     *
     * @param year  조회 대상 연도
     * @param month 조회 대상 월 (1~12)
     * @param model 뷰에 전달할 모델
     * @return 캘린더 뷰 템플릿 이름
     */
    @GetMapping("/calendar/{year}/{month}")
    public String showCalendar(@PathVariable("year") final int year,
                               @PathVariable("month") final int month,
                               final Model model) {
        final YearMonth yearMonth = YearMonth.of(year, month);
        final LocalDate today = LocalDate.now(KST);

        final List<ScheduleResponse> schedules = scheduleService.findByMonth(yearMonth);
        final List<Anniversary> anniversaries = anniversaryCalculator.getAnniversariesForMonth(yearMonth);
        final long dDay = anniversaryCalculator.calculateDDay(today);

        final YearMonth previousMonth = yearMonth.minusMonths(1);
        final YearMonth nextMonth = yearMonth.plusMonths(1);

        model.addAttribute("year", year);
        model.addAttribute("month", month);
        model.addAttribute("schedules", schedules);
        model.addAttribute("anniversaries", anniversaries);
        model.addAttribute("dDay", dDay);
        model.addAttribute("today", today);
        model.addAttribute("prevYear", previousMonth.getYear());
        model.addAttribute("prevMonth", previousMonth.getMonthValue());
        model.addAttribute("nextYear", nextMonth.getYear());
        model.addAttribute("nextMonth", nextMonth.getMonthValue());
        model.addAttribute("calendarHelper", calendarViewHelper);

        return "calendar";
    }
}
