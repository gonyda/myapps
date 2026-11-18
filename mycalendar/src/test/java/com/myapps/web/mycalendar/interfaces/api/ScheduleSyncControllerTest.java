package com.myapps.web.mycalendar.interfaces.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myapps.web.mycalendar.application.dto.GitHubSyncResult;
import com.myapps.web.mycalendar.application.service.GitHubCalendarSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** {@link ScheduleSyncController}의 웹 슬라이스 테스트. */
@WebMvcTest(ScheduleSyncController.class)
class ScheduleSyncControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GitHubCalendarSyncService gitHubCalendarSyncService;

    @Test
    @DisplayName("POST /schedules/sync/github 성공 시 200 OK와 동기화 결과를 반환한다")
    void should_return200Ok_when_manualSyncSucceeds() throws Exception {
        // given
        when(gitHubCalendarSyncService.syncNow())
                .thenReturn(GitHubSyncResult.success(3, "2026-11-18T19:30:00+09:00"));

        // when & then
        mockMvc.perform(post("/schedules/sync/github"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.syncedAt").value("2026-11-18T19:30:00+09:00"));
    }

    @Test
    @DisplayName("POST /schedules/sync/github 스킵 시 200 OK와 SKIPPED 상태를 반환한다")
    void should_return200Ok_when_manualSyncSkipped() throws Exception {
        // given
        when(gitHubCalendarSyncService.syncNow())
                .thenReturn(GitHubSyncResult.skipped("GitHub sync is disabled"));

        // when & then
        mockMvc.perform(post("/schedules/sync/github"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"));
    }

    @Test
    @DisplayName("POST /schedules/sync/github 실패 시 502 Bad Gateway와 FAILED 상태를 반환한다")
    void should_return502BadGateway_when_manualSyncFails() throws Exception {
        // given
        when(gitHubCalendarSyncService.syncNow())
                .thenReturn(GitHubSyncResult.failed("GitHub API 500"));

        // when & then
        mockMvc.perform(post("/schedules/sync/github"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("GitHub API 500"));
    }
}
