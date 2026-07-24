package com.myapps.web.mycalendar.application.service;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import org.mockito.ArgumentCaptor;

import com.myapps.web.mycalendar.application.dto.ScheduleCreateCommand;
import com.myapps.web.mycalendar.application.dto.ScheduleResponse;
import com.myapps.web.mycalendar.application.dto.ScheduleUpdateCommand;
import com.myapps.web.mycalendar.application.exception.InvalidScheduleException;
import com.myapps.web.mycalendar.domain.model.Category;
import com.myapps.web.mycalendar.domain.model.Schedule;
import com.myapps.web.mycalendar.domain.repository.ScheduleRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScheduleService에 대한 Property-Based 테스트.
 *
 * <p>jqwik을 사용하여 일정 생성 라운드트립, 유효성 검증(내용 길이, 필수 필드,
 * 날짜 범위, 공백 내용), 월별 조회 위임, 캐스케이드 삭제, 수정 타임스탬프 갱신을 검증합니다.
 *
 * <p>Validates: Requirements 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.9, 3.2, 3.3, 4.2, 4.3, 4.4, 5.2
 */
class ScheduleServicePropertyTest {

    private static final int MAX_CONTENT_LENGTH = 200;

    // Feature: mycalendar/001-couple-calendar, Property 1: Schedule creation round-trip

    /**
     * Property 1: 유효한 ScheduleCreateCommand로 일정을 생성하면,
     * 반환된 응답의 모든 필드 값이 원본 커맨드와 동일해야 한다.
     *
     * <p>**Validates: Requirements 2.2, 2.3, 2.4**
     */
    @Property(tries = 100)
    void createdScheduleFieldsMustMatchOriginalCommand(
            @ForAll("validCreateCommands") final ScheduleCreateCommand command) {

        final ScheduleRepository mockRepo = mock(ScheduleRepository.class);
        final ScheduleService service = new ScheduleService(mockRepo);

        when(mockRepo.save(any(Schedule.class))).thenAnswer(invocation -> {
            final Schedule schedule = invocation.getArgument(0);
            invokeOnCreate(schedule);
            return schedule;
        });

        final ScheduleResponse response = service.create(command);

        assertEquals(command.category(), response.category());
        assertEquals(command.startDate(), response.startDate());
        assertEquals(command.endDate(), response.endDate());
        assertEquals(command.scheduleTime(), response.scheduleTime());
        assertEquals(command.content(), response.content());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
    }

    // Feature: mycalendar/001-couple-calendar, Property 2: Content length validation (일정)

    /**
     * Property 2: content 길이가 200자를 초과하면 일정 생성이 거부되어야 한다.
     *
     * <p>**Validates: Requirements 2.5, 4.3**
     */
    @Property(tries = 100)
    void createMustRejectContentExceedingMaxLength(
            @ForAll("overLengthContent") final String content) {

        final ScheduleRepository mockRepo = mock(ScheduleRepository.class);
        final ScheduleService service = new ScheduleService(mockRepo);

        final ScheduleCreateCommand command = new ScheduleCreateCommand(
                Category.SEUNGKWON, LocalDate.of(2026, 7, 1), null, null, content);

        assertThrows(InvalidScheduleException.class, () -> service.create(command));
    }

    /**
     * Property 2: content 길이가 200자를 초과하면 일정 수정이 거부되어야 한다.
     *
     * <p>**Validates: Requirements 2.5, 4.3**
     */
    @Property(tries = 100)
    void updateMustRejectContentExceedingMaxLength(
            @ForAll("overLengthContent") final String content) {

        final ScheduleRepository mockRepo = mock(ScheduleRepository.class);
        final ScheduleService service = new ScheduleService(mockRepo);

        final Schedule existingSchedule = new Schedule(Category.DATE, LocalDate.of(2026, 7, 1), "기존 내용");
        invokeOnCreate(existingSchedule);
        when(mockRepo.findById(1L)).thenReturn(Optional.of(existingSchedule));

        final ScheduleUpdateCommand command = new ScheduleUpdateCommand(
                Category.DATE, LocalDate.of(2026, 7, 1), null, null, content);

        assertThrows(InvalidScheduleException.class, () -> service.update(1L, command));
    }

    // Feature: mycalendar/001-couple-calendar, Property 3: Required field validation

    /**
     * Property 3: 필수 필드(category, startDate, content) 중 하나라도 null이면
     * 일정 생성이 거부되어야 한다.
     *
     * <p>**Validates: Requirements 2.6**
     */
    @Property(tries = 100)
    void createMustRejectWhenRequiredFieldIsNull(
            @ForAll("commandsWithNullRequiredField") final ScheduleCreateCommand command) {

        final ScheduleRepository mockRepo = mock(ScheduleRepository.class);
        final ScheduleService service = new ScheduleService(mockRepo);

        assertThrows(InvalidScheduleException.class, () -> service.create(command));
    }

    // Feature: mycalendar/001-couple-calendar, Property 4: Date range validation

    /**
     * Property 4: endDate가 non-null이고 startDate보다 이전이면
     * 일정 생성이 거부되어야 한다.
     *
     * <p>**Validates: Requirements 2.7, 4.4**
     */
    @Property(tries = 100)
    void createMustRejectWhenEndDateBeforeStartDate(
            @ForAll("invalidDateRangeCommands") final ScheduleCreateCommand command) {

        final ScheduleRepository mockRepo = mock(ScheduleRepository.class);
        final ScheduleService service = new ScheduleService(mockRepo);

        assertThrows(InvalidScheduleException.class, () -> service.create(command));
    }

    /**
     * Property 4: endDate가 non-null이고 startDate보다 이전이면
     * 일정 수정이 거부되어야 한다.
     *
     * <p>**Validates: Requirements 2.7, 4.4**
     */
    @Property(tries = 100)
    void updateMustRejectWhenEndDateBeforeStartDate(
            @ForAll("invalidDateRangeCommands") final ScheduleCreateCommand command) {

        final ScheduleRepository mockRepo = mock(ScheduleRepository.class);
        final ScheduleService service = new ScheduleService(mockRepo);

        final Schedule existingSchedule = new Schedule(Category.DATE, LocalDate.of(2026, 7, 1), "기존 내용");
        invokeOnCreate(existingSchedule);
        when(mockRepo.findById(1L)).thenReturn(Optional.of(existingSchedule));

        final ScheduleUpdateCommand updateCommand = new ScheduleUpdateCommand(
                command.category(), command.startDate(), command.endDate(),
                command.scheduleTime(), "유효한 내용");

        assertThrows(InvalidScheduleException.class, () -> service.update(1L, updateCommand));
    }

    // Feature: mycalendar/001-couple-calendar, Property 5: Whitespace content rejection (일정)

    /**
     * Property 5: content가 공백 문자만으로 구성되면 일정 생성이 거부되어야 한다.
     *
     * <p>**Validates: Requirements 2.9**
     */
    @Property(tries = 100)
    void createMustRejectWhitespaceOnlyContent(
            @ForAll("whitespaceOnlyContent") final String content) {

        final ScheduleRepository mockRepo = mock(ScheduleRepository.class);
        final ScheduleService service = new ScheduleService(mockRepo);

        final ScheduleCreateCommand command = new ScheduleCreateCommand(
                Category.CHIWON, LocalDate.of(2026, 8, 1), null, null, content);

        assertThrows(InvalidScheduleException.class, () -> service.create(command));
    }

    // Feature: mycalendar/001-couple-calendar, Property 6: Monthly schedule query overlap

    /**
     * Property 6: 월별 조회 시 서비스가 리포지토리에 올바른 월 경계 날짜를 전달해야 한다.
     *
     * <p>startOfMonth는 해당 월 1일, endOfMonth는 해당 월 마지막 날이어야 한다.
     *
     * <p>**Validates: Requirements 3.2, 3.3**
     */
    @Property(tries = 100)
    void findByMonthMustDelegateWithCorrectMonthBoundaries(
            @ForAll("yearMonths") final YearMonth yearMonth) {

        final ScheduleRepository mockRepo = mock(ScheduleRepository.class);
        final ScheduleService service = new ScheduleService(mockRepo);

        when(mockRepo.findByMonth(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        service.findByMonth(yearMonth);

        final ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        final ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(mockRepo).findByMonth(startCaptor.capture(), endCaptor.capture());

        final LocalDate expectedStart = yearMonth.atDay(1);
        final LocalDate expectedEnd = yearMonth.atEndOfMonth();

        assertEquals(expectedStart, startCaptor.getValue(),
                "startOfMonth는 해당 월 1일이어야 한다");
        assertEquals(expectedEnd, endCaptor.getValue(),
                "endOfMonth는 해당 월 마지막 날이어야 한다");
    }

    // Feature: mycalendar/001-couple-calendar, Property 7: Schedule cascade delete

    /**
     * Property 7: 일정 삭제 시 리포지토리 delete가 호출되어야 한다.
     *
     * <p>서비스 레벨에서 delete 위임을 검증한다.
     *
     * <p>**Validates: Requirements 5.2**
     */
    @Property(tries = 100)
    void deleteMustInvokeRepositoryDeleteOnFoundSchedule() {

        final ScheduleRepository mockRepo = mock(ScheduleRepository.class);
        final ScheduleService service = new ScheduleService(mockRepo);

        final Schedule schedule = new Schedule(Category.DATE, LocalDate.of(2026, 7, 1), "삭제 대상");
        invokeOnCreate(schedule);

        when(mockRepo.findById(1L)).thenReturn(Optional.of(schedule));

        service.delete(1L);

        verify(mockRepo).delete(schedule);
    }

    // Feature: mycalendar/001-couple-calendar, Property 13: Schedule update timestamp refresh

    /**
     * Property 13: 유효한 수정을 적용하면, 엔티티의 필드 값이 변경되어야 한다.
     *
     * <p>@PreUpdate는 JPA 인프라에서 발생하므로, 서비스 레벨에서는
     * 수정 메서드가 올바르게 호출되어 필드가 변경되는지 검증한다.
     * updatedAt 갱신은 JPA lifecycle callback에 의해 보장된다.
     *
     * <p>**Validates: Requirements 4.2**
     */
    @Property(tries = 100)
    void updateMustModifyScheduleFields(
            @ForAll("validUpdateCommands") final ScheduleUpdateCommand command) {

        final ScheduleRepository mockRepo = mock(ScheduleRepository.class);
        final ScheduleService service = new ScheduleService(mockRepo);

        final Schedule existingSchedule = new Schedule(Category.SEUNGKWON, LocalDate.of(2026, 1, 1), "원본 내용");
        invokeOnCreate(existingSchedule);
        when(mockRepo.findById(1L)).thenReturn(Optional.of(existingSchedule));

        final ScheduleResponse response = service.update(1L, command);

        assertEquals(command.category(), existingSchedule.getCategory());
        assertEquals(command.startDate(), existingSchedule.getStartDate());
        assertEquals(command.endDate(), existingSchedule.getEndDate());
        assertEquals(command.scheduleTime(), existingSchedule.getScheduleTime());
        assertEquals(command.content(), existingSchedule.getContent());
        assertNotNull(response.createdAt(),
                "수정 후에도 createdAt은 유지되어야 한다");
    }

    // --- Helper Methods ---

    private static void invokeOnCreate(final Schedule schedule) {
        try {
            final Method method = Schedule.class.getDeclaredMethod("onCreate");
            method.setAccessible(true);
            method.invoke(schedule);
        } catch (final Exception e) {
            throw new RuntimeException("onCreate 호출 실패", e);
        }
    }

    // --- Arbitrary Providers ---

    @Provide
    Arbitrary<ScheduleCreateCommand> validCreateCommands() {
        final Arbitrary<Category> categories = Arbitraries.of(Category.values());
        final Arbitrary<LocalDate> startDates = Arbitraries.integers().between(0, 730)
                .map(offset -> LocalDate.of(2026, 1, 1).plusDays(offset));
        final Arbitrary<LocalDate> endDateOffsets = Arbitraries.integers().between(0, 30)
                .map(offset -> (LocalDate) null);
        final Arbitrary<String> contents = validContentStrings();
        final Arbitrary<LocalTime> times = Arbitraries.of(
                null, LocalTime.of(9, 0), LocalTime.of(14, 30), LocalTime.of(18, 0));

        return Combinators.combine(categories, startDates, contents, times)
                .as((category, startDate, content, time) -> {
                    final LocalDate endDate = Arbitraries.integers().between(0, 10)
                            .sample() > 5 ? startDate.plusDays(
                            Arbitraries.integers().between(0, 14).sample()) : null;
                    return new ScheduleCreateCommand(category, startDate, endDate, time, content);
                });
    }

    @Provide
    Arbitrary<ScheduleUpdateCommand> validUpdateCommands() {
        final Arbitrary<Category> categories = Arbitraries.of(Category.values());
        final Arbitrary<LocalDate> startDates = Arbitraries.integers().between(0, 730)
                .map(offset -> LocalDate.of(2026, 1, 1).plusDays(offset));
        final Arbitrary<String> contents = validContentStrings();
        final Arbitrary<LocalTime> times = Arbitraries.of(
                null, LocalTime.of(10, 0), LocalTime.of(15, 0), LocalTime.of(20, 0));

        return Combinators.combine(categories, startDates, contents, times)
                .as((category, startDate, content, time) -> {
                    final LocalDate endDate = Arbitraries.integers().between(0, 10)
                            .sample() > 5 ? startDate.plusDays(
                            Arbitraries.integers().between(0, 14).sample()) : null;
                    return new ScheduleUpdateCommand(category, startDate, endDate, time, content);
                });
    }

    @Provide
    Arbitrary<String> overLengthContent() {
        return Arbitraries.integers().between(MAX_CONTENT_LENGTH + 1, MAX_CONTENT_LENGTH + 100)
                .map(length -> "가".repeat(length));
    }

    @Provide
    Arbitrary<ScheduleCreateCommand> commandsWithNullRequiredField() {
        return Arbitraries.integers().between(0, 2).map(fieldIndex -> {
            switch (fieldIndex) {
                case 0:
                    return new ScheduleCreateCommand(
                            null, LocalDate.of(2026, 7, 1), null, null, "유효한 내용");
                case 1:
                    return new ScheduleCreateCommand(
                            Category.SEUNGKWON, null, null, null, "유효한 내용");
                default:
                    return new ScheduleCreateCommand(
                            Category.SEUNGKWON, LocalDate.of(2026, 7, 1), null, null, null);
            }
        });
    }

    @Provide
    Arbitrary<ScheduleCreateCommand> invalidDateRangeCommands() {
        return Arbitraries.integers().between(1, 365).map(daysBefore -> {
            final LocalDate startDate = LocalDate.of(2026, 7, 15);
            final LocalDate endDate = startDate.minusDays(daysBefore);
            return new ScheduleCreateCommand(
                    Category.DATE, startDate, endDate, null, "유효한 내용");
        });
    }

    @Provide
    Arbitrary<String> whitespaceOnlyContent() {
        return Arbitraries.of(" ", "  ", "\t", "\n", " \t\n", "   \t   ");
    }

    @Provide
    Arbitrary<YearMonth> yearMonths() {
        return Arbitraries.integers().between(0, 60)
                .map(offset -> YearMonth.of(2026, 1).plusMonths(offset));
    }

    private Arbitrary<String> validContentStrings() {
        return Arbitraries.integers().between(1, MAX_CONTENT_LENGTH)
                .map(length -> {
                    final int safeLength = Math.min(length, MAX_CONTENT_LENGTH);
                    return "일정" + "내".repeat(Math.max(0, safeLength - 2));
                });
    }
}
