# Implementation Plan: 골드·아이템·인벤토리·장비 시스템

## Overview

스펙 005(`skill-system`)까지 구축한 `myrpg` Web 모듈(`com.myapps.web.myrpg`) 위에, 설계 문서의 DDD 4계층 구조를 따라 골드·아이템·인벤토리·장비 시스템을 점진적으로 구현한다.

구현 순서 원칙:

- **A. 골드/은행** → **B. 아이템 카탈로그·보유·장비** → **C. UI(리스트 팝업)** 순으로 조립한다.
- 하위 계층(값 enum → 카탈로그 record/정책 → 영속 엔티티/리포지토리 → JSON → 로더 → 애플리케이션 서비스) → 기존 산출물 확장(뷰헬퍼/캐릭터생성/NpcType/예외) → 표현 계층(컨트롤러/템플릿/정적 리소스) 순으로 쌓는다.
- 각 순수 로직 구현 직후 설계의 Correctness Property를 jqwik 프로퍼티 테스트로 확인한다.
- **기존 산출물 확장(뷰헬퍼 장비 보너스 합산, 캐릭터 생성 시드, NpcType 라벨)은 호출부·영향 테스트를 함께 갱신하여 원자적으로 완료**하고, 완료 시점에 빌드가 그린이어야 한다.
- 마지막 표현 계층 배선에서 모든 컴포넌트가 통합된다(고아 코드 없음).

> **테스트 정책 안내**: 워크스페이스 스티어링(`task-build-validation.md`)에 따라 "optional task"는 없다. 아래 모든 테스트 하위 작업은 **필수**이며 `*`를 사용하지 않는다. 각 Task는 `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인 후에만 완료 처리한다.

> **Spring Boot 4.0 / Java 25 규약**: 생성자 주입만(`@Autowired` 금지), Lombok 금지, `var` 금지, VO/DTO는 `record`, `final` 파라미터/지역변수, 커스텀 예외(`RuntimeException` 직접 금지 — `InsufficientGoldException`/`ItemDataException`/`InventoryFullException`/`EquipConflictException`). 테스트는 `@MockitoBean` / `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` / `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`(+`@TestConstructor(ALL)`), Jackson 3(`tools.jackson`). jqwik 프로퍼티는 `Mockito.mock()` 직접 사용(`@Mock` 금지), `@Property(tries = 100)`. 프로퍼티 태그 주석: `Feature: 006-gold-item-inventory, Property {번호}: {프로퍼티 텍스트}`.

> **지식 보존·이연 seam(`docs/gold-item-system.md`)**: 코드에는 이연 seam(`OwnedItem.reduceDurability`/`repairToMax`, `Item.buyPrice`, 임시 골드 버튼 `/gold/*`)에 **담당 순위·제거/확정 조건을 서술형 JavaDoc**으로 남긴다(나열식 `// TODO` 금지).

## Tasks

### A. 골드 / 은행

- [x] 1. 골드 소지금 (CharacterProgress 확장)
  - [x] 1.1 InsufficientGoldException + CharacterProgress.gold 구현
    - `application/exception/InsufficientGoldException.java`(신규, `InsufficientAbilityPointsException` 선례)
    - `domain/model/CharacterProgress.java`[확장]: `gold`(long) 컬럼, `createDefault` 0 초기화, `gainGold(long)`/`spendGold(long)`(부족 시 예외, 음수 방지)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 1.2 골드 증감 프로퍼티/단위 테스트
    - `GoldGainSpendPropertyTest.java` — **Property 1: 골드 증감 불변식** — gain/spend 후 ≥0, spend 초과 시 예외·불변
    - **Validates: Requirements 1.3, 1.4, 1.5**
    - `CharacterProgressGoldTest.java` — 경계 예시(0, 정확히 보유액, 초과)
    - _Requirements: 1.1, 1.2_

- [x] 2. 은행 통합 금고 (Bank 엔티티·서비스)
  - [x] 2.1 Bank 엔티티 + 리포지토리
    - `domain/model/Bank.java`(`@Entity @Table("bank")`, `gold`, `createDefault`, `deposit`/`withdraw`(부족 시 `InsufficientGoldException`))
    - `domain/repository/BankRepository.java`(`findFirstByOrderByIdAsc`)
    - _Requirements: 2.1_

  - [x] 2.2 BankService (loadOrCreateDefault·입출금)
    - `application/service/BankService.java`: `@Transactional loadOrCreateDefault()`(없으면 gold=0 생성, `CharacterService` 선례), `deposit(ch, amount)`(spendGold→deposit), `withdraw(ch, amount)`(withdraw→gainGold). 최소 1골드/상한 없음/수수료 없음
    - _Requirements: 2.2, 2.3, 3.1, 3.2, 3.3, 3.4, 3.5_

  - [x] 2.3 은행 입출금·영속 프로퍼티/단위 테스트
    - `BankTransferPropertyTest.java` — **Property 2: 은행 입출금 총량 보존** — gold+bankGold 보존, 부족 시 예외·불변
    - **Validates: Requirements 3.1, 3.2, 3.3**
    - `BankPersistencePropertyTest.java`(`@DataJpaTest`+`@TestConstructor(ALL)`) — **Property 17(은행 부분): 영속 라운드트립** — Bank.gold 보존, loadOrCreateDefault 단일 행
    - **Validates: Requirements 2.1, 2.2**

  - [x] 2.4 골드 사망/환생 불변 프로퍼티 테스트
    - `GoldDeathRebirthInvariantPropertyTest.java` — **Property 3: 골드 사망/환생 불변** — 사망 패널티·환생 후 gold·bankGold 불변
    - **Validates: Requirements 1.6**

- [x] 3. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

### B. 아이템 카탈로그 · 보유 · 장비

- [x] 4. 아이템 분류 값 타입 (신규, 기존 빌드 무영향)
  - [x] 4.1 ItemType / EquipSlot / StorageKind enum 구현
    - `domain/model/ItemType.java`(`POTION`/`WEAPON`/`ARMOR` + `code`/`label`/`isEquipment`/`fromString`)
    - `domain/model/EquipSlot.java`(`MAIN_HAND`/`OFF_HAND`/`BODY`)
    - `domain/model/StorageKind.java`(`INVENTORY`/`BANK`)
    - _Requirements: 6.1, 6.2, 7.2_

  - [x] 4.2 EquipmentKind enum 구현
    - `domain/model/EquipmentKind.java`: 4상수 + `label`/`primarySlot`/`requiredSlots`(불변 Set)/`fromString`
    - _Requirements: 6.3_

  - [x] 4.3 값 enum 프로퍼티/단위 테스트
    - `ItemTypeClassificationPropertyTest.java` — **Property 5: 아이템 타입 분류 및 파싱** — fromString 대응·미지 empty, isEquipment
    - **Validates: Requirements 6.1**
    - `EquipmentKindSlotPropertyTest.java` — **Property 6: 장비 슬롯 정의 정합** — requiredSlots⊇primarySlot, 양손검만 2슬롯
    - **Validates: Requirements 6.3**
    - `ItemTypeTest.java` / `EquipmentKindTest.java` — 라벨·상수값 예시

- [x] 5. 카탈로그 모델 (순수 record, 기존 빌드 무영향)
  - [x] 5.1 Item sealed + record + EquipBonus 구현
    - `domain/model/Item.java`(sealed interface: `id`/`name`/`type`/`buyPrice`), `PotionItem.java`(record + `healHp`), `EquipmentItem.java`(record + `kind`/`bonuses`/`maxDurability`), `EquipBonus.java`(record `(BonusTarget, int)`)
    - `buyPrice`에 담당 순위(7순위 상점) JavaDoc 명시
    - _Requirements: 5.6, 5.7, 6.4, 10.1, 16.1_

- [x] 6. 영속 모델 (OwnedItem)
  - [x] 6.1 OwnedItem 엔티티 + 리포지토리
    - `domain/model/OwnedItem.java`: `@Entity @Table("owned_item")`, `itemId`/`quantity`/`storage`(EnumType.STRING)/`equipped`/`currentDurability`, `increaseQuantity`/`decreaseQuantity`(0 방지)/`moveTo`/`equip`/`unequip`/`reduceDurability`(0 방지, 6순위 호출 JavaDoc)/`repairToMax`(7순위 호출 JavaDoc)
    - `domain/repository/OwnedItemRepository.java`: `findByStorageOrderById`, `findByStorageAndItemId`, `countByStorage`, `findByStorageAndEquippedTrue`
    - _Requirements: 7.1, 7.2, 7.5, 7.7, 17.2, 17.4_

  - [x] 6.2 내구도 도메인 프로퍼티 테스트
    - `DurabilityPropertyTest.java` — **Property 14: 내구도 초기화·감소·수리** — reduce 0 바닥, repairToMax=max
    - **Validates: Requirements 17.2, 17.3, 17.4**

  - [x] 6.3 영속 라운드트립 프로퍼티 테스트
    - `OwnedItemPersistencePropertyTest.java`(`@DataJpaTest`+`@TestConstructor(ALL)`) — **Property 17: 영속 라운드트립** — itemId·quantity·storage·equipped·currentDurability 보존, `findBy*` 조회
    - **Validates: Requirements 7.1, 7.6**

- [x] 7. 카탈로그 리소스 + 로더 (신규)
  - [x] 7.1 item.json 카탈로그 리소스
    - `resources/data/item.json`: 5종(hp_potion_50 healHp50/buyPrice30, beginner_one_hand_sword STR5, beginner_two_hand_sword STR10, beginner_shield DEF5, beginner_armor DEF10). 장비 maxDurability 20
    - _Requirements: 5.1_

  - [x] 7.2 ItemDataException + ItemCatalogService 구현
    - `application/exception/ItemDataException.java`(`SkillDataException` 선례)
    - `application/service/ItemCatalogService.java`: `@PostConstruct init()`(classpath:data/item.json), `loadFromStream(InputStream):List<Item>`(파싱·검증 분리), `all()`, `byId(String)`. 검증: 최상위 배열/필수필드/`type`·`kind`·`bonuses.target` enum 변환/id 중복/장비 maxDurability 필수. `buyPrice` optional, `bonuses` 미기재 빈 목록. Jackson 3(`tools.jackson`)
    - _Requirements: 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9_

  - [x] 7.3 카탈로그 검증 프로퍼티/통합 테스트
    - `ItemCatalogParsingPropertyTest.java` — **Property 4: 아이템 카탈로그 검증** — 미지 type/kind/target·중복 id·필드 누락·장비 maxDurability 누락 시 `ItemDataException`, 유효 입력 불변 목록·optional 처리
    - **Validates: Requirements 5.2, 5.4, 5.5, 5.6, 5.7, 5.8**
    - `ItemCatalogLoadIntegrationTest.java` — 실제 `data/item.json` 로드: 5종·id 유일·장비 maxDurability 완비
    - _Requirements: 5.1, 5.9_

- [x] 8. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. 인벤토리 애플리케이션 서비스 (착용/사용/이동/보너스/상세/시드)
  - [x] 9.1 EquipConflictException + InventoryFullException + InventoryService 핵심
    - `application/exception/EquipConflictException.java`, `application/exception/InventoryFullException.java`
    - `application/service/InventoryService.java`: `equip`(착용 규칙: requiredSlots 충돌 검사 → 같은 primary 해제 후 착용, 충돌 시 `EquipConflictException`), `unequip`, `usePotion`(HP 회복·수량-1·0 삭제), `moveToBank`(장착 거부·용량 30·소비형 스택 누적), `moveToInventory`(용량 30), `equippedBonus()`(STAT→Stats/VITAL→VitalMax 분기), `seedDefault()`(기본 지급+장착)
    - _Requirements: 7.3, 7.4, 8.1, 8.2, 8.3, 8.4, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 9.9, 10.1, 10.2, 10.4, 11.1, 11.2, 11.3, 11.4, 15.5, 15.6, 15.7, 15.8, 18.1, 18.2, 18.3, 18.4_

  - [x] 9.2 착용 규칙 프로퍼티 테스트
    - `EquipConflictPropertyTest.java` — **Property 7: 장비 착용 충돌/스왑 규칙** — primary 상이 점유 시 거부·불변, 같은 primary 스왑, 한손검+방패 병용/양손검↔방패 배타
    - **Validates: Requirements 9.1~9.7**
    - `EquipSlotUniquenessPropertyTest.java` — **Property 8: 슬롯 점유 유일성 불변식** — 슬롯당 장착 ≤1, 장착은 INVENTORY만
    - **Validates: Requirements 9.8, 9.9**

  - [x] 9.3 스택·용량·이동 프로퍼티 테스트
    - `ItemStackPropertyTest.java` — **Property 9: 스택 규칙** — 소비형 누적/장비 개별
    - **Validates: Requirements 7.3, 7.4**
    - `InventoryCapacityPropertyTest.java` — **Property 10: 저장소 용량 가드** — 신규 스택 30 초과 거부, 누적은 통과
    - **Validates: Requirements 8.1~8.4**
    - `ItemMovePropertyTest.java` — **Property 11: 맡기기/찾기 저장위치 전환** — storage 전환, 장착 맡기기 거부
    - **Validates: Requirements 15.5, 15.6, 15.7**

  - [x] 9.4 보너스 합산·포션 프로퍼티 테스트
    - `EquippedBonusPropertyTest.java` — **Property 12: 장비 보너스 합산 STAT/VITAL 분기** — 장착 INVENTORY만, STAT→Stats/VITAL→VitalMax
    - **Validates: Requirements 10.1, 10.2, 10.4**
    - `UsePotionPropertyTest.java` — **Property 13: 포션 사용 회복·수량** — hp 클램프, 수량-1·0 삭제
    - **Validates: Requirements 11.2, 11.3**

  - [x] 9.5 상세 생성·정렬 프로퍼티 테스트
    - `ItemDescribePropertyTest.java` — **Property 15: 상세 자동 생성** — 포션 회복 문구·장비 보너스/내구도·양손검 배타
    - **Validates: Requirements 12.3, 12.4, 12.5**
    - `InventorySortPropertyTest.java` — **Property 16: 인벤토리 정렬 결정성** — 획득순/이름순/타입순 결정적
    - **Validates: Requirements 14.1, 14.4**

  - [x] 9.6 기본 지급 프로퍼티/단위 테스트
    - `SeedDefaultItemsPropertyTest.java` — **Property 18: 기본 지급 결과** — 5행·장착 3종·내구도 20·STR+5/DEF+15
    - **Validates: Requirements 18.2, 18.3, 18.4, 18.5**
    - `InventoryServiceTest.java` — 착용 예시(한손검+방패 병용, 양손검+방패 거부, 스왑), usePotion 상한, describe 문구
    - _Requirements: 9.4, 9.5, 11.2, 12.4_

- [x] 10. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

### 기존 산출물 확장 (원자적, 빌드 그린 유지)

- [x] 11. 기존 산출물 확장
  - [x] 11.1 CharacterService 기본 아이템 시드
    - `application/service/CharacterService.java`[확장]: 신규 캐릭터 생성 시 `skillService.seedDefault(id)` 옆에서 `inventoryService.seedDefault()` 호출
    - `CharacterServiceDefault*Test` 갱신(신규 캐릭터 아이템 5행·기본 장착)
    - _Requirements: 18.1_

  - [x] 11.2 PlayScreenViewHelper 장비 보너스 합산
    - `interfaces/api/PlayScreenViewHelper.java`[확장]: `buildStatLines`에 `equippedBonus().statBonus()` 합산(기존 skillBonus에 더함), 최대 바이탈에 `equippedBonus().vitalBonus()` 합산. `InventoryService` 생성자 주입
    - `PlayScreenViewHelperInfoTest` 갱신(기본 장착 STR+5/DEF+15 반영, 미장착 제외)
    - _Requirements: 10.3, 10.4, 10.5_

  - [x] 11.3 NpcType BANK 라벨 통합 + GlobalExceptionHandler 확장
    - `domain/model/NpcType.java`[확장]: BANK `actionLabels` → `["은행"]`
    - `interfaces/api/GlobalExceptionHandler.java`[확장]: `InsufficientGoldException`/`EquipConflictException`/`InventoryFullException` 안내(상태 불변)
    - `NpcTypeCompletenessPropertyTest`/`NpcActionButtonsPropertyTest` 갱신
    - _Requirements: 15.1, 19.1, 19.2_

- [x] 12. 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

### C. 표현 계층 배선 (리스트 팝업 · 상세 모달)

- [x] 13. 임시 골드 버튼 + 뷰 모델
  - [x] 13.1 뷰 모델 + InventoryService 뷰 조립
    - `application/dto/InventoryView.java`/`BankView.java`/`OwnedItemView.java`(설계 필드, `detailLines` 임베드)
    - `InventoryService`: `buildInventoryView`(획득순 id asc·행 조립·`describe` 임베드), `buildBankView`(좌 BANK/우 INVENTORY)
    - _Requirements: 12.6, 13.2, 15.3_

  - [x] 13.2 PlayScreenController 임시 골드 엔드포인트
    - `interfaces/api/PlayScreenController.java`[확장]: `POST /gold/gain`·`/gold/spend`(`TEST_GOLD_AMOUNT=100`, 부족 시 미차감·안내), 제거 예정 JavaDoc. `progress-response` 반환
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 14. 인벤토리 팝업 (리스트·정렬·상세)
  - [x] 14.1 InventoryController + inventory-popup.html + item-detail.html
    - `interfaces/api/InventoryController.java`: `GET /inventory`(정렬 기본 획득순), `POST /inventory/use|equip|unequip`(fragment 스왑)
    - `templates/fragments/inventory-popup.html`[신규]: 정렬 컨트롤·리스트(이름+🔍+[장착중]·타입·수량·사용/착용/해제)·하단 보유골드
    - `templates/fragments/item-detail.html`[신규]: 공용 상세 모달
    - `templates/play.html`[확장]: inventory-popup·item-detail include
    - _Requirements: 12.1, 12.2, 12.7, 13.1, 13.2, 13.3, 13.4, 13.5, 13.6, 13.7, 14.1, 14.2_

  - [x] 14.2 left-sidebar.html + myrpg.js/css (인벤토리·상세·정렬·골드 버튼)
    - `templates/fragments/left-sidebar.html`[확장]: "인벤토리" → `openInventory()`, "경험치 다운" 아래 `골드 획득`/`골드 소모` 버튼
    - `static/js/myrpg.js`[확장]: `openInventory`/`closeInventory`, `usePotion`/`equipItem`/`unequipItem`(불가 alert), `openItemDetail`/`closeItemDetail`(임베드), `sortInventory`(클라 정렬), `goldGain`/`goldSpend`
    - `static/css/myrpg.css`[확장]: 리스트 팝업·상세 모달·[장착중] 배지
    - _Requirements: 4.1, 12.1, 12.2, 12.6, 12.7, 13.1, 13.4, 14.2, 14.3, 14.4_

  - [x] 14.3 인벤토리 컨트롤러 슬라이스 테스트
    - `InventoryControllerTest.java`(`@WebMvcTest`+`@MockitoBean InventoryService`) — 목록 렌더, 사용/착용/해제 fragment 스왑, 착용 충돌 안내(`EquipConflictException`)
    - _Requirements: 13.7, 19.2_

- [x] 15. 은행 팝업 (리스트·골드·아이템 이동)
  - [x] 15.1 BankController + bank-popup.html + center.html 배선
    - `interfaces/api/BankController.java`: `GET /bank`, `POST /bank/deposit|withdraw`(트랜잭션·최소 1골드), `POST /bank/item/deposit|withdraw`(맡기기/찾기)
    - `templates/fragments/bank-popup.html`[신규]: 좌 은행/우 소지품 리스트(각 🔍)·골드 2칸·입출금 소형 모달
    - `templates/fragments/center.html`[확장]: NPC 버튼 `npcAction(this.textContent)`, `label==='은행'` → `openBank()`
    - `templates/play.html`[확장]: bank-popup include
    - `static/js/myrpg.js`/`myrpg.css`[확장]: `openBank`/`closeBank`, 입출금 모달, `depositItem`/`withdrawItem`
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.9, 3.4, 3.5_

  - [x] 15.2 은행 컨트롤러 슬라이스 테스트
    - `BankControllerTest.java`(`@WebMvcTest`+`@MockitoBean`) — 팝업 렌더, 입출금 성공·부족 안내(`InsufficientGoldException`), 맡기기 장착 거부·용량 초과(`InventoryFullException`), 찾기
    - _Requirements: 15.5, 15.6, 15.8, 19.2_

- [x] 16. 통합·스모크·로컬 세이브 초기화
  - [x] 16.1 로컬 H2 세이브 초기화
    - 로컬 세이브 파일(`myrpg/data/myrpg*`)을 삭제하여 다음 기동 시 신규 캐릭터(gold 0·기본 아이템 시드)가 생성되도록 한다. 프로덕션(`ddl-auto: create`)은 자동 초기화
    - _Requirements: 20.1, 20.2, 20.3, 20.4_

  - [x] 16.2 컨텍스트 로드 스모크 테스트
    - `GoldItemContextLoadSmokeTest.java`(`@SpringBootTest`) — 기동 및 `ItemCatalogService`/`InventoryService`/`BankService` 빈 로딩, 정보 팝업 장비 보너스 경로·인벤토리/은행 팝업 렌더 정상, 상단바 무변경(골드 미표시)
    - _Requirements: 5.1, 10.3, 20.5_

- [x] 17. 최종 체크포인트 — 모든 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- 각 Task는 이전 Task 위에 점진적으로 쌓이며, 표현 계층 배선(13~15)에서 모든 컴포넌트가 통합된다(고아 코드 없음).
- 기존 산출물 확장(11)은 `CharacterService` 시드·`PlayScreenViewHelper` 장비 보너스·`NpcType` 라벨·`GlobalExceptionHandler`로 영향이 있으므로, 호출부·영향 테스트를 함께 갱신하여 **완료 시점에 빌드 그린**을 보장한다.
- 프로퍼티 테스트는 설계의 18개 정확성 속성(jqwik, `@Property(tries=100)`)을 검증하고, 단위/슬라이스/통합 테스트가 구체 값·렌더링·컨텍스트 로딩·영속을 보완한다.
- 각 Correctness Property는 단 하나의 프로퍼티 테스트로 구현하며, 태그 주석 `Feature: 006-gold-item-inventory, Property {번호}: {프로퍼티 텍스트}`를 부착한다.
- **이연 항목**: (6순위 전투) `OwnedItem.reduceDurability(0.2)` 턴당 호출·전투 중 포션 사용 UI·장비 스탯 데미지 반영. (7순위 상점/대장간) 구매(`buyPrice`)·판매(`Sell_Value` 실제 `WEIGHT`)·`repairToMax` 수리비. (추후) 내구도 0 파손 처리·인챈트(`ENCHANT`). 임시 골드 버튼(`/gold/*`)은 실제 획득/소모 경로 도입 시 제거. 각 seam은 담당 순위·제거/확정 조건을 JavaDoc으로 명시한다.
- Task 완료 전 `mvn test -pl myrpg` 통과와 `mvn clean install -pl myrpg -am` `BUILD SUCCESS`를 확인한다.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "4.1", "5.1"] },
    { "id": 1, "tasks": ["1.2", "2.1", "4.2", "6.1"] },
    { "id": 2, "tasks": ["2.2", "4.3", "6.2", "6.3", "7.1"] },
    { "id": 3, "tasks": ["2.3", "2.4", "7.2"] },
    { "id": 4, "tasks": ["7.3", "9.1"] },
    { "id": 5, "tasks": ["9.2", "9.3", "9.4", "9.5", "9.6"] },
    { "id": 6, "tasks": ["11.1", "11.2", "11.3"] },
    { "id": 7, "tasks": ["13.1", "13.2"] },
    { "id": 8, "tasks": ["14.1", "14.2", "15.1"] },
    { "id": 9, "tasks": ["14.3", "15.2"] },
    { "id": 10, "tasks": ["16.1", "16.2"] }
  ]
}
```
