# Requirements Document

## Introduction

본 스펙(004)은 `myrpg` 모듈(`com.myapps.web.myrpg`)에 **AP(어빌리티 포인트)와 재능 시스템**을 추가한다. 스펙 003(`character-progression-and-rebirth`)에서 구축한 레벨/경험치/스탯 계산/사망 패널티/환생/정보 팝업 위에서 동작하며, 003이 재능(`TalentType`)을 정의만 하고 환생 시 `MELEE`로 고정하던 것을 **선택 가능한 성장 축**으로 확장한다. 상세 설계 배경은 `docs/talent-system.md`를 근거로 한다.

이번 스펙의 범위는 다음과 같다.

1. **AP(어빌리티 포인트)** — 레벨업/환생 시 지급하고 보유량을 영속 저장하며 정보 팝업에 표시한다. 소모(스킬 랭크업)는 **API 정의만** 두고 실제 소모 로직은 3순위 스킬 시스템으로 이연한다.
2. **재능 선택** — 환생 흐름을 2단계(환생 확인 → 재능 택1)로 확장한다. 첫 캐릭터는 근접전투(`MELEE`)로 고정한다.
3. **재능별 레벨업 보너스(효과 A)** — 재능마다 주 스탯(+2/Lv)과 보조 성장 1개를 레벨업당 추가 부여한다(MELEE→STR·HP, ARCHERY→DEX·Critical, MAGIC→INT·MP).
4. **재능 데미지 보너스(효과 B, 정의만)** — 재능과 일치하는 공격 타입에 데미지 +10% modifier를 **정의만** 두고, 실제 적용은 7순위 전투 시스템으로 이연한다.
5. **재능 데이터** — 재능 목록·효과(라벨/보너스/데미지%/요약)를 `TalentType` enum이 자체 보유한다(`NpcType` 선례). 별도 JSON 파일을 두지 않는다.
6. **바이탈별 최대치 리팩터** — 근접(HP)·마법(MP)이 특정 바이탈만 올리므로 003의 "단일 바이탈 최대치"를 "바이탈별 최대치(HP/MP/Stamina)"로 전환한다.
7. **정보 팝업 확장** — 정보 팝업에 보유 AP와 재능 효과 요약을 추가 표시한다.
8. **영속 모델 변경** — `CharacterProgress`에 `abilityPoints` 컬럼 1개를 추가한다. 스탯·바이탈 최대치는 003과 동일하게 저장하지 않고 레벨·재능에서 계산한다.

핵심 방향은 "**성장은 레벨에서 계산한다**"는 003 원칙의 확장이다. 재능은 이미 영속 저장되고 한 생애 동안 불변이므로, 스탯·바이탈 최대치를 `기본값 + 레벨파생(레벨, 재능)`으로 계산하면 새 스탯 저장 필드 없이 재능별 성장이 구현되고, 환생(레벨 1 복귀) 시 재능 보너스도 자동 초기화된다. AP는 소모 때문에 레벨에서 순수 계산할 수 없으므로 잔량을 직접 저장한다.

전투(7순위)와 스킬(3순위)은 아직 없으므로, 본 스펙은 데미지 보너스와 AP 소모를 **훅/시그니처 정의**로만 두고 실제 적용은 이후 스펙으로 이연한다. AP 지급 검증은 003이 제공한 임시 테스트 버튼([경험치 업], [경험치 다운])과 환생 흐름으로 수행한다.

## Glossary

### 기존(003) 재사용 용어

- **Myrpg_Web_Module**: `com.myapps.web.myrpg` 기본 패키지를 가지는 Spring Boot 4.0 Web 모듈.
- **Character_Progress**: 유일한 영속 엔티티(닉네임, 현재/누적 레벨, 경험치, 재능, 마지막 환생 시각, HP/MP/Stamina 현재값, 현재 노드 id). 본 스펙에서 `abilityPoints`가 추가된다.
- **Character_Service**: 캐릭터 진행상황 로드/생성/턴 저장 애플리케이션 서비스.
- **Progression_Service**: 경험치 획득, 레벨업(연속 포함), 사망 패널티, 환생을 처리하는 애플리케이션 서비스. 본 스펙에서 AP 지급과 재능 선택이 추가된다.
- **Stat_Progression**: 레벨로부터 스탯·바이탈 최대치를 계산하는 순수 정책. 본 스펙에서 재능 인자를 받는 오버로드가 추가된다.
- **Current_Level / Max_Level / Accumulated_Level**: 현재 레벨(1~100) / 최대 레벨(100) / 누적 레벨(모든 생애 도달 레벨의 총합, 신규 1).
- **Level_Stat**: `기본값 + 레벨 파생분`(스킬 랭크업분 제외). 정보 팝업 본체 수치.
- **Skill_Rankup_Bonus**: 스킬 랭크업으로 얻는 스탯 증가분. 현 시점 값은 0(3순위에서 산출).
- **Info_Popup / Info_Popup_Top / Info_Popup_Middle / Info_Popup_Bottom**: 정보 팝업(상/중/하 3구역).
- **Rebirth_Button**: 정보 팝업 하단의 [환생하기] 버튼.
- **Rebirth_Cooldown**: 직전 환생 후 다시 환생 가능해질 때까지의 대기 시간(현실시간 24시간).

### 본 스펙(004) 신규 용어

- **Ability_Points (AP)**: 어빌리티 포인트. 레벨업/환생으로 지급되고 스킬 랭크업으로 소모되는 성장 포인트. 현재 **보유 잔량**을 `Character_Progress`에 영속 저장한다.
- **AP_Grant_Level_Up**: 레벨업 1회당 AP 지급량. 값은 `1`(연속 레벨업 시 상승 레벨 수만큼 합산).
- **AP_Grant_Rebirth**: 환생 1회당 AP 지급량. 값은 `1`(누적레벨 +1과 동기).
- **AP_Invariant**: AP 정합성 불변식. 소모가 발생하기 전까지 `abilityPoints == Accumulated_Level - 1`.
- **Talent_Type**: 재능 종류. 값은 `MELEE`(근접전투), `ARCHERY`(활), `MAGIC`(마법). 본 스펙에서 3종 모두 실사용으로 승격된다.
- **Talent_Label**: Talent_Type의 한글 표시명. `MELEE`→`근접전투`, `ARCHERY`→`활`, `MAGIC`→`마법`.
- **Bonus_Target**: 재능 보너스가 적용되는 대상. `STR`, `DEX`, `INT`, `CRITICAL`(스탯 계열) 또는 `HP`, `MP`, `STAMINA`(바이탈 계열).
- **Bonus_Kind**: Bonus_Target의 분류. 스탯 계열(`STAT`) 또는 바이탈 계열(`VITAL`). 스탯/바이탈 적용 분기의 단일 소스.
- **Talent_Bonus**: `(Bonus_Target, 레벨당 증가치)` 쌍. 재능의 주/보조 성장을 표현한다.
- **Primary_Bonus**: 재능의 주 스탯 보너스. 세 재능 모두 대상은 스탯 계열이며 레벨당 증가치는 `+2`(MELEE→STR, ARCHERY→DEX, MAGIC→INT).
- **Secondary_Bonus**: 재능의 보조 성장 보너스. MELEE→HP `+5/Lv`, MAGIC→MP `+5/Lv`, ARCHERY→Critical `+1/Lv`(0.1% 단위, 즉 +0.1%/Lv).
- **Damage_Bonus_Percent**: 재능과 일치하는 공격 타입에 부여되는 데미지 증가율(퍼센트 정수). 세 재능 모두 `10`(+10%). 본 스펙에서는 값 정의만 하고 실제 적용은 전투(7순위)로 이연한다.
- **Talent_Effect_Summary**: 재능 효과를 한 줄로 요약한 문자열(예: `근접 데미지 +10%, STR +2/Lv, HP +5/Lv`). 정보 팝업에 표시한다.
- **Vital_Max**: HP/MP/Stamina 각각의 최대치를 담는 값(바이탈별 최대치). 003의 단일 최대치(`vitalMaxFor(level)→int`)를 대체한다.
- **Talent_Select_Popup**: 환생 확인 후 재능 3종 중 1개를 선택하는 작은 오버레이 팝업.
- **Default_Talent**: 첫 캐릭터의 재능. `MELEE`(근접전투).
- **Fallback_Talent**: 환생 요청에 재능이 누락/이상값일 때 서버가 적용하는 폴백 재능. `MELEE`.

## Requirements

### Requirement 1: AP 지급 규칙

**User Story:** 플레이어로서, 레벨업하거나 환생할 때마다 어빌리티 포인트를 얻고 싶다. 그래야 나중에 스킬을 강화할 자원이 쌓인다.

#### Acceptance Criteria

1. WHEN 신규 캐릭터가 생성되면, THE Character_Service SHALL Ability_Points를 `0`으로 설정한다.
2. WHEN 레벨업이 발생하면, THE Progression_Service SHALL 상승한 레벨 수(AP_Grant_Level_Up × 획득 레벨 수)만큼 Ability_Points를 증가시킨다.
3. WHEN 한 번의 경험치 획득으로 연속 레벨업이 발생하면, THE Progression_Service SHALL 상승한 모든 레벨에 대해 AP를 합산 지급한다(레벨 1당 +1).
4. WHEN 환생이 수행되면, THE Progression_Service SHALL Ability_Points를 AP_Grant_Rebirth(`1`)만큼 증가시킨다.
5. WHEN 사망 패널티가 적용되면, THE Progression_Service SHALL Ability_Points를 변경하지 않는다.
6. WHERE Current_Level이 Max_Level(100)이면, THE Progression_Service SHALL 추가 레벨업이 발생하지 않으므로 AP를 증가시키지 않는다.
7. WHEN 레벨업 또는 환생으로 Ability_Points가 변경되면, THE Character_Service SHALL 변경분을 Character_Progress에 저장한다.

### Requirement 2: AP 저장 및 소모 API

**User Story:** 개발자로서, AP 잔량을 안전하게 저장하고 향후 스킬 시스템이 소모할 수 있는 진입점을 갖고 싶다.

#### Acceptance Criteria

1. THE Character_Progress SHALL Ability_Points를 정수 컬럼(`ability_points`, not null, 기본값 0)으로 영속 저장한다.
2. THE Character_Progress SHALL Ability_Points 잔량을 직접 저장한다(레벨에서 파생 계산하지 않는다).
3. THE Character_Progress SHALL AP를 지급하는 mutator(예: `increaseAbilityPoints(amount)`)와 AP를 소모하는 mutator(예: `spendAbilityPoints(amount)`)를 제공한다.
4. WHEN AP 소모 요청량이 현재 보유량을 초과하면, THE Character_Progress SHALL 도메인 규칙 위반으로 처리한다(예외 처리 구체화는 3순위 스킬 시스템 스펙에서 다룬다).
5. THE Myrpg_Web_Module SHALL AP 소모의 실제 트리거(스킬 랭크업)를 본 스펙에서 구현하지 않는다(소모 mutator 정의와 검증까지만 다룬다).

### Requirement 3: AP 정합성 불변식

**User Story:** 개발자로서, AP 지급이 누적레벨과 정확히 동기화되어 데이터가 어긋나지 않기를 원한다.

#### Acceptance Criteria

1. WHERE AP 소모가 한 번도 발생하지 않았으면, THE Myrpg_Web_Module SHALL `abilityPoints == Accumulated_Level - 1`(AP_Invariant)을 항상 만족한다.
2. WHEN 임의의 레벨업 시퀀스가 처리되면, THE Progression_Service SHALL AP 증가량이 Accumulated_Level 증가량과 같도록 유지한다.
3. WHEN 환생이 수행되면, THE Progression_Service SHALL AP를 +1, Accumulated_Level을 +1 증가시켜 AP_Invariant를 보존한다.
4. WHERE AP 소모가 도입되면(3순위), THE Myrpg_Web_Module SHALL 불변식을 `abilityPoints == (Accumulated_Level - 1) - 누적 소모량`으로 확장한다(본 스펙에서는 소모량 0).

### Requirement 4: AP 표시

**User Story:** 플레이어로서, 현재 보유한 AP를 정보 팝업에서 바로 확인하고 싶다.

#### Acceptance Criteria

1. THE Info_Popup_Top SHALL 현재 재능 표시 바로 아래에 보유 AP(Ability_Points) 행을 표시한다.
2. WHEN 레벨업 또는 환생으로 Ability_Points가 변경되면, THE Info_Popup SHALL 갱신된 보유 AP를 즉시 반영한다(기존 progress-response 스왑 경로 재사용).
3. THE Info_Popup SHALL 보유 AP를 정수로 표시한다.

### Requirement 5: 재능 선택 흐름

**User Story:** 플레이어로서, 환생할 때 세 가지 재능 중 하나를 직접 선택해 다음 생애의 성장 방향을 정하고 싶다.

#### Acceptance Criteria

1. WHEN 신규 캐릭터가 생성되면, THE Character_Service SHALL 재능을 Default_Talent(`MELEE`)로 설정하고 재능 선택 UI를 제공하지 않는다.
2. WHEN 플레이어가 활성 상태의 Rebirth_Button을 누르면, THE Myrpg_Web_Module SHALL 1단계로 `환생을 진행하시겠습니까?` 확인(confirm) 대화상자를 표시한다.
3. WHEN 플레이어가 1단계 확인에서 확인을 선택하면, THE Myrpg_Web_Module SHALL 2단계로 Talent_Select_Popup(재능 3종 버튼 + 취소)을 표시한다.
4. THE Talent_Select_Popup SHALL 재능 3종(`MELEE`/`ARCHERY`/`MAGIC`)을 각 Talent_Label 버튼으로 노출한다.
5. WHEN 플레이어가 Talent_Select_Popup에서 재능을 선택하면, THE Myrpg_Web_Module SHALL 선택한 재능을 파라미터로 하여 환생을 요청한다(`POST /rebirth?talent=…`).
6. WHEN 플레이어가 Talent_Select_Popup에서 취소를 선택하면, THE Myrpg_Web_Module SHALL 팝업만 닫고 환생을 수행하지 않으며 캐릭터 상태를 변경하지 않는다.
7. WHEN 플레이어가 1단계 확인에서 취소를 선택하면, THE Myrpg_Web_Module SHALL Talent_Select_Popup을 표시하지 않고 캐릭터 상태를 변경하지 않는다.
8. IF 환생 요청의 재능 파라미터가 누락되었거나 유효한 Talent_Type이 아니면, THEN THE Myrpg_Web_Module SHALL Fallback_Talent(`MELEE`)로 처리한다.
9. WHEN 환생 요청이 들어오면, THE Progression_Service SHALL Rebirth_Cooldown(24시간) 조건을 서버에서 재검증하고, 쿨다운이 남아 있으면 재능과 무관하게 환생을 거부하며 상태를 변경하지 않는다.

### Requirement 6: 재능별 주 스탯 성장 (효과 A - 주 스탯)

**User Story:** 플레이어로서, 선택한 재능에 맞는 주 스탯이 레벨업마다 더 빠르게 성장하기를 원한다.

#### Acceptance Criteria

1. THE Talent_Type SHALL 각 재능에 대해 Primary_Bonus를 스탯 계열 대상 + 레벨당 `+2`로 정의한다(`MELEE`→STR, `ARCHERY`→DEX, `MAGIC`→INT).
2. WHEN 특정 레벨의 Level_Stat을 계산하면, THE Stat_Progression SHALL 공통 레벨 성장(재능 무관)에 더해 재능의 Primary_Bonus를 `레벨당 증가치 × (Current_Level - 1)`만큼 가산한다.
3. THE Stat_Progression SHALL 재능의 주 스탯이 아닌 스탯에는 Primary_Bonus를 가산하지 않는다.
4. THE Stat_Progression SHALL 재능 보너스를 저장하지 않고 Current_Level과 재능으로부터 계산한다.
5. WHEN 정보 팝업 중앙 스탯을 계산하면, THE Myrpg_Web_Module SHALL 재능 보너스가 반영된 Level_Stat을 본체 수치로 표시한다(스킬 보너스 괄호는 현 시점 `+0`).

### Requirement 7: 재능별 보조 성장 (효과 A - 보조)

**User Story:** 플레이어로서, 재능마다 서로 다른 보조 성장(맷집/자원/치명타)으로 뚜렷한 개성을 갖기를 원한다.

#### Acceptance Criteria

1. THE Talent_Type SHALL `MELEE`의 Secondary_Bonus를 HP 최대치 `+5/Lv`로 정의한다.
2. THE Talent_Type SHALL `MAGIC`의 Secondary_Bonus를 MP 최대치 `+5/Lv`로 정의한다.
3. THE Talent_Type SHALL `ARCHERY`의 Secondary_Bonus를 Critical `+1/Lv`(0.1% 단위, 즉 +0.1%/Lv)로 정의한다.
4. WHEN 특정 레벨의 Vital_Max를 계산하면, THE Stat_Progression SHALL Secondary_Bonus 대상이 바이탈 계열(HP/MP)이면 해당 바이탈 최대치에만 `레벨당 증가치 × (Current_Level - 1)`을 가산한다.
5. WHEN 특정 레벨의 Level_Stat을 계산하면, THE Stat_Progression SHALL Secondary_Bonus 대상이 스탯 계열(Critical)이면 해당 스탯에만 `레벨당 증가치 × (Current_Level - 1)`을 가산한다.
6. THE Stat_Progression SHALL Secondary_Bonus를 재능의 지정 대상 외 바이탈/스탯에는 가산하지 않는다(예: `ARCHERY`의 세 바이탈은 모두 공통값과 동일하다).

### Requirement 8: 바이탈별 최대치

**User Story:** 개발자로서, 재능이 특정 바이탈(HP 또는 MP)만 올릴 수 있도록 바이탈별 최대치를 분리하고 싶다.

#### Acceptance Criteria

1. THE Stat_Progression SHALL 주어진 레벨과 재능에 대해 HP/MP/Stamina 각각의 최대치(Vital_Max)를 산출하는 계산을 제공한다.
2. THE Stat_Progression SHALL 공통 바이탈 최대치를 `100 + 10 × (Current_Level - 1)`로 계산하고, 재능의 바이탈 계열 보너스를 해당 바이탈에만 가산한다.
3. WHEN 레벨업 또는 환생으로 풀회복이 발생하면, THE Progression_Service SHALL HP/MP/Stamina 현재값을 각각 대응하는 Vital_Max로 설정한다.
4. WHEN 상단바 및 정보 팝업의 HP/MP/Stamina 게이지를 조립하면, THE Myrpg_Web_Module SHALL 각 바이탈의 현재값과 해당 바이탈의 Vital_Max로 게이지를 구성한다.
5. THE Myrpg_Web_Module SHALL 003의 단일 바이탈 최대치(`vitalMaxFor(level)→int`) 기반 조립을 바이탈별 최대치 기반 조립으로 대체한다.

### Requirement 9: 재능 데미지 보너스 (효과 B, 정의만)

**User Story:** 개발자로서, 재능이 전투에서 데미지 우위를 갖도록 데미지 보너스를 미리 정의해 두고 싶다.

#### Acceptance Criteria

1. THE Talent_Type SHALL 각 재능에 대해 Damage_Bonus_Percent를 `10`(+10%)으로 정의한다.
2. THE Talent_Type SHALL Damage_Bonus_Percent를 재능과 일치하는 공격 타입(근접→근접, 활→원거리, 마법→마법)에 대한 보너스로 규정한다.
3. THE Myrpg_Web_Module SHALL Damage_Bonus_Percent를 조회할 수 있는 접근자(예: `talent.damageBonusPercent()`)를 제공한다.
4. THE Myrpg_Web_Module SHALL 데미지 보너스의 실제 전투 적용을 본 스펙에서 구현하지 않는다(값 정의와 접근자까지만 다루며, 적용은 7순위 전투 시스템으로 이연한다).

### Requirement 10: 재능 효과 요약 표시

**User Story:** 플레이어로서, 현재 재능이 어떤 효과를 주는지 정보 팝업에서 한눈에 보고 싶다.

#### Acceptance Criteria

1. THE Talent_Type SHALL 각 재능에 대해 비어 있지 않은 Talent_Effect_Summary 문자열을 보유한다.
2. THE Info_Popup_Top SHALL 현재 재능의 Talent_Effect_Summary를 재능 행과 함께 표시한다.
3. THE Talent_Effect_Summary SHALL 데미지 보너스, 주 스탯 성장, 보조 성장을 요약한다(예: `근접 데미지 +10%, STR +2/Lv, HP +5/Lv`).
4. WHEN 환생으로 재능이 변경되면, THE Info_Popup SHALL 변경된 재능의 Talent_Effect_Summary를 반영한다.

### Requirement 11: 재능 데이터 (TalentType enum)

**User Story:** 개발자로서, 재능 목록과 효과를 한 곳(enum)에서 컴파일 타임 안정성과 함께 관리하고 싶다.

#### Acceptance Criteria

1. THE Talent_Type SHALL 각 상수가 Talent_Label, Primary_Bonus, Secondary_Bonus, Damage_Bonus_Percent, Talent_Effect_Summary를 자체 보유한다(`NpcType` 패턴).
2. THE Talent_Type SHALL 각 상수의 Primary_Bonus와 Secondary_Bonus를 유효한 Bonus_Target과 비음수 레벨당 증가치로 정의한다.
3. THE Talent_Type SHALL 각 상수의 Damage_Bonus_Percent를 0 이상으로 정의한다.
4. THE Myrpg_Web_Module SHALL 재능 목록·효과를 별도 JSON 파일이 아니라 Talent_Type enum이 보유하도록 한다.
5. THE Myrpg_Web_Module SHALL `MELEE`/`ARCHERY`/`MAGIC` 3종을 모두 실사용으로 완비한다(정의 누락 시 컴파일 오류).

### Requirement 12: 환생 재능 반영 및 리셋

**User Story:** 플레이어로서, 환생하면 선택한 재능이 즉시 적용되고 이전 생애의 레벨 성장은 초기화되기를 원한다.

#### Acceptance Criteria

1. WHEN 유효한 재능으로 환생이 수행되면, THE Progression_Service SHALL 캐릭터의 재능을 선택된 Talent_Type으로 설정한다(003의 `MELEE` 고정을 대체).
2. WHEN 환생이 수행되면, THE Progression_Service SHALL Current_Level을 1로, Experience를 0으로 초기화하고 Accumulated_Level을 +1 증가시킨다.
3. WHEN 환생이 수행되면, THE Progression_Service SHALL 새 재능 기준의 Vital_Max로 HP/MP/Stamina를 풀회복한다.
4. WHEN 환생이 수행되어 Current_Level이 1이 되면, THE Myrpg_Web_Module SHALL 재능 보너스분(주 스탯·보조 성장 모두)을 0으로 되돌린다(레벨 파생분을 저장하지 않으므로 자동 초기화).
5. WHEN 환생이 수행되면, THE Progression_Service SHALL Skill_Rankup_Bonus를 유지한다(현 시점 0).
6. WHEN 환생으로 Character_Progress가 변경되면, THE Character_Service SHALL 변경분을 저장한다.

### Requirement 13: 영속 모델 변경 및 데이터 무결성

**User Story:** 개발자로서, AP를 저장하되 기존 003 영속 구조를 최소한으로만 확장하고 싶다.

#### Acceptance Criteria

1. THE Character_Progress SHALL 기존 저장 필드(닉네임, 현재/누적 레벨, 경험치, 재능, 마지막 환생 시각, HP/MP/Stamina 현재값, 현재 노드 id)에 Ability_Points 컬럼 1개만 추가한다.
2. THE Character_Progress SHALL 스탯·바이탈 최대치를 저장하지 않고 레벨·재능에서 계산하는 003 원칙을 유지한다.
3. THE Myrpg_Web_Module SHALL 재능 효과 수치(주/보조 보너스, 데미지%, 요약)를 위한 새 영속 테이블/엔티티를 도입하지 않는다(Talent_Type enum이 보유).
4. WHEN 기존에 저장된 캐릭터를 로드하면, THE Character_Service SHALL 저장→로드 라운드트립에서 Ability_Points와 재능을 포함한 모든 진행상황 필드가 보존되도록 한다.

### Requirement 14: 기존 로컬 세이브 처리

**User Story:** 개발자로서, AP 컬럼 추가와 불변식 도입 후 로컬 환경의 기존 세이브가 불변식을 깨지 않기를 원한다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL 로컬 환경(H2 파일, `ddl-auto: update`)의 기존 세이브를 초기화하기 위해 로컬 DB 파일(`./data/myrpg`)을 삭제 후 새 캐릭터로 시작하는 것을 마이그레이션 방식으로 채택한다.
2. WHERE 프로덕션 환경(`ddl-auto: create`)이면, THE Myrpg_Web_Module SHALL 기동 시 스키마가 재생성되어 별도 마이그레이션 없이 초기화된다.
3. WHEN 새 캐릭터로 시작하면, THE Character_Service SHALL Ability_Points 0, 재능 `MELEE`로 생성하여 AP_Invariant(`0 == 1 - 1`)를 만족한다.
