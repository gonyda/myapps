package com.myapps.web.mycalendar.interfaces.api;

import com.myapps.web.mycalendar.application.dto.CommentCreateCommand;
import com.myapps.web.mycalendar.application.dto.CommentResponse;
import com.myapps.web.mycalendar.application.dto.CommentUpdateCommand;
import com.myapps.web.mycalendar.application.service.CommentService;
import com.myapps.web.mycalendar.domain.model.Author;
import com.myapps.web.mycalendar.interfaces.dto.CommentForm;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 댓글 CRUD를 처리하는 컨트롤러.
 *
 * <p>댓글 생성, 수정 폼 표시, 수정 처리, 삭제 기능을 제공합니다.
 * 모든 작업 완료 후 해당 일정 상세 페이지로 리다이렉트합니다.
 */
@Controller
public class CommentController {

    private final CommentService commentService;

    /**
     * CommentController를 생성합니다.
     *
     * @param commentService 댓글 서비스
     */
    public CommentController(final CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 특정 일정에 새 댓글을 생성합니다.
     *
     * @param scheduleId  댓글을 추가할 일정 ID
     * @param commentForm 폼에서 바인딩된 댓글 데이터
     * @return 일정 상세 페이지로의 리다이렉트 경로
     */
    @PostMapping("/schedules/{scheduleId}/comments")
    public String create(@PathVariable("scheduleId") final Long scheduleId,
                         @ModelAttribute final CommentForm commentForm) {
        final CommentCreateCommand command =
                new CommentCreateCommand(commentForm.author(), commentForm.content());
        commentService.create(scheduleId, command);
        return "redirect:/schedules/" + scheduleId;
    }

    /**
     * 댓글 수정 폼을 표시합니다.
     *
     * <p>현재 댓글의 내용을 폼에 미리 채워서 보여줍니다.
     *
     * @param id    수정할 댓글 ID
     * @param model 뷰에 전달할 모델
     * @return 댓글 수정 폼 뷰 이름
     */
    @GetMapping("/comments/{id}/edit")
    public String editForm(@PathVariable("id") final Long id, final Model model) {
        final CommentResponse comment = commentService.findById(id);
        final Long scheduleId = commentService.findScheduleIdByCommentId(id);

        final CommentForm commentForm = new CommentForm(comment.author(), comment.content());
        model.addAttribute("commentForm", commentForm);
        model.addAttribute("commentId", id);
        model.addAttribute("scheduleId", scheduleId);
        model.addAttribute("authors", Author.values());

        return "comment-form";
    }

    /**
     * 댓글을 수정합니다.
     *
     * @param id          수정할 댓글 ID
     * @param scheduleId  댓글이 속한 일정 ID (리다이렉트용)
     * @param commentForm 폼에서 바인딩된 수정 데이터
     * @return 일정 상세 페이지로의 리다이렉트 경로
     */
    @PutMapping("/comments/{id}")
    public String update(@PathVariable("id") final Long id,
                         @RequestParam("scheduleId") final Long scheduleId,
                         @ModelAttribute final CommentForm commentForm) {
        final CommentUpdateCommand command = new CommentUpdateCommand(commentForm.content());
        commentService.update(id, command);
        return "redirect:/schedules/" + scheduleId;
    }

    /**
     * 댓글을 삭제합니다.
     *
     * @param id         삭제할 댓글 ID
     * @param scheduleId 댓글이 속한 일정 ID (리다이렉트용)
     * @return 일정 상세 페이지로의 리다이렉트 경로
     */
    @DeleteMapping("/comments/{id}")
    public String delete(@PathVariable("id") final Long id,
                         @RequestParam("scheduleId") final Long scheduleId) {
        commentService.delete(id);
        return "redirect:/schedules/" + scheduleId;
    }
}
