package com.myapps.web.mycalendar.interfaces.api;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.myapps.web.mycalendar.application.dto.ScheduleResponse;
import com.myapps.web.mycalendar.application.service.ScheduleService;
import com.myapps.web.mycalendar.domain.model.Anniversary;
import com.myapps.web.mycalendar.domain.service.AnniversaryCalculator;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

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
     * 루트 경로 접근 시 현재 월 캘린더 뷰로 내부 포워딩합니다.
     *
     * <p>주소창에 {@code /} 상태를 유지하면서 현재 월 캘린더를 렌더링합니다.
     *
     * @return 현재 연도/월 캘린더 경로로의 forward 문자열
     */
    @GetMapping("/")
    public String forwardToCurrentMonth() {
        final LocalDate today = LocalDate.now(KST);
        return "forward:/calendar/" + today.getYear() + "/" + today.getMonthValue();
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

        // 주간 일정 섹션: 오늘이 포함된 주 (일~토 기준)
        final LocalDate weekReferenceDate = resolveWeekReferenceDate(today, yearMonth);
        final LocalDate weekStart = weekReferenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        final LocalDate weekEnd = weekStart.plusDays(6);
        final Map<LocalDate, List<ScheduleResponse>> weeklyScheduleMap = buildWeeklyScheduleMap(weekStart, weekEnd);

        model.addAttribute("weekStart", weekStart);
        model.addAttribute("weekEnd", weekEnd);
        model.addAttribute("weeklyScheduleMap", weeklyScheduleMap);

        return "calendar";
    }

    /**
     * HTMX 요청으로 주간 일정 부분 렌더링을 제공합니다.
     *
     * <p>주간 네비게이션 버튼 클릭 시 해당 주의 일정만 프래그먼트로 반환합니다.
     *
     * @param startDate 조회 대상 주의 시작일 (일요일, yyyy-MM-dd)
     * @param model     뷰에 전달할 모델
     * @return 주간 일정 프래그먼트 뷰 이름
     */
    @GetMapping("/calendar/weekly")
    public String weeklySchedule(@RequestParam("startDate") final LocalDate startDate,
                                 final Model model) {
        final LocalDate weekStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        final LocalDate weekEnd = weekStart.plusDays(6);
        final Map<LocalDate, List<ScheduleResponse>> weeklyScheduleMap = buildWeeklyScheduleMap(weekStart, weekEnd);

        model.addAttribute("weekStart", weekStart);
        model.addAttribute("weekEnd", weekEnd);
        model.addAttribute("weeklyScheduleMap", weeklyScheduleMap);

        return "fragments/weekly-schedule";
    }

    /**
     * 주간 기준 날짜를 결정합니다.
     *
     * <p>현재 표시 중인 월에 오늘이 포함되면 오늘을 기준으로,
     * 그렇지 않으면 해당 월의 1일을 기준으로 합니다.
     *
     * @param today     오늘 날짜
     * @param yearMonth 현재 표시 중인 월
     * @return 주간 기준 날짜
     */
    private LocalDate resolveWeekReferenceDate(final LocalDate today, final YearMonth yearMonth) {
        if (YearMonth.from(today).equals(yearMonth)) {
            return today;
        }
        return yearMonth.atDay(1);
    }

    /**
     * 주간 일정을 날짜별로 그룹핑한 맵을 생성합니다.
     *
     * <p>일정이 있는 날짜만 포함하며, 날짜 순서가 보장됩니다.
     *
     * @param weekStart 주 시작일
     * @param weekEnd   주 종료일
     * @return 날짜별 일정 맵 (일정 있는 날짜만 포함, 날짜순 정렬)
     */
    private Map<LocalDate, List<ScheduleResponse>> buildWeeklyScheduleMap(final LocalDate weekStart,
                                                                          final LocalDate weekEnd) {
        final List<ScheduleResponse> weekSchedules = scheduleService.findByWeek(weekStart, weekEnd);
        final Map<LocalDate, List<ScheduleResponse>> scheduleMap = new LinkedHashMap<>();

        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            final LocalDate currentDate = date;
            final List<ScheduleResponse> daySchedules = weekSchedules.stream()
                    .filter(s -> overlapsDate(s, currentDate))
                    .toList();
            if (!daySchedules.isEmpty()) {
                scheduleMap.put(currentDate, daySchedules);
            }
        }
        return scheduleMap;
    }

    /**
     * 일정이 특정 날짜와 겹치는지 판별합니다.
     *
     * @param schedule 판별 대상 일정
     * @param date     대상 날짜
     * @return 겹치면 true
     */
    private boolean overlapsDate(final ScheduleResponse schedule, final LocalDate date) {
        final LocalDate start = schedule.startDate();
        if (start == null || start.isAfter(date)) {
            return false;
        }
        final LocalDate end = schedule.endDate();
        if (end != null) {
            return !end.isBefore(date);
        }
        return start.equals(date);
    }
}
