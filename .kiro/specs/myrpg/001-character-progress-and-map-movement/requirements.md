# Requirements Document

## Introduction

`myrpg`는 웹 기반 솔로 RPG의 Web 모듈(`com.myapps.web.myrpg`)이다. 본 스펙(001)은 `myrpg` 모듈의 첫 번째 기능으로, 다음 세 가지 핵심 역량에 한정한다.

1. **플레이 화면 제공** — `docs/myrpg-mockup.html` 목업(모바일 세로 전용)을 서버사이드 렌더링(Thymeleaf)으로 제공하되, 화면을 **영역별 Thymeleaf 조각(fragment) 파일로 분리**하여 개발한다. 이때 목업의 시각적 디자인과 상호작용 동작(CSS/JS 포함)을 **1도 변경 없이 그대로(1:1)** 보존한다.
2. **맵 노드 간 이동** — 마을(town)과 자유필드(field) 노드 사이를 그래프 연결(links)을 따라 이동한다. 던전(dungeon)은 **입구 노드만** 존재하며, 던전 내부 진입은 본 스펙의 **범위 밖(향후 확장)** 이다. 게임은 **턴제**로 진행되며 맵 이동 1회가 1턴이다.
3. **캐릭터 진행상황 영속화** — 캐릭터의 현재 상태(현재 맵 노드 포함)를 데이터베이스에 저장하고, 플레이 시작 시 로드한다. 저장은 **매 턴 종료 시점**에 수행한다.

고정 게임 데이터(맵, 몬스터, 아이템, 상황 멘트)는 JSON 파일로 관리하되, `docs/`가 아니라 **모듈 리소스 폴더**(예: `myrpg/src/main/resources/data/map.json`, `myrpg/src/main/resources/data/ambience.json`)에서 **클래스패스 리소스**로 로드한다. `docs/`의 JSON/목업 파일은 프로토타입/원본이며, 실제 구현에서는 모듈 리소스로 이관하여 사용한다. 새로운 고정 데이터(맵/몬스터/아이템/상황 멘트 등)가 추가될 때는 코드 변경 없이 **해당 JSON 리소스 파일에만 추가**하는 방식으로 관리한다. **데이터베이스에는 오직 캐릭터의 진행상황/상태만 저장**한다. 아이템·스킬·장비·인벤토리·전투·던전 내부는 본 스펙에서 구현하지 않되, 향후 확장을 막지 않도록 도메인/영속 모델을 설계하는 것을 명시적 요구사항으로 포함한다.

## Glossary

- **Myrpg_Web_Module**: `com.myapps.web.myrpg` 기본 패키지를 가지는 Spring Boot 4.0 Web 모듈. 아래 하위 시스템의 집합.
- **PlayScreen_Controller**: 플레이 화면(HTTP GET) 요청을 받아 Thymeleaf 템플릿(영역별 fragment 조합)을 서버사이드 렌더링하는 시스템.
- **Play_Screen_Fragment**: 플레이 화면을 영역별로 분리한 Thymeleaf 조각 파일. 예: 상단 바, 좌측 사이드바, 가운데 영역, 하단 행동 로그, 팝업(장비/인벤/스킬/정보, 전체지도) 등. 각 fragment는 목업(`docs/myrpg-mockup.html`)의 해당 영역을 디자인/동작 변경 없이 1:1로 보존한다.
- **Character_Service**: 캐릭터 진행상황의 생성/조회/갱신을 담당하는 애플리케이션 서비스.
- **Character_Store**: 캐릭터 진행상황을 저장하는 관계형 데이터베이스 영속 계층(JPA 기반).
- **Map_Service**: 모듈 리소스 `classpath:data/map.json`을 로드하여 노드 그래프(노드/연결/좌표)를 제공하는 시스템.
- **Movement_Service**: 현재 노드에서 인접 노드로의 이동 요청을 검증하고 처리하는 시스템.
- **Ambience_Service**: 모듈 리소스 `classpath:data/ambience.json`을 기반으로 현재 테마·계절·시간대에 맞는 상황 멘트를 선택하는 시스템.
- **Game_Data_Resource**: 고정 게임 데이터를 담는 모듈 리소스(클래스패스) JSON 파일. `myrpg/src/main/resources/data/` 하위에 위치(예: `map.json`, `ambience.json`). 신규 고정 데이터 추가는 코드 변경 없이 해당 파일 수정만으로 이루어진다.
- **Turn**: 턴제 진행의 최소 단위. 맵 이동 1회가 1턴이며, 향후 전투 행동도 동일하게 1턴 단위로 처리된다. 각 턴 종료 시 Character_Progress가 Character_Store에 저장된다.
- **Character_Progress**: DB에 저장되는 캐릭터 상태 단위. 닉네임, 레벨, 경험치, 스탯, 현재 맵 노드 id 등을 포함.
- **Map_Node**: 맵 그래프의 단일 지점. `id`, `name`, `type`(town|field|dungeon), 좌표 `x`/`y`, 연결 `links[]`(양방향)를 가진다.
- **Node_Type**: 노드 유형. 값은 `town`(마을), `field`(자유필드), `dungeon`(던전 입구) 중 하나.
- **Start_Node**: 신규 캐릭터의 시작 위치. `classpath:data/map.json`의 `startNodeId`(현재 `tir-chonaill`, 티르코네일).
- **Default_Character**: Character_Store가 비어 있을 때 자동 생성되는 기본 캐릭터. 닉네임은 `고니`.
- **Current_Level**: 캐릭터의 현재 레벨. 신규 캐릭터는 1로 시작.
- **Accumulated_Level**: 캐릭터가 지금까지 누적한 총 레벨(누적레벨). 신규 캐릭터는 1로 시작. 향후 환생/전직 등에서 현재 레벨과 별도로 누적되도록 분리 보관.
- **Base_Stats**: 신규 캐릭터의 초기 스탯 집합. STR 10, DEX 10, INT 10, Critical 5, Defense(DEF) 5, HP 100, MP 100, Stamina 100. HP/MP/Stamina는 백분율(%)이 아니라 정수 현재값/최대값 쌍으로 관리·표시하며(예: `23/100`), 신규 캐릭터는 현재값이 최대값과 동일한 `100/100` 상태로 시작한다. UI의 게이지/바는 현재값 대비 최대값 비율로 렌더링될 수 있으나, 표시 값과 저장 데이터는 정수이다.
- **Season**: 실제 월(1~12)에서 매핑된 계절 키(`spring`/`summer`/`autumn`/`winter`).
- **Time_Of_Day**: 실제 시(0~23)에서 매핑된 시간대 키(`dawn`/`morning`/`afternoon`/`late-afternoon`/`night`/`late-night`).
- **Theme**: 상황 멘트 선택에 사용하는 장소 성격 키. 노드의 `theme` 값이 있으면 그것을, 없으면 노드의 `type`을 사용.
- **Action_Log**: 플레이어 행동과 결과를 시간순으로 기록하는 로그. 각 항목은 타임스탬프(`YYYY-MM-DD HH:MM:SS` 형식, 초 단위까지 표기), 메시지, 타입(예: move)을 가진다.

## Requirements

### Requirement 1: 플레이 화면 서버사이드 렌더링

**User Story:** 플레이어로서, 브라우저에서 `myrpg` 플레이 화면에 접속하면 목업과 동일한 화면을 보고 싶다. 그래야 별도 클라이언트 설치 없이 게임을 시작할 수 있다.

#### Acceptance Criteria

1. WHEN 플레이어가 플레이 화면 경로로 HTTP GET 요청을 보내면, THE PlayScreen_Controller SHALL `docs/myrpg-mockup.html`과 동일한 레이아웃(상단 바 / 좌측 사이드바 / 가운데 영역 / 하단 행동 로그)을 가진 HTML 페이지를 반환한다.
2. THE PlayScreen_Controller SHALL 목업의 CSS 디자인 토큰(`:root` 변수)과 스타일을 변경 없이 재사용한다.
3. THE PlayScreen_Controller SHALL 목업의 지도 줌/이동(줌/팬), 이동 패드, 팝업 동작을 포함한 JavaScript를 변경 없이 재사용한다.
4. WHEN 플레이 화면을 렌더링하면, THE PlayScreen_Controller SHALL 상단 바에 현재 캐릭터의 닉네임과 Current_Level을 표시하고, 경험치, HP, MP, Stamina를 각각 현재값 대비 최대값 비율로 채워진 게이지 바(목업과 동일한 퍼센테이지 채움 방식)로 표시한다.
5. WHEN 플레이 화면을 렌더링하면, THE PlayScreen_Controller SHALL HP, MP, Stamina 각 게이지 바 위에 현재값과 최대값 수치를 `현재값 / 최대값` 형식(예: `23 / 100`)으로 연하게(faint) 오버레이하여 표시한다.
6. WHEN 플레이 화면을 렌더링하면, THE PlayScreen_Controller SHALL 경험치 게이지 바 위에 현재 경험치와 다음 레벨까지 필요한 경험치 수치를 `현재 경험치 / 다음 레벨 필요 경험치` 형식(예: `23 / 100`)으로 연하게(faint) 오버레이하여 표시한다.
7. WHEN 플레이 화면을 렌더링하면, THE PlayScreen_Controller SHALL 현재 노드 기준의 미니맵과 현재 맵 이름을 표시한다.
8. THE PlayScreen_Controller SHALL 플레이 화면을 영역별 Play_Screen_Fragment(최소: 상단 바, 좌측 사이드바, 가운데 영역, 하단 행동 로그, 팝업(장비/인벤/스킬/정보), 전체지도 팝업)로 분리된 Thymeleaf 템플릿으로 구성한다.
9. THE PlayScreen_Controller SHALL 분리된 Play_Screen_Fragment를 조합하여 렌더링한 최종 HTML의 시각적 디자인(디자인 토큰, 레이아웃, 색상, 간격)을 `docs/myrpg-mockup.html`과 동일하게 유지한다.
10. THE PlayScreen_Controller SHALL 분리된 Play_Screen_Fragment의 상호작용 동작(지도 줌/팬, 이동 패드 동작, 팝업 열기/닫기)을 `docs/myrpg-mockup.html`과 동일하게 유지한다.
11. WHERE HP, MP, Stamina의 현재값이 동시에 모두 0(예: `0/100`)인 경우, THE PlayScreen_Controller SHALL 해당 상태를 보정하지 않고 각 게이지 바를 0% 채움으로 렌더링하며, 수치 오버레이를 `0 / 최대값` 형식으로 그대로 표시한다.

### Requirement 2: 기본 캐릭터 자동 생성

**User Story:** 신규 플레이어로서, 별도 회원가입이나 캐릭터 생성 절차 없이 바로 게임을 시작하고 싶다. 그래야 진입 장벽 없이 플레이할 수 있다.

#### Acceptance Criteria

1. WHEN 플레이어가 플레이 화면에 접속하고 Character_Store에 저장된 Character_Progress가 하나도 없으면, THE Character_Service SHALL 닉네임이 `고니`인 Default_Character를 정확히 1개만 생성한다.
2. WHEN Default_Character를 생성하면, THE Character_Service SHALL Base_Stats(STR 10, DEX 10, INT 10, Critical 5, DEF 5, HP 100, MP 100, Stamina 100)를 초기값으로 설정한다.
3. WHEN Default_Character를 생성하면, THE Character_Service SHALL Current_Level을 1, Accumulated_Level을 1, 경험치를 0으로 설정한다.
4. WHEN Default_Character를 생성하면, THE Character_Service SHALL 현재 맵 노드 id를 Start_Node(`tir-chonaill`)로 설정한다.
5. WHEN Default_Character를 생성하면, THE Character_Service SHALL 생성된 Character_Progress를 Character_Store에 저장한다.
6. IF 플레이어가 플레이 화면에 접속했을 때 Character_Store에 이미 1개 이상의 Character_Progress가 존재하면, THEN THE Character_Service SHALL 새로운 Default_Character를 생성하지 않고 기존 Character_Progress를 로드한다.
7. IF Default_Character의 Character_Progress 저장이 실패하면, THEN THE Character_Service SHALL 부분 생성 상태를 남기지 않도록 저장을 롤백하고 생성 실패를 나타내는 오류를 반환한다.
8. THE Character_Service SHALL 저장(save) 연산 자체가 실패한 경우에 한하여 생성 실패 오류를 반환하며, 그 외 조건에서는 생성 실패 오류를 반환하지 않는다.
9. IF Character_Progress 저장 실패 후 수행한 롤백 연산 자체가 실패하면, THEN THE Character_Service SHALL 오류를 반환하되 실패한 롤백이 남긴 상태를 그대로 둔다(추가 복구를 시도하지 않는다).

### Requirement 3: 캐릭터 진행상황 영속화 및 로드

**User Story:** 플레이어로서, 게임을 종료했다가 다시 접속해도 이전 상태(레벨, 스탯, 현재 위치)가 유지되기를 원한다. 그래야 진행 내용을 잃지 않는다.

#### Acceptance Criteria

1. THE Character_Store SHALL Character_Progress의 닉네임, Current_Level, Accumulated_Level, 경험치, STR, DEX, INT, Critical, DEF, HP, MP, Stamina, 현재 맵 노드 id를 영속 저장한다.
2. WHEN 플레이어가 플레이 화면에 접속하면, THE Character_Service SHALL Character_Store에서 캐릭터 진행상황을 로드하여 화면 모델로 제공한다.
3. WHEN 한 Turn이 종료되면(맵 이동 1회 완료 시, 향후 전투 행동 완료 시 포함), THE Character_Service SHALL 변경된 Character_Progress를 Character_Store에 저장한다.
4. THE Character_Store SHALL `spring-boot-starter-data-jpa`를 사용하여 관계형 데이터베이스에 진행상황을 저장한다.
5. THE Character_Store SHALL 고정 게임 데이터(맵/몬스터/아이템/상황 멘트)를 저장하지 않는다.

### Requirement 4: 맵 데이터 로드

**User Story:** 시스템 구성원으로서, 맵 구조를 코드 밖 JSON으로 관리하고 싶다. 그래야 노드나 연결을 추가할 때 코드 변경 없이 데이터만 수정하면 된다.

#### Acceptance Criteria

1. THE Map_Service SHALL 모듈 리소스 `classpath:data/map.json`(원본은 `docs/map.json`에서 모듈로 이관)과 동일한 스키마(`nodes[]`의 `id`, `name`, `type`, `x`, `y`, `links[]`, `startNodeId`, `dungeons[]`)를 파싱하여 노드 그래프를 제공한다.
2. WHEN 노드 그래프를 로드하면, THE Map_Service SHALL 각 Map_Node의 Node_Type을 `town`, `field`, `dungeon` 중 하나로 분류한다.
3. WHEN 특정 노드 id로 조회 요청을 받으면, THE Map_Service SHALL 해당 Map_Node의 `name`, `type`, 좌표, `links`를 반환한다.
4. IF 존재하지 않는 노드 id로 조회 요청을 받으면, THEN THE Map_Service SHALL 조회 실패를 나타내는 오류를 반환한다.
5. THE Map_Service SHALL 두 Map_Node의 `links`가 양방향임을 보장한다(A의 links에 B가 있으면 B의 links에도 A가 있음).

### Requirement 5: 맵 노드 간 이동

**User Story:** 플레이어로서, 이동 패드의 화살표로 인접한 마을이나 필드로 이동하고 싶다. 그래야 세계를 탐험할 수 있다.

#### Acceptance Criteria

1. WHEN 플레이어가 현재 노드와 `links`로 연결된 대상 노드로 이동을 요청하면, THE Movement_Service SHALL 캐릭터의 현재 맵 노드 id를 대상 노드로 변경한다.
2. WHEN 이동이 성공하면(해당 Turn이 종료되면), THE Character_Service SHALL 변경된 현재 맵 노드 id를 포함한 Character_Progress를 Character_Store에 저장한다.
3. WHEN 이동이 성공하면, THE Movement_Service SHALL `이동` 타입의 Action_Log 항목(예: `{맵이름}(으)로 이동했습니다`)을 추가한다.
4. IF 플레이어가 현재 노드와 연결되지 않은 노드로 이동을 요청하면, THEN THE Movement_Service SHALL 이동을 거부하고 이동 불가 안내를 반환한다.
5. WHEN 이동이 성공하면, THE PlayScreen_Controller SHALL 미니맵, 현재 맵 이름, 상황 멘트를 이동 후 노드 기준으로 갱신한다.

### Requirement 6: 던전 입구 노드 처리(진입은 범위 밖)

**User Story:** 플레이어로서, 던전 입구 노드로는 이동할 수 있되 아직 내부에는 들어갈 수 없다는 것을 명확히 알고 싶다. 그래야 미구현 기능에 혼란을 겪지 않는다.

#### Acceptance Criteria

1. WHERE 대상 노드의 Node_Type이 `dungeon`이고 현재 노드와 연결되어 있으면, THE Movement_Service SHALL 던전 입구 노드로의 이동을 허용한다.
2. THE Map_Service SHALL 던전 정의(`dungeons[]`)를 `implemented: false`, `map: null` 상태로 그대로 노출한다.
3. IF 플레이어가 던전 내부 진입(입장)을 요청하면, THEN THE Movement_Service SHALL 진입을 거부하고 준비 중(미구현) 안내를 반환한다.
4. THE PlayScreen_Controller SHALL 던전 입구 노드를 미니맵/전체지도에서 `dungeon` 유형 색(`.type-dungeon`)으로 표시한다.
5. IF 던전 내부 진입 요청을 거부하는 과정에서 준비 중 안내 메시지 전송/생성이 실패하더라도, THEN THE Movement_Service SHALL 진입 거부를 그대로 유지한다.

### Requirement 7: 상황 멘트 선택 시스템

**User Story:** 플레이어로서, 현재 장소·계절·시간대에 어울리는 분위기 묘사를 보고 싶다. 그래야 게임 세계에 몰입할 수 있다.

#### Acceptance Criteria

1. WHEN 상황 멘트를 요청받으면, THE Ambience_Service SHALL 실제 현재 시각으로부터 Season과 Time_Of_Day를 산출한다.
2. WHEN Season과 Time_Of_Day가 정해지면, THE Ambience_Service SHALL 현재 노드의 Theme에 해당하는 `themes[theme][season][timeOfDay]` 후보 목록에서 무작위로 한 개를 선택한다.
3. IF 해당 Theme·Season·Time_Of_Day 후보가 비어 있으면, THEN THE Ambience_Service SHALL 동일 Theme 내 폴백 후보에서 한 개를 선택한다.
4. IF 어떤 후보도 존재하지 않으면, THEN THE Ambience_Service SHALL `{맵이름} 주변을 둘러봅니다.` 기본 문구를 반환한다.
5. THE Ambience_Service SHALL 노드의 Theme 결정 시 노드에 `theme` 값이 있으면 그 값을, 없으면 노드의 `type` 값을 사용한다.

### Requirement 8: 미니맵 및 전체지도 렌더링 데이터 제공

**User Story:** 플레이어로서, 내 위치를 중심으로 주변 노드와 연결선을 보고 싶다. 그래야 어디로 이동할 수 있는지 파악할 수 있다.

#### Acceptance Criteria

1. WHEN 미니맵 데이터를 요청받으면, THE Map_Service SHALL 현재 노드의 절대 좌표(x, y)를 기준으로 오프셋 dx(-4 이상 +4 이하)와 dy(-2 이상 +2 이하) 범위(가로 9칸 × 세로 5칸, 최대 45개 노드) 안의 노드만 포함하며, 현재 노드(dx=0, dy=0)를 항상 포함한다.
2. WHEN 미니맵 데이터를 제공하면, THE Map_Service SHALL 포함된 각 노드에 대해 노드 id, 미니맵 격자 좌표(`grid-column = 5+dx`(1~9), `grid-row = 3+dy`(1~5)), Node_Type을 함께 제공한다.
3. WHEN 미니맵/전체지도 데이터를 제공하면, THE Map_Service SHALL 두 노드가 모두 해당 표시 범위 안에 있고 `links`로 실제 연결된 경우에만 그 두 노드 id 쌍의 간선 정보를 포함한다.
4. WHEN 미니맵 또는 전체지도를 렌더링하면, THE PlayScreen_Controller SHALL 현재 노드에 `.current` 강조 클래스를 적용하여 렌더링한다.
5. WHEN 전체지도 데이터를 요청받으면, THE Map_Service SHALL 모든 Map_Node에 대해 노드 id, 이름 라벨, Node_Type, `links` 연결 정보를 제공한다.
6. IF 미니맵 또는 전체지도 데이터 요청 시 현재 노드 id가 좌표(x, y)를 가지지 않거나 노드 그래프에서 확인되지 않으면, THEN THE Map_Service SHALL 데이터를 생성하지 않고 데이터 생성 실패를 나타내는 오류를 반환한다.
7. IF 미니맵 또는 전체지도 데이터 생성이 실패하면, THEN THE Map_Service SHALL 개별 간선이 표시 범위·연결 조건을 만족하더라도 간선 정보를 포함하지 않는다.

### Requirement 9: 행동 로그

**User Story:** 플레이어로서, 내가 한 행동과 결과가 시간순으로 기록되기를 원한다. 그래야 최근에 무슨 일이 있었는지 확인할 수 있다.

#### Acceptance Criteria

1. WHEN Action_Log 항목이 추가되면, THE PlayScreen_Controller SHALL 해당 항목을 타임스탬프(`YYYY-MM-DD HH:MM:SS` 형식, 초 단위까지 표기), 메시지 텍스트, 타입과 함께 로그 목록에 추가한다.
2. WHEN Action_Log 항목이 추가되어 전체 항목 수가 10개를 초과하면, THE PlayScreen_Controller SHALL 가장 오래된 항목부터 초과분을 제거하여 항목 수를 최대 10개로 유지한다.
3. WHEN Action_Log 항목이 타입 값 없이 추가되면, THE PlayScreen_Controller SHALL 해당 항목의 타입을 `move`(이동)로 설정한다.
4. WHEN Action_Log 목록을 표시할 때, THE PlayScreen_Controller SHALL 모든 항목을 타임스탬프 오름차순(가장 오래된 항목이 먼저, 가장 최신 항목이 마지막)으로 정렬하여 표시한다.

### Requirement 10: 향후 확장을 위한 모델 설계 제약

**User Story:** 개발자로서, 지금은 아이템·스킬·장비·인벤토리·전투·던전을 구현하지 않지만, 나중에 이들을 추가할 때 기존 캐릭터/영속 모델을 크게 바꾸지 않기를 원한다. 그래야 확장 비용이 낮아진다.

#### Acceptance Criteria

1. THE Character_Progress 모델 SHALL 인벤토리, 장착 장비, 스킬 목록을 향후 별도 연관 엔티티로 추가할 수 있도록 캐릭터 식별자를 안정적인 기본 키로 노출한다.
2. THE Map_Service SHALL 던전 정의에 향후 자체 `map`(nodes 구조 동일)을 채워 넣을 수 있는 참조(`dungeonId`)를 던전 입구 노드에서 유지한다.
3. THE Myrpg_Web_Module SHALL 고정 데이터(맵/상황 멘트)와 캐릭터 진행상황(DB)을 분리된 계층으로 유지하여, 몬스터·아이템 등 신규 고정 데이터 파일 추가가 캐릭터 영속 모델 변경을 요구하지 않도록 한다.
4. THE Node_Type SHALL 새로운 유형(예: 던전 내부 노드) 추가 시 기존 `town`/`field`/`dungeon` 처리 로직을 깨지 않도록 확장 가능한 형태로 정의된다.
