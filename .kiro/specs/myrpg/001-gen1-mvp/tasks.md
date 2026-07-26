# Implementation Plan: myrpg 세대 1 MVP

## Overview

Spring Boot 4.0 + Thymeleaf 기반 텍스트 턴제 웹 RPG `myrpg` 모듈(`com.myapps.web.myrpg`)의 세대 1(레벨 밴드 1~10) MVP를 구현한다. 콘텐츠(JSON)/상태(DB) 분리, 순수 도메인 규칙 서비스, `RandomSource` 추상화 기반 결정론적 테스트를 원칙으로 한다.

구현 순서는 아래를 따른다.

1. 모듈 스캐폴딩 → 열거형/값 객체 → JPA 엔티티/리포지터리 → 마스터 데이터 로더
2. 순수 규칙 서비스(StatCalculator → Character → Battle → Drop → Dungeon → Shop → Equipment)를 각각 구현하고 곧바로 속성 기반 테스트(jqwik)로 검증
3. application 오케스트레이션(GameSessionService, BattleSessionService)으로 유스케이스를 조립
4. interfaces 계층(GameController + Thymeleaf 8화면) 및 전체 와이어링

빌드/테스트 검증은 워크스페이스 규칙(`task-build-validation.md`)을 따라 각 Task 완료 전 `mvn test -pl myrpg` 및 `mvn clean install -pl myrpg -am`으로 확인한다.

> 주의: 본 프로젝트 규칙상 모든 Task(테스트 서브태스크 포함)는 필수다. Optional 표시(`*`)는 사용하지 않으며 모든 서브태스크를 반드시 구현한다.

## Tasks

- [x] 1. myrpg 모듈 스캐폴딩 및 빌드 설정
  - [x] 1.1 myrpg/pom.xml 생성 및 Parent POM 모듈 등록
    - `artifactId = myrpg`, `groupId = com.myapps`, 버전 `1.0.0-SNAPSHOT`, Parent POM 상속
    - 의존성 추가: `spring-boot-starter-web`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-data-jpa`, H2 런타임, Jackson 3(`tools.jackson`), 테스트(`spring-boot-starter-test`, `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, jqwik)
    - 루트 `pom.xml`의 `<modules>`에 `myrpg` 추가
    - _Requirements: 25.1, 25.2_
  - [x] 1.2 애플리케이션 진입점 및 리소스 골격 생성
    - `com.myapps.web.myrpg.MyrpgApplication` 작성
    - `resources/application.yml`에 H2 데이터소스/JPA 설정
    - DDD 패키지 디렉터리(domain/application/interfaces) 및 `resources/data`, `resources/templates` 생성
    - _Requirements: 25.1, 25.2_

- [x] 2. 도메인 열거형 및 값 객체 정의
  - [x] 2.1 게임 열거형 정의
    - `Grade, WeaponType, ArmorSlot, StatType, DamageType, DropCategory, StageEventType, ItemType, EffectType` 작성
    - `Grade`에 등급 레벨 보너스(0/2/5/8/10), 판매가 배수(1.0/1.6/3.0/6.0/12.0), 스킬슬롯 수(1~5)를 필드/메서드로 노출
    - _Requirements: 12.1, 14.1, 14.2, 15.2, 23.2_
  - [x] 2.2 값 객체 record 정의
    - `EffectiveStats, DamageResult, StatRoll, RolledWeapon, RolledArmor, DropResult, TreasureReward, LevelUpResult, TurnOrder, TreasureKind` 작성
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 14.5, 17.1_
  - [x] 2.3 열거형 단위 테스트 작성
    - `Grade`의 등급 레벨 보너스·판매가 배수·스킬슬롯 수 값 검증(JUnit 5)
    - _Requirements: 14.2, 15.2, 23.2_

- [x] 3. JPA 엔티티 및 리포지터리 구현
  - [x] 3.1 rpg_ 접두사 JPA 엔티티 작성
    - `Player, PlayerWeapon, PlayerWeaponStat, PlayerWeaponSkill, PlayerArmor, PlayerArmorStat, PlayerInventory, PlayerDungeonProgress, PlayerActiveRun` 작성
    - Lombok 미사용, 명시적 getter/상태 변경 메서드, JPA용 `protected` 기본 생성자, `@Table(name = "rpg_...")`
    - 연관관계 대신 `player_id`/`template_id`/`item_ref_id` 식별자 컬럼 직접 보유
    - _Requirements: 25.2, 25.3_
  - [x] 3.2 Spring Data JPA 리포지터리 인터페이스 작성
    - `PlayerRepository, PlayerWeaponRepository, PlayerArmorRepository, PlayerInventoryRepository, PlayerDungeonProgressRepository, PlayerActiveRunRepository`
    - _Requirements: 25.2, 25.3_
  - [x] 3.3 리포지터리 슬라이스 테스트 작성
    - `@DataJpaTest`(Spring Boot 4.0 패키지)로 엔티티 저장/조회 및 `rpg_player_active_run` 플레이어당 유일성 검증
    - _Requirements: 25.2, 21.5_

- [x] 4. 마스터 데이터 템플릿 및 로더 구현
  - [x] 4.1 마스터 데이터 template record 작성
    - `MonsterTemplate, WeaponTemplate, ArmorTemplate, SkillTemplate, ItemTemplate, DungeonSpawn, DungeonTemplate`
    - _Requirements: 25.1_
  - [x] 4.2 JSON 마스터 데이터 복사 및 MasterDataLoader 구현
    - `docs/generations/gen1/data/*.json`을 `resources/data/`로 복사
    - Jackson 3(`tools.jackson.databind.ObjectMapper`)로 기동 시 로딩, `id → 템플릿` 인덱싱, 조회 메서드 제공
    - 미존재 id 조회 시 `MasterDataNotFoundException`, `gradeChance` 합(1e-6 허용오차) 검증 실패 시 `MasterDataValidationException`
    - _Requirements: 25.1, 18.4_
  - [x] 4.3 로더 및 세대1 콘텐츠 볼륨 테스트 작성
    - 던전별 gradeChance 분포 값(숲/광산/탑) 및 합 = 1.0 검증
    - 콘텐츠 볼륨(던전 3·무기 6·방어구 4·스킬 6·몬스터 9) 검증
    - **Property 34(부분): 등급 분포 정합성 — 합 = 1.0**
    - **Property 50: 모든 세대1 던전은 무기 6종·방어구 4종 전체를 드랍**
    - _Requirements: 18.1, 18.2, 18.3, 18.4, 27.1, 27.2, 27.3, 27.4, 27.5_

- [x] 5. RandomSource 추상화 구현
  - [x] 5.1 RandomSource 인터페이스 및 운영 구현 작성
    - `RandomSource` 인터페이스(`nextDouble`, `nextInt`, `nextIntInclusive`, `nextDoubleInRange`)
    - `ThreadLocalRandomSource` 운영 구현
    - 테스트용 고정 값 주입 구현(`FixedRandomSource`)을 테스트 소스에 작성
    - _Requirements: 25.1_
  - [x] 5.2 RandomSource 단위 테스트 작성
    - 고정 시드/고정 값 주입 시 결정론적 반환 검증
    - _Requirements: 25.1_

- [x] 6. StatCalculator (유효 스탯 합산) 구현
  - [x] 6.1 StatCalculator.compute 구현
    - 캐릭터 기본 스탯 + 무기 base값 + 장비 랜덤 스탯 합산, 무기 타입에 따른 데미지 타입(STAFF→MAGICAL) 산출
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 12.2, 12.3_
  - [x] 6.2 StatCalculator 속성 기반 테스트 작성
    - **Property 9: 유효 스탯 합산 — Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 12.2**
    - **Property 10: 무기 타입에 따른 데미지 타입 — Validates: Requirements 5.6, 5.7, 12.3**
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 12.2, 12.3_

- [x] 7. CharacterService (스탯·레벨업·페널티) 구현
  - [x] 7.1 CharacterService 구현
    - `requiredExp`, `gainExp`, `applyExpPenalty`, `restoreToTown`, 초기 캐릭터 생성 팩토리
    - _Requirements: 1.1, 2.2, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 4.1, 4.2, 4.4, 4.5, 4.6_
  - [x] 7.2 필요 경험치 공식 속성 테스트 작성
    - **Property 3: 필요 경험치 공식 — Validates: Requirements 3.1**
    - _Requirements: 3.1_
  - [x] 7.3 레벨업 불변식 속성 테스트 작성
    - **Property 4: 레벨업 종료 불변식 — Validates: Requirements 2.2, 3.2, 3.5, 3.7**
    - **Property 5: 레벨업 시 스탯 증가 및 HP/MP 완충 — Validates: Requirements 3.3, 3.4**
    - **Property 6: 음수 경험치 거부 — Validates: Requirements 3.6**
    - _Requirements: 2.2, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_
  - [x] 7.4 경험치 페널티·마을 복귀 속성 테스트 작성
    - **Property 7: 경험치 페널티 불변식 — Validates: Requirements 4.1, 4.2, 4.4, 4.5**
    - **Property 8: 마을 복귀 시 완전 회복과 아이템 보존 — Validates: Requirements 4.6, 4.7**
    - _Requirements: 4.1, 4.2, 4.4, 4.5, 4.6, 4.7_
  - [x] 7.5 초기 캐릭터 생성 단위 테스트 작성
    - Lv1/HP100/MP50/공10/방5/속5/치0/exp0/gold0 초기값 검증
    - _Requirements: 1.1_

- [x] 8. BattleService (데미지·치명타·선후공·자원) 구현
  - [x] 8.1 BattleService 구현
    - `criticalChance`, `computeDamage`(기본→편차→치명타→최소1 순서), `decideTurnOrder`, `monsterDamage`, `attemptFlee`, MP 검증/차감·회복, 포션 회복 상한, 소모품 수량 감소
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 7.1, 7.2, 7.3, 7.4, 8.1, 8.2, 8.3, 9.1, 10.2, 10.3, 10.4, 10.5, 10.6, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_
  - [x] 8.2 데미지 파이프라인 속성 테스트 작성
    - **Property 11: 데미지 최소값과 산출 순서 보장 — Validates: Requirements 6.4, 7.4, 10.2, 10.6**
    - **Property 12: 데미지 방어 계수와 배율 단조성 — Validates: Requirements 6.1, 6.2, 6.6, 10.3, 10.4**
    - _Requirements: 6.1, 6.2, 6.4, 6.6, 7.4, 10.2, 10.3, 10.4, 10.6_
  - [x] 8.3 랜덤 편차·치명타 배율 속성 테스트 작성
    - **Property 13: 랜덤 편차 범위 — Validates: Requirements 6.3, 10.5**
    - **Property 14: 치명타 배율 — Validates: Requirements 7.3**
    - _Requirements: 6.3, 7.3, 10.5_
  - [x] 8.4 치명타 확률·선후공·도망 속성 테스트 작성
    - **Property 15: 치명타 확률 클램프 — Validates: Requirements 7.1**
    - **Property 16: 치명타 판정 규칙 — Validates: Requirements 7.2**
    - **Property 17: 선후공 판정 — Validates: Requirements 8.1, 8.2, 8.3**
    - **Property 19: 도망 성공 판정 — Validates: Requirements 9.1**
    - _Requirements: 7.1, 7.2, 8.1, 8.2, 8.3, 9.1_
  - [x] 8.5 전투 자원 관리 속성 테스트 작성
    - **Property 20: 전투 종료 시 MP 완전 회복 — Validates: Requirements 11.1**
    - **Property 21: 스킬 MP 소비와 부족 시 거부 — Validates: Requirements 11.2, 11.3**
    - **Property 22: 포션 회복 상한 — Validates: Requirements 11.4, 11.5**
    - **Property 23: 소모품 사용 시 수량 감소 — Validates: Requirements 11.6**
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

- [x] 9. DropService (드랍 롤) 구현
  - [x] 9.1 DropService 구현
    - `slotCount`, `rollStatCount`, `effectivePowerLevel`, `rollBaseAttack`, `rollStats`, `rollGrade`, `rollDrop`, `buildWeaponInstance`, `buildArmorInstance`
    - _Requirements: 14.2, 14.3, 14.4, 14.5, 15.1, 15.2, 15.3, 15.4, 15.5, 16.1, 16.2, 16.3, 16.4, 17.1, 17.2, 17.3, 17.4, 17.5, 17.6, 17.7, 18.4_
  - [x] 9.2 등급 구조·표시명·파워 레벨 속성 테스트 작성
    - **Property 24: 등급별 무기 스킬슬롯 수 — Validates: Requirements 14.2**
    - **Property 25: 등급별 능력치 개수 범위 — Validates: Requirements 14.3, 16.3**
    - **Property 26: 인스턴스 표시명 형식 — Validates: Requirements 14.5**
    - **Property 27: 유효 파워 레벨 산출 — Validates: Requirements 15.2**
    - _Requirements: 14.2, 14.3, 14.5, 15.2, 16.3_
  - [x] 9.3 수치 스케일·능력치 롤 속성 테스트 작성
    - **Property 28: 무기 기본공격력 스케일 — Validates: Requirements 15.3**
    - **Property 29: 타입 고유 스탯은 스케일에서 제외 — Validates: Requirements 15.5**
    - **Property 30: 랜덤 능력치 롤 불변식 — Validates: Requirements 14.4, 15.4, 16.1, 16.2, 16.4**
    - _Requirements: 14.4, 15.3, 15.4, 15.5, 16.1, 16.2, 16.4_
  - [x] 9.4 드랍 카테고리·풀·독립 인스턴스 속성 테스트 작성
    - **Property 31: 드랍은 상호 배타적 단일 롤 — Validates: Requirements 17.1, 17.7**
    - **Property 32: 몬스터 종류별 카테고리 제약 — Validates: Requirements 17.2, 17.3**
    - **Property 33: 드랍 세부 롤은 던전 풀 내에서 선택 — Validates: Requirements 17.4, 17.5, 17.6**
    - **Property 34: 등급 분포 정합성(rollGrade 유효 등급 반환) — Validates: Requirements 18.4**
    - **Property 47: 드랍마다 독립 인스턴스 — Validates: Requirements 25.4**
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5, 17.6, 17.7, 18.4, 25.4_

- [x] 10. DungeonService (스테이지·이벤트·보물) 구현
  - [x] 10.1 DungeonService 구현
    - `rollStageEvent`(5스테이지 항상 BATTLE), `pickMonster`(spawnWeight 가중), `applyRest`, `applyTrap`, `rollTreasure`
    - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5, 20.1, 20.2, 20.3, 20.5, 20.6, 20.7, 20.8_
  - [x] 10.2 스테이지 이벤트·몬스터 선택 속성 테스트 작성
    - **Property 35: 5스테이지는 항상 보스 전투 — Validates: Requirements 19.3**
    - **Property 36: 몬스터 선택은 스테이지 풀 안에서 — Validates: Requirements 19.4**
    - **Property 37: 스테이지 이벤트 분포 집합 — Validates: Requirements 20.1**
    - _Requirements: 19.3, 19.4, 20.1_
  - [x] 10.3 휴식·함정·보물상자 속성 테스트 작성
    - **Property 38: 휴식 회복 상한 — Validates: Requirements 20.2**
    - **Property 39: 함정 감소와 최소 HP 보장 — Validates: Requirements 20.3**
    - **Property 40: 보물상자 보상 종류 — Validates: Requirements 20.5, 20.7, 20.8**
    - **Property 41: 보물상자 골드 공식 — Validates: Requirements 20.6**
    - _Requirements: 20.2, 20.3, 20.5, 20.6, 20.7, 20.8_

- [x] 11. ShopService (판매·구매) 구현
  - [x] 11.1 ShopService 구현
    - `sellPrice`, `sell`(착용 중/스킬북/포션 판매 거부), `buyPotion`(골드 부족 거부)
    - _Requirements: 23.1, 23.2, 23.3, 23.4, 23.5, 24.1, 24.2, 24.3, 24.4_
  - [x] 11.2 판매·구매 속성 테스트 작성
    - **Property 44: 판매가 공식 — Validates: Requirements 23.1, 23.2**
    - **Property 45: 판매 거부 규칙 — Validates: Requirements 23.4, 23.5**
    - **Property 46: 포션 구매 — Validates: Requirements 24.2, 24.3**
    - _Requirements: 23.1, 23.2, 23.4, 23.5, 24.2, 24.3_

- [x] 12. EquipmentService (착용·스킬 장착 불변식) 구현
  - [x] 12.1 EquipmentService 구현
    - `equipWeapon`(최대 1개), `equipArmor`(부위별 1개), `attachSkillBook`(타입 불일치/중복/슬롯 규칙·소모·덮어쓰기), 던전 진입 후 변경 금지
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 13.6, 13.7, 26.1, 26.2, 26.3, 26.4, 26.5, 26.6_
  - [x] 12.2 착용·스킬 장착 속성 테스트 작성
    - **Property 48: 착용 불변식 — Validates: Requirements 26.1, 26.2, 26.3, 26.4, 26.5**
    - **Property 49: 스킬북 장착 규칙 — Validates: Requirements 13.1, 13.2, 13.3, 13.4, 13.6**
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.6, 26.1, 26.2, 26.3, 26.4, 26.5_

- [x] 13. Checkpoint - 도메인 규칙 서비스 검증
  - 모든 규칙 서비스 속성/단위 테스트가 통과하는지 확인하고, 의문이 생기면 사용자에게 질문한다.

- [x] 14. 애플리케이션 오케스트레이션 구현
  - [x] 14.1 GameSessionService 구현
    - 캐릭터 생성 및 시작 무기 지급(`[일반] 낡은 검`), 전투 보상 지급(1회), 사망/도망 페널티·진행 삭제, 마을 복귀, 체크포인트 저장/재개, 던전 포기, 상인 이벤트 상점 연동
    - 도메인 예외(`InsufficientMpException`, `InsufficientGoldException`, `IllegalEquipmentException`, `IllegalActionException`, `PlayerNotFoundException`) 정의 및 사용
    - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 4.1, 4.2, 4.3, 4.6, 4.7, 20.4, 20.9, 21.1, 21.2, 21.3, 21.4, 21.5, 21.6, 21.7, 22.1, 22.2, 22.3, 22.4, 22.5, 25.4_
  - [x] 14.2 BattleSessionService 및 전투 루프 구현
    - 전투 진행 상태(현재 HP/MP·몬스터·턴)를 HTTP 세션에 유지하고 DB 미저장, 선공 결정→행동→종료 판정 반복
    - _Requirements: 8.4, 10.1, 10.7, 21.2_
  - [x] 14.3 전투 보상 지급 속성 테스트 작성
    - **Property 1: 전투 보상 지급 정확성 — Validates: Requirements 2.1**
    - **Property 2: 비전투 이벤트는 보상을 주지 않는다 — Validates: Requirements 2.3, 20.9**
    - _Requirements: 2.1, 2.3, 20.9_
  - [x] 14.4 던전 재개·진행 유일성 속성 테스트 작성
    - **Property 42: 재개 스테이지 규칙 — Validates: Requirements 21.3**
    - **Property 43: 플레이어당 진행 중 던전 최대 1개 — Validates: Requirements 21.5**
    - _Requirements: 21.3, 21.5_
  - [x] 14.5 전투 종료 및 상태전이 통합 테스트 작성
    - **Property 18: 전투 종료 보장 — Validates: Requirements 8.4**
    - 체크포인트 저장/재개, 던전 포기, 보스 클리어 자동 복귀 상태전이 통합 검증(`@SpringBootTest`/`@DataJpaTest`)
    - _Requirements: 8.4, 21.1, 21.4, 21.6, 22.2, 22.3, 22.5_

- [x] 15. interfaces 계층(컨트롤러·뷰) 구현
  - [x] 15.1 GameController 및 뷰 모델/폼 DTO 구현
    - 마을·장비(방어구/무기 탭)·던전 선택·던전 탐색·전투·전투 승리/드랍·상점(판매/구매) 요청 처리, 스테이지 사이 `다음 스테이지로`/`포기하고 마을로`, 보스 승리 시 자동 마을 복귀
    - _Requirements: 28.1, 28.2, 28.3, 28.4, 28.5, 28.6_
  - [x] 15.2 Thymeleaf 화면 8종 템플릿 작성
    - 마을·장비 방어구 탭·장비 무기 탭·던전 선택·던전 탐색·전투·전투 승리/드랍·상점 화면(모바일 텍스트 UI)
    - _Requirements: 28.1, 28.2, 28.3, 28.4, 28.5, 28.6_
  - [x] 15.3 GlobalExceptionHandler 구현
    - `@ControllerAdvice`로 도메인/애플리케이션 예외를 사용자 친화적 화면·메시지로 변환
    - _Requirements: 11.3, 13.3, 13.4, 22.4, 23.4, 24.3, 26.6_
  - [x] 15.4 컨트롤러 슬라이스 테스트 작성
    - `@WebMvcTest`(Spring Boot 4.0) + `@MockitoBean`으로 화면 렌더링·행동 라우팅 검증
    - _Requirements: 28.1, 28.2, 28.3, 28.4, 28.6_

- [x] 16. 통합 및 마무리
  - [x] 16.1 전체 컴포넌트 와이어링 및 기동 검증
    - GameController → GameSessionService/BattleSessionService → 규칙 서비스/리포지터리/MasterDataLoader 의존성 연결, 애플리케이션 컨텍스트 기동 통합 테스트(`@SpringBootTest`)
    - _Requirements: 25.1, 25.2, 27.6_

- [x] 17. Final Checkpoint - 전체 테스트 및 빌드 검증
  - `mvn test -pl myrpg` 및 `mvn clean install -pl myrpg -am`으로 전체 테스트 통과·빌드 성공을 확인하고, 의문이 생기면 사용자에게 질문한다.

## Notes

- 본 프로젝트 규칙상 모든 Task(테스트 서브태스크 포함)는 필수이며 건너뛸 수 없다.
- 속성 기반 테스트는 jqwik(`@Property(tries = 100)`)로 작성하고, 난수 의존 로직은 `RandomSource` 스텁/고정 값 구현을 주입하여 결정론적으로 검증한다.
- jqwik 테스트에서 리포지터리가 필요하면 `Mockito.mock()`을 직접 호출한다(`@Mock`/`@ExtendWith` 미사용).
- 각 속성 테스트에는 `// Feature: myrpg-gen1-mvp, Property {번호}: {property 텍스트}` 주석을 태깅한다.
- 각 Task는 완료 전 `mvn test -pl myrpg` + `mvn clean install -pl myrpg -am`로 검증한다(`task-build-validation.md`).
- 체크포인트(13, 17)는 진행 중 검증 지점이며 코드 산출물이 없다.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "5.1"] },
    { "id": 3, "tasks": ["3.1", "4.1", "5.2"] },
    { "id": 4, "tasks": ["3.2", "4.2", "6.1"] },
    { "id": 5, "tasks": ["3.3", "4.3", "6.2", "7.1", "8.1", "9.1", "10.1", "11.1", "12.1"] },
    { "id": 6, "tasks": ["7.2", "7.3", "7.4", "7.5", "8.2", "8.3", "8.4", "8.5", "9.2", "9.3", "9.4", "10.2", "10.3", "11.2", "12.2"] },
    { "id": 7, "tasks": ["14.1", "14.2"] },
    { "id": 8, "tasks": ["14.3", "14.4", "14.5", "15.1"] },
    { "id": 9, "tasks": ["15.2", "15.3"] },
    { "id": 10, "tasks": ["15.4", "16.1"] }
  ]
}
```
