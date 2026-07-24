package com.myapps.web.mycalendar.interfaces.api;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.myapps.web.mycalendar.application.dto.ScheduleCreateCommand;
import com.myapps.web.mycalendar.application.dto.ScheduleResponse;
import com.myapps.web.mycalendar.application.dto.ScheduleUpdateCommand;
import com.myapps.web.mycalendar.application.service.ScheduleService;
import com.myapps.web.mycalendar.domain.model.Category;
import com.myapps.web.mycalendar.interfaces.dto.ScheduleForm;

/**
 * 일정 CRUD를 처리하는 컨트롤러.
 *
 * <p>일정 생성, 조회, 수정, 삭제 요청을 처리하며
 * HTMX 부분 렌더링을 지원합니다.
 */
@Controller
@RequestMapping("/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * ScheduleController를 생성합니다.
     *
     * @param scheduleService 일정 서비스
     */
    public ScheduleController(final ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * 일정 상세 정보를 JSON으로 반환합니다.
     *
     * <p>캘린더 모달에서 일정 상세를 표시하기 위해 사용됩니다.
     * 일정이 존재하지 않는 경우 404 응답을 반환합니다.
     *
     * @param id 조회할 일정 ID
     * @return 일정 응답 JSON 또는 404
     */
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ScheduleResponse> detail(@PathVariable("id") final Long id) {
        final ScheduleResponse schedule = scheduleService.findById(id);
        return ResponseEntity.ok(schedule);
    }

    /**
     * 일정 생성 폼을 렌더링합니다.
     *
     * <p>선택적으로 {@code startDate} 쿼리 파라미터를 받아 폼의 시작일 필드를 미리 설정합니다.
     * 캘린더 모달에서 날짜 셀 클릭 시 해당 날짜가 전달됩니다.
     *
     * @param startDate 시작일 미리 설정 값 (yyyy-MM-dd 형식, 선택)
     * @param model     뷰에 전달할 모델
     * @return 일정 폼 뷰 이름
     */
    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) final String startDate,
                          final Model model) {
        final LocalDate parsedStartDate = parseStartDate(startDate);
        model.addAttribute("scheduleForm",
                new ScheduleForm(null, parsedStartDate, null, null, null));
        model.addAttribute("scheduleId", null);
        model.addAttribute("categories", Category.values());
        return "schedule-form";
    }

    /**
     * 일정을 생성하고 캘린더 뷰로 리다이렉트합니다.
     *
     * @param form 폼에서 바인딩된 일정 데이터
     * @return 캘린더 뷰 리다이렉트 URL
     */
    @PostMapping
    public String create(@ModelAttribute final ScheduleForm form) {
        final ScheduleCreateCommand command = toCreateCommand(form);
        scheduleService.create(command);
        return buildCalendarRedirect(form);
    }

    /**
     * 일정 수정 폼을 렌더링합니다.
     *
     * @param id    수정할 일정 ID
     * @param model 뷰에 전달할 모델
     * @return 일정 폼 뷰 이름
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") final Long id, final Model model) {
        final ScheduleResponse schedule = scheduleService.findById(id);
        final ScheduleForm form = toScheduleForm(schedule);
        model.addAttribute("scheduleForm", form);
        model.addAttribute("scheduleId", id);
        model.addAttribute("categories", Category.values());
        return "schedule-form";
    }

    /**
     * 일정을 수정하고 캘린더 뷰로 리다이렉트합니다.
     *
     * @param id   수정할 일정 ID
     * @param form 폼에서 바인딩된 일정 데이터
     * @return 캘린더 뷰 리다이렉트 URL
     */
    @PutMapping("/{id}")
    public String update(@PathVariable("id") final Long id,
                         @ModelAttribute final ScheduleForm form) {
        final ScheduleUpdateCommand command = toUpdateCommand(form);
        scheduleService.update(id, command);
        return buildCalendarRedirect(form);
    }

    /**
     * 일정을 삭제하고 캘린더 뷰로 리다이렉트합니다.
     *
     * @param id 삭제할 일정 ID
     * @return 캘린더 뷰 리다이렉트 URL
     */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable("id") final Long id) {
        final ScheduleResponse schedule = scheduleService.findById(id);
        scheduleService.delete(id);
        if (schedule.startDate() != null) {
            final int year = schedule.startDate().getYear();
            final int month = schedule.startDate().getMonthValue();
            return "redirect:/calendar/" + year + "/" + month;
        }
        return "redirect:/";
    }

    private ScheduleCreateCommand toCreateCommand(final ScheduleForm form) {
        return new ScheduleCreateCommand(
                form.category(),
                form.startDate(),
                form.endDate(),
                form.scheduleTime(),
                form.content()
        );
    }

    private ScheduleUpdateCommand toUpdateCommand(final ScheduleForm form) {
        return new ScheduleUpdateCommand(
                form.category(),
                form.startDate(),
                form.endDate(),
                form.scheduleTime(),
                form.content()
        );
    }

    private ScheduleForm toScheduleForm(final ScheduleResponse response) {
        return new ScheduleForm(
                response.category(),
                response.startDate(),
                response.endDate(),
                response.scheduleTime(),
                response.content()
        );
    }

    private String buildCalendarRedirect(final ScheduleForm form) {
        if (form.startDate() != null) {
            final int year = form.startDate().getYear();
            final int month = form.startDate().getMonthValue();
            return "redirect:/calendar/" + year + "/" + month;
        }
        return "redirect:/";
    }

    private LocalDate parseStartDate(final String startDate) {
        if (startDate == null || startDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(startDate);
    }
}
