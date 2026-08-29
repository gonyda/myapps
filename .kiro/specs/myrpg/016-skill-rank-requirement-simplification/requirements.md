# Requirements Document: 스킬 승급 조건 단순화 (막타 처치 항목 전면 제거 & 사용 횟수 단일화)

> **폴더 위치 가이드**: `.kiro/specs/myrpg/016-skill-rank-requirement-simplification/requirements.md`  
> **관련 규칙**: `rules/project/spec-conventions.md`, `rules/project/tech-stack.md`

---

## 1. Introduction (개요 및 배경)

### 1.1. 배경 및 목적
- **현재 상태 및 문제점**:
  - 기존 스킬 시스템은 직접 타격 액티브 스킬(스매시, 윈드밀, 매그넘 샷, 파이어볼트 등)의 승급 조건으로 `사용 횟수(5~5,000회)` 외에 **`막타 처치 수(1~1,500회)`**를 필수로 요구했습니다.
  - 반면 디펜스, 회복, 버프, 궁극기 스킬 등은 반격 피해나 메커니즘 특성상 `isKillExempt()` 플래그를 두어 막타를 면제하는 등 도메인 모델과 서비스 전반에 분기 및 예외 처리가 파편화되어 있었습니다.
  - 턴제 전투 환경에서 특정 스킬로만 몬스터 체력을 0으로 맞춰 끝내야 하는 '막타 작업'은 플레이어에게 심각한 스트레스와 피로도를 유발하며, UI(승급 팝업 및 프로그레스 바)에서도 불필요하게 복잡한 다중 지표를 노출했습니다.
- **핵심 목표**:
  - 스킬 승급 조건에서 번거롭고 불합리한 **막타 처치(requiredKills / killCount)** 요소를 도메인, 엔티티, 서비스, DTO, UI 전반에서 완전히 제거합니다.
  - 패시브(AP 단독 소모)를 제외한 모든 액티브 스킬의 승급 조건을 **'스킬 사용 횟수(Usage Count) + AP 소모'** 단일 체계로 통합합니다.
  - UI를 깔끔하게 정돈하고, 전투 로직(`onSkillKill`) 및 예외 메서드(`isKillExempt`)를 제거하여 아키텍처의 단순성과 유지보수성을 극대화합니다.
- **선행 스펙과의 연계**:
  - `005-skill-system`: 스킬 시스템 기반 및 랭크업 정책
  - `012-defense-counter-skill-redesign`: 방어 스킬 막타 면제 로직 도입
  - `014-skill-system-expansion`: 스킬 다형성 및 신규 스킬 확장

### 1.2. 이번 스펙의 범위 (In-Scope)
1. **도메인 모델 단순화**:
   - `RankUpRequirement`: `requiredKills` 제거, `record RankUpRequirement(int requiredUsage)`로 단일화.
   - `SkillRankPolicy`: 랭크별 `REQUIREMENTS` 및 `ULTIMATE_REQUIREMENTS`에서 막타 요구치 제거.
   - `SkillType` & `Skill`: `isKillExempt()` 메서드 완전 삭제.
2. **영속 모델(엔티티) 정제**:
   - `CharacterSkill`: `killCount` 컬럼/필드 및 `getKillCount()`, `increaseKill()`, `setKillCount()` 제거.
   - `rankUpTo(next)` 시 사용 횟수(`usageCount = 0`)만 초기화.
3. **응용 서비스 및 전투 파이프라인 정리**:
   - `SkillService`: `rankUp()`, `calculateProgressPercent()`, `calculateRankable()`, `buildRankUpView()` 내 막타 판정 전면 제거.
   - `SkillService`: `onSkillKill()`, `isKillExempt()` 메서드 완전 삭제.
   - `BattleService`: 전투 턴 종료 시 `skillService.onSkillKill(...)` 호출부 제거.
4. **DTO 및 뷰 모델 간소화**:
   - `SkillRankUpView`: `killCurrent`, `killRequired`, `hasKillRequirement` 필드 삭제.
5. **프론트엔드 UI/UX 정비**:
   - `fragments/skill-popup.html`: 승급 모달에서 막타 달성도 표시 영역 제거, 사용 횟수와 AP만 직관적으로 노출.
6. **테스트 스위트 전수 갱신**:
   - 막타 관련 단위/통합/PBT 테스트를 사용 횟수 단일 조건 기준으로 개편.

### 1.3. 제외 및 이연 범위 (Out-of-Scope / Deferred)
- **스킬 요구 사용 횟수(Usage Count) 수치 변경**: 기존 사용 횟수 테이블(F→E: 5회 ~ 1→Master: 5,000회)은 그대로 유지하며, 수치 밸런스 재조정은 본 스펙 범위에서 제외합니다.
- **스킬 목록 뱃지 표기**: todo 2번 섹션의 별도 하위 항목인 '스킬 유형 뱃지 표기'는 본 스킬 승급 조건 단순화 완료 후 후속 작업으로 연계합니다.

---

## 2. Glossary (용어 사전)

### 2.1. 기존 재사용 용어
- **`SkillRank`**: F부터 Master까지 16단계 스킬 숙련도 열거형.
- **`CharacterSkill`**: 캐릭터가 습득한 스킬의 랭크, 사용 횟수, 슬롯 인덱스를 영속화하는 JPA 엔티티.
- **`SkillRankPolicy`**: 랭크 승급에 필요한 사용 횟수 및 AP 소모량을 정의하는 순수 도메인 정책 객체.
- **`SkillService`**: 스킬 습득, 승급, 슬롯 장착, 필드 사용 등을 담당하는 애플리케이션 서비스.
- **`BattleService`**: 전투 턴 진행 및 스킬 사용 이벤트를 발행하는 애플리케이션 서비스.

### 2.2. 본 스펙 변경 용어 (`Pascal_Snake_Case`)
- **`Rank_Up_Requirement`**: 스킬 승급에 필요한 조건(오직 `requiredUsage` 단일 필드)을 정의하는 불변 Record.
- **`Usage_Only_Rank_Up`**: 패시브를 제외한 모든 액티브 스킬이 오직 '스킬 사용 횟수 충족 + AP 소모'만으로 승급하는 단순화된 규칙.
- **`Kill_Count_Purge`**: 도메인/서비스/엔티티/UI 전반에서 막타 카운트 및 막타 검증 로직을 완전히 제거(Purge)하는 아키텍처 정제 작업.

---

## 3. Requirements (기능 요구사항)

### Requirement 1: 액티브 스킬 승급 조건 단순화 (사용 횟수 + AP 소모 단일화)

**User Story:**  
플레이어로서, 액티브 스킬 승급 시 번거로운 몬스터 막타 처치 조건 없이 스킬 사용 횟수와 AP만으로 승급하고 싶다.  
그래야 전투 중 몬스터 체력을 계산하며 막타를 치는 스트레스 없이 자연스럽게 스킬을 성장시킬 수 있다.

#### Acceptance Criteria

1. **WHEN** 플레이어가 액티브 스킬(일반/강/방어/회복/버프/디버프/CC/궁극기 등) 승급을 요청하면, **THE** `SkillService` **SHALL** 막타 처치 수와 무관하게 `현재 사용 횟수(usageCount) >= 요구 사용 횟수(requiredUsage)` 및 `보유 AP >= 필요 AP(apCost)` 조건만 검증한다.
2. **IF** 사용 횟수가 부족하거나 보유 AP가 부족하면, **THEN THE** `SkillService` **SHALL** 승급을 수행하지 않는다. (AP 부족 시 `InsufficientAbilityPointsException` 발생, 사용 횟수 부족 시 `false` 반환)
3. **WHEN** 액티브 스킬 승급이 성공하면, **THE** `CharacterSkill` **SHALL** 랭크를 다음 단계로 올리고 현재 사용 횟수(`usageCount`)를 0으로 리셋한다.
4. **THE** `RankUpRequirement` **SHALL** `requiredKills` 필드를 포함하지 않으며 오직 `requiredUsage` 단일 필드만을 유지한다.
5. **THE** `SkillType` 및 `Skill` **SHALL** `isKillExempt()` 메서드를 보유하지 않는다.

---

### Requirement 2: 패시브 스킬 승급 조건 유지 (AP 단독 소모)

**User Story:**  
플레이어로서, 상시 효과를 제공하는 패시브 스킬은 기존과 동일하게 수련(사용 횟수) 없이 AP만으로 즉시 승급하고 싶다.  
그래야 액티브 스킬과의 차별화된 성장 방식을 유지할 수 있다.

#### Acceptance Criteria

1. **WHEN** 플레이어가 패시브 스킬(컴뱃 마스터리, 크리티컬 히트, 메디테이션 등) 승급을 요청하면, **THE** `SkillService` **SHALL** 사용 횟수 검증을 면제하고 `보유 AP >= 필요 AP` 조건만으로 승급을 수행한다.
2. **THE** `SkillRankPolicy` **SHALL** 패시브 스킬 타입에 대해 요구 사용 횟수가 0인 `RankUpRequirement(0)`을 반환한다.

---

### Requirement 3: 전투 파이프라인 내 막타 호출 로직 완전 제거

**User Story:**  
시스템 개발자로서, 전투 턴 종료 시 더 이상 필요 없는 막타 이벤트(`onSkillKill`) 호출 및 처리를 제거하고 싶다.  
그래야 불필요한 DB 업데이트 및 메서드 호출 오버헤드를 줄이고 코드를 깔끔하게 유지할 수 있다.

#### Acceptance Criteria

1. **WHEN** 전투 턴에서 스킬이 사용되면, **THE** `BattleService` **SHALL** 오직 `skillService.onSkillUsed(characterId, skillId)`만을 호출하고 `onSkillKill`은 호출하지 않는다.
2. **THE** `SkillService` **SHALL** `onSkillKill(Long characterId, String skillId)` 메서드를 완전히 제거한다.
3. **THE** `CharacterSkill` **SHALL** `increaseKill()`, `getKillCount()`, `setKillCount()` 메서드를 완전히 제거한다.

---

### Requirement 4: UI/UX 승급 모달 및 팝업의 막타 항목 제거

**User Story:**  
플레이어로서, 스킬 목록 팝업 및 승급 모달에서 막타 관련 불필요한 정보 없이 사용 횟수와 AP 정보만 깔끔하게 보고 싶다.  
그래야 현재 스킬의 수련 진행도와 승급 가능 여부를 한눈에 직관적으로 파악할 수 있다.

#### Acceptance Criteria

1. **WHEN** 스킬 승급 모달(`rankup-modal`)이 렌더링되면, **THE** 뷰 템플릿 **SHALL** '수련 방법' 섹션에 오직 **[사용 횟수]** (`usageCurrent / usageRequired`) 행만을 표시하고 막타 처치 행은 렌더링하지 않는다.
2. **WHERE** 패시브 스킬인 경우, **THE** 승급 모달 **SHALL** 기존과 동일하게 '패시브 스킬은 수련 없이 AP로 즉시 승급할 수 있습니다.' 안내문만을 노출한다.
3. **WHEN** 스킬 목록 팝업(`skill-list`)의 수련치 프로그레스 바(%)를 계산할 때, **THE** `SkillService` **SHALL** 막타 비율 합산 없이 `(usageCurrent / usageRequired) * 100` 단일 비례식으로 산출한다.
4. **THE** `SkillRankUpView` **SHALL** `killCurrent`, `killRequired`, `hasKillRequirement` 필드를 포함하지 않는다.

---

## 4. Non-Functional & Quality Requirements (비기능 및 품질 요구사항)

1. **5대 품질 가드레일 (Task 완료 필수 기준)**:
   - **Spotless**: Java 포맷팅 자동 교정 (`mvn spotless:apply`).
   - **Error Prone**: 정적 결함 컴파일 타임 차단 (컴파일 경고 0건).
   - **ArchUnit**: DDD 4계층(`interfaces` → `application` → `domain`) 아키텍처 규칙 준수.
   - **JaCoCo**: 신규 및 변경 코드 대상 테스트 라인 커버리지 80% 이상 달성.
   - **PMD & CPD**: 복잡도(`CognitiveComplexity`), 안티패턴 및 중복 코드 0건.
2. **무결성 및 안정성**:
   - 기존 모든 스킬(15종)의 랭크 사다리(F→Master) 승급 및 AP 소모 불변식 100% 유지.
   - H2 데이터베이스 매핑 오류 없는 안전한 엔티티 필드 제거.
3. **CodeGraph 동기화**:
   - 코드베이스 변경 후 `codegraph sync` 필수 수행.
