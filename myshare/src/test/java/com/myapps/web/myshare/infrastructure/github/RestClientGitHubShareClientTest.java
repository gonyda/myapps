package com.myapps.web.myshare.infrastructure.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.myapps.web.myshare.application.dto.ShareFetchResult;
import com.myapps.web.myshare.application.exception.GitHubShareException;
import com.myapps.web.myshare.application.exception.ShareConfigurationException;
import com.myapps.web.myshare.infrastructure.config.GitHubShareProperties;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * {@link RestClientGitHubShareClient}의 GitHub Contents API 읽기 연동 테스트.
 *
 * <p>{@link MockRestServiceServer}를 사용해 MIME Base64 UTF-8 디코딩, {@code 304 Not Modified}, {@code 404
 * Not Found}, 대용량 파일 Raw 폴백, 오류/타임아웃 래핑, 토큰 미설정 차단을 검증합니다.
 */
class RestClientGitHubShareClientTest {

    private static final String BASE_URL = "https://api.github.com";
    private static final String EXPECTED_URL =
            "https://api.github.com/repos/gonyda/mike/contents/share/hello-mike/index.html?ref=main";
    private static final String EXPECTED_LEGACY_URL =
            "https://api.github.com/repos/gonyda/mike/contents/share/hello-mike.html?ref=main";
    private static final String TOKEN = "test-read-only-token";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    @DisplayName("GitHub JSON 응답의 줄바꿈 포함 Base64 본문을 UTF-8 HTML로 디코딩해 반환한다")
    void should_decodeMultilineBase64Utf8Html_when_gitHubReturns200() {
        // given
        final RestClientGitHubShareClient client =
                new RestClientGitHubShareClient(restClientBuilder.build(), activeProperties());
        final String html =
                "<!DOCTYPE html><html><body><h1>안녕 Mike 🚀</h1><script>console.log(1);</script></body></html>";
        final String rawBase64 =
                Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
        final String multilineBase64 = rawBase64.substring(0, 20) + "\\n" + rawBase64.substring(20);
        final String jsonResponse =
                "{\"type\":\"file\",\"encoding\":\"base64\",\"sha\":\"abc-sha-123\",\"content\":\""
                        + multilineBase64
                        + "\"}";

        mockServer
                .expect(requestTo(EXPECTED_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        // when
        final ShareFetchResult result = client.fetchHtml("hello-mike", null);

        // then
        assertThat(result).isEqualTo(new ShareFetchResult.Found(html, "abc-sha-123"));
        mockServer.verify();
    }

    @Test
    @DisplayName("knownSha가 있으면 If-None-Match를 전송하고 304 응답 시 NotModified를 반환한다")
    void should_sendIfNoneMatchAndReturnNotModified_when_gitHubReturns304() {
        // given
        final RestClientGitHubShareClient client =
                new RestClientGitHubShareClient(restClientBuilder.build(), activeProperties());

        mockServer
                .expect(requestTo(EXPECTED_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.IF_NONE_MATCH, "\"abc-sha-123\""))
                .andRespond(withStatus(HttpStatus.NOT_MODIFIED));

        // when
        final ShareFetchResult result = client.fetchHtml("hello-mike", "abc-sha-123");

        // then
        assertThat(result).isInstanceOf(ShareFetchResult.NotModified.class);
        mockServer.verify();
    }

    @Test
    @DisplayName("index.html이 404이면 하위 호환 경로 share/{slug}.html로 폴백 조회한다")
    void should_fallbackToLegacyHtml_when_indexHtmlReturns404() {
        // given
        final RestClientGitHubShareClient client =
                new RestClientGitHubShareClient(restClientBuilder.build(), activeProperties());
        final String legacyHtml = "<!DOCTYPE html><html><body>legacy html</body></html>";
        final String base64 =
                Base64.getEncoder().encodeToString(legacyHtml.getBytes(StandardCharsets.UTF_8));

        mockServer
                .expect(requestTo(EXPECTED_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        mockServer
                .expect(requestTo(EXPECTED_LEGACY_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                "{\"type\":\"file\",\"encoding\":\"base64\",\"sha\":\"legacy-sha\",\"content\":\""
                                        + base64
                                        + "\"}",
                                MediaType.APPLICATION_JSON));

        // when
        final ShareFetchResult result = client.fetchHtml("hello-mike", null);

        // then
        assertThat(result).isEqualTo(new ShareFetchResult.Found(legacyHtml, "legacy-sha"));
        mockServer.verify();
    }

    @Test
    @DisplayName("index.html과 레거시 html이 모두 404이거나 파일 타입이 dir이면 NotFound를 반환한다")
    void should_returnNotFound_when_404OrNonFileType() {
        // given
        final RestClientGitHubShareClient client =
                new RestClientGitHubShareClient(restClientBuilder.build(), activeProperties());

        mockServer
                .expect(requestTo(EXPECTED_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        mockServer
                .expect(requestTo(EXPECTED_LEGACY_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // when
        final ShareFetchResult notFoundResult = client.fetchHtml("hello-mike", null);

        // then
        assertThat(notFoundResult).isInstanceOf(ShareFetchResult.NotFound.class);
        mockServer.verify();

        // given (dir type on index.html and 404 on legacy)
        mockServer.reset();
        mockServer
                .expect(requestTo(EXPECTED_URL))
                .andRespond(
                        withSuccess(
                                "{\"type\":\"dir\",\"sha\":\"dir-sha\"}",
                                MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(EXPECTED_LEGACY_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // when
        final ShareFetchResult dirResult = client.fetchHtml("hello-mike", null);

        // then
        assertThat(dirResult).isInstanceOf(ShareFetchResult.NotFound.class);
        mockServer.verify();
    }

    @Test
    @DisplayName("encoding이 none인 대용량 파일은 raw+json 미디어 타입으로 폴백 조회한다")
    void should_fallbackToRawMediaType_when_encodingIsNone() {
        // given
        final RestClientGitHubShareClient client =
                new RestClientGitHubShareClient(restClientBuilder.build(), activeProperties());
        final String largeHtml = "<!DOCTYPE html><html><body>대용량 HTML</body></html>";

        mockServer
                .expect(requestTo(EXPECTED_URL))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github+json"))
                .andRespond(
                        withSuccess(
                                "{\"type\":\"file\",\"encoding\":\"none\",\"sha\":\"big-sha\",\"content\":\"\"}",
                                MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(EXPECTED_URL))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github.raw+json"))
                .andRespond(withSuccess(largeHtml, MediaType.TEXT_HTML));

        // when
        final ShareFetchResult result = client.fetchHtml("hello-mike", null);

        // then
        assertThat(result).isEqualTo(new ShareFetchResult.Found(largeHtml, "big-sha"));
        mockServer.verify();
    }

    @Test
    @DisplayName("5xx 오류, 잘못된 Base64 본문, 네트워크 타임아웃 시 GitHubShareException을 던진다")
    void should_throwGitHubShareException_when_serverErrorOrInvalidBase64OrTimeout() {
        // given
        final RestClientGitHubShareClient client =
                new RestClientGitHubShareClient(restClientBuilder.build(), activeProperties());

        mockServer
                .expect(requestTo(EXPECTED_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // when & then
        assertThatThrownBy(() -> client.fetchHtml("hello-mike", null))
                .isInstanceOf(GitHubShareException.class);
        mockServer.verify();

        // given (invalid base64)
        mockServer.reset();
        mockServer
                .expect(requestTo(EXPECTED_URL))
                .andRespond(
                        withSuccess(
                                "{\"type\":\"file\",\"encoding\":\"base64\",\"sha\":\"s\",\"content\":\"!!!invalid!!!\"}",
                                MediaType.APPLICATION_JSON));

        // when & then
        assertThatThrownBy(() -> client.fetchHtml("hello-mike", null))
                .isInstanceOf(GitHubShareException.class);
        mockServer.verify();

        // given (network timeout)
        mockServer.reset();
        mockServer
                .expect(requestTo(EXPECTED_URL))
                .andRespond(
                        request -> {
                            throw new ResourceAccessException("Read timed out");
                        });

        // when & then
        assertThatThrownBy(() -> client.fetchHtml("hello-mike", null))
                .isInstanceOf(GitHubShareException.class);
        mockServer.verify();
    }

    @Test
    @DisplayName("연동 비활성화 또는 토큰 누락 시 HTTP 호출 없이 ShareConfigurationException을 던진다")
    void should_throwConfigurationExceptionWithoutHttpCall_when_inactive() {
        // given
        final GitHubShareProperties disabledProps =
                new GitHubShareProperties(false, TOKEN, "gonyda", "mike", "main", "share");
        final GitHubShareProperties blankTokenProps =
                new GitHubShareProperties(true, "  ", "gonyda", "mike", "main", "share");
        final RestClientGitHubShareClient disabledClient =
                new RestClientGitHubShareClient(restClientBuilder.build(), disabledProps);
        final RestClientGitHubShareClient blankTokenClient =
                new RestClientGitHubShareClient(restClientBuilder.build(), blankTokenProps);

        // when & then
        assertThatThrownBy(() -> disabledClient.fetchHtml("hello-mike", null))
                .isInstanceOf(ShareConfigurationException.class);
        assertThatThrownBy(() -> blankTokenClient.fetchHtml("hello-mike", null))
                .isInstanceOf(ShareConfigurationException.class);
        mockServer.verify();
    }

    private GitHubShareProperties activeProperties() {
        return new GitHubShareProperties(true, TOKEN, "gonyda", "mike", "main", "/share/");
    }
}
