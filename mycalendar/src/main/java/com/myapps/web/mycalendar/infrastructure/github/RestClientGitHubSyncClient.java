package com.myapps.web.mycalendar.infrastructure.github;

import com.myapps.web.mycalendar.application.exception.GitHubSyncException;
import com.myapps.web.mycalendar.application.service.GitHubSyncClient;
import com.myapps.web.mycalendar.infrastructure.config.GitHubSyncProperties;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/**
 * Spring {@link RestClient}를 사용해 GitHub Contents API로 스냅샷을 푸시하는 구현체.
 *
 * <p>대상 파일의 {@code sha}를 조회한 뒤(404인 경우 신규 생성), Base64 인코딩된 본문을 {@code PUT}으로 업로드하며, {@code 409
 * Conflict} 응답 시 최신 {@code sha}를 재조회하여 최대 1회 재시도합니다.
 */
@Component
public class RestClientGitHubSyncClient implements GitHubSyncClient {

    private static final Logger LOG = LoggerFactory.getLogger(RestClientGitHubSyncClient.class);
    private static final String ACCEPT_HEADER_VALUE = "application/vnd.github+json";
    private static final String API_VERSION_HEADER = "X-GitHub-Api-Version";
    private static final String API_VERSION_VALUE = "2022-11-28";

    private final RestClient restClient;
    private final GitHubSyncProperties properties;

    /**
     * RestClientGitHubSyncClient를 생성합니다.
     *
     * @param gitHubRestClient GitHub API 호출용 {@link RestClient}
     * @param properties GitHub 동기화 설정 프로퍼티
     */
    public RestClientGitHubSyncClient(
            final RestClient gitHubRestClient, final GitHubSyncProperties properties) {
        this.restClient = gitHubRestClient;
        this.properties = properties;
    }

    /**
     * 스냅샷 JSON을 Base64로 인코딩하여 GitHub 저장소 대상 경로에 푸시합니다.
     *
     * @param jsonContent UTF-8 JSON 본문
     * @throws GitHubSyncException GitHub API 통신 실패 또는 잘못된 입력 시
     */
    @Override
    public void pushSnapshot(final String jsonContent) {
        if (!properties.isActive()) {
            LOG.debug("GitHub 동기화가 비활성화되어 있거나 토큰이 비어 있어 HTTP 호출을 건너뜁니다.");
            return;
        }
        if (jsonContent == null) {
            throw new GitHubSyncException("동기화할 JSON 본문이 null입니다.");
        }

        final String encodedContent =
                Base64.getEncoder().encodeToString(jsonContent.getBytes(StandardCharsets.UTF_8));
        final Optional<String> currentSha = fetchCurrentSha();
        putWithConflictRetry(encodedContent, currentSha.orElse(null));
    }

    private Optional<String> fetchCurrentSha() {
        try {
            final GitHubContentResponse response =
                    restClient
                            .get()
                            .uri(uriBuilder -> buildContentsUri(uriBuilder, true))
                            .headers(this::applyGitHubHeaders)
                            .retrieve()
                            .body(GitHubContentResponse.class);
            if (response == null || response.sha() == null || response.sha().isBlank()) {
                throw new GitHubSyncException("GitHub 파일 조회 응답에 sha가 포함되지 않았습니다.");
            }
            return Optional.of(response.sha());
        } catch (final HttpClientErrorException.NotFound ex) {
            LOG.debug("대상 파일이 아직 존재하지 않아 신규 생성으로 진행합니다: path={}", properties.path());
            return Optional.empty();
        } catch (final RestClientException ex) {
            throw new GitHubSyncException("GitHub 파일 sha 조회 중 오류가 발생했습니다.", ex);
        }
    }

    private void putWithConflictRetry(final String encodedContent, final String initialSha) {
        try {
            executePut(encodedContent, initialSha);
        } catch (final HttpClientErrorException.Conflict ex) {
            LOG.warn(
                    "GitHub 파일 업로드 중 409 Conflict가 발생하여 최신 sha를 재조회해 1회 재시도합니다: path={}",
                    properties.path());
            final Optional<String> refreshedSha = fetchCurrentSha();
            try {
                executePut(encodedContent, refreshedSha.orElse(null));
            } catch (final RestClientException retryEx) {
                throw new GitHubSyncException("GitHub 파일 업로드 재시도에 실패했습니다.", retryEx);
            }
        } catch (final RestClientException ex) {
            throw new GitHubSyncException("GitHub 파일 업로드 중 오류가 발생했습니다.", ex);
        }
    }

    private void executePut(final String encodedContent, final String sha) {
        final GitHubContentUpdateRequest requestBody =
                new GitHubContentUpdateRequest(
                        properties.commitMessage(), encodedContent, properties.branch(), sha);
        restClient
                .put()
                .uri(uriBuilder -> buildContentsUri(uriBuilder, false))
                .headers(this::applyGitHubHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }

    private void applyGitHubHeaders(final HttpHeaders headers) {
        headers.setBearerAuth(properties.token());
        headers.set(HttpHeaders.ACCEPT, ACCEPT_HEADER_VALUE);
        headers.set(API_VERSION_HEADER, API_VERSION_VALUE);
    }

    private URI buildContentsUri(final UriBuilder uriBuilder, final boolean includeBranchRef) {
        uriBuilder.pathSegment("repos", properties.owner(), properties.repo(), "contents");
        final String[] segments = properties.path().split("/", -1);
        for (final String segment : segments) {
            if (!segment.isEmpty()) {
                uriBuilder.pathSegment(segment);
            }
        }
        if (includeBranchRef) {
            uriBuilder.queryParam("ref", properties.branch());
        }
        return uriBuilder.build();
    }
}
