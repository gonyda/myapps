package com.myapps.web.mycalendar.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myapps.web.mycalendar.domain.model.ScheduleComment;

/**
 * 댓글 엔티티에 대한 리포지토리 인터페이스.
 *
 * <p>Spring Data JPA를 통해 댓글의 영속성 작업을 처리합니다.
 */
public interface CommentRepository extends JpaRepository<ScheduleComment, Long> {

    /**
     * 특정 일정에 속한 댓글을 생성 시각 오름차순으로 조회합니다.
     *
     * @param scheduleId 조회할 일정의 ID
     * @return 생성 시각 오름차순으로 정렬된 댓글 목록
     */
    List<ScheduleComment> findByScheduleIdOrderByCreatedAtAsc(Long scheduleId);
}
