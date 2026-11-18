# Requirements Document: github-calendar-sync

> **폴더 위치**: `.kiro/specs/mycalendar/003-github-calendar-sync/requirements.md`
> **관련 규칙**: `rules/project/spec-conventions.md`, `rules/project/tech-stack.md`

---

## 1. Introduction (개요 및 배경)

### 1.1. 배경 및 목적
- **현재 상태 및 문제점**: `mycalendar` 모듈은 일정 데이터를 DB에만 저장하고 있어 외부 AI(Mike)가 최신 일정 스냅샷을 직접 조회할 수 없다.
- **핵심 목표**: `mycalendar` DB에서 일정이 생성·수정·삭제될 때마다 비공개 GitHub 레포지토리(`gonyda/mike`)의 `calendar/mycalendar.json` 파일로 전체 일정을 단방향 덮어쓰기(스냅샷) 동기화하고, 수동 전체 재동기화 엔드포인트를 제공한다.
- **선행 스펙과의 연계**: `001-couple-calendar`, `002-calendar-bugfix-and-cleanup`에서 구축된 `Schedule` 엔티티, `ScheduleService`, `ScheduleRepository`, KST 타임존 정책 위에서 동작한다.

### 1.2. 이번 스펙의 범위 (In-Scope)
1. **트랜잭션 커밋 후 비동기 트리거**: `ScheduleService`의 생성·수정·삭제 커밋 완료(`AFTER_COMMIT`) 시 자동 동기화 트리거.
2. **직렬화된 GitHub Contents API 푸시**: 단일 스레드 실행기 기반 직렬화, `GET` `sha` 조회(200/404), `PUT` 덮어쓰기, `409 Conflict` 1회 재시도, `RestClient` 타임아웃 설정.
3. **결정론적 Pretty JSON 스냅샷 생성**: `Asia/Seoul` 기준 `syncedAt`, 시작일/시간/ID 오름차순 정렬, UTF-8 Pretty JSON 직렬화.
4. **수동 전체 재동기화 엔드포인트**: `POST /schedules/sync/github` 제공.
5. **설정 바인딩 및 Fail-safe**: `@ConfigurationProperties(prefix = "mycalendar.github-sync")`, 기본값 `enabled=false`, 토큰 누락 시 no-op, 외부 실패가 CUD 트랜잭션/응답에 무영향.

### 1.3. 제외 및 이연 범위 (Out-of-Scope / Deferred)
- **양방향 병합(Two-way Sync)**: GitHub 측 수정 내역을 DB로 역반영하지 않는다(`mycalendar` DB가 유일한 Source of Truth).
- **인증/인가 추가**: 기존 `mycalendar` 정책에 따라 별도 로그인/보안 필터를 추가하지 않는다.

---

## 2. Glossary (용어 사전)

- **Snapshot_Sync**: DB의 전체 `Schedule` 목록을 조회하여 단일 JSON 문서로 직렬화하고 GitHub 파일 전체를 덮어쓰는 단방향 동기화 방식.
- **GitHub_Contents_API**: `https://api.github.com/repos/{owner}/{repo}/contents/{path}` 엔드포인트(`GET`으로 `sha` 조회, `PUT`으로 커밋 생성/갱신).
- **Coalescing_Serializer**: 단일 스레드 실행기와 대기 플래그를 조합하여 동시 동기화 요청을 직렬화하고 대기 중인 중복 요청을 병합하는 실행 구조.

---

## 3. Requirements (기능 요구사항)

### Requirement 1: 트랜잭션 커밋 후 비동기 동기화 트리거 및 Fail-safe

**User Story:**  
캘린더 사용자로서, 일정을 등록·수정·삭제할 때 GitHub 동기화 상태와 무관하게 일정 관리가 지연 없이 성공하기를 원한다.

#### Acceptance Criteria
1. **WHEN** `ScheduleService`의 `create`, `update`, `delete` 트랜잭션이 DB에 커밋 완료되면, **THE** 시스템 **SHALL** `TransactionPhase.AFTER_COMMIT` 리스너를 통해 비동기 스냅샷 동기화를 요청한다.
2. **IF** 유효성 검증 실패 또는 예외로 인해 트랜잭션이 롤백되면, **THEN THE** 시스템 **SHALL** GitHub 동기화를 실행하지 않는다.
3. **IF** GitHub API 오류(5xx, 4xx), 네트워크 타임아웃, 예외가 발생하더라도, **THEN THE** 시스템 **SHALL** 예외를 로깅 후 삼켜 일정 CUD 요청을 실패시키거나 롤백하지 않으며 응답 지연을 유발하지 않는다.

### Requirement 2: 직렬화된 GitHub Contents API 호출 및 409 재시도

**User Story:**  
시스템 운영자로서, 연속된 일정 변경 요청에서도 GitHub 파일 `sha` 충돌이나 순서 역전 없이 최신 스냅샷이 반영되기를 원한다.

#### Acceptance Criteria
1. **THE** 시스템 **SHALL** 단일 스레드 실행기(`SingleThreadExecutor`)를 통해 모든 동기화 작업을 직렬화하며, 작업 실행 시점마다 DB에서 최신 전체 일정을 다시 조회한다.
2. **WHEN** 동기화를 실행할 때, **THE** 시스템 **SHALL** `GET /repos/{owner}/{repo}/contents/{path}?ref={branch}`를 호출해 `200 OK`이면 `sha`를 추출하고 `404 Not Found`이면 `sha` 없이 신규 파일 생성으로 진행한다.
3. **WHEN** `PUT /repos/{owner}/{repo}/contents/{path}`를 호출할 때, **THE** 시스템 **SHALL** `Authorization: Bearer {token}`, `Accept: application/vnd.github+json`, `X-GitHub-Api-Version: 2022-11-28` 헤더와 개행 없는 Base64 `content` 바디를 전송한다.
4. **IF** `PUT` 요청이 `409 Conflict`를 반환하면, **THEN THE** 시스템 **SHALL** 최신 `sha`를 재조회하여 최대 1회 재시도한다.
5. **THE** 시스템 **SHALL** Spring Boot 표준 `RestClient`에 connect/read 타임아웃(3초 / 5초)을 지정한다.

### Requirement 3: 스냅샷 JSON 스키마 및 결정론적 정렬

**User Story:**  
외부 AI(Mike)로서, 한국 시간 기준의 읽기 쉬운 일관된 JSON 스냅샷을 조회하고 싶다.

#### Acceptance Criteria
1. **THE** 시스템 **SHALL** `syncedAt`(KST `+09:00`), `timezone`(`"Asia/Seoul"`), `totalCount`, `events` 구조의 UTF-8 Pretty JSON을 생성한다.
2. **THE** 시스템 **SHALL** `events` 배열을 `startDate` 오름차순, `scheduleTime` 오름차순(null 후순위), `id` 오름차순으로 정렬한다.
3. **THE** 시스템 **SHALL** 실제 `Schedule` 엔티티에 존재하는 필드(`id`, `category`, `startDate`, `endDate`, `scheduleTime`, `content`)만 매핑한다.

### Requirement 4: 수동 재동기화 엔드포인트 및 설정/보안

**User Story:**  
운영자로서, 필요 시 수동으로 전체 일정을 재동기화할 수 있고 로컬/테스트 환경에서는 외부 푸시가 차단되기를 원한다.

#### Acceptance Criteria
1. **WHEN** `POST /schedules/sync/github` 요청이 수신되면, **THE** 시스템 **SHALL** 직렬화된 실행기에서 즉시 전체 동기화를 수행하고 결과를 JSON으로 반환한다.
2. **WHERE** `mycalendar.github-sync.enabled`가 `false`이거나 `token`이 blank일 때, **THE** 시스템 **SHALL** GitHub HTTP 요청을 수행하지 않고 로그만 기록한 뒤 no-op으로 종료한다.
3. **THE** 시스템 **SHALL** 토큰 값을 코드·설정 파일·로그에 하드코딩하거나 노출하지 않고 `${MIKE_GITHUB_TOKEN:}` 환경변수로 바인딩한다.

---

## 4. Non-Functional & Quality Requirements

1. **5대 품질 가드레일**: Spotless, Error Prone, ArchUnit, JaCoCo(라인 80%+, 브랜치 70%+), PMD/CPD 통과.
2. **보안 빈 비존재 불변식**: `MycalendarApplicationTest`의 보안/인증/인가 빈 미존재 검증 통과.
3. **CodeGraph 동기화**: 변경 완료 후 `codegraph sync` 실행.
