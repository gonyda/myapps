package com.myapps.web.myshare.infrastructure.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * GitHub Contents API {@code GET /contents} JSON 응답에서 파일 메타데이터 및 Base64 본문을 추출하는 DTO.
 *
 * @param type 콘텐츠 유형({@code file}, {@code dir} 등)
 * @param encoding 본문 인코딩 방식({@code base64}, {@code none} 등)
 * @param content Base64 인코딩된 파일 본문
 * @param sha 파일 SHA 해시
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubContentFileResponse(String type, String encoding, String content, String sha) {}
