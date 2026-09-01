# Implementation Plan: {기능명}

> **폴더 위치 가이드**: `.kiro/specs/{모듈명}/{3자리순번}-{기능명}/tasks.md`  
> **관련 규칙**: `rules/workflow/task-build-validation.md`, `rules/workflow/git-workflow.md`

---

## Overview

본 작업 명세는 `{모듈명}` Web 모듈(`com.myapps.web.{모듈명}`)에 `{기능명}`을 점진적으로 구현하기 위한 체크포인트 단위 작업 목록이다.

### 구현 순서 및 원칙
1. **Bottom-Up 계층 조립**:  
   **A. 데이터/도메인(엔티티·DTO·순수 계산)** $\rightarrow$ **B. 애플리케이션 서비스(트랜잭션·오케스트레이션)** $\rightarrow$ **C. 웹 컨트롤러(엔드포인트·뷰 헬퍼)** $\rightarrow$ **D. 프론트엔드(Thymeleaf·CSS·JS)** $\rightarrow$ **E. 5대 가드레일 통합 검증** 순으로 구현한다.
2. **원자적 완료 및 빌드 그린**:  
   기존 코드 확장 시 영향받는 호출부와 테스트를 함께 수정하여 각 단계 완료 시점에 빌드가 반드시 그린(`BUILD SUCCESS`)이어야 한다.
3. **5대 품질 가드레일 준수**:  
   Spotless(포맷팅) $\rightarrow$ Error Prone(정적 결함) $\rightarrow$ ArchUnit(계층 아키텍처) $\rightarrow$ JaCoCo(라인 80%+·브랜치 70%+) $\rightarrow$ PMD/CPD(복잡도/중복)를 필수 검증한다.
4. **테스트 필수 & Given-When-Then**:  
   기능 구현 Task는 테스트 코드를 함께 작성하며, 모든 테스트는 `// given` / `// when` / `// then` 3단계와 `should_{동작}_when_{조건}` 네이밍을 준수한다. (Spring Boot 4.0: `@MockitoBean`, `@WebMvcTest`/`@DataJpaTest` 신규 패키지, Jackson 3 `tools.jackson` 사용)

---

## Tasks

### A. 도메인 엔티티 & DTO 확장 (Data & Domain Layer)

- [ ] 1. 도메인 모델 및 JPA 엔티티 구현
  - [ ] 1.1 `{EntityName}.java` 신규 생성 또는 필드 확장
  - [ ] 1.2 Getter/Setter 및 도메인 비즈니스 메서드 구현
  - _Requirements: 1.1_ / _Design: 4.1_

- [ ] 2. DTO 및 Value Object (`record`) 정의
  - [ ] 2.1 `{Domain}View.java` 뷰 렌더링용 불변 Record 정의
  - [ ] 2.2 `{Action}Result.java` 작업 결과 sealed Record 정의
  - _Requirements: 1.2_ / _Design: 4.2_

- [ ] 3. 도메인 순수 계산 엔진 및 정책 구현
  - [ ] 3.1 `{Domain}Resolver.java` 또는 `{Domain}Policy.java` 순수 로직 구현
  - _Requirements: 2.1_ / _Design: 3.3_

- [ ] 4. 도메인 단위 및 jqwik 프로퍼티 테스트 작성
  - [ ] 4.1 `{Domain}ResolverTest.java`[신규] — 단위 기능 검증
  - [ ] 4.2 `{Domain}ResolverPropertyTest.java`[신규, jqwik] — **Property 1, 2** 불변식 검증
  - _Validates: Requirements 1.1, 2.1_

- [ ] 5. **체크포인트 A** — 도메인 계층 빌드 & 단위 테스트 검증
  - `mvn test -pl {modulename} -Dtest="{Domain}*"` 통과 확인

---

### B. 애플리케이션 서비스 로직 확장 (Application Service Layer)

- [ ] 6. JSON 카탈로그 로더 및 데이터 정의 (필요 시)
  - [ ] 6.1 `src/main/resources/data/{catalog}.json` 데이터 추가/수정
  - [ ] 6.2 `{Catalog}CatalogService.java` 파싱, 캐싱 및 무결성 검증 로직 구현
  - _Requirements: 3.1_ / _Design: 4.3_

- [ ] 7. 비즈니스 서비스 오케스트레이션 구현
  - [ ] 7.1 `{Domain}Service.java` 생성/확장 (`@Transactional`, 리포지토리/도메인 엔진 연동)
  - [ ] 7.2 예외 처리 및 방어적 폴백 로직 구현
  - _Requirements: 2.2, 3.2_ / _Design: 3.2_

- [ ] 8. 서비스 단위 테스트 작성
  - [ ] 8.1 `{Domain}ServiceTest.java`[신규/확장] — 비즈니스 시나리오 및 예외 케이스 Mock 검증
  - _Validates: Requirements 2.2, 3.2_

- [ ] 9. **체크포인트 B** — 서비스 계층 빌드 & 단위 테스트 검증
  - `mvn test -pl {modulename} -Dtest="{Domain}Service*,{Catalog}*"` 통과 확인

---

### C. 웹 컨트롤러 계층 구현 (Web Controller Layer)

- [ ] 10. 웹 컨트롤러 엔드포인트 구현
  - [ ] 10.1 `{Domain}Controller.java` 엔드포인트 구현 (`GET`, `POST`)
  - [ ] 10.2 `{Domain}ViewHelper.java`를 통한 뷰 모델 조립 및 전달
  - _Requirements: 1.1, 4.1_ / _Design: 3.1_

- [ ] 11. 웹 컨트롤러 슬라이스 테스트 작성
  - [ ] 11.1 `{Domain}ControllerTest.java`[신규/확장] — `MockMvc` 기반 HTTP 요청/응답 및 모델 속성 검증
  - _Validates: Requirements 1.1, 4.1_

- [ ] 12. **체크포인트 C** — 컨트롤러 계층 빌드 & 웹 테스트 검증
  - `mvn test -pl {modulename} -Dtest="{Domain}Controller*"` 통과 확인

---

### D. 프론트엔드 UI/UX (Thymeleaf & JS & CSS)

- [ ] 13. Thymeleaf 템플릿 마크업 구현
  - [ ] 13.1 `src/main/resources/templates/fragments/{view}.html` 프래그먼트 작성/수정
  - [ ] 13.2 접근성, 시맨틱 태그 및 동적 바인딩 속성 적용
  - _Requirements: 4.1_ / _Design: 3.4_

- [ ] 14. CSS 스타일링 및 인터랙션 정의
  - [ ] 14.1 `src/main/resources/static/css/{module}.css` 스타일 클래스 및 `:root` 토큰 활용
  - _Requirements: 4.2_ / _Design: 3.4_

- [ ] 15. JavaScript 이벤트 및 비동기 통신 구현
  - [ ] 15.1 `src/main/resources/static/js/{module}.js` AJAX 호출 및 DOM 부분 교체 함수 작성
  - [ ] 15.2 불필요한 `alert()` 제거 및 인게임 UI/활동 로그 피드백 연동
  - _Requirements: 4.3_ / _Design: 3.4_

- [ ] 16. UI 및 자바스크립트 회귀 테스트 검증
  - [ ] 16.1 기존 UI/JS 보존 회귀 테스트(예: `{Module}VisualPreservationIntegrationTest.java`) 실행하여 프래그먼트/정적 리소스 무결성 확인
  - _Validates: Requirements 4.1, 4.2, 4.3_

- [ ] 17. **체크포인트 D** — 프론트엔드 연동 빌드 & 뷰 테스트 검증
  - `mvn test -pl {modulename}` 전체 단위/통합 테스트 통과 확인

---

### E. 전체 통합 검증 및 5대 품질 가드레일 (Integration & Quality Guardrails)

- [ ] 18. 5대 품질 가드레일 전체 파이프라인 검증
  - [ ] 18.1 `mvn -B -q spotless:apply -pl {modulename}` — 소스 포맷팅 자동 교정
  - [ ] 18.2 `mvn -B clean install -pl {modulename} -am` — 컴파일, 아키텍처, 커버리지(80%+), PMD/CPD 전수 검증
  - _Requirements: 5.1_ / _Design: 6.2_

- [ ] 19. CodeGraph 인덱스 동기화
  - [ ] 19.1 `codegraph sync` 실행하여 최신 심볼 및 호출 관계 인덱스 갱신
  - _Workflow 규칙 준수_

- [ ] 20. Memory Bank 갱신 (Compaction)
  - [ ] 20.1 `memory-bank/activeContext.md`에 완료 내역 요약 및 다음 단계 기록
  - _AGENTS.md 규칙 준수_
