# Implementation Plan: 개별 크롤러 실행

## Overview

mycrawler 모듈의 크롤링 실행 방식을 "전체 일괄 실행"에서 "개별 타겟 실행"으로 전환합니다. AdminController의 엔드포인트 교체, SchedulerService의 개별 순회 방식 전환, 대시보드 UI 변경, wepoll-stock 크롤러 등록을 구현합니다.

## Tasks

- [x] 1. application.yml에 wepoll-stock 타겟 추가
  - [x] 1.1 application.yml의 crawler.targets 목록에 wepoll-stock 항목 추가
    - name: `wepoll-stock`, url: `https://wepoll.kr/g2/bbs/board.php?bo_table=stock`
    - 기존 fmkorea-stock 항목 유지
    - _Requirements: 5.1, 5.2_

- [x] 2. SchedulerService 개별 실행 방식 전환
  - [x] 2.1 SchedulerService에 AntiDetectionService 의존성 및 scheduledRunning AtomicBoolean 추가
    - 생성자에 AntiDetectionService 파라미터 추가
    - `private final AtomicBoolean scheduledRunning = new AtomicBoolean(false)` 필드 추가
    - `isScheduledRunning()` public 메서드 추가
    - _Requirements: 6.1, 6.3_

  - [x] 2.2 executeCrawl 메서드를 개별 순회 방식으로 변경
    - `scheduledRunning.compareAndSet(false, true)` 가드 추가 (이미 실행 중이면 로그 후 리턴)
    - `crawlerConfig.validTargets()` 목록 조회 후 각 타겟에 대해 `crawlerService.executeSingle(target.name(), TriggerSource.SCHEDULED)` 호출
    - null 반환 또는 예외 발생 시 에러 로그 기록 후 다음 타겟 계속 진행
    - 마지막 타겟이 아닌 경우 `antiDetectionService.randomInterTargetDelay()` 후 Thread.sleep 적용
    - finally 블록에서 `scheduledRunning.set(false)`
    - Thread.sleep InterruptedException 발생 시 `Thread.currentThread().interrupt()` 호출 후 경고 로그
    - _Requirements: 6.1, 6.2, 6.3_

  - [x] 2.3 SchedulerServiceTest에 개별 실행 전환 단위 테스트 추가
    - executeCrawl이 각 타겟별 executeSingle 호출하는지 검증
    - 실패 타겟을 건너뛰고 나머지 계속 진행하는지 검증
    - 타겟 간 randomInterTargetDelay 적용 검증
    - scheduledRunning 중복 실행 방지 검증
    - _Requirements: 6.1, 6.2, 6.3_

  - [x] 2.4 SchedulerServicePropertyTest에 개별 실행 property 테스트 추가
    - **Property 5: 스케줄러 개별 순회 실행** — N개 타겟에 대해 executeSingle이 정확히 N번 호출됨을 검증
    - **Validates: Requirements 6.1**
    - **Property 6: 스케줄러 장애 격리** — 일부 타겟 실패 시에도 나머지 타겟 모두 처리됨을 검증
    - **Validates: Requirements 6.2**
    - **Property 7: 스케줄러 타겟 간 딜레이 적용** — N개 타겟 실행 시 randomInterTargetDelay가 N-1번 호출됨을 검증
    - **Validates: Requirements 6.3**
    - **Property 8: 스케줄러 중복 실행 방지** — scheduledRunning이 true일 때 후속 호출이 타겟을 처리하지 않음을 검증
    - **Validates: Requirements 6.1**

- [x] 3. Checkpoint - SchedulerService 변경 완료 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. AdminController 엔드포인트 교체
  - [x] 4.1 AdminController에 CrawlerConfig 의존성 추가 및 triggerCrawl 메서드 제거
    - 생성자에 CrawlerConfig 파라미터 추가
    - `triggerCrawl` 메서드 (`@PostMapping("/crawl")`) 전체 제거
    - _Requirements: 2.1, 2.3_

  - [x] 4.2 triggerSingleCrawl 메서드 추가 (`POST /admin/crawl/{targetName}`)
    - `@PostMapping("/crawl/{targetName}")` 매핑
    - `crawlerService.isRunning()` 또는 `schedulerService.isScheduledRunning()` 확인 → 어느 하나라도 true면 경고 flash(`warningMessage`) + redirect
    - `crawlerService.executeSingle(targetName, TriggerSource.MANUAL)` 호출
    - 결과 null이면 오류 flash(`errorMessage`: "크롤링 대상을 찾을 수 없습니다: {targetName}") + redirect
    - 결과 존재하면 성공 flash(`successMessage`: "{targetName} 크롤링이 완료되었습니다") + redirect
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 4.3 populateDashboardModel에 targets 모델 속성 추가
    - `model.addAttribute("targets", crawlerConfig.validTargets())` 추가
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 4.4 AdminControllerTest에 개별 실행 엔드포인트 슬라이스 테스트 추가
    - `POST /admin/crawl/{targetName}` 성공 케이스: executeSingle이 CrawlResult 반환 → successMessage flash 확인
    - `POST /admin/crawl/{targetName}` 실행 중 케이스: isRunning true → warningMessage flash 확인
    - `POST /admin/crawl/{targetName}` 미등록 타겟 케이스: executeSingle null 반환 → errorMessage flash 확인
    - `POST /admin/crawl` (기존 전체 실행) 404 응답 확인
    - `GET /admin` 모델에 targets 속성 포함 확인
    - `@WebMvcTest` 사용, `@MockitoBean`으로 CrawlerService, SchedulerService, CrawlerConfig mock
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.3, 4.1_

  - [x] 4.5 AdminControllerPropertyTest 작성
    - **Property 1: 개별 실행 위임 및 성공 응답** — isRunning false, executeSingle non-null일 때 executeSingle 호출 및 성공 flash 설정 검증
    - **Validates: Requirements 1.2, 1.3**
    - **Property 2: 실행 중 가드** — isRunning true 또는 isScheduledRunning true일 때 executeSingle 미호출 및 경고 flash 설정 검증
    - **Validates: Requirements 1.4**
    - **Property 3: 미등록 타겟 오류 처리** — executeSingle null 반환 시 오류 flash 설정 검증
    - **Validates: Requirements 1.5**
    - **Property 4: 대시보드 모델 타겟 목록 포함** — validTargets 반환값이 모델의 "targets"에 그대로 포함됨을 검증
    - **Validates: Requirements 3.1, 4.1, 4.2**

- [x] 5. Checkpoint - AdminController 변경 완료 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. admin.html 대시보드 UI 변경
  - [x] 6.1 admin.html에서 "수동 실행" 섹션 제거 및 errorMessage 표시 영역 추가
    - "수동 실행" 섹션 전체(h2, form, button) 제거
    - warningMessage, successMessage와 동일한 위치에 errorMessage flash attribute 표시 div 추가
    - 스타일: `background-color: #f8d7da; border-color: #f5c6cb; color: #721c24`
    - _Requirements: 2.2_

  - [x] 6.2 admin.html에 등록된 크롤러 목록 섹션 추가
    - 섹션 제목: "등록된 크롤러"
    - 타겟 목록 테이블: 이름(name), URL(url), 실행 버튼
    - 각 행에 form (`POST /admin/crawl/{target.name()}`)
    - `th:disabled="${isRunning}"` 으로 실행 버튼 비활성화
    - 빈 목록 시 "등록된 크롤러가 없습니다" 안내 문구 표시
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 7. Final checkpoint - 전체 빌드 및 테스트 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- 모든 Task는 필수이며 빌드 성공으로 완료해야 합니다
- 테스트 어노테이션: `@MockitoBean` (Spring Boot 4.0), `@WebMvcTest` (`org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`)
- 코드 스타일: no Lombok, no var, final on params/locals, JavaDoc on all classes/methods
- Property test는 jqwik `@Property(tries = 100)` 사용, mock은 `Mockito.mock()` 직접 호출
- 빌드 검증 명령: `mvn clean install -pl mycrawler -am` (workspace root에서 실행)
- 각 task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from design document

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["2.2"] },
    { "id": 3, "tasks": ["2.3", "2.4"] },
    { "id": 4, "tasks": ["4.1"] },
    { "id": 5, "tasks": ["4.2", "4.3"] },
    { "id": 6, "tasks": ["4.4", "4.5"] },
    { "id": 7, "tasks": ["6.1", "6.2"] }
  ]
}
```
