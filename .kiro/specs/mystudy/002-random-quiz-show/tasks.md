# Implementation Plan: Random Quiz Show

## Overview

영어 학습 페이지(`/english-study`)에 랜덤 퀴즈쇼 기능을 구현합니다. 기존 `EnglishStudy` 테이블 데이터를 활용하여 서버 사이드에서 퀴즈를 생성하고, Modal UI로 10개의 객관식 퀴즈를 출제합니다. DDD 계층(domain/application/interfaces)을 따르며, jqwik 기반 property-based test로 퀴즈 생성 알고리즘의 정확성을 검증합니다.

## Tasks

- [x] 1. 프로젝트 의존성 설정 및 DTO 생성
  - [x] 1.1 jqwik 테스트 의존성 추가
    - Parent POM(`pom.xml`)의 `<dependencyManagement>`에 `net.jqwik:jqwik:1.9.4` 버전 선언
    - mystudy 모듈 `pom.xml`의 `<dependencies>`에 `net.jqwik:jqwik` test scope 의존성 추가 (버전 없이)
    - _Requirements: 5.1_

  - [x] 1.2 QuestionType enum 생성
    - `com.myapps.web.mystudy.domain.model.QuestionType` enum 생성
    - `ENGLISH_TO_KOREAN`, `KOREAN_TO_ENGLISH` 두 값 정의
    - JavaDoc 주석 작성
    - _Requirements: 2.4, 2.5, 2.6_

  - [x] 1.3 QuizQuestionDto record 생성
    - `com.myapps.web.mystudy.application.dto.QuizQuestionDto` record 생성
    - 필드: `String question`, `List<String> choices`, `int answerIndex`
    - JavaDoc 주석 작성
    - _Requirements: 5.3_

  - [x] 1.4 QuizResponse record 생성
    - `com.myapps.web.mystudy.application.dto.QuizResponse` record 생성
    - 필드: `List<QuizQuestionDto> questions`
    - JavaDoc 주석 작성
    - _Requirements: 5.3_

- [x] 2. QuizService 구현
  - [x] 2.1 QuizService 클래스 생성 및 generateQuiz() 구현
    - `com.myapps.web.mystudy.application.service.QuizService` 클래스 생성
    - 상수 정의: `QUIZ_QUESTION_COUNT=10`, `CHOICE_COUNT=4`, `MIN_SENTENCES_PER_EPISODE=2`
    - 생성자 주입으로 `EnglishStudyRepository` 의존성 주입
    - 테스트 용이성을 위해 `Random` 객체를 생성자에서 주입받을 수 있도록 설계 (기본 생성자에서는 `new Random()` 사용)
    - `generateQuiz()` 메서드 구현: 설계 문서의 알고리즘을 따라 에피소드 그룹핑 → 적격 에피소드 필터링 → 랜덤 문제 생성 → 보기 구성
    - JavaDoc 주석 작성 (클래스 및 메서드)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 5.4, 5.5_

  - [x] 2.2 QuizService 단위 테스트 작성
    - `QuizServiceTest` 클래스 생성 (`@ExtendWith(MockitoExtension.class)`)
    - 테스트 케이스: 빈 데이터셋 → 빈 응답 반환
    - 테스트 케이스: 모든 에피소드가 1개 문장만 존재 → 빈 응답 반환
    - 테스트 케이스: 정확히 2개 문장인 에피소드 → 2개 보기 생성 확인
    - 테스트 케이스: 10개 초과 적격 문장 → 정확히 10개 문제 생성 확인
    - _Requirements: 2.1, 2.3, 2.9, 3.3_

  - [x] 2.3 Property 1: Episode Eligibility 테스트 작성
    - **Property 1: Episode Eligibility**
    - **Validates: Requirements 2.1, 2.2**
    - jqwik `@Property(tries = 100)` 사용
    - `@Provide`로 다양한 에피소드/문장 수 조합의 `List<EnglishStudy>` Arbitrary 생성
    - 생성된 퀴즈의 모든 문제가 문장 2개 이상인 에피소드에서 출제되었는지 검증
    - Tag: `Feature: random-quiz-show, Property 1: Episode Eligibility`

  - [x] 2.4 Property 2: Question Count Invariant 테스트 작성
    - **Property 2: Question Count Invariant**
    - **Validates: Requirements 2.3**
    - 생성된 퀴즈 문제 수 == `min(10, totalEligibleSentences)` 검증
    - Tag: `Feature: random-quiz-show, Property 2: Question Count Invariant`

  - [x] 2.5 Property 3: Direction Mapping Correctness 테스트 작성
    - **Property 3: Direction Mapping Correctness**
    - **Validates: Requirements 2.4, 2.5, 2.6**
    - 문제 텍스트가 영어 문장이면 정답은 한국어 문장, 반대도 동일하게 검증
    - Tag: `Feature: random-quiz-show, Property 3: Direction Mapping Correctness`

  - [x] 2.6 Property 4: No Duplicate Questions 테스트 작성
    - **Property 4: No Duplicate Questions**
    - **Validates: Requirements 2.8**
    - 퀴즈 내 모든 문제가 서로 다른 소스 문장에서 출제되었는지 검증
    - Tag: `Feature: random-quiz-show, Property 4: No Duplicate Questions`

  - [x] 2.7 Property 5: Choices From Same Episode 테스트 작성
    - **Property 5: Choices From Same Episode**
    - **Validates: Requirements 3.1**
    - 모든 보기가 해당 문제의 에피소드 문장들에서만 구성되었는지 검증
    - Tag: `Feature: random-quiz-show, Property 5: Choices From Same Episode`

  - [x] 2.8 Property 6: Choice Count Invariant 테스트 작성
    - **Property 6: Choice Count Invariant**
    - **Validates: Requirements 3.2, 3.3**
    - 보기 수 == `min(4, 에피소드 문장 수)` 이고 최소 2개 이상 검증
    - Tag: `Feature: random-quiz-show, Property 6: Choice Count Invariant`

  - [x] 2.9 Property 7: Answer Inclusion 테스트 작성
    - **Property 7: Answer Inclusion**
    - **Validates: Requirements 3.4**
    - 정답이 choices 목록의 answerIndex 위치에 존재하며, answerIndex가 유효 범위 검증
    - Tag: `Feature: random-quiz-show, Property 7: Answer Inclusion`

  - [x] 2.10 Property 8: No Duplicate Choices 테스트 작성
    - **Property 8: No Duplicate Choices**
    - **Validates: Requirements 3.5**
    - 각 문제의 choices 목록에 중복 문자열이 없는지 검증
    - Tag: `Feature: random-quiz-show, Property 8: No Duplicate Choices`

- [x] 3. Checkpoint - 퀴즈 생성 로직 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Controller 엔드포인트 추가
  - [x] 4.1 EnglishStudyController에 퀴즈 API 엔드포인트 추가
    - `QuizService`를 생성자 주입으로 추가
    - `@GetMapping("/api/english-study/quiz")` 엔드포인트 구현
    - `@ResponseBody`로 `QuizResponse` JSON 반환
    - JavaDoc 주석 작성
    - _Requirements: 5.1, 5.2, 5.3, 5.5, 5.6_

  - [x] 4.2 EnglishStudyController 슬라이스 테스트 업데이트
    - 기존 `EnglishStudyControllerTest`에 퀴즈 API 테스트 추가
    - `@MockitoBean`으로 `QuizService` mock 등록
    - 테스트 케이스: GET `/api/english-study/quiz` 성공 → HTTP 200, JSON 구조 검증
    - 테스트 케이스: 빈 퀴즈 결과 → HTTP 200, 빈 questions 배열
    - 응답 JSON 필드 검증: question, choices, answerIndex 존재 확인
    - _Requirements: 5.1, 5.2, 5.3, 5.6_

- [x] 5. Checkpoint - API 엔드포인트 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. 프론트엔드 퀴즈 모달 구현
  - [x] 6.1 english_study.html에 퀴즈 모달 HTML/CSS 추가
    - 모달 오버레이 및 컨테이너 HTML 마크업 추가
    - 문제 번호 표시 영역 (`{현재번호}/{전체문제수}` 형식)
    - 문제 텍스트 표시 영역
    - 4개 보기 버튼 영역
    - 다음 문제 / 퀴즈 종료 버튼
    - 닫기(X) 버튼
    - 모달 CSS 스타일: 오버레이, 정답(초록)/오답(빨강) 배경색, 비활성화 스타일
    - 반응형 디자인 (모바일 대응)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 4.1, 4.2, 6.1, 6.2, 6.3, 6.4_

  - [x] 6.2 english_study.html에 퀴즈 JavaScript 로직 추가
    - `loadQuiz()`: 페이지 로드 시 `/api/english-study/quiz` fetch → 성공 시 모달 표시
    - `renderQuestion(index)`: 현재 문제 렌더링 (문제 텍스트, 보기 버튼, 진행 상태)
    - `selectChoice(choiceIndex)`: 보기 선택 시 정답/오답 피드백 표시, 클릭 비활성화
    - `nextQuestion()`: 다음 문제로 이동
    - `closeQuizModal()`: 모달 닫기 및 스크롤 복원
    - ESC 키 이벤트 리스너 등록 (모달 닫기)
    - 배경 클릭 시 모달 닫히지 않도록 이벤트 전파 차단
    - 빈 퀴즈 응답 또는 API 실패 시 모달 미표시
    - 마지막 문제에서는 다음 버튼 대신 퀴즈 종료 버튼 표시
    - 페이지 로드 시 `loadQuiz()` 자동 호출
    - _Requirements: 1.1, 1.3, 1.6, 1.7, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 6.1, 6.2, 6.3, 6.4_

- [x] 7. Final Checkpoint - 전체 빌드 및 테스트 검증
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document using jqwik
- Unit tests validate specific examples and edge cases
- 프론트엔드는 Thymeleaf 템플릿 내 vanilla JavaScript로 구현 (별도 JS 테스트 프레임워크 불필요)
- `QuizService`의 `Random` 의존성은 테스트 시 시드 고정을 위해 주입 가능하도록 설계
- Spring Boot 4.0 테스트: `@MockitoBean` 사용, `@WebMvcTest` import 경로 준수

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "1.4"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4", "2.5", "2.6", "2.7", "2.8", "2.9", "2.10"] },
    { "id": 3, "tasks": ["4.1"] },
    { "id": 4, "tasks": ["4.2", "6.1"] },
    { "id": 5, "tasks": ["6.2"] }
  ]
}
```
