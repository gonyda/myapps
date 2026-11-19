package com.myapps.web.myshare.infrastructure.github;

import com.myapps.web.myshare.application.dto.ShareFetchResult;
import com.myapps.web.myshare.application.exception.GitHubShareException;
import com.myapps.web.myshare.application.exception.ShareConfigurationException;
import com.myapps.web.myshare.application.port.GitHubShareClient;
import com.myapps.web.myshare.infrastructure.config.GitHubShareProperties;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/**
 * Spring {@link RestClient}를 사용해 GitHub Contents API에서 공유 HTML 파일을 읽기 전용으로 조회하는 구현체.
 *
 * <p>기본적으로 {@code application/vnd.github+json}으로 파일 메타데이터(`sha`)와 MIME Base64 본문을 조회하며, 1MB 초과
 * 파일({@code encoding=none})인 경우 {@code application/vnd.github.raw+json}으로 원문 UTF-8 HTML을 폴백 조회합니다.
 */
@Component
public class RestClientGitHubShareClient implements GitHubShareClient {

    private static final String ACCEPT_JSON_VALUE = "application/vnd.github+json";
    private static final String ACCEPT_RAW_VALUE = "application/vnd.github.raw+json";
    private static final String API_VERSION_HEADER = "X-GitHub-Api-Version";
    private static final String API_VERSION_VALUE = "2022-11-28";
    private static final String HTML_EXTENSION = ".html";
    private static final String FILE_TYPE = "file";
    private static final String BASE64_ENCODING = "base64";
    private static final String NONE_ENCODING = "none";

    private final RestClient restClient;
    private final GitHubShareProperties properties;

    /**
     * RestClientGitHubShareClient를 생성합니다.
     *
     * @param gitHubShareRestClient GitHub API 호출용 {@link RestClient}
     * @param properties GitHub 공유 설정 프로퍼티
     */
    public RestClientGitHubShareClient(
            final RestClient gitHubShareRestClient, final GitHubShareProperties properties) {
        this.restClient = gitHubShareRestClient;
        this.properties = properties;
    }

    /**
     * 지정된 슬러그의 HTML 파일을 GitHub Contents API로 조회합니다.
     *
     * @param slug 검증된 공유 슬러그
     * @param knownSha 이전 캐시에 보관 중인 파일 SHA (없으면 {@code null})
     * @return 조회 결과 ({@link ShareFetchResult})
     * @throws ShareConfigurationException 연동이 비활성화되었거나 토큰이 누락된 경우
     * @throws GitHubShareException GitHub API 호출, 타임아웃 또는 디코딩 실패 시
     */
    @Override
    public ShareFetchResult fetchHtml(final String slug, final String knownSha) {
        if (!properties.isActive()) {
            throw new ShareConfigurationException("GitHub 공유 연동이 비활성화되었거나 인증 토큰이 설정되지 않았습니다.");
        }
        try {
            final JsonOutcome outcome =
                    restClient
                            .get()
                            .uri(uriBuilder -> buildContentsUri(uriBuilder, slug))
                            .headers(
                                    headers ->
                                            applyGitHubHeaders(
                                                    headers, ACCEPT_JSON_VALUE, knownSha))
                            .exchange(
                                    (request, response) -> {
                                        final HttpStatusCode status = response.getStatusCode();
                                        if (status.value() == HttpStatus.NOT_MODIFIED.value()) {
                                            return new JsonOutcome.Resolved(
                                                    new ShareFetchResult.NotModified());
                                        }
                                        if (status.value() == HttpStatus.NOT_FOUND.value()) {
                                            return new JsonOutcome.Resolved(
                                                    new ShareFetchResult.NotFound());
                                        }
                                        if (!status.is2xxSuccessful()) {
                                            throw new GitHubShareException(
                                                    "GitHub Contents API 호출 실패: HTTP "
                                                            + status.value());
                                        }
                                        return parseJsonOutcome(
                                                response.bodyTo(GitHubContentFileResponse.class));
                                    });
            return switch (outcome) {
                case JsonOutcome.Resolved resolved -> resolved.result();
                case JsonOutcome.NeedsRawFallback fallback -> fetchRawHtml(slug, fallback.sha());
            };
        } catch (final RestClientException ex) {
            throw new GitHubShareException("GitHub Contents API 통신 중 오류가 발생했습니다.", ex);
        }
    }

    private JsonOutcome parseJsonOutcome(final GitHubContentFileResponse body) {
        if (body == null) {
            throw new GitHubShareException("GitHub Contents API 응답 본문이 비어 있습니다.");
        }
        if (!FILE_TYPE.equals(body.type())) {
            return new JsonOutcome.Resolved(new ShareFetchResult.NotFound());
        }
        if (needsRawFallback(body)) {
            return new JsonOutcome.NeedsRawFallback(body.sha());
        }
        final String decodedHtml = decodeBase64Utf8(body.content());
        return new JsonOutcome.Resolved(new ShareFetchResult.Found(decodedHtml, body.sha()));
    }

    private ShareFetchResult fetchRawHtml(final String slug, final String sha) {
        return restClient
                .get()
                .uri(uriBuilder -> buildContentsUri(uriBuilder, slug))
                .headers(headers -> applyGitHubHeaders(headers, ACCEPT_RAW_VALUE, null))
                .exchange(
                        (request, response) -> {
                            final HttpStatusCode status = response.getStatusCode();
                            if (status.value() == HttpStatus.NOT_FOUND.value()) {
                                return new ShareFetchResult.NotFound();
                            }
                            if (!status.is2xxSuccessful()) {
                                throw new GitHubShareException(
                                        "GitHub Contents API Raw 조회 실패: HTTP " + status.value());
                            }
                            final byte[] rawBytes = response.getBody().readAllBytes();
                            final String html = new String(rawBytes, StandardCharsets.UTF_8);
                            return new ShareFetchResult.Found(html, sha);
                        });
    }

    private boolean needsRawFallback(final GitHubContentFileResponse body) {
        if (NONE_ENCODING.equalsIgnoreCase(body.encoding())) {
            return true;
        }
        return body.content() == null
                || body.content().isBlank()
                || !BASE64_ENCODING.equalsIgnoreCase(body.encoding());
    }

    private String decodeBase64Utf8(final String base64Content) {
        try {
            final String normalized = base64Content.replaceAll("\\s+", "");
            final byte[] decoded = Base64.getDecoder().decode(normalized);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException ex) {
            throw new GitHubShareException("GitHub 파일 Base64 본문 디코딩에 실패했습니다.", ex);
        }
    }

    private void applyGitHubHeaders(
            final HttpHeaders headers, final String acceptHeader, final String knownSha) {
        headers.setBearerAuth(properties.token());
        headers.set(HttpHeaders.ACCEPT, acceptHeader);
        headers.set(API_VERSION_HEADER, API_VERSION_VALUE);
        if (knownSha != null && !knownSha.isBlank()) {
            headers.setIfNoneMatch(formatStrongEtag(knownSha));
        }
    }

    private URI buildContentsUri(final UriBuilder uriBuilder, final String slug) {
        uriBuilder.pathSegment("repos", properties.owner(), properties.repo(), "contents");
        final String[] segments = properties.basePath().split("/", -1);
        for (final String segment : segments) {
            if (!segment.isEmpty()) {
                uriBuilder.pathSegment(segment);
            }
        }
        uriBuilder.pathSegment(slug + HTML_EXTENSION);
        uriBuilder.queryParam("ref", properties.branch());
        return uriBuilder.build();
    }

    private static String formatStrongEtag(final String sha) {
        final String trimmed = sha.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed;
        }
        return "\"" + trimmed + "\"";
    }

    private sealed interface JsonOutcome
            permits JsonOutcome.Resolved, JsonOutcome.NeedsRawFallback {

        record Resolved(ShareFetchResult result) implements JsonOutcome {}

        record NeedsRawFallback(String sha) implements JsonOutcome {}
    }
}
