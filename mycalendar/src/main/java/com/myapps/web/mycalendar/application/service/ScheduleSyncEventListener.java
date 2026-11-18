package com.myapps.web.mycalendar.application.service;

import com.myapps.web.mycalendar.application.dto.ScheduleChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 일정 변경 트랜잭션 커밋 후 GitHub 동기화를 트리거하는 이벤트 리스너.
 *
 * <p>{@link TransactionPhase#AFTER_COMMIT} 단계에서만 동작하므로 유효성 검증 실패 등으로 롤백된 요청에서는 외부 동기화가 실행되지 않습니다.
 */
@Component
public class ScheduleSyncEventListener {

    private final GitHubCalendarSyncService gitHubCalendarSyncService;

    /**
     * ScheduleSyncEventListener를 생성합니다.
     *
     * @param gitHubCalendarSyncService GitHub 동기화 오케스트레이션 서비스
     */
    public ScheduleSyncEventListener(final GitHubCalendarSyncService gitHubCalendarSyncService) {
        this.gitHubCalendarSyncService = gitHubCalendarSyncService;
    }

    /**
     * 일정 변경 트랜잭션이 커밋된 직후 비동기 스냅샷 동기화를 요청합니다.
     *
     * @param event 발생한 일정 변경 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onScheduleChanged(final ScheduleChangedEvent event) {
        gitHubCalendarSyncService.requestSyncAsync();
    }
}
