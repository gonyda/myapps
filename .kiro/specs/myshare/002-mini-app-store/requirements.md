# Requirements Document: mini-app-store

> **폴더 위치**: `.kiro/specs/myshare/002-mini-app-store/requirements.md`
> **관련 규칙**: `rules/project/spec-conventions.md`, `rules/project/tech-stack.md`

---

## 1. Introduction (개요 및 배경)

### 1.1. 배경 및 목적
- **현재 상태 및 문제점**: `myshare` 모듈(`:8084`)은 비공개 GitHub 저장소(`gonyda/mike`, `share/{slug}.html`)의 정적 HTML 페이지를 읽기 전용으로 서빙하고 있으며, DB/JPA AutoConfiguration이 비활성화되어 있다. 정적 HTML 미니앱들이 상태 데이터를 영속 저장·공유할 수 있는 공용 백엔드 JSON 저장소가 없다.
- **핵심 목표**: `:8084` 동일 호스트에 정적 HTML 미니앱들이 공용으로 사용하는 `MINI_APP_STORE` 저장소 API(`GET` / `PUT /api/mini/{appKey}`)를 추가한다. Oracle Cloud DB 영속화, `X-Mini-Pin` 헤더 기반 PIN 인증, 저장 성공 후 `@Async` 기반 `gonyda/mike` `mini/{appKey}.json` 비동기 백업/동기화를 제공한다.
- **선행 스펙과의 연계**: `myshare/001-mike-html-share`의 `RestClient` 기반 GitHub API 연동 및 `/share/{slug}` 서빙을 그대로 유지하면서 Oracle DB 영속화 및 `/api/mini/**` API를 확장한다. 또한 `mycalendar/003-github-calendar-sync`의 `gonyda/mike` GitHub Contents API 푸시·`409 Conflict` 재시도 패턴을 재사용한다.

### 1.2. 이번 스펙의 범위 (In-Scope)
1. **Oracle DB 및 JPA 영속화 활성화**: `myshare`의 DataSource/Hibernate/JPA 제외 설정을 해제하고 `mycalendar`·`mystudy`와 동일한 Oracle Cloud Wallet 프로파일(`local`/`prod`) 및 테스트 H2 설정을 구성한다.
2. **`MINI_APP_STORE` 엔티티 및 리포지토리**: `app_key VARCHAR2(64) PRIMARY KEY`, `data_json CLOB NOT NULL`, `updated_at TIMESTAMP` 스키마에 매핑되는 JPA 엔티티 및 Spring Data JPA 리포지토리를 구현한다.
3. **REST API (`GET` / `PUT /api/mini/{appKey}`)**: `appKey` 정규식(`^[a-z0-9_-]{1,64}$`) 검증, 미존재 시 `{}` 200 OK 반환, `PUT` raw `String` 바디에 대한 `ObjectMapper.readTree()` JSON 문법 검증 및 최대 크기(기본 1MB) 검증 후 upsert를 제공한다.
4. **PIN 인증 (`X-Mini-Pin`)**: `/api/mini/**` 모든 요청에 대해 `X-Mini-Pin` 헤더와 설정값(`myshare.mini-app.pin` / `MINI_APP_PIN`)을 검증하며, 기존 `/share/**` 및 `/health` 경로에는 영향을 주지 않는다.
5. **GitHub 비동기 동기화 (`@Async`)**: `PUT /api/mini/{appKey}` DB 저장 성공 시 백그라운드(`@Async`) 단일 스레드 실행기로 `gonyda/mike` 레포 `mini/{appKey}.json`에 커밋·푸시하고, 실패해도 DB 저장 응답에는 영향을 주지 않는다.

### 1.3. 제외 및 이연 범위 (Out-of-Scope / Deferred)
- **미니앱별 세분화 RBAC / 사용자 계정 로그인**: 공용 단일 PIN(`X-Mini-Pin`) 인증만 제공하며, Spring Security 도입은 범위에서 제외한다.
- **`DELETE /api/mini/{appKey}` 또는 목록 조회 API**: 외부 노출 최소화를 위해 단건 `GET`/`PUT`만 제공한다.
- **정적 HTML 프론트엔드 코드 수정**: `gonyda/mike`에 올라가는 HTML 미니앱 파일 자체는 본 스펙 범위 밖이다.

---

## 2. Glossary (용어 사전)

### 2.1. 기존 재사용 용어
- **myshare**: `com.myapps.web.myshare` 패키지의 Spring Boot 4.0 Web 모듈 (포트 `8084`).
- **GitHubShareProperties**: `myshare.github.*` (`enabled`, `token`, `owner`, `repo`, `branch`, `basePath`)를 바인딩하는 설정 프로퍼티.
- **MIKE_GITHUB_TOKEN**: `gonyda/mike` 저장소 접근용 GitHub PAT 환경변수.

### 2.2. 본 스펙 신규 용어
- **MiniAppKey**: `^[a-z0-9_-]{1,64}$` 규칙을 만족하는 미니앱 식별자 값 객체.
- **MiniAppStore**: `MINI_APP_STORE` 테이블에 매핑되는 JPA 엔티티 (`app_key`, `data_json`, `updated_at`).
- **MiniAppPinFilter**: `/api/mini/**` 요청의 `X-Mini-Pin` 헤더를 상수 시간 비교로 검증하는 서블릿 필터.
- **MINI_APP_PIN**: 운영 환경변수 및 `myshare.mini-app.pin` 프로퍼티에 매핑되는 공용 인증 PIN.
- **MiniAppGitHubSyncService**: `@Async` 단일 스레드 실행기로 `gonyda/mike` `mini/{appKey}.json` 푸시를 직렬화·비동기 실행하는 서비스.

---

## 3. Requirements (기능 요구사항)

### Requirement 1: `MINI_APP_STORE` 영속화 및 `appKey` 검증

**User Story:**  
정적 HTML 미니앱 클라이언트로서, `appKey` 단위로 자유 형식 JSON 데이터를 DB에 저장하고 조회하고 싶다.  
그래야 서버 재시작 후에도 미니앱 상태가 유지된다.

#### Acceptance Criteria

1. **THE** `myshare` **SHALL** `MINI_APP_STORE` 테이블(`app_key VARCHAR2(64) PRIMARY KEY`, `data_json CLOB NOT NULL`, `updated_at TIMESTAMP`)과 매핑되는 JPA 엔티티 및 리포지토리를 제공한다.
2. **WHEN** `/api/mini/{appKey}` 요청이 들어오면, **THE** `MiniAppKey` **SHALL** `appKey`가 `^[a-z0-9_-]{1,64}$` 정규식을 만족하는지 검증한다.
3. **IF** `appKey`가 null, 공백, 대문자 포함, 64자 초과, 점(`.`)·슬래시(`/`)·특수문자 포함 또는 하위 경로(`/api/mini/{appKey}/...`) 요청이면, **THEN THE** API **SHALL** `400 Bad Request` JSON 오류 응답을 반환한다.

---

### Requirement 2: `GET` / `PUT /api/mini/{appKey}` REST API

**User Story:**  
미니앱 프론트엔드로서, 스키마 제약 없이 자유로운 JSON 문자열을 `PUT`으로 저장하고 `GET`으로 그대로 조회하고 싶다.  
그래야 별도 백엔드 DTO 수정 없이 다양한 정적 HTML 미니앱을 즉시 연동할 수 있다.

#### Acceptance Criteria

1. **WHEN** 유효한 `appKey`로 `GET /api/mini/{appKey}`를 호출하고 해당 데이터가 DB에 존재하면, **THE** API **SHALL** 저장된 `data_json`을 `Content-Type: application/json`으로 가공 없이 반환한다 (`200 OK`).
2. **IF** 유효한 `appKey`로 `GET /api/mini/{appKey}`를 호출했으나 DB에 해당 데이터가 없으면, **THEN THE** API **SHALL** `200 OK` 상태와 빈 JSON 객체 `{}`를 `Content-Type: application/json`으로 반환한다.
3. **WHEN** `PUT /api/mini/{appKey}` 요청이 도착하면, **THE** API **SHALL** 요청 바디를 raw `String`으로 수신하여 비어 있지 않고 UTF-8 바이트 크기가 제한(기본 1MB = 1,048,576 bytes) 이하인지 검증한다.
4. **WHEN** `PUT /api/mini/{appKey}` 바디의 크기가 허용 범위 이내이면, **THE** `MiniAppJsonValidator` **SHALL** Jackson 3 `ObjectMapper.readTree()`로 유효한 JSON 문법인지(후행 토큰 불허 포함) 검증한다.
5. **IF** 요청 바디가 누락·공백·크기 초과이거나 유효하지 않은 JSON 문법이면, **THEN THE** API **SHALL** DB 저장 및 GitHub 동기화를 수행하지 않고 `400 Bad Request`를 반환한다.
6. **WHEN** 유효한 JSON 바디로 `PUT /api/mini/{appKey}`가 호출되면, **THE** `MiniAppStoreService` **SHALL** `MINI_APP_STORE`에 upsert(미존재 시 insert, 존재 시 `data_json` 및 `updated_at` 갱신)를 수행하고 `200 OK`(`Content-Type: application/json`)로 저장된 JSON을 반환한다.

---

### Requirement 3: `X-Mini-Pin` 헤더 기반 PIN 인증

**User Story:**  
서버 운영자로서, 외부에 개방된 `:8084` 서버의 `/api/mini/**` API를 공용 PIN 헤더로 보호하고 싶다.  
그래야 기존 `/share/{slug}` 공개 서빙은 유지하면서도 인가되지 않은 미니앱 저장소 접근을 차단할 수 있다.

#### Acceptance Criteria

1. **WHEN** `/api/mini/**` (또는 `/api/mini`) 경로로 `GET`, `PUT` 등 HTTP 요청이 들어오면, **THE** `MiniAppPinFilter` **SHALL** 컨트롤러 실행 전에 `X-Mini-Pin` 요청 헤더를 검증한다.
2. **IF** 설정된 `myshare.mini-app.pin`(`MINI_APP_PIN`)이 비어 있거나, 요청에 `X-Mini-Pin` 헤더가 없거나, 설정값과 일치하지 않으면, **THEN THE** `MiniAppPinFilter` **SHALL** `401 Unauthorized`와 `Content-Type: application/json` 오류 본문을 반환한다.
3. **WHERE** 요청 경로가 `/share/**` 또는 `/health`인 경우, **THE** `MiniAppPinFilter` **SHALL** PIN 검증을 적용하지 않고 기존 서빙 동작을 그대로 통과시킨다.
4. **THE** `myshare` **SHALL** Spring Security 의존성이나 Security/Authentication/Authorization 명칭의 빈을 추가하지 않는다.

---

### Requirement 4: `@Async` 기반 `gonyda/mike` GitHub 비동기 동기화

**User Story:**  
시스템 운영자로서, `PUT /api/mini/{appKey}`로 DB에 저장된 JSON을 `gonyda/mike` 저장소의 `mini/{appKey}.json`에도 비동기로 백업하고 싶다.  
그래야 GitHub 이력 관리가 가능하면서도 GitHub API 지연·장애가 미니앱 응답 속도에 영향을 주지 않는다.

#### Acceptance Criteria

1. **WHEN** `PUT /api/mini/{appKey}`의 DB upsert 트랜잭션이 성공적으로 완료되면, **THE** `MiniAppController` **SHALL** `@Async` 메서드(`MiniAppGitHubSyncService.syncAsync`)를 호출하여 백그라운드 동기화를 예약한다.
2. **WHILE** `MiniAppGitHubSyncService`가 실행될 때, **THE** 실행기(`miniAppGitHubSyncExecutor`) **SHALL** 단일 스레드(`corePoolSize=1, maxPoolSize=1`)로 작업을 직렬화하여 `gonyda/mike` 브랜치 HEAD 경합 및 `409 Conflict`를 최소화한다.
3. **WHEN** `RestClientGitHubMiniAppSyncClient`가 `mini/{appKey}.json`을 업로드할 때, **THE** 클라이언트는 `GET /contents`로 기존 파일의 `sha`를 조회(404 시 신규 생성)한 뒤 `PUT /contents`로 Base64 UTF-8 본문을 푸시하며, `409 Conflict` 발생 시 최신 `sha`를 재조회해 재시도한다.
4. **IF** GitHub 동기화가 비활성화되어 있거나 `MIKE_GITHUB_TOKEN`이 비어 있으면, **THEN THE** 동기화 클라이언트는 외부 HTTP 호출 없이 작업을 건너뛴다.
5. **IF** GitHub API 타임아웃·네트워크 오류·HTTP 에러가 발생하더라도, **THEN THE** `MiniAppGitHubSyncService` **SHALL** 예외를 로깅하고 삼켜 `PUT /api/mini/{appKey}` API 응답과 DB 저장 데이터에 어떠한 영향도 주지 않는다.

---

## 4. Non-Functional & Quality Requirements (비기능 및 품질 요구사항)

1. **5대 품질 가드레일 (Task 완료 필수 기준)**:
   - **Spotless**: Java 포맷팅 자동 교정 (`mvn spotless:apply`).
   - **Error Prone**: 정적 결함 컴파일 타임 차단 (컴파일 경고 0건).
   - **ArchUnit**: DDD 4계층(`interfaces` → `application` → `domain`, `application` → `infrastructure` 의존 금지) 아키텍처 규칙 준수.
   - **JaCoCo**: 모듈 전체 테스트 커버리지 **명령어/라인 80% 이상, 브랜치 70% 이상** 달성.
   - **PMD & CPD**: 복잡도(`CognitiveComplexity`), 안티패턴 및 중복 코드 0건.
2. **CodeGraph 동기화**:
   - 구현 및 검증 완료 후 `codegraph sync` 필수 수행.
