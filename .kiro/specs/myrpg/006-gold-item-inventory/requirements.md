# Requirements Document

## Introduction

본 스펙(006)은 `myrpg` 모듈(`com.myapps.web.myrpg`)에 **골드·아이템·인벤토리·장비 시스템**을 통합 추가한다. 스펙 001~005가 구축한 캐릭터 진행(`CharacterProgress`), 재능/스탯 계산(`StatProgression`·`Stats`·`VitalMax`), 스킬 랭크업 보너스(`SkillService.rankupBonus`), NPC(`NpcService`·`NpcType`), 정보 팝업 스탯 조립(`PlayScreenViewHelper.buildStatLines`) 위에서 동작한다. 상세 설계 배경과 확정 사항은 `docs/gold-item-system.md`를 근거로 한다.

핵심 방향은 004·005의 "계산형/저장형/카탈로그형 구분" 원칙의 확장이다.

- **소지금(골드)**·**보유 아이템**·**장비 착용 상태**·**내구도**는 계산으로 복원할 수 없으므로 **영속 저장**(`CharacterProgress.gold` 컬럼 + 신규 `Bank`·`OwnedItem` 엔티티)한다.
- **아이템 정의**(이름·타입·보너스·회복량·구매가·최대 내구도)는 계속 늘어나는 콘텐츠 데이터이므로 **`data/item.json` 카탈로그**로 분리한다(`skill.json`/`SkillCatalogService` 선례).
- **타입·장비 슬롯·착용 규칙**은 로직 결합이라 **코드(enum/순수 정책)** 로 둔다.

이번 스펙의 범위:

1. **골드** — `CharacterProgress`에 소지금 컬럼 추가, 획득/소모 도메인 메서드, 부족 시 정식 예외. 실제 획득/소모 경로(몬스터·판매·상점)가 없어 **임시 골드 버튼**(+100/-100)으로 검증.
2. **은행 통합 금고** — 신규 `Bank` 엔티티(모든 지점 공유, 행 1개). 소지금 ↔ 은행 골드 입금/출금(트랜잭션). NPC 은행 행동을 단일 "은행"으로 통합.
3. **아이템 카탈로그** — `item.json`을 기동 시 로드·검증(`ItemCatalogService`, `SkillCatalogService` 선례). 타입/장비종류/보너스대상 문자열을 enum으로 검증하고 무결성 위반 시 기동 실패.
4. **아이템 분류** — 타입(`ItemType`: POTION/WEAPON/ARMOR), 장비 슬롯(`EquipSlot`: MAIN_HAND/OFF_HAND/BODY), 장비 종류(`EquipmentKind`: 한손검/양손검/방패/갑옷)를 enum으로 정의.
5. **보유 아이템 영속** — 신규 `OwnedItem` 엔티티(itemId·quantity·storage·equipped·currentDurability). 인벤토리·은행을 `storage`로 구분. 소비형은 스택 누적, 장비는 개별 인스턴스.
6. **장비 착용 규칙** — 슬롯 점유 기반 충돌 검사로 착용/해제. 한손검+방패 병용, 양손검↔방패 배타, 같은 슬롯 무기 스왑.
7. **장비 보너스 합산** — 장착 장비의 `(BonusTarget, 수치)` 목록을 STAT/VITAL로 분기 합산하여 정보 팝업 스탯(005의 스킬 보너스 자리에 더해) 및 최대 바이탈에 반영.
8. **포션 사용** — HP 회복(소비형), 쿨다운·횟수 제한 없음. 전투 UI 연동은 6순위로 이연.
9. **아이템 상세보기** — 목록의 🔍 버튼 → 자동 생성 설명 모달(인벤토리·은행 공용, 데이터는 렌더 시 임베드).
10. **가격 모델(정의만)** — 구매가(`buyPrice`) optional 필드 + 판매가 계산 모델(인스턴스 가치 = 보너스 동일 가중치). 실제 가중치 값·상점 구매/판매는 7순위로 이연.
11. **내구도(필드·표시만)** — 카탈로그 `maxDurability` + 인스턴스 `currentDurability`. 지급 시 최대치 초기화·목록/상세 표시. 턴당 감소는 6순위, 수리는 7순위, 0 도달 시 동작은 추후 설계로 이연.
12. **인벤토리 팝업** — 리스트형(정렬 전환·하단 보유 골드), 포션 사용·장비 착용/해제.
13. **은행 팝업** — 리스트형(좌 은행/우 소지품·골드 2칸·입출금), 아이템 맡기기/찾기.
14. **캐릭터 생성 기본 지급** — 신규 캐릭터에 초보자 장비 4종 + 포션 지급, 한손검·방패·갑옷 기본 장착.

전투 적용(내구도 턴당 감소·장비 스탯의 데미지 반영), 상점 구매/판매·수리·판매가 실제 값, 내구도 0 파손 처리, 인챈트(`ENCHANT` 타입)는 훅/데이터/모델 정의로만 두고 실제 적용은 이후 스펙(5·6·7순위 및 추후)으로 이연한다.

## Glossary

### 기존(005 이하) 재사용 용어

- **Myrpg_Web_Module**: `com.myapps.web.myrpg` 기본 패키지의 Spring Boot 4.0 Web 모듈.
- **Character_Progress**: 유일한 캐릭터 진행 엔티티. 본 스펙에서 소지금 컬럼(`gold`)이 추가되고 보유 아이템(`OwnedItem`)과 `id`로 연관된다.
- **Stats**: STR/DEX/INT/Critical(0.1%단위)/DEF를 담는 표시 VO. 불변 델타 헬퍼(`withStrDelta` 등)와 `Stats.ZERO` 보유.
- **VitalMax**: HP/MP/Stamina 최대치를 담는 표시 VO. 불변 델타 헬퍼 보유.
- **BonusTarget / BonusKind**: 보너스 대상(STR/DEX/INT/CRITICAL/DEF/HP/MP/STAMINA)과 분류(STAT/VITAL). 재능/스킬 보너스가 사용하는 기존 어휘.
- **Stat_Progression**: 레벨·재능으로 스탯/바이탈 최대치를 계산하는 순수 정책(`levelStatsFor`·`vitalMaxFor`).
- **Skill_Rankup_Bonus**: 005가 정보 팝업 중앙 스탯의 보너스 자리에 채운 스킬 랭크업 누적 스탯 보너스(`SkillService.rankupBonus`).
- **Info_Popup / Play_Screen_View_Helper**: 정보 팝업과 그 조립기. 중앙 스탯을 `본체(+보너스)`로 렌더링한다(`buildStatLines`).
- **Character_Service**: 캐릭터 진행 로드/생성 서비스. `createAndSaveDefault`에서 신규 캐릭터 저장 후 `skillService.seedDefault(id)`로 기본 스킬을 시드한다.
- **Catalog_Loading_Pattern**: `SkillCatalogService`/`NpcService`가 `classpath:data/*.json`을 기동 시 1회 파싱·검증하고 무결성 위반 시 전용 `*DataException`으로 기동을 실패시키는 로딩 패턴. 파싱을 `loadFromStream`으로 분리해 인메모리 주입 테스트가 가능하다. Jackson 3(`tools.jackson`) 사용.
- **Npc / Npc_Type**: NPC와 그 타입 enum. `BANK`(은행) 타입이 행동 라벨 목록(`actionLabels`)을 보유한다.
- **Npc_Action_Button**: NPC 상호작용 시 렌더링되는 행동 버튼(현재 `center.html`에서 `npcAction()` 호출).
- **Overlay_Popup_Pattern**: `.overlay` + `.open` 토글 팝업. 동적 팝업은 `GET` fetch → 응답 HTML 주입. 모달 위 모달(`rankup-overlay`) 패턴이 존재한다.
- **Action_Log**: 화면 하단 활동 로그. 골드/아이템 동작 결과를 기록한다.

### 본 스펙(006) 신규 용어

- **Gold**: 캐릭터 소지금. `Character_Progress.gold`(`long`)에 저장된다.
- **Bank**: 모든 은행 지점이 공유하는 통합 금고 엔티티. 보관 골드를 저장하며 계정=단일 캐릭터이므로 **행 1개**만 존재한다.
- **Bank_Gold**: `Bank`에 보관된 골드.
- **Insufficient_Gold_Exception**: 소지금/은행 잔액 부족으로 소모·출금·입금이 거부될 때 던지는 정식 비즈니스 예외.
- **Test_Gold_Amount**: 임시 골드 버튼이 사용하는 고정 금액(100).
- **Item_Catalog**: `classpath:data/item.json`에 정의된 아이템 목록. 기동 시 로드되어 불변 `Item` 목록으로 보관된다.
- **Item**: 카탈로그 항목(sealed 도메인 계열). `id`, `name`, `type`, `buyPrice`(optional)를 공통 보유. enum이 아니다.
- **Item_Id**: 아이템 정체성 키(예: `"hp_potion_50"`). `OwnedItem`이 문자열로 참조하는 단일 소스.
- **Item_Type**: 아이템 타입 enum. `POTION`(포션) / `WEAPON`(무기) / `ARMOR`(방어구). `isEquipment()`는 WEAPON 또는 ARMOR.
- **Potion_Item**: 포션 정의(record). `healHp`(HP 회복량) 보유.
- **Equipment_Item**: 장비(무기/방어구) 정의(record). `kind`(Equipment_Kind), `bonuses`(Equip_Bonus 목록), `maxDurability` 보유.
- **Equip_Bonus**: 장비 보너스 한 줄(record). `(BonusTarget target, int amount)`. 기존 BonusTarget/BonusKind 어휘 재사용.
- **Equip_Slot**: 장비 점유 슬롯 enum. `MAIN_HAND`(주무기) / `OFF_HAND`(보조손) / `BODY`(몸통).
- **Equipment_Kind**: 장비 종류 enum. `ONE_HANDED_SWORD`(한손검) / `TWO_HANDED_SWORD`(양손검) / `SHIELD`(방패) / `ARMOR_BODY`(갑옷). 각자 `primarySlot`(주 슬롯)과 `requiredSlots`(점유 슬롯 집합)를 보유한다.
- **Owned_Item**: 캐릭터의 보유 아이템 인스턴스를 저장하는 신규 엔티티. `itemId`, `quantity`, `storage`, `equipped`, `currentDurability`.
- **Storage_Kind**: 보유 아이템의 저장 위치 enum. `INVENTORY`(소지품) / `BANK`(은행 보관).
- **Inventory_Capacity / Bank_Capacity**: 각 저장소의 최대 항목 수(각 30). 장착품도 INVENTORY 항목으로 포함된다.
- **Stackable**: 같은 `Item_Id`를 한 행으로 누적하는 성질. 포션 등 소비형(POTION)은 스택하고, 장비(WEAPON/ARMOR)는 스택하지 않는다(개별 인스턴스).
- **Equipped**: `OwnedItem`의 장착 여부. `storage=INVENTORY`인 장비만 `true`가 될 수 있다.
- **Equip_Conflict**: 착용하려는 장비의 필요 슬롯을 primary 슬롯이 다른 장비가 점유하여 착용이 불가한 상태.
- **Equip_Conflict_Exception**: Equip_Conflict로 착용이 거부될 때 던지는 예외("착용 할 수 없습니다").
- **Inventory_Full_Exception**: 저장소 용량(30) 초과로 아이템 이동/획득이 거부될 때 던지는 예외.
- **Equipped_Bonus**: 장착 중인 장비들의 Equip_Bonus 합산 결과. STAT 계열은 `Stats`로, VITAL 계열은 `VitalMax`로 산출된다.
- **Item_Data_Exception**: 카탈로그 로드/검증 실패 시 던지는 예외(`SkillDataException` 선례).
- **Buy_Price**: 상점 구매가(카탈로그, optional). 없으면(null) 상점 미판매(드랍 전용).
- **Sell_Value**: 판매가. 저장하지 않고 인스턴스 가치에서 계산한다. `baseValue(옵션) + 총보너스포인트 × WEIGHT`(모든 BonusTarget 동일 가중치). 실제 `WEIGHT`·판매 비율은 7순위로 이연.
- **Max_Durability / Current_Durability**: 장비의 최대 내구도(카탈로그) / 현재 내구도(인스턴스, 소수). 모든 초보자 장비는 최대 20.
- **Durability_Decrement**: 공격 턴당 장착 장비 감소량(0.2). 실제 감소 로직은 6순위(전투)로 이연.
- **Item_Detail**: 아이템 상세 설명. 효과 데이터에서 **자동 생성**되며, 목록 렌더 시 임베드되어 🔍 클릭 시 모달로 표시된다.
- **Owned_Item_View**: 목록 행 뷰(record). 이름·타입 라벨·수량·장착 여부·사용/착용 가능·내구도·상세 텍스트를 담는다.
- **Inventory_View / Bank_View**: 인벤토리 팝업 뷰(보유 골드 + 목록) / 은행 팝업 뷰(은행·보유 골드 + 은행/소지품 두 목록).
- **Inventory_Sort**: 인벤토리 목록 정렬 기준. 기본 `획득순`(id 오름차순), 화면에서 `이름순`/`타입순` 전환(클라이언트 정렬).
- **Default_Seed_Items**: 신규 캐릭터가 보유한 채 시작하는 아이템. 초보자용 한손검/양손검/방패/갑옷 각 1 + 생명력 50 포션 ×5. 한손검·방패·갑옷은 기본 장착.

## Requirements

### Requirement 1: 골드 소지금

**User Story:** 플레이어로서, 골드를 보유하고 획득·소모할 수 있기를 원한다.

#### Acceptance Criteria

1. THE Character_Progress SHALL 소지금 Gold를 `gold`(`long`) 컬럼으로 영속 저장한다.
2. WHEN 신규 캐릭터가 생성되면, THE Character_Progress SHALL `gold`를 0으로 초기화한다.
3. THE Character_Progress SHALL 소지금을 증가시키는 `gainGold(long)`과 차감하는 `spendGold(long)`을 제공한다.
4. IF `spendGold`의 요청 금액이 보유 Gold를 초과하면, THEN THE Myrpg_Web_Module SHALL Insufficient_Gold_Exception으로 거부하고 소지금을 변경하지 않는다.
5. THE `gainGold`/`spendGold` SHALL 음수 잔액을 만들지 않는다.
6. WHEN 사망/환생이 수행되면, THE Myrpg_Web_Module SHALL 소지금과 Bank_Gold를 변경하지 않는다(손실 없음).

### Requirement 2: 은행 통합 금고

**User Story:** 플레이어로서, 어느 은행에서든 동일한 잔고에 골드를 맡기고 찾고 싶다.

#### Acceptance Criteria

1. THE Bank SHALL Bank_Gold를 저장하는 신규 엔티티이며, 계정=단일 캐릭터이므로 **행 1개**만 유지한다(통합 금고).
2. THE Myrpg_Web_Module SHALL Bank를 로드하거나 없으면 기본 행(`gold=0`)을 생성하는 진입점(`loadOrCreateDefault`)을 제공한다(Character_Service 선례).
3. WHEN 어느 은행 노드에서 은행을 열어도, THE Myrpg_Web_Module SHALL 동일한 단일 Bank 잔고를 조회·갱신한다.

### Requirement 3: 골드 입금/출금

**User Story:** 플레이어로서, 소지금을 은행에 맡기고 필요할 때 찾고 싶다.

#### Acceptance Criteria

1. WHEN 입금이 수행되면, THE Myrpg_Web_Module SHALL `Character_Progress.spendGold(amount)` 후 `Bank.deposit(amount)`를 하나의 트랜잭션으로 처리한다.
2. WHEN 출금이 수행되면, THE Myrpg_Web_Module SHALL `Bank.withdraw(amount)` 후 `Character_Progress.gainGold(amount)`를 하나의 트랜잭션으로 처리한다.
3. IF 입금 금액이 보유 Gold를 초과하거나 출금 금액이 Bank_Gold를 초과하면, THEN THE Myrpg_Web_Module SHALL Insufficient_Gold_Exception으로 거부하고 소지금·Bank_Gold를 변경하지 않는다.
4. THE 입금/출금 SHALL 최소 1골드, 상한 없음, 수수료 없음의 정책을 따른다.
5. IF 입금/출금 금액이 1 미만이거나 숫자가 아니면, THEN THE Myrpg_Web_Module SHALL 이를 거부한다.

### Requirement 4: 임시 골드 버튼 (검증용)

**User Story:** 개발자로서, 실제 획득/소모 경로가 없어도 골드 흐름을 검증하고 싶다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL 좌측 사이드바 "경험치 다운" 버튼 아래에 `골드 획득`·`골드 소모` 임시 버튼을 제공한다.
2. WHEN `골드 획득`을 누르면, THE Myrpg_Web_Module SHALL Gold를 Test_Gold_Amount(100)만큼 증가시키고 Action_Log에 기록한다.
3. WHEN `골드 소모`를 누르면, THE Myrpg_Web_Module SHALL Gold가 100 이상이면 100 차감·기록하고, 미만이면 차감하지 않고 부족 안내를 표시한다.
4. THE 임시 골드 버튼 SHALL 실제 획득/소모 경로(몬스터 5·6순위·아이템 판매/상점 7순위) 구현 시 제거될 임시 장치임을 코드·태스크에 명시한다.

### Requirement 5: 아이템 카탈로그 로드 및 검증

**User Story:** 개발자로서, 아이템 정의를 재컴파일 없이 관리하고 싶다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL 아이템 카탈로그를 `classpath:data/item.json`(최상위 JSON 배열)로 관리한다.
2. WHEN 애플리케이션이 기동되면, THE Item_Catalog SHALL `item.json`을 1회 파싱하여 불변 `Item` 목록으로 보관한다(Catalog_Loading_Pattern).
3. THE Item_Catalog SHALL 파싱 로직을 스트림 입력(`loadFromStream`)으로 분리하여 인메모리 데이터 주입 테스트가 가능하도록 한다.
4. WHEN `item.json`의 `type`이 유효한 Item_Type이 아니거나, 장비의 `kind`가 유효한 Equipment_Kind가 아니거나, `bonuses[].target`이 유효한 BonusTarget이 아니면, THE Item_Catalog SHALL Item_Data_Exception으로 기동을 실패시킨다.
5. WHEN 필수 필드(`id`/`name`/`type`)가 누락되었거나, `Item_Id`가 중복되면, THE Item_Catalog SHALL Item_Data_Exception으로 기동을 실패시킨다.
6. WHEN `type`이 potion이면, THE Item_Catalog SHALL `PotionItem`(+`healHp`)으로 파싱한다.
7. WHEN `type`이 weapon/armor이면, THE Item_Catalog SHALL `EquipmentItem`(+`kind`+`bonuses`+`maxDurability`)으로 파싱하며, `maxDurability`는 필수이다.
8. THE Item_Catalog SHALL `buyPrice`를 optional로 파싱한다(없으면 null). `bonuses`가 없으면 빈 목록으로 처리한다.
9. THE Item_Catalog SHALL `Item_Id`로 아이템을 조회하는 기능(`byId`)과 전체 목록 조회 기능을 제공한다.

### Requirement 6: 아이템 분류 (타입/슬롯/장비종류)

**User Story:** 개발자로서, 아이템 타입·장비 슬롯·장비 종류를 컴파일 타임 안정성과 함께 관리하고 싶다.

#### Acceptance Criteria

1. THE Item_Type SHALL `POTION`, `WEAPON`, `ARMOR`를 정의하고 `fromString(code)`과 `isEquipment()`(WEAPON 또는 ARMOR)를 제공한다.
2. THE Equip_Slot SHALL `MAIN_HAND`, `OFF_HAND`, `BODY`를 정의한다.
3. THE Equipment_Kind SHALL `ONE_HANDED_SWORD`(primary MAIN_HAND, required {MAIN_HAND}), `TWO_HANDED_SWORD`(primary MAIN_HAND, required {MAIN_HAND, OFF_HAND}), `SHIELD`(primary OFF_HAND, required {OFF_HAND}), `ARMOR_BODY`(primary BODY, required {BODY})를 정의하고 `fromString(code)`을 제공한다.
4. THE Myrpg_Web_Module SHALL 아이템 목록·수치는 카탈로그(JSON)에, 타입/슬롯/종류는 enum(코드)에 둔다(둘의 역할을 혼용하지 않는다).

### Requirement 7: 보유 아이템 영속 모델

**User Story:** 개발자로서, 보유 아이템과 은행 보관을 최소 확장으로 저장하고 싶다.

#### Acceptance Criteria

1. THE Owned_Item SHALL `itemId`(문자열, `item.json` id 참조), `quantity`, `storage`(Storage_Kind, EnumType.STRING), `equipped`, `currentDurability`를 영속 저장한다.
2. THE Storage_Kind SHALL `INVENTORY`, `BANK`를 정의하며, 인벤토리 아이템과 은행 보관 아이템을 한 테이블에서 `storage`로 구분한다.
3. WHERE 아이템이 소비형(POTION)이면, THE Myrpg_Web_Module SHALL 같은 `itemId`+`storage`를 한 행으로 누적(`quantity` 증가)한다(Stackable).
4. WHERE 아이템이 장비(WEAPON/ARMOR)이면, THE Myrpg_Web_Module SHALL 스택하지 않고 개별 행으로 저장한다.
5. THE `equipped` SHALL `storage=INVENTORY`인 장비에서만 `true`가 될 수 있으며, 포션·은행 항목은 항상 `false`이다.
6. WHEN 저장→로드 라운드트립이 수행되면, THE Myrpg_Web_Module SHALL `itemId`·`quantity`·`storage`·`equipped`·`currentDurability`가 모두 보존되도록 한다.
7. THE Myrpg_Web_Module SHALL 계정=단일 캐릭터이므로 `OwnedItem`에 소유자 식별자를 두지 않는다(향후 다중 캐릭터 확장 시 추가).

### Requirement 8: 저장소 용량

**User Story:** 플레이어로서, 인벤토리와 은행이 각각 제한된 칸을 갖기를 원한다.

#### Acceptance Criteria

1. THE Inventory_Capacity SHALL 최대 30항목, THE Bank_Capacity SHALL 최대 30항목이다(스택 종류 수 기준).
2. THE Inventory_Capacity SHALL 장착 중인 장비 항목을 포함하여 계산한다.
3. IF 아이템 이동/획득으로 대상 저장소 항목 수가 30을 초과하면(신규 스택 추가 시), THEN THE Myrpg_Web_Module SHALL Inventory_Full_Exception으로 거부한다.
4. WHERE 이동이 기존 스택에 누적되는 경우(소비형), THE Myrpg_Web_Module SHALL 항목 수를 늘리지 않으므로 용량 검사를 통과시킨다.

### Requirement 9: 장비 착용 규칙

**User Story:** 플레이어로서, 한손검+방패는 함께 끼되 양손검은 방패와 함께 낄 수 없기를 원한다.

#### Acceptance Criteria

1. WHEN 장비 착용이 요청되면, THE Myrpg_Web_Module SHALL 착용 대상의 `requiredSlots` 각 슬롯을 점유한 장착 장비를 조사한다.
2. IF 필요 슬롯을 점유한 장착 장비의 `primarySlot`이 착용 대상의 `primarySlot`과 다르면, THEN THE Myrpg_Web_Module SHALL Equip_Conflict_Exception으로 착용을 거부한다("착용 할 수 없습니다").
3. WHERE 필요 슬롯을 점유한 장비의 `primarySlot`이 대상과 같으면(같은 역할), THE Myrpg_Web_Module SHALL 그 장비를 해제한 뒤 대상을 착용한다(스왑).
4. WHERE 한손검과 방패는 슬롯이 겹치지 않으므로, THE Myrpg_Web_Module SHALL 둘의 병용 착용을 허용한다.
5. WHERE 양손검 착용 중 방패 착용을 시도하면, THE Myrpg_Web_Module SHALL 이를 거부한다(양손검이 OFF_HAND를 점유, primary 상이).
6. WHERE 방패 착용 중 양손검 착용을 시도하면, THE Myrpg_Web_Module SHALL 이를 거부한다(방패가 OFF_HAND를 점유, primary 상이).
7. WHERE 갑옷(BODY)은 무기/방패와 슬롯이 겹치지 않으므로, THE Myrpg_Web_Module SHALL 갑옷을 독립적으로 착용·교체(갑옷끼리 스왑)한다.
8. THE 착용/해제 SHALL `storage=INVENTORY`인 장비에만 적용되며, 수행 후 상태를 저장하고 Action_Log에 기록한다.
9. WHEN 장비를 해제하면, THE Myrpg_Web_Module SHALL 해당 Owned_Item의 `equipped`를 false로 만든다.

### Requirement 10: 장비 보너스 합산

**User Story:** 플레이어로서, 장비를 착용하면 그 보너스가 내 스탯에 반영되기를 원한다.

#### Acceptance Criteria

1. THE Equipment_Item SHALL 보너스를 `Equip_Bonus`(`(BonusTarget, 수치)`) 목록으로 보유한다(단일 `Stats`가 아니다).
2. WHEN Equipped_Bonus를 계산하면, THE Myrpg_Web_Module SHALL `storage=INVENTORY && equipped` 장비들의 Equip_Bonus를 `BonusTarget.kind()`로 분기하여 STAT 계열은 `Stats`로, VITAL 계열은 `VitalMax`로 합산한다.
2. THE Myrpg_Web_Module SHALL BonusTarget/BonusKind의 기존 STAT/VITAL 처리 방식(Stat_Progression·Skill_Rankup_Bonus 선례)과 동일하게 분기한다.
3. WHEN 정보 팝업을 조립하면, THE Play_Screen_View_Helper SHALL 기존 Skill_Rankup_Bonus(STAT)에 장비 STAT 보너스를 합산하여 스탯 보너스 표기에 반영한다.
4. WHEN 최대 바이탈을 계산하면, THE Myrpg_Web_Module SHALL 장비 VITAL 보너스를 `vitalMaxFor` 결과에 합산하여 게이지 최대값에 반영한다.
5. WHERE 초보자 장비가 기본 장착(한손검 STR+5, 방패 DEF+5, 갑옷 DEF+10)되면, THE Info_Popup SHALL 생성 직후 STR +5, DEF +15의 장비 보너스를 반영한다.

### Requirement 11: 포션 사용

**User Story:** 플레이어로서, 인벤토리에서 포션을 언제든 사용해 HP를 회복하고 싶다.

#### Acceptance Criteria

1. WHEN 포션 사용이 요청되면, THE Myrpg_Web_Module SHALL 해당 Owned_Item을 카탈로그 Potion_Item으로 조회한다.
2. WHEN 포션을 사용하면, THE Myrpg_Web_Module SHALL `hpCurrent = min(hpCurrent + healHp, hpMax)`로 HP를 회복한다(hpMax는 `vitalMaxFor(level, talent)` 재사용).
3. WHEN 포션을 사용하면, THE Myrpg_Web_Module SHALL `quantity`를 1 감소시키고 0이 되면 해당 행을 삭제한다.
4. THE 포션 사용 SHALL 쿨다운·횟수 제한 없이 보유 수량 내에서 자유롭게 수행되며, 수행 후 저장하고 Action_Log에 기록한다.
5. THE Myrpg_Web_Module SHALL 전투 중 포션 사용의 전투 UI 연동을 본 스펙에서 구현하지 않는다(인벤토리에서의 사용까지만, 전투 UI 연동은 6순위로 이연).

### Requirement 12: 아이템 상세보기

**User Story:** 플레이어로서, 아이템의 효과(포션 회복량, 장비 보너스 등)를 상세히 확인하고 싶다.

#### Acceptance Criteria

1. THE 인벤토리·은행 목록 SHALL 각 행 아이템명 오른쪽에 상세보기(🔍) 버튼을 제공한다.
2. WHEN 🔍를 누르면, THE Myrpg_Web_Module SHALL 목록 팝업 위에 작은 상세 모달(Overlay_Popup_Pattern의 모달 위 모달)을 표시한다.
3. THE Item_Detail SHALL 효과 데이터에서 **자동 생성**된다(별도 설명 문구를 저장하지 않는다).
4. WHERE 아이템이 포션이면, THE Item_Detail SHALL "생명력을 {healHp} 회복한다." 형식의 설명을 포함한다.
5. WHERE 아이템이 장비이면, THE Item_Detail SHALL 각 Equip_Bonus를 한 줄씩(대상 라벨 + 부호 수치; Critical은 0.1% 단위 포맷), 장비 종류/슬롯 정보, 배타 안내(양손검은 방패와 함께 착용 불가), 내구도(`현재/최대`)를 포함한다.
6. THE Item_Detail SHALL 목록 렌더 시 Owned_Item_View에 실려 임베드되며, 🔍 클릭 시 별도 서버 요청 없이 표시된다.
7. THE 상세 모달 SHALL 인벤토리·은행이 공용 컴포넌트를 사용한다.

### Requirement 13: 인벤토리 팝업

**User Story:** 플레이어로서, 보유 아이템과 보유 골드를 한 화면에서 관리하고 싶다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL 좌측 사이드바 "인벤토리" 버튼을 눌러 인벤토리 팝업을 열도록 한다.
2. THE Inventory_View SHALL 보유 Gold와 Owned_Item_View 목록을 담으며, 팝업은 목록(리스트형) + 하단 보유 골드로 구성된다.
3. THE 인벤토리 목록 각 행 SHALL 이름(+장착 중이면 `[장착중]` 배지) · 타입 라벨 · 수량 · 상세보기(🔍) · 동작 버튼으로 구성된다.
4. WHERE 아이템이 포션이면, THE 동작 버튼 SHALL `사용`을, 장비이면 미장착 시 `착용`·장착 중 시 `해제`를 표시한다.
5. THE 인벤토리 팝업 SHALL `맡기기`/`찾기` 버튼을 표시하지 않는다(은행 팝업 전용).
6. WHERE 보유 아이템이 없으면, THE 인벤토리 팝업 SHALL "보유 아이템이 없습니다" 안내를 표시한다.
7. WHEN 사용/착용/해제가 수행되면, THE Myrpg_Web_Module SHALL 갱신된 인벤토리 fragment(목록·상세·골드 포함)를 반환하여 화면을 재렌더한다.

### Requirement 14: 인벤토리 정렬

**User Story:** 플레이어로서, 아이템 목록을 획득순/이름순/타입순으로 바꿔 보고 싶다.

#### Acceptance Criteria

1. THE 인벤토리 목록 SHALL 기본 정렬을 항상 획득순(`Owned_Item.id` 오름차순)으로 한다.
2. THE 인벤토리 팝업 SHALL 화면에서 획득순/이름순/타입순으로 정렬을 전환하는 컨트롤을 제공한다.
3. WHEN 이름순/타입순으로 전환하면, THE Myrpg_Web_Module SHALL 이미 임베드된 목록을 **클라이언트에서 재정렬**한다(서버 왕복 없음).
4. WHERE 타입순이면, THE Myrpg_Web_Module SHALL 타입 그룹 내에서 이름순 보조 정렬을 적용한다.

### Requirement 15: 은행 팝업 및 NPC 통합

**User Story:** 플레이어로서, 은행 NPC에서 골드와 아이템을 한 팝업에서 맡기고 찾고 싶다.

#### Acceptance Criteria

1. THE Npc_Type.BANK SHALL 행동 라벨을 단일 `["은행"]`으로 통합한다(기존 `["아이템 보관","골드 입/출금"]` 대체).
2. WHEN 은행 NPC의 "은행" 버튼을 누르면, THE Myrpg_Web_Module SHALL 은행 팝업(`GET /bank`)을 연다.
3. THE Bank_View SHALL 은행 보관 골드·보유 골드와 은행(`storage=BANK`)·소지품(`storage=INVENTORY`) 두 Owned_Item_View 목록을 담으며, 팝업은 좌(은행)/우(소지품) 목록 + 골드 2칸 + 입금/출금으로 구성된다.
4. THE 은행 팝업의 양쪽 목록 각 행 SHALL 상세보기(🔍)를 제공한다(인벤토리와 공용 상세 모달).
5. WHEN 아이템 맡기기가 수행되면, THE Myrpg_Web_Module SHALL 해당 Owned_Item의 `storage`를 INVENTORY→BANK로 변경한다(소비형은 은행 스택 누적).
6. IF 맡기려는 장비가 장착 중이면, THEN THE Myrpg_Web_Module SHALL 맡기기를 거부하고 안내를 표시한다(해제 후 가능).
7. WHEN 아이템 찾기가 수행되면, THE Myrpg_Web_Module SHALL 해당 Owned_Item의 `storage`를 BANK→INVENTORY로 변경한다.
8. IF 맡기기/찾기로 대상 저장소가 30을 초과하면, THEN THE Myrpg_Web_Module SHALL Inventory_Full_Exception으로 거부하고 안내를 표시한다.
9. WHEN 골드/아이템 이동이 수행되면, THE Myrpg_Web_Module SHALL 갱신된 은행 fragment를 반환하여 화면을 재렌더한다.

### Requirement 16: 아이템 가격 모델

**User Story:** 개발자로서, 상점·인챈트가 나중에 붙어도 가격이 확장 가능하도록 모델을 미리 정의하고 싶다.

#### Acceptance Criteria

1. THE Item SHALL Buy_Price를 optional 필드로 보유하며, 없으면(null) 상점 미판매(드랍 전용)로 취급한다.
2. THE Myrpg_Web_Module SHALL Sell_Value를 저장하지 않고 인스턴스 가치에서 계산하는 모델로 정의한다: `baseValue(옵션) + 총보너스포인트 × WEIGHT`(모든 BonusTarget 동일 가중치).
3. WHERE 인챈트(향후 `ENCHANT`)로 인스턴스 보너스가 추가되면, THE Sell_Value SHALL 동일 가중치로 자동 합산되도록 설계된다.
4. THE Myrpg_Web_Module SHALL 실제 `WEIGHT` 값·판매 비율·포션 `baseValue`·상점 구매/판매 처리를 본 스펙에서 구현하지 않는다(모델과 `buyPrice` optional 필드 파싱까지만, 실제 값·상점은 7순위~로 이연).

### Requirement 17: 장비 내구도

**User Story:** 플레이어로서, 장비 내구도를 확인할 수 있고 전투로 닳으면 대장간에서 수리하고 싶다.

#### Acceptance Criteria

1. THE Equipment_Item SHALL Max_Durability를 카탈로그 필드로 보유하며, 모든 초보자 장비는 20이다. 포션은 내구도를 갖지 않는다.
2. THE Owned_Item SHALL Current_Durability를 인스턴스 필드(소수)로 보유한다.
3. WHEN 장비가 지급/생성되면, THE Myrpg_Web_Module SHALL Current_Durability를 Max_Durability로 초기화한다.
4. THE Myrpg_Web_Module SHALL Durability_Decrement(공격 턴당 0.2, 20÷0.2=100턴에 0)와 `reduceDurability`/`repairToMax` 도메인 메서드를 정의하되, **턴당 감소의 실제 호출은 본 스펙에서 구현하지 않는다**(6순위 전투로 이연).
5. THE Myrpg_Web_Module SHALL 내구도 수리(대장간, 골드 소모)를 본 스펙에서 구현하지 않는다(7순위로 이연).
6. THE Myrpg_Web_Module SHALL 내구도 0 도달 시 동작(파손 처리)을 본 스펙에서 정의하지 않는다(추후 설계로 이연). 본 스펙에서는 필드·표시·초기화까지만 다룬다.
7. THE 인벤토리·은행 상세 모달 SHALL 장비의 내구도를 `현재/최대` 형식으로 표시한다.

### Requirement 18: 캐릭터 생성 시 기본 지급

**User Story:** 플레이어로서, 새 캐릭터가 기본 장비를 착용한 채 시작하기를 원한다.

#### Acceptance Criteria

1. WHEN 신규 캐릭터가 생성되면, THE Character_Service SHALL `skillService.seedDefault(id)` 지점 옆에서 Default_Seed_Items를 지급한다.
2. THE Default_Seed_Items SHALL 초보자용 한손검 ×1, 초보자용 방패 ×1, 초보자용 갑옷 ×1, 초보자용 양손검 ×1, 생명력 50 포션 ×5(모두 INVENTORY)를 포함한다.
3. WHEN 기본 지급이 수행되면, THE Myrpg_Web_Module SHALL 한손검·방패·갑옷을 기본 장착(양손검은 미장착)한다.
4. THE 지급되는 장비 SHALL Current_Durability를 최대치(20)로 초기화한다.
5. WHEN 신규 캐릭터의 정보 팝업을 조립하면, THE Info_Popup SHALL 기본 장착 보너스(STR +5, DEF +15)를 반영한다.

### Requirement 19: 예외 처리 및 UX

**User Story:** 플레이어로서, 불가능한 동작을 시도하면 명확한 안내를 받고 상태가 망가지지 않기를 원한다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL 비즈니스 예외로 Insufficient_Gold_Exception, Equip_Conflict_Exception, Inventory_Full_Exception, Item_Data_Exception을 신설한다(`RuntimeException` 직접 사용 금지).
2. WHEN 잔액 부족/착용 충돌/용량 초과가 발생하면, THE Myrpg_Web_Module SHALL 해당 동작을 거부하고 상태를 변경하지 않으며 클라이언트가 alert로 안내한다.
3. THE Item_Data_Exception SHALL 카탈로그 무결성 위반 시 기동을 실패시킨다.

### Requirement 20: 데이터 무결성 및 마이그레이션

**User Story:** 개발자로서, 골드·아이템 테이블 추가가 기존 세이브를 깨지 않기를 원한다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL `Character_Progress`에 `gold` 컬럼을 추가하고 Bank·Owned_Item 신규 테이블(`bank`, `owned_item`)만 추가하며 기존 스키마의 다른 부분을 변경하지 않는다.
2. WHERE 로컬 환경(H2 파일, `ddl-auto: update`)이면, THE Myrpg_Web_Module SHALL 신규 컬럼/테이블을 자동 생성하고, 필요 시 기존 세이브 파일 삭제로 초기화한다.
3. WHERE 프로덕션 환경(`ddl-auto: create`)이면, THE Myrpg_Web_Module SHALL 기동 시 스키마가 재생성되어 별도 마이그레이션 없이 초기화된다.
4. WHEN 새 캐릭터로 시작하면, THE Myrpg_Web_Module SHALL Gold 0·Bank_Gold 0·Default_Seed_Items 보유로 초기화된다.
5. THE 상단바(`TopBarView`/`top-bar.html`) SHALL 골드를 표시하지 않으므로 변경되지 않는다(골드 표시는 인벤토리·은행 팝업에서만).
