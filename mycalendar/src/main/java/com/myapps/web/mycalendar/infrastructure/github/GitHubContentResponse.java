package com.myapps.web.mycalendar.infrastructure.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** GitHub Contents API {@code GET /contents} 응답에서 파일 {@code sha}를 추출하기 위한 DTO. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubContentResponse(String sha) {}
