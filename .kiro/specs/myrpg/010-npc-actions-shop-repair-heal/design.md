# Design Document

## Overview

본 설계는 `myrpg` Web 모듈(`com.myapps.web.myrpg`)에 **NPC 행동 실기능 — 상점(구매/판매)·수리(대장간)·치료(힐러집) 및 인챈트 플레이스홀더**를 추가한다(스펙 010). 스펙 006(`gold-item-inventory`)의 골드(`CharacterProgress.gold`·`gainGold`·`spendGold`·`InsufficientGoldException`), 아이템 카탈로그(`ItemCatalogService`·`Item`·`EquipmentItem`·`PotionItem`·`buyPrice`), 보유 아이템(`OwnedItem`·`StorageKind`·`increaseQuantity`/`decreaseQuantity`), 인벤토리 서비스(`InventoryService`·`InventoryFullException`·`EquipConflictException`), 은행 팝업 패턴(`BankController` + `bank-popup.html`) 위에서 동작하며, `docs/npc-actions-system.md`(§0 확정사항 M1~M21)가 확정한 설계를 온전히 구현으로 옮긴다.

핵심 원칙:

- **도메인 계산 일관성 (`ShopService`)**: 아이템 판매가(`sellValueOf`)는 저장되지 않고 조회 시점에 매번 계산된다. `기본가 + (인스턴스보너스 × 대상별가중치)` 공식을 적용하며, 기본가는 `buyPrice` 유무에 따라 상점템(`round(buyPrice × 0.5)`)과 드랍템(`Σ 카탈로그보너스 × 가중치`)으로 배타 적용된다. 1포인트 수리비는 이 판매가를 그대로 재사용한다.
- **NPC별 판매 목록 분리 (`Npc.shopItems`)**: `Npc` 레코드에 `List<String> shopItems` optional 필드를 추가하여, 같은 상점 버튼이라도 말 거는 NPC(`talkingNpcId`)에 따라 서로 다른 구매 목록을 제공한다(예: 티르코네일 퍼거스=숏소드, 던바튼 네리스=롱소드, 힐러=생명력30포션).
- **1포인트 · 95% 성공 수리 (`OwnedItem.repairBy`)**: 기존 `repairToMax` 대신 1포인트씩 수리하는 `repairBy(1.0, max)` 도메인 메서드를 신설한다. 수리 성공률은 95% 고정이며, 주입 `Random`으로 판정한다. 실패 시 골드 환불은 없으며 내구도 변화가 없다. 수리 목록은 내구도가 닳은 장비(`ceil(currentDurability) < maxDurability`)만 노출한다.
- **치료 (100골드 풀회복)**: 팝업 없이 `POST /heal` 단일 호출로 100골드를 소모하고, 상단바 최대치(`StatProgression.vitalMaxFor` + 장비 바이탈 보너스)를 기준으로 HP/MP/스태미나를 풀회복한다.
- **UI 일관성 및 재사용**: 상점 팝업(`shop-popup.html`)은 은행의 모바일 세로 배치 패턴(상점물건 위 / 소지품 아래 / 골드 하단)을 복제하고, 수리 팝업(`repair-popup.html`)은 인벤토리의 `.inventory-item`/`.item-info`/`.item-meta` CSS 클래스를 재사용한다.

### 이번 스펙에서 구현 vs 이연

- **구현**:
  1. 판매가 계산식(`ShopService.sellValueOf` — 기본가 배타 규칙, 대상별 가중치 `CRITICAL=1`, 그 외 `10`, `SELL_RATIO=0.5`).
  2. `Npc` 레코드 `shopItems` optional 필드 확장 및 `NpcService` 파싱/검증.
  3. 대화 중인 NPC 식별자(`talkingNpcId`) 뷰/컨트롤러 전달 및 `center.html` `npcAction(label, npcId)` 연동.
  4. 상점 컨트롤러(`ShopController`) 및 팝업(`shop-popup.html`), 구매(`POST /shop/buy` 1개 단위) 및 판매(`POST /shop/sell` 1개 단위, 장착중 판매 거부).
  5. 장비 수리 도메인 메서드 `OwnedItem.repairBy(amount, max)` 및 수리 컨트롤러(`RepairController`), 수리 팝업(`repair-popup.html`, 닳은 장비만 `ceil(current)<max`, 올림 정수 표시), 1포인트 95% 확률 수리(주입 Random).
  6. 힐러 치료 컨트롤러(`HealController`, 100골드 소모 풀회복), 프론트 상단바 갱신 함수(`refreshTopBar`) 추출 및 alert.
  7. 마법학교 `NpcType.MAGIC_SCHOOL`에 `인챈트` 라벨 추가(placeholder alert).
  8. `item.json`에 `short_sword`·`long_sword` 추가 및 `npc.json` NPC별 `shopItems` authoring.
- **이연**:
  - 인챈트 실제 로직(인챈트 스크롤 소모, 랭크별 확률, 인스턴스 보너스 부여) → 후속 인챈트 스펙.
  - 촌장 퀘스트(8순위), 왼쪽 팝업(9순위).
  - 상점 수량 다중 입력 모달(1클릭=1개 확정).

---

## Architecture

### 모듈 구조 (DDD 4계층)

```
myrpg/src/
├── main/java/com/myapps/web/myrpg/
│   ├── interfaces/api/
│   │   ├── ShopController.java                 # [신규] GET /shop, POST /shop/buy, POST /shop/sell
│   │   ├── RepairController.java               # [신규] GET /repair, POST /repair
│   │   ├── HealController.java                 # [신규] POST /heal
│   │   ├── PlayScreenController.java           # [확장] talkingNpcId 모델 속성 세팅
│   │   └── PlayScreenViewHelper.java           # [유지/보조] 상단바/상호작용 뷰 조립
│   ├── application/
│   │   ├── service/
│   │   │   ├── ShopService.java                # [신규] 판매가 계산, 구매 목록 조립, 구매/판매 오케스트레이션
│   │   │   ├── NpcService.java                 # [확장] shopItems optional 필드 파싱 및 검증
│   │   │   └── InventoryService.java           # [확장/보조] 수리 뷰 모델 조립 및 도메인 연동
│   │   └── dto/
│   │       ├── ShopView.java                   # [신규] 상점 팝업 뷰 (buyItems, sellItems, gold, npcId)
│   │       ├── ShopBuyItemView.java            # [신규] 상점 판매 물건 뷰 (id, name, typeLabel, buyPrice, detailLines)
│   │       ├── ShopSellItemView.java           # [신규] 내 인벤토리 판매 대상 뷰 (ownedItemId, name, typeLabel, quantity, sellValue, equipped, detailLines)
│   │       ├── RepairView.java                 # [신규] 수리 팝업 뷰 (repairItems, gold)
│   │       └── RepairItemView.java             # [신규] 수리 대상 장비 뷰 (ownedItemId, name, typeLabel, currentDurabilityCeil, maxDurability, repairCost, equipped, detailLines)
│   └── domain/
│       ├── model/
│       │   ├── Npc.java                        # [확장] List<String> shopItems 추가
│       │   ├── NpcType.java                    # [확장] MAGIC_SCHOOL actionLabels에 "인챈트" 추가
│       │   ├── OwnedItem.java                  # [확장] repairBy(double amount, double max) 신설
│       │   └── (재사용) CharacterProgress, Item, EquipmentItem, PotionItem, BonusTarget, EquipBonus
│       └── repository/
│           └── (재사용) OwnedItemRepository, CharacterProgressRepository
└── main/resources/
    ├── data/
    │   ├── item.json                           # [확장] short_sword, long_sword 추가 (buyPrice 정의)
    │   └── npc.json                            # [확장] ferghus, neris, dilys, manus 등에 shopItems authoring
    ├── templates/fragments/
    │   ├── shop-popup.html                     # [신규] 상점 팝업 overlay + content
    │   ├── repair-popup.html                   # [신규] 수리 팝업 overlay + content
    │   ├── center.html                         # [확장] NPC 버튼 onclick에 talkingNpcId 주입
    │   └── play.html                           # [확장] shop-popup, repair-popup 프래그먼트 include
    └── static/
        ├── css/myrpg.css                       # [확장] 상점/수리 팝업 스타일
        └── js/myrpg.js                         # [확장] npcAction 분기, openShop, openRepair, heal, refreshTopBar
```

### 상점·수리·치료 흐름

```
[NPC 대화]
  PlayScreenController -> model.addAttribute("talkingNpcId", npcId)
  center.html: <button onclick="npcAction('상점', 'ferghus')">

[1. 상점 흐름]
  openShop(npcId) -> GET /shop?npcId=ferghus
    -> ShopController.shop()
    -> ShopService.buildShopView(npcId, progress.getGold())
    -> fragments/shop-popup :: shop-content
  buyItem(npcId, itemId) -> POST /shop/buy?npcId=...&itemId=...
    -> ShopService.buy(progress, npcId, itemId)
       - NPC shopItems 포함 및 buyPrice 존재 검증
       - progress.spendGold(buyPrice)
       - InventoryService.acquireItem(itemId, 1)
       - actionLog.add("아이템을 구매했습니다", "item")
  sellItem(ownedItemId) -> POST /shop/sell?ownedItemId=...
    -> ShopService.sell(progress, ownedItemId)
       - equipped 여부 검증 (장착 중이면 EquipConflictException)
       - sellValue = ShopService.sellValueOf(ownedItem)
       - ownedItem.decreaseQuantity(1) (0이면 삭제)
       - progress.gainGold(sellValue)
       - actionLog.add("아이템을 판매했습니다", "item")

[2. 수리 흐름]
  openRepair() -> GET /repair
    -> RepairController.repairPopup()
    -> 닳은 장비 필터링 (ceil(currentDurability) < maxDurability)
    -> fragments/repair-popup :: repair-content
  repairItem(ownedItemId) -> POST /repair?ownedItemId=...
    -> RepairController.repair(ownedItemId)
       - 대상 장비 확인 및 ceil(current) < max 검증
       - cost = ShopService.sellValueOf(ownedItem) (1p 수리비 = 판매가)
       - progress.spendGold(cost) (시도 시 소모, 실패 환불 없음)
       - Random(95%) 판정:
           성공: ownedItem.repairBy(1.0, max) -> "수리 성공! 내구도 +1"
           실패: 내구도 변화 없음 -> "퍼거스가 손을 삐끗했다… 수리 실패!"
       - characterService.saveTurn(progress)

[3. 치료 흐름]
  heal() -> POST /heal
    -> HealController.heal()
       - progress.spendGold(100) (HEAL_COST = 100)
       - vitalMax = statProgression.vitalMaxFor(level, talent) + equippedVitalBonus
       - progress.fullRecover(vitalMax)
       - characterService.saveTurn(progress)
       - alert("치료되었습니다!") & refreshTopBar()
```

---

## Components and Interfaces

### 1. Npc & NpcType (domain/model) [확장]

```java
public record Npc(
        String id,
        String name,
        NpcType type,
        String nodeId,
        String personality,
        NpcLines lines,
        List<String> shopItems
) {
    /** 하위 호환 보조 생성자 (shopItems 생략 시 빈 리스트) */
    public Npc(final String id, final String name, final NpcType type,
               final String nodeId, final String personality, final NpcLines lines) {
        this(id, name, type, nodeId, personality, lines, List.of());
    }
}
```

- `NpcType.MAGIC_SCHOOL`: `actionLabels`를 `List.of("상점", "인챈트")`로 확장.

### 2. OwnedItem (domain/model) [확장]

```java
/**
 * 내구도를 지정된 양만큼 복구한다 (최대 내구도 상한).
 *
 * @param amount 복구량 (1.0)
 * @param max    장비의 최대 내구도
 */
public void repairBy(final double amount, final double max) {
    this.currentDurability = Math.min(max, this.currentDurability + amount);
}
```

### 3. ShopService (application/service) [신규]

판매가 계산, 상점 뷰 구성, 구매/판매 로직을 담당한다.

```java
@Service
public class ShopService {
    public static final double SELL_RATIO = 0.5;
    public static final int WEIGHT = 10;
    public static final int CRITICAL_WEIGHT = 1;

    private final ItemCatalogService itemCatalogService;
    private final NpcService npcService;
    private final OwnedItemRepository ownedItemRepository;
    private final InventoryService inventoryService;
    private final CharacterService characterService;
    private final ActionLog actionLog;

    /**
     * 보유 아이템의 판매가(1개당)를 계산한다.
     * 공식: 기본가 + 인스턴스 보너스 (현재 인스턴스 보너스는 0)
     * 기본가: buyPrice 존재 시 round(buyPrice * 0.5), 부재 시 Σ(보너스 * 가중치)
     */
    public long sellValueOf(final OwnedItem ownedItem) {
        final Item item = itemCatalogService.getById(ownedItem.getItemId());
        return calculateSellValue(item);
    }

    public long calculateSellValue(final Item item) {
        if (item.buyPrice() != null) {
            return Math.round(item.buyPrice() * SELL_RATIO);
        }
        if (item instanceof EquipmentItem equip) {
            long total = 0;
            for (final EquipBonus bonus : equip.bonuses()) {
                total += (long) bonus.amount() * weightOf(bonus.target());
            }
            return total;
        }
        return 0L;
    }

    public int weightOf(final BonusTarget target) {
        return (target == BonusTarget.CRITICAL) ? CRITICAL_WEIGHT : WEIGHT;
    }

    public ShopView buildShopView(final String npcId, final long currentGold);
    
    @Transactional
    public void buy(final CharacterProgress progress, final String npcId, final String itemId);

    @Transactional
    public void sell(final CharacterProgress progress, final long ownedItemId);
}
```

### 4. ShopController (interfaces/api) [신규]

```java
@Controller
@RequestMapping("/shop")
public class ShopController {
    private static final String FRAGMENT_SHOP_POPUP = "fragments/shop-popup :: shop-content";

    @GetMapping
    public String shop(@RequestParam(required = false) final String npcId, final Model model);

    @PostMapping("/buy")
    public String buy(@RequestParam final String npcId, @RequestParam final String itemId, final Model model);

    @PostMapping("/sell")
    public String sell(@RequestParam(required = false) final String npcId, @RequestParam final long ownedItemId, final Model model);
}
```

### 5. RepairController (interfaces/api) [신규]

```java
@Controller
@RequestMapping("/repair")
public class RepairController {
    private static final String FRAGMENT_REPAIR_POPUP = "fragments/repair-popup :: repair-content";
    private static final int REPAIR_SUCCESS_RATE_PERCENT = 95;

    private final Random random; // 테스트 주입 가능 생성자 제공

    @GetMapping
    public String repairPopup(final Model model);

    @PostMapping
    public String repair(@RequestParam final long ownedItemId, final Model model);
}
```

### 6. HealController (interfaces/api) [신규]

```java
@Controller
@RequestMapping("/heal")
public class HealController {
    private static final int HEAL_COST = 100;

    private final CharacterService characterService;
    private final StatProgression statProgression;
    private final InventoryService inventoryService;
    private final ActionLog actionLog;

    @PostMapping
    @ResponseBody
    public ResponseEntity<Void> heal();
}
```

---

## Data Models

### 1. `item.json` (신규 무기 추가)

```json
[
  {
    "id": "short_sword",
    "name": "숏소드",
    "type": "weapon",
    "kind": "one_handed_sword",
    "bonuses": [ { "target": "STR", "amount": 8 } ],
    "maxDurability": 15,
    "buyPrice": 300
  },
  {
    "id": "long_sword",
    "name": "롱소드",
    "type": "weapon",
    "kind": "one_handed_sword",
    "bonuses": [ { "target": "STR", "amount": 12 } ],
    "maxDurability": 15,
    "buyPrice": 700
  }
]
```

### 2. `npc.json` (`shopItems` authoring)

- `ferghus` (티르코네일 대장간): `"shopItems": ["short_sword"]`
- `neris` (던바튼 대장간): `"shopItems": ["long_sword"]`
- `dilys` (티르코네일 힐러집): `"shopItems": ["hp_potion_30"]`
- `manus` (던바튼 힐러집): `"shopItems": ["hp_potion_30"]`
- 그 외 NPC (duncan, lazar, stewart, aran, malcolm 등): `shopItems` 필드 없거나 빈 배열 `[]`.

---

## Correctness Properties

*프로퍼티는 시스템의 모든 유효한 실행에서 참이어야 하는 특성이다.* (jqwik PBT 대상)

### Property 1: 판매가 계산식 배타성 및 결정성 (BuyPrice vs Catalog Bonuses)
*For any* `buyPrice`가 존재하는 아이템에 대해, `sellValueOf`는 항상 `round(buyPrice * 0.5)`이며 카탈로그 스탯 보너스를 합산하지 않는다. *For any* `buyPrice`가 없는 장비 아이템에 대해, `sellValueOf`는 `Σ (amount * weightOf(target))`와 정확히 일치한다.
**Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.8, 14.5**

### Property 2: NPC 데이터 파싱 및 shopItems 기본값 불변
*For any* 유효한 NPC JSON 노드에 대해, `shopItems`가 배열로 주어지면 해당 아이템 ID 목록이 불변 리스트로 로드되고, 필드가 없거나 null이면 빈 불변 리스트(`List.of()`)가 할당되며 기존 필수 필드 검증 규칙이 유지된다.
**Validates: Requirements 2.1, 2.2, 2.3, 15.3**

### Property 3: 상점 구매 유효성 검증 및 골드/인벤토리 상태 보존
*For any* 상점 구매 요청에 대해, 요청된 `itemId`가 해당 NPC의 `shopItems`에 포함되지 않거나 `buyPrice`가 null인 경우 또는 캐릭터의 골드가 부족한 경우, 구매는 거부되고 캐릭터 골드 및 인벤토리 상태는 변경되지 않는다.
**Validates: Requirements 4.1, 4.2, 4.4, 15.2**

### Property 4: 상점 판매 시 장착 중 장비 보호 및 1개 단위 처리
*For any* `equipped == true`인 장비에 대한 판매 요청은 `EquipConflictException`으로 거부되며, 판매가 성공한 아이템은 수량이 정확히 1 감소(수량 0 시 행 제거)하고 캐릭터 골드는 정확히 `sellValueOf`만큼 증가한다.
**Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5**

### Property 5: 수리 도메인 repairBy 연산 및 max 상한 불변
*For any* 현재 내구도 `currentDurability >= 0.0` 및 최대 내구도 `max > 0.0`에 대해, `repairBy(1.0, max)` 호출 후의 내구도는 `min(max, currentDurability + 1.0)`과 정확히 일치하며 소수점 오차가 누적되지 않고 `max`를 초과하지 않는다.
**Validates: Requirements 7.1, 7.2, 7.3**

### Property 6: 수리 목록 필터링 조건 일치 (ceil(current) < max)
*For any* 보유 아이템 목록에 대해, 수리 목록에 포함되는 아이템은 오직 `EquipmentItem`이면서 `Math.ceil(currentDurability) < maxDurability`인 아이템뿐이며, 포션 및 풀내구도 장비(`ceil >= max`)는 항상 제외된다.
**Validates: Requirements 8.1, 8.2, 8.3, 8.5, 8.6**

### Property 7: 수리 비용과 1포인트 판매가 동치성
*For any* 수리 가능한 장비에 대해, 1포인트 수리 시 소모되는 골드는 해당 장비의 `sellValueOf`와 정확히 일치한다.
**Validates: Requirements 9.3**

### Property 8: 수리 시도 시 골드 소모 및 실패 비환불 불변
*For any* 수리 시도에 대해, 수리 성공(95%) 시 내구도가 +1 되고 골드가 차감되며, 수리 실패(5%) 시에도 차감된 수리비는 환불되지 않고 내구도는 유지된다. 골드 부족 시에는 골드와 내구도 모두 변경되지 않는다.
**Validates: Requirements 9.4, 9.5, 9.6**

### Property 9: 치료 후 활력치 상단바 최대치 완벽 일치
*For any* 캐릭터 레벨과 재능, 장착 장비 보너스 상태에서 `POST /heal` 성공 후의 캐릭터 `hpCurrent`, `mpCurrent`, `staminaCurrent`는 `PlayScreenViewHelper.buildTopBar`에서 산출하는 최대치(`vitalMax`)와 정확히 일치한다.
**Validates: Requirements 11.3, 11.4**

### Property 10: NpcType actionLabels 무결성
*For any* `NpcType`에 대해 `actionLabels`는 null이 아니며, `MAGIC_SCHOOL`은 `["상점", "인챈트"]`, `BLACKSMITH`는 `["상점", "수리"]`, `HEALER`는 `["상점", "치료받기"]`를 정확히 포함한다.
**Validates: Requirements 12.1, 12.4**

---

## Error Handling

| 상황 | 예외 / 처리 | 반환 / UX |
|---|---|---|
| 골드 부족 (구매/수리/치료) | `InsufficientGoldException` (006 기존) | `GlobalExceptionHandler` 에러 fragment → 클라이언트 alert |
| 인벤토리 용량 초과 (구매) | `InventoryFullException` (006 기존) | `GlobalExceptionHandler` 에러 fragment → 클라이언트 alert |
| 장착 중인 장비 판매 시도 | `EquipConflictException` (006 기존) | "장착을 해제한 후 판매할 수 있습니다" alert |
| 해당 NPC 판매 목록 외 아이템 구매 시도 | 비즈니스 예외 / 거부 | 구매 거부, 상태 불변 |
| 내구도 가득 찬 장비 수리 요청 | 무시 (수리 미수행, 무비용) | 갱신 fragment 반환 |
| 존재하지 않는 아이템 ID 또는 NPC ID | `IllegalArgumentException` / `ItemDataException` | 안전한 기본값(빈 목록) 또는 에러 처리 |

---

## Testing Strategy

### 1. 이중 테스트 접근 (jqwik PBT + Mockito/JUnit5 단위 테스트)

- **jqwik 프로퍼티 기반 테스트**:
  - `ShopServiceSellValuePropertyTest` (Property 1)
  - `NpcServiceShopItemsParsingPropertyTest` (Property 2)
  - `ShopServiceBuyValidationPropertyTest` (Property 3)
  - `ShopServiceSellEquippedProtectionPropertyTest` (Property 4)
  - `OwnedItemRepairByPropertyTest` (Property 5)
  - `RepairListFilterPropertyTest` (Property 6)
  - `RepairCostEquivalencePropertyTest` (Property 7)
  - `RepairExecutionPropertyTest` (Property 8)
  - `HealVitalMaxEquivalencePropertyTest` (Property 9)
  - `NpcTypeActionLabelsPropertyTest` (Property 10)

- **단위 및 슬라이스 테스트**:
  - `ShopServiceTest`: 대표 판매가 검증(포션 25, 초보한손검 50, 초보활 110, 숏소드 150, 롱소드 350), 구매/판매 트랜잭션 검증.
  - `OwnedItemTest`: `repairBy` 메서드 동작(소수점 +1, max 캡).
  - `ShopControllerTest` (`@WebMvcTest` + `@MockitoBean`): `/shop`, `/shop/buy`, `/shop/sell` 프래그먼트 렌더 및 모델 속성 검증.
  - `RepairControllerTest` (`@WebMvcTest` + `@MockitoBean`): `/repair` 닳은 장비 렌더, 수리 성공/실패 mock 주입 검증.
  - `HealControllerTest` (`@WebMvcTest` + `@MockitoBean`): `/heal` 100골드 소모 및 `fullRecover` 호출 검증.
  - `NpcTypeTest`: `MAGIC_SCHOOL` 인챈트 라벨 검증.
  - `ItemCatalogServiceTest` & `NpcServiceTest`: 신규 JSON 데이터 로딩 및 무결성 검증.
  - `VisualJsPreservationAndJsonLoadingIntegrationTest`: 템플릿/JS 리소스 기대값 검증.

### 2. 빌드 및 품질 검증

- 모든 작업 단위에서 `mvn test -pl myrpg` 및 `mvn clean install -pl myrpg -am` 검증 수행.
- 미사용 import/변수 제거, 매직 넘버 상수화(`HEAL_COST = 100`, `REPAIR_SUCCESS_RATE = 95` 등) 엄격 준수.

---

## Migration 영향 범위 (기존 산출물)

- **`Npc`**: 7-인자 record로 확장 + 기존 6-인자 보조 생성자 제공으로 기존 테스트 및 호출부 완전 호환.
- **`NpcService`**: `shopItems` optional 파싱 추가. 기존 필수 필드 및 중복 ID 검증 유지.
- **`NpcType`**: `MAGIC_SCHOOL`의 `actionLabels`에 `"인챈트"` 추가. 관련 테스트 기대값 갱신.
- **`OwnedItem`**: `repairBy(amount, max)` 신설. 기존 `repairToMax`는 보존 또는 주석 정리.
- **`item.json`**: `short_sword`, `long_sword` 추가 (`buyPrice` 정의). 기존 카탈로그 파싱 무회귀.
- **`npc.json`**: 주요 NPC에 `shopItems` 추가.
- **`center.html` & `PlayScreenController`**: 대화 중인 NPC ID(`talkingNpcId`) 바인딩 추가.
- **`myrpg.js`**: `npcAction` 분기 확장, 상점/수리/치료/상단바 갱신 함수 추가.
