# Requirements Document: 신규 아이템 '장작' 및 나무 스폰 & 5초 채집 시스템

> **폴더 위치 가이드**: `.kiro/specs/myrpg/017-firewood-gathering/requirements.md`  
> **관련 규칙**: `rules/project/spec-conventions.md`, `rules/project/tech-stack.md`, `rules/myrpg/ui-style-guide.md`  
> **기획서 참조**: `docs/firewood-gathering-system-design.md`

---

## 1. Introduction (개요 및 배경)

### 1.1. 배경 및 목적
- **현재 상태 및 문제점**: 현재 MyRPG는 9칸 상성 턴제 전투, 던전 탐험, 장비/스킬 육성 등 전투 중심의 루프는 완비되어 있으나, 필드와 마을에서 즐길 수 있는 **생활·채집·휴식(Fantasy Life)** 요소가 부재합니다.
- **핵심 목표**: 마비노기 감성의 생활 채집 1단계로서 **신규 재료 아이템 '장작(Firewood)'**을 도입하고, 마을/필드에서 **50% 확률로 발견되는 나무 오브젝트**, **5초 채집 연출 팝업(도끼질/프로그레스 바)**, 그리고 **50% 성공/실패 획득 메커니즘**을 구현합니다.
- **선행/후속 스펙과의 연계**: 획득한 장작은 차후 예정된 **[기획 1] 캠프파이어 & 야간 위험도 시스템**(야간 야영, 기습 방지, 바이탈 완충)의 핵심 필수 연료로 연계됩니다.

### 1.2. 이번 스펙의 범위 (In-Scope)
1. **신규 아이템 '장작' 및 재료 타입 확장**:
   - `ItemType.MATERIAL` ("material", "재료") 열거값 추가 및 `isStackable()` 확장.
   - `Item` sealed interface를 구현하는 불변 레코드 `MaterialItem` 추가.
   - `item.json`에 `firewood` (구매가 20G, 판매가 10G) 등록.
   - `ItemCatalogService` 및 `InventoryService`의 재료 스택 누적 적재 및 은행 이동 처리.
2. **나무 오브젝트 50% 확률 스폰 및 상태 관리**:
   - 던전 제외 마을(`town`) 및 자유 필드(`field`) 노드 진입 시 50% 확률 나무 스폰 판정.
   - 노드 이동 시점에만 롤 수행하여 새로고침(F5) 리롤 어뷰징 방지.
   - 나무가 스폰된 노드에서 상호작용 목록에 `🌲 나무 (장작 패기)` 버튼 노출.
   - 1회 채집 시도(성공/실패/인벤풀 무관) 완료 시 해당 노드의 나무 오브젝트 소멸.
3. **스태미나 소모 및 도구 조건**:
   - 채집 1회당 **스태미나 5 SP** 소모.
   - 맨손 및 모든 무기 장착 상태에서 채집 가능.
   - 스태미나 5 미만 시 즉시 `"스태미나가 부족합니다 (필요: 5 SP)"` 토스트 안내 및 채집 모달 진입 차단.
4. **5초 채집 연출 팝업 & 결과 판정**:
   - 채집 모달 진입 시 화면 조작 잠금 + 도끼질(`🪓`) 펄스 애니메이션 + 5초 유동 프로그레스 바(0% → 100%).
   - 5초 경과 시 서버 판정 요청 (`POST /gathering/woodcut`).
   - 50% 확률 성공 시 `장작 +1` 지급, 실패 시 아이템 없음.
   - 인벤토리 30칸 풀 상태에서 기존 장작이 없을 때: 몬스터 드랍과 동일하게 바닥 버림 처리 및 로그 안내.
   - 모달 내부에서 결과(성공 `🪵` / 실패 `💨`)를 약 1초간 표시 후 모달 자동 닫힘 및 상단바/로그/상호작용 실시간 갱신.

### 1.3. 제외 및 이연 범위 (Out-of-Scope / Deferred)
- **캠프파이어 설치 및 야영 기능**: 장작을 소모하여 불을 피우는 시스템은 후속 스펙(`018-campfire-system`)에서 구현.
- **채집 전용 도구(도끼 등) 내구도 소모**: 본 스펙에서는 맨손/모든 무기에서 5 SP 소모만으로 채집 가능하며, 도구 내구도/도구 전용 슬롯은 추후 확장.
- **생활 스킬 수련치**: 채집 성공에 따른 목공/채집 스킬 랭크업 연계는 추후 스킬 확장 시 연동.

---

## 2. Glossary (용어 사전)

### 2.1. 기존 재사용 용어
- **`myrpg`**: `com.myapps.web.myrpg` 패키지의 Spring Boot 4.0 모듈.
- **`Item`**: 아이템 카탈로그의 공통 계약을 정의하는 봉인 인터페이스(`sealed interface Item`).
- **`ItemType`**: 아이템 유형 열거형 (`POTION`, `WEAPON`, `ARMOR`).
- **`OwnedItem`**: 캐릭터가 소유한 인벤토리/은행 아이템 JPA 엔티티.
- **`StorageKind`**: 저장소 구분 열거형 (`INVENTORY`, `BANK`).
- **`MapNode`**: 맵 노드 레코드 (타입: `town`, `field`, `dungeon`).
- **`PlayScreenView`**: 메인 플레이 화면 조립용 뷰 DTO.
- **`InteractionItem`**: 화면 중앙 3단 영역에 표시되는 상호작용 버튼 DTO.

### 2.2. 본 스펙 신규 용어 (`Pascal_Snake_Case`)
- **`MaterialItem`**: `Item` sealed interface를 구현하는 신규 재료 아이템 불변 레코드.
- **`ItemType.MATERIAL`**: 소비/재료성 스택 아이템을 분류하기 위한 신규 열거형 상수 (`"material"`, `"재료"`).
- **`GatheringService`**: 노드별 나무 스폰 상태 관리, 스태미나 차감, 50% 성공률 채집 판정을 전담하는 애플리케이션 서비스.
- **`WoodcutResult`**: 장작 채집 시도 결과(성공 여부, 획득 아이템, 결과 메시지, 갱신된 플레이 뷰)를 담은 불변 DTO Record.
- **`GatheringModal`**: 5초간 도끼질 애니메이션 및 앤틱 골드 프로그레스 바를 렌더링하고 결과를 표시하는 프론트엔드 모달 컴포넌트.

---

## 3. Requirements (기능 요구사항)

### Requirement 1: 신규 아이템 '장작' 및 재료 카탈로그 시스템

**User Story:**  
플레이어로서, 나무에서 채집한 장작을 인벤토리에 보관하고 수량을 누적하고 싶다.  
그래야 가방 슬롯을 낭비하지 않고 장작을 모아 상점에 판매하거나 향후 캠프파이어에 활용할 수 있다.

#### Acceptance Criteria

1. **THE** `ItemType` **SHALL** `MATERIAL("material", "재료")` 상수를 제공하고, `isStackable()` 호출 시 `POTION`과 `MATERIAL`에 대해 `true`를 반환한다.
2. **THE** `MaterialItem` **SHALL** `Item` 봉인 인터페이스를 구현하며 `id`, `name`, `buyPrice` 필드를 갖는 불변 레코드여야 한다.
3. **WHEN** `ItemCatalogService`가 기동할 때, **THE** `ItemCatalogService` **SHALL** `classpath:data/item.json`에서 `type="material"`인 아이템을 `MaterialItem`으로 올바르게 파싱하여 카탈로그에 등록한다.
4. **WHEN** `InventoryService.acquireItem` 또는 `acquire`가 `MATERIAL` 타입 아이템을 처리할 때, **THE** `InventoryService` **SHALL** 인벤토리에 이미 동일한 `itemId`의 `OwnedItem`이 존재하면 새로운 슬롯을 생성하지 않고 기존 행의 `quantity`를 증가시킨다.
5. **WHEN** `MATERIAL` 아이템을 인벤토리와 은행 간 이동(`moveToBank`, `moveToInventory`)할 때, **THE** `InventoryService` **SHALL** `POTION`과 동일하게 대상 저장소의 동일 `itemId` 행에 수량을 합산(스택 누적)한다.

---

### Requirement 2: 마을/필드 50% 나무 스폰 및 상태 관리

**User Story:**  
플레이어로서, 마을이나 필드를 돌아다닐 때 주변에서 자연스럽게 나무를 발견하고 싶다.  
그래야 채집 상호작용을 통해 장작을 패는 생활 활동을 시작할 수 있다.

#### Acceptance Criteria

1. **WHEN** 플레이어가 `POST /move`를 통해 노드를 이동할 때, **THE** `GatheringService` **SHALL** 이동 대상 노드의 타입이 `town` 또는 `field`인 경우 50% 확률(`Random < 0.50`)로 해당 노드에 나무를 스폰한다.
2. **IF** 이동 대상 노드의 타입이 `dungeon`이거나 던전 내부 방인 경우, **THEN THE** `GatheringService` **SHALL** 나무를 스폰하지 않는다.
3. **WHERE** 브라우저가 새로고침(F5)되거나 노드 이동 없이 단순 화면 갱신이 일어나는 경우, **THE** `GatheringService` **SHALL** 기존에 판정된 현재 노드의 나무 스폰 상태를 그대로 유지하여 리롤 어뷰징을 방지한다.
4. **IF** 현재 노드에 나무가 스폰되어 있는 경우, **THEN THE** `NodeViewAssembler` **SHALL** 상호작용 목록(`interactions`)에 `InteractionItem("gather-wood", "🌲 나무 (장작 패기)", false, "gathering", "wood")` 버튼을 추가하여 렌더링한다.
5. **WHEN** 플레이어가 해당 노드에서 1회 장작 채집 시도(성공/실패/인벤풀 무관)를 완료하면, **THE** `GatheringService` **SHALL** 해당 노드의 나무 오브젝트를 즉시 소멸시키고, 이후 화면 갱신 시 `🌲 나무 (장작 패기)` 버튼을 제거한다.

---

### Requirement 3: 5초 채집 연출 및 50% 성공/실패 판정

**User Story:**  
플레이어로서, 나무 상호작용 버튼을 눌렀을 때 5초간 도끼질을 하는 생동감 있는 연출을 보고 일정 확률로 장작을 얻고 싶다.  
그래야 실제 게임 속에서 나무를 패는 듯한 손맛과 긴장감을 느낄 수 있다.

#### Acceptance Criteria

1. **WHEN** 플레이어가 `🌲 나무 (장작 패기)` 버튼을 클릭하면, **THE** 클라이언트는 캐릭터의 현재 스태미나가 5 이상인지 검사한다.
2. **IF** 현재 스태미나가 5 미만인 경우, **THEN THE** 클라이언트는 채집 모달을 열지 않고 즉시 `"스태미나가 부족합니다 (필요: 5 SP)"` 화면 중앙 토스트 알림을 띄우고 동작을 중단한다.
3. **IF** 현재 스태미나가 5 이상인 경우, **THEN THE** 클라이언트는 5초 채집 전용 모달(`GatheringModal`)을 오픈하고 전체 화면 터치/조작을 잠근 후 5초 동안 유동 프로그레스 바(0% → 100%)와 도끼질 펄스 애니메이션을 재생한다.
4. **WHEN** 5초 카운트다운이 완료되면, **THE** 클라이언트는 `POST /gathering/woodcut`을 비동기 호출한다.
5. **WHEN** `POST /gathering/woodcut`이 호출되면, **THE** `GatheringService` **SHALL** 다음 순서로 트랜잭션을 실행한다:
   - 캐릭터의 스태미나 5 SP 차감.
   - 현재 노드의 나무 오브젝트 소멸 처리.
   - 50% 확률(`Random < 0.50`)로 성공/실패 판정.
6. **IF** 채집이 성공하고 인벤토리에 빈 공간이 있거나 기존 장작 스택이 존재하는 경우, **THEN THE** `GatheringService` **SHALL** 인벤토리에 `firewood` 1개를 지급하고, 액션 로그에 `[채집] 🪵 단단한 장작을 1개 얻었습니다!`를 기록한다.
7. **IF** 채집이 성공했으나 인벤토리에 기존 장작이 없고 30칸이 꽉 찬 경우, **THEN THE** `GatheringService` **SHALL** 아이템을 버림 처리하고 액션 로그에 `[채집] 장작 획득 실패! (가방이 가득 찼습니다)`를 기록한다.
8. **IF** 채집이 실패한 경우, **THEN THE** `GatheringService` **SHALL** 아이템을 지급하지 않고 액션 로그에 `[채집] 💨 헛도끼질을 하여 장작을 얻지 못했습니다.`를 기록한다.
9. **WHEN** 서버로부터 채집 결과 DTO(`WoodcutResult`)를 응답받으면, **THE** 클라이언트는 모달 내부에서 약 1초간 결과 메시지(성공 `🪵` / 실패 `💨`)를 노출한 뒤 모달을 자동으로 닫고, 상단바(SP 수치), 하단 행동 로그, 센터 상호작용 목록을 실시간 동기화한다.

---

## 4. Non-Functional & Quality Requirements (비기능 및 품질 요구사항)

1. **UI/UX 디자인 가이드라인 준수 (`rules/myrpg/ui-style-guide.md`)**:
   - 모바일 세로모드(360~480px) 뷰포트에 100% 최적화 (`max-width: 480px`, `100dvh`).
   - 앤틱 골드 테두리, 다크 그라디언트 패널 배경, CSS `:root` 유동 토큰(`clamp()`) 사용.
   - 데스크톱 마우스 호버 의존 없이 100% 원터치 터치 인터랙션 보장.
2. **5대 품질 가드레일 (Task 완료 필수 기준)**:
   - **Spotless**: Java 포맷팅 자동 교정 (`mvn spotless:apply`).
   - **Error Prone**: 정적 결함 컴파일 타임 차단 (컴파일 경고 0건).
   - **ArchUnit**: DDD 4계층(`interfaces` → `application` → `domain`) 아키텍처 규칙 준수.
   - **JaCoCo**: 신규 및 변경 코드 대상 테스트 라인 커버리지 80% 이상 달성.
   - **PMD & CPD**: 복잡도, 안티패턴 및 중복 코드 0건.
3. **CodeGraph 동기화**:
   - 코드베이스 변경 후 `codegraph sync` 필수 수행.
