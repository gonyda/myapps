# 7순위 — NPC 행동 실기능 (상점 / 수리 / 치료) 상세 설계

> 로드맵 7순위 `npc-actions-system`. 실제 소스(`myrpg`) 위에 쌓는 기능 설계 문서.
> **보관 / 은행은 이미 개발 완료**(`BankController` + `InventoryService.moveToBank/moveToInventory` + `Bank`)이므로 본 문서에서 다루지 않는다.
> 개발이 완료되면 이 문서는 삭제한다.

---

## 0. 미결정사항 (확정 필요 — 여기부터 결정하고 시작)

이 스펙의 핵심 결정 사항이다. **모든 항목 확정 완료** — 개발 착수 가능 상태다. (M18·M20은 이미 코드에 반영 완료. 마법학교/학교 상점 판매 목록은 이번 스펙 범위에서 제외.)

| # | 항목 | 확정값 | 근거 / 비고 | 상태 |
|---|---|---|---|---|
| M1 | 판매가 가중치 (대상별) | **CRITICAL=1, 그 외(STR/DEX/INT/DEF/HP/MP/STAMINA)=WEIGHT=10** | CRITICAL amount는 0.1%단위(10=1%)라 ×10이면 과대 → 1/10 보정. 수리비=판매가라 골드싱크 스케일도 겸함. §4.3 | **확정** |
| M2 | 기본가 규칙 (배타) | **buyPrice 있으면 `buyPrice × 0.5`, 없으면 `카탈로그 보너스 × 가중치`** | `기본가` = 인챈트 전 고유 판매가(계산값, 저장 필드 아님). §4.3 | **확정** |
| M2a | 인챈트 프리미엄 | **판매가 = 기본가 + 인스턴스보너스 × 가중치** (상점템·드랍템 공통) | 인챈트로 붙은 인스턴스 보너스는 항상 덧셈 → 상점템도 인챈트 시 판매가·수리비↑. 미구현 현재는 0 | **확정** |
| M3 | 판매율 `SELL_RATIO` | **0.5** (구매가의 50%) | 포션(buyPrice 50) → 25 | **확정** |
| M4 | 구매가(`buyPrice`) authoring | **사용자가 `item.json`에 아이템별 수기 지정** | 없으면(`null`) 상점 미판매 = 드랍 전용 = 보너스 기반 판매가 | **확정** |
| M5 | 초보자용 장비 판매 여부 | **상점에서 팔지 않음**(buyPrice 미지정) | 초보 장비는 드랍/기본지급 전용, 소모품 취급 | **확정** |
| M6 | 수리비(판매가 연동) | **`수리비/1p = sellValue` (판매가 그대로)** | ✅ 사용자 확정. 인챈트로 가치↑ 장비는 수리비도 비싸지지만, 인챈트 리스크(실패=스크롤 파괴)를 뚫은 장비라 소유가치가 높아 비싼 수리를 감수할 만함(골드 싱크). §5.3 | **확정** |
| M7 | 치료비 | **100골드 고정** | ✅ 사용자 확정 | **확정** |
| M8 | 치료 효과 | **HP / MP / 스태미나 전부 풀회복** | ✅ 사용자 확정 | **확정** |
| M9 | 치료 UX | 팝업 없이 `치료받기` 버튼 → 성공 시 `alert("치료되었습니다!")` | ✅ 사용자 확정 | **확정** |
| M10 | 마법학교 `인챈트` 버튼 | 버튼만 추가, 클릭 시 `alert("추후 설계 예정입니다.")` | ✅ 사용자 확정. 실제 기능은 인챈트 스크롤 스펙에서 | **확정** |
| M11 | 상점 판매 목록 구성 | **재고 개념 없음. NPC별 `shopItems`(npc.json) 목록으로 분리** → 던바튼/티르코네일 대장간이 서로 다른 목록 | ✅ 사용자 확정. §4.5 | **확정** |
| M12 | 수리 단위 | **무조건 1포인트씩** (버튼 1클릭 = 내구도 +1) | ✅ 사용자 확정. `repairToMax` 대신 1p 수리 메서드 신설 | **확정** |
| M13 | 수리 성공확률 | **95% 고정** (모든 수리 기능 공통) | ✅ 사용자 확정. 실패 5% = 내구도 증가 없음 | **확정** |
| M14 | 수리 화면 | **인벤토리 레이아웃 재사용**(착용/해제 버튼 자리에 `수리` 버튼) | ✅ 사용자 확정. 인벤토리가 이미 내구도 표시 | **확정** |
| M15 | 수리 실패 시 골드 | **시도 시 소모, 실패해도 환불 없음** | ✅ 사용자 확정. 퍼거스 "손이 미끄러졌네" 플레이버 | **확정** |
| M16 | 각 NPC의 `shopItems` 실제 내용 | ferghus=[숏소드], neris=[롱소드], 모든 힐러집=[생명력30포션]. **마법학교/학교는 이번 스펙에서 판매목록 추가 안 함**(빈 목록 = 판매만 가능) | 숏/롱소드 정의 §8.1 | **확정** |
| M17 | 수리 목록 노출 범위 | **내구도 닳은 장비만 표시**(`ceil(currentDurability) < maxDurability`) | 풀내구 장비는 목록에서 제외. §5 | **확정** |
| M18 | 내구도 표시 (전 화면 공통) | **`ceil(currentDurability)/max` 정수**(예: 12.4/20 → 13/20). 수리 화면 + 인벤토리 화면 | 표시는 올림 정수, 실제 데이터는 double 유지. **인벤토리(`inventory-popup.html`)는 이미 반영 완료**(`formatInteger(ceil(...))`). §5.3 | **확정(인벤토리 반영 완료)** |
| M19 | 상점 레이아웃 | **모바일 세로 배치**: 상점 물건(위) / 소지품(아래) / 소지 골드(하단) | 은행 팝업 구조 참고, 가로 확장 금지. §4.4 | **확정** |
| M20 | 내구도 감소율(턴당) | **0.2 → 0.05 확정**(1p=20턴 ≈ 전투 4회). 수리비=판매가 유지비 완화 | `BattleService.DURABILITY_PER_ATTACK=0.05`로 **반영 완료**(관련 테스트·주석 포함, 빌드 성공). ⚠️ `data-balance-guide.md`(0.2/100턴 문구)는 스티어링이라 미변경 — 추후 갱신 필요 | **확정(코드 반영 완료)** |
| M21 | 구매·판매 수량 단위 | **1클릭 = 1개**(포션 스택도 1개씩 구매/판매) | ✅ 사용자 확정. 수량 입력 UI 없음 | **확정** |

---

## 1. 개요 & 범위

### 1.1 이번 스펙에서 구현
- **상점(Shop)**: 아이템 구매 + 판매. 판매가 계산식 확정.
- **수리(Repair, 대장간)**: 장비 내구도 수리 + 수리비 정책.
- **치료(Heal, 힐러집)**: 100골드 소모 → HP/MP/스태미나 풀회복 → `alert("치료되었습니다!")`.
- **아이템 판매가 계산식**: `기본가 + 인스턴스보너스 × 가중치`. 기본가(인챈트 전 고유가)는 buyPrice 있으면 `buyPrice × 0.5`, 없으면 `카탈로그 보너스 × 가중치`. 확정(§4.3).
- **마법학교 인챈트 버튼(placeholder)**: 버튼 추가 + "추후 설계 예정" alert (기능은 인챈트 스크롤 스펙에서).

### 1.2 범위 밖 (건드리지 않음)
- 보관 / 은행 (완료).
- 인챈트 실제 로직(성공확률·스크롤 소모·인스턴스 보너스) → 인챈트 스크롤 스펙.
- 촌장 퀘스트(8순위), 왼쪽 팝업(9순위).

---

## 2. 기존 소스 분석 (재사용 지점)

| 대상 | 위치 | 재사용 포인트 |
|---|---|---|
| `NpcType` (enum) | `domain/model/NpcType.java` | `actionLabels`가 NPC별 버튼의 단일 소스. 여기 라벨 추가/변경 |
| NPC 행동 분기 | `static/js/myrpg.js` `npcAction(label)` | 현재 `은행`만 처리, 나머지 `구현 예정입니다`. 여기 분기 추가 |
| 버튼 렌더 | `PlayScreenViewHelper.buildNpcActions()` | `type().actionLabels()` → `ActionButton`. 로직 변경 불필요(라벨만 늘면 됨) |
| 팝업 패턴 | `BankController` + `fragments/bank-popup.html` | GET=팝업 fragment, POST=조작 후 갱신 fragment 스왑. **상점/수리 동일 패턴 복제** |
| 골드 | `CharacterProgress.spendGold/gainGold` + `InsufficientGoldException` | 구매/판매/수리/치료 골드 처리. 부족 시 `GlobalExceptionHandler` 경유 |
| 풀회복 | `CharacterProgress.fullRecover(VitalMax)` | 치료가 그대로 호출. `VitalMax`는 `StatProgression.vitalMaxFor(level, talent)` + 장비 보너스 |
| 내구도 수리 | `OwnedItem.repairToMax(double max)` (기존) | **이번 스펙은 1포인트 수리라 미사용**. 대신 `repairBy(1, max)`(내구도 +1, max 상한) 신설 |
| 최대 내구도 | `EquipmentItem.maxDurability()` | 수리 목표(상한) + 수리 가능 여부 판정 |
| 인벤토리 팝업/레이아웃 | `InventoryController` + `fragments/inventory-popup.html` | **수리 화면이 이 레이아웃을 재사용**. `item-durability`는 **ceil 정수 표시로 이미 변경 완료**(M18). `item-actions`의 착용/해제 자리에 `수리` 버튼 |
| 아이템 상세/보너스 | `InventoryService.describe()`, `EquipmentItem.bonuses()`, `EquipBonus.amount()` | 판매가 계산·상점 목록 상세에 재사용 |
| 구매가 | `Item.buyPrice()` (nullable) | `null`이면 상점 미판매. 사용자가 item.json에 수기 지정 |
| NPC 조회 | `NpcService.byId(npcId)` | `GET /shop?npcId=`에서 해당 NPC의 `shopItems` 조회 |
| NPC 데이터 파싱 | `NpcService.parseNpcNode` + `Npc` 레코드 | `shopItems` optional 필드 추가(§4.5). `parseStringList` 재사용 |
| 말 거는 대상 id 노출 | 몬스터 `encounteredMonsterId` (center.html onclick) | **동일 패턴으로 `talkingNpcId`** 추가 → `npcAction(label, npcId)` |
| 아이템 이동 | `OwnedItem.decreaseQuantity/increaseQuantity`, `StorageKind` | 판매 시 인벤토리 차감, 구매 시 획득(`InventoryService`) |

> ⚠️ `PlayScreenViewHelper.buildNpcActions`, `NpcService.parse*`는 커버 테스트 없음 — 라벨 추가는 `NpcTypeTest` / `NpcTypeCompletenessPropertyTest`가 잡아줌.

### 2.1 현재 NPC별 버튼 (변경 전/후)

| NpcType | typeString | 현재 actionLabels | 변경 후 |
|---|---|---|---|
| CHIEF | chief | [퀘스트] | (변경 없음, 8순위) |
| BLACKSMITH | blacksmith | [상점, 수리] | (그대로 — 이번에 실기능 부여) |
| MAGIC_SCHOOL | magic-school | [상점] | **[상점, 인챈트]** ← 버튼 추가 |
| SCHOOL | school | [상점] | (그대로) |
| HEALER | healer | [상점, 치료받기] | (그대로 — 치료받기 실기능 부여) |
| BANK | bank | [은행] | (완료) |

> 마을 구성 참고: **티르코네일**(은행 없음: 촌장/대장간/마법학교/학교/힐러), **던바튼**(대장간/힐러/마법학교/학교/은행). 상점·수리·치료는 두 마을 모두에서 동작.

---

## 3. NPC 행동 → 라우팅 매핑

`myrpg.js`의 `npcAction(label)` 분기를 아래로 확장한다.

```js
// 버튼 onclick에서 라벨 + 말 거는 NPC id를 함께 넘긴다.
// (center.html: onclick="npcAction(this.textContent, /*[[${talkingNpcId}]]*/)")
function npcAction(label, npcId) {
    if (label === '은행') { openBank(); }
    else if (label === '상점') { openShop(npcId); }   // GET /shop?npcId=… → 팝업
    else if (label === '수리') { openRepair(); }      // GET /repair → 팝업(인벤토리 재사용)
    else if (label === '치료받기') { heal(); }        // POST /heal → alert
    else if (label === '인챈트') { alert('추후 설계 예정입니다.'); }
    else { alert('구현 예정입니다'); }
}
```

> `talkingNpcId`는 몬스터의 `encounteredMonsterId`와 동일하게 컨트롤러가 모델 속성으로 심고, `center.html`의 NPC 버튼 onclick에 주입한다(§4.5).

| 라벨 | 트리거 | 서버 엔드포인트 | 응답 |
|---|---|---|---|
| 상점 | `openShop(npcId)` | `GET /shop?npcId=…`, `POST /shop/buy`, `POST /shop/sell` | 팝업 fragment 스왑. 구매 목록은 **해당 NPC의 `shopItems`**(§4.5) |
| 수리 | `openRepair()` | `GET /repair`(인벤토리 레이아웃 재사용·수리 모드), `POST /repair`(1포인트 수리 시도) | 팝업 fragment 스왑 |
| 치료받기 | `heal()` | `POST /heal` | 성공 시 상단바 갱신 + `alert("치료되었습니다!")` |
| 인챈트 | 즉시 `alert` | (없음) | placeholder |

---

## 4. 상점 (Shop)

### 4.1 컨트롤러 (`ShopController`, `@RequestMapping("/shop")`)
`BankController` 패턴 그대로 복제.

| 메서드 | 엔드포인트 | 동작 |
|---|---|---|
| `shop(npcId, Model)` | `GET /shop?npcId=…` | **해당 NPC의 `shopItems`**로 구매 목록 조립 + 판매 목록(내 인벤토리) + 보유 골드 → 팝업 fragment |
| `buy(npcId, itemId, Model)` | `POST /shop/buy` | itemId가 그 NPC `shopItems`에 있는지 검증 → `spendGold(buyPrice)` → `InventoryService`로 획득 → 갱신 |
| `sell(ownedItemId, Model)` | `POST /shop/sell` | 판매가 계산 → 인벤토리 차감 → `gainGold(sellValue)` → 갱신 fragment |

- 반환: `"fragments/shop-popup :: shop-content"` (스왑 방식은 은행과 동일).
- 예외: 골드 부족 `InsufficientGoldException`, 인벤토리 초과 `InventoryFullException` → 기존 `GlobalExceptionHandler` 재사용.
- **판매 제약**: 장착 중(`equipped`) 아이템은 판매 거부(은행 맡기기와 동일 정책, `EquipConflictException` — "장착을 해제한 후 판매할 수 있습니다.").
- **구매 대상 검증**: 구매 요청 itemId가 해당 NPC `shopItems`에 없거나 buyPrice가 `null`이면 거부(위변조 방지).
- **수량 단위(M21)**: 구매/판매는 **1클릭 = 1개** 제안. 포션 스택도 1개씩 구매(`increaseQuantity(1)`)·판매(`decreaseQuantity(1)`, 0되면 행 제거). 장비는 항상 1개. (대안: 수량 입력 모달 — 확정 필요.)

### 4.2 신설 서비스 로직 (`ShopService` 또는 `InventoryService` 확장)
- `long sellValueOf(OwnedItem owned)` — §4.3 공식.
- `List<Item> shopBuyList(String npcId)` — `NpcService.byId(npcId).shopItems()` → 카탈로그 조회(buyPrice 있는 것만).
- `void buy(CharacterProgress, String npcId, String itemId)` — shopItems 포함·buyPrice 검증 → 차감 → 획득.
- `void sell(long ownedItemId)` — 판매가 계산 → 차감/삭제 → 가산.

### 4.3 아이템 판매가 계산식 (확정)

> **용어**: **`기본가`** = 인챈트 전 아이템의 고유 판매 가치. **JSON에 저장하는 필드가 아니라 계산으로 나오는 값**이다(item.json엔 `buyPrice`만 존재). 상점 판매 아이템이면 `buyPrice × 0.5`, 드랍 전용이면 `카탈로그 보너스 × 가중치`로 산출한다.

```
판매가(sellValue) = 기본가 + (인스턴스보너스 × 가중치)

기본가 (배타 — 인챈트 전 고유 가치):
  · buyPrice 있음(상점 판매) → round(buyPrice × SELL_RATIO)          // SELL_RATIO = 0.5
  · buyPrice 없음(드랍 전용) → Σ (카탈로그 amount × weightOf(target)) // 대상별 가중치

인스턴스보너스 항 = Σ (인챈트 amount × weightOf(target))   // 인챈트 프리미엄

weightOf(target):
  · CRITICAL → 1        // amount가 0.1%단위(10=1%) → 10배 스케일 보정
  · 그 외(STR/DEX/INT/DEF/HP/MP/STAMINA) → WEIGHT = 10
```

- `WEIGHT = 10`, `CRITICAL 가중치 = 1` (M1), `SELL_RATIO = 0.5` (M3).
- **대상별 가중치**: STR/DEX/INT/DEF는 amount×10, **CRITICAL만 amount×1**(0.1%단위 보정). 1%크리(amount 10)가 STR 1포인트(=10)와 동일 가치.
- **핵심**: "카탈로그(고유) 보너스"와 "인챈트 인스턴스 보너스"를 분리한다. 기본가는 상점템/드랍템에 따라 **배타**로 정하고, **인챈트 인스턴스 보너스는 상점템·드랍템 무관하게 항상 위에 더한다** → 어떤 아이템이든 인챈트하면 판매가·수리비가 오른다(M2a). 인챈트 보너스에도 동일한 `weightOf`를 적용한다.
- 포션(buyPrice 50) → 25 + 0. 초보 한손검(드랍, STR+5) → 50 + 0. 초보 활(드랍, DEX+10·CRIT+10) → 10×10 + 10×1 = 110. 상점 검(buyPrice 200, 미인챈트) → 100 + 0 (< 200, 되팔이 차익 없음). 상점 검 + 인챈트(+3 STR) → 100 + 30 = 130.
- **인챈트 미구현인 현재는 인스턴스보너스 항 = 0** → 현재 판매가 = 기본가. 인챈트 스펙에서 `OwnedItem` 인스턴스 보너스 저장 구조가 생기면 이 항이 활성(판매가는 저장하지 않고 매번 계산).
- **수리비(1포인트당) = 이 판매가를 그대로 재사용**한다(§5.3).

#### 판매가 예시 (WEIGHT=10, 드랍템=보너스×10) — 판매/수리비 공통 기준

> 아래는 **플레이어가 보유한 아이템을 팔 때의 판매가**이자 **1포인트 수리비**의 기준값이다(§5.3). 초보 장비는 상점에서 **사지는 못하지만**(buyPrice 미지정), 드랍/기본지급분을 **팔 수는 있다**.

| 아이템 | 보너스 | 계산 | 판매가 |
|---|---|---|---|
| 초보자용 한손검 | STR+5 | 5×10 | 50 |
| 초보자용 양손검 | STR+10 | 10×10 | 100 |
| 초보자용 완드 | INT+5 | 5×10 | 50 |
| 초보자용 스태프 | INT+10 | 10×10 | 100 |
| 초보자용 활 | DEX+10, CRITICAL+10 | 10×10 + 10×**1** | **110** |
| 초보자용 방패 | DEF+5 | 5×10 | 50 |
| 초보자용 갑옷 | DEF+5 | 5×10 | 50 |
| 초보자용 투구 | DEF+3 | 3×10 | 30 |
| 초보자용 장갑 | DEF+2 | 2×10 | 20 |
| 초보자용 부츠 | DEF+2 | 2×10 | 20 |
| 생명력 30 포션 | (buyPrice 50) | 50×0.5 | 25 |

> 장비는 모두 드랍 전용(buyPrice 없음) → 대상별 가중치 합. 포션만 buyPrice 기반(×50%).
> **활**: DEX는 ×10, CRITICAL(0.1%단위)만 ×1로 보정 → 110. (이전 동일가중 방식의 200에서 낮아짐.)

### 4.4 상점 화면 레이아웃 (모바일 · 세로 배치)

**모바일 전용이라 가로로 넓히지 않는다.** 은행 팝업(`overlay > panel > header + body`) 골격과 **세로 스택** 구성을 그대로 참고한다. 은행의 좌/우 2단(`.bank-lists`)을 상점에서는 **위(상점 물건) / 아래(내 소지품)** 로 쌓는다. 하단에 소지 골드.

```
┌───────────────────────────────┐
│  상점                     [✕]  │  ← .panel-header
├───────────────────────────────┤
│  ── 판매 물건 (상점) ──────────  │  ← 섹션1: 상점 물건 (위)
│  숏소드            🔍           │
│   무기  300G          [구매]    │
│  … (해당 NPC shopItems 목록만)  │
│                               │
│  ── 내 소지품 ────────────────  │  ← 섹션2: 내 소지품 (아래)
│  초보자용 한손검 [장착중] 🔍    │
│   무기  판매가 50G     [판매]   │
│  생명력 30 포션  x3    🔍       │
│   포션  판매가 25G     [판매]   │
│  … (인벤토리 전체, 장착중 판매불가)│
├───────────────────────────────┤
│  💰 소지 골드            1,234  │  ← 하단 골드(소지 골드 1칸)
└───────────────────────────────┘
```

- 구조: `.bank-lists`처럼 **두 개의 리스트 섹션을 세로로 쌓고**(위=상점 물건, 아래=소지품), 각 행은 은행의 `.bank-item-row` 스타일 재사용. 가로 스크롤 없음.
- `🔍` = 아이템 상세 모달(기존 `openItemDetail(this)` + `data-detail` 재사용).
- 상점 물건(위): 해당 NPC `shopItems` 목록만. buyPrice 표시 + `[구매]`.
- 내 소지품(아래): 인벤토리 아이템 + `판매가`(계산값) + `[판매]`. 장착 중이면 `[장착중]` 배지 + 판매 거부.
- 하단 골드 1칸(은행은 2칸이지만 상점은 소지 골드만).
- 구매/판매 후 팝업 fragment 스왑 + 상단바 골드 갱신(은행 `withdraw` 후 갱신 패턴 참고).

---

### 4.5 NPC별 상점 판매 목록 (던바튼 vs 티르코네일 대장간)

**목표**: 같은 `상점` 버튼이라도 **어느 NPC와 대화 중이냐에 따라 판매 목록이 다르다**. (재고 수량 개념은 없음 — 무제한 판매되는 아이템 "목록"만 NPC별로 다름.)

**데이터 — `npc.json`에 NPC별 `shopItems` 추가 (optional)**
```json
{
  "id": "ferghus", "name": "퍼거스", "type": "blacksmith", "nodeId": "tir-chonaill",
  "shopItems": ["short_sword"],
  "lines": { ... }
}
```
```json
{
  "id": "neris", "name": "네리스", "type": "blacksmith", "nodeId": "dunbarton",
  "shopItems": ["long_sword"],
  "lines": { ... }
}
```
- 티르코네일 대장간(ferghus)=**숏소드**, 던바튼 대장간(neris)=**롱소드**로 판매 목록을 분리(§8.1 아이템 정의).
- `shopItems` 없으면 빈 목록(구매 불가, 판매만 가능).
- 목록에 넣는 아이템은 `item.json`에 **buyPrice가 지정**돼 있어야 구매 가능.

**코드 변경**
1. `Npc` 레코드에 `List<String> shopItems` 필드 추가(현재 `id, name, type, nodeId, personality, lines`).
2. `NpcService.parseNpcNode`에서 `shopItems` optional 파싱(없으면 `List.of()`). 기존 `parseStringList` 재사용.
3. 말 거는 NPC id를 화면에 노출: 컨트롤러가 `talkingNpcId` 모델 속성 추가(몬스터 `encounteredMonsterId`와 동일 패턴). `center.html`의 NPC 버튼 onclick에 주입 → `npcAction(label, npcId)`.
4. `GET /shop?npcId=…` → `NpcService.byId(npcId).shopItems()` → 구매 목록 조립.

> `NpcType.actionLabels`는 그대로(상점 버튼 노출 여부만 결정). **무엇을 파는지는 NpcType이 아니라 개별 NPC의 `shopItems`가 결정**한다.
> **마법학교/학교는 이번 스펙에서 `shopItems`를 비워둔다**(상점 버튼은 있으나 구매 목록 없음 = 판매만 가능). 힐러도 동일 메커니즘으로 목록(포션)을 가진다. 마법학교/학교 목록은 후속 스펙에서 authoring.

---

## 5. 수리 (대장간 Repair)

> **설계 방침(사용자 확정)**: 수리 전용 팝업/레이아웃을 새로 그리지 않고 **기존 인벤토리 팝업 레이아웃을 재사용**한다. `item-actions`의 **착용/해제 버튼 자리에 `수리` 버튼**만 바꿔 넣는다. 단, **수리 목록에는 내구도가 닳은 장비만**(M17) 보이고, **내구도는 올림 정수(`ceil(current)/max`)로 표시**(M18)한다. 수리는 **무조건 1포인트씩**, **성공확률 95% 고정**이다.

### 5.1 컨트롤러 (`RepairController`, `@RequestMapping("/repair")`)

| 메서드 | 엔드포인트 | 동작 |
|---|---|---|
| `repairPopup(Model)` | `GET /repair` | **내구도 닳은 장비만** 수리 모드로 조립 → fragment (착용/해제 대신 수리 버튼) |
| `repair(ownedItemId, Model)` | `POST /repair` | 수리비 소모 → 95% 성공 시 내구도 +1(`repairBy(1, max)`) → 갱신 fragment |

- 수리 대상: `EquipmentItem`(무기/방어구) **중 `ceil(currentDurability) < maxDurability`인 것만**(M17). 풀내구·포션은 목록에서 제외.
- **장착 중인 장비도 수리 가능**(전투로 닳은 착용 장비 수리가 핵심).
- 골드 부족 시 `InsufficientGoldException` → 수리 미수행(기존 `GlobalExceptionHandler`).

### 5.2 인벤토리 레이아웃 재사용 방식

기존 `fragments/inventory-popup.html`은 아래 구조로 아이템 1행을 렌더한다(그대로 유지):

```
.inventory-item
 ├─ .item-info    : 이름 + [장착중] 배지 + 🔍 상세
 ├─ .item-meta    : 타입 + 내구도(수리화면=ceil(current)/max 정수)
 └─ .item-actions : 수리화면 → [수리] 버튼 (착용/해제/사용 대신)
```

**재사용 옵션 (택1, 개발 시 확정)**
- **(A · 권장) 수리 전용 fragment**: `fragments/repair-popup.html`을 만들되 `.inventory-item`/`.item-meta` **동일 CSS 클래스**를 재사용하고, `.item-actions`만 `수리` 버튼으로 교체. 내구도는 올림 정수로 표시. 공유 인벤토리 fragment를 건드리지 않아 안전.
- **(B) 인벤토리 fragment에 모드 플래그**: 공유 fragment 수정이라 인벤토리 테스트 회귀 위험.

→ **(A) 채택**. 서버가 **닳은 장비만**(`ceil(current) < max`) 뷰에 담고, 수리비·`ceil(current)`를 파생 필드로 함께 전달.

### 5.3 수리비 (판매가 = 1포인트 수리비, M6) · 수리 로직 (1포인트 · 95% 고정, M12/M13/M15)

**수리비 = 판매가(§4.3)를 1포인트당 그대로 사용**

```
수리비/1p = sellValue(owned)          // = §4.3 판매가(기본가 + 인스턴스보너스 × 대상별가중)
```

- `sellValue(owned)`는 §4.3 판매가 그대로. **인챈트로 인스턴스 보너스가 붙으면 판매가↑ → 수리비도 그만큼↑.**

| 장비 | 판매가 = 수리비/1p | 풀수리(0→max) |
|---|---|---|
| 초보자용 한손검 | 50 | 1,000G (max 20) |
| 초보자용 양손검 | 100 | 2,000G (max 20) |
| 초보자용 활 | 110 | 2,200G (max 20) |
| 초보자용 완드 | 50 | 1,000G (max 20) |
| 초보자용 방패/갑옷 | 50 | 1,000G (max 20) |
| 초보자용 투구 | 30 | 600G (max 20) |
| 초보자용 장갑/부츠 | 20 | 400G (max 20) |
| 숏소드 (상점) | 150 | 2,250G (max 15) |
| 롱소드 (상점) | 350 | 5,250G (max 15) |

> **설계 의도(가치 기반 골드 싱크)**: 수리비가 비싼 건 의도다.
> - **미인챈트 초보 장비**: 풀수리비(1,000G) ≫ 판매가(50G)·구매가 → 수리보다 **팔고 재구매가 이득**. 즉 초보 장비는 소모품이고 수리 대상이 아니다.
> - **인챈트 장비**: 인챈트는 리스크(실패 시 스크롤 파괴, 랭크↑일수록 성공률↓)를 뚫어야 완성된다. 그렇게 완성된 장비는 **재현이 어렵고 소유가치가 높으므로** 비싼 수리비를 내고 유지할 가치가 있다. → 고가치 장비에만 수리가 의미를 갖는 자연스러운 골드 싱크.
> - 내구도 감소율 **0.05/턴**(M20, 기존 0.2에서 완화·반영 완료). 1포인트 = 20턴 ≈ 전투 4회. 즉 1포인트 수리(=판매가)가 "전투 약 4회치 마모 복구 비용" → 골드싱크가 감당 가능한 수준.

**수리 로직**

```
POST /repair (ownedItemId)
  owned = 보유 아이템 조회 (EquipmentItem 확인)
  max   = equipItem.maxDurability()
  if (ceil(owned.currentDurability) >= max) → 수리 불필요(목록에서 이미 제외, 요청 무시·무비용)
  cost  = sellValue(owned)                             // 1포인트당 판매가 그대로 (M6)
  progress.spendGold(cost)                             // 시도 시 소모 (M15: 실패해도 환불 없음)
  if (random.nextInt(100) < 95) {                      // 95% 성공 (M13)
      owned.repairBy(1.0, max)                          // 내구도 +1, max 상한 (M12)
      → 로그/응답: "수리 성공! 내구도 +1"
  } else {
      → 로그/응답: "퍼거스가 손을 삐끗했다… 수리 실패!"   // 내구도 변화 없음, 골드는 이미 소모
  }
  characterService.saveTurn(progress)
  → 갱신 fragment 스왑 + 상단바 골드 갱신
```

- **신설 도메인 메서드** `OwnedItem.repairBy(double amount, double max)`:
  ```
  this.currentDurability = Math.min(max, this.currentDurability + amount);
  ```
  → 기존 `repairToMax`는 이번 스펙에서 미사용(1포인트 정책). 관련 임시 주석은 `repairBy` 도입으로 갱신.
- 난수는 테스트 가능하도록 `java.util.Random`을 **주입**(전투/드랍 서비스의 기존 Random 주입 패턴과 동일하게).
- 내구도는 `double`(턴당 감소, 감소율은 M20). `repairBy(1, max)`는 12.4 → 13.4처럼 소수 상태에서도 +1, max 초과분은 잘림.

**내구도 표시(올림)와 수리 로직 분리 — "로직 안 꼬임" (M18)**
- **적용 범위**: 수리 화면 + 인벤토리 화면 공통으로 ceil 정수 표시. **인벤토리(`inventory-popup.html`)는 이미 반영 완료** — `formatDecimal(currentDurability,1,1)` → `formatInteger(T(java.lang.Math).ceil(currentDurability), 1)`.
- **표시**는 `ceil(currentDurability)/max` 정수(예: 실제 12.4 → `13/20`). **판정/저장은 실제 double 값** 그대로 사용 → 두 관심사를 분리하므로 꼬이지 않는다.
  - 수리비: 판매가(§4.3)로 계산 — 내구도와 무관.
  - 수리: `repairBy(1.0, max)`가 실제 double(12.4→13.4)에 적용, max 상한.
  - 목록 노출/수리 필요 판정: **표시와 동일하게 `ceil(current) < max`** 로 통일 → "13/20으로 보이는데 목록에 없다" 같은 불일치 없음.
- 유일한 효과: 실제값이 `(max-1, max]` 구간(예: 19.4, ceil=20)일 때 "가득 참"으로 간주되어 목록에서 빠진다. 남은 1p 미만은 미수리로 남지만 **표시상 풀이고 플레이어에겐 한 번 덜 클릭 = 이득**이라 무해하다.
- 예: 실제 12.4 → 표시 13/20 → [수리] → 13.4(표시 14/20) → … → 19.4(ceil 20 = 풀 취급, 목록에서 사라짐).

### 5.4 수리 화면 레이아웃 (= 인벤토리 팝업, 닳은 장비만 · 버튼 `수리`)

```
┌───────────────────────────────┐
│  대장간 · 수리            [✕]  │   ← 헤더 타이틀 "수리"
├───────────────────────────────┤
│  초보자용 한손검 [장착중] 🔍   │   ← .item-info
│  무기        내구도 13/20      │   ← .item-meta (ceil 정수 표시, 실제 12.4)
│                  [수리] 50G·95%│   ← .item-actions: 수리 버튼(1p 비용=판매가)
│  ─────────────────────────────  │
│  숏소드           🔍           │
│  무기        내구도 9/15       │
│                 [수리] 150G·95%│
│  ─────────────────────────────  │
│  (닳은 장비 없으면) 수리할 장비가 없습니다. │
├───────────────────────────────┤
│  💰 보유 골드: 1,234           │   ← .inventory-footer
└───────────────────────────────┘
```

- **목록엔 내구도 닳은 장비만**(`ceil(current) < max`, M17). 풀내구·포션은 아예 안 보임 → 비활성 버튼 자체가 없음.
- 내구도는 `ceil(current)/max` 정수 표시(M18).
- 버튼 라벨에 `(비용G·95%)`처럼 아이템별 수리비·확률을 노출(선택). 1포인트 수리비 = 해당 아이템 판매가(§5.3).
- 1클릭 = 1포인트 수리 → 풀수리하려면 여러 번 클릭(마비노기 감성). 클릭마다 fragment 스왑으로 내구도·골드 즉시 갱신, 풀이 되면 목록에서 사라짐.
- 모바일 세로 배치(가로 확장 없음). 프론트 `repairItem(ownedItemId)`는 인벤토리 `equipItem`의 fetch→스왑 패턴 복제 + 성공/실패 메시지 노출.

---

## 6. 치료 (힐러집 Heal)

**팝업 없음.** `치료받기` 버튼 → `POST /heal` 1회 호출로 종료.

### 6.1 컨트롤러 (`HealController`, `@RequestMapping("/heal")`)

```
POST /heal
  progress = characterService.loadOrCreateDefault()
  progress.spendGold(100)                                  // 치료비 100골드 고정 (M7)
  vitalMax = statProgression.vitalMaxFor(level, talent)
             + 장비 바이탈 보너스(equippedBonus)             // 상단바 계산과 동일
  progress.fullRecover(vitalMax)                           // HP/MP/스태미나 풀회복 (M8)
  characterService.saveTurn(progress)
  → 성공 응답
```

- 치료비 `100`은 `private static final int HEAL_COST = 100;` 상수로(매직 넘버 금지, `code-style.md`).
- 프론트: 성공 시 상단바(HP/MP/스태미나/골드) 갱신 + `alert("치료되었습니다!")` (M9).
- 골드 부족: `InsufficientGoldException` → 프론트에서 실패 메시지 alert(치료 미수행).
- `vitalMax` 계산은 `PlayScreenViewHelper.buildTopBar`의 계산식과 동일하게 맞춘다(기본 최대치 + 장비 HP/MP/스태미나 보너스). → 회복 후 게이지가 상단바 최대치와 정확히 일치.

### 6.2 프론트 (`myrpg.js`)

```js
function heal() {
    fetch('/heal', { method: 'POST' })
        .then(function (r) {
            if (!r.ok) {
                return r.text().then(function (html) {
                    // GlobalExceptionHandler의 error fragment에서 메시지 추출
                    alert('골드가 부족합니다.');
                    return null;
                });
            }
            return r.text();
        })
        .then(function (ok) {
            if (ok === null) { return; }
            alert('치료되었습니다!');
            refreshTopBar();   // 상단바 HP/MP/스태미나/골드 갱신
        });
}
```

> `refreshTopBar()`는 `usePotion`이 쓰는 상단바 교체 로직(`/` 재요청 → `.top-bar` 스왑)을 함수로 추출해 재사용.

---

## 7. 인챈트 버튼 (마법학교 · placeholder)

- `NpcType.MAGIC_SCHOOL.actionLabels`에 **`"인챈트"`** 추가 → `[상점, 인챈트]`.
- 프론트 `npcAction`에서 `인챈트` → `alert('추후 설계 예정입니다.')`.
- **실제 기능은 인챈트 스크롤 스펙에서 구현**한다(성공확률·랭크·스크롤 소모·접두/접미·인스턴스 보너스). 본 스펙은 **버튼 자리와 alert만** 만든다.

---

## 8. 데이터 변경 (`item.json` / `npc.json`)

### 8.1 `item.json` — buyPrice (사용자 authoring) + 한손검 티어 추가
- **구매가(`buyPrice`)는 사용자가 아이템별로 직접 지정**한다(M4). 지정 없으면 상점 미판매.
- **초보자용 장비는 buyPrice 미지정**(상점 미판매, 드랍/기본지급 전용). 기존 `hp_potion_30`의 `buyPrice: 50`은 유지.
- **한손검 티어(강한 순)**: `beginner_one_hand_sword`(STR+5, 드랍전용) < `short_sword`(STR+8) < `long_sword`(STR+12). 셋 다 `one_handed_sword`(방패 병용 가능).
- **숏소드/롱소드는 상점 판매 + 몬스터 드랍 병행 가능** — buyPrice가 있으므로 드랍으로 얻어 팔아도 판매가 = `buyPrice × 0.5`로 일관(되팔이 차익 없음). 아무 문제 없음.

추가 예시(값은 밸런스 조정 가능):
```json
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
```
- 판매가/수리비: 숏소드 150(=300×0.5), 롱소드 350(=700×0.5). buyPrice 기반이라 STR 보너스는 판매가에 미반영(배타 규칙, §4.3).

### 8.2 `npc.json` — shopItems (§4.5)
- 각 NPC에 `shopItems: [itemId, ...]` optional 추가.
- 확정:
  - **ferghus(티르코네일 대장간) → `["short_sword"]`**
  - **neris(던바튼 대장간) → `["long_sword"]`**
  - **모든 힐러집 → `["hp_potion_30"]`** (dilys=티르코네일, manus=던바튼) — 생명력 30 포션 판매.
- **마법학교/학교는 `shopItems` 비워둠**(이번 스펙 제외 — 상점 버튼은 뜨지만 구매 목록 없이 판매만 가능). 목록은 후속 스펙에서 authoring.

> `ENCHANT` 타입 및 인챈트 관련 데이터는 **추가 금지**(스펙 미확정).

---

## 9. 구현 태스크 (개발 착수 시)

> 각 태스크는 완료 전 `mvn test -pl myrpg` + `mvn clean install -pl myrpg -am` 성공 필수.

1. ~~미결정사항 §0 확정~~ ✅ **전 항목 확정 완료** — 바로 착수 가능.
2. `NpcType.MAGIC_SCHOOL`에 `인챈트` 라벨 추가 + `NpcTypeTest`/`NpcTypeCompletenessPropertyTest` 갱신.
3. `Npc` 레코드에 `shopItems` 추가 + `NpcService.parseNpcNode` optional 파싱 + 파싱 테스트.
4. 말 거는 NPC id 노출: 컨트롤러 `talkingNpcId` 모델 속성 + `center.html` onclick `npcAction(label, npcId)` + `npc.json` shopItems authoring(§8.2).
5. 판매가 계산: `ShopService.sellValueOf` + 단위/PBT 테스트(대상별 가중 CRITICAL=1, 배타 규칙, 인스턴스 보너스 포함).
6. `ShopController`(npcId별 구매목록·구매 검증·판매) + `shop-popup.html` fragment + `@WebMvcTest`(`@MockitoBean`) 슬라이스 테스트.
7. `OwnedItem.repairBy(1, max)` 신설(+단위 테스트: max 상한/소수 내구도) → 수리비 = 판매가(sellValue) 재사용 → `RepairController` + `repair-popup.html`(인벤토리 CSS 재사용, **닳은 장비만 `ceil(current)<max`**, 내구도 `ceil(current)/max` 정수 표시) + 1포인트·95% 수리 로직(주입 Random, 실패 시 골드 소모·환불 없음). 착용 장비 수리 허용.
8. `HealController` + `fullRecover(VitalMax)` 호출 + 골드 부족 예외 테스트.
9. `item.json` buyPrice authoring(§8.1, 초보 장비 제외) — 카탈로그 파싱 테스트 통과 확인.
10. `myrpg.js` `npcAction(label, npcId)` 분기 확장 + `openShop(npcId)/openRepair/heal/refreshTopBar`.
11. ~~기존 인벤토리 내구도 표시를 ceil 정수로 변경~~ ✅ **완료** (`inventory-popup.html`: `formatDecimal(...,1,1)` → `formatInteger(ceil(...),1)`, 빌드 성공 확인).
12. ~~내구도 감소율 `0.2 → 0.05`(M20) — BattleService 상수 변경~~ ✅ **완료**(`DURABILITY_PER_ATTACK=0.05`, `BattleServiceTurnIntegrationTest` 갱신, 빌드 성공). 단 `data-balance-guide.md`(스티어링) 문구는 미변경 — 추후.
13. `style.css` 상점/수리 팝업 스타일(은행·인벤토리 팝업 클래스 재활용).

## 10. 테스트 계획 (`code-style.md` 기준)

| 대상 | 유형 | 어노테이션 |
|---|---|---|
| `ShopService.sellValueOf` (배타 규칙 buyPrice×0.5 / 대상별가중; CRITICAL=1·그외=10; 활=110 검증) | 단위 + PBT | `@ExtendWith(MockitoExtension.class)` / jqwik `@Property` + `Mockito.mock()` |
| `OwnedItem.repairBy(1, max)` (max 상한·소수 내구도) | 단위/PBT | `@ExtendWith(MockitoExtension.class)` / jqwik |
| 수리비 = 판매가(sellValue) 재사용 확인 | 단위/PBT | Mockito / jqwik |
| 수리 1포인트·95% 성공/5% 실패(주입 Random 고정)·시도 시 골드 소모(실패 환불 없음)·골드부족 예외 | 단위 | Mockito(Random stub) |
| 수리 목록 필터(`ceil(current)<max`만 노출)·내구도 `ceil` 표시(12.4→13, 19.4→풀 취급 제외) | 슬라이스/단위 | `@WebMvcTest` + `@MockitoBean` / Mockito |
| 인벤토리 내구도 ceil 정수 표시(12.4→13/max) 회귀 | 슬라이스 | `@WebMvcTest` + `@MockitoBean` (기존 `InventoryControllerTest` 확장) |
| 상점 팝업 세로 레이아웃(상점물건 위/소지품 아래) 렌더 | 슬라이스 | `@WebMvcTest` + `@MockitoBean` |
| `NpcService` shopItems 파싱(있음/없음/빈배열) | 단위 | Mockito |
| 상점 npcId별 구매 목록 분리 + 목록 외 아이템 구매 거부 | 슬라이스/단위 | `@WebMvcTest` + `@MockitoBean` / Mockito |
| 구매/판매 골드 증감·부족 예외 | 단위 | Mockito |
| `HealController` (100골드 소모·풀회복·부족 예외) | 슬라이스 | `@WebMvcTest` + `@MockitoBean` |
| `ShopController` / `RepairController` fragment | 슬라이스 | `@WebMvcTest` + `@MockitoBean` |
| NpcType 인챈트 라벨 | 단위/PBT | 기존 `NpcTypeTest` 확장 |

---

## 부록. 완료 시 로드맵 반영
- `todo.md` 7순위 행 상세문서 경로/완료 표시 갱신.
- 개발 완료 후 본 문서 삭제(로드맵 규칙).
- 수리는 1포인트 정책이라 `OwnedItem.repairToMax`를 쓰지 않고 `repairBy(1, max)`를 신설한다. `repairToMax`의 "7순위 대장간 스펙에서 호출하여 확정" 임시 주석은 (미사용 확정 시) 정리 대상 검토.
