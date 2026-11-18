package com.myapps.web.mycalendar.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.myapps.web.mycalendar.application.dto.ScheduleCreateCommand;
import com.myapps.web.mycalendar.application.dto.ScheduleResponse;
import com.myapps.web.mycalendar.application.dto.ScheduleUpdateCommand;
import com.myapps.web.mycalendar.application.exception.GitHubSyncException;
import com.myapps.web.mycalendar.application.exception.InvalidScheduleException;
import com.myapps.web.mycalendar.domain.model.Category;
import com.myapps.web.mycalendar.domain.repository.ScheduleRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** 일정 생성·수정·삭제 트랜잭션 커밋과 {@code AFTER_COMMIT} 비동기 GitHub 동기화 연동을 검증하는 통합 테스트. */
@SpringBootTest
@TestPropertySource(
        properties = {
            "mycalendar.github-sync.enabled=true",
            "mycalendar.github-sync.token=integration-test-token"
        })
class ScheduleSyncIntegrationTest {

    @Autowired private ScheduleService scheduleService;
    @Autowired private ScheduleRepository scheduleRepository;

    @MockitoBean private GitHubSyncClient gitHubSyncClient;

    @BeforeEach
    void setUp() {
        scheduleRepository.deleteAll();
        reset(gitHubSyncClient);
    }

    @Test
    @DisplayName("일정 생성·수정 커밋 후 비동기로 스냅샷 동기화가 실행되고 외부 실패가 CUD에 영향을 주지 않는다")
    void should_triggerSyncAfterCommitAndIsolateFailure_when_scheduleCreatedAndUpdated()
            throws Exception {
        // given
        final CountDownLatch createdLatch = new CountDownLatch(1);
        final AtomicReference<String> pushedJson = new AtomicReference<>();
        doAnswer(
                        invocation -> {
                            pushedJson.set(invocation.getArgument(0));
                            createdLatch.countDown();
                            return null;
                        })
                .when(gitHubSyncClient)
                .pushSnapshot(anyString());

        // when (create)
        final ScheduleResponse created =
                scheduleService.create(
                        new ScheduleCreateCommand(
                                Category.DATE,
                                LocalDate.of(2026, 11, 28),
                                LocalDate.of(2026, 11, 28),
                                LocalTime.of(7, 0),
                                "청량산 등산"));

        // then (create committed & async synced)
        assertThat(createdLatch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(pushedJson.get()).contains("청량산 등산");
        assertThat(pushedJson.get()).contains("\"timezone\" : \"Asia/Seoul\"");

        // given (GitHub fails on update)
        reset(gitHubSyncClient);
        final CountDownLatch failedLatch = new CountDownLatch(1);
        doAnswer(
                        invocation -> {
                            failedLatch.countDown();
                            throw new GitHubSyncException("Simulated GitHub 500");
                        })
                .when(gitHubSyncClient)
                .pushSnapshot(anyString());

        // when (update)
        final ScheduleResponse updated =
                scheduleService.update(
                        created.id(),
                        new ScheduleUpdateCommand(
                                Category.DATE,
                                LocalDate.of(2026, 11, 29),
                                null,
                                LocalTime.of(10, 0),
                                "수정된 산행"));

        // then (DB update still succeeds despite GitHub failure)
        assertThat(failedLatch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(updated.content()).isEqualTo("수정된 산행");
        assertThat(scheduleRepository.findById(created.id()))
                .isPresent()
                .get()
                .extracting("content")
                .isEqualTo("수정된 산행");
    }

    @Test
    @DisplayName("유효성 검증 실패로 트랜잭션이 롤백되면 GitHub 동기화는 실행되지 않는다")
    void should_notTriggerSync_when_transactionRollsBack() throws Exception {
        // given
        doThrow(new AssertionError("롤백 시 호출되면 안 됩니다"))
                .when(gitHubSyncClient)
                .pushSnapshot(anyString());
        final ScheduleCreateCommand invalidCommand =
                new ScheduleCreateCommand(
                        Category.SEUNGKWON, LocalDate.of(2026, 11, 28), null, null, "   ");

        // when & then
        assertThatThrownBy(() -> scheduleService.create(invalidCommand))
                .isInstanceOf(InvalidScheduleException.class);
        Thread.sleep(150L);
        verify(gitHubSyncClient, never()).pushSnapshot(anyString());
    }
}
