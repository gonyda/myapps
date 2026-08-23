# Implementation Plan: 014-skill-system-expansion

> **폴더 위치 가이드**: `.kiro/specs/myrpg/014-skill-system-expansion/tasks.md`  
> **관련 규칙**: `rules/workflow/task-build-validation.md`, `rules/workflow/git-workflow.md`, `.kiro/specs/myrpg/014-skill-system-expansion/requirements.md`, `design.md`

---

## Overview

본 작업 명세는 `myrpg` 모듈(`com.myapps.web.myrpg`)에 29종 스킬 시스템 확장, Sealed Interface 8종 다형성 도메인 모델, 10종 `SkillType` 표준 체계, 전투 엔진 상성 및 특수 메커니즘, 4대 수련 체계, 프론트엔드 UI를 점진적으로 구현하기 위한 체크포인트 단위 작업 목록이다.

### 구현 순서 및 원칙
1. **Bottom-Up 계층 조립**:  
   **A. 데이터/도메인(엔티티·Record·순수 계산)** $\rightarrow$ **B. 애플리케이션 서비스(카탈로그·전투·육성 오케스트레이션)** $\rightarrow$ **C. 웹 컨트롤러(엔드포인트·DTO)** $\rightarrow$ **D. 프론트엔드(Thymeleaf·CSS·JS)** $\rightarrow$ **E. 5대 가드레일 통합 검증** 순으로 구현한다.
2. **원자적 완료 및 빌드 그린**:  
   기존 코드 확장 시 영향받는 호출부와 테스트를 함께 수정하여 각 단계 완료 시점에 빌드가 반드시 그린(`BUILD SUCCESS`)이어야 한다.
3. **5대 품질 가드레일 준수**:  
   Spotless(포맷팅) $\rightarrow$ Error Prone(정적 결함) $\rightarrow$ ArchUnit(계층 아키텍처) $\rightarrow$ JaCoCo(커버리지 80%+) $\rightarrow$ PMD/CPD(복잡도/중복)를 필수 검증한다.

---

## Tasks

### A. 도메인 모델, Sealed Records & JPA 엔티티 확장 (Domain & Data Layer)

- [ ] 1. 도메인 열거형 확장
  - [ ] 1.1 `SkillType.java` 확장: `NORMAL`, `HEAVY`, `DEFENSE`, `RECOVERY`, `ULTIMATE`, `PASSIVE`, `BUFF`, `DEBUFF`, `CC`, `DOT` 10종 상수 정의
  - [ ] 1.2 `BonusTarget.java` 확장: `STR`, `DEX`, `INT`, `DEF`, `CRITICAL`, `HP`, `MP`, `STAMINA`, `MP_REGEN` 상수 정의
  - _Requirements: 1.2, 6.3_ / _Design: 2.1, 3.3_

- [ ] 2. `Skill` Sealed Interface 및 8종 Domain Record 구현
  - [ ] 2.1 `Skill.java` sealed interface permits 절 확장 (8종 record 허용)
  - [ ] 2.2 `DamageSkill.java` 확장 (`minHits`, `maxHits`, `defensePierce`, `freezeRateByRank` 필드 및 하위호환 생성자)
  - [ ] 2.3 `DefenseSkill.java` 유지 및 보강
  - [ ] 2.4 `RecoverySkill.java`[신규] 생성 (`healAmountByRank`, `resourceCostByRank`)
  - [ ] 2.5 `UltimateSkill.java`[신규] 생성 (`multiplierByRank`, `hitCountByRank`, `coolWinsByRank`)
  - [ ] 2.6 `PassiveSkill.java`[신규] 생성 (`totalStatBonus`)
  - [ ] 2.7 `BuffSkill.java`[신규] 생성 (`durationTurns`, `absorbRateByRank`)
  - [ ] 2.8 `CcSkill.java`[신규] 생성 (`successRateByRank`)
  - [ ] 2.9 `DotSkill.java`[신규] 생성 (`initialMultiplierByRank`, `dotPerTurnByRank`, `dotTurnsByRank`)
  - _Requirements: 1.1, 1.2_ / _Design: 3.3_

- [ ] 3. JPA 엔티티 확장
  - [ ] 3.1 `BattleState.java` 필드 확장 (`nextAttackAmpPercent`, `manaShieldTurnsLeft`, `manaShieldAbsorbRate`, `monsterStunnedTurns`, `dotDamagePerTurn`, `dotTurnsLeft`) 및 비즈니스 메서드 추가
  - [ ] 3.2 `CharacterSkill.java` 필드 확장 (`ultimateCooldown` 및 `decrementUltimateCooldown()` 메서드 추가)
  - _Requirements: 3.1, 4.3, 5.1, 5.4_ / _Design: 4.1, 4.2_

- [ ] 4. 도메인 순수 계산 엔진 및 정책 리팩토링
  - [ ] 4.1 `SkillRankupBonus.java` 리팩토링 (패시브 6종 F→MASTER 선형 스탯 분배 및 누적 합산 로직 구현)
  - [ ] 4.2 `SkillRankPolicy.java` 확장 (F→MASTER 총 200 AP 및 궁극기 전용 1~20회 수련 요구치 정의)
  - [ ] 4.3 `SkillDamagePolicy.java` 확장 (궁극기, 힐링, 마나실드 등 신규 record 수치 조회 메서드 지원)
  - [ ] 4.4 `BattleResolver.java` 확장 (라이트닝 로드 방어력 0 관통 계산 지원)
  - _Requirements: 2.2, 3.3, 6.3, 7.1_ / _Design: 3.3_

- [ ] 5. 도메인 단위 및 jqwik 프로퍼티 테스트 작성
  - [ ] 5.1 `SkillRankupBonusTest.java` / `SkillRankPolicyTest.java` 단위 테스트 갱신 및 신규 케이스 추가
  - [ ] 5.2 `BattleResolverDamagePiercePropertyTest.java`[신규, jqwik] — **Property 2, 3** 랜덤 타수 및 방어 관통 불변식 검증
  - [ ] 5.3 `SkillRankupBonusPropertyTest.java`[jqwik] — **Property 6** 패시브 선형 누적 불변식 검증
  - _Validates: Requirements 1.1, 2.1, 2.2, 6.3_ / _Design: 5.0_

- [ ] 6. **체크포인트 A** — 도메인 계층 빌드 & 단위 테스트 검증
  - `mvn test -pl myrpg -Dtest="*Skill*,*Policy*,*Bonus*,*Resolver*"` 통과 확인

---

### B. 카탈로그 및 애플리케이션 서비스 계층 구현 (Application Service Layer)

- [ ] 7. JSON 카탈로그 29종 데이터 완비 및 로더 확장
  - [ ] 7.1 `data/skill.json` 29종 스킬 16키 랭크맵 및 메타데이터 전면 갱신 (Python `verify_all_skills.py` 정합성 0건 오류 일치)
  - [ ] 7.2 `SkillCatalogService.java` 파싱 `switch` 분기 확장 (8종 Sealed Record 전면 매핑)
  - [ ] 7.3 `SkillCatalogDataConformancePropertyTest.java` 29종 스킬 대상 **Property 1** 단조성 전수 검증
  - _Requirements: 1.3, 1.4_ / _Design: 3.2, 4.3_

- [ ] 8. `SkillService` 육성 및 필드 스킬 로직 확장
  - [ ] 8.1 `buildListView()` 탭 필터링 수정 (`COMMON` 탭에 디펜스 + 패시브 6종 매핑)
  - [ ] 8.2 `rankUp()` 4대 수련 체계 분기 구현 (패시브 AP 즉시 승급, 지원/특수/궁극기 막타 면제, 직접공격 사용+막타)
  - [ ] 8.3 `useFieldSkill(characterId, skillId)` 신설 (MP 부족/HP 최대치 검증, 회복 및 usageCount 증가)
  - [ ] 8.4 `combatSkillList()`에서 `PassiveSkill` 자동 제외 필터링 보장
  - _Requirements: 1.5, 4.2, 6.1, 6.2, 7.2, 7.3, 7.4, 7.5_ / _Design: 3.2_

- [ ] 9. `BattleService` 전투 오케스트레이션 및 상태효과 처리 확장
  - [ ] 9.1 `takeTurn()` 궁극기(`ULTIMATE`) 분기: 몬스터 행동 무시 절대 우위 100% 관통 및 적 공격 차단, 쿨타임 설정
  - [ ] 9.2 `takeTurn()` 힐링(`RECOVERY`) 분기: 턴 소모 + MP 차감 + 즉시 회복 + 몬스터 공격 100% 무방비 피격
  - [ ] 9.3 `takeTurn()` 마나 실드(`BUFF`) 분기: 5턴 지속 등록, 시전 턴부터 MP 감쇄 흡수 및 MP 고갈 시 HP 전가, 재시전 갱신
  - [ ] 9.4 `takeTurn()` CC(`spider_shot`) 및 빙결(`ice_spear`) 분기: 시전 턴 피격 후 성공 시 `monsterStunnedTurns = 1` 예약
  - [ ] 9.5 `takeTurn()` 턴 시작 속박 처리: `monsterStunnedTurns > 0`일 때 몬스터 턴 스킵 및 턴 차감
  - [ ] 9.6 `takeTurn()` 도트(`mirage_missile`) 분기: 즉발 30% + 매 턴 독 피해 적용 및 턴 차감, 재시전 갱신
  - [ ] 9.7 `takeTurn()` 디버프(`rage_impact`) 분기: 다음 일반/강공격 피해 +30% 증폭 (궁극기 증폭 제외)
  - [ ] 9.8 턴 종료 메디테이션 발동: 공방 완료 후 다음 턴 개시 전 시점에 `MP +1~+5` 자연 회복 (필드 미회복)
  - [ ] 9.9 전투 승리 처리: 몬스터 처치 시 보유한 모든 궁극기 `ultimateCooldown` 1회씩 차감
  - [ ] 9.10 `BattleLogFormatter.java` 확장: 신규 스킬 타입(궁극기, 힐링, 마나실드 흡수, CC 속박/빙결, 독 도트, 방어 관통, 피해 증폭, 메디테이션 재생) 전용 전투 로그 멘트 포맷팅 구현
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.2, 3.4, 4.1, 4.3, 4.4, 4.5, 5.1, 5.2, 5.3, 5.4, 5.5, 6.4, 6.5_ / _Design: 3.2_

- [ ] 10. 서비스 계층 단위 및 PBT 테스트 작성
  - [ ] 10.1 `SkillServiceTest.java` / `SkillServiceFieldUseTest.java`[신규] — 랭크업 4대 체계 및 필드 힐링 검증
  - [ ] 10.2 `BattleServiceUltimateTest.java`[신규] — **Property 4** 궁극기 절대우위 및 쿨타임 차감 검증
  - [ ] 10.3 `BattleServiceManaShieldTest.java`[신규] — **Property 5** 마나 실드 감쇄 및 전가 검증
  - [ ] 10.4 `BattleServiceMeditationPropertyTest.java`[신규, jqwik] — **Property 7** 턴 종료 메디테이션 회복 검증
  - [ ] 10.5 `BattleLogFormatterTest.java` 확장 — 신규 7종 스킬 타입 및 특수 상태이상 로그 멘트 생성 전수 검증
  - _Validates: Requirements 3.2, 4.1, 4.4, 6.4, 7.2_ / _Design: 5.0_

- [ ] 11. **체크포인트 B** — 서비스 계층 빌드 & 단위/PBT 테스트 검증
  - `mvn test -pl myrpg -Dtest="SkillService*,BattleService*,SkillCatalog*"` 통과 확인

---

### C. 웹 컨트롤러 계층 구현 (Web Controller Layer)

- [ ] 12. 컨트롤러 엔드포인트 및 DTO 확장
  - [ ] 12.1 `SkillRowView.java` DTO 확장 (`boolean fieldUsable`, `String cooldownBadgeText`)
  - [ ] 12.2 `SkillController.java`에 `POST /skills/{id}/use` 필드 힐링 엔드포인트 구현 (성공/실패 JSON 및 스왑 응답)
  - [ ] 12.3 `BattleController.java` 및 `PlayScreenController.java` 궁극기 쿨타임 상태 모델 전달
  - _Requirements: 3.5, 4.2_ / _Design: 3.1, 4.2_

- [ ] 13. 웹 컨트롤러 슬라이스 테스트 작성
  - [ ] 13.1 `SkillControllerTest.java` 확장 (`GET /skills` 공용 탭 조회 및 `POST /skills/healing/use` 성공/실패 검증)
  - [ ] 13.2 `BattleControllerTest.java` 확장 (궁극기 쿨타임 표기 및 전투 액션 검증)
  - _Validates: Requirements 3.5, 4.2, 6.1_ / _Design: 3.1_

- [ ] 14. **체크포인트 C** — 컨트롤러 계층 빌드 & 웹 테스트 검증
  - `mvn test -pl myrpg -Dtest="*Controller*"` 통과 확인

---

### D. 프론트엔드 UI/UX (Thymeleaf & JS & CSS)

- [ ] 15. Thymeleaf 템플릿 마크업 갱신
  - [ ] 15.1 `templates/fragments/skill-popup.html` 수정 (`공용` 탭에 디펜스 + 패시브 6종 노출, 승급 좌측 `[사용]` 버튼 배치, 궁극기 쿨타임 뱃지)
  - [ ] 15.2 `templates/fragments/battle-view.html` 수정 (궁극기 쿨타임 시 `disabled` + `[🔒 (N승 남음)]`, 준비 완료 시 `[⚡ (READY!)]` 강조 펄스, 패시브 스킬 버튼 미노출)
  - _Requirements: 3.5, 4.2, 6.1, 6.2_ / _Design: 3.4_

- [ ] 16. JavaScript 및 CSS 인터랙션 구현
  - [ ] 16.1 `static/js/myrpg.js`에 `useFieldSkill(skillId)` 작성 (MP 부족/체력 가득 참 `alert()` 피드백 및 상단바/팝업 실시간 갱신)
  - [ ] 16.2 `static/css/myrpg.css`에 `.skill-use-btn`, `.badge-cooldown`, 궁극기 READY 펄스 효과 스타일 추가
  - _Requirements: 3.5, 4.2_ / _Design: 3.4_

- [ ] 17. UI 회귀 및 통합 테스트 검증
  - [ ] 17.1 `VisualJsPreservationAndJsonLoadingIntegrationTest.java` 등 기존 통합 테스트 실행 및 통과 확인
  - _Validates: Requirements 1.3, 3.5, 4.2_

- [ ] 18. **체크포인트 D** — 프론트엔드 연동 빌드 & 통합 테스트 검증
  - `mvn test -pl myrpg` 전체 테스트 통과 확인

---

### E. 전체 통합 검증 및 5대 품질 가드레일 (Integration & Quality Guardrails)

- [ ] 19. 5대 품질 가드레일 전체 파이프라인 검증
  - [ ] 19.1 `mvn -B -q spotless:apply -pl myrpg` — 소스 포맷팅 자동 교정
  - [ ] 19.2 `mvn -B clean install -pl myrpg -am` — Spotless, Error Prone, ArchUnit, JaCoCo(80%+), PMD/CPD 전수 검증
  - _Requirements: 4.1_ / _Design: 6.2_

- [ ] 20. CodeGraph 인덱스 동기화 및 Memory Bank 갱신
  - [ ] 20.1 `codegraph sync` 실행하여 최신 29종 스킬 및 도메인 심볼 인덱스 갱신
  - [ ] 20.2 `memory-bank/activeContext.md`에 완료 내역 요약(Compaction) 및 다음 단계 기록
  - _Workflow & AGENTS.md 규칙 준수_
