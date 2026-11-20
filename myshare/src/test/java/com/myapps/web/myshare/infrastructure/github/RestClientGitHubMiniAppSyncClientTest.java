package com.myapps.web.myshare.infrastructure.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.myapps.web.myshare.application.exception.GitHubMiniAppSyncException;
import com.myapps.web.myshare.infrastructure.config.GitHubShareProperties;
import com.myapps.web.myshare.infrastructure.config.MiniAppProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** {@link RestClientGitHubMiniAppSyncClient} 단위 테스트. */
class RestClientGitHubMiniAppSyncClientTest {

    private static final String CONTENTS_URL =
            "https://api.github.com/repos/gonyda/mike/contents/share/todo/data.json";

    private MockRestServiceServer mockServer;
    private RestClientGitHubMiniAppSyncClient client;
    private RestClient.Builder restClientBuilder;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl("https://api.github.com");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = createClient(true, "ghp_test_token", true);
    }

    @Test
    @DisplayName("기존 파일이 존재하면 GET sha를 포함해 Base64 UTF-8 JSON을 PUT으로 갱신한다")
    void should_updateExistingFileWithSha_when_getReturns200() {
        // given
        final String json = "{\"title\":\"한글 미니앱\"}";
        final String expectedBase64 =
                Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        mockServer
                .expect(requestTo(CONTENTS_URL + "?ref=main"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer ghp_test_token"))
                .andRespond(withSuccess("{\"sha\":\"old-sha-1\"}", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(CONTENTS_URL))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.message").value("chore(mini): sync todo/data.json"))
                .andExpect(jsonPath("$.content").value(expectedBase64))
                .andExpect(jsonPath("$.branch").value("main"))
                .andExpect(jsonPath("$.sha").value("old-sha-1"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        // when
        final boolean pushed = client.pushJson("todo", json);

        // then
        assertThat(pushed).isTrue();
        mockServer.verify();
    }

    @Test
    @DisplayName("대상 파일이 404이면 sha 없이 신규 생성 PUT을 요청한다")
    void should_createFileWithoutSha_when_getReturns404() {
        // given
        mockServer
                .expect(requestTo(CONTENTS_URL + "?ref=main"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        mockServer
                .expect(requestTo(CONTENTS_URL))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.sha").doesNotExist())
                .andRespond(withStatus(HttpStatus.CREATED));

        // when
        final boolean pushed = client.pushJson("todo", "{\"new\":true}");

        // then
        assertThat(pushed).isTrue();
        mockServer.verify();
    }

    @Test
    @DisplayName("PUT 중 409 Conflict가 발생하면 최신 sha를 재조회해 재시도한다")
    void should_refetchShaAndRetry_when_putReturns409Conflict() {
        // given
        mockServer
                .expect(requestTo(CONTENTS_URL + "?ref=main"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"sha\":\"sha-v1\"}", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(CONTENTS_URL))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.sha").value("sha-v1"))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        mockServer
                .expect(requestTo(CONTENTS_URL + "?ref=main"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"sha\":\"sha-v2\"}", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(CONTENTS_URL))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.sha").value("sha-v2"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        // when
        final boolean pushed = client.pushJson("todo", "{\"v\":2}");

        // then
        assertThat(pushed).isTrue();
        mockServer.verify();
    }

    @Test
    @DisplayName("동기화가 비활성화되거나 토큰이 비어 있으면 HTTP 호출 없이 false를 반환한다")
    void should_skipWithoutHttpCall_when_disabledOrTokenMissing() {
        // given
        final RestClientGitHubMiniAppSyncClient disabledClient =
                createClient(true, "ghp_tok", false);
        final RestClientGitHubMiniAppSyncClient noTokenClient = createClient(true, "", true);

        // when & then
        assertThat(disabledClient.pushJson("todo", "{}")).isFalse();
        assertThat(noTokenClient.pushJson("todo", "{}")).isFalse();
        mockServer.verify();
    }

    @Test
    @DisplayName("재시도 한도 초과 또는 GitHub 5xx 오류 시 GitHubMiniAppSyncException을 던진다")
    void should_throwException_when_retriesExhaustedOrServerError() {
        // given
        mockServer
                .expect(requestTo(CONTENTS_URL + "?ref=main"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // when & then
        assertThatThrownBy(() -> client.pushJson("todo", "{}"))
                .isInstanceOf(GitHubMiniAppSyncException.class);
        assertThatThrownBy(() -> client.pushJson(" ", "{}"))
                .isInstanceOf(GitHubMiniAppSyncException.class);
    }

    @Test
    @DisplayName("data.json이 존재하면 sha를 조회한 뒤 해당 파일만 DELETE 커밋·푸시한다")
    void should_deleteExistingDataJsonWithSha_when_getReturns200() {
        // given
        mockServer
                .expect(requestTo(CONTENTS_URL + "?ref=main"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer ghp_test_token"))
                .andRespond(withSuccess("{\"sha\":\"del-sha-1\"}", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(CONTENTS_URL))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(jsonPath("$.message").value("chore(mini): delete todo/data.json"))
                .andExpect(jsonPath("$.sha").value("del-sha-1"))
                .andExpect(jsonPath("$.branch").value("main"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        // when
        final boolean deleted = client.deleteJson("todo");

        // then
        assertThat(deleted).isTrue();
        mockServer.verify();
    }

    @Test
    @DisplayName("GitHub에 data.json이 이미 없으면 DELETE 호출 없이 조용히 false를 반환한다")
    void should_skipQuietlyWithoutDelete_when_fileAlreadyMissing() {
        // given
        mockServer
                .expect(requestTo(CONTENTS_URL + "?ref=main"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // when
        final boolean deleted = client.deleteJson("todo");

        // then
        assertThat(deleted).isFalse();
        mockServer.verify();
    }

    @Test
    @DisplayName("DELETE 중 409 Conflict가 발생하면 최신 sha를 재조회해 재시도한다")
    void should_refetchShaAndRetryDelete_when_deleteReturns409Conflict() {
        // given
        mockServer
                .expect(requestTo(CONTENTS_URL + "?ref=main"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"sha\":\"del-v1\"}", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(CONTENTS_URL))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(jsonPath("$.sha").value("del-v1"))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        mockServer
                .expect(requestTo(CONTENTS_URL + "?ref=main"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"sha\":\"del-v2\"}", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(CONTENTS_URL))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(jsonPath("$.sha").value("del-v2"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        // when
        final boolean deleted = client.deleteJson("todo");

        // then
        assertThat(deleted).isTrue();
        mockServer.verify();
    }

    @Test
    @DisplayName("동기화 비활성화 또는 토큰 누락 시 deleteJson은 HTTP 호출 없이 false를 반환한다")
    void should_skipDeleteWithoutHttpCall_when_disabledOrTokenMissing() {
        // given
        final RestClientGitHubMiniAppSyncClient disabledClient =
                createClient(true, "ghp_tok", false);
        final RestClientGitHubMiniAppSyncClient noTokenClient = createClient(true, "", true);

        // when & then
        assertThat(disabledClient.deleteJson("todo")).isFalse();
        assertThat(noTokenClient.deleteJson("todo")).isFalse();
        mockServer.verify();
    }

    @Test
    @DisplayName("deleteJson 호출 시 공백 키 또는 GitHub 5xx 오류이면 GitHubMiniAppSyncException을 던진다")
    void should_throwException_when_deleteBlankKeyOrServerError() {
        // given
        mockServer
                .expect(requestTo(CONTENTS_URL + "?ref=main"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"sha\":\"del-sha\"}", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(CONTENTS_URL))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // when & then
        assertThatThrownBy(() -> client.deleteJson("todo"))
                .isInstanceOf(GitHubMiniAppSyncException.class);
        assertThatThrownBy(() -> client.deleteJson(" "))
                .isInstanceOf(GitHubMiniAppSyncException.class);
    }

    private RestClientGitHubMiniAppSyncClient createClient(
            final boolean shareGitHubEnabled, final String token, final boolean miniGitHubEnabled) {
        final GitHubShareProperties shareProps =
                new GitHubShareProperties(
                        shareGitHubEnabled, token, "gonyda", "mike", "main", "share");
        final MiniAppProperties miniProps =
                new MiniAppProperties(
                        "pin",
                        1_048_576,
                        new MiniAppProperties.GitHub(
                                miniGitHubEnabled, "share", "chore(mini): sync"));
        return new RestClientGitHubMiniAppSyncClient(
                restClientBuilder.build(), shareProps, miniProps);
    }
}
