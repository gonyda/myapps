package com.myapps.web.myshare.application.dto;

import java.util.Objects;

/** GitHub 저장소에서 공유 HTML 파일을 조회한 결과를 표현하는 봉인된 응답 타입. */
public sealed interface ShareFetchResult
        permits ShareFetchResult.Found, ShareFetchResult.NotModified, ShareFetchResult.NotFound {

    /**
     * 대상 HTML 파일을 정상 조회한 경우의 결과.
     *
     * @param html UTF-8 디코딩된 HTML 본문
     * @param sha GitHub 파일 SHA (ETag 생성 및 조건부 재검증에 사용)
     */
    record Found(String html, String sha) implements ShareFetchResult {

        /** HTML 본문의 null 여부를 검증합니다. */
        public Found {
            Objects.requireNonNull(html, "html must not be null");
            sha = sha != null ? sha : "";
        }
    }

    /** 조건부 요청({@code If-None-Match})에 대해 파일 변경이 없음을 나타내는 결과. */
    record NotModified() implements ShareFetchResult {}

    /** 대상 경로에 파일이 존재하지 않음을 나타내는 결과. */
    record NotFound() implements ShareFetchResult {}
}
