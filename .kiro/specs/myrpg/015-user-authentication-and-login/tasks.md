# Implementation Plan: 015-user-authentication-and-login

> **폴더 위치**: `.kiro/specs/myrpg/015-user-authentication-and-login/tasks.md`  
> **관련 규칙**: `rules/workflow/task-build-validation.md`, `rules/workflow/git-workflow.md`

---

## Overview

본 작업 명세는 `myrpg` Web 모듈(`com.myapps.web.myrpg`)에 **간이 로그인 및 세션 기반 사용자 인증 시스템**을 점진적으로 구현하기 위한 체크포인트 단위 작업 목록이다.

### 구현 순서 및 원칙
1. **Bottom-Up 계층 조립**:  
   **A. 데이터/도메인(엔티티·리포지토리·PBT)** $\rightarrow$ **B. 애플리케이션 서비스(인증·시드·인벤토리 분리)** $\rightarrow$ **C. 웹 컨트롤러 & 인터셉터(엔드포인트·리다이렉트)** $\rightarrow$ **D. 프론트엔드(Thymeleaf·CSS·JS)** $\rightarrow$ **E. 5대 가드레일 통합 검증** 순으로 구현한다.
2. **원자적 완료 및 빌드 그린**:  
   각 태스크 완료 시점에 빌드 및 단위 테스트가 반드시 통과(`BUILD SUCCESS`)해야 한다.
3. **5대 품질 가드레일 준수**:  
   Spotless(포맷팅) $\rightarrow$ Error Prone(정적 결함) $\rightarrow$ ArchUnit(계층 아키텍처) $\rightarrow$ JaCoCo(커버리지 80%+) $\rightarrow$ PMD/CPD(복잡도/중복)를 필수 검증한다.

---

## Tasks

### A. 도메인 엔티티 & 리포지토리 계층 (Data & Domain Layer)

- [x] 1. `UserAccount` 도메인 엔티티 및 DTO 구현
  - [x] 1.1 `com.myapps.web.myrpg.domain.model.UserAccount.java`[신규] 생성 (`id`, `username`, `password`, `nickname`, `characterId`)
  - [x] 1.2 `com.myapps.web.myrpg.application.dto.UserSession.java`[신규] 불변 세션 Record 정의
  - [x] 1.3 `com.myapps.web.myrpg.application.dto.LoginRequest.java`[신규] 로그인 폼 바인딩 Record 정의
  - [x] 1.4 `com.myapps.web.myrpg.application.exception.AuthenticationException.java`[신규] 커스텀 예외 정의
  - _Requirements: 1.1, 3.1_ / _Design: 3.2, 4.1_

- [x] 2. `OwnedItem` 엔티티 `characterId` 확장 및 리포지토리 갱신
  - [x] 2.1 `OwnedItem.java`에 `characterId` 컬럼 추가 및 생성자/접근자 확장
  - [x] 2.2 `OwnedItemRepository.java`에 `findByCharacterIdAndStorage`, `findByCharacterId` 등 캐릭터별 쿼리 메서드 정의
  - _Requirements: 1.2, 1.3_ / _Design: 4.2_

- [x] 3. `UserAccountRepository` 생성 및 도메인 단위/PBT 테스트 작성
  - [x] 3.1 `com.myapps.web.myrpg.domain.repository.UserAccountRepository.java`[신규] JPA 인터페이스 생성
  - [x] 3.2 `UserAccountRepositoryTest.java`[신규] — 계정 저장 및 `findByUsername` 검증
  - [x] 3.3 `UserAccountPropertyTest.java`[신규, jqwik] — **Property 1** 자격증명 일치성 불변식 검증
  - _Validates: Requirements 1.1, 3.1_

- [x] 4. **체크포인트 A** — 도메인 계층 빌드 & 단위 테스트 검증
  - `mvn test -pl myrpg -Dtest="UserAccount*,OwnedItem*"` 통과 확인

---

### B. 애플리케이션 서비스 계층 (Application Service Layer)

- [x] 5. `AuthService` 구현 (인증 및 기본 계정 자동 시드)
  - [x] 5.1 `com.myapps.web.myrpg.application.service.AuthService.java`[신규] 생성
  - [x] 5.2 `initDefaultAccounts()` 구현:
    - `bbsk` 계정 검사 및 기존 "고니" 캐릭터(`id=1`) 매핑
    - `admin` 계정 검사 및 "관리자" 캐릭터 생성, **35종 전스킬 F랭크 일괄 습득**, **초보자 장비 풀세트(6종 착용+4종 보유+포션 15개) 지급**
  - [x] 5.3 `authenticate(username, password)` 자격증명 검증 로직 구현
  - _Requirements: 2.1, 2.2, 2.3, 3.1_ / _Design: 3.2_

- [x] 6. `CharacterService` 및 `InventoryService`의 `characterId` 연동 확장
  - [x] 6.1 `CharacterService.java`: `loadByCharacterId(Long characterId)` 추가
  - [x] 6.2 `InventoryService.java`: `characterId`를 파라미터로 받아 해당 캐릭터의 인벤토리/장비/은행 조작하도록 확장 (하위 호환성 유지)
  - _Requirements: 1.3, 2.3_ / _Design: 3.2_

- [x] 7. 서비스 단위 및 PBT 테스트 작성
  - [x] 7.1 `AuthServiceTest.java`[신규] — `bbsk`/`admin` 로그인 성공 및 실패 시나리오 Mock 검증
  - [x] 7.2 `AdminPresetPropertyTest.java`[신규, jqwik] — **Property 2, 3** 어드민 35종 스킬 무결성 및 인벤토리 격리 검증
  - _Validates: Requirements 2.1, 2.3, 3.1_

- [x] 8. **체크포인트 B** — 서비스 계층 빌드 & 단위 테스트 검증
  - `mvn test -pl myrpg -Dtest="AuthService*,CharacterService*,InventoryService*"` 통과 확인

---

### C. 웹 컨트롤러 & 인터셉터 계층 (Web Controller & Interceptor Layer)

- [x] 9. `AuthInterceptor` 및 `WebMvcConfig` 구현
  - [x] 9.1 `com.myapps.web.myrpg.infrastructure.interceptor.AuthInterceptor.java`[신규] 생성 (`preHandle` 세션 검사, HTML 302 리다이렉트, AJAX 401 에러)
  - [x] 9.2 `com.myapps.web.myrpg.infrastructure.config.WebMvcConfig.java`[신규] 생성 (인터셉터 등록 및 정적 자원/로그인 화이트리스트 등록)
  - _Requirements: 4.1, 4.2, 4.3, 4.4_ / _Design: 3.3_

- [x] 10. `AuthController` 구현
  - [x] 10.1 `com.myapps.web.myrpg.interfaces.api.AuthController.java`[신규] 생성 (`GET /login`, `POST /login`, `GET/POST /logout`)
  - [x] 10.2 로그인 성공 시 세션 등록 및 리다이렉트, 실패 시 에러 메시지 뷰 반환
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 5.1, 5.2_ / _Design: 3.1_

- [x] 11. `PlayScreenController` 및 주요 API 컨트롤러의 세션 캐릭터 연동
  - [x] 11.1 컨트롤러에서 세션의 `UserSession.characterId`를 조회하여 해당 캐릭터 데이터 로드
  - _Requirements: 1.1, 3.2_ / _Design: 3.1_

- [x] 12. 웹 컨트롤러 및 인터셉터 슬라이스 테스트 작성
  - [x] 12.1 `AuthControllerTest.java`[신규] — 로그인 폼 렌더, 로그인 성공/실패, 로그아웃 동작 검증
  - [x] 12.2 `AuthInterceptorTest.java`[신규] — 세션 유무에 따른 리다이렉트 및 401 응답 검증
  - _Validates: Requirements 3.1, 4.1, 5.1_

- [x] 13. **체크포인트 C** — 컨트롤러 계층 빌드 & 웹 테스트 검증
  - `mvn test -pl myrpg -Dtest="AuthController*,AuthInterceptor*"` 통과 확인

---

### D. 프론트엔드 UI/UX (Thymeleaf & CSS & JS)

- [x] 14. `login.html` 뷰 템플릿 마크업 작성
  - [x] 14.1 `src/main/resources/templates/login.html`[신규] 생성
  - [x] 14.2 다크 판타지 글래스모피즘 로그인 카드, 아이디/비번 폼, 에러 배너 마크업
  - [x] 14.3 `[👤 bbsk (고니)]`, `[👑 admin (전스킬+풀장비)]` 퀵 로그인 버튼 마크업
  - _Requirements: 6.1, 6.2_ / _Design: 5.1_

- [x] 15. 상단바(`top-bar.html`) 로그아웃 버튼 마크업
  - [x] 15.1 `src/main/resources/templates/fragments/top-bar.html` 상단 우측에 `[로그아웃]` 링크 버튼 추가
  - _Requirements: 5.3_ / _Design: 5.2_

- [x] 16. `myrpg.css` 스타일링 및 `myrpg.js` 퀵 로그인 스크립트 작성
  - [x] 16.1 `src/main/resources/static/css/myrpg.css`에 `.login-wrapper`, `.login-card`, `.quick-login-btn`, `.btn-logout` 스타일 추가
  - [x] 16.2 `src/main/resources/static/js/myrpg.js`에 퀵 로그인 원클릭 폼 채우기 & 제출 스크립트 연동
  - _Requirements: 6.1, 6.3_ / _Design: 5.1, 5.2_

- [x] 17. 프론트엔드 뷰 렌더링 통합 테스트 작성
  - [x] 17.1 `LoginViewIntegrationTest.java`[신규] — `GET /login` 및 퀵 프리셋 UI 렌더링 검증
  - _Validates: Requirements 6.1, 6.2_

- [x] 18. **체크포인트 D** — UI/UX 빌드 및 통합 테스트 검증
  - `mvn test -pl myrpg -Dtest="*IntegrationTest,*SmokeTest"` 통과 확인

---

### E. 5대 품질 가드레일 & 지식 그래프 동기화

- [x] 19. 5대 품질 가드레일 전체 빌드 검증
  - [x] 19.1 `mvn -B -q spotless:apply -pl myrpg && (mvn -B clean install -pl myrpg -am > /tmp/mvn.log 2>&1 || (tail -n 30 /tmp/mvn.log && exit 1)) && tail -n 12 /tmp/mvn.log` 실행
  - [x] 19.2 Spotless, Error Prone, ArchUnit, JaCoCo(80%+), PMD/CPD 위반 0건 확인
  - _Requirements: 4.1_ / _Design: 6.1_

- [x] 20. CodeGraph 및 Memory Bank 동기화
  - [x] 20.1 `codegraph sync` 실행하여 지식 그래프 갱신
  - [x] 20.2 `memory-bank/activeContext.md`에 015 스펙 작업 완료 상태 갱신
