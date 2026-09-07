# Requirements Document: BattleService God Class 분해 (Battle Service Decomposition)

> **폴더 위치**: `.kiro/specs/myrpg/022-battle-service-decomposition/requirements.md`
> **관련 규칙**: `rules/project/spec-conventions.md`, `rules/project/tech-stack.md`, `rules/coding/code-style.md`
> **분석 출처**: `docs/myrpg-refactoring-candidates.md` (🟡 3순위)

---

## 1. Introduction (개요 및 배경)

### 1.1. 배경 및 목적

- **현재 상태 및 문제점**:
  `BattleService`(`application/service/BattleService.java`)는 약 **1,752줄**이며 생성자로 **의존성 19개**
  (`BattleStateRepository`, `BattleResolver`, `MonsterService`, `MonsterAiService`, `MonsterRewardService`,
  `SkillService`, `SkillDamagePolicy`, `BattleLogFormatter`, `InventoryService`, `ProgressionService`,
  `CharacterService`, `StatProgression`, `ActionLog`, `Random`, `SkillCatalogService`,
  `CharacterSkillRepository`, `ItemCatalogService`, `DungeonService`, `GameMessageService`,
  `GameProperties`)를 받는 God Class다. `takeTurn`을 중심으로 서로 다른 관심사가 한 클래스에 집중돼 있다.
  - **전투 스탯/자원 해석**: 공격력·주스탯·계수·방어·크리티컬·배율·블록률·카운터·히트수·자원 소모·자원 검사/차감·마법 캐스팅 실패.
  - **스킬 타입별 전투 처리 및 디스패치**: 선제/일반 전투, 선후공 결정, 궁극기/회복/버프/CC 전투, 몬스터 단독 피해, 특수 스킬 사이드이펙트, `CombatDispatchVisitor`(020에서 도입).
  - **피해 적용 및 턴 지속효과**: 마나 실드 흡수·피해 적용, DoT 틱, 명상 MP 재생.
  - **처치 보상/던전/사망 결산**: 드랍·경험치·레벨업, 던전 보스/처치/연쇄 조우, 사망 처리.
  - **대치 페이즈 뷰 조립**: 공방 뷰·스탠스 뱃지 라벨/클래스·클래시 지속시간.
  - **턴 흐름 오케스트레이션**: `takeTurn`/`flee`/타임아웃/도망 성공·실패, 결과(`BattleTurnResult`) 빌드, 턴 종료 상태 확정.

- **중복 코드 문제**:
  - 캐릭터 스킬 조회 `characterSkillRepository.findByCharacterIdAndSkillId(progress.getId(), skill.id())`가
    **5개 스탯 해석 메서드에 반복**된다.
  - 스탯 합산 `levelStatsFor(...) + resolveEquippedBonus(...) + skillService.rankupBonus(...)` 3종 필드
    추출이 **공격력/방어/크리티컬 3곳에 반복**된다.
  - 몬스터 배율 선택 `monsterAction == HEAVY ? heavyMult : normalMult`가 2곳에 반복된다.
  - **19개 인자짜리 `BattleTurnResult` 생성자 호출이 7곳**(takeTurn·타임아웃·도망 성공·도망 실패·
    안전 종료·no-op·자원 부족)에 거의 같은 형태로 반복된다.
  - **턴 종료 상태 확정 블록**(`setPreemptiveParty(NONE)`/`setCurrentMonsterIntent(null)`/`setStandby(true)`/
    `setTurnCount(+1)`/`save`/`characterService.saveTurn`/정산 라인→actionLog)이 `takeTurn`·타임아웃·
    도망 실패 **3곳에 반복**된다.

- **핵심 목표**:
  God Class를 **응집도 기준 협력 객체로 분해**하여 SRP를 회복하고, 위 중복을 단일 지점으로 통합하여 CPD
  중복·인지 복잡도를 낮춘다. 020(스킬 타입 다형화·Visitor)으로 낮아진 결합도를 기반으로, 신규 전투 처리/
  보상 규칙/뷰 요소 추가 시 관련 협력 객체만 열어 확장(OCP)할 수 있게 한다.

- **최우선 제약 (사용자 확정)**:
  **기존 로직의 동작을 1%도 바꾸지 않는 순수 리팩토링(behavior-preserving refactoring)** 이다.
  전투 결과(데미지/크리/회복/CC/버프/DoT/마나실드/선후공/자원/궁극기 쿨타임/드랍·경험치·레벨업/사망/
  던전 진행/연쇄 조우), 로그 라우팅(중앙 `combatLines` vs 하단 `actionLog`), 예외, **RNG 소비 순서**,
  트랜잭션 경계가 리팩토링 전후 완전히 동일해야 한다.

### 1.2. 이번 스펙의 범위 (In-Scope)

1. **`BattleService` 파사드 유지**: `BattleService`의 **모든 public 메서드 시그니처**(`start`, `startClash`,
   `takeTurn`, `flee`, `resumeIfActive`, `combatSkills`, `swapWeapon`)와 **두 생성자(18-arg Spring 주입용 +
   16-arg 하위호환용)** 를 그대로 보존한다. 내부 처리만 협력 객체로 위임/이관한다.
2. **책임 분해**: 전투 스탯 해석, 스킬 전투 처리·디스패치, 피해·턴 지속효과 적용, 처치/사망 결산, 대치 뷰
   조립을 응집도 기준의 협력 객체로 추출한다. (협력 객체 최종 목록·이름은 design.md에서 확정)
3. **중복 통합**: §1.1의 중복(스킬 조회, 스탯 합산, 몬스터 배율, `BattleTurnResult` 빌드, 턴 종료 확정)을
   단일 지점(공유 헬퍼/팩토리)으로 통합한다.
4. **동작 보존(behavior-preserving)**: 리팩토링 전후 게임 플레이 결과·로그·예외·RNG 소비 순서·트랜잭션
   경계가 완전히 동일함을 기존 회귀/PBT 테스트 및 필요 시 characterization 테스트로 보장한다.

### 1.3. 제외 및 이연 범위 (Out-of-Scope / Deferred)

- **밸런스/전투 규칙 변경**: 계수(근접1.0/활0.85/마법1.2 등), 배율, 확률(마법 실패율, 도망 성공률, 연쇄 조우
  10%, 크리 상한 1000), 드랍/경험치/사망 페널티 등 모든 수치·규칙은 불변이다.
- **스킬 서브타입 다형화 추가**: 020에서 완료된 다형화/Visitor를 재구조화하지 않는다. 본 스펙은 서비스 계층의
  분해·중복 통합에 한정한다.
- **`InventoryService`(2순위, 021)의 재변경**: `BattleService`가 `inventoryService`를 호출하는 계약은
  그대로 유지하며 021 결과물을 수정하지 않는다.
- **협력 서비스 내부 변경**: `BattleResolver`, `MonsterAiService`, `MonsterRewardService`,
  `ProgressionService`, `DungeonService`, `BattleLogFormatter`, `SkillService` 등 협력 서비스의 내부
  로직·시그니처는 변경하지 않는다.
- **DTO/엔티티/JSON 스키마 변경**: `BattleState`, `BattleTurnResult`, `BattleView`, `TurnInput` 등의 필드
  구성은 변경하지 않는다. (신규 내부 컨텍스트/결과 타입은 협력 객체 간 전달용으로만 도입 가능)
- **컨트롤러/호출부 변경**: 파사드 API를 보존하므로 `BattleController`, `PlayScreenController`는 수정하지
  않는다.

---

## 2. Glossary (용어 사전)

### 2.1. 기존 재사용 용어

- **BattleService**: 전투 오케스트레이션 파사드. 본 스펙의 분해 대상.
- **BattleState**: 전투 진행 상태 엔티티(몬스터 HP, 턴 수, 선제 파티, 마나실드/DoT/스턴/증폭 상태 등).
- **BattleResolver**: 데미지/크리티컬/블록/카운터를 계산하는 순수 도메인 서비스(이미 분리됨).
- **SkillCombatVisitor / CombatDispatchVisitor**: 020에서 도입된 스킬 타입별 전투 디스패치 Visitor.
- **TurnCombatResult**: 한 턴의 전투 산출(플레이어/몬스터 피해, 크리, 블록, 카운터, 선제, 히트 목록)을
  담는 `BattleService` 내부 record.
- **BattleTurnResult**: 컨트롤러로 반환되는 턴 결과 DTO(결과·로그·보상 포함).
- **BattleView**: 대치/공방 페이즈 뷰 DTO.
- **PreemptiveParty**: 선제 주체(NONE/PLAYER/MONSTER) 열거형.
- **Outcome**: 턴 결과 상태(NONE/WIN/LOSE/FLED) 열거형.

### 2.2. 본 스펙 신규 용어 (`Pascal_Snake_Case`)

- **Battle_Facade**: 기존 public API와 두 생성자를 그대로 유지하며 내부 처리를 협력 객체로 위임/오케스트레이션
  하는 `BattleService`의 역할.
- **Battle_Collaborator**: 파사드가 소유(구성)하고 위임하는, 단일 책임의 협력 객체. Spring 빈이 아닌
  파사드-구성(plain) 객체로 두어 기존 생성자·주입 계약을 바꾸지 않는다. (근거는 design.md §1.1)
- **Skill_Combat_Context**: 한 턴의 전투 처리에 필요한 입력 묶음(진행상황/몬스터/상태/보유스킬/로그 버퍼/
  착용 재능/선제 여부/몬스터 행동)을 협력 객체로 전달하기 위한 내부 컨텍스트 타입.
- **Skill_Combat_Outcome**: 전투 처리 결과(`TurnCombatResult` + 다음 선제 파티)를 파사드로 되돌리기 위한
  내부 결과 타입.
- **Behavior_Preserving_Refactoring**: 외부 관측 가능한 동작(게임 결과·로그·예외·RNG 소비·트랜잭션)을 바꾸지
  않고 내부 구조만 변경하는 리팩토링.

---

## 3. Requirements (기능 요구사항)

> **EARS 키워드 가이드**: WHEN(이벤트), IF...THEN(원치 않는 상태 분기),
> WHILE(지속 상태), WHERE(선택 기능). 모든 문장은 `THE {주체} SHALL {동작}` 형태.

### Requirement 1: 파사드 계약 보존 (외부 호환성)

**User Story:**
개발자로서, `BattleService`를 분해하되 그것을 사용하는 컨트롤러와 20여 개 테스트가 한 줄도 바뀌지 않기를
원한다. 그래야 리팩토링이 외부에 무해하고 회귀 위험이 최소화된다.

#### Acceptance Criteria

1. **THE** `BattleService` **SHALL** 리팩토링 이전의 public 메서드 시그니처(`start`, `startClash`,
   `takeTurn`, `flee`, `resumeIfActive`, `combatSkills`, `swapWeapon`)를 이름/파라미터/반환 타입 그대로
   유지한다.
2. **THE** `BattleService` **SHALL** 기존 두 생성자(18-arg Spring 주입 생성자와 16-arg 하위호환 생성자)의
   파라미터 목록을 동일하게 유지하여, 이를 직접 호출하는 20여 개 테스트와 Spring 주입부가 변경 없이
   컴파일·동작하도록 한다.
3. **WHERE** 메서드가 기존에 `@Transactional`이었다면(`start`/`startClash`/`takeTurn`/`flee`/
   `resumeIfActive`/`swapWeapon`), **THE** 시스템 **SHALL** 동일한 트랜잭션 경계를 유지한다(파사드 진입점
   기준 단일 트랜잭션 보존).
4. **IF** 기존에 특정 입력에서 예외(`IllegalStateException` 등)를 던졌다면(예: `startClash`에서 몬스터 미지),
   **THEN THE** 시스템 **SHALL** 동일한 예외 타입과 메시지를 동일한 조건에서 던진다.

---

### Requirement 2: 책임 분해 (SRP 회복)

**User Story:**
개발자로서, 하나의 거대한 전투 서비스 대신 응집도 높은 협력 객체들이 각자의 책임을 담당하게 하고 싶다.
그래야 변경 파급이 국소화되고 각 관심사를 독립적으로 이해·테스트할 수 있다.

#### Acceptance Criteria

1. **THE** 시스템 **SHALL** `BattleService`의 책임을 응집도 기준으로 분리하여, 아래 관심사가 각각 전담
   협력 객체로 추출되도록 한다(협력 객체 최종 목록·이름은 design.md에서 확정):
   전투 스탯/자원 해석, 스킬 타입별 전투 처리·디스패치(+Visitor), 피해·턴 지속효과 적용, 처치/사망 결산,
   대치 페이즈 뷰 조립, `BattleTurnResult` 빌드.
2. **THE** 각 협력 객체 **SHALL** 하나의 명확한 책임만 담당하며, 파사드는 해당 책임을 대응 협력 객체로
   위임하거나(오케스트레이션 지점에서) 호출한다.
3. **THE** 추출된 협력 객체 **SHALL** DDD 4계층 규칙(ArchUnit)을 준수한다. 즉 `application` 계층에 위치하며
   `interfaces`(상위 계층)를 참조하지 않는다.
4. **WHERE** `takeTurn`이 여러 협력 객체를 순차 호출하는 오케스트레이터로 남는 경우, **THE** 시스템 **SHALL**
   각 단계(자원 검사/소모 → 캐스팅 실패 → 전투 디스패치 → 증폭 → 사이드이펙트 → 피해·턴효과 → 스킬 훅·내구도
   → 결산 → 턴 종료 확정)의 **순서와 조건을 리팩토링 이전과 동일하게 유지**한다.
5. **THE** 시스템 **SHALL** 분해 후 `BattleService`(파사드) 및 각 협력 객체의 클래스·메서드 크기가
   `rules/coding/code-style.md` 기준(메서드 50줄 이하 권장)에 부합하도록 정리하고, 가능하면 기존
   `@SuppressWarnings`(`PMD.CyclomaticComplexity`/`PMD.CognitiveComplexity`/`PMD.NcssCount`)를 제거·축소한다.

---

### Requirement 3: 중복 코드 제거 및 확장성 (DRY / OCP)

**User Story:**
개발자로서, 스킬 조회·스탯 합산·몬스터 배율·결과 빌드·턴 종료 처리 등 여러 곳에 복사된 로직을 한 곳으로
모으고 싶다. 그래야 규칙이 한 지점에서 관리되고 신규 기능 추가 시 재사용할 수 있다.

#### Acceptance Criteria

1. **THE** 시스템 **SHALL** `characterSkillRepository.findByCharacterIdAndSkillId(progress.getId(),
   skill.id())` 반복 조회(5곳)를 단일 지원 메서드로 통합한다.
2. **THE** 시스템 **SHALL** 레벨 스탯 + 장비 보너스 + 스킬 랭크업 보너스 합산(공격력/방어/크리티컬 3곳)의
   중복을 단일 스탯 합산 헬퍼로 통합한다.
3. **THE** 시스템 **SHALL** 몬스터 행동별 배율 선택(`HEAVY→heavyMult, else normalMult`) 중복을 단일 헬퍼로
   통합한다.
4. **THE** 시스템 **SHALL** 19-인자 `BattleTurnResult` 생성 반복(7곳)을 의미 있는 이름의 팩토리/빌더로
   통합하되, 각 호출 지점이 생성하던 **필드 값을 정확히 동일하게 유지**한다.
5. **THE** 시스템 **SHALL** 턴 종료 상태 확정 블록(선제 초기화·의도 초기화·standby 전환·턴수 증가·상태 저장·
   `saveTurn`·정산 라인 로그 반영)의 반복(3곳)을 단일 헬퍼로 통합하되, 각 경로의 부수효과·순서를 동일하게
   유지한다.
6. **THE** 시스템 **SHALL** 리팩토링 결과로 `myrpg` 모듈의 **CPD 중복과 PMD 복잡도(`CyclomaticComplexity`/
   `CognitiveComplexity`/`NcssCount`)가 리팩토링 이전 대비 감소**하도록 한다(증가시키지 않는다).
7. **WHERE** 통합된 지원 메서드가 도입되는 경우, **THE** 시스템 **SHALL** 통합 전후로 각 호출 지점의 결과
   (반환값·부수효과·예외·RNG 소비)를 동일하게 유지한다.

---

### Requirement 4: 동작 보존 및 회귀 안전성

**User Story:**
플레이어로서, 이 리팩토링 후에도 전투 결과·로그·성장·던전 진행이 이전과 정확히 동일하기를 원한다.
그래야 눈에 띄는 변화나 밸런스 붕괴 없이 코드 품질만 개선된다.

#### Acceptance Criteria

1. **THE** 시스템 **SHALL** 한 턴의 전투 산출(플레이어/몬스터 피해, 크리티컬, 블록, 카운터, 선후공, 히트 수,
   자원 소모, 마법 캐스팅 실패, 궁극기 쿨타임)을 리팩토링 이전과 동일하게 계산한다.
2. **THE** 시스템 **SHALL** 특수 스킬 효과(회복량, 마나 실드 흡수율·지속, CC 성공/저항·스턴, DoT 틱/지속,
   빙결, 분노 증폭, 명상 MP 재생)와 그 적용 순서를 리팩토링 이전과 동일하게 유지한다.
3. **THE** 시스템 **SHALL** 처치 결산(드랍·골드·경험치·레벨업 로그), 던전 처리(보스 클리어·몬스터 카운트
   차감·10% 연쇄 조우), 사망 처리(경험치 손실·던전 사망 훅)를 리팩토링 이전과 동일하게 수행한다.
4. **THE** 시스템 **SHALL** 로그 라우팅 규약(전투 액션은 중앙 `combatLines`에만, 결산·성장·사망·도망 성공은
   하단 `actionLog`에)을 리팩토링 이전과 동일하게 유지한다.
5. **THE** 시스템 **SHALL** 대치 페이즈 뷰(스탠스 뱃지 라벨/클래스, 클래시 지속시간, 활 1턴 선제 표시)와
   타임아웃/도망 성공·실패 흐름을 리팩토링 이전과 동일하게 산출한다.
6. **THE** 시스템 **SHALL** `Random` 소비 순서와 횟수를 리팩토링 이전과 동일하게 유지하여, 시드 고정 기반
   프로퍼티 테스트가 동일 결과를 재현하도록 한다.
7. **THE** 시스템 **SHALL** 기존 전투 관련 단위/프로퍼티(jqwik)/통합/슬라이스 테스트를 수정 없이 모두
   통과한다.
8. **WHERE** 분해 대상 로직 중 현행 회귀 테스트로 커버되지 않는 지점이 있는 경우, **THE** 시스템 **SHALL**
   코드 이동에 **착수하기 전에** 해당 지점의 현행 동작을 고정하는 characterization(특성화) 테스트를 먼저
   추가하여 동작 보존 안전망을 확보한다.

---

## 4. Non-Functional & Quality Requirements (비기능 및 품질 요구사항)

1. **5대 품질 가드레일 (Task 완료 필수 기준)**:
   - **Spotless**: Java 포맷팅 자동 교정 (`mvn spotless:apply`).
   - **Error Prone**: 정적 결함 컴파일 타임 차단 (경고 0건).
   - **ArchUnit**: DDD 4계층(`interfaces` → `application` → `domain`) 규칙 준수. 추출된 협력 객체는
     `application` 계층에 위치하며 상위 계층을 참조하지 않는다.
   - **JaCoCo**: 신규/변경 코드 대상 라인 80%+, 브랜치 70%+ 커버리지.
   - **PMD & CPD**: 인지·순환 복잡도 및 중복 코드가 리팩토링 이전 대비 **감소**하는 방향이어야 한다.
2. **동작 보존 검증**:
   - 리팩토링이므로 밸런스 스크립트(`tools/balance/`) 수치·전투 규칙은 불변. 기존 회귀/PBT 테스트 전수 통과가
     1차 기준이다. `BattleServiceCombatCharacterizationTest`, `BattleCombatPreservationPropertyTest` 등
     기존 안전망을 활용하고, 미커버 지점(Req 4.8)은 착수 전 characterization으로 고정한다.
   - **RNG 결정성**: 시드 고정 `Random` 기반 테스트가 리팩토링 전후 동일 결과를 내야 한다(Req 4.6).
3. **CodeGraph 동기화**:
   - 코드 변경 후 `codegraph sync` 필수 수행.
4. **불변성·코딩 규칙 (`rules/coding/code-style.md`)**:
   - Lombok 금지, `@Autowired` 지양(순수 생성자 주입), 메서드 파라미터·비재할당 지역변수 `final`,
     `var` 금지, public 클래스/메서드 JavaDoc 필수, 미사용 import/변수 제거.
