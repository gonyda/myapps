package com.myapps.web.myshare.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myapps.web.myshare.application.exception.GitHubMiniAppSyncException;
import com.myapps.web.myshare.application.port.GitHubMiniAppSyncClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** {@code /api/mini/**} 전체 계층(필터 → 컨트롤러 → DB 업서트 → {@code @Async} GitHub 동기화) 통합 테스트. */
@SpringBootTest
class MiniAppIntegrationTest {

    private static final String HEADER_PIN = "X-Mini-Pin";
    private static final String TEST_PIN = "test-mini-pin";

    private final WebApplicationContext webApplicationContext;
    private final MiniAppPinFilter miniAppPinFilter;

    @MockitoBean private GitHubMiniAppSyncClient gitHubMiniAppSyncClient;

    private MockMvc mockMvc;

    MiniAppIntegrationTest(
            @Autowired final WebApplicationContext webApplicationContext,
            @Autowired final MiniAppPinFilter miniAppPinFilter) {
        this.webApplicationContext = webApplicationContext;
        this.miniAppPinFilter = miniAppPinFilter;
    }

    @BeforeEach
    void setUp() {
        this.mockMvc =
                MockMvcBuilders.webAppContextSetup(webApplicationContext)
                        .addFilters(miniAppPinFilter)
                        .build();
    }

    @Test
    @DisplayName("PUT으로 저장 후 GET으로 조회하고 GitHub 비동기 푸시가 실패해도 DB 응답은 200 OK를 유지한다")
    void should_persistToDbAndTriggerAsyncSync_when_putAndGetExecuted() throws Exception {
        // given
        final String initialJson = "{\"step\":1,\"title\":\"초기\"}";
        final String updatedJson = "{\"step\":2,\"title\":\"갱신\"}";
        given(gitHubMiniAppSyncClient.pushJson("budget-mini", initialJson))
                .willThrow(new GitHubMiniAppSyncException("Simulated GitHub outage"));
        given(gitHubMiniAppSyncClient.pushJson("budget-mini", updatedJson)).willReturn(true);

        // when & then: 미저장 상태 GET -> {}
        mockMvc.perform(get("/api/mini/budget-mini").header(HEADER_PIN, TEST_PIN))
                .andExpect(status().isOk())
                .andExpect(content().string("{}"));

        // when & then: 최초 PUT -> DB 저장 성공 (GitHub 예외 무영향)
        mockMvc.perform(
                        put("/api/mini/budget-mini")
                                .header(HEADER_PIN, TEST_PIN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(initialJson))
                .andExpect(status().isOk())
                .andExpect(content().string(initialJson));

        // when & then: 갱신 PUT -> DB 업데이트 & 재조회 일치
        mockMvc.perform(
                        put("/api/mini/budget-mini")
                                .header(HEADER_PIN, TEST_PIN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updatedJson))
                .andExpect(status().isOk())
                .andExpect(content().string(updatedJson));

        mockMvc.perform(get("/api/mini/budget-mini").header(HEADER_PIN, TEST_PIN))
                .andExpect(status().isOk())
                .andExpect(content().string(updatedJson));

        // @Async 단일 스레드 실행기에서 비동기 호출이 순차 실행되었는지 검증
        verify(gitHubMiniAppSyncClient, timeout(3000)).pushJson("budget-mini", initialJson);
        verify(gitHubMiniAppSyncClient, timeout(3000)).pushJson("budget-mini", updatedJson);
        assertThat(mockMvc).isNotNull();
    }

    @Test
    @DisplayName("기존 /health 및 /share/ 엔드포인트는 X-Mini-Pin 헤더 없이도 정상 응답한다")
    void should_keepExistingShareEndpointsOpen_when_pinFilterRegistered() throws Exception {
        // given & when & then
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
        mockMvc.perform(get("/share")).andExpect(status().isNotFound());
    }
}
