# 골드 · 아이템 · 인벤토리 · 장비 시스템 (통합 상세 설계)

> 대상 우선순위: **4순위 (통합)** — 재화(골드)/은행 + 아이템/인벤토리/장비를 **하나의 흐름으로 개발**한다.
> 연관: 6순위(전투)에서 장비 스탯·포션이 실제 전투에 쓰인다. 7순위(NPC 행동)에서 은행 외 상점/수리/치료가 골드를 소모한다.
> 작업 브랜치: **현재 브랜치에서 진행**(신규 브랜치 생성 안 함).
> 이 문서는 실제 `myrpg` 소스를 분석하여 작성했으며, 개발 완료 후 삭제한다.

---

## ✅ 결정사항 (전부 확정)

### 골드
- **소지금**(`gold`, `long`)은 `CharacterProgress` 컬럼. **은행 보관 골드**는 별도 `Bank` 엔티티(모든 지점 통합, 행 1개).
- **골드 표시**: 상단바에는 **표시하지 않음**. **인벤토리 팝업 하단** + **은행 팝업**에서 표시.
- **임시 골드 버튼**: 좌측 사이드바 "경험치 다운" 아래에 **골드 획득(+100)** / **골드 소모(-100)**. 부족 시 alert, 피드백은 행동 로그. (실제 획득/소모 경로 구현 시 제거)
- **입/출금 정책**: 최소 1골드, 상한 없음, 수수료 없음.
- **잔액 부족**: alert.
- **사망/환생 시 골드·아이템 손실 없음**.

### 아이템 / 장비
- **아이템 종류**: `ItemType` = `POTION`(포션) · `WEAPON`(무기) · `ARMOR`(방어구). 향후 `ENCHANT`(인챈트) 등 확장.
- **실제 아이템 5종** (모두 실사용 데이터):

  | 아이템 | type | kind | 보너스 | 효과 |
  |---|---|---|---|---|
  | 생명력 50 포션 | POTION | – | – | HP 50 회복(소비형) |
  | 초보자용 한손검 | WEAPON | ONE_HANDED_SWORD | STR +5 | 주무기, 방패 병용 가능 |
  | 초보자용 양손검 | WEAPON | TWO_HANDED_SWORD | STR +10 | 주무기+보조손 점유, 방패 병용 불가 |
  | 초보자용 방패 | WEAPON | SHIELD | DEF +5 | 보조손 |
  | 초보자용 갑옷 | ARMOR | ARMOR_BODY | DEF +10 | 몸통 |

  > 보너스는 **실제 데이터**로 장착 시 총합 스탯에 합산한다(3.5).
- **장비 보너스 모델**: 장비 보너스는 `Stats` 단일이 아니라 **`(BonusTarget, 수치)` 목록**으로 표현한다. 기존 재능/스킬이 쓰는 `BonusTarget`(STR·DEX·INT·CRITICAL·DEF·HP·MP·STAMINA) + `BonusKind`(STAT/VITAL) 어휘를 재사용하여, 향후 "DEF +5, HP +30" 같은 **스탯+바이탈 혼합 보너스**까지 확장 가능하게 한다.
- **아이템 상세보기**: 목록에서 아이템명 오른쪽 **🔍** 버튼 → **작은 상세 모달**. 설명은 **자동 생성**(효과 데이터 기반, 별도 문구 관리 없음). 인벤토리·은행 **공용 컴포넌트**. (상세는 3.7 참조)
- **가격/판매가**: 구매가(`buyPrice`)는 **optional**로 상점 판매 아이템에만 명시(포션 등), 드랍 전용은 없음(null). **판매가는 저장하지 않고 판매 시 인스턴스 가치에서 계산**하며, 가치는 보너스 스탯 **동일 가중치** 방식이다. (상세·값 시점은 3.8)
- **캐릭터 생성 시 기본 지급**: 신규 캐릭터에게 위 장비 4종 + 생명력 50 포션 ×5 지급. 생성 시 **한손검 + 방패 + 갑옷을 기본 장착**(양손검은 미장착으로 인벤토리 보관).
- **동작 버튼**: 포션 → `사용`, 장비 → `착용`(장착 중이면 `해제`). 맡기기/찾기는 은행 팝업/인벤토리 공용.
- **포션 사용 정책**: **언제든 사용 가능**(마을/필드 + **전투 중 포함**). 인벤토리를 열어 **실시간으로 그때그때** 사용하며, **쿨다운·횟수 제한 없음**(보유 수량만큼 자유롭게). ※ 전투 UI 연동은 6순위, 지금은 인벤토리에서 사용.
- **목록 정렬**: **기본은 항상 획득순**. 화면에서 **획득순 / 이름순 / 타입순** 으로 전환 가능(4.2).
- **장비 착용 규칙**(3.4): 한손검+방패 병용 O, 양손검+방패 병용 X(alert). 방패 착용 중 양손검 착용도 **불가(alert, 방패 먼저 해제)**. 같은 슬롯 무기 교체는 스왑.
- **내구도(Durability)**: 장비만 보유. 최대치는 카탈로그 `maxDurability`(**모든 초보자 장비 = 20 고정**), 현재치는 인스턴스 `currentDurability`. **공격 턴마다 장착 중인 모든 장비가 0.2씩 감소**(20 ÷ 0.2 = 100턴에 0 도달). 대장간에서 수리. **내구도 0 시 동작은 추후 설계**. 감소 로직은 6순위(전투), 수리는 7순위(대장간). (상세 3.9)
- **장착 중 장비도 인벤토리 목록에 `[장착중]` 표시되고 30 용량에 포함**.
- **장착 중 장비는 은행에 맡길 수 없음**(해제 후 가능, alert).
- **레이아웃**: 인벤토리·은행 모두 **리스트(목록)형**.
- **용량**: 인벤토리 최대 30항목, 은행 보관 최대 30항목(장착품 포함). 초과 시 alert.
- **스택**: 포션 등 소비형은 같은 아이템을 **1행으로 누적**(수량 표기). **장비는 스택하지 않음**(개별 인스턴스, 같은 장비 2개면 2행).

### 남은 미결정사항
1. **향후 타입(`ENCHANT` 등) 세부 스펙** — 보류(추후).
2. **가격 세부 값** — 가격 **모델은 확정(3.8)**. 아이콘·등급·희귀도는 **불필요(미채택)**. 남은 것은 판매가 가중치 `WEIGHT` 상수·(적용 시)판매 비율·포션 `baseValue`·상점 UI·인챈트(`ENCHANT`) 구현으로, **7순위~에서 확정**한다.
3. **내구도 0 시 동작** — 파손 처리(스탯 보너스 미적용/자동 해제/그대로 등)는 **추후 설계에서 확정**. 그때까지 내구도는 감소·표시만 하고 0 도달 시 특수 동작 없음.

---

## 1. 현재 소스 분석 (as-is)

- **골드 미존재**: `CharacterProgress`에 골드 필드 없음. `ResourceKind`(STAMINA/MP)는 스킬 소모 자원 전용.
- **은행 NPC**: `data/npc.json` 의 오스틴(`austeyn`, `dunbarton`), `NpcType.BANK` 의 행동 라벨이 현재 `["아이템 보관", "골드 입/출금"]` 2개. `center.html` 의 NPC 버튼은 모두 동일하게 `npcAction()`(alert 플레이스홀더) 호출 — 라벨별 분기 없음.
- **인벤토리 버튼**: `left-sidebar.html` "인벤토리" = `openPanel('인벤토리')` 플레이스홀더.
- **카탈로그 로딩 관례**: `SkillCatalogService`/`NpcService` = `@Service` + `@PostConstruct` 에서 `classpath:data/*.json` 을 Jackson 3(`tools.jackson`)로 1회 파싱 → 불변 목록, 무결성 위반 시 전용 `*DataException` 으로 기동 실패.
- **도메인 다형성 관례**: `Skill`(sealed 계열) → `DamageSkill`/`DefenseSkill`(record).
- **스탯 조립**: `PlayScreenViewHelper.buildStatLines()` 가 `StatProgression.levelStatsFor(level, talent)`(레벨+재능) + `SkillService.rankupBonus(id)`(스킬 보너스)로 `StatLine`(STR/DEX/INT/CRIT/DEF) 구성. `Stats`(record: str/dex/int/critical/defense), `Stats.ZERO` 존재.
- **캐릭터 생성**: `CharacterService.createAndSaveDefault()` 가 기본 캐릭터 저장 후 `skillService.seedDefault(id)` 로 기본 스킬 시드. → **동일 지점에 기본 아이템 시드 추가**.
- **팝업 패턴**: `GET` fetch → 응답 HTML 주입 → `.overlay.open` 토글. 모달 위 모달(`rankup-overlay`) 패턴 존재.

---

## 2. 골드 시스템

### 2.1 소지금 — `CharacterProgress` 컬럼
```java
@Column(name = "gold", nullable = false)
private long gold;   // 소지금
```
- 기본값 `createDefault()` 에서 `gold = 0`.
- 도메인 메서드: `gainGold(long)`, `spendGold(long)`(보유 초과 시 `InsufficientGoldException`).

### 2.2 은행 통합 금고 — 신규 `Bank` 엔티티
```java
@Entity @Table(name = "bank")
public class Bank {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private long gold;           // 통합 보관 골드
    // deposit(long), withdraw(long)(부족 시 InsufficientGoldException)
}
```
- `BankRepository`(신규), `BankService.loadOrCreateDefault()`(없으면 기본 행 생성 — `CharacterService` 패턴).
- 모든 은행 지점이 이 단일 행을 공유.

### 2.3 입금/출금 (소지금 ↔ 은행, `@Transactional`)
| 동작 | 처리 | 실패 |
|---|---|---|
| 입금 | `character.spendGold(n)` → `bank.deposit(n)` | 소지금 부족 → alert |
| 출금 | `bank.withdraw(n)` → `character.gainGold(n)` | 은행 잔액 부족 → alert |

- 예외: `InsufficientGoldException`(신규, `RuntimeException` 직접 사용 금지 규칙 준수).

### 2.4 임시 골드 버튼 (테스트용)
- `left-sidebar.html` "경험치 다운" 아래 `골드 획득`/`골드 소모` 버튼.
- 상수 `TEST_GOLD_AMOUNT = 100L`.

| 메서드 | 경로 | 동작 | 반환 |
|---|---|---|---|
| POST | `/gold/gain` | `gainGold(100)` + 로그 | `fragments/progress-response` |
| POST | `/gold/spend` | `spendGold(100)`; 부족 시 미차감·alert | `fragments/progress-response` |

- 상단바 골드 미표시 → 피드백은 행동 로그. 실제 경로 구현 시 제거.

---

## 3. 아이템 / 장비 시스템

### 3.1 열거형 — `ItemType` / `EquipSlot` / `EquipmentKind`
```java
public enum ItemType {
    POTION("potion", "포션"), WEAPON("weapon", "무기"), ARMOR("armor", "방어구");
    // code, label, fromString(code); isEquipment() = (WEAPON || ARMOR)
}

public enum EquipSlot { MAIN_HAND, OFF_HAND, BODY }

public enum EquipmentKind {
    ONE_HANDED_SWORD("한손검", EquipSlot.MAIN_HAND, Set.of(EquipSlot.MAIN_HAND)),
    TWO_HANDED_SWORD("양손검", EquipSlot.MAIN_HAND, Set.of(EquipSlot.MAIN_HAND, EquipSlot.OFF_HAND)),
    SHIELD("방패", EquipSlot.OFF_HAND, Set.of(EquipSlot.OFF_HAND)),
    ARMOR_BODY("갑옷", EquipSlot.BODY, Set.of(EquipSlot.BODY));
    // primarySlot, requiredSlots(불변 Set), label, fromString(code)
}
```

### 3.2 카탈로그 — `Item`(다형 record) + `data/item.json`
```java
public sealed interface Item permits PotionItem, EquipmentItem {
    String id(); String name(); ItemType type();
    Integer buyPrice();   // optional 상점 구매가 (없으면 null = 상점 미판매)
}

public record PotionItem(String id, String name, int healHp, Integer buyPrice) implements Item {
    @Override public ItemType type() { return ItemType.POTION; }
}

public record EquipmentItem(String id, String name, ItemType type,
                            EquipmentKind kind, List<EquipBonus> bonuses,
                            Integer buyPrice, int maxDurability) implements Item {
    // type 은 WEAPON 또는 ARMOR
}

/** 장비 보너스 한 줄. 기존 BonusTarget/BonusKind 어휘 재사용. */
public record EquipBonus(BonusTarget target, int amount) {
    // target.kind() 로 STAT/VITAL 분기, target.label() 로 상세 표시
}
```

`src/main/resources/data/item.json`
```json
[
  { "id": "hp_potion_50",            "name": "생명력 50 포션", "type": "potion", "healHp": 50, "buyPrice": 30 },
  { "id": "beginner_one_hand_sword", "name": "초보자용 한손검", "type": "weapon", "kind": "one_handed_sword", "bonuses": [ { "target": "STR", "amount": 5 } ],  "maxDurability": 20 },
  { "id": "beginner_two_hand_sword", "name": "초보자용 양손검", "type": "weapon", "kind": "two_handed_sword", "bonuses": [ { "target": "STR", "amount": 10 } ], "maxDurability": 20 },
  { "id": "beginner_shield",         "name": "초보자용 방패",   "type": "weapon", "kind": "shield",           "bonuses": [ { "target": "DEF", "amount": 5 } ],  "maxDurability": 20 },
  { "id": "beginner_armor",          "name": "초보자용 갑옷",   "type": "armor",  "kind": "armor_body",       "bonuses": [ { "target": "DEF", "amount": 10 } ], "maxDurability": 20 }
]
```
> 포션은 상점 판매 아이템이라 `buyPrice` 명시(값 30은 임시, 7순위에서 확정). 초보자 장비는 상점 미판매라 `buyPrice` 생략(null). 혼합 보너스 예(향후): `"bonuses": [ { "target": "DEF", "amount": 5 }, { "target": "HP", "amount": 30 } ]`.
- `ItemCatalogService`(신규): `SkillCatalogService`와 동일 구조. `type`=potion → `PotionItem`, weapon/armor → `EquipmentItem`(+`kind`+`bonuses`+`maxDurability`). `bonuses`의 `target`은 `BonusTarget.valueOf(...)`로 파싱, 미기재 시 빈 목록. `buyPrice`는 **optional 파싱**(없으면 null). `maxDurability`는 **장비 필수**(포션은 없음). 무결성 위반 시 신규 `ItemDataException`.

### 3.3 보유 아이템 — 신규 `OwnedItem` 엔티티
```java
@Entity @Table(name = "owned_item")
public class OwnedItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "item_id", nullable = false)
    private String itemId;                 // item.json 참조
    @Column(nullable = false)
    private int quantity;                   // 소비형 스택 수량 (장비는 1)
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StorageKind storage;            // INVENTORY | BANK
    @Column(nullable = false)
    private boolean equipped;               // 장착 여부 (INVENTORY 장비만 true)
    @Column(name = "current_durability", nullable = false)
    private double currentDurability;       // 현재 내구도 (장비만 의미, 소수 0.2 단위 감소)
    // increaseQuantity/decreaseQuantity(0 미만 방지), moveTo(storage), equip(), unequip(),
    // reduceDurability(amount)(0 미만 방지), repairToMax(maxDurability)
}
```
- `StorageKind { INVENTORY, BANK }`.
- **스택**: 소비형은 같은 `itemId`+`storage` 를 1행 누적. **장비는 스택 안 함**(개별 행).
- **`equipped`**: `storage=INVENTORY` 장비만 `true`. 포션/은행 항목은 항상 `false`.
- **용량**: `storage` 별 행 수 ≤ 30 (장착품 포함).
- 계정=단일 캐릭터이므로 `ownerId` 미보유(향후 다중 캐릭터 시 추가). 은행(BANK)은 통합.
- `OwnedItemRepository`(신규): `findByStorageOrderById`, `findByStorageAndItemId`, `countByStorage`, `findByStorageAndEquippedTrue`.

### 3.4 장비 착용 규칙 — `InventoryService.equip/unequip`
착용 대상 `N`(kind K):
1. **충돌 검사**: `K.requiredSlots` 각 슬롯을 점유한 장착 장비의 `primarySlot`이 `K.primarySlot`과 **다르면 → 착용 불가**(`EquipConflictException` → alert "착용 할 수 없습니다").
2. 충돌 없으면: 해당 슬롯을 점유하던 **같은 역할 장비 해제** 후 `N.equip()`.

> 규칙 하나로 요구사항 전부 성립:
> - 한손검(MAIN_HAND) + 방패(OFF_HAND): 슬롯 안 겹침 → 병용 O.
> - 양손검(MAIN_HAND+OFF_HAND) 착용 중 방패: 방패의 OFF_HAND를 primary가 다른 양손검이 점유 → 불가(alert).
> - 방패 착용 중 양손검: 양손검의 OFF_HAND를 primary가 다른 방패가 점유 → 불가(alert, 방패 먼저 해제).
> - 한손검 → 한손검/양손검 교체: 같은 primary(MAIN_HAND) → 스왑. (한손검→양손검은 방패 없을 때만)
> - 갑옷(BODY): 독립, 갑옷끼리 스왑.

- **해제(`unequip`)**: `equipped=false`. 버튼은 장착 중이면 "해제" 표기.
- 착용/해제는 INVENTORY 장비에만. 저장 후 행동 로그.

### 3.5 장비 보너스 합산 (실제 데이터 반영, STAT/VITAL 분기)
- `InventoryService.equippedBonus()`: `storage=INVENTORY && equipped` 장비들의 `EquipBonus` 목록을 모아, `BonusTarget.kind()` 로 분기하여 **STAT 계열은 `Stats` 로, VITAL 계열은 `VitalMax` 로** 각각 합산. (기존 `StatProgression`/`SkillRankupBonus` 의 STAT/VITAL 처리 방식과 동일)
- 반영 지점:
  - `PlayScreenViewHelper.buildStatLines()` 에서 기존 `skillBonus`(STAT) 에 **장비 STAT 보너스를 합산**하여 보너스 표기에 반영.
  - **VITAL 보너스(HP/MP/Stamina)** 는 최대치 계산(`vitalMaxFor`)에 합산하여 게이지 최대값에 반영. (지금 5종은 STAT만 사용 — VITAL 경로는 확장 대비 그릇)
- 기본 장착(한손검 STR+5, 방패 DEF+5, 갑옷 DEF+10) → 생성 직후 STR +5, DEF +15 가 정보 팝업에 반영.

### 3.6 포션 사용 — `InventoryService.usePotion(ownedItemId)`
1. `OwnedItem` → 카탈로그 `PotionItem`.
2. `hpCurrent = min(hpCurrent + healHp, hpMax)` (hpMax는 `vitalMaxFor(level, talent)` 재사용).
3. `quantity--`; 0이면 행 삭제.
4. 저장 + 행동 로그("생명력 50 포션 사용, HP +N").

- **언제든/무제한**: 마을·필드는 물론 **전투 중에도** 사용 가능하며 쿨다운·횟수 제한 없음(보유 수량 내 자유). 인벤토리에서 실시간으로 사용한다.
- 전투 중 사용의 전투 UI 연동은 6순위. 이번 단계는 인벤토리 팝업에서의 사용까지 구현한다.

### 3.7 아이템 상세보기 (자동 생성, 공용 모달)
- **트리거**: 인벤토리·은행 목록의 각 행에서 **아이템명 오른쪽 🔍** 클릭 → 목록 팝업 위에 **작은 상세 모달**(`rankup-overlay` 겹치기 패턴 재사용). 인벤토리·은행이 **같은 컴포넌트**를 공유.
- **설명 자동 생성**(별도 문구 관리 없음, 효과 데이터 = 단일 진실):
  - 포션: `"생명력을 {healHp} 회복한다."`
  - 장비: `bonuses` 를 한 줄씩 (`target.label()` + 부호 수치, 예: `"STR +5"`; Critical은 0.1% 단위 포맷) + 장비 종류/슬롯 안내(한손검/양손검/방패/갑옷) + 배타 안내(양손검: `"방패와 함께 착용할 수 없다."`) + **내구도 표시(예: `"내구도 12.4/20"`)**.
  - 생성 위치: `Item` 다형 `describe()`(포션/장비별) 또는 뷰 조립부 포맷터. `EquipBonus` 목록을 순회하므로 보너스가 몇 개든/혼합이든 그대로 확장.
- **데이터 전달 = 렌더 시 임베드**: 상세 텍스트를 `OwnedItemView` 에 함께 내려 🔍의 data 속성/숨김 블록에 실어둔다. **서버 왕복 없음.**
  - 안전 근거: 목록은 최대 30(은행 열면 ≤60)으로 상한이 있고, 상세는 카탈로그 기반이라 보관 위치와 무관하며, 아이템 이동·사용·착용 등 변경 시 프래그먼트가 재렌더되어 상세도 함께 갱신 → **stale 없음**.
  - 확장 노트: 30칸 상한을 없앨 경우엔 지연 로드가 아니라 **페이지네이션/가상 스크롤**(페이지당 일정 행만 렌더)로 대응한다.

### 3.8 가격 · 판매가 (모델 방향, 실제 값은 7순위~)
- **구매가 `buyPrice`** (카탈로그, optional):
  - 상점에서 파는 아이템에만 명시(포션 등). **없으면(null) 상점 미판매**(드랍 전용, 예: 난폭자의 검).
  - 실제 상점 구매 처리·UI는 **7순위(NPC 행동)** 에서.
- **판매가** (저장하지 않고 판매 시 계산):
  ```
  sellValue = baseValue(옵션) + totalBonusPoints × WEIGHT
    - totalBonusPoints = 기본 보너스 합 + 인챈트 보너스 합 (인스턴스 기준)
    - WEIGHT = 모든 BonusTarget 공통 동일 가중치 상수 1개
  ```
  - **장비**: 보너스 × 가중치가 주 가치(`baseValue` 생략 시 0).
  - **포션 등 보너스 없는 소비형**: `baseValue` 를 카탈로그에 명시(값은 7순위).
  - **인챈트(향후 `ENCHANT`)**: `OwnedItem` 인스턴스에 보너스가 붙으면 **동일 가중치로 자동 합산** → 판매가 자동 상승. (판매가를 저장하지 않는 이유)
- **미확정(7순위~)**: `WEIGHT` 실제 숫자, (적용 여부) 판매 비율, 포션 `baseValue`, 상점/인챈트 구현.
- 이번 A~C 범위에서는 판매/구매가 실제로 쓰이지 않으며(임시 골드 버튼으로 골드 테스트), 위 모델과 `buyPrice` optional 필드만 데이터에 반영한다.

### 3.9 내구도 (Durability)
- **대상**: 장비만. 포션 등 소비형은 내구도 없음.
- **값**:
  - 최대치 `maxDurability`(카탈로그) — **모든 초보자 장비 = 20 고정**.
  - 현재치 `currentDurability`(`OwnedItem` 인스턴스) — 지급/생성 시 `maxDurability` 로 시작.
- **감소**: **공격 턴마다 장착 중인 모든 장비가 0.2 감소**(무기·방패·갑옷 동일). **20 ÷ 0.2 = 100턴**에 정확히 0 도달. 소수라 `double` 로 관리하되, 부동소수 누적 오차를 피하려면 **×10 스케일 정수 저장**(최대 200, 턴당 -2) 방식을 구현 시 권장.
  - 감소 로직은 **전투(6순위)** 의 턴 처리 API 안에서 수행: 스킬 발사 → 데미지 계산 → **장착 장비 `reduceDurability(0.2)`** → 결과 리턴.
- **수리**: **대장간(7순위)** 에서 `repairToMax` 로 복구, 골드 소모. 수리비 정책은 7순위에서 확정.
- **0 도달 시 동작**: **추후 설계**(미결정 3). 그때까지는 감소·표시만 하고 0에서 멈추며 특수 동작 없음. (파손 규칙 확정 시 3.5 스탯 합산에 "내구도 0 장비 제외" 등 반영 여지)
- **표시**: 상세 모달(3.7) 및 목록에 `내구도 12.4/20` 형태로 노출.
- **이번 범위(4순위)**: `maxDurability`(카탈로그)·`currentDurability`(인스턴스) 필드 + 지급 시 최대치로 초기화 + 표시까지. **감소(6순위)·수리(7순위)·0 처리(추후)** 는 이월.

---

## 4. 인벤토리 팝업 (리스트형)

### 4.1 트리거
- `left-sidebar.html` "인벤토리" 버튼을 `openInventory()` 로 교체 → `GET /inventory` → `.open`.

### 4.2 레이아웃
```
┌──────────────── 인벤토리 ────────────────[✕]┐
│  정렬: [획득순▼] (획득순/이름순/타입순)         │
│  아이템                  타입   수량   동작     │
│  ───────────────────────────────────────────  │
│  생명력 50 포션 🔍        포션   ×5   [사용]     │
│  초보자용 한손검 🔍[장착중] 무기   ×1   [해제]     │
│  초보자용 방패 🔍[장착중]   무기   ×1   [해제]     │
│  초보자용 갑옷 🔍[장착중]   방어구  ×1  [해제]     │
│  초보자용 양손검 🔍       무기   ×1   [착용]     │
│  (…최대 30항목)                                │
│  ───────────────────────────────────────────  │
│                          보유 골드 💰 3,500     │
└────────────────────────────────────────────────┘
```
- 상단: **정렬 선택**(획득순/이름순/타입순). **기본값 획득순**.
- 행: 이름 + **🔍(상세보기)** (+`[장착중]` 배지) · 타입 라벨 · 수량 · 동작.
  - 🔍 클릭 → 상세 모달(3.7).
  - 포션 → `사용`
  - 장비 → 미장착 `착용` / 장착 중 `해제`
- **`맡기기`/`찾기` 는 이 팝업에 없다** — 은행 앞에서만 의미가 있으므로 **은행 팝업(5장)에서만** 노출한다.
- 비어있으면 "보유 아이템이 없습니다".
- 하단: 보유 골드.

**정렬 동작**
- **기본 획득순** = `OwnedItem.id` 오름차순(= 획득/저장 순서). 서버가 이 순서로 내려준다.
- 이름순/타입순 전환은 **클라이언트에서 정렬**(목록·상세가 이미 임베드돼 있으므로 서버 왕복 없음). 타입순은 타입 그룹 내 이름순 보조 정렬.
- 스크롤/재렌더 후에도 선택된 정렬 유지.

### 4.3 엔드포인트
| 메서드 | 경로 | 동작 |
|---|---|---|
| GET | `/inventory` | 소지품 목록 + 보유골드 |
| POST | `/inventory/use?ownedItemId=` | 포션 사용 |
| POST | `/inventory/equip?ownedItemId=` | 장비 착용(충돌 시 불가) |
| POST | `/inventory/unequip?ownedItemId=` | 장비 해제 |

- 상세보기는 별도 엔드포인트 없음(임베드, 3.7). `use`/`equip`/`unequip` 은 반환 fragment로 목록·상세를 함께 재렌더.
- 반환: 갱신된 `fragments/inventory-popup :: inventory-content`.
- `InventoryView`(신규): `long gold`, `List<OwnedItemView> items`.
- `OwnedItemView`(신규 record): `long ownedItemId, String name, String typeLabel, ItemType type, int quantity, boolean equipped, boolean usable, boolean equippable, Double currentDurability, Integer maxDurability`(장비만, 포션은 null), `List<String> detailLines`(상세 모달용 자동 생성 텍스트).

### 4.4 프런트(JS)
- `openInventory()`/`closeInventory()`, `usePotion(id)`, `equipItem(id)`/`unequipItem(id)`(불가 응답 시 alert).
- `openItemDetail(el)`/`closeItemDetail()`: 🔍의 임베드 데이터(data 속성/숨김 블록)로 상세 모달을 채워 표시. **서버 왕복 없음**. 인벤토리·은행 공용.
- `sortInventory(mode)`: 획득순/이름순/타입순으로 **클라이언트에서 목록 행 재정렬**(서버 왕복 없음). 기본 획득순.
- `depositItem(id)`(맡기기)/`withdrawItem(id)`(찾기)는 **은행 팝업(5장) 전용** JS이며, 이 팝업에서는 호출하지 않는다.

---

## 5. 은행 팝업 (리스트형)

### 5.1 행동 버튼 통합
- `NpcType.BANK` 행동 라벨 `["아이템 보관","골드 입/출금"]` → **`["은행"]`**.
- `center.html` 버튼을 `onclick="npcAction(this.textContent)"` 로, `npcAction(label)` 에서 `label==='은행'` → `openBank()`.

### 5.2 레이아웃
```
┌──────────────────── 은행 (오스틴) ────────────────────[✕]┐
│  은행 보관 (목록)            │  내 소지품 (목록)            │
│  ──────────────────────     │  ──────────────────────     │
│  생명력 50 포션 🔍 ×2 [찾기]  │  생명력 50 포션 🔍 ×3 [맡기기] │
│  (…최대 30)                 │  (…최대 30)                 │
│  ──────────────────────     │  ──────────────────────     │
│  은행 보관 골드 💰 12,340    │  보유 골드 💰 3,500          │
│                [ 입금 ]        [ 출금 ]                     │
└────────────────────────────────────────────────────────────┘
```
- 좌 = `storage=BANK`, 우 = `storage=INVENTORY`.
- 양쪽 목록 각 행에도 **🔍(상세보기)** 노출 — 인벤토리와 **공용 상세 모달**(3.7) 사용.
- 입금/출금 클릭 → 금액 입력 소형 모달(`rankup-overlay` 패턴, 최소 1골드).

### 5.3 엔드포인트 (신규 `BankController`)
| 메서드 | 경로 | 동작 |
|---|---|---|
| GET | `/bank` | 은행 팝업(양쪽 목록 + 골드 2칸) |
| POST | `/bank/deposit?amount=` | 골드 입금 |
| POST | `/bank/withdraw?amount=` | 골드 출금 |
| POST | `/bank/item/deposit?ownedItemId=` | 아이템 맡기기(INVENTORY→BANK) |
| POST | `/bank/item/withdraw?ownedItemId=` | 아이템 찾기(BANK→INVENTORY) |

- 맡기기: 은행 30 초과 시 alert. **장착 중 장비는 맡기기 불가**(alert). 소비형은 스택 누적.
- 찾기: 인벤토리 30 초과 시 alert.
- 골드 입출금은 `@Transactional`, 부족 시 `InsufficientGoldException` → alert.
- `BankView`(신규): 은행 보관 골드·보유 골드 + 은행/소지품 두 `List<OwnedItemView>`(각 상세 임베드, 🔍 공용).

---

## 6. 캐릭터 생성 시 기본 지급

- `CharacterService.createAndSaveDefault()` 에서 `skillService.seedDefault(id)` 옆에 **`inventoryService.seedDefault()`** 추가.
- 지급 내용:
  - 초보자용 한손검 ×1 (INVENTORY, **장착**)
  - 초보자용 방패 ×1 (INVENTORY, **장착**)
  - 초보자용 갑옷 ×1 (INVENTORY, **장착**)
  - 초보자용 양손검 ×1 (INVENTORY, 미장착)
  - 생명력 50 포션 ×5 (INVENTORY)
- 지급되는 장비의 `currentDurability` 는 **최대치(20)로 초기화**.
- 결과: 생성 직후 정보 팝업에 STR +5, DEF +15(장비 보너스) 반영.

---

## 7. 영향 받는 파일 요약

| 구분 | 파일 | 변경 |
|---|---|---|
| 도메인 | `domain/model/CharacterProgress.java` | `gold` + `gainGold`/`spendGold` |
| 도메인 | `domain/model/Bank.java` | **신규** 통합 금고 |
| 리포지토리 | `domain/repository/BankRepository.java` | **신규** |
| 서비스 | `application/service/BankService.java` | **신규** |
| 열거 | `domain/model/ItemType.java`, `EquipSlot.java`, `EquipmentKind.java` | **신규** |
| 도메인 | `domain/model/Item.java`(sealed) + `PotionItem.java` + `EquipmentItem.java` + `EquipBonus.java` | **신규** (보너스는 `BonusTarget` 재사용) |
| 도메인 | `domain/model/OwnedItem.java`, `StorageKind.java` | **신규** (`equipped` 포함) |
| 리포지토리 | `domain/repository/OwnedItemRepository.java` | **신규** |
| 서비스 | `application/service/ItemCatalogService.java` | **신규** item.json 로딩 |
| 서비스 | `application/service/InventoryService.java` | **신규** 목록/사용/착용/해제/이동/시드/스탯합산/상세생성 |
| 서비스 | `application/service/CharacterService.java` | 기본 아이템 시드 호출 |
| 예외 | `application/exception/InsufficientGoldException.java`, `ItemDataException.java`, `InventoryFullException.java`, `EquipConflictException.java` | **신규** |
| 열거 | `domain/model/NpcType.java` | BANK 라벨 `["은행"]` 통합 |
| 뷰모델 | `application/dto/InventoryView.java`, `OwnedItemView.java`, `BankView.java` | **신규** |
| 뷰조립 | `interfaces/api/PlayScreenViewHelper.java` | `buildStatLines` 에 장비 보너스 합산 |
| 컨트롤러 | `interfaces/api/PlayScreenController.java` | `/gold/gain`·`/gold/spend` |
| 컨트롤러 | `interfaces/api/InventoryController.java`, `BankController.java` | **신규** `/inventory*`, `/bank*` |
| 데이터 | `resources/data/item.json` | **신규** (5종) |
| 템플릿 | `fragments/inventory-popup.html`(신규), `fragments/bank-popup.html`(신규, 리스트), `fragments/item-detail.html`(신규, 공용 상세 모달), `fragments/center.html`, `fragments/left-sidebar.html`, `play.html` | 팝업·버튼·🔍 상세 |
| 정적 | `static/js/myrpg.js`, `static/css/myrpg.css` | 팝업·목록·버튼·상세 모달 |
| 데이터 | `resources/data/npc.json` | 변경 없음(라벨은 코드) |

> 상단바(`TopBarView`/`top-bar.html`)는 골드 미표시로 **변경 없음**. 사망/환생 로직은 골드·아이템을 건드리지 않으므로 변경 없음.

---

## 8. 통합 구현 순서 (Task 초안)

**A. 골드/은행**
1. `CharacterProgress.gold` + `gainGold`/`spendGold` + `InsufficientGoldException` (테스트).
2. `Bank` + `BankRepository` + `BankService.loadOrCreateDefault`/`deposit`/`withdraw` (테스트).
3. 임시 골드 버튼(+100/-100) + `/gold/gain`·`/gold/spend`.

**B. 아이템 카탈로그·보유·장비**
4. `ItemType`/`EquipSlot`/`EquipmentKind` + `Item`/`PotionItem`/`EquipmentItem`/`EquipBonus`(`BonusTarget` 재사용) + `data/item.json`(5종) + `ItemCatalogService` + `ItemDataException` (로딩/파싱 테스트).
5. `OwnedItem`(`equipped`) + `StorageKind` + `OwnedItemRepository` (영속 테스트).
6. `InventoryService`: 목록/`usePotion`/`equip`·`unequip`(3.4 규칙, `EquipConflictException`)/맡기기·찾기(용량 30, `InventoryFullException`)/`equippedBonus`(STAT/VITAL 분기)/`describe`(상세 자동 생성)/`seedDefault` (단위 테스트 — 장비 규칙·상세 생성 케이스 포함).
7. `CharacterService.createAndSaveDefault` 에 기본 아이템 시드 연결 + 착용 상태 반영 (테스트).
8. `PlayScreenViewHelper.buildStatLines` 에 장비 STAT 보너스 합산 + VITAL 보너스 vitalMax 합산 (테스트).

**C. UI 통합 (리스트형)**
9. 인벤토리 팝업 `inventory-popup.html` + 공용 상세 모달 `item-detail.html` + 정렬 컨트롤(클라이언트, 기본 획득순) + `GET /inventory`(획득순=`id` asc), `POST /inventory/use|equip|unequip` + `InventoryController` + JS/CSS(상세 모달 포함) + 슬라이스 테스트.
10. 은행 팝업 `bank-popup.html`(양쪽 목록 🔍 포함) + `NpcType.BANK` "은행" 통합 + `BankController`(골드/아이템 이동) + JS/CSS + 슬라이스 테스트.

> **가격/판매가(3.8)**: 이번 범위에서는 `buyPrice` optional 필드 파싱까지만. 판매가 계산 유틸·상점 구매/판매·인챈트는 **7순위~**에서 구현.
> **내구도(3.9)**: 이번 범위에서는 `maxDurability`(카탈로그)·`currentDurability`(인스턴스) 필드 + 최대치 초기화 + 표시까지. 턴당 감소는 **6순위(전투)**, 수리는 **7순위(대장간)**, 0 처리는 **추후 설계**.
> 각 Task는 `mvn clean install -pl myrpg -am` 빌드 성공 + 기능 추가 시 테스트 코드 작성 규칙(`code-style.md`, `task-build-validation.md`)을 따른다.
