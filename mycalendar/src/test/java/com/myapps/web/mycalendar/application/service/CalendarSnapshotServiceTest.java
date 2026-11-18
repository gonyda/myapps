package com.myapps.web.mycalendar.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.myapps.web.mycalendar.application.dto.CalendarSnapshot;
import com.myapps.web.mycalendar.domain.model.Category;
import com.myapps.web.mycalendar.domain.model.Schedule;
import com.myapps.web.mycalendar.domain.repository.ScheduleRepository;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** {@link CalendarSnapshotService}의 정렬, KST 타임스탬프, UTF-8 Pretty JSON 직렬화 검증 테스트. */
@ExtendWith(MockitoExtension.class)
class CalendarSnapshotServiceTest {

    @Mock private ScheduleRepository scheduleRepository;

    private ObjectMapper objectMapper;
    private CalendarSnapshotService snapshotService;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().build();
        snapshotService = new CalendarSnapshotService(scheduleRepository, objectMapper);
    }

    @Test
    @DisplayName("일정을 시작 날짜, 일정 시간(null 후순위), ID 오름차순으로 정렬하고 KST Pretty UTF-8 JSON을 생성한다")
    void should_sortAndSerializeKoreanPrettyJson_when_schedulesProvided() throws Exception {
        // given
        final Schedule lateDate =
                createSchedule(10L, Category.SEUNGKWON, LocalDate.of(2026, 12, 1), null, "12월 일정");
        final Schedule sameTimeId2 =
                createSchedule(
                        2L,
                        Category.DATE,
                        LocalDate.of(2026, 11, 28),
                        LocalTime.of(7, 0),
                        "청량산 등산");
        sameTimeId2.updateEndDate(LocalDate.of(2026, 11, 28));
        final Schedule sameTimeId1 =
                createSchedule(
                        1L,
                        Category.CHIWON,
                        LocalDate.of(2026, 11, 28),
                        LocalTime.of(7, 0),
                        "아침 준비");
        final Schedule noTimeSameDate =
                createSchedule(
                        3L, Category.DATE, LocalDate.of(2026, 11, 28), null, "여자친구와 당일치기 등산");

        final OffsetDateTime utcTime =
                OffsetDateTime.of(2026, 11, 18, 10, 30, 15, 0, ZoneOffset.UTC);

        // when
        final CalendarSnapshot snapshot =
                snapshotService.buildSnapshot(
                        List.of(lateDate, sameTimeId2, noTimeSameDate, sameTimeId1), utcTime);
        final String json = snapshotService.serialize(snapshot);

        // then
        assertThat(snapshot.syncedAt()).isEqualTo("2026-11-18T19:30:15+09:00");
        assertThat(snapshot.timezone()).isEqualTo("Asia/Seoul");
        assertThat(snapshot.totalCount()).isEqualTo(4);
        assertThat(json).contains("\n  \"syncedAt\"");
        assertThat(json).contains("청량산 등산");
        assertThat(json).contains("여자친구와 당일치기 등산");
        assertThat(json).doesNotContain("\\u");

        final JsonNode root = objectMapper.readTree(json);
        assertThat(root.get("totalCount").asInt()).isEqualTo(4);
        assertThat(root.get("events").get(0).get("id").asLong()).isEqualTo(1L);
        assertThat(root.get("events").get(1).get("id").asLong()).isEqualTo(2L);
        assertThat(root.get("events").get(1).get("scheduleTime").asString()).isEqualTo("07:00");
        assertThat(root.get("events").get(2).get("id").asLong()).isEqualTo(3L);
        assertThat(root.get("events").get(2).get("scheduleTime").isNull()).isTrue();
        assertThat(root.get("events").get(3).get("id").asLong()).isEqualTo(10L);
    }

    @Test
    @DisplayName("buildCurrentSnapshot은 리포지토리에서 전체 일정을 조회해 KST 스냅샷을 반환한다")
    void should_queryRepositoryAndBuildSnapshot_when_buildCurrentSnapshotCalled() {
        // given
        final Schedule schedule =
                createSchedule(
                        5L,
                        Category.DATE,
                        LocalDate.of(2026, 11, 20),
                        LocalTime.of(18, 30),
                        "기념일 식사");
        when(scheduleRepository.findAllForSync()).thenReturn(List.of(schedule));

        // when
        final CalendarSnapshot snapshot = snapshotService.buildCurrentSnapshot();

        // then
        assertThat(snapshot.timezone()).isEqualTo("Asia/Seoul");
        assertThat(snapshot.syncedAt()).endsWith("+09:00");
        assertThat(snapshot.totalCount()).isEqualTo(1);
        assertThat(snapshot.events().get(0).content()).isEqualTo("기념일 식사");
    }

    private Schedule createSchedule(
            final Long id,
            final Category category,
            final LocalDate startDate,
            final LocalTime time,
            final String content) {
        final Schedule schedule = new Schedule(category, startDate, content);
        schedule.updateScheduleTime(time);
        setId(schedule, id);
        return schedule;
    }

    private void setId(final Schedule schedule, final Long id) {
        try {
            final Field idField = Schedule.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(schedule, id);
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException("테스트용 id 설정 실패", ex);
        }
    }
}
