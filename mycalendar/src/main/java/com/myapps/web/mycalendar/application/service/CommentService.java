package com.myapps.web.mycalendar.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myapps.web.mycalendar.application.dto.CommentCreateCommand;
import com.myapps.web.mycalendar.application.dto.CommentResponse;
import com.myapps.web.mycalendar.application.dto.CommentUpdateCommand;
import com.myapps.web.mycalendar.application.exception.CommentNotFoundException;
import com.myapps.web.mycalendar.application.exception.InvalidCommentException;
import com.myapps.web.mycalendar.application.exception.ScheduleNotFoundException;
import com.myapps.web.mycalendar.domain.model.Schedule;
import com.myapps.web.mycalendar.domain.model.ScheduleComment;
import com.myapps.web.mycalendar.domain.repository.CommentRepository;
import com.myapps.web.mycalendar.domain.repository.ScheduleRepository;

/**
 * 댓글 생성/조회/수정/삭제 유스케이스를 오케스트레이션하는 서비스.
 *
 * <p>유효성 검증, 엔티티 변환, 리포지토리 위임을 담당합니다.
 */
@Service
@Transactional(readOnly = true)
public class CommentService {

    private static final int MAX_CONTENT_LENGTH = 200;

    private final ScheduleRepository scheduleRepository;
    private final CommentRepository commentRepository;

    /**
     * CommentService를 생성합니다.
     *
     * @param scheduleRepository 일정 리포지토리
     * @param commentRepository  댓글 리포지토리
     */
    public CommentService(final ScheduleRepository scheduleRepository,
                          final CommentRepository commentRepository) {
        this.scheduleRepository = scheduleRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * 유효성 검증 후 새 댓글을 저장합니다.
     *
     * <p>대상 일정의 {@link Schedule#addComment(ScheduleComment)} 메서드를 사용하여
     * 양방향 연관관계를 동기화합니다.
     *
     * @param scheduleId 댓글을 추가할 일정 ID
     * @param command    댓글 생성 커맨드
     * @return 생성된 댓글 응답
     * @throws ScheduleNotFoundException 해당 ID의 일정이 존재하지 않을 때
     * @throws InvalidCommentException   유효성 검증 실패 시
     */
    @Transactional
    public CommentResponse create(final Long scheduleId, final CommentCreateCommand command) {
        validateCreateCommand(command);

        final Schedule schedule = findScheduleById(scheduleId);
        final ScheduleComment comment = new ScheduleComment(command.author(), command.content());
        schedule.addComment(comment);

        final ScheduleComment savedComment = commentRepository.save(comment);
        return toResponse(savedComment);
    }

    /**
     * ID로 단일 댓글을 조회합니다.
     *
     * @param commentId 조회할 댓글의 ID
     * @return 댓글 응답
     * @throws CommentNotFoundException 해당 ID의 댓글이 존재하지 않을 때
     */
    public CommentResponse findById(final Long commentId) {
        final ScheduleComment comment = findCommentById(commentId);
        return toResponse(comment);
    }

    /**
     * 특정 일정에 속한 모든 댓글을 생성 시각 오름차순으로 조회합니다.
     *
     * @param scheduleId 조회할 일정의 ID
     * @return 생성 시각 오름차순으로 정렬된 댓글 응답 목록
     */
    public List<CommentResponse> findByScheduleId(final Long scheduleId) {
        final List<ScheduleComment> comments =
                commentRepository.findByScheduleIdOrderByCreatedAtAsc(scheduleId);
        return comments.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 유효성 검증 후 기존 댓글을 수정합니다.
     *
     * @param commentId 수정할 댓글 ID
     * @param command   댓글 수정 커맨드
     * @return 수정된 댓글 응답
     * @throws CommentNotFoundException 해당 ID의 댓글이 존재하지 않을 때
     * @throws InvalidCommentException  유효성 검증 실패 시
     */
    @Transactional
    public CommentResponse update(final Long commentId, final CommentUpdateCommand command) {
        validateContent(command.content());

        final ScheduleComment comment = findCommentById(commentId);
        comment.updateContent(command.content());

        return toResponse(comment);
    }

    /**
     * 댓글을 삭제합니다.
     *
     * <p>일정의 {@link Schedule#removeComment(ScheduleComment)} 메서드를 사용하여
     * 양방향 연관관계를 동기화합니다.
     *
     * @param commentId 삭제할 댓글 ID
     * @throws CommentNotFoundException 해당 ID의 댓글이 존재하지 않을 때
     */
    @Transactional
    public void delete(final Long commentId) {
        final ScheduleComment comment = findCommentById(commentId);
        final Schedule schedule = comment.getSchedule();
        schedule.removeComment(comment);
    }

    /**
     * 댓글이 속한 일정의 ID를 반환합니다.
     *
     * @param commentId 댓글 ID
     * @return 해당 댓글이 속한 일정의 ID
     * @throws CommentNotFoundException 해당 ID의 댓글이 존재하지 않을 때
     */
    public Long findScheduleIdByCommentId(final Long commentId) {
        final ScheduleComment comment = findCommentById(commentId);
        return comment.getSchedule().getId();
    }

    private Schedule findScheduleById(final Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ScheduleNotFoundException("일정을 찾을 수 없습니다: ID=" + id));
    }

    private ScheduleComment findCommentById(final Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new CommentNotFoundException("댓글을 찾을 수 없습니다: ID=" + id));
    }

    private void validateCreateCommand(final CommentCreateCommand command) {
        validateAuthor(command);
        validateContent(command.content());
    }

    private void validateAuthor(final CommentCreateCommand command) {
        if (command.author() == null) {
            throw new InvalidCommentException("작성자를 선택해주세요");
        }
    }

    private void validateContent(final String content) {
        if (content == null || content.isBlank()) {
            throw new InvalidCommentException("내용을 입력해주세요");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new InvalidCommentException("200자를 초과할 수 없습니다");
        }
    }

    private CommentResponse toResponse(final ScheduleComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthor(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
