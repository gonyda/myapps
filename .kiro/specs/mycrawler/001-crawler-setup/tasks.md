# Implementation Plan: mycrawler 모듈 크롤러 기본 셋업

## Overview

Playwright Java 기반 웹 크롤링 엔진을 갖춘 Spring Boot 모듈을 구축한다. DDD 계층 구조를 따르며, 스케줄 기반 자동 실행과 Thymeleaf Admin UI를 통한 수동 실행을 지원한다. 데이터 영속화 없이 in-memory 방식으로 최근 결과를 관리한다.

## Tasks

- [x] 1. 모듈 프로젝트 구조 구성
  - [x] 1.1 Parent POM에 mycrawler 모듈 등록 및 mycrawler/pom.xml 생성
    - Parent POM의 `<modules>` 섹션에 `<module>mycrawler</module>` 추가
    - mycrawler/pom.xml 생성: parent 참조(com.myapps:myapps:1.0.0-SNAPSHOT), packaging jar
    - 모듈 고유 의존성 추가 (버전 없이): spring-boot-starter-web, spring-boot-starter-thymeleaf, com.microsoft.playwright:playwright, spring-boot-starter-webmvc-test(test), net.jqwik:jqwik(test), h2(test)
    - Parent POM `<dependencyManagement>`에 playwright 버전 추가
    - _Requirements: 1.1_

  - [x] 1.2 DDD 계층 패키지 구조 및 메인 클래스 생성
    - com.myapps.web.mycrawler 패키지에 MycrawlerApplication 클래스 생성 (@SpringBootApplication)
    - domain/model, application/service, interfaces/api, infrastructure/config, infrastructure/crawler, infrastructure/antidetect 패키지 구조 생성
    - 각 패키지에 package-info.java 또는 최소 1개 클래스 배치하여 패키지 존재 보장
    - _Requirements: 1.2, 1.3_

  - [x] 1.3 설정 파일 생성
    - src/main/resources에 application.yml, application-local.yml, application-prod.yml 생성
    - application.yml에 server.port: 8081, crawler 설정 구조(cron, timeout-seconds, browsers-path, targets) 정의
    - 기본 타겟: name: fmkorea-stock, url: https://www.fmkorea.com/stock
    - application-local.yml에 crawler.browsers-path: /Users/gony/Library/Caches/ms-playwright 설정
    - application-prod.yml에 crawler.browsers-path: /home/ubuntu/.cache/ms-playwright 설정
    - _Requirements: 1.4, 5.1, 5.3_

- [x] 2. 도메인 모델 구현
  - [x] 2.1 CrawlStatus, TriggerSource enum 및 CrawlTarget, CrawlResult record 생성
    - domain/model 패키지에 CrawlStatus enum (SUCCESS, FAILURE) 생성
    - domain/model 패키지에 TriggerSource enum (SCHEDULED, MANUAL) 생성
    - domain/model 패키지에 CrawlTarget record (name, url) 생성
    - domain/model 패키지에 CrawlResult record (targetName, targetUrl, status, triggerSource, content, errorMessage, startTime, endTime) 생성
    - CrawlResult에 durationMillis(), contentSummary(int maxLength) 메서드 포함
    - _Requirements: 2.3, 5.2_

  - [x] 2.2 Property 테스트: CrawlResult 구조적 무결성
    - **Property 1: CrawlResult 구조적 무결성 및 실패 매핑**
    - status, triggerSource, startTime, endTime non-null 불변식 검증
    - endTime >= startTime 불변식 검증
    - 예외 발생 시 status=FAILURE이면 errorMessage non-empty 검증
    - **Validates: Requirements 2.3, 2.4**

- [x] 3. 설정 바인딩 및 크롤 타겟 유효성 검증
  - [x] 3.1 CrawlerConfig 생성 및 타겟 유효성 검증 로직 구현
    - infrastructure/config 패키지에 CrawlerConfig record 생성 (@ConfigurationProperties(prefix = "crawler"))
    - 필드: cron, timeoutSeconds, browsersPath, List<TargetConfig> targets
    - TargetConfig 중첩 record (name, url) 포함
    - 유효성 검증 로직 구현: URL/이름 누락, URL 형식 무효, 이름 중복 항목 필터링
    - 무효 항목에 대한 경고 로그 출력
    - 타겟 목록 비어있을 때 경고 로그 출력
    - _Requirements: 5.1, 5.2, 5.4, 5.5_

  - [x] 3.2 Property 테스트: 크롤 타겟 유효성 검증
    - **Property 8: 크롤 타겟 유효성 검증**
    - 이름 null/빈 문자열, URL null/빈 문자열/무효 형식, 이름 중복 항목이 유효 목록에서 제외됨을 검증
    - **Validates: Requirements 5.2, 5.5**

- [x] 4. 안티 디텍션 서비스 구현
  - [x] 4.1 AntiDetectionService 구현
    - infrastructure/antidetect 패키지에 AntiDetectionService 클래스 생성
    - randomUserAgent(): 사전 정의된 UA 목록(Chrome, Firefox, Edge 최신 버전)에서 랜덤 선택
    - randomViewport(): width [1280, 1920], height [720, 1080] 범위 내 랜덤 생성
    - randomPageDelay(): [1000, 5000]ms 범위 내 랜덤 딜레이
    - randomInterTargetDelay(): [3000, 10000]ms 범위 내 랜덤 딜레이
    - simulateHumanBehavior(Page page): 랜덤 마우스 이동/스크롤 시뮬레이션
    - applyStealthSettings(BrowserContext context): navigator.webdriver 제거, 자동화 탐지 시그니처 제거
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

  - [x] 4.2 Property 테스트: User-Agent 랜덤 선택 멤버십
    - **Property 2: User-Agent 랜덤 선택 멤버십**
    - randomUserAgent() 반환값이 사전 정의된 목록의 원소임을 검증
    - **Validates: Requirements 6.1**

  - [x] 4.3 Property 테스트: 랜덤 딜레이 범위 불변식
    - **Property 3: 랜덤 딜레이 범위 불변식**
    - randomPageDelay() 결과가 [1000, 5000]ms 범위 내임을 검증
    - randomInterTargetDelay() 결과가 [3000, 10000]ms 범위 내임을 검증
    - **Validates: Requirements 6.2, 6.6**

  - [x] 4.4 Property 테스트: 랜덤 Viewport 범위 불변식
    - **Property 4: 랜덤 Viewport 범위 불변식**
    - randomViewport() width [1280, 1920], height [720, 1080] 범위 내임을 검증
    - **Validates: Requirements 6.4**

- [x] 5. Checkpoint - 도메인 및 안티 디텍션 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. 크롤링 엔진 구현
  - [x] 6.1 PlaywrightCrawlerEngine 구현
    - infrastructure/crawler 패키지에 CrawlerEngine 인터페이스 정의 (CrawlResult crawl(CrawlTarget target))
    - PlaywrightCrawlerEngine 구현 클래스 생성
    - 애플리케이션 시작 시 Playwright 인스턴스 초기화, headless Chromium Browser 싱글톤 유지
    - CrawlerConfig.browsersPath()가 설정되어 있으면 환경변수 PLAYWRIGHT_BROWSERS_PATH를 해당 값으로 설정하여 환경별 브라우저 경로 해소
    - 요청마다 BrowserContext 새로 생성 (AntiDetectionService를 통한 UA, viewport, 스텔스 설정 적용)
    - 페이지 로드 타임아웃 30초 설정
    - 페이지 로드 후 AntiDetectionService.simulateHumanBehavior() 호출
    - 성공 시 SUCCESS CrawlResult 반환 (콘텐츠 포함)
    - 실패 시 (타임아웃, 네트워크 오류, 크래시) FAILURE CrawlResult 반환 (에러 메시지 포함, 예외 전파 방지)
    - 실행 결과 INFO 레벨 로그 출력 (상태, URL, 응답 본문 요약, 소요 시간)
    - BrowserContext 정리 (finally 블록)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 7. 크롤러 서비스 구현
  - [x] 7.1 CrawlerService 구현
    - application/service 패키지에 CrawlerService 클래스 생성
    - AtomicBoolean으로 isRunning 상태 관리
    - executeAll(TriggerSource): 모든 유효 타겟에 대해 순차 크롤링 수행, 타겟 간 randomInterTargetDelay 적용
    - executeSingle(String targetName, TriggerSource): 특정 타겟 크롤링 실행
    - isRunning() true 시 중복 실행 방지 (빈 결과 반환 + 로그)
    - ConcurrentLinkedDeque로 최근 결과 최대 20건 유지 (시간 역순)
    - 개별 타겟 실패가 전체 배치 중단하지 않도록 try-catch 처리
    - _Requirements: 2.3, 3.2, 4.3, 4.4, 6.6_

  - [x] 7.2 Property 테스트: 중복 실행 방지
    - **Property 5: 중복 실행 방지**
    - isRunning이 true일 때 executeAll() 호출이 새 크롤링을 시작하지 않고 빈 결과를 반환함을 검증
    - **Validates: Requirements 3.2**

  - [x] 7.3 Property 테스트: 최근 결과 목록 크기 제한 및 정렬 순서
    - **Property 7: 최근 결과 목록 크기 제한 및 정렬 순서**
    - N개 결과 추가 후 getRecentResults() 크기가 min(N, 20) 이하임을 검증
    - 목록이 시간 역순으로 정렬됨을 검증
    - **Validates: Requirements 4.4**

  - [x] 7.4 CrawlerService 단위 테스트
    - executeAll 성공/실패 시나리오 테스트
    - executeSingle 테스트
    - 결과 저장 및 크기 제한 테스트
    - _Requirements: 2.3, 3.2, 4.4_

- [x] 8. 스케줄러 서비스 구현
  - [x] 8.1 SchedulerService 구현
    - application/service 패키지에 SchedulerService 클래스 생성 (SchedulingConfigurer 구현)
    - configureTasks(): CronTrigger 조합으로 매 트리거 시점마다 cron 재평가 (재시작 없이 변경 반영)
    - CronExpression.isValidExpression()으로 cron 유효성 검증
    - 유효하지 않은 cron: 에러 로그 출력, 스케줄링 비활성화, 앱 정상 기동
    - isEnabled(), getNextExecutionTime(), getCronExpression() 메서드 제공
    - infrastructure/config에 SchedulerConfig 클래스 생성 (@EnableScheduling)
    - _Requirements: 3.1, 3.3, 3.4, 3.5_

  - [x] 8.2 Property 테스트: 유효하지 않은 Cron 표현식 시 스케줄러 비활성화
    - **Property 6: 유효하지 않은 Cron 표현식 시 스케줄러 비활성화**
    - 유효하지 않은 cron 문자열(파싱 불가능, null, 빈 문자열)에 대해 isEnabled()가 false를 반환함을 검증
    - **Validates: Requirements 3.5**

  - [x] 8.3 SchedulerService 단위 테스트
    - 유효 cron으로 스케줄러 활성화 테스트
    - 다음 실행 시간 계산 테스트
    - 유효하지 않은 cron으로 비활성화 테스트
    - _Requirements: 3.1, 3.5_

- [x] 9. Checkpoint - 엔진 및 스케줄러 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Admin UI 구현
  - [x] 10.1 AdminController 및 Thymeleaf 템플릿 구현
    - interfaces/api 패키지에 AdminController 생성 (@Controller, @RequestMapping("/admin"))
    - GET /admin: 대시보드 (최근 결과 목록, 스케줄러 상태, 수동 실행 버튼)
    - POST /admin/crawl: 수동 실행 트리거 (PRG 패턴으로 리다이렉트)
    - 실행 중일 때 "현재 크롤링이 진행 중입니다" 메시지 표시
    - 결과 표시: 성공/실패 상태, 응답 본문 처음 500자, 소요 시간, 트리거 출처(스케줄러/수동)
    - 스케줄러 상태 표시: 활성/비활성, 다음 실행 예정 시간
    - src/main/resources/templates/admin.html Thymeleaf 템플릿 생성 (인라인 스타일)
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [x] 10.2 AdminController 웹 슬라이스 테스트 (@WebMvcTest)
    - 대시보드 GET 요청 정상 응답 테스트
    - 수동 실행 POST 요청 및 리다이렉트 테스트
    - 실행 중 중복 실행 방지 메시지 테스트
    - _Requirements: 4.1, 4.2, 4.3_

- [x] 11. 통합 및 전역 예외 처리
  - [x] 11.1 전역 예외 처리 및 통합 테스트
    - @ControllerAdvice로 전역 예외 핸들러 구현 (에러 페이지 표시)
    - 애플리케이션 컨텍스트 로드 통합 테스트 (@SpringBootTest)
    - 설정 바인딩 검증 테스트 (CrawlerConfig가 application.yml에서 올바르게 로드되는지)
    - _Requirements: 2.4, 5.1_

- [x] 12. Final checkpoint - 전체 테스트 및 빌드 검증
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- 모든 Task는 필수이며, `*` 표시된 테스트 Task도 반드시 실행합니다
- 각 Task는 specific requirements를 참조하여 추적 가능성을 보장합니다
- Property 테스트는 jqwik `@Property(tries = 100)`으로 구현합니다
- Playwright 브라우저 조작은 통합 테스트가 아닌 Mock 기반 단위 테스트로 검증합니다
- Checkpoints에서 빌드 성공(mvn clean install -pl mycrawler -am)을 확인합니다
- 포트: 8081 (mystudy:8080 다음)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "1.3"] },
    { "id": 2, "tasks": ["2.1", "3.1"] },
    { "id": 3, "tasks": ["2.2", "3.2", "4.1"] },
    { "id": 4, "tasks": ["4.2", "4.3", "4.4"] },
    { "id": 5, "tasks": ["6.1"] },
    { "id": 6, "tasks": ["7.1", "8.1"] },
    { "id": 7, "tasks": ["7.2", "7.3", "7.4", "8.2", "8.3"] },
    { "id": 8, "tasks": ["10.1"] },
    { "id": 9, "tasks": ["10.2", "11.1"] }
  ]
}
```
