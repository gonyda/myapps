package com.myapps.web.myshare.domain.model;

import java.util.Objects;

/**
 * 서빙할 단일 HTML 페이지와 메타데이터(ETag용 {@code sha})를 보관하는 불변 값 객체.
 *
 * @param slug 공유 슬러그
 * @param html UTF-8 단일 HTML 원문
 * @param sha GitHub 파일 SHA (비어 있을 수 있음)
 */
public record SharePage(String slug, String html, String sha) {

    /** 필수 필드를 검증하고 누락된 {@code sha}를 빈 문자열로 정규화합니다. */
    public SharePage {
        Objects.requireNonNull(slug, "slug must not be null");
        Objects.requireNonNull(html, "html must not be null");
        sha = sha != null ? sha : "";
    }
}
