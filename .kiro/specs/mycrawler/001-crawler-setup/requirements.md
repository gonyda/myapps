# Requirements Document

## Introduction

mycrawler 모듈의 기본 프레임워크를 구성합니다. 이 모듈은 Spring Boot 기반 웹 애플리케이션으로, 설정 가능한 스케줄에 따라 웹 크롤링을 수행하며, 웹 UI를 통해 크롤러를 수동으로 즉시 실행하거나 테스트할 수 있는 기능을 제공합니다. 이번 스펙의 범위는 크롤링이 정상적으로 동작하는지 확인하는 기본 셋업 단계이며, 수집된 데이터의 파싱이나 가공은 포함하지 않습니다.

## Glossary

- **Crawler_Engine**: Playwright Java를 사용하여 대상 URL을 브라우저로 렌더링하고 페이지 데이터를 수집하는 핵심 크롤링 실행 컴포넌트
- **Anti_Detection**: IP 차단 방지를 위해 실제 사용자의 브라우저 행동을 모방하는 기법(랜덤 딜레이, User-Agent 랜덤화, 마우스 이동/스크롤 시뮬레이션 등)
- **Scheduler**: Spring의 스케줄링 메커니즘을 사용하여 Crawler_Engine을 주기적으로 실행하는 컴포넌트
- **Crawl_Job**: Crawler_Engine이 한 번 실행되어 대상 URL을 크롤링하고 결과를 저장하는 단위 작업
- **Crawl_Result**: Crawl_Job 실행 후 수집된 데이터와 실행 메타정보(상태, 시작시간, 종료시간, 에러 메시지, 트리거 출처)를 포함하는 값 객체. 영속화하지 않으며 로그 출력 및 화면 표시 용도로만 사용한다
- **Trigger_Source**: Crawl_Job이 실행된 원인을 나타내는 구분값. SCHEDULED(스케줄러에 의한 자동 실행) 또는 MANUAL(Admin_UI를 통한 수동 실행)로 구분된다
- **Admin_UI**: 크롤러 상태 확인 및 수동 실행을 위한 웹 기반 관리 화면
- **Crawl_Target**: 크롤링 대상이 되는 URL과 관련 설정 정보를 담는 도메인 객체

## Requirements

### Requirement 1: 모듈 프로젝트 구조 구성

**User Story:** As a 개발자, I want mycrawler 모듈이 프로젝트 표준 구조에 맞게 구성되기를, so that 일관된 개발 환경에서 크롤러 기능을 개발할 수 있다.

#### Acceptance Criteria

1. THE mycrawler 모듈 SHALL Parent POM의 modules 섹션에 "mycrawler"로 등록되고, mycrawler/pom.xml에 parent 참조(groupId: com.myapps, artifactId: myapps, version: 1.0.0-SNAPSHOT)와 packaging jar 설정을 포함한다
2. THE mycrawler 모듈 SHALL com.myapps.web.mycrawler 기본 패키지 하위에 DDD 4개 계층 패키지(domain, application, interfaces, infrastructure)를 갖추며, 각 계층은 최소 1개 이상의 하위 패키지(domain/model, application/service, interfaces/api, infrastructure/config)를 포함한다
3. THE mycrawler 모듈 SHALL com.myapps.web.mycrawler 패키지에 @SpringBootApplication 어노테이션이 선언된 MycrawlerApplication 메인 클래스를 포함한다
4. THE mycrawler 모듈 SHALL src/main/resources 디렉터리에 application.yml, application-local.yml, application-prod.yml 3개의 설정 파일을 포함한다

### Requirement 2: 크롤링 엔진 기본 구조

**User Story:** As a 개발자, I want 크롤링 실행의 기본 골격이 마련되기를, so that 향후 다양한 크롤링 대상을 쉽게 추가할 수 있다.

#### Acceptance Criteria

1. THE Crawler_Engine SHALL Playwright Java(com.microsoft.playwright) 라이브러리를 사용하여 headless Chromium 브라우저로 대상 URL을 로드하고, 페이지 렌더링 완료 후 콘텐츠를 문자열로 반환한다
2. THE Crawler_Engine SHALL 페이지 로드 타임아웃을 30초로 설정하며, 타임아웃 초과 시 FAILURE로 처리한다
3. WHEN Crawler_Engine이 크롤링을 완료하면, THE Crawler_Engine SHALL Crawl_Result 객체를 생성하여 실행 상태(SUCCESS 또는 FAILURE), 시작 시간, 종료 시간, 트리거 출처(Trigger_Source)를 기록한다
4. IF 페이지 로드가 실패하면(네트워크 오류, 타임아웃, 브라우저 크래시), THEN THE Crawler_Engine SHALL Crawl_Result에 FAILURE 상태와 실패 원인을 나타내는 에러 메시지를 기록하며, 어떠한 실패 조건에서도 SUCCESS 상태가 기록되지 않아야 한다
5. WHEN Crawl_Result 객체가 생성되면, THE Crawler_Engine SHALL 실행 결과(상태, 대상 URL, 응답 본문 요약, 소요 시간)를 INFO 레벨 로그로 출력한다

### Requirement 6: 안티 디텍션 (Anti-Detection)

**User Story:** As a 운영자, I want 크롤러가 실제 사용자처럼 행동하기를, so that 대상 사이트로부터 IP 차단이나 봇 탐지를 회피할 수 있다.

#### Acceptance Criteria

1. THE Crawler_Engine SHALL 각 크롤링 세션마다 실제 브라우저의 User-Agent 문자열 목록에서 랜덤으로 하나를 선택하여 적용한다
2. THE Crawler_Engine SHALL 페이지 로드 후 1초~5초 범위의 랜덤 딜레이를 적용한 뒤 데이터 추출을 시작한다
3. THE Crawler_Engine SHALL 페이지 내에서 랜덤한 마우스 이동과 스크롤 동작을 수행하여 사람의 브라우징 패턴을 시뮬레이션한다
4. THE Crawler_Engine SHALL Playwright의 브라우저 컨텍스트 설정에서 viewport 크기를 일반적인 데스크톱 해상도(1280x720 ~ 1920x1080) 범위 내에서 랜덤하게 설정한다
5. THE Crawler_Engine SHALL navigator.webdriver 속성을 숨기고, Playwright 자동화 탐지 시그니처를 제거하는 스텔스 설정을 적용한다
6. IF 다수의 Crawl_Target이 존재할 경우, THEN THE Crawler_Engine SHALL 각 타겟 사이 요청 간격을 3초~10초 랜덤 딜레이로 설정하여 요청 패턴을 분산시킨다

### Requirement 3: 스케줄 기반 크롤링 실행

**User Story:** As a 운영자, I want 크롤러가 설정된 주기에 따라 자동으로 실행되기를, so that 수동 개입 없이 정기적으로 데이터를 수집할 수 있다.

#### Acceptance Criteria

1. THE Scheduler SHALL application 설정 파일(application.yml)에 정의된 cron 표현식에 따라 Crawl_Job을 실행한다
2. WHILE Crawl_Job이 실행 중인 상태에서, THE Scheduler SHALL 동일한 Crawl_Job의 중복 실행 요청을 무시하고 해당 실행이 생략되었음을 로그에 기록한다
3. WHEN 애플리케이션이 시작되면, THE Scheduler SHALL 설정된 스케줄에 따라 자동으로 활성화된다
4. THE Scheduler SHALL 스케줄 cron 표현식을 애플리케이션 재시작 없이 변경 가능하도록 외부 설정으로 관리하며, 변경된 cron 표현식은 다음 스케줄 트리거 시점부터 적용된다
5. IF application 설정 파일에 cron 표현식이 누락되었거나 유효하지 않은 형식인 경우, THEN THE Scheduler SHALL 애플리케이션 시작 시 에러를 나타내는 로그를 출력하고 스케줄링을 비활성화한 채로 애플리케이션을 정상 기동한다

### Requirement 4: 웹 UI를 통한 수동 실행 및 테스트

**User Story:** As a 운영자, I want 웹 화면에서 크롤러를 즉시 실행하고 결과를 확인할 수 있기를, so that 크롤러 동작을 빠르게 테스트하고 문제를 진단할 수 있다.

#### Acceptance Criteria

1. THE Admin_UI SHALL 크롤러 수동 실행 버튼을 제공하며, 버튼 클릭 시 Crawl_Job을 즉시 실행한다
2. WHEN 수동 실행이 요청되면, THE Admin_UI SHALL 실행 결과(성공/실패 상태, 응답 본문의 처음 500자, 소요 시간)를 화면에 표시한다
3. IF 수동 실행 요청 시 이미 Crawl_Job이 실행 중이면, THEN THE Admin_UI SHALL "현재 크롤링이 진행 중입니다" 메시지를 표시하고 중복 실행을 방지한다
4. THE Admin_UI SHALL 최근 실행 결과를 메모리 내 목록으로 최대 20건까지 시간 역순으로 표시하며, 각 결과에 트리거 출처(스케줄러/수동)를 구분하여 표시한다
5. THE Admin_UI SHALL 현재 스케줄러 상태(활성/비활성, 다음 실행 예정 시간)를 표시한다

### Requirement 5: 크롤링 대상 설정 관리

**User Story:** As a 운영자, I want 크롤링 대상 URL을 설정으로 관리할 수 있기를, so that 코드 변경 없이 크롤링 대상을 변경할 수 있다.

#### Acceptance Criteria

1. THE Crawl_Target SHALL application 설정 파일(application.yml)에서 1개 이상의 대상 URL 목록을 읽어온다
2. THE Crawl_Target SHALL 각 항목에 대해 URL과 크롤링 대상의 이름을 필수 속성으로 포함하며, 이름은 목록 내에서 고유해야 한다
3. THE Crawl_Target SHALL 기본 설정으로 name: "fmkorea-stock", url: "https://www.fmkorea.com/stock" 항목을 포함한다
4. IF 설정에 정의된 Crawl_Target이 없는 경우, THEN THE Crawler_Engine SHALL 애플리케이션 시작 시 경고 로그를 출력하고 정상적으로 기동한다
5. IF Crawl_Target 설정에서 URL 또는 이름이 누락되었거나 URL 형식이 유효하지 않은 경우, THEN THE Crawler_Engine SHALL 해당 항목을 무시하고 경고 로그를 출력한다
