# Implementation Plan: 스킬 타입 다형화 (Skill Type Polymorphism)

> **폴더 위치**: `.kiro/specs/myrpg/020-skill-type-polymorphism/tasks.md`
> **관련 규칙**: `rules/workflow/task-build-validation.md`, `rules/workflow/git-workflow.md`

---

## Overview

본 작업 명세는 `myrpg` 모듈의 `SkillService`·`BattleService`에 산재한 스킬 서브타입 `instanceof`
분기를 다형성 + Visitor 패턴으로 축약하기 위한 체크포인트 단위 작업 목록이다.
**동작 보존 리팩토링**이므로 각 단계에서 기존 테스트가 그린을 유지해야 한다.

### 구현 순서 및 원칙
1. **안전망 우선(Characterization)**: 변경 전 기존 전투/스킬 테스트가 전부 그린인지 확인하고,
   동치성 검증용 테스트를 먼저 추가한다.
2. **Bottom-Up**: **A. 도메인 다형 계약(인터페이스+서브타입+Visitor)** → **B. `SkillService` 치환**
   → **C. `BattleService` 치환** → **D. 회귀/PBT 검증** → **E. 5대 가드레일 통합** 순.
3. **원자적 그린**: 각 단계 완료 시 `BUILD SUCCESS`. 다형 메서드는 기존 메서드에 위임하여 중복 방지.
4. **테스트 필수 & Given-When-Then**: `should_{동작}_when_{조건}`, Spring Boot 4.0 규약 준수.

---

## Tasks

### A. 도메인 다형 계약 구축 (Domain Layer)

- [x] 1. 변경 전 안전망 확인 및 characterization 테스트 선행 작성
  - [x] 1.1 기존 전투/스킬 테스트 전수 그린 확인 (`mvn test -pl myrpg -Dtest="Battle*,Skill*"`)
  - [x] 1.2 리팩토링 대상 instanceof 지점 목록을 design.md 매핑표(§3.1 값 메서드, §3.3 BattleService fallback 표)와 대조 확정
  - [x] 1.3 **미커버 지점 characterization 테스트 선행 작성** — `resolveUltimateCombat`,
        `resolveRecoveryCombat`, `resolveBuffCombat`, `resolveCcCombat` 및 스탯 해석
        (`resolvePlayerMultiplier` fallback=monsterNormalMultiplier, `resolvePlayerBlockRate`/
        `resolvePlayerCounterPercent`=0, `resolvePlayerHitCount`=1, `resolveResourceCost` 고정폴백)의
        **현행 동작을 고정**하는 테스트를 변경 착수 전에 추가한다. (시드 고정 `Random`, 미보유/타입 외 케이스 포함)
  - _Requirements: 3.4, 3.5_ / _Design: 3.3, 6.1_

- [x] 2. `SkillCombatVisitor<R>` 신규 인터페이스 정의
  - [x] 2.1 `domain/service/SkillCombatVisitor.java` 생성 (8종 `visitXxx(subtype)` 선언, JavaDoc)
  - _Requirements: 2.1, 2.2_ / _Design: 3.2_

- [x] 3. `Skill` 인터페이스 다형 계약 확장
  - [x] 3.1 `primaryStatLabel()`, `primaryValueAt(rank)` 추상 메서드 추가
  - [x] 3.2 `resourceCostAt(rank)`(default), `counterValueAt(rank)`/`critBonusValueAt(rank)`(Optional default) 추가
  - [x] 3.3 `<R> R accept(SkillCombatVisitor<R> visitor)` 추상 메서드 추가
  - _Requirements: 1.1, 2.1, 3.5_ / _Design: 3.1, 3.2_

- [x] 4. 8종 서브타입 record에 다형 메서드 구현 (기존 메서드/정책에 위임)
  - [x] 4.1 `DamageSkill`, `DefenseSkill`, `RecoverySkill`, `UltimateSkill` 구현 (`accept` + 값 메서드)
  - [x] 4.2 `PassiveSkill`, `BuffSkill`, `CcSkill`, `DotSkill` 구현 (`accept` + 값 메서드)
  - [x] 4.3 `DefenseSkill`의 blockRate/counter/crit는 `SkillDamagePolicy` 순수 계산 위임
  - _Requirements: 1.2, 1.3, 1.4_ / _Design: 3.1_

- [x] 5. 도메인 다형 메서드 단위 + jqwik 프로퍼티 테스트
  - [x] 5.1 `SkillPolymorphismTest`[신규] — 서브타입별 값 메서드 테이블 검증
  - [x] 5.2 `SkillCombatVisitorPropertyTest`[신규, jqwik] — **Property 1(동치성), Property 2(디스패치 정합성)**
  - _Validates: Requirements 1.2, 1.3, 1.4, 2.2, 3.5_

- [x] 6. **체크포인트 A** — 도메인 계층 빌드 & 단위 테스트
  - `mvn test -pl myrpg -Dtest="Skill*"` 통과 확인

---

### B. `SkillService` instanceof 치환 (Application Layer)

- [x] 7. `SkillService` 표시/값 메서드 다형 호출 전환
  - [x] 7.1 `primaryStatLabel`/`primaryValue`/`resourceCost` → `catalog.primaryStatLabel()`/`primaryValueAt(rank)`/`resourceCostAt(rank)`
  - [x] 7.2 `counterValue`/`critBonusValue` → `counterValueAt(rank).orElse(null)`/`critBonusValueAt(rank).orElse(null)`
  - [x] 7.3 `calculateRankable`의 `instanceof PassiveSkill` → `catalog.isPassive()`
  - _Requirements: 1.5_ / _Design: 3.4_

- [x] 8. `SkillService` 회귀 테스트 확인/보강
  - [x] 8.1 기존 `SkillService`/카탈로그 테스트 그린 유지, 필요 시 동치성 케이스 보강
  - _Validates: Requirements 1.2, 1.3, 3.1_

- [x] 9. **체크포인트 B** — 서비스 계층 빌드 & 테스트
  - `mvn test -pl myrpg -Dtest="Skill*"` 통과 확인

---

### C. `BattleService` instanceof 치환 (Application Layer)

- [x] 10. 전투 스탯 해석 다형 호출 전환
  - [x] 10.1 `resolvePlayerMultiplier`/`resolvePlayerBlockRate`/`resolvePlayerCounterPercent`/`resolveResourceCost` 다형 치환 (폴백 분기 유지)
  - [x] 10.2 `resolveEffectivePlayerCritical`/`resolvePlayerHitCount` 다형/switch 치환
  - _Requirements: 2.1_ / _Design: 3.3_

- [x] 11. `takeTurn` 전투 디스패치 Visitor 전환
  - [x] 11.1 `CombatDispatchVisitor implements SkillCombatVisitor<TurnCombatResult>` 내부 구현 (협력객체 캡처)
  - [x] 11.2 `visitXxx`가 기존 `resolveXxxCombat` 호출, `takeTurn`의 if/else instanceof 사슬을 `skill.accept(visitor)`로 대체
  - [x] 11.3 DotSkill 사이드이펙트 및 궁극기 쿨타임/미보유 방어 분기 동작 보존
  - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.6_ / _Design: 3.3_

- [x] 12. `BattleService` 회귀 + 동작 보존 PBT
  - [x] 12.1 기존 `BattleService*Test` 전수 그린 유지
  - [x] 12.2 `BattleCombatPreservationPropertyTest`[신규, jqwik] — **Property 3(동작 보존), Property 4(폴백)** (시드 고정 Random)
  - _Validates: Requirements 2.2, 3.2, 3.3_

- [x] 13. **체크포인트 C** — 전투 계층 빌드 & 테스트
  - `mvn test -pl myrpg -Dtest="Battle*"` 통과 확인

---

### D. 전체 회귀 및 동작 보존 검증 (Regression)

- [x] 14. 모듈 전체 테스트 회귀
  - [x] 14.1 `mvn test -pl myrpg` 전체 단위/통합/슬라이스 테스트 그린 확인
  - [x] 14.2 잔여 스킬 서브타입 `instanceof` 부재 확인 (grep 재검증, 아이템 instanceof는 out-of-scope 제외)
  - _Validates: Requirements 1.5, 2.6, 3.1, 3.2, 3.4_

---

### E. 5대 품질 가드레일 및 마무리 (Quality Guardrails)

- [x] 15. 5대 품질 가드레일 전체 파이프라인 검증
  - [x] 15.1 `mvn -B -q spotless:apply -pl myrpg` — 포맷팅 자동 교정
  - [x] 15.2 `mvn -B clean install -pl myrpg -am` — 컴파일/Error Prone/ArchUnit/JaCoCo(80%+)/PMD·CPD 전수 검증
  - [x] 15.3 PMD `CognitiveComplexity` 및 CPD 중복이 리팩토링 이전 대비 감소했는지 확인
  - _Requirements: 4.1_ / _Design: 6.3_

- [x] 16. CodeGraph 인덱스 동기화
  - [x] 16.1 `codegraph sync` 실행하여 심볼/호출 관계 갱신
  - _Workflow 규칙 준수_

- [x] 17. Memory Bank 갱신 (Compaction)
  - [x] 17.1 `memory-bank/activeContext.md`에 020 완료 요약 및 다음 단계(2·3순위) 기록
  - _AGENTS.md 규칙 준수_

- [x] 18. Git 커밋 (사용자 요청 시)
  - [x] 18.1 `rules/workflow/git-workflow.md` 준수 커밋 메시지 작성
  - _Workflow 규칙 준수_

---

## 요구사항 역추적 (Requirements Traceability)

| Requirement | 대응 Task |
|---|---|
| Req 1 (다형 값 계약: 1.1~1.7, 라벨/예외/sentinel 보존 포함) | 3, 4, 5, 7, 8 |
| Req 2 (전투 해석 fallback 보존 + Visitor 디스패치: 2.1~2.6) | 2, 10, 11, 12 |
| Req 3 (동작 보존: 3.3 예외보존, 3.5 characterization 선행) | 1.3, 5.2, 8, 12, 14 |
| Req 4 (5대 가드레일) | 15, 16 |
