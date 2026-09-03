# Requirements Document: InventoryService God Class 분해 (Inventory Service Decomposition)

> **폴더 위치**: `.kiro/specs/myrpg/021-inventory-service-decomposition/requirements.md`
> **관련 규칙**: `rules/project/spec-conventions.md`, `rules/project/tech-stack.md`, `rules/coding/code-style.md`
> **분석 출처**: `docs/myrpg-refactoring-candidates.md` (🔴 2순위)

---

## 1. Introduction (개요 및 배경)

### 1.1. 배경 및 목적

- **현재 상태 및 문제점**:
  `InventoryService`(`application/service/InventoryService.java`)는 약 **1,708줄**의 모듈 내 최대 클래스이며,
  생성자로 **의존성 10개**(`OwnedItemRepository`, `ItemCatalogService`, `CharacterProgressRepository`,
  `StatProgression`, `ActionLog`, `SkillCatalogService`, `CharacterSkillRepository`, `SkillRankupBonus`,
  `GameMessageService`, `GameProperties`)를 받는다. 하나의 클래스가 서로 다른 관심사를 모두 처리하는
  **God Class(SRP 위반)** 상태다. 현재 한 클래스가 담당하는 책임은 다음과 같다.
  - 장비 착용/해제/스마트 장착/양손무기·방패 자동 처리/무기 세트(I·II) 스왑/착용 후보 조회
  - 포션 사용(HP/MP/Stamina 회복 및 수량 감소)
  - 은행 ↔ 인벤토리 아이템 이동 및 용량(capacity) 관리
  - 인벤토리/은행/장비 뷰(View) 조립 및 아이템 상세(describe) 문자열 생성
  - 기본 아이템 시딩(seed) 및 드랍/구매 아이템 획득(acquire)
  - 장착 장비 보너스(STAT/VITAL) 합산
  - 내구도 감소 및 파손 시 자동 장착 해제
  - 전투 스킬 버튼(슬롯) 구성 및 착용 무기 재능(talent) 해석

- **중복 코드 문제**:
  카탈로그 조회 후 장비 판별(`itemCatalogService.byId(...).get() instanceof EquipmentItem`) 블록,
  장착 아이템 조회(`findEquippedInventoryItems`)와 슬롯 맵 구성, 캐릭터 안전 조회
  (`characterId != null ? ... : 1L` 폴백), 용량 검사(`checkCapacity`/`hasCapacity`/`countStorage`/
  `isInventoryFull`), 무기 세트(1/2) 슬롯 처리가 **여러 메서드에 반복(CPD 중복)** 되어 있다.

- **핵심 목표**:
  God Class를 **응집도 기준으로 협력 객체(collaborator)로 분해**하여 SRP를 회복하고, 중복 조회/판별
  로직을 단일 지점으로 통합하여 CPD 중복과 인지 복잡도를 낮춘다. 이를 통해 신규 아이템/장비/보관
  기능 추가 시 관련 협력 객체만 열어 확장(OCP)할 수 있게 한다.

- **최우선 제약 (사용자 확정)**:
  **기존 로직의 동작을 1%도 바꾸지 않는 순수 리팩토링(behavior-preserving refactoring)** 이다.
  게임 결과·표시·로그·예외·트랜잭션 경계가 리팩토링 전후 완전히 동일해야 한다.

### 1.2. 이번 스펙의 범위 (In-Scope)

1. **`InventoryService` 파사드 유지**: `InventoryService`의 **모든 public 메서드 시그니처와 두 생성자
   (9-arg Spring 주입용 + 7-arg 하위호환용)를 그대로 보존**한다. 내부 구현만 협력 객체로 위임한다.
2. **책임 분해**: 장비 착용/보관 이동/획득·시딩/뷰 조립/보너스·내구도/포션 사용/전투 스킬 로딩 등의
   관심사를 응집도 기준의 협력 객체로 추출한다. (구체 분해 단위는 design.md에서 확정)
3. **중복 조회/판별 로직 통합**: 카탈로그→장비 판별, 장착 아이템 조회/슬롯 맵, 캐릭터 폴백 조회,
   용량 검사, 무기 세트 슬롯 처리 등 반복 블록을 공유 지원 컴포넌트로 단일화한다.
4. **동작 보존(behavior-preserving)**: 리팩토링 전후 게임 플레이 결과(장착 규칙/보관 이동/획득/포션
   회복/보너스 합산/내구도 자동 해제/전투 스킬 버튼/뷰 렌더링/예외/로그 메시지)가 완전히 동일함을
   기존 회귀 테스트 및 필요 시 characterization 테스트로 보장한다.

### 1.3. 제외 및 이연 범위 (Out-of-Scope / Deferred)

- **아이템 서브타입 다형화**: `EquipmentItem`/`PotionItem`/`MaterialItem` 자체에 다형 메서드를 추가하여
  `instanceof`를 제거하는 도메인 다형화는 본 스펙 범위가 아니다(별도 스펙 후보). 본 스펙은 **분기 로직의
  위치 이동(추출)과 중복 통합**에 한정하며, 판별 방식(`instanceof`/`pattern switch`) 자체 변경은
  동작 보존 범위 내에서만 허용한다.
- **`BattleService` God Class 분리(3순위)**: 본 스펙과 독립적으로 진행한다.
- **게임 밸런스/수치 변경**: 용량(maxSlots), 포션 회복량, 기본 지급 구성, 내구도, 보너스 계산식 등
  모든 수치·규칙은 불변이다.
- **신규 기능 추가**: 새 아이템 종류, 새 보관 위치, 새 장비 슬롯 등 기능 추가는 하지 않는다.
- **DB 스키마/JSON 카탈로그/DTO 필드 변경**: 영속 스키마와 데이터·DTO 구조는 변경하지 않는다.
- **컨트롤러/타 서비스 호출부 변경**: 파사드 API를 보존하므로 `InventoryController`,
  `EquipmentController`, `BankController`, `ShopService`, `DungeonService`, `GatheringService`,
  `BattleService`, `AuthService`, `CharacterService`, `PlayScreenViewHelper`, `HealController`,
  `RepairController` 등 **호출부는 수정하지 않는다.**

---

## 2. Glossary (용어 사전)

### 2.1. 기존 재사용 용어

- **InventoryService**: 인벤토리 핵심 비즈니스 로직 파사드. 본 스펙의 분해 대상.
- **OwnedItem**: 캐릭터가 보유한 아이템 인스턴스(수량·저장위치·장착여부·내구도)를 담는 엔티티.
- **EquipmentItem / PotionItem / MaterialItem**: `Item` sealed 계층의 장비/포션/재료 서브타입.
- **EquipSlot / EquipmentKind**: 장비 슬롯(MAIN_HAND, OFF_HAND, HEAD 등)과 장비 종류(양손검, 방패 등).
- **StorageKind**: 아이템 보관 위치 열거형(`INVENTORY`, `BANK`).
- **CharacterProgress**: 캐릭터 진행상황 엔티티. 무기 세트(weapon1/2 main/off) 및 활성 세트를 보유.
- **EquippedBonusResult**: 장착 장비의 STAT(`Stats`)·VITAL(`VitalMax`) 보너스 합산 결과 DTO.
- **InventoryView / BankView / EquipmentView / OwnedItemView / EquipmentSlotView**: 팝업 뷰 조립 DTO.
- **BattleSkillButton**: 전투 스킬 슬롯(0~9) 버튼 뷰 DTO.
- **DropResult / DroppedItem**: 전투/던전 드랍 결과(골드 + 아이템 목록) DTO.

### 2.2. 본 스펙 신규 용어 (`Pascal_Snake_Case`)

- **Inventory_Facade**: 기존 public API와 두 생성자를 그대로 유지하면서 내부 처리를 협력 객체로 위임하는
  `InventoryService`의 역할. 외부 계약(호출부/테스트)을 100% 보존한다.
- **Inventory_Collaborator**: 파사드가 소유(구성)하고 위임하는, 단일 책임의 협력 객체. Spring 빈이 아닌
  파사드-구성(plain) 객체로 두어 기존 생성자·주입 계약을 바꾸지 않는다. (근거는 design.md §1.1)
- **Inventory_Query_Support**: 여러 협력 객체가 공통으로 사용하는 조회/판별 지원 컴포넌트
  (카탈로그→장비 해석, 장착 아이템/슬롯 맵 조회, 캐릭터 폴백 조회, 용량 산정 등). 중복 통합의 단일 지점.
- **Behavior_Preserving_Refactoring**: 외부 관측 가능한 동작(게임 결과·로그·표시·예외·트랜잭션)을 바꾸지
  않고 내부 구조만 변경하는 리팩토링.

---

## 3. Requirements (기능 요구사항)

> **EARS 키워드 가이드**: WHEN(이벤트), IF...THEN(원치 않는 상태 분기),
> WHILE(지속 상태), WHERE(선택 기능). 모든 문장은 `THE {주체} SHALL {동작}` 형태.

### Requirement 1: 파사드 계약 보존 (외부 호환성)

**User Story:**
개발자로서, `InventoryService`를 분해하되 그것을 사용하는 모든 컨트롤러·서비스·테스트가 한 줄도
바뀌지 않기를 원한다. 그래야 리팩토링이 외부에 무해하고 회귀 위험이 최소화된다.

#### Acceptance Criteria

1. **THE** `InventoryService` **SHALL** 리팩토링 이전의 모든 public 메서드 시그니처(이름/파라미터/반환
   타입/오버로드)를 동일하게 유지한다. 대상 메서드는 최소한 다음을 포함한다: `equip`, `unequip`,
   `smartEquip`, `swapWeapon`, `findEquippableForSlot`, `usePotion`, `moveToBank`, `moveToInventory`,
   `equippedBonus`(+`Long`), `buildInventoryView`(+오버로드), `buildBankView`(+오버로드),
   `buildEquipmentView`(+오버로드), `describe`, `acquire`, `acquireItem`(+오버로드), `seedDefault`(+오버로드),
   `combatSkills`, `reduceDurabilityAndAutoUnequip`, `equippedWeaponTalent`(+`Long`), `isBowEquipped`(+`Long`).
2. **THE** `InventoryService` **SHALL** 기존 두 생성자(9-arg `@Autowired` 없는 Spring 주입 생성자와
   7-arg 하위호환 생성자)의 파라미터 목록을 동일하게 유지하여, 이를 직접 호출하는 20여 개 테스트와
   Spring 주입부가 변경 없이 컴파일·동작하도록 한다.
3. **WHERE** 메서드가 기존에 `@Transactional`이었다면, **THE** 시스템 **SHALL** 동일한 트랜잭션 경계를
   유지한다(파사드 진입점 기준 단일 트랜잭션 보존).
4. **IF** 기존에 특정 입력에서 예외(`EquipConflictException`, `InventoryFullException`,
   `IllegalStateException`)를 던졌다면, **THEN THE** 시스템 **SHALL** 동일한 예외 타입과 메시지를 동일한
   조건에서 던진다.

---

### Requirement 2: 책임 분해 (SRP 회복)

**User Story:**
개발자로서, 하나의 거대한 서비스 대신 응집도 높은 협력 객체들이 각자의 책임을 담당하게 하고 싶다.
그래야 변경 파급이 국소화되고 각 관심사를 독립적으로 이해·테스트할 수 있다.

#### Acceptance Criteria

1. **THE** 시스템 **SHALL** `InventoryService`의 책임을 응집도 기준으로 분리하여, 아래 관심사가 각각
   전담 협력 객체로 추출되도록 한다(협력 객체 최종 목록·이름은 design.md에서 확정):
   장비 착용/해제/스왑/후보 조회, 보관 이동/용량, 획득/시딩, 뷰 조립/상세, 보너스 합산/내구도,
   포션 사용, 전투 스킬 로딩/무기 재능 해석.
2. **THE** 각 협력 객체 **SHALL** 하나의 명확한 책임만 담당하며, 파사드는 해당 책임에 대한 호출을
   대응 협력 객체로 위임한다.
3. **THE** 추출된 협력 객체 **SHALL** DDD 4계층 규칙(ArchUnit)을 준수한다. 즉 `application` 계층에
   위치하며 `interfaces`(상위 계층)를 참조하지 않는다.
4. **THE** 시스템 **SHALL** 분해 후 `InventoryService`(파사드) 및 각 협력 객체의 클래스·메서드 크기가
   `rules/coding/code-style.md` 기준(메서드 50줄 이하 권장)에 부합하도록 정리한다.

---

### Requirement 3: 중복 코드 제거 및 확장성 (DRY / OCP)

**User Story:**
개발자로서, 카탈로그 조회·장비 판별·용량 검사·무기 세트 처리 등 여러 곳에 복사된 로직을 한 곳으로
모으고 싶다. 그래야 규칙이 한 지점에서 관리되고 신규 기능 추가 시 재사용할 수 있다.

#### Acceptance Criteria

1. **THE** 시스템 **SHALL** `itemCatalogService.byId(...)` 조회 후 `EquipmentItem`으로 좁히는 반복 블록을
   단일 지원 메서드(예: `resolveEquipment(owned) -> Optional<EquipmentItem>`)로 통합하여, 동일 로직의
   중복 정의를 제거한다.
2. **THE** 시스템 **SHALL** 장착 아이템 조회(`findEquippedInventoryItems`)와 슬롯 맵 구성
   (`resolveEquippedSlotMap`), 캐릭터 폴백 조회(`findCharacterSafe`/`loadCharacter`), 용량 산정
   (`countStorage`/`checkCapacity`/`hasCapacity`/`isInventoryFull`)을 공유 지원 컴포넌트로 단일화한다.
3. **THE** 시스템 **SHALL** 무기 세트(1/2)의 슬롯 배정·해제·다른 세트 정리 로직(`clearFromOtherWeaponSet`,
   `resolveOtherSetAssignedItemIds`, `syncCurrentWeaponSetSlots` 등)에 존재하는 중복 분기를 단일 규칙
   지점으로 통합한다.
4. **THE** 시스템 **SHALL** 리팩토링 결과로 `myrpg` 모듈의 **CPD 중복과 PMD `CognitiveComplexity`가
   리팩토링 이전 대비 감소**하도록 한다(증가시키지 않는다).
5. **WHERE** 통합된 지원 메서드가 도입되는 경우, **THE** 시스템 **SHALL** 통합 전후로 각 호출 지점의
   결과(반환값·부수효과·예외)를 동일하게 유지한다(폴백 값·null 처리·1L 기본 캐릭터 폴백 포함).

---

### Requirement 4: 동작 보존 및 회귀 안전성

**User Story:**
플레이어로서, 이 리팩토링 후에도 인벤토리·장비·은행·전투 관련 동작이 이전과 정확히 동일하기를 원한다.
그래야 눈에 띄는 변화나 데이터 손상 없이 코드 품질만 개선된다.

#### Acceptance Criteria

1. **THE** 시스템 **SHALL** 장착 규칙(슬롯 점유 충돌 검사, 같은 primarySlot 스왑, 양손무기↔방패 자동
   해제, 스마트 장착 시 무기 세트 활성 무기 갱신)을 리팩토링 이전과 동일하게 수행한다.
2. **THE** 시스템 **SHALL** 보관 이동(장착/타 세트 배정 아이템 맡기기 거부, 스택형 누적 이동, 장비 저장위치
   전환, 용량 초과 거부)과 획득/시딩(포션 스택 누적, 장비 개별 저장, 용량 초과 시 소실+실패 로그,
   기본 지급 구성 및 기본 장착 세트)을 리팩토링 이전과 동일하게 수행한다.
3. **THE** 시스템 **SHALL** 포션 사용 시 HP/MP/Stamina를 각각 `min(current + heal, max)`로 회복하고
   (장비/스킬 VITAL 보너스 포함 최대치 산정) 수량 1 감소 및 0 도달 시 삭제하는 동작을 보존한다.
4. **THE** 시스템 **SHALL** 장착 보너스 합산(STAT/VITAL 분기), 내구도 감소 및 0 도달 시 자동 장착 해제
   와 로그 기록, 전투 스킬 버튼 10슬롯 구성(활성/비활성 사유, 궁극기 쿨타임·ready, 아이콘)과 착용 무기
   재능/활 장착 판정을 리팩토링 이전과 동일하게 산출한다.
5. **THE** 시스템 **SHALL** 인벤토리/은행/장비 뷰 및 아이템 상세(describe) 문자열(장비 종류·보너스 표기·
   양손무기 안내·내구도 문구·포맷)과 정렬 순서를 리팩토링 이전과 바이트 단위로 동일하게 렌더링한다.
6. **THE** 시스템 **SHALL** 기존 인벤토리·장비·은행·전투 관련 단위/프로퍼티(jqwik)/슬라이스 테스트를
   수정 없이 모두 통과한다.
7. **WHERE** 분해 대상 로직 중 현행 회귀 테스트로 커버되지 않는 지점이 있는 경우, **THE** 시스템 **SHALL**
   코드 이동에 **착수하기 전에** 해당 지점의 현행 동작을 고정하는 characterization(특성화) 테스트를 먼저
   추가하여 동작 보존 안전망을 확보한다.

---

## 4. Non-Functional & Quality Requirements (비기능 및 품질 요구사항)

1. **5대 품질 가드레일 (Task 완료 필수 기준)**:
   - **Spotless**: Java 포맷팅 자동 교정 (`mvn spotless:apply`).
   - **Error Prone**: 정적 결함 컴파일 타임 차단 (경고 0건).
   - **ArchUnit**: DDD 4계층(`interfaces` → `application` → `domain`) 규칙 준수. 추출된 협력 객체는
     `application` 계층에 위치하며 상위 계층을 참조하지 않는다.
   - **JaCoCo**: 신규/변경 코드 대상 라인 80%+, 브랜치 70%+ 커버리지.
   - **PMD & CPD**: 인지 복잡도 및 중복 코드가 리팩토링 이전 대비 **감소**하는 방향이어야 한다.
     가능하면 기존 `@SuppressWarnings("PMD.CyclomaticComplexity")`를 제거하거나 축소한다.
2. **동작 보존 검증**:
   - 리팩토링이므로 밸런스 스크립트(`tools/balance/`) 수치·게임 규칙은 불변. 기존 회귀 테스트 전수
     통과가 1차 기준이다.
   - 현재 미커버된 분해 지점(Req 4.7)은 착수 전 characterization 테스트로 현행 동작을 고정한 뒤 진행한다.
3. **CodeGraph 동기화**:
   - 코드 변경 후 `codegraph sync` 필수 수행.
4. **불변성·코딩 규칙 (`rules/coding/code-style.md`)**:
   - Lombok 금지, `@Autowired` 금지(순수 생성자 주입), 메서드 파라미터·비재할당 지역변수 `final`,
     `var` 금지, public 클래스/메서드 JavaDoc 필수, 미사용 import/변수 제거.
