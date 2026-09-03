# Requirements Document: 스킬 타입 다형화 (Skill Type Polymorphism)

> **폴더 위치**: `.kiro/specs/myrpg/020-skill-type-polymorphism/requirements.md`
> **관련 규칙**: `rules/project/spec-conventions.md`, `rules/project/tech-stack.md`, `rules/coding/code-style.md`
> **분석 출처**: `docs/myrpg-refactoring-candidates.md` (🔴 1순위)

---

## 1. Introduction (개요 및 배경)

### 1.1. 배경 및 목적

- **현재 상태 및 문제점**:
  `Skill`은 8종 서브타입(`DamageSkill`, `DefenseSkill`, `RecoverySkill`, `UltimateSkill`,
  `PassiveSkill`, `BuffSkill`, `CcSkill`, `DotSkill`)을 permit하는 `sealed interface`이다.
  그러나 각 서브타입의 계산·표시 규칙이 서브타입 자신이 아니라 `SkillService`와 `BattleService`의
  여러 private 메서드에 `instanceof` 체인 형태로 흩어져 중복 구현되어 있다.
  - `SkillService`: `primaryStatLabel`, `primaryValue`, `resourceCost`, `counterValue`,
    `critBonusValue`, `calculateRankable`(PassiveSkill 분기) 등에서 동일 타입 체인 반복.
  - `BattleService`: `resolveResourceCost`, `resolvePlayerMultiplier`, `resolvePlayerBlockRate`,
    `resolvePlayerCounterPercent`, `resolveEffectivePlayerCritical`, `resolvePlayerHitCount`,
    그리고 `takeTurn`의 전투 디스패치(Ultimate/Recovery/Buff/Cc)와 DotSkill 사이드이펙트에서
    같은 타입 분기 반복.

- **핵심 목표**:
  스킬 서브타입별 분기 로직을 **다형성(polymorphism)** 또는 **Visitor 패턴**으로 위임하여,
  서비스의 `instanceof` 체인을 단일 다형 호출로 축약한다.
  이를 통해 **산탄총 수술(Shotgun Surgery)** 을 제거하고, 신규 스킬 타입 추가 시
  기존 서비스 코드를 열지 않고 서브타입 클래스만 추가·구현하면 되도록(OCP) 한다.

- **선행 스펙과의 연계**:
  `005-skill-system`, `009-skill-differentiation-and-battle-log`, `012-defense-counter-skill-redesign`,
  `013-active-telegraph-combat`, `014-skill-system-expansion`에서 구축·확장된 스킬/전투 계층을
  기능 변경 없이(순수 리팩토링) 재구조화한다.

### 1.2. 이번 스펙의 범위 (In-Scope)

1. **`Skill` 다형 계약 확장**: `Skill` 인터페이스에 값 조회용 다형 메서드
   (예: `primaryStatLabel()`, `primaryValueAt(rank)`, `resourceCostAt(rank)`,
   `counterValueAt(rank)`, `critBonusValueAt(rank)`)를 정의하고 각 서브타입 record가 구현.
2. **전투 처리 Visitor 도입**: 서비스 협력객체가 필요한 전투 분기
   (Ultimate/Recovery/Buff/Cc/Dot)를 `instanceof` 대신 Visitor(또는 sealed switch) 디스패치로 전환.
3. **`SkillService` 분기 제거**: 표시/값 계산 private 메서드의 `instanceof` 체인을 다형 호출로 축약.
4. **`BattleService` 분기 제거**: 스탯 해석 및 `takeTurn` 전투 디스패치의 `instanceof` 체인을
   다형 호출/Visitor로 축약.
5. **동작 보존(behavior-preserving)**: 리팩토링 전후 게임 플레이 결과(데미지/회복/CC/버프/승급 표시)가
   완전히 동일함을 회귀 테스트로 보장.

### 1.3. 제외 및 이연 범위 (Out-of-Scope / Deferred)

- **아이템 다형화(`EquipmentItem`/`PotionItem` instanceof)**: `InventoryService`, `ShopService`의
  아이템 `instanceof` 분기는 본 스펙 대상이 아니며 별도 스펙(2순위 God Class 분리 시)에서 다룬다.
- **`BattleService`/`InventoryService` God Class 분리(2·3순위)**: 본 스펙은 스킬 타입 분기 제거에
  한정한다. 단, 1순위 완료로 3순위 결합도가 자연 감소하는 부수 효과는 허용한다.
- **스킬 밸런스 수치 변경**: JSON 카탈로그 수치, 배율, 확률 등 밸런스 값은 변경하지 않는다.
- **신규 스킬 타입 추가**: 본 스펙은 기존 8종 구조 재정비이며 신규 타입은 추가하지 않는다.

---

## 2. Glossary (용어 사전)

### 2.1. 기존 재사용 용어

- **Skill**: `com.myapps.web.myrpg.domain.model.Skill`. 8종 서브타입을 permit하는 sealed interface.
- **Skill_Subtype**: `DamageSkill`, `DefenseSkill`, `RecoverySkill`, `UltimateSkill`,
  `PassiveSkill`, `BuffSkill`, `CcSkill`, `DotSkill` 8종 불변 record.
- **SkillService**: 스킬 목록/승급/표시 뷰 및 필드 스킬 처리를 담당하는 애플리케이션 서비스.
- **BattleService**: 전투 턴 처리(선후공, 자원 소모, 스킬 타입별 전투, 사이드이펙트)를 담당하는 서비스.
- **SkillDamagePolicy**: 배율/블록률/카운터 배율을 계산하는 순수 도메인 정책 객체.
- **SkillRank**: 스킬 랭크 열거형(F~MASTER, 16단계).
- **CharacterSkill**: 캐릭터가 보유한 스킬의 랭크·사용횟수·쿨타임 영속 엔티티.

### 2.2. 본 스펙 신규 용어 (`Pascal_Snake_Case`)

- **Skill_Combat_Visitor**: 전투 처리 분기를 각 스킬 서브타입으로 위임하기 위한 방문자(Visitor)
  인터페이스. 서비스가 협력객체(랜덤/리포지토리/메시지 등)를 담아 구현하고, `Skill`이 `accept`한다.
  (대안으로 sealed `switch` 패턴 매칭 채택 가능 — design.md에서 확정)
- **Polymorphic_Value_Method**: `Skill` 인터페이스에 정의되어 각 서브타입이 자신의 계산 규칙을
  구현하는 값 조회 메서드군(라벨/기본값/자원비용/카운터/크리보너스).
- **Behavior_Preserving_Refactoring**: 외부 관측 가능한 동작(게임 결과·로그·표시)을 바꾸지 않고
  내부 구조만 변경하는 리팩토링.

---

## 3. Requirements (기능 요구사항)

> **EARS 키워드 가이드**: WHEN(이벤트), IF...THEN(원치 않는 상태 분기),
> WHILE(지속 상태), WHERE(선택 기능). 모든 문장은 `THE {주체} SHALL {동작}` 형태.

### Requirement 1: 스킬 서브타입 다형 값 계약

**User Story:**
개발자로서, 각 스킬 서브타입의 표시/값 계산 규칙을 서브타입 클래스 자신이 갖게 하고 싶다.
그래야 `SkillService`가 타입을 구분하지 않고 단일 다형 호출로 값을 얻어, 신규 타입 추가 시
서비스를 수정하지 않아도 된다.

#### Acceptance Criteria

1. **THE** `Skill` 인터페이스 **SHALL** 각 서브타입이 자신의 규칙으로 구현하는 값 조회 다형 메서드
   (기본 스탯 라벨, 랭크별 기본값, 랭크별 자원 소모, 카운터 값, 크리 보너스 값)를 정의한다.
2. **WHEN** `SkillService`가 스킬의 표시용 기본 스탯 라벨을 조회하면,
   **THE** 시스템 **SHALL** 각 서브타입의 다형 메서드 결과를 반환하며 리팩토링 이전과 동일한 라벨을 산출한다.
3. **WHEN** `SkillService`가 특정 랭크의 기본값/자원 소모/카운터/크리 보너스를 조회하면,
   **THE** 시스템 **SHALL** 서브타입 다형 메서드로 위임하여 리팩토링 이전과 동일한 수치를 반환한다.
4. **IF** 카운터/크리 보너스가 정의되지 않은 서브타입이면,
   **THEN THE** 시스템 **SHALL** 리팩토링 이전과 동일한 sentinel을 반환한다. 구체적으로
   `counterValue`/`critBonusValue` 대응 값은 기존과 동일하게 **`Integer null`** 을 반환하고,
   `primaryValue`의 미정의(PassiveSkill) 대응 값은 기존과 동일하게 **`int 0`** 을 반환한다.
   (다형 메서드가 내부적으로 `Optional`을 쓰더라도, `SkillService` 호출부는 기존과 동일한 반환값
   — `null` 또는 `0` — 으로 되돌려 다운스트림 렌더링을 바꾸지 않는다.)
5. **THE** 시스템 **SHALL** `SkillService`의 스킬 타입 판별 `instanceof` 체인을 제거하고
   다형 호출 또는 sealed switch로 대체한다.
6. **THE** 시스템 **SHALL** `DamageSkill`/`UltimateSkill`의 기본 스탯 라벨을 기존 default 분기와 동일하게
   `PRIMARY_STAT_DAMAGE`("보너스 데미지")로, `DefenseSkill`은 `PRIMARY_STAT_DEFENSE`("피해 경감")로
   유지한다. (다형화로 default fall-through가 각 서브타입 구현으로 분산될 때 라벨 문자열이 바뀌지 않아야 한다.)
7. **WHERE** 기존 계산이 `SkillDamagePolicy`를 경유하는 경우(딜/방어 배율·블록률·카운터),
   **THE** 다형 메서드 **SHALL** 동일한 `SkillDamagePolicy` 계산식에 위임하며, 누락 랭크 키에 대해
   기존과 동일하게 `IllegalArgumentException`을 던지는 동작을 보존한다(0으로 폴백하지 않는다).

---

### Requirement 2: 전투 처리 분기 Visitor/다형 디스패치

**User Story:**
개발자로서, 전투에서 스킬 타입별로 달라지는 처리(궁극기/회복/버프/CC/도트)를 타입 분기 없이
디스패치하고 싶다. 그래야 `BattleService.takeTurn`의 `instanceof` 사슬을 제거하고 전투 처리 로직의
응집도를 높일 수 있다.

#### Acceptance Criteria

1. **THE** 시스템 **SHALL** 전투 스탯 해석(자원 소모, 배율, 블록률, 카운터 배율, 실효 크리티컬, 히트 수)의
   스킬 서브타입 판별 `instanceof`만 다형 호출/Visitor 디스패치로 대체하고,
   **각 해석 메서드가 처리하는 타입 부분집합과 fallback 값은 기존과 100% 동일하게 보존한다.**
   폴리모픽 값 메서드(Req 1의 `primaryValueAt` 등)와 **통합·재사용하지 않는다.** 구체 보존 규칙:
   - `resolvePlayerMultiplier`: `DamageSkill`→배율, `DotSkill`→초기배율, **그 외 모든 타입 및 스킬 미보유
     시 `gameProperties.battle().monsterNormalMultiplier()`** (0이 아님).
   - `resolvePlayerBlockRate`: `DefenseSkill`만 블록률, **그 외 0**, 스킬 미보유 시 0.
   - `resolvePlayerCounterPercent`: `DefenseSkill`만 카운터 배율, **그 외 0**, 스킬 미보유 시 0.
   - `resolvePlayerHitCount`: `DamageSkill`만 `hitCount`, **그 외 1**.
   - `resolveEffectivePlayerCritical`: `DamageSkill`→`critBonus()`, `DefenseSkill`→`critBonusAt(rank)`
     (스킬 보유 시), **그 외 0**. `UltimateSkill`의 추가 `critBonus()` 가산은 궁극기 전투 핸들러에서
     기존과 동일하게 유지.
   - `resolveResourceCost`: `DefenseSkill`/`RecoverySkill`만 랭크별 소모, **그 외 `resourceCost()` 고정**.
2. **WHEN** 플레이어가 특수 스킬로 턴을 수행하면, **THE** 시스템 **SHALL** 기존 `takeTurn`의 명시 분기
   4종(`UltimateSkill`/`RecoverySkill`/`BuffSkill`/`CcSkill`)을 대응 전투 핸들러로 Visitor/sealed
   switch 디스패치하여 리팩토링 이전과 동일한 데미지/회복/상태효과/로그를 산출한다.
3. **THE** 시스템 **SHALL** `DotSkill`은 별도 사이드이펙트 경로(`applySpecialSkillSideEffects`)에서,
   `DamageSkill`/`DefenseSkill`은 일반 전투 경로에서 처리하는 기존 흐름을 보존하며,
   해당 지점의 `instanceof` 판별만 다형 디스패치로 대체한다.
4. **WHERE** 전투 핸들러가 서비스 협력객체(`resolver`, `random`, `characterSkillRepository`,
   `gameMessageService`, `statProgression`, `skillService`)를 필요로 하는 경우,
   **THE** 시스템 **SHALL** 순수 record로 로직을 옮기지 않고 협력객체를 보유한 Visitor/전략 객체에
   해당 처리를 위임한다.
5. **IF** 스킬을 보유하지 않았거나 궁극기 쿨타임이 남아 사용 불가한 경우,
   **THEN THE** 시스템 **SHALL** 리팩토링 이전과 동일한 방어 분기(폴백/차단) 동작을 유지한다.
6. **THE** 시스템 **SHALL** `takeTurn`의 전투 디스패치 및 사이드이펙트에서 스킬 서브타입 판별
   `instanceof` 체인을 제거한다.

---

### Requirement 3: 동작 보존 및 회귀 안전성

**User Story:**
플레이어로서, 이 리팩토링 후에도 스킬 표시·전투 결과가 이전과 정확히 동일하기를 원한다.
그래야 눈에 띄는 변화나 밸런스 붕괴 없이 코드 품질만 개선된다.

#### Acceptance Criteria

1. **THE** 시스템 **SHALL** 스킬 목록/승급 뷰의 기본 스탯 라벨·값·카운터·크리 보너스 표시를
   리팩토링 이전과 동일하게 렌더링한다.
2. **THE** 시스템 **SHALL** 전투 결과(데미지 합, 크리티컬 여부, 회복량, CC 성공/저항, 버프 지속·흡수율,
   도트 틱/지속, 선후공, 자원 소모, 궁극기 쿨타임)를 리팩토링 이전과 동일하게 산출한다.
3. **IF** 미지의 스킬 식별자나 미보유 스킬이 입력되면,
   **THEN THE** 시스템 **SHALL** 리팩토링 이전과 **동일한** 처리를 유지한다. 즉 기존에 예외를 던지던
   지점(예: `SkillDamagePolicy`의 누락 랭크 키 → `IllegalArgumentException`)은 예외를 그대로 유지하고,
   기존에 안전 기본값으로 폴백하던 지점은 동일한 기본값으로 폴백하며, 어느 경우에도 데이터 손상이 없어야 한다.
   (예외 지점을 임의로 기본값 폴백으로 바꾸지 않는다.)
4. **THE** 시스템 **SHALL** 기존 전투·스킬 관련 단위/프로퍼티/슬라이스 테스트를 모두 통과하며,
   불필요한 브라우저 `alert` 없이 기존 UI/로그 동작을 보존한다.
5. **WHERE** 리팩토링 대상 전투 핸들러(`resolveUltimateCombat`, `resolveRecoveryCombat`,
   `resolveBuffCombat`, `resolveCcCombat`) 및 스탯 해석(`resolvePlayerBlockRate` 등)에 현재 커버하는
   테스트가 없는 경우, **THE** 시스템 **SHALL** 코드 변경에 **착수하기 전에** 해당 지점의 현행 동작을
   고정하는 characterization(특성화) 테스트를 먼저 추가하여 동작 보존의 안전망을 확보한다.
6. **THE** `Skill` 인터페이스 **SHALL** `sealed`를 유지하여 서브타입 집합의 exhaustiveness를 보장한다.

---

## 4. Non-Functional & Quality Requirements (비기능 및 품질 요구사항)

1. **5대 품질 가드레일 (Task 완료 필수 기준)**:
   - **Spotless**: Java 포맷팅 자동 교정 (`mvn spotless:apply`).
   - **Error Prone**: 정적 결함 컴파일 타임 차단 (경고 0건).
   - **ArchUnit**: DDD 4계층(`interfaces` → `application` → `domain`) 규칙 준수.
     Visitor/다형 메서드는 `domain` 계층에 위치하며 상위 계층을 참조하지 않는다.
   - **JaCoCo**: 신규/변경 코드 대상 라인 80%+, 브랜치 70%+ 커버리지.
   - **PMD & CPD**: 인지 복잡도 감소(분기 제거) 및 중복 코드 0건. 본 리팩토링은 CPD 중복 및
     `CognitiveComplexity` 위반을 **감소**시키는 방향이어야 한다.
2. **동작 보존 검증**:
   - 리팩토링이므로 밸런스 스크립트(`tools/balance/`) 수치는 불변. 기존 회귀 테스트 전수 통과가 1차 기준.
   - 단, 현재 미커버된 전투 핸들러/스탯 해석 지점(Req 3.5)은 기존 회귀 테스트만으로 동작 보존을 보장할 수
     없으므로, 착수 전 characterization 테스트로 현행 동작을 고정한 뒤 리팩토링을 진행한다.
3. **CodeGraph 동기화**:
   - 코드 변경 후 `codegraph sync` 필수 수행.
4. **불변성·코딩 규칙 (`rules/coding/code-style.md`)**:
   - Lombok 금지, 메서드 파라미터 `final`, public 클래스/메서드 JavaDoc 필수, `record` 불변성 유지.
