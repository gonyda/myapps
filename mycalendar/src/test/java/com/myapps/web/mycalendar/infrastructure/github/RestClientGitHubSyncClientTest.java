package com.myapps.web.mycalendar.infrastructure.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.myapps.web.mycalendar.application.exception.GitHubSyncException;
import com.myapps.web.mycalendar.infrastructure.config.GitHubSyncProperties;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link RestClientGitHubSyncClient}의 GitHub Contents API 연동 테스트.
 *
 * <p>{@link MockRestServiceServer}를 이용해 GET 200/404, PUT 200/201, 409 Conflict 재시도, 5xx/네트워크 예외,
 * 비활성화/토큰 누락 시 무호출을 검증합니다.
 */
class RestClientGitHubSyncClientTest {

    private static final String BASE_URL = "https://api.github.com";
    private static final String GET_URL =
            "https://api.github.com/repos/gonyda/mike/contents/calendar/mycalendar.json?ref=main";
    private static final String PUT_URL =
            "https://api.github.com/repos/gonyda/mike/contents/calendar/mycalendar.json";
    private static final String TOKEN = "test-github-token";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        objectMapper = JsonMapper.builder().build();
    }

    @Test
    @DisplayName("기존 파일이 존재할 때 GET 200으로 sha를 추출한 뒤 PUT 200으로 덮어쓴다")
    void should_updateExistingFileWithSha_when_getReturns200() {
        // given
        final GitHubSyncProperties properties = activeProperties();
        final RestClientGitHubSyncClient client =
                new RestClientGitHubSyncClient(restClientBuilder.build(), properties);
        final String snapshotJson = "{\"content\":\"청량산 등산\"}\n";
        final String expectedBase64 =
                Base64.getEncoder().encodeToString(snapshotJson.getBytes(StandardCharsets.UTF_8));

        mockServer
                .expect(requestTo(GET_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(
                        withSuccess("{\"sha\":\"existing-sha-123\"}", MediaType.APPLICATION_JSON));

        mockServer
                .expect(requestTo(PUT_URL))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(
                        request -> {
                            final MockClientHttpRequest mockRequest =
                                    (MockClientHttpRequest) request;
                            final JsonNode body =
                                    objectMapper.readTree(mockRequest.getBodyAsString());
                            assertThat(body.get("message").asString())
                                    .isEqualTo("chore(calendar): sync mycalendar events");
                            assertThat(body.get("branch").asString()).isEqualTo("main");
                            assertThat(body.get("sha").asString()).isEqualTo("existing-sha-123");
                            assertThat(body.get("content").asString()).isEqualTo(expectedBase64);
                            assertThat(body.get("content").asString()).doesNotContain("\n", "\r");
                        })
                .andRespond(
                        withSuccess("{\"commit\":{\"sha\":\"new\"}}", MediaType.APPLICATION_JSON));

        // when
        client.pushSnapshot(snapshotJson);

        // then
        mockServer.verify();
    }

    @Test
    @DisplayName("파일이 최초 생성일 때 GET 404를 받고 sha 없이 PUT 201로 생성한다")
    void should_createFileWithoutSha_when_getReturns404() {
        // given
        final GitHubSyncProperties properties = activeProperties();
        final RestClientGitHubSyncClient client =
                new RestClientGitHubSyncClient(restClientBuilder.build(), properties);
        final String snapshotJson = "{\"totalCount\":0,\"events\":[]}\n";

        mockServer
                .expect(requestTo(GET_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        mockServer
                .expect(requestTo(PUT_URL))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(
                        request -> {
                            final MockClientHttpRequest mockRequest =
                                    (MockClientHttpRequest) request;
                            final JsonNode body =
                                    objectMapper.readTree(mockRequest.getBodyAsString());
                            assertThat(body.has("sha")).isFalse();
                            assertThat(body.get("branch").asString()).isEqualTo("main");
                        })
                .andRespond(
                        withStatus(HttpStatus.CREATED)
                                .location(URI.create(PUT_URL))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body("{\"content\":{\"sha\":\"created-sha\"}}"));

        // when
        client.pushSnapshot(snapshotJson);

        // then
        mockServer.verify();
    }

    @Test
    @DisplayName("PUT 중 409 Conflict가 발생하면 최신 sha를 재조회해 최대 1회 재시도한다")
    void should_refetchShaAndRetryOnce_when_putReturns409Conflict() {
        // given
        final GitHubSyncProperties properties = activeProperties();
        final RestClientGitHubSyncClient client =
                new RestClientGitHubSyncClient(restClientBuilder.build(), properties);
        final String snapshotJson = "{\"totalCount\":1}\n";

        mockServer
                .expect(requestTo(GET_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"sha\":\"stale-sha\"}", MediaType.APPLICATION_JSON));

        mockServer
                .expect(requestTo(PUT_URL))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        mockServer
                .expect(requestTo(GET_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"sha\":\"latest-sha\"}", MediaType.APPLICATION_JSON));

        mockServer
                .expect(requestTo(PUT_URL))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(
                        request -> {
                            final MockClientHttpRequest mockRequest =
                                    (MockClientHttpRequest) request;
                            final JsonNode body =
                                    objectMapper.readTree(mockRequest.getBodyAsString());
                            assertThat(body.get("sha").asString()).isEqualTo("latest-sha");
                        })
                .andRespond(
                        withSuccess(
                                "{\"content\":{\"sha\":\"updated\"}}", MediaType.APPLICATION_JSON));

        // when
        client.pushSnapshot(snapshotJson);

        // then
        mockServer.verify();
    }

    @Test
    @DisplayName("재시도 후에도 409 Conflict가 반복되면 GitHubSyncException을 던진다")
    void should_throwGitHubSyncException_when_retryAlsoFailsWith409() {
        // given
        final GitHubSyncProperties properties = activeProperties();
        final RestClientGitHubSyncClient client =
                new RestClientGitHubSyncClient(restClientBuilder.build(), properties);

        mockServer
                .expect(requestTo(GET_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"sha\":\"sha-1\"}", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(PUT_URL))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        mockServer
                .expect(requestTo(GET_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"sha\":\"sha-2\"}", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(PUT_URL))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        // when & then
        assertThatThrownBy(() -> client.pushSnapshot("{}")).isInstanceOf(GitHubSyncException.class);
        mockServer.verify();
    }

    @Test
    @DisplayName("GitHub API 5xx 응답 또는 네트워크 예외 발생 시 GitHubSyncException으로 래핑한다")
    void should_throwGitHubSyncException_when_serverErrorOrNetworkExceptionOccurs() {
        // given
        final GitHubSyncProperties properties = activeProperties();
        final RestClientGitHubSyncClient client =
                new RestClientGitHubSyncClient(restClientBuilder.build(), properties);

        mockServer
                .expect(requestTo(GET_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // when & then
        assertThatThrownBy(() -> client.pushSnapshot("{}")).isInstanceOf(GitHubSyncException.class);
        mockServer.verify();

        // given (network timeout simulation)
        mockServer.reset();
        mockServer
                .expect(requestTo(GET_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        request -> {
                            throw new ResourceAccessException("Read timed out");
                        });

        // when & then
        assertThatThrownBy(() -> client.pushSnapshot("{}")).isInstanceOf(GitHubSyncException.class);
        mockServer.verify();
    }

    @Test
    @DisplayName("enabled가 false이거나 토큰이 비어 있으면 HTTP 요청을 수행하지 않는다")
    void should_notCallHttp_when_disabledOrTokenMissing() {
        // given
        final GitHubSyncProperties disabledProps =
                new GitHubSyncProperties(
                        false, TOKEN, "gonyda", "mike", "main", "calendar/mycalendar.json", "msg");
        final GitHubSyncProperties blankTokenProps =
                new GitHubSyncProperties(
                        true, "   ", "gonyda", "mike", "main", "calendar/mycalendar.json", "msg");
        final RestClient restClient = restClientBuilder.build();
        final RestClientGitHubSyncClient disabledClient =
                new RestClientGitHubSyncClient(restClient, disabledProps);
        final RestClientGitHubSyncClient blankTokenClient =
                new RestClientGitHubSyncClient(restClient, blankTokenProps);

        // when
        disabledClient.pushSnapshot("{}");
        blankTokenClient.pushSnapshot("{}");

        // then
        mockServer.verify();
    }

    private GitHubSyncProperties activeProperties() {
        return new GitHubSyncProperties(
                true,
                TOKEN,
                "gonyda",
                "mike",
                "main",
                "/calendar/mycalendar.json",
                "chore(calendar): sync mycalendar events");
    }
}
