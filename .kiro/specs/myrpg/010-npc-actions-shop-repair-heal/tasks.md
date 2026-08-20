# Implementation Plan: NPC 행동 실기능 — 상점(구매/판매) · 수리(대장간) · 치료(힐러집) 및 인챈트 플레이스홀더

## Overview

스펙 006(`gold-item-inventory`)까지 구축한 `myrpg` Web 모듈(`com.myapps.web.myrpg`) 위에, 설계 문서의 DDD 4계층 구조를 따라 **NPC 행동 실기능(상점·수리·치료) 및 인챈트 플레이스홀더**를 점진적으로 구현한다.

구현 순서 원칙:

- **A. 도메인 모델 및 데이터 확장 (`Npc`·`NpcType`·`OwnedItem`·`item.json`·`npc.json`)** → **B. 핵심 애플리케이션 서비스 (`ShopService`·DTO)** → **C. 컨트롤러 계층 (`ShopController`·`RepairController`·`HealController`·`PlayScreenController`)** → **D. UI 템플릿 및 정적 리소스 (`shop-popup.html`·`repair-popup.html`·`center.html`·`myrpg.css`·`myrpg.js`)** 순으로 조립한다.
- 하위 계층(도메인/데이터/순수 계산) → 상위 계층(오케스트레이션/컨트롤러/UI) 순으로 쌓으며, 각 계층 구현 직후 설계의 Correctness Property(Property 1~10)를 jqwik으로 검증한다.
- **기존 산출물 확장(`Npc`·`NpcType`·`OwnedItem`·`NpcService`·`center.html`·`play.html`·`myrpg.js` 및 관련 테스트)은 호출부와 영향 테스트를 함께 갱신하여 원자적으로 완료**하고, 완료 시점에 빌드가 그린이어야 한다.
- 골드 부족·용량 초과·착용 충돌은 006의 기존 예외(`InsufficientGoldException`·`InventoryFullException`·`EquipConflictException`)와 `GlobalExceptionHandler`를 재사용한다.

> **테스트 정책(`task-build-validation.md`)**: "optional task"는 없다. 아래 모든 테스트 하위 작업은 **필수**이며 `*`를 사용하지 않는다. 각 Task는 `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인 후에만 완료 처리한다.

> **Spring Boot 4.0 / Java 25 규약**: 생성자 주입만(`@Autowired` 금지), Lombok 금지, `var` 금지, VO/DTO는 `record`, `final` 파라미터/지역변수, 커스텀 예외(`RuntimeException` 직접 금지). jqwik 프로퍼티는 `Mockito.mock()` 직접(`@Mock` 금지), `@Property(tries = 100)`. 프로퍼티 태그 주석: `Feature: 010-npc-actions-shop-repair-heal, Property {번호}: {프로퍼티 텍스트}`.

> **소스 정리(`code-style`)**: 소스 수정 Task는 완료 전 미사용 import/변수 제거·매직넘버 상수화·메서드 분리(50줄 초과 시)·불필요 주석 제거를 수행한다. 상수(`HEAL_COST = 100`, `REPAIR_SUCCESS_RATE_PERCENT = 95`, `SELL_RATIO = 0.5`, `WEIGHT = 10`, `CRITICAL_WEIGHT = 1` 등)는 `private static final` / `public static final`로 관리한다.

## Tasks

### A. 도메인 모델 및 데이터 확장

- [x] 1. Npc & NpcType 확장 + 카탈로그 데이터(item.json, npc.json)
  - [x] 1.1 Npc 레코드 필드 확장 및 NpcType actionLabels 갱신
    - `domain/model/Npc.java`[확장]: `List<String> shopItems` 컴포넌트 추가(7-인자) + 6-인자 보조 생성자(`shopItems=List.of()`) 제공으로 하위호환 유지
    - `domain/model/NpcType.java`[확장]: `MAGIC_SCHOOL`의 `actionLabels`를 `List.of("상점", "인챈트")`로 갱신
    - `application/service/NpcService.java`[확장]: `parseNpcNode`에서 `shopItems`를 optional로 읽도록 확장(`parseStringList` 재사용, 누락/null/비배열 시 `List.of()`). 기존 필수 필드 및 중복 ID 검증 유지
    - _Requirements: 2.1, 2.2, 2.3, 2.6, 12.1, 15.3_

  - [x] 1.2 item.json 신규 무기 추가 및 npc.json shopItems authoring
    - `resources/data/item.json`[확장]: `short_sword`(숏소드, weapon, one_handed_sword, STR+8, maxDurability 15, buyPrice 300), `long_sword`(롱소드, weapon, one_handed_sword, STR+12, maxDurability 15, buyPrice 700) 추가. 기존 초보자용 장비는 buyPrice 미지정(드랍 전용) 유지
    - `resources/data/npc.json`[확장]: `ferghus`(`["short_sword"]`), `neris`(`["long_sword"]`), `dilys`(`["hp_potion_30"]`), `manus`(`["hp_potion_30"]`) shopItems 지정. 그 외 NPC는 생략 또는 `[]`
    - _Requirements: 2.4, 2.5, 14.1, 14.2, 14.3, 14.4, 14.6_

  - [x] 1.3 Npc 및 데이터 프로퍼티 / 단위 테스트
    - `domain/model/NpcTest.java`[확장/신규] — 6-인자/7-인자 생성자 동작 검증
    - `domain/model/NpcTypeTest.java`[갱신] — `MAGIC_SCHOOL`의 `actionLabels` 검증
    - `NpcServiceShopItemsParsingPropertyTest.java`[신규] — **Property 2: NPC 데이터 파싱 및 shopItems 기본값 불변**
    - `NpcTypeActionLabelsPropertyTest.java`[신규/갱신] — **Property 10: NpcType actionLabels 무결성**
    - `ItemCatalogServiceTest.java`[갱신] — `short_sword`·`long_sword` 카탈로그 로드 및 검증
    - **Validates: Requirements 2.1, 2.2, 2.3, 12.1, 12.4, 14.1, 14.2, 15.3**

- [x] 2. OwnedItem repairBy 도메인 메서드 신설
  - [x] 2.1 OwnedItem.repairBy 구현
    - `domain/model/OwnedItem.java`[확장]: `repairBy(final double amount, final double max)` 메서드 신설 (`this.currentDurability = Math.min(max, this.currentDurability + amount)`)
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x] 2.2 OwnedItem.repairBy 프로퍼티 / 단위 테스트
    - `OwnedItemTest.java`[확장] — `repairBy` 기본 동작, 소수점 내구도 +1, max 캡핑 단위 검증
    - `OwnedItemRepairByPropertyTest.java`[신규] — **Property 5: 수리 도메인 repairBy 연산 및 max 상한 불변**
    - **Validates: Requirements 7.1, 7.2, 7.3**

- [x] 3. 체크포인트 — A단계 테스트 통과 및 빌드 확인
  - `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인.

---

### B. 핵심 애플리케이션 서비스 (ShopService & DTO)

- [x] 4. 상점/수리 DTO 정의
  - [x] 4.1 ShopView, ShopBuyItemView, ShopSellItemView, RepairView, RepairItemView 구현
    - `application/dto/ShopBuyItemView.java`[신규 record]: `(String id, String name, String typeLabel, long buyPrice, List<String> detailLines)`
    - `application/dto/ShopSellItemView.java`[신규 record]: `(long ownedItemId, String name, String typeLabel, int quantity, long sellValue, boolean equipped, List<String> detailLines)`
    - `application/dto/ShopView.java`[신규 record]: `(List<ShopBuyItemView> buyItems, List<ShopSellItemView> sellItems, long currentGold, String npcId)`
    - `application/dto/RepairItemView.java`[신규 record]: `(long ownedItemId, String name, String typeLabel, int currentDurabilityCeil, int maxDurability, long repairCost, boolean equipped, List<String> detailLines)`
    - `application/dto/RepairView.java`[신규 record]: `(List<RepairItemView> repairItems, long currentGold)`
    - _Requirements: 6.1, 6.3, 6.4, 6.5, 8.1, 8.4, 10.3, 10.4_

- [x] 5. ShopService 구현 (판매가 계산 · 상점 뷰 빌드 · 구매 / 판매)
  - [x] 5.1 ShopService 구현
    - `application/service/ShopService.java`[신규]:
      - 상수 정의: `SELL_RATIO = 0.5`, `WEIGHT = 10`, `CRITICAL_WEIGHT = 1`
      - `sellValueOf(OwnedItem)` / `calculateSellValue(Item)` / `weightOf(BonusTarget)`: 배타 규칙(buyPrice 존재 시 `round(buyPrice * 0.5)`, 부재 시 카탈로그 보너스 합산, CRITICAL=1 그외=10, 인스턴스보너스=0)
      - `shopBuyList(String npcId)` / `buildShopView(String npcId, long currentGold)`: NPC shopItems 필터링(buyPrice 보유 품목만), 인벤토리 아이템 판매 뷰 조립
      - `buy(CharacterProgress progress, String npcId, String itemId)`: NPC shopItems 검증, `spendGold(buyPrice)`, `inventoryService.acquireItem(itemId, 1)`, `actionLog.add(...)`
      - `sell(CharacterProgress progress, long ownedItemId)`: 장착 여부 검증(장착 중이면 `EquipConflictException`), `sellValueOf` 산출, `decreaseQuantity(1)`(0이면 저장소 삭제), `gainGold(sellValue)`, `actionLog.add(...)`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.8, 1.9, 2.7, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.1, 5.2, 5.3, 5.4, 5.5, 15.1, 15.2_

  - [x] 5.2 ShopService 프로퍼티 / 단위 테스트
    - `ShopServiceTest.java`[신규, 단위/Mockito] — 대표 판매가 검증(포션 25, 초보한손검 50, 초보활 110, 숏소드 150, 롱소드 350), 구매/판매 성공 및 거부 검증
    - `ShopServiceSellValuePropertyTest.java`[신규] — **Property 1: 판매가 계산식 배타성 및 결정성 (BuyPrice vs Catalog Bonuses)**
    - `ShopServiceBuyValidationPropertyTest.java`[신규] — **Property 3: 상점 구매 유효성 검증 및 골드/인벤토리 상태 보존**
    - `ShopServiceSellEquippedProtectionPropertyTest.java`[신규] — **Property 4: 상점 판매 시 장착 중 장비 보호 및 1개 단위 처리**
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.8, 4.1, 4.2, 4.4, 5.1, 5.2, 5.3, 5.4, 5.5, 14.5, 15.2**

- [x] 6. 체크포인트 — B단계 테스트 통과 및 빌드 확인
  - `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인.

---

### C. 컨트롤러 계층 (Shop, Repair, Heal & PlayScreen)

- [x] 7. ShopController, RepairController, HealController 및 PlayScreen 확장
  - [x] 7.1 ShopController 구현
    - `interfaces/api/ShopController.java`[신규]:
      - `GET /shop`: `@RequestParam(required = false) String npcId` 처리, `shopService.buildShopView` 호출 후 `"fragments/shop-popup :: shop-content"` 반환
      - `POST /shop/buy`: `@RequestParam String npcId, @RequestParam String itemId`, `shopService.buy` 수행, 갱신된 `"fragments/shop-popup :: shop-content"` 반환
      - `POST /shop/sell`: `@RequestParam(required = false) String npcId, @RequestParam long ownedItemId`, `shopService.sell` 수행, 갱신된 `"fragments/shop-popup :: shop-content"` 반환
    - _Requirements: 3.3, 3.4, 4.7, 5.6, 6.1, 6.7_

  - [x] 7.2 RepairController 구현
    - `interfaces/api/RepairController.java`[신규]:
      - 상수 정의: `REPAIR_SUCCESS_RATE_PERCENT = 95`
      - `Random` 주입 생성자 제공(시드 고정 테스트 가능)
      - `GET /repair`: 닳은 장비(`ceil(currentDurability) < maxDurability`) 필터링, 1포인트 수리비(`shopService.sellValueOf`) 산출, `RepairView` 모델 바인딩 후 `"fragments/repair-popup :: repair-content"` 반환
      - `POST /repair`: `@RequestParam long ownedItemId`, 대상 장비 검증(`ceil < max`), `cost = shopService.sellValueOf(ownedItem)`, `progress.spendGold(cost)`, 95% 성공 시 `ownedItem.repairBy(1.0, max)` + 성공 로그 / 실패 시 내구도 유지(비환불) + 실패 플레이버 로그, `characterService.saveTurn(progress)`, 갱신된 `"fragments/repair-popup :: repair-content"` 반환
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8_

  - [x] 7.3 HealController 구현
    - `interfaces/api/HealController.java`[신규]:
      - 상수 정의: `HEAL_COST = 100`
      - `POST /heal`: `progress.spendGold(HEAL_COST)`, `vitalMax = statProgression.vitalMaxFor(level, talent) + equippedVitalBonus`, `progress.fullRecover(vitalMax)`, `characterService.saveTurn(progress)`, `ResponseEntity.ok().build()` 반환
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.7_

  - [x] 7.4 PlayScreenController & ViewHelper talkingNpcId 바인딩
    - `interfaces/api/PlayScreenController.java`[확장]: NPC 대화 화면 렌더 시 `model.addAttribute("talkingNpcId", npc.id())` 전달
    - _Requirements: 3.1_

  - [x] 7.5 컨트롤러 및 수리/치료 프로퍼티 / 단위 테스트
    - `RepairListFilterPropertyTest.java`[신규] — **Property 6: 수리 목록 필터링 조건 일치 (ceil(current) < max)**
    - `RepairCostEquivalencePropertyTest.java`[신규] — **Property 7: 수리 비용과 1포인트 판매가 동치성**
    - `RepairExecutionPropertyTest.java`[신규] — **Property 8: 수리 시도 시 골드 소모 및 실패 비환불 불변**
    - `HealVitalMaxEquivalencePropertyTest.java`[신규] — **Property 9: 치료 후 활력치 상단바 최대치 완벽 일치**
    - `ShopControllerTest.java`[신규, `@WebMvcTest`] — `/shop`, `/shop/buy`, `/shop/sell` 프래그먼트 렌더링 및 모델 검증
    - `RepairControllerTest.java`[신규, `@WebMvcTest`] — `/repair`, `/repair` POST 성공/실패 시드 분기 및 모델 검증
    - `HealControllerTest.java`[신규, `@WebMvcTest`] — `/heal` 100골드 차감, 풀회복, 골드 부족 시 예외 핸들링 검증
    - **Validates: Requirements 3.1, 3.3, 3.4, 8.1, 8.2, 8.3, 8.5, 8.6, 9.3, 9.4, 9.5, 9.6, 11.2, 11.3, 11.4, 11.5, 11.7**

- [x] 8. 체크포인트 — C단계 테스트 통과 및 빌드 확인
  - `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인.

---

### D. UI 템플릿 및 정적 리소스 (프래그먼트, CSS, JS)

- [x] 9. HTML 프래그먼트 작성 및 center.html/play.html 연동
  - [x] 9.1 shop-popup.html 및 repair-popup.html 프래그먼트 신규 작성
    - `resources/templates/fragments/shop-popup.html`[신규]: 모바일 세로 배치(상점 물건 위 / 내 소지품 아래 / 골드 하단), `[구매]`·`[판매]` 버튼, `[장착중]` 비활성화 배지, 상세보기(🔍) 모달 연동
    - `resources/templates/fragments/repair-popup.html`[신규]: `.inventory-item` CSS 클래스 재사용, 닳은 장비 목록, `[수리]` 버튼(비용·95% 표기), 소지금 하단 바, "수리할 장비가 없습니다" 빈 목록 처리
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 8.7, 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

  - [x] 9.2 center.html 및 play.html 확장
    - `resources/templates/fragments/center.html`[확장]: NPC 행동 버튼 onclick에 `talkingNpcId` 전달 (`onclick="npcAction('${action}', '${talkingNpcId}')"`)
    - `resources/templates/play.html`[확장]: `shop-popup` 및 `repair-popup` 프래그먼트 include 삽입
    - _Requirements: 3.2, 6.1, 10.1_

- [ ] 10. CSS 스타일 및 myrpg.js NPC 행동 라우팅 구현
  - [x] 10.1 myrpg.css 상점/수리 팝업 스타일 추가
    - `resources/static/css/myrpg.css`[확장]: `.shop-popup`, `.repair-popup`, `.shop-buy-list`, `.shop-sell-list` 등 모바일 세로 배치 및 스크롤 스타일 정의
    - _Requirements: 6.2, 10.1, 10.6_

  - [x] 10.2 myrpg.js NPC 행동 라우팅, openShop, openRepair, heal, refreshTopBar 구현
    - `resources/static/js/myrpg.js`[확장]:
      - `npcAction(label, npcId)` 분기: `은행`→`openBank()`, `상점`→`openShop(npcId)`, `수리`→`openRepair()`, `치료받기`→`heal()`, `인챈트`→`alert("추후 설계 예정입니다.")`, 그 외→기존 안내
      - `openShop(npcId)` / `buyShopItem(npcId, itemId)` / `sellShopItem(npcId, ownedItemId)`: GET/POST AJAX 및 상점 팝업 DOM 스왑, 상단바 갱신
      - `openRepair()` / `repairItem(ownedItemId)`: GET/POST AJAX 및 수리 팝업 DOM 스왑, 상단바 갱신
      - `heal()`: `POST /heal` 호출, 성공 시 `refreshTopBar()` + `alert("치료되었습니다!")`, 골드 부족 시 alert
      - `refreshTopBar()` 함수 추출: 상단바 DOM 갱신 로직 단일화(`usePotion`, `heal`, `buy`, `sell`, `repair` 공통 사용)
    - _Requirements: 11.1, 11.6, 12.2, 13.1, 13.2, 13.3, 13.4, 13.5_

  - [x] 10.3 정적 리소스 및 스모크 테스트 갱신
    - `NpcContextLoadSmokeTest.java`[확장] — 신규 팝업 렌더 및 NPC 행동 스모크 검증
    - `VisualJsPreservationAndJsonLoadingIntegrationTest.java`[갱신] — `shop-popup.html`·`repair-popup.html`·`center.html`·`myrpg.js` 리소스 기대값 반영
    - _Requirements: 15.4_

- [x] 11. 체크포인트 — D단계 테스트 통과 및 빌드 확인
  - `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인.

---

### E. 최종 검증 및 회귀 확인

- [x] 12. 전체 테스트 스위트 및 통합 빌드 검증
  - `mvn test -pl myrpg` 전체 테스트 통과 확인
  - `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인
  - 기존 001~009 기능(전투, 인벤토리, 스킬, 은행 등) 무회귀 확인
  - 미사용 import/변수 제거, 매직 넘버 상수화 등 code-style 정리 확인

---

## Notes

- 각 Task는 하위 계층(도메인/데이터)에서 상위 계층(서비스/컨트롤러/UI)으로 점진적으로 쌓이며, 최종 단계(10·12)에서 모든 컴포넌트가 통합된다.
- 각 Correctness Property(Property 1~10)는 독립적인 jqwik 프로퍼티 테스트로 작성하며, 태그 주석 `Feature: 010-npc-actions-shop-repair-heal, Property {번호}: {프로퍼티 텍스트}`를 부착한다.
- Task 완료 전 반드시 `mvn test -pl myrpg` 통과와 `mvn clean install -pl myrpg -am` `BUILD SUCCESS`를 확인한다.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "2.1"] },
    { "id": 1, "tasks": ["1.3", "2.2", "4.1"] },
    { "id": 2, "tasks": ["5.1"] },
    { "id": 3, "tasks": ["5.2", "7.1", "7.2", "7.3", "7.4"] },
    { "id": 4, "tasks": ["7.5", "9.1", "9.2"] },
    { "id": 5, "tasks": ["10.1", "10.2"] },
    { "id": 6, "tasks": ["10.3", "12"] }
  ]
}
```
