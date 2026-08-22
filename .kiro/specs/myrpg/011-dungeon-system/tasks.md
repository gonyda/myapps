# Implementation Plan: 알비 던전 시스템 (Dungeon System)

## Overview

본 작업 명세(011)는 `myrpg` Web 모듈(`com.myapps.web.myrpg`)에 **알비 던전(`alby`) 프로시저럴 맵 생성, 전장의 안개(Fog of War), 상호작용 방 클리어 및 10% 연쇄 전투, 보스전 및 DB 영속 생명주기**를 구현한다.

`docs/dungeon_design.md`와 `requirements.md`, `design.md`를 바탕으로 DDD 4계층 구조를 따라 점진적으로 구현하며, 각 단계마다 단위 테스트 및 jqwik 프로퍼티 기반 테스트를 작성하고 5대 품질 가드레일을 준수한다.

---

## Tasks

### A. 데이터 & DTO / 도메인 모델

- [x] 1. 몬스터 카탈로그 확장 (`data/monster.json`)
  - [x] 1.1 `data/monster.json`에 알비 던전 몬스터 5종 추가
    - `spider` (거미, Lv2, HP 65, ATK 48, DEF 4, Crit 30, EXP 30, Gold 8~20, 드랍 hp_potion_30 10%)
    - `red-spider` (붉은거미, Lv3, HP 80, ATK 50, DEF 5, Crit 30, EXP 40, Gold 12~25, 드랍 hp_potion_30 15%)
    - `goblin` (고블린, Lv3, HP 70, ATK 54, DEF 3, Crit 30, EXP 42, Gold 15~30, 드랍 hp_potion_30 20%)
    - `black-spider` (검은거미, Lv4, HP 100, ATK 56, DEF 6, Crit 40, EXP 55, Gold 20~45, 드랍 hp_potion_30 25%)
    - `giant-spider` (거대거미, Lv7 보스, HP 380, ATK 72, DEF 12, Crit 70, 경감 60%, 반격 50%, EXP 350, Gold 150~300, 드랍 hp_potion_30 100% 2~3개)
    - _Requirements: 10.1, 10.2_
  - [x] 1.2 몬스터 서비스 단위 테스트
    - `MonsterServiceTest.java`[확장] — 신규 5종 몬스터 카탈로그 로드 및 필드 검증
    - _Validates: Requirements 10.1, 10.2_

- [x] 2. 던전 메타데이터 작성 및 DTO / Repository 구현
  - [x] 2.1 `data/dungeons.json` 작성
    - `alby` (알비 던전, active, 최단거리 10 고정, 총 방 20~23개, 분기확률 40%, 보스 giant-spider, 확정보상 EXP 1000/Gold 2000/포션 3개)
    - `ciar`, `rabbie` (확장용 템플릿, `implemented: false`)
    - _Requirements: 2.1, 2.2, 2.3_
  - [x] 2.2 던전 DTO 레코드 정의
    - `application/dto/DungeonSpec.java`[신규 record]
    - `application/dto/DungeonGenerationSpec.java`[신규 record]
    - `application/dto/DungeonMonsterEntry.java`[신규 record]
    - `application/dto/DungeonBossSpec.java`[신규 record]
    - `application/dto/DungeonRewardSpec.java`[신규 record]
    - _Requirements: 2.1, 2.2_
  - [x] 2.3 `DungeonSpecRepository` 구현
    - `application/service/DungeonSpecRepository.java`[신규] — `dungeons.json` 파싱, 불변 캐싱, `findById`, `findAll`
    - _Requirements: 2.1_
  - [x] 2.4 던전 스펙 로더 단위 테스트
    - `DungeonSpecRepositoryTest.java`[신규] — 알비 던전 스펙 파싱 및 유효성 검증
    - _Validates: Requirements 2.1, 2.2, 2.3_

- [x] 3. 도메인 모델 및 JPA 엔티티 구현
  - [x] 3.1 `DungeonRoomState` 및 `DungeonInstance` 구현
    - `domain/model/DungeonRoomState.java`[신규 record] — `(String roomId, boolean cleared, boolean discovered, List<String> remainingMonsters)`
    - `domain/model/DungeonInstance.java`[신규] — `MapGraph`, `roomStates`, `currentRoomId`, `bossRoomId`, `revealAdjacent`, `removeMonster`, `markCleared` 등 도메인 메서드
    - _Requirements: 3.1, 4.4, 5.2, 5.4_
  - [x] 3.2 `DungeonProgressEntity` 및 Repository 구현
    - `domain/model/DungeonProgressEntity.java`[신규 JPA Entity] — `dungeon_progress` 테이블 매핑
    - `domain/repository/DungeonProgressRepository.java`[신규 Spring Data JPA] — `findByCharacterId(Long characterId)`, `deleteByCharacterId(Long characterId)`
    - _Requirements: 9.1, 9.2, 9.3_
  - [x] 3.3 도메인 모델 단위/프로퍼티 테스트
    - `DungeonInstanceTest.java`[신규] — 방 클리어 전이, 몬스터 제거, 이웃 방 발견(revealAdjacent) 검증
    - `DungeonRoomStateTest.java`[신규] — 상태 불변 record 갱신 검증
    - _Validates: Requirements 4.4, 5.2, 5.4_

- [x] 4. 체크포인트 — A단계 테스트 통과 및 빌드 확인
  - `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인

---

### B. 프로시저럴 던전 맵 생성 엔진 & 뷰 팩토리

- [x] 5. 프로시저럴 던전 맵 생성 엔진 (`DungeonGenerator`)
  - [x] 5.1 `DungeonGenerator` 구현
    - `domain/service/DungeonGenerator.java`[신규]:
      - `(0, 0)` 시작방 생성
      - 4방향 비순환 Random Walk로 거리 10 주 경로 생성 및 종점 노드를 `bossRoomId`로 지정
      - 주 경로 노드들에서 서브 브랜치(깊이 1~3) 확장하여 총 20~23개 방 완성
      - `MapNode.links` 양방향 연결 검증 및 좌표 충돌 방지
      - 일반 방 가중치 기반 몬스터 스폰 및 보스방 거대거미 스폰
      - 시작방 및 인접 노드 `discovered` 초기화
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 4.4_
  - [x] 5.2 `DungeonGenerator` 프로퍼티 / 단위 테스트
    - `DungeonGeneratorTest.java`[신규] — 알비 던전 생성 기본 특성(보스방 최단거리 10, 총 방 20~23개, 시작방 안전) 검증
    - `DungeonGeneratorPropertyTest.java`[신규, jqwik] — **Property 1: 생성된 던전의 보스방 최단 거리 및 전체 방 개수 범위 불변**, **Property 2: 모든 통로의 양방향 연결성 및 좌표 고유성 불변**
    - _Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 6. 맵 뷰 팩토리 확장 (`MapViewFactory`)
  - [x] 6.1 `MapViewFactory` 안개(Fog of War) 및 노드 상태 렌더링 확장
    - `domain/service/MapViewFactory.java`[확장]:
      - 던전 인스턴스 전용 미니맵/전체지도 격자 셀 생성 오버로딩
      - `discovered == false` 노드 렌더링 제외 (투명 처리)
      - `discovered == true && cleared == false` $\rightarrow$ `node-dungeon-uncleared` 스타일
      - `cleared == true` $\rightarrow$ `node-dungeon-cleared` 스타일
    - _Requirements: 4.1, 4.2, 4.3, 4.6_
  - [x] 6.2 `MapViewFactory` 던전 뷰 단위 테스트
    - `MapViewFactoryDungeonTest.java`[신규] — 안개 마스킹 및 클리어/미클리어 셀 CSS 클래스 부여 검증
    - _Validates: Requirements 4.1, 4.2, 4.3, 4.6_

- [x] 7. 체크포인트 — B단계 테스트 통과 및 빌드 확인
  - `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인

---

### C. 던전 애플리케이션 서비스 & 전투 연동

- [x] 8. `DungeonService` 구현
  - [x] 8.1 `DungeonService` 핵심 기능 구현
    - `application/service/DungeonService.java`[신규]:
      - `enterDungeon(Long characterId, String dungeonId)`: 던전 생성, DB 저장, 시작방 이동
      - `leaveDungeon(Long characterId)`: DB 엔티티 삭제, 던전 입구 노드로 캐릭터 복귀
      - `moveToRoom(Long characterId, String targetRoomId)`: 백트래킹 검증(현재방 미클리어 시 전진 차단 / 후퇴 허용), 방 이동, 인접 방 발견, 보스방 인접 경고 힌트 출력, DB 갱신
      - `onMonsterDefeated(Long characterId, String monsterId)`: 잔여 몬스터 제거, 전원 처치 시 `cleared = true` 전이, DB 갱신
      - `onBossDefeated(Long characterId)`: 확정 보상(EXP 1000, Gold 2000, 포션 3개) 지급, DB 엔티티 삭제, 던전 입구 노드로 복귀
      - `getActiveDungeon(Long characterId)`: DB에서 `DungeonInstance` 역직렬화 복원
      - `handlePlayerDeath(Long characterId)`: 사망 시 DB 엔티티 삭제 및 마을 리스폰 처리
    - _Requirements: 1.2, 1.4, 4.4, 4.5, 5.2, 5.3, 5.4, 7.1, 7.2, 7.3, 8.2, 9.1, 9.2, 9.3_
  - [x] 8.2 `DungeonService` 프로퍼티 / 단위 테스트
    - `DungeonServiceTest.java`[신규] — 입장/퇴장, 백트래킹 전진 차단/후퇴 허용, 몬스터 격파 동기화, 보스 처치 보상/복귀 단위 테스트
    - `DungeonBacktrackingPropertyTest.java`[신규, jqwik] — **Property 3: 미클리어 방에서의 백트래킹 이동 규칙 불변 (클리어방 후퇴 허용 vs 미클리어방 전진 차단)**
    - `DungeonPersistencePropertyTest.java`[신규, jqwik] — **Property 4: DB 직렬화/역직렬화 후 던전 그래프 및 룸 상태 무손실 복원**
    - _Validates: Requirements 1.2, 1.4, 4.5, 5.2, 5.4, 7.1, 7.2, 8.2, 9.1, 9.2_

- [x] 9. 전투 시스템 (`BattleService` / `BattleController`) 던전 및 연쇄 전투 연동
  - [x] 9.1 `BattleService` 10% 연쇄 전투 및 보스전 분기 확장
    - `application/service/BattleService.java`[확장]:
      - 일반 방 몬스터 승리 시 10% 연쇄 전투 판정 (`Random` 주입)
      - 연쇄 전투 발동 시 추가 전투 뷰 반환
      - 보스방(`giant-spider`) 승리 시 연쇄 전투 없이 즉시 `DungeonService.onBossDefeated` 호출
      - 던전 내 전투 중 사망(`hpCurrent == 0`) 시 `DungeonService.handlePlayerDeath` 호출
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 8.2, 9.3_
  - [x] 9.2 `BattleService` 연쇄 전투 단위 테스트
    - `BattleServiceChainCombatTest.java`[신규] — 10% 연쇄 전투 발동/미발동 및 보스방 배제 검증
    - _Validates: Requirements 6.1, 6.2, 6.3, 6.4_

- [x] 10. 체크포인트 — C단계 테스트 통과 및 빌드 확인
  - `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인

---

### D. 웹 컨트롤러 & 화면 UI 뷰 조립

- [x] 11. `DungeonController` 구현
  - [x] 11.1 던전 API 엔드포인트 구현
    - `interfaces/api/DungeonController.java`[신규]:
      - `POST /dungeon/enter`: 던전 입장 요청 처리
      - `POST /dungeon/leave`: 시작방 던전 나가기 요청 처리
      - `POST /dungeon/move`: 던전 내 방 이동 요청 처리
    - _Requirements: 1.2, 1.4, 7.1_
  - [x] 11.2 `DungeonControllerTest` 구현
    - `DungeonControllerTest.java`[신규, MockMvc] — 입장, 나가기, 이동 엔드포인트 요청/응답 검증
    - _Validates: Requirements 1.2, 1.4, 7.1_

- [x] 12. 뷰 조립기 확장 (`NodeViewAssembler` & `PlayScreenViewHelper`)
  - [x] 12.1 던전 활성 상태에 따른 뷰 모델 분기
    - `interfaces/api/NodeViewAssembler.java`[확장]: 던전 인스턴스 존재 시 던전 맵/노드 뷰 조립
    - `interfaces/api/PlayScreenViewHelper.java`[확장]:
      - 던전 입구 노드: `[던전 입장]` 상호작용 버튼 조립
      - 던전 시작방: `[던전 나가기]` 상호작용 버튼 조립
      - 일반 미클리어 방: `remainingMonsters` 개별 버튼 조립
      - 보스방: `[거대거미 (BOSS Lv.7)]` 버튼 조립
    - _Requirements: 1.1, 1.3, 5.1, 5.5, 8.1_
  - [x] 12.2 뷰 조립기 단위 테스트
    - `NodeViewAssemblerDungeonTest.java`[신규] — 던전 활성 시 상호작용 버튼 및 맵 뷰 모델 조립 검증
    - _Validates: Requirements 1.1, 1.3, 5.1, 8.1_

- [x] 13. UI 템플릿, CSS 및 프론트엔드 스크립트 확장
  - [x] 13.1 HTML 템플릿 확장
    - `resources/templates/fragments/center.html`[확장]: 던전 입장/퇴장/이동 상호작용 버튼 렌더링
    - `resources/templates/play.html`[확장]: 던전 클리어 모달 프래그먼트 추가
    - _Requirements: 1.1, 1.3, 8.2_
  - [x] 13.2 CSS 스타일 추가 (`myrpg.css`)
    - `resources/static/css/myrpg.css`[확장]: `.node-dungeon-uncleared`, `.node-dungeon-cleared`, 던전 상호작용 버튼 스타일
    - _Requirements: 4.2, 4.3_
  - [x] 13.3 JS 스크립트 확장 (`myrpg.js`)
    - `resources/static/js/myrpg.js`[확장]: `enterDungeon`, `leaveDungeon`, `moveDungeonRoom`, 던전 클리어 모달 표시 로직
    - _Requirements: 1.2, 1.4, 7.1, 8.2_

- [x] 14. 최종 검증 및 가드레일 파이프라인
  - [x] 14.1 5대 품질 가드레일 통합 검증
    - `mvn -B -q spotless:apply -pl myrpg && (mvn -B clean install -pl myrpg -am > /tmp/mvn.log 2>&1 || (tail -n 30 /tmp/mvn.log && exit 1)) && tail -n 12 /tmp/mvn.log && codegraph sync`
    - Spotless, Error Prone, ArchUnit, JaCoCo 커버리지 80%, PMD & CPD 검증 통과 확인
  - [x] 14.2 `memory-bank/activeContext.md` 최종 동기화
