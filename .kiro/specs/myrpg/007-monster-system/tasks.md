# Implementation Plan: 몬스터 시스템

## Overview

스펙 006(`gold-item-inventory`)까지 구축한 `myrpg` Web 모듈(`com.myapps.web.myrpg`) 위에, 설계 문서의 DDD 4계층 구조를 따라 몬스터 시스템을 점진적으로 구현한다. 몬스터는 NPC 파이프라인의 완성 레퍼런스를 복제·확장한다.

구현 순서 원칙:

- **A. 데이터·도메인** → **B. 대사·AI·드랍·선공(순수 정책)** → **C. 뷰·UI 배선** 순으로 조립한다.
- 하위 계층(값 enum/record → 카탈로그 JSON → 로더/교차검증 → 순수 정책 서비스) → 기존 산출물 확장(MapNode/MapService/ActionButton 리네임/PlayScreenView/헬퍼) → 표현 계층(컨트롤러/템플릿/정적 리소스) 순으로 쌓는다.
- 각 순수 로직 구현 직후 설계의 Correctness Property를 jqwik 프로퍼티 테스트로 확인한다.
- **기존 산출물 확장(MapNode 확장, ActionButton 리네임, PlayScreenView/헬퍼 확장, 컨트롤러 주입)은 호출부·영향 테스트를 함께 갱신하여 원자적으로 완료**하고, 완료 시점에 빌드가 그린이어야 한다.
- 마지막 표현 계층 배선에서 모든 컴포넌트가 통합된다(고아 코드 없음).

> **테스트 정책 안내**: 워크스페이스 스티어링(`task-build-validation.md`)에 따라 "optional task"는 없다. 아래 모든 테스트 하위 작업은 **필수**이며 `*`를 사용하지 않는다. 각 Task는 `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인 후에만 완료 처리한다.

> **Spring Boot 4.0 / Java 25 규약**: 생성자 주입만(`@Autowired` 금지), Lombok 금지, `var` 금지, VO/DTO는 `record`, `final` 파라미터/지역변수, 커스텀 예외(`RuntimeException` 직접 금지 — `MonsterDataException`). 테스트는 `@MockitoBean` / `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, Jackson 3(`tools.jackson`). jqwik 프로퍼티는 `Mockito.mock()` 직접 사용(`@Mock` 금지), `@Property(tries = 100)`. 프로퍼티 태그 주석: `Feature: 007-monster-system, Property {번호}: {프로퍼티 텍스트}`.

> **지식 보존·이연 seam(`docs/monster-system.md`)**: 코드에는 이연 seam(선공 신호 `preemptiveMonsterName`·`전투` 버튼·`MonsterRewardService.rollDrop`·`MonsterAiService.nextAction`)에 **담당 순위(6순위 전투 등)·교체 조건을 서술형 JavaDoc**으로 남긴다(나열식 `// TODO` 금지).

## Tasks

### A. 데이터 · 도메인

- [x] 1. 몬스터 분류·데이터 모델 (신규, 기존 빌드 무영향)
  - [x] 1.1 MonsterType enum + Monster/GoldDrop/ItemDrop record 구현
    - `domain/model/MonsterType.java`(신규): `NORMAL`("normal","일반","")·`BOSS`("boss","보스","👑") + `actionLabels`(`["전투"]`)·`badge`·`typeString`·`label`·`fromType`
    - `domain/model/GoldDrop.java`(신규 record): `(int min, int max)`, 컴팩트 생성자 `0 ≤ min ≤ max` 검증
    - `domain/model/ItemDrop.java`(신규 record): `(String itemId, int chancePercent, int minQuantity, int maxQuantity)`
    - `domain/model/Monster.java`(신규 record): 스탯·드랍·`lines` + `buttonLabel()`(배지 비면 이름만, 아니면 `이름 + " " + 배지`)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

  - [x] 1.2 타입·라벨 프로퍼티/단위 테스트
    - `MonsterTypeCompletenessPropertyTest.java` — **Property 1: 몬스터 타입 완전성** — label/actionLabels non-empty, badge(NORMAL=""/BOSS="👑"), fromType 왕복·미지 empty
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4**
    - `MonsterButtonLabelPropertyTest.java` — **Property 2: 몬스터 버튼 라벨 포맷** — 일반=이름, 보스=이름+👑
    - **Validates: Requirements 4.6, 10.4**
    - `MonsterTypeTest.java` / `GoldDropTest.java` — 라벨·상수 예시, `GoldDrop` min>max 거부
    - _Requirements: 4.4_

- [x] 2. 맵 출현 매핑 (MapNode 확장 · map.json)
  - [x] 2.1 MapNode monsters 컴포넌트 + MapService 파싱 + map.json 매핑
    - `domain/model/MapNode.java`[확장]: `monsters`(List<String>) 컴포넌트 + 기존 9인자 보조 생성자(`List.of()`)
    - `application/service/MapService.java`[확장]: `parseLinks`를 `parseStringArray(node, field)`로 일반화하여 `links`/`monsters` 공용, `monsters` optional 파싱(`has(...)`)
    - `resources/data/map.json`[확장]: `dugald-north`에 `"monsters": ["raccoon"]`
    - 기존 `MapNode` 9인자 호출부(테스트 ~20곳) 무변경 확인, 기존 맵 파싱/이동 테스트 무회귀
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 3. 카탈로그 리소스 + 로더 (신규)
  - [x] 3.1 monster.json 카탈로그 리소스
    - `resources/data/monster.json`(신규): 너구리 1종(normal, Lv1, HP25, atk4/def1/crit10, exp15, goldDrop 3~10, itemDrops hp_potion_50 15%×1, lines 3개[소리1+행동2])
    - _Requirements: 8.5_

  - [x] 3.2 MonsterDataException + MonsterService 구현
    - `application/exception/MonsterDataException.java`(신규, `SkillDataException`/`NpcDataException` 선례, 생성자 2개)
    - `application/service/MonsterService.java`(신규): `@PostConstruct init()`(classpath:data/monster.json), `loadFromStream(InputStream):List<Monster>`(파싱·검증 분리), `all()`, `byId(String)`, `byNode(String)`(map.json 순서 보존, 미지/null→[]). 생성자 주입 `ObjectMapper`·`MapService`·`ItemCatalogService`. 검증: 필수필드/type/goldDrop·수량·확률 범위/lines=3, 교차검증(id 중복·map monsters 존재·노드 내 중복·itemDrops.itemId 존재). Jackson 3(`tools.jackson`)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 5.5, 5.6, 5.7, 5.8, 5.9, 14.1, 14.2_

  - [x] 3.3 카탈로그 파싱·검증·조회 프로퍼티/통합 테스트
    - `MonsterServiceParsingPropertyTest.java` — **Property 3: 카탈로그 파싱·필드 보존** — 유효 입력 불변 목록·필드 보존·itemDrops 미기재 빈 목록
    - **Validates: Requirements 1.2, 2.7, 4.1**
    - `MonsterServiceLoadFailurePropertyTest.java` — **Property 4: 카탈로그 검증 실패** — 중복 id·미지 type·필드 누락·범위 위반·미존재 itemId·lines≠3 → `MonsterDataException`
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6**
    - `MonsterServiceByNodePropertyTest.java` — **Property 5: 노드별 조회 순서·관용성** — monsters 순서 보존, 미지/null → []
    - **Validates: Requirements 5.5, 5.6**
    - `MonsterServiceLoadIntegrationTest.java` — 실제 `monster.json`/`map.json` 로드: 너구리 필드 값·`dugald-north` 배치
    - _Requirements: 1.1, 8.5_

- [x] 4. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

### B. 대사 · AI · 드랍 · 선공 (순수 정책)

- [x] 5. 조우 대사 서비스
  - [x] 5.1 MonsterDialogueService 구현
    - `application/service/MonsterDialogueService.java`(신규): `Random` 빈 주입, `selectLine(Monster)`(lines 3개 중 `random.nextInt(3)`, 폴백 없음, 시간대 분기 없음)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [x] 5.2 대사 선택 프로퍼티 테스트
    - `MonsterDialogueServicePropertyTest.java` — **Property 6: 조우 대사 선택** — 반환값 항상 lines 포함, 고정 시드 결정성, 폴백 없음
    - **Validates: Requirements 6.2, 6.3, 6.5**

- [x] 6. 가위바위보 AI 서비스
  - [x] 6.1 MonsterAiService 구현
    - `application/service/MonsterAiService.java`(신규): 상수 34/33/33, `actionFor(int roll)` 순수 함수(0~33 NORMAL/34~66 HEAVY/67~99 DEFENSE), `nextAction()`(`actionFor(random.nextInt(100))`). `nextAction` JavaDoc에 6순위 전투 소비 seam 명시
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

  - [x] 6.2 가위바위보 분포 프로퍼티 테스트
    - `MonsterAiActionDistributionPropertyTest.java` — **Property 7: 가위바위보 분포** — actionFor 0..99 = 34/33/33, 경계값(33/34/66/67)
    - **Validates: Requirements 7.2, 7.3**

- [x] 7. 드랍 계산 서비스
  - [x] 7.1 DropResult/DroppedItem + MonsterRewardService 구현
    - `application/dto/DropResult.java`(신규 record `(long gold, List<DroppedItem>)` + `EMPTY`), `application/dto/DroppedItem.java`(신규 record `(String itemId, int quantity)`)
    - `application/service/MonsterRewardService.java`(신규): `Random` 빈 주입, `goldFor(GoldDrop, int roll)` 순수 함수(`min + roll % (max-min+1)`), `rollDrop(Monster)`(골드 필수 + itemDrops chance 판정·수량 추첨). `rollDrop` JavaDoc에 6순위 지급 seam(골드 가산·인벤토리 획득 API) 명시
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.6_

  - [x] 7.2 드랍 계산 프로퍼티 테스트
    - `MonsterRewardServicePropertyTest.java` — **Property 8: 드랍 골드 범위·아이템 확률** — gold ∈ [min,max], chance 0/100 경계, 수량 범위
    - **Validates: Requirements 8.2, 8.3, 8.4**

- [x] 8. 선공 판정 서비스
  - [x] 8.1 MonsterEncounterService 구현
    - `application/service/MonsterEncounterService.java`(신규): 상수 `PREEMPTIVE_STRIKE_PERCENT=5`, `triggers(int roll)` 순수 함수(`roll<5`), `rollPreemptiveStrike(List<Monster>):Optional<Monster>`(빈/미발동 empty, 발동 시 랜덤 선택). `Random` 빈 주입. 6순위 실전투 진입 seam JavaDoc 명시
    - _Requirements: 9.1, 9.2, 9.3, 9.8_

  - [x] 8.2 선공 판정 프로퍼티 테스트
    - `MonsterEncounterServicePropertyTest.java` — **Property 9: 선공 판정 경계·선택** — triggers 경계(4→발동/5→미발동), 빈 목록 empty, 선택 몬스터 ∈ 입력·고정 시드 결정성
    - **Validates: Requirements 9.1, 9.2, 9.3**

- [x] 9. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

### 기존 산출물 확장 (원자적, 빌드 그린 유지)

- [x] 10. 뷰모델 확장 (ActionButton 리네임 · TalkTarget · PlayScreenView)
  - [x] 10.1 NpcActionButton → ActionButton 리네임
    - `application/dto/ActionButton.java`(`NpcActionButton`에서 리네임): NPC·몬스터 공용
    - 참조 갱신: `PlayScreenViewHelper`, `PlayScreenView`(타입), 기존 테스트 4곳(`NpcActionButtonsPropertyTest` 클래스명 유지·타입만 갱신)
    - _Requirements: 13.1, 13.3, 13.4_

  - [x] 10.2 TalkTarget + PlayScreenView 몬스터 슬롯 확장
    - `application/dto/TalkTarget.java`(신규 record `(Npc, Monster, String dialogue)` + `EMPTY`/`ofNpc`/`ofMonster`)
    - `application/dto/PlayScreenView.java`[확장]: `monsterName`/`monsterDialogue`/`monsterLevel`(Integer)/`monsterMaxHp`(Integer)/`monsterActions` 추가 + 기존 10-인자 보조 생성자(몬스터 슬롯 null)
    - 기존 `PlayScreenView` 직접 생성 테스트(8곳) 무변경 확인
    - _Requirements: 11.6, 11.7, 11.8_

- [x] 11. PlayScreenViewHelper 확장 (상호작용 합류 · 몬스터 대사/행동)
  - [x] 11.1 buildInteractions(npcs, monsters) + TalkTarget 오버로드 + buildMonsterActions
    - `interfaces/api/PlayScreenViewHelper.java`[확장]: `buildInteractions(List<Npc>, List<Monster>)`(NPC 먼저·몬스터 이어서, `toInteractionItem(Monster)` → `npc=false`), 기존 `buildInteractions(List<Npc>)`는 `(npcs, List.of())` 위임, `TalkTarget` 기반 `buildPlayScreen` 오버로드(기존 5/8/9-인자는 `EMPTY`/`ofNpc` 위임), `talkTarget.monster()` 시 `monsterName`/`monsterLevel`/`monsterMaxHp`/`monsterDialogue` 채움, `buildMonsterActions`
    - _Requirements: 10.2, 10.3, 11.6, 11.8, 13.2_

  - [x] 11.2 상호작용 라벨·행동버튼 프로퍼티 테스트
    - `MonsterActionButtonsPropertyTest.java` — **Property 10: 몬스터 행동 버튼 조립** — monsterActions 라벨 == MonsterType.actionLabels() 개수·순서
    - **Validates: Requirements 11.5, 13.2**
    - `PlayScreenViewHelperMonsterTest.java` — `buildInteractions(npcs, monsters)` 합류 순서·`npc=false`, TalkTarget.ofMonster 시 name/level/maxHp/dialogue 채움·ofNpc/EMPTY 시 몬스터 슬롯 null
    - _Requirements: 10.2, 10.3, 11.6, 11.8_

- [x] 12. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

### C. 표현 계층 배선 (조우 · 선공 · 템플릿/정적)

- [x] 13. PlayScreenController 배선 (조우 엔드포인트 · 선공 · 서비스 주입)
  - [x] 13.1 몬스터 서비스 주입 + buildViewFromProgress + /monster/encounter + move() 선공
    - `interfaces/api/PlayScreenController.java`[확장]: 생성자에 `MonsterService`·`MonsterDialogueService`·`MonsterEncounterService` 주입, `COMBAT_TYPE="combat"`, `buildViewFromProgress`의 interactions를 `buildInteractions(npcsOnNode, monsterService.byNode(nodeId))`로 교체, `@PostMapping("/monster/encounter")`(대사·조우 로그·`monster-response`, 미지 id 관용), `move()` `Moved` 분기에 `rollPreemptiveStrike` → `preemptiveMonsterName` 모델 속성·`combat` 로그. `전투`/선공 seam JavaDoc 명시
    - `templates/fragments/monster-response.html`(신규): `center` 교체 프래그먼트
    - _Requirements: 9.4, 9.5, 9.6, 9.7, 9.8, 11.1, 11.2, 11.9, 12.3, 14.4_

  - [x] 13.2 컨트롤러 슬라이스 테스트
    - `PlayScreenControllerMonsterTest.java`(`@WebMvcTest`+`@MockitoBean` 몬스터 3종): 이동 후 몬스터 버튼 노출, `/monster/encounter` → `monster-response` + `monsterName`/`monsterLevel`/`monsterMaxHp`/`monsterDialogue`/`monsterActions`, 미지 id 관용, NPC·몬스터 슬롯 배타
    - `PlayScreenControllerPreemptiveTest.java`(`@WebMvcTest`): `/move` 선공 발동 시 `preemptiveMonsterName`·`combat` 로그, empty 시 신호 없음
    - 기존 `PlayScreenControllerTest`/`PlayScreenControllerNpcTest`/`PlayScreenControllerProgressionTest`에 `@MockitoBean` 몬스터 3종 추가(`rollPreemptiveStrike` 기본 empty 스텁) → 회귀 없음
    - _Requirements: 11.1, 11.9, 14.5_

- [x] 14. 템플릿 · 정적 리소스 (몬스터 버튼 · 대사 · 선공 alert)
  - [x] 14.1 center.html / move-response.html + myrpg.js/css
    - `templates/fragments/center.html`[확장]: 몬스터 대사 블록(`.monster-name` + `.monster-meta` `Lv.{level} · HP {maxHp}` + 대사), `.monster-actions` 행동 버튼(`monsterAction`), 상호작용 버튼 `data-monster-id`·`onInteractionClick`
    - `templates/fragments/move-response.html`[확장]: 선공 발동 시 `#preemptiveSignal`(data-monster) 요소
    - `static/js/myrpg.js`[확장]: `swapCenter(html)` 추출, `onInteractionClick(el)`(npc/monster 분기), `encounterMonster(monsterId)`, `monsterAction(label)`(alert "구현 예정입니다", 6순위 seam 주석), `move()` 스왑 직후 `#preemptiveSignal` 있으면 `alert("몬스터 선공 발동")`
    - `static/css/myrpg.css`[확장]: `.monster-name`(인라인·굵게), `.monster-meta`(이름 옆 작은 글씨), `.monster-actions button`(붉은 계열). `.log-combat` 재사용(신규 불필요)
    - _Requirements: 10.1, 10.5, 10.6, 11.2, 11.3, 11.4, 11.5, 12.1, 12.2, 12.4_

  - [x] 14.2 정적 리소스 보존 테스트 갱신 + 컨텍스트 스모크
    - `VisualJsPreservationAndJsonLoadingIntegrationTest`[갱신]: `myrpg.js`·`center.html` 신규 함수/마크업 기대값 반영
    - `MonsterContextLoadSmokeTest.java`(신규, `@SpringBootTest`): 몬스터 서비스 4종 빈 로딩 + 컨텍스트 기동
    - _Requirements: 1.1, 14.3, 14.4_

- [x] 15. 최종 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- 각 Task는 이전 Task 위에 점진적으로 쌓이며, 표현 계층 배선(13~14)에서 모든 컴포넌트가 통합된다(고아 코드 없음).
- 기존 산출물 확장(10~11·13)은 `MapNode`/`MapService`·`ActionButton` 리네임·`PlayScreenView`/헬퍼·`PlayScreenController` 주입으로 영향이 있으므로, 호출부·영향 테스트를 함께 갱신하여 **완료 시점에 빌드 그린**을 보장한다.
- 프로퍼티 테스트는 설계의 10개 정확성 속성(jqwik, `@Property(tries=100)`)을 검증하고, 단위/슬라이스/통합 테스트가 구체 값·렌더링·컨텍스트 로딩을 보완한다.
- 각 Correctness Property는 단 하나의 프로퍼티 테스트로 구현하며, 태그 주석 `Feature: 007-monster-system, Property {번호}: {프로퍼티 텍스트}`를 부착한다.
- **이연 항목**: (6순위 전투) 전투 턴·데미지·선후공·사망, `nextAction` 소비, `rollDrop` 지급 + 인벤토리 획득 API·HP 감소 메서드, `SkillService.onSkillKill`, 선공 `alert` → `POST /battle/start`, 크리티컬 배율, 내구도 턴당 감소, 임시 골드 버튼 제거. (인챈트 스펙 후) 보스 실데이터·보스 인챈트 드랍. (10순위) 던전 몬스터. (추후) 보스 필드 랜덤 등장(sealed 불필요). 각 seam은 담당 순위·교체 조건을 JavaDoc으로 명시한다.
- Task 완료 전 `mvn test -pl myrpg` 통과와 `mvn clean install -pl myrpg -am` `BUILD SUCCESS`를 확인한다.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "2.1"] },
    { "id": 2, "tasks": ["3.1", "3.2"] },
    { "id": 3, "tasks": ["3.3"] },
    { "id": 4, "tasks": ["5.1", "6.1", "7.1", "8.1"] },
    { "id": 5, "tasks": ["5.2", "6.2", "7.2", "8.2"] },
    { "id": 6, "tasks": ["10.1", "10.2"] },
    { "id": 7, "tasks": ["11.1", "11.2"] },
    { "id": 8, "tasks": ["13.1"] },
    { "id": 9, "tasks": ["13.2", "14.1"] },
    { "id": 10, "tasks": ["14.2"] }
  ]
}
```
