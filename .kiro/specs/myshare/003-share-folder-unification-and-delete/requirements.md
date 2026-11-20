# Requirements Document: share-folder-unification-and-delete

> **폴더 위치**: `.kiro/specs/myshare/003-share-folder-unification-and-delete/requirements.md`  
> **관련 규칙**: `rules/project/spec-conventions.md`, `rules/project/tech-stack.md`

---

## 1. Introduction (개요 및 배경)

### 1.1. 배경 및 목적
- **현재 상태 및 문제점**:
  - `myshare/001-mike-html-share`는 `gonyda/mike` 저장소의 `share/{slug}.html` 단일 경로에서만 HTML을 조회하며, 슬러그 검증이 대문자를 허용(`^[a-zA-Z0-9_-]{1,200}$`)하고, 중첩 경로(`/share/{slug}/**`) 요청에 `400 Bad Request`를 반환한다.
  - `myshare/002-mini-app-store`는 미니앱 JSON을 `mini/{appKey}.json`(`MINI_APP_GITHUB_BASE_PATH` 기본값 `mini`) 경로에 동기화하고 있으며, `DELETE /api/mini/{appKey}` 엔드포인트가 제공되지 않는다.
  - 하나의 미니앱/공유 페이지에 대한 HTML(`share/`)과 JSON(`mini/`)이 서로 다른 루트 폴더로 분리되어 있어, 단일 프로젝트 단위로 관리하기 어렵다.
- **핵심 목표**:
  1. `gonyda/mike` 저장소 파일 구조를 `share/{slug}/` 폴더 단위(`share/{slug}/index.html`, `share/{slug}/data.json`)로 통일한다.
  2. `GET /share/{slug}` 서빙 시 `share/{slug}/index.html` 우선 조회 및 하위 호환 `share/{slug}.html` 폴백을 제공하고, `/share/{slug}/data.json` 등 하위/JSON 직접 접근을 `404 Not Found`로 철저히 차단한다.
  3. 멱등한 `DELETE /api/mini/{appKey}` 엔드포인트(DB 삭제 + `@Async` GitHub `share/{appKey}/data.json` 단일 파일 삭제)를 추가한다.
- **선행 스펙과의 연계**: `myshare/001-mike-html-share`의 HTML 캐싱·ETag 서빙과 `myshare/002-mini-app-store`의 `MINI_APP_STORE`·`X-Mini-Pin`·`@Async` 단일 스레드 GitHub 동기화 기반을 확장·통합한다.

### 1.2. 이번 스펙의 범위 (In-Scope)
1. **GitHub 폴더 구조 통일 (`share/{slug}/`)**: `MINI_APP_GITHUB_BASE_PATH` 기본값을 `mini` → `share`로 변경하고, `PUT /api/mini/{appKey}` 동기화 경로를 `{basePath}/{appKey}/data.json`으로 통일한다.
2. **HTML 서빙 우선순위 & 하위 호환 폴백**: `GET /share/{slug}` 요청 시 `{slug}` 패턴(`^[a-z0-9_-]{1,200}$`) 검증 후 `share/{slug}/index.html`을 우선 조회하고, 미존재(`NotFound`) 시 `share/{slug}.html`로 폴백한다.
3. **`.json` 및 중첩 경로 접근 보안 차단**: `/share/{slug}/data.json` 등 `/share/{slug}/**` 요청과 `.json` 직접 접근을 `404 Not Found`로 차단하여 오직 `/share/{slug}` 경로에서만 HTML을 서빙한다.
4. **`DELETE /api/mini/{appKey}` 엔드포인트 및 비동기 GitHub 파일 삭제**:
   - `X-Mini-Pin` 인증(`401`) 및 `appKey` 검증(`400`) 적용.
   - `ADMIN.MINI_APP_STORE` 멱등 삭제(`200 OK`, `{"deleted": true/false}`).
   - `@Async`로 `share/{appKey}/data.json`만 삭제 커밋·푸시(이미 없으면 조용히 스킵, 같은 폴더의 `index.html` 무간섭).
5. **단위/슬라이스/통합 테스트 보강 및 5대 품질 가드레일 통과**.

### 1.3. 제외 및 이연 범위 (Out-of-Scope / Deferred)
- **`gonyda/mike` 기존 파일 일괄 마이그레이션 스크립트**: 기존 `share/{slug}.html` 파일은 하위 호환 폴백으로 무중단 지원되므로 원격 저장소 파일 자동 이동 작업은 범위에서 제외한다.
- **HTML 파일(`share/{slug}/index.html`) 삭제 API**: `DELETE /api/mini/{appKey}`는 오직 `data.json`만 삭제하며 `index.html` 생성·수정·삭제는 범위 밖이다.

---

## 2. Glossary (용어 사전)

### 2.1. 기존 재사용 용어
- **ShareSlug**: `/share/{slug}` 경로의 공유 페이지 식별자 값 객체.
- **MiniAppKey**: `^[a-z0-9_-]{1,64}$` 규칙을 만족하는 미니앱 식별자 값 객체.
- **MiniAppPinFilter**: `/api/mini/**` 요청의 `X-Mini-Pin` 헤더를 상수 시간 비교로 검증하는 서블릿 필터.
- **GitHubShareClient / GitHubMiniAppSyncClient**: `gonyda/mike` GitHub Contents API 읽기/쓰기 포트 인터페이스.

### 2.2. 본 스펙 신규 용어 (`Pascal_Snake_Case`)
- **Unified_Share_Directory**: 하나의 미니앱/공유 페이지 자산(`index.html`, `data.json`)을 함께 보관하는 `gonyda/mike` 내 `share/{slug}/` 디렉터리 구조.
- **Legacy_Html_Fallback**: `share/{slug}/index.html` 미존재 시 기존 `share/{slug}.html` 파일을 조회해 서빙하는 하위 호환 동작.
- **Idempotent_Mini_App_Delete**: 대상 `app_key` row나 원격 `data.json`이 이미 없어도 오류 없이 성공(`{"deleted": false}` 또는 조용한 스킵)으로 완료되는 삭제 동작.
- **GitHub_Contents_Delete_Request**: GitHub Contents API `DELETE` 호출 시 필수 전송하는 `message`, `sha`, `branch` JSON 요청 본문.

---

## 3. Requirements (기능 요구사항)

### Requirement 1: `share/{slug}/` 폴더 구조 통일 및 GitHub 업로드 경로 변경

**User Story:**  
시스템 운영자로서, 하나의 미니앱에 속한 `index.html`과 `data.json`을 `gonyda/mike`의 `share/{slug}/` 폴더 하나에 함께 관리하고 싶다.  
그래야 미니앱 자산과 상태 백업 파일이 흩어지지 않고 단일 디렉터리로 일관되게 관리된다.

#### Acceptance Criteria

1. **THE** `MiniAppProperties` 및 `application.yml`(기본/테스트 설정) **SHALL** `myshare.mini-app.github.base-path`(`MINI_APP_GITHUB_BASE_PATH`)의 기본값을 `share`로 설정한다.
2. **WHEN** `PUT /api/mini/{appKey}` 저장 성공 후 GitHub 동기화가 실행되면, **THE** `RestClientGitHubMiniAppSyncClient` **SHALL** `{basePath}/{appKey}/data.json`(기본 `share/{appKey}/data.json`) 경로에 Base64 UTF-8 JSON 파일을 커밋·푸시한다.
3. **WHEN** `{basePath}` 설정값 앞뒤에 슬래시(`/`)가 포함되거나 다중 세그먼트인 경우, **THE** 클라이언트는 경로 세그먼트를 안전하게 정규화하여 `{basePath}/{appKey}/data.json` URI를 구성한다.

---

### Requirement 2: `GET /share/{slug}` 우선 조회·폴백 서빙 및 `.json` 직접 접근 차단

**User Story:**  
공유 페이지 방문자 및 보안 관리자로서, `GET /share/{slug}`로 최신 폴더 구조(`share/{slug}/index.html`)와 기존 파일(`share/{slug}.html`)을 모두 문제없이 열람하되, 같은 폴더에 공존하는 `data.json`은 외부 웹 요청으로 절대 노출되지 않기를 원한다.  
그래야 기존 링크 하위 호환성과 미니앱 JSON 데이터의 기밀성을 동시에 지킬 수 있다.

#### Acceptance Criteria

1. **WHEN** `GET /share/{slug}` 요청이 들어오면, **THE** `ShareSlug` **SHALL** `{slug}`가 `^[a-z0-9_-]{1,200}$`(영문 소문자, 숫자, `_`, `-`) 패턴에 일치하는지 검증한다.
2. **IF** `{slug}`가 패턴에 일치하지 않으면(대문자, 공백, `..`, `/` 등), **THEN THE** 시스템 **SHALL** 외부 GitHub 호출 없이 `400 Bad Request`를 반환한다(단, `.json` 확장자 직접 접근 요청은 아래 기준에 따라 `404 Not Found`로 차단한다).
3. **WHEN** 유효한 `slug`에 대해 캐시 미스 또는 TTL 만료가 발생하면, **THE** `RestClientGitHubShareClient` **SHALL** 먼저 `{basePath}/{slug}/index.html`을 조회한다.
4. **IF** `{basePath}/{slug}/index.html` 조회 결과가 `NotFound`(404 또는 `type != "file"`)이면, **THEN THE** 클라이언트는 하위 호환을 위해 `{basePath}/{slug}.html`을 폴백 조회한다.
5. **IF** `index.html`과 `{slug}.html`이 모두 `NotFound`이면, **THEN THE** 시스템 **SHALL** 캐시를 제거하고 `404 Not Found`를 반환한다.
6. **IF** 우선 조회(`index.html`)에서 `Found` 또는 `NotModified`가 반환되면, **THEN THE** 클라이언트는 `{slug}.html`을 추가 호출하지 않고 즉시 해당 결과를 반환하며, **IF** GitHub API 장애(`5xx`, 타임아웃 등)나 설정 오류가 발생하면, **THEN THE** 폴백 없이 즉시 예외(`502`/`503`)를 전파한다.
7. **WHEN** 클라이언트가 `/share/{slug}/data.json`, `/share/{slug}/index.html`, `/share/{slug}/anything` 등 `/share/{slug}/**` 하위 경로나 `/share/{name}.json` 경로를 요청하면, **THE** `ShareController` **SHALL** `data.json` 또는 어떤 JSON 파일도 직접 서빙하지 않고 `404 Not Found`를 반환한다.

---

### Requirement 3: `DELETE /api/mini/{appKey}` 멱등 삭제 및 GitHub 단일 파일 비동기 삭제

**User Story:**  
미니앱 클라이언트 또는 관리자로서, `DELETE /api/mini/{appKey}`를 호출해 DB와 GitHub에 저장된 해당 앱의 JSON 데이터를 멱등하게 삭제하고 싶다.  
그래야 이미 삭제된 앱에 재요청해도 오류 없이 처리되고, 같은 폴더의 `index.html`은 안전하게 보존된다.

#### Acceptance Criteria

1. **WHEN** `DELETE /api/mini/{appKey}` 요청에 `X-Mini-Pin` 헤더가 없거나 서버 설정 `MINI_APP_PIN`과 일치하지 않으면, **THE** `MiniAppPinFilter` **SHALL** 컨트롤러 실행 전 `401 Unauthorized` JSON(`{"error":"Unauthorized"}`)을 반환한다.
2. **WHEN** 인증된 `DELETE /api/mini/{appKey}` 요청의 `appKey`가 `^[a-z0-9_-]{1,64}$`에 위배되면, **THE** 시스템 **SHALL** `400 Bad Request` JSON 오류 응답을 반환한다.
3. **WHEN** 유효한 `DELETE /api/mini/{appKey}` 요청이 들어오면, **THE** `MiniAppStoreService` **SHALL** `ADMIN.MINI_APP_STORE`에서 해당 `app_key` 행을 삭제하고, 삭제된 행이 있었으면 `{"deleted":true}`, 이미 존재하지 않았으면 예외 없이 `{"deleted":false}`와 함께 `200 OK`(`application/json`, `Cache-Control: no-store`)를 반환한다.
4. **WHEN** `DELETE /api/mini/{appKey}` 컨트롤러 처리가 성공하면, **THE** `MiniAppGitHubSyncService` **SHALL** `@Async("miniAppGitHubSyncExecutor")` 단일 스레드 실행기로 백그라운드 파일 삭제를 예약한다.
5. **WHEN** 백그라운드 삭제가 실행될 때, **THE** `RestClientGitHubMiniAppSyncClient` **SHALL** `{basePath}/{appKey}/data.json`의 현재 `sha`를 조회해 존재하는 경우에만 `DELETE /repos/{owner}/{repo}/contents/{basePath}/{appKey}/data.json`을 호출하여 해당 파일만 삭제 커밋·푸시한다.
6. **IF** `{basePath}/{appKey}/data.json`이 GitHub에 이미 존재하지 않거나(`404 NotFound`), 비활성화/토큰 미설정 상태이면, **THEN THE** 클라이언트는 예외를 던지지 않고 조용히 스킵(`false` 반환)한다.
7. **IF** GitHub `DELETE` 요청 중 `409 Conflict`가 발생하면, **THEN THE** 클라이언트는 최신 `sha`를 재조회하여 최대 2회 재시도하며(재조회 시 404면 이미 삭제된 것으로 간주해 조용히 스킵), 최종 실패 또는 네트워크/5xx 오류 시 `MiniAppGitHubSyncService`가 예외를 로깅 후 삼켜 HTTP 응답과 DB 상태에 영향을 주지 않는다.
8. **THE** GitHub 삭제 동기화는 `{basePath}/{appKey}/data.json` 단일 파일만 대상으로 하며, 동일 폴더의 `{basePath}/{appKey}/index.html`은 절대 수정·삭제하지 않는다.

---

## 4. Non-Functional & Quality Requirements (비기능 및 품질 요구사항)

1. **5대 품질 가드레일 (Task 완료 필수 기준)**:
   - **Spotless**: Java 포맷팅 자동 교정 (`mvn spotless:apply`).
   - **Error Prone**: 정적 결함 컴파일 타임 차단 (컴파일 경고 0건).
   - **ArchUnit**: `ArchitectureRuleTest` 계층 규칙(`interfaces` → `application` → `domain`, `application` → `infrastructure` 금지) 준수.
   - **JaCoCo**: `myshare` 모듈 테스트 커버리지 **라인 80% 이상, 브랜치 70% 이상** 달성.
   - **PMD & CPD**: 복잡도, 안티패턴 및 중복 코드 0건.
2. **외부 네트워크/DB 독립 테스트**: 모든 테스트는 H2 인메모리 DB, `MockMvc`, `MockRestServiceServer`, Mockito Mock으로 결정론적으로 실행된다.
3. **CodeGraph 동기화**: 구현 완료 후 `codegraph sync` 실행.
