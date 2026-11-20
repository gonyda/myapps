# Implementation Plan: mini-app-store

> **폴더 위치**: `.kiro/specs/myshare/002-mini-app-store/tasks.md`
> **관련 규칙**: `rules/workflow/task-build-validation.md`, `rules/workflow/git-workflow.md`

---

## Overview

본 작업 명세는 `myshare` 모듈(`com.myapps.web.myshare`, 포트 `8084`)에 정적 HTML 미니앱 공용 JSON 저장소(`MINI_APP_STORE`) REST API, `X-Mini-Pin` 인증, `@Async` `gonyda/mike` GitHub 동기화를 점진적으로 구현하기 위한 체크포인트 단위 작업 목록이다.

### 구현 순서 및 원칙
1. **Bottom-Up 계층 조립**:
   **A. 빌드/설정 & 도메인** $\rightarrow$ **B. 애플리케이션 서비스 & 검증기** $\rightarrow$ **C. 인프라(설정·GitHub 푸시 클라이언트)** $\rightarrow$ **D. 웹 컨트롤러 & PIN 필터 & 예외 처리** $\rightarrow$ **E. 5대 품질 가드레일 통합 검증**
2. **테스트 필수 & Given-When-Then**: 모든 테스트에 `// given`, `// when`, `// then` 주석과 `should_{동작}_when_{조건}` 네이밍 적용 (Spring Boot 4.0: `@MockitoBean`, `@WebMvcTest`, `@DataJpaTest`, Jackson 3 `tools.jackson`).

---

## Tasks

### A. 모듈 의존성/설정 활성화 및 도메인 계층 (Config & Domain Layer)

- [x] 1. `myshare` DB/JPA 활성화 및 설정 프로파일 구성
  - [x] 1.1 `myshare/pom.xml`에 `com.h2database:h2` (`runtime`) 및 `spring-boot-starter-data-jpa-test` (`test`) 의존성 추가
  - [x] 1.2 `MyshareApplication.java`의 DataSource/Hibernate/JPA AutoConfiguration `exclude` 제거
  - [x] 1.3 `application.yml`, `application-local.yml`, `application-prod.yml`, `src/test/resources/application.yml`에 Oracle DB(Wallet)/H2(test) 및 `myshare.mini-app.*` 설정 추가
  - _Requirements: 1.1, 3.2_ / _Design: 1.1, 4.1_

- [x] 2. 도메인 모델, 예외 및 JPA 리포지토리 구현
  - [x] 2.1 `InvalidMiniAppKeyException.java`, `InvalidMiniAppPayloadException.java` 생성
  - [x] 2.2 `MiniAppKey.java` 값 객체 구현 (`^[a-z0-9_-]{1,64}$`)
  - [x] 2.3 `MiniAppStore.java` JPA 엔티티 구현 (`MINI_APP_STORE`: `app_key`, `data_json` CLOB, `updated_at`)
  - [x] 2.4 `MiniAppStoreRepository.java` 인터페이스 생성
  - _Requirements: 1.1, 1.2, 1.3_ / _Design: 3.1, 4.2_

- [x] 3. 도메인 단위/jqwik/JPA 슬라이스 테스트 작성 및 체크포인트 A
  - [x] 3.1 `MiniAppKeyPropertyTest.java` (jqwik 100 tries) 작성
  - [x] 3.2 `MiniAppStoreRepositoryTest.java` (`@DataJpaTest`) 작성
  - [x] 3.3 `mvn test -pl myshare -Dtest="MiniAppKeyPropertyTest,MiniAppStoreRepositoryTest"` 통과 확인
  - _Validates: Requirements 1.1, 1.2, 1.3_

---

### B. 애플리케이션 서비스 계층 (Application Layer)

- [x] 4. 애플리케이션 예외, 포트, 검증기 및 서비스 구현
  - [x] 4.1 `GitHubMiniAppSyncException.java`, `GitHubMiniAppSyncClient.java` 생성
  - [x] 4.2 `MiniAppJsonValidator.java` 구현 (크기 제한 + Jackson 3 `ObjectMapper.readTree()` + 후행 토큰 차단)
  - [x] 4.3 `MiniAppStoreService.java` 구현 (`loadJson` 미존재 시 `"{}"`, `@Transactional upsertJson`)
  - [x] 4.4 `MiniAppGitHubSyncService.java` 구현 (`@Async("miniAppGitHubSyncExecutor")`, 예외 로깅 후 삼킴)
  - _Requirements: 2.1~2.6, 4.1, 4.5_ / _Design: 3.2_

- [x] 5. 애플리케이션 계층 테스트 작성 및 체크포인트 B
  - [x] 5.1 `MiniAppJsonValidatorPropertyTest.java` (jqwik + 경계/크기초과/깨진JSON 단위 케이스) 작성
  - [x] 5.2 `MiniAppStoreServiceTest.java`, `MiniAppGitHubSyncServiceTest.java` 작성
  - [x] 5.3 `mvn test -pl myshare -Dtest="MiniAppJsonValidator*,MiniAppStoreServiceTest,MiniAppGitHubSyncServiceTest"` 통과 확인
  - _Validates: Requirements 2.1~2.6, 4.1, 4.5_

---

### C. 인프라 계층 (Infrastructure Layer)

- [x] 6. 미니앱 프로퍼티, `@Async` 설정 및 GitHub 푸시 클라이언트 구현
  - [x] 6.1 `MiniAppProperties.java` 구현 (`pin` 마스킹 `toString`, `maxPayloadBytes`, `github`)
  - [x] 6.2 `MiniAppConfiguration.java` 구현 (`@EnableAsync`, 단일 스레드 `miniAppGitHubSyncExecutor`, `MiniAppJsonValidator` 빈)
  - [x] 6.3 `GitHubMiniAppUpdateRequest.java`, `RestClientGitHubMiniAppSyncClient.java` 구현 (`sha` 조회, 404 신규 생성, Base64 PUT, 409 재시도)
  - _Requirements: 3.2, 4.2, 4.3, 4.4_ / _Design: 3.3_

- [x] 7. 인프라 계층 테스트 작성 및 체크포인트 C
  - [x] 7.1 `MiniAppPropertiesTest.java` 작성 (기본값 및 PIN 마스킹 검증)
  - [x] 7.2 `RestClientGitHubMiniAppSyncClientTest.java` (`MockRestServiceServer`) 작성 (200 갱신, 404 신규 생성, 409 재시도, 비활성화 스킵, 오류 예외)
  - [x] 7.3 `mvn test -pl myshare -Dtest="MiniAppPropertiesTest,RestClientGitHubMiniAppSyncClientTest"` 통과 확인
  - _Validates: Requirements 4.2, 4.3, 4.4_

---

### D. 웹 인터페이스 계층 (Interfaces Layer)

- [x] 8. PIN 인증 필터, REST 컨트롤러 및 JSON 예외 핸들러 구현
  - [x] 8.1 `MiniAppPinFilter.java` 구현 (`/api/mini/**` `X-Mini-Pin` 상수 시간 검증, 실패 시 401 JSON)
  - [x] 8.2 `MiniAppController.java` 구현 (`GET/PUT /api/mini/{appKey}`, 하위 경로/루트 400 차단)
  - [x] 8.3 `MiniAppExceptionHandler.java` 구현 & `ShareExceptionHandler.java` `@Order(LOWEST_PRECEDENCE)` 보완
  - _Requirements: 1.3, 2.1~2.6, 3.1~3.4, 4.1_ / _Design: 3.4_

- [x] 9. 웹 슬라이스 및 통합 테스트 작성 및 체크포인트 D
  - [x] 9.1 `MiniAppControllerTest.java` (`@WebMvcTest`) 작성 (`X-Mini-Pin` 401/통과, GET/PUT 정상·예외)
  - [x] 9.2 `MiniAppIntegrationTest.java` (`@SpringBootTest` + `MockMvc`) 작성 (DB upsert 후 GET 조회 + `@Async` 비동기 호출 + `/share/**` 무인증 확인)
  - [x] 9.3 `MyshareApplicationTest.java` (기존 테스트 및 `MiniAppController` 빈 등록 확인) 통과 확인
  - _Validates: Requirements 1~4_

---

### E. 전체 통합 검증 및 5대 품질 가드레일 (Quality Guardrails & Finalization)

- [x] 10. 5대 품질 가드레일 전체 파이프라인 검증
  - [x] 10.1 `mvn -B -q spotless:apply -pl myshare` 실행
  - [x] 10.2 `mvn -B clean install -pl myshare -am` 실행 (Spotless, Error Prone, ArchUnit, JaCoCo 80%+/70%+, PMD/CPD 통과)
  - _Requirements: Non-Functional 1_ / _Design: 6.2_

- [x] 11. CodeGraph 동기화 및 Memory Bank 갱신
  - [x] 11.1 `codegraph sync` 실행
  - [x] 11.2 `memory-bank/activeContext.md`에 `myshare/002-mini-app-store` 완료 내역(Compacted) 및 운영 DDL/PIN 안내 기록
  - _Workflow 규칙 준수_
