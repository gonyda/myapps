package com.myapps.web.myshare.interfaces.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myapps.web.myshare.application.service.MiniAppGitHubSyncService;
import com.myapps.web.myshare.application.service.MiniAppStoreService;
import com.myapps.web.myshare.domain.exception.InvalidMiniAppKeyException;
import com.myapps.web.myshare.domain.exception.InvalidMiniAppPayloadException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@link MiniAppController}, {@link MiniAppPinFilter}, {@link MiniAppExceptionHandler} 웹 슬라이스 테스트.
 */
@WebMvcTest(MiniAppController.class)
class MiniAppControllerTest {

    private static final String HEADER_PIN = "X-Mini-Pin";
    private static final String VALID_PIN = "test-mini-pin";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MiniAppStoreService miniAppStoreService;

    @MockitoBean private MiniAppGitHubSyncService miniAppGitHubSyncService;

    @Test
    @DisplayName("X-Mini-Pin 헤더가 없거나 불일치하면 GET/PUT 모두 401 Unauthorized를 반환한다")
    void should_return401Unauthorized_when_pinMissingOrInvalid() throws Exception {
        // given & when & then
        mockMvc.perform(get("/api/mini/todo"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        mockMvc.perform(
                        put("/api/mini/todo")
                                .header(HEADER_PIN, "wrong-pin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"a\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        then(miniAppStoreService).shouldHaveNoInteractions();
        then(miniAppGitHubSyncService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("유효한 PIN으로 GET /api/mini/{appKey} 호출 시 200 OK와 저장된 JSON을 그대로 반환한다")
    void should_return200Json_when_getWithValidPin() throws Exception {
        // given
        final String storedJson = "{\"items\":[\"coffee\",\"book\"]}";
        given(miniAppStoreService.loadJson("todo-1")).willReturn(storedJson);

        // when & then
        mockMvc.perform(get("/api/mini/todo-1").header(HEADER_PIN, VALID_PIN))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(storedJson))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow"));
    }

    @Test
    @DisplayName("유효한 PIN과 JSON으로 PUT /api/mini/{appKey} 호출 시 저장 후 비동기 동기화를 호출하고 200 OK를 반환한다")
    void should_upsertAndTriggerAsyncSync_when_putValidJson() throws Exception {
        // given
        final String body = "{\"count\":42,\"name\":\"미니앱\"}";
        given(miniAppStoreService.upsertJson("counter_app", body)).willReturn(body);

        // when & then
        mockMvc.perform(
                        put("/api/mini/counter_app")
                                .header(HEADER_PIN, VALID_PIN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(body));

        then(miniAppGitHubSyncService).should().syncAsync("counter_app", body);
    }

    @Test
    @DisplayName("잘못된 JSON 본문으로 PUT 호출 시 400 Bad Request를 반환하고 GitHub 동기화를 호출하지 않는다")
    void should_return400BadRequest_when_putInvalidJson() throws Exception {
        // given
        given(miniAppStoreService.upsertJson("counter_app", "{broken"))
                .willThrow(new InvalidMiniAppPayloadException("유효한 JSON 형식이 아닙니다."));

        // when & then
        mockMvc.perform(
                        put("/api/mini/counter_app")
                                .header(HEADER_PIN, VALID_PIN)
                                .content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists());

        then(miniAppGitHubSyncService).should(never()).syncAsync(anyString(), anyString());
    }

    @Test
    @DisplayName("허용되지 않은 appKey 또는 하위/루트 경로 요청은 400 Bad Request JSON을 반환한다")
    void should_return400BadRequest_when_appKeyOrPathInvalid() throws Exception {
        // given
        given(miniAppStoreService.loadJson("UpperCase"))
                .willThrow(new InvalidMiniAppKeyException("허용되지 않은 미니앱 키 형식입니다."));

        // when & then
        mockMvc.perform(get("/api/mini/UpperCase").header(HEADER_PIN, VALID_PIN))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/api/mini").header(HEADER_PIN, VALID_PIN))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/api/mini/a/b").header(HEADER_PIN, VALID_PIN))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("예기치 못한 내부 예외 발생 시 500 Internal Server Error JSON을 반환한다")
    void should_return500InternalServerError_when_unexpectedErrorOccurs() throws Exception {
        // given
        given(miniAppStoreService.loadJson("todo"))
                .willThrow(new IllegalStateException("DB connection lost"));

        // when & then
        mockMvc.perform(get("/api/mini/todo").header(HEADER_PIN, VALID_PIN))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}
