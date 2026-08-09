# Implementation Plan: 스킬 시스템

## Overview

스펙 004(`talent-and-ability-points`)로 구축한 `myrpg` Web 모듈(`com.myapps.web.myrpg`) 위에, 설계 문서의 DDD 4계층 구조를 따라 스킬 시스템을 점진적으로 구현한다.

구현 순서 원칙:

- 하위 계층(값 enum → 카탈로그 record/정책) → 영속(엔티티/리포지토리/JSON) → 로더 → 애플리케이션 서비스 → 004 산출물 확장(뷰헬퍼/캐릭터생성/예외) → 표현 계층(컨트롤러/템플릿/정적 리소스) 순으로 조립한다.
- 각 순수 로직 구현 직후 설계의 Correctness Property를 jqwik 프로퍼티 테스트로 확인한다.
- **004 산출물 확장(뷰헬퍼 `skillBonus` 주입, 캐릭터 생성 시드)은 호출부·영향 테스트를 함께 갱신하여 원자적으로 완료**하고, 완료 시점에 빌드가 그린이어야 한다.
- 마지막 표현 계층 배선에서 모든 컴포넌트가 통합된다(고아 코드 없음).

> **테스트 정책 안내**: 워크스페이스 스티어링(`task-build-validation.md`)에 따라 "optional task"는 없다. 아래 모든 테스트 하위 작업은 **필수**이며 `*`를 사용하지 않는다. 각 Task는 `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인 후에만 완료 처리한다.

> **Spring Boot 4.0 / Java 25 규약**: 생성자 주입만(`@Autowired` 금지), Lombok 금지, `var` 금지, VO/DTO는 `record`, `final` 파라미터/지역변수, 커스텀 예외(`RuntimeException` 직접 금지 — `SkillDataException`/`InsufficientAbilityPointsException` 사용). 테스트는 `@MockitoBean` / `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` / `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`(+`@TestConstructor(ALL)`), Jackson 3(`tools.jackson`). jqwik 프로퍼티는 `Mockito.mock()` 직접 사용(`@Mock` 금지), `@Property(tries = 100)`. 프로퍼티 태그 주석: `Feature: 005-skill-system, Property {번호}: {프로퍼티 텍스트}`.

> **지식 보존(`docs/skill-system.md` §14)**: 이 스펙 문서는 영구 보존된다. 코드에는 크로스 시스템 훅(`onSkillUsed`/`onSkillKill`/`learnSkill`/`SkillDamagePolicy`)·임시 드라이버(`dev/fill-*`)·이연 seam에 **담당 순위·제거 조건을 서술형 JavaDoc**으로 남긴다(나열식 `// TODO` 금지).

## Tasks

- [x] 1. 스킬 분류 값 타입 (신규, 기존 빌드 무영향)
  - [x] 1.1 SkillType / ResourceKind enum 구현
    - `domain/model/SkillType.java`: `NORMAL`/`HEAVY`/`DEFENSE` + `label()` + `fromString(String) → Optional`
    - `domain/model/ResourceKind.java`: `STAMINA`/`MP` + `label()`("스태미나"/"MP")
    - _Requirements: 2.1, 9.1_

  - [x] 1.2 SkillTalent enum 구현
    - `domain/model/SkillTalent.java`: `MELEE(TalentType.MELEE)`/`ARCHERY`/`MAGIC`/`COMMON(null)`, `matchingTalent():Optional<TalentType>`, `resourceKind():ResourceKind`(MAGIC→MP, else STAMINA), `rankupStatTarget():BonusTarget`(MELEE→STR/ARCHERY→DEX/MAGIC→INT/COMMON→DEF), `fromString`
    - _Requirements: 2.2, 3.1, 3.2, 8.1, 9.1_

  - [x] 1.3 SkillRank enum 구현
    - `domain/model/SkillRank.java`: 16상수(F..A, R9..R1, MASTER), `label()`("9".."1"/"Master"), `order()`(0~15), `next():Optional`, `isMax()`, `first()`
    - _Requirements: 2.3_

  - [x] 1.4 값 enum 프로퍼티/단위 테스트
    - `SkillRankLadderPropertyTest.java` — **Property 1: 랭크 사다리 정합** — order 0~15, next 체인, MASTER만 isMax/empty
    - **Validates: Requirements 2.3**
    - `SkillTalentMatchingPropertyTest.java` — **Property 4: 재능 매칭** — MELEE/ARCHERY/MAGIC↔TalentType, COMMON empty
    - **Validates: Requirements 2.2, 3.1, 3.2**
    - `SkillTalentResourceKindPropertyTest.java` — **Property 5: 자원 종류 파생** — MAGIC→MP, 그 외→STAMINA
    - **Validates: Requirements 9.1**
    - `SkillRankTest.java` / `SkillTypeTest.java` — 라벨·`fromString` 예시(미지 문자열 empty), `rankupStatTarget` 상수값
    - _Requirements: 2.1, 2.2, 2.3, 8.1_

- [x] 2. 카탈로그 모델·정책 (순수, 기존 빌드 무영향)
  - [x] 2.1 Skill sealed + record + RankUpRequirement 구현
    - `domain/model/Skill.java`(sealed interface), `DamageSkill.java`(record + `multiplierByRank`), `DefenseSkill.java`(record + `blockRateByRank`/`counterMultiplierByRank`), `RankUpRequirement.java`(record `requiredUsage`/`requiredKills`)
    - _Requirements: 4.1, 4.2_

  - [x] 2.2 SkillRankPolicy 구현 (요구치·AP 표)
    - `domain/model/SkillRankPolicy.java`: `requirement(SkillRank):Optional<RankUpRequirement>`, `apCost(SkillRank):OptionalInt`. 설계 표(F→E:5/1/1 … 1→Master:5000/1500/34, apCost 합 200), MASTER는 empty
    - _Requirements: 5.1, 5.2, 6.1, 6.2_

  - [x] 2.3 SkillDamagePolicy / SkillRankupBonus 구현
    - `domain/model/SkillDamagePolicy.java`: `multiplier(DamageSkill, SkillRank)`, `blockRate`/`counterMultiplier(DefenseSkill, SkillRank)` = 맵 조회
    - `domain/model/SkillRankupBonus.java`: `sum(List<CharacterSkill>, SkillCatalog):Stats` = `Σ(rank.order() × talent.rankupStatTarget() 델타)`, 스탯 계열만
    - _Requirements: 4.3, 8.2, 8.3, 8.4_

  - [x] 2.4 정책 프로퍼티/단위 테스트
    - `SkillRankRequirementPropertyTest.java` — **Property 2: 요구치 양수·단조 증가**
    - **Validates: Requirements 5.1, 5.2**
    - `SkillRankApCostPropertyTest.java` — **Property 3: AP 소모 곡선 양수·단조·합 200**
    - **Validates: Requirements 6.1, 6.2**
    - `SkillDamagePolicyPropertyTest.java` — **Property 6: 랭크별 수치 단조 + 조회 정확**
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.4**
    - `SkillRankPolicyTest.java` — 요구치·AP 경계 예시(F→E, 1→Master, 합계 200)
    - _Requirements: 5.1, 6.1_

- [x] 3. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. 영속 모델 + 카탈로그 리소스 (신규)
  - [x] 4.1 CharacterSkill 엔티티 + 리포지토리
    - `domain/model/CharacterSkill.java`: `@Entity @Table("character_skill")`, `characterId`/`skillId`/`rank`(EnumType.STRING)/`usageCount`/`killCount`, `static newSkill(characterId, skillId)`(F/0/0), `increaseUsage`/`increaseKill`/`setUsage`/`setKill`/`rankUpTo(next)`(카운트 0 리셋)
    - `domain/repository/CharacterSkillRepository.java`: `findByCharacterId`, `findByCharacterIdAndSkillId`
    - _Requirements: 10.1, 7.1_

  - [x] 4.2 skill.json 카탈로그 리소스 이식
    - `resources/data/skill.json`: `docs/skills.json`의 7종(smash/windmill/magnum_shot/arrow_revolver/firebolt/icebolt/defense) 이식. 딜스킬 `multiplierByRank`(16키), 디펜스 `blockRateByRank`/`counterMultiplierByRank`(16키), `resourceCost`
    - _Requirements: 1.1_

  - [x] 4.3 영속 라운드트립 프로퍼티 테스트
    - `CharacterSkillPersistencePropertyTest.java`(`@DataJpaTest` + `@TestConstructor(ALL)`) — **Property 16: 영속 라운드트립** — skillId·rank·usageCount·killCount 보존, `findBy*` 조회
    - **Validates: Requirements 10.1, 10.5**

- [x] 5. 카탈로그 로더 (신규)
  - [x] 5.1 SkillDataException + SkillCatalogService 구현
    - `application/exception/SkillDataException.java`(`NpcDataException` 선례)
    - `application/service/SkillCatalogService.java`: `@PostConstruct init()`(classpath:data/skill.json), `loadFromStream(InputStream):List<Skill>`(파싱·검증 분리), `all()`, `byId(String):Optional<Skill>`. 검증: 최상위 배열/필수필드/`type`·`talent` enum 변환/id 중복/랭크맵 16키. Jackson 3(`tools.jackson`)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_

  - [x] 5.2 카탈로그 검증 프로퍼티/통합 테스트
    - `SkillCatalogParsingPropertyTest.java` — **Property 7: 카탈로그 검증** — 미지 type/talent·중복 id·필드 누락·랭크맵 15키 주입 시 `SkillDataException`, 유효 입력은 불변 목록
    - **Validates: Requirements 1.2, 1.4, 1.5, 1.6**
    - `SkillCatalogLoadIntegrationTest.java` — 실제 `data/skill.json` 로드: 7종·랭크 16키 완비·id 유일
    - _Requirements: 1.1, 1.7_

- [x] 6. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. 애플리케이션 서비스 (랭크업·보너스·시드·습득·임시 드라이버)
  - [x] 7.1 InsufficientAbilityPointsException + SkillService 핵심
    - `application/exception/InsufficientAbilityPointsException.java`
    - `application/service/SkillService.java`: `rankUp(CharacterProgress, skillId):RankUpResult`(rankable 판정 → AP 사전검증(부족 시 예외) → `spendAbilityPoints` → `rankUpTo(next)` → 저장), `rankupBonus(characterId):Stats`(`SkillRankupBonus.sum`), `learnSkill(characterId, skillId)`(F 추가·중복/미지 방지), `seedDefault(characterId)`(windmill F)
    - 카운팅 훅 `onSkillUsed`/`onSkillKill`(정의 + 전투 7순위 호출 JavaDoc)
    - 임시 드라이버 `fillUsageToRequirement`/`fillKillToRequirement`(요구치까지 setUsage/setKill, 제거 예정 JavaDoc)
    - _Requirements: 5.3, 5.4, 5.5, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 8.3, 10.3, 11.1, 11.2, 11.3, 14.2, 14.3_

  - [x] 7.2 랭크업 게이트 프로퍼티 테스트
    - `SkillRankUpGatePropertyTest.java` — **Property 8: 랭크업 게이트** — 조건+AP+≠MASTER일 때만 성공, 그 외 상태 불변
    - **Validates: Requirements 5.3, 5.4, 5.5, 7.3**

  - [x] 7.3 랭크업 트랜잭션 효과 프로퍼티 테스트
    - `SkillRankUpEffectPropertyTest.java` — **Property 9: 랭크업 트랜잭션 효과** — rank=next, usage/kill=0, ap-=cost
    - **Validates: Requirements 7.1, 7.2, 6.3**

  - [x] 7.4 AP 소모 가드 프로퍼티 테스트
    - `SkillRankUpApGuardPropertyTest.java` — **Property 10: AP 소모 가드** — ap<cost면 `InsufficientAbilityPointsException`, 상태 불변·음수 방지
    - **Validates: Requirements 6.4, 6.5**

  - [x] 7.5 AP 정합성 불변식(확장) 프로퍼티 테스트
    - `SkillApInvariantPropertyTest.java` — **Property 11: AP 정합성 불변식(확장)** — `ap == (accLv-1) - Σ 소모AP`
    - **Validates: Requirements 6.6**

  - [x] 7.6 랭크업 영구 보너스 합산 프로퍼티 테스트
    - `SkillRankupBonusPropertyTest.java` — **Property 12: 랭크업 영구 보너스 합산** — Σ(order×주스탯), 스탯 계열만·바이탈/Critical 0
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4**

  - [x] 7.7 스킬 습득 프로퍼티 테스트
    - `LearnSkillPropertyTest.java` — **Property 13: 스킬 습득** — 신규 F 추가/중복 무시/미지 거부
    - **Validates: Requirements 11.1, 11.2, 11.3**

  - [x] 7.8 신규 시드/환생 유지 프로퍼티 테스트
    - `SkillSeedPropertyTest.java` — **Property 14: 신규 캐릭터 시드** — windmill 1개만 F/0/0
    - **Validates: Requirements 10.3, 15.4**
    - `SkillRebirthRetentionPropertyTest.java` — **Property 15: 환생 시 스킬 유지** — 목록·랭크·카운트 불변
    - **Validates: Requirements 10.6**

  - [x] 7.9 임시 드라이버 프로퍼티 테스트
    - `SkillTemporaryDriverPropertyTest.java` — **Property 18: 임시 드라이버 100% 충전** — fillUsage/fillKill 후 카운트 = 현재 랭크 요구치
    - **Validates: Requirements 14.2, 14.3**

  - [x] 7.10 랭크업/보너스 예시 단위 테스트
    - `SkillServiceTest.java` — F 스킬 충전→승급→rank=E·카운트0·AP-1·STR+1; windmill A(order5)→STR+5; 전부 마스터→STR/DEX/INT+30·DEF+15
    - _Requirements: 7.1, 8.3, 11.1_

- [x] 8. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. 004 산출물 확장 (원자적, 빌드 그린 유지)
  - [x] 9.1 CharacterService 신규 캐릭터 windmill 시드
    - `application/service/CharacterService.java`: 신규 캐릭터 생성(`loadOrCreateDefault`) 시 `CharacterProgress` 저장 후 `skillService.seedDefault(id)` 호출
    - `CharacterServiceDefault*Test` 갱신(신규 캐릭터 스킬 1개=windmill F)
    - _Requirements: 10.3, 15.4_

  - [x] 9.2 PlayScreenViewHelper 스킬 보너스 주입
    - `interfaces/api/PlayScreenViewHelper.java`: `buildInfo`의 `skillBonus = Stats.ZERO` → `skillService.rankupBonus(progress.getId())`. `SkillService` 생성자 주입 추가. `vitalMaxFor`·게이지·중앙 스탯 표시 구조 무변경
    - `PlayScreenViewHelperInfoTest` 갱신(신규 캐릭터 보너스 0 보존, 스킬 랭크업 후 `(+X)` 반영)
    - _Requirements: 8.5, 8.6_

  - [x] 9.3 GlobalExceptionHandler 확장
    - `interfaces/api/GlobalExceptionHandler.java`: `InsufficientAbilityPointsException` → 승급 거부 안내(상태 불변)
    - _Requirements: 6.4_

- [x] 10. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. 표현 계층 배선 (스킬 팝업·승급 모달)
  - [x] 11.1 뷰 모델 + SkillService 뷰 조립
    - `application/dto/SkillListView.java`/`SkillRowView.java`/`SkillRankUpView.java`(설계 필드)
    - `SkillService`: `buildListView(characterId, activeTab)`(탭 필터·행 조립·`progressPercent` 동일가중 평균·`rankable`), `buildRankUpView(characterId, skillId)`(현재→다음 수치·요구치·AP)
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.6, 13.1_

  - [x] 11.2 진행바 프로퍼티 테스트
    - `SkillProgressPercentPropertyTest.java` — **Property 17: 진행바 동일가중 평균** — `(min(usage/req,1)+min(kill/req,1))/2×100`, [0,100], 둘 다 충족 시 100
    - **Validates: Requirements 12.3**

  - [x] 11.3 SkillController 엔드포인트
    - `interfaces/api/SkillController.java`: `GET /skills`(+`?tab=`), `GET /skills/{id}/rankup-modal`, `POST /skills/{id}/rankup`(→ 새 랭크 모달 스왑), `POST /skills/{id}/dev/fill-usage`·`/dev/fill-kill`(임시, 제거 예정 JavaDoc). fragment 스왑 응답
    - _Requirements: 12.5, 13.2, 13.3, 13.5, 14.1_

  - [x] 11.4 skill-popup.html + play.html include
    - `templates/fragments/skill-popup.html`[신규]: 목록 팝업(탭·행·진행바·승급 버튼 강조색) + 승급 모달(다음 랭크·수치·사용/막타·AP·`[승급]`/`[닫기]`/임시 버튼). MASTER "MAX"/최고 랭크 처리
    - `templates/play.html`[확장]: skill-popup fragment include
    - _Requirements: 12.1, 12.2, 12.4, 12.6, 13.1, 13.6, 14.1_

  - [x] 11.5 myrpg.js 확장 (팝업·탭·승급 confirm·임시 드라이버)
    - `static/js/myrpg.js`: 팝업 열기/닫기, 탭 전환, `openRankUpModal(id)`, `confirmRankUp(id)`=`confirm("승급하시겠습니까?")`→`POST /rankup`→모달 스왑, 임시 `fillUsage/fillKill(id)`→모달 갱신
    - _Requirements: 12.5, 13.2, 13.3, 13.4, 13.5, 14.1_

  - [x] 11.6 myrpg.css 확장
    - `static/css/myrpg.css`: 스킬 팝업/탭/행/진행바/승급 버튼 강조색·모달 스타일(기존 `:root` 디자인 토큰 재사용)
    - _Requirements: 12.4_

  - [x] 11.7 컨트롤러 슬라이스 테스트
    - `SkillControllerTest.java`(`@WebMvcTest` + `@MockitoBean SkillService`) — 목록/탭 렌더, 승급 모달, `POST /rankup` 성공→새 랭크 스왑·AP부족→안내(`InsufficientAbilityPointsException` 핸들링), `dev/fill-*` 충전 후 갱신
    - _Requirements: 12.1, 12.5, 13.3, 13.5, 14.1, 6.4_

- [x] 12. 통합·스모크·로컬 세이브 초기화
  - [x] 12.1 로컬 H2 세이브 초기화
    - 로컬 세이브 파일(`myrpg/data/myrpg*`)을 삭제하여 다음 기동 시 신규 캐릭터(windmill F 시드)가 생성되도록 한다. 프로덕션(`ddl-auto: create`)은 기동 시 자동 초기화
    - _Requirements: 15.1, 15.2, 15.3, 15.4_

  - [x] 12.2 컨텍스트 로드 스모크 테스트
    - `SkillContextLoadSmokeTest.java`(`@SpringBootTest`) — 기동 및 `SkillCatalogService`/`SkillService` 빈 로딩, 정보 팝업 스킬 보너스 경로·스킬 팝업 렌더 정상
    - _Requirements: 1.1, 8.5_

- [x] 13. 최종 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- 각 Task는 이전 Task 위에 점진적으로 쌓이며, 표현 계층 배선(11)에서 모든 컴포넌트가 통합된다(고아 코드 없음).
- 004 산출물 확장(9)은 `PlayScreenViewHelper` 스킬 보너스 주입·`CharacterService` 시드·`GlobalExceptionHandler`로 영향이 있으므로, 호출부·영향 테스트를 함께 갱신하여 **완료 시점에 빌드 그린**을 보장한다. `CharacterProgress`는 무변경(AP 검증은 서비스단).
- 프로퍼티 테스트는 설계의 18개 정확성 속성(jqwik, `@Property(tries=100)`)을 검증하고, 단위/슬라이스/통합 테스트가 구체 값·렌더링·컨텍스트 로딩·영속을 보완한다.
- 각 Correctness Property는 단 하나의 프로퍼티 테스트로 구현하며, 태그 주석 `Feature: 005-skill-system, Property {번호}: {프로퍼티 텍스트}`를 부착한다.
- **이연 항목(전투 7순위 등)**: 카운팅 훅(`onSkillUsed`/`onSkillKill`), `SkillDamagePolicy` 소비, 재능 +10% 적용, 자원 차감, 무기 재능 스킬 필터링, 임시 드라이버(`dev/fill-*`) 제거. 각 seam은 담당 순위·제거 조건을 JavaDoc으로 명시한다(`docs/skill-system.md` §13·§14.4).
- Task 완료 전 `mvn test -pl myrpg` 통과와 `mvn clean install -pl myrpg -am` `BUILD SUCCESS`를 확인한다.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.3", "2.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["1.4", "2.2", "2.3"] },
    { "id": 3, "tasks": ["2.4", "4.1", "4.2"] },
    { "id": 4, "tasks": ["4.3", "5.1"] },
    { "id": 5, "tasks": ["5.2", "7.1"] },
    { "id": 6, "tasks": ["7.2", "7.3", "7.4", "7.5", "7.6", "7.7", "7.8", "7.9", "7.10"] },
    { "id": 7, "tasks": ["9.1", "9.2", "9.3"] },
    { "id": 8, "tasks": ["11.1", "11.3", "11.4", "11.6"] },
    { "id": 9, "tasks": ["11.2", "11.5", "11.7"] },
    { "id": 10, "tasks": ["12.1", "12.2"] }
  ]
}
```
