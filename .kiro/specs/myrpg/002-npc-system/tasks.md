# Implementation Plan: NPC 시스템 (맵 배치 · 시간대 기반 멘트 · 타입별 행동 버튼)

## Overview

스펙 001에서 구축한 `myrpg` Web 모듈(`com.myapps.web.myrpg`)의 플레이 화면 SSR · 맵 노드 이동 · 진행상황 영속화 위에, 설계 문서의 DDD 4계층 구조를 따라 NPC 시스템을 점진적으로 구현한다.

구현 순서는 아래 원칙을 따른다.

- 하위 계층(도메인 enum/record → 고정 데이터 리소스 → 애플리케이션 서비스) 먼저 구현하고, 이후 뷰 모델·헬퍼 → 표현 계층(컨트롤러/템플릿/정적 리소스) 순으로 조립한다.
- 각 순수 로직 컴포넌트를 구현한 직후, 설계의 Correctness Property를 jqwik 프로퍼티 테스트로 확인하여 오류를 조기에 잡는다.
- 001의 확장 지점(`InteractionItem`, `PlayScreenView`, `center.html`의 `.npc-talk`/`.interactions`, `Clock`/`Random` 빈)을 재사용·확장하며, 001 레이아웃·이동 동작을 깨지 않는다.
- 마지막에 컨트롤러/템플릿/정적 리소스를 배선하여, 어떤 코드도 통합되지 않은 채 남지 않도록 한다.

> **테스트 정책 안내**: 워크스페이스 스티어링(`task-build-validation.md`)에 따라 이 프로젝트에는 "optional task" 개념이 없다. 아래 모든 테스트 하위 작업은 **필수**이며 `*` 표시를 사용하지 않는다. 각 Task는 `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인 후에만 완료 처리한다.

> **Spring Boot 4.0 / Java 25 규약**: 생성자 주입만 사용(`@Autowired` 금지), Lombok 금지, `var` 금지, VO/DTO는 `record`, Jackson 3(`tools.jackson.databind.ObjectMapper`), 커스텀 예외 사용(`RuntimeException` 직접 금지). 테스트는 `@MockitoBean` / `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` 사용. jqwik 프로퍼티는 `Mockito.mock()` 직접 사용(`@Mock` 금지), `@Property(tries = 100)`. 프로퍼티 테스트 태그 주석 형식: `Feature: 002-npc-system, Property {번호}: {프로퍼티 텍스트}`.

## Tasks

- [x] 1. NPC 도메인 모델 및 예외
  - [x] 1.1 NpcType enum 구현 (라벨·행동 정의 단일 소스)
    - `domain/model/NpcType.java`: 상수 `CHIEF`/`BLACKSMITH`/`MAGIC_SCHOOL`/`SCHOOL`/`HEALER`/`BANK`에 `typeString`, `label`, `actionLabels`(불변 `List.of(...)`) 내장
    - `label()`, `actionLabels()` 접근자와 `static Optional<NpcType> fromType(String)`(미지 타입 → 빈 `Optional`, 001 `NodeType.fromType` 패턴) 제공
    - _Requirements: 1.4, 5.2, 5.3, 5.4, 5.5_

  - [x] 1.2 TimeOfDay enum 구현 (반열린 구간 경계 단일 소스)
    - `domain/model/TimeOfDay.java`: `LATE_NIGHT`[0,5)/`DAWN`[5,8)/`MORNING`[8,12)/`AFTERNOON`[12,16)/`LATE_AFTERNOON`[16,19)/`NIGHT`[19,24)에 `key`, `from`, `to` 내장
    - `key()`와 `static TimeOfDay fromHour(int hour)`(`from <= hour < to`인 상수 정확히 하나 반환) 제공
    - _Requirements: 3.1_

  - [x] 1.3 Npc / NpcLines record 구현
    - `domain/model/NpcLines.java` record: `List<String> defaultLines`(JSON `lines.default` 매핑, `@JsonProperty("default")`), `Map<String, List<String>> byTime`
    - `domain/model/Npc.java` record: `id`, `name`, `type`(`NpcType`), `nodeId`, `personality`, `lines`(`NpcLines`)
    - _Requirements: 1.2_

  - [x] 1.4 NpcDataException 구현
    - `application/exception/NpcDataException.java`: 고정 데이터 로드/검증 실패용 커스텀 예외(001 `MapDataException`과 동형, `RuntimeException` 직접 사용 금지)
    - _Requirements: 1.5, 1.7_

  - [x] 1.5 NpcType 매핑 완전성 프로퍼티 테스트
    - `NpcTypeCompletenessPropertyTest.java` — **Property 9: Npc_Type 매핑 완전성(단일 소스)** — `Arbitraries.of(NpcType.values())`로 전 상수 커버, `label()`은 비어있지 않은 문자열, `actionLabels()`는 비어있지 않은 목록 검증
    - **Validates: Requirements 5.2, 5.3, 5.5**

  - [x] 1.6 시각→TimeOfDay 매핑 프로퍼티 테스트
    - `TimeOfDayPropertyTest.java` — **Property 5: 시각→Time_Of_Day 매핑** — `hour ∈ [0,23]` 전 구간(경계 0/4/5/7/8/11/12/15/16/18/19/23 포함)에서 `fromHour(hour)`가 `from <= hour < to`를 만족하는 상수를 정확히 하나 반환, 6개 구간이 `[0,24)`를 빈틈·중복 없이 분할함을 검증
    - **Validates: Requirements 3.1**

  - [x] 1.7 NpcType 실제 매핑값 단위 테스트
    - `NpcTypeTest.java` — 6개 타입의 `typeString`→`label`·`actionLabels`가 요구사항 표와 정확히 일치(`chief`→`촌장`/[`퀘스트`], `blacksmith`→`대장간`/[`상점`,`수리`], `magic-school`→`마법학교`/[`상점`], `school`→`학교`/[`상점`], `healer`→`힐러집`/[`상점`,`치료`], `bank`→`은행`/[`아이템 보관`,`골드 입/출금`]), `fromType` 미지 타입 시 빈 `Optional` 검증
    - _Requirements: 4.3, 5.4_

- [x] 2. NPC 고정 데이터 리소스 이관
  - [x] 2.1 npc.json 리소스 이관
    - `docs/npc-dialogue.json`의 `npcs` 배열만 `myrpg/src/main/resources/data/npc.json`으로 이관(권위 데이터). 원본의 `typeLabels`·`timeOfDay` 메타데이터는 코드(enum)로 단일화되므로 리소스에서 제거
    - 티르코네일(`tir-chonaill`) 5명 + 던바튼(`dunbarton`) 5명, 총 10명 유지
    - _Requirements: 1.1, 1.3, 5.1_

- [x] 3. NpcService (JSON 로드·검증·노드 조회)
  - [x] 3.1 NpcService 구현
    - `application/service/NpcService.java`: `@Service`, 생성자 주입(`ObjectMapper`), `@PostConstruct`에서 `classpath:data/npc.json`을 Jackson 3로 1회 파싱하여 불변 `List<Npc>` 구성
    - 검증: 각 항목의 `id`/`name`/`type`/`nodeId` 존재·비어있지 않음, `id` 전역 유일, `type`이 `NpcType.fromType(...)`으로 분류 가능 — 위반 시 `NpcDataException`으로 기동 실패(부분 목록 미제공)
    - 조회 메서드: `all()`(정의 순서 불변), `byNode(String nodeId)`(일치 항목 정의 순서, 미일치/미지 노드 시 빈 목록), `byId(String npcId)`
    - 파싱 로직을 리소스 로딩과 분리(인메모리 문자열/스트림 로드 가능하도록)하여 프로퍼티 테스트가 로드 경로를 투입할 수 있게 설계
    - _Requirements: 1.1, 1.2, 1.4, 1.5, 1.6, 1.7, 2.1, 2.2, 5.1_

  - [x] 3.2 NPC 데이터 파싱 라운드트립 프로퍼티 테스트
    - `NpcServiceParsingPropertyTest.java` — **Property 1: NPC 데이터 파싱 라운드트립** — 유효 데이터셋(유효 `type` 6개, 유일 `id`, `lines.default`/`lines.byTime` 임의)을 Jackson 3로 직렬화 후 로드하면 `id`/`name`/`nodeId`/`personality`·`lines` 원소가 순서까지 보존되고 `type`이 원본 문자열에 대응하는 `NpcType`으로 분류됨을 검증
    - **Validates: Requirements 1.1, 1.2, 1.4**

  - [x] 3.3 NPC 데이터 로드 실패 및 무생성 프로퍼티 테스트
    - `NpcServiceLoadFailurePropertyTest.java` — **Property 2: NPC 데이터 로드 실패 및 무생성(all-or-nothing)** — 유효 데이터셋에 (a) 필수 필드 누락, (b) `id` 중복, (c) 미지 `type` 중 하나 이상 주입 시 로드가 `NpcDataException`을 던지고 어떤 목록(부분 목록 포함)도 제공하지 않음을 검증
    - **Validates: Requirements 1.5, 1.7**

  - [x] 3.4 노드별 NPC 조회 필터·순서 프로퍼티 테스트
    - `NpcServiceByNodePropertyTest.java` — **Property 3: 노드별 NPC 조회 필터 및 순서** — 임의 노드 id(그래프에 없는 임의 문자열 포함)에 대해 `byNode`가 일치 Npc만 정의 순서대로 반환, 미일치 시 오류 없이 빈 목록 반환을 검증
    - **Validates: Requirements 2.1, 2.2**

  - [x] 3.5 실제 npc.json 로딩 통합 테스트
    - `NpcServiceLoadIntegrationTest.java` — 실제 `data/npc.json` 로드 후 총 10명, `tir-chonaill` 5명, `dunbarton` 5명, 각 `type`이 유효 `NpcType`으로 분류됨을 검증
    - _Requirements: 1.3_

- [x] 4. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. NpcDialogueService (시간대 매핑 · 후보 병합 · 무작위 선택)
  - [x] 5.1 NpcDialogueService 구현
    - `application/service/NpcDialogueService.java`: `@Service`, `Clock`/`Random` 주입(001 `AmbienceService`와 동일 빈 재사용)
    - `selectLine(Npc npc)` 및 테스트용 오버로드 `selectLine(Npc npc, int hour)`: `TimeOfDay.fromHour(...)` → `lines.default` 뒤에 `lines.byTime[tod]`를 순서 보존 병합(누락·중복 제거 없음, 키 부재/빈 목록 시 default만) → 비어있지 않으면 `random.nextInt(size)` 균등 무작위 선택, 비어있으면 `personalityFallback(npc)`(npc `name` 기반 비어있지 않은 결정적 문구) 반환
    - **계절 정보를 입력·사용하지 않음**(시그니처·내부 로직에 season 개념 없음)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.8_

  - [x] 5.2 후보 풀 구성 프로퍼티 테스트
    - `NpcDialogueCandidatePoolPropertyTest.java` — **Property 6: 후보 풀 구성** — 임의 Npc·`TimeOfDay`에 대해 후보 풀이 `lines.default` 전체 뒤 `lines.byTime[tod]` 전체를 이어붙인 목록과 정확히 일치(순서·개수 보존), 키 부재/빈 목록 시 `default`만으로 구성됨을 검증(`default`·`byTime[tod]` 채움/한쪽/양쪽 빔 케이스 커버)
    - **Validates: Requirements 3.2, 3.3**

  - [x] 5.3 대사 선택 유효성 프로퍼티 테스트
    - `NpcDialogueSelectionPropertyTest.java` — **Property 7: 대사 선택은 항상 유효 결과** — 시드 고정 `Random` 주입으로 (a) 풀이 비어있지 않으면 결과가 풀의 원소, (b) 풀이 비어있으면 비어있지 않은 단일 `Personality_Fallback_Line` 반환을 검증
    - **Validates: Requirements 3.4, 3.5**

- [x] 6. 뷰 모델 확장 및 PlayScreenViewHelper
  - [x] 6.1 뷰 모델 확장·신규 구현
    - `application/dto/InteractionItem.java` [확장]: `id` 필드 추가 → `record InteractionItem(String id, String name, boolean npc)`
    - `application/dto/NpcActionButton.java` [신규] record: `label`
    - `application/dto/PlayScreenView.java` [확장]: `npcActions`(`List<NpcActionButton>`) 필드 추가
    - _Requirements: 2.4, 4.1_

  - [x] 6.2 PlayScreenViewHelper 확장
    - `interfaces/api/PlayScreenViewHelper.java` [확장]: `buildPlayScreen(...)` 시그니처에 `interactions`·`talkingNpc`·`dialogue` 인자 추가
    - `interactions` 라벨을 `name + " (" + type.label() + ")"` 형식으로 조립, NPC 항목은 `npc=true`
    - `talkingNpc == null`이면 `npcName`/`npcDialogue`/`npcActions` 모두 비움; `talkingNpc != null`이면 `npcName`/`npcDialogue` 채우고 `talkingNpc.type().actionLabels()`를 정의 순서대로 `NpcActionButton`으로 변환
    - _Requirements: 2.4, 4.1, 4.2, 4.3, 4.7_

  - [x] 6.3 상호작용 버튼 라벨 형식 프로퍼티 테스트
    - `InteractionLabelPropertyTest.java` — **Property 4: 상호작용 버튼 라벨 형식** — 임의 Npc에 대해 생성된 라벨이 정확히 `name + " (" + type.label() + ")"` 형식임을 검증(예: `네리스 (대장간)`)
    - **Validates: Requirements 2.4**

  - [x] 6.4 행동 버튼 목록 일치 프로퍼티 테스트
    - `NpcActionButtonsPropertyTest.java` — **Property 8: 행동 버튼 목록은 타입 정의와 일치** — 임의 Npc에 대해 생성되는 `NpcActionButton` 라벨 목록이 `NpcType.actionLabels()`와 개수·순서·라벨이 정확히 동일함을 검증
    - **Validates: Requirements 4.1, 4.2, 4.3**

- [x] 7. 컨트롤러 · 템플릿 · 정적 리소스 배선
  - [x] 7.1 PlayScreenController 확장 (interactions 노출 + POST /npc/talk)
    - `interfaces/api/PlayScreenController.java` [확장]: `GET /`·`POST /move` 뷰 조립 시 `npcService.byNode(currentNodeId)` 결과를 `InteractionItem` 목록으로 변환하여 `.interactions`에 노출(대사·행동 버튼은 비움)
    - `POST /npc/talk`(param `npcId`) [신규]: `loadOrCreateDefault()`로 현재 노드 확인 → `byNode`로 상호작용 버튼 재구성 + `byId(npcId)`로 대상 조회 → `npcDialogueService.selectLine(npc)` → 헬퍼로 `npcName`/`npcDialogue`/`npcActions` 채운 뷰 조립 → `.center`만 교체하는 `fragments/npc-response` 반환(이전 이름·대사·행동 버튼 완전 교체)
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 2.7, 3.6, 3.7, 4.6, 4.7_

  - [x] 7.2 템플릿 확장·신규
    - `templates/fragments/center.html` [확장]: `.npc-talk` 하단에 `.npc-actions` 영역 추가, `view.npcActions`를 `th:each`로 렌더(각 버튼 `onclick="npcAction()"`), `.interactions`의 NPC 버튼을 `onclick="talkToNpc(item.id)"`로 연결
    - `templates/fragments/npc-response.html` [신규]: `.center`만 교체하는 래퍼(`th:replace="~{fragments/center :: center}"`)
    - _Requirements: 2.3, 2.5, 3.6, 4.1, 4.2_

  - [x] 7.3 정적 리소스 확장
    - `static/js/myrpg.js` [확장]: 001 `move()` 패턴의 `talkToNpc(npcId)`(POST `/npc/talk` → `.center` swap)와 `npcAction()`(단순 `alert("구현 예정입니다")`, 서버 요청·DOM 변경 없음) 추가
    - `static/css/myrpg.css` [확장]: 행동 버튼용 `.npc-actions`(가로 flex, 작은 버튼) 규칙을 목업 디자인 토큰에 맞춰 추가
    - _Requirements: 4.4, 4.5_

  - [x] 7.4 컨트롤러 슬라이스 테스트
    - `PlayScreenControllerNpcTest.java` — `@WebMvcTest(PlayScreenController.class)` + `@MockitoBean`(서비스): NPC 있는 노드 GET → `.interactions`에 `{name} ({label})` 버튼과 `npc` 클래스 노출·`.npc-talk` 비움; `POST /npc/talk?npcId=` → `.npc-name`·대사·`.npc-actions`(타입 정의) 노출·이전 내용 미포함(교체); `POST /move`로 노드 변경 시 `.interactions` 재구성 확인
    - _Requirements: 2.3, 2.5, 2.6, 2.7, 3.6, 3.7, 4.4, 4.6, 4.7_

- [x] 8. 통합 및 컨텍스트 로드 스모크
  - [x] 8.1 컨텍스트 로드 스모크 테스트
    - `NpcContextLoadSmokeTest.java` — `@SpringBootTest`로 기동 및 NPC 리소스 로딩 성공, `NpcService`가 DB(Repository) 의존 없이 동작함을 확인
    - _Requirements: 1.6, 5.1_

- [x] 9. 최종 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- 각 Task는 이전 Task 위에 점진적으로 쌓이며, 마지막 컨트롤러/템플릿/정적 리소스 배선(7)에서 모든 컴포넌트가 통합된다(고아 코드 없음).
- 각 Task는 특정 요구사항(세부 조항)을 참조하여 추적성을 확보한다.
- 프로퍼티 테스트는 설계의 9개 보편 정확성 속성(jqwik, `@Property(tries = 100)`)을 검증하고, 단위/통합/슬라이스 테스트는 구체 매핑값·수치·렌더링·컨텍스트 로딩을 보완한다.
- 각 Correctness Property는 단 하나의 프로퍼티 테스트로 구현하며, 태그 주석 `Feature: 002-npc-system, Property {번호}: {프로퍼티 텍스트}`를 부착한다.
- 워크스페이스 스티어링에 따라 모든 테스트 하위 작업은 필수(`*` 미사용)이며, Task 완료 전 `mvn test -pl myrpg` 통과와 `mvn clean install -pl myrpg -am` `BUILD SUCCESS`를 확인한다.
- 계절 미사용(Req 3.8)은 `NpcDialogueService` 시그니처·로직에 season 입력이 없음을 5.1 구현으로 구조적으로 보장한다(별도 테스트 대상 아님).

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "1.4", "2.1"] },
    { "id": 1, "tasks": ["1.5", "1.6", "1.7", "3.1", "5.1", "6.1"] },
    { "id": 2, "tasks": ["3.2", "3.3", "3.4", "3.5", "5.2", "5.3", "6.2"] },
    { "id": 3, "tasks": ["6.3", "6.4", "7.1", "7.2", "7.3"] },
    { "id": 4, "tasks": ["7.4", "8.1"] }
  ]
}
```
