# Requirements Document

## Introduction

본 스펙(005)은 `myrpg` 모듈(`com.myapps.web.myrpg`)에 **스킬 시스템**을 추가한다. 스펙 004(`talent-and-ability-points`)가 구축한 AP(어빌리티 포인트)·재능(`TalentType`)·스탯 계산(`StatProgression`) 위에서 동작하며, 004가 정의만 하고 이연해 둔 훅(`CharacterProgress.spendAbilityPoints`, 정보 팝업의 `Skill_Rankup_Bonus` 표시 자리(현재 `Stats.ZERO`), `TalentType.damageBonusPercent()`)을 실제로 채운다. 상세 설계 배경과 확정 사항(D1~D9)은 `docs/skill-system.md`를 근거로 한다.

핵심 방향은 004의 "계산형/저장형 구분" 원칙의 확장이다. 스킬의 랭크·사용 횟수·처치 수는 계산으로 복원할 수 없으므로 **신규 엔티티(`CharacterSkill`)에 영속 저장**하고, 스킬 목록·수치(라벨·타입·재능·배율·자원)는 계속 늘어나는 콘텐츠 데이터이므로 **`data/skill.json` 카탈로그**로 분리한다(`npc.json`/`NpcService` 선례). 반면 타입/랭크/재능/정책은 로직 결합이라 **코드(enum/순수 정책)**로 둔다.

이번 스펙의 범위:

1. **스킬 카탈로그** — `skill.json`을 기동 시 로드·검증(`SkillCatalogService`, `NpcService` 선례). 타입/재능 문자열 참조를 enum으로 검증하고 무결성 위반 시 기동 실패.
2. **스킬 분류** — 타입(`SkillType`: NORMAL/HEAVY/DEFENSE), 재능(`SkillTalent`: MELEE/ARCHERY/MAGIC/COMMON), 랭크(`SkillRank`: F~Master 16단계)를 enum으로 정의.
3. **랭크업 진행** — 두 조건(사용 횟수 + 막타 처치)을 모두 충족하고 AP를 소모하면 랭크업. 랭크업 시 카운터 리셋·영구 스탯 보너스 재계산.
4. **AP 소모** — 스킬 랭크업을 004 AP의 유일한 소모처로 연결. 한 스킬 F→Master 총 200 AP. AP 정합성 불변식을 소모분까지 확장.
5. **영구 스탯 보너스** — 랭크업 1회당 재능 주 스탯 +1(대칭). 보유 스킬 합산을 정보 팝업 `Skill_Rankup_Bonus` 자리에 표시.
6. **자원 소모 데이터** — 종류는 재능 파생(MAGIC→MP, 그 외→Stamina), 양은 스킬별 고정(랭크 무관). 데이터·규칙만 제공하고 실제 차감은 전투(7순위)로 이연.
7. **데미지/방어 계약(정의만)** — 랭크별 데미지 배율(딜스킬)·경감/반격(디펜스) 데이터와 재능 매칭 정보를 제공. 실제 데미지 계산·적용은 전투(7순위)로 이연.
8. **영속 모델** — `CharacterSkill`(skillId·rank·usageCount·killCount) 신규 엔티티. 신규 캐릭터는 윈드밀 1개만 F랭크 시드. 환생 시 보유 스킬·랭크·카운트 유지.
9. **스킬 습득 진입점(정의만)** — `learnSkill(skillId)`. 스킬북 판매/구매(아이템 5순위·NPC상점 8순위)는 이연.
10. **스킬 팝업 UI** — 목록 팝업(탭·행·진행바·승급 버튼)과 승급 모달(정보·확인·재세팅). 최종 좌측 패널 통합은 10순위로 이연.
11. **임시 드라이버** — 전투(7순위)가 없어 카운트가 안 쌓이므로, 승급 모달에 `[사용횟수 업]`·`[막타 처치 업]` 임시 버튼(요구치까지 100% 충전)을 둔다. 전투 스펙이 실제 이벤트로 교체·제거.

전투 적용(데미지 계산·자원 차감·재능 +10%·사용/막타 이벤트)과 무기 재능 기반 스킬 필터링은 훅/데이터 정의로만 두고 실제 적용은 이후 스펙(5·7·8순위)으로 이연한다.

## Glossary

### 기존(004 이하) 재사용 용어

- **Myrpg_Web_Module**: `com.myapps.web.myrpg` 기본 패키지의 Spring Boot 4.0 Web 모듈.
- **Character_Progress**: 유일한 캐릭터 진행 엔티티. 본 스펙에서 보유 스킬(`CharacterSkill`)과 `id`로 연관된다.
- **Ability_Points (AP)**: 004에서 레벨업/환생으로 지급되고 잔량이 저장되는 성장 포인트. 본 스펙에서 스킬 랭크업으로 **소모**된다.
- **Accumulated_Level**: 누적 레벨(모든 생애 도달 레벨의 합). AP 정합성 불변식의 기준.
- **AP_Invariant**: 004 불변식. 본 스펙에서 `abilityPoints == (Accumulated_Level - 1) - 누적 소모 AP`로 확장된다.
- **Talent_Type**: 캐릭터의 환생 재능(`MELEE`/`ARCHERY`/`MAGIC`). 성장축이며 데미지 +10% 및 스킬 매칭의 주체.
- **Stat_Progression**: 레벨·재능으로 스탯/바이탈 최대치를 계산하는 순수 정책.
- **Level_Stat / Skill_Rankup_Bonus**: 정보 팝업 스탯의 본체 값 / 스킬 랭크업 누적 보너스. 004에서 후자는 `Stats.ZERO`였고 본 스펙이 채운다.
- **Stats**: STR/DEX/INT/Critical(0.1%단위)/DEF를 담는 표시 VO. 불변 델타 헬퍼(`withStrDelta` 등) 보유.
- **BonusTarget / BonusKind**: 보너스 대상(STR/DEX/INT/CRITICAL/HP/MP/STAMINA)과 분류(STAT/VITAL).
- **Info_Popup**: 정보 팝업(상/중/하). 중앙 스탯이 `본체(+Skill_Rankup_Bonus)`로 렌더링된다.
- **Npc_Service_Pattern**: `NpcService`가 `npc.json`을 기동 시 1회 파싱·검증하고 무결성 위반 시 `NpcDataException`으로 기동 실패시키는 로딩 패턴. 파싱을 `loadFromStream`으로 분리해 테스트가 인메모리 주입 가능.

### 본 스펙(005) 신규 용어

- **Skill_Catalog**: `classpath:data/skill.json`에 정의된 스킬 목록. 기동 시 로드되어 불변 `Skill` 목록으로 보관된다.
- **Skill**: 카탈로그 항목(도메인 record). `id`, `label`, `type`, `talent`, `resourceCost`, 랭크별 수치 맵, `effectSummary` 보유. enum이 아니다.
- **Skill_Id**: 스킬 정체성 키(예: `"windmill"`). `CharacterSkill`이 문자열로 참조하는 단일 소스.
- **Skill_Type**: 스킬 타입 enum. `NORMAL`(일반) / `HEAVY`(강) / `DEFENSE`(방어).
- **Skill_Talent**: 스킬 분류 전용 enum. `MELEE` / `ARCHERY` / `MAGIC` / `COMMON`. `Talent_Type`(성장축)과 분리된다. `COMMON`은 매칭 재능이 없다.
- **Skill_Rank**: 스킬 랭크 enum. `F, E, D, C, B, A, R9, R8, R7, R6, R5, R4, R3, R2, R1, MASTER` (16단계). `label()`은 `"9".."1"`/`"Master"`를 반환하고 `order()`는 0(F)~15(MASTER)이다.
- **Rank_Ladder**: 랭크 사다리 `F → E → D → C → B → A → 9 → 8 → 7 → 6 → 5 → 4 → 3 → 2 → 1 → Master`.
- **Skill_Rank_Policy**: 랭크 전이별 요구치(사용/막타)와 소모 AP를 반환하는 순수 정책(`ExperiencePolicy` 선례).
- **Skill_Damage_Policy**: 스킬·랭크로부터 랭크별 수치(딜=배율%, 디펜스=경감%/반격%)를 맵 조회로 반환하는 순수 정책.
- **Character_Skill**: 캐릭터의 보유 스킬 진행을 저장하는 신규 엔티티. `characterId`, `skillId`, `rank`, `usageCount`, `killCount`.
- **Usage_Count / Kill_Count**: 현재 랭크에서의 스킬 사용 횟수 / 막타 처치 수. 랭크업 시 0으로 리셋.
- **Rank_Up_Requirement**: 현재 랭크에서 다음 랭크로 오르기 위한 (필요 사용 횟수, 필요 막타 처치) 쌍.
- **Rank_Up_AP_Cost**: 현재 랭크에서 다음 랭크로 오르는 데 필요한 AP(후반 급증형, F→Master 총 200).
- **Rankable**: 승급 가능 상태. `Usage_Count ≥ 요구 && Kill_Count ≥ 요구 && Ability_Points ≥ Rank_Up_AP_Cost && rank ≠ MASTER`.
- **Multiplier_By_Rank**: 딜스킬(NORMAL/HEAVY)의 16개 랭크별 데미지 배율(%). 랭크가 오를수록 단조 증가.
- **Block_Rate_By_Rank / Counter_Multiplier_By_Rank**: 디펜스 스킬의 16개 랭크별 피해 경감률(%) / 반격 배율(%).
- **Resource_Cost**: 스킬 사용 시 소모하는 자원량(정수, 랭크 무관 고정). 종류는 `Skill_Talent`에서 파생.
- **Resource_Kind**: 소모 자원 종류. `MAGIC`→MP, `MELEE`/`ARCHERY`/`COMMON`→Stamina.
- **Skill_Rankup_Stat_Bonus**: 스킬 재능별 랭크업 1회당 영구 스탯 증가. `MELEE`→STR +1, `ARCHERY`→DEX +1, `MAGIC`→INT +1, `COMMON`→DEF +1.
- **Skill_List_Popup / Skill_Rankup_Modal**: 보유 스킬 목록 팝업 / 개별 스킬 승급 모달.
- **Progress_Percent**: 목록 행 진행바 값. `(min(Usage_Count/요구, 1) + min(Kill_Count/요구, 1)) / 2 × 100` (동일가중 평균).
- **Temporary_Driver**: 전투(7순위) 부재로 카운트를 채우기 위한 임시 버튼(`[사용횟수 업]`/`[막타 처치 업]`). 누르면 해당 카운트를 다음 랭크 요구치까지 100% 충전한다.
- **Learn_Skill**: 스킬 습득 진입점(`learnSkill(skillId)`). `CharacterSkill`을 F랭크로 추가하며 중복 습득을 방지한다.
- **Default_Seed_Skill**: 신규 캐릭터가 보유한 채 시작하는 유일한 스킬. `windmill`(윈드밀, NORMAL, MELEE) F랭크.
- **Insufficient_Ability_Points_Exception**: AP 부족으로 랭크업이 거부될 때 던지는 정식 비즈니스 예외.
- **Skill_Data_Exception**: 카탈로그 로드/검증 실패 시 던지는 예외(`NpcDataException` 선례).

## Requirements

### Requirement 1: 스킬 카탈로그 로드 및 검증

**User Story:** 개발자로서, 스킬 목록·수치를 재컴파일 없이 관리하고 싶다. 그래야 스킬을 계속 추가·튜닝할 수 있다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL 스킬 카탈로그를 `classpath:data/skill.json`(최상위 JSON 배열)로 관리한다.
2. WHEN 애플리케이션이 기동되면, THE Skill_Catalog SHALL `skill.json`을 1회 파싱하여 불변 `Skill` 목록으로 보관한다(Npc_Service_Pattern).
3. THE Skill_Catalog SHALL 파싱 로직을 스트림 입력(`loadFromStream`)으로 분리하여 인메모리 데이터 주입 테스트가 가능하도록 한다.
4. WHEN `skill.json`의 `type`/`talent` 문자열이 유효한 Skill_Type/Skill_Talent가 아니면, THE Skill_Catalog SHALL Skill_Data_Exception으로 기동을 실패시킨다.
5. WHEN 필수 필드(`id`/`label`/`type`/`talent`/`resourceCost`)가 누락되었거나, `Skill_Id`가 중복되면, THE Skill_Catalog SHALL Skill_Data_Exception으로 기동을 실패시킨다.
6. WHEN 딜스킬(NORMAL/HEAVY)의 `multiplierByRank` 또는 디펜스(DEFENSE)의 `blockRateByRank`·`counterMultiplierByRank`가 16개 Skill_Rank 키를 모두 갖지 않으면, THE Skill_Catalog SHALL Skill_Data_Exception으로 기동을 실패시킨다.
7. THE Skill_Catalog SHALL `Skill_Id`로 스킬을 조회하는 기능(`byId`)과 전체 목록 조회 기능을 제공한다.

### Requirement 2: 스킬 분류 (타입/재능/랭크)

**User Story:** 개발자로서, 스킬의 타입·재능·랭크를 컴파일 타임 안정성과 함께 관리하고 싶다.

#### Acceptance Criteria

1. THE Skill_Type SHALL `NORMAL`, `HEAVY`, `DEFENSE`를 정의한다.
2. THE Skill_Talent SHALL `MELEE`, `ARCHERY`, `MAGIC`, `COMMON`을 정의하고, `MELEE`/`ARCHERY`/`MAGIC`은 대응 Talent_Type을, `COMMON`은 매칭 재능 없음(빈 값)을 반환하는 접근자(`matchingTalent`)를 제공한다.
3. THE Skill_Rank SHALL Rank_Ladder(16단계)를 정의하고, `label()`(`"F"..."9".."1","Master"`), `order()`(0~15), `next()`(MASTER면 빈 값), `isMax()`를 제공한다.
4. THE Myrpg_Web_Module SHALL 스킬 목록·효과 수치는 카탈로그(JSON)에, 타입/재능/랭크는 enum(코드)에 둔다(둘의 역할을 혼용하지 않는다).

### Requirement 3: 재능 매칭 및 데미지 보너스 계약

**User Story:** 개발자로서, 스킬과 캐릭터 재능이 일치할 때의 +10% 규칙을 미리 정의해 전투가 소비할 수 있게 하고 싶다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL 스킬의 Skill_Talent가 캐릭터 Talent_Type과 일치하는지 판정하는 기능을 제공한다.
2. WHERE 스킬 Skill_Talent가 `COMMON`이면, THE Myrpg_Web_Module SHALL 어떤 Talent_Type과도 매칭되지 않는 것으로 처리한다(데미지 보너스 대상 아님).
3. THE Myrpg_Web_Module SHALL 재능 데미지 보너스(+10%)의 실제 데미지 적용을 본 스펙에서 구현하지 않는다(매칭 판정과 `damageBonusPercent()` 조회까지만, 적용은 7순위 전투로 이연).

### Requirement 4: 랭크별 수치 데이터 (배율/방어/반격)

**User Story:** 플레이어로서, 스킬을 랭크업하면 그 스킬이 강해지기를 원한다.

#### Acceptance Criteria

1. THE Skill(딜스킬) SHALL 16개 랭크 각각의 데미지 배율(Multiplier_By_Rank, %)을 보유한다.
2. THE Skill(디펜스) SHALL 16개 랭크 각각의 Block_Rate_By_Rank(경감%)와 Counter_Multiplier_By_Rank(반격%)를 보유한다.
3. THE Skill_Damage_Policy SHALL 스킬·Skill_Rank로부터 해당 랭크의 수치를 **맵 조회**(보간 아님)로 반환한다.
4. THE Multiplier_By_Rank / Block_Rate_By_Rank / Counter_Multiplier_By_Rank SHALL 랭크 순서(F→Master)에 대해 단조 증가(비감소)한다.
5. THE Myrpg_Web_Module SHALL 이 수치를 실제 데미지·방어·반격 계산에 적용하는 것을 본 스펙에서 구현하지 않는다(데이터·조회 정책까지만, 적용은 7순위 전투로 이연).

### Requirement 5: 랭크업 조건

**User Story:** 플레이어로서, 스킬을 많이 쓰고 그 스킬로 몬스터를 처치해야 랭크업할 수 있기를 원한다.

#### Acceptance Criteria

1. THE Skill_Rank_Policy SHALL 각 랭크 전이(현재→다음)에 대해 Rank_Up_Requirement(필요 사용 횟수, 필요 막타 처치)를 반환한다.
2. THE Rank_Up_Requirement SHALL 모든 랭크 전이에 대해 사용/막타 요구치가 양수이며 랭크가 오를수록 단조 증가한다.
3. WHEN Usage_Count와 Kill_Count가 **둘 다** 해당 랭크의 Rank_Up_Requirement 이상이면, THE Myrpg_Web_Module SHALL 그 스킬을 조건 충족으로 판정한다.
4. WHERE 두 조건 중 하나라도 미충족이면, THE Myrpg_Web_Module SHALL 랭크업을 허용하지 않는다.
5. WHERE 스킬 랭크가 MASTER이면, THE Myrpg_Web_Module SHALL 추가 랭크업을 허용하지 않으며 요구치·AP 검사를 하지 않는다.

### Requirement 6: 랭크업 AP 소모 및 정합성

**User Story:** 플레이어로서, 랭크업에 AP를 소모하되 마스터가 오래 걸리는 목표이기를 원한다.

#### Acceptance Criteria

1. THE Skill_Rank_Policy SHALL 각 랭크 전이에 대해 Rank_Up_AP_Cost를 반환하며, F→Master 전 구간 합계는 200 AP이다.
2. THE Rank_Up_AP_Cost SHALL 랭크가 오를수록 증가(비감소)한다(후반 급증형).
3. WHEN 랭크업이 수행되면, THE Myrpg_Web_Module SHALL `Character_Progress.spendAbilityPoints(Rank_Up_AP_Cost)`로 AP를 소모한다.
4. IF Ability_Points가 Rank_Up_AP_Cost 미만이면, THEN THE Myrpg_Web_Module SHALL Insufficient_Ability_Points_Exception으로 랭크업을 거부하고 캐릭터 상태를 변경하지 않는다.
5. THE Myrpg_Web_Module SHALL 004의 임시 가드(`IllegalArgumentException`)를 대체하여 AP 소모 실패를 Insufficient_Ability_Points_Exception 정식 예외로 처리한다.
6. WHERE 스킬 랭크업이 도입되면, THE Myrpg_Web_Module SHALL AP_Invariant를 `abilityPoints == (Accumulated_Level - 1) - 누적 소모 AP`로 만족한다(누적 소모 AP = 각 스킬이 F에서 현재 랭크까지 오는 데 든 AP의 합).

### Requirement 7: 랭크업 트랜잭션

**User Story:** 플레이어로서, 조건과 AP가 충족될 때 랭크업하면 랭크가 오르고 진행도가 초기화되기를 원한다.

#### Acceptance Criteria

1. WHEN 조건 충족(Req 5) + AP 충족(Req 6) 상태에서 랭크업이 수행되면, THE Myrpg_Web_Module SHALL 순서대로 (a) AP 소모, (b) 랭크를 `next()`로 +1, (c) Usage_Count·Kill_Count를 0으로 리셋, (d) Skill_Rankup_Bonus 재계산, (e) 저장을 수행한다.
2. WHEN 랭크업으로 Character_Skill이 변경되면, THE Myrpg_Web_Module SHALL 변경분을 영속 저장한다.
3. IF 랭크업 선행조건(조건 미충족 또는 MASTER 또는 AP 부족)이 위반되면, THEN THE Myrpg_Web_Module SHALL 랭크·카운트·AP를 변경하지 않는다.

### Requirement 8: 랭크업 영구 스탯 보너스

**User Story:** 플레이어로서, 스킬을 랭크업할 때마다 그 스킬 재능에 맞는 스탯이 영구히 오르기를 원한다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL Skill_Rankup_Stat_Bonus를 재능별 주 스탯 +1로 정의한다: `MELEE`→STR, `ARCHERY`→DEX, `MAGIC`→INT, `COMMON`→DEF.
2. THE Skill_Rankup_Stat_Bonus SHALL 스탯 계열(STR/DEX/INT/DEF)만 대상으로 하며 HP/MP/Stamina 최대치는 올리지 않는다.
3. WHEN 캐릭터의 스킬 랭크업 누적 보너스를 계산하면, THE Myrpg_Web_Module SHALL `Σ(보유 스킬, skill.rank.order() × 재능 주 스탯 보너스)`를 Stats로 산출한다(F=0배 … Master=15배).
4. THE Myrpg_Web_Module SHALL Skill_Rankup_Bonus를 저장하지 않고 보유 스킬 랭크로부터 계산한다.
5. WHEN 정보 팝업을 조립하면, THE Info_Popup SHALL 중앙 스탯의 스킬 보너스 자리(004의 `Stats.ZERO`)를 계산된 Skill_Rankup_Bonus로 대체 표시한다.
6. THE Myrpg_Web_Module SHALL HP/MP/Stamina 최대치 계산(`Stat_Progression.vitalMaxFor`)을 변경하지 않는다.

### Requirement 9: 스킬 자원 소모 데이터

**User Story:** 개발자로서, 스킬별 소모 자원을 정의해 전투가 차감할 수 있게 하고 싶다.

#### Acceptance Criteria

1. THE Skill_Talent SHALL Resource_Kind를 파생 제공한다: `MAGIC`→MP, `MELEE`/`ARCHERY`/`COMMON`→Stamina.
2. THE Skill SHALL Resource_Cost를 정수로 보유하며, 랭크에 따라 변하지 않는다.
3. THE Myrpg_Web_Module SHALL Resource_Kind를 `skill.json`에 저장하지 않고 Skill_Talent에서 계산한다.
4. THE Myrpg_Web_Module SHALL 자원의 실제 차감을 본 스펙에서 구현하지 않는다(종류 규칙 + Resource_Cost 데이터까지만, 차감은 7순위 전투로 이연).

### Requirement 10: 영속 모델 및 초기 데이터

**User Story:** 개발자로서, 스킬 진행을 저장하되 기존 영속 구조를 최소 확장하고 싶다.

#### Acceptance Criteria

1. THE Character_Skill SHALL `characterId`, `skillId`(문자열, `skill.json` id 참조), `rank`(Skill_Rank, EnumType.STRING), `usageCount`, `killCount`를 영속 저장한다.
2. THE Myrpg_Web_Module SHALL 스킬 랭크업 영구 보너스와 랭크별 수치를 별도 컬럼으로 저장하지 않는다(랭크·카운트만 저장, 나머지는 계산/카탈로그).
3. WHEN 신규 캐릭터가 생성되면, THE Myrpg_Web_Module SHALL Default_Seed_Skill(`windmill`)만 F랭크·usageCount 0·killCount 0으로 시드한다.
4. WHEN 저장된 Character_Skill을 로드하면, THE Myrpg_Web_Module SHALL `skillId`가 Skill_Catalog에 존재하는지 검증한다.
5. WHEN 저장→로드 라운드트립이 수행되면, THE Myrpg_Web_Module SHALL `skillId`·`rank`·`usageCount`·`killCount`가 모두 보존되도록 한다.
6. WHEN 환생이 수행되면, THE Myrpg_Web_Module SHALL 보유 스킬 목록과 각 스킬의 rank·usageCount·killCount를 변경하지 않는다(유지).

### Requirement 11: 스킬 습득 진입점

**User Story:** 개발자로서, 향후 스킬북(NPC 판매)이 스킬을 습득시킬 수 있는 진입점을 미리 두고 싶다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL Learn_Skill(`learnSkill(skillId)`) 진입점을 제공하며, 호출 시 해당 스킬을 F랭크·카운트 0의 Character_Skill로 추가한다.
2. IF 이미 보유한 스킬을 다시 습득하려 하면, THEN THE Myrpg_Web_Module SHALL 중복 추가하지 않는다.
3. IF 카탈로그에 없는 `skillId`로 습득을 시도하면, THEN THE Myrpg_Web_Module SHALL 습득을 거부한다.
4. THE Myrpg_Web_Module SHALL 스킬북 판매/구매 흐름을 본 스펙에서 구현하지 않는다(진입점 정의까지만, 판매·구매는 아이템 5순위·NPC상점 8순위로 이연).

### Requirement 12: 스킬 목록 팝업 UI

**User Story:** 플레이어로서, 보유 스킬과 각 랭크·승급 가능 여부를 한눈에 보고 싶다.

#### Acceptance Criteria

1. THE Skill_List_Popup SHALL 탭(`전체`/`근접전투`/`활`/`마법`/`공용`)으로 보유 스킬을 재능별로 필터링해 보여준다.
2. THE Skill_List_Popup SHALL 각 스킬 행을 `스킬명 · 랭크 · 진행바 · 승급 버튼`으로 구성한다(아이콘·사용 버튼 없음).
3. THE 진행바 SHALL Progress_Percent(동일가중 평균: `(min(사용/요구,1)+min(막타/요구,1))/2×100`)로 채우며, MASTER면 "MAX"로 표기한다.
4. WHERE 스킬이 Rankable이면, THE 승급 버튼 SHALL 강조색으로, 아니면 회색/비활성으로 표시한다.
5. WHEN 승급 버튼을 누르면, THE Myrpg_Web_Module SHALL Skill_Rankup_Modal을 표시한다.
6. THE `공용` 탭 SHALL Skill_Talent가 `COMMON`인 스킬(디펜스)을 보여준다.

### Requirement 13: 스킬 승급 모달 UI

**User Story:** 플레이어로서, 승급 전에 다음 랭크 효과와 진행도, 필요 AP를 확인하고 승급하고 싶다.

#### Acceptance Criteria

1. THE Skill_Rankup_Modal SHALL 다음 랭크 라벨, 현재→다음 수치(딜=보너스 데미지%, 디펜스=경감%/반격%), Resource_Kind·Resource_Cost, Usage_Count·Kill_Count(현재/요구), Rank_Up_AP_Cost·보유 Ability_Points를 표시한다.
2. WHEN 플레이어가 승급 버튼을 누르면, THE Myrpg_Web_Module SHALL "승급하시겠습니까?" 확인(confirm)을 표시한다.
3. WHEN 플레이어가 확인을 선택하면, THE Myrpg_Web_Module SHALL 랭크업 트랜잭션(Req 7)을 수행한다.
4. WHEN 플레이어가 취소를 선택하면, THE Myrpg_Web_Module SHALL 캐릭터 상태를 변경하지 않는다.
5. WHEN 랭크업이 성공하면, THE Skill_Rankup_Modal SHALL 팝업을 닫지 않고 **새 랭크 기준**(현재 랭크=오른 랭크, 다음 랭크 미리보기·수치·카운트 0/새 요구치·AP)으로 재세팅한다.
6. WHERE 재세팅된 랭크가 MASTER이면, THE Skill_Rankup_Modal SHALL 다음 랭크 미리보기·승급 버튼·임시 드라이버를 숨기고 최고 랭크임을 표시한다.
7. WHERE Rankable이 아니면, THE 승급 버튼 SHALL 비활성으로 표시하고 랭크업을 수행하지 않는다.

### Requirement 14: 임시 드라이버 (검증용)

**User Story:** 개발자로서, 전투가 없어도 스킬 랭크업 흐름을 시연·검증하고 싶다.

#### Acceptance Criteria

1. THE Skill_Rankup_Modal SHALL 임시 버튼 `[사용횟수 업]`·`[막타 처치 업]`(Temporary_Driver)을 제공한다.
2. WHEN `[사용횟수 업]`을 누르면, THE Myrpg_Web_Module SHALL 해당 스킬의 Usage_Count를 현재 랭크의 필요 사용 횟수까지 즉시 설정(100% 충전)한다.
3. WHEN `[막타 처치 업]`을 누르면, THE Myrpg_Web_Module SHALL 해당 스킬의 Kill_Count를 현재 랭크의 필요 막타 처치까지 즉시 설정(100% 충전)한다.
4. THE Temporary_Driver SHALL 전투(7순위)의 실제 사용/막타 이벤트로 교체되며 제거될 임시 장치임을 코드·태스크에 명시한다.

### Requirement 15: 데이터 무결성 및 마이그레이션

**User Story:** 개발자로서, 스킬 테이블 추가가 기존 세이브를 깨지 않기를 원한다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL Character_Skill을 위한 신규 테이블(`character_skill`)만 추가하고 기존 004 스키마를 변경하지 않는다.
2. WHERE 로컬 환경(H2 파일, `ddl-auto: update`)이면, THE Myrpg_Web_Module SHALL 신규 테이블을 자동 생성하고, 필요 시 기존 세이브 파일 삭제로 초기화한다.
3. WHERE 프로덕션 환경(`ddl-auto: create`)이면, THE Myrpg_Web_Module SHALL 기동 시 스키마가 재생성되어 별도 마이그레이션 없이 초기화된다.
4. WHEN 새 캐릭터로 시작하면, THE Myrpg_Web_Module SHALL Default_Seed_Skill 1개(F랭크)만 보유하여 AP_Invariant(소모 0)를 만족한다.
