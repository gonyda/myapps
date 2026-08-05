# Implementation Plan: 캐릭터 진행상황 영속화 + 맵 노드 이동 + 플레이 화면 SSR

## Overview

`myrpg` Web 모듈(`com.myapps.web.myrpg`)을 신규 생성하고, 설계 문서의 DDD 4계층 구조(interfaces / application / domain + resources)를 따라 세 축(플레이 화면 SSR, 턴제 맵 이동, 캐릭터 진행상황 영속화)을 점진적으로 구현한다.

구현 순서는 아래 원칙을 따른다.

- 하위 계층(고정 데이터 도메인 모델 → 영속 모델 → 순수 로직) 먼저 구현하고, 이후 애플리케이션 서비스 → 표현 계층(컨트롤러/템플릿) 순으로 조립한다.
- 각 순수 로직 컴포넌트를 구현한 직후, 설계의 Correctness Property를 jqwik 프로퍼티 테스트로 확인하여 오류를 조기에 잡는다.
- 마지막에 컨트롤러/템플릿/정적 리소스를 목업과 1:1로 배선하여, 어떤 코드도 통합되지 않은 채 남지 않도록 한다.

> **테스트 정책 안내**: 워크스페이스 스티어링(`task-build-validation.md`)에 따라 이 프로젝트에는 "optional task" 개념이 없다. 아래 모든 테스트 하위 작업은 **필수**이며 `*` 표시를 사용하지 않는다. 각 Task는 `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인 후에만 완료 처리한다.

> **Spring Boot 4.0 / Java 25 규약**: 생성자 주입만 사용(`@Autowired` 금지), Lombok 금지, `var` 금지, VO/DTO는 `record`, Jackson 3(`tools.jackson.databind.ObjectMapper`), 테스트는 `@MockitoBean` / `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` / `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` 사용. jqwik 프로퍼티는 `Mockito.mock()` 직접 사용(`@Mock` 금지), `@Property(tries = 100)`.

## Tasks

- [x] 1. 모듈 스캐폴딩 및 설정
  - [x] 1.1 `myrpg` 모듈 pom.xml 생성 및 Parent POM 등록
    - Parent `pom.xml`의 `<modules>`에 `myrpg` 추가 (기존 Child pom은 수정 금지)
    - `myrpg/pom.xml` 생성: parent 상속, `<artifactId>myrpg</artifactId>`, `<packaging>jar</packaging>`
    - 모듈 고유 의존성(버전 없이): `spring-boot-starter-web`, `spring-boot-starter-thymeleaf`, `h2`(runtime), test scope `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `net.jqwik:jqwik`
    - _Requirements: 3.4_

  - [x] 1.2 애플리케이션 진입점 및 설정 파일 생성
    - `com.myapps.web.myrpg.MyrpgApplication` 메인 클래스 작성
    - `application.yml`(+ `application-local.yml`, `application-prod.yml`) 작성: H2 datasource, JPA `ddl-auto`, Thymeleaf 설정
    - DDD 계층 패키지 디렉터리 구조 생성(interfaces/api, application/service, application/dto, application/exception, domain/model, domain/repository, domain/service)
    - _Requirements: 3.4_

  - [x] 1.3 고정 데이터 및 정적 리소스 이관
    - `docs/map.json` → `myrpg/src/main/resources/data/map.json`, `docs/ambience.json` → `data/ambience.json` 이관
    - 목업 `docs/myrpg-mockup.html`의 `<style>`을 `static/css/myrpg.css`로 무수정 이관(`:root` 디자인 토큰 포함)
    - 목업의 줌/팬/팝업 JS를 `static/js/myrpg.js`로 1:1 이관(이동 함수의 htmx 배선은 12.3에서 대체)
    - _Requirements: 1.2, 1.3, 4.1_

- [x] 2. 고정 데이터 도메인 모델
  - [x] 2.1 노드 기본 모델 정의
    - `NodeType` enum(`TOWN`/`FIELD`/`DUNGEON`, 확장 가능 — 미지 타입은 일반 통행 취급)
    - `MapNode` record(`id`,`name`,`type`,`nodeType`,`x`,`y`,`dungeonId`,`theme`,`links`) — 원본 `type` 문자열 보존
    - `Dungeon` record(`id`,`name`,`entranceNodeId`,`implemented`,`map`)
    - _Requirements: 4.2, 6.2, 10.2, 10.4_

  - [x] 2.2 집계·상황·정책 모델 정의
    - `MapGraph` 집계(`nodes`, `byId`, `byCoord("x,y")`, `dungeons`, `startNodeId`, 좌표 이웃 탐색 헬퍼)
    - `AmbienceData` record 계층(`season`, `timeOfDay`(from/to), `themes[theme][season][tod]`)
    - `ExperiencePolicy`(다음 레벨 필요 경험치 산출, 기본 정책 문서화된 확장 지점)
    - _Requirements: 4.1, 7.1, 1.6_

  - [x] 2.3 커스텀 예외 정의
    - `MapDataException`, `NodeNotFoundException`, `MapViewGenerationException`, `CharacterCreationException` 작성(`RuntimeException` 직접 사용 금지, 각 커스텀 예외)
    - _Requirements: 4.4, 8.6, 2.7_

- [x] 3. 영속 모델 및 리포지토리
  - [x] 3.1 캐릭터 진행상황 엔티티 및 임베더블 정의
    - `Stats` `@Embeddable record`(str/dex/intelligence/critical/defense)
    - `Vital` `@Embeddable record`(current/max, `@AttributeOverrides`로 hp/mp/stamina 매핑)
    - `CharacterProgress` `@Entity`(id, nickname, currentLevel, accumulatedLevel, experience, stats, hp/mp/stamina, currentNodeId)
    - _Requirements: 3.1, 10.1_

  - [x] 3.2 캐릭터 진행상황 리포지토리 구현
    - `CharacterProgressRepository extends JpaRepository<CharacterProgress, Long>` + `findFirstByOrderByIdAsc()`
    - _Requirements: 3.1, 3.4, 2.6_

  - [x] 3.3 진행상황 영속 라운드트립 프로퍼티 테스트
    - **Property 12: 진행상황 영속 라운드트립** — `@DataJpaTest` + `@TestConstructor(autowireMode = ALL)`, `TestEntityManager`로 저장→조회 전 필드 보존 검증(embeddable `Stats`/`Vital` 포함)
    - **Validates: Requirements 3.1**

- [x] 4. Map_Service (JSON 로딩 및 노드 조회)
  - [x] 4.1 MapService 구현(로딩·검증·조회)
    - 기동 시 Jackson 3로 `classpath:data/map.json` 1회 파싱하여 불변 `MapGraph` 구성
    - 로딩 시 `links` 양방향성 검증, 위반 시 `MapDataException`으로 기동 실패
    - `node(id)`(부재 시 `NodeNotFoundException`), `graph()`, `dungeons()` 제공
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 6.2, 10.2, 10.3_

  - [x] 4.2 맵 파싱 라운드트립 프로퍼티 테스트
    - **Property 1: 맵 파싱 라운드트립** — 유효 그래프를 JSON 직렬화 후 파싱하면 모든 노드의 id/name/type/좌표/links 보존 및 `NodeType` 대응
    - **Validates: Requirements 4.1, 4.2**

  - [x] 4.3 노드 조회와 부재 오류 프로퍼티 테스트
    - **Property 2: 노드 조회와 부재 오류** — 존재하는 id는 name/type/좌표/links 반환, 미존재 id는 `NodeNotFoundException`
    - **Validates: Requirements 4.3, 4.4**

  - [x] 4.4 링크 양방향 불변식 프로퍼티 테스트
    - **Property 3: 링크 양방향 불변식** — 로드 성공한 그래프에서 A.links⊇{B} ⇒ B.links⊇{A}
    - **Validates: Requirements 4.5**

  - [x] 4.5 던전 노출 단위 테스트
    - `dungeons()`가 `implemented:false`, `map:null`을 그대로 노출하고 `entranceNodeId`/`dungeonId` 참조 유지하는지 검증
    - _Requirements: 6.2, 10.2_

- [x] 5. MapViewFactory 및 맵 뷰 모델(미니맵/전체지도)
  - [x] 5.1 MapViewFactory 및 뷰 모델 구현
    - 뷰 모델 record: `MinimapCell`, `MapEdge`, `MinimapView`, `FullMapCell`, `FullMapView`
    - `MapViewFactory`(순수 로직): 미니맵(중심 grid 5/3, dx∈[-4,4]·dy∈[-2,2], 최대 45칸), 전체지도(바운딩박스 배치), 범위·연결 동시 만족 간선만 포함, 좌표 부재/미확인 시 `MapViewGenerationException`(무생성)
    - `MapService.minimap(currentNodeId)`, `fullMap(currentNodeId)`를 MapViewFactory에 위임
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_

  - [x] 5.2 미니맵 셀 구성 프로퍼티 테스트
    - **Property 4: 미니맵 셀 구성** — 창 범위 내 노드와 정확히 일치(최대 45), 현재 노드 항상 포함, `gridColumn=5+dx`/`gridRow=3+dy`, 타입 문자열 보존, `current=true` 셀 유일
    - **Validates: Requirements 8.1, 8.2, 8.4, 6.4, 1.7**

  - [x] 5.3 뷰 간선 정합성 프로퍼티 테스트
    - **Property 5: 뷰 간선 정합성** — 간선 포함 ⇔ (두 노드 모두 표시 범위 내 ∧ `links` 실제 연결)
    - **Validates: Requirements 8.3, 8.7**

  - [x] 5.4 전체지도 완전성 프로퍼티 테스트
    - **Property 6: 전체지도 완전성** — 셀 nodeId 집합 = 전체 노드 집합, 이름/타입/links 보존, `gridColumn=x-minX+1`/`gridRow=y-minY+1`
    - **Validates: Requirements 8.5**

  - [x] 5.5 뷰 생성 실패 시 무생성 프로퍼티 테스트
    - **Property 7: 뷰 생성 실패 시 무생성(all-or-nothing)** — 좌표 부재/미확인 현재 노드 시 `MapViewGenerationException`, 셀·간선 미생성
    - **Validates: Requirements 8.6, 8.7**

  - [x] 5.6 NodeType 확장성 프로퍼티 테스트
    - **Property 22: NodeType 확장성** — 미지 type 노드 포함 그래프에서 조회·미니맵/전체지도 생성이 예외 없이 동작하고 `type-{type}` 정보 보존
    - **Validates: Requirements 10.4**

- [x] 6. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Character_Service (기본 캐릭터 생성/로드/턴 저장)
  - [x] 7.1 CharacterService 구현
    - `loadOrCreateDefault()`: 저장소가 비면 Default_Character(닉네임 `고니`, Base_Stats, Lv1/누적1/EXP0, 시작 노드 `tir-chonaill`)를 정확히 1개 생성·저장; 아니면 기존 로드
    - `saveTurn(progress)`: 턴 종료 저장. `@Transactional` 경계에서 save 실패 시 롤백 후 `CharacterCreationException`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 3.2, 3.3_

  - [x] 7.2 빈 저장소 기본 캐릭터 단일 생성 프로퍼티 테스
    - **Property 13: 빈 저장소 시 기본 캐릭터 단일 생성** — 빈 `Character_Store`에서 `loadOrCreateDefault()` 호출 시 닉네임 `고니` 캐릭터가 정확히 한 번 저장(`Mockito.mock()`로 리포지토리 mock)
    - **Validates: Requirements 2.1, 2.5**

  - [x] 7.3 기존 진행상황 로드 프로퍼티 테스트
    - **Property 14: 기존 진행상황 로드(재생성 없음)** — 1개 이상 존재 시 신규 생성/저장 없이 기존 반환
    - **Validates: Requirements 2.6**

  - [x] 7.4 기본값 및 생성 실패 롤백 단위 테스트
    - Base_Stats/Lv1/누적1/EXP0/시작 노드 초기값 검증, save 실패 시 롤백 + `CharacterCreationException`, 저장 실패에 한해 오류 반환
    - _Requirements: 2.2, 2.3, 2.4, 2.7, 2.8, 2.9_

- [x] 8. Ambience_Service (상황 멘트 선택)
  - [x] 8.1 AmbienceService 구현
    - `Clock`/`Random` 주입. 현재 시각 → Season(월 매핑)·Time_Of_Day(시 매핑, 자정 넘김 `late-night` 포함)
    - Theme = `node.theme` 우선, 없으면 `node.type`
    - 후보: `themes[theme][season][tod]` → 동일 theme·season 다른 tod → theme 전체 → `"{맵이름} 주변을 둘러봅니다."` 폴백, 후보 있으면 균등 무작위 선택
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

  - [x] 8.2 계절/시간대 매핑 프로퍼티 테스트
    - **Property 16: 계절/시간대 매핑** — 월 1~12 → season 키, 시 0~23(late-night 포함) → timeOfDay 키가 `ambience.json` 정의에 부합
    - **Validates: Requirements 7.1**

  - [x] 8.3 상황 멘트 유효 후보 선택 프로퍼티 테스트
    - **Property 17: 상황 멘트 선택은 항상 유효 후보** — (a) 후보 존재 시 그 목록의 원소, (b) 비면 동일 theme 폴백 목록의 원소, (c) theme 후보 전무 시 정확히 기본 문구(시드 고정 `Random`)
    - **Validates: Requirements 7.2, 7.3, 7.4**

  - [x] 8.4 Theme 결정 규칙 프로퍼티 테스트
    - **Property 18: Theme 결정 규칙** — `theme` 있으면 그 값, 없으면 `type`
    - **Validates: Requirements 7.5**

- [x] 9. Action_Log 및 Movement_Service (턴제 이동)
  - [x] 9.1 ActionLog / ActionLogEntry 구현
    - `ActionLogEntry` record(`timestamp("yyyy-MM-dd HH:mm:ss")`, `message`, `type`), `Clock` 주입
    - `ActionLog`(세션 보관): 추가 시 타입 미지정이면 `move`, 최대 10개 유지(초과 시 오래된 것부터 제거), 표시 시 오름차순
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [x] 9.2 행동 로그 항목 구성 프로퍼티 테스트
    - **Property 19: 행동 로그 항목 구성과 기본 타입** — 항목은 `yyyy-MM-dd HH:mm:ss` 타임스탬프·메시지·타입(널이면 `move`)을 가짐
    - **Validates: Requirements 9.1, 9.3**

  - [x] 9.3 행동 로그 최대 10개 FIFO 프로퍼티 테스트
    - **Property 20: 행동 로그 최대 10개 유지(FIFO)** — N개 추가 시 크기 `min(N,10)`, 최근 10개 보존
    - **Validates: Requirements 9.2**

  - [x] 9.4 행동 로그 오름차순 표시 프로퍼티 테스트
    - **Property 21: 행동 로그 오름차순 표시** — 표시 목록은 항상 추가 순서 오름차순
    - **Validates: Requirements 9.4**

  - [x] 9.5 MovementService 및 MovementResult 구현
    - `MovementResult` sealed(`Moved`/`Blocked`/`DungeonLocked`)
    - `move(progress, dx, dy)`: 인접·연결 시 현재 노드 갱신 + `move` 로그(대상 맵 이름 포함), 비인접 시 `Blocked`(상태 불변)
    - `enterDungeon(progress, dungeonId)`: 항상 `DungeonLocked`(준비 중), 안내 문구 생성 실패와 무관하게 거부 유지
    - _Requirements: 5.1, 5.3, 5.4, 6.1, 6.3, 6.5_

  - [x] 9.6 인접 이동 성공 프로퍼티 테스트
    - **Property 8: 인접 이동 성공** — 이웃·연결 대상(던전 입구 포함) 이동 성공, 현재 노드 변경, `move` 타입·대상 맵 이름 포함 로그 생성
    - **Validates: Requirements 5.1, 5.3, 6.1**

  - [x] 9.7 비인접 이동 거부 프로퍼티 테스트
    - **Property 9: 비인접 이동 거부** — 미연결 방향 요청은 `Blocked`, 현재 노드 불변
    - **Validates: Requirements 5.4**

  - [x] 9.8 던전 내부 진입 거부 프로퍼티 테스트
    - **Property 10: 던전 내부 진입 거부** — 내부 진입 요청은 항상 `DungeonLocked`, 현재 노드 불변
    - **Validates: Requirements 6.3**

- [x] 10. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. 플레이 화면 뷰 모델 및 뷰 헬퍼
  - [x] 11.1 게이지/상단바 뷰 모델 및 PlayScreenViewHelper 구현
    - `GaugeView`(percent = max>0 ? clamp(round(current*100/max),0,100) : 0, overlay `"current / max"`), EXP는 max=`ExperiencePolicy.requiredForNext(level)`
    - `TopBarView`, `PlayScreenView` 뷰 모델 및 `PlayScreenViewHelper`(표현 계산 위임)
    - _Requirements: 1.4, 1.5, 1.6, 1.11_

  - [x] 11.2 게이지 계산 프로퍼티 테스트
    - **Property 15: 게이지 계산과 수치 오버레이** — `percent=clamp(round(current*100/max),0,100)`, overlay `"current / max"`, current=0 ⇒ percent=0·`"0 / max"`, EXP max=다음 레벨 필요치
    - **Validates: Requirements 1.4, 1.5, 1.6, 1.11**

- [x] 12. 컨트롤러·템플릿·정적 리소스 배선
  - [x] 12.1 PlayScreenController(GET) 및 템플릿·예외 처리 구현
    - `PlayScreenController` `GET /`(루트): `loadOrCreateDefault()` + 맵/미니맵/전체지도/상황멘트/로그 뷰 조합 → `play` 렌더
    - `templates/play.html`(fragment `th:replace` 조합) + `templates/fragments/`(top-bar/left-sidebar/center/minimap/move-pad/action-log/panel-popup/full-map) + `templates/error.html`
    - `GlobalExceptionHandler`(`@ControllerAdvice`, NodeNotFound→404, MapViewGeneration→500, CharacterCreation→error)
    - _Requirements: 1.1, 1.7, 1.8, 1.9, 6.4_

  - [x] 12.2 정적 리소스 배선 및 fragment 상호작용 보존
    - `myrpg.css` 디자인 토큰과 `myrpg.js` 줌/팬/팝업 함수를 fragment 구조에 연결하여 목업과 시각/동작 1:1 유지
    - _Requirements: 1.9, 1.10_

  - [x] 12.3 이동(턴) 엔드포인트 및 htmx 배선 구현
    - `POST /move`(params `dx`,`dy`): `MovementService.move(...)` → 성공 시 `saveTurn` 저장 후 갱신 fragment(top-bar/center/minimap/action-log) 반환, 거부 시 안내 반환
    - `myrpg.js`의 `move(dx,dy)`를 htmx `POST /move` 호출 + fragment swap으로 대체(디자인/동작 동일)
    - _Requirements: 5.1, 5.2, 5.5, 3.3_

  - [x] 12.4 컨트롤러 슬라이스 테스트
    - `@WebMvcTest(PlayScreenController.class)` + `@MockitoBean`, GET/POST 응답에 상단바·미니맵·로그 fragment 및 갱신 결과 포함 검증
    - _Requirements: 1.1, 1.7, 1.8, 1.10, 5.5_

  - [x] 12.5 턴 종료 저장 반영 프로퍼티 테스트
    - **Property 11: 턴 종료 저장 반영** — 성공적 인접 이동(턴 종료) 시 `Character_Store`에 저장되는 진행상황이 변경된 현재 노드 id를 담음(`Mockito.mock()` 리포지토리로 저장 인자 캡처)
    - **Validates: Requirements 3.3, 5.2**

  - [x] 12.6 시각/JS 보존 및 JSON 로딩 통합 테스트
    - `myrpg.css` 디자인 토큰·`myrpg.js` 줌/팬/팝업 함수 보존, fragment 존재 및 `play.html` `th:replace` 조합 검증
    - Jackson 3로 `map.json`/`ambience.json` 역직렬화 및 양방향 링크(Req 4.5) 통합 검증
    - _Requirements: 1.2, 1.3, 4.5_

- [x] 13. 통합 및 컨텍스트 로드 스모크
  - [-] 13.1 컨텍스트 로드 및 리소스 로딩 스모크 테스트
    - `@SpringBootTest`로 기동 및 맵/상황 리소스 로딩 성공 확인
    - _Requirements: 3.5, 10.3_

- [x] 14. 최종 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- 각 Task는 이전 Task 위에 점진적으로 쌓이며, 마지막 컨트롤러/템플릿/정적 리소스 배선(12)에서 모든 컴포넌트가 통합된다(고아 코드 없음).
- 각 Task는 특정 요구사항을 참조하여 추적성을 확보한다.
- 프로퍼티 테스트는 설계의 보편 정확성 속성(jqwik, `@Property(tries = 100)`)을 검증하고, 단위/통합 테스트는 구체 초기값·경계·오류·렌더링 존재를 보완한다.
- 워크스페이스 스티어링에 따라 모든 테스트 하위 작업은 필수(`*` 미사용)이며, Task 완료 전 `mvn test -pl myrpg` 통과와 `mvn clean install -pl myrpg -am` `BUILD SUCCESS`를 확인한다.
- 각 프로퍼티 테스트에는 태그 주석 `Feature: 001-character-progress-and-map-movement, Property {번호}: {프로퍼티 텍스트}`를 부착하고, 각 Correctness Property는 단 하나의 프로퍼티 테스트로 구현한다.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "1.3"] },
    { "id": 2, "tasks": ["2.1", "2.3", "3.1", "9.1"] },
    { "id": 3, "tasks": ["2.2", "3.2", "9.2", "9.3", "9.4"] },
    { "id": 4, "tasks": ["4.1", "7.1", "8.1", "3.3"] },
    { "id": 5, "tasks": ["5.1", "4.2", "4.3", "4.4", "4.5", "7.2", "7.3", "7.4", "8.2", "8.3", "8.4", "9.5"] },
    { "id": 6, "tasks": ["5.2", "5.3", "5.4", "5.5", "5.6", "9.6", "9.7", "9.8", "11.1"] },
    { "id": 7, "tasks": ["11.2", "12.1"] },
    { "id": 8, "tasks": ["12.2"] },
    { "id": 9, "tasks": ["12.3"] },
    { "id": 10, "tasks": ["12.4", "12.5", "12.6", "13.1"] }
  ]
}
```
