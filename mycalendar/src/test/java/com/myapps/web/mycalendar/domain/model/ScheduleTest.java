package com.myapps.web.mycalendar.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schedule 엔티티의 생성, 필드 업데이트, 댓글 양방향 연관관계 관리를 검증합니다.
 */
class ScheduleTest {

    @Test
    @DisplayName("필수 필드로 일정을 생성하면 해당 필드가 올바르게 설정된다")
    void should_createSchedule_when_requiredFieldsProvided() {
        final Schedule schedule = new Schedule(Category.DATE, LocalDate.of(2026, 7, 1), "데이트 일정");

        assertThat(schedule.getCategory()).isEqualTo(Category.DATE);
        assertThat(schedule.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(schedule.getContent()).isEqualTo("데이트 일정");
        assertThat(schedule.getEndDate()).isNull();
        assertThat(schedule.getScheduleTime()).isNull();
        assertThat(schedule.getComments()).isEmpty();
    }

    @Test
    @DisplayName("일정 필드를 업데이트하면 값이 변경된다")
    void should_updateFields_when_updateMethodsCalled() {
        final Schedule schedule = new Schedule(Category.SEUNGKWON, LocalDate.of(2026, 8, 1), "원래 내용");

        schedule.updateCategory(Category.CHIWON);
        schedule.updateContent("수정된 내용");
        schedule.updateStartDate(LocalDate.of(2026, 8, 5));
        schedule.updateEndDate(LocalDate.of(2026, 8, 10));
        schedule.updateScheduleTime(LocalTime.of(14, 30));

        assertThat(schedule.getCategory()).isEqualTo(Category.CHIWON);
        assertThat(schedule.getContent()).isEqualTo("수정된 내용");
        assertThat(schedule.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(schedule.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(schedule.getScheduleTime()).isEqualTo(LocalTime.of(14, 30));
    }

    @Test
    @DisplayName("@PrePersist 콜백이 createdAt과 updatedAt을 설정한다")
    void should_setTimestamps_when_onCreateCalled() {
        final Schedule schedule = new Schedule(Category.DATE, LocalDate.of(2026, 7, 1), "테스트");

        schedule.onCreate();

        assertThat(schedule.getCreatedAt()).isNotNull();
        assertThat(schedule.getUpdatedAt()).isNotNull();
        assertThat(schedule.getCreatedAt()).isEqualTo(schedule.getUpdatedAt());
    }

    @Test
    @DisplayName("@PreUpdate 콜백이 updatedAt을 갱신한다")
    void should_refreshUpdatedAt_when_onUpdateCalled() {
        final Schedule schedule = new Schedule(Category.DATE, LocalDate.of(2026, 7, 1), "테스트");
        schedule.onCreate();

        schedule.onUpdate();

        assertThat(schedule.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("addComment로 댓글을 추가하면 양방향 연관관계가 설정된다")
    void should_setRelationship_when_commentAdded() {
        final Schedule schedule = new Schedule(Category.SEUNGKWON, LocalDate.of(2026, 7, 1), "일정");
        final ScheduleComment comment = new ScheduleComment(Author.CHIWON, "좋아!");

        schedule.addComment(comment);

        final List<ScheduleComment> comments = schedule.getComments();
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0)).isEqualTo(comment);
        assertThat(comment.getSchedule()).isEqualTo(schedule);
    }

    @Test
    @DisplayName("removeComment로 댓글을 제거하면 양방향 연관관계가 해제된다")
    void should_clearRelationship_when_commentRemoved() {
        final Schedule schedule = new Schedule(Category.SEUNGKWON, LocalDate.of(2026, 7, 1), "일정");
        final ScheduleComment comment = new ScheduleComment(Author.CHIWON, "삭제될 댓글");
        schedule.addComment(comment);

        schedule.removeComment(comment);

        assertThat(schedule.getComments()).isEmpty();
        assertThat(comment.getSchedule()).isNull();
    }
}
