package com.myapps.web.mycalendar.application.service;

import com.myapps.web.mycalendar.application.dto.ScheduleCreateCommand;
import com.myapps.web.mycalendar.application.dto.ScheduleResponse;
import com.myapps.web.mycalendar.application.dto.ScheduleUpdateCommand;
import com.myapps.web.mycalendar.application.exception.InvalidScheduleException;
import com.myapps.web.mycalendar.application.exception.ScheduleNotFoundException;
import com.myapps.web.mycalendar.domain.model.Schedule;
import com.myapps.web.mycalendar.domain.repository.ScheduleRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일정 생성/조회/수정/삭제 유스케이스를 오케스트레이션하는 서비스.
 *
 * <p>유효성 검증, 엔티티 변환, 리포지토리 위임을 담당합니다.
 */
@Service
@Transactional(readOnly = true)
public class ScheduleService {

    private static final int MAX_CONTENT_LENGTH = 200;

    private final ScheduleRepository scheduleRepository;

    /**
     * ScheduleService를 생성합니다.
     *
     * @param scheduleRepository 일정 리포지토리
     */
    public ScheduleService(final ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * 유효성 검증 후 새 일정을 저장합니다.
     *
     * @param command 일정 생성 커맨드
     * @return 생성된 일정 응답
     * @throws InvalidScheduleException 유효성 검증 실패 시
     */
    @Transactional
    public ScheduleResponse create(final ScheduleCreateCommand command) {
        validateCommand(
                command.content(), command.category(), command.startDate(), command.endDate());

        final Schedule schedule =
                new Schedule(command.category(), command.startDate(), command.content());
        schedule.updateEndDate(command.endDate());
        schedule.updateScheduleTime(command.scheduleTime());

        final Schedule savedSchedule = scheduleRepository.save(schedule);
        return toResponse(savedSchedule);
    }

    /**
     * ID로 일정을 조회합니다.
     *
     * @param id 일정 식별자
     * @return 일정 응답
     * @throws ScheduleNotFoundException 해당 ID의 일정이 존재하지 않을 때
     */
    public ScheduleResponse findById(final Long id) {
        final Schedule schedule = findScheduleById(id);
        return toResponse(schedule);
    }

    /**
     * 특정 월의 모든 일정을 조회합니다.
     *
     * <p>Multi_Day_Schedule의 날짜 범위가 조회 대상 월과 겹치는 경우도 포함됩니다.
     *
     * @param yearMonth 조회 대상 연월
     * @return 해당 월과 겹치는 일정 응답 목록
     */
    public List<ScheduleResponse> findByMonth(final YearMonth yearMonth) {
        final LocalDate startOfMonth = yearMonth.atDay(1);
        final LocalDate endOfMonth = yearMonth.atEndOfMonth();

        final List<Schedule> schedules = scheduleRepository.findByMonth(startOfMonth, endOfMonth);
        return schedules.stream().map(this::toResponse).toList();
    }

    /**
     * 특정 주간 범위의 일정을 시작일/시간순으로 조회합니다.
     *
     * @param weekStart 조회 대상 주의 시작일 (일요일)
     * @param weekEnd 조회 대상 주의 마지막 날 (토요일)
     * @return 해당 주와 겹치는 일정 응답 목록 (시작일, 시간순 정렬)
     */
    public List<ScheduleResponse> findByWeek(final LocalDate weekStart, final LocalDate weekEnd) {
        final List<Schedule> schedules = scheduleRepository.findByWeek(weekStart, weekEnd);
        return schedules.stream().map(this::toResponse).toList();
    }

    /**
     * 유효성 검증 후 기존 일정을 수정합니다.
     *
     * <p>수정 시 엔티티의 {@code @PreUpdate}에 의해 updatedAt이 자동 갱신됩니다.
     *
     * @param id 수정할 일정 ID
     * @param command 일정 수정 커맨드
     * @return 수정된 일정 응답
     * @throws ScheduleNotFoundException 해당 ID의 일정이 존재하지 않을 때
     * @throws InvalidScheduleException 유효성 검증 실패 시
     */
    @Transactional
    public ScheduleResponse update(final Long id, final ScheduleUpdateCommand command) {
        final Schedule schedule = findScheduleById(id);
        validateCommand(
                command.content(), command.category(), command.startDate(), command.endDate());

        schedule.updateCategory(command.category());
        schedule.updateStartDate(command.startDate());
        schedule.updateEndDate(command.endDate());
        schedule.updateScheduleTime(command.scheduleTime());
        schedule.updateContent(command.content());

        return toResponse(schedule);
    }

    /**
     * 일정을 삭제합니다.
     *
     * @param id 삭제할 일정 ID
     * @throws ScheduleNotFoundException 해당 ID의 일정이 존재하지 않을 때
     */
    @Transactional
    public void delete(final Long id) {
        final Schedule schedule = findScheduleById(id);
        scheduleRepository.delete(schedule);
    }

    private Schedule findScheduleById(final Long id) {
        return scheduleRepository
                .findById(id)
                .orElseThrow(() -> new ScheduleNotFoundException("일정을 찾을 수 없습니다: ID=" + id));
    }

    private void validateCommand(
            final String content,
            final Object category,
            final LocalDate startDate,
            final LocalDate endDate) {
        validateCategory(category);
        validateStartDate(startDate);
        validateContent(content);
        validateDateRange(startDate, endDate);
    }

    private void validateCategory(final Object category) {
        if (category == null) {
            throw new InvalidScheduleException("카테고리를 선택해주세요");
        }
    }

    private void validateStartDate(final LocalDate startDate) {
        if (startDate == null) {
            throw new InvalidScheduleException("시작 날짜를 입력해주세요");
        }
    }

    private void validateContent(final String content) {
        if (content == null || content.isBlank()) {
            throw new InvalidScheduleException("내용을 입력해주세요");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new InvalidScheduleException("200자를 초과할 수 없습니다");
        }
    }

    private void validateDateRange(final LocalDate startDate, final LocalDate endDate) {
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new InvalidScheduleException("종료 날짜는 시작 날짜 이후여야 합니다");
        }
    }

    private ScheduleResponse toResponse(final Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getCategory(),
                schedule.getStartDate(),
                schedule.getEndDate(),
                schedule.getScheduleTime(),
                schedule.getContent(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt());
    }
}
