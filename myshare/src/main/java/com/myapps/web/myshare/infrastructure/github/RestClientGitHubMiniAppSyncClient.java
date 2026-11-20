package com.myapps.web.myshare.infrastructure.github;

import com.myapps.web.myshare.application.exception.GitHubMiniAppSyncException;
import com.myapps.web.myshare.application.port.GitHubMiniAppSyncClient;
import com.myapps.web.myshare.infrastructure.config.GitHubShareProperties;
import com.myapps.web.myshare.infrastructure.config.MiniAppProperties;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/**
 * Spring {@link RestClient}를 사용해 {@code gonyda/mike} 저장소의 {@code share/{appKey}/data.json} 경로에 JSON
 * 파일을 커밋·푸시하거나 삭제하는 구현체.
 *
 * <p>대상 파일의 {@code sha}를 조회한 뒤(404 시 신규 생성 또는 삭제 스킵) Base64 UTF-8 본문을 {@code PUT}으로 업로드하거나 {@code
 * DELETE}로 제거하며, {@code 409 Conflict} 응답 시 최신 {@code sha}를 재조회하여 최대 2회 재시도합니다.
 */
@Component
public class RestClientGitHubMiniAppSyncClient implements GitHubMiniAppSyncClient {

    private static final Logger LOG =
            LoggerFactory.getLogger(RestClientGitHubMiniAppSyncClient.class);
    private static final String ACCEPT_HEADER_VALUE = "application/vnd.github+json";
    private static final String API_VERSION_HEADER = "X-GitHub-Api-Version";
    private static final String API_VERSION_VALUE = "2022-11-28";
    private static final String DATA_JSON_FILE = "data.json";
    private static final String DELETE_COMMIT_PREFIX = "chore(mini): delete";
    private static final int MAX_CONFLICT_RETRIES = 2;

    private final RestClient restClient;
    private final GitHubShareProperties shareProperties;
    private final MiniAppProperties miniAppProperties;

    /**
     * RestClientGitHubMiniAppSyncClient를 생성합니다.
     *
     * @param gitHubShareRestClient GitHub API 호출용 {@link RestClient}
     * @param shareProperties GitHub 저장소 및 토큰 설정 프로퍼티
     * @param miniAppProperties 미니앱 동기화 경로 및 커밋 메시지 설정 프로퍼티
     */
    public RestClientGitHubMiniAppSyncClient(
            final RestClient gitHubShareRestClient,
            final GitHubShareProperties shareProperties,
            final MiniAppProperties miniAppProperties) {
        this.restClient = gitHubShareRestClient;
        this.shareProperties = shareProperties;
        this.miniAppProperties = miniAppProperties;
    }

    /**
     * 지정된 미니앱 JSON 본문을 {@code share/{appKey}/data.json} 경로에 Base64 인코딩하여 커밋·푸시합니다.
     *
     * @param appKey 검증된 미니앱 키
     * @param jsonContent 저장할 UTF-8 JSON 본문
     * @return 실제 업로드 수행 시 {@code true}, 비활성화·토큰 미설정 시 {@code false}
     * @throws GitHubMiniAppSyncException 입력 누락 또는 GitHub Contents API 호출 실패 시
     */
    @Override
    public boolean pushJson(final String appKey, final String jsonContent) {
        if (!shareProperties.isActive() || !miniAppProperties.isGitHubSyncEnabled()) {
            LOG.debug("GitHub 미니앱 동기화가 비활성화되어 있거나 토큰이 비어 있어 푸시를 건너뜁니다.");
            return false;
        }
        if (appKey == null || appKey.isBlank() || jsonContent == null) {
            throw new GitHubMiniAppSyncException("동기화할 미니앱 키 또는 JSON 본문이 비어 있습니다.");
        }

        final String encodedContent =
                Base64.getEncoder().encodeToString(jsonContent.getBytes(StandardCharsets.UTF_8));
        final Optional<String> currentSha = fetchCurrentSha(appKey);
        putWithConflictRetry(appKey, encodedContent, currentSha.orElse(null));
        return true;
    }

    /**
     * 지정된 미니앱의 {@code share/{appKey}/data.json} 파일만 GitHub 저장소에서 삭제합니다.
     *
     * <p>파일이 이미 존재하지 않는 경우({@code 404 NotFound}) 오류 없이 조용히 건너뛰며, 동일 폴더의 {@code index.html}에는 영향을 주지
     * 않습니다.
     *
     * @param appKey 검증된 미니앱 키
     * @return 실제 삭제 커밋 수행 시 {@code true}, 비활성화·토큰 미설정·미존재 시 {@code false}
     * @throws GitHubMiniAppSyncException 입력 누락 또는 GitHub Contents API 호출 실패 시
     */
    @Override
    public boolean deleteJson(final String appKey) {
        if (!shareProperties.isActive() || !miniAppProperties.isGitHubSyncEnabled()) {
            LOG.debug("GitHub 미니앱 동기화가 비활성화되어 있거나 토큰이 비어 있어 삭제를 건너뜁니다.");
            return false;
        }
        if (appKey == null || appKey.isBlank()) {
            throw new GitHubMiniAppSyncException("삭제할 미니앱 키가 비어 있습니다.");
        }

        final Optional<String> currentSha = fetchCurrentSha(appKey);
        if (currentSha.isEmpty()) {
            LOG.debug("원격 GitHub data.json 파일이 이미 없어 삭제를 건너뜁니다: appKey={}", appKey);
            return false;
        }
        return deleteWithConflictRetry(appKey, currentSha.get());
    }

    private Optional<String> fetchCurrentSha(final String appKey) {
        try {
            final GitHubContentFileResponse response =
                    restClient
                            .get()
                            .uri(uriBuilder -> buildContentsUri(uriBuilder, appKey, true))
                            .headers(this::applyGitHubHeaders)
                            .retrieve()
                            .body(GitHubContentFileResponse.class);
            if (response == null || response.sha() == null || response.sha().isBlank()) {
                throw new GitHubMiniAppSyncException("GitHub 파일 조회 응답에 sha가 포함되지 않았습니다.");
            }
            return Optional.of(response.sha());
        } catch (final HttpClientErrorException.NotFound ex) {
            LOG.debug("GitHub 미니앱 data.json 파일이 아직 존재하지 않습니다: appKey={}", appKey);
            return Optional.empty();
        } catch (final RestClientException ex) {
            throw new GitHubMiniAppSyncException("GitHub 미니앱 파일 sha 조회 중 오류가 발생했습니다.", ex);
        }
    }

    private void putWithConflictRetry(
            final String appKey, final String encodedContent, final String initialSha) {
        String candidateSha = initialSha;
        for (int attempt = 0; attempt <= MAX_CONFLICT_RETRIES; attempt++) {
            try {
                executePut(appKey, encodedContent, candidateSha);
                return;
            } catch (final HttpClientErrorException.Conflict ex) {
                if (attempt == MAX_CONFLICT_RETRIES) {
                    throw new GitHubMiniAppSyncException(
                            "GitHub 미니앱 파일 업로드 409 Conflict 재시도에 모두 실패했습니다.", ex);
                }
                LOG.warn(
                        "GitHub 미니앱 파일 업로드 중 409 Conflict 발생 — 최신 sha 재조회 후 재시도합니다: appKey={}, attempt={}",
                        appKey,
                        attempt + 1);
                candidateSha = fetchCurrentSha(appKey).orElse(null);
            } catch (final RestClientException ex) {
                throw new GitHubMiniAppSyncException("GitHub 미니앱 파일 업로드 중 오류가 발생했습니다.", ex);
            }
        }
    }

    private boolean deleteWithConflictRetry(final String appKey, final String initialSha) {
        String candidateSha = initialSha;
        for (int attempt = 0; attempt <= MAX_CONFLICT_RETRIES; attempt++) {
            try {
                executeDelete(appKey, candidateSha);
                return true;
            } catch (final HttpClientErrorException.NotFound ex) {
                LOG.debug("GitHub data.json 파일이 삭제 도중 이미 제거되었습니다: appKey={}", appKey);
                return false;
            } catch (final HttpClientErrorException.Conflict ex) {
                if (attempt == MAX_CONFLICT_RETRIES) {
                    throw new GitHubMiniAppSyncException(
                            "GitHub 미니앱 파일 삭제 409 Conflict 재시도에 모두 실패했습니다.", ex);
                }
                LOG.warn(
                        "GitHub 미니앱 파일 삭제 중 409 Conflict 발생 — 최신 sha 재조회 후 재시도합니다: appKey={}, attempt={}",
                        appKey,
                        attempt + 1);
                final Optional<String> latestSha = fetchCurrentSha(appKey);
                if (latestSha.isEmpty()) {
                    return false;
                }
                candidateSha = latestSha.get();
            } catch (final RestClientException ex) {
                throw new GitHubMiniAppSyncException("GitHub 미니앱 파일 삭제 중 오류가 발생했습니다.", ex);
            }
        }
        return false;
    }

    private void executePut(final String appKey, final String encodedContent, final String sha) {
        final String message =
                miniAppProperties.github().commitMessagePrefix()
                        + " "
                        + appKey
                        + "/"
                        + DATA_JSON_FILE;
        final GitHubMiniAppUpdateRequest requestBody =
                new GitHubMiniAppUpdateRequest(
                        message, encodedContent, shareProperties.branch(), sha);
        restClient
                .put()
                .uri(uriBuilder -> buildContentsUri(uriBuilder, appKey, false))
                .headers(this::applyGitHubHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }

    private void executeDelete(final String appKey, final String sha) {
        final String message = DELETE_COMMIT_PREFIX + " " + appKey + "/" + DATA_JSON_FILE;
        final GitHubMiniAppDeleteRequest requestBody =
                new GitHubMiniAppDeleteRequest(message, sha, shareProperties.branch());
        restClient
                .method(HttpMethod.DELETE)
                .uri(uriBuilder -> buildContentsUri(uriBuilder, appKey, false))
                .headers(this::applyGitHubHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }

    private void applyGitHubHeaders(final HttpHeaders headers) {
        headers.setBearerAuth(shareProperties.token());
        headers.set(HttpHeaders.ACCEPT, ACCEPT_HEADER_VALUE);
        headers.set(API_VERSION_HEADER, API_VERSION_VALUE);
    }

    private URI buildContentsUri(
            final UriBuilder uriBuilder, final String appKey, final boolean includeBranchRef) {
        uriBuilder.pathSegment(
                "repos", shareProperties.owner(), shareProperties.repo(), "contents");
        final String[] segments = miniAppProperties.github().basePath().split("/", -1);
        for (final String segment : segments) {
            if (!segment.isEmpty()) {
                uriBuilder.pathSegment(segment);
            }
        }
        uriBuilder.pathSegment(appKey);
        uriBuilder.pathSegment(DATA_JSON_FILE);
        if (includeBranchRef) {
            uriBuilder.queryParam("ref", shareProperties.branch());
        }
        return uriBuilder.build();
    }
}
