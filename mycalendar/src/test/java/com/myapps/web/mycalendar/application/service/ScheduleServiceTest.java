package com.myapps.web.mycalendar.application.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myapps.web.mycalendar.application.dto.ScheduleCreateCommand;
import com.myapps.web.mycalendar.application.dto.ScheduleResponse;
import com.myapps.web.mycalendar.application.dto.ScheduleUpdateCommand;
import com.myapps.web.mycalendar.application.exception.InvalidScheduleException;
import com.myapps.web.mycalendar.application.exception.ScheduleNotFoundException;
import com.myapps.web.mycalendar.domain.model.Category;
import com.myapps.web.mycalendar.domain.model.Schedule;
import com.myapps.web.mycalendar.domain.repository.ScheduleRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScheduleService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(scheduleRepository);
    }

    @Test
    void should_createSchedule_when_validCommand() {
        // given
        final ScheduleCreateCommand command = new ScheduleCreateCommand(
                Category.DATE, LocalDate.of(2026, 7, 1), null, LocalTime.of(18, 0), "저녁 데이트"
        );
        final Schedule savedSchedule = new Schedule(Category.DATE, LocalDate.of(2026, 7, 1), "저녁 데이트");
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(savedSchedule);

        // when
        final ScheduleResponse response = scheduleService.create(command);

        // then
        assertThat(response.category()).isEqualTo(Category.DATE);
        assertThat(response.content()).isEqualTo("저녁 데이트");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        verify(scheduleRepository).save(any(Schedule.class));
    }

    @Test
    void should_createMultiDaySchedule_when_endDateProvided() {
        // given
        final ScheduleCreateCommand command = new ScheduleCreateCommand(
                Category.SEUNGKWON, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3),
                null, "출장"
        );
        final Schedule savedSchedule = new Schedule(Category.SEUNGKWON, LocalDate.of(2026, 7, 1), "출장");
        savedSchedule.updateEndDate(LocalDate.of(2026, 7, 3));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(savedSchedule);

        // when
        final ScheduleResponse response = scheduleService.create(command);

        // then
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 7, 3));
    }

    @Test
    void should_throwInvalidScheduleException_when_contentIsNull() {
        // given
        final ScheduleCreateCommand command = new ScheduleCreateCommand(
                Category.DATE, LocalDate.of(2026, 7, 1), null, null, null
        );

        // when & then
        assertThatThrownBy(() -> scheduleService.create(command))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessage("내용을 입력해주세요");
    }

    @Test
    void should_throwInvalidScheduleException_when_contentIsBlank() {
        // given
        final ScheduleCreateCommand command = new ScheduleCreateCommand(
                Category.DATE, LocalDate.of(2026, 7, 1), null, null, "   "
        );

        // when & then
        assertThatThrownBy(() -> scheduleService.create(command))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessage("내용을 입력해주세요");
    }

    @Test
    void should_throwInvalidScheduleException_when_contentExceeds200() {
        // given
        final String longContent = "가".repeat(201);
        final ScheduleCreateCommand command = new ScheduleCreateCommand(
                Category.DATE, LocalDate.of(2026, 7, 1), null, null, longContent
        );

        // when & then
        assertThatThrownBy(() -> scheduleService.create(command))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessage("200자를 초과할 수 없습니다");
    }

    @Test
    void should_throwInvalidScheduleException_when_categoryIsNull() {
        // given
        final ScheduleCreateCommand command = new ScheduleCreateCommand(
                null, LocalDate.of(2026, 7, 1), null, null, "일정 내용"
        );

        // when & then
        assertThatThrownBy(() -> scheduleService.create(command))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessage("카테고리를 선택해주세요");
    }

    @Test
    void should_throwInvalidScheduleException_when_startDateIsNull() {
        // given
        final ScheduleCreateCommand command = new ScheduleCreateCommand(
                Category.DATE, null, null, null, "일정 내용"
        );

        // when & then
        assertThatThrownBy(() -> scheduleService.create(command))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessage("시작 날짜를 입력해주세요");
    }

    @Test
    void should_throwInvalidScheduleException_when_endDateBeforeStartDate() {
        // given
        final ScheduleCreateCommand command = new ScheduleCreateCommand(
                Category.DATE, LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 3), null, "일정 내용"
        );

        // when & then
        assertThatThrownBy(() -> scheduleService.create(command))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessage("종료 날짜는 시작 날짜 이후여야 합니다");
    }

    @Test
    void should_returnScheduleResponse_when_findById() {
        // given
        final Schedule schedule = new Schedule(Category.CHIWON, LocalDate.of(2026, 8, 1), "치원 일정");
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        // when
        final ScheduleResponse response = scheduleService.findById(1L);

        // then
        assertThat(response.category()).isEqualTo(Category.CHIWON);
        assertThat(response.content()).isEqualTo("치원 일정");
    }

    @Test
    void should_throwScheduleNotFoundException_when_idNotFound() {
        // given
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> scheduleService.findById(999L))
                .isInstanceOf(ScheduleNotFoundException.class)
                .hasMessage("일정을 찾을 수 없습니다: ID=999");
    }

    @Test
    void should_returnScheduleList_when_findByMonth() {
        // given
        final YearMonth yearMonth = YearMonth.of(2026, 7);
        final LocalDate startOfMonth = LocalDate.of(2026, 7, 1);
        final LocalDate endOfMonth = LocalDate.of(2026, 7, 31);
        final Schedule schedule = new Schedule(Category.DATE, LocalDate.of(2026, 7, 15), "데이트");
        when(scheduleRepository.findByMonth(startOfMonth, endOfMonth)).thenReturn(List.of(schedule));

        // when
        final List<ScheduleResponse> responses = scheduleService.findByMonth(yearMonth);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).content()).isEqualTo("데이트");
    }

    @Test
    void should_updateSchedule_when_validCommand() {
        // given
        final Schedule schedule = new Schedule(Category.DATE, LocalDate.of(2026, 7, 1), "원본 내용");
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        final ScheduleUpdateCommand command = new ScheduleUpdateCommand(
                Category.SEUNGKWON, LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 5),
                LocalTime.of(10, 0), "수정된 내용"
        );

        // when
        final ScheduleResponse response = scheduleService.update(1L, command);

        // then
        assertThat(response.category()).isEqualTo(Category.SEUNGKWON);
        assertThat(response.content()).isEqualTo("수정된 내용");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 7, 2));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 7, 5));
    }

    @Test
    void should_throwScheduleNotFoundException_when_updateNonExistentSchedule() {
        // given
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());
        final ScheduleUpdateCommand command = new ScheduleUpdateCommand(
                Category.DATE, LocalDate.of(2026, 7, 1), null, null, "수정 내용"
        );

        // when & then
        assertThatThrownBy(() -> scheduleService.update(999L, command))
                .isInstanceOf(ScheduleNotFoundException.class)
                .hasMessage("일정을 찾을 수 없습니다: ID=999");
    }

    @Test
    void should_returnScheduleList_when_findByWeek() {
        // given
        final LocalDate weekStart = LocalDate.of(2026, 7, 5);
        final LocalDate weekEnd = LocalDate.of(2026, 7, 11);
        final Schedule schedule = new Schedule(Category.DATE, LocalDate.of(2026, 7, 8), "주간 일정");
        when(scheduleRepository.findByWeek(weekStart, weekEnd)).thenReturn(List.of(schedule));

        // when
        final List<ScheduleResponse> responses = scheduleService.findByWeek(weekStart, weekEnd);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).content()).isEqualTo("주간 일정");
        assertThat(responses.get(0).startDate()).isEqualTo(LocalDate.of(2026, 7, 8));
        verify(scheduleRepository).findByWeek(weekStart, weekEnd);
    }

    @Test
    void should_returnEmptyList_when_noSchedulesInWeek() {
        // given
        final LocalDate weekStart = LocalDate.of(2026, 8, 3);
        final LocalDate weekEnd = LocalDate.of(2026, 8, 9);
        when(scheduleRepository.findByWeek(weekStart, weekEnd)).thenReturn(List.of());

        // when
        final List<ScheduleResponse> responses = scheduleService.findByWeek(weekStart, weekEnd);

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    void should_deleteSchedule_when_exists() {
        // given
        final Schedule schedule = new Schedule(Category.DATE, LocalDate.of(2026, 7, 1), "삭제 대상");
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        // when
        scheduleService.delete(1L);

        // then
        verify(scheduleRepository).delete(schedule);
    }

    @Test
    void should_throwScheduleNotFoundException_when_deleteNonExistentSchedule() {
        // given
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> scheduleService.delete(999L))
                .isInstanceOf(ScheduleNotFoundException.class)
                .hasMessage("일정을 찾을 수 없습니다: ID=999");
    }
}
