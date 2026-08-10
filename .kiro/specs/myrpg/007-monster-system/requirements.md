# Requirements Document

## Introduction

본 스펙(007)은 `myrpg` 모듈(`com.myapps.web.myrpg`)에 **몬스터 시스템**을 추가한다. 스펙 001~006이 구축한 맵/이동(`MapService`·`MapNode`·`MovementService`), NPC 파이프라인(`NpcService`·`NpcType`·`NpcDialogueService`·`center.html`), 화면 조립(`PlayScreenController`·`PlayScreenViewHelper`·`InteractionItem`·`PlayScreenView`), 가위바위보 3항(`SkillType`), 캐릭터/스탯(`CharacterProgress`·`Stats`), 골드/아이템(`CharacterProgress.gold`·`Item`·`OwnedItem`) 위에서 동작한다. 상세 설계 배경과 확정 사항은 `docs/monster-system.md`를 근거로 한다.

핵심 방향은 004~006의 "계산형/저장형/카탈로그형 구분" 원칙의 재사용이다. 몬스터는 NPC 파이프라인("맵별 배치 → 상호작용 버튼 → 클릭 시 대사 + 행동 버튼")의 완성 레퍼런스를 그대로 복제·확장한다.

- **몬스터 정의**(스탯·드랍·대사)는 계속 늘어나는 콘텐츠 데이터이므로 **`data/monster.json` 카탈로그**로 분리한다(`skill.json`/`npc.json` 선례).
- **맵별 출현 매핑**은 "한 맵에 여러 몬스터, 한 몬스터가 여러 맵"을 지원하고 맵 열람 시 한눈에 보이도록 **`data/map.json` 노드의 `monsters` 배열**(맵 데이터가 단일 진실)에 둔다.
- **타입·행동 확률·선공 판정·드랍 계산**은 로직 결합이라 **코드(enum/순수 정책 서비스)** 로 둔다. 몬스터는 영속 대상이 아니라 **불변 record + 메모리 카탈로그**이며 전투 중 상태는 6순위에서 관리한다.

이번 스펙의 범위:

1. **몬스터 카탈로그** — `monster.json`을 기동 시 로드·검증(`MonsterService`, `SkillCatalogService`/`NpcService` 선례). 실제로 쓸 몬스터는 `너구리`(가장 약한 일반) 1종만 정의.
2. **몬스터 분류** — `MonsterType`(NORMAL/BOSS): 코드·라벨·버튼 배지(일반="", 보스="👑")·행동 라벨.
3. **몬스터 데이터 모델** — `level`/`maxHp`/`attackPower`/`defense`/`critical`(플랫 스탯) + `experience` + `goldDrop{min,max}`(필수) + `itemDrops[]`(확률·수량) + `lines`(정확히 3개).
4. **맵 출현 매핑** — `map.json` 노드에 `monsters` 배열 추가, `MapNode` 확장(하위 호환), 노드별 조회(`byNode`). 몬스터는 정적·상주(처치해도 맵 유지).
5. **조우 대사** — `lines` 3개(소리 1 + 행동 묘사 2) 중 랜덤 1개. 시간대 분기 없음.
6. **가위바위보 AI** — 스킬 없이 `SkillType`(일반/강/방어)을 34/33/33 고정 확률로 발동(정책+테스트). 실제 턴 소비는 6순위.
7. **드랍 계산** — 골드(필수)+아이템(확률·수량) 추첨(정책+테스트). 실제 지급은 6순위.
8. **몬스터 선공** — 필드 진입 5%(전 맵 고정) 확률로 노드 몬스터 중 1마리 랜덤 선공. 이번 범위는 서버 판정 + alert 신호까지, 실제 전투 진입은 6순위.
9. **조우 UI** — 상호작용 버튼 노출(NPC와 동일 자리), 클릭 시 이름 + (옆에 작게) 레벨·HP + 대사 + `전투` 버튼(플레이스홀더).

전투 턴 처리·드랍 지급·HP 감소·선공 실전투 진입·내구도 감소·크리티컬 배율은 6순위로, 보스 실데이터·인챈트 드랍은 인챈트 스펙 확정 후로, 던전 내부 몬스터는 10순위로, 보스 필드 랜덤 등장은 추후 기능으로 이연한다.

## Glossary

### 기존(006 이하) 재사용 용어

- **Myrpg_Web_Module**: `com.myapps.web.myrpg` 기본 패키지의 Spring Boot 4.0 Web 모듈.
- **Catalog_Loading_Pattern**: `SkillCatalogService`/`NpcService`/`ItemCatalogService`가 `classpath:data/*.json`을 기동 시 1회 파싱·검증하고 무결성 위반 시 전용 `*DataException`으로 기동을 실패시키는 로딩 패턴. 파싱을 `loadFromStream(InputStream)`으로 분리해 인메모리 주입 테스트가 가능하다. Jackson 3(`tools.jackson`) 사용.
- **Map_Node / Map_Service / Map_Graph**: 맵 노드(불변 record)·맵 로딩 서비스·불변 그래프. `MapService.parseNode`가 `theme` 등 optional 필드를 `nodeJson.has(...)`로 파싱하며, `MapGraph.byId(nodeId)`는 Optional을 반환한다.
- **Movement_Result**: 이동 결과 sealed(`Moved`/`Blocked`/`DungeonLocked`). `PlayScreenController.move`가 `Moved`일 때만 진행 저장·후처리를 수행한다.
- **Npc / Npc_Type / Npc_Dialogue_Service**: NPC·타입 enum(코드/라벨/이모지/행동 라벨)·대사 랜덤 선택 서비스(`Clock`·`Random` 빈 주입).
- **Interaction_Item**: 상호작용 대상 뷰 record `(String id, String name, boolean npc)`. `npc=false`가 몬스터 용도로 이미 문서화되어 있다.
- **Play_Screen_View / Play_Screen_View_Helper**: 플레이 화면 뷰 record와 그 조립기. `buildInteractions`·`buildPlayScreen`·게이지 조립을 수행한다.
- **Npc_Action_Button**: 행동 버튼 뷰 record `(String label)`. 본 스펙에서 `ActionButton`으로 리네임된다.
- **Npc_Response_Fragment / Center_Fragment**: `center.html`(상황 멘트·대사·행동 버튼·상호작용 버튼·미니맵)과 이를 교체 렌더하는 `npc-response.html`. `myrpg.js`가 `.center`를 스왑한다.
- **Move_Response_Fragment**: `POST /move` 응답 프래그먼트(`.top-bar`/`.center`/`.action-log`/`#mapGrid` 교체).
- **Skill_Type**: 가위바위보 3항 enum. `NORMAL("일반")`/`HEAVY("강")`/`DEFENSE("방어")` + `fromString`.
- **Skill_Service_Kill_Hook**: `SkillService.onSkillKill(characterId, skillId)`. 몬스터 처치 시 스킬 킬 카운트를 올리는 연결 지점(6순위에서 호출).
- **Character_Progress**: 유일한 캐릭터 진행 엔티티. `gold`·`gainGold`가 존재하며, HP 감소 메서드는 본 스펙 범위 밖(6순위 신설).
- **Stats**: STR/DEX/INT/Critical(0.1%단위 정수)/DEF를 담는 표시 VO.
- **Item / Item_Catalog_Service / Owned_Item**: 아이템 정의(sealed)·카탈로그 서비스·보유 인스턴스 엔티티. `Item.buyPrice()`가 null이면 상점 미판매(드랍 전용)로 이미 열려 있다.
- **Random_Bean**: `ApplicationServiceConfiguration`이 제공하는 `Random` 빈. 대사·AI·드랍·선공의 난수원(테스트에서 고정/시드).
- **Action_Log**: 세션 스코프 활동 로그(`add(message, type)`). 타입별 CSS(`.log-combat` 등)가 이미 존재한다.

### 본 스펙(007) 신규 용어

- **Monster_Catalog**: `classpath:data/monster.json`에 정의된 몬스터 목록. 기동 시 로드되어 불변 `Monster` 목록으로 보관된다.
- **Monster**: 카탈로그 항목(불변 record). `id`/`name`/`type`/`level`/`maxHp`/`attackPower`/`defense`/`critical`/`experience`/`goldDrop`/`itemDrops`/`lines`. 영속 대상이 아니다.
- **Monster_Id**: 몬스터 정체성 키(예: `"raccoon"`). `map.json`의 `monsters` 배열이 참조하는 단일 소스.
- **Monster_Type**: 몬스터 타입 enum. `NORMAL`(일반) / `BOSS`(보스). 코드·라벨·버튼 배지(`badge`: 일반="", 보스="👑")·행동 라벨(`["전투"]`)을 내장한다.
- **Monster_Stats**: 몬스터 전용 플랫 스탯. `attackPower`(공격력, 근접·마법 구분 없는 단일 값)·`defense`(방어)·`critical`(크리티컬 확률, 0.1% 단위 정수). 플레이어 `Stats`(DEX/INT 포함)를 재사용하지 않는다.
- **Gold_Drop**: 골드 드랍 범위 record `(int min, int max)`. `0 ≤ min ≤ max`. 모든 몬스터 필수.
- **Item_Drop**: 아이템 드랍 테이블 항목 record `(String itemId, int chancePercent, int minQuantity, int maxQuantity)`.
- **Monster_Lines**: 몬스터 조우 대사 목록. **정확히 3개**(소리 1 + 행동 묘사 2). 조우 시 랜덤 1개 노출.
- **Monster_Service**: 카탈로그 로딩·교차검증·조회(`all`/`byId`/`byNode`) 서비스. `MapService`·`ItemCatalogService`를 주입하여 맵 배치·드랍 아이템 존재를 교차검증한다.
- **Monster_Dialogue_Service**: 조우 대사 랜덤 선택 서비스. `Random` 빈 주입, 시간대 분기 없음.
- **Monster_Ai_Service**: 가위바위보 행동 선택 서비스. `actionFor(roll)` 순수 함수 + `nextAction()`. 34/33/33 고정.
- **Monster_Reward_Service**: 드랍 계산 서비스. `goldFor(goldDrop, roll)` 순수 함수 + `rollDrop(monster)`.
- **Monster_Encounter_Service**: 필드 진입 선공 판정 서비스. `triggers(roll)` 순수 함수 + `rollPreemptiveStrike(monsters):Optional<Monster>`.
- **Monster_Data_Exception**: 카탈로그 로드/검증 실패 시 던지는 예외(`RuntimeException` 상속, `SkillDataException`/`NpcDataException` 선례).
- **Drop_Result / Dropped_Item**: 드랍 계산 결과 record `(long gold, List<DroppedItem> items)` / `(String itemId, int quantity)`.
- **Action_Button**: 행동 버튼 뷰 record `(String label)`. 기존 `NpcActionButton`을 NPC·몬스터 공용으로 리네임한 것.
- **Talk_Target**: 현재 대사 대상 묶음 record `(Npc npc, Monster monster, String dialogue)` + `EMPTY`/`ofNpc`/`ofMonster`. `buildPlayScreen` 파라미터 폭증을 막는다.
- **Preemptive_Strike**: 몬스터 선공. 필드 진입 시 `PREEMPTIVE_STRIKE_PERCENT`(=5, 전 맵 고정) 확률로 노드 몬스터 중 1마리가 먼저 공격하는 이벤트.
- **Monster_Button_Label**: 상호작용 버튼 라벨. 일반은 이름만(`"너구리"`), 보스는 이름 + 배지(`"너구리왕 👑"`).
- **Combat_Log_Type**: Action_Log의 로그 타입 `"combat"`(연빨강 `.log-combat`, 이미 존재). 조우/선공 로그에 사용.

## Requirements

### Requirement 1: 몬스터 카탈로그 로드 및 검증

**User Story:** 개발자로서, 몬스터 정의를 재컴파일 없이 관리하고 무결성을 기동 시 보장받고 싶다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL 몬스터 카탈로그를 `classpath:data/monster.json`(최상위 JSON 배열)로 관리한다.
2. WHEN 애플리케이션이 기동되면, THE Monster_Catalog SHALL `monster.json`을 1회 파싱하여 불변 `Monster` 목록으로 보관한다(Catalog_Loading_Pattern).
3. THE Monster_Service SHALL 파싱 로직을 스트림 입력(`loadFromStream(InputStream)`)으로 분리하여 인메모리 데이터 주입 테스트가 가능하도록 한다.
4. THE Monster_Service SHALL `Monster_Id`로 조회하는 `byId(String):Optional<Monster>`와 전체 목록 `all()`을 제공한다.
5. IF 최상위 구조가 배열이 아니거나 파싱에 실패하면, THEN THE Monster_Service SHALL Monster_Data_Exception으로 기동을 실패시킨다.
6. THE Monster_Service SHALL Jackson 3(`tools.jackson`) ObjectMapper를 생성자 주입으로 사용한다.

### Requirement 2: 몬스터 데이터 검증 규칙

**User Story:** 개발자로서, 잘못된 몬스터 데이터가 런타임이 아니라 기동 시점에 걸러지기를 원한다.

#### Acceptance Criteria

1. IF `Monster_Id`가 중복되면, THEN THE Monster_Service SHALL Monster_Data_Exception으로 기동을 실패시킨다.
2. IF `type`이 유효한 Monster_Type이 아니면, THEN THE Monster_Service SHALL Monster_Data_Exception으로 기동을 실패시킨다.
3. IF 필수 필드(`id`/`name`/`type`/`level`/`maxHp`/`attackPower`/`defense`/`critical`/`experience`/`goldDrop`)가 누락되면, THEN THE Monster_Service SHALL Monster_Data_Exception으로 기동을 실패시킨다.
4. IF `goldDrop`이 `0 ≤ min ≤ max`를 위반하거나, `itemDrops[].chancePercent`가 1~100을 벗어나거나, `itemDrops[].minQuantity`/`maxQuantity`가 `1 ≤ min ≤ max`를 위반하면, THEN THE Monster_Service SHALL Monster_Data_Exception으로 기동을 실패시킨다.
5. IF `itemDrops[].itemId`가 Item_Catalog_Service에 존재하지 않으면, THEN THE Monster_Service SHALL Monster_Data_Exception으로 기동을 실패시킨다.
6. IF `lines`의 개수가 정확히 3이 아니면, THEN THE Monster_Service SHALL Monster_Data_Exception으로 기동을 실패시킨다.
7. WHERE `itemDrops`가 미기재이면, THE Monster_Service SHALL 이를 빈 목록으로 처리한다.

### Requirement 3: 몬스터 분류 (Monster_Type)

**User Story:** 개발자로서, 몬스터 종류(일반/보스)를 컴파일 타임 안정성과 함께 관리하고 싶다.

#### Acceptance Criteria

1. THE Monster_Type SHALL `NORMAL`(코드 `normal`, 라벨 "일반")과 `BOSS`(코드 `boss`, 라벨 "보스")를 정의한다.
2. THE Monster_Type SHALL 버튼 배지를 보유한다: `NORMAL`은 빈 문자열(""), `BOSS`는 "👑".
3. THE Monster_Type SHALL 행동 라벨 목록 `["전투"]`을 보유한다.
4. THE Monster_Type SHALL 코드 문자열로 조회하는 `fromType(String):Optional<Monster_Type>`을 제공하며, 미지 코드/`null`은 빈 Optional을 반환한다.
5. THE Myrpg_Web_Module SHALL 몬스터 목록·수치는 카탈로그(JSON)에, 타입/배지/행동 라벨은 enum(코드)에 둔다(둘의 역할을 혼용하지 않는다).

### Requirement 4: 몬스터 데이터 모델 (스탯·드랍·대사)

**User Story:** 개발자로서, 몬스터의 스탯과 드랍을 6순위 전투가 그대로 소비할 수 있는 형태로 정의하고 싶다.

#### Acceptance Criteria

1. THE Monster SHALL 불변 record이며 `id`/`name`/`type`/`level`/`maxHp`/`attackPower`/`defense`/`critical`/`experience`/`goldDrop`/`itemDrops`/`lines`를 보유한다.
2. THE Monster SHALL 플레이어 `Stats`(STR/DEX/INT/CRITICAL/DEF)를 재사용하지 않고 Monster_Stats(`attackPower`·`defense`·`critical`)만 플랫하게 보유한다(DEX/INT 없음, MP·스태미나 없음).
3. THE `critical` SHALL 플레이어와 동일한 0.1% 단위 정수 규약을 따른다(10 = 1.0%).
4. THE Gold_Drop SHALL `(int min, int max)` record로서 생성 시 `0 ≤ min ≤ max`를 검증한다.
5. THE Item_Drop SHALL `(String itemId, int chancePercent, int minQuantity, int maxQuantity)` record이다.
6. THE Monster SHALL `Monster_Button_Label`을 산출하는 `buttonLabel()`을 제공한다: 배지가 비면 이름만, 아니면 `이름 + " " + 배지`.
7. THE Monster SHALL 영속 대상이 아니며(메모리 카탈로그), 전투 중 현재 HP는 본 스펙에서 저장하지 않는다.

### Requirement 5: 맵 출현 매핑

**User Story:** 플레이어로서, 특정 필드로 이동하면 그 맵에 사는 몬스터가 버튼으로 나타나기를 원한다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL `data/map.json` 노드에 optional `monsters` 배열(`Monster_Id` 목록)을 추가하여 맵별 출현을 매핑한다(맵 데이터가 단일 진실).
2. THE Map_Node SHALL `monsters`(List<String>) 컴포넌트를 보유하되, 기존 9인자 생성자를 보조 생성자로 유지하여 기존 호출부(테스트 포함)를 변경하지 않는다(하위 호환).
3. THE Map_Service SHALL `monsters`를 `theme` 등과 동일한 방식(`nodeJson.has(...)`)으로 optional 파싱하며, 미기재 시 빈 목록으로 처리한다.
4. WHERE `dugald-north`(두갈드 아일 북부) 노드이면, THE map.json SHALL `monsters: ["raccoon"]`을 매핑한다.
5. THE Monster_Service SHALL 노드별 몬스터를 `byNode(String nodeId):List<Monster>`로 제공하며, `map.json`의 `monsters` 배열 순서를 보존한다.
6. IF `nodeId`가 미지 노드이거나 `null`이면, THEN THE `byNode` SHALL 빈 목록을 반환한다(예외를 던지지 않음, `NpcService.byNode` 관례).
7. IF `map.json`의 어떤 `monsters` 항목이 Monster_Catalog에 존재하지 않으면, THEN THE Monster_Service SHALL 기동 시 Monster_Data_Exception으로 실패시킨다.
8. IF 한 노드의 `monsters` 배열에 동일 `Monster_Id`가 중복 기재되면, THEN THE Monster_Service SHALL 기동 시 Monster_Data_Exception으로 실패시킨다.
9. THE 몬스터 배치 SHALL 정적·상주이며, 조우·처치가 맵 상태를 변경하지 않는다(소멸/리스폰/스폰 수량 없음).

### Requirement 6: 조우 대사 선택

**User Story:** 플레이어로서, 몬스터를 만나면 그 몬스터다운 소리나 행동 묘사를 보고 싶다.

#### Acceptance Criteria

1. THE Monster_Lines SHALL 몬스터당 정확히 3개로 구성되며, 1개는 울음/위협음, 2개는 행동 묘사(지시문 형태)이다.
2. WHEN 조우가 발생하면, THE Monster_Dialogue_Service SHALL `lines` 3개 중 Random_Bean으로 1개를 선택하여 반환한다.
3. THE Monster_Dialogue_Service SHALL 반환값이 항상 해당 몬스터의 `lines`에 포함되도록 한다.
4. THE Monster_Dialogue_Service SHALL 시간대(TimeOfDay) 분기를 두지 않는다(NPC와 상이).
5. WHERE 카탈로그 검증으로 `lines`가 항상 3개임이 보장되므로, THE Monster_Dialogue_Service SHALL 폴백 문구를 두지 않는다.

### Requirement 7: 가위바위보 AI

**User Story:** 개발자로서, 몬스터가 스킬 없이 일반/강/방어를 균등하게 발동하는 규칙을 6순위 전투가 재사용할 수 있게 정의하고 싶다.

#### Acceptance Criteria

1. THE Monster_Ai_Service SHALL 몬스터 행동을 Skill_Type(NORMAL/HEAVY/DEFENSE)으로 표현하며 신규 enum을 만들지 않는다.
2. THE Monster_Ai_Service SHALL 행동 확률을 상수로 34/33/33(일반/강/방어)으로 고정하며 몬스터별 override를 두지 않는다.
3. THE Monster_Ai_Service SHALL `actionFor(int roll)` 순수 함수를 제공한다: `roll < 34` → NORMAL, `34 ≤ roll < 67` → HEAVY, `roll ≥ 67` → DEFENSE(roll은 0~99).
4. THE Monster_Ai_Service SHALL `nextAction()`으로 Random_Bean에서 `actionFor(random.nextInt(100))`을 반환한다.
5. THE 몬스터 SHALL 스킬(`Skill`/`CharacterSkill`)·랭크·배율·자원 소모를 갖지 않는다.
6. THE Myrpg_Web_Module SHALL 실제 데미지 계산·턴 소비를 본 스펙에서 구현하지 않는다(6순위로 이연).

### Requirement 8: 드랍 계산

**User Story:** 개발자로서, 처치 보상(골드·아이템)을 6순위 전투가 소비할 수 있는 순수 계산으로 정의하고 싶다.

#### Acceptance Criteria

1. THE 모든 Monster SHALL 기본 골드를 드랍하며, Gold_Drop이 필수이다.
2. THE Monster_Reward_Service SHALL `goldFor(Gold_Drop, int roll)` 순수 함수로 `[min, max]` 범위의 금액을 산출한다.
3. WHEN `rollDrop(Monster)`이 수행되면, THE Monster_Reward_Service SHALL 골드(필수) + 아이템(0개 이상)을 담은 Drop_Result를 반환한다.
4. WHERE Item_Drop 항목이 있으면, THE Monster_Reward_Service SHALL `chancePercent` 판정을 통과한 경우에만 `[minQuantity, maxQuantity]` 범위 수량으로 Dropped_Item을 추가한다.
5. WHERE `너구리`이면, THE monster.json SHALL 생명력 50 포션(`hp_potion_50`)을 15% 확률·1개 고정으로 드랍하도록 정의한다.
6. THE Myrpg_Web_Module SHALL 드랍의 실제 지급(골드 가산·인벤토리 적재)과 인벤토리 획득 API를 본 스펙에서 구현하지 않는다(6순위로 이연).

### Requirement 9: 몬스터 선공 (필드 진입)

**User Story:** 플레이어로서, 몬스터가 사는 필드에 들어설 때 가끔 몬스터가 먼저 덤비는 긴장감을 느끼고 싶다.

#### Acceptance Criteria

1. THE Monster_Encounter_Service SHALL 선공 임계값을 상수 `PREEMPTIVE_STRIKE_PERCENT = 5`(전 맵 고정)로 둔다.
2. THE Monster_Encounter_Service SHALL `triggers(int roll)` 순수 함수를 제공한다: `roll < 5`이면 발동(roll은 0~99).
3. WHEN `rollPreemptiveStrike(List<Monster>)`이 수행되면, THE Monster_Encounter_Service SHALL 몬스터가 없거나 5% 미발동이면 빈 Optional을, 발동이면 목록 중 Random_Bean으로 1마리를 선택하여 `Optional<Monster>`로 반환한다.
4. WHEN 이동이 성공하면(`Movement_Result.Moved`), THE PlayScreenController SHALL 이동 후 노드의 몬스터로 선공을 판정한다.
5. THE Myrpg_Web_Module SHALL 선공 판정을 `GET /`(새로고침)·이동 거부(`Blocked`)에서는 수행하지 않는다(진입 시 1회).
6. WHEN 선공이 발동하면, THE Myrpg_Web_Module SHALL 이동 응답에 선공 신호(선택된 몬스터 이름 포함)를 실어 내리고, 클라이언트가 `alert("몬스터 선공 발동")`을 표시한다.
7. WHEN 선공이 발동하면, THE Myrpg_Web_Module SHALL Action_Log에 Combat_Log_Type으로 선공 기록을 남긴다.
8. THE Myrpg_Web_Module SHALL 선공 발동 시 실제 전투 진입을 본 스펙에서 구현하지 않는다(alert 신호까지만, `POST /battle/start` 교체는 6순위로 이연).

### Requirement 10: 상호작용 버튼 노출

**User Story:** 플레이어로서, 마을에서 NPC 버튼을 보듯 필드에서 몬스터 버튼을 보고 싶다.

#### Acceptance Criteria

1. WHEN 몬스터가 배치된 노드로 이동하거나 그 노드를 렌더링하면, THE Myrpg_Web_Module SHALL NPC 버튼과 동일한 자리(`.interactions`)에 몬스터 버튼을 노출한다.
2. THE Play_Screen_View_Helper SHALL NPC와 몬스터를 한 상호작용 목록으로 합치며(`buildInteractions(npcs, monsters)`), NPC 버튼을 먼저, 이어서 몬스터 버튼을 각각 정의 순서로 배치한다.
3. THE 몬스터 상호작용 항목 SHALL `Interaction_Item(id=Monster_Id, name=Monster_Button_Label, npc=false)`로 표현된다.
4. THE 몬스터 버튼 라벨 SHALL 일반은 이름만, 보스는 이름 뒤에 "👑"을 붙인다.
5. WHEN 몬스터 버튼이 렌더링되면, THE center.html SHALL `npc=false` 항목에 `data-monster-id`를 부여하고 클릭 시 몬스터 조우를 호출한다(`onInteractionClick`).
6. WHERE 몬스터 버튼은 클래스 미부여로 기존 `.interactions button`(붉은 계열) 스타일을 사용한다.

### Requirement 11: 몬스터 조우 화면

**User Story:** 플레이어로서, 몬스터를 클릭하면 이름·레벨·HP·대사와 전투 버튼을 보고 싶다.

#### Acceptance Criteria

1. WHEN 몬스터 버튼을 클릭하면, THE Myrpg_Web_Module SHALL `POST /monster/encounter?monsterId=`를 호출하여 센터 프래그먼트(`monster-response`)를 반환한다.
2. THE 몬스터 조우 화면 SHALL NPC 멘트가 나오던 자리(`.npc-talk`)에 몬스터 이름을 표시한다.
3. THE 몬스터 조우 화면 SHALL 이름 오른쪽에 작게 레벨·HP(`Lv.{level} · HP {maxHp}`)를 표시한다(조우 시점엔 전투 상태가 없으므로 최대 HP).
4. THE 몬스터 조우 화면 SHALL 이름·레벨·HP 아래에 선택된 대사(3개 중 랜덤 1개)를 표시한다.
5. THE 몬스터 조우 화면 SHALL NPC 행동 버튼과 같은 자리에 `전투` 버튼 1개를 표시한다.
6. THE Play_Screen_View SHALL 몬스터 조우용으로 `monsterName`/`monsterDialogue`/`monsterLevel`(Integer)/`monsterMaxHp`(Integer)/`monsterActions`를 보유하되, 몬스터가 없으면 모두 `null`이다.
7. THE Play_Screen_View SHALL 기존 인자 수의 보조 생성자를 유지하여 기존 호출부(테스트 포함)를 변경하지 않는다(하위 호환).
8. THE NPC 대사와 몬스터 대사 SHALL 동시에 활성되지 않는다(둘 중 하나만 non-null): NPC 클릭 시 몬스터 슬롯이, 몬스터 클릭 시 NPC 슬롯이 비워진다.
9. IF `monsterId`가 미지이거나 현재 노드에 배치되지 않았으면, THEN THE Myrpg_Web_Module SHALL 예외 없이 대사·행동 버튼을 비운 채 정상 렌더링한다(`talkToNpc` 관용 설계와 동일).

### Requirement 12: 전투 버튼 및 조우 로그

**User Story:** 플레이어로서, 전투 버튼을 누르면 (아직 미구현이라도) 명확한 안내를 받고, 조우가 로그에 남기를 원한다.

#### Acceptance Criteria

1. THE `전투` 버튼 SHALL 본 스펙에서 `alert("구현 예정입니다")` 플레이스홀더로 동작한다(기존 `npcAction()` 관례).
2. THE Myrpg_Web_Module SHALL 6순위에서 `전투` 버튼을 `POST /battle/start`로 교체할 것임을 코드 주석으로 명시한다.
3. WHEN 몬스터 조우가 발생하면, THE Myrpg_Web_Module SHALL Action_Log에 Combat_Log_Type으로 조우 기록(예: "너구리와(과) 마주쳤다.")을 남긴다.
4. THE Combat_Log_Type SHALL 기존 CSS `.log-combat`를 재사용한다(신규 스타일 불필요).

### Requirement 13: 행동 버튼 뷰 리네임

**User Story:** 개발자로서, 행동 버튼 뷰를 NPC·몬스터가 공용으로 쓰도록 이름을 중립화하고 싶다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL `NpcActionButton` record를 `ActionButton`으로 리네임하여 NPC 행동 버튼과 몬스터 행동 버튼에 공용으로 사용한다.
2. THE Play_Screen_View_Helper SHALL `monsterActions`를 Monster_Type의 행동 라벨(`["전투"]`)로부터 개수·순서 그대로 `ActionButton`으로 생성한다.
3. THE 리네임 SHALL 참조 지점(DTO·헬퍼·기존 테스트)을 함께 갱신하여 완료 시점에 빌드가 그린이도록 한다.
4. THE 기존 테스트 클래스명(`NpcActionButtonsPropertyTest`)은 002 스펙 추적성 때문에 유지하되 타입 참조만 갱신한다.

### Requirement 14: 예외 처리 및 데이터 무결성

**User Story:** 개발자로서, 몬스터 데이터 오류가 조기에 드러나고 기존 산출물이 깨지지 않기를 원한다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL 카탈로그 무결성 위반에 대해 Monster_Data_Exception(`RuntimeException` 상속, `(String)`·`(String, Throwable)` 생성자 2개)을 신설한다(`RuntimeException` 직접 사용 금지).
2. THE Monster_Data_Exception SHALL 카탈로그 무결성 위반 시 기동을 실패시킨다.
3. THE `data/item.json` SHALL 변경되지 않는다(`너구리`가 드랍하는 `hp_potion_50`은 이미 존재하므로 신규 아이템을 추가하지 않는다).
4. THE 상단바(`TopBarView`/`top-bar.html`)·사이드바·미니맵·인벤토리·은행 SHALL 변경되지 않는다.
5. WHEN `PlayScreenController` 생성자에 몬스터 서비스 3종(Monster_Service·Monster_Dialogue_Service·Monster_Encounter_Service)이 추가되면, THE 기존 `@WebMvcTest` 컨트롤러 테스트 SHALL 해당 협력자를 `@MockitoBean`으로 추가하고 `rollPreemptiveStrike`가 기본 빈 Optional을 반환하도록 스텁하여 회귀 없이 통과한다.

### Requirement 15: 이연 항목 명시

**User Story:** 개발자로서, 본 스펙이 어디까지이고 무엇이 이후 순위로 넘어가는지 코드/문서에 남기고 싶다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL 전투 턴 처리·데미지·선후공·사망, 선공 실전투 진입, 드랍 지급·인벤토리 획득 API, `CharacterProgress` HP 감소 메서드, `SkillService.onSkillKill` 호출, 임시 골드 버튼 제거, 크리티컬 배율 확정, 장착 장비 내구도 턴당 감소를 6순위로 이연한다.
2. THE Myrpg_Web_Module SHALL 보스 몬스터 실데이터·보스 인챈트 아이템 드랍을 인챈트 시스템 확정 후로 이연한다.
3. THE Myrpg_Web_Module SHALL 던전 내부 몬스터 출현을 10순위로 이연한다.
4. THE Myrpg_Web_Module SHALL 보스 필드 랜덤 등장(랜덤 시간·필드)을 추후 기능으로 이연하며, 이는 스폰 배치 문제라 `Monster`의 sealed 승격을 요구하지 않는다.
5. THE 각 이연 seam(선공 신호·`전투` 버튼·`rollDrop`) SHALL 담당 순위·교체 조건을 서술형 주석으로 명시한다(나열식 `// TODO` 금지).
