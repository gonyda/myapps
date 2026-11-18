package com.myapps.web.mycalendar.application.dto;

/**
 * 일정 생성·수정·삭제 발생 시 발행되는 애플리케이션 이벤트.
 *
 * <p>트랜잭션 커밋 이후({@code AFTER_COMMIT}) GitHub 스냅샷 동기화를 비동기로 트리거하기 위해 사용됩니다.
 */
public record ScheduleChangedEvent(ChangeType changeType, Long scheduleId) {

    /** 일정 변경 유형. */
    public enum ChangeType {
        CREATED,
        UPDATED,
        DELETED
    }
}
