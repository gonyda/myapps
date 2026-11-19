package com.myapps.web.myshare.application.port;

import com.myapps.web.myshare.application.dto.ShareFetchResult;

/** GitHub 저장소에서 공유 HTML 파일을 읽기 전용으로 조회하는 포트 인터페이스. */
public interface GitHubShareClient {

    /**
     * 지정된 슬러그에 대응하는 HTML 파일을 GitHub 저장소에서 조회합니다.
     *
     * @param slug 검증이 완료된 공유 슬러그
     * @param knownSha 이전 캐시에 저장된 SHA (없으면 {@code null} 또는 빈 문자열)
     * @return 조회 결과 ({@link ShareFetchResult.Found}, {@link ShareFetchResult.NotModified}, {@link
     *     ShareFetchResult.NotFound})
     */
    ShareFetchResult fetchHtml(String slug, String knownSha);
}
