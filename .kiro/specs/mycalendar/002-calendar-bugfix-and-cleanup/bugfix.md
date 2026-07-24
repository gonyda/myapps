# Bugfix Requirements Document

## Introduction

mycalendar 모듈에서 발견된 복합 이슈를 수정합니다. 핵심 버그 3건(일정 수정 불가, 일정 삭제 불가, 당일 날짜 표시 오류)과 기능 변경 2건(일정 등록 UX 팝업 변경, 댓글 기능 삭제)을 포함합니다.

**원인 요약:**
- 일정 수정/삭제: Spring Boot 4.0에서 `HiddenHttpMethodFilter`가 기본 비활성화되어 HTML 폼의 `_method=PUT/DELETE` 요청이 POST로만 처리됨
- 날짜 표시 오류: 프로덕션 서버의 기본 타임존이 UTC인 경우 `LocalDate.now()`가 한국 시간 대비 -9시간으로 계산되어 자정~오전 9시 사이에 전날 날짜가 today로 표시됨
- 기능 변경: 일정추가 버튼 제거 후 캘린더 데이 셀 클릭으로 팝업 등록 전환, 댓글 기능 전체 삭제

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN 사용자가 일정 수정 폼에서 수정 버튼을 클릭하여 PUT 요청을 전송하면 THEN HiddenHttpMethodFilter가 비활성화되어 있어 `_method=PUT`이 무시되고 POST /schedules/{id}로 처리되어 405 Method Not Allowed 또는 매핑 실패가 발생한다

1.2 WHEN 사용자가 일정 상세 페이지에서 삭제 버튼을 클릭하여 DELETE 요청을 전송하면 THEN HiddenHttpMethodFilter가 비활성화되어 있어 `_method=DELETE`가 무시되고 POST /schedules/{id}로 처리되어 405 Method Not Allowed 또는 매핑 실패가 발생한다

1.3 WHEN 서버 타임존이 UTC이고 한국 시간으로 당일(0시~9시 사이)에 일정을 등록한 후 캘린더를 조회하면 THEN `LocalDate.now()`가 UTC 기준으로 전날 날짜를 반환하여 오늘 날짜 하이라이트(`.today` 클래스)가 전날에 표시된다

1.4 WHEN 사용자가 캘린더에서 일정을 추가하려면 THEN 별도의 "일정 추가" 버튼을 클릭하여 전체 페이지 이동이 발생한다

1.5 WHEN 일정 상세 페이지에 접근하면 THEN 댓글 작성/조회/수정/삭제 기능이 표시되지만, 이 기능은 더 이상 필요하지 않다

### Expected Behavior (Correct)

2.1 WHEN 사용자가 일정 수정 폼에서 수정 버튼을 클릭하면 THEN 시스템은 `HiddenHttpMethodFilter`를 통해 `_method=PUT`을 인식하여 PUT /schedules/{id} 엔드포인트로 정상 라우팅하고 일정을 수정한 뒤 상세 페이지로 리다이렉트 SHALL 한다

2.2 WHEN 사용자가 일정 상세 페이지에서 삭제 버튼을 클릭하면 THEN 시스템은 `HiddenHttpMethodFilter`를 통해 `_method=DELETE`를 인식하여 DELETE /schedules/{id} 엔드포인트로 정상 라우팅하고 일정을 삭제한 뒤 캘린더로 리다이렉트 SHALL 한다

2.3 WHEN 캘린더를 조회하면 THEN 시스템은 `Asia/Seoul` 타임존을 명시적으로 사용하여 `LocalDate.now(ZoneId.of("Asia/Seoul"))`로 오늘 날짜를 계산하고, 한국 시간 기준 정확한 날짜에 `.today` 하이라이트를 표시 SHALL 한다

2.4 WHEN 사용자가 캘린더의 날짜 셀을 클릭하면 THEN 시스템은 해당 날짜가 시작일로 미리 설정된 일정 등록 폼을 팝업(모달)으로 표시 SHALL 한다

2.5 WHEN 일정 상세 페이지에 접근하면 THEN 시스템은 댓글 관련 UI(댓글 목록, 댓글 작성 폼, 수정/삭제 버튼)를 표시하지 않 SHALL 한다

### Unchanged Behavior (Regression Prevention)

3.1 WHEN 사용자가 일정 생성 폼에서 등록 버튼을 클릭(POST /schedules)하면 THEN 시스템은 기존과 동일하게 일정을 생성하고 캘린더로 리다이렉트 SHALL CONTINUE TO 한다

3.2 WHEN 캘린더 페이지에서 이전/다음 월 네비게이션을 사용하면 THEN 시스템은 기존과 동일하게 해당 월 캘린더를 정상 렌더링 SHALL CONTINUE TO 한다

3.3 WHEN 일정 상세 페이지에서 일정 정보(카테고리, 시작일, 종료일, 시간, 내용)를 조회하면 THEN 시스템은 기존과 동일하게 정확한 일정 데이터를 표시 SHALL CONTINUE TO 한다

3.4 WHEN 서버 타임존이 Asia/Seoul이고 오전 9시 이후에 캘린더를 조회하면 THEN 시스템은 기존과 동일하게 정확한 오늘 날짜에 하이라이트를 표시 SHALL CONTINUE TO 한다

3.5 WHEN 캘린더에서 일정이 있는 날짜를 표시할 때 THEN 시스템은 기존과 동일하게 해당 날짜의 일정 목록(최대 5개)을 셀 내에 표시 SHALL CONTINUE TO 한다

3.6 WHEN 스와이프 제스처로 월을 이동하면 THEN 시스템은 기존과 동일하게 이전/다음 월 캘린더로 네비게이션 SHALL CONTINUE TO 한다
