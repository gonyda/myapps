package com.myapps.web.myshare.application.port;

/** 저장된 미니앱 JSON 스냅샷을 원격 GitHub 저장소에 커밋·푸시하는 포트 인터페이스. */
public interface GitHubMiniAppSyncClient {

    /**
     * 지정된 {@code appKey}에 해당하는 JSON 본문을 GitHub 저장소에 업로드합니다.
     *
     * @param appKey 검증된 미니앱 키
     * @param jsonContent 저장할 UTF-8 JSON 본문
     * @return 실제 GitHub 업로드 수행 시 {@code true}, 비활성화·토큰 미설정으로 스킵 시 {@code false}
     */
    boolean pushJson(String appKey, String jsonContent);

    /**
     * 지정된 {@code appKey}에 해당하는 {@code data.json} 파일을 GitHub 저장소에서 삭제합니다.
     *
     * @param appKey 검증된 미니앱 키
     * @return 실제 삭제 커밋 수행 시 {@code true}, 비활성화·토큰 미설정·원격 파일 미존재로 스킵 시 {@code false}
     */
    boolean deleteJson(String appKey);
}
