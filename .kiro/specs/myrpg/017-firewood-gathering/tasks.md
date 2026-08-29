# Implementation Plan: 신규 아이템 '장작' 및 나무 스폰 & 5초 채집 시스템

> **폴더 위치 가이드**: `.kiro/specs/myrpg/017-firewood-gathering/tasks.md`  
> **관련 규칙**: `rules/workflow/task-build-validation.md`, `rules/workflow/git-workflow.md`  
> **설계 참조**: `.kiro/specs/myrpg/017-firewood-gathering/design.md`

---

## Overview

본 작업 명세는 MyRPG 모듈(`com.myapps.web.myrpg`)에 신규 아이템 '장작(Firewood)' 및 마을/필드 50% 나무 스폰과 5초 채집 시스템을 점진적으로 구현하기 위한 체크포인트 단위 작업 목록입니다.

### 구현 순서 및 원칙
1. **Bottom-Up 계층 조립**:  
   **A. 데이터/도메인(ItemType, MaterialItem, item.json)** $\rightarrow$ **B. 애플리케이션 서비스(GatheringService, InventoryService 스택 확장)** $\rightarrow$ **C. 웹 컨트롤러(GatheringController, NodeViewAssembler 연동)** $\rightarrow$ **D. 프론트엔드(Thymeleaf 모달·CSS·JS)** $\rightarrow$ **E. 5대 가드레일 통합 검증** 순으로 구현합니다.
2. **원자적 완료 및 빌드 그린**:  
   각 단계 완료 시점에 단위 테스트 및 5대 품질 가드레일이 반드시 그린(`BUILD SUCCESS`)이어야 합니다.
3. **5대 품질 가드레일 준수**:  
   Spotless $\rightarrow$ Error Prone $\rightarrow$ ArchUnit $\rightarrow$ JaCoCo (80%+) $\rightarrow$ PMD/CPD를 필수 검증합니다.

---

## Tasks

### A. 도메인 모델 & 카탈로그 확장 (Domain Layer)

- [x] 1. 도메인 모델 및 인터페이스 확장
  - [x] 1.1 `ItemType.java`에 `MATERIAL("material", "재료")` 열거값 추가 및 `isStackable()` 메서드 구현
  - [x] 1.2 `MaterialItem.java` 불변 Record 생성 (`id`, `name`, `buyPrice`)
  - [x] 1.3 `Item.java` sealed permits 목록에 `MaterialItem` 등록
  - _Requirements: 1.1, 1.2_ / _Design: 3.3_

- [x] 2. 카탈로그 데이터 및 로더 확장
  - [x] 2.1 `src/main/resources/data/item.json`에 `firewood` (구매가 20G) 등록
  - [x] 2.2 `ItemCatalogService.java`에 `MATERIAL` 타입 파싱 로직(`parseMaterialItem`) 추가
  - _Requirements: 1.3_ / _Design: 3.2, 4.1_

- [x] 3. 도메인 단위 및 jqwik 프로퍼티 테스트 작성
  - [x] 3.1 `MaterialItemTest.java`[신규] — 불변성 및 타입 일치 단위 테스트
  - [x] 3.2 `ItemCatalogServiceMaterialTest.java`[신규] — 카탈로그 파싱 검증
  - [x] 3.3 `MaterialStackPropertyTest.java`[신규, jqwik] — **Property 2** 장작 스택 누적 불변식 검증
  - _Validates: Requirements 1.1, 1.2, 1.3_

- [x] 4. **체크포인트 A** — 도메인 & 카탈로그 계층 빌드 & 단위 테스트 검증
  - `mvn test -pl myrpg -Dtest="*Material*,*ItemCatalog*"` 통과 확인

---

### B. 애플리케이션 서비스 로직 확장 (Application Service Layer)

- [x] 5. 인벤토리 서비스 재료 아이템 스택 지원
  - [x] 5.1 `InventoryService.java`의 `acquireItem`, `moveToBank`, `moveToInventory`에서 `MATERIAL` 타입의 스택 누적 지원
  - [x] 5.2 인벤토리 용량 초과 시 버림 처리 및 로그 기록 무결성 보장
  - _Requirements: 1.4, 1.5_ / _Design: 3.2_

- [x] 6. 채집 서비스 (`GatheringService`) 구현
  - [x] 6.1 `GatheringService.java` 신규 생성 (`@Service`, `@Transactional`)
  - [x] 6.2 `rollTreeSpawn(Long characterId, String nodeId, String nodeType)`: 마을/필드 50% 확률 스폰 롤 및 세션/메모리 상태 저장
  - [x] 6.3 `isTreeAvailable(Long characterId, String nodeId)`: 현재 노드 나무 스폰 여부 조회
  - [x] 6.4 `gatherWood(CharacterProgress progress)`: 스태미나 5 SP 검증/차감, 노드 나무 소멸, 50% 성공률 판정, `InventoryService` 연동, 액션 로그 기록, `WoodcutResult` 반환
  - _Requirements: 2.1, 2.2, 2.3, 2.5, 3.5, 3.6, 3.7, 3.8_ / _Design: 3.2_

- [x] 7. 서비스 단위 및 jqwik 프로퍼티 테스트 작성
  - [x] 7.1 `GatheringServiceTest.java`[신규] — 스폰 롤, 스태미나 부족 차단, 50% 성공/실패 분기, 1회 소멸 단위 테스트
  - [x] 7.2 `GatheringStaminaPropertyTest.java`[신규, jqwik] — **Property 1** 스태미나 차감 불변식 검증
  - [x] 7.3 `GatheringSpawnPropertyTest.java`[신규, jqwik] — **Property 3, 4** 50% 스폰/던전 격리 및 1회 채집 후 나무 소멸 불변식 검증
  - _Validates: Requirements 2.1, 2.3, 2.5, 3.5, 3.6, 3.7_

- [x] 8. **체크포인트 B** — 서비스 계층 빌드 & 단위 테스트 검증
  - `mvn test -pl myrpg -Dtest="GatheringService*,*Inventory*"` 통과 확인

---

### C. 웹 컨트롤러 및 뷰 어셈블러 연동 (Web Layer)

- [x] 9. 웹 컨트롤러 엔드포인트 및 뷰 어셈블러 구현
  - [x] 9.1 `GatheringController.java` 신규 생성 (`POST /gathering/woodcut` 엔드포인트)
  - [x] 9.2 `PlayScreenController.java`의 `/move`에서 `GatheringService.rollTreeSpawn` 호출 연동
  - [x] 9.3 `NodeViewAssembler.java`에서 현재 노드에 나무가 있을 때 `InteractionItem("gather-wood", "🌲 나무 (장작 패기)", false, "gathering", "wood")` 버튼 추가
  - _Requirements: 2.4, 3.4, 3.9_ / _Design: 3.1_

- [x] 10. 웹 컨트롤러 슬라이스 테스트 작성
  - [x] 10.1 `GatheringControllerTest.java`[신규] — `MockMvc` 기반 `/gathering/woodcut` 정상 요청 및 DTO 응답 검증
  - [x] 10.2 `PlayScreenControllerGatheringTest.java`[신규] — 노드 이동 시 나무 버튼 포함 여부 검증
  - _Validates: Requirements 2.4, 3.4_

- [x] 11. **체크포인트 C** — 컨트롤러 계층 빌드 & 웹 테스트 검증
  - `mvn test -pl myrpg -Dtest="GatheringController*,PlayScreenController*"` 통과 확인

---

### D. 프론트엔드 UI/UX (Thymeleaf & JS & CSS)

- [x] 12. 템플릿 마크업 구현
  - [x] 12.1 `src/main/resources/templates/fragments/gathering-modal.html` 생성 (5초 게이지 및 도끼질 연출)
  - [x] 12.2 `src/main/resources/templates/play.html`에 `gathering-modal` 프래그먼트 인클루드
  - [x] 12.3 `src/main/resources/templates/fragments/center.html`에서 `actionType == 'gathering'` 버튼 스타일 바인딩
  - _Requirements: 3.3, 4.1_ / _Design: 3.4_

- [x] 13. CSS 스타일링 및 인터랙션 정의
  - [x] 13.1 `src/main/resources/static/css/myrpg.css`에 앤틱 골드 채집 모달, 프로그레스 바, 도끼질 애니메이션(`@keyframes axeSwing`) 스타일 추가
  - _Requirements: 4.1_ / _Design: 3.4_

- [x] 14. JavaScript 비동기 통신 및 5초 채집 타이머 구현
  - [x] 14.1 `src/main/resources/static/js/myrpg.js`의 `onInteractionClick`에 `actionType === 'gathering'` 분기 추가
  - [x] 14.2 `startWoodcutting()`: SP 검사(5 미만 시 토스트 표시), 모달 열기 및 5초 프로그레스 애니메이션, 5초 후 비동기 호출 및 1초 결과 노출 후 화면 갱신
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.9_ / _Design: 3.4_

- [x] 15. **체크포인트 D** — 프론트엔드 통합 수동/자동 빌드 검증

---

### E. 5대 품질 가드레일 통합 검증 & 동기화 (Validation & Sync)

- [x] 16. 전체 멀티모듈 통합 테스트 및 5대 가드레일 검증
  - [x] 16.1 Spotless 코드 포맷팅 자동 교정 (`mvn -B -q spotless:apply -pl myrpg`)
  - [x] 16.2 Error Prone 정적 결함 컴파일 타임 검증 (0 warnings)
  - [x] 16.3 ArchUnit 계층 아키텍처 규칙 검증
  - [x] 16.4 JaCoCo 라인 커버리지 80%+ 달성 검증
  - [x] 16.5 PMD & CPD 복잡도 및 중복 코드 검증
  - [x] 16.6 전체 테스트 100% 그린 확인 (`BUILD SUCCESS`)
- [x] 17. CodeGraph 동기화 (`codegraph sync`)
- [x] 18. `memory-bank/activeContext.md` 최신 작업 상태 갱신 (Compaction)
