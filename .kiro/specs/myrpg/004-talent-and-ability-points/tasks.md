# Implementation Plan: AP(어빌리티 포인트) & 재능 시스템

## Overview

스펙 003(`character-progression-and-rebirth`)으로 구축한 `myrpg` Web 모듈(`com.myapps.web.myrpg`) 위에, 설계 문서의 DDD 4계층 구조를 따라 AP와 재능 시스템을 점진적으로 구현한다.

구현 순서 원칙:

- 하위 계층(신규 값 타입 → 재능 데이터 enum) → 도메인 계산·엔티티 마이그레이션 → 애플리케이션 서비스 → 뷰 모델/헬퍼 → 표현 계층(컨트롤러/템플릿/정적 리소스) 순으로 조립한다.
- **바이탈별 최대치 리팩터(단일 `vitalMaxFor(int)` 제거, `fullRecover(int)` → `fullRecover(VitalMax)`)와 `rebirth(p, talent)` 시그니처 변경은 호출부·영향 테스트를 함께 갱신하여 원자적으로 완료**한다(설계 "Migration 영향 범위"). 이 마이그레이션 Task는 완료 시점에 빌드가 그린이어야 한다.
- 각 순수 로직을 구현한 직후 설계의 Correctness Property를 jqwik 프로퍼티 테스트로 확인한다.
- 마지막 표현 계층 배선에서 모든 컴포넌트가 통합된다(고아 코드 없음).

> **테스트 정책 안내**: 워크스페이스 스티어링(`task-build-validation.md`)에 따라 "optional task"는 없다. 아래 모든 테스트 하위 작업은 **필수**이며 `*`를 사용하지 않는다. 각 Task는 `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인 후에만 완료 처리한다.

> **Spring Boot 4.0 / Java 25 규약**: 생성자 주입만(`@Autowired` 금지), Lombok 금지, `var` 금지, VO/DTO는 `record`, `final` 파라미터/지역변수, 커스텀 예외(`RuntimeException` 직접 금지 — AP 소모 가드는 표준 선행조건 예외 `IllegalArgumentException` 최소 사용, 정식 예외는 3순위 이연). 테스트는 `@MockitoBean` / `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` / `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`. jqwik 프로퍼티는 `Mockito.mock()` 직접 사용(`@Mock` 금지), `@Property(tries = 100)`. 프로퍼티 테스트 태그 주석 형식: `Feature: 004-talent-and-ability-points, Property {번호}: {프로퍼티 텍스트}`.

## Tasks

- [x] 1. 재능 성장 값 타입 (신규, 기존 빌드 무영향)
  - [x] 1.1 BonusKind / BonusTarget enum 구현
    - `domain/model/BonusKind.java`: `STAT`, `VITAL`
    - `domain/model/BonusTarget.java`: `STR`/`DEX`/`INT`/`CRITICAL`(→`STAT`), `HP`/`MP`/`STAMINA`(→`VITAL`), `kind()` 접근자
    - _Requirements: 11.2_

  - [x] 1.2 TalentBonus / VitalMax record 구현
    - `domain/model/TalentBonus.java`: `record TalentBonus(BonusTarget target, int perLevel)`
    - `domain/model/VitalMax.java`: `record VitalMax(int hp, int mp, int stamina)` + 불변 델타 헬퍼 `withHpDelta`/`withMpDelta`/`withStaminaDelta`
    - _Requirements: 8.1, 11.2_

  - [x] 1.3 값 타입 단위 테스트
    - `VitalMaxTest.java` — 델타 헬퍼가 불변 새 인스턴스를 반환하고 대상 필드만 변경하는지, `TalentBonus` 생성/접근자 검증
    - `BonusTargetTest.java` — 7개 상수의 `kind()` 분류 예시(STAT 4종/VITAL 3종) 검증
    - _Requirements: 8.1, 11.2_

- [x] 2. TalentType 재능 데이터 확장 (라벨 유지, 기존 빌드 무영향)
  - [x] 2.1 Stats 델타 헬퍼 추가
    - `domain/model/Stats.java`: 불변 델타 헬퍼 `withStrDelta`/`withDexDelta`/`withIntDelta`/`withCriticalDelta` 추가(기존 필드/`ZERO`/`createDefault` 유지)
    - _Requirements: 6.2, 7.5_

  - [x] 2.2 TalentType 확장 (재능 데이터·폴백 파서)
    - `domain/model/TalentType.java`: 각 상수에 `primary`(`TalentBonus`)/`secondary`(`TalentBonus`)/`damageBonusPercent`(int)/`effectSummary`(String) 보유 + 접근자, `label()`(003 유지)
    - 값 정의: `MELEE`=primary(STR,+2)/secondary(HP,+5)/10/"근접 데미지 +10%, STR +2/Lv, HP +5/Lv", `ARCHERY`=primary(DEX,+2)/secondary(CRITICAL,+1)/10/"원거리 데미지 +10%, DEX +2/Lv, 치명 +0.1%/Lv", `MAGIC`=primary(INT,+2)/secondary(MP,+5)/10/"마법 데미지 +10%, INT +2/Lv, MP +5/Lv"
    - `static TalentType fromNameOrFallback(String name, TalentType fallback)`: null/공백/미지 상수명이면 fallback 반환
    - _Requirements: 6.1, 7.1, 7.2, 7.3, 9.1, 9.2, 9.3, 10.1, 10.3, 11.1, 11.2, 11.3, 11.4, 11.5_

  - [x] 2.3 재능 데이터 완비 프로퍼티 테스트
    - `TalentTypeCompletenessPropertyTest.java` — **Property 13: 재능 데이터 완비** — 3종 각 상수가 비어있지 않은 `label`/`effectSummary`, 유효 `BonusTarget`·비음수 `perLevel`의 `primary`/`secondary`, 0 이상 `damageBonusPercent` 보유
    - **Validates: Requirements 9.1, 9.2, 9.3, 10.1, 10.3, 11.1, 11.3, 11.5**

  - [x] 2.4 재능 파라미터 폴백 프로퍼티 테스트
    - `TalentFallbackPropertyTest.java` — **Property 14: 재능 파라미터 폴백** — 유효 상수명이면 해당 재능, null/공백/미지값이면 `MELEE` 반환
    - **Validates: Requirements 5.8**

  - [x] 2.5 TalentType 값·라벨 예시 단위 테스트
    - `TalentTypeTest.java` 확장 — 3종 라벨 매핑(003 유지), 각 상수의 `primary`/`secondary`/`damageBonusPercent`/`effectSummary` 구체값, `fromNameOrFallback` 예시(`"ARCHERY"`→ARCHERY, `null`/`""`/`"XXX"`→MELEE)
    - _Requirements: 9.1, 10.1, 10.3, 11.1_

- [x] 3. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. 도메인 계산·엔티티 마이그레이션 + 호출부 교체 (원자적, 빌드 그린 유지)
  - [x] 4.1 StatProgression 재능 오버로드 + 바이탈별 최대치
    - `domain/model/StatProgression.java`: `levelStatsFor(int, TalentType)`(공통 + 재능 스탯 계열 보너스), `vitalMaxFor(int, TalentType) → VitalMax`(공통 바이탈 + 재능 바이탈 계열 보너스) 추가; `applyStatBonus`/`applyVitalBonus`는 `bonus.target().kind()`로 분기 후 `perLevel×(level-1)` 가산
    - 003 단일 `vitalMaxFor(int level)` 제거(공통 계산은 `levelStatsFor(int)` 유지·재사용)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 8.1, 8.2, 12.4_

  - [x] 4.2 CharacterProgress AP 컬럼 + 바이탈별 풀회복
    - `domain/model/CharacterProgress.java`: `abilityPoints`(`@Column(name="ability_points", nullable=false)`) 필드/생성자 인자/`getAbilityPoints()` 추가, `createDefault()` `abilityPoints=0`
    - mutator: `increaseAbilityPoints(int amount)`, `spendAbilityPoints(int amount)`(`amount > abilityPoints`면 `IllegalArgumentException`)
    - `fullRecover(int max)` → `fullRecover(VitalMax vitalMax)`로 변경(hp/mp/stamina 각 필드 대입)
    - _Requirements: 1.1, 2.1, 2.2, 2.3, 2.4, 2.5, 5.1, 13.1, 13.2, 14.3_

  - [x] 4.3 ProgressionService 확장 (AP 지급·선택 재능 환생·VitalMax 풀회복)
    - `application/service/ProgressionService.java`: `gainExperience`에서 `gained>0`이면 `increaseAbilityPoints(gained)` 추가, 풀회복을 `fullRecover(vitalMaxFor(level, p.getTalent()))`로 교체
    - `rebirth(p)` → `rebirth(p, TalentType talent)`: 쿨다운 재검증(불가 시 `CooldownActive`·상태 불변), 가능 시 level=1/exp=0/누적+1/`increaseAbilityPoints(1)`/`setTalent(talent)`/lastRebirthAt=now/`fullRecover(vitalMaxFor(1, talent))`
    - 사망 패널티는 003 그대로(AP 불변)
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 1.6, 5.9, 8.3, 12.1, 12.2, 12.3, 12.4, 12.5_

  - [x] 4.4 뷰 모델·헬퍼·컨트롤러 호출부 교체
    - `application/dto/InfoPopupView.java`: `int abilityPoints`, `String talentEffectSummary` 필드 추가
    - `interfaces/api/PlayScreenViewHelper.java`: `buildTopBar`·`buildInfo`의 HP/MP/Stamina 게이지를 `vitalMaxFor(level, talent)`의 각 대응 필드로 조립, 중앙 스탯을 `levelStatsFor(level, talent)` 본체로 계산, `abilityPoints`·`talentEffectSummary` 매핑
    - `interfaces/api/PlayScreenController.java`: `POST /rebirth`에 `@RequestParam(name="talent", required=false) String talentParam` 수신 → `TalentType.fromNameOrFallback(talentParam, MELEE)` → `rebirth(p, talent)`, 성공 시 `saveTurn`+로그(재능 포함), 쿨다운 시 저장 없이 안내 로그
    - _Requirements: 1.7, 4.1, 4.2, 4.3, 5.8, 8.4, 8.5, 10.2_

  - [x] 4.5 영향받는 003 테스트 갱신 (빌드 그린)
    - 신규 시그니처/AP/바이탈별 최대치에 맞게 갱신: `ProgressionServiceTest`, `RebirthEffectPropertyTest`, `RebirthCooldownPropertyTest`, `GainExperienceLevelUpPropertyTest`, `AccumulatedLevelInvariantPropertyTest`, `MaxLevelCapPropertyTest`, `LevelUpFullRecoveryPropertyTest`, `DeathPenaltyPropertyTest`, `StatProgressionPropertyTest`, `CharacterServiceDefault*Test`, `CharacterProgressRepositoryTest`, `PlayScreenViewHelperTest`, `PlayScreenViewHelperGaugePropertyTest`, `PlayScreenViewHelperInfoTest`, `PlayScreenControllerProgressionTest`
    - 맵/이동/NPC/상황멘트 관련 테스트는 무영향 확인
    - _Requirements: 13.4_

- [x] 5. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. StatProgression 재능 계산 프로퍼티 테스트
  - [x] 6.1 재능별 주 스탯 성장 프로퍼티 테스트
    - `TalentPrimaryStatPropertyTest.java` — **Property 5: 재능별 주 스탯 성장** — `L∈[1,100]`×3종에서 주 스탯 = `공통값(L) + 2×(L-1)`, 비주력 스탯은 공통값과 동일
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4, 11.2**

  - [x] 6.2 재능별 보조 바이탈 성장 프로퍼티 테스트
    - `TalentSecondaryVitalPropertyTest.java` — **Property 6: 재능별 보조 바이탈 성장 (근접/마법)** — `MELEE`는 HP만 `공통+5×(L-1)`, `MAGIC`은 MP만, `ARCHERY`는 세 바이탈 모두 공통값
    - **Validates: Requirements 7.1, 7.2, 7.4, 7.6, 8.1, 8.2**

  - [x] 6.3 재능별 보조 치명 성장 프로퍼티 테스트
    - `TalentSecondaryCriticalPropertyTest.java` — **Property 7: 재능별 보조 치명 성장 (활)** — `ARCHERY`의 Critical = `공통 Critical(L) + 1×(L-1)`, `MELEE`/`MAGIC`은 공통값
    - **Validates: Requirements 7.3, 7.5**

  - [x] 6.4 대상 종류 분류 프로퍼티 테스트
    - `BonusTargetKindPropertyTest.java` — **Property 8: 대상 종류 분류** — `BonusTarget.kind()` 분류가 정확하고, `StatProgression`이 STAT 보너스를 바이탈에·VITAL 보너스를 스탯에 가산하지 않음
    - **Validates: Requirements 7.6, 11.2**

  - [x] 6.5 환생 시 재능 보너스 초기화 프로퍼티 테스트
    - `RebirthTalentResetPropertyTest.java` — **Property 10: 환생 시 재능 보너스 초기화** — 3종 재능에서 `levelStatsFor(1, T)`=공통 기본값, `vitalMaxFor(1, T)`=세 바이탈 모두 100
    - **Validates: Requirements 12.4**

- [x] 7. 진행 서비스·엔티티 프로퍼티/단위 테스트
  - [x] 7.1 AP 지급과 누적레벨 동기 프로퍼티 테스트
    - `AbilityPointGrantPropertyTest.java` — **Property 1: AP 지급과 누적레벨 동기** — 레벨업·환생 임의 시퀀스에서 AP 증가량 = 누적레벨 증가량, 최대레벨에서 둘 다 불변
    - **Validates: Requirements 1.2, 1.3, 1.4, 1.6, 3.2, 3.3**

  - [x] 7.2 AP 정합성 불변식 프로퍼티 테스트
    - `AbilityPointInvariantPropertyTest.java` — **Property 2: AP 정합성 불변식** — 소모 없는 진행상황에서 `abilityPoints == accumulatedLevel - 1`, 신규 생성 시 `0 == 1-1`
    - **Validates: Requirements 1.1, 3.1, 14.3**

  - [x] 7.3 사망 패널티 AP 불변 프로퍼티 테스트
    - `DeathPenaltyAbilityPointPropertyTest.java` — **Property 3: 사망 패널티 AP 불변** — `applyDeathPenalty` 후 `abilityPoints` 불변
    - **Validates: Requirements 1.5**

  - [x] 7.4 AP 소모 가드 프로퍼티 테스트
    - `AbilityPointSpendGuardPropertyTest.java` — **Property 4: AP 소모 가드** — `c≤보유`면 `spendAbilityPoints(c)`로 `c`만큼 감소, `c>보유`면 예외 발생·음수 방지
    - **Validates: Requirements 2.3, 2.4**

  - [x] 7.5 환생 재능 반영과 AP 지급 프로퍼티 테스트
    - `RebirthTalentEffectPropertyTest.java` — **Property 9: 환생 재능 반영과 AP 지급** — `rebirth(p, T)` 후 `talent==T`/level=1/exp=0/누적+1/AP+1/HP·MP·Stamina=`vitalMaxFor(1,T)` 각 필드
    - **Validates: Requirements 12.1, 12.2, 12.3, 3.3, 1.4**

  - [x] 7.6 레벨업/환생 풀회복 프로퍼티 테스트
    - `FullRecoveryVitalMaxPropertyTest.java` — **Property 11: 레벨업/환생 풀회복 (바이탈별)** — 레벨업 1회 이상 또는 환생 후 HP/MP/Stamina 현재값 = `vitalMaxFor(최종 레벨, talent)` 각 대응 필드
    - **Validates: Requirements 8.3, 8.4, 8.5**

  - [x] 7.7 환생 쿨다운 재검증 프로퍼티 테스트
    - `RebirthCooldownRevalidatePropertyTest.java`(`Clock` 고정 주입) — **Property 12: 환생 쿨다운 재검증** — `available==false`면 `rebirth(p, T)`가 `CooldownActive` 반환·재능 포함 상태 불변
    - **Validates: Requirements 5.9**

  - [x] 7.8 진행상황 영속 라운드트립 프로퍼티 테스트
    - `AbilityPointsPersistencePropertyTest.java`(`@DataJpaTest` + `@TestConstructor(ALL)`, `TestEntityManager`) — **Property 15: 진행상황 영속 라운드트립** — `abilityPoints`·`talent` 포함 모든 필드 저장→조회 보존
    - **Validates: Requirements 2.1, 2.2, 13.4**

  - [x] 7.9 AP·재능 성장 예시 단위 테스트
    - `AbilityPointsAndTalentGrowthTest.java` — 신규 생성 AP 0, 레벨업 3회 시 AP 3, 환생 후 AP+1·재능 반영; 재능별 계산 샘플(`MAGIC` Lv.10→INT 55, `ARCHERY` Lv.10→Critical 86(8.6%), `MELEE` Lv.10→HP 235/MP·Stamina 190, `ARCHERY` Lv.10→세 바이탈 190)
    - _Requirements: 1.1, 1.2, 1.4, 6.1, 7.1, 7.2, 7.3, 8.2_

- [x] 8. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. 표현 계층 배선 (정보 팝업·재능 선택 UI)
  - [x] 9.1 info-popup.html 확장 (보유 AP·재능 효과 행)
    - `templates/fragments/info-popup.html`: 재능 행 바로 아래에 `보유 AP`(`${view.info.abilityPoints}`) 행과 `재능 효과`(`${view.info.talentEffectSummary}`) 행 추가([환생하기] 버튼·경과 텍스트는 그대로 유지)
    - _Requirements: 4.1, 4.3, 10.2_

  - [x] 9.2 talent-select.html 신규 + play.html include
    - `templates/fragments/talent-select.html`[신규]: `th:fragment="talent-select"` 오버레이(`#talentSelectOverlay`), `TalentType.values()` 3종을 각 `label()` 버튼(`onclick="confirmRebirth('MELEE')"` 등) + 취소 버튼
    - `templates/play.html`[확장]: `info-popup` 아래에 `talent-select` fragment include
    - _Requirements: 5.3, 5.4, 5.6_

  - [x] 9.3 myrpg.js 확장 (환생 2단계 흐름)
    - `static/js/myrpg.js`: `rebirth()`=1단계 `confirm("환생을 진행하시겠습니까?")` 통과 시 `openTalentSelect()`만 수행, `confirmRebirth(talent)`=`POST /rebirth?talent=…` → `swapProgressResponse(html)` → `closeTalentSelect()`, `openTalentSelect()`/`closeTalentSelect()`(취소는 팝업만 닫음)
    - _Requirements: 5.2, 5.3, 5.5, 5.6, 5.7, 4.2, 10.4_

  - [x] 9.4 myrpg.css 확장
    - `static/css/myrpg.css`: 재능 선택 오버레이·3종 버튼·보유 AP/재능 효과 행 스타일(기존 `:root` 디자인 토큰 재사용)
    - _Requirements: 4.1, 5.3_

  - [x] 9.5 PlayScreenViewHelper 정보 팝업 단위 테스트 확장
    - `PlayScreenViewHelperInfoTest.java` 확장 — `buildInfo` 결과의 `abilityPoints`·`talentEffectSummary` 매핑, 재능 반영 중앙 스탯 본체(예: `ARCHERY` Lv.10 CRIT `"8.6%"`), HP/MP/Stamina 게이지 max가 `vitalMaxFor(level, talent)` 각 필드와 일치
    - _Requirements: 4.1, 4.3, 8.4, 8.5, 10.2_

  - [x] 9.6 컨트롤러 슬라이스 테스트 확장
    - `PlayScreenControllerProgressionTest.java` 확장(`@WebMvcTest` + `@MockitoBean`) — `GET /` info-popup에 보유 AP·재능 효과 요약 노출, `POST /rebirth?talent=ARCHERY` 재능 반영·AP+1·누적+1(가능 시), `POST /rebirth`(talent 누락) `MELEE` 폴백, 쿨다운 시 상태 불변
    - _Requirements: 4.1, 4.2, 5.8, 5.9, 10.2, 10.4, 12.1_

- [x] 10. 통합·스모크·로컬 세이브 초기화
  - [x] 10.1 로컬 H2 세이브 초기화
    - 로컬 세이브 파일(`myrpg/data/myrpg*`)을 삭제하여 다음 기동 시 새 캐릭터(AP 0, 재능 `MELEE`)가 생성되도록 한다(Req 14 마이그레이션 방식). 프로덕션(`ddl-auto: create`)은 기동 시 자동 초기화되므로 별도 조치 없음
    - _Requirements: 14.1, 14.2, 14.3_

  - [x] 10.2 컨텍스트 로드 스모크 테스트
    - `ProgressionContextLoadSmokeTest.java` 확장(`@SpringBootTest`) — 기동 및 확장 빈(`StatProgression`, `ProgressionService`) 로딩, 정보 팝업·재능 선택 렌더링 경로 정상
    - _Requirements: 13.3_

- [x] 11. 최종 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- 각 Task는 이전 Task 위에 점진적으로 쌓이며, 표현 계층 배선(9)에서 모든 컴포넌트가 통합된다(고아 코드 없음).
- 마이그레이션(4)은 단일 `vitalMaxFor(int)` 제거·`fullRecover(VitalMax)`·`rebirth(p, talent)` 시그니처 변경으로 003 산출물/테스트에 영향이 크므로, 호출부·영향 테스트를 함께 갱신하여 **완료 시점에 빌드 그린**을 보장한다.
- 프로퍼티 테스트는 설계의 15개 정확성 속성(jqwik, `@Property(tries=100)`)을 검증하고, 단위/슬라이스/통합 테스트가 구체 초기값·포맷·렌더링·컨텍스트 로딩을 보완한다.
- 각 Correctness Property는 단 하나의 프로퍼티 테스트로 구현하며, 태그 주석 `Feature: 004-talent-and-ability-points, Property {번호}: {프로퍼티 텍스트}`를 부착한다.
- AP 소모의 실제 트리거(스킬 랭크업)와 정식 예외 체계는 3순위 스킬 시스템, 데미지 보너스 실제 적용은 7순위 전투 시스템으로 이연한다(본 스펙은 mutator/접근자 정의와 검증까지). 소모 도입 후 불변식은 `abilityPoints == (accumulatedLevel - 1) - 누적 소모량`으로 확장된다(Req 3.4).
- Task 완료 전 `mvn test -pl myrpg` 통과와 `mvn clean install -pl myrpg -am` `BUILD SUCCESS`를 확인한다.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["1.3", "2.2"] },
    { "id": 3, "tasks": ["2.3", "2.4", "2.5"] },
    { "id": 4, "tasks": ["4.1", "4.2"] },
    { "id": 5, "tasks": ["4.3"] },
    { "id": 6, "tasks": ["4.4"] },
    { "id": 7, "tasks": ["4.5"] },
    { "id": 8, "tasks": ["6.1", "6.2", "6.3", "6.4", "6.5", "7.1", "7.2", "7.3", "7.4", "7.5", "7.6", "7.7", "7.8", "7.9"] },
    { "id": 9, "tasks": ["9.1", "9.2", "9.3", "9.4"] },
    { "id": 10, "tasks": ["9.5", "9.6", "10.1", "10.2"] }
  ]
}
```
