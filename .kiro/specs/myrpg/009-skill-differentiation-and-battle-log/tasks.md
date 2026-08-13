# Implementation Plan: 딜 스킬 2축 차별화 + 전투 로그 재설계

## Overview

스펙 008(`battle-system`)·005(`skill-system`)까지 구축한 `myrpg` Web 모듈(`com.myapps.web.myrpg`) 위에, 설계 문서의 DDD 4계층 구조를 따라 **딜 스킬 2축(`hitCount`·`critBonus`)** 과 **전투 로그 UI 재설계**를 점진적으로 구현한다.

구현 순서 원칙:

- **A. 카탈로그·데이터(스킬 필드·파싱·skill.json)** → **B. 순수 멀티히트 산출(BattleResolver)** → **C. 오케스트레이션(BattleService 조립·로그 분리)** → **D. 로그 포맷(BattleLogFormatter)** → **E. 표현(battle-view·controller·css)** 순으로 조립한다.
- 하위 계층(데이터/순수 로직) → 상위 계층(오케스트레이션/표현) 순으로 쌓으며, 각 순수 로직 직후 설계의 Correctness Property를 jqwik으로 확인한다.
- **기존 산출물 확장(`DamageSkill`·`SkillCatalogService`·`BattleResolver`·`TurnInput`·`ResolvedTurn`·`BattleTurnResult`·`BattleLogInput`·`BattleService`·`BattleLogFormatter`·`BattleController`·`battle-view.html`)은 호출부·영향 테스트를 함께 갱신하여 원자적으로 완료**하고, 완료 시점에 빌드가 그린이어야 한다.
- `hitCount == 1`·`critBonus == 0`에서 기존 008 동작과 난수 시퀀스·결과가 동일해야 한다(하위호환).

> **테스트 정책(`task-build-validation.md`)**: "optional task"는 없다. 아래 모든 테스트 하위 작업은 **필수**이며 `*`를 사용하지 않는다. 각 Task는 `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인 후에만 완료 처리한다.

> **Spring Boot 4.0 / Java 25 규약**: 생성자 주입만(`@Autowired` 금지), Lombok 금지, `var` 금지, VO/DTO는 `record`, `final` 파라미터/지역변수, 커스텀 예외(`RuntimeException` 직접 금지). jqwik 프로퍼티는 `Mockito.mock()` 직접(`@Mock` 금지), `@Property(tries = 100)`. 프로퍼티 태그 주석: `Feature: 009-skill-differentiation-and-battle-log, Property {번호}: {프로퍼티 텍스트}`.

> **소스 정리(`code-style`)**: 소스 수정 Task는 완료 전 미사용 import/변수 제거·매직넘버 상수화·메서드 분리(50줄 초과 시)·불필요 주석 제거를 수행한다. 상한/기본값(`MAX_HIT_COUNT`·`MAX_CRIT_BONUS`·`DEFAULT_*`·`CRITICAL_ROLL_MAX`)은 `private static final` 상수로 둔다.

> **밸런스 일치(`data-balance-guide.md`)**: 공식(§0)·9개 딜 스킬 수치(§4)와 정확히 일치시킨다. 마법 `critBonus` 0, `critBonus` 상한 100, 다단 총 배율 밴드(3타 105→195·4타 108→200) 준수.

## Tasks

### A. 카탈로그 · 데이터

- [x] 1. HitResult + DamageSkill 확장 + 카탈로그 파싱/검증
  - [x] 1.1 HitResult record · DamageSkill 필드 · SkillCatalogService 파싱
    - `domain/model/HitResult.java`(신규 record): `(int damage, boolean critical)`
    - `domain/model/DamageSkill.java`[확장]: `hitCount`·`critBonus` 컴포넌트 추가(9-인자) + 7-인자 보조 생성자(hitCount=1, critBonus=0)로 하위호환. `multiplierByRank`=1히트당 배율(주석 갱신)
    - `application/service/SkillCatalogService.java`[확장]: `extractOptionalInt(node, field, skillId, default, min, max)` 신규, `parseDamageSkill`에서 `hitCount`(기본1, [1,8])·`critBonus`(기본0, [0,100]) 파싱. 범위/비숫자 시 `SkillDataException`. `parseDefenseSkill`은 미파싱. 상수 `DEFAULT_HIT_COUNT`/`MIN_HIT_COUNT`/`MAX_HIT_COUNT`/`DEFAULT_CRIT_BONUS`/`MIN_CRIT_BONUS`/`MAX_CRIT_BONUS`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [x] 1.2 스킬 필드·파싱 프로퍼티/단위 테스트
    - `DamageSkillTest.java`(신규) — 7-인자 보조 생성자(→hitCount1/critBonus0), 9-인자 생성 예시
    - `SkillCatalogHitCountCritBonusPropertyTest.java`(신규) — **Property 6: 카탈로그 파싱 기본값·검증** — 부재→기본값, `hitCount<1`·`critBonus∉[0,100]`·비숫자→예외, 16키/단조 유지
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.6**

- [x] 2. skill.json 9개 딜 스킬 확정
  - [x] 2.1 skill.json 데이터 갱신
    - `resources/data/skill.json`[확장]: 9개 딜 스킬에 `hitCount`·`critBonus` 명시 + per-hit 배율 갱신(§4 확정표): `windmill`/`icebolt` 3타(히트당 F35→65), `arrow_revolver` 4타(히트당 F27→50), `smash` critBonus 80·`magnum_shot` critBonus 100, 마법 3종 critBonus 0, 단일 스킬 밴드 유지. 모든 `multiplierByRank` 16키·단조 유지. 방어 스킬 무변경
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

  - [x] 2.2 skill.json 데이터 규격 프로퍼티 테스트
    - `SkillCatalogDataConformancePropertyTest.java`(신규) — **Property 10: skill.json 데이터 규격** — 9개 스킬 hitCount/critBonus 확정표 일치, 마법 critBonus 0, critBonus≤100, 16키·단조, 다단 총 배율 밴드
    - **Validates: Requirements 3.1, 3.2, 3.4, 3.5, 3.6, 12.2, 12.4, 12.5**

- [x] 3. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

### B. 순수 멀티히트 산출 (BattleResolver)

- [x] 4. 멀티히트 데미지 + TurnInput/ResolvedTurn 확장
  - [x] 4.1 BattleResolver.multiHitDamage + 공격 경로 멀티히트화
    - `domain/model/TurnInput.java`[확장]: 끝에 `int playerHitCount` 추가(`playerMultiplierPercent`=1히트당 의미)
    - `domain/model/ResolvedTurn.java`[확장]: 끝에 `List<HitResult> playerHits` 추가
    - `domain/service/BattleResolver.java`[확장]: `multiHitDamage(attackPower, perHitMultiplierPercent, targetDefense, affinityCoefficient, critChance, hitCount):List<HitResult>`(히트별 `rollCritical`→`finalDamage`), `resolveAttackWins`·`resolveDrawAttack`·`resolveNormalLosesToDefense`가 이를 호출해 합계=`playerDamageToMonster`·리스트=`playerHits`. 반격/몬스터/관통패(0)/교착(0) 단일 유지·`playerHits=List.of()`. `resolve`가 `playerHitCount` 전달
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 6.1(히트별 상세)_

  - [x] 4.2 멀티히트 프로퍼티/단위 테스트
    - `BattleResolverSingleHitBackwardCompatPropertyTest.java` — **Property 1: 단일 히트 하위호환 동치**(시드 고정)
    - **Validates: Requirements 4.5, 11.1**
    - `BattleResolverMultiHitSumPropertyTest.java` — **Property 2: 멀티히트 합산·최소 보장**
    - **Validates: Requirements 4.1, 4.4**
    - `BattleResolverMultiHitDefensePropertyTest.java` — **Property 3: 히트별 방어 차감**
    - **Validates: Requirements 4.2, 4.7**
    - `BattleResolverMultiHitDeterminismPropertyTest.java` — **Property 4: 히트별 독립 크리·편차(결정성)**(시드 고정)
    - **Validates: Requirements 4.3, 4.8**
    - `BattleResolverMonsterSingleHitPropertyTest.java` — **Property 9: 몬스터·반격 단일 히트 불변**
    - **Validates: Requirements 4.6, 6.4**
    - `BattleResolverTest.java`[확장] — 3타/4타 예시, 고방어 폭락, 단일 동치 예시

- [x] 5. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

### C. 오케스트레이션 (BattleService) + 히트별 결과 전달

- [x] 6. BattleService 조립 (크리 보너스 · hitCount · 선제 멀티히트) + 턴 결과 확장
  - [x] 6.1 크리 보너스·hitCount 조립 + BattleTurnResult 확장
    - `domain/model/BattleTurnResult.java`[확장]: `List<HitResult> playerHits` 추가, `logLines`→`combatLines`(액션 라인, 중앙용)로 의미/이름 정리. `playerDamage == Σ playerHits.damage`
    - `application/service/BattleService.java`[확장]: `resolvePlayerCritical(skill, progress)=min(CRITICAL_ROLL_MAX, 캐릭터크리 + (DamageSkill?critBonus:0))`, `resolvePlayerHitCount(skill)=DamageSkill?hitCount:1`, `resolveNormalCombat`가 `TurnInput`에 `playerHitCount` 포함·`playerHits` 전달, `resolveBowFirstStrike`가 `resolver.multiHitDamage(...,1.0,실효크리,hitCount)`로 멀티히트, `TurnCombatResult`(내부)에 `playerHits` 추가. `CRITICAL_ROLL_MAX=1000` 상수
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.2, 6.4, 6.5, 7.5, 11.2_

  - [x] 6.2 크리 보너스 프로퍼티/통합 테스트
    - `BattleServiceEffectiveCriticalPropertyTest.java` — **Property 5: 실효 크리 = 캐릭터 크리 + critBonus(상한)** — 딜 스킬 합산·상한 1000, 방어/몬스터 무영향
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5**
    - `BattleServiceCritBonusTest.java`(신규, Mockito) — `smash`/`magnum_shot` 실효 크리=캐릭터+보너스, 마법 스킬 +0

- [x] 7. BattleService 로그 분리 (액션 ↔ 결산)
  - [x] 7.1 combatLines(중앙) ↔ actionLog(하단) 분리
    - `application/service/BattleService.java`[확장]: 액션 라인(플레이어/몬스터 행동·선제·캐스팅 실패·도망 실패)은 `BattleTurnResult.combatLines`에 담고 **`actionLog`에 추가하지 않음**. 결산(`processKillReward` 골드/아이템/경험치)·사망(`handleDeath`)·도망 성공은 `actionLog.add(line, LOG_TYPE_COMBAT)`(하단). `start`의 하단 시작 로그 제거(인트로는 컨트롤러 turnLog). `flee` 성공→하단, 실패→combatLines
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_

  - [x] 7.2 로그 라우팅 프로퍼티/통합 테스트
    - `BattleServiceLogRoutingPropertyTest.java` — **Property 8: 액션↔결산 로그 라우팅** — 액션 라인 combatLines·actionLog 미추가, 결산/사망/도망성공 actionLog 추가
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5**
    - `BattleServiceLogSplitTest.java`(신규, Mockito verify) — 승리 턴: 결산 라인만 `actionLog.add` 호출, 액션 라인 미추가; 시작 로그 하단 미추가
    - 기존 `BattleService*` 테스트[갱신]: `combatLines`/`playerHits` 시그니처 반영, 단일 히트 무회귀

- [x] 8. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

### D. 로그 포맷 (BattleLogFormatter C안)

- [x] 9. 멀티히트 하이브리드 로그 포맷
  - [x] 9.1 BattleLogInput 확장 + BattleLogFormatter 멀티히트 분기
    - `application/dto/BattleLogInput.java`[확장]: `List<HitResult> playerHits` 추가(`playerDamage` 합계 유지)
    - `application/service/BattleLogFormatter.java`[확장]: `addPlayerLine`에서 `playerHits.size() >= 2`면 헤더(`{스킬}({타입}) {N}연타`)+브레이크다운(`{d1}  {d2}(치명)  … = {합계} 피해`), size ≤ 1이면 기존 단일 형식. 선제(firstStrike) 멀티히트 반영. 몬스터/방어/빗나감/캐스팅 실패 문구 보존
    - `BattleService`의 `BattleLogInput` 생성부[확장]: `playerHits` 전달
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 6.2, 6.3, 6.5_

  - [x] 9.2 로그 포맷 프로퍼티/단위 테스트
    - `BattleLogFormatterMultiHitPropertyTest.java` — **Property 7: 멀티히트 로그 포맷** — size≥2 헤더+브레이크다운(치명 표기·합계), size≤1 단일 형식
    - **Validates: Requirements 7.1, 7.2, 7.3, 6.5**
    - `BattleLogFormatterTest.java`[확장] — C안 3타/4타 예시, 크리 `(치명)`, 단일/방어/몬스터/빗나감/실패 보존

- [x] 10. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

### E. 표현 (전투 뷰 중앙 로그 · 컨트롤러 · css)

- [x] 11. battle-view 중앙 로그 섹션 + BattleController turnLog
  - [x] 11.1 battle-center 서브프래그먼트 + battle-log + turnLog 배선
    - `templates/fragments/battle-view.html`[확장]: 중복된 전투 화면 본문을 `th:fragment="battle-center"`로 추출(DRY), `battle-view`·`battle-response`가 `th:replace`로 재사용. HP 바와 `#battleSkills` 사이에 `<div class="battle-log" id="battleLog">`(`th:each="line : ${turnLog}"`) 삽입. `battle-skills` 서브프래그먼트는 `battle-center` 내부 유지
    - `interfaces/api/BattleController.java`[확장]: `populateBattleModel`/`buildOngoingBattleResponse`에 `turnLog` 모델 속성(start=`List.of("{monster} Lv.{level} 출현!")`, turn=`result.combatLines()`)
    - `static/css/myrpg.css`[확장]: `.battle-log`·`.battle-log-line` 스타일(중앙 배치, 스크롤 불필요한 소형 영역)
    - _Requirements: 8.3, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 10.1, 10.2, 10.3, 10.4_

  - [x] 11.2 컨트롤러 슬라이스 + 정적 리소스 보존 테스트
    - `BattleControllerTest.java`[확장] — `/battle/turn` 응답 `turnLog` 모델·`battle-log` 렌더, `/battle/start` 인트로 라인, `GET /battle/skills`는 `battle-skills`만(중앙 로그 미포함)
    - `VisualJsPreservationAndJsonLoadingIntegrationTest`[갱신] — `battle-view.html`(`battle-center`·`battle-log`) 기대값, `skill.json` 로드
    - _Requirements: 9.1, 9.5, 11.4_

- [x] 12. 최종 체크포인트 — 모든 테스트 통과 + 빌드 검증
  - `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인. 하위호환(008/005 기존 테스트) 무회귀 확인.

## Notes

- 각 Task는 이전 Task 위에 점진적으로 쌓이며, 표현 계층(11)에서 모든 컴포넌트가 통합된다(고아 코드 없음).
- 기존 산출물 확장(1·4·6·7·9·11)은 호출부·영향 테스트를 함께 갱신하여 **완료 시점에 빌드 그린**을 보장한다. `hitCount==1`·`critBonus==0`에서 008 난수 시퀀스·결과 불변(하위호환).
- 각 Correctness Property는 단 하나의 프로퍼티 테스트로 구현하며, 태그 주석 `Feature: 009-skill-differentiation-and-battle-log, Property {번호}: {프로퍼티 텍스트}`를 부착한다.
- 방어 스킬(`defense`·`counter_attack`)·`ActionLog`·`action-log.html`·`BattleState`는 변경하지 않는다.
- Task 완료 전 `mvn test -pl myrpg` 통과와 `mvn clean install -pl myrpg -am` `BUILD SUCCESS`를 확인하고, 소스 수정 Task는 `code-style` 정리 항목을 완료한다.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "2.1"] },
    { "id": 2, "tasks": ["2.2"] },
    { "id": 3, "tasks": ["4.1"] },
    { "id": 4, "tasks": ["4.2"] },
    { "id": 5, "tasks": ["6.1"] },
    { "id": 6, "tasks": ["6.2", "7.1"] },
    { "id": 7, "tasks": ["7.2", "9.1"] },
    { "id": 8, "tasks": ["9.2", "11.1"] },
    { "id": 9, "tasks": ["11.2"] }
  ]
}
```
