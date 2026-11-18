package com.myapps.web.mycalendar.application.service;

/** 외부 GitHub 저장소에 캘린더 스냅샷 JSON을 푸시하는 포트 인터페이스. */
public interface GitHubSyncClient {

    /**
     * UTF-8 JSON 스냅샷 문자열을 대상 GitHub 파일로 덮어쓰기(또는 최초 생성) 푸시합니다.
     *
     * @param jsonContent UTF-8로 직렬화된 스냅샷 JSON 본문
     */
    void pushSnapshot(String jsonContent);
}
