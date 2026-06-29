# Requirements Document

## Introduction

mycrawler 모듈의 대시보드를 개선하여 등록된 크롤러 목록을 표시하고, 기존의 "전체 실행" 방식을 "개별 실행" 방식으로 전환하는 기능을 구현합니다. 추가로 wepoll.kr 주식 게시판 크롤러를 새로 등록합니다. HTML 파싱 로직은 이번 스펙 범위에 포함하지 않으며, 크롤링 실행 구조와 대시보드 UI 프레임워크만 구성합니다.

## Glossary

- **Dashboard**: AdminController가 제공하는 관리 웹 UI 화면 (`/admin` 경로)
- **CrawlerService**: 크롤링 실행의 오케스트레이션을 담당하는 애플리케이션 서비스
- **CrawlTarget**: 크롤링 대상의 이름과 URL을 포함하는 불변 값 객체
- **CrawlerConfig**: application.yml에서 크롤링 대상 목록과 설정을 바인딩하는 설정 레코드
- **AdminController**: 대시보드 화면 렌더링과 수동 크롤링 실행 엔드포인트를 제공하는 컨트롤러
- **Registered_Crawler**: CrawlerConfig의 targets에 등록된 유효한 CrawlTarget 항목

## Requirements

### Requirement 1: 개별 크롤러 실행 엔드포인트

**User Story:** As a 관리자, I want to 특정 크롤러를 선택하여 개별적으로 실행할 수 있도록, so that 필요한 대상만 즉시 크롤링할 수 있습니다.

#### Acceptance Criteria

1. THE AdminController SHALL 개별 실행 엔드포인트를 POST /admin/crawl/{targetName} 경로로 제공하며, targetName은 CrawlerConfig에 등록된 유효 타겟의 name 값과 일치하는 문자열이다
2. WHEN 관리자가 POST /admin/crawl/{targetName} 요청을 전송하면, THE AdminController SHALL isRunning() 상태를 먼저 확인한 후 CrawlerService의 executeSingle 메서드를 targetName과 TriggerSource.MANUAL을 파라미터로 전달하여 호출한다
3. WHEN executeSingle 호출이 CrawlResult를 반환하면, THE AdminController SHALL 처리된 타겟명을 포함하는 성공 메시지를 flash attribute로 설정하고 /admin으로 리다이렉트한다
4. IF executeSingle 호출 시 크롤링이 이미 실행 중이면(isRunning()이 true), THEN THE AdminController SHALL executeSingle을 호출하지 않고 크롤링 진행 중임을 나타내는 경고 메시지를 flash attribute로 설정하고 /admin으로 리다이렉트한다
5. IF executeSingle 호출 결과가 null이면(지정된 targetName에 해당하는 크롤러가 유효 타겟 목록에 존재하지 않음), THEN THE AdminController SHALL 해당 targetName을 찾을 수 없음을 나타내는 오류 메시지를 flash attribute로 설정하고 /admin으로 리다이렉트한다

### Requirement 2: 전체 실행 엔드포인트 제거

**User Story:** As a 관리자, I want to 전체 실행 버튼 대신 개별 실행 버튼만 사용하도록, so that 불필요한 전체 크롤링을 방지할 수 있습니다.

#### Acceptance Criteria

1. THE AdminController SHALL 기존 POST /admin/crawl 전체 실행 엔드포인트(triggerCrawl 메서드)를 제거하여, 해당 경로로 요청 시 HTTP 404 응답을 반환하도록 한다
2. THE Dashboard SHALL "수동 실행" 섹션 전체(섹션 제목, form, "크롤링 실행" 버튼)를 제거한다
3. IF 외부 클라이언트가 POST /admin/crawl 경로로 요청을 보내면, THEN THE AdminController SHALL HTTP 404 상태 코드를 반환한다

### Requirement 3: 등록된 크롤러 목록 대시보드 표시

**User Story:** As a 관리자, I want to 대시보드에서 등록된 크롤러 목록을 확인할 수 있도록, so that 어떤 크롤러가 설정되어 있는지 한눈에 파악할 수 있습니다.

#### Acceptance Criteria

1. WHEN 관리자가 대시보드에 접속하면, THE Dashboard SHALL CrawlerConfig에 등록된 모든 유효한 CrawlTarget 목록을 표시한다
2. THE Dashboard SHALL 각 CrawlTarget 항목에 대해 이름(name)과 URL(url)을 표시한다
3. THE Dashboard SHALL 각 CrawlTarget 항목마다 해당 타겟을 크롤링하는 개별 실행 버튼을 표시한다
4. WHEN 관리자가 특정 CrawlTarget의 개별 실행 버튼을 클릭하면, THE Dashboard SHALL 해당 타겟 이름으로 단일 크롤링을 실행하고 완료 후 대시보드로 리다이렉트한다
5. WHILE 크롤링이 진행 중인 상태(isRunning=true)이면, THE Dashboard SHALL 모든 개별 실행 버튼을 비활성화(disabled)하여 클릭할 수 없도록 한다
6. IF 유효한 CrawlTarget이 0건이면, THEN THE Dashboard SHALL 크롤러 목록 영역에 등록된 크롤러가 없음을 나타내는 안내 문구를 표시한다

### Requirement 4: 대시보드에 등록된 크롤러 목록 제공 API

**User Story:** As a AdminController, I want to 등록된 크롤러 목록을 모델에 포함시킬 수 있도록, so that Thymeleaf 템플릿에서 크롤러 목록을 렌더링할 수 있습니다.

#### Acceptance Criteria

1. WHEN GET /admin 요청이 수신되면, THE AdminController SHALL CrawlerConfig의 validTargets 메서드를 호출하여 반환된 CrawlTarget 목록을 "targets" 속성명으로 모델에 추가한다
2. WHEN CrawlerConfig의 validTargets가 빈 목록을 반환하면, THE AdminController SHALL 빈 리스트를 "targets" 속성명으로 모델에 추가한다
3. THE AdminController SHALL "targets" 모델 속성의 각 요소를 CrawlTarget 타입으로 제공하며, 각 요소는 name(크롤러 식별명)과 url(크롤링 대상 URL)을 포함한다

### Requirement 5: wepoll.kr 주식 게시판 크롤러 등록

**User Story:** As a 관리자, I want to wepoll.kr 주식 게시판 크롤러를 등록하여, so that 해당 사이트의 콘텐츠를 크롤링할 수 있습니다.

#### Acceptance Criteria

1. THE CrawlerConfig SHALL application.yml의 targets 목록에 name이 "wepoll-stock"이고 url이 "https://wepoll.kr/g2/bbs/board.php?bo_table=stock"인 항목을 포함한다
2. WHEN 애플리케이션이 시작되면, THE CrawlerConfig SHALL validTargets() 결과 목록에 name이 "wepoll-stock"이고 url이 "https://wepoll.kr/g2/bbs/board.php?bo_table=stock"인 CrawlTarget을 포함한다
3. IF application.yml의 targets 목록에 "wepoll-stock"과 동일한 name을 가진 항목이 이미 존재하면, THEN THE CrawlerConfig SHALL 중복된 항목을 무시하고 첫 번째 항목만 validTargets() 결과에 포함한다

### Requirement 6: SchedulerService의 개별 실행 방식 전환

**User Story:** As a 시스템, I want to 스케줄러도 전체 실행 대신 개별 실행 방식을 사용하도록, so that 스케줄 크롤링도 일관된 실행 패턴을 따릅니다.

#### Acceptance Criteria

1. WHEN 스케줄링된 크롤링 시각이 도래하면, THE SchedulerService SHALL CrawlerConfig의 validTargets 목록을 순회하며 각 타겟에 대해 타겟의 name과 TriggerSource.SCHEDULED를 인자로 CrawlerService의 executeSingle 메서드를 호출한다
2. IF 개별 타겟에 대한 executeSingle 호출이 null을 반환하거나 예외를 던지면, THEN THE SchedulerService SHALL 해당 타겟의 이름과 실패 원인을 포함하는 에러 로그를 기록하고 나머지 타겟의 크롤링을 계속 진행한다
3. WHEN 스케줄링된 크롤링에서 복수의 타겟을 순회할 때, THE SchedulerService SHALL 각 타겟 실행 사이에 AntiDetectionService의 randomInterTargetDelay를 적용한다
