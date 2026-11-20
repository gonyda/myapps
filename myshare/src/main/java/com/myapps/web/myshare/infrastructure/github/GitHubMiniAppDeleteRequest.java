package com.myapps.web.myshare.infrastructure.github;

/**
 * GitHub Contents API {@code DELETE /contents} 파일 삭제 요청 바디 DTO.
 *
 * @param message 삭제 커밋 메시지
 * @param sha 삭제 대상 파일의 현재 SHA 해시
 * @param branch 대상 브랜치명
 */
public record GitHubMiniAppDeleteRequest(String message, String sha, String branch) {}
