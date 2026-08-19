# Requirements Document

## Introduction

본 스펙(010)은 `myrpg` 모듈(`com.myapps.web.myrpg`)에 **NPC 행동 실기능 — 상점(구매/판매)·수리(대장간)·치료(힐러집)** 를 추가한다. 스펙 006(`gold-item-inventory`)이 구축한 골드(`CharacterProgress.gold`·`gainGold`·`spendGold`·`InsufficientGoldException`), 아이템 카탈로그(`ItemCatalogService`·`Item`·`EquipmentItem`·`PotionItem`·`buyPrice`), 보유 아이템(`OwnedItem`·`StorageKind`·`repairToMax`·`increaseQuantity`/`decreaseQuantity`), 착용 규칙(`InventoryService`·`EquipConflictException`·`InventoryFullException`), 은행(`Bank`·`BankController`·`bank-popup.html`) 위에서 동작한다. 상세 설계 배경과 확정 사항은 `docs/npc-actions-system.md`(§0 미결정사항 M1~M21 전부 확정)를 근거로 한다.

**해결하려는 문제**: 006에서 골드·아이템·인벤토리·은행은 완성됐지만, NPC의 `상점`·`수리`·`치료받기` 버튼은 클릭 시 "구현 예정입니다" alert만 표시할 뿐 실제 기능이 없다. 아이템 판매가 계산식, 상점 구매/판매, 대장간 수리(내구도), 힐러 치료를 실제로 구현하여 006이 정의만 해둔 가격 모델(`Sell_Value`)·내구도 수리(`repairToMax`)를 실동작시킨다.

핵심 방향은 006의 "은행 팝업(`BankController` + `bank-popup.html`) 패턴"을 상점·수리 화면으로 복제하고, 006이 이연한 판매가·수리비 정책을 확정 수치로 구현하는 것이다.

- **상점(Shop)**: NPC별 판매 목록(`shopItems`)에서 아이템 구매 + 보유 아이템 판매. 판매가 계산식 확정. NPC마다 판매 목록이 다르다(던바튼 대장간 ≠ 티르코네일 대장간).
- **수리(Repair, 대장간)**: 006 인벤토리 팝업 레이아웃을 재사용해 **내구도가 닳은 장비만** 표시하고, **1포인트씩·95% 성공확률**로 수리한다. 수리비는 판매가와 동일.
- **치료(Heal, 힐러집)**: 팝업 없이 `치료받기` 버튼 → 100골드 소모 → HP/MP/스태미나 풀회복 → alert.
- **인챈트 버튼(placeholder)**: 마법학교에 `인챈트` 버튼만 추가, 클릭 시 "추후 설계 예정입니다" alert(실기능은 후속 인챈트 스크롤 스펙).

이번 스펙의 범위:

1. **판매가 계산식** — `기본가 + 인스턴스보너스 × 대상별 가중치`. 기본가는 배타 규칙(`buyPrice` 있으면 `buyPrice × 0.5`, 없으면 `카탈로그 보너스 × 가중치`). 인챈트 미구현이므로 현재 인스턴스보너스 항은 0.
2. **NPC별 상점 판매 목록** — `Npc` 레코드에 `shopItems` optional 필드 추가, `npc.json` authoring, 대화 중인 NPC id(`talkingNpcId`) 화면 노출.
3. **상점 팝업** — 구매(NPC `shopItems`) + 판매(내 인벤토리). 모바일 세로 배치. 은행 팝업 패턴 복제.
4. **수리** — `OwnedItem.repairBy(1, max)` 신설, 수리 팝업(인벤토리 CSS 재사용·닳은 장비만·내구도 올림 정수 표시), 1포인트·95%·실패 시 골드 소모(환불 없음).
5. **치료** — 100골드 소모 후 풀회복, 상단바 갱신 + alert.
6. **인챈트 버튼 placeholder** — `NpcType.MAGIC_SCHOOL`에 `인챈트` 라벨 추가 + alert.
7. **데이터** — `item.json`에 `short_sword`·`long_sword`(buyPrice 지정) 추가, `npc.json`에 NPC별 `shopItems` authoring.
8. **NPC 행동 라우팅** — `myrpg.js` `npcAction(label, npcId)` 분기 확장.

보관/은행(006 완료)은 다루지 않는다. 인챈트 실제 로직(성공확률·스크롤 소모·인스턴스 보너스), 촌장 퀘스트, 상점 수량 입력 UI(1클릭=1개 고정)는 범위 밖이다. 내구도 감소율 `0.05/턴`(M20)과 인벤토리 내구도 올림 정수 표시(M18)는 이미 코드에 반영 완료이므로 본 스펙은 이를 전제한다.

## Glossary

### 기존(006~009) 재사용 용어

- **Myrpg_Web_Module**: `com.myapps.web.myrpg` 기본 패키지의 Spring Boot 4.0 Web 모듈.
- **Character_Progress**: 유일한 캐릭터 진행 엔티티. `gold`(소지금)·`spendGold`·`gainGold`·`fullRecover(VitalMax)`를 보유한다.
- **Gold**: 캐릭터 소지금(`Character_Progress.gold`, `long`).
- **Insufficient_Gold_Exception**: 소지금 부족으로 소모·구매·수리·치료가 거부될 때 던지는 비즈니스 예외(006 신설).
- **Item / Item_Catalog / Item_Catalog_Service**: `classpath:data/item.json`에서 기동 시 로드되는 아이템 카탈로그와 서비스. `Item`은 sealed interface(`PotionItem`·`EquipmentItem` permits), 공통으로 `id`·`name`·`type`·`buyPrice`(optional) 보유.
- **Item_Id**: 아이템 정체성 키(예: `"short_sword"`). `OwnedItem`·`shopItems`가 참조하는 단일 소스.
- **Item_Type**: 아이템 타입 enum(`POTION`/`WEAPON`/`ARMOR`).
- **Potion_Item**: 포션 정의(record). `healHp` 보유.
- **Equipment_Item**: 장비 정의(record). `kind`(Equipment_Kind)·`bonuses`(Equip_Bonus 목록)·`maxDurability` 보유.
- **Equip_Bonus**: 장비 보너스 한 줄(record). `(BonusTarget target, int amount)`.
- **BonusTarget**: 보너스 대상(STR/DEX/INT/CRITICAL/DEF/HP/MP/STAMINA). Critical은 0.1% 단위.
- **Buy_Price**: 상점 구매가(카탈로그, optional). `null`이면 상점 미판매(드랍 전용).
- **Owned_Item**: 캐릭터 보유 아이템 인스턴스 엔티티. `itemId`·`quantity`·`storage`·`equipped`·`currentDurability`. `increaseQuantity`/`decreaseQuantity`·`equip`/`unequip`·`repairToMax`·`reduceDurability` 보유.
- **Storage_Kind**: 보유 아이템 저장 위치 enum(`INVENTORY`/`BANK`).
- **Equipped**: `OwnedItem`의 장착 여부(`storage=INVENTORY` 장비만 true 가능).
- **Inventory_Service**: 인벤토리·은행 도메인 조작 서비스. 아이템 획득·착용/해제·맡기기/찾기·상세 조립(`describe`) 등을 담당한다.
- **Inventory_Full_Exception**: 저장소 용량(30) 초과로 아이템 획득/이동이 거부될 때 던지는 예외(006 신설).
- **Equip_Conflict_Exception**: 착용 충돌로 착용이 거부될 때 던지는 예외(006 신설). 장착 중 아이템 이동 거부에도 재사용.
- **Bank / Bank_Controller / Bank_Popup**: 통합 금고 엔티티 및 은행 팝업 컨트롤러/프래그먼트(`bank-popup.html`). 상점/수리 팝업이 GET=팝업 fragment, POST=조작 후 갱신 fragment 스왑 패턴을 복제한다.
- **Npc / Npc_Type**: NPC와 그 타입 enum. `Npc`는 현재 `id`·`name`·`type`(NpcType)·`nodeId`·`personality`·`lines`를 보유하는 record. `NpcType`은 `actionLabels`(행동 버튼 라벨의 단일 소스)를 보유(예: `BLACKSMITH=["상점","수리"]`).
- **Npc_Service**: `classpath:data/npc.json`을 기동 시 파싱·검증하는 서비스. `parseNpcNode`·`byId`·`byNode`·`parseStringList` 보유.
- **Play_Screen_View_Helper**: 플레이 화면 뷰 조립기. `buildNpcActions`가 `NpcType.actionLabels`를 행동 버튼으로 변환한다(라벨만 늘면 버튼 자동 추가).
- **Vital_Max / Stat_Progression**: HP/MP/Stamina 최대치 VO와 그 계산 정책. `vitalMaxFor(level, talent)` + 장비 바이탈 보너스로 상단바 최대치를 산출한다.
- **Global_Exception_Handler**: 비즈니스 예외를 화면 error fragment로 변환하는 핸들러(006 존재). 골드 부족·착용 충돌·용량 초과를 클라이언트 alert로 안내한다.
- **Action_Log**: 화면 하단 활동 로그. 구매/판매/수리 결과를 기록한다.
- **Encountered_Monster_Id**: 몬스터 조우 시 화면(`center.html`)에 노출되어 onclick에 주입되는 대상 id. 본 스펙 `Talking_Npc_Id`가 이 패턴을 복제한다.
- **Inventory_Popup**: 006의 인벤토리 팝업 프래그먼트(`inventory-popup.html`). 내구도를 이미 `ceil(current)/max` 올림 정수로 표시(M18 반영 완료). 수리 화면이 이 레이아웃/CSS를 재사용한다.

### 본 스펙(010) 신규 용어

- **Shop_Items**: NPC별 상점 판매 목록. `Npc` 레코드의 신규 optional 필드 `List<String> shopItems`(Item_Id 참조). 재고 수량 개념 없음(무제한 판매되는 목록만 NPC별로 다름). 없거나 비면 빈 목록(구매 불가, 판매만 가능).
- **Talking_Npc_Id**: 현재 대화 중인 NPC의 id. 컨트롤러가 모델 속성으로 심고 `center.html` NPC 버튼 onclick에 주입되어 `npcAction(label, npcId)`로 전달된다(Encountered_Monster_Id 패턴 복제).
- **Sell_Value**: 판매가. 저장하지 않고 매번 계산한다. `Base_Value + (인스턴스보너스 × 대상별 가중치)`. 인챈트 미구현 현재는 인스턴스보너스 항 = 0이므로 `Sell_Value = Base_Value`.
- **Base_Value**: 인챈트 전 아이템의 고유 판매 가치(JSON 저장 필드가 아닌 계산값). 배타 규칙: `buyPrice`가 있으면 `round(buyPrice × Sell_Ratio)`, 없으면 `Σ(카탈로그 보너스 amount × weightOf(target))`.
- **Sell_Ratio**: 구매가 대비 판매가 비율. `0.5`(M3).
- **Weight**: STR/DEX/INT/DEF/HP/MP/STAMINA 보너스의 판매가 가중치. `10`(M1).
- **Critical_Weight**: CRITICAL 보너스의 가중치. `1`(M1). CRITICAL amount가 0.1% 단위(10=1%)라 과대평가 방지를 위해 그 외 대상의 1/10.
- **Weight_Of**: 보너스 대상별 가중치 함수. `CRITICAL → Critical_Weight(1)`, 그 외 → `Weight(10)`.
- **Repair_Cost**: 1포인트당 수리비. `Sell_Value`를 그대로 사용한다(M6). 인챈트로 인스턴스 보너스가 붙으면 판매가↑ → 수리비↑.
- **Repair_By**: `OwnedItem`의 신규 도메인 메서드 `repairBy(double amount, double max)`. `currentDurability = min(max, currentDurability + amount)`. 1포인트 정책이라 기존 `repairToMax`를 대체 사용한다.
- **Repair_Success_Rate**: 수리 성공 확률. `95%` 고정(M13). 실패(5%) 시 내구도 증가 없음.
- **Repairable_Item**: 수리 목록에 노출되는 대상. `EquipmentItem`(무기/방어구) 중 `ceil(currentDurability) < maxDurability`인 것만(M17). 풀내구·포션은 제외. 장착 중인 장비도 수리 가능.
- **Durability_Display**: 내구도 표시값. 실제 double을 `ceil(current)/max` 올림 정수로 표시(M18). 판정/저장은 실제 double 값을 그대로 사용(표시와 로직 분리).
- **Shop_Controller / Shop_Popup**: 상점 컨트롤러(`@RequestMapping("/shop")`)와 팝업 프래그먼트(`shop-popup.html`). 은행 패턴 복제.
- **Repair_Controller / Repair_Popup**: 수리 컨트롤러(`@RequestMapping("/repair")`)와 팝업 프래그먼트(`repair-popup.html`). 인벤토리 CSS 재사용.
- **Heal_Controller**: 치료 컨트롤러(`@RequestMapping("/heal")`). 팝업 없이 `POST /heal` 1회로 종료.
- **Heal_Cost**: 치료비. `100`골드 고정(M7). 매직넘버 금지 상수(`HEAL_COST`).
- **Shop_Service**: 판매가 계산·상점 목록 조립·구매/판매를 담당하는 신규 애플리케이션 서비스(또는 `InventoryService` 확장). `sellValueOf(OwnedItem)`·`shopBuyList(npcId)`·`buy(...)`·`sell(...)`.

## Requirements

### Requirement 1: 아이템 판매가 계산식

**User Story:** 개발자로서, 상점 판매·수리비의 기준이 되는 판매가를 저장 없이 아이템 인스턴스에서 일관되게 계산하고 싶다.

#### Acceptance Criteria

1. THE Shop_Service SHALL 주어진 Owned_Item의 Sell_Value를 `Base_Value + (인스턴스보너스 × Weight_Of)`로 산출한다.
2. WHERE 아이템에 Buy_Price가 있으면(상점 판매 아이템), THE Base_Value SHALL `round(buyPrice × Sell_Ratio)`(Sell_Ratio=0.5)로 산출한다.
3. WHERE 아이템에 Buy_Price가 없으면(드랍 전용), THE Base_Value SHALL 카탈로그 보너스 각 항을 `amount × Weight_Of(target)`로 합산하여 산출한다.
4. THE Weight_Of SHALL CRITICAL 대상은 Critical_Weight(1), 그 외(STR/DEX/INT/DEF/HP/MP/STAMINA)는 Weight(10)로 적용한다.
5. WHERE Base_Value 규칙이 Buy_Price 유무로 **배타**이면, THE Shop_Service SHALL Buy_Price가 있을 때 카탈로그 보너스를 Base_Value에 반영하지 않는다(둘을 동시에 더하지 않는다).
6. WHERE 인챈트가 미구현이면(현재), THE 인스턴스보너스 항 SHALL 0이며 따라서 Sell_Value = Base_Value이다.
7. WHERE 인챈트로 Owned_Item에 인스턴스 보너스가 붙으면(향후), THE Sell_Value SHALL Base_Value에 `Σ(인챈트 amount × Weight_Of(target))`를 항상 더한다(상점템·드랍템 무관).
8. THE Shop_Service SHALL 다음 대표값을 만족한다: 생명력 30 포션(buyPrice 50) → 25, 초보자용 한손검(STR+5, 드랍) → 50, 초보자용 활(DEX+10·CRITICAL+10, 드랍) → 110, 숏소드(buyPrice 300) → 150, 롱소드(buyPrice 700) → 350.
9. THE Sell_Value SHALL 어디에도 영속 저장되지 않고 조회 시점에 매번 계산된다.

### Requirement 2: NPC별 상점 판매 목록 (shopItems)

**User Story:** 플레이어로서, 같은 `상점` 버튼이라도 어느 NPC와 대화 중이냐에 따라 서로 다른 물건을 사고 싶다.

#### Acceptance Criteria

1. THE Npc SHALL 신규 optional 필드 Shop_Items(`List<String> shopItems`, Item_Id 목록)를 보유한다(기존 `id`·`name`·`type`·`nodeId`·`personality`·`lines`에 추가).
2. WHEN Npc_Service가 NPC 노드를 파싱하면, THE Npc_Service SHALL `shopItems`를 optional로 읽고(기존 `parseStringList` 재사용), 없거나 배열이 아니면 빈 목록(`List.of()`)을 사용한다.
3. THE Npc_Service SHALL `shopItems` 파싱이 기존 필수 필드·중복 id 검증을 회귀 없이 유지한다.
4. THE `npc.json` SHALL ferghus(티르코네일 대장간) → `["short_sword"]`, neris(던바튼 대장간) → `["long_sword"]`, 모든 힐러집(dilys·manus) → `["hp_potion_30"]`로 authoring한다.
5. THE 마법학교·학교 NPC SHALL `shopItems`를 비워둔다(상점 버튼은 있으나 구매 목록 없이 판매만 가능; 후속 스펙에서 authoring).
6. THE `NpcType.actionLabels` SHALL 무엇을 파는지를 결정하지 않으며(상점 버튼 노출 여부만 결정), 판매 목록은 개별 NPC의 Shop_Items가 결정한다.
7. THE Shop_Service SHALL `shopBuyList(npcId)`로 해당 NPC의 Shop_Items 중 Buy_Price가 있는 아이템만 구매 목록으로 조립한다.

### Requirement 3: 대화 중인 NPC id 노출 (talkingNpcId)

**User Story:** 개발자로서, 상점을 열 때 어느 NPC와 대화 중인지 서버가 알 수 있도록 NPC id를 화면에서 전달하고 싶다.

#### Acceptance Criteria

1. WHEN NPC와 대화하는 화면이 렌더되면, THE Myrpg_Web_Module SHALL Talking_Npc_Id를 모델 속성으로 심는다(Encountered_Monster_Id와 동일 패턴).
2. THE `center.html`의 NPC 행동 버튼 onclick SHALL Talking_Npc_Id를 함께 넘겨 `npcAction(label, npcId)`를 호출한다.
3. WHEN 상점을 열면(`GET /shop?npcId=…`), THE Shop_Controller SHALL 전달받은 npcId로 `NpcService.byId(npcId)`를 조회하여 그 NPC의 Shop_Items로 구매 목록을 조립한다.
4. IF npcId가 미존재하거나 해당 NPC가 없으면, THEN THE Shop_Controller SHALL 구매 목록을 빈 목록으로 처리한다(판매만 가능).

### Requirement 4: 상점 구매

**User Story:** 플레이어로서, 상점에서 골드로 아이템을 살 수 있기를 원한다.

#### Acceptance Criteria

1. WHEN 구매가 요청되면(`POST /shop/buy`, npcId·itemId), THE Shop_Service SHALL itemId가 해당 NPC Shop_Items에 포함되고 Buy_Price가 `null`이 아닌지 검증한다.
2. IF itemId가 해당 NPC Shop_Items에 없거나 Buy_Price가 `null`이면, THEN THE Shop_Service SHALL 구매를 거부한다(위변조 방지).
3. WHEN 구매가 진행되면, THE Shop_Service SHALL `spendGold(buyPrice)` 후 Inventory_Service로 아이템을 획득시킨다(포션은 스택 누적, 장비는 개별 인스턴스).
4. IF 보유 Gold가 Buy_Price 미만이면, THEN THE Myrpg_Web_Module SHALL Insufficient_Gold_Exception으로 거부하고 소지금·인벤토리를 변경하지 않는다.
5. IF 인벤토리 용량(30)을 초과하면(신규 스택 추가 시), THEN THE Myrpg_Web_Module SHALL Inventory_Full_Exception으로 거부한다.
6. THE 구매 SHALL 1클릭 = 1개 단위이며(M21), 수량 입력 UI를 제공하지 않는다.
7. WHEN 구매가 완료되면, THE Shop_Controller SHALL 갱신된 상점 fragment를 반환하고 상단바 골드를 갱신하며 Action_Log에 기록한다.

### Requirement 5: 상점 판매

**User Story:** 플레이어로서, 보유 아이템을 상점에 팔아 골드로 바꾸고 싶다.

#### Acceptance Criteria

1. WHEN 판매가 요청되면(`POST /shop/sell`, ownedItemId), THE Shop_Service SHALL 해당 Owned_Item의 Sell_Value(Requirement 1)를 계산한다.
2. WHEN 판매가 진행되면, THE Shop_Service SHALL 인벤토리에서 아이템을 차감(`decreaseQuantity(1)`, 0이 되면 행 제거)하고 `gainGold(sellValue)`를 수행한다.
3. IF 판매 대상이 장착 중(Equipped)이면, THEN THE Myrpg_Web_Module SHALL 판매를 거부한다(은행 맡기기와 동일 정책, Equip_Conflict_Exception — "장착을 해제한 후 판매할 수 있습니다.").
4. THE 판매 SHALL 1클릭 = 1개 단위이며(M21), 포션 스택도 1개씩 판매한다.
5. THE 판매 대상 SHALL `storage=INVENTORY` 아이템에 한하며, 은행 보관 아이템은 판매 대상이 아니다.
6. WHEN 판매가 완료되면, THE Shop_Controller SHALL 갱신된 상점 fragment를 반환하고 상단바 골드를 갱신하며 Action_Log에 기록한다.

### Requirement 6: 상점 팝업 레이아웃

**User Story:** 플레이어로서, 모바일 화면에서 상점 물건과 내 소지품, 보유 골드를 한 팝업에서 관리하고 싶다.

#### Acceptance Criteria

1. WHEN 상점을 열면(`GET /shop?npcId=…`), THE Shop_Controller SHALL 구매 목록(NPC Shop_Items) + 판매 목록(내 인벤토리) + 보유 Gold를 담은 팝업 fragment(`shop-popup.html`)를 반환한다.
2. THE Shop_Popup SHALL 모바일 세로 배치로 상점 물건(위) / 내 소지품(아래) / 보유 골드(하단) 순서로 구성하며, 가로로 확장하지 않는다(은행 팝업 골격 참고).
3. THE 상점 물건 섹션 SHALL 해당 NPC Shop_Items만 표시하고 각 행에 Buy_Price 표기와 `[구매]` 버튼, 상세보기(🔍)를 제공한다.
4. THE 내 소지품 섹션 SHALL 인벤토리 아이템을 표시하고 각 행에 Sell_Value(계산값) 표기와 `[판매]` 버튼, 상세보기(🔍)를 제공한다.
5. WHERE 소지품 아이템이 장착 중이면, THE 내 소지품 섹션 SHALL `[장착중]` 배지를 표시하고 판매를 거부한다.
6. THE Shop_Popup SHALL 상세보기(🔍)에 006의 아이템 상세 모달(`openItemDetail`·`data-detail`)을 재사용한다.
7. WHEN 구매/판매가 수행되면, THE Shop_Popup SHALL 팝업 fragment 스왑과 상단바 골드 갱신으로 화면을 재렌더한다(은행 갱신 패턴 참고).

### Requirement 7: 수리 도메인 메서드 (repairBy)

**User Story:** 개발자로서, 장비 내구도를 1포인트씩 최대치 상한 안에서 회복시키는 도메인 메서드를 원한다.

#### Acceptance Criteria

1. THE Owned_Item SHALL 신규 메서드 Repair_By(`repairBy(double amount, double max)`)를 제공하며 `currentDurability = min(max, currentDurability + amount)`로 동작한다.
2. WHERE 현재 내구도가 소수(예: 12.4)이면, THE Repair_By(1.0, max) SHALL 실제 double에 `+1`을 적용한다(예: 12.4 → 13.4).
3. WHERE `currentDurability + amount`가 max를 초과하면, THE Repair_By SHALL 결과를 max로 제한한다(초과분 잘림).
4. THE 본 스펙 SHALL 1포인트 정책이므로 기존 `repairToMax`를 사용하지 않고 Repair_By를 사용한다(`repairToMax` 미사용 확정 시 임시 주석 정리 검토).

### Requirement 8: 수리 목록 및 내구도 표시

**User Story:** 플레이어로서, 대장간에서 내구도가 닳은 장비만 골라 보고 그 내구도를 명확한 정수로 확인하고 싶다.

#### Acceptance Criteria

1. WHEN 수리 화면을 열면(`GET /repair`), THE Repair_Controller SHALL Repairable_Item(EquipmentItem 중 `ceil(currentDurability) < maxDurability`인 것만)을 수리 목록으로 조립한다.
2. THE 수리 목록 SHALL 풀내구 장비(`ceil(current) >= max`)와 포션을 제외한다(M17).
3. THE 수리 목록 SHALL 장착 중인 장비도 포함한다(전투로 닳은 착용 장비 수리가 핵심).
4. THE 내구도 표시(Durability_Display) SHALL 모든 화면 공통으로 `ceil(currentDurability)/max` 올림 정수로 표시한다(예: 실제 12.4 → 13/20)(M18).
5. THE 수리 필요 여부 판정 SHALL 표시와 동일하게 `ceil(current) < max` 기준으로 통일한다(표시-목록 불일치 방지).
6. WHERE 실제값이 `(max-1, max]` 구간(예: 19.4, ceil=20)이면, THE 수리 목록 SHALL 이를 "가득 참"으로 간주하여 제외한다(남은 1p 미만 미수리는 표시상 풀이라 무해).
7. WHERE 닳은 장비가 없으면, THE Repair_Popup SHALL "수리할 장비가 없습니다" 안내를 표시한다.

### Requirement 9: 수리 실행 (1포인트 · 95%)

**User Story:** 플레이어로서, 대장간에서 골드를 내고 장비를 한 포인트씩 수리하되, 가끔 실패도 감수하고 싶다.

#### Acceptance Criteria

1. WHEN 수리가 요청되면(`POST /repair`, ownedItemId), THE Repair_Controller SHALL 대상 Owned_Item을 조회하고 EquipmentItem인지, `ceil(currentDurability) < maxDurability`인지 확인한다.
2. WHERE 대상이 이미 가득 찬(수리 불필요) 상태이면, THE Repair_Controller SHALL 수리를 수행하지 않고 비용도 청구하지 않는다.
3. WHEN 수리가 진행되면, THE Repair_Controller SHALL Repair_Cost(= 해당 아이템 Sell_Value, 1포인트당)를 `spendGold`로 시도 시점에 소모한다(M6).
4. WHERE 수리 성공(Repair_Success_Rate 95% 판정)이면, THE Owned_Item SHALL `repairBy(1.0, max)`로 내구도를 +1하고 성공 로그를 남긴다.
5. WHERE 수리 실패(5%)이면, THE Owned_Item SHALL 내구도를 변경하지 않으며 소모된 골드는 환불하지 않는다(M15) — 실패 플레이버 로그를 남긴다.
6. IF 보유 Gold가 Repair_Cost 미만이면, THEN THE Myrpg_Web_Module SHALL Insufficient_Gold_Exception으로 거부하고 수리를 수행하지 않는다.
7. THE 수리 성공/실패 난수 SHALL 주입 `java.util.Random`으로 판정하여 시드 고정 테스트가 가능해야 한다(전투/드랍 서비스의 Random 주입 패턴과 동일).
8. THE 수리 SHALL 1클릭 = 1포인트이며, 풀수리하려면 여러 번 클릭한다(클릭마다 fragment 스왑으로 내구도·골드 즉시 갱신, 풀이 되면 목록에서 제외).

### Requirement 10: 수리 팝업 레이아웃 (인벤토리 재사용)

**User Story:** 플레이어로서, 익숙한 인벤토리 레이아웃 그대로 대장간에서 장비를 수리하고 싶다.

#### Acceptance Criteria

1. THE Repair_Popup(`repair-popup.html`) SHALL 인벤토리 팝업(`inventory-popup.html`)의 `.inventory-item`/`.item-info`/`.item-meta` CSS 클래스를 재사용한다.
2. THE Repair_Popup SHALL `.item-actions`의 착용/해제/사용 버튼 자리에 `[수리]` 버튼을 배치한다.
3. THE `[수리]` 버튼 SHALL 해당 아이템의 Repair_Cost(1포인트 비용 = Sell_Value)와 Repair_Success_Rate(95%)를 라벨에 노출할 수 있다(예: `[수리] 50G·95%`).
4. THE Repair_Popup SHALL 하단에 보유 Gold를 표시한다(`.inventory-footer` 재사용).
5. THE Repair_Popup SHALL 공유 인벤토리 fragment를 수정하지 않고 전용 fragment로 분리하여 인벤토리 회귀 위험을 없앤다.
6. THE Repair_Popup SHALL 모바일 세로 배치로 구성하며 가로로 확장하지 않는다.

### Requirement 11: 치료 (힐러집)

**User Story:** 플레이어로서, 힐러집에서 골드를 내고 HP/MP/스태미나를 한 번에 완전 회복하고 싶다.

#### Acceptance Criteria

1. WHEN `치료받기` 버튼을 누르면, THE Myrpg_Web_Module SHALL 팝업 없이 `POST /heal`을 1회 호출한다(M9).
2. WHEN 치료가 진행되면, THE Heal_Controller SHALL Heal_Cost(100골드 고정)를 `spendGold`로 소모한다(M7).
3. WHEN 치료가 진행되면, THE Heal_Controller SHALL `vitalMaxFor(level, talent)` + 장비 바이탈 보너스로 산출한 Vital_Max로 `fullRecover(VitalMax)`를 호출하여 HP/MP/스태미나를 모두 풀회복한다(M8).
4. THE Vital_Max 계산 SHALL `PlayScreenViewHelper.buildTopBar`의 상단바 최대치 계산식과 동일하여, 회복 후 게이지가 상단바 최대치와 정확히 일치한다.
5. IF 보유 Gold가 Heal_Cost 미만이면, THEN THE Myrpg_Web_Module SHALL Insufficient_Gold_Exception으로 거부하고 회복을 수행하지 않으며, 클라이언트는 실패 메시지를 alert한다.
6. WHEN 치료가 성공하면, THE Myrpg_Web_Module SHALL 상단바(HP/MP/스태미나/골드)를 갱신하고 `alert("치료되었습니다!")`를 표시한다(M9).
7. THE Heal_Cost SHALL 매직넘버 없이 상수(`private static final int HEAL_COST = 100;`)로 정의한다(code-style).

### Requirement 12: 인챈트 버튼 (마법학교 · placeholder)

**User Story:** 플레이어로서, 마법학교에 인챈트 기능이 곧 온다는 것을 버튼으로 미리 확인하고 싶다.

#### Acceptance Criteria

1. THE `NpcType.MAGIC_SCHOOL.actionLabels` SHALL `["상점", "인챈트"]`로 `인챈트` 라벨을 추가한다.
2. WHEN `인챈트` 버튼을 누르면, THE Myrpg_Web_Module SHALL `alert("추후 설계 예정입니다.")`만 표시한다(서버 엔드포인트 없음).
3. THE 본 스펙 SHALL 인챈트 실기능(성공확률·랭크·스크롤 소모·인스턴스 보너스)을 구현하지 않는다(후속 인챈트 스크롤 스펙으로 이연).
4. THE `NpcType` 라벨 추가 SHALL 기존 `NpcTypeTest`·`NpcTypeCompletenessPropertyTest`를 갱신하여 회귀 없이 통과한다.

### Requirement 13: NPC 행동 라우팅

**User Story:** 플레이어로서, NPC 행동 버튼을 누르면 각 기능(상점/수리/치료/인챈트)이 올바르게 동작하기를 원한다.

#### Acceptance Criteria

1. THE `myrpg.js`의 `npcAction(label, npcId)` SHALL 라벨별로 분기한다: `은행`→기존 `openBank()`, `상점`→`openShop(npcId)`, `수리`→`openRepair()`, `치료받기`→`heal()`, `인챈트`→"추후 설계 예정입니다" alert, 그 외→기존 "구현 예정입니다" alert.
2. WHEN `상점`이 선택되면, THE `openShop(npcId)` SHALL `GET /shop?npcId=…`로 상점 팝업을 연다.
3. WHEN `수리`가 선택되면, THE `openRepair()` SHALL `GET /repair`로 수리 팝업을 연다.
4. WHEN `치료받기`가 선택되면, THE `heal()` SHALL `POST /heal`을 호출하고, 성공 시 상단바 갱신 후 alert, 실패(골드 부족) 시 실패 메시지 alert를 표시한다.
5. THE 상단바 갱신 로직(`refreshTopBar`) SHALL 기존 포션 사용(`usePotion`)의 상단바 교체 로직을 함수로 추출하여 재사용한다.

### Requirement 14: 상점 데이터 (item.json)

**User Story:** 플레이어로서, 대장간에서 살 수 있는 검(숏소드·롱소드)이 존재하기를 원한다.

#### Acceptance Criteria

1. THE `item.json` SHALL `short_sword`(숏소드, weapon, one_handed_sword, STR+8, maxDurability 15, buyPrice 300)를 추가한다.
2. THE `item.json` SHALL `long_sword`(롱소드, weapon, one_handed_sword, STR+12, maxDurability 15, buyPrice 700)를 추가한다.
3. THE 숏소드·롱소드 SHALL 한손검(방패 병용 가능)이며, 한손검 티어(강한 순)는 `beginner_one_hand_sword`(STR+5, 드랍전용) < `short_sword`(STR+8) < `long_sword`(STR+12)를 만족한다.
4. THE 초보자용 장비 SHALL buyPrice를 지정하지 않는다(상점 미판매, 드랍/기본지급 전용). 기존 `hp_potion_30`의 `buyPrice: 50`은 유지한다.
5. THE 숏소드·롱소드 SHALL buyPrice 기반이므로 Base_Value가 `buyPrice × 0.5`이며(숏소드 150·롱소드 350), STR 보너스는 Base_Value에 반영되지 않는다(배타 규칙, Requirement 1-5).
6. THE `item.json` SHALL 기존 카탈로그 파싱 테스트를 회귀 없이 통과한다(`ENCHANT` 타입·인챈트 데이터 추가 금지).

### Requirement 15: 예외 처리 및 하위 호환

**User Story:** 개발자로서, 상점/수리/치료가 006의 예외 체계·테스트와 충돌 없이 동작하기를 원한다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL 골드 부족·용량 초과·착용 충돌을 006의 기존 예외(Insufficient_Gold_Exception·Inventory_Full_Exception·Equip_Conflict_Exception)와 Global_Exception_Handler로 처리하며 `RuntimeException`을 직접 던지지 않는다(code-style).
2. WHEN 구매/판매/수리/치료가 거부되면, THE Myrpg_Web_Module SHALL 해당 동작을 수행하지 않고 소지금·인벤토리·내구도 상태를 변경하지 않으며 클라이언트가 alert로 안내한다.
3. THE Npc 레코드 확장(shopItems 추가) SHALL 기존 `Npc`·`NpcService`를 참조하는 코드·테스트를 회귀 없이 갱신한다.
4. THE 정적 리소스 보존 테스트 SHALL 신규 fragment(`shop-popup.html`·`repair-popup.html`) 및 `center.html`·`myrpg.js` 변경 기대값을 반영하여 통과한다.
5. THE 내구도 감소율(0.05/턴, M20)과 인벤토리 내구도 올림 정수 표시(M18) SHALL 이미 코드에 반영되어 있으므로 본 스펙이 이를 전제하며 재변경하지 않는다.
6. THE Myrpg_Web_Module SHALL 미사용 import/변수 제거·매직넘버 상수화 등 code-style 정리 항목을 준수한다.
