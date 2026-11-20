package com.myapps.web.myshare.infrastructure.github;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * GitHub Contents API {@code PUT /contents} 파일 생성·갱신 요청 바디 DTO.
 *
 * <p>{@code sha}가 {@code null}이면 JSON 직렬화 시 생략되어 신규 파일 생성 요청으로 동작합니다.
 *
 * @param message 커밋 메시지
 * @param content Base64 인코딩된 파일 본문
 * @param branch 대상 브랜치명
 * @param sha 기존 파일 SHA 해시 (신규 생성 시 {@code null})
 */
public record GitHubMiniAppUpdateRequest(
        String message,
        String content,
        String branch,
        @JsonInclude(JsonInclude.Include.NON_NULL) String sha) {}
