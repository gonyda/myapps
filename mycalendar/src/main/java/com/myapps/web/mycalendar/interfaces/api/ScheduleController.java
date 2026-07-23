package com.myapps.web.mycalendar.interfaces.api;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.myapps.web.mycalendar.application.dto.ScheduleCreateCommand;
import com.myapps.web.mycalendar.application.dto.ScheduleResponse;
import com.myapps.web.mycalendar.application.dto.ScheduleUpdateCommand;
import com.myapps.web.mycalendar.application.service.ScheduleService;
import com.myapps.web.mycalendar.domain.model.Category;
import com.myapps.web.mycalendar.interfaces.dto.CommentForm;
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
     * 일정 상세 조회 페이지를 렌더링합니다.
     *
     * @param id    조회할 일정 ID
     * @param model 뷰에 전달할 모델
     * @return 일정 상세 뷰 이름
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable("id") final Long id, final Model model) {
        final ScheduleResponse schedule = scheduleService.findById(id);
        model.addAttribute("schedule", schedule);
        model.addAttribute("commentForm", new CommentForm(null, null));
        model.addAttribute("categories", Category.values());
        return "schedule-detail";
    }

    /**
     * 일정 생성 폼을 렌더링합니다.
     *
     * @param model 뷰에 전달할 모델
     * @return 일정 폼 뷰 이름
     */
    @GetMapping("/new")
    public String newForm(final Model model) {
        model.addAttribute("scheduleForm",
                new ScheduleForm(null, null, null, null, null));
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
     * 일정을 수정하고 일정 상세 페이지로 리다이렉트합니다.
     *
     * @param id   수정할 일정 ID
     * @param form 폼에서 바인딩된 일정 데이터
     * @return 일정 상세 뷰 리다이렉트 URL
     */
    @PutMapping("/{id}")
    public String update(@PathVariable("id") final Long id,
                         @ModelAttribute final ScheduleForm form) {
        final ScheduleUpdateCommand command = toUpdateCommand(form);
        scheduleService.update(id, command);
        return "redirect:/schedules/" + id;
    }

    /**
     * 일정을 삭제하고 루트 페이지로 리다이렉트합니다.
     *
     * @param id 삭제할 일정 ID
     * @return 루트 페이지 리다이렉트 URL
     */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable("id") final Long id) {
        scheduleService.delete(id);
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
}
