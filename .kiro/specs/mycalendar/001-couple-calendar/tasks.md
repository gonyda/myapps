# Implementation Plan: Couple Calendar

## Overview

커플 일정 관리 웹 애플리케이션(mycalendar)의 구현 계획입니다. Spring Boot 4.0 + Thymeleaf SSR + HTMX 기반의 모바일 우선 캘린더로, 일정 CRUD, 댓글, D-Day/기념일 카운터 기능을 인증 없이 제공합니다. DDD 계층 구조(domain → application → infrastructure → interfaces)를 따르며, 도메인 계층부터 점진적으로 구현합니다.

## Tasks

- [x] 1. 프로젝트 구조 및 모듈 설정
  - [x] 1.1 mycalendar Maven 모듈 생성 및 POM 설정
    - Parent POM에 mycalendar 모듈 추가
    - mycalendar/pom.xml 생성 (spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-thymeleaf, H2, jqwik, spring-boot-starter-webmvc-test 의존성)
    - MycalendarApplication.java 메인 클래스 생성
    - application.yml 설정 (H2 인메모리 DB, JPA DDL-Auto, Thymeleaf 설정)
    - _Requirements: 10.1, 10.2, 10.3_

  - [x] 1.2 DDD 패키지 구조 생성
    - domain/model, domain/repository, domain/service 패키지 생성
    - application/service, application/dto 패키지 생성
    - infrastructure/persistence, infrastructure/config 패키지 생성
    - interfaces/api, interfaces/dto 패키지 생성
    - _Requirements: 전체 아키텍처 기반_

- [x] 2. 도메인 계층 구현
  - [x] 2.1 Enum 및 값 객체 구현
    - Category enum (SEUNGKWON, CHIWON, DATE) 구현
    - Author enum (SEUNGKWON, CHIWON) 구현
    - Anniversary record (date, name) 구현
    - _Requirements: 1.6, 2.1, 6.1_

  - [x] 2.2 Schedule JPA Entity 구현
    - id, category, startDate, endDate, scheduleTime, content, createdAt, updatedAt 필드
    - @OneToMany 댓글 연관관계 (CascadeType.ALL, orphanRemoval)
    - @PrePersist, @PreUpdate로 시간 자동 설정
    - content 최대 200자 제약
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 5.2_

  - [x] 2.3 ScheduleComment JPA Entity 구현
    - id, schedule(ManyToOne LAZY), author, content, createdAt 필드
    - @PrePersist로 createdAt 자동 설정
    - content 최대 200자 제약
    - _Requirements: 6.1, 6.2, 6.3_

  - [x] 2.4 AnniversaryCalculator 도메인 서비스 구현
    - BASE_DATE 상수 (2026-06-17) 정의
    - calculateDDay(LocalDate): 한국식 D-Day 계산 (Base_Date를 1일로 산정)
    - calculateHundredDayAnniversaries(): 100일~1000일 기념일 목록
    - calculateYearlyAnniversaries(): 1주년~10주년 기념일 목록
    - getAnniversariesForMonth(YearMonth): 특정 월의 기념일 필터링
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_

  - [x] 2.5 AnniversaryCalculator property 테스트 작성
    - **Property 9: Anniversary date calculation**
    - **Property 10: Korean D-Day calculation**
    - **Property 11: D-Day display format**
    - **Property 12: Anniversary month filtering**
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 8.6**

- [x] 3. Repository 및 Infrastructure 계층 구현
  - [x] 3.1 ScheduleRepository 인터페이스 및 JPA 구현
    - JpaRepository<Schedule, Long> 상속
    - findByMonth 커스텀 쿼리 (Multi_Day_Schedule 포함 월별 조회)
    - _Requirements: 3.2, 3.3_

  - [x] 3.2 CommentRepository 인터페이스 및 JPA 구현
    - JpaRepository<ScheduleComment, Long> 상속
    - findByScheduleIdOrderByCreatedAtAsc 메서드
    - _Requirements: 6.3_

  - [x] 3.3 Repository 통합 테스트 작성 (ScheduleRepositoryTest)
    - @DataJpaTest 슬라이스 테스트
    - 월별 조회 쿼리 검증 (Single_Day, Multi_Day_Schedule 포함)
    - cascade 삭제 검증
    - _Requirements: 3.2, 3.3, 5.2_

- [x] 4. Checkpoint - 도메인/인프라 계층 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Application 계층 — ScheduleService 구현
  - [x] 5.1 DTO records 구현
    - ScheduleCreateCommand, ScheduleUpdateCommand record 생성
    - ScheduleResponse record 생성 (CommentResponse 포함)
    - CommentCreateCommand, CommentUpdateCommand, CommentResponse record 생성
    - _Requirements: 2.1, 3.1, 4.1, 6.1_

  - [x] 5.2 커스텀 예외 클래스 구현
    - ScheduleNotFoundException (404)
    - CommentNotFoundException (404)
    - InvalidScheduleException (400)
    - InvalidCommentException (400)
    - _Requirements: 2.5, 2.6, 2.7, 2.8, 2.9, 4.3, 4.4, 4.5, 4.6, 5.3, 6.4, 6.5, 6.6, 6.7, 7.5, 7.6, 7.7_

  - [x] 5.3 ScheduleService 구현
    - create(ScheduleCreateCommand): 유효성 검증 후 일정 저장
    - findById(Long): ID로 일정 조회 (ScheduleResponse 반환)
    - findByMonth(YearMonth): 월별 일정 조회
    - update(Long, ScheduleUpdateCommand): 유효성 검증 후 수정, updatedAt 갱신
    - delete(Long): 일정 및 연관 댓글 cascade 삭제
    - 유효성 검증: content 200자, 필수 필드, 날짜 범위, category ENUM, 공백 content
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 3.1, 3.2, 3.3, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 5.2, 5.3_

  - [x] 5.4 ScheduleService property 테스트 작성
    - **Property 1: Schedule creation round-trip**
    - **Property 2: Content length validation (일정)**
    - **Property 3: Required field validation**
    - **Property 4: Date range validation**
    - **Property 5: Whitespace content rejection (일정)**
    - **Property 6: Monthly schedule query overlap**
    - **Property 7: Schedule cascade delete**
    - **Property 13: Schedule update timestamp refresh**
    - **Validates: Requirements 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.9, 3.2, 3.3, 4.2, 4.3, 4.4, 5.2**

  - [x] 5.5 ScheduleService 단위 테스트 작성
    - 일정 CRUD 정상 동작 테스트
    - 삭제 확인 테스트
    - 존재하지 않는 ID 처리 테스트
    - _Requirements: 2.2, 3.1, 4.2, 4.5, 5.2, 5.3_

- [x] 6. Application 계층 — CommentService 구현
  - [x] 6.1 CommentService 구현
    - create(Long scheduleId, CommentCreateCommand): 유효성 검증 후 댓글 저장
    - findByScheduleId(Long): 일정의 모든 댓글 시간순 조회
    - update(Long commentId, CommentUpdateCommand): 유효성 검증 후 수정
    - delete(Long commentId): 댓글 삭제
    - 유효성 검증: author 필수, content 200자, 공백 content, 대상 일정/댓글 존재 확인
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7_

  - [x] 6.2 CommentService property 테스트 작성
    - **Property 2: Content length validation (댓글)**
    - **Property 5: Whitespace content rejection (댓글)**
    - **Property 8: Comment ordering**
    - **Validates: Requirements 6.3, 6.4, 6.6, 7.5, 7.6**

  - [x] 6.3 CommentService 단위 테스트 작성
    - 댓글 CRUD 정상 동작 테스트
    - 존재하지 않는 일정/댓글 처리 테스트
    - _Requirements: 6.2, 6.7, 7.2, 7.4, 7.7_

- [x] 7. Checkpoint - Application 계층 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Interfaces 계층 — Controller 구현
  - [x] 8.1 GlobalExceptionHandler 구현
    - @ControllerAdvice 전역 예외 처리
    - ScheduleNotFoundException → 404 에러 페이지
    - CommentNotFoundException → 404 에러 페이지
    - InvalidScheduleException → 400 에러 메시지 표시
    - InvalidCommentException → 400 에러 메시지 표시
    - _Requirements: 2.5, 2.6, 2.7, 2.8, 2.9, 4.3, 4.4, 4.5, 4.6, 5.3, 6.4, 6.5, 6.6, 6.7, 7.5, 7.6, 7.7_

  - [x] 8.2 ScheduleForm, CommentForm DTO 구현
    - ScheduleForm record (폼 바인딩용)
    - CommentForm record (폼 바인딩용)
    - _Requirements: 2.1, 6.1_

  - [x] 8.3 CalendarController 구현
    - GET / → 현재 월 캘린더로 리다이렉트
    - GET /calendar/{year}/{month} → 월별 캘린더 뷰 렌더링
    - Model에 일정 데이터 + 기념일 데이터 + D-Day 카운터 전달
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.6, 1.7, 3.2, 3.3, 3.4, 8.3, 8.4_

  - [x] 8.4 ScheduleController 구현
    - GET /schedules/{id} → 일정 상세 조회 (HTMX 부분 렌더링)
    - GET /schedules/new → 일정 생성 폼
    - POST /schedules → 일정 생성 처리
    - GET /schedules/{id}/edit → 일정 수정 폼
    - PUT /schedules/{id} → 일정 수정 처리
    - DELETE /schedules/{id} → 일정 삭제 처리
    - _Requirements: 2.1, 2.2, 3.1, 4.1, 4.2, 5.1, 5.2, 5.4_

  - [x] 8.5 CommentController 구현
    - POST /schedules/{scheduleId}/comments → 댓글 생성
    - GET /comments/{id}/edit → 댓글 수정 폼
    - PUT /comments/{id} → 댓글 수정 처리
    - DELETE /comments/{id} → 댓글 삭제
    - _Requirements: 6.1, 6.2, 7.1, 7.2, 7.3, 7.4_

  - [x] 8.6 CalendarController 슬라이스 테스트 작성
    - @WebMvcTest 슬라이스 테스트
    - 월별 뷰 모델 데이터 검증
    - 월 이동 리다이렉트 검증
    - D-Day 카운터 모델 데이터 검증
    - **Property 14: Calendar day cell display limit**
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 8.3, 8.4**

  - [x] 8.7 ScheduleController 슬라이스 테스트 작성
    - @WebMvcTest 슬라이스 테스트
    - 일정 생성/수정/삭제 HTTP 요청 처리 검증
    - 유효성 검증 실패 시 에러 응답 검증
    - _Requirements: 2.2, 3.1, 4.2, 5.2_

  - [x] 8.8 CommentController 슬라이스 테스트 작성
    - @WebMvcTest 슬라이스 테스트
    - 댓글 생성/수정/삭제 HTTP 요청 처리 검증
    - _Requirements: 6.2, 7.2, 7.4_

- [x] 9. Checkpoint - Interfaces 계층 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Thymeleaf 템플릿 및 프론트엔드 구현
  - [x] 10.1 캘린더 월별 뷰 템플릿 구현
    - calendar.html: 월별 격자형 캘린더 레이아웃
    - 연도/월 제목, 요일 헤더(일~토), 날짜 셀 표시
    - 날짜 셀에 Category별 색상 구분 일정 표시 (최대 3건 + "+N" 초과 표시)
    - 오늘 날짜 강조 표시
    - 이전/다음 월 이동 버튼
    - D-Day 카운터 ("D+N" 형식) 표시
    - 기념일 마커 표시
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.6, 1.7, 8.3, 8.4_

  - [x] 10.2 일정 상세/생성/수정 폼 템플릿 구현
    - schedule-detail.html: 일정 상세 정보 + 댓글 목록 표시
    - schedule-form.html: 일정 생성/수정 폼 (category, start_date, end_date, schedule_time, content)
    - HTMX를 활용한 부분 렌더링 지원
    - _Requirements: 2.1, 3.1, 4.1, 5.1_

  - [x] 10.3 댓글 관련 템플릿 구현
    - comment-list.html (fragment): 댓글 목록 (author, content, created_at)
    - comment-form.html (fragment): 댓글 생성/수정 폼
    - HTMX를 활용한 댓글 추가/수정/삭제 실시간 반영
    - _Requirements: 6.1, 6.2, 6.3, 7.1, 7.3_

  - [x] 10.4 반응형 모바일 우선 CSS 및 스와이프 구현
    - 모바일 우선 레이아웃 (768px 이하 기본 디자인)
    - 데스크톱 확장 레이아웃 (769px 이상: 셀 확대, 최대 5건 표시)
    - 최소 터치 영역 44×44px, 인접 요소 간격 8px
    - 입력 필드 100% 너비(패딩 16px 제외), 높이 최소 44px
    - Hammer.js를 활용한 좌/우 스와이프 월 이동
    - Category별 색상 정의 (SEUNGKWON, CHIWON, DATE)
    - _Requirements: 1.5, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

  - [x] 10.5 에러 페이지 템플릿 구현
    - error.html: 에러 메시지 표시 (errorMessage 모델 속성 활용)
    - _Requirements: 2.5, 2.6, 2.7, 2.8, 2.9, 4.3, 4.4, 4.5, 5.3_

- [x] 11. 통합 및 최종 검증
  - [x] 11.1 HTMX 및 정적 리소스 설정
    - HTMX 라이브러리 CDN 또는 로컬 설정
    - Hammer.js CDN 또는 로컬 설정
    - 정적 리소스 경로 설정 (CSS, JS, favicon)
    - _Requirements: 9.5, 9.6_

  - [x] 11.2 MycalendarApplicationTest 통합 테스트 작성
    - @SpringBootTest 애플리케이션 컨텍스트 로드 검증
    - _Requirements: 10.1, 10.2, 10.3_

- [x] 12. Final checkpoint - 전체 빌드 및 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- 도메인 계층(Entity, Enum, 도메인 서비스)부터 구현하여 핵심 비즈니스 로직을 먼저 검증
- Spring Boot 4.0 테스트 변경사항 (@MockitoBean, @WebMvcTest 패키지) 준수 필수
- 모든 코드는 Java 25, final 파라미터, 명시적 타입 선언, JavaDoc 주석 규칙 준수

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["2.1"] },
    { "id": 3, "tasks": ["2.2", "2.3"] },
    { "id": 4, "tasks": ["2.4", "3.1", "3.2"] },
    { "id": 5, "tasks": ["2.5", "3.3"] },
    { "id": 6, "tasks": ["5.1", "5.2"] },
    { "id": 7, "tasks": ["5.3", "6.1"] },
    { "id": 8, "tasks": ["5.4", "5.5", "6.2", "6.3"] },
    { "id": 9, "tasks": ["8.1", "8.2"] },
    { "id": 10, "tasks": ["8.3", "8.4", "8.5"] },
    { "id": 11, "tasks": ["8.6", "8.7", "8.8"] },
    { "id": 12, "tasks": ["10.1", "10.5"] },
    { "id": 13, "tasks": ["10.2", "10.3", "10.4"] },
    { "id": 14, "tasks": ["11.1"] },
    { "id": 15, "tasks": ["11.2"] }
  ]
}
```
