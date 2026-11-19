package com.myapps.web.myshare.application.service;

import com.myapps.web.myshare.application.dto.ShareFetchResult;
import com.myapps.web.myshare.application.exception.GitHubShareException;
import com.myapps.web.myshare.application.exception.ShareNotFoundException;
import com.myapps.web.myshare.application.port.GitHubShareClient;
import com.myapps.web.myshare.domain.model.SharePage;
import com.myapps.web.myshare.domain.model.ShareSlug;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** 공유 슬러그 검증, TTL 인메모리 캐시 조회, GitHub 저장소 페치를 조율하는 애플리케이션 서비스. */
@Service
public class SharePageService {

    private final GitHubShareClient gitHubShareClient;
    private final InMemoryShareCache shareCache;

    /**
     * SharePageService를 생성합니다.
     *
     * @param gitHubShareClient GitHub HTML 조회 포트
     * @param shareCache 짧은 TTL 인메모리 캐시
     */
    public SharePageService(
            final GitHubShareClient gitHubShareClient, final InMemoryShareCache shareCache) {
        this.gitHubShareClient = gitHubShareClient;
        this.shareCache = shareCache;
    }

    /**
     * 슬러그를 검증한 뒤 캐시 또는 GitHub 저장소에서 HTML 페이지를 로드합니다.
     *
     * @param rawSlug 요청 경로의 공유 슬러그
     * @return 조회된 {@link SharePage}
     * @throws ShareNotFoundException 저장소에 파일이 존재하지 않는 경우
     */
    public SharePage loadPage(final String rawSlug) {
        final ShareSlug slug = ShareSlug.of(rawSlug);
        final Optional<SharePage> freshPage = shareCache.findFresh(slug.value());
        if (freshPage.isPresent()) {
            return freshPage.get();
        }

        final String knownSha =
                shareCache
                        .findEntry(slug.value())
                        .map(entry -> entry.page().sha())
                        .filter(sha -> !sha.isBlank())
                        .orElse(null);

        final ShareFetchResult result = gitHubShareClient.fetchHtml(slug.value(), knownSha);
        return switch (result) {
            case ShareFetchResult.Found found -> cacheAndReturn(slug.value(), found);
            case ShareFetchResult.NotModified ignored ->
                    shareCache
                            .refreshTtl(slug.value())
                            .orElseGet(() -> refetchWithoutEtag(slug.value()));
            case ShareFetchResult.NotFound ignored -> {
                shareCache.remove(slug.value());
                throw new ShareNotFoundException("요청한 공유 페이지를 찾을 수 없습니다.");
            }
        };
    }

    private SharePage cacheAndReturn(final String slug, final ShareFetchResult.Found found) {
        final SharePage page = new SharePage(slug, found.html(), found.sha());
        shareCache.put(page);
        return page;
    }

    private SharePage refetchWithoutEtag(final String slug) {
        final ShareFetchResult retry = gitHubShareClient.fetchHtml(slug, null);
        return switch (retry) {
            case ShareFetchResult.Found found -> cacheAndReturn(slug, found);
            case ShareFetchResult.NotFound ignored ->
                    throw new ShareNotFoundException("요청한 공유 페이지를 찾을 수 없습니다.");
            case ShareFetchResult.NotModified ignored ->
                    throw new GitHubShareException("조건부 요청 헤더 없이 304 Not Modified가 반환되었습니다.");
        };
    }
}
