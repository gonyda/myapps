package com.myapps.web.mycalendar.application.service;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.mycalendar.application.dto.CommentCreateCommand;
import com.myapps.web.mycalendar.application.dto.CommentResponse;
import com.myapps.web.mycalendar.application.dto.CommentUpdateCommand;
import com.myapps.web.mycalendar.application.exception.InvalidCommentException;
import com.myapps.web.mycalendar.domain.model.Author;
import com.myapps.web.mycalendar.domain.model.Category;
import com.myapps.web.mycalendar.domain.model.Schedule;
import com.myapps.web.mycalendar.domain.model.ScheduleComment;
import com.myapps.web.mycalendar.domain.repository.CommentRepository;
import com.myapps.web.mycalendar.domain.repository.ScheduleRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CommentService에 대한 Property-Based 테스트.
 *
 * <p>jqwik을 사용하여 댓글 내용 길이 검증, 공백 내용 거부,
 * 댓글 목록 시간순 정렬을 검증합니다.
 *
 * <p>Validates: Requirements 6.3, 6.4, 6.6, 7.5, 7.6
 */
class CommentServicePropertyTest {

    private static final int MAX_CONTENT_LENGTH = 200;

    // Feature: mycalendar/001-couple-calendar, Property 2: Content length validation (댓글)

    /**
     * Property 2: content 길이가 200자를 초과하면 댓글 생성이 거부되어야 한다.
     *
     * <p>**Validates: Requirements 6.4, 7.5**
     */
    @Property(tries = 100)
    void createMustRejectContentExceedingMaxLength(
            @ForAll("overLengthContent") final String content) {

        final ScheduleRepository mockScheduleRepo = mock(ScheduleRepository.class);
        final CommentRepository mockCommentRepo = mock(CommentRepository.class);
        final CommentService service = new CommentService(mockScheduleRepo, mockCommentRepo);

        final Schedule schedule = new Schedule(Category.DATE, LocalDate.of(2026, 7, 1), "테스트 일정");
        when(mockScheduleRepo.findById(1L)).thenReturn(Optional.of(schedule));

        final CommentCreateCommand command = new CommentCreateCommand(Author.SEUNGKWON, content);

        assertThrows(InvalidCommentException.class, () -> service.create(1L, command));
    }

    /**
     * Property 2: content 길이가 200자를 초과하면 댓글 수정이 거부되어야 한다.
     *
     * <p>**Validates: Requirements 7.5, 7.6**
     */
    @Property(tries = 100)
    void updateMustRejectContentExceedingMaxLength(
            @ForAll("overLengthContent") final String content) {

        final ScheduleRepository mockScheduleRepo = mock(ScheduleRepository.class);
        final CommentRepository mockCommentRepo = mock(CommentRepository.class);
        final CommentService service = new CommentService(mockScheduleRepo, mockCommentRepo);

        final CommentUpdateCommand command = new CommentUpdateCommand(content);

        assertThrows(InvalidCommentException.class, () -> service.update(1L, command));
    }

    // Feature: mycalendar/001-couple-calendar, Property 5: Whitespace content rejection (댓글)

    /**
     * Property 5: content가 공백 문자만으로 구성되면 댓글 생성이 거부되어야 한다.
     *
     * <p>**Validates: Requirements 6.6**
     */
    @Property(tries = 100)
    void createMustRejectWhitespaceOnlyContent(
            @ForAll("whitespaceOnlyContent") final String content) {

        final ScheduleRepository mockScheduleRepo = mock(ScheduleRepository.class);
        final CommentRepository mockCommentRepo = mock(CommentRepository.class);
        final CommentService service = new CommentService(mockScheduleRepo, mockCommentRepo);

        final CommentCreateCommand command = new CommentCreateCommand(Author.CHIWON, content);

        assertThrows(InvalidCommentException.class, () -> service.create(1L, command));
    }

    /**
     * Property 5: content가 공백 문자만으로 구성되면 댓글 수정이 거부되어야 한다.
     *
     * <p>**Validates: Requirements 7.6**
     */
    @Property(tries = 100)
    void updateMustRejectWhitespaceOnlyContent(
            @ForAll("whitespaceOnlyContent") final String content) {

        final ScheduleRepository mockScheduleRepo = mock(ScheduleRepository.class);
        final CommentRepository mockCommentRepo = mock(CommentRepository.class);
        final CommentService service = new CommentService(mockScheduleRepo, mockCommentRepo);

        final CommentUpdateCommand command = new CommentUpdateCommand(content);

        assertThrows(InvalidCommentException.class, () -> service.update(1L, command));
    }

    // Feature: mycalendar/001-couple-calendar, Property 8: Comment ordering

    /**
     * Property 8: 댓글 목록을 조회하면 createdAt 오름차순으로 정렬되어야 한다.
     *
     * <p>**Validates: Requirements 6.3**
     */
    @Property(tries = 100)
    void findByScheduleIdMustReturnCommentsOrderedByCreatedAtAsc(
            @ForAll("commentCounts") final int commentCount) {

        final ScheduleRepository mockScheduleRepo = mock(ScheduleRepository.class);
        final CommentRepository mockCommentRepo = mock(CommentRepository.class);
        final CommentService service = new CommentService(mockScheduleRepo, mockCommentRepo);

        final List<ScheduleComment> sortedComments = createSortedComments(commentCount);
        when(mockCommentRepo.findByScheduleIdOrderByCreatedAtAsc(1L)).thenReturn(sortedComments);

        final List<CommentResponse> responses = service.findByScheduleId(1L);

        for (int i = 1; i < responses.size(); i++) {
            final LocalDateTime previous = responses.get(i - 1).createdAt();
            final LocalDateTime current = responses.get(i).createdAt();
            assertTrue(
                    !previous.isAfter(current),
                    "댓글 목록은 createdAt 오름차순이어야 한다: "
                            + previous + " > " + current
            );
        }
    }

    // --- Helper Methods ---

    private List<ScheduleComment> createSortedComments(final int count) {
        final List<ScheduleComment> comments = new ArrayList<>();
        final LocalDateTime baseTime = LocalDateTime.of(2026, 7, 1, 10, 0, 0);

        for (int i = 0; i < count; i++) {
            final ScheduleComment comment = new ScheduleComment(
                    i % 2 == 0 ? Author.SEUNGKWON : Author.CHIWON,
                    "댓글 내용 " + (i + 1)
            );
            setCreatedAt(comment, baseTime.plusMinutes(i * 5L));
            comments.add(comment);
        }
        return comments;
    }

    private static void setCreatedAt(final ScheduleComment comment, final LocalDateTime createdAt) {
        try {
            final Field field = ScheduleComment.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(comment, createdAt);
        } catch (final Exception e) {
            throw new RuntimeException("createdAt 설정 실패", e);
        }
    }

    // --- Arbitrary Providers ---

    @Provide
    Arbitrary<String> overLengthContent() {
        return Arbitraries.integers().between(MAX_CONTENT_LENGTH + 1, MAX_CONTENT_LENGTH + 100)
                .map(length -> "가".repeat(length));
    }

    @Provide
    Arbitrary<String> whitespaceOnlyContent() {
        return Arbitraries.of(" ", "  ", "\t", "\n", " \t\n", "   \t   ");
    }

    @Provide
    Arbitrary<Integer> commentCounts() {
        return Arbitraries.integers().between(0, 20);
    }
}
