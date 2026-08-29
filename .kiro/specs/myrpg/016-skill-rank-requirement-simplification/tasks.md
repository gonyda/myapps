# Implementation Plan: 스킬 승급 조건 단순화 (막타 처치 항목 전면 제거 & 사용 횟수 단일화)

> **폴더 위치 가이드**: `.kiro/specs/myrpg/016-skill-rank-requirement-simplification/tasks.md`  
> **관련 규칙**: `rules/workflow/task-build-validation.md`, `rules/workflow/git-workflow.md`

---

## Overview

본 작업 명세는 `myrpg` Web 모듈(`com.myapps.web.myrpg`)에서 스킬 승급 조건의 막타 처치 항목(`requiredKills`, `killCount`, `onSkillKill`, `isKillExempt`)을 전면 제거하고, 모든 액티브 스킬의 승급 조건을 **'스킬 사용 횟수(Usage Count) + AP 소모'**로 단일화하기 위한 점진적 구현 작업 목록이다.

### 구현 순서 및 원칙
1. **Bottom-Up 계층 조립**:  
   **A. 도메인 모델 & JPA 엔티티 정제** $\rightarrow$ **B. DTO 및 애플리케이션 서비스** $\rightarrow$ **C. 프론트엔드 UI/UX** $\rightarrow$ **D. 테스트 스위트 갱신** $\rightarrow$ **E. 5대 가드레일 통합 검증** 순으로 구현한다.
2. **원자적 완료 및 빌드 그린**:  
   각 단계 완료 시점에 모든 단위/통합 테스트가 100% 통과하고 컴파일 결함이 없어야 한다.
3. **5대 품질 가드레일 준수**:  
   Spotless $\rightarrow$ Error Prone $\rightarrow$ ArchUnit $\rightarrow$ JaCoCo(80%+) $\rightarrow$ PMD/CPD 검증 및 `codegraph sync`를 수행한다.

---

## Tasks

### A. 도메인 모델 & JPA 엔티티 정제 (Data & Domain Layer)

- [x] 1. `RankUpRequirement` record 리팩토링
  - [x] 1.1 `RankUpRequirement.java`에서 `requiredKills` 필드 삭제 및 `record RankUpRequirement(int requiredUsage)`로 단순화
  - _Requirements: 1.4_ / _Design: 3.1.1_

- [x] 2. `SkillRankPolicy` 정책 클래스 단순화
  - [x] 2.1 `REQUIREMENTS` 배열 내 15개 랭크 전이 요소를 `new RankUpRequirement(usage)` 단일 인자 생성자로 교체
  - [x] 2.2 `ULTIMATE_REQUIREMENTS` 배열도 단일 인자 생성자로 교체
  - [x] 2.3 `requirementFor(rank, type)`에서 패시브 스킬일 경우 `new RankUpRequirement(0)` 반환
  - _Requirements: 1.1, 2.2_ / _Design: 3.1.2_

- [x] 3. `SkillType` 및 `Skill` 인터페이스 정리
  - [x] 3.1 `SkillType.java`에서 `isKillExempt()` 메서드 삭제
  - [x] 3.2 `Skill.java`에서 `default boolean isKillExempt()` 메서드 삭제
  - _Requirements: 1.5_ / _Design: 2.1_

- [x] 4. `CharacterSkill` JPA 엔티티 정제
  - [x] 4.1 `killCount` 컬럼/필드 및 `getKillCount()`, `increaseKill()`, `setKillCount()` 메서드 삭제
  - [x] 4.2 생성자 파라미터에서 `killCount` 제거 및 하위호환 생성자 정리
  - [x] 4.3 `newSkill(characterId, skillId)` 팩토리 메서드 정리
  - [x] 4.4 `rankUpTo(next)`에서 `this.usageCount = 0;`만 수행하도록 간소화
  - _Requirements: 1.3, 3.3_ / _Design: 3.1.3_

- [x] 5. **체크포인트 A** — 도메인 계층 수정 완료 확인
  - 도메인 모델 및 엔티티 시그니처 정제 완료

---

### B. DTO 및 애플리케이션 서비스 리팩토링 (Application Service Layer)

- [x] 6. `SkillRankUpView` DTO 정리
  - [x] 6.1 `SkillRankUpView.java`에서 `killCurrent`, `killRequired`, `hasKillRequirement` 필드 삭제
  - [x] 6.2 호환 생성자 제거 및 23-arg 표준 Record 정의 유지
  - _Requirements: 4.4_ / _Design: 3.2.1_

- [x] 7. `SkillService` 승급 및 뷰 빌더 로직 단순화
  - [x] 7.1 `rankUp(progress, skillId)`: `killExempt` 및 `killCount < requirement.requiredKills()` 검증 구문 삭제
  - [x] 7.2 `calculateProgressPercent()`: 막타 비율(`killRatio`) 합산 로직 제거, `usageRatio * FULL_PROGRESS_PERCENT` 단일화
  - [x] 7.3 `calculateRankable()`: 막타 검사 조건 제거 (`characterSkill.getUsageCount() >= requirement.requiredUsage()`)
  - [x] 7.4 `buildRankUpView()`: `RankUpRequirementInfo`에서 `killRequired` 제거 및 `SkillRankUpView` 생성 인자 바인딩
  - [x] 7.5 `onSkillKill()` 및 `isKillExempt()` 메서드 완전 삭제
  - _Requirements: 1.1, 1.2, 3.2, 4.3_ / _Design: 3.2.2_

- [x] 8. `BattleService` 전투 파이프라인 정리
  - [x] 8.1 `BattleService.java`에서 전투 턴 종료 시 `skillService.onSkillKill(...)` 호출 구문 삭제
  - _Requirements: 3.1_ / _Design: 2.1_

- [x] 9. **체크포인트 B** — 애플리케이션 계층 연동 완료
  - 서비스/전투 파이프라인 컴파일 및 기본 바인딩 검증

---

### C. 프론트엔드 UI/UX 템플릿 수정 (Presentation Layer)

- [x] 10. `fragments/skill-popup.html` 마크업 정리
  - [x] 10.1 `rankup-modal` 프래그먼트 내 `th:if="${rankUp.hasKillRequirement()}"` 막타 표시 행 완전 삭제
  - [x] 10.2 사용 횟수(`usageCurrent / usageRequired`) 행과 패시브 안내 영역이 시각적으로 깔끔하게 렌더링되도록 확인
  - _Requirements: 4.1, 4.2_ / _Design: 3.3_

- [x] 11. **체크포인트 C** — 템플릿 마크업 검증
  - 프론트엔드 Thymeleaf 템플릿 구문 정상 검증

---

### D. 테스트 스위트 전수 갱신 및 검증 (Test Layer)

- [x] 12. 도메인 단위 및 jqwik 프로퍼티 테스트 갱신
  - [x] 12.1 `SkillRankPolicyTest.java`: `requiredKills` 단언문 삭제 및 `requiredUsage` 검증 위주 수정
  - [x] 12.2 `SkillRankRequirementPropertyTest.java`: `requiredKills` 단조증가 검증 삭제, `requiredUsage` 단조증가 검증 유지
  - [x] 12.3 `CharacterSkillTest.java`: `increaseKill` 테스트 삭제 및 `rankUpTo` 사용횟수 리셋 검증
  - [x] 12.4 `CharacterSkillPersistencePropertyTest.java`: `CharacterSkill` 생성자 갱신
  - [x] 12.5 `SkillRankupBonusTest.java` & `SkillRankupBonusPropertyTest.java`: `CharacterSkill` 생성자 갱신
  - [x] 12.6 `SkillPolymorphismTest.java`: `isKillExempt()` 단언문 삭제

- [x] 13. 애플리케이션 서비스 및 PBT 테스트 갱신
  - [x] 13.1 `SkillRankUpGatePropertyTest.java`: `killCount` 생성자 및 임의값 생성기 제거, `usageCount >= required && AP >= apCost` 게이트 검증으로 단순화 (**Property 1**)
  - [x] 13.2 `SkillRankUpDefenseKillExemptPropertyTest.java`: 모든 액티브 스킬이 막타 없이 사용횟수+AP만으로 승급 가능함을 검증하는 `SkillRankUpActiveUsageOnlyPropertyTest`로 개편
  - [x] 13.3 `SkillServiceTest.java`: `onSkillKill` 테스트 삭제, `rankUp` 사용횟수 승급 시나리오 검증
  - [x] 13.4 `SkillServiceViewTest.java`: `CharacterSkill` 생성자 및 `SkillRankUpView` 단언문 갱신
  - [x] 13.5 `SkillServiceFieldUseTest.java`, `SkillApInvariantPropertyTest.java`, `SkillRankUpApGuardPropertyTest.java`, `SkillRankUpEffectPropertyTest.java`, `CombatSkillListPropertyTest.java`, `AdminPresetPropertyTest.java`, `SkillRebirthRetentionPropertyTest.java`, `SkillSeedPropertyTest.java`, `SkillProgressPercentPropertyTest.java`, `LearnSkillPropertyTest.java`: `CharacterSkill` 생성자 및 검증 갱신
  - [x] 13.6 `BattleServiceTurnIntegrationTest.java`: `onSkillKill` verify 단언문 삭제

- [x] 14. **체크포인트 D** — 전체 단위 및 통합 테스트 검증
  - `mvn test -pl myrpg` 전체 테스트 100% 그린 확인

---

### E. 전체 통합 검증 및 5대 품질 가드레일 (Integration & Quality Guardrails)

- [x] 15. 5대 품질 가드레일 전체 파이프라인 검증
  - [x] 15.1 `mvn -B -q spotless:apply -pl myrpg` — 소스 포맷팅 자동 교정
  - [x] 15.2 `mvn -B clean install -pl myrpg -am` — 컴파일, 아키텍처, 커버리지(80%+), PMD/CPD 전수 검증
  - _Requirements: 4.1_ / _Design: 5.2_

- [x] 16. CodeGraph 인덱스 동기화
  - [x] 16.1 `codegraph sync` 실행하여 최신 심볼 및 호출 관계 인덱스 갱신

- [x] 17. Memory Bank 갱신 (Compaction)
  - [x] 17.1 `memory-bank/activeContext.md`에 스킬 승급 조건 단순화 완료 내역 요약 및 다음 단계 갱신
  - _AGENTS.md 규칙 준수_
