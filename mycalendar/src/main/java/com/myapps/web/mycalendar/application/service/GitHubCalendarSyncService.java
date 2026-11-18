package com.myapps.web.mycalendar.application.service;

import com.myapps.web.mycalendar.application.dto.CalendarSnapshot;
import com.myapps.web.mycalendar.application.dto.GitHubSyncResult;
import com.myapps.web.mycalendar.infrastructure.config.GitHubSyncProperties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

/**
 * GitHub 캘린더 스냅샷 동기화를 직렬화·비동기·Fail-safe로 조율하는 서비스.
 *
 * <p>단일 스레드 실행기({@code SingleThreadExecutor})를 통해 동시 요청 시 {@code 409 Conflict} 및 커밋 순서 역전을 방지하고, 실행
 * 시점마다 DB에서 최신 전체 일정을 조회해 푸시합니다. 외부 오류는 내부에서 로깅하고 삼켜 일정 CRUD 흐름에 영향을 주지 않습니다.
 */
@Service
public class GitHubCalendarSyncService implements DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubCalendarSyncService.class);
    private static final long SYNC_WAIT_TIMEOUT_SECONDS = 30L;
    private static final long SHUTDOWN_WAIT_SECONDS = 5L;

    private final GitHubSyncProperties properties;
    private final CalendarSnapshotService snapshotService;
    private final GitHubSyncClient gitHubSyncClient;
    private final ExecutorService syncExecutor;
    private final AtomicBoolean pendingSync;

    /**
     * GitHubCalendarSyncService를 생성합니다.
     *
     * @param properties GitHub 동기화 설정 프로퍼티
     * @param snapshotService 스냅샷 생성 및 직렬화 서비스
     * @param gitHubSyncClient GitHub Contents API 클라이언트
     */
    public GitHubCalendarSyncService(
            final GitHubSyncProperties properties,
            final CalendarSnapshotService snapshotService,
            final GitHubSyncClient gitHubSyncClient) {
        this.properties = properties;
        this.snapshotService = snapshotService;
        this.gitHubSyncClient = gitHubSyncClient;
        this.syncExecutor =
                Executors.newSingleThreadExecutor(
                        runnable -> {
                            final Thread thread = new Thread(runnable, "github-calendar-sync");
                            thread.setDaemon(true);
                            return thread;
                        });
        this.pendingSync = new AtomicBoolean(false);
    }

    /**
     * 비동기 동기화 작업을 단일 스레드 큐에 예약합니다.
     *
     * <p>설정이 비활성화되었거나 토큰이 없으면 HTTP 호출 없이 즉시 반환하며, 이미 실행 대기 중인 작업이 있는 경우 중복 푸시를 병합(coalesce)합니다.
     */
    public void requestSyncAsync() {
        if (!properties.isActive()) {
            LOG.debug("GitHub 캘린더 동기화 비활성화 또는 토큰 미설정으로 비동기 동기화를 건너뜁니다.");
            return;
        }
        if (!pendingSync.compareAndSet(false, true)) {
            LOG.debug("대기 중인 GitHub 캘린더 동기화 작업이 이미 존재하여 요청을 병합합니다.");
            return;
        }
        try {
            syncExecutor.execute(this::runCoalescedSync);
        } catch (final RejectedExecutionException ex) {
            pendingSync.set(false);
            LOG.warn("동기화 실행기가 종료 중이어서 비동기 동기화 요청이 거부되었습니다: {}", ex.getMessage());
        }
    }

    /**
     * 수동 재동기화를 위해 단일 스레드 큐에서 즉시 동기화를 실행하고 결과를 반환합니다.
     *
     * @return 동기화 실행 결과 ({@code SUCCESS}, {@code SKIPPED}, {@code FAILED})
     */
    public GitHubSyncResult syncNow() {
        if (!properties.isActive()) {
            LOG.info("GitHub 캘린더 동기화 비활성화 또는 토큰 미설정으로 수동 동기화를 건너뜁니다.");
            return GitHubSyncResult.skipped("GitHub sync is disabled or token is missing");
        }
        try {
            final Future<GitHubSyncResult> future = syncExecutor.submit(this::executeSyncInternal);
            return future.get(SYNC_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.error("GitHub 수동 동기화 대기 중 인터럽트가 발생했습니다.", ex);
            return GitHubSyncResult.failed("Sync interrupted");
        } catch (final ExecutionException | TimeoutException | RejectedExecutionException ex) {
            LOG.error("GitHub 수동 동기화 실행 중 오류가 발생했습니다: {}", ex.getMessage(), ex);
            return GitHubSyncResult.failed(
                    ex.getMessage() != null ? ex.getMessage() : "Sync execution failed");
        }
    }

    /** 스프링 컨텍스트 종료 시 백그라운드 동기화 스레드 풀을 안전하게 종료합니다. */
    @Override
    public void destroy() {
        syncExecutor.shutdown();
        try {
            if (!syncExecutor.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                syncExecutor.shutdownNow();
            }
        } catch (final InterruptedException ex) {
            syncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void runCoalescedSync() {
        pendingSync.set(false);
        executeSyncInternal();
    }

    private GitHubSyncResult executeSyncInternal() {
        if (!properties.isActive()) {
            LOG.debug("실행 시점에 GitHub 동기화가 비활성화 상태이므로 작업을 종료합니다.");
            return GitHubSyncResult.skipped("GitHub sync is disabled or token is missing");
        }
        try {
            final CalendarSnapshot snapshot = snapshotService.buildCurrentSnapshot();
            final String json = snapshotService.serialize(snapshot);
            gitHubSyncClient.pushSnapshot(json);
            LOG.info(
                    "GitHub 캘린더 동기화 완료: totalCount={}, syncedAt={}",
                    snapshot.totalCount(),
                    snapshot.syncedAt());
            return GitHubSyncResult.success(snapshot.totalCount(), snapshot.syncedAt());
        } catch (final Exception ex) {
            LOG.error("GitHub 캘린더 동기화 실패 (일정 데이터는 정상 유지됨): {}", ex.getMessage(), ex);
            return GitHubSyncResult.failed(
                    ex.getMessage() != null ? ex.getMessage() : "GitHub sync failed");
        }
    }
}
