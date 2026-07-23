package com.myapps.web.mycalendar.application.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myapps.web.mycalendar.application.dto.CommentCreateCommand;
import com.myapps.web.mycalendar.application.dto.CommentResponse;
import com.myapps.web.mycalendar.application.dto.CommentUpdateCommand;
import com.myapps.web.mycalendar.application.exception.CommentNotFoundException;
import com.myapps.web.mycalendar.application.exception.InvalidCommentException;
import com.myapps.web.mycalendar.application.exception.ScheduleNotFoundException;
import com.myapps.web.mycalendar.domain.model.Author;
import com.myapps.web.mycalendar.domain.model.Category;
import com.myapps.web.mycalendar.domain.model.Schedule;
import com.myapps.web.mycalendar.domain.model.ScheduleComment;
import com.myapps.web.mycalendar.domain.repository.CommentRepository;
import com.myapps.web.mycalendar.domain.repository.ScheduleRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CommentService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private CommentRepository commentRepository;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(scheduleRepository, commentRepository);
    }

    @Test
    void should_createComment_when_validCommand() {
        // given
        final Schedule schedule = new Schedule(Category.DATE, java.time.LocalDate.of(2026, 7, 1), "데이트");
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        final CommentCreateCommand command = new CommentCreateCommand(Author.SEUNGKWON, "좋아요!");
        final ScheduleComment savedComment = new ScheduleComment(Author.SEUNGKWON, "좋아요!");
        when(commentRepository.save(any(ScheduleComment.class))).thenReturn(savedComment);

        // when
        final CommentResponse response = commentService.create(1L, command);

        // then
        assertThat(response.author()).isEqualTo(Author.SEUNGKWON);
        assertThat(response.content()).isEqualTo("좋아요!");
        verify(commentRepository).save(any(ScheduleComment.class));
    }

    @Test
    void should_throwScheduleNotFoundException_when_scheduleNotExists() {
        // given
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());
        final CommentCreateCommand command = new CommentCreateCommand(Author.CHIWON, "댓글 내용");

        // when & then
        assertThatThrownBy(() -> commentService.create(999L, command))
                .isInstanceOf(ScheduleNotFoundException.class)
                .hasMessage("일정을 찾을 수 없습니다: ID=999");
    }

    @Test
    void should_throwInvalidCommentException_when_authorIsNull() {
        // given
        final CommentCreateCommand command = new CommentCreateCommand(null, "댓글 내용");

        // when & then
        assertThatThrownBy(() -> commentService.create(1L, command))
                .isInstanceOf(InvalidCommentException.class)
                .hasMessage("작성자를 선택해주세요");
    }

    @Test
    void should_throwInvalidCommentException_when_contentIsNull() {
        // given
        final CommentCreateCommand command = new CommentCreateCommand(Author.SEUNGKWON, null);

        // when & then
        assertThatThrownBy(() -> commentService.create(1L, command))
                .isInstanceOf(InvalidCommentException.class)
                .hasMessage("내용을 입력해주세요");
    }

    @Test
    void should_throwInvalidCommentException_when_contentIsBlank() {
        // given
        final CommentCreateCommand command = new CommentCreateCommand(Author.SEUNGKWON, "   ");

        // when & then
        assertThatThrownBy(() -> commentService.create(1L, command))
                .isInstanceOf(InvalidCommentException.class)
                .hasMessage("내용을 입력해주세요");
    }

    @Test
    void should_throwInvalidCommentException_when_contentExceeds200() {
        // given
        final String longContent = "가".repeat(201);
        final CommentCreateCommand command = new CommentCreateCommand(Author.SEUNGKWON, longContent);

        // when & then
        assertThatThrownBy(() -> commentService.create(1L, command))
                .isInstanceOf(InvalidCommentException.class)
                .hasMessage("200자를 초과할 수 없습니다");
    }

    @Test
    void should_returnCommentList_when_findByScheduleId() {
        // given
        final ScheduleComment comment1 = new ScheduleComment(Author.SEUNGKWON, "첫 번째 댓글");
        final ScheduleComment comment2 = new ScheduleComment(Author.CHIWON, "두 번째 댓글");
        when(commentRepository.findByScheduleIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(comment1, comment2));

        // when
        final List<CommentResponse> responses = commentService.findByScheduleId(1L);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).content()).isEqualTo("첫 번째 댓글");
        assertThat(responses.get(1).content()).isEqualTo("두 번째 댓글");
    }

    @Test
    void should_returnEmptyList_when_noCommentsFound() {
        // given
        when(commentRepository.findByScheduleIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        // when
        final List<CommentResponse> responses = commentService.findByScheduleId(1L);

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    void should_updateComment_when_validCommand() {
        // given
        final ScheduleComment comment = new ScheduleComment(Author.SEUNGKWON, "원본 댓글");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        final CommentUpdateCommand command = new CommentUpdateCommand("수정된 댓글");

        // when
        final CommentResponse response = commentService.update(1L, command);

        // then
        assertThat(response.content()).isEqualTo("수정된 댓글");
        assertThat(response.author()).isEqualTo(Author.SEUNGKWON);
    }

    @Test
    void should_throwCommentNotFoundException_when_updateNonExistentComment() {
        // given
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());
        final CommentUpdateCommand command = new CommentUpdateCommand("수정 내용");

        // when & then
        assertThatThrownBy(() -> commentService.update(999L, command))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessage("댓글을 찾을 수 없습니다: ID=999");
    }

    @Test
    void should_throwInvalidCommentException_when_updateContentIsNull() {
        // given
        final CommentUpdateCommand command = new CommentUpdateCommand(null);

        // when & then
        assertThatThrownBy(() -> commentService.update(1L, command))
                .isInstanceOf(InvalidCommentException.class)
                .hasMessage("내용을 입력해주세요");
    }

    @Test
    void should_throwInvalidCommentException_when_updateContentIsBlank() {
        // given
        final CommentUpdateCommand command = new CommentUpdateCommand("   ");

        // when & then
        assertThatThrownBy(() -> commentService.update(1L, command))
                .isInstanceOf(InvalidCommentException.class)
                .hasMessage("내용을 입력해주세요");
    }

    @Test
    void should_throwInvalidCommentException_when_updateContentExceeds200() {
        // given
        final String longContent = "가".repeat(201);
        final CommentUpdateCommand command = new CommentUpdateCommand(longContent);

        // when & then
        assertThatThrownBy(() -> commentService.update(1L, command))
                .isInstanceOf(InvalidCommentException.class)
                .hasMessage("200자를 초과할 수 없습니다");
    }

    @Test
    void should_deleteComment_when_exists() {
        // given
        final Schedule schedule = new Schedule(Category.DATE, java.time.LocalDate.of(2026, 7, 1), "데이트");
        final ScheduleComment comment = new ScheduleComment(Author.SEUNGKWON, "삭제 대상 댓글");
        schedule.addComment(comment);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        // when
        commentService.delete(1L);

        // then
        assertThat(schedule.getComments()).doesNotContain(comment);
    }

    @Test
    void should_throwCommentNotFoundException_when_deleteNonExistentComment() {
        // given
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.delete(999L))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessage("댓글을 찾을 수 없습니다: ID=999");
    }
}
