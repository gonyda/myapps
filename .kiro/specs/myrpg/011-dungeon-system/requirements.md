# Requirements Document

## Introduction

본 스펙(011)은 `myrpg` 모듈(`com.myapps.web.myrpg`)에 **인스턴스 랜덤 던전 시스템 — 알비 던전(`alby`) 맵 프로시저럴 생성, 안개 탐색(Fog of War), 상호작용 방 클리어 및 연쇄 전투, 보스전 및 DB 영속 생명주기**를 구현한다.

스펙 001(`character-progress-and-map-movement`)의 맵 그래프/미니맵/전체지도 뷰, 스펙 006/010의 골드·아이템·보상 체계, 스펙 007/008/009의 몬스터 카탈로그 및 전투 시스템 위에서 동작하며, 상세 기획 및 검증 사항은 `docs/dungeon_design.md`를 단일 진실 공급원(SSOT)으로 삼는다.

### 해결하려는 문제
현재 `map.json`에 정의된 던전 입구 노드(`alby-entrance`, `ciar-entrance`, `rabbie-entrance`)는 `dungeons` 배열에 `implemented: false, map: null`로만 정의되어 있으며, 던전 입구에 도달해도 던전에 진입하여 플레이할 수 있는 실제 기능이 없다. 고정 맵 외에 매 입장 시마다 새로운 구조로 펼쳐지는 **2D 랜덤 격자 미로 던전과 탐색/전투/보스전 루프**를 구축하여 게임의 핵심 탐험 콘텐츠를 제공한다.

### 핵심 구현 범위
1. **알비 던전(`alby`) 우선 단독 구현**: 다중 던전 설정 확장성을 갖춘 `dungeons.json` 스키마 구축 및 알비 던전 활성화(`implemented: true`).
2. **프로시저럴 2D 격자 맵 생성**: (0,0) 시작방 기준 동·서·남·북 4방향 무작위 꺾임(Random Walk)으로 보스방 최단거리 10개 고정 및 서브 브랜치(막다른 길 1~3칸)를 통해 총 20~23개 방의 유효 미로 그래프 생성.
3. **탐색 기반 전장의 안개 (Fog of War)**: 미방문 방 숨김, 방문/인접 방 노출(`DISCOVERED`: 회색, `CLEARED`: 흰색), 보스방 인접 방 진입 시 불길한 기운 경고 힌트 출력.
4. **상호작용 기반 방 클리어 & 10% 연쇄 전투**: 방 진입 시 몬스터가 버튼으로 출력되며, 개별 클릭 전투 승리 시 10% 확률로 연쇄 전투(추가 기습) 발생 (보스방은 연쇄 전투 제외). 방 안의 모든 몬스터 격파 시 방이 클리어 상태로 전이.
5. **이동 및 백트래킹(Backtracking) 제어**: 미클리어 방에서는 이미 클리어한 이전 방으로의 후퇴만 허용하고 새로운 미클리어 방으로의 전진은 차단. 클리어된 방은 몬스터 재스폰 없이 자유 통행.
6. **보스 처치 및 자동 복귀**: 보스(`giant-spider`) 처치 승리 즉시 클리어 모달 및 확정 보상(EXP 1000, 골드 2000, 생명력 30 포션 3개) 지급 후 던전 입구 노드(`alby-entrance`)로 자동 귀환.
7. **DB 완전 영속 저장 (`DungeonProgressEntity`)**: 브라우저를 닫거나 장시간 후 재접속해도 마지막 방 위치 및 클리어/미클리어 상태 완벽 복원.
8. **던전 생명주기 (퇴장 및 사망)**: 시작방 `[던전 나가기]` 클릭 시 던전 인스턴스 삭제 및 입구 복귀. 던전 내 전투 사망 시 인스턴스 삭제 및 마을 리스폰 (사망 전 획득한 EXP/골드는 유지).
9. **몬스터 5종 추가 (`monster.json`)**: 거미(Lv2), 붉은거미(Lv3, 방어형), 고블린(Lv3, 공격형 유리대포), 검은거미(Lv4), 거대거미(Lv7 보스) 밸런스 데이터 등록.

---

## Glossary

### 기존 재사용 용어
- **Myrpg_Web_Module**: `com.myapps.web.myrpg` 기본 패키지의 Spring Boot 4.0 Web 모듈.
- **Character_Progress**: 유일한 캐릭터 진행 JPA 엔티티. `id`, `currentNodeId`, `gold`, `experience`, `hpCurrent` 등을 보유한다.
- **Map_Node / Map_Graph**: 맵의 단일 노드(record) 및 전체 그래프 모델. `id`, `name`, `x`, `y`, `type`, `nodeType`, `links`, `monsters` 등을 보유한다.
- **Map_View_Factory**: 미니맵(`MinimapView`)과 전체지도(`FullMapView`) 격자 셀 모델을 생성하는 도메인 서비스.
- **Battle_Service / Battle_Controller**: 턴제 전투 진행, 승패 판정, 보상 지급 및 뷰 반환을 처리하는 컴포넌트.
- **Action_Log**: 화면 하단 활동 로그. 이동, 전투 결과, 조우, 힌트 등을 기록한다.
- **Item_Catalog_Service / Item**: `data/item.json` 기반 아이템 카탈로그. `hp_potion_30`(생명력 30 포션)을 제공한다.
- **Monster_Service / Monster**: `data/monster.json` 기반 몬스터 데이터 모델.

### 본 스펙(011) 신규 용어
- **Dungeon_Spec**: `classpath:data/dungeons.json`에서 로드되는 던전 설정 메타데이터(record). `id`, `name`, `entranceNodeId`, `theme`, `implemented`, `generation`, `monsterPool`, `chainCombatProbability`, `boss`, `rewards`를 보유한다.
- **Dungeon_Generation_Spec**: 던전 맵 생성 파라미터(record). `minDistanceToBoss`, `maxDistanceToBoss`, `minTotalRooms`, `maxTotalRooms`, `branchProbability`, `maxBranchDepth`를 보유한다.
- **Dungeon_Instance**: 단일 플레이어의 활성 던전 런타임 모델. 고유 ID, `characterId`, `dungeonId`, 생성된 `MapGraph`, 방별 `DungeonRoomState` 맵, 시작방 ID, 보스방 ID, 현재 방 ID를 보유한다.
- **Dungeon_Room_State**: 던전 내 개별 방의 동적 상태 모델. `roomId`, `cleared`(클리어 여부), `discovered`(발견/노출 여부), `remainingMonsters`(남은 몬스터 ID 목록)를 보유한다.
- **Dungeon_Progress_Entity**: 던전 진행 상태를 DB에 영속 보관하는 JPA 엔티티(`dungeon_progress`). `characterId`, `dungeonId`, `currentRoomId`, `dungeonGraphJson`, `roomStatesJson` 등을 보관한다.
- **Fog_Of_War**: 미탐색 영역을 가리고 방문한 방(`CLEARED`: 흰색) 및 인접한 방(`DISCOVERED`: 회색)만 지도에 표시하는 안개 시스템.
- **Chain_Combat**: 일반 방에서 몬스터를 격파했을 때 일정 확률(10%)로 추가 적이 즉시 연달아 전투를 걸어오는 기습/연쇄 전투 메커니즘.
- **Boss_Warning_Hint**: 플레이어가 보스방과 인접한 방에 진입했을 때 액션로그 및 상황 멘트에 출력되는 불길한 기운 감지 경고 멘트.
- **Dungeon_Generator**: `Dungeon_Generation_Spec`과 몬스터 풀을 바탕으로 2D 격자상에 충돌 없는 미로 그래프(`MapGraph`)를 프로시저럴 생성하는 엔진 컴포넌트.
- **Dungeon_Service**: 던전 입장, 퇴장, 방 이동 검증, 몬스터 격파 동기화, 보스전 승리 처리, DB 영속 복원을 총괄하는 애플리케이션 서비스.
- **Dungeon_Controller**: 던전 입장/퇴장 및 방 이동, 상호작용 요청을 처리하는 웹 컨트롤러.

---

## Requirements

### Requirement 1: 던전 입구 상호작용 및 입장/퇴장

**User Story:** 플레이어로서, 필드의 던전 입구 노드에서 던전에 입장하고, 던전 시작방에서 안전하게 필드로 나가고 싶다.

#### Acceptance Criteria
1. WHERE 캐릭터가 던전 입구 노드(`MapNode.type == "dungeon"`, 예: `alby-entrance`)에 위치하면, THE Play_Screen_View_Helper SHALL 상호작용 버튼 영역에 `[던전 입장]` 버튼을 노출한다.
2. WHEN 플레이어가 `[던전 입장]`을 요청하면(`POST /dungeon/enter?dungeonId=alby`), THE Dungeon_Service SHALL 던전이 구현 상태(`implemented == true`)인지 검증하고, 새 `Dungeon_Instance`를 생성하여 DB에 저장한 뒤 시작방 `(0, 0)`으로 플레이어를 진입시킨다.
3. WHERE 캐릭터가 던전 내부의 시작방(`(0, 0)`)에 위치하면, THE Play_Screen_View_Helper SHALL 상호작용 버튼 영역에 `[던전 나가기]` 버튼을 노출한다.
4. WHEN 플레이어가 시작방에서 `[던전 나가기]`를 요청하면(`POST /dungeon/leave`), THE Dungeon_Service SHALL DB의 `Dungeon_Progress_Entity`를 삭제하고, 캐릭터 위치를 해당 던전의 입구 노드(`entranceNodeId`)로 복귀시킨다.

---

### Requirement 2: 던전별 개별 메타데이터 및 생성 스펙 설정 (`dungeons.json`)

**User Story:** 시스템 설계자로서, 각 던전마다 보스방 거리, 방 개수, 출현 몬스터 풀, 보스 및 보상을 독립적인 JSON 데이터로 정의하고 싶다.

#### Acceptance Criteria
1. THE Dungeon_Spec_Repository SHALL `classpath:data/dungeons.json`을 애플리케이션 기동 시 파싱하여 검증하고 불변 목록으로 로드한다.
2. THE Dungeon_Spec SHALL 던전마다 독립된 `generation` 파라미터(`minDistanceToBoss`, `maxDistanceToBoss`, `minTotalRooms`, `maxTotalRooms`, `branchProbability`, `maxBranchDepth`)를 갖는다.
3. THE 알비_던전_스펙(`id: "alby"`) SHALL 다음 확정값을 가진다:
   - `entranceNodeId`: `"alby-entrance"`
   - `implemented`: `true`
   - `minDistanceToBoss`: `10`, `maxDistanceToBoss`: `10` (최단거리 10개 고정)
   - `minTotalRooms`: `20`, `maxTotalRooms`: `23` (전체 방 수 20~23개 범위)
   - `branchProbability`: `0.40`, `maxBranchDepth`: `3`
   - `chainCombatProbability`: `0.10` (10%)
   - `boss`: `monsterId: "giant-spider"`, `name: "거대거미"`
   - `rewards`: `exp: 1000`, `gold: 2000`, `items: [{ itemId: "hp_potion_30", quantity: 3 }]`
   - `monsterPool`: `spider`(40%), `red-spider`(25%), `goblin`(25%), `black-spider`(10%)

---

### Requirement 3: 2차원 비선형 프로시저럴 던전 맵 생성

**User Story:** 플레이어로서, 던전에 들어갈 때마다 매번 새롭고 구불구불한 2차원 격자 미로 맵을 탐험하고 싶다.

#### Acceptance Criteria
1. THE Dungeon_Generator SHALL `(x: 0, y: 0)` 좌표에 시작방(`isCleared = true`, `isDiscovered = true`, 몬스터 0마리)을 배치한다.
2. THE Dungeon_Generator SHALL 동·서·남·북 4방향 비순환 랜덤 워크(Self-avoiding Random Walk)를 수행하여 길이 10(알비 던전 기준)의 주 경로를 생성하고, 종점 노드를 **보스방(Boss Room)**으로 지정한다.
3. THE Dungeon_Generator SHALL 주 경로 노드들로부터 빈 격자 방향으로 서브 브랜치(깊이 1~3칸의 막다른 길)를 뻗어 전체 방 수가 20~23개(알비 던전 기준)에 도달할 때까지 확장한다.
4. THE Dungeon_Generator SHALL 동일한 `(x, y)` 격자 좌표에 2개 이상의 노드가 중복 생성되지 않도록 격자 충돌을 방지한다.
5. THE Dungeon_Generator SHALL 모든 인접 연결 통로에 대해 `MapNode.links`의 **양방향성(Bidirectional Link)**을 완벽히 보장한다.
6. THE Dungeon_Generator SHALL 일반 방 및 갈림길 방에 `monsterPool` 가중치 확률에 따라 1~2종류(종류당 1~2마리)의 몬스터를 무작위 배치하고, 보스방에는 고정 보스(`giant-spider`)를 배치한다.

---

### Requirement 4: 전장의 안개 (Fog of War) 및 보스방 경고 힌트

**User Story:** 플레이어로서, 아직 가보지 않은 미지의 방들을 직접 밝혀가며 탐험하고, 보스방 근처에 도달했을 때 긴장감 있는 힌트를 얻고 싶다.

#### Acceptance Criteria
1. THE Map_View_Factory SHALL 던전 내에서 플레이어의 현재 위치 노드와 인접하지 않은 미발견 방(`discovered == false`)을 미니맵과 전체지도에서 숨김(빈 투명 셀) 처리한다.
2. THE Map_View_Factory SHALL 발견되었으나 아직 소탕되지 않은 미클리어 방(`discovered == true && cleared == false`)을 **회색(`node-dungeon-uncleared`)**으로 렌더링한다.
3. THE Map_View_Factory SHALL 몬스터가 모두 소탕된 안전한 클리어 방(`cleared == true`)을 **흰색(`node-dungeon-cleared`)**으로 렌더링한다.
4. WHEN 플레이어가 특정 방 $R$에 진입하면, THE Dungeon_Service SHALL $R$ 및 $R$과 연결된 모든 이웃 방 $N \in R.\text{links}$의 상태를 `discovered = true`로 전환한다.
5. WHERE 플레이어가 보스방과 직접 연결된 방(보스방 바로 옆 방)에 진입하면, THE Dungeon_Service SHALL 액션로그 및 상황 멘트에 *"어두운 통로 너머 깊은 곳에서 불길하고 강력한 기운이 느껴집니다..."* 경고 힌트를 출력한다.
6. THE Map_View_Factory SHALL 일반 미클리어 방의 구체적인 출현 몬스터 정보나 보스방 아이콘을 해당 방에 진입하기 전까지 지도에 노출하지 않는다.

---

### Requirement 5: 방 상호작용 및 몬스터 처치 / 방 클리어 메커니즘

**User Story:** 플레이어로서, 방에 진입했을 때 몬스터를 클릭하여 전투를 치르고, 모든 적을 쓰러뜨려 방을 클리어하고 싶다.

#### Acceptance Criteria
1. WHERE 미클리어 방에 진입하면, THE Play_Screen_View_Helper SHALL 해당 방의 `remainingMonsters` 목록을 상호작용 버튼(예: `[거미 (Lv.2)]`, `[고블린 (Lv.3)]`)으로 노출한다.
2. WHEN 플레이어가 특정 몬스터 버튼을 클릭하여 전투에 돌입하고 승리하면, THE Dungeon_Service SHALL 해당 몬스터를 현재 방의 `remainingMonsters` 목록에서 제거하고 DB 상태를 즉시 동기화한다.
3. THE Battle_Service SHALL 몬스터를 격파할 때마다 경험치(`EXP`)와 골드(`Gold`) 및 전리품을 즉시 `Character_Progress`에 지급/누적한다.
4. WHEN 방 안의 마지막 몬스터가 처치되어 `remainingMonsters`가 비어있게 되면, THE Dungeon_Service SHALL 해당 방의 상태를 `cleared = true`로 전이시키고 액션로그에 *"방 안의 모든 적을 소탕했습니다! 전진 통로가 열립니다."*를 기록한다.
5. WHERE 이미 클리어된 방(`cleared == true`)에 위치하면, THE Play_Screen_View_Helper SHALL 상호작용 버튼 영역에 몬스터를 노출하지 않고 안전한 상태를 유지한다.

---

### Requirement 6: 일반방 10% 연쇄 전투 (Chain Combat)

**User Story:** 플레이어로서, 일반 몬스터 처치 직후 간혹 발생하는 기습 연쇄 전투로 긴장감 넘치는 전투를 경험하고 싶다.

#### Acceptance Criteria
1. WHEN 플레이어가 **일반 방**에서 몬스터와의 전투에서 승리하면, THE Battle_Service SHALL `chainCombatProbability`(10%) 확률을 판정한다.
2. WHERE 10% 연쇄 전투가 발동하면, THE Battle_Service SHALL 전투 종료 뷰 대신 즉시 동일 몬스터 그룹과의 추가 연속 전투 화면으로 전환하고 액션로그에 *"{몬스터} 무리가 추가로 기습해왔다!"*를 출력한다.
3. WHERE 연쇄 전투에서도 승리하면, THE Battle_Service SHALL 추가 처치 보상(EXP/골드)을 정상 지급하고 해당 몬스터 그룹 소탕을 완료한다.
4. WHERE 전투 대상이 **보스방 몬스터(`giant-spider`)**인 경우, THE Battle_Service SHALL 연쇄 전투 확률 판정을 완전히 배제하고 즉시 최종 던전 클리어 단계로 진행한다.

---

### Requirement 7: 던전 이동 및 백트래킹 (Backtracking) 제어

**User Story:** 플레이어로서, 위험할 때는 이미 깬 안전한 방으로 후퇴할 수 있고, 방을 깨야만 새로운 미지의 방으로 나아갈 수 있는 규칙을 따르고 싶다.

#### Acceptance Criteria
1. WHERE 플레이어가 미클리어 방(`cleared == false`)에 위치할 때:
   - WHEN 이미 클리어된 인접 방(`cleared == true`)으로 이동을 요청하면, THE Movement_Service SHALL 이동을 **허용**하고 정상 이동 처리한다 (후퇴 허용).
   - WHEN 새로운 미클리어 인접 방(`cleared == false`)으로 이동을 요청하면, THE Movement_Service SHALL `BlockedMovementException`을 던지고 이동을 **차단**하며 액션로그에 *"앞으로 나아가려면 이 방의 적들을 모두 처치해야 합니다."*를 출력한다.
2. WHERE 플레이어가 클리어된 방(`cleared == true`)에 위치할 때, THE Movement_Service SHALL 연결된 모든 인접 방으로의 이동을 자유롭게 허용한다.
3. THE Dungeon_Service SHALL 이미 클리어된 방을 다시 지나갈 때 몬스터를 재스폰시키지 않는다.

---

### Requirement 8: 보스 처치, 클리어 보상 및 던전 복귀

**User Story:** 플레이어로서, 보스 거대거미를 물리치고 알비 던전을 정복하여 풍성한 보상을 획득하고 입구로 귀환하고 싶다.

#### Acceptance Criteria
1. WHERE 플레이어가 보스방에 진입하면, THE Play_Screen_View_Helper SHALL `[거대거미 (BOSS Lv.7)]` 상호작용 버튼을 노출한다.
2. WHEN 플레이어가 거대거미를 격파하여 승리하면:
   - THE Dungeon_Service SHALL 던전 클리어 모달을 출력한다 (*"알비 던전을 완전히 정복했습니다!"*).
   - THE Dungeon_Service SHALL 확정 클리어 보상(경험치 1000, 골드 2000, 생명력 30 포션 3개)을 캐릭터 인벤토리 및 스탯에 일괄 지급한다.
   - THE Dungeon_Service SHALL DB의 `Dungeon_Progress_Entity`를 삭제(소멸)한다.
   - THE Dungeon_Service SHALL 캐릭터의 현재 노드를 던전 입구 노드(`alby-entrance`)로 복귀시키고 월드맵 화면으로 전환한다.

---

### Requirement 9: 던전 상태 DB 영속성 및 생명주기

**User Story:** 플레이어로서, 플레이 도중 웹 브라우저를 닫고 몇 시간 뒤에 다시 접속해도 던전 진행 상황이 안전하게 유지되기를 원한다.

#### Acceptance Criteria
1. THE Dungeon_Service SHALL 던전 입장, 방 이동, 몬스터 처치 등 모든 상태 변경 시마다 `Dungeon_Progress_Entity`를 DB 테이블(`dungeon_progress`)에 즉시 저장/갱신한다.
2. WHEN 플레이어가 브라우저를 닫거나 세션이 종료된 후 재접속(`GET /`)할 때, THE Dungeon_Service SHALL 해당 캐릭터의 활성 `Dungeon_Progress_Entity`를 조회하여 마지막으로 머물던 방 위치와 클리어/미클리어 상태를 완벽히 복원하여 렌더링한다.
3. WHEN 플레이어가 던전 내 전투 중 사망(`hpCurrent == 0`)하면:
   - THE Dungeon_Service SHALL DB의 `Dungeon_Progress_Entity`를 즉시 삭제한다 (던전 진행도 초기화).
   - THE Character_Service SHALL 캐릭터를 시작 마을(`tir-chonaill`)로 리스폰시키고 HP/MP/스태미나를 100% 회복시킨다 (단, 사망 전 획득한 경험치와 골드는 보존된다).

---

### Requirement 10: 알비 던전 몬스터 카탈로그 데이터 (`monster.json`)

**User Story:** 게임 밸런서로서, 알비 던전에 출현하는 몬스터 5종이 정해진 레벨과 '대등' 밸런스 규격을 준수하여 등록되기를 원한다.

#### Acceptance Criteria
1. THE Monster_Service SHALL `classpath:data/monster.json`에 다음 5종의 신규 몬스터를 추가 파싱 및 로드한다:
   - `spider`: 이름 "거미", 타입 `normal`, 레벨 `2`, HP `65`, ATK `48`, DEF `4`, Crit `30`(3.0%), EXP `30`, Gold `8~20`, 드랍 `hp_potion_30`(10%)
   - `red-spider`: 이름 "붉은거미", 타입 `normal`, 레벨 `3`, HP `80`, ATK `50`, DEF `5` (방어형), Crit `30`(3.0%), EXP `40`, Gold `12~25`, 드랍 `hp_potion_30`(15%)
   - `goblin`: 이름 "고블린", 타입 `normal`, 레벨 `3`, HP `70`, ATK `54`, DEF `3` (공격형 유리대포), Crit `30`(3.0%), EXP `42`, Gold `15~30`, 드랍 `hp_potion_30`(20%)
   - `black-spider`: 이름 "검은거미", 타입 `normal`, 레벨 `4`, HP `100`, ATK `56`, DEF `6`, Crit `40`(4.0%), EXP `55`, Gold `20~45`, 드랍 `hp_potion_30`(25%)
   - `giant-spider`: 이름 "거대거미", 타입 `boss`, 레벨 `7`, HP `380`, ATK `72`, DEF `12`, Crit `70`(7.0%), 경감 `60%`, 반격 `50%`, EXP `350`, Gold `150~300`, 드랍 `hp_potion_30`(100%, 2~3개)
2. THE Monster_Service SHALL 모든 일반 몬스터의 CP가 동레벨 플레이어 기준 난이도비 1.08 ~ 1.19 범위의 **대등(정면 접전)** 규격을 만족하도록 보장한다.
