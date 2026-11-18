package com.myapps.web.mycalendar.application.service;

import com.myapps.web.mycalendar.application.dto.CalendarSnapshot;
import com.myapps.web.mycalendar.application.dto.CalendarSnapshotEvent;
import com.myapps.web.mycalendar.application.exception.GitHubSyncException;
import com.myapps.web.mycalendar.domain.model.Schedule;
import com.myapps.web.mycalendar.domain.repository.ScheduleRepository;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

/**
 * 전체 일정을 조회하여 GitHub 스냅샷 JSON으로 변환·직렬화하는 서비스.
 *
 * <p>일정 목록을 시작 날짜, 일정 시간(null 후순위), ID 오름차순으로 정렬하고 KST({@code Asia/Seoul}) 기준 타임스탬프를 포함한 UTF-8
 * Pretty JSON을 생성합니다.
 */
@Service
public class CalendarSnapshotService {

    private static final String TIMEZONE_ID = "Asia/Seoul";
    private static final ZoneId KST = ZoneId.of(TIMEZONE_ID);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Comparator<Schedule> SNAPSHOT_ORDER =
            Comparator.comparing(
                            Schedule::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(
                            Schedule::getScheduleTime,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(
                            Schedule::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final ScheduleRepository scheduleRepository;
    private final ObjectWriter prettyWriter;

    /**
     * CalendarSnapshotService를 생성합니다.
     *
     * @param scheduleRepository 일정 리포지토리
     * @param objectMapper JSON 직렬화에 사용할 Jackson ObjectMapper
     */
    public CalendarSnapshotService(
            final ScheduleRepository scheduleRepository, final ObjectMapper objectMapper) {
        this.scheduleRepository = scheduleRepository;
        this.prettyWriter = objectMapper.writer().with(createPrettyPrinter());
    }

    /**
     * DB에서 최신 전체 일정을 조회하여 현재 KST 시각 기준 스냅샷을 생성합니다.
     *
     * @return 정렬된 전체 일정이 담긴 {@link CalendarSnapshot}
     */
    @Transactional(readOnly = true)
    public CalendarSnapshot buildCurrentSnapshot() {
        final List<Schedule> schedules = scheduleRepository.findAllForSync();
        return buildSnapshot(schedules, OffsetDateTime.now(KST));
    }

    /**
     * 전달받은 일정 목록과 기준 시각으로 스냅샷을 조립합니다.
     *
     * @param schedules 스냅샷에 포함할 일정 목록
     * @param syncedAt 기준 동기화 시각
     * @return 정렬 및 매핑이 완료된 {@link CalendarSnapshot}
     */
    public CalendarSnapshot buildSnapshot(
            final List<Schedule> schedules, final OffsetDateTime syncedAt) {
        final List<CalendarSnapshotEvent> events =
                schedules.stream().sorted(SNAPSHOT_ORDER).map(this::toSnapshotEvent).toList();
        final String formattedSyncedAt =
                syncedAt.atZoneSameInstant(KST)
                        .toOffsetDateTime()
                        .truncatedTo(ChronoUnit.SECONDS)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return new CalendarSnapshot(formattedSyncedAt, TIMEZONE_ID, events.size(), events);
    }

    /**
     * {@link CalendarSnapshot}을 Git diff 친화적인 UTF-8 Pretty JSON 문자열로 직렬화합니다.
     *
     * @param snapshot 직렬화할 스냅샷
     * @return 들여쓰기 및 후행 개행이 포함된 JSON 문자열
     * @throws GitHubSyncException JSON 직렬화 실패 시
     */
    public String serialize(final CalendarSnapshot snapshot) {
        try {
            return prettyWriter.writeValueAsString(snapshot) + "\n";
        } catch (final JacksonException ex) {
            throw new GitHubSyncException("캘린더 스냅샷 JSON 직렬화에 실패했습니다.", ex);
        }
    }

    private CalendarSnapshotEvent toSnapshotEvent(final Schedule schedule) {
        final String startDate =
                schedule.getStartDate() != null ? schedule.getStartDate().toString() : null;
        final String endDate =
                schedule.getEndDate() != null ? schedule.getEndDate().toString() : null;
        final String scheduleTime =
                schedule.getScheduleTime() != null
                        ? schedule.getScheduleTime().format(TIME_FORMATTER)
                        : null;
        return new CalendarSnapshotEvent(
                schedule.getId(),
                schedule.getCategory(),
                startDate,
                endDate,
                scheduleTime,
                schedule.getContent());
    }

    private static DefaultPrettyPrinter createPrettyPrinter() {
        final DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        return new DefaultPrettyPrinter().withObjectIndenter(indenter).withArrayIndenter(indenter);
    }
}
