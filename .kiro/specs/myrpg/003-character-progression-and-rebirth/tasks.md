# Implementation Plan: 기본 시스템 행동 (경험치·레벨업·스탯 성장·사망·환생·재능·정보 팝업)

## Overview

스펙 001/002로 구축한 `myrpg` Web 모듈(`com.myapps.web.myrpg`) 위에, 설계 문서의 DDD 4계층 구조를 따라 기본 시스템 행동을 점진적으로 구현한다.

구현 순서 원칙:

- 하위 계층(도메인 enum/정책/모델) → 애플리케이션 서비스(`ProgressionService`) → 뷰 모델/헬퍼 → 표현 계층(컨트롤러/템플릿/정적 리소스) 순으로 조립한다.
- **모델 리팩터링(스탯·최대 바이탈 저장 제거)은 빌드가 깨지지 않도록 의존 코드·영향 테스트를 함께 갱신하여 원자적으로 완료**한다(설계 "Migration 영향 범위").
- 각 순수 로직을 구현한 직후 설계의 Correctness Property를 jqwik 프로퍼티 테스트로 확인한다.
- 마지막 표현 계층 배선에서 모든 컴포넌트가 통합된다(고아 코드 없음).

> **테스트 정책 안내**: 워크스페이스 스티어링(`task-build-validation.md`)에 따라 "optional task"는 없다. 아래 모든 테스트 하위 작업은 **필수**이며 `*`를 사용하지 않는다. 각 Task는 `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인 후에만 완료 처리한다.

> **Spring Boot 4.0 / Java 25 규약**: 생성자 주입만(`@Autowired` 금지), Lombok 금지, `var` 금지, VO/DTO는 `record`, `final` 파라미터/지역변수, 커스텀 예외(`RuntimeException` 직접 금지). 테스트는 `@MockitoBean` / `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` / `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`. jqwik 프로퍼티는 `Mockito.mock()` 직접 사용(`@Mock` 금지), `@Property(tries = 100)`. 프로퍼티 테스트 태그 주석 형식: `Feature: 003-character-progression-and-rebirth, Property {번호}: {프로퍼티 텍스트}`.

## Tasks

- [x] 1. 도메인 정책·enum (기존 빌드 무영향 신규 추가)
  - [x] 1.1 TalentType enum 구현
    - `domain/model/TalentType.java`: `MELEE("근접전투")`/`ARCHERY("활")`/`MAGIC("마법")`, `label()` 접근자
    - _Requirements: 9.1_

  - [x] 1.2 TalentType 라벨 단위 테스트
    - `TalentTypeTest.java` — 3개 상수의 `label()` 매핑(`MELEE`→`근접전투`, `ARCHERY`→`활`, `MAGIC`→`마법`) 검증
    - _Requirements: 9.1_

  - [x] 1.3 ExperiencePolicy 곡선 교체
    - `domain/model/ExperiencePolicy.java`: `requiredForNext(level) = 100L × level × level`로 변경(매직 넘버 상수화)
    - _Requirements: 2.1, 11.5_

  - [x] 1.4 경험치 곡선 프로퍼티 테스트
    - `ExperiencePolicyCurvePropertyTest.java` — **Property 1: 경험치 곡선** — `L ∈ [1,100]`에서 `requiredForNext(L) = 100×L²`, 단조 증가 검증
    - **Validates: Requirements 2.1, 11.5**

  - [x] 1.5 StatProgression 구현
    - `domain/model/StatProgression.java`(순수): `levelStatsFor(level)`(기본값+레벨파생, critical 0.1%단위, 스킬 보너스 제외), `vitalMaxFor(level)`(`100 + 10×(level-1)`); 기본값·레벨업당 증가는 `private static final` 상수
    - `domain/model/Stats.java`에 `Stats.ZERO`(모든 값 0) 상수 추가
    - `domain/service/DomainServiceConfiguration.java`에 `StatProgression` 빈 등록
    - _Requirements: 3.1, 3.2, 4.1, 4.2, 4.3, 5.1, 5.3_

  - [x] 1.6 스탯 계산 프로퍼티 테스트
    - `StatProgressionPropertyTest.java` — **Property 5: 레벨 파생 스탯 계산** — `levelStatsFor(L)`의 STR/DEX/INT/Critical(tenths)/DEF와 `vitalMaxFor(L)` 공식 검증
    - **Validates: Requirements 3.1, 3.2, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3**

- [x] 2. CharacterProgress 재정의 및 모델 마이그레이션 (원자적, 빌드 그린 유지)
  - [x] 2.1 Stats/Vital VO화 및 CharacterProgress 필드 재정의
    - `Stats`/`Vital`에서 `@Embeddable` 제거(순수 표시 VO). `Stats.critical`은 0.1% 단위 의미(주석 명시)
    - `CharacterProgress`: 저장 필드를 nickname/currentLevel/accumulatedLevel/experience/`talent`(`@Enumerated(STRING)`)/`lastRebirthAt`(`LocalDateTime`, nullable)/`hpCurrent`/`mpCurrent`/`staminaCurrent`/currentNodeId로 재정의(스탯·최대 바이탈 저장 제거)
    - mutator 추가: `setCurrentLevel`, `increaseAccumulatedLevel`, `setExperience`, `setTalent`, `setLastRebirthAt`, `fullRecover(int max)`
    - `createDefault()`: Lv1/누적1/EXP0, 재능 `MELEE`, `lastRebirthAt=null`, 현재 바이탈 100/100/100, 노드 `tir-chonaill`
    - _Requirements: 5.2, 9.2, 11.1, 11.2, 11.3_

  - [x] 2.2 PlayScreenViewHelper 컴파일 대응(계산 기반 게이지)
    - `StatProgression` 주입. HP/MP/Stamina 게이지를 `현재치(저장) / vitalMaxFor(level)`로 조립하도록 변경(기존 `progress.getHp()` 등 제거)
    - EXP 게이지: 최대레벨(100)이면 `percent=100`·overlay `"MAX"`, 미만이면 `experience / requiredForNext(level)`
    - _Requirements: 2.5, 2.6, 4.1_

  - [x] 2.3 영향받는 001 테스트 갱신 (빌드 그린)
    - 신규 모델/곡선/기본값에 맞게 갱신: `CharacterServiceDefaultValuesTest`, `CharacterServiceDefaultCreationPropertyTest`, `CharacterServiceLoadExistingPropertyTest`, `CharacterServiceTurnSavePropertyTest`, `PlayScreenViewHelperTest`, `PlayScreenViewHelperGaugePropertyTest`
    - `MovementService`/맵/NPC 관련 테스트는 무영향 확인(현재 노드 id·이동 로직 불변)
    - _Requirements: 11.1, 11.4_

  - [x] 2.4 진행상황 영속 라운드트립 프로퍼티 테스트
    - `CharacterProgressPersistencePropertyTest.java`(`@DataJpaTest` + `@TestConstructor(ALL)`, `TestEntityManager`) — **Property 10: 진행상황 영속 라운드트립** — 신규 필드(talent/lastRebirthAt(nullable)/현재 바이탈/레벨/누적/경험치/노드) 저장→조회 보존
    - **Validates: Requirements 11.1, 11.2, 11.4**

- [x] 3. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. ProgressionService (경험치/레벨업/사망/환생 규칙)
  - [x] 4.1 결과·상태 DTO 정의
    - `application/dto`: `LevelUpResult(int levelsGained, int newLevel)`, `DeathResult(long experienceLost)`, `RebirthStatus(boolean available, boolean everRebirthed, Duration elapsed, Duration remaining)`, `RebirthResult` **sealed**(`Reborn`/`CooldownActive(Duration remaining)`)
    - _Requirements: 6, 7, 8_

  - [x] 4.2 ProgressionService 구현
    - `application/service/ProgressionService.java`: `@Service`, 생성자 주입(`ExperiencePolicy`, `StatProgression`, `Clock`)
    - `gainExperience(p, amount)`: 최대레벨이면 무변경; `amount<0`은 0 취급; 연속 레벨업(필요치 차감·레벨/누적 증가), 최대레벨 도달 시 잔여 경험치 0, 레벨업 발생 시 풀회복
    - `applyDeathPenalty(p)`: 최대레벨이면 무변경; `newExp = max(0, exp - floor(requiredForNext(level)×0.10))`, 레벨/누적/재능 불변
    - `rebirthStatus(p)`: `lastRebirthAt==null`→available; 아니면 `elapsed≥24h` 판정, elapsed/remaining 산출
    - `rebirth(p)`: 불가 시 `CooldownActive`; 가능 시 level=1/exp=0/누적+1/재능 MELEE/lastRebirthAt=now/풀회복 후 `Reborn`
    - _Requirements: 1.2, 1.5, 2.2, 2.3, 2.4, 3.3, 3.4, 6.1, 6.2, 6.3, 6.4, 7.1, 7.2, 7.3, 7.4, 8.1, 8.2, 8.3, 8.5, 8.6, 9.3_

  - [x] 4.3 레벨업·경험치 보존 프로퍼티 테스트
    - `GainExperienceLevelUpPropertyTest.java` — **Property 2: 레벨업과 경험치 보존** — 최대레벨 미만+임의 amount≥0에서 최종 exp<필요치, 획득 레벨 수=증가 레벨, 경험치 총량 보존
    - **Validates: Requirements 2.2, 2.3**

  - [x] 4.4 최대레벨 캡 프로퍼티 테스트
    - `MaxLevelCapPropertyTest.java` — **Property 3: 최대레벨 캡** — level≤100 유지, level==100이면 무변경, 100 도달 시 잔여 경험치 0
    - **Validates: Requirements 1.1, 1.5, 2.4**

  - [x] 4.5 누적레벨 불변식 프로퍼티 테스트
    - `AccumulatedLevelInvariantPropertyTest.java` — **Property 4: 누적레벨 불변식** — 레벨업·환생 임의 시퀀스에서 레벨업당 +1/환생당 +1, `누적 = 현재 + 과거 생애 도달 합`
    - **Validates: Requirements 1.2, 1.4, 8.2**

  - [x] 4.6 레벨업 풀회복 프로퍼티 테스트
    - `LevelUpFullRecoveryPropertyTest.java` — **Property 6: 레벨업 시 풀회복** — 1회 이상 레벨업 시 HP/MP/Stamina 현재치 = `vitalMaxFor(최종 레벨)`
    - **Validates: Requirements 3.3, 3.4**

  - [x] 4.7 사망 패널티 프로퍼티 테스트
    - `DeathPenaltyPropertyTest.java` — **Property 7: 사망 패널티** — `exp=max(0, prev - floor(required×0.1))`, 레벨/누적/재능 불변, 최대레벨 무변경
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4**

  - [x] 4.8 환생 효과 프로퍼티 테스트
    - `RebirthEffectPropertyTest.java` — **Property 8: 환생 효과** — level=1/exp=0/누적+1/재능 MELEE/lastRebirthAt=now/풀회복, 표시 스탯 기본값 복귀
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.5, 8.6, 9.3**

  - [x] 4.9 환생 쿨다운 판정 프로퍼티 테스트
    - `RebirthCooldownPropertyTest.java`(`Clock` 고정 주입) — **Property 9: 환생 쿨다운 판정** — available 규칙(null/≥24h/그외), 불가 시 `rebirth`는 `CooldownActive`·상태 불변
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4, 10.9**

  - [x] 4.10 사망 패널티 예시 단위 테스트
    - `ProgressionServiceTest.java` — `23/100`→`13/100`, `5/100`→`0/100`, 곡선 샘플(L1→100,L2→400,L10→10000), 신규 캐릭터 기본값(재능 MELEE, 바이탈 100, Critical 50) 검증
    - _Requirements: 5.2, 6.1, 6.2, 9.2_

- [x] 5. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. 정보 팝업 뷰 모델 및 뷰 헬퍼
  - [x] 6.1 뷰 모델 신규·확장
    - `application/dto/StatLine.java`(record: `label`, `value`, `bonus`), `application/dto/InfoPopupView.java`(nickname/currentLevel/accumulatedLevel/talentLabel/hp·mp·stamina(GaugeView)/`List<StatLine>` stats/rebirthAvailable/rebirthElapsedText)
    - `application/dto/PlayScreenView.java`에 `InfoPopupView info` 필드 추가
    - _Requirements: 10.1, 10.2, 10.3_

  - [x] 6.2 PlayScreenViewHelper.buildInfo 및 포맷 확장
    - `buildInfo(progress, rebirthStatus)`: 상단(닉네임/레벨/누적/`talent.label()`/HP·MP·Stamina 게이지), 중앙(`levelStatsFor(level)` 본체 + 스킬 보너스 `Stats.ZERO`로 `StatLine` 목록), 하단(available/elapsedText)
    - `formatCritical(tenths)`(`"X.X%"`), `formatCriticalDelta(tenths)`(`"+X.X%"`), 정수 스탯 괄호 `"+"+bonus`
    - `rebirthElapsedText`: everRebirthed면 `"환생 후 {H}시간 {M}분 경과"`, 아니면 `"환생 기록 없음"`
    - `buildPlayScreen(...)`에 `InfoPopupView info` 인자 추가(모든 호출 경로에서 조립)
    - _Requirements: 10.2, 10.3, 10.4, 10.7, 5.4_

  - [x] 6.3 Critical 포맷 프로퍼티 테스트
    - `CriticalFormatPropertyTest.java` — **Property 11: Critical 표시 포맷** — `t≥0`에서 `formatCritical(t) = "{t/10}.{t%10}%"`
    - **Validates: Requirements 5.4**

  - [x] 6.4 EXP 게이지·최대레벨 프로퍼티 테스트
    - `ExpGaugeMaxLevelPropertyTest.java` — **Property 12: EXP 게이지와 최대레벨 표기** — level<100이면 percent/overlay 계산, level==100이면 percent=100·overlay `"MAX"`
    - **Validates: Requirements 2.5, 2.6**

  - [x] 6.5 StatLine 분리 표기 단위 테스트
    - `PlayScreenViewHelperInfoTest.java` — `buildInfo` 결과의 StatLine 형식(예: `("STR","23","+0")`, `("CRIT","34.7%","+0.0%")`), 재능 라벨, 환생 경과/기록 없음 텍스트 검증
    - _Requirements: 10.2, 10.3, 10.7_

- [x] 7. 컨트롤러·템플릿·정적 리소스 배선
  - [x] 7.1 PlayScreenController 확장 (info 포함 + 진행 엔드포인트)
    - `GET /` 뷰에 `info`(rebirthStatus 기반) 포함
    - `POST /exp/up`(고정 500), `POST /exp/down`, `POST /rebirth` 추가: load → ProgressionService 호출 → (변경 시) `saveTurn` → 피드백 로그(ActionLog) → `fragments/progress-response` 반환. `/rebirth`는 `CooldownActive` 시 저장 없이 안내 로그
    - `TEST_EXP_AMOUNT = 500L` 컨트롤러 상수
    - _Requirements: 2.5, 6.5, 8.7, 10.8, 10.9, 10.12, 12.1, 12.2, 12.3, 12.4, 12.5_

  - [x] 7.2 템플릿 신규·확장
    - `fragments/info-popup.html`[신규]: `info-popup`(overlay `#infoOverlay`) + `info-content`(`#infoContent`) 상/중/하 3구역, 환생 버튼 `th:attr="disabled=${view.info.rebirthAvailable} ? null : 'disabled'"` + `onclick="rebirth()"`, 경과시간 텍스트
    - `fragments/progress-response.html`[신규]: top-bar + info-content + action-log 스왑 응답
    - `fragments/left-sidebar.html`[확장]: 정보 버튼 `onclick="openInfo()"`, 아래에 `[경험치 업] onclick="expUp()"`·`[경험치 다운] onclick="expDown()"`
    - `play.html`[확장]: `info-popup` fragment include
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 12.1_

  - [x] 7.3 정적 리소스 확장
    - `static/js/myrpg.js`[확장]: `openInfo()/closeInfo()`, `expUp()/expDown()`(POST → `.top-bar` replace, `#infoContent` innerHTML 교체, `.action-log` replace), `rebirth()`(`confirm("환생을 진행하시겠습니까?")` 후 POST → 동일 스왑)
    - `static/css/myrpg.css`[확장]: 정보 팝업 상/중/하 레이아웃·비활성 버튼 스타일(디자인 토큰 재사용)
    - _Requirements: 10.8, 10.13, 12.1_

  - [x] 7.4 컨트롤러 슬라이스 테스트
    - `PlayScreenControllerProgressionTest.java`(`@WebMvcTest` + `@MockitoBean`): `GET /` info-popup 상/중/하 렌더(재능 라벨·StatLine·환생 버튼 상태), 최대레벨 EXP `MAX`; `POST /exp/up`·`/exp/down` 갱신; `POST /rebirth` 가능/쿨다운 분기; 좌측 사이드바 테스트 버튼 노출
    - _Requirements: 2.5, 10.1, 10.2, 10.3, 10.5, 10.6, 10.8, 10.9, 12.1, 12.2, 12.4_

- [x] 8. 통합 및 컨텍스트 로드 스모크
  - [x] 8.1 컨텍스트 로드 스모크 테스트
    - `ProgressionContextLoadSmokeTest.java`(`@SpringBootTest`): 기동 및 신규 빈(`StatProgression`, `ProgressionService`) 로딩, 정보 팝업 렌더링 경로 정상
    - _Requirements: 11.3_

- [x] 9. 최종 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- 각 Task는 이전 Task 위에 점진적으로 쌓이며, 표현 계층 배선(7)에서 모든 컴포넌트가 통합된다(고아 코드 없음).
- 모델 리팩터링(2)은 스탯·최대 바이탈 저장 제거로 001 산출물/테스트에 영향이 크므로, 의존 코드·영향 테스트를 함께 갱신하여 **완료 시점에 빌드 그린**을 보장한다.
- 프로퍼티 테스트는 설계의 12개 정확성 속성(jqwik, `@Property(tries=100)`)을 검증하고, 단위/슬라이스/통합 테스트가 구체 초기값·포맷·렌더링·컨텍스트 로딩을 보완한다.
- 각 Correctness Property는 단 하나의 프로퍼티 테스트로 구현하며, 태그 주석 `Feature: 003-character-progression-and-rebirth, Property {번호}: {프로퍼티 텍스트}`를 부착한다.
- Task 완료 전 `mvn test -pl myrpg` 통과와 `mvn clean install -pl myrpg -am` `BUILD SUCCESS`를 확인한다.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.3", "1.5"] },
    { "id": 1, "tasks": ["1.2", "1.4", "1.6"] },
    { "id": 2, "tasks": ["2.1"] },
    { "id": 3, "tasks": ["2.2", "2.3"] },
    { "id": 4, "tasks": ["2.4"] },
    { "id": 5, "tasks": ["4.1"] },
    { "id": 6, "tasks": ["4.2"] },
    { "id": 7, "tasks": ["4.3", "4.4", "4.5", "4.6", "4.7", "4.8", "4.9", "4.10"] },
    { "id": 8, "tasks": ["6.1"] },
    { "id": 9, "tasks": ["6.2"] },
    { "id": 10, "tasks": ["6.3", "6.4", "6.5"] },
    { "id": 11, "tasks": ["7.1", "7.2", "7.3"] },
    { "id": 12, "tasks": ["7.4", "8.1"] }
  ]
}
```
