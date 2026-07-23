# Requirements Document

## Introduction

커플 일정 관리 웹 애플리케이션(mycalendar)의 요구사항을 정의합니다. 이 앱은 두 사람(승권, 치원)이 독점적으로 사용하는 모바일 우선 캘린더로, 일정 CRUD, 댓글, D-Day/기념일 카운터 기능을 제공합니다. 인증 없이 페이지 로드 시 바로 캘린더에 접근합니다.

## Glossary

- **Calendar_View**: 월별 일정을 표시하는 메인 화면 컴포넌트
- **Schedule**: 캘린더에 등록되는 개별 일정 항목 (schedule 테이블에 저장)
- **Schedule_Comment**: 일정에 작성되는 짧은 댓글 (schedule_comment 테이블에 저장)
- **Category**: 일정의 소유 구분 (SEUNGKWON, CHIWON, DATE 중 하나)
- **Author**: 댓글 작성자 구분 (SEUNGKWON, CHIWON 중 하나)
- **Anniversary_Calculator**: 기념일 및 D-Day를 런타임에 계산하는 컴포넌트
- **Base_Date**: 사귄 날 기준일 (2026-06-17, 상수로 관리)
- **Multi_Day_Schedule**: end_date가 null이 아닌 일정으로, 여러 날에 걸쳐 표시되는 일정
- **Single_Day_Schedule**: end_date가 null인 일정으로, start_date 하루에만 표시되는 일정

## Requirements

### Requirement 1: 월별 캘린더 뷰 표시

**User Story:** As a 사용자, I want 월별 캘린더 형태로 일정을 확인하고 싶다, so that 한 달의 일정을 한눈에 파악할 수 있다.

#### Acceptance Criteria

1. WHEN 사용자가 페이지에 접근하면, THE Calendar_View SHALL 현재 연도와 월 제목, 요일 헤더(일~토), 그리고 해당 월의 모든 날짜를 포함하는 격자형 캘린더를 표시한다
2. WHEN 사용자가 이전 월 버튼을 누르면, THE Calendar_View SHALL 이전 월의 캘린더로 전환한다
3. WHEN 사용자가 다음 월 버튼을 누르면, THE Calendar_View SHALL 다음 월의 캘린더로 전환한다
4. THE Calendar_View SHALL 각 날짜 셀에 해당 날짜의 일정을 Category별 색상으로 구분하여 최대 3건까지 표시하고, 3건을 초과하는 경우 남은 일정 수를 나타내는 텍스트(예: "+N")를 표시한다
5. THE Calendar_View SHALL 모바일 화면(뷰포트 너비 768px 이하)에서 최소 44x44px 터치 영역을 가진 버튼과 날짜 셀을 제공하고, 단일 컬럼 또는 축약된 격자 레이아웃으로 전환한다
6. THE Calendar_View SHALL SEUNGKWON Category를 고유 색상으로, CHIWON Category를 별도의 고유 색상으로, DATE Category를 또 다른 고유 색상으로 표시한다
7. THE Calendar_View SHALL 오늘 날짜 셀을 다른 날짜 셀과 시각적으로 구분되는 강조 표시로 나타낸다

### Requirement 2: 일정 생성

**User Story:** As a 사용자, I want 새로운 일정을 등록하고 싶다, so that 개인 또는 데이트 일정을 캘린더에 기록할 수 있다.

#### Acceptance Criteria

1. WHEN 사용자가 일정 생성을 요청하면, THE Schedule SHALL category, start_date, content를 필수 입력 필드로, schedule_time과 end_date를 선택 입력 필드로 제공한다
2. WHEN 모든 필수 필드가 입력되고 각 필드의 유효성 검증을 통과하면, THE Schedule SHALL 새 일정을 저장하고 캘린더 해당 날짜에 일정을 표시한다
3. WHERE end_date가 입력된 경우, THE Schedule SHALL start_date부터 end_date까지의 Multi_Day_Schedule로 저장한다
4. WHERE end_date가 입력되지 않은 경우, THE Schedule SHALL end_date를 null로 저장하여 Single_Day_Schedule로 처리한다
5. IF content가 200자를 초과하면, THEN THE Schedule SHALL 저장을 거부하고 글자 수 초과 오류 메시지를 표시한다
6. IF 필수 필드(category, start_date, content) 중 하나라도 입력되지 않으면, THEN THE Schedule SHALL 저장을 거부하고 해당 필드의 필수 항목 누락 오류 메시지를 표시한다
7. IF end_date가 start_date보다 이전이면, THEN THE Schedule SHALL 저장을 거부하고 날짜 범위 오류 메시지를 표시한다
8. IF category 값이 허용된 값(SEUNGKWON, CHIWON, DATE) 이외의 값이면, THEN THE Schedule SHALL 저장을 거부하고 유효하지 않은 카테고리 오류 메시지를 표시한다
9. IF content가 공백 문자만으로 구성되어 있으면, THEN THE Schedule SHALL 저장을 거부하고 내용 입력 필요 오류 메시지를 표시한다

### Requirement 3: 일정 조회

**User Story:** As a 사용자, I want 등록된 일정의 상세 정보를 확인하고 싶다, so that 일정의 세부 내용을 파악할 수 있다.

#### Acceptance Criteria

1. WHEN 사용자가 캘린더에서 특정 일정을 선택하면, THE Schedule SHALL 해당 일정의 category, start_date, schedule_time, content를 상세 화면에 표시하고, end_date가 존재하는 경우 end_date를 추가로 표시한다
2. WHEN 사용자가 특정 월의 캘린더를 조회하면, THE Calendar_View SHALL 해당 월에 포함되는 모든 일정을 category별 색상으로 구분하여 표시한다
3. IF Multi_Day_Schedule의 날짜 범위(start_date ~ end_date)가 조회 대상 월의 범위와 겹치면, THEN THE Calendar_View SHALL 해당 일정을 조회 대상 월 내에서 겹치는 모든 날짜에 걸쳐 연속적으로 표시한다
4. IF 조회 대상 월에 등록된 일정이 없으면, THEN THE Calendar_View SHALL 일정이 없는 빈 캘린더를 표시한다

### Requirement 4: 일정 수정

**User Story:** As a 사용자, I want 기존 일정을 수정하고 싶다, so that 변경된 계획을 캘린더에 반영할 수 있다.

#### Acceptance Criteria

1. WHEN 사용자가 기존 일정의 수정을 요청하면, THE Schedule SHALL 해당 일정의 category, start_date, end_date, schedule_time, content 값을 수정 폼에 표시한다
2. WHEN 사용자가 유효한 수정 데이터를 제출하면, THE Schedule SHALL 변경 내용을 저장하고 updated_at을 현재 시각으로 갱신한다
3. IF 수정된 content가 200자를 초과하면, THEN THE Schedule SHALL 저장을 거부하고 글자 수 초과 오류 메시지를 표시한다
4. IF end_date가 null이 아니며 수정된 end_date가 start_date보다 이전이면, THEN THE Schedule SHALL 저장을 거부하고 날짜 범위 오류 메시지를 표시한다
5. IF 수정 요청한 일정 ID에 해당하는 일정이 존재하지 않으면, THEN THE Schedule SHALL 수정을 거부하고 일정을 찾을 수 없다는 오류 메시지를 표시한다
6. IF 수정된 category가 허용된 ENUM 값(SEUNGKWON, CHIWON, DATE) 중 하나가 아니거나, content 또는 start_date가 비어 있으면, THEN THE Schedule SHALL 저장을 거부하고 입력값 오류 메시지를 표시한다
7. IF 수정 데이터 저장 중 오류가 발생하면, THEN THE Schedule SHALL 변경 내용을 저장하지 않고 저장 실패 오류 메시지를 표시한다

### Requirement 5: 일정 삭제

**User Story:** As a 사용자, I want 불필요한 일정을 삭제하고 싶다, so that 캘린더를 정리할 수 있다.

#### Acceptance Criteria

1. WHEN 사용자가 일정 삭제를 요청하면, THE Schedule SHALL 삭제 확인 메시지를 표시한다
2. WHEN 사용자가 삭제를 확인하면, THE Schedule SHALL 해당 일정을 데이터베이스에서 영구 삭제하고, 연관된 모든 Schedule_Comment를 함께 영구 삭제한 뒤, Calendar_View에서 해당 일정을 제거한다
3. IF 삭제 대상 일정이 존재하지 않으면, THEN THE Schedule SHALL 일정 없음 오류 메시지를 표시한다
4. IF 사용자가 삭제 확인 메시지에서 취소를 선택하면, THEN THE Schedule SHALL 삭제를 수행하지 않고 일정을 현재 상태 그대로 유지한다

### Requirement 6: 일정 댓글 작성

**User Story:** As a 사용자, I want 일정에 짧은 댓글을 남기고 싶다, so that 상대방에게 의견이나 메모를 전달할 수 있다.

#### Acceptance Criteria

1. WHEN 사용자가 일정 상세 화면에서 댓글을 작성하면, THE Schedule_Comment SHALL author(SEUNGKWON 또는 CHIWON 중 선택), content(1자 이상 200자 이하)를 필수로 입력받는다
2. WHEN 사용자가 유효한 댓글 데이터를 제출하면, THE Schedule_Comment SHALL 해당 일정에 댓글을 저장하고 댓글 목록에 즉시 반영한다
3. WHEN 사용자가 일정 상세 화면을 조회하면, THE Schedule_Comment SHALL 해당 일정의 모든 댓글을 작성 시간 오름차순으로 표시하되, 각 댓글은 author, content, created_at을 포함한다
4. IF comment content가 200자를 초과하면, THEN THE Schedule_Comment SHALL 저장을 거부하고 글자 수 초과 오류 메시지를 표시한다
5. IF author가 선택되지 않으면, THEN THE Schedule_Comment SHALL 저장을 거부하고 작성자 선택 필수 오류 메시지를 표시한다
6. IF comment content가 빈 값이거나 공백만으로 구성되면, THEN THE Schedule_Comment SHALL 저장을 거부하고 내용 입력 필수 오류 메시지를 표시한다
7. IF 댓글 대상 일정이 존재하지 않으면, THEN THE Schedule_Comment SHALL 저장을 거부하고 일정 없음 오류 메시지를 표시한다

### Requirement 7: 일정 댓글 수정 및 삭제

**User Story:** As a 사용자, I want 작성한 댓글을 수정하거나 삭제하고 싶다, so that 잘못 작성한 댓글을 정정하거나 불필요한 댓글을 제거할 수 있다.

#### Acceptance Criteria

1. WHEN 사용자가 댓글 수정을 요청하면, THE Schedule_Comment SHALL 해당 댓글의 현재 content를 수정 입력 필드에 표시한다
2. WHEN 사용자가 유효한 수정 내용을 제출하면, THE Schedule_Comment SHALL 변경된 content를 저장하고 댓글 목록에 즉시 반영한다
3. WHEN 사용자가 댓글 삭제를 요청하면, THE Schedule_Comment SHALL 삭제 확인 메시지를 표시한다
4. WHEN 사용자가 삭제를 확인하면, THE Schedule_Comment SHALL 해당 댓글을 데이터베이스에서 영구 삭제하고 댓글 목록에서 제거한다
5. IF 수정된 comment content가 200자를 초과하면, THEN THE Schedule_Comment SHALL 저장을 거부하고 글자 수 초과 오류 메시지를 표시한다
6. IF 수정된 comment content가 빈 값이거나 공백만으로 구성되면, THEN THE Schedule_Comment SHALL 저장을 거부하고 내용 입력 필수 오류 메시지를 표시한다
7. IF 수정 또는 삭제 대상 댓글이 존재하지 않으면, THEN THE Schedule_Comment SHALL 댓글 없음 오류 메시지를 표시한다

### Requirement 8: 기념일 및 D-Day 표시

**User Story:** As a 사용자, I want 사귄 날 기준 기념일과 D-Day를 확인하고 싶다, so that 중요한 기념일을 놓치지 않을 수 있다.

#### Acceptance Criteria

1. THE Anniversary_Calculator SHALL Base_Date(2026-06-17)를 기준으로 100일, 200일, 300일, ..., 1000일과 1주년, 2주년, ..., 10주년 기념일 날짜를 런타임에 계산한다
2. THE Anniversary_Calculator SHALL 한국식 D-Day 계산 방식(Base_Date를 1일로 산정)을 적용하여 기념일을 계산한다
3. WHEN 캘린더에 기념일이 포함된 월이 표시되면, THE Calendar_View SHALL 해당 기념일을 캘린더 날짜 셀에 기념일 이름(예: "100일", "1주년")을 포함하는 마커로 표시한다
4. THE Calendar_View SHALL 현재 날짜 기준으로 사귄 날로부터 경과한 일수를 "D+N" 형식(예: "D+37")의 D-Day 카운터로 표시한다
5. THE Anniversary_Calculator SHALL 100일 단위(100일, 200일, 300일, ..., 1000일) 기념일을 계산한다
6. THE Anniversary_Calculator SHALL 연 단위(1주년, 2주년, 3주년, ..., 10주년) 기념일을 계산한다
7. THE Anniversary_Calculator SHALL 기념일 계산 시 데이터베이스 조회 없이 Base_Date 상수만 사용한다

### Requirement 9: 반응형 모바일 우선 UI

**User Story:** As a 사용자, I want 모바일에서 편하게 사용할 수 있는 캘린더를 원한다, so that 스마트폰으로 일정을 빠르게 확인하고 관리할 수 있다.

#### Acceptance Criteria

1. THE Calendar_View SHALL 모바일 화면(뷰포트 너비 768px 이하)을 기본 디자인 대상으로 레이아웃을 구성한다
2. WHILE 뷰포트 너비가 769px 이상이면, THE Calendar_View SHALL 캘린더 그리드의 각 날짜 셀을 확대하고 일정 목록을 셀 내에 최대 5개까지 텍스트로 표시하는 확장된 레이아웃을 제공한다
3. THE Calendar_View SHALL 모든 터치 가능한 요소(버튼, 날짜 셀, 링크)의 최소 크기를 44×44px로 설정하고, 인접한 터치 요소 간 최소 간격을 8px로 유지한다
4. WHILE 뷰포트 너비가 768px 이하이면, THE Schedule SHALL 일정 생성 및 수정 폼의 입력 필드를 뷰포트 너비의 100%(좌우 패딩 16px 제외)로 표시하고, 입력 필드 높이를 최소 44px로 설정한다
5. WHEN 사용자가 모바일 화면에서 캘린더를 좌로 스와이프하면, THE Calendar_View SHALL 다음 월의 캘린더로 전환한다
6. WHEN 사용자가 모바일 화면에서 캘린더를 우로 스와이프하면, THE Calendar_View SHALL 이전 월의 캘린더로 전환한다

### Requirement 10: 인증 없는 직접 접근

**User Story:** As a 사용자, I want 로그인 없이 바로 캘린더를 사용하고 싶다, so that 별도의 인증 절차 없이 빠르게 접근할 수 있다.

#### Acceptance Criteria

1. WHEN 사용자가 애플리케이션의 임의의 URL에 접근하면, THE Calendar_View SHALL 로그인 페이지나 인증 확인 화면으로 리다이렉트하지 않고 요청된 화면을 즉시 표시한다
2. THE Calendar_View SHALL 로그인 폼, 회원가입 폼, 세션 관리, 토큰 기반 인증 등 어떠한 사용자 인증 메커니즘도 포함하지 않는다
3. THE Calendar_View SHALL 모든 페이지 및 API 요청에 대해 HTTP 401(Unauthorized) 또는 403(Forbidden) 응답을 반환하지 않는다
