package com.myapps.web.mycalendar.domain.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.myapps.web.mycalendar.domain.model.Schedule;

/**
 * 일정 엔티티에 대한 영속성 인터페이스.
 *
 * <p>Spring Data JPA를 활용하여 기본 CRUD와 월별 조회 기능을 제공합니다.
 * Multi_Day_Schedule(endDate != null)의 날짜 범위가 조회 대상 월과 겹치는 경우도
 * 결과에 포함합니다.
 */
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 지정된 월 범위와 겹치는 모든 일정을 조회합니다.
     *
     * <p>조회 조건:
     * <ul>
     *   <li>startDate가 월말 이전이고</li>
     *   <li>endDate가 월초 이후이거나 (Multi_Day_Schedule)</li>
     *   <li>endDate가 null이면서 startDate가 해당 월 범위 내인 경우 (Single_Day_Schedule)</li>
     * </ul>
     *
     * @param startOfMonth 조회 대상 월의 첫째 날
     * @param endOfMonth   조회 대상 월의 마지막 날
     * @return 해당 월과 겹치는 일정 목록
     */
    @Query("SELECT s FROM Schedule s WHERE s.startDate <= :endOfMonth AND "
            + "(s.endDate >= :startOfMonth OR "
            + "(s.endDate IS NULL AND s.startDate >= :startOfMonth AND s.startDate <= :endOfMonth))")
    List<Schedule> findByMonth(@Param("startOfMonth") final LocalDate startOfMonth,
                               @Param("endOfMonth") final LocalDate endOfMonth);
}
