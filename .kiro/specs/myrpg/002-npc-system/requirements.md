# Requirements Document

## Introduction

본 스펙(002)은 `myrpg` 모듈(`com.myapps.web.myrpg`)에 **NPC 시스템**을 추가한다. 스펙 001에서 구축한 플레이 화면(서버사이드 렌더링) · 맵 노드 이동 · 캐릭터 진행상황 영속화 위에서 동작한다.

이번 스펙의 범위는 다음 네 가지에 한정한다.

1. **NPC 데이터 추가** — 티르코네일/던바튼에 배치될 NPC 데이터(총 10명)를 고정 게임 데이터로 관리한다. 원본 초안은 `docs/npc-dialogue.json`(id/name/type/nodeId/personality/lines 구조)이며, 실제 구현에서는 스펙 001과 동일한 방침에 따라 모듈 리소스 `myrpg/src/main/resources/data/`로 이관하여 클래스패스 리소스로 로드한다.
2. **NPC 맵 배치** — 각 NPC는 `nodeId`로 맵 노드(`map.json`의 town 노드)에 배치된다. 플레이어가 해당 노드에 진입(위치)하면 그 노드에 속한 NPC들이 가운데 영역의 상호작용 버튼(`.interactions`)으로 노출된다.
3. **NPC 멘트** — NPC 상호작용 버튼을 클릭하면 NPC 멘트 칸(`.npc-talk`)에 대사가 출력된다. 대사 선택은 **시간대(Time_Of_Day) 기반**이다. `lines.default`와 `lines.byTime[현재 시간대]`를 병합한 후보 풀에서 무작위 1개를 선택하며, 후보가 전혀 없으면 성격 기반 기본 문구로 폴백한다. 계절은 사용하지 않는다. 이는 스펙 001의 `Ambience_Service`(계절+시간대) 패턴과 유사하되 계절을 제외한 것이다.
4. **NPC 타입별 상호작용 버튼** — NPC 멘트 칸 하단에 해당 NPC 타입에 대응하는 행동 버튼을 가로로 작게 배치한다. 1차 구현에서는 버튼 클릭 시 실제 기능 대신 `alert`로 "구현 예정입니다" 안내만 표시한다. 각 행동 버튼의 실제 기능(상점/수리/보관 등)은 이번 스펙의 범위 밖이며 별도 스펙에서 진행한다.

고정 데이터(NPC)는 JSON 리소스로만 관리하며, NPC를 추가/수정할 때 코드 변경 없이 해당 JSON 파일만 수정한다. **데이터베이스에는 NPC 데이터를 저장하지 않는다.** NPC 데이터는 스펙 001의 데이터 계층 분리 방침(고정 데이터는 JSON, 진행상황만 DB)을 그대로 따른다.

## Glossary

- **Myrpg_Web_Module**: `com.myapps.web.myrpg` 기본 패키지를 가지는 Spring Boot 4.0 Web 모듈. 본 스펙의 상위 시스템.
- **Npc_Data_Resource**: NPC 고정 데이터를 담는 모듈 리소스(클래스패스) JSON 파일. `myrpg/src/main/resources/data/npc.json`에 위치하며 원본은 `docs/npc-dialogue.json`. 신규 NPC 추가는 코드 변경 없이 이 파일 수정만으로 이루어진다.
- **Npc**: 단일 NPC 데이터 단위. `id`(인물 식별자), `name`(표시 이름), `type`(Npc_Type), `nodeId`(배치된 맵 노드 id), `personality`(성격 서술), `lines`(대사 풀: `default`, `byTime`)를 가진다.
- **Npc_Type**: NPC의 기능 분류 키. 값은 `chief`, `blacksmith`, `magic-school`, `school`, `healer`, `bank` 중 하나.
- **Npc_Type_Label**: Npc_Type의 한글 표시명. 매핑은 `chief`→`촌장`, `blacksmith`→`대장간`, `magic-school`→`마법학교`, `school`→`학교`, `healer`→`힐러집`, `bank`→`은행`.
- **Npc_Service**: `classpath:data/npc.json`을 로드하여 NPC 목록을 제공하고, 특정 노드 id에 배치된 NPC 목록을 반환하는 시스템.
- **Npc_Dialogue_Service**: 특정 NPC와 현재 Time_Of_Day를 입력받아 출력할 대사 1개를 선택하는 시스템.
- **Npc_Interaction_Button**: 가운데 영역 상호작용 버튼(`.interactions`)에 노출되는 NPC 버튼. 라벨은 `{이름} ({Npc_Type_Label})` 형식(예: `네리스 (대장간)`).
- **Npc_Talk_Area**: NPC 멘트 칸(`.npc-talk`). NPC 이름(`.npc-name`)과 선택된 대사(`<p>`)를 표시한다.
- **Npc_Action_Button**: Npc_Talk_Area 하단에 가로로 배치되는 타입별 행동 버튼. 1차 구현에서는 클릭 시 "구현 예정입니다" 안내만 표시한다.
- **Npc_Action_Definition**: Npc_Type별 Npc_Action_Button 라벨 정의. `chief`→[`퀘스트`], `blacksmith`→[`상점`, `수리`], `magic-school`→[`상점`], `school`→[`상점`], `healer`→[`상점`, `치료받기`], `bank`→[`아이템 보관`, `골드 입/출금`].
- **Time_Of_Day**: 실제 시(0~23)에서 매핑된 시간대 키(`dawn`/`morning`/`afternoon`/`late-afternoon`/`night`/`late-night`). `docs/npc-dialogue.json`의 `timeOfDay` 경계 정의를 따른다.
- **Dialogue_Candidate_Pool**: 특정 NPC와 현재 Time_Of_Day에 대해 `lines.default`와 `lines.byTime[현재 시간대]`를 병합한 대사 후보 목록.
- **Personality_Fallback_Line**: Dialogue_Candidate_Pool이 비어 있을 때 사용하는 성격 기반 기본 문구.
- **PlayScreen_Controller**: 스펙 001에서 정의된, 플레이 화면을 서버사이드 렌더링하는 시스템. 본 스펙에서 NPC 관련 렌더링 책임이 확장된다.
- **Current_Node**: 캐릭터의 현재 맵 노드. 캐릭터 진행상황의 현재 맵 노드 id로 식별된다.

## Requirements

### Requirement 1: NPC 고정 데이터 로드

**User Story:** 시스템 구성원으로서, NPC를 코드 밖 JSON으로 관리하고 싶다. 그래야 NPC를 추가하거나 대사를 수정할 때 코드 변경 없이 데이터 파일만 수정하면 된다.

#### Acceptance Criteria

1. THE Npc_Service SHALL 모듈 리소스 `classpath:data/npc.json`(원본은 `docs/npc-dialogue.json`에서 모듈로 이관)을 파싱하여 Npc 목록을 제공한다.
2. WHEN Npc_Data_Resource를 로드하면, THE Npc_Service SHALL 각 Npc의 `id`, `name`, `type`, `nodeId`, `personality`, `lines`(`default`, `byTime`)를 읽어 Npc 객체로 구성한다.
3. WHEN Npc_Data_Resource를 로드하면, THE Npc_Service SHALL 티르코네일(`tir-chonaill`)에 5명, 던바튼(`dunbarton`)에 5명, 총 10명의 Npc를 제공한다.
4. WHEN Npc_Service가 각 Npc의 `type` 값을 읽으면, THE Npc_Service SHALL 해당 값을 `chief`, `blacksmith`, `magic-school`, `school`, `healer`, `bank` 중 하나의 Npc_Type으로 분류한다.
5. IF Npc_Data_Resource를 로드/파싱할 수 없거나, 필수 필드(`id`, `name`, `type`, `nodeId`)가 누락된 Npc 항목이 있거나, `id`가 중복되면, THEN THE Npc_Service SHALL 실패 사유를 나타내는 오류를 발생시키고 어떤 Npc 목록(부분 목록 포함)도 제공하지 않는다.
6. THE Npc_Service SHALL Npc 데이터를 데이터베이스에 저장하지 않고 클래스패스 리소스에서만 로드한다.
7. IF 어떤 Npc의 `type` 값이 `chief`/`blacksmith`/`magic-school`/`school`/`healer`/`bank` 중 어느 것도 아니면, THEN THE Npc_Service SHALL 데이터 로드 실패로 처리하고 오류를 발생시킨다.

### Requirement 2: NPC 맵 배치 및 상호작용 버튼 노출

**User Story:** 플레이어로서, 마을 노드에 도착하면 그 마을에 있는 NPC들과 상호작용할 수 있는 버튼을 보고 싶다. 그래야 누구와 대화할 수 있는지 알 수 있다.

#### Acceptance Criteria

1. WHEN 특정 노드 id로 NPC 목록을 요청받으면, THE Npc_Service SHALL 해당 노드의 `nodeId`와 일치하는 Npc만 Npc_Data_Resource 정의 순서대로 반환한다.
2. IF 요청받은 노드 id와 일치하는 Npc가 없거나 알 수 없는 노드 id이면, THEN THE Npc_Service SHALL 오류 없이 빈 목록을 반환한다.
3. WHEN 플레이 화면을 렌더링하면, THE PlayScreen_Controller SHALL Current_Node에 배치된 각 Npc에 대해 Npc_Interaction_Button을 상호작용 버튼 영역(`.interactions`)에 Npc_Service 반환 순서대로 노출한다.
4. WHEN Npc_Interaction_Button을 렌더링하면, THE PlayScreen_Controller SHALL 버튼 라벨을 `{name} ({Npc_Type_Label})` 형식으로 표시한다(예: `네리스 (대장간)`).
5. WHEN Npc_Interaction_Button을 렌더링하면, THE PlayScreen_Controller SHALL 해당 버튼에 NPC 구분 스타일 클래스(`npc`)를 적용한다.
6. IF Current_Node에 배치된 Npc가 하나도 없으면, THEN THE PlayScreen_Controller SHALL 상호작용 버튼 영역에 어떤 Npc_Interaction_Button도 노출하지 않는다.
7. WHEN 플레이어가 다른 노드로 이동하여 Current_Node가 변경되면, THE PlayScreen_Controller SHALL 이전 노드의 Npc_Interaction_Button을 모두 제거하고 이동 후 노드에 배치된 Npc의 버튼만 노출한다.

### Requirement 3: NPC 멘트 선택 및 출력

**User Story:** 플레이어로서, NPC 버튼을 누르면 그 NPC의 성격과 현재 시간대에 어울리는 대사를 보고 싶다. 그래야 게임 세계에 몰입할 수 있다.

#### Acceptance Criteria

1. WHEN 플레이어가 Npc_Interaction_Button을 클릭하면, THE Npc_Dialogue_Service SHALL 실제 현재 시각의 시(0~23)를 반열린 구간 경계(`late-night`[0,5), `dawn`[5,8), `morning`[8,12), `afternoon`[12,16), `late-afternoon`[16,19), `night`[19,24))에 따라 정확히 하나의 Time_Of_Day 키로 매핑한다.
2. WHEN Time_Of_Day가 정해지면, THE Npc_Dialogue_Service SHALL 해당 Npc의 `lines.default` 전체와 `lines.byTime[현재 시간대]` 전체를 순서대로 병합하여 Dialogue_Candidate_Pool을 구성하며, 어떤 항목도 누락·제거하지 않는다.
3. WHERE 해당 Npc의 `lines.byTime`에 현재 Time_Of_Day 키가 없거나 그 값이 빈 목록이면, THE Npc_Dialogue_Service SHALL `lines.default`만으로 Dialogue_Candidate_Pool을 구성한다.
4. WHEN Dialogue_Candidate_Pool에 N개(N≥1)의 후보가 있으면, THE Npc_Dialogue_Service SHALL 각 후보를 균등 확률(1/N)로 무작위 1개 선택하여 반환한다.
5. IF Dialogue_Candidate_Pool이 비어 있으면(`lines.default`와 `lines.byTime[현재 시간대]`가 모두 빔), THEN THE Npc_Dialogue_Service SHALL 비어 있지 않은 단일 문자열인 Personality_Fallback_Line을 반환한다.
6. WHEN 대사가 선택되면, THE PlayScreen_Controller SHALL Npc_Talk_Area의 `.npc-name`에 해당 Npc의 `name`을, `<p>`에 선택된 대사를 표시한다.
7. WHEN 다른 Npc_Interaction_Button이 클릭되면, THE PlayScreen_Controller SHALL Npc_Talk_Area의 이전 이름과 대사를 새로 선택된 Npc의 이름·대사로 교체하여 이전 내용이 남지 않도록 한다.
8. THE Npc_Dialogue_Service SHALL 대사 선택 시 계절(Season) 정보를 사용하지 않는다.

### Requirement 4: NPC 타입별 행동 버튼

**User Story:** 플레이어로서, NPC와 대화할 때 그 NPC 타입에 맞는 행동(상점, 수리 등) 버튼을 보고 싶다. 그래야 앞으로 어떤 기능을 이용할 수 있을지 알 수 있다.

#### Acceptance Criteria

1. WHEN NPC 대사가 Npc_Talk_Area에 출력되면, THE PlayScreen_Controller SHALL 해당 Npc의 Npc_Type에 대응하는 Npc_Action_Definition 라벨 개수와 정확히 동일한 수의 Npc_Action_Button을 Npc_Talk_Area 하단에 가로 한 줄로 배치한다.
2. WHEN Npc_Action_Button들을 렌더링하면, THE PlayScreen_Controller SHALL Npc_Action_Definition에 나열된 순서대로 왼쪽에서 오른쪽으로 하나씩 배치한다.
3. WHEN Npc_Action_Button을 렌더링하면, THE PlayScreen_Controller SHALL Npc_Action_Definition에 정의된 라벨을 사용한다(`chief`→`퀘스트`; `blacksmith`→`상점`, `수리`; `magic-school`→`상점`; `school`→`상점`; `healer`→`상점`, `치료받기`; `bank`→`아이템 보관`, `골드 입/출금`).
4. WHEN 플레이어가 Npc_Action_Button을 클릭하면, THE Myrpg_Web_Module SHALL `alert`로 "구현 예정입니다" 안내만 표시하고 Npc_Talk_Area의 대사와 Npc_Action_Button 목록을 변경하지 않는다.
5. THE Myrpg_Web_Module SHALL 1차 구현 범위에서 Npc_Action_Button 클릭 시 상점/수리/치료/보관/입출금 등 실제 기능을 수행하지 않는다.
6. WHEN 다른 Npc_Interaction_Button이 클릭되어 Npc_Talk_Area의 대사가 갱신되면, THE PlayScreen_Controller SHALL 기존 Npc_Action_Button을 모두 제거한 뒤 새로 선택된 Npc의 Npc_Type 기준 목록으로 교체한다.
7. IF Npc_Talk_Area에 출력된 대사가 없으면, THEN THE PlayScreen_Controller SHALL 어떤 Npc_Action_Button도 표시하지 않는다.

### Requirement 5: 향후 확장을 위한 데이터/코드 분리 제약

**User Story:** 개발자로서, 지금은 NPC 행동 버튼이 안내만 표시하지만, 나중에 상점·수리 등 실제 기능을 붙일 때 기존 NPC 데이터 구조나 렌더링 로직을 크게 바꾸지 않기를 원한다. 그래야 확장 비용이 낮아진다.

#### Acceptance Criteria

1. THE Npc_Service SHALL NPC 데이터(고정)와 캐릭터 진행상황(DB)을 분리하여, 신규 NPC 추가가 `npc.json` 수정만으로 완료되고 DB 스키마·엔티티 변경을 요구하지 않도록 한다.
2. THE Myrpg_Web_Module SHALL Npc_Type과 Npc_Type_Label의 매핑을 정확히 한 곳에서 관리하여, 신규 Npc_Type 추가 시 해당 위치에 매핑 항목 1개 추가만으로 완료되도록 한다.
3. THE Myrpg_Web_Module SHALL Npc_Type과 Npc_Action_Definition의 매핑을 정확히 한 곳에서 관리하여, 신규 Npc_Type의 행동 버튼 추가 시 해당 위치에 정의 1개 추가만으로 완료되도록 한다.
4. WHEN 신규 Npc_Type이 추가되면, THE Npc_Service SHALL 기존 6개 Npc_Type(`chief`/`blacksmith`/`magic-school`/`school`/`healer`/`bank`)의 분류·처리 결과를 변경하지 않는다.
5. IF 어떤 Npc_Type이 Npc_Type_Label 매핑 또는 Npc_Action_Definition 매핑에 존재하지 않으면, THEN THE Myrpg_Web_Module SHALL 오류를 반환하고 해당 화면 상태를 변경하지 않는다.
