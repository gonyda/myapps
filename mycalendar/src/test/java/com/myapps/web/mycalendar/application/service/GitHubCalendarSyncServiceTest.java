package com.myapps.web.mycalendar.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.mycalendar.application.dto.CalendarSnapshot;
import com.myapps.web.mycalendar.application.dto.GitHubSyncResult;
import com.myapps.web.mycalendar.application.dto.GitHubSyncStatus;
import com.myapps.web.mycalendar.application.exception.GitHubSyncException;
import com.myapps.web.mycalendar.infrastructure.config.GitHubSyncProperties;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link GitHubCalendarSyncService}의 직렬화, 비활성화 no-op, Fail-safe 동작 검증 테스트. */
@ExtendWith(MockitoExtension.class)
class GitHubCalendarSyncServiceTest {

    @Mock private CalendarSnapshotService snapshotService;
    @Mock private GitHubSyncClient gitHubSyncClient;

    private GitHubCalendarSyncService syncService;

    @AfterEach
    void tearDown() {
        if (syncService != null) {
            syncService.destroy();
        }
    }

    @Test
    @DisplayName("enabled가 false이거나 토큰이 비어 있으면 스킵 결과를 반환하고 외부 클라이언트를 호출하지 않는다")
    void should_skipWithoutClientCall_when_disabledOrTokenBlank() {
        // given
        final GitHubSyncProperties disabledProps =
                new GitHubSyncProperties(false, "token", "gonyda", "mike", "main", "p", "m");
        syncService =
                new GitHubCalendarSyncService(disabledProps, snapshotService, gitHubSyncClient);

        // when
        syncService.requestSyncAsync();
        final GitHubSyncResult result = syncService.syncNow();

        // then
        assertThat(result.status()).isEqualTo(GitHubSyncStatus.SKIPPED);
        verify(gitHubSyncClient, never()).pushSnapshot(anyString());
    }

    @Test
    @DisplayName("수동 동기화 성공 시 SUCCESS 결과와 건수·시각을 반환한다")
    void should_returnSuccessResult_when_syncNowSucceeds() {
        // given
        final GitHubSyncProperties activeProps = activeProperties();
        syncService = new GitHubCalendarSyncService(activeProps, snapshotService, gitHubSyncClient);
        final CalendarSnapshot snapshot =
                new CalendarSnapshot("2026-11-18T19:30:00+09:00", "Asia/Seoul", 2, List.of());
        when(snapshotService.buildCurrentSnapshot()).thenReturn(snapshot);
        when(snapshotService.serialize(snapshot)).thenReturn("{\"totalCount\":2}\n");

        // when
        final GitHubSyncResult result = syncService.syncNow();

        // then
        assertThat(result.status()).isEqualTo(GitHubSyncStatus.SUCCESS);
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.syncedAt()).isEqualTo("2026-11-18T19:30:00+09:00");
        verify(gitHubSyncClient).pushSnapshot("{\"totalCount\":2}\n");
    }

    @Test
    @DisplayName("GitHub API 예외가 발생해도 비동기/수동 동기화 모두 호출자에게 예외를 전파하지 않는다")
    void should_swallowException_when_gitHubClientFails() {
        // given
        final GitHubSyncProperties activeProps = activeProperties();
        syncService = new GitHubCalendarSyncService(activeProps, snapshotService, gitHubSyncClient);
        final CalendarSnapshot snapshot =
                new CalendarSnapshot("2026-11-18T19:30:00+09:00", "Asia/Seoul", 1, List.of());
        when(snapshotService.buildCurrentSnapshot()).thenReturn(snapshot);
        when(snapshotService.serialize(snapshot)).thenReturn("{}\n");
        doThrow(new GitHubSyncException("GitHub 500 error"))
                .when(gitHubSyncClient)
                .pushSnapshot(anyString());

        // when
        syncService.requestSyncAsync();
        final GitHubSyncResult result = syncService.syncNow();

        // then
        assertThat(result.status()).isEqualTo(GitHubSyncStatus.FAILED);
        assertThat(result.message()).contains("GitHub 500 error");
    }

    @Test
    @DisplayName("동시 다발적 동기화 요청이 들어와도 단일 스레드에서 순차적으로 직렬화 실행된다")
    void should_serializeConcurrentRequests_when_multipleSyncsTriggered() throws Exception {
        // given
        final GitHubSyncProperties activeProps = activeProperties();
        syncService = new GitHubCalendarSyncService(activeProps, snapshotService, gitHubSyncClient);
        final CalendarSnapshot snapshot =
                new CalendarSnapshot("2026-11-18T19:30:00+09:00", "Asia/Seoul", 1, List.of());
        when(snapshotService.buildCurrentSnapshot()).thenReturn(snapshot);
        when(snapshotService.serialize(snapshot)).thenReturn("{}\n");

        final AtomicInteger concurrentExecutions = new AtomicInteger(0);
        final AtomicInteger maxConcurrentExecutions = new AtomicInteger(0);
        final CountDownLatch firstEntered = new CountDownLatch(1);
        final CountDownLatch releaseFirst = new CountDownLatch(1);

        doAnswer(
                        invocation -> {
                            final int active = concurrentExecutions.incrementAndGet();
                            maxConcurrentExecutions.accumulateAndGet(active, Math::max);
                            firstEntered.countDown();
                            releaseFirst.await(2, TimeUnit.SECONDS);
                            concurrentExecutions.decrementAndGet();
                            return null;
                        })
                .when(gitHubSyncClient)
                .pushSnapshot(anyString());

        // when
        syncService.requestSyncAsync();
        assertThat(firstEntered.await(2, TimeUnit.SECONDS)).isTrue();
        syncService.requestSyncAsync();
        syncService.requestSyncAsync();
        releaseFirst.countDown();
        final GitHubSyncResult manualResult = syncService.syncNow();

        // then
        assertThat(manualResult.status()).isEqualTo(GitHubSyncStatus.SUCCESS);
        assertThat(maxConcurrentExecutions.get()).isEqualTo(1);
    }

    private GitHubSyncProperties activeProperties() {
        return new GitHubSyncProperties(
                true, "token", "gonyda", "mike", "main", "calendar/mycalendar.json", "msg");
    }
}
