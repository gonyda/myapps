# Implementation Plan: share-folder-unification-and-delete

> **폴더 위치**: `.kiro/specs/myshare/003-share-folder-unification-and-delete/tasks.md`  
> **관련 규칙**: `rules/workflow/task-build-validation.md`, `rules/workflow/git-workflow.md`

---

## Overview

본 작업 명세는 `myshare` 모듈(`com.myapps.web.myshare`, 포트 `8084`)에서:
1. GitHub 저장소 구조를 `share/{slug}/` 폴더 단위(`index.html`, `data.json`)로 통일하고,
2. `GET /share/{slug}` 우선 조회·하위 호환 폴백 및 `.json` 직접 접근 `404` 차단을 적용하며,
3. `DELETE /api/mini/{appKey}` 멱등 삭제 및 `@Async` GitHub 단일 파일 삭제를 구현하기 위한 체크포인트 단위 작업 목록이다.

### 구현 순서 및 원칙
1. **Bottom-Up 계층 조립**:  
   **A. 설정/도메인** $\rightarrow$ **B. 애플리케이션 서비스 & 포트** $\rightarrow$ **C. GitHub 인프라 클라이언트** $\rightarrow$ **D. 웹 컨트롤러 & 보안 차단** $\rightarrow$ **E. 통합 테스트 & 5대 품질 가드레일**
2. **테스트 필수 & Given-When-Then**: 모든 테스트에 `// given`, `// when`, `// then` 주석과 `should_{동작}_when_{조건}` 네이밍을 적용한다.

---

## Tasks

### A. 설정 및 도메인 계층 (Config & Domain Layer)

- [x] 1. GitHub 기본 경로 통일 및 `ShareSlug` 소문자 패턴 적용
  - [x] 1.1 `MiniAppProperties.java`의 `DEFAULT_GITHUB_BASE_PATH`를 `"mini"` → `"share"`로 변경
  - [x] 1.2 `myshare/src/main/resources/application.yml` 및 `myshare/src/test/resources/application.yml`의 `myshare.mini-app.github.base-path` 기본값을 `share`로 변경
  - [x] 1.3 `ShareSlug.java` 허용 정규식을 `^[a-z0-9_-]{1,200}$`로 변경
  - _Requirements: 1.1, 2.1, 2.2_ / _Design: 3.1, 3.3_

- [x] 2. 설정/도메인 테스트 갱신 및 체크포인트 A
  - [x] 2.1 `MiniAppPropertiesTest.java` 기본 `basePath` 기대값을 `"share"`로 갱신
  - [x] 2.2 `ShareSlugPropertyTest.java`의 `validSlugs`를 소문자 기반으로 갱신하고 대문자 거부 케이스 검증 추가
  - [x] 2.3 `mvn test -pl myshare -Dtest="MiniAppPropertiesTest,ShareSlugPropertyTest"` 통과 확인
  - _Validates: Requirements 1.1, 2.1, 2.2_

---

### B. 애플리케이션 서비스 계층 (Application Layer)

- [x] 3. 멱등 삭제 서비스 및 비동기 GitHub 삭제 포트·서비스 확장
  - [x] 3.1 `GitHubMiniAppSyncClient.java`에 `boolean deleteJson(String appKey)` 메서드 추가
  - [x] 3.2 `MiniAppStoreService.java`에 `@Transactional public boolean deleteApp(final String rawAppKey)` 멱등 삭제 구현
  - [x] 3.3 `MiniAppGitHubSyncService.java`에 `@Async("miniAppGitHubSyncExecutor") public void deleteAsync(final String appKey)` 구현 (예외 로깅 후 삼킴)
  - _Requirements: 3.2, 3.3, 3.4, 3.7_ / _Design: 3.2_

- [x] 4. 애플리케이션 서비스 단위 테스트 보강 및 체크포인트 B
  - [x] 4.1 `MiniAppStoreServiceTest.java`에 `deleteApp` 존재/미존재 멱등성 및 잘못된 키 예외 테스트 추가
  - [x] 4.2 `MiniAppGitHubSyncServiceTest.java`에 `deleteAsync` 성공 및 예외 삼킴 테스트 추가
  - [x] 4.3 `mvn test -pl myshare -Dtest="MiniAppStoreServiceTest,MiniAppGitHubSyncServiceTest"` 통과 확인
  - _Validates: Requirements 3.3, 3.4, 3.7_

---

### C. 인프라 계층 (Infrastructure Layer — GitHub Clients)

- [x] 5. GitHub HTML 우선 조회·폴백 및 `data.json` 업로드·삭제 클라이언트 구현
  - [x] 5.1 `RestClientGitHubShareClient.java`에서 `{basePath}/{slug}/index.html` 우선 조회 → `NotFound` 시 `{basePath}/{slug}.html` 폴백 구현
  - [x] 5.2 `GitHubMiniAppDeleteRequest.java` 신규 생성 (`message`, `sha`, `branch`)
  - [x] 5.3 `RestClientGitHubMiniAppSyncClient.java` URI를 `{basePath}/{appKey}/data.json`으로 변경 및 `deleteJson(appKey)` 구현 (`sha` 조회 → 404 조용한 스킵, 존재 시 `DELETE`, 409 최대 2회 재시도, `index.html` 무간섭)
  - _Requirements: 1.2, 1.3, 2.3~2.6, 3.5~3.8_ / _Design: 3.3_

- [x] 6. GitHub 클라이언트 테스트 보강 및 체크포인트 C
  - [x] 6.1 `RestClientGitHubShareClientTest.java`에 `index.html` 우선 성공, `index.html` 404 → `{slug}.html` 폴백, 둘 다 404, 5xx 비폴백 예외 테스트 추가
  - [x] 6.2 `RestClientGitHubMiniAppSyncClientTest.java`의 URI 기대값을 `/contents/share/todo/data.json`으로 갱신하고 `deleteJson` 성공/404 스킵/409 재시도/비활성화 스킵/5xx 예외 테스트 추가
  - [x] 6.3 `mvn test -pl myshare -Dtest="RestClientGitHubShareClientTest,RestClientGitHubMiniAppSyncClientTest"` 통과 확인
  - _Validates: Requirements 1.2, 2.3~2.6, 3.5~3.8_

---

### D. 웹 인터페이스 계층 (Web Controller & Security Layer)

- [x] 7. `.json` 직접 접근 차단 및 `DELETE /api/mini/{appKey}` 컨트롤러 엔드포인트 구현
  - [x] 7.1 `ShareController.java`에서 `/share/{slug}/**` 및 `/share/*.json` 요청 시 `ShareNotFoundException`(`404 Not Found`) 반환하도록 수정
  - [x] 7.2 `MiniAppController.java`에 `@DeleteMapping("/{appKey}")` 구현 (`{"deleted":true/false}` 반환 + `deleteAsync` 트리거)
  - _Requirements: 2.7, 3.1, 3.2, 3.3, 3.4_ / _Design: 3.4_

- [x] 8. 웹 슬라이스 및 전체 통합 테스트 보강 및 체크포인트 D
  - [x] 8.1 `ShareControllerTest.java`에 `/share/{slug}/data.json`, `/share/todo.json`, `/share/a/b` `404` 차단 및 대문자 슬러그 `400` 검증 추가
  - [x] 8.2 `MiniAppControllerTest.java`에 `DELETE /api/mini/{appKey}` PIN `401`, 잘못된 키 `400`, 삭제 `{"deleted":true}`, 미존재 `{"deleted":false}` 테스트 추가
  - [x] 8.3 `MiniAppIntegrationTest.java`에 `PUT` → `GET` → `DELETE`(true) → 재`DELETE`(false) 멱등성 → `GET` `{}` 확인 + `@Async deleteJson` 검증 + `/share/{slug}/data.json` `404` 통합 검증 추가
  - [x] 8.4 `mvn test -pl myshare -Dtest="ShareControllerTest,MiniAppControllerTest,MiniAppIntegrationTest"` 통과 확인
  - _Validates: Requirements 2.7, 3.1~3.8_

---

### E. 전체 통합 검증 및 5대 품질 가드레일 (Quality Guardrails & Finalization)

- [x] 9. 5대 품질 가드레일 전체 파이프라인 검증
  - [x] 9.1 `mvn -B -q spotless:apply -pl myshare` 실행
  - [x] 9.2 `mvn -B clean install -pl myshare -am` 실행 (Spotless, Error Prone, ArchUnit, JaCoCo 80%+/70%+, PMD/CPD 통과)
  - _Requirements: Non-Functional 1_ / _Design: 6.2_

- [x] 10. CodeGraph 동기화 및 Memory Bank 갱신
  - [x] 10.1 `codegraph sync` 실행
  - [x] 10.2 `memory-bank/activeContext.md`에 `myshare/003-share-folder-unification-and-delete` 완료 내역(Compacted) 및 `.env`/`curl` 확인사항 기록
  - _Workflow 규칙 준수_
