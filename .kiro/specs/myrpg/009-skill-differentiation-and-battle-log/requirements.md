# Requirements Document

## Introduction

본 스펙(009)은 `myrpg` 모듈(`com.myapps.web.myrpg`)에 **딜 스킬 2축 차별화 시스템**과 **전투 로그 UI 재설계**를 추가한다. 008(`battle-system`)이 구축한 가위바위보 턴제 전투(`BattleResolver`·`BattleService`·`BattleTurnResult`·`BattleController`·`battle-view.html`)와 005(`skill-system`)의 스킬 카탈로그(`DamageSkill`·`SkillCatalogService`·`SkillDamagePolicy`) 위에서 동작한다. 데이터 밸런스 기준은 스티어링 `data-balance-guide.md`(§0 데미지 공식, §4 딜 스킬 2축 설계)를 근거로 하며, 본 스펙은 그 가이드가 확정한 설계를 구현으로 옮긴다.

**해결하려는 문제**: 현재 각 재능(MELEE/ARCHERY/MAGIC)에는 일반(NORMAL) 공격 스킬이 2개씩 존재하지만, 배율(`multiplierByRank`)·자원 소모가 완전히 동일하여 게임적으로 구별되지 않는다(하나는 사실상 사용할 이유가 없음). 방어 스킬이 `경감률↔반격율` 2축으로 차별화된 것처럼, 딜 스킬에도 **배율과 트레이드되는 두 번째 축**을 도입해 각 스킬이 고유한 사용 이유를 갖게 한다.

핵심 방향은 딜 스킬에 두 개의 새 축을 부여하는 것이다:

- **축 A — `hitCount`(연타 수)**: `multiplierByRank`를 **1히트당 배율**로 재정의하고, 한 번 사용 시 `hitCount`번 타격한다. 감산형 공식에서 방어력이 **히트마다 차감**되므로 다단은 고방어 대상에 급격히 약해지고(방어 N회 차감), 크리·편차가 히트별로 독립 발생해 평균화(안정)된다. 단일 히트는 방어를 1번만 빼 관통에 유리하고 편차·크리가 all-or-nothing(버스트)이다.
- **축 B — `critBonus`(크리 특화)**: 스킬 사용 시 크리 확률에 가산되는 값(0.1% 단위). 크리를 얹는 대신 기본 배율을 소폭 낮춰 대가를 치른다. 상한 +100(=+10%p, 매그넘 샷이 상한선).

동시에 다단히트 도입으로 한 턴에 여러 타격이 발생하므로, 전투 활동 로그를 재설계한다:

- **전투 액션 로그를 화면 하단이 아닌 전투 뷰 중앙(HP 바와 스킬 목록 사이)으로 이동**하고, **이번 턴 로그만** 표시한다(누적하지 않음, 매 턴 교체).
- **멀티히트 로그 포맷(하이브리드, C안)**: 헤더(스킬명 + 연타 수) + 히트별 브레이크다운(각 히트 피해·크리 표기) + 총 피해 1줄.
- **전투 종료 결산 로그(경험치·골드·드랍 획득, 사망/부활)는 기존처럼 화면 하단 `ActionLog`에 남겨** 전투 종료 후에도 확인 가능하게 유지한다.

이번 스펙의 범위:

1. **딜 스킬 필드 확장** — `DamageSkill`에 `hitCount`(기본 1)·`critBonus`(기본 0) 추가, `multiplierByRank`를 1히트당 배율로 재정의(하위 호환).
2. **카탈로그 파싱** — `SkillCatalogService`가 두 필드를 optional 파싱(미지정 시 기본값), 값 검증(`hitCount ≥ 1`, `critBonus ∈ [0, 100]`).
3. **skill.json 데이터 확정** — 9개 딜 스킬을 `data-balance-guide.md` §4 확정표대로 갱신.
4. **멀티히트 데미지 산출** — `BattleResolver`가 플레이어 딜 스킬 피해를 `hitCount`번 반복 산출(히트마다 방어 차감·크리·편차 독립)하여 합산하고, 히트별 상세를 노출.
5. **스킬 크리 보너스** — 플레이어 공격 크리 확률 = 캐릭터 크리 + `skill.critBonus`(몬스터는 무관).
6. **전투 로그 재설계** — 멀티히트 하이브리드 포맷(C안), 전투 액션 로그 ↔ 결산 로그 분리.
7. **전투 뷰 레이아웃** — 전투 뷰 중앙에 이번 턴 로그 섹션 삽입(HP 바와 스킬 사이), 매 턴 교체.

방어 스킬(`defense`/`counter_attack`)은 이미 2축(경감률↔반격율)으로 차별화되어 있어 **변경하지 않는다**. 부류 2 확장 축(상태이상·흡혈·조건부 등, `data-balance-guide.md` "향후 확장 축")은 이연한다.

## Glossary

### 기존(008 이하) 재사용 용어

- **Myrpg_Web_Module**: `com.myapps.web.myrpg` 기본 패키지의 Spring Boot 4.0 Web 모듈.
- **Skill / Damage_Skill / Defense_Skill**: `skill.json` 카탈로그 항목. `Skill`은 sealed interface(`DamageSkill`·`DefenseSkill` permits). `DamageSkill`(NORMAL/HEAVY)은 `multiplierByRank` 맵, `DefenseSkill`은 `blockRateByRank`·`counterMultiplierByRank` 맵 보유.
- **Skill_Type**: 가위바위보 3항 enum `NORMAL("일반")`/`HEAVY("강")`/`DEFENSE("방어")`.
- **Skill_Talent**: 스킬 재능 enum `MELEE`/`ARCHERY`/`MAGIC`/`COMMON`(방어 공용).
- **Skill_Rank**: 스킬 랭크 enum 16키(`F`→`MASTER`).
- **Skill_Catalog_Service**: `skill.json`을 tree-sitter가 아닌 Jackson으로 로드·검증하는 서비스. `parseDamageSkill`·`parseDefenseSkill`·`parseRankMap`·16키/단조 검증.
- **Skill_Damage_Policy**: 랭크→% 조회 정책. `multiplier(DamageSkill, rank)`·`blockRate`·`counterMultiplier`.
- **Skill_Data_Exception**: 스킬 카탈로그 파싱/무결성 위반 커스텀 예외.
- **Battle_Resolver**: 가위바위보 상성·감산형 데미지 공식·선후공을 계산하는 도메인 순수 서비스. 난수는 주입 `Random`. `baseDamage`·`affinityCoefficient`·`rollCritical`·`finalDamage`·`resolve(TurnInput):ResolvedTurn`.
- **Turn_Input / Resolved_Turn**: `BattleResolver.resolve`의 입력/출력. `TurnInput`은 양측 타입·공격력·방어·배율·경감/반격율·크리티컬 값, `ResolvedTurn`은 양측 피해·크리티컬/방어/반격 플래그.
- **Damage_Formula**: 감산형 데미지 공식(`data-balance-guide.md` §0). 기본피해 `= max(1, floor(공격력 × 배율% / 100) − 대상.defense)`, 최종피해 `= max(1, round(기본피해 × 상성계수 × (크리 ? 1.5 : 1) × rand(0.90~1.10)))`.
- **Critical_Hit**: 크리티컬. `random.nextInt(1000) < critical`(0.1% 단위) 판정 시 데미지 ×1.5.
- **Battle_Service**: 전투 오케스트레이션 애플리케이션 서비스. `start`·`takeTurn`·`flee`·`resumeIfActive`·`combatSkills`. `resolvePlayerMultiplier`·`resolvePlayerCritical`·`attackPower` 등 보조.
- **Battle_Turn_Result**: 한 턴 결과 record. 플레이어/몬스터 행동·피해, 크리티컬/방어/반격/캐스팅실패/선제 플래그, 종료 여부·`Outcome`, 드랍/경험치, 로그 라인(`logLines`).
- **Battle_Log_Formatter**: 한 턴의 전투 로그 문자열을 생성하는 순수 포매터(`combatLines(BattleLogInput):List<String>`). 상태·외부 의존 없음.
- **Battle_Log_Input**: `Battle_Log_Formatter`의 입력 record(스킬 라벨·타입·양측 피해·크리티컬·선제·캐스팅실패·몬스터명 등).
- **Action_Log**: 세션 스코프 활동 로그(`add(message, type)`, 최대 10건). `ActionLogEntry(timestamp, message, type)`. `action-log.html`이 `<footer>`로 화면 하단에 렌더. 타입 `"combat"` 등.
- **Battle_View**: 전투 화면 뷰 모델 record. 몬스터 이름·레벨·현재/최대 HP, 전투 스킬 버튼, 도망 버튼 상태.
- **Battle_View_Fragment**: 전투 전용 프래그먼트 `battle-view.html`(`battle-view` 전체 + `battle-response`(top-bar+center+action-log 교체) + `battle-skills` 서브프래그먼트). `.center`를 교체하며 미니맵 포함.
- **Battle_Controller**: 전투 엔드포인트 컨트롤러. `POST /battle/start`·`/battle/turn`·`/battle/flee` + `GET /battle/skills`. `buildBattleView`·`populateBattleModel`.
- **Bow_First_Strike**: 활 1턴 선제 사격. 첫 턴 활 장착 시 몬스터 행동 무효 + 유저 스킬 100% 적중.
- **Kill_Reward**: 처치 보상. `rollDrop` → 골드 가산 + 인벤토리 적재 + 경험치 지급. 결산 로그(골드/아이템/경험치)를 남김.

### 본 스펙(009) 신규 용어

- **Hit_Count**: 딜 스킬 1회 사용 시 타격 횟수. `DamageSkill`의 신규 필드(기본 1, `≥ 1`). 몬스터·방어 스킬·반격은 항상 1.
- **Per_Hit_Multiplier**: 1히트당 데미지 배율. `multiplierByRank`의 재정의된 의미. 총 배율 ≈ `Per_Hit_Multiplier × Hit_Count`.
- **Crit_Bonus**: 딜 스킬 사용 시 크리 확률에 가산되는 값(0.1% 단위 정수, 기본 0, 상한 100 = +10%p). `DamageSkill`의 신규 필드. 몬스터·방어 스킬에는 없음.
- **Multi_Hit_Damage**: 다단히트 피해 산출. 플레이어 딜 스킬 피해를 `Hit_Count`번 반복하여 각 히트마다 감산·상성계수·크리(히트별 독립)·편차(히트별 독립)를 적용하고 합산한다. 각 히트 최소 1(총 최소 `Hit_Count`).
- **Hit_Result**: 한 히트의 결과 값(record). 히트 피해(int)·크리티컬 여부(boolean). 멀티히트 로그 브레이크다운의 소스.
- **Effective_Player_Critical**: 플레이어 공격 실효 크리 확률 = `캐릭터 크리(Stats.critical + 장비/스킬 보너스) + skill.critBonus`. 몬스터 크리는 `Monster.critical`로 불변.
- **Turn_Combat_Lines**: 이번 턴의 전투 액션 로그 줄 목록(플레이어 행동·몬스터 행동, 멀티히트 브레이크다운 포함). 전투 뷰 중앙에 렌더되며 매 턴 교체(누적 안 함). 화면 하단 `Action_Log`에는 추가하지 않는다.
- **Battle_Log_Section**: 전투 뷰 중앙의 이번 턴 로그 표시 영역(HP 바와 스킬 목록 사이). `battle-view.html` 내 신규 섹션.
- **Reward_Outcome_Lines**: 전투 종료 결산 로그(골드/아이템/경험치 획득, 사망/부활). 전투 종료 후에도 확인 가능하도록 화면 하단 `Action_Log`에 남긴다.
- **Multi_Hit_Log_Format**: 멀티히트 하이브리드 로그 포맷(C안). 헤더(`{스킬명}({타입}) {N}연타`) + 브레이크다운(`{d1}  {d2}(치명)  … = {합계} 피해`) 구성. 단일 히트(`Hit_Count == 1`)는 기존 한 줄 형식(`{스킬명}({타입})로 {N} 피해`, 크리 시 `(크리티컬!)`).

## Requirements

### Requirement 1: 딜 스킬 필드 확장 (hitCount · critBonus)

**User Story:** 개발자로서, 딜 스킬에 배율 외의 차별화 축을 데이터로 부여할 수 있도록 `hitCount`·`critBonus` 필드를 추가하고 싶다.

#### Acceptance Criteria

1. THE Damage_Skill SHALL `hitCount`(int, 타격 횟수)와 `critBonus`(int, 0.1% 단위 크리 확률 가산) 두 필드를 신규 보유한다.
2. THE `multiplierByRank` SHALL 총 배율이 아닌 **1히트당 배율**(Per_Hit_Multiplier)로 의미가 재정의된다.
3. THE Defense_Skill SHALL `hitCount`·`critBonus`를 갖지 않는다(방어 스킬은 본 축의 대상이 아니다).
4. THE Damage_Skill SHALL 기존 필드(`id`·`label`·`type`·`talent`·`resourceCost`·`multiplierByRank`·`description`)의 접근자·의미를 보존하고 두 필드만 추가하여 하위 호환을 유지한다.
5. THE `hitCount`·`critBonus` SHALL 불변(record 컴포넌트 또는 final 필드)이며 생성 시 확정된다.

### Requirement 2: 스킬 카탈로그 파싱 및 검증

**User Story:** 개발자로서, `skill.json`에서 `hitCount`·`critBonus`를 안전하게 파싱하고 잘못된 값을 로드 시점에 걸러내고 싶다.

#### Acceptance Criteria

1. WHEN 딜 스킬 노드를 파싱하면, THE Skill_Catalog_Service SHALL `hitCount`·`critBonus`를 optional로 읽고, 미지정 시 각각 기본값 `1`·`0`을 사용한다.
2. IF `hitCount`가 존재하고 숫자가 아니거나 `1` 미만이면, THEN THE Skill_Catalog_Service SHALL `Skill_Data_Exception`을 던진다.
3. IF `critBonus`가 존재하고 숫자가 아니거나 `[0, 100]` 범위를 벗어나면, THEN THE Skill_Catalog_Service SHALL `Skill_Data_Exception`을 던진다.
4. THE Skill_Catalog_Service SHALL `multiplierByRank`의 16키(F→MASTER) 완비 및 단조 비감소 검증을 기존과 동일하게 유지한다.
5. THE Defense_Skill 파싱 SHALL `hitCount`·`critBonus`를 읽지 않으며, 방어 스킬 노드에 해당 필드가 있어도 무시한다(하위 호환).
6. THE 기존 스킬 카탈로그 로드/검증 테스트 SHALL 회귀 없이 통과한다(필드 미지정 시 기본값으로 로드).

### Requirement 3: skill.json 데이터 확정

**User Story:** 플레이어로서, 같은 재능의 두 일반 스킬이 각기 뚜렷한 사용 이유(단일 버스트 / 다단 안정 / 크리 특화)를 갖기를 원한다.

#### Acceptance Criteria

1. THE `skill.json` SHALL 9개 딜 스킬을 `data-balance-guide.md` §4 확정표대로 정의한다:
   - MELEE `slash`(NORMAL, hitCount 1, critBonus 0), `windmill`(NORMAL, hitCount 3, critBonus 0), `smash`(HEAVY, hitCount 1, critBonus 80).
   - ARCHERY `aimed_shot`(NORMAL, hitCount 1, critBonus 0), `arrow_revolver`(NORMAL, hitCount 4, critBonus 0), `magnum_shot`(HEAVY, hitCount 1, critBonus 100).
   - MAGIC `mana_bolt`(NORMAL, hitCount 1, critBonus 0), `icebolt`(NORMAL, hitCount 3, critBonus 0), `firebolt`(HEAVY, hitCount 1, critBonus 0).
2. THE 다단 스킬(`windmill`·`icebolt` 3타, `arrow_revolver` 4타) SHALL `multiplierByRank`를 1히트당 배율로 낮게 쪼개어, 총 배율(1히트당 × hitCount)이 단일 NORMAL 밴드(F90→170)보다 약간 위가 되도록 authoring한다(3타: 히트당 F35→65 = 총 105→195, 4타: 히트당 F27→50 = 총 108→200).
3. THE 단일 히트 딜 스킬 SHALL 기존 밴드를 유지한다(NORMAL F90→170, HEAVY 스매시·파이어볼트 F130→250, 매그넘 샷 F140→260).
4. THE 모든 마법 딜 스킬(`mana_bolt`·`icebolt`·`firebolt`) SHALL `critBonus`가 0이다(마법은 깡뎀 지향, 크리 미사용).
5. THE `critBonus`가 0이 아닌 스킬 SHALL `smash`(80)·`magnum_shot`(100) 뿐이며, 어떤 스킬도 `critBonus` 상한 100을 넘지 않는다.
6. THE 모든 딜 스킬의 `multiplierByRank` SHALL 16키 완비 + 단조 비감소를 유지한다.
7. THE 방어 스킬(`defense`·`counter_attack`) SHALL 변경 없이 유지된다.

### Requirement 4: 멀티히트 데미지 산출

**User Story:** 플레이어로서, 다단히트 스킬은 여러 번 나눠 때리므로 안정적이지만 고방어 적에게는 약해지는 트레이드오프를 체감하고 싶다.

#### Acceptance Criteria

1. WHEN 플레이어가 딜 스킬로 피해를 주면, THE Battle_Resolver SHALL 해당 스킬의 `hitCount`만큼 히트를 반복 산출하여 합산한다.
2. THE 각 히트 SHALL 감산형 기본피해(`max(1, floor(공격력 × Per_Hit_Multiplier% / 100) − 대상.defense)`)를 독립적으로 산출하며, 방어력은 히트마다 차감된다.
3. THE 각 히트 SHALL 크리티컬(Effective_Player_Critical 기준)과 ±10% 편차를 **히트별로 독립** 판정·적용한다.
4. THE 멀티히트 총 피해 SHALL 각 히트 피해(최소 1)의 합이며, 따라서 총 피해는 최소 `hitCount`이다.
5. WHERE `hitCount == 1`이면, THE Battle_Resolver SHALL 기존 단일 히트 산출과 동일한 결과를 낸다(하위 호환).
6. THE 몬스터 피해·방어 반격 피해·마법 캐스팅 실패·자원 부족 처리 SHALL `hitCount`와 무관하게 항상 단일 히트로 동작한다(몬스터·반격은 다단이 아니다).
7. WHERE 상성 무승부(50%)·방어 승(경감) 등 상성계수가 1.0이 아닌 경우에도, THE Battle_Resolver SHALL 각 히트에 동일 상성계수를 적용하여 `hitCount`번 산출·합산한다.
8. THE 멀티히트 산출 SHALL 결정적 부분(감산·상성)은 순수하게, 크리티컬·편차만 주입 `Random`으로 계산하여 시드 고정 테스트가 가능해야 한다.

### Requirement 5: 스킬 크리 보너스

**User Story:** 플레이어로서, 크리 특화 스킬(스매시·매그넘 샷)은 다른 스킬보다 크리가 더 잘 터지기를 원한다.

#### Acceptance Criteria

1. THE Effective_Player_Critical SHALL `캐릭터 크리(Stats.critical + 장비/스킬 보너스) + 사용 스킬의 critBonus`로 산출한다.
2. THE 플레이어 딜 스킬의 크리 판정 SHALL Effective_Player_Critical을 크리 확률로 사용한다(멀티히트 시 히트마다 이 확률로 독립 판정).
3. THE 몬스터 크리 확률 SHALL `Monster.critical`로 유지되며 `critBonus`의 영향을 받지 않는다.
4. THE 방어 스킬 반격의 크리 판정 SHALL 캐릭터 크리만 사용하고 `critBonus`를 더하지 않는다(방어 스킬은 `critBonus`가 없다).
5. THE Effective_Player_Critical SHALL 크리 판정 상한(0.1% 단위 1000 = 100%)을 초과하지 않도록 보정한다(초과 시 판정은 항상 크리).

### Requirement 6: 히트별 결과 노출

**User Story:** 개발자로서, 멀티히트 로그를 그리기 위해 턴 결과가 히트별 피해·크리 상세를 담기를 원한다.

#### Acceptance Criteria

1. THE Battle_Resolver 플레이어 피해 산출 SHALL 각 히트의 `Hit_Result`(피해·크리티컬 여부) 목록을 노출한다.
2. THE Battle_Turn_Result SHALL 플레이어 히트별 상세(`Hit_Result` 목록)를 담아 로그 포매터가 소비할 수 있게 한다.
3. WHERE 플레이어 행동이 피해를 주지 않으면(캐스팅 실패·자원 부족·상성 패배 0피해·방어 교착), THE 히트별 상세 SHALL 비어 있거나 피해 0을 반영한다.
4. THE 히트별 상세 SHALL 플레이어 딜 스킬에만 존재하며, 몬스터 피해·반격 피해는 단일 값으로 유지한다.
5. THE Battle_Turn_Result의 기존 `playerDamage` SHALL 히트별 피해의 합계와 일치한다(하위 호환 유지).

### Requirement 7: 멀티히트 로그 포맷 (하이브리드 · C안)

**User Story:** 플레이어로서, 다단히트 공격의 각 타격 피해와 크리 여부를 로그에서 명확히 보고 싶다.

#### Acceptance Criteria

1. WHERE 플레이어 딜 스킬 `hitCount ≥ 2`이고 피해가 발생하면, THE Battle_Log_Formatter SHALL 헤더 줄 `"{스킬명}({타입}) {N}연타"`와 브레이크다운 줄 `"{d1}  {d2}  {d3} = {합계} 피해"`를 생성한다.
2. THE 브레이크다운 SHALL 각 히트 피해를 순서대로 나열하고, 크리티컬 히트에는 `"(치명)"` 표기를 붙인다(예: `"22  33(치명)  19 = 74 피해"`).
3. WHERE 플레이어 딜 스킬 `hitCount == 1`이면, THE Battle_Log_Formatter SHALL 기존 단일 히트 형식 `"{스킬명}({타입})로 {몬스터명}에게 {N} 피해"`(크리 시 `" (크리티컬!)"`)를 유지한다.
4. THE 몬스터 행동 로그 SHALL 기존 형식(`"{몬스터명}의 {타입}공격, {N} 피해를 입음"` 등)을 유지한다.
5. WHEN Bow_First_Strike가 다단 스킬로 발동하면, THE Battle_Log_Formatter SHALL 선제 사격 로그에도 멀티히트 브레이크다운을 반영한다.
6. WHEN 플레이어 공격이 빗나가거나(피해 0) 캐스팅 실패하면, THE Battle_Log_Formatter SHALL 기존 문구(`"공격이 빗나갔다!"`·`"캐스팅 실패!"`)를 유지한다.
7. THE 방어 스킬(반격/관통/교착) 로그 SHALL 기존 형식을 유지한다(방어는 다단 대상이 아니다).

### Requirement 8: 전투 액션 로그 ↔ 결산 로그 분리

**User Story:** 플레이어로서, 매 턴 치고받는 로그는 전투 화면에서만 보고, 전투가 끝나면 경험치·드랍 결산은 하단에 남아 확인되기를 원한다.

#### Acceptance Criteria

1. THE Battle_Service SHALL 전투 액션 로그(플레이어 행동·몬스터 행동·선제·캐스팅 실패)를 `Turn_Combat_Lines`로 턴 결과에 담고, 화면 하단 `Action_Log`에는 추가하지 않는다.
2. THE Battle_Service SHALL 전투 종료 결산 로그(`Reward_Outcome_Lines`: 골드/아이템/경험치 획득, 사망/부활)를 화면 하단 `Action_Log`에 `"combat"` 타입으로 추가한다.
3. WHEN 전투가 시작되면, THE Battle_Service SHALL 전투 시작 안내를 `Turn_Combat_Lines`(중앙, 인트로)로 표시하고 화면 하단 `Action_Log`에는 전투 시작 로그를 남기지 않는다.
4. WHEN 도망이 처리되면, THE Battle_Service SHALL 도망 성공/실패 로그를 액션 로그(중앙)로 표시하되, 도망 실패로 인한 진행은 전투를 계속한다.
5. THE 결산 로그와 액션 로그의 분리 SHALL 기존 `Kill_Reward`의 골드→아이템→경험치 순서와 문구를 보존한다(위치만 하단으로 고정).
6. THE 화면 하단 `Action_Log` SHALL 전투 외 이동/시스템 로그를 기존대로 유지한다(전투 액션 로그만 제외).

### Requirement 9: 전투 뷰 레이아웃 (중앙 로그 섹션)

**User Story:** 플레이어로서, 전투 로그를 화면 하단이 아닌 전투 뷰 중앙(HP 바와 스킬 목록 사이)에서 보고 싶다.

#### Acceptance Criteria

1. THE Battle_View_Fragment SHALL 몬스터 HP 바와 스킬 버튼 영역(`#battleSkills`) 사이에 `Battle_Log_Section`(이번 턴 전투 로그)을 렌더한다.
2. THE 레이아웃 순서 SHALL `몬스터 이름+레벨 → 몬스터 HP 바 → Battle_Log_Section → 스킬 버튼 + 도망 → 미니맵`이다.
3. THE Battle_Log_Section SHALL `Turn_Combat_Lines`만 표시하며, 턴이 진행될 때마다 이전 턴 로그를 대체한다(누적하지 않음).
4. WHEN 전투가 시작되면(`POST /battle/start`), THE Battle_Log_Section SHALL 전투 시작 인트로 줄을 표시한다(턴 로그 없음).
5. THE `battle-view` 프래그먼트와 `battle-response` 인라인 center의 중복된 전투 화면 마크업 SHALL 공용 서브프래그먼트로 추출하여 `Battle_Log_Section` 정의가 한 곳에만 존재하게 한다(DRY).
6. THE 화면 하단 `Action_Log` footer SHALL 유지되며(전투 중에도 노출), 전투 액션 로그가 아닌 결산/이동 로그만 담는다.
7. THE Battle_View SHALL `Turn_Combat_Lines`(및 멀티히트 브레이크다운)를 전투 뷰가 렌더할 수 있도록 뷰 모델/모델 속성으로 전달한다.

### Requirement 10: 턴마다 로그 교체 (누적 방지)

**User Story:** 플레이어로서, 전투 뷰 중앙 로그가 계속 쌓이지 않고 이번 턴 내용만 보이기를 원한다.

#### Acceptance Criteria

1. WHEN `POST /battle/turn`이 응답하면, THE Battle_View_Fragment SHALL 이번 턴의 `Turn_Combat_Lines`만 `Battle_Log_Section`에 렌더하고 이전 턴 로그는 포함하지 않는다.
2. THE `Turn_Combat_Lines` SHALL 세션에 누적 저장되지 않으며 매 턴 결과에서만 생성된다.
3. WHEN 무기 교체 등으로 `#battleSkills`만 재렌더(`GET /battle/skills`)되면, THE Battle_Log_Section SHALL 직전 턴 로그를 그대로 유지하며 과거 턴 로그를 되살리지 않는다.
4. THE 한 턴의 `Turn_Combat_Lines` SHALL 플레이어 행동 블록(멀티히트 시 헤더+브레이크다운) + 몬스터 행동 블록으로 구성되며, 일반 턴 기준 표시 줄 수가 과도하지 않게 유지된다.

### Requirement 11: 하위 호환 및 회귀 방지

**User Story:** 개발자로서, 008 전투 시스템과 005 스킬 시스템의 기존 동작·테스트가 본 스펙 변경으로 깨지지 않기를 원한다.

#### Acceptance Criteria

1. THE 단일 히트 딜 스킬(`hitCount == 1`) SHALL 본 스펙 이전과 동일한 데미지·로그 결과를 낸다(크리 보너스 0인 스킬 기준).
2. THE Battle_Turn_Result의 기존 컴포넌트(`playerDamage`·`playerCritical` 등) SHALL 시그니처·의미를 보존하고, 신규 히트별 상세는 추가 컴포넌트로 확장한다.
3. WHEN `Battle_Resolver`·`Battle_Service`·`Damage_Skill`·`Skill_Catalog_Service` 시그니처가 확장되면, THE 기존 008/005 프로퍼티·단위·통합·슬라이스 테스트 SHALL 회귀 없이 통과하도록 함께 갱신한다.
4. THE 정적 리소스 보존 테스트 SHALL `battle-view.html` 변경(중앙 로그 섹션·공용 서브프래그먼트) 기대값을 반영하여 통과한다.
5. THE Myrpg_Web_Module SHALL 커스텀 예외를 사용하고 `RuntimeException`을 직접 던지지 않으며(code-style), 미사용 import/변수 제거·매직넘버 상수화 등 정리 항목을 준수한다.

### Requirement 12: 밸런스 가이드 일치

**User Story:** 개발자로서, 구현된 공식·수치가 `data-balance-guide.md`의 확정 설계와 정확히 일치하기를 원한다.

#### Acceptance Criteria

1. THE 멀티히트 데미지 공식 SHALL `data-balance-guide.md` §0(히트당 배율을 hitCount번 반복 합산, 방어 히트마다 차감, 크리·편차 히트별 독립)과 일치한다.
2. THE 9개 딜 스킬 데이터 SHALL `data-balance-guide.md` §4 확정표(hitCount·critBonus·배율 밴드)와 일치한다.
3. THE `critBonus` 상한 SHALL +100(=+10%p)이며 매그넘 샷이 상한선이다.
4. THE 모든 마법 딜 스킬 SHALL `critBonus` 0이다.
5. THE 다단 스킬의 히트당 배율 총합 SHALL 가이드가 명시한 밴드(3타 총 105→195, 4타 총 108→200) 안에 있다.
