# Requirements Document

## Introduction

영어 학습 페이지(`/english-study`)에 랜덤 퀴즈쇼 기능을 추가합니다. 사용자가 페이지에 접속할 때마다 Modal 형태로 10개의 객관식 퀴즈가 출제되며, 기존 EnglishStudy 테이블의 데이터를 활용하여 추가 DB 설계 없이 동작합니다. 랜덤으로 선택된 회차(episode)의 문장들로 문제와 보기를 구성합니다.

## Glossary

- **Quiz_Modal**: 페이지 접속 시 자동으로 표시되는 퀴즈 팝업 UI 컴포넌트. 닫기 버튼만 존재하며 배경 클릭으로 닫히지 않음
- **Quiz_Question**: 하나의 퀴즈 문제. 특정 회차의 영어 또는 한국어 문장이 문제로 출제됨
- **Quiz_Choice**: 객관식 보기 항목. 해당 회차의 다른 문장들 중에서 랜덤으로 선택됨
- **Quiz_Show**: 10개의 Quiz_Question으로 구성된 하나의 퀴즈 세트
- **Episode**: EnglishStudy 엔티티의 회차(episode) 필드. 문장들을 그룹핑하는 단위
- **EnglishStudy_Table**: 기존 영어 학습 데이터를 저장하는 테이블 (id, episode, koreanSentence, englishSentence)
- **Quiz_API**: 퀴즈 데이터를 생성하여 반환하는 REST API 엔드포인트
- **Pagination**: 학습 데이터 목록을 10개씩 나누어 표시하는 페이징 기능 (기존 프론트엔드에 구현됨)

## Requirements

### Requirement 1: 퀴즈 모달 표시

**User Story:** As a 학습자, I want 페이지 접속 시 자동으로 퀴즈 모달을 보고 싶다, so that 매번 접속할 때마다 복습할 수 있다.

#### Acceptance Criteria

1. WHEN 사용자가 `/english-study` 페이지에 접속하여 Quiz_API로부터 퀴즈 데이터를 성공적으로 수신하면, THE Quiz_Modal SHALL 자동으로 화면에 표시된다
2. THE Quiz_Modal SHALL 닫기 버튼을 포함한다
3. WHEN 사용자가 닫기 버튼을 클릭하면, THE Quiz_Modal SHALL 화면에서 사라지고 페이지 스크롤을 복원한다
4. THE Quiz_Modal SHALL 배경 영역 클릭으로 닫히지 않는다
5. WHILE Quiz_Modal이 표시된 상태에서, THE Quiz_Modal SHALL 페이지 스크롤을 차단한다
6. WHEN 사용자가 ESC 키를 누르면, THE Quiz_Modal SHALL 닫기 버튼 클릭과 동일하게 동작한다
7. IF Quiz_API 호출이 실패하거나 반환된 퀴즈 목록이 비어있으면, THEN THE Quiz_Modal SHALL 표시되지 않는다

### Requirement 2: 퀴즈 문제 생성

**User Story:** As a 학습자, I want 여러 회차에서 랜덤으로 선택된 문장으로 퀴즈를 풀고 싶다, so that 다양한 회차의 내용을 골고루 복습할 수 있다.

#### Acceptance Criteria

1. WHEN Quiz_API가 호출되면, THE Quiz_API SHALL EnglishStudy_Table에서 문장이 2개 이상 존재하는 Episode 목록을 조회한다 (보기 구성을 위해 최소 2개 문장이 필요)
2. WHEN Quiz_Question을 생성할 때, THE Quiz_API SHALL 적격 Episode 목록 중에서 매 문제마다 랜덤으로 하나의 Episode를 선택한다 (각 문제가 서로 다른 Episode에서 출제될 수 있다)
3. THE Quiz_API SHALL 총 10개의 Quiz_Question을 생성한다 (적격 Episode들의 전체 문장 수가 10개 미만이면 가용한 문장 수만큼 생성한다)
4. WHEN Quiz_Question을 생성할 때, THE Quiz_API SHALL 각 문제마다 영어 문장 또는 한국어 문장 중 하나를 랜덤으로 선택하여 문제(question)로 출제하고, 나머지를 정답(answer)으로 설정한다
5. WHEN 영어 문장이 문제로 출제되면, THE Quiz_Question SHALL 한국어 문장을 정답으로 설정한다
6. WHEN 한국어 문장이 문제로 출제되면, THE Quiz_Question SHALL 영어 문장을 정답으로 설정한다
7. THE Quiz_Question SHALL 문제 텍스트(question), 정답 텍스트(answer), 출제 방향(questionType: ENGLISH_TO_KOREAN 또는 KOREAN_TO_ENGLISH), 출제 회차(episode)를 포함한다
8. THE Quiz_API SHALL 동일한 문장이 한 Quiz_Show 내에서 중복 출제되지 않도록 한다
9. IF EnglishStudy_Table에 적격 Episode가 하나도 없으면, THEN THE Quiz_API SHALL 빈 퀴즈 목록과 함께 정상 응답을 반환한다

### Requirement 3: 객관식 보기 구성

**User Story:** As a 학습자, I want 같은 회차의 문장들로 구성된 객관식 보기를 보고 싶다, so that 비슷한 난이도의 보기 중에서 정답을 선택할 수 있다.

#### Acceptance Criteria

1. THE Quiz_Choice SHALL 해당 Quiz_Question과 동일한 Episode의 문장들 중에서 정답과 반대 언어의 문장을 랜덤으로 선택하여 구성된다
2. THE Quiz_Question SHALL 정답 1개를 포함하여 총 4개의 Quiz_Choice를 포함한다
3. IF 해당 문제의 Episode에 속한 문장 수가 4개 미만이면, THEN THE Quiz_Question SHALL 해당 Episode의 가용한 문장 수만큼의 Quiz_Choice를 포함하되, 최소 2개(정답 1개 + 오답 1개) 이상이어야 한다
4. THE Quiz_Choice SHALL 정답을 반드시 포함한다
5. THE Quiz_Choice SHALL 동일한 문장이 두 번 이상 포함되지 않도록 구성된다
6. THE Quiz_Choice SHALL 표시 순서를 매 문제마다 랜덤으로 섞는다
7. IF 해당 Episode에서 오답 보기로 사용할 문장이 1개 미만이면, THEN THE Quiz_Question SHALL 해당 문장을 퀴즈 문제에서 제외한다

### Requirement 4: 퀴즈 진행 및 페이징

**User Story:** As a 학습자, I want 10개의 문제를 하나씩 풀어나가고 싶다, so that 집중해서 학습할 수 있다.

#### Acceptance Criteria

1. THE Quiz_Show SHALL 한 번에 하나의 Quiz_Question을 표시한다
2. THE Quiz_Modal SHALL 현재 문제 번호와 전체 문제 수를 "{현재번호}/{전체문제수}" 형식으로 표시한다 (전체 문제 수는 Quiz_Show에 포함된 실제 Quiz_Question 수를 반영한다)
3. WHEN 사용자가 Quiz_Choice를 선택하면, THE Quiz_Modal SHALL 추가 네트워크 요청 없이 정답 여부를 표시한다 (사용자의 보기 선택만이 정답 표시를 트리거하며, 타임아웃이나 기타 트리거는 허용하지 않는다)
4. WHEN 마지막 문제가 아닌 Quiz_Question의 정답 여부가 표시된 후, THE Quiz_Modal SHALL 다음 문제로 이동하는 버튼을 표시한다
5. WHEN 마지막 문제의 정답 여부가 표시된 후, THE Quiz_Modal SHALL 퀴즈 종료 버튼을 표시한다 (다음 문제 버튼은 표시하지 않는다)
6. WHEN 퀴즈 종료 버튼이 클릭되면, THE Quiz_Modal SHALL 화면에서 사라진다
7. THE Quiz_Modal SHALL 이전 문제로 되돌아가는 기능을 제공하지 않는다 (순방향 진행만 허용)

### Requirement 5: 퀴즈 API 엔드포인트

**User Story:** As a 개발자, I want 퀴즈 데이터를 REST API로 제공하고 싶다, so that 프론트엔드에서 퀴즈 데이터를 비동기로 가져올 수 있다.

#### Acceptance Criteria

1. THE Quiz_API SHALL `/api/english-study/quiz` GET 엔드포인트를 제공한다
2. WHEN 퀴즈 데이터 생성에 성공하면, THE Quiz_API SHALL HTTP 200 상태 코드와 Content-Type `application/json`으로 응답한다
3. THE Quiz_API SHALL 응답 JSON에 퀴즈 문제 목록을 포함하며, 각 문제는 문제 텍스트(question), 보기 목록(choices), 정답 보기의 인덱스(answerIndex)를 포함한다
4. THE Quiz_API SHALL 추가 데이터베이스 테이블 없이 EnglishStudy_Table만 사용하여 동작한다
5. THE Quiz_API SHALL 매 호출 시 랜덤한 퀴즈 데이터를 생성하여 반환한다
6. IF EnglishStudy_Table에 데이터가 존재하지 않거나 퀴즈 데이터 생성에 실패하면, THEN THE Quiz_API SHALL HTTP 200 상태 코드와 빈 퀴즈 문제 목록(빈 배열)을 반환한다

### Requirement 6: 정답 피드백 표시

**User Story:** As a 학습자, I want 정답과 오답에 대한 시각적 피드백을 받고 싶다, so that 학습 결과를 즉시 확인할 수 있다.

#### Acceptance Criteria

1. WHEN 사용자가 정답인 Quiz_Choice를 선택하면, THE Quiz_Modal SHALL 해당 보기의 배경색을 초록색으로 변경하여 정답임을 표시한다
2. WHEN 사용자가 오답인 Quiz_Choice를 선택하면, THE Quiz_Modal SHALL 선택된 보기의 배경색을 빨간색으로 변경하고, 정답인 보기의 배경색을 초록색으로 변경하여 동시에 표시한다
3. WHEN 사용자가 Quiz_Choice를 선택하면, THE Quiz_Modal SHALL 모든 Quiz_Choice의 클릭 이벤트를 즉시 비활성화하여 추가 선택을 차단한다
4. WHILE 피드백이 표시된 상태에서, THE Quiz_Modal SHALL 사용자가 다음 문제로 이동하거나 퀴즈를 종료할 때까지 정답/오답 배경색 표시와 비활성화 상태를 유지한다
