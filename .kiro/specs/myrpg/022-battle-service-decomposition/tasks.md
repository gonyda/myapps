# Implementation Plan: BattleService God Class 분해 (Battle Service Decomposition)

> **폴더 위치**: `.kiro/specs/myrpg/022-battle-service-decomposition/tasks.md`
> **관련 규칙**: `rules/workflow/task-build-validation.md`, `rules/workflow/git-workflow.md`

---

## Overview

본 작업 명세는 `myrpg` 모듈의 God Class `BattleService`(약 1,752줄, 의존성 19개)를 **파사드 + 협력 객체**로
분해하고 중복(스킬 조회·스탯 합산·몬스터 배율·`BattleTurnResult` 빌드·턴 종료 확정)을 통합하기 위한
체크포인트 단위 작업 목록이다. **동작 보존 리팩토링**이므로 각 단계에서 기존 테스트가 그린을 유지해야 한다.

### 구현 순서 및 원칙
1. **안전망 우선(Characterization)**: 변경 전 기존 전투 테스트(≈20개)가 전부 그린인지 확인하고, 미커버 분해
   지점의 현행 동작을 고정하는 테스트를 먼저 추가한다.
2. **Bottom-Up + 단일 이동**: **A. 안전망 & 신규 전달 타입** → **B. `CombatStatResolver`** →
   **C. `SkillCombatResolver`(+Visitor 이관)** → **D. `TurnEffectApplier`** → **E. `BattleRewardService`** →
   **F. `BattleViewAssembler`** → **G. `BattleTurnResultFactory` & `finalizeTurn`** → **H. 중복 통합 마감** →
   **I. 회귀 검증** → **J. 5대 가드레일** 순.
3. **원자적 그린**: 각 협력 객체를 하나 추출·위임할 때마다 `BUILD SUCCESS` + 전체 테스트 그린 유지. 한 번에
   하나의 관심사만 이동하여 회귀 지점을 국소화한다.
4. **계약 불변**: `BattleService`의 public 7개 메서드 시그니처와 두 생성자(18-arg/16-arg)는 절대 바꾸지 않는다.
5. **RNG 결정성**: 협력 객체는 파사드가 보유한 **동일 `Random` 인스턴스**를 주입받아 소비 순서를 보존한다.
6. **테스트 필수 & Given-When-Then**: `should_{동작}_when_{조건}`, Spring Boot 4.0 규약 준수.

---

## Tasks

### A. 안전망 및 신규 전달 타입 (Foundation)

- [x] 1. 변경 전 안전망 확인 및 characterization 테스트 선행 작성
  - [x] 1.1 기존 전투 테스트 전수 그린 확인 (`mvn test -pl myrpg -Dtest="Battle*"`)
  - [x] 1.2 분해 대상 메서드 목록을 design.md §3.1 매핑표와 대조 확정
  - [x] 1.3 **미커버 지점 characterization 테스트 선행 작성** — `finalizeTurn` 3경로(takeTurn/타임아웃/도망
        실패) 상태 확정 동치, `BattleTurnResultFactory` 각 팩토리 필드값, 명상/DoT 경계, 던전 보스 판정
        (`bossRoomId`/`type==boss`/`giant-spider`)·연쇄 조우(10%)·몬스터 카운트 차감 1회 보장 경계.
    - **판단 기준**: 각 협력 객체 추출 직전 JaCoCo 리포트로 커버리지 공백을 식별하고, 이관 대상의 미커버
      분기만 추가한다. 기존 회귀/PBT가 덮는 경로는 중복 작성하지 않는다.
  - _Requirements: 4.7, 4.8_ / _Design: 6.1_

- [x] 2. `application/service/battle` 패키지 및 신규 전달 타입 생성
  - [x] 2.1 `battle/TurnCombatResult.java` 이동(기존 `BattleService` 내부 record → battle 패키지 공개 record, 필드 불변)
  - [x] 2.2 `battle/SkillCombatContext.java`, `battle/SkillCombatOutcome.java` record 생성
  - [x] 2.3 `battle/KillResolution.java` 이동(공개 record)
  - _Requirements: 2.1_ / _Design: 3.2, 4_

- [x] 3. **체크포인트 A** — 전달 타입 도입 후 빌드 & 전체 테스트 그린 (`mvn test -pl myrpg`)

---

### B. `CombatStatResolver` 추출 (전투 스탯/자원 해석)

- [x] 4. `CombatStatResolver` 추출 및 파사드 위임
  - [x] 4.1 `battle/CombatStatResolver.java` 생성, 스탯/자원 해석 메서드 이관
        (`attackPower`, `resolvePrimaryStat`, `resolveCoefficient`, `resolveEquippedBonus`,
        `resolvePlayerDefense`, `resolvePlayerCritical`, `resolveEffectivePlayerCritical`,
        `resolvePlayerHitCount`, `resolvePlayerMultiplier`, `resolvePlayerBlockRate`,
        `resolvePlayerCounterPercent`, `resolveResourceCost`, `hasEnoughResource`, `deductResource`,
        `checkMagicCastFailure`)
  - [x] 4.2 중복 통합: `findCharacterSkill(progress, skill)` 단일 헬퍼(5곳), `aggregatedStats(progress)`
        스탯 합산 헬퍼(3곳) 도입 — 값 동치 유지
  - [x] 4.3 파사드 및 `SkillCombatResolver` 예정 호출부가 `combatStatResolver` 위임하도록 전환
  - [x] 4.4 `mvn test -pl myrpg -Dtest="Battle*"` 그린 확인 (특히 EffectiveCritical/Resource/BowFirstStrike)
  - _Requirements: 1.1, 2.1, 3.1, 3.2, 4.1, 4.6_ / _Design: 3.1, 3.4_

- [x] 5. **체크포인트 B** — 스탯 해석 이관 후 빌드 & 테스트 그린

---

### C. `SkillCombatResolver` 추출 (전투 처리·디스패치 + Visitor 내부화)

- [x] 6. `SkillCombatResolver` 추출 및 `CombatDispatchVisitor` 이관
  - [x] 6.1 `battle/SkillCombatResolver.java` 생성, 전투 처리 메서드 이관
        (`resolveCombat`, `isBowFirstStrike`, `resolvePlayerPreemptive`, `resolveMonsterPreemptiveDamage`,
        `resolveNormalCombat`, `determineTurnOrder`, `resolveUltimateCombat`, `resolveRecoveryCombat`,
        `resolveBuffCombat`, `resolveCcCombat`, `resolveMonsterDamageForSupportTurn`,
        `resolveMonsterOnlyDamage`, `applySpecialSkillSideEffects`)
  - [x] 6.2 `CombatDispatchVisitor`를 `SkillCombatResolver` 내부 클래스로 이관, 단일 진입점
        `resolveSkillCombat(SkillCombatContext) → SkillCombatOutcome` 노출 (`nextPreemptive` 포함)
  - [x] 6.3 중복 통합: `monsterMultiplier(monsterAction)` 헬퍼(2곳) 도입 — 값 동치 유지
  - [x] 6.4 파사드 `takeTurn`이 `skill.accept(visitor)` 인라인 대신 `skillCombatResolver.resolveSkillCombat(context)`
        호출로 전환, `combatLines` 동일 인스턴스 전달로 로그 순서 보존
  - [x] 6.5 `mvn test -pl myrpg -Dtest="Battle*,SkillCombatVisitor*"` 그린 확인 (동작 보존/디스패치 정합성)
  - _Requirements: 1.1, 2.1, 2.4, 3.3, 4.1, 4.2, 4.6_ / _Design: 3.2, 3.4_

- [x] 7. **체크포인트 C** — 전투 처리·디스패치 이관 후 빌드 & 테스트 그린

---

### D~F. 나머지 협력 객체 추출 (원자 단위)

- [x] 8. `TurnEffectApplier` 추출 (피해·턴 지속효과)
  - [x] 8.1 `battle/TurnEffectApplier.java` 생성, `applyManaShieldAndDamage`/`applyDoTTick`/
        `applyMeditationRegen` 이관, 파사드 위임 전환
  - [x] 8.2 `mvn test -pl myrpg -Dtest="BattleServiceManaShield*,Battle*"` 그린 확인
  - _Requirements: 1.1, 2.1, 4.2_ / _Design: 3.1_

- [x] 9. `BattleRewardService` 추출 (처치/던전/사망 결산)
  - [x] 9.1 `battle/BattleRewardService.java` 생성, `processKillReward`/`resolveItemName`/
        `handleMonsterKilled`/`handleDeath` 이관, `KillResolution` 사용, 파사드 위임 전환
  - [x] 9.2 `mvn test -pl myrpg -Dtest="BattleServiceChainCombat*,BattleServiceDeath*,Battle*"` 그린 확인
  - _Requirements: 1.1, 2.1, 4.3, 4.4_ / _Design: 3.1_

- [x] 10. `BattleViewAssembler` 추출 (대치 뷰/뱃지)
  - [x] 10.1 `battle/BattleViewAssembler.java` 생성, `buildClashBattleView`/`resolveClashDuration`/
        `resolveStanceBadgeLabel`/`resolveStanceBadgeClass` 이관. 스킬 버튼은 파사드가 `combatSkills(progress)`
        결과를 인자로 전달(순환 방지)
  - [x] 10.2 `mvn test -pl myrpg -Dtest="BattleServiceClash*,Battle*"` 그린 확인
  - _Requirements: 1.1, 2.1, 4.5_ / _Design: 3.1, 3.3_

- [x] 11. **체크포인트 D~F** — 효과/결산/뷰 이관 후 빌드 & 전체 테스트 그린

---

### G. 결과 팩토리 & 턴 종료 통합 (Result Factory & finalizeTurn)

- [x] 12. `BattleTurnResultFactory` 추출 및 `BattleTurnResult` 중복 제거
  - [x] 12.1 `battle/BattleTurnResultFactory.java` 생성, 의미별 팩토리 메서드
        (`combat(...)`, `monsterOnly(...)`, `terminal(outcome, lines)`, `noOp()`, `insufficient(skill, kind)`)
        도입. 각 호출 지점(7곳)의 기존 19-인자 필드값을 정확히 재현
  - [x] 12.2 `takeTurn`/`handleTimeoutTurn`/`handleFleeSuccess`/`handleFleeFailure`/`safeTerminateAndReturn`/
        `buildNoOpResult`/`buildInsufficientResult`가 팩토리를 사용하도록 전환
  - _Requirements: 1.1, 3.4, 4.1, 4.5_ / _Design: 3.4_

- [x] 13. 턴 종료 확정 중복 통합 (`finalizeTurn`)
  - [x] 13.1 파사드에 `finalizeTurn(state, progress, nextPreemptive)` private 헬퍼 도입, 3경로(takeTurn/
        타임아웃/도망 실패)가 공유. 정산 라인→`actionLog` 반영 위치·순서는 각 경로 기존대로 보존
  - _Requirements: 3.5, 4.4_ / _Design: 3.3_

- [x] 14. **체크포인트 G** — 팩토리/finalizeTurn 통합 후 빌드 & 테스트 그린

---

### H. 중복 통합 마감 및 정리 (Consolidation & Cleanup)

- [x] 15. 잔여 중복 통합 및 코드 정리
  - [x] 15.1 협력 객체 내부에 남은 스킬 조회/스탯 합산/몬스터 배율 잔재를 헬퍼 호출로 마저 치환(CPD 마감)
  - [x] 15.2 미사용 import/변수/private 메서드 제거, 메서드 길이(50줄 이하) 정리
  - [x] 15.3 `BattleService`/`takeTurn`의 `@SuppressWarnings`(`PMD.CyclomaticComplexity`/
        `PMD.CognitiveComplexity`/`PMD.NcssCount`) 제거/축소 가능 여부 확인 및 적용
  - _Requirements: 2.5, 3.6, 3.7_ / _Design: 6.3_

---

### I. 전체 회귀 및 동작 보존 검증 (Regression)

- [x] 16. 모듈 전체 테스트 회귀
  - [x] 16.1 `mvn test -pl myrpg` 전체 단위/통합/슬라이스/프로퍼티 테스트 그린 확인
  - [x] 16.2 `BattleService` public 7개 메서드 시그니처·두 생성자 파라미터가 리팩토링 이전과 동일한지 확인
        (컨트롤러·테스트 무수정 컴파일)
  - [x] 16.3 시드 고정 PBT(선후공/도망/마법실패/궁극기/마나실드)로 RNG 소비 순서 동일 재현 확인
  - _Validates: Requirements 1.1, 1.2, 1.3, 4.1~4.7_

---

### J. 5대 품질 가드레일 및 마무리 (Quality Guardrails)

- [x] 17. 5대 품질 가드레일 전체 파이프라인 검증
  - [x] 17.1 `mvn -B -q spotless:apply -pl myrpg` — 포맷팅 자동 교정
  - [x] 17.2 `mvn -B clean install -pl myrpg -am` — 컴파일/Error Prone/ArchUnit/JaCoCo(80%+)/PMD·CPD 전수 검증
  - [x] 17.3 PMD 복잡도 및 CPD 중복이 리팩토링 이전 대비 감소했는지 확인
  - _Requirements: 4(비기능) 1~4_ / _Design: 6.3_

- [x] 18. CodeGraph 인덱스 동기화
  - [x] 18.1 `codegraph sync` 실행하여 심볼/호출 관계 갱신
  - _Workflow 규칙 준수_

- [x] 19. Memory Bank 갱신 (Compaction)
  - [x] 19.1 `memory-bank/activeContext.md`에 022 완료 요약 및 다음 단계(리팩토링 백로그 소진/신규 컨텐츠) 기록
  - _AGENTS.md 규칙 준수_

- [x] 20. Git 커밋 (사용자 요청 시)
  - [x] 20.1 `rules/workflow/git-workflow.md` 준수 커밋 메시지 작성
  - _Workflow 규칙 준수_

---

## 요구사항 역추적 (Requirements Traceability)

| Requirement | 대응 Task |
|---|---|
| Req 1 (파사드 계약 보존: 시그니처/생성자/트랜잭션/예외) | 4~13, 16 |
| Req 2 (책임 분해 SRP + 계층 준수 + 오케스트레이션 순서) | 2, 4, 6, 8, 9, 10, 15 |
| Req 3 (중복 제거·확장성 DRY/OCP) | 4.2, 6.3, 12, 13, 15 |
| Req 4 (동작 보존·RNG 결정성 + characterization 선행) | 1, 4~13, 16 |
| Req 4(비기능, 5대 가드레일/CodeGraph/코딩규칙) | 17, 18 |
