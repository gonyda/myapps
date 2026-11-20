package com.myapps.web.myshare.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.myapps.web.myshare.application.exception.GitHubMiniAppSyncException;
import com.myapps.web.myshare.application.port.GitHubMiniAppSyncClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link MiniAppGitHubSyncService} 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class MiniAppGitHubSyncServiceTest {

    @Mock private GitHubMiniAppSyncClient gitHubMiniAppSyncClient;

    @Test
    @DisplayName("동기화 성공 시 GitHub 클라이언트를 호출하여 JSON을 푸시한다")
    void should_pushJson_when_syncAsyncCalled() {
        // given
        final MiniAppGitHubSyncService syncService =
                new MiniAppGitHubSyncService(gitHubMiniAppSyncClient);
        given(gitHubMiniAppSyncClient.pushJson("todo", "{\"ok\":true}")).willReturn(true);

        // when
        syncService.syncAsync("todo", "{\"ok\":true}");

        // then
        then(gitHubMiniAppSyncClient).should().pushJson("todo", "{\"ok\":true}");
    }

    @Test
    @DisplayName("GitHub 클라이언트가 예외를 던져도 비동기 서비스는 예외를 삼키고 정상 종료된다")
    void should_swallowException_when_gitHubClientFails() {
        // given
        final MiniAppGitHubSyncService syncService =
                new MiniAppGitHubSyncService(gitHubMiniAppSyncClient);
        given(gitHubMiniAppSyncClient.pushJson("todo", "{}"))
                .willThrow(new GitHubMiniAppSyncException("GitHub timeout"));

        // when & then
        assertThatCode(() -> syncService.syncAsync("todo", "{}")).doesNotThrowAnyException();
        then(gitHubMiniAppSyncClient).should().pushJson("todo", "{}");
    }
}
