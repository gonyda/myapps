# Requirements Document: 메시지 및 게임 프로퍼티 외부화 리팩토링 (018-message-and-properties-externalization)

> **폴더 위치 가이드**: `.kiro/specs/myrpg/018-message-and-properties-externalization/requirements.md`  
> **관련 문서**: `docs/hardcoding-analysis-and-refactoring-plan.md`, `rules/project/spec-conventions.md`, `rules/project/tech-stack.md`

---

## 1. Introduction (개요 및 배경)

### 1.1. 배경 및 목적
- **현재 상태 및 문제점**: 현재 MyRPG는 아이템 카탈로그(`item.json`), 몬스터(`monster.json`), 스킬(`skill.json`) 등은 JSON으로 성공적으로 분리했으나, **하단 활동 로그(ActionLog)**, **전투 공방 로그(BattleLogFormatter + BattleService 인라인)**, **유저 노출 비즈니스 예외**, **프론트엔드 `myrpg.js` alert/toast**, **게임 밸런스 상수(확률, 스태미나 소모량, 슬롯 제한, 전투 계수 등)**가 여전히 Java/JS 소스 코드 내에 하드코딩되어 있습니다.
- **핵심 목표**:
  1. `messages.properties`와 `GameMessageService`를 도입하여 모든 인게임 메시지/로그를 단일 진실 공급원(SSOT)으로 통합 관리.
  2. 서버(ActionLog/Battle)와 클라이언트(`myrpg.js`)가 동일 상황에서 동일한 메시지를 바라보도록 일원화.
  3. 전투 턴 공방 템플릿 중복을 약 50% 슬림화(~40건 → ~20건).
  4. `GameProperties`(`@ConfigurationProperties`) 및 `application-game.yml`을 통해 코드 재컴파일 없이 게임 밸런스 수치를 튜닝할 수 있는 기반 구축.
- **선행 스펙과의 연계**: `017-firewood-gathering`(장작 채집) 및 `013-active-telegraph-combat`(전투 시스템)을 포함한 전역 인게임 시스템의 하드코딩 요소를 외부화합니다.

### 1.2. 이번 스펙의 범위 (In-Scope)
1. **메시지 인프라 (`messages.properties` + `GameMessageService`)**:
   - Spring Boot 표준 `MessageSource` 기반 한국어 메시지 관리.
   - 활동 로그(26건), 전투 인라인 로그(15건), 전투 턴 공방 로그(~20건), 장비 포맷팅/유저 예외(8건) 외부화.
2. **게임 밸런스 설정 (`GameProperties` + `application-game.yml`)**:
   - 채집, 인벤토리, 마을(치료/수리), 전투 계수/확률, 성장/이동 등 50여 개 매직 넘버를 `@ConfigurationProperties(prefix = "game")`로 분리.
3. **서버-프론트엔드 메시지 일원화**:
   - `myrpg.js` 내 하드코딩된 alert/toast 문구를 제거하고 서버 메시지 및 전역 메시지 맵으로 대체.
4. **품질 검증**:
   - jqwik 프로퍼티 기반 테스트를 통한 메시지 포맷팅 및 프로퍼티 로딩 불변식 검증, 5대 품질 가드레일 100% 통과.

### 1.3. 제외 및 이연 범위 (Out-of-Scope / Deferred)
- **개발자 디버깅용 방어 코드 외부화 제외**: `IllegalArgumentException`, `IllegalStateException`, 카탈로그 JSON 파싱 에러 메시지 등 순수 내부 개발용 예외는 코드 인라인 유지.
- **JSON 데이터 카탈로그 스키마 변경 제외**: `item.json`, `monster.json`, `skill.json`의 구조 자체는 변경하지 않음.

---

## 2. Glossary (용어 사전)

### 2.1. 기존 재사용 용어
- **`ActionLog`**: 화면 하단에 최근 10개의 플레이어 활동(아이템 획득, 레벨업, 이동 등)을 표시하는 도메인 모델.
- **`BattleLogFormatter`**: 가위바위보 상성 공방 결과(단일/다단 타격, 방어, 반격 등)를 텍스트로 조립하는 렌더러.
- **`BattleService`**: 전투 턴 진행, 스킬 발동, 자원 소모, 승패 및 보상 처리를 담당하는 애플리케이션 서비스.
- **`GatheringService`**: 장작 채집 롤, 스태미나 소모 및 인벤토리 적재를 담당하는 애플리케이션 서비스.

### 2.2. 본 스펙 신규 용어 (`Pascal_Snake_Case`)
- **`Game_Message_Service`**: `MessageSource`를 래핑하여 프로퍼티 키와 파라미터로 완성된 메시지를 반환하는 헬퍼 서비스 (`GameMessageService`).
- **`Game_Properties`**: `application-game.yml`에 정의된 게임 밸런스 수치들을 바인딩하는 불변 Record 클래스 (`GameProperties`).
- **`Messages_SSOT`**: `src/main/resources/messages.properties` 파일에 위치한 전체 텍스트 단일 진실 공급원.

---

## 3. Requirements (기능 요구사항)

### Requirement 1: 메시지 시스템 인프라 및 활동 로그(ActionLog) 외부화

**User Story:**  
시스템 개발자 및 기획자로서, 인게임 활동 로그 문구를 Java 코드 수정 없이 `messages.properties`에서 일괄 수정하고 싶다.  
그래야 텍스트 변경이나 다국어 지원 시 코드 변경 및 재빌드 부담을 최소화할 수 있다.

#### Acceptance Criteria
1. **WHEN** `GameMessageService.get(code, args)`가 호출되면, **THE** 시스템 **SHALL** `messages.properties`에 정의된 템플릿에 인자를 바인딩하여 한국어 로케일 포맷 문자열을 반환한다.
2. **WHEN** 플레이어가 채집 성공/실패, 아이템 구매/판매, 포션 사용, 던전 입장/클리어, 레벨업/승급을 수행하면, **THE** 시스템 **SHALL** `messages.properties`에 정의된 키(`log.*`)를 통해 활동 로그(`ActionLog`)를 생성한다.
3. **THE** 채집 성공/실패 로그는 특정 도구/아이템에 종속되지 않는 범용 템플릿(`log.gathering.success=[채집] {0} 획득!`, `log.gathering.failure=[채집] 채집에 실패했습니다.`)을 사용한다.
4. **IF** 정의되지 않은 메시지 키가 전달되면, **THEN THE** `GameMessageService` **SHALL** `NoSuchMessageException` 대신 안전하게 키 이름 자체 또는 기본 대체 문구를 반환하여 서버 에러를 방지한다.

---

### Requirement 2: 전투 인라인 및 턴 공방 로그 외부화 & 템플릿 최적화

**User Story:**  
플레이어 및 개발자로서, 전투 공방 로그와 스킬 효과 로그가 표준화된 템플릿으로 명확하게 렌더링되기를 원한다.  
그래야 전투 상황을 직관적으로 이해하고 텍스트 톤앤매너를 일관되게 유지할 수 있다.

#### Acceptance Criteria
1. **WHEN** 전투 중 스킬 캐스팅 실패, 힐, 기절/빙결, DoT 피해, 메디테이션, 보상 획득, 사망/부활이 발생하면, **THE** `BattleService` **SHALL** `messages.properties`의 `battle.*` 키를 통해 인라인 로그를 생성한다.
2. **WHEN** `BattleLogFormatter`가 턴 공방 결과를 조립할 때, **THE** 시스템 **SHALL** 선제/일반 타격 공통 템플릿(`battle.attack.multi`, `battle.attack.single` 등 약 20종)을 사용하여 텍스트를 생성한다.
3. **THE** 시스템 **SHALL** 기존 공방 로그의 의미와 포맷(피해량 표기, 화살표 `➔`, 이모지 등)을 100% 보존한다.

---

### Requirement 3: 장비 상세 포맷팅 및 유저 노출 비즈니스 예외 외부화

**User Story:**  
플레이어로서, 장비 착용 불가나 골드/스태미나 부족 등의 오류 상황에서 일관되고 명확한 피드백 메시지를 받고 싶다.

#### Acceptance Criteria
1. **WHEN** 인벤토리에서 장비 상세 정보(`InventoryService.describe`)를 조회하면, **THE** 시스템 **SHALL** 종류/타입, 내구도 문구를 `describe.equip.*` 키를 통해 생성한다.
2. **WHEN** 장착 충돌(`EquipConflictException`), 골드 부족(`InsufficientGoldException`), 스태미나 부족(`InsufficientStaminaException`), 인벤토리 가득 참(`InventoryFullException`) 등의 비즈니스 예외가 발생하면, **THE** 시스템 **SHALL** `exception.*` 키의 메시지를 클라이언트에 전달한다.
3. **WHERE** 내부 유효성 검증용 예외(`IllegalArgumentException`, JSON 파싱 예외 등)의 경우, **THE** 시스템 **SHALL** 소스 코드 내 인라인 메시지를 유지하여 디버깅 용이성을 확보한다.

---

### Requirement 4: 게임 밸런스 프로퍼티 외부화 (`GameProperties`)

**User Story:**  
게임 기획자로서, 채집 확률, 수리 성공률, 전투 계수, 이동 시간 등 밸런스 수치를 코드 수정 없이 YAML 설정 파일에서 즉시 변경하고 싶다.

#### Acceptance Criteria
1. **THE** 시스템 **SHALL** `application-game.yml`에 `game.*` 프리픽스로 밸런스 수치를 정의하고, `@ConfigurationProperties(prefix = "game")`를 통해 불변 Record `GameProperties`로 바인딩한다.
2. **THE** `GameProperties` **SHALL** `gathering`, `inventory`, `town`, `battle`, `progression`, `movement`의 6대 도메인 그룹으로 플랫하게 구성된다.
3. **WHEN** 서비스 클래스(`GatheringService`, `InventoryService`, `BattleService`, `HealController`, `RepairController` 등)가 실행될 때, **THE** 시스템 **SHALL** 하드코딩된 `static final` 상수 대신 주입된 `GameProperties` 값을 참조한다.

---

### Requirement 5: 프론트엔드(`myrpg.js`) 메시지 SSOT 연동

**User Story:**  
플레이어로서, 웹 브라우저에서 조작할 때 서버와 브라우저 알림창의 문구가 서로 일치하여 혼선이 없기를 원한다.

#### Acceptance Criteria
1. **THE** 프론트엔드 `myrpg.js` **SHALL** 소스 코드 내의 한국어 하드코딩 문자열(alert, toast, fallback 문구 23건)을 제거한다.
2. **WHEN** 비동기 요청(채집, 구매, 판매, 수리 등)의 응답을 수신하면, **THE** `myrpg.js` **SHALL** 서버가 내려준 `res.message`를 표시한다.
3. **WHEN** 클라이언트 측 즉각 검증(전투 중 이동 방어, 입력값 검증 등)을 수행할 때, **THE** `myrpg.js` **SHALL** 뷰에 주입된 전역 메시지 객체(`window.GAME_MESSAGES`)를 참조한다.

---

## 4. Non-Functional & Quality Requirements (비기능 및 품질 요구사항)

1. **5대 품질 가드레일 (Task 완료 필수 기준)**:
   - **Spotless**: Java 포맷팅 자동 교정 (`mvn spotless:apply`).
   - **Error Prone**: 컴파일 경고 0건 (`-Werror`).
   - **ArchUnit**: DDD 계층 아키텍처 규칙 준수 (config/support 패키지 의존성 검증).
   - **JaCoCo**: 신규/수정 코드 대상 라인 커버리지 80% 이상 달성.
   - **PMD & CPD**: 복잡도 및 중복 코드 0건.
2. **프로퍼티 검증 (jqwik)**:
   - 모든 정의된 메시지 키의 포맷팅 정확성 검증 (Property-based Testing).
   - `GameProperties`의 유효 범위 검증 (확률 0~100, 양수 등).
3. **CodeGraph 동기화**:
   - 코드 변경 완료 후 `codegraph sync` 필수 수행.
