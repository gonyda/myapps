package com.myapps.web.mycalendar.domain.repository;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestConstructor;

import com.myapps.web.mycalendar.domain.model.Author;
import com.myapps.web.mycalendar.domain.model.Category;
import com.myapps.web.mycalendar.domain.model.Schedule;
import com.myapps.web.mycalendar.domain.model.ScheduleComment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ScheduleRepository 통합 테스트.
 *
 * <p>{@code @DataJpaTest}를 활용한 슬라이스 테스트로, 월별 조회 쿼리와
 * cascade 삭제 동작을 검증합니다.
 */
@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ScheduleRepositoryTest {

    private final TestEntityManager entityManager;
    private final ScheduleRepository scheduleRepository;
    private final CommentRepository commentRepository;

    ScheduleRepositoryTest(final TestEntityManager entityManager,
                           final ScheduleRepository scheduleRepository,
                           final CommentRepository commentRepository) {
        this.entityManager = entityManager;
        this.scheduleRepository = scheduleRepository;
        this.commentRepository = commentRepository;
    }

    @Test
    @DisplayName("Single_Day_Schedule이 대상 월에 포함되면 월별 조회에 반환된다")
    void should_returnSchedule_when_singleDayScheduleInTargetMonth() {
        final Schedule schedule = new Schedule(Category.SEUNGKWON, LocalDate.of(2026, 7, 15), "7월 일정");
        entityManager.persistAndFlush(schedule);
        entityManager.clear();

        final LocalDate startOfMonth = LocalDate.of(2026, 7, 1);
        final LocalDate endOfMonth = LocalDate.of(2026, 7, 31);
        final List<Schedule> result = scheduleRepository.findByMonth(startOfMonth, endOfMonth);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("7월 일정");
    }

    @Test
    @DisplayName("Multi_Day_Schedule이 대상 월과 겹치면 월별 조회에 포함된다")
    void should_returnSchedule_when_multiDayScheduleOverlapsTargetMonth() {
        final Schedule schedule = new Schedule(Category.DATE, LocalDate.of(2026, 6, 25), "월 걸침 일정");
        schedule.updateEndDate(LocalDate.of(2026, 7, 5));
        entityManager.persistAndFlush(schedule);
        entityManager.clear();

        final LocalDate startOfMonth = LocalDate.of(2026, 7, 1);
        final LocalDate endOfMonth = LocalDate.of(2026, 7, 31);
        final List<Schedule> result = scheduleRepository.findByMonth(startOfMonth, endOfMonth);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("월 걸침 일정");
    }

    @Test
    @DisplayName("Multi_Day_Schedule이 대상 월과 겹치지 않으면 월별 조회에서 제외된다")
    void should_excludeSchedule_when_multiDayScheduleOutsideTargetMonth() {
        final Schedule schedule = new Schedule(Category.CHIWON, LocalDate.of(2026, 5, 10), "5월 일정");
        schedule.updateEndDate(LocalDate.of(2026, 5, 20));
        entityManager.persistAndFlush(schedule);
        entityManager.clear();

        final LocalDate startOfMonth = LocalDate.of(2026, 7, 1);
        final LocalDate endOfMonth = LocalDate.of(2026, 7, 31);
        final List<Schedule> result = scheduleRepository.findByMonth(startOfMonth, endOfMonth);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("다른 월의 Single_Day_Schedule은 월별 조회에서 제외된다")
    void should_excludeSchedule_when_singleDayScheduleInDifferentMonth() {
        final Schedule schedule = new Schedule(Category.SEUNGKWON, LocalDate.of(2026, 8, 10), "8월 일정");
        entityManager.persistAndFlush(schedule);
        entityManager.clear();

        final LocalDate startOfMonth = LocalDate.of(2026, 7, 1);
        final LocalDate endOfMonth = LocalDate.of(2026, 7, 31);
        final List<Schedule> result = scheduleRepository.findByMonth(startOfMonth, endOfMonth);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("일정 삭제 시 연관된 댓글도 cascade 삭제된다")
    void should_deleteComments_when_scheduleDeleted() {
        final Schedule schedule = new Schedule(Category.DATE, LocalDate.of(2026, 7, 1), "삭제 대상 일정");
        final ScheduleComment comment1 = new ScheduleComment(Author.SEUNGKWON, "첫 번째 댓글");
        final ScheduleComment comment2 = new ScheduleComment(Author.CHIWON, "두 번째 댓글");
        schedule.addComment(comment1);
        schedule.addComment(comment2);
        entityManager.persistAndFlush(schedule);
        final Long scheduleId = schedule.getId();
        entityManager.clear();

        scheduleRepository.deleteById(scheduleId);
        entityManager.flush();
        entityManager.clear();

        assertThat(scheduleRepository.findById(scheduleId)).isEmpty();
        final List<ScheduleComment> remainingComments =
                commentRepository.findByScheduleIdOrderByCreatedAtAsc(scheduleId);
        assertThat(remainingComments).isEmpty();
    }
}
