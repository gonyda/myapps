# Calendar Bugfix and Cleanup Design

## Overview

mycalendar 모듈에서 발견된 복합 이슈를 수정합니다. Spring Boot 4.0 마이그레이션으로 인한 `HiddenHttpMethodFilter` 비활성화 버그(일정 수정/삭제 불가), 서버 타임존 불일치 버그(당일 날짜 표시 오류), UX 개선(캘린더 셀 클릭 모달 등록), 불필요한 기능 제거(댓글 전체 삭제)를 포함합니다. 최소 침습적(minimal-invasive) 수정 전략으로 기존 동작을 보존하면서 문제를 해결합니다.

## Glossary

- **Bug_Condition (C)**: HTML 폼에서 `_method=PUT` 또는 `_method=DELETE`를 사용하는 요청이 POST로만 처리되거나, UTC 타임존에서 `LocalDate.now()`가 한국 시간과 불일치하는 조건
- **Property (P)**: PUT/DELETE 요청이 올바르게 라우팅되고, 한국 시간 기준 정확한 오늘 날짜가 표시되는 기대 동작
- **Preservation**: 일정 생성(POST), 캘린더 네비게이션, 일정 상세 조회, 스와이프 제스처 등 기존 동작이 변경되지 않아야 하는 요구사항
- **HiddenHttpMethodFilter**: Spring Web의 서블릿 필터로, HTML 폼의 `_method` hidden 필드를 읽어 HTTP 메서드를 변환(POST→PUT/DELETE)하는 역할
- **CalendarController**: `com.myapps.web.mycalendar.interfaces.api.CalendarController` — 캘린더 뷰 렌더링 담당
- **ScheduleController**: `com.myapps.web.mycalendar.interfaces.api.ScheduleController` — 일정 CRUD 처리 담당
- **ZoneId("Asia/Seoul")**: 한국 표준시(KST, UTC+9) 타임존 식별자

## Bug Details

### Bug Condition

버그는 세 가지 독립적 조건에서 발생합니다:

1. **PUT/DELETE 라우팅 실패**: HTML 폼이 `_method=PUT` 또는 `_method=DELETE` hidden 필드를 포함하여 POST 요청을 전송할 때, Spring Boot 4.0에서 `HiddenHttpMethodFilter`가 기본 비활성화되어 메서드 변환이 이루어지지 않음
2. **타임존 불일치**: 프로덕션 서버의 JVM 기본 타임존이 UTC일 때, `LocalDate.now()`가 한국 시간 대비 -9시간으로 계산되어 자정~오전 9시 사이에 전날 날짜를 today로 판단

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type HttpRequest OR CalendarRenderRequest
  OUTPUT: boolean

  // 버그 조건 1: PUT/DELETE 라우팅 실패
  IF input.type == HTTP_FORM_SUBMISSION THEN
    RETURN input.method == POST
           AND input.hasParameter("_method")
           AND input.getParameter("_method") IN ["PUT", "DELETE"]
           AND hiddenHttpMethodFilterDisabled()
  END IF

  // 버그 조건 2: 타임존 불일치
  IF input.type == CALENDAR_RENDER THEN
    RETURN JVM_DEFAULT_TIMEZONE != "Asia/Seoul"
           AND currentTimeInKST().hour < 9
           AND LocalDate.now() != LocalDate.now(ZoneId.of("Asia/Seoul"))
  END IF

  RETURN false
END FUNCTION
```

### Examples

- **일정 수정 실패**: 사용자가 수정 폼에서 "수정" 클릭 → `POST /schedules/1` + `_method=PUT` 전송 → 필터 미작동 → `@PutMapping("/{id}")`에 매핑 안 됨 → 405 에러
- **일정 삭제 실패**: 상세 페이지에서 "삭제" 클릭 → `POST /schedules/1` + `_method=DELETE` 전송 → 필터 미작동 → `@DeleteMapping("/{id}")`에 매핑 안 됨 → 405 에러
- **날짜 표시 오류**: 한국시간 2026-01-15 02:00(= UTC 2026-01-14 17:00) → `LocalDate.now()` = 2026-01-14 → `.today` 클래스가 14일에 표시됨 (정답: 15일)
- **정상 시나리오(영향 없음)**: 한국시간 2026-01-15 10:00(= UTC 2026-01-15 01:00) → `LocalDate.now()` = 2026-01-15 → 정상 (UTC 기준으로도 같은 날짜)

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- 일정 생성(POST /schedules) 동작은 기존과 동일하게 유지
- 캘린더 이전/다음 월 네비게이션은 기존과 동일하게 유지
- 일정 상세 페이지의 일정 정보(카테고리, 시작일, 종료일, 시간, 내용) 표시는 기존과 동일
- 캘린더 내 일정 목록 표시(최대 5개)는 기존과 동일
- 스와이프 제스처 월 이동은 기존과 동일
- 서버 타임존이 Asia/Seoul이고 오전 9시 이후일 때의 날짜 표시는 기존과 동일

**Scope:**
버그 조건에 해당하지 않는 모든 요청(일반 GET 요청, POST /schedules 일정 생성, 스와이프/네비게이션)은 수정의 영향을 받지 않아야 합니다.

## Hypothesized Root Cause

Based on the bug description, the most likely issues are:

1. **HiddenHttpMethodFilter 기본 비활성화 (Spring Boot 4.0)**:
   - Spring Boot 3.x까지는 `HiddenHttpMethodFilter`가 자동 등록되었으나, Spring Boot 4.0에서 기본 비활성화됨
   - `application.yml`에 활성화 설정이 없고, `WebMvcConfigurer`에서 필터를 등록하는 코드도 없음
   - `schedule-form.html` 라인 17: `<input type="hidden" name="_method" value="PUT"/>` — 필터 없이는 무시됨
   - `schedule-detail.html` 라인 57: `<input type="hidden" name="_method" value="DELETE"/>` — 필터 없이는 무시됨

2. **LocalDate.now() 타임존 미지정 (CalendarController)**:
   - `CalendarController.redirectToCurrentMonth()`: `LocalDate.now()` 사용 (ZoneId 미지정)
   - `CalendarController.showCalendar()`: `LocalDate.now()` 사용 (ZoneId 미지정)
   - 프로덕션 서버(Oracle Cloud 등)의 JVM 기본 타임존이 UTC인 경우 한국 시간과 최대 9시간 차이 발생
   - `today` 변수가 모델에 전달되어 템플릿에서 `.today` CSS 클래스 결정에 사용됨

3. **댓글 기능 잔존**:
   - `CommentController`, `CommentService`, `ScheduleComment` 엔티티, `CommentRepository` 등이 모두 존재
   - `Schedule` 엔티티의 `@OneToMany comments` 관계가 존재
   - `ScheduleService.toResponse()`에서 댓글을 매핑하는 로직 존재
   - `schedule-detail.html`에 댓글 섹션(목록, 작성 폼, 수정/삭제 UI) 존재

4. **일정 등록 UX (페이지 전환 방식)**:
   - `calendar.html`의 `nav.actions`에 `<a class="btn-add" th:href="@{/schedules/new}">+ 일정 추가</a>` 존재
   - 전체 페이지 이동 방식으로 별도 폼 페이지(`schedule-form.html`)로 이동

## Correctness Properties

Property 1: Bug Condition - HiddenHttpMethodFilter를 통한 PUT/DELETE 라우팅

_For any_ HTTP POST 요청에 `_method=PUT` 또는 `_method=DELETE` hidden 필드가 포함된 경우, 시스템은 `HiddenHttpMethodFilter`를 통해 해당 메서드로 변환하여 `@PutMapping` 또는 `@DeleteMapping` 엔드포인트로 정상 라우팅 SHALL 한다.

**Validates: Requirements 2.1, 2.2**

Property 2: Bug Condition - Asia/Seoul 타임존 기반 오늘 날짜 계산

_For any_ 캘린더 렌더링 요청에서, 시스템은 `LocalDate.now(ZoneId.of("Asia/Seoul"))`을 사용하여 한국 시간 기준 정확한 오늘 날짜를 계산하고, JVM 기본 타임존에 관계없이 올바른 날짜에 `.today` 하이라이트를 표시 SHALL 한다.

**Validates: Requirements 2.3**

Property 3: Bug Condition - 캘린더 셀 클릭 모달 등록

_For any_ 캘린더 날짜 셀 클릭 이벤트에서, 시스템은 해당 날짜를 시작일로 미리 설정한 일정 등록 팝업(모달)을 표시 SHALL 한다.

**Validates: Requirements 2.4**

Property 4: Bug Condition - 댓글 기능 완전 제거

_For any_ 일정 상세 페이지 접근 시, 시스템은 댓글 관련 UI(댓글 목록, 댓글 작성 폼, 수정/삭제 버튼)를 표시하지 않고, 댓글 관련 엔드포인트(/comments/*, /schedules/*/comments)가 존재하지 않 SHALL 한다.

**Validates: Requirements 2.5**

Property 5: Preservation - 기존 동작 보존

_For any_ 버그 조건에 해당하지 않는 입력(일정 생성 POST, 캘린더 네비게이션, 일정 상세 조회, 스와이프 제스처)에서, 수정된 시스템은 수정 전과 동일한 결과를 반환하여 기존 동작을 보존 SHALL 한다.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct:

**1. HiddenHttpMethodFilter 활성화**

**File**: `src/main/resources/application.yml`

**Specific Changes**:
- `spring.mvc.hiddenmethod.filter.enabled: true` 설정 추가
- 이 설정으로 Spring Boot 4.0에서도 `HiddenHttpMethodFilter`가 자동 등록됨

**2. LocalDate.now() 타임존 명시**

**File**: `src/main/java/com/myapps/web/mycalendar/interfaces/api/CalendarController.java`

**Function**: `redirectToCurrentMonth()`, `showCalendar()`

**Specific Changes**:
1. `ZoneId` import 추가
2. `redirectToCurrentMonth()`: `LocalDate.now()` → `LocalDate.now(ZoneId.of("Asia/Seoul"))`
3. `showCalendar()`: `LocalDate.now()` → `LocalDate.now(ZoneId.of("Asia/Seoul"))`
4. 타임존 상수를 클래스 레벨 `private static final ZoneId KST = ZoneId.of("Asia/Seoul")`로 추출

**3. 댓글 기능 삭제**

**삭제 대상 파일:**
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

**수정 대상 파일:**

**File**: `domain/model/Schedule.java`
- `@OneToMany comments` 필드 및 관련 메서드(`addComment`, `removeComment`, `getComments`) 삭제
- `ScheduleComment` import 삭제

**File**: `application/service/ScheduleService.java`
- `toResponse()` 메서드에서 댓글 매핑 로직 삭제
- `CommentResponse`, `ScheduleComment` import 삭제
- `ScheduleResponse` 생성 시 comments 파라미터 제거

**File**: `application/dto/ScheduleResponse.java`
- `comments` 필드 제거 (record 수정)

**File**: `interfaces/api/ScheduleController.java`
- `detail()` 메서드에서 `commentForm` 모델 속성 제거
- `CommentForm` import 삭제

**File**: `templates/schedule-detail.html`
- `<section class="comments-section">` 전체 삭제

**4. 일정 등록 UX 변경 (모달)**

**File**: `templates/calendar.html`

**Specific Changes**:
1. `nav.actions`의 "일정 추가" 링크 제거
2. 날짜 셀(`div.day-cell`)에 클릭 이벤트 추가 (해당 날짜를 data attribute로 전달)
3. 모달 HTML 구조 추가 (일정 등록 폼 — `schedule-form.html`의 폼 내용을 인라인화)
4. 모달 open/close JavaScript 로직 추가
5. 모달 내 폼의 startDate 필드를 클릭한 날짜로 자동 설정

**File**: `css/style.css`
- 모달 관련 CSS 스타일 추가 (overlay, modal container, close button)

**5. ScheduleController 모달 지원**

**File**: `interfaces/api/ScheduleController.java`
- `newForm()` 메서드: `startDate` 쿼리 파라미터를 받아 폼에 미리 설정 (모달에서 직접 POST하므로 기존 엔드포인트 유지 가능)

## Testing Strategy

### Validation Approach

테스트 전략은 두 단계로 진행합니다: 먼저 수정 전 코드에서 버그를 재현하는 반례(counterexample)를 확인하고, 수정 후 올바른 동작과 기존 동작 보존을 검증합니다.

### Exploratory Bug Condition Checking

**Goal**: 수정 전 코드에서 버그를 재현하여 근본 원인 분석을 확인/반증합니다. 반증 시 원인 분석을 재수행합니다.

**Test Plan**: MockMvc를 사용하여 `_method=PUT/DELETE` 포함 POST 요청을 전송하고, 타임존을 UTC로 설정한 상태에서 캘린더를 렌더링합니다. 수정 전 코드에서 실패를 관찰합니다.

**Test Cases**:
1. **PUT 라우팅 실패 테스트**: `POST /schedules/1` + `_method=PUT` 전송 시 405 응답 확인 (수정 전 실패)
2. **DELETE 라우팅 실패 테스트**: `POST /schedules/1` + `_method=DELETE` 전송 시 405 응답 확인 (수정 전 실패)
3. **UTC 타임존 날짜 불일치 테스트**: JVM 타임존을 UTC로 설정하고 한국시간 0시~9시 시점을 시뮬레이션하여 today 값 확인 (수정 전 실패)
4. **댓글 엔드포인트 존재 테스트**: `GET /comments/1/edit`, `POST /schedules/1/comments` 접근 시 200 응답 확인 (제거 후 404)

**Expected Counterexamples**:
- PUT/DELETE 요청이 `@PostMapping`으로 매핑되지 않아 405 Method Not Allowed 발생
- `LocalDate.now()`가 UTC 기준으로 전날 날짜를 반환하여 모델의 `today` 값이 부정확

### Fix Checking

**Goal**: 버그 조건이 성립하는 모든 입력에 대해 수정된 함수가 기대 동작을 만족하는지 검증합니다.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := handleRequest_fixed(input)
  ASSERT expectedBehavior(result)
END FOR
```

**구체적 검증:**
```
// PUT 라우팅 검증
FOR ALL scheduleId, validForm WHERE _method=PUT DO
  response := POST /schedules/{scheduleId} with _method=PUT and validForm
  ASSERT response.status == 302
  ASSERT response.redirectedUrl == "/schedules/{scheduleId}"
END FOR

// DELETE 라우팅 검증
FOR ALL scheduleId WHERE _method=DELETE DO
  response := POST /schedules/{scheduleId} with _method=DELETE
  ASSERT response.status == 302
  ASSERT response.redirectedUrl == "/"
END FOR

// 타임존 검증
FOR ALL timezone, koreanTime WHERE timezone != "Asia/Seoul" DO
  today := renderCalendar_fixed(timezone)
  ASSERT today == LocalDate.now(ZoneId.of("Asia/Seoul"))
END FOR
```

### Preservation Checking

**Goal**: 버그 조건에 해당하지 않는 모든 입력에 대해 수정 전후 동작이 동일한지 검증합니다.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT handleRequest_original(input) = handleRequest_fixed(input)
END FOR
```

**Testing Approach**: Property-based testing (jqwik)을 사용하여 보존 검증을 수행합니다:
- 다양한 입력을 자동 생성하여 넓은 범위 커버
- 엣지 케이스를 자동으로 발견
- 수정 전후 동작 동일성을 강하게 보장

**Test Plan**: 수정 전 코드에서 정상 동작하는 요청들(일정 생성, 캘린더 조회, 일정 상세 조회)의 동작을 관찰하고, 수정 후에도 동일한 결과를 반환하는지 property-based test로 검증합니다.

**Test Cases**:
1. **일정 생성 보존**: 랜덤 ScheduleCreateCommand로 POST /schedules 요청 시 수정 전후 동일한 응답
2. **캘린더 네비게이션 보존**: 랜덤 year/month 조합으로 GET /calendar/{year}/{month} 요청 시 동일한 모델 데이터
3. **일정 상세 조회 보존**: 존재하는 일정 ID로 GET /schedules/{id} 요청 시 동일한 일정 데이터 (댓글 제외)
4. **스와이프 네비게이션 보존**: prev/next URL 계산 로직이 동일한 결과 생성

### Unit Tests

- `CalendarController`: `LocalDate.now(ZoneId.of("Asia/Seoul"))` 사용 확인 (타임존 주입 또는 Clock 활용)
- `ScheduleController`: PUT/DELETE 매핑이 HiddenHttpMethodFilter 활성화 후 정상 동작 확인
- `ScheduleService`: 댓글 매핑 제거 후 `toResponse()`가 정상 동작 확인
- `Schedule` 엔티티: comments 관계 제거 후 기본 CRUD 동작 확인

### Property-Based Tests

- 랜덤 `ZoneId`와 시각 조합을 생성하여 `LocalDate.now(ZoneId.of("Asia/Seoul"))` 결과가 항상 한국 시간 기준인지 검증
- 랜덤 `ScheduleCreateCommand`를 생성하여 일정 생성 후 조회 시 데이터 일관성 검증
- 랜덤 year/month 조합으로 캘린더 렌더링 시 prev/next 월 계산 정확성 검증

### Integration Tests

- 전체 플로우: 일정 생성 → 캘린더 확인 → 일정 수정 → 일정 삭제 사이클 테스트
- 모달 등록 플로우: 캘린더 날짜 셀 클릭 → 모달 표시 → 폼 제출 → 캘린더 반영
- HiddenHttpMethodFilter 통합: 실제 서블릿 컨테이너에서 `_method` 필터 동작 확인
