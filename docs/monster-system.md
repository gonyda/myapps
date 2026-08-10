# 몬스터 시스템 상세 설계

> 대상 우선순위: **5순위** — 몬스터 카탈로그(`monster.json`) + 맵별 출현 매핑(`map.json`) + 필드 이동 시 몬스터 버튼 노출 + 클릭 시 대사·`전투` 버튼.
> 연관: **6순위(전투)** 가 이 문서의 스탯·가위바위보 AI·드랍 테이블·**선공 판정**을 실제로 소비한다. **7순위(상점)** 이 드랍 아이템의 판매가를 확정한다.
> 스펙 폴더(구현 착수 시): `.kiro/specs/myrpg/007-monster-system/`
> 작업 브랜치: 구현 착수 전 사용자에게 브랜치 생성 여부를 확인한다.
> 이 문서는 실제 `myrpg` 소스를 분석하여 작성했으며, 개발 완료 후 삭제한다.

---

## ✅ 결정사항

### 이번 범위(5순위)에서 하는 것
- `data/monster.json` 신규 — 몬스터 카탈로그(전체 목록). **실제로 쓸 몬스터는 `너구리` 1종만** 정의한다(가장 약한 일반 몬스터).
- `data/map.json` 노드에 `monsters: [...]` 배열 추가 — **출현 매핑은 맵 데이터가 단일 진실**. 한 노드에 여러 몬스터 가능.
- 몬스터 종류: `MonsterType` = `NORMAL`(일반) · `BOSS`(보스). 너구리는 `NORMAL`.
- 유저가 몬스터가 배치된 노드로 이동하면 **마을의 NPC 버튼과 동일한 자리(`.interactions`)에 몬스터 버튼**이 노출된다. NPC 버튼 뒤에 정의 순서대로 붙는다.
- 몬스터 버튼 클릭 → **NPC 멘트가 나오던 자리(`.npc-talk`)에 몬스터 이름 + (이름 옆에 작게) 레벨·HP + 대사**, 그 아래 **NPC 행동 버튼과 같은 자리에 `전투` 버튼** 1개. 레벨·HP는 카탈로그의 `level`·`maxHp`를 쓴다(조우 시점엔 전투 상태가 없으므로 **최대 HP**를 표시; 6순위 전투에서 현재/최대로 확장).
- 대사(`lines`)는 몬스터당 **정확히 3개**로 구성한다 — **1개는 울음/위협음**("크르릉…"), **2개는 행동 묘사**("(너구리가 몸을 숙여 경계 태세를 갖춘다.)" 형태). 사람 말/신음("크으으…")은 쓰지 않는다. 조우 시 3개 중 **랜덤 1개** 노출(NPC와 동일한 랜덤 방식, 단 **시간대 분기 없음**).
- 스탯 모델: **몬스터는 `level` + `maxHp` + `attackPower`(공격력) + `defense`(방어) + `critical`(크리티컬) 5개만 보유**한다. 플레이어의 `Stats`(STR/DEX/INT/CRITICAL/DEF) record를 재사용하지 않고 몬스터 전용으로 평평하게 둔다.
  - **DEX/INT 미사용 이유**: 플레이어가 DEX/INT를 갖는 건 재능(근접/활/마법)이 주력 공격 스탯을 고르기 때문인데, 몬스터는 재능 선택이 없어 "얼마나 세게 때리는가"(`attackPower`) 하나면 충분하다. 마법사류 몬스터도 게임적으로는 동일하며 마법은 이모지·대사 같은 **연출**일 뿐이다.
  - **`critical` 사용 이유**: 크리티컬이 없으면 전투가 단조로워진다. 플레이어와 동일하게 크리 확률을 갖는다. 값은 플레이어와 같은 **0.1% 단위 정수**(50 = 5.0%) 규약을 따른다. 실제 크리 배율·판정 규칙은 6순위(전투)에서 확정한다.
  - 플레이어 방어가 `DEF` **단일**(물리/마법 구분 없음)이라, 몬스터가 무슨 속성으로 때리든 결국 같은 `DEF` 하나에 부딪힌다 → 몬스터 쪽 속성 스탯을 나눌 기계적 이유가 없다.
  - 몬스터는 스킬이 없으므로 **MP·스태미나도 미보유**.
- 드랍 모델: **모든 몬스터는 기본 골드를 드랍**(`goldDrop {min,max}` 필수) + **`itemDrops` 배열**(확률·수량 범위). 너구리는 **생명력 50 포션(`hp_potion_50`)을 15% 확률·1개 고정**으로 드랍한다. 일반/보스 모두 아이템 드랍이 추가될 수 있는 구조다.
- 가위바위보 AI: 몬스터는 스킬 없이 **`SkillType`(일반/강/방어) 3항을 `34 / 33 / 33` 확률로 발동**. 이번 범위에서 **정책 클래스 + 단위/프로퍼티 테스트까지 구현**하고, 실제 턴 소비는 6순위.
- 드랍 계산(`rollDrop`)도 이번 범위에서 **순수 계산 + 테스트까지** 구현하고, 실제 지급(골드 가산·인벤토리 적재)은 6순위.
- 몬스터 배치는 **정적·상주** — **처치해도 그 맵에 항상 그대로 유지**된다. 소멸/리스폰/스폰 수량 개념이 없고, 조우·처치는 맵 상태를 바꾸지 않는다(추후에도 상주 유지가 기본).
- 조우는 **무상태**. 서버에 조우 상태를 저장하지 않고, 클릭할 때마다 대사를 다시 추첨한다.
- **몬스터 선공(preemptive strike)**: **몬스터가 배치된 노드로 이동(진입)하면 `5%`(전 맵 고정) 확률로**, 그 노드의 몬스터 중 **1마리를 랜덤 선택**하여 선공 이벤트가 발생한다.
  - **판정은 서버(6순위 실전투가 서버 주도이므로 동일 위치)** 에서 이뤄진다. `MonsterEncounterService`가 `Random` 빈으로 5% + 랜덤 선택을 수행하고, 발동 시 **선공 몬스터**를 반환한다.
  - **판정 시점**: `POST /move` 이동 성공(`MovementResult.Moved`) 직후에만. `GET /`(새로고침)에서는 판정하지 않는다(진입 시 1회).
  - **이번 범위(5순위)에서는 실제 전투 로직 없이 신호만**: 발동 시 이동 응답에 선공 신호를 실어 내리고, 프런트가 **`alert("몬스터 선공 발동")`** 만 띄운다. 실제 "몬스터가 먼저 공격하는" 전투 진입은 **6순위에서 이 신호 자리를 `POST /battle/start`(선공 플래그)로 교체**한다.
  - **6순위 대비 설계**: 발동 결과를 boolean이 아니라 **선택된 `Monster`** 로 반환하도록 만들어, 6순위가 "누가 선공했는지"를 그대로 활용한다. 5% 임계값은 상수 `PREEMPTIVE_STRIKE_PERCENT = 5`.
- `전투` 버튼은 이번 범위에서 `alert("구현 예정입니다")` 플레이스홀더(기존 `npcAction()` 관례와 동일). 6순위에서 `POST /battle/start`로 교체.
- 조우 시 행동 로그 1줄 추가(`"너구리와 마주쳤다."`, type `combat`). CSS `.log-combat`은 이미 존재한다.

### 하지 않는 것(이월)
| 항목 | 이월 대상 |
|---|---|
| 실제 전투 턴 처리, 데미지 계산, 선후공, 사망 | 6순위 |
| 몬스터 선공 발동 시 **실제 전투 진입**(alert → `POST /battle/start` 교체, 몬스터 선공권 반영) | 6순위 |
| 드랍 **지급**(골드 가산 / 인벤토리 적재) + `InventoryService` 획득 API 신설 | 6순위 |
| `CharacterProgress` HP 감소 메서드(`damageHp` 등) 신설 | 6순위 |
| 임시 골드 버튼(`/gold/gain`·`/gold/spend`, 사이드바 버튼 2개, JS 2개) 제거 | 6순위 (실제 골드 획득 경로가 열리는 시점) |
| 장착 장비 내구도 턴당 0.2 감소 호출 | 6순위 |
| 보스 몬스터 실데이터, 인챈트 아이템 드랍 | 인챈트 시스템 확정 후 |
| 던전 내부 몬스터 출현 | 10순위(던전) |
| 보스 필드 랜덤 등장(랜덤 시간·필드) — 스폰 스케줄러+런타임 상태, `Monster` sealed 불필요 | 추후 기능 |

### 미결정
1. **인챈트 아이템** 스펙 미정 → `itemDrops` 구조에 인챈트 계열 데이터는 아직 넣지 않는다(포션 등 기존 아이템 드랍은 투입 가능).
2. **드랍 아이템 획득 시 인벤토리 30칸 초과** 처리(획득 실패 / 자동 은행 / 바닥 드랍) → 6순위에서 확정.

---

## 1. 현재 소스 분석 (as-is)

몬스터·전투 관련 소스는 **하나도 없다**(`Monster*`, `Battle*`, `AttackType`, `ActionType` 부재). 대신 NPC 파이프라인이 **"맵별 배치 → 상호작용 버튼 → 클릭 시 대사 + 행동 버튼"** 의 완성된 레퍼런스로 존재하므로 그대로 복제·확장한다.

| 재사용 대상 | 위치 | 몬스터에서의 활용 |
|---|---|---|
| 카탈로그 로딩 패턴 | `NpcService`, `ItemCatalogService`, `SkillCatalogService` | `@PostConstruct` + `loadFromStream(InputStream)` 분리 + `*DataException` 기동 실패 |
| 타입 메타 내장 enum | `NpcType`(코드/라벨/이모지/행동 라벨) | `MonsterType`(코드/라벨/버튼 배지/행동 라벨) 유사 구조 |
| 노드별 조회 | `NpcService.byNode(nodeId)` | `MonsterService.byNode(nodeId)` |
| 대사 랜덤 선택 | `NpcDialogueService`(`Clock`·`Random` 빈 주입, 폴백 문구) | `MonsterDialogueService`(시간대 분기·폴백 제거, `lines` 3개 고정) |
| 상호작용 버튼 | `PlayScreenViewHelper.buildInteractions`, `InteractionItem(id, name, npc)` | `InteractionItem`의 `npc=false`가 **이미 몬스터용으로 문서화**되어 있음 |
| 버튼 스타일 | `myrpg.css` `.interactions button`(붉은 계열 `#3a2a2a/#ffcccc/#6a4a4a`), `.interactions button.npc`(녹색 오버라이드) | **몬스터 버튼 스타일은 이미 준비됨** — 클래스 미부여 시 붉은 계열 |
| 로그 타입 색상 | `myrpg.css` `.log-combat`(연빨강), `.log-reward`, `.log-item` | 조우/전투/보상 로그에 그대로 사용 |
| 대사 영역 | `center.html` `.npc-talk` + `.npc-actions` | 몬스터 대사·행동 버튼이 같은 박스 공유 |
| `.center` 스왑 | `myrpg.js` `talkToNpc()`, `fragments/npc-response.html` | `encounterMonster()`, `fragments/monster-response.html` |
| 가위바위보 3항 | `SkillType { NORMAL("일반"), HEAVY("강"), DEFENSE("방어") }` | 몬스터 행동도 **동일 enum 재사용**(신규 enum 만들지 않음) |
| 드랍 전용 아이템 표현 | `Item.buyPrice()` JavaDoc: "없으면(null) 상점 미판매(드랍 전용)" | 이미 열려 있음 |
| 처치 훅 | `SkillService.onSkillKill(characterId, skillId)` | 6순위에서 몬스터 처치 시 호출 |

이동 시 화면 갱신 경로도 이미 충분하다. `POST /move` → `fragments/move-response`가 `.top-bar` / `.center` / `.action-log` / `#mapGrid`를 교체하고, 몬스터 버튼은 `.center` 안 `.interactions`에 있으므로 **이동만으로 자동 갱신**된다. 필드 노드(`east-hill`, `dugald-north`, `dugald-south`, `west-forest`)에는 현재 NPC가 0개여서 상호작용 영역이 비어 있다 → 몬스터 버튼이 들어갈 자리다.

---

## 2. 데이터 설계

### 2.1 `src/main/resources/data/monster.json` (신규)

```json
[
  {
    "id": "raccoon",
    "name": "너구리",
    "type": "normal",
    "level": 1,
    "maxHp": 25,
    "attackPower": 4,
    "defense": 1,
    "critical": 10,
    "experience": 15,
    "goldDrop": { "min": 3, "max": 10 },
    "itemDrops": [
      { "itemId": "hp_potion_50", "chancePercent": 15, "minQuantity": 1, "maxQuantity": 1 }
    ],
    "lines": [
      "크르릉… 쉭, 쉭!",
      "(너구리가 몸을 잔뜩 웅크린 채 경계 태세를 갖춘다.)",
      "(너구리가 이빨을 드러내며 앞발을 천천히 들어올린다.)"
    ]
  }
]
```

> `lines`는 **정확히 3개**다: **1개는 울음/위협음**(`"크르릉… 쉭, 쉭!"`), **나머지 2개는 행동 묘사**(`"(…)"` 형태). 조우 때 이 3개 중 **랜덤 1개**가 노출된다.

**필드 스키마**

| 필드 | 필수 | 타입 | 설명 |
|---|---|---|---|
| `id` | ✅ | string | 고유 식별자. 중복 시 기동 실패 |
| `name` | ✅ | string | 표시 이름 |
| `type` | ✅ | string | `MonsterType.typeString` (`normal` \| `boss`). 미지 값은 기동 실패 |
| `level` | ✅ | int | 몬스터 레벨(≥1) |
| `maxHp` | ✅ | int | 최대 HP(≥1) |
| `attackPower` | ✅ | int | 공격력(≥0). 근접·마법 구분 없이 단일 값 |
| `defense` | ✅ | int | 방어(≥0) |
| `critical` | ✅ | int | 크리티컬 확률. **0.1% 단위 정수**(10 = 1.0%, 플레이어와 동일 규약) |
| `experience` | ✅ | long | 처치 시 획득 경험치. 지급은 6순위 |
| `goldDrop` | ✅ | object | `{ min, max }`, `0 ≤ min ≤ max`. **모든 몬스터 필수** |
| `itemDrops` | – | array | 아이템 드랍 테이블. 미기재 시 빈 목록 |
| `itemDrops[].itemId` | ✅ | string | `item.json`의 아이템 id. 미존재 시 기동 실패 |
| `itemDrops[].chancePercent` | ✅ | int | 1~100 |
| `itemDrops[].minQuantity` / `maxQuantity` | ✅ | int | `1 ≤ min ≤ max` |
| `lines` | ✅ | array | 조우 대사. **정확히 3개**(소리 1 + 행동 묘사 2). 개수가 3이 아니면 기동 실패. 조우 시 랜덤 1개 노출 |

> **제거된 필드** — `emoji`·`description`은 불필요하여 스키마에서 뺐다. 버튼 라벨은 **일반 몬스터는 이름만**("너구리"), **보스만 이름 뒤에 `👑` 배지**("너구리왕 👑")를 붙인다(배지는 `MonsterType`이 관리, 아래 3장). 도감/설명 용도는 현재 화면에 없어 보관하지 않는다.

**대사 설계 근거** — 너구리는 말을 못 하는 짐승이라, 대사 3개를 **소리 1개 + 행동 묘사 2개**로 구성한다. 소리는 울음/위협음(`"크르릉… 쉭, 쉭!"`), 행동 묘사는 지시문 형태(`"(너구리가 몸을 잔뜩 웅크린 채 경계 태세를 갖춘다.)"`)로 쓴다. 3개 중 랜덤 1개가 조우 화면에 나온다. `lines`가 정확히 3개임을 카탈로그 로딩 시 검증하므로 폴백 문구는 두지 않는다.

**보스 예시(향후, 데이터 미투입)**
```json
{
  "id": "golden-raccoon", "name": "황금 너구리", "type": "boss",
  "level": 10, "maxHp": 400, "attackPower": 30, "defense": 15, "critical": 80,
  "experience": 900, "goldDrop": { "min": 300, "max": 800 },
  "itemDrops": [ { "itemId": "enchant_scroll_basic", "chancePercent": 30, "minQuantity": 1, "maxQuantity": 1 } ],
  "lines": [
    "그르르르… 크아앙!",
    "(황금 너구리가 거대한 앞발로 땅을 내리찍는다.)",
    "(황금 너구리가 황금빛 털을 곤두세우며 노려본다.)"
  ]
}
```

### 2.2 `data/map.json` — 노드에 `monsters` 추가

`dugald-north` 노드에만 매핑한다(나머지 노드는 필드 미기재 = 빈 목록).

```diff
     {
       "id": "dugald-north",
       "name": "두갈드 아일 북부",
       "type": "field",
       "x": 0,
       "y": 1,
+      "monsters": ["raccoon"],
       "links": ["tir-chonaill", "dugald-south"]
     },
```

- **출현 매핑을 map.json에 두는 이유**: 한 종의 몬스터가 여러 맵에 출현할 수 있고("한 맵에 여러 몬스터" + "한 몬스터가 여러 맵"), 맵을 열었을 때 그 맵에 무엇이 나오는지 한눈에 보인다. NPC는 반대로 `npc.json`의 `nodeId`가 1:1이라 NPC 쪽에 두었다.
- 배열 순서 = 버튼 노출 순서.
- 동일 id 중복 기재는 기동 실패(같은 종 다수 스폰은 추후 별도 스펙).

---

## 3. 도메인 모델 (신규)

```java
// domain/model/MonsterType.java — 코드/라벨/버튼 배지/행동 라벨
public enum MonsterType {
    NORMAL("normal", "일반", "", List.of("전투")),
    BOSS("boss", "보스", "👑", List.of("전투"));

    public String typeString();
    public String label();
    public String badge();                    // 버튼 라벨 접미 배지(일반="", 보스="👑")
    public List<String> actionLabels();
    public static Optional<MonsterType> fromType(String type);
}
```

```java
// domain/model/Monster.java
public record Monster(
        String id, String name, MonsterType type,
        int level, int maxHp, int attackPower, int defense, int critical, long experience,
        GoldDrop goldDrop, List<ItemDrop> itemDrops,
        List<String> lines) {

    /**
     * 버튼 라벨. 일반은 이름만("너구리"), 보스는 이름 뒤에 배지("너구리왕 👑").
     * badge가 비면 이름만, 아니면 {@code name + " " + badge}.
     */
    public String buttonLabel() { ... }
}

// domain/model/GoldDrop.java
public record GoldDrop(int min, int max) { }        // 생성자에서 0 ≤ min ≤ max 검증

// domain/model/ItemDrop.java
public record ItemDrop(String itemId, int chancePercent, int minQuantity, int maxQuantity) { }
```

- **sealed 계층을 쓰지 않는 이유**: 일반/보스는 형태(필드 구성)가 같고 값만 다르다. 보스 전용 필드가 생기면 `Skill`(→`DamageSkill`/`DefenseSkill`)처럼 그때 sealed로 승격한다.
- 몬스터는 영속 대상이 아니다(불변 record + 메모리 카탈로그). 전투 중 현재 HP는 6순위의 전투 상태에서 관리한다.
- **플랫 스탯**(`attackPower`/`defense`/`critical`) — 플레이어 `Stats`(STR/DEX/INT/CRITICAL/DEF)를 재사용하지 않는다. 몬스터는 재능이 없어 공격 스탯이 하나면 충분하고, 플레이어 방어가 단일 `DEF`라 속성 구분이 무의미하다. `critical`은 플레이어와 같은 0.1% 단위 정수 규약(결정사항 참조).
- **MP·스태미나 없음** — 스킬을 쓰지 않으므로 자원 개념이 불필요하다.

```java
// application/dto/DropResult.java — 드랍 계산 결과
public record DropResult(long gold, List<DroppedItem> items) {
    public static final DropResult EMPTY = new DropResult(0L, List.of());
}

// application/dto/DroppedItem.java
public record DroppedItem(String itemId, int quantity) { }
```

### 3.1 `MapNode` 확장 (하위 호환 유지)

`MapNode`는 테스트 ~20곳에서 9인자로 직접 생성되므로, **컴포넌트를 추가하고 9인자 보조 생성자를 둔다** → 기존 호출부 무수정.

```java
public record MapNode(String id, String name, String type, NodeType nodeType,
                      int x, int y, String dungeonId, String theme,
                      List<String> links, List<String> monsters) {

    /** 몬스터 배치가 없는 노드용 생성자 (기존 호출부 호환). */
    public MapNode(final String id, final String name, final String type, final NodeType nodeType,
                   final int x, final int y, final String dungeonId, final String theme,
                   final List<String> links) {
        this(id, name, type, nodeType, x, y, dungeonId, theme, links, List.of());
    }
}
```

`MapService.parseNode()`는 `links`와 같은 방식으로 optional 배열을 파싱한다(`parseLinks`를 `parseStringArray(nodeJson, fieldName)`로 일반화해 `links`/`monsters` 공용).

---

## 4. 서비스 (신규)

### 4.1 `MonsterService` — 카탈로그 로딩 + 노드별 조회

```java
@Service
public class MonsterService {
    private static final String MONSTER_JSON_PATH = "data/monster.json";

    private final ObjectMapper objectMapper;      // tools.jackson (Jackson 3)
    private final MapService mapService;
    private final ItemCatalogService itemCatalogService;
    private List<Monster> monsters;

    @PostConstruct
    void init() { /* ClassPathResource → loadFromStream → validateMapPlacement/validateItemDrops */ }

    public List<Monster> loadFromStream(InputStream inputStream);   // 파싱 분리(프로퍼티 테스트 주입용)
    public List<Monster> all();
    public List<Monster> byNode(String nodeId);                     // map.json의 monsters 순서 보존
    public Optional<Monster> byId(String monsterId);
}
```

- `byNode`는 `mapService.graph().byId(nodeId)`(Optional)로 조회 → 미지 노드/`null`이면 빈 목록. `MapService.node()`(예외 던짐) 대신 관용 조회를 쓴다(`NpcService.byNode` 관례).
- **기동 시 교차 검증** (위반 시 `MonsterDataException` → 기동 실패):
  1. 몬스터 `id` 중복 금지.
  2. `map.json`의 모든 `monsters` 항목이 카탈로그에 존재.
  3. 노드별 `monsters` 배열 내 중복 금지.
  4. `itemDrops[].itemId`가 `item.json`에 존재(`ItemCatalogService.byId`).
  5. `goldDrop`/수량 범위·확률 범위·필수 필드 검증.
  6. `lines` 개수가 **정확히 3개**(아니면 기동 실패).
- `MapService`·`ItemCatalogService`를 생성자 주입하므로 Spring이 두 빈의 `@PostConstruct`를 먼저 완료시킨 뒤 이 빈을 초기화한다.
- 신규 예외: `application/exception/MonsterDataException`(`RuntimeException` 상속, `(String)`·`(String, Throwable)` 생성자 2개 — 기존 `*DataException`과 동일).

### 4.2 `MonsterDialogueService` — 조우 대사 선택

```java
@Service
public class MonsterDialogueService {
    private final Random random;                 // 기존 Random 빈 주입

    /** lines 3개 중 랜덤 1개 반환. lines는 카탈로그 검증으로 항상 3개가 보장된다. */
    public String selectLine(Monster monster);
}
```

- `lines`가 **정확히 3개**(소리 1 + 행동 묘사 2)임을 `MonsterService`가 기동 시 보장하므로, 폴백 문구가 필요 없다. `random.nextInt(3)`로 1개를 고른다.
- NPC와 달리 **시간대(`TimeOfDay`) 분기를 두지 않는다** — 짐승 대사에 아침/밤 구분은 의미가 적고, 필요해지면 `lines`를 객체로 확장한다(현재는 문자열 배열).

### 4.3 `MonsterAiService` — 가위바위보 행동 선택

```java
@Service
public class MonsterAiService {
    private static final int NORMAL_WEIGHT = 34;
    private static final int HEAVY_WEIGHT = 33;
    private static final int DEFENSE_WEIGHT = 33;
    private static final int TOTAL_WEIGHT = NORMAL_WEIGHT + HEAVY_WEIGHT + DEFENSE_WEIGHT;  // 100

    private final Random random;

    /** 0 이상 100 미만의 롤 값을 행동으로 변환한다(순수 함수, 테스트 진입점). */
    public SkillType actionFor(int roll) {
        if (roll < NORMAL_WEIGHT) return SkillType.NORMAL;                        // 0~33
        if (roll < NORMAL_WEIGHT + HEAVY_WEIGHT) return SkillType.HEAVY;          // 34~66
        return SkillType.DEFENSE;                                                 // 67~99
    }

    /** 다음 행동을 추첨한다. 6순위 전투가 턴마다 호출한다. */
    public SkillType nextAction() { return actionFor(random.nextInt(TOTAL_WEIGHT)); }
}
```

- 몬스터는 **스킬을 갖지 않는다** — `Skill`/`CharacterSkill`을 참조하지 않고 `SkillType` 3항만 쓴다. 따라서 랭크·배율·자원 소모도 없다(데미지 계산 규칙은 6순위에서 확정).
- 확률은 상수로 **34/33/33 고정(확정)** — 몬스터별 override는 두지 않는다.
- `actionFor(roll)`가 순수 함수라 프로퍼티 테스트에서 `0..99` 전 구간을 훑어 34/33/33 분포를 정확히 검증할 수 있다.

### 4.4 `MonsterRewardService` — 드랍 계산 (지급은 6순위)

```java
@Service
public class MonsterRewardService {
    private static final int PERCENT_BOUND = 100;
    private final Random random;

    /** 골드 드랍 범위에서 롤 값으로 금액을 산출한다(순수 함수). */
    public long goldFor(GoldDrop goldDrop, int roll);          // min + roll % (max - min + 1)

    /** 몬스터의 드랍(골드 + 아이템)을 추첨한다. */
    public DropResult rollDrop(Monster monster);
}
```

- `rollDrop`은 **골드 필수 + 아이템 0개 이상**을 담은 `DropResult`를 반환한다. 아이템 항목은 `chancePercent` 판정 통과 시 수량 범위에서 추첨.
- 6순위 전투가 처치 시 이 결과를 소비한다: `progress.gainGold(result.gold())` + (신설) `InventoryService.acquire(itemId, quantity)` + `actionLog.add(..., "reward")` / `"item"`.
- **인벤토리 획득 API가 아직 없다** — `InventoryService.seedDefault()`가 `new OwnedItem(...)`을 직접 저장할 뿐이다. 소비형 스택 누적·용량 30 검사(`InventoryFullException`)를 포함한 `acquire`는 6순위에서 신설한다(미결정 2).

### 4.5 `MonsterEncounterService` — 필드 진입 선공 판정

```java
@Service
public class MonsterEncounterService {
    private static final int PREEMPTIVE_STRIKE_PERCENT = 5;   // 전 맵 고정
    private static final int PERCENT_BOUND = 100;

    private final Random random;

    /**
     * 필드 진입 시 몬스터 선공을 판정한다.
     * 발동 시 노드의 몬스터 중 1마리를 랜덤 선택해 반환한다(6순위가 선공권 주체로 사용).
     * 몬스터가 없거나 5% 미발동이면 빈 Optional.
     */
    public Optional<Monster> rollPreemptiveStrike(final List<Monster> monsters) {
        if (monsters == null || monsters.isEmpty()) {
            return Optional.empty();
        }
        if (!triggers(random.nextInt(PERCENT_BOUND))) {
            return Optional.empty();
        }
        return Optional.of(monsters.get(random.nextInt(monsters.size())));
    }

    /** roll(0~99)이 선공 임계값 미만이면 발동(순수 함수, 테스트 진입점). */
    public boolean triggers(final int roll) {
        return roll < PREEMPTIVE_STRIKE_PERCENT;
    }
}
```

- **왜 서버 판정인가**: 6순위의 실제 선공 전투가 서버 주도(전투 상태·데미지 계산이 서버)라, 판정 위치를 지금부터 서버에 두면 6순위에서 `alert` 신호만 `POST /battle/start` 호출로 바꾸면 된다.
- **왜 `Optional<Monster>` 인가**: 두갈드 아일 북부에 `너구리`, `너구리왕`이 함께 있으면 5% 발동 시 둘 중 하나가 랜덤으로 선공한다. 6순위는 "누가 선공했는지"가 필요하므로 boolean이 아니라 선택된 몬스터를 돌려준다.
- `triggers(roll)`가 순수 함수라 경계값(`4`→발동, `5`→미발동)과 5% 분포를 프로퍼티 테스트로 검증한다. `rollPreemptiveStrike`는 고정 시드 `Random`으로 결정적 테스트.
- **이번 범위**: 발동 여부·선택만. 실제 전투 진입은 6순위(이월).

---

## 5. 뷰 / UI

### 5.1 화면 흐름

```
[두갈드 아일 북부로 이동]
┌──────────────────────────────────────┐
│ 두갈드 아일 북부. 바람에 풀이 눕는다.    │  ← .situation (ambience)
├──────────────────────────────────────┤
│                                      │  ← .npc-talk (비어 있음)
├──────────────────────────────────────┤
│  [ 너구리 ]                           │  ← .interactions (붉은 계열 = 몬스터, 일반은 이모지 없음)
├──────────────────────────────────────┤
│  (미니맵)                             │
└──────────────────────────────────────┘

[너구리 클릭]
┌──────────────────────────────────────┐
│ 두갈드 아일 북부. 바람에 풀이 눕는다.    │
├──────────────────────────────────────┤
│ 너구리  Lv.1 · HP 25                  │  ← .monster-name + .monster-meta(작게)
│ 크르릉… 쉭, 쉭!                        │  ← 대사 (3개 중 랜덤 1개)
│ ─────────────────────────────         │
│ [ 전투 ]                              │  ← .npc-actions.monster-actions
├──────────────────────────────────────┤
│  [ 너구리 ]                           │
├──────────────────────────────────────┤
│  (미니맵)                             │
└──────────────────────────────────────┘
```

### 5.2 `PlayScreenView` 확장 (하위 호환 유지)

`PlayScreenView`는 컨트롤러 테스트 8곳에서 10인자로 직접 생성되므로, `MapNode`와 같은 방식으로 **보조 생성자**를 둔다.

```java
public record PlayScreenView(
        TopBarView topBar, MinimapView minimap, FullMapView fullMap, String ambience,
        String npcName, String npcDialogue,
        String monsterName, String monsterDialogue,          // 신규
        Integer monsterLevel, Integer monsterMaxHp,          // 신규 (조우 시 이름 옆 표시, 몬스터 없으면 null)
        List<InteractionItem> interactions,
        List<ActionButton> npcActions,
        List<ActionButton> monsterActions,                   // 신규
        List<ActionLogEntry> logs, InfoPopupView info) {

    /** 몬스터 조우가 없는 화면용 생성자 (기존 호출부 호환). */
    public PlayScreenView(TopBarView topBar, MinimapView minimap, FullMapView fullMap, String ambience,
                          String npcName, String npcDialogue, List<InteractionItem> interactions,
                          List<ActionButton> npcActions, List<ActionLogEntry> logs, InfoPopupView info) {
        this(topBar, minimap, fullMap, ambience, npcName, npcDialogue, null, null, null, null,
             interactions, npcActions, null, logs, info);
    }
}
```

- **`NpcActionButton` → `ActionButton` 리네임**: 필드 1개(`label`)뿐인 record를 몬스터 행동 버튼에도 쓰므로 이름을 중립화한다. 영향은 기계적 참조 교체 6곳(DTO 1, `PlayScreenViewHelper` 1, 테스트 4). `NpcActionButtonsPropertyTest` 클래스명은 002 스펙 추적성 때문에 유지하고 타입 참조만 바꾼다.
- NPC 대사와 몬스터 대사는 **동시에 활성되지 않는다**(둘 중 하나만 non-null). NPC 클릭 시 몬스터 슬롯이 비고, 몬스터 클릭 시 NPC 슬롯이 빈다.

### 5.3 `PlayScreenViewHelper` 확장

파라미터 폭증을 막기 위해 대사 대상을 **묶음 record**로 받는 오버로드를 추가하고, 기존 오버로드는 이 오버로드에 위임한다(기존 Mockito 스텁 무수정).

```java
// application/dto/TalkTarget.java — 현재 대사 대상 묶음
public record TalkTarget(Npc npc, Monster monster, String dialogue) {
    public static final TalkTarget EMPTY = new TalkTarget(null, null, null);
    public static TalkTarget ofNpc(Npc npc, String dialogue);
    public static TalkTarget ofMonster(Monster monster, String dialogue);
}
```

```java
// PlayScreenViewHelper 신규/변경
/** NPC + 몬스터를 한 목록으로 합친다. NPC 버튼 먼저, 이어서 몬스터 버튼(각각 정의 순서). */
public List<InteractionItem> buildInteractions(List<Npc> npcs, List<Monster> monsters);

/** 신규 진입점. 기존 5/8/9-파라미터 오버로드는 TalkTarget.EMPTY / ofNpc(...)로 위임. */
public PlayScreenView buildPlayScreen(CharacterProgress progress, MinimapView minimap,
        FullMapView fullMap, String ambience, List<InteractionItem> interactions,
        TalkTarget talkTarget, List<ActionLogEntry> logs, InfoPopupView info);

private InteractionItem toInteractionItem(Monster monster) {
    return new InteractionItem(monster.id(), monster.buttonLabel(), false);   // npc=false
}
private List<ActionButton> buildMonsterActions(Monster monster) {
    return monster == null ? null
            : monster.type().actionLabels().stream().map(ActionButton::new).toList();
}
```

기존 `buildInteractions(List<Npc>)`는 유지하고 내부에서 `buildInteractions(npcs, List.of())`로 위임한다.

`talkTarget.monster()`가 있으면 `buildPlayScreen`이 `monsterName`(이름)·`monsterLevel`(`level`)·`monsterMaxHp`(`maxHp`)·`monsterDialogue`(대사)를 함께 채운다. NPC 대상이거나 대상이 없으면 이 4개는 모두 `null`이다.

### 5.4 `templates/fragments/center.html` 변경

```diff
-    <!-- NPC 멘트 + 행동 버튼 (스크롤 영역) -->
+    <!-- NPC/몬스터 멘트 + 행동 버튼 (스크롤 영역) -->
     <div class="npc-talk" id="npcTalk">
         <th:block th:if="${view.npcName != null}">
             <span class="npc-name" th:text="${view.npcName}"></span>
             <p th:text="${view.npcDialogue}"></p>
         </th:block>
+        <th:block th:if="${view.monsterName != null}">
+            <span class="monster-name" th:text="${view.monsterName}"></span>
+            <span class="monster-meta" th:if="${view.monsterLevel != null}"
+                  th:text="'Lv.' + ${view.monsterLevel} + ' · HP ' + ${view.monsterMaxHp}"></span>
+            <p th:text="${view.monsterDialogue}"></p>
+        </th:block>
         <div class="npc-actions" id="npcActions"
              th:if="${view.npcActions != null and !view.npcActions.isEmpty()}">
             <button th:each="action : ${view.npcActions}"
                     th:text="${action.label}"
                     onclick="npcAction(this.textContent)">
             </button>
         </div>
+        <div class="npc-actions monster-actions" id="monsterActions"
+             th:if="${view.monsterActions != null and !view.monsterActions.isEmpty()}">
+            <button th:each="action : ${view.monsterActions}"
+                    th:text="${action.label}"
+                    onclick="monsterAction(this.textContent)">
+            </button>
+        </div>
     </div>

     <!-- 상호작용 버튼 -->
     <div class="interactions" id="interactions">
         <th:block th:if="${view.interactions != null}">
             <button th:each="item : ${view.interactions}"
                     th:classappend="${item.npc} ? 'npc' : ''"
                     th:text="${item.name}"
-                    th:attr="data-npc-id=${item.npc ? item.id : null}"
-                    onclick="if(this.dataset.npcId){talkToNpc(this.dataset.npcId)}">
+                    th:attr="data-npc-id=${item.npc ? item.id : null},
+                             data-monster-id=${item.npc ? null : item.id}"
+                    onclick="onInteractionClick(this)">
             </button>
         </th:block>
     </div>
```

`templates/fragments/monster-response.html` (신규, `npc-response.html`과 동일 구조):
```html
<div th:fragment="monster-response">
    <section class="center" id="center" th:replace="~{fragments/center :: center}"></section>
</div>
```

### 5.5 `static/css/myrpg.css` 추가

```css
/* 몬스터 이름 (대사 영역) — 이름과 레벨·HP를 한 줄에, 대사(<p>)는 아래 줄 */
.npc-talk .monster-name { color: #d98b8b; font-weight: bold; }

/* 몬스터 레벨·HP (이름 옆 작은 글씨) */
.npc-talk .monster-meta { margin-left: 6px; font-size: 0.75em; color: #b08a8a; }

/* 몬스터 행동 버튼 — 상호작용 몬스터 버튼과 같은 붉은 계열 */
.npc-actions.monster-actions button {
    background: #3a2a2a;
    color: #ffcccc;
    border-color: #6a4a4a;
}
```

상호작용 버튼(`.interactions button`)은 이미 붉은 계열이 기본이고 `.npc`가 녹색 오버라이드이므로 **몬스터 버튼용 CSS 추가는 불필요**하다. `.log-combat`도 이미 정의되어 있다.

### 5.6 `static/js/myrpg.js` 변경

`.center` 스왑 로직이 NPC/몬스터 공통이므로 `swapCenter(html)`로 추출한다.

```javascript
function swapCenter(html) {
    if (!html) { return; }
    var container = document.createElement("div");
    container.innerHTML = html;
    var newCenter = container.querySelector(".center");
    if (!newCenter) { return; }
    var oldCenter = document.querySelector(".center");
    if (oldCenter) { oldCenter.replaceWith(newCenter); }
}

function onInteractionClick(el) {
    if (el.dataset.npcId) { talkToNpc(el.dataset.npcId); return; }
    if (el.dataset.monsterId) { encounterMonster(el.dataset.monsterId); }
}

// ===== 몬스터 조우: POST /monster/encounter 호출 + .center swap =====
function encounterMonster(monsterId) {
    fetch("/monster/encounter?monsterId=" + monsterId, { method: "POST" })
        .then(function (response) { return response.ok ? response.text() : null; })
        .then(swapCenter);
}

// ===== 몬스터 행동 버튼 =====
function monsterAction(label) {
    // 6순위(전투)에서 label === '전투' → POST /battle/start 로 교체
    alert("구현 예정입니다");
}
```

`talkToNpc()`도 동일하게 `swapCenter`를 쓰도록 정리한다(동작 변화 없음).

**이동 응답에서 선공 신호 처리** — 기존 `move(dx, dy)`가 이동 응답 프래그먼트를 스왑한 **직후**, 응답에 선공 신호(`#preemptiveSignal`)가 있으면 alert 한다. 서버가 5% 판정을 이미 마쳤으므로 프런트는 신호 유무만 본다.

```javascript
// move() 내부, 프래그먼트 스왑 이후 추가
var preemptive = container.querySelector("#preemptiveSignal");
if (preemptive) {
    // 6순위(전투): 여기서 POST /battle/start (몬스터 선공권) 로 교체
    alert("몬스터 선공 발동");
}
```
`container`는 `move()`가 이동 응답 HTML을 파싱해 둔 임시 요소(기존 `.top-bar`/`.center`/`.action-log`/`#mapGrid` 추출에 쓰던 것과 동일)라 **DOM에 삽입하지 않아도 조회 가능**하다. 선공 신호는 `move-response.html`에 발동 시에만 렌더된다(아래 5.7).

### 5.7 이동 시 몬스터 선공 (`POST /move` 확장)

새 엔드포인트가 아니라 **기존 `/move` 처리에 선공 판정을 얹는다**. 조우(`/monster/encounter`, 수동 클릭)와 별개의 자동 이벤트다.

```java
// PlayScreenController.move(...) — MovementResult.Moved 분기 내부
if (result instanceof MovementResult.Moved) {
    characterService.saveTurn(progress);

    final List<Monster> monstersOnNode = monsterService.byNode(progress.getCurrentNodeId());
    final Optional<Monster> ambusher = monsterEncounterService.rollPreemptiveStrike(monstersOnNode);
    ambusher.ifPresent(monster -> {
        actionLog.add(monster.name() + "이(가) 선공을 걸어왔다!", COMBAT_TYPE);
        model.addAttribute("preemptiveMonsterName", monster.name());   // 신호(6순위에서 전투 진입으로 교체)
    });
}
```

`templates/fragments/move-response.html` (선공 신호 요소 추가):
```html
<!-- 선공 발동 시에만 렌더. JS가 존재 여부만 확인해 alert -->
<div id="preemptiveSignal" th:if="${preemptiveMonsterName != null}"
     th:attr="data-monster=${preemptiveMonsterName}"></div>
```

- 선택된 몬스터 이름을 `data-monster`에 실어둔다 → **6순위에서 "누가 선공했는지"를 그대로 사용**(지금 alert 문구는 고정 "몬스터 선공 발동").
- 판정은 **`Moved`일 때만** 수행 → 새로고침(`GET /`)·이동 거부(`Blocked`)에서는 발동하지 않는다.
- 몬스터가 없는 노드(마을 등)는 `rollPreemptiveStrike`가 빈 목록으로 즉시 빈 `Optional` → 신호 없음.

### 5.8 엔드포인트

| 메서드 | 경로 | 동작 | 반환 |
|---|---|---|---|
| POST | `/monster/encounter?monsterId=` | 대사 추첨 + 행동 버튼 구성 + 조우 로그 | `fragments/monster-response` |

```java
@PostMapping("/monster/encounter")
public String encounterMonster(@RequestParam final String monsterId, final Model model) {
    // 1. progress 로드, 현재 노드의 NPC + 몬스터로 interactions 재구성
    // 2. monsterService.byId(monsterId) → 없으면 talkTarget = TalkTarget.EMPTY (NPC 관용 설계와 동일, 예외 없음)
    // 3. monsterDialogueService.selectLine(monster)
    // 4. actionLog.add(monster.name() + "와(과) 마주쳤다.", COMBAT_TYPE)
    // 5. buildPlayScreen(..., TalkTarget.ofMonster(monster, dialogue), logs, info)
    return "fragments/monster-response";
}
```

- 미지 `monsterId`는 예외 없이 대사·행동 버튼만 비운 채 정상 렌더링한다(`talkToNpc`의 관용 설계와 동일).
- 현재 노드에 배치되지 않은 몬스터 id가 들어오면 대사를 만들지 않는다(노드 소속 검증).
- `PlayScreenController`에 상수 `COMBAT_TYPE = "combat"` 추가.

### 5.9 `buildViewFromProgress` 변경

```diff
     final List<Npc> npcsOnNode = npcService.byNode(currentNodeId);
-    final List<InteractionItem> interactions = playScreenViewHelper.buildInteractions(npcsOnNode);
+    final List<Monster> monstersOnNode = monsterService.byNode(currentNodeId);
+    final List<InteractionItem> interactions =
+            playScreenViewHelper.buildInteractions(npcsOnNode, monstersOnNode);
```

`PlayScreenController` 생성자에 `MonsterService`, `MonsterDialogueService`, `MonsterEncounterService` 3개가 추가된다 → `@WebMvcTest` 3개 클래스(`PlayScreenControllerTest`, `PlayScreenControllerNpcTest`, `PlayScreenControllerProgressionTest`)에 `@MockitoBean` 3개를 추가해야 한다. 기존 `/move` 테스트는 `monsterEncounterService.rollPreemptiveStrike(...)`가 기본 `Optional.empty()`를 반환하도록 스텁하면 선공 없이 기존 검증이 그대로 통과한다.

---

## 6. 영향 파일 요약

| 구분 | 파일 | 변경 |
|---|---|---|
| 데이터 | `resources/data/monster.json` | **신규** (너구리 1종) |
| 데이터 | `resources/data/map.json` | `dugald-north`에 `monsters: ["raccoon"]` |
| 도메인 | `domain/model/MonsterType.java` | **신규** |
| 도메인 | `domain/model/Monster.java`, `GoldDrop.java`, `ItemDrop.java` | **신규** |
| 도메인 | `domain/model/MapNode.java` | `monsters` 컴포넌트 + 9인자 보조 생성자 |
| 서비스 | `application/service/MonsterService.java` | **신규** 로딩·교차검증·`byNode`/`byId` |
| 서비스 | `application/service/MonsterDialogueService.java` | **신규** 대사 선택 |
| 서비스 | `application/service/MonsterAiService.java` | **신규** 가위바위보 34/33/33 |
| 서비스 | `application/service/MonsterRewardService.java` | **신규** 드랍 계산 |
| 서비스 | `application/service/MonsterEncounterService.java` | **신규** 필드 진입 선공 판정(5% 고정) |
| 서비스 | `application/service/MapService.java` | `parseNode`에 `monsters` optional 배열 파싱 |
| 예외 | `application/exception/MonsterDataException.java` | **신규** |
| 뷰모델 | `application/dto/ActionButton.java` | `NpcActionButton` **리네임** |
| 뷰모델 | `application/dto/PlayScreenView.java` | 몬스터 3필드 + 10인자 보조 생성자 |
| 뷰모델 | `application/dto/TalkTarget.java` | **신규** |
| 뷰모델 | `application/dto/DropResult.java`, `DroppedItem.java` | **신규** |
| 뷰조립 | `interfaces/api/PlayScreenViewHelper.java` | `buildInteractions(npcs, monsters)`, `buildMonsterActions`, `TalkTarget` 오버로드 |
| 컨트롤러 | `interfaces/api/PlayScreenController.java` | `/monster/encounter` + 몬스터 서비스 3개 주입 + `buildViewFromProgress` + `move()`에 선공 판정 |
| 템플릿 | `fragments/center.html` | 몬스터 대사·행동 버튼, `onInteractionClick` |
| 템플릿 | `fragments/monster-response.html` | **신규** |
| 템플릿 | `fragments/move-response.html` | 선공 신호 요소(`#preemptiveSignal`) 추가 |
| 정적 | `static/js/myrpg.js` | `swapCenter`, `onInteractionClick`, `encounterMonster`, `monsterAction`, `move()` 선공 alert |
| 정적 | `static/css/myrpg.css` | `.monster-name`, `.monster-actions button` |

> 상단바·사이드바·미니맵·인벤토리·은행은 변경 없음. `item.json`도 변경 없음 — 너구리가 드랍하는 `hp_potion_50`은 이미 `item.json`에 존재한다(신규 아이템 추가 아님).

---

## 7. 테스트 계획

`code-style.md`의 기준(단위 `@ExtendWith(MockitoExtension.class)`, 프로퍼티 jqwik `@Property` + `Mockito.mock()`, 컨트롤러 `@WebMvcTest` + `@MockitoBean`)을 따른다. 기존 NPC 테스트 구성과 대칭으로 만든다.

| 테스트 | 유형 | 검증 |
|---|---|---|
| `MonsterTypeTest` | 단위 | 타입 문자열·라벨·배지(일반=""/보스="👑")·행동 라벨, `fromType` 미지값 |
| `MonsterTypeCompletenessPropertyTest` | 프로퍼티 | 모든 상수가 라벨·행동 라벨을 비우지 않음 |
| `MonsterServiceLoadIntegrationTest` | 통합 | 실제 `monster.json` 로딩, 너구리 필드 값 |
| `MonsterServiceParsingPropertyTest` | 프로퍼티 | 인메모리 JSON(`objectMapper.createArrayNode()`) 파싱, 필드 보존 |
| `MonsterServiceByNodePropertyTest` | 프로퍼티 | `map.json` 배치 순서 보존, 미지 노드·`null` → 빈 목록 |
| `MonsterServiceLoadFailurePropertyTest` | 프로퍼티 | id 중복·미지 type·필수 필드 누락·미존재 itemId·범위 위반·`lines` 개수 ≠ 3 → `MonsterDataException` |
| `MonsterDialogueServicePropertyTest` | 프로퍼티 | 고정 `Random`으로 3개 중 선택 결정성, 반환값이 항상 `lines`에 포함 |
| `MonsterAiActionDistributionPropertyTest` | 프로퍼티 | `actionFor(0..99)` 분포가 정확히 34/33/33, 경계값(33/34/66/67) |
| `MonsterRewardServicePropertyTest` | 프로퍼티 | 골드가 항상 `[min, max]`, 확률 0/100 경계, 수량 범위 |
| `MonsterEncounterServicePropertyTest` | 프로퍼티 | `triggers(0..99)` 경계(`4`→발동, `5`→미발동), 5% 분포, 빈 목록 → 빈 Optional, 고정 시드로 선택 몬스터 결정성 |
| `MonsterInteractionLabelPropertyTest` | 프로퍼티 | 버튼 라벨 포맷(일반=이름만, 보스=`이름 👑`), `InteractionItem.npc == false` |
| `MonsterActionButtonsPropertyTest` | 프로퍼티 | `monsterActions` 라벨이 `MonsterType.actionLabels()`와 개수·순서 일치 |
| `PlayScreenControllerMonsterTest` | `@WebMvcTest` | 이동 후 몬스터 버튼 노출, `/monster/encounter` → `fragments/monster-response` + `monsterName`/`monsterLevel`/`monsterMaxHp`/`monsterDialogue`/`monsterActions`, 미지 id 관용 처리, NPC·몬스터 슬롯 배타성 |
| `PlayScreenControllerPreemptiveTest` | `@WebMvcTest` | `/move`에서 `rollPreemptiveStrike`가 몬스터 반환 시 `preemptiveMonsterName` 모델 속성·`combat` 로그 존재, 빈 Optional이면 신호 없음 |
| `MonsterContextLoadSmokeTest` | `@SpringBootTest` | 몬스터 빈 포함 컨텍스트 기동 |

정적 리소스 보존 검증 테스트(`VisualJsPreservationAndJsonLoadingIntegrationTest`)가 JS/JSON 문자열을 검사하므로, `myrpg.js`·`center.html` 변경 시 이 테스트의 기대값도 함께 갱신한다.

---

## 8. 구현 순서 (Task 초안)

**A. 데이터 · 도메인**
1. `MonsterType` + `Monster`/`GoldDrop`/`ItemDrop` + `MonsterDataException` (단위·프로퍼티 테스트).
2. `MapNode.monsters` 컴포넌트 + 9인자 보조 생성자, `MapService.parseNode`의 `monsters` optional 파싱, `map.json`에 `dugald-north` 매핑 추가 (기존 맵 테스트 무회귀 확인).
3. `data/monster.json`(너구리) + `MonsterService`(로딩·교차검증·`byNode`/`byId`) (로딩 통합 + 파싱/실패 프로퍼티 테스트).

**B. 대사 · AI · 드랍 · 선공**
4. `MonsterDialogueService` (고정 `Random` 프로퍼티 테스트).
5. `MonsterAiService`(34/33/33) (`actionFor` 전 구간 분포 프로퍼티 테스트).
6. `MonsterRewardService` + `DropResult`/`DroppedItem` (골드 범위·확률 경계 프로퍼티 테스트).
7. `MonsterEncounterService`(선공 5% 고정 + 랜덤 선택) (`triggers` 경계·분포·빈 목록 프로퍼티 테스트).

**C. 뷰 · UI**
8. `NpcActionButton` → `ActionButton` 리네임 (참조 6곳, 무회귀 확인).
9. `TalkTarget` + `PlayScreenView` 확장(보조 생성자) + `PlayScreenViewHelper`(`buildInteractions(npcs, monsters)`, `buildMonsterActions`, `TalkTarget` 오버로드) (라벨·행동버튼 프로퍼티 테스트).
10. `PlayScreenController`에 `/monster/encounter` + 몬스터 서비스 3개 주입 + `buildViewFromProgress` 갱신 + `move()` 선공 판정 + `fragments/monster-response.html` + `move-response.html` 선공 신호 (`@WebMvcTest`, 기존 컨트롤러 테스트 3곳에 `@MockitoBean` 3개 추가).
11. `center.html`·`myrpg.js`(조우 + 이동 선공 alert)·`myrpg.css` 수정 + 정적 리소스 보존 테스트 갱신 + 스모크 테스트.

> 각 Task는 `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` 빌드 성공으로 완료 처리한다(`task-build-validation.md`).
> 완료 후 수동 확인 시나리오: 티르코네일 → **아래(두갈드 아일 북부) 이동** → (5% 발동 시 `"몬스터 선공 발동"` alert) → `너구리` 버튼(이모지 없음) 확인 → 클릭 → 대사 + `전투` 버튼 확인 → `전투` 클릭 시 "구현 예정입니다" → 다시 티르코네일로 이동 시 몬스터 버튼 사라지고 NPC 버튼만 노출.
