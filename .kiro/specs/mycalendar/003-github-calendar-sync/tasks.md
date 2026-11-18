# Implementation Plan: github-calendar-sync

> **폴더 위치**: `.kiro/specs/mycalendar/003-github-calendar-sync/tasks.md`
> **관련 규칙**: `rules/workflow/task-build-validation.md`

---

## Tasks

### A. 설정, 도메인 조회 및 DTO/예외 정의
- [x] 1. `GitHubSyncProperties`, `GitHubSyncConfiguration`, `application*.yml` 설정 구성
- [x] 2. `ScheduleRepository.findAllForSync()` 추가 및 DTO/예외(`ScheduleChangedEvent`, `CalendarSnapshot`, `CalendarSnapshotEvent`, `GitHubSyncResult`, `GitHubSyncStatus`, `GitHubSyncException`) 작성

### B. 스냅샷 생성, GitHub 클라이언트 및 동기화 서비스 구현
- [x] 3. `CalendarSnapshotService` 구현 (KST 타임존, 결정론적 정렬, UTF-8 Pretty JSON 직렬화)
- [x] 4. `GitHubSyncClient` 및 `RestClientGitHubSyncClient` 구현 (GET 200/404, PUT, 409 1회 재시도, 타임아웃 3s/5s)
- [x] 5. `GitHubCalendarSyncService` 및 `ScheduleSyncEventListener` 구현 (`SingleThreadExecutor` 직렬화, `AFTER_COMMIT`, Fail-safe)
- [x] 6. `ScheduleService` CUD 메서드에 `ScheduleChangedEvent` 발행 연동

### C. 웹 컨트롤러 구현
- [x] 7. `ScheduleSyncController` 구현 (`POST /schedules/sync/github`)

### D. 테스트 작성 및 5대 품질 가드레일 검증
- [x] 8. 단위/슬라이스/통합 테스트 작성 및 기존 `ScheduleService*Test` 생성자 반영
- [x] 9. `spotless:apply`, `mvn clean install -pl mycalendar -am` (Error Prone, ArchUnit, JaCoCo 80%+, PMD/CPD), `codegraph sync` 통과
- [x] 10. `memory-bank/activeContext.md` 갱신
