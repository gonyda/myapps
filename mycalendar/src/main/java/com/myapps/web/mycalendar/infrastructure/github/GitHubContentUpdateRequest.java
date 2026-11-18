package com.myapps.web.mycalendar.infrastructure.github;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * GitHub Contents API {@code PUT /contents} 파일 생성·갱신 요청 바디 DTO.
 *
 * <p>{@code sha}가 {@code null}이면 JSON 직렬화 시 생략되어 신규 파일 생성 요청으로 동작합니다.
 */
public record GitHubContentUpdateRequest(
        String message,
        String content,
        String branch,
        @JsonInclude(JsonInclude.Include.NON_NULL) String sha) {}
