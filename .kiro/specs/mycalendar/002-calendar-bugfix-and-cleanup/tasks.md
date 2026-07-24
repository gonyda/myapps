# Implementation Plan

## Overview

mycalendar 모듈의 복합 버그(HiddenHttpMethodFilter 비활성화, 타임존 불일치)를 수정하고, UX 개선(캘린더 셀 클릭 모달 등록) 및 불필요한 기능(댓글) 제거를 수행합니다. Exploratory bugfix workflow에 따라 수정 전 버그 재현 → 보존 테스트 → 수정 구현 → 검증 순서로 진행합니다.

## Tasks

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - HiddenHttpMethodFilter 비활성화 및 타임존 불일치 재현
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists
  - **Scoped PBT Approach**: 버그 조건을 구체적 케이스로 한정하여 재현성 확보
  - **Bug Condition 1 - PUT 라우팅 실패**: `POST /schedules/{id}` + `_method=PUT` 전송 시 `@PutMapping("/{id}")`로 라우팅되지 않고 405 반환 확인
  - **Bug Condition 2 - DELETE 라우팅 실패**: `POST /schedules/{id}` + `_method=DELETE` 전송 시 `@DeleteMapping("/{id}")`로 라우팅되지 않고 405 반환 확인
  - **Bug Condition 3 - 타임존 불일치**: JVM 타임존을 UTC로 설정하고 한국시간 0시~9시 시점 시뮬레이션 시 `LocalDate.now()`가 전날 날짜를 반환하는지 확인
  - `isBugCondition(input)`: (input.method==POST AND input.hasParameter("_method") AND hiddenHttpMethodFilterDisabled()) OR (JVM_DEFAULT_TIMEZONE != "Asia/Seoul" AND currentTimeInKST().hour < 9)
  - `expectedBehavior(result)`: PUT/DELETE 요청이 정상 라우팅(302), 타임존 무관하게 Asia/Seoul 기준 정확한 today 값 반환
  - MockMvc 기반 @WebMvcTest 슬라이스 테스트 작성
  - jqwik @Property로 랜덤 ZoneId/시각 조합에서 타임존 버그 재현
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the bug exists)
  - Document counterexamples found to understand root cause
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - 기존 동작 보존 (일정 생성, 네비게이션, 상세 조회)
  - **IMPORTANT**: Follow observation-first methodology
  - **IMPORTANT**: 수정 전 코드에서 정상 동작하는 요청을 관찰하고, 해당 동작을 property-based test로 캡처
  - Observe: `POST /schedules` (일정 생성) → 302 리다이렉트, 일정 정상 저장
  - Observe: `GET /calendar/{year}/{month}` (네비게이션) → 200, 정확한 prev/next 월 계산
  - Observe: `GET /schedules/{id}` (상세 조회) → 200, 일정 데이터 정상 표시
  - jqwik @Property: 랜덤 year/month 조합으로 캘린더 네비게이션 시 올바른 prev/next URL 생성 확인
  - jqwik @Property: 랜덤 ScheduleCreateCommand로 일정 생성 → 조회 시 데이터 일관성 확인
  - jqwik @Property: CalendarViewHelper의 prev/next 월 계산이 year 경계(12월→1월, 1월→12월)에서도 정확한지 확인
  - MockMvc 기반 테스트로 일정 생성 POST가 정상 302 리다이렉트하는지 확인
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 3. Fix for HiddenHttpMethodFilter 비활성화 및 타임존 불일치 버그

  - [x] 3.1 HiddenHttpMethodFilter 활성화 (application.yml)
    - `spring.mvc.hiddenmethod.filter.enabled: true` 설정 추가
    - 이 설정으로 Spring Boot 4.0에서 HiddenHttpMethodFilter가 자동 등록됨
    - HTML 폼의 `_method=PUT/DELETE` hidden 필드가 정상적으로 HTTP 메서드 변환됨
    - _Bug_Condition: isBugCondition(input) where input.hasParameter("_method") AND hiddenHttpMethodFilterDisabled()_
    - _Expected_Behavior: PUT/DELETE 요청이 @PutMapping/@DeleteMapping으로 정상 라우팅 (302 redirect)_
    - _Preservation: POST /schedules 일정 생성은 기존과 동일하게 동작_
    - _Requirements: 2.1, 2.2_

  - [x] 3.2 LocalDate.now() 타임존 명시 (CalendarController)
    - `private static final ZoneId KST = ZoneId.of("Asia/Seoul")` 상수 추가
    - `redirectToCurrentMonth()`: `LocalDate.now()` → `LocalDate.now(KST)`
    - `showCalendar()`: `LocalDate.now()` → `LocalDate.now(KST)`
    - `ZoneId` import 추가
    - JVM 기본 타임존에 관계없이 한국 시간 기준 정확한 오늘 날짜 계산
    - _Bug_Condition: isBugCondition(input) where JVM_DEFAULT_TIMEZONE != "Asia/Seoul" AND currentTimeInKST().hour < 9_
    - _Expected_Behavior: LocalDate.now(ZoneId.of("Asia/Seoul")) 결과가 항상 한국 시간 기준 today_
    - _Preservation: 서버 타임존이 Asia/Seoul이고 오전 9시 이후일 때 기존과 동일한 날짜 표시_
    - _Requirements: 2.3_

  - [x] 3.3 댓글 기능 전체 삭제
    - **삭제 대상 파일:**
      - `interfaces/api/CommentController.java`
      - `interfaces/dto/CommentForm.java`
      - `application/service/CommentService.java`
      - `application/dto/CommentCreateCommand.java`
      - `application/dto/CommentResponse.java`
      - `application/dto/CommentUpdateCommand.java`
      - `application/exception/CommentNotFoundException.java`
      - `application/exception/InvalidCommentException.java`
      - `domain/model/ScheduleComment.java`
      - `domain/repository/CommentRepository.java`
      - `templates/comment-form.html`
      - `templates/fragments/comment-list.html`
    - **수정 대상:**
      - `domain/model/Schedule.java`: `@OneToMany comments` 필드 및 관련 메서드 삭제
      - `application/service/ScheduleService.java`: `toResponse()`에서 댓글 매핑 로직 삭제
      - `application/dto/ScheduleResponse.java`: `comments` 필드 제거
      - `interfaces/api/ScheduleController.java`: `detail()`에서 `commentForm` 모델 속성 제거
      - `templates/schedule-detail.html`: `<section class="comments-section">` 전체 삭제
    - 댓글 관련 테스트 파일도 함께 삭제
    - _Bug_Condition: 댓글 기능이 존재하는 상태 자체가 제거 대상_
    - _Expected_Behavior: 댓글 관련 엔드포인트(/comments/*, /schedules/*/comments) 미존재 (404)_
    - _Preservation: 일정 상세 페이지의 일정 정보(카테고리, 시작일, 종료일, 시간, 내용) 표시는 기존과 동일_
    - _Requirements: 2.5, 3.3_

  - [x] 3.4 일정 등록 UX 변경 - 캘린더 셀 클릭 모달
    - `templates/calendar.html`: `nav.actions`의 "일정 추가" 링크 제거
    - `templates/calendar.html`: 날짜 셀(`div.day-cell`)에 클릭 이벤트 추가 (data-date attribute)
    - `templates/calendar.html`: 모달 HTML 구조 추가 (일정 등록 폼 인라인화)
    - `templates/calendar.html`: 모달 open/close JavaScript 로직 추가
    - `templates/calendar.html`: 모달 내 폼의 startDate 필드를 클릭한 날짜로 자동 설정
    - `css/style.css`: 모달 overlay, container, close button 스타일 추가
    - _Bug_Condition: 일정 추가 시 전체 페이지 이동이 발생하는 UX_
    - _Expected_Behavior: 날짜 셀 클릭 → 해당 날짜 시작일 미리 설정된 모달 팝업 표시_
    - _Preservation: 캘린더 내 일정 목록 표시(최대 5개), 스와이프 제스처 동작 보존_
    - _Requirements: 2.4, 3.5, 3.6_

  - [x] 3.5 ScheduleController 모달 지원
    - `ScheduleController.create()`: 모달에서 직접 POST하므로 기존 엔드포인트 유지
    - `ScheduleController.newForm()`: `startDate` 쿼리 파라미터를 @RequestParam(required=false)로 받아 폼에 미리 설정
    - 모달 폼 제출 시 redirect 경로가 캘린더 페이지로 돌아가도록 확인
    - _Bug_Condition: 모달에서 일정 등록 시 올바르게 처리되지 않는 경우_
    - _Expected_Behavior: 모달 폼 제출 → 일정 생성 → 캘린더 페이지로 리다이렉트_
    - _Preservation: 기존 /schedules/new 페이지 접근 시에도 정상 동작 유지_
    - _Requirements: 2.4, 3.1_

  - [x] 3.6 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - HiddenHttpMethodFilter 활성화 및 타임존 수정 후 정상 동작
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior
    - When this test passes, it confirms the expected behavior is satisfied
    - PUT/DELETE 요청이 @PutMapping/@DeleteMapping으로 정상 라우팅됨 (302)
    - 모든 ZoneId 조합에서 Asia/Seoul 기준 정확한 today 값 반환됨
    - Run bug condition exploration test from step 1
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 3.7 Verify preservation tests still pass
    - **Property 2: Preservation** - 기존 동작 보존 확인
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - 일정 생성, 캘린더 네비게이션, 상세 조회 모두 수정 전과 동일한 결과
    - Confirm all tests still pass after fix (no regressions)

- [x] 4. Checkpoint - Ensure all tests pass
  - `mvn clean install -pl mycalendar -am` 실행하여 전체 빌드 성공 확인
  - 모든 property-based test (bug condition + preservation) 통과 확인
  - 댓글 관련 코드가 완전히 제거되었는지 최종 확인 (grep으로 CommentController, CommentService 등 잔존 참조 검색)
  - 모달 등록 플로우 동작 확인 (수동 테스트 안내)
  - Ensure all tests pass, ask the user if questions arise.

## Task Dependency Graph

```json
{
  "waves": [
    ["1", "2"],
    ["3.1", "3.2", "3.3", "3.4"],
    ["3.5"],
    ["3.6", "3.7"],
    ["4"]
  ]
}
```

## Notes

- Task 1, 2는 수정 전(UNFIXED) 코드에서 실행해야 하므로 반드시 구현(Task 3) 이전에 완료
- Task 1은 실패가 정상 — 버그 존재를 확인하는 목적
- Task 2는 성공이 정상 — 보존 대상 동작이 현재 작동 중임을 확인
- 댓글 삭제(3.3) 시 관련 테스트 파일도 함께 삭제 필요
- 모달 구현(3.4)은 프론트엔드 작업으로 E2E 테스트는 수동으로 확인
- jqwik property-based test 사용 시 `Mockito.mock()` 직접 호출 (code-style.md 참조)
- Spring Boot 4.0 테스트: `@MockitoBean`, `@WebMvcTest` 새 패키지 import 사용
