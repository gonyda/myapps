# Requirements Document: 015-user-authentication-and-login

> **폴더 위치**: `.kiro/specs/myrpg/015-user-authentication-and-login/requirements.md`  
> **관련 규칙**: `rules/project/spec-conventions.md`, `rules/project/tech-stack.md`, `rules/coding/code-style.md`

---

## 1. Introduction (개요 및 배경)

### 1.1. 배경 및 목적
- **현재 상태 및 문제점**:
  - 현재 MyRPG는 단일 캐릭터(`id=1`, "고니")를 전제로 동작하며, 인증 체계가 없어 브라우저가 `/`에 접근하면 무조건 첫 번째 캐릭터를 로드합니다.
  - 다양한 빌드(근접/궁술/마법/전스킬 마스터) 및 신규 기능 테스트를 위해 별도의 계정 분리 및 테스트용 풀세팅 어드민 계정이 필요합니다.
- **핵심 목표**:
  - `user_account` 테이블 및 간이 로그인/세션 시스템을 도입하여 다중 계정 체계를 구축합니다.
  - 기본 프리셋 계정인 `bbsk`(기존 "고니" 캐릭터 계승)와 `admin`(35종 전스킬 F랭크 습득 + 초보자 풀장비 착용/보유)을 자동 시드합니다.
  - 로그인 세션이 없는 사용자가 게임 화면(`/` 및 API)에 접근하면 `/login`으로 안전하게 리다이렉트합니다.
- **선행 스펙과의 연계**:
  - `001-character-progress-and-map-movement` ~ `014-skill-system-expansion`: 구축된 35종 스킬, 장비, 인벤토리, 던전, 전투 시스템을 계정별 `characterId` 기반으로 온전히 격리 구동합니다.

### 1.2. 이번 스펙의 범위 (In-Scope)
1. **UserAccount 도메인 모델 및 영속 계층**:
   - `UserAccount` JPA 엔티티 (`username`, `password`, `nickname`, `characterId`) 및 `UserAccountRepository`.
   - `OwnedItem` 엔티티에 `characterId` 추가로 계정별 인벤토리 및 장착 상태 완전 격리.
2. **초기 계정 및 캐릭터 자동 시드 (`AuthService.initDefaultAccounts`)**:
   - `bbsk` / `1` ➜ 기존 "고니" 캐릭터(`id=1`) 매핑.
   - `admin` / `1` ➜ "관리자" 신규 캐릭터 생성, **35종 전체 스킬 F랭크 일괄 습득**, **초보자 장비 6종 착용 + 4종 무기 및 포션 15개 인벤토리 지급**.
3. **인증 서비스 & 세션 인터셉터 (`AuthService`, `AuthInterceptor`)**:
   - `HttpSession` 기반 `UserSession` 관리.
   - `AuthInterceptor`를 통한 미인증 접근 제어 (`/login` 리다이렉트 및 AJAX `401 Unauthorized`).
4. **인증 웹 컨트롤러 (`AuthController`)**:
   - `GET /login`: 로그인 뷰 렌더링 (이미 로그인된 경우 `/` 리다이렉트).
   - `POST /login`: 자격증명 검증, 세션 발급 및 에러 처리.
   - `GET /logout`, `POST /logout`: 세션 만료 및 `/login` 리다이렉트.
5. **플레이 화면 및 전체 API 컨트롤러의 세션 캐릭터 연동**:
   - 컨트롤러가 세션의 `characterId`를 바탕으로 캐릭터/인벤토리/스킬/전투 데이터를 처리.
6. **프론트엔드 UI/UX**:
   - 다크 판타지 글래스모피즘 테마의 `login.html` 구현.
   - 원클릭 퀵 로그인 프리셋 뱃지 (`[bbsk (고니)]`, `[admin (전스킬+풀장비)]`) 제공.
   - 상단바(`top-bar.html`)에 접속자 닉네임 및 `[로그아웃]` 버튼 배치.

### 1.3. 제외 및 이연 범위 (Out-of-Scope / Deferred)
- 일반 사용자 회원가입(Sign-up) 폼 및 이메일 인증 기능 (관리자 DB 계정 관리 방식으로 한정).
- 비밀번호 암호화(BCrypt)는 본 간이 로그인 스펙에서는 평문 대조(`"1"`)로 단순화하며 향후 보안 고도화 시 이연.
- 다중 동시 접속자 간의 실시간 상호작용 (멀티플레이 네트워크)은 향후 백로그로 분리.

---

## 2. Glossary (용어 사전)

### 2.1. 기존 재사용 용어
- **`MyRPG`**: `com.myapps.web.myrpg` 패키지의 Spring Boot 4.0 턴제 RPG 모듈.
- **`CharacterProgress`**: 캐릭터의 레벨, 경험치, 스탯, 바이탈(HP/MP/Stamina), 현재 위치를 보관하는 JPA 엔티티.
- **`CharacterSkill`**: 캐릭터가 습득한 스킬의 랭크, 사용 횟수, 궁극기 쿨타임 등을 보관하는 JPA 엔티티.
- **`OwnedItem`**: 캐릭터가 소유한 장비/소비 아이템 인스턴스 JPA 엔티티.

### 2.2. 본 스펙 신규 용어
- **`User_Account` (`UserAccount`)**: 사용자 계정 정보를 보관하는 JPA 엔티티 (`username`, `password`, `nickname`, `characterId`).
- **`User_Session` (`UserSession`)**: `HttpSession`에 저장되는 불변 세션 인증 DTO (`userId`, `username`, `nickname`, `characterId`).
- **`Auth_Interceptor` (`AuthInterceptor`)**: HTTP 요청 시 로그인 세션 유무를 검사하여 미인증 요청을 차단/리다이렉트하는 Spring MVC 핸들러 인터셉터.
- **`Admin_Preset_Character`**: 35종 전체 스킬 F랭크 및 초보자 장비 풀세트를 장착/보유한 어드민 전용 테스트 캐릭터.
- **`Bbsk_Preset_Account`**: 기존 1번 "고니" 캐릭터 데이터를 계승하는 사용자 계정.

---

## 3. Requirements (기능 요구사항)

### Requirement 1: 사용자 계정 도메인 및 데이터 영속화

**User Story:**  
시스템 관리자로서, 사용자 계정 정보(`username`, `password`, `nickname`, `characterId`)를 DB에 안전하게 영속화하고 계정별 캐릭터와 인벤토리를 독립적으로 관리하고 싶다.

#### Acceptance Criteria
1. **THE** 시스템 **SHALL** `user_account` 테이블에 매핑되는 `UserAccount` 엔티티를 제공한다 (`id`, `username` UNIQUE, `password`, `nickname`, `characterId`).
2. **THE** `OwnedItem` 엔티티 **SHALL** `characterId` (Long, NOT NULL) 필드를 포함하여 계정별 아이템 소유권을 완벽히 격리한다.
3. **WHEN** `OwnedItemRepository`에서 아이템을 조회/삭제/저장할 때, **THE** 시스템 **SHALL** 해당 `characterId` 조건으로 필터링한다.

---

### Requirement 2: 기본 프리셋 계정 자동 초기화 (`bbsk`, `admin`)

**User Story:**  
플레이어 및 개발자로서, 서버 기동 시 사전 정의된 `bbsk`(기존 고니) 및 `admin`(전스킬+풀장비) 계정이 자동으로 준비되어 즉시 테스트할 수 있기를 원한다.

#### Acceptance Criteria
1. **WHEN** 애플리케이션이 기동(`ApplicationReadyEvent` 또는 `@PostConstruct`)되면, **THE** `AuthService` **SHALL** `bbsk` 및 `admin` 계정의 존재 여부를 검사하고 없으면 자동 생성한다.
2. **IF** `bbsk` 계정이 없으면, **THEN THE** `AuthService` **SHALL**:
   - 기존 "고니" 캐릭터(`id=1`)가 존재하면 해당 캐릭터를 연결하고, 없으면 기본 캐릭터를 생성하여 연결한다.
   - `UserAccount("bbsk", "1", "고니", goniChar.getId())`를 저장한다.
3. **IF** `admin` 계정이 없으면, **THEN THE** `AuthService` **SHALL**:
   - 닉네임 "관리자"의 신규 `CharacterProgress`를 생성한다.
   - 초보자 방어구 5종(방패·갑옷·투구·장갑·부츠)과 한손검을 기본 장착(`equipped=true`) 상태로 지급한다.
   - 양손검·활·완드·스태프 및 HP/MP/스태미나 포션 각 5개를 인벤토리에 지급한다.
   - **스킬 카탈로그에 존재하는 35종 전체 스킬을 F랭크(`CharacterSkill`)로 생성하여 일괄 지급한다**.
   - `UserAccount("admin", "1", "관리자", adminChar.getId())`를 저장한다.
4. **THE** 계정 비밀번호는 두 계정 모두 `"1"`로 설정된다.

---

### Requirement 3: 사용자 로그인 및 인증 세션 발급

**User Story:**  
사용자로서, 아이디와 비밀번호를 입력하여 로그인하고, 로그인 성공 시 게임 화면(`/`)으로 이동하여 자신의 캐릭터로 플레이하고 싶다.

#### Acceptance Criteria
1. **WHEN** 사용자가 `POST /login`으로 `username`과 `password`를 제출하면, **THE** `AuthService` **SHALL** 자격증명을 검증한다.
2. **IF** 자격증명이 일치하면, **THEN THE** `AuthController` **SHALL** `HttpSession`에 `LOGIN_USER` 키로 `UserSession` 객체를 등록하고 `/`로 리다이렉트(302)한다.
3. **IF** 아이디가 없거나 비밀번호가 일치하지 않으면, **THEN THE** `AuthController` **SHALL** HTTP 200과 함께 "아이디 또는 비밀번호가 일치하지 않습니다." 에러 메시지를 `login` 뷰에 렌더링한다.
4. **WHEN** 이미 로그인된 사용자가 `GET /login`에 접근하면, **THE** `AuthController` **SHALL** `/`로 즉시 리다이렉트한다.

---

### Requirement 4: 세션 기반 접근 제어 및 미인증 리다이렉트 (`AuthInterceptor`)

**User Story:**  
시스템으로서, 로그인하지 않은 사용자의 게임 플레이 화면 및 내부 API 무단 접근을 차단하고 로그인 화면으로 안내하고 싶다.

#### Acceptance Criteria
1. **WHEN** 미인증 사용자가 보호된 엔드포인트(예: `GET /`, `POST /move` 등)에 접근하면, **THE** `AuthInterceptor` **SHALL** 요청을 가로챈다.
2. **IF** 브라우저 일반 페이지 요청(HTML)인 경우, **THEN THE** `AuthInterceptor` **SHALL** `/login`으로 리다이렉트(302)한다.
3. **IF** AJAX/JSON API 요청(`Accept: application/json` 또는 `X-Requested-With: XMLHttpRequest`)인 경우, **THEN THE** `AuthInterceptor` **SHALL** `401 Unauthorized` 상태코드를 반환한다.
4. **THE** `AuthInterceptor` **SHALL** 정적 자원(`/css/**`, `/js/**`, `/images/**`, `/favicon.ico`) 및 로그인/로그아웃 경로(`/login`, `/logout`), H2 콘솔(`/h2-console/**`)을 검사에서 제외한다.

---

### Requirement 5: 로그아웃 처리

**User Story:**  
사용자로서, 플레이 중 언제든지 로그아웃하여 세션을 종료하고 로그인 화면으로 돌아가고 싶다.

#### Acceptance Criteria
1. **WHEN** 사용자가 `GET /logout` 또는 `POST /logout`을 호출하면, **THE** `AuthController` **SHALL** 현재 `HttpSession`을 완전히 무효화(`session.invalidate()`)한다.
2. **THE** `AuthController` **SHALL** 로그아웃 처리 후 `/login`으로 리다이렉트(302)한다.
3. **THE** 상단바(`top-bar.html`) **SHALL** 접속 중인 유저 닉네임과 함께 `[로그아웃]` 링크/버튼을 제공한다.

---

### Requirement 6: UI/UX 및 빠른 로그인 편의 기능

**User Story:**  
플레이어로서, 직관적이고 세련된 판타지풍 로그인 화면을 이용하고, 테스트 시 원클릭으로 계정을 선택하여 빠르게 접속하고 싶다.

#### Acceptance Criteria
1. **THE** `login.html` **SHALL** MyRPG의 다크 판타지 테마(글래스모피즘, 골드/크림슨 포인트, Inter 폰트)를 적용한 로그인 카드 UI를 렌더링한다.
2. **THE** `login.html` **SHALL** `[👤 bbsk (고니)]` 및 `[👑 admin (전스킬+풀장비)]` 퀵 프리셋 버튼을 제공한다.
3. **WHEN** 퀵 프리셋 버튼을 클릭하면, **THE** 클라이언트 스크립트 **SHALL** 아이디/비밀번호 필드를 자동으로 채우고 즉시 로그인을 진행한다.

---

## 4. Non-Functional & Quality Requirements (비기능 및 품질 요구사항)

1. **5대 품질 가드레일 (Task 완료 필수 기준)**:
   - **Spotless**: Java 포맷팅 자동 교정 (`mvn spotless:apply`).
   - **Error Prone**: 컴파일 타임 결함 및 경고 0건.
   - **ArchUnit**: DDD 4계층(`interfaces` → `application` → `domain`) 의존성 규칙 엄격 준수.
   - **JaCoCo**: 신규 인증/세션/인터셉터 코드 대상 라인 커버리지 80% 이상 달성.
   - **PMD & CPD**: 복잡도 및 안티패턴 0건.
2. **하위 호환성 및 기존 테스트 보존**:
   - 세션 객체가 없는 Mock 단위 테스트 환경에서도 `CharacterService`가 정상 동작하도록 방어적 설계 유지.
3. **CodeGraph 동기화**:
   - 코드베이스 변경 후 `codegraph sync` 필수 수행.
