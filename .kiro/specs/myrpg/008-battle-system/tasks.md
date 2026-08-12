# Implementation Plan: 전투 시스템

## Overview

스펙 007(`monster-system`)까지 구축한 `myrpg` Web 모듈(`com.myapps.web.myrpg`) 위에, 설계 문서의 DDD 4계층 구조를 따라 가위바위보 기반 턴제 전투를 점진적으로 구현한다. 007이 열어둔 전투 seam(`nextAction`/`rollDrop`/`rollPreemptiveStrike`)을 실제 전투 루프로 연결한다.

구현 순서 원칙:

- **A. 도메인 순수 로직·값** → **B. 영속·오케스트레이션 서비스** → **C. 표현 계층 배선** → **D. 정리(임시 버튼 제거)** 순으로 조립한다.
- 하위 계층(순수 상성/데미지 계산 → HP/사망 값 → 영속 엔티티 → 드랍/사망 서비스 → 전투 오케스트레이션) → 표현 계층(컨트롤러/프래그먼트/정적 리소스) → 정리 순으로 쌓는다.
- 각 순수 로직 구현 직후 설계의 Correctness Property를 jqwik 프로퍼티 테스트로 확인한다.
- **기존 산출물 확장(`CharacterProgress`·`Monster`·`InventoryService`·`ProgressionService`·`PlayScreenController`·`SkillController`)은 호출부·영향 테스트를 함께 갱신하여 원자적으로 완료**하고, 완료 시점에 빌드가 그린이어야 한다.
- 마지막 표현 계층 배선에서 모든 컴포넌트가 통합된다(고아 코드 없음).

> **테스트 정책 안내**: 워크스페이스 스티어링(`task-build-validation.md`)에 따라 "optional task"는 없다. 아래 모든 테스트 하위 작업은 **필수**이며 `*`를 사용하지 않는다. 각 Task는 `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인 후에만 완료 처리한다.

> **Spring Boot 4.0 / Java 25 규약**: 생성자 주입만(`@Autowired` 금지), Lombok 금지, `var` 금지, VO/DTO는 `record`, `final` 파라미터/지역변수, 커스텀 예외(`RuntimeException` 직접 금지). 테스트는 `@MockitoBean`, `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, `@DataJpaTest`는 `spring-boot-starter-data-jpa-test` + `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`(+`@TestConstructor`), Jackson 3(`tools.jackson`). jqwik 프로퍼티는 `Mockito.mock()` 직접(`@Mock` 금지), `@Property(tries = 100)`. 프로퍼티 태그 주석: `Feature: 008-battle-system, Property {번호}: {프로퍼티 텍스트}`.

> **소스 정리(`code-style`)**: 소스 수정 Task는 완료 전 미사용 import/변수 제거·매직넘버 상수화·메서드 분리(50줄 초과 시)·불필요 주석 제거를 수행한다. 튜닝값(재능계수·몬스터 배율·크리티컬 배율·편차폭)은 `private static final` 상수로 둔다.

> **지식 보존·이연 seam(`docs/battle-system.md`)**: 이연 seam(내구도 수리 7순위·보스 실데이터/인챈트 드랍·던전 전투 10순위)에 **담당 순위·조건을 서술형 JavaDoc**으로 남긴다(나열식 `// TODO` 금지). `SkillDamagePolicy`·`MonsterAiService`의 "7순위" 오기 JavaDoc은 "6순위"로 정정한다.

## Tasks

### A. 도메인 순수 로직 · 값

- [x] 1. 가위바위보 상성 + 감산형 데미지 공식 (순수)
  - [x] 1.1 RockPaperScissors / AffinityResult / BattleResolver 구현
    - `domain/model/AffinityResult.java`(신규 enum): `WIN`/`LOSE`/`DRAW`
    - `domain/service/RockPaperScissors.java`(신규): `judge(SkillType, SkillType):AffinityResult`(일반>강·강>방어·방어>일반, 동일 DRAW)
    - `domain/service/BattleResolver.java`(신규): `Random` 주입, `baseDamage`(감산 최소1)·`affinityCoefficient`(승1.0/무0.5/방어당함(1-blockRate)/관통0.0)·`rollCritical`·`finalDamage`(×1.5 크리티컬 후 ±10% 편차)·`resolve`(9칸 매트릭스 양측 피해). 결정 부분 순수 + 크리티컬/편차만 주입 `Random`. 몬스터 배율(일반100/강150)·크리티컬 배율·편차폭 상수화
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.8, 5.1, 5.2, 5.3, 5.4_

  - [x] 1.2 상성·데미지·매트릭스 프로퍼티/단위 테스트
    - `RockPaperScissorsPropertyTest.java` — **Property 1: 가위바위보 상성** — 승/패/무 및 대칭 역관계
    - **Validates: Requirements 3.1, 3.2, 3.3**
    - `BattleResolverBaseDamagePropertyTest.java` — **Property 2: 감산형 기본피해·최소 1**
    - **Validates: Requirements 4.2, 4.4**
    - `BattleResolverAffinityCoefficientPropertyTest.java` — **Property 3: 상성계수 매핑**
    - **Validates: Requirements 3.4, 3.5, 3.6, 3.7, 3.8, 3.9**
    - `BattleResolverCriticalPropertyTest.java` — **Property 4: 크리티컬 판정·배율**(시드 고정)
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
    - `BattleResolverVariancePropertyTest.java` — **Property 5: 데미지 편차 범위**(시드 고정)
    - **Validates: Requirements 4.6, 4.8**
    - `BattleResolverMatrixPropertyTest.java` — **Property 6: 9칸 매트릭스 피해 산출**
    - **Validates: Requirements 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**
    - `BattleResolverTest.java` — 9칸/감산 경계(방어≥공격→1)/크리티컬 on·off 예시

- [x] 2. HP 감소·사망 값 (CharacterProgress 확장)
  - [x] 2.1 CharacterProgress.damageHp / isDead 구현
    - `domain/model/CharacterProgress.java`[확장]: `damageHp(int)`(`max(0, hpCurrent-amount)`), `isDead()`(`hpCurrent==0`). 기존 필드·생성자 무변경
    - _Requirements: 11.1, 11.2_

  - [x] 2.2 HP 감소 프로퍼티 테스트
    - `CharacterProgressDamageHpPropertyTest.java` — **Property 11: HP 감소·사망 전이** — 0 바닥, isDead 동치
    - **Validates: Requirements 11.1, 11.2**

- [x] 3. 몬스터 방어 상수 (Monster 확장 · monster.json)
  - [x] 3.1 Monster defense 상수 + MonsterService optional 파싱
    - `domain/model/Monster.java`[확장]: `defenseBlockRate`(기본 40)·`defenseCounterRate`(기본 30) + 기존 필드 순서 보존·보조 생성자(미지정)로 하위 호환
    - `application/service/MonsterService.java`[확장]: 두 필드 optional 파싱(`has(...)`), 미지정 시 전역 기본
    - `resources/data/monster.json`[확장]: (옵션) 방어 상수 — 너구리는 미지정 유지(전역 기본 사용) 또는 명시. `SkillDamagePolicy`/`MonsterAiService` JavaDoc "7순위"→"6순위" 정정
    - 기존 몬스터 카탈로그 로드/검증 테스트 무회귀 확인
    - _Requirements: 22.1, 22.2, 22.4, 22.5, 24.4_

  - [x] 3.2 방어 상수 기본값 단위 테스트
    - `MonsterDefenseConstantTest.java` — 미지정 시 40/30, 명시 시 오버라이드 값
    - _Requirements: 22.1_

- [x] 4. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

### B. 영속 · 오케스트레이션 서비스

- [x] 5. 전투 상태 영속 (BattleState · Repository · BattleTurnResult)
  - [x] 5.1 BattleState 엔티티/리포지토리 + BattleTurnResult 구현
    - `domain/model/BattleState.java`(신규 @Entity): `characterId`·`monsterId`·`monsterCurrentHp`·`turnCount`·`ambush`·`active`
    - `domain/repository/BattleStateRepository.java`(신규): `findByCharacterIdAndActiveTrue(long)`
    - `domain/model/BattleTurnResult.java`(신규 record): 플레이어/몬스터 행동·피해, 크리티컬/방어/반격/캐스팅실패/선제/자원부족 플래그, 종료 여부·`Outcome`, 드랍/경험치, 로그 라인
    - _Requirements: 1.1, 1.2, 1.3, 1.8_

  - [x] 5.2 영속 왕복 리포지토리 테스트
    - `BattleStateRepositoryTest.java`(`@DataJpaTest` 신 패키지 + `@TestConstructor`) — **Property 18: 전투 상태 영속 왕복** — 저장→재조회 필드 보존, `findByCharacterIdAndActiveTrue` 활성만
    - **Validates: Requirements 1.1, 1.2, 1.4, 1.6**

- [x] 6. 인벤토리 확장 (드랍 적재 · 전투 스킬 목록 · 내구도 자동 해제)
  - [x] 6.1 InventoryService.acquire / combatSkills / 내구도 0 자동 해제 구현
    - `application/service/InventoryService.java`[확장]: `acquire(progress, DropResult)`(골드 항상 가산, 아이템 적재, 용량 30 초과 시 소실+`"{아이템명} 획득 실패!"` 로그), `combatSkills(progress):List<BattleSkillButton>`(착용 무기 재능 스킬 + 공통 방어), `reduceDurability` 결과 0 도달 시 자동 `unequip` + `"{아이템명} 내구도 0 — 장착 해제됨"` 로그
    - `application/dto/BattleSkillButton.java`(신규 record `(id, label, SkillType, ResourceKind, cost)`)
    - 기존 `equip`/`unequip`/`usePotion` 시그니처 무변경 확인
    - _Requirements: 13.2, 13.3, 15.1, 15.2, 15.3, 16.1, 16.2_

  - [x] 6.2 드랍 적재·내구도·전투 스킬 목록 프로퍼티 테스트
    - `InventoryAcquirePropertyTest.java` — **Property 14: 드랍 적재·용량 초과** — 골드 가산·용량 초과 소실+로그·나머지 계속
    - **Validates: Requirements 13.2, 13.3**
    - `DurabilityAutoUnequipPropertyTest.java` — **Property 15: 내구도 0 자동 해제** — 0 도달 시 unequip·보너스 제외
    - **Validates: Requirements 15.1, 15.2**
    - `CombatSkillListPropertyTest.java` — **Property 16: 전투 스킬 목록 = 무기 재능 + 공통** — 무기 재능+공통만·무기 변경 반영
    - **Validates: Requirements 16.1, 16.2, 20.4**

- [x] 7. 사망 처리 (ProgressionService 확장)
  - [x] 7.1 ProgressionService.die 구현
    - `application/service/ProgressionService.java`[확장]: `die(progress)` — `applyDeathPenalty`(경험치 -10%) + `fullRecover` + `currentNodeId="tir-chonaill"`, 골드/아이템 불변. 상수 `RESPAWN_NODE_ID`
    - _Requirements: 11.3, 11.4, 11.5_

  - [x] 7.2 사망 처리 프로퍼티 테스트
    - `ProgressionDeathPropertyTest.java` — **Property 12: 사망 처리 불변식** — 경험치 -10%·풀 회복·티르코네일·골드/아이템 불변
    - **Validates: Requirements 11.3, 11.4, 11.5**

- [x] 8. 전투 오케스트레이션 (BattleService)
  - [x] 8.1 BattleService.start/takeTurn/flee/resumeIfActive/combatSkills 구현
    - `application/service/BattleService.java`(신규): 협력자 주입(`BattleStateRepository`·`BattleResolver`·`MonsterService`·`MonsterAiService`·`MonsterRewardService`·`SkillService`·`SkillDamagePolicy`·`InventoryService`·`ProgressionService`·`CharacterService`·`StatProgression`·`ActionLog`·`Random`)
    - `start(progress, monsterId, ambush)`: `BattleState`(HP 풀·turnCount=1·active) 저장 + 시작 로그
    - `takeTurn(progress, state, skillId)`: 자원 검사·소모(+마법 10% 실패 판정) → 재능 분기(활 `turnCount==1` 선제 / `resolver.resolve`) → 선후공(동일 타입 50:50·일반↔방어 결정론·선공 처치 시 후공 스킵) → `damageHp`/`monsterCurrentHp` → `onSkillUsed`(+처치 시 `onSkillKill`) → 공격 턴 `reduceDurability(0.2)` → 처치 시 `rollDrop`→`acquire`→`gainExperience` → 사망 시 `die` → `saveTurn` + `BattleState` 저장(turnCount+1) → `BattleTurnResult`
    - `flee(progress, state)`: 50% 성공(종료) / 실패(몬스터 1대·`saveTurn`·전투 유지, HP 0이면 `die`)
    - `resumeIfActive(progress)`: 활성 전투 반환, `monsterId` 소실 시 안전 종료
    - `combatSkills(progress)`: `inventoryService.combatSkills` 위임
    - `attackPower(progress, talent)`: 착용 무기 재능 주스탯(`StatProgression`+`equippedBonus`+스킬 보너스) × 재능계수(근접1.0/활0.85/마법1.2 상수)
    - 이연 seam(내구도 수리 7순위 등) JavaDoc 명시
    - _Requirements: 1.3, 1.4, 1.5, 1.7, 2.2, 2.3, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 9.1, 9.2, 9.4, 9.5, 10.2, 10.3, 10.4, 10.5, 10.7, 11.3, 11.4, 12.3, 12.4, 12.5, 12.6, 13.1, 14.1, 14.2, 15.1, 16.4, 25.4, 25.5_

  - [x] 8.2 전투 오케스트레이션 프로퍼티/통합 테스트
    - `BattleServiceTurnOrderPropertyTest.java` — **Property 7: 선후공 규칙**(시드 고정)
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4**
    - `BattleServiceBowFirstStrikePropertyTest.java` — **Property 8: 활 1턴 선제**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.6**
    - `BattleServiceMagicCastFailurePropertyTest.java` — **Property 9: 마법 캐스팅 실패**(시드 고정)
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 9.5**
    - `BattleServiceResourcePropertyTest.java` — **Property 10: 자원 소모·부족**
    - **Validates: Requirements 9.1, 9.2, 9.4**
    - `BattleServiceFleePropertyTest.java` — **Property 13: 도망 판정**(시드 고정)
    - **Validates: Requirements 12.3, 12.4, 12.5, 12.6**
    - `BattleServiceTurnIntegrationTest.java`(Mockito verify) — `onSkillUsed`/`onSkillKill`, `reduceDurability(0.2)`, `saveTurn`+`BattleState` 저장, 처치 시 `rollDrop`→`acquire`→`gainExperience`
    - `BattleServiceDeathTest.java` — HP 0 → `die`(경험치 -10%·풀 회복·티르코네일)·골드/아이템 불변, `BattleState` active=false
    - _Requirements: 10.2, 10.3, 10.5, 11.3, 13.1, 14.1, 14.2_

- [x] 9. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

### C. 표현 계층 배선 (전투 컨트롤러 · 프래그먼트 · 정적)

- [x] 10. BattleController + 전투 뷰 프래그먼트
  - [x] 10.1 BattleController + BattleView + battle-view.html 구현
    - `interfaces/api/BattleController.java`(신규): `POST /battle/start`(→battle-view)·`/battle/turn`(→top-bar+battle-view+action-log)·`/battle/flee`·`GET /battle/skills`(→`battle-view :: battle-skills`)
    - `application/dto/BattleView.java`(신규 record): 몬스터 name/level/현재·최대 HP, 스킬 버튼, 도망 상태
    - `templates/fragments/battle-view.html`(신규): `th:fragment="battle-view"`(몬스터 이름+레벨, HP 바 `.bar` 재사용, `#battleSkills`, 도망 버튼, 미니맵 포함) + `th:fragment="battle-skills"` 서브프래그먼트
    - _Requirements: 2.1, 2.2, 2.3, 10.1, 10.6, 12.1, 12.2, 16.3, 18.1, 18.2, 18.3, 18.4, 18.5, 18.6_

  - [x] 10.2 전투 컨트롤러 슬라이스 테스트
    - `BattleControllerTest.java`(`@WebMvcTest`+`@MockitoBean`): `/battle/start`→battle-view, `/battle/turn`→top-bar+battle-view+action-log(2줄), `/battle/flee`, `GET /battle/skills`→battle-skills 프래그먼트
    - _Requirements: 2.1, 2.2, 10.6, 16.3_

- [x] 11. PlayScreenController 배선 (기습 자동 전투 · 재개 · 이동 거부)
  - [x] 11.1 기습 자동 start + GET / 재개 + /move 전투 중 거부
    - `interfaces/api/PlayScreenController.java`[확장]: `move()` `Moved` 분기에서 `rollPreemptiveStrike` 발동 시 `battleService.start(..., ambush=true)` + `#ambushSignal`(몬스터명), 활성 전투 있으면 이동 거부, `GET /`에서 `resumeIfActive`로 battle-view 복원. 전투 서비스 주입
    - `interfaces/api/PlayScreenViewHelper.java`[확장]: 재개/조우 시 battle-view 뷰 조립 보조
    - `templates/fragments/monster-response.html`[확장]·`center.html`[확장]: 조우 `전투` 버튼 → `POST /battle/start`
    - _Requirements: 1.6, 1.7, 2.5, 17.1, 17.2, 17.3, 17.4, 17.5, 17.6, 19.4, 24.3_

  - [x] 11.2 기습·재개·이동거부 컨트롤러 테스트
    - `PlayScreenControllerBattleTest.java`(`@WebMvcTest`) — **Property 17: 기습 판정 경계·선택** — `/move` 기습 발동 시 자동 start + `#ambushSignal`, 미발동 시 신호 없음, `GET /` 재개, 활성 전투 중 이동 거부
    - **Validates: Requirements 17.1, 17.3**
    - 기존 `PlayScreenControllerTest`/`...NpcTest`/`...MonsterTest`/`...PreemptiveTest`에 전투 서비스 `@MockitoBean` 추가(활성 전투 없음 기본 스텁) → 회귀 없음
    - _Requirements: 1.6, 1.7, 19.4, 24.3_

- [x] 12. 정적 리소스 (전투 UI · 이동 차단 · 포션/장비 실시간)
  - [x] 12.1 myrpg.js / center / battle-view / css 배선
    - `static/js/myrpg.js`[확장]: `battleActive` 플래그, `startBattle(monsterId)`(→`/battle/start`, alert 제거), `battleTurn(skillId)`(alert "{스킬명} 스킬을 사용하였습니다." → `/battle/turn` → top-bar+battle-view+action-log 교체), `flee()`(→`/battle/flee`), `move(dx,dy)` 진입부 `battleActive` 차단 alert("전투 중에는 이동할 수 없습니다."), 포션 사용 시 top-bar 실시간 갱신(기존 흐름 재사용), 전투 중 `equipItem`/`unequipItem` 성공 후 `GET /battle/skills`로 `#battleSkills` 재렌더, 기습 `#ambushSignal` alert + battleActive=true, 승리/패배/도망 alert·battleActive=false
    - `templates/fragments/center.html`[확장]·`static/css/myrpg.css`[확장]: `.battle-view`·`#battleSkills`·`.flee-btn`, 몬스터 HP 바(기존 `.bar` 재사용)
    - _Requirements: 2.5, 10.1, 11.5, 12.2, 13.4, 13.5, 17.2, 19.1, 19.2, 19.3, 20.1, 20.2, 20.3, 20.4, 20.5, 21.1, 21.2, 21.3, 21.4, 21.5, 21.6, 21.7, 21.8, 21.9_

  - [x] 12.2 정적 리소스 보존 테스트 + 컨텍스트 스모크
    - `VisualJsPreservationAndJsonLoadingIntegrationTest`[갱신]: `myrpg.js`(battleActive/startBattle/battleTurn/flee/move 차단/포션·장비 실시간)·`battle-view.html` 기대값 반영
    - `BattleContextLoadSmokeTest.java`(신규, `@SpringBootTest`): `BattleService`·`BattleResolver`·`BattleStateRepository` 빈 로딩 + 컨텍스트 기동
    - _Requirements: 18.1, 24.5_

- [x] 13. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

### D. 정리 (임시 개발용 버튼·드라이버 제거)

- [x] 14. 임시 골드·경험치·승급 드라이버 제거 및 회귀 정리
  - [x] 14.1 임시 버튼·엔드포인트 제거 + 회귀 테스트 정리
    - `interfaces/api/PlayScreenController.java`[제거]: `POST /gold/gain`·`/gold/spend`·`/exp/up`·`/exp/down`
    - `interfaces/api/SkillController.java`[제거]: `POST /{id}/dev/fill-usage`·`/{id}/dev/fill-kill`
    - `templates/fragments/left-sidebar.html`[제거]: 골드 획득/소모·경험치 업/다운 버튼
    - 승급 모달 프래그먼트/`static/js/myrpg.js`[제거]: `.rankup-temp-btn`(횟수/처치수) 및 관련 함수(`goldGain`/`goldSpend`/`expUp`/`expDown`)
    - 제거된 엔드포인트를 참조하던 기존 컨트롤러/뷰 테스트 삭제·정리
    - _Requirements: 14.3, 23.1, 23.2, 23.3, 23.4_

- [x] 15. 최종 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- 각 Task는 이전 Task 위에 점진적으로 쌓이며, 표현 계층 배선(10~12)에서 모든 컴포넌트가 통합된다(고아 코드 없음).
- 기존 산출물 확장(2·3·6·7·11·14)은 `CharacterProgress`·`Monster`·`InventoryService`·`ProgressionService`·`PlayScreenController`·`SkillController` 영향이 있으므로, 호출부·영향 테스트를 함께 갱신하여 **완료 시점에 빌드 그린**을 보장한다.
- 프로퍼티 테스트는 설계의 18개 정확성 속성(jqwik, `@Property(tries=100)`)을 검증하고, 단위/통합/슬라이스/영속 테스트가 구체 값·verify·렌더링·영속 왕복을 보완한다.
- 각 Correctness Property는 단 하나의 프로퍼티 테스트로 구현하며, 태그 주석 `Feature: 008-battle-system, Property {번호}: {프로퍼티 텍스트}`를 부착한다.
- **이연 항목**: (10순위) 던전 내부 전투. (인챈트 스펙 후) 보스 실데이터·보스 인챈트 드랍. (7순위 대장간) 내구도 수리 — 본 스펙은 파손 시 자동 장착 해제까지만. 각 seam은 담당 순위·조건을 JavaDoc으로 명시한다. 밸런싱 튜닝값은 `data-balance-guide.md` 기준.
- Task 완료 전 `mvn test -pl myrpg` 통과와 `mvn clean install -pl myrpg -am` `BUILD SUCCESS`를 확인하고, 소스 수정 Task는 `code-style` 정리 항목을 완료한다.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "3.1", "5.1"] },
    { "id": 1, "tasks": ["1.2", "2.2", "3.2", "5.2"] },
    { "id": 2, "tasks": ["6.1", "7.1"] },
    { "id": 3, "tasks": ["6.2", "7.2"] },
    { "id": 4, "tasks": ["8.1"] },
    { "id": 5, "tasks": ["8.2"] },
    { "id": 6, "tasks": ["10.1"] },
    { "id": 7, "tasks": ["10.2", "11.1"] },
    { "id": 8, "tasks": ["11.2", "12.1"] },
    { "id": 9, "tasks": ["12.2"] },
    { "id": 10, "tasks": ["14.1"] }
  ]
}
```
