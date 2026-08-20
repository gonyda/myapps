package com.myapps.web.mycalendar.domain.repository;

import com.myapps.web.mycalendar.domain.model.Schedule;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 일정 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 기본 CRUD와 월별 조회 기능을 제공합니다. Multi_Day_Schedule(endDate != null)의 날짜 범위가 조회
 * 대상 월과 겹치는 경우도 결과에 포함합니다.
 */
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 지정된 월 범위와 겹치는 모든 일정을 조회합니다.
     *
     * <p>조회 조건:
     *
     * <ul>
     *   <li>startDate가 월말 이전이고
     *   <li>endDate가 월초 이후이거나 (Multi_Day_Schedule)
     *   <li>endDate가 null이면서 startDate가 해당 월 범위 내인 경우 (Single_Day_Schedule)
     * </ul>
     *
     * @param startOfMonth 조회 대상 월의 첫째 날
     * @param endOfMonth 조회 대상 월의 마지막 날
     * @return 해당 월과 겹치는 일정 목록
     */
    @Query(
            "SELECT s FROM Schedule s WHERE s.startDate <= :endOfMonth AND "
                    + "(s.endDate >= :startOfMonth OR "
                    + "(s.endDate IS NULL AND s.startDate >= :startOfMonth AND s.startDate <= :endOfMonth))")
    List<Schedule> findByMonth(
            @Param("startOfMonth") final LocalDate startOfMonth,
            @Param("endOfMonth") final LocalDate endOfMonth);

    /**
     * 지정된 주간 범위와 겹치는 모든 일정을 시작일, 시간순으로 조회합니다.
     *
     * <p>조회 조건은 {@link #findByMonth}와 동일하며 결과를 startDate, scheduleTime 순으로 정렬합니다.
     *
     * @param weekStart 조회 대상 주의 시작일 (일요일)
     * @param weekEnd 조회 대상 주의 마지막 날 (토요일)
     * @return 해당 주와 겹치는 일정 목록 (시작일, 시간순 정렬)
     */
    @Query(
            "SELECT s FROM Schedule s WHERE s.startDate <= :weekEnd AND "
                    + "(s.endDate >= :weekStart OR "
                    + "(s.endDate IS NULL AND s.startDate >= :weekStart AND s.startDate <= :weekEnd)) "
                    + "ORDER BY s.startDate ASC, s.scheduleTime ASC NULLS LAST")
    List<Schedule> findByWeek(
            @Param("weekStart") final LocalDate weekStart,
            @Param("weekEnd") final LocalDate weekEnd);
}
