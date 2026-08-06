# Requirements Document

## Introduction

본 스펙(003)은 `myrpg` 모듈(`com.myapps.web.myrpg`)에 **기본 시스템 행동**을 추가한다. 스펙 001에서 구축한 캐릭터 진행상황 영속화(`CharacterProgress`)·플레이 화면 SSR·턴제 위에서 동작하며, 001은 레벨/스탯/경험치/HP·MP·Stamina의 **데이터 모델만** 마련했고 성장·환생 등 **행동 로직은 전혀 없다**. 본 스펙은 그 행동을 채운다. 상세 설계 배경은 `docs/basic-system.md`를 근거로 한다.

이번 스펙의 범위는 다음과 같다.

1. **레벨 시스템** — 현재레벨(1~100)과 누적레벨(모든 생애 도달 레벨의 총합)을 규정하고, 레벨업 시 실시간으로 갱신한다.
2. **경험치 곡선 및 레벨업** — 다음 레벨 필요 경험치 곡선(`100 × L²`)을 정의하고, 경험치 획득 시 필요치 도달마다 레벨업(연속 레벨업 포함)한다. 최대레벨에서는 경험치를 누적하지 않는다.
3. **레벨업 스탯 성장** — 레벨업마다 정해진 값으로 스탯을 성장시키고, HP/MP/Stamina를 풀 회복한다.
4. **스탯 계산 구조** — 표시 스탯을 `기본값 + 레벨 파생분 + 스킬 랭크업분`으로 계산한다. 레벨 파생분은 저장하지 않고 현재레벨로부터 계산하며, 스킬 랭크업분은 환생 시 유지한다(스킬 랭크업분의 실제 산출은 3순위 스킬 시스템의 범위이며 현 시점 값은 0).
5. **Critical 표현** — 크리티컬을 0.1% 단위 정수로 저장하고 소수 1자리 퍼센트로 표시한다.
6. **사망 패널티** — 사망 시 현재 경험치를 현재 레벨 필요치의 10%(게이지 10%p)만큼 차감한다.
7. **환생** — "직전 환생 후 24시간 경과"라는 유일한 조건으로 환생을 허용하고, 현재레벨/경험치/레벨 파생 스탯을 초기화하되 스킬 스탯·누적레벨은 유지하며, 환생 시 재능은 근접전투로 설정한다.
8. **재능(최소 범위)** — 재능 종류(근접전투/활/마법)를 정의하고, 신규 캐릭터 기본 재능을 근접전투로 부여하며, 환생 시에도 근접전투로 설정한다. **재능 선택 UI(3종 중 택1)와 게임 효과(보너스/랭크업 기여)는 본 스펙 범위 밖**(2순위 `talent-system.md`)이다.
9. **정보 팝업 화면** — 좌측 정보 팝업(정보 탭)을 상/중/하 3구역으로 구성해 캐릭터 정보를 표시하고, 하단에 [환생하기] 버튼과 환생 후 경과시간을 노출한다(24시간 미만이면 버튼 비활성). 장비/인벤토리/스킬 팝업은 **본 스펙 범위 밖**(10순위 `ui-left-popup.md`)이며, 본 스펙은 좌측 팝업 중 **정보 팝업만** 다룬다.

경험치 획득의 주 원천인 전투는 7순위(별도 스펙)이므로, 본 스펙은 경험치 획득/레벨업/사망 패널티를 **도메인 로직**으로 구현하여 전투가 이후 호출할 수 있게 한다. 전투가 없는 현 시점의 검증을 위해, 좌측 사이드바에 경험치 획득·사망 패널티를 즉시 실행하는 **임시 테스트 버튼**([경험치 업], [경험치 다운])을 제공한다(전투 도입 시 대체).

## Glossary

- **Myrpg_Web_Module**: `com.myapps.web.myrpg` 기본 패키지를 가지는 Spring Boot 4.0 Web 모듈.
- **Character_Progress**: 001에서 정의된 유일한 영속 엔티티(닉네임, 현재레벨, 누적레벨, 경험치, 스탯, HP/MP/Stamina, 현재 노드 id). 본 스펙에서 재능·마지막 환생 시각 등이 추가된다.
- **Character_Service**: 001에서 정의된 캐릭터 진행상황 생성/로드/턴 저장 애플리케이션 서비스. 본 스펙에서 성장·환생 책임이 확장된다.
- **Progression_Service**: 경험치 획득, 레벨업(연속 포함), 사망 패널티, 환생을 처리하는 도메인/애플리케이션 로직의 총칭.
- **Current_Level**: 캐릭터의 현재 레벨. 범위 `1 ~ 100`.
- **Max_Level**: 최대 레벨. 값은 `100`.
- **Accumulated_Level**: 모든 생애(환생 포함)에서 도달한 레벨의 총합. 신규 생성 시 `1`. 항상 `현재레벨 + 과거 생애 도달 레벨의 합`과 일치한다.
- **Experience**: 현재 레벨에서 쌓은 경험치(정수). 다음 레벨 진행도.
- **Experience_Policy**: 레벨 `L`에서 `L+1`로 가는 데 필요한 경험치를 산출하는 정책. 곡선은 `requiredForNext(L) = 100 × L²`.
- **Base_Stats**: 신규 캐릭터의 기본 스탯. STR 10, DEX 10, INT 10, Critical 5.0%, DEF 5, HP/MP/Stamina 각 100/100.
- **Level_Derived_Bonus**: 레벨업으로 얻는 스탯 증가분. 저장하지 않고 `현재레벨`로부터 계산한다.
- **Skill_Rankup_Bonus**: 스킬 랭크업으로 얻는 스탯 증가분. 환생 시 유지된다. 실제 산출은 3순위 스킬 시스템의 범위이며 현 시점 값은 0.
- **Level_Stat**: `Base_Stats + Level_Derived_Bonus`(스킬 랭크업분 제외). 정보 팝업에서 각 스탯의 본체 수치로 표시된다(예: `STR 23`).
- **Displayed_Stat**: 캐릭터의 실효 스탯. `Level_Stat + Skill_Rankup_Bonus`. 정보 팝업은 이를 `Level_Stat (+Skill_Rankup_Bonus)` 형식으로 분리 표기한다.
- **Level_Up_Gain**: 레벨업 1회당 스탯 증가치. STR/DEX/INT +3, Critical +0.3%, DEF +1, HP/MP/Stamina 최대치 +10.
- **Vital**: HP/MP/Stamina의 현재값/최대값 쌍(001에서 정의). 최대치는 `100 + 10 × (현재레벨 - 1) + 스킬 랭크업분`.
- **Full_Recovery**: HP/MP/Stamina의 현재값을 각 최대치와 같게 만드는 것.
- **Death_Penalty**: 캐릭터 사망 시 현재 경험치를 `floor(requiredForNext(현재레벨) × 0.10)`만큼 차감(최소 0, 레벨 다운 없음)하는 처리.
- **Rebirth**: 환생. 캐릭터의 현재 진행을 초기화하고 새 생애를 시작하는 처리.
- **Last_Rebirth_At**: 마지막 환생 시각. 값이 없으면(첫 환생) 시간 제한이 없다.
- **Rebirth_Cooldown**: 직전 환생 후 환생이 다시 가능해질 때까지의 대기 시간. 현실시간 24시간.
- **Talent**: 재능. 캐릭터가 보유하는 전투 성향 분류. 본 스펙에서는 재능 값의 저장만 다루며, 신규 생성·환생 모두 근접전투(MELEE)로 설정한다. 재능 선택 UI와 게임 효과는 범위 밖(2순위)이다.
- **Talent_Type**: 재능 종류. 값은 `MELEE`(근접전투), `ARCHERY`(활), `MAGIC`(마법) 셋 중 하나. 본 스펙에서 실제 사용되는 값은 `MELEE`이며, 나머지는 향후 재능 시스템을 위해 정의만 해 둔다.
- **Default_Talent**: 신규 캐릭터 및 환생 후 재능. `MELEE`(근접전투).
- **Talent_Label**: Talent_Type의 한글 표시명. `MELEE`→`근접전투`, `ARCHERY`→`활`, `MAGIC`→`마법`. 정보 팝업의 재능 표시에 사용한다.
- **Info_Popup**: 좌측 정보 팝업(001의 `panel-popup.html` 정보 영역). 본 스펙에서 상/중/하 3구역으로 구성한다.
- **Info_Popup_Top / Info_Popup_Middle / Info_Popup_Bottom**: 정보 팝업의 상단/중앙/하단 구역.
- **Rebirth_Button**: Info_Popup 하단의 [환생하기] 버튼.
- **Rebirth_Elapsed**: 마지막 환생(Last_Rebirth_At) 이후 경과한 현실시간. Last_Rebirth_At이 없으면 "환생 기록 없음"으로 취급한다.
- **Exp_Gauge**: 상단 바의 경험치 게이지(001에서 정의). 채움 비율과 `현재/최대` 수치 오버레이를 가진다.
- **Left_Sidebar**: 001에서 정의된 좌측 사이드바(장비/인벤/스킬/정보 팝업 버튼 영역).
- **Exp_Up_Test_Button**: Left_Sidebar의 정보 버튼 아래에 위치하는 [경험치 업] 임시 테스트 버튼. 누르면 경험치를 Test_Exp_Amount만큼 획득한다.
- **Exp_Down_Test_Button**: Left_Sidebar의 정보 버튼 아래에 위치하는 [경험치 다운] 임시 테스트 버튼. 누르면 사망 패널티 로직을 실행한다.
- **Test_Exp_Amount**: 테스트 버튼의 고정 획득 경험치. 값은 `500`.

## Requirements

### Requirement 1: 레벨 및 누적레벨

**User Story:** 플레이어로서, 내 현재 레벨과 지금까지 쌓아온 누적 레벨을 정확히 보고 싶다. 그래야 성장 정도를 파악할 수 있다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL Current_Level을 최소 1, 최대 Max_Level(100)로 유지한다.
2. WHEN 레벨업이 발생하면, THE Progression_Service SHALL Current_Level을 1 증가시키고 Accumulated_Level을 1 증가시킨다.
3. WHEN 레벨업이 발생하여 Accumulated_Level이 변경되면, THE Character_Service SHALL 해당 턴 종료 시 변경된 Accumulated_Level을 Character_Progress에 저장하고, 정보 표시에 즉시 반영한다(환생 시점에 몰아서 정산하지 않는다).
4. THE Myrpg_Web_Module SHALL Accumulated_Level이 항상 `Current_Level + 과거 생애에서 도달한 레벨의 합`과 일치하도록 유지한다.
5. WHERE Current_Level이 Max_Level(100)이면, THE Progression_Service SHALL 추가 레벨업을 발생시키지 않는다.

### Requirement 2: 경험치 곡선과 레벨업

**User Story:** 플레이어로서, 경험치를 모으면 레벨이 오르기를 원한다. 그래야 캐릭터가 성장한다.

#### Acceptance Criteria

1. THE Experience_Policy SHALL 레벨 `L`(1 이상)에서 다음 레벨까지 필요한 경험치를 `100 × L²`로 산출한다.
2. WHEN 경험치가 획득되어 Experience가 `requiredForNext(Current_Level)` 이상이 되면, THE Progression_Service SHALL 필요치만큼 Experience를 차감하고 레벨업(Requirement 1.2)을 수행한다.
3. WHEN 한 번의 경험치 획득으로 여러 레벨의 필요치를 초과하면, THE Progression_Service SHALL 남는 경험치로 레벨업을 반복하여 Experience가 현재 레벨 필요치 미만이 될 때까지 연속 레벨업을 처리한다.
4. WHERE Current_Level이 Max_Level(100)이면, THE Progression_Service SHALL 이후 경험치를 누적하지 않고 초과분을 폐기한다.
5. WHEN 최대레벨(100)에 도달하면, THE Exp_Gauge SHALL 게이지를 100% 채움으로 렌더링하고 수치 오버레이를 `MAX`로 표시한다.
6. WHERE Current_Level이 Max_Level 미만이면, THE Exp_Gauge SHALL 채움 비율을 `Experience / requiredForNext(Current_Level)`로, 수치 오버레이를 `Experience / requiredForNext(Current_Level)` 형식으로 표시한다.

### Requirement 3: 레벨업 스탯 성장 및 회복

**User Story:** 플레이어로서, 레벨업하면 스탯이 오르고 체력이 회복되기를 원한다. 그래야 성장의 이득을 체감한다.

#### Acceptance Criteria

1. WHEN 레벨업이 발생하면, THE Progression_Service SHALL Displayed_Stat 기준으로 STR +3, DEX +3, INT +3, Critical +0.3%, DEF +1이 반영되도록 처리한다.
2. WHEN 레벨업이 발생하면, THE Progression_Service SHALL HP/MP/Stamina의 최대치를 각각 10 증가시킨다.
3. WHEN 레벨업이 발생하면, THE Progression_Service SHALL HP/MP/Stamina를 Full_Recovery(현재값 = 최대치)한다.
4. WHEN 여러 레벨의 연속 레벨업이 한 번에 처리되면, THE Progression_Service SHALL 상승한 레벨 수만큼 스탯 성장을 누적 반영하고 마지막에 Full_Recovery를 1회 적용한다.

### Requirement 4: 스탯 계산 구조

**User Story:** 개발자로서, 레벨로 인한 스탯과 스킬로 인한 스탯을 분리해 관리하고 싶다. 그래야 환생 시 레벨 스탯만 초기화하고 스킬 스탯은 유지할 수 있다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL Displayed_Stat을 `Base_Stats + Level_Derived_Bonus + Skill_Rankup_Bonus`로 계산한다.
2. THE Myrpg_Web_Module SHALL Level_Derived_Bonus를 저장하지 않고 `Level_Up_Gain × (Current_Level - 1)`로 계산한다.
3. THE Vital SHALL 최대치를 `100 + 10 × (Current_Level - 1) + 해당 Skill_Rankup_Bonus`로 계산한다.
4. THE Myrpg_Web_Module SHALL Skill_Rankup_Bonus를 환생으로 초기화하지 않고 유지한다.
5. WHERE Skill_Rankup_Bonus의 산출 주체(3순위 스킬 시스템)가 아직 없으면, THE Myrpg_Web_Module SHALL Skill_Rankup_Bonus를 0으로 취급한다.

### Requirement 5: Critical 표현

**User Story:** 플레이어로서, 크리티컬 확률을 소수점 한 자리 퍼센트로 정확히 보고 싶다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL Critical을 0.1% 단위 정수로 저장한다(예: 5.0% = 저장값 50).
2. THE Myrpg_Web_Module SHALL 신규 캐릭터의 Critical을 5.0%(저장값 50)로 설정한다.
3. WHEN 레벨업이 발생하면, THE Progression_Service SHALL Critical 저장값을 3(=0.3%)만큼 증가시킨다.
4. WHEN Critical을 표시하면, THE Myrpg_Web_Module SHALL `저장값 ÷ 10`을 소수점 한 자리 퍼센트로 표기한다(예: 저장값 347 → `34.7%`).

### Requirement 6: 사망 패널티

**User Story:** 플레이어로서, 사망하면 경험치 손실 페널티를 받되 레벨이 내려가지는 않기를 원한다.

#### Acceptance Criteria

1. WHEN 캐릭터가 사망하면, THE Progression_Service SHALL Experience에서 `floor(requiredForNext(Current_Level) × 0.10)`을 차감한다.
2. IF 차감 결과가 0 미만이면, THEN THE Progression_Service SHALL Experience를 0으로 고정하고 Current_Level을 낮추지 않는다.
3. WHEN 사망 패널티가 적용되면, THE Progression_Service SHALL Current_Level, Accumulated_Level, 스탯, Skill_Rankup_Bonus, 재능을 변경하지 않는다.
4. WHERE Current_Level이 Max_Level(100)이면, THE Progression_Service SHALL 경험치가 누적되지 않는 상태이므로 사망 패널티로 인한 경험치 변화를 적용하지 않는다.
5. WHEN 사망 패널티가 적용되어 Character_Progress가 변경되면, THE Character_Service SHALL 변경분을 저장한다.

### Requirement 7: 환생 가능 조건

**User Story:** 플레이어로서, 직전 환생 후 하루가 지나면 언제든 환생할 수 있기를 원한다.

#### Acceptance Criteria

1. WHERE Last_Rebirth_At이 없으면(한 번도 환생하지 않음), THE Progression_Service SHALL 환생을 허용한다.
2. WHERE Last_Rebirth_At이 있고 현재 시각이 `Last_Rebirth_At + 24시간` 이상이면, THE Progression_Service SHALL 환생을 허용한다.
3. IF Last_Rebirth_At이 있고 현재 시각이 `Last_Rebirth_At + 24시간` 미만이면, THEN THE Progression_Service SHALL 환생을 거부하고 남은 대기 시간을 안내한다.
4. THE Progression_Service SHALL 환생 가능 여부를 Current_Level과 무관하게 판정한다(레벨 조건 없음).

### Requirement 8: 환생 효과

**User Story:** 플레이어로서, 환생하면 레벨과 경험치가 초기화되지만 누적레벨과 스킬로 얻은 스탯은 유지되기를 원한다.

#### Acceptance Criteria

1. WHEN 환생이 수행되면, THE Progression_Service SHALL Current_Level을 1로, Experience를 0으로 초기화한다.
2. WHEN 환생이 수행되면, THE Progression_Service SHALL Accumulated_Level을 1 증가시킨다(초기화하지 않는다).
3. WHEN 환생이 수행되면, THE Progression_Service SHALL Level_Derived_Bonus가 사라지도록 처리한다(Current_Level=1이 되어 자동 반영).
4. WHEN 환생이 수행되면, THE Progression_Service SHALL Skill_Rankup_Bonus를 유지한다.
5. WHEN 환생이 수행되면, THE Progression_Service SHALL HP/MP/Stamina 최대치를 재계산하고 Full_Recovery한다.
6. WHEN 환생이 수행되면, THE Progression_Service SHALL 캐릭터의 재능을 근접전투(MELEE)로 설정하고 Last_Rebirth_At을 현재 시각으로 갱신한다.
7. WHEN 환생으로 Character_Progress가 변경되면, THE Character_Service SHALL 변경분을 저장한다.

### Requirement 9: 재능 선택 및 저장

**User Story:** 플레이어로서, 신규 캐릭터가 근접전투로 시작하고 환생 후에도 근접전투로 이어지기를 원한다. (재능 선택 기능은 이후 재능 시스템에서 추가된다.)

#### Acceptance Criteria

1. THE Talent_Type SHALL `MELEE`(근접전투), `ARCHERY`(활), `MAGIC`(마법) 세 값을 정의한다.
2. WHEN 신규 캐릭터가 생성되면, THE Character_Service SHALL 재능을 Default_Talent(`MELEE`)로 설정한다.
3. WHEN 환생이 수행되면, THE Progression_Service SHALL 재능을 근접전투(`MELEE`)로 설정한다(선택 없이 고정).
4. THE Myrpg_Web_Module SHALL 재능 선택 UI(3종 중 택1)를 본 스펙에서 제공하지 않는다. 재능 선택 기능은 재능 시스템(2순위, `talent-system.md`)에서 다룬다.
5. THE Myrpg_Web_Module SHALL 재능의 게임 효과(보너스 스탯, 스킬 랭크업 기여 등)를 본 스펙에서 구현하지 않는다(재능 값의 저장만 다룬다).

### Requirement 10: 정보 팝업 화면

**User Story:** 플레이어로서, 좌측 정보 팝업에서 내 캐릭터 정보를 한눈에 보고 하단 버튼으로 환생하고 싶다.

#### Acceptance Criteria

1. THE Info_Popup SHALL 내용을 상단(Info_Popup_Top)/중앙(Info_Popup_Middle)/하단(Info_Popup_Bottom) 3개 구역으로 구성한다.
2. THE Info_Popup_Top SHALL 닉네임, Current_Level, Accumulated_Level, 현재 재능(Talent_Label), 그리고 HP/MP/Stamina를 각각 `현재값 / 최대값` 형식으로 표시한다.
3. THE Info_Popup_Middle SHALL STR, DEX, INT, Critical, DEF 각각을 `Level_Stat (+Skill_Rankup_Bonus)` 형식으로 분리하여 표시한다(예: `STR 23 (+11)`). 본체 수치(예: `23`)는 Level_Stat(기본값 + 레벨 파생분)이며, 괄호 안 수치(예: `+11`)는 Skill_Rankup_Bonus로 본체에 포함되지 않는다.
3-1. WHERE Skill_Rankup_Bonus가 0이면(현 시점 스킬 시스템 부재), THE Info_Popup_Middle SHALL 괄호 안 수치를 `+0`(Critical은 `+0.0%`)으로 표시한다.
3-2. THE Info_Popup_Middle SHALL Critical의 본체와 괄호 값을 모두 소수점 한 자리 퍼센트(Requirement 5.4)로 표기한다(예: `CRIT 34.7% (+0.0%)`).
4. THE Info_Popup_Bottom SHALL Rebirth_Button([환생하기])과 Rebirth_Elapsed(환생 후 경과시간)를 표시한다.
5. WHERE Last_Rebirth_At이 있고 Rebirth_Elapsed가 24시간 미만이면, THE Info_Popup SHALL Rebirth_Button을 비활성(disabled) 상태로 렌더링한다.
6. WHERE Last_Rebirth_At이 없거나 Rebirth_Elapsed가 24시간 이상이면, THE Info_Popup SHALL Rebirth_Button을 활성(enabled) 상태로 렌더링한다.
7. WHERE Last_Rebirth_At이 없으면, THE Info_Popup SHALL Rebirth_Elapsed 자리에 환생 기록이 없음을 나타내는 표시를 한다.
8. WHEN 플레이어가 활성 상태의 Rebirth_Button을 누르면, THE Myrpg_Web_Module SHALL `환생을 진행하시겠습니까?` 확인(confirm) 대화상자를 표시한다.
9. WHEN 플레이어가 확인 대화상자에서 확인을 선택하면, THE Myrpg_Web_Module SHALL 환생(Requirement 8, 재능은 근접전투로 고정)을 수행하고 갱신된 정보 팝업/화면을 반환한다.
10. WHEN 플레이어가 확인 대화상자에서 취소를 선택하면, THE Myrpg_Web_Module SHALL 환생을 수행하지 않고 캐릭터 상태를 변경하지 않는다.
11. IF 비활성이어야 할 조건에서 환생 요청이 들어오면, THEN THE Myrpg_Web_Module SHALL 서버에서 환생 가능 조건(Requirement 7)을 재검증하여 거부하고 캐릭터 상태를 변경하지 않는다.
12. WHEN 환생이 성공하면, THE Info_Popup SHALL Current_Level(1)·Accumulated_Level(+1)·중앙 스탯(초기화된 레벨 파생 반영)·Rebirth_Elapsed·Rebirth_Button(비활성) 상태로 갱신된다.
13. THE Info_Popup SHALL 001의 디자인 토큰/스타일을 재사용하여 기존 화면과 시각적으로 일관되게 렌더링한다.
14. THE Myrpg_Web_Module SHALL 장비/인벤토리/스킬 팝업을 본 스펙에서 구현하지 않고 정보 팝업만 다룬다.

### Requirement 11: 영속 모델 변경 및 데이터 무결성

**User Story:** 개발자로서, 성장·환생·재능을 저장하되 기존 001 영속 구조를 최소한으로만 확장하고 싶다.

#### Acceptance Criteria

1. THE Character_Progress SHALL 재능(Talent_Type)과 Last_Rebirth_At을 영속 저장한다.
2. THE Character_Progress SHALL Critical을 0.1% 단위 정수로 영속 저장한다(Requirement 5.1).
3. THE Character_Progress SHALL 고정 게임 데이터를 저장하지 않고 캐릭터 진행상황만 저장한다(001 원칙 유지).
4. WHEN 기존에 저장된 캐릭터를 로드하면, THE Character_Service SHALL Requirement 8/6/3의 처리 후에도 저장→로드 라운드트립에서 모든 진행상황 필드가 보존되도록 한다.
5. THE Experience_Policy SHALL 001의 선형 placeholder(`level × 100`)를 Requirement 2.1의 곡선(`100 × L²`)으로 대체한다.

### Requirement 12: 개발용 경험치 조작 테스트 버튼

**User Story:** 테스터로서, 전투가 아직 없으므로 경험치 획득과 사망 패널티를 버튼으로 즉시 시험하고 싶다. 그래야 성장·환생·사망 로직을 전투 없이 검증할 수 있다.

#### Acceptance Criteria

1. THE Left_Sidebar SHALL 정보 버튼 바로 아래에 Exp_Up_Test_Button([경험치 업])과 Exp_Down_Test_Button([경험치 다운])을 노출한다.
2. WHEN 플레이어가 Exp_Up_Test_Button을 누르면, THE Progression_Service SHALL 경험치를 Test_Exp_Amount(500) 획득 처리하고(Requirement 2의 레벨업/연속 레벨업/최대레벨 캡 규칙 적용), 갱신된 화면(상단 Exp_Gauge, 정보 팝업 등)을 반환한다.
3. THE Exp_Up_Test_Button SHALL 1회 클릭당 획득 경험치를 항상 Test_Exp_Amount(500)로 고정한다.
4. WHEN 플레이어가 Exp_Down_Test_Button을 누르면, THE Progression_Service SHALL 사망 패널티 로직(Requirement 6)을 실행하고 갱신된 화면을 반환한다.
5. WHEN 테스트 버튼으로 Character_Progress가 변경되면, THE Character_Service SHALL 변경분을 저장한다.
6. THE Myrpg_Web_Module SHALL Exp_Up_Test_Button과 Exp_Down_Test_Button을 전투(7순위) 도입 시 대체될 임시 테스트 수단으로 취급한다(정식 게임 기능이 아니다).
