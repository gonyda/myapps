package com.myapps.web.myshare.interfaces.api;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myapps.web.myshare.application.exception.GitHubShareException;
import com.myapps.web.myshare.application.exception.ShareConfigurationException;
import com.myapps.web.myshare.application.exception.ShareNotFoundException;
import com.myapps.web.myshare.application.service.SharePageService;
import com.myapps.web.myshare.domain.exception.InvalidShareSlugException;
import com.myapps.web.myshare.domain.model.SharePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** {@link ShareController} 및 {@link ShareExceptionHandler}의 웹 슬라이스 테스트. */
@WebMvcTest(ShareController.class)
class ShareControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SharePageService sharePageService;

    @Test
    @DisplayName("GET /share/{slug} 성공 시 200 OK와 UTF-8 HTML, 보안/캐시 헤더를 반환하고 CSP는 강제하지 않는다")
    void should_return200HtmlWithExpectedHeaders_when_pageFound() throws Exception {
        // given
        final String html =
                "<!DOCTYPE html><html><head><style>body{color:red;}</style></head>"
                        + "<body><h1>모바일 보고서</h1><script>window.__ok=true;</script></body></html>";
        given(sharePageService.loadPage("weekly_report-1"))
                .willReturn(new SharePage("weekly_report-1", html, "sha-xyz"));

        // when & then
        mockMvc.perform(get("/share/weekly_report-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(html))
                .andExpect(header().string(HttpHeaders.ETAG, "\"sha-xyz\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, private"))
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().doesNotExist("Content-Security-Policy"))
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    @DisplayName("클라이언트의 If-None-Match가 현재 SHA와 일치하면 304 Not Modified를 반환한다")
    void should_return304NotModified_when_ifNoneMatchMatchesSha() throws Exception {
        // given
        given(sharePageService.loadPage("report"))
                .willReturn(new SharePage("report", "<h1>hi</h1>", "sha-123"));

        // when & then
        mockMvc.perform(get("/share/report").header(HttpHeaders.IF_NONE_MATCH, "W/\"sha-123\""))
                .andExpect(status().isNotModified())
                .andExpect(header().string(HttpHeaders.ETAG, "\"sha-123\""));
    }

    @Test
    @DisplayName("If-None-Match가 불일치하면 200 OK와 HTML 본문을 반환한다")
    void should_return200Ok_when_ifNoneMatchDoesNotMatch() throws Exception {
        // given
        given(sharePageService.loadPage("report"))
                .willReturn(new SharePage("report", "<h1>v2</h1>", "sha-new"));

        // when & then
        mockMvc.perform(get("/share/report").header(HttpHeaders.IF_NONE_MATCH, "\"sha-old\""))
                .andExpect(status().isOk())
                .andExpect(content().string("<h1>v2</h1>"));
    }

    @Test
    @DisplayName("유효하지 않은 슬러그(대문자·점·경로탐색) 요청은 400 Bad Request를 반환한다")
    void should_return400BadRequest_when_slugInvalid() throws Exception {
        // given
        given(sharePageService.loadPage(".."))
                .willThrow(new InvalidShareSlugException("유효하지 않은 슬러그"));
        given(sharePageService.loadPage("report.html"))
                .willThrow(new InvalidShareSlugException("유효하지 않은 슬러그"));
        given(sharePageService.loadPage("UpperCase"))
                .willThrow(new InvalidShareSlugException("유효하지 않은 슬러그"));

        // when & then
        mockMvc.perform(get("/share/..")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/share/report.html")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/share/UpperCase")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("data.json 등 .json 파일 직접 접근 및 중첩 경로 요청은 404 Not Found로 차단한다")
    void should_return404NotFound_when_nestedOrJsonPathRequested() throws Exception {
        // given & when & then
        mockMvc.perform(get("/share/todo/data.json")).andExpect(status().isNotFound());
        mockMvc.perform(get("/share/todo/index.html")).andExpect(status().isNotFound());
        mockMvc.perform(get("/share/a/b")).andExpect(status().isNotFound());
        mockMvc.perform(get("/share/data.json")).andExpect(status().isNotFound());
        mockMvc.perform(get("/share/TODO.JSON")).andExpect(status().isNotFound());
        then(sharePageService).should(never()).loadPage("todo");
        then(sharePageService).should(never()).loadPage("a");
        then(sharePageService).should(never()).loadPage("data.json");
    }

    @Test
    @DisplayName("/share 인덱스 요청 및 미존재 파일 요청은 404 Not Found를 반환한다")
    void should_return404NotFound_when_indexOrMissingSlugRequested() throws Exception {
        // given
        given(sharePageService.loadPage("missing"))
                .willThrow(new ShareNotFoundException("찾을 수 없습니다."));

        // when & then
        mockMvc.perform(get("/share")).andExpect(status().isNotFound());
        mockMvc.perform(get("/share/")).andExpect(status().isNotFound());
        mockMvc.perform(get("/share/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("text/html;charset=UTF-8"));
    }

    @Test
    @DisplayName("GitHub API 장애 시 502 Bad Gateway, 설정 누락 시 503 Service Unavailable을 반환한다")
    void should_return502Or503_when_upstreamOrConfigFails() throws Exception {
        // given
        given(sharePageService.loadPage("upstream-fail"))
                .willThrow(new GitHubShareException("GitHub 500"));
        given(sharePageService.loadPage("config-missing"))
                .willThrow(new ShareConfigurationException("토큰 누락"));

        // when & then
        mockMvc.perform(get("/share/upstream-fail")).andExpect(status().isBadGateway());
        mockMvc.perform(get("/share/config-missing")).andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("GET /health 요청 시 200 OK를 반환한다")
    void should_return200Ok_when_healthRequested() throws Exception {
        // given & when & then
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}
