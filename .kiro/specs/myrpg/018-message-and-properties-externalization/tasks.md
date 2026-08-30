# Implementation Plan: 메시지 및 게임 프로퍼티 외부화 리팩토링 (018-message-and-properties-externalization)

> **폴더 위치 가이드**: `.kiro/specs/myrpg/018-message-and-properties-externalization/tasks.md`  
> **관련 규칙**: `rules/workflow/task-build-validation.md`, `rules/workflow/git-workflow.md`

---

## Overview

본 작업 명세는 `com.myapps.web.myrpg` 모듈의 하드코딩 문자열과 게임 밸런스 상수를 `messages.properties` 및 `application-game.yml`로 외부화하기 위한 체크포인트 단위 순차 작업 목록입니다.

### 구현 순서 및 원칙
1. **Bottom-Up 점진적 전환**:  
   **A. 메시지/설정 인프라 구축** $\rightarrow$ **B. 서비스 계층 로그/프로퍼티 이관** $\rightarrow$ **C. 웹 컨트롤러/예외 연동** $\rightarrow$ **D. 프론트엔드 JS 하드코딩 제거** $\rightarrow$ **E. 5대 가드레일 통합 검증** 순으로 구현합니다.
2. **원자적 완료 및 빌드 그린**:  
   각 단계마다 수정된 서비스에 대응하는 단위 테스트를 동시 갱신하여 상시 `BUILD SUCCESS`를 유지합니다.
3. **5대 품질 가드레일 준수**:  
   Spotless $\rightarrow$ Error Prone $\rightarrow$ ArchUnit $\rightarrow$ JaCoCo(80%+) $\rightarrow$ PMD/CPD를 필수 검증합니다.

---

## Tasks

### A. 메시지 인프라 & 프로퍼티 모델 구축 (Infrastructure & Configuration Layer)

- [x] 1. `messages.properties` 리소스 파일 생성 및 공통 키 정의
  - [x] 1.1 `src/main/resources/messages.properties` 생성 (시스템, 활동 로그, 전투 로그, 장비 포맷팅, 예외 문구 작성)
  - _Requirements: 1.1, 1.3, 2.2, 3.1, 3.2_ / _Design: 4.2_

- [x] 2. `GameMessageService` 구현 및 단위/jqwik 테스트
  - [x] 2.1 `com.myapps.web.myrpg.support.GameMessageService.java` 생성 (`MessageSource` 래핑, 안전한 예외 폴백)
  - [x] 2.2 `GameMessageServiceTest.java`[신규] 단위 테스트 작성
  - [x] 2.3 `GameMessagePropertyTest.java`[신규, jqwik] — **Property 1** (유효 키 및 임의 인자 치환 불변식) 검증
  - _Requirements: 1.1, 1.4_ / _Design: 3.1, 5.1_

- [x] 3. `GameProperties` 및 `application-game.yml` 구성
  - [x] 3.1 `src/main/resources/application-game.yml` 생성 및 `application.yml`에 import 추가
  - [x] 3.2 `com.myapps.web.myrpg.config.GameProperties.java` 불변 Record 생성 및 설정 활성화
  - [x] 3.3 `GamePropertiesTest.java`[신규] 바인딩 테스트 및 `GamePropertiesPropertyTest.java`[신규, jqwik] — **Property 2** (수치 범위 불변식) 검증
  - _Requirements: 4.1, 4.2_ / _Design: 3.2, 4.1, 5.2_

- [x] 4. **체크포인트 A** — 메시지/설정 인프라 빌드 & 단위 테스트 검증
  - `mvn test -pl myrpg -Dtest="GameMessage*,GameProperties*"` 통과 확인

---

### B. 애플리케이션 서비스 계층 로그 & 프로퍼티 이관 (Application Service Layer)

- [x] 5. 활동 로그(`ActionLog`) 발생 서비스의 `GameMessageService` 연동
  - [x] 5.1 `GatheringService.java`: 범용 채집 성공/실패 로그(`log.gathering.*`) 및 `GameProperties` 채집 수치 적용
  - [x] 5.2 `ShopService.java`, `InventoryService.java`, `DungeonService.java`, `ProgressionService.java`: `log.shop.*`, `log.item.*`, `log.dungeon.*`, `log.growth.*` 이관
  - [x] 5.3 `InventoryService.describe()`: 장비 종류/내구도 문구 `describe.equip.*` 이관
  - _Requirements: 1.2, 1.3, 3.1, 4.3_ / _Design: 2.1, 2.2_

- [x] 6. 전투 시스템(`BattleService` & `BattleLogFormatter`) 템플릿 외부화
  - [x] 6.1 `BattleService.java`: 인라인 전투 로그 15건 `battle.*` 이관 및 전투 계수/확률 `GameProperties` 적용
  - [x] 6.2 `BattleLogFormatter.java`: 40여 개 문장을 ~20개 공통 템플릿(`battle.attack.*`, `battle.turn.*`, `battle.monster.*`)으로 통합 리팩토링
  - _Requirements: 2.1, 2.2, 2.3, 4.3_ / _Design: 3.3_

- [x] 7. 서비스 계층 단위 테스트 갱신 및 검증
  - [x] 7.1 `GatheringServiceTest.java`, `BattleServiceTest.java`, `InventoryServiceTest.java`, `DungeonServiceTest.java` 로그 검증부 갱신
  - _Validates: Requirements 1.2, 2.1, 2.2_

- [x] 8. **체크포인트 B** — 서비스 계층 빌드 & 단위 테스트 검증
  - `mvn test -pl myrpg -Dtest="GatheringServiceTest,BattleServiceTest,InventoryServiceTest,DungeonServiceTest"` 통과 확인

---

### C. 웹 컨트롤러 및 예외 응답 계층 연동 (Web Controller Layer)

- [x] 9. 컨트롤러 상수의 `GameProperties` 주입 및 치트 로그 이관
  - [x] 9.1 `HealController.java` (`game.town.heal-cost`), `RepairController.java` (`game.town.repair-*`) 프로퍼티 주입
  - [x] 9.2 `PlayScreenController.java`: 치트 로그(`log.cheat.*`) 및 이동 불가 로그 이관
  - [x] 9.3 유저 노출 비즈니스 예외(`EquipConflictException`, `InsufficientGoldException` 등)에 `GameMessageService` 연동
  - _Requirements: 3.2, 4.3_ / _Design: 2.1_

- [x] 10. 웹 컨트롤러 슬라이스 테스트 갱신
  - [x] 10.1 `HealControllerTest.java`, `RepairControllerTest.java`, `PlayScreenControllerTest.java` 갱신
  - _Validates: Requirements 3.2, 4.3_

- [x] 11. **체크포인트 C** — 컨트롤러 계층 빌드 & 웹 테스트 검증
  - `mvn test -pl myrpg -Dtest="*ControllerTest"` 통과 확인

---

### D. 프론트엔드 연동 및 하드코딩 제거 (Frontend Layer)

- [x] 12. Thymeleaf 템플릿에 공통 메시지 맵 연동
  - [x] 12.1 `play.html` / `layout.html`에 전역 `window.GAME_MESSAGES` 스크립트 주입 블록 구성
  - _Requirements: 5.3_ / _Design: 1.1_

- [x] 13. `myrpg.js` 하드코딩 문자열 제거 및 서버 메시지 연동
  - [x] 13.1 `myrpg.js` 내 alert/toast 문구 23건을 서버 응답(`res.message`) 및 `GAME_MESSAGES` 키 호출로 교체
  - [x] 13.2 장작 채집 모달(`finishWoodcutting`)의 하드코딩 텍스트를 서버 `result.message` 기반으로 단일화
  - _Requirements: 5.1, 5.2, 5.3_ / _Design: 1.1, 2.2_

- [x] 14. UI 및 JS 보존 테스트 검증
  - [x] 14.1 `VisualJsPreservationAndJsonLoadingIntegrationTest.java` 실행하여 프론트엔드 연동 정상성 검증
  - _Validates: Requirements 5.1, 5.2, 5.3_

- [x] 15. **체크포인트 D** — 프론트엔드 연동 빌드 & 뷰 테스트 검증
  - `mvn test -pl myrpg` 전체 테스트 통과 확인

---

### E. 전체 통합 검증 및 5대 품질 가드레일 (Integration & Quality Guardrails)

- [x] 16. 5대 품질 가드레일 전체 파이프라인 검증
  - [x] 16.1 `mvn -B -q spotless:apply -pl myrpg` — 소스 포맷팅 자동 교정
  - [x] 16.2 `mvn -B clean install -pl myrpg -am` — 컴파일, 아키텍처(ArchUnit), 커버리지(JaCoCo 80%+), PMD/CPD 전수 검증
  - _Requirements: 4.1_ / _Design: 6.2_

- [x] 17. CodeGraph 인덱스 동기화
  - [x] 17.1 `codegraph sync` 실행하여 최신 심볼 및 호출 관계 인덱스 갱신
  - _Workflow 규칙 준수_

- [x] 18. Memory Bank 갱신 (Compaction)
  - [x] 18.1 `memory-bank/activeContext.md`에 하드코딩 외부화 완료 내역 요약 및 다음 기획 단계 기록
  - _AGENTS.md 규칙 준수_
