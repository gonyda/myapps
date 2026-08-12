# Requirements Document

## Introduction

본 스펙(008)은 `myrpg` 모듈(`com.myapps.web.myrpg`)에 **가위바위보(일반/강/방어) 기반 턴제 전투 시스템**을 추가한다. 스펙 001~007이 구축한 맵/이동, NPC/몬스터 조우 파이프라인(`MonsterService`·`MonsterAiService`·`MonsterRewardService`·`MonsterEncounterService`), 스킬 카탈로그(`SkillDamagePolicy`·`SkillService`), 캐릭터/스탯(`CharacterProgress`·`Stats`·`StatProgression`), 골드/아이템/인벤토리(`InventoryService`·`OwnedItem`), 활동 로그(`ActionLog`) 위에서 동작한다. 상세 설계 배경·확정 사항은 `docs/battle-system.md`를 근거로 한다(개발 완료 후 삭제 예정, 데이터 밸런싱 기준은 스티어링 `data-balance-guide.md`가 영구 보관).

핵심 방향은 007이 열어둔 전투 seam(접점)들을 실제 전투 루프로 연결하는 것이다. 007은 몬스터 카탈로그·AI·드랍·선공 판정을 "정의·정책·신호"까지만 만들었고(`nextAction`/`rollDrop`/`rollPreemptiveStrike`), 전투 턴·데미지·상태·보상 지급·사망은 6순위로 이연했다. 본 스펙이 그 이연분을 구현한다.

- **전투 규칙(가위바위보 상성·데미지 공식·선후공·크리티컬·편차)** 은 순수·결정적 로직이므로 **도메인 순수 서비스(`BattleResolver`)** 로 둔다(테스트 용이, 난수는 주입 `Random`으로 분리).
- **전투 진행 상태(몬스터 현재 HP·턴 수·기습 여부·진행 플래그)** 는 브라우저 종료 후 재개를 위해 **DB에 영속**한다(`BattleState` @Entity, 캐릭터 1:1). 세션 보관은 재개 불가라 채택하지 않는다.
- **전투 오케스트레이션(턴 처리·자원 소모·재능 특성·보상·사망·저장)** 은 **애플리케이션 서비스(`BattleService`)** 가 기존 서비스들을 조립해 수행한다.
- **전투 화면** 은 **전용 프래그먼트(`battle-view.html`)** 로 `.center`를 교체하며, 포션·장비 교체는 인벤토리 탭에서 **턴 소모 없이 실시간 반영**된다.

이번 스펙의 범위(`docs/battle-system.md` §"이번 범위"):

1. **전투 상태 머신** — 전투 시작(`POST /battle/start`) → 턴 진행(`POST /battle/turn`) → 승리/패배/도망 종료.
2. **9칸 데미지 매트릭스** — 세 행동(일반/강/방어) 상성 판정 + 감산형 데미지 공식.
3. **재능 특성** — 근접(STR)·활(DEX + 1턴 선제 사격)·마법(INT + 10% 캐스팅 실패). 스킬 정의 자원(스태미나/MP) 소모.
4. **선후공** — 동일 타입 50:50, 선공 처치 시 후공 무효, 일반↔방어 결정론.
5. **기습(5%)** — 필드 진입 시 강제 전투 돌입(선후공과 별개).
6. **HP 감소·사망·부활** — `CharacterProgress.damageHp` 신설, HP 0 판정, 경험치 −10% + 풀 회복 + 티르코네일 리스폰.
7. **도망**(성공 50%), **전투 스킬 목록**(착용 무기 재능 스킬 + 공통 스킬, 실시간 갱신).
8. **처치 보상 지급** — `MonsterRewardService.rollDrop` → 골드 가산 + 인벤토리 획득 API(신설) + 경험치 지급.
9. **부수 효과** — 스킬 카운트 훅(`onSkillUsed`/`onSkillKill`) 호출, 공격 턴당 장착 장비 `reduceDurability(0.2)`, 내구도 0 자동 장착 해제.
10. **전투 상태 영속·재개** — 매 턴 `saveTurn` + `BattleState` 저장, `GET /` 재접속 시 전투 복원.
11. **전투 UI** — 전용 프래그먼트, 몬스터 HP 바, 전투 중 이동 차단 alert, 포션·장비 실시간 반영, 활동 로그 2줄.
12. **정리** — 몬스터 방어 상수(`defenseBlockRate`/`defenseCounterRate`) 옵션 필드, 임시 개발용 버튼(골드/경험치/승급 드라이버) 제거.

던전 내부 전투(10순위), 보스 실데이터·인챈트 드랍(인챈트 스펙 확정 후), 내구도 수리(대장간, 7순위)는 이연한다.

## Glossary

### 기존(007 이하) 재사용 용어

- **Myrpg_Web_Module**: `com.myapps.web.myrpg` 기본 패키지의 Spring Boot 4.0 Web 모듈.
- **Character_Progress**: 유일한 캐릭터 진행 엔티티. `hpCurrent`/`mpCurrent`/`staminaCurrent`, `fullRecover`, `gainGold`, `currentNodeId` 등을 보유. HP 감소 메서드는 본 스펙 신설 대상.
- **Stats**: STR/DEX/INT/Critical(0.1% 단위 정수)/DEF를 담는 표시 VO. `StatProgression`이 레벨별 산출, `InventoryService.equippedBonus`가 장비 보너스 제공.
- **Stat_Progression**: 레벨·재능 기반 기본 스탯 산출기(`levelStatsFor`).
- **Skill_Type**: 가위바위보 3항 enum. `NORMAL("일반")`/`HEAVY("강")`/`DEFENSE("방어")` + `fromString`.
- **Skill / Skill_Catalog**: `skill.json` 카탈로그 항목. `id`/`label`/`type`(NORMAL/HEAVY/DEFENSE)/`talent`(MELEE/ARCHERY/MAGIC/COMMON)/`resourceCost` + 랭크 맵(`multiplierByRank`/`blockRateByRank`/`counterMultiplierByRank`).
- **Skill_Damage_Policy**: 랭크→% 조회 정책. `multiplier(skill, rank)`·`blockRate(skill, rank)`·`counterMultiplier(skill, rank)`.
- **Skill_Service_Hooks**: `SkillService.onSkillUsed(characterId, skillId)`(매 사용)·`onSkillKill(characterId, skillId)`(막타). 스킬 숙련/킬 카운트를 올리는 연결 지점.
- **Monster**: 몬스터 카탈로그 항목(불변 record). `id`/`name`/`type`/`level`/`maxHp`/`attackPower`/`defense`/`critical`/`experience`/`goldDrop`/`itemDrops`/`lines`. 전투 중 현재 HP는 미보유(본 스펙에서 `BattleState`가 관리).
- **Monster_Ai_Service**: 가위바위보 행동 선택 서비스. `nextAction():SkillType`(34/33/33). 본 스펙이 결과를 소비.
- **Monster_Reward_Service**: 드랍 계산 서비스. `rollDrop(Monster):DropResult`(골드 + 아이템). 본 스펙이 지급을 배선.
- **Monster_Encounter_Service**: 필드 진입 판정 서비스. `rollPreemptiveStrike(List<Monster>):Optional<Monster>`(5% 고정). 실제 의미는 "강제 전투 돌입(기습)"이며 본 스펙이 자동 전투 진입으로 배선.
- **Drop_Result / Dropped_Item**: 드랍 계산 결과 record `(long gold, List<DroppedItem> items)` / `(String itemId, int quantity)`.
- **Inventory_Service**: 인벤토리 서비스. `usePotion(ownedItemId)`·`equip(ownedItemId)`·`unequip(ownedItemId)`·`equippedBonus`·용량 30 검사(이동 시). 본 스펙이 드랍 적재(`acquire`)·내구도 0 자동 해제·전투 스킬 목록 조회를 추가.
- **Owned_Item**: 보유 아이템 엔티티. `reduceDurability(double)`·`repairToMax`·`maxDurability`(초보자 20). 공격 턴당 0.2 감소 호출부는 본 스펙 신설.
- **Progression_Service**: 진행 서비스. `applyDeathPenalty → DeathResult(experienceLost)`(경험치 −10%)·`gainExperience`(레벨업 연쇄). 본 스펙이 사망 처리를 확장.
- **Character_Service**: `saveTurn(progress)`(이동/경험치/환생에서 호출). 본 스펙이 전투 턴 저장에 재사용.
- **Action_Log**: 세션 스코프 활동 로그(`add(message, type)`, 최대 10건). `ActionLogEntry(timestamp, message, type)`, `action-log.html`이 `log-<type>` CSS로 렌더. 타입 `"combat"`(연빨강)·`"reward"`·`"item"` 등 존재.
- **Play_Screen_Controller**: `GET /`·`POST /move`·`POST /monster/encounter` 등 화면 조립 컨트롤러. 임시 골드(`/gold/*`)·경험치(`/exp/*`) 버튼 보유(본 스펙에서 제거).
- **Play_Screen_View / Play_Screen_View_Helper**: 플레이 화면 뷰 record와 조립기. `buildViewFromProgress`·게이지 조립.
- **Center_Fragment / Monster_Response_Fragment**: `center.html`(상황멘트·대사·상호작용·미니맵)과 이를 교체 렌더하는 `monster-response.html`. `myrpg.js`가 `.center`를 스왑.
- **Top_Bar_View / Top_Bar_Fragment**: 상단 바(`top-bar.html`)의 닉네임·레벨·EXP + `.stat-bars`(HP/MP/스태미나 게이지). 플레이어 게이지는 사이드바가 아니라 여기에 있다.
- **Move_Pad_Fragment**: 우하단 이동 패드(`move-pad.html`), `.phone` 기준 absolute 오버레이. `move(dx, dy)`가 `POST /move` 호출.
- **Random_Bean**: `ApplicationServiceConfiguration`이 제공하는 `Random` 빈. 크리티컬·편차·선후공·도망·캐스팅 실패의 난수원(테스트에서 시드 고정).
- **Skill_Controller**: 스킬 목록/승급 컨트롤러. 임시 드라이버 `POST /{id}/dev/fill-usage`·`/{id}/dev/fill-kill` 보유(본 스펙에서 제거).
- **Tir_Chonaill_Node**: 리스폰 노드. `map.json`의 `tir-chonaill`.

### 본 스펙(008) 신규 용어

- **Battle_State**: 진행 중 전투 상태 엔티티(JPA @Entity, 캐릭터 1:1). `monsterId`·`monsterCurrentHp`·`turnCount`·`ambush`·`active`. 매 턴 DB 영속되어 재접속 시 재개를 지원한다.
- **Battle_State_Repository**: `Battle_State`의 Spring Data JPA 리포지토리(캐릭터 기준 단일 활성 전투).
- **Battle_Service**: 전투 오케스트레이션 애플리케이션 서비스. `start`·`takeTurn`·`flee`·`resumeIfActive`·`combatSkills`를 제공.
- **Battle_Resolver**: 가위바위보 상성·데미지 공식·선후공을 계산하는 도메인 순수 서비스. 난수는 주입 `Random`으로 분리(시드 고정 테스트 가능).
- **Rock_Paper_Scissors**: 두 `Skill_Type`의 상성 승패를 판정하는 순수 함수/enum 헬퍼(일반>강, 강>방어, 방어>일반).
- **Affinity_Result**: 상성 판정 결과(WIN/LOSE/DRAW). 9칸 매트릭스의 각 셀을 표현.
- **Affinity_Coefficient**: 상성계수. 승 1.0 / 무승부 0.5 / 방어당함 `(1 − blockRate)` / 관통패 0.0.
- **Damage_Formula**: 감산형 데미지 계산. `기본피해 = max(1, floor(공격력 × 스킬배율%/100) − 대상.defense)`, `최종피해 = max(1, round(기본피해 × 상성계수 × 크리티컬배율 × 편차))`.
- **Attack_Power**: 공격력 `= round(주스탯 × 재능계수)`. 주스탯은 착용 무기 재능별(근접 STR / 활 DEX / 마법 INT, 장비·스킬 보너스 포함). 재능계수 근접 1.0 / 활 0.85 / 마법 1.2(튜닝값).
- **Critical_Hit**: 크리티컬. `random.nextInt(1000) < critical`(0.1% 단위) 판정 시 데미지 ×1.5. 공격·반격·무승부 모든 결과에 적용.
- **Damage_Variance**: 데미지 편차. 상성·크리티컬까지 끝난 최종 데미지에 마지막으로 ±10% 균등 랜덤(`× rand(0.90~1.10)`)을 곱함. 최소 1 유지.
- **Turn_Order**: 선후공. 양쪽 모두 피해가 있는 턴에서만 의미. 동일 타입 무승부 50:50, 일반↔방어(방어 승)는 결정론(공격자 경감피해 → 방어자 반격), 선공 처치 시 후공 무효.
- **Bow_First_Strike**: 활 1턴 선제 사격. 전투 첫 턴(`turnCount == 1`)에 활이 장착돼 있으면 몬스터 1턴 행동 무효 + 유저 스킬 상성 무시 100% 적중. 2턴차부터 일반 규칙.
- **Magic_Cast_Failure**: 마법 캐스팅 실패. 공격 마법(일반/강) 사용 시 10% 확률로 실패(행동 무효 + 턴 소모 + MP 소모). 방어(공통)는 실패 없음.
- **Resource_Cost**: 스킬 자원 소모. 근접/활 = 스태미나, 마법 = MP(`Skill.resourceCost`). 자원 부족 시 턴 미진행. 마법은 캐스팅 실패해도 MP 소모.
- **Battle_Turn_Result**: 한 턴 결과 record. 플레이어 행동/피해, 몬스터 행동/피해, 크리티컬·방어·반격·캐스팅실패·선제 플래그, 전투 종료 여부·승패, 드랍/경험치(종료 시), 활동 로그 라인.
- **Death_Respawn**: 사망 처리. 경험치 −10%(`applyDeathPenalty`) + HP/MP/스태미나 풀 회복(`fullRecover`) + 티르코네일 강제 이동. 골드·아이템 불변.
- **Flee**: 도망. 성공 50%면 전투 종료(노드 유지), 실패면 턴 소모 + 몬스터 일방 1대 피격(전투 계속).
- **Kill_Reward**: 처치 보상. `rollDrop` 결과를 골드 가산 + 인벤토리 적재 + 경험치 지급.
- **Inventory_Acquire**: 드랍 적재 API(`InventoryService.acquire(progress, DropResult)`). 골드는 항상 획득, 아이템은 용량 30 초과 시 소실(로그).
- **Durability_Break**: 내구도 0 도달 시 자동 장착 해제(unequip). 보너스 소멸, 7순위 대장간 수리 후 재장착.
- **Ambush**: 기습(강제 전투 돌입). 필드 진입 시 5% 확률로 자동 `battle/start`. 선후공과 별개 개념.
- **Combat_Skill_List**: 전투 스킬 목록. 착용 무기 재능 스킬 + 공통 스킬(방어)만 노출. 무기 교체 시 실시간 갱신.
- **Battle_View**: 전투 화면 뷰 모델(record). 몬스터 이름·레벨·현재/최대 HP, 전투 스킬 버튼, 도망 버튼 상태.
- **Battle_View_Fragment / Battle_Skills_Fragment**: 전투 전용 프래그먼트 `battle-view.html`(`battle-view` 전체 + `battle-skills` 서브프래그먼트). `.center`를 교체하며 미니맵을 포함.
- **Battle_Controller**: 전투 엔드포인트 컨트롤러. `POST /battle/start`·`/battle/turn`·`/battle/flee` + `GET /battle/skills`.
- **Battle_Active_Flag**: 클라이언트 전투 플래그(`battleActive`). 전투 시작 시 true, 승리/패배/도망 성공 시 false. 전투 중 이동 차단·재개 판정에 사용.
- **Monster_Defense_Constant**: 몬스터 방어 상수. `defenseBlockRate`(경감률, 기본 40)·`defenseCounterRate`(반격율, 기본 30). `monster.json` per-monster 오버라이드(미지정 시 전역 기본). 보스 권장 60/50.
- **Battle_Data_Exception**: 전투 진행 무결성 위반(예: 저장된 `monsterId`가 카탈로그에서 소실) 시 안전 종료를 위한 처리 지점. `RuntimeException` 직접 사용 금지.

## Requirements

### Requirement 1: 전투 상태 영속 및 재개

**User Story:** 플레이어로서, 전투 중 브라우저를 닫았다 다시 들어와도 같은 몬스터·같은 HP로 전투를 이어가고 싶다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL 진행 중 전투 상태를 `Battle_State`(JPA @Entity, 캐릭터 1:1) — `monsterId`·`monsterCurrentHp`·`turnCount`·`ambush`·`active` — 로 DB에 영속한다.
2. THE Battle_State_Repository SHALL 캐릭터당 최대 1개의 활성(`active=true`) 전투만 존재하도록 조회·저장을 제공한다.
3. WHEN 전투가 시작되면, THE Battle_Service SHALL `Battle_State`를 `monsterCurrentHp = monster.maxHp`·`turnCount = 1`·`active = true`로 생성하여 저장한다.
4. WHEN 공격 턴(`takeTurn`)·도망 실패가 종료되면, THE Battle_Service SHALL `Character_Service.saveTurn`과 함께 `Battle_State`(몬스터 현재 HP·`turnCount`)를 DB에 저장한다.
5. WHEN 전투가 승리/패배/도망 성공으로 종료되면, THE Battle_Service SHALL 해당 `Battle_State`를 `active=false`로 전환(또는 삭제)한다.
6. WHEN `GET /`가 요청되면, THE Play_Screen_Controller SHALL `Battle_Service.resumeIfActive`로 활성 전투가 있으면 저장된 몬스터 HP로 `Battle_View_Fragment`를 복원하고 `Battle_Active_Flag`를 true로 진입시킨다.
7. IF 저장된 `monsterId`가 `Monster` 카탈로그(또는 현재 노드 배치)에서 사라졌으면, THEN THE Battle_Service SHALL 전투를 안전 종료(`active=false`)하고 일반 플레이 화면으로 복원한다.
8. THE 캐릭터의 `currentNodeId`는 이미 영속되므로, THE Myrpg_Web_Module SHALL 전투 장소를 `Battle_State`에 별도 저장하지 않고 노드로부터 복원한다.

### Requirement 2: 전투 시작

**User Story:** 플레이어로서, 몬스터를 만나 전투 버튼을 누르면 전투 화면으로 진입하고 싶다.

#### Acceptance Criteria

1. WHEN 일반 조우에서 `전투` 버튼을 클릭하면, THE Myrpg_Web_Module SHALL `POST /battle/start?monsterId=`를 호출한다.
2. WHEN `POST /battle/start`가 처리되면, THE Battle_Service SHALL 현재 노드에 배치된 해당 `monsterId`로 `Battle_State`를 생성(§1.3)하고 `Battle_View_Fragment`를 렌더하여 `.center`를 교체한다.
3. THE Battle_Service SHALL 전투 시작 시 Action_Log에 `"combat"` 타입으로 전투 시작 로그를 남긴다.
4. IF `monsterId`가 미지이거나 현재 노드에 배치되지 않았으면, THEN THE Battle_Service SHALL 예외 없이 전투를 시작하지 않고 일반 플레이 화면을 반환한다.
5. THE `기존 monsterAction()`의 `alert("구현 예정입니다")` SHALL `POST /battle/start` 호출로 교체된다.

### Requirement 3: 가위바위보 상성 및 9칸 데미지 매트릭스

**User Story:** 개발자로서, 세 행동(일반/강/방어)의 상성 승패와 각 칸의 피해 규칙을 순수 로직으로 정의하고 싶다.

#### Acceptance Criteria

1. THE Rock_Paper_Scissors SHALL 상성을 순환으로 정의한다: 일반(NORMAL) > 강(HEAVY), 강 > 방어(DEFENSE), 방어 > 일반.
2. THE Battle_Resolver SHALL 플레이어 행동 타입과 몬스터 행동 타입을 받아 9칸 매트릭스에 따라 양측 피해를 산출한다.
3. WHERE 공격 vs 공격이고 상성 승패가 갈리면, THE Battle_Resolver SHALL 이긴 쪽 자기 데미지 100%·진 쪽 0으로 산출한다.
4. WHERE 방어가 일반을 이기면(D>N), THE Battle_Resolver SHALL 공격자 데미지를 방어자 경감률만큼 감소(`× (1 − blockRate)`)시키고 방어자 반격을 발생시킨다.
5. WHERE 강이 방어를 이기면(H>D), THE Battle_Resolver SHALL 강 데미지 100% 관통·방어자 반격 무효로 산출한다.
6. WHERE 동일 타입 무승부(일반↔일반, 강↔강)이면, THE Battle_Resolver SHALL 양쪽 각자 데미지의 50%로 산출한다.
7. WHERE 방어↔방어이면, THE Battle_Resolver SHALL 양쪽 0(교착)으로 산출한다.
8. THE Battle_Resolver SHALL 상성계수(Affinity_Coefficient)를 승 1.0 / 무승부 0.5 / 방어당함 `(1 − blockRate)` / 관통패 0.0으로 적용한다.
9. THE 방어자가 플레이어이면 THE Battle_Resolver SHALL 디펜스 스킬의 `blockRateByRank`를 경감률로, 방어자가 몬스터이면 `Monster_Defense_Constant.defenseBlockRate`(기본 40%)를 경감률로 사용한다.

### Requirement 4: 데미지 공식 (감산형·크리티컬·편차)

**User Story:** 개발자로서, 예측 가능하면서 약간의 변동성이 있는 감산형 데미지 공식을 정의하고 싶다.

#### Acceptance Criteria

1. THE Attack_Power SHALL `round(주스탯 × 재능계수)`로 산출되며, 주스탯은 착용 무기 재능별(근접 STR / 활 DEX / 마법 INT, 장비·스킬 보너스 포함), 재능계수는 근접 1.0 / 활 0.85 / 마법 1.2(튜닝값)이다.
2. THE Damage_Formula 기본피해 SHALL `max(1, floor(Attack_Power × 스킬배율% / 100) − 대상.defense)`로 산출하며, 최소 데미지 1을 보장한다.
3. THE 스킬배율(%) SHALL 플레이어는 `Skill_Damage_Policy.multiplier(skill, rank)`, 몬스터는 행동별 상수(일반 100% / 강 150%, 튜닝값)를 사용한다.
4. THE 대상.defense SHALL 정액 감산이며, 플레이어는 `Stats.defense`(+장비/스킬 보너스), 몬스터는 `Monster.defense`이다.
5. THE Damage_Formula 보정피해 SHALL `기본피해 × 상성계수 × (크리티컬 ? 1.5 : 1)`로 산출한다.
6. THE Damage_Formula 최종피해 SHALL `max(1, round(보정피해 × rand(0.90 ~ 1.10)))`로 산출하며, ±10% 편차는 매 타격 마지막에 적용한다.
7. THE 방어 반격 데미지 SHALL 플레이어는 `counterMultiplier(defenseSkill, rank) × Attack_Power`, 몬스터는 `Monster_Defense_Constant.defenseCounterRate% × Monster.attackPower`(기본 30%)로 산출한다.
8. THE 감산·상성 등 결정적 부분은 순수 유지하고, 크리티컬·편차만 주입 `Random_Bean`으로 계산하여 시드 고정 테스트가 가능해야 한다.

### Requirement 5: 크리티컬

**User Story:** 개발자로서, 모든 결과에 적용되는 일관된 크리티컬 규칙을 정의하고 싶다.

#### Acceptance Criteria

1. THE Critical_Hit 판정 SHALL `random.nextInt(1000) < critical`(critical은 0.1% 단위 정수)로 하며, 플레이어·몬스터 동일하다.
2. THE Critical_Hit 배율 SHALL 1.5배(튜닝값)이다.
3. THE Critical_Hit SHALL 공격뿐 아니라 방어(반격)·무승부(50%) 데미지에도 판정·적용한다.
4. THE 계산 순서 SHALL 크리티컬(×1.5) 적용 후 마지막에 ±10% Damage_Variance가 곱해진다.

### Requirement 6: 선후공 (턴 순서)

**User Story:** 플레이어로서, 양쪽이 서로 피해를 주는 턴에서 누가 먼저 때리는지가 결과에 반영되기를 원한다.

#### Acceptance Criteria

1. THE Turn_Order SHALL 매 턴 결정되며, 양쪽 모두 피해가 있는 턴에서만 결과에 영향을 준다(한쪽만 피해면 순서 무의미).
2. WHEN 선공이 후공을 먼저 처치하면, THE Battle_Service SHALL 후공 피해를 적용하지 않는다.
3. WHERE 동일 타입 무승부(일반↔일반, 강↔강)이면, THE Turn_Order SHALL 플레이어/몬스터 50:50(Random_Bean)으로 정한다.
4. WHERE 일반↔방어(방어 승)이면, THE Turn_Order SHALL 결정론적으로 공격자(일반)의 경감된 피해가 먼저 들어가고 그다음 방어자가 반격한다(코인플립 없음).
5. THE Turn_Order SHALL 기습 여부와 무관하다(기습은 강제 전투 돌입일 뿐, §17).
6. WHERE 활 재능이고 첫 턴이면, THE Turn_Order SHALL Bow_First_Strike(§7)에 우선순위를 양보하고 2턴차부터 이 규칙을 따른다.

### Requirement 7: 재능 특성 — 활 1턴 선제 사격

**User Story:** 활 플레이어로서, 전투를 활로 시작하면 첫 턴에 선제 사격의 이점을 누리고 싶다.

#### Acceptance Criteria

1. WHERE 전투 첫 턴(`turnCount == 1`)에 활이 장착돼 있으면, THE Battle_Service SHALL Bow_First_Strike를 발동한다.
2. WHEN Bow_First_Strike가 발동하면, THE Battle_Service SHALL 몬스터의 1턴 행동을 완전 무효(피해·방어·반격 0)로 처리한다.
3. WHEN Bow_First_Strike가 발동하면, THE Battle_Service SHALL 유저 스킬을 상성 무시 100% 적중(상성계수 1.0)으로 처리하며, 방어 스킬이어도 반격 데미지 100%가 몬스터에 적중한다.
4. THE Bow_First_Strike SHALL 크리티컬 판정을 정상 적용하고, 2턴차부터 일반 규칙(RPS + 50:50)으로 복귀한다.
5. THE Bow_First_Strike SHALL 기습(강제 전투)으로 시작해도 동일하게 첫 턴에 적용한다.
6. THE Bow_First_Strike 발동 조건 SHALL "활을 처음 든 턴"이 아니라 "첫 턴에 활 장착"이며, 다른 무기로 첫 턴을 치른 뒤 활로 교체해도(§22) 발동하지 않는다(그 시점엔 `turnCount != 1`).

### Requirement 8: 재능 특성 — 마법 캐스팅 실패

**User Story:** 마법 플레이어로서, 고화력의 대가로 가끔 캐스팅이 실패하는 리스크를 감수하고 싶다.

#### Acceptance Criteria

1. WHEN 공격 마법 스킬(일반/강)을 사용하면, THE Battle_Service SHALL 10% 확률로 Magic_Cast_Failure를 판정한다.
2. THE 방어(재능 무관 공통 스킬) SHALL 캐스팅 실패가 없다.
3. WHEN Magic_Cast_Failure가 발생하면, THE Battle_Service SHALL 플레이어 행동을 완전 무효(피해 0)로 하되 턴은 소모하고 MP는 소모한다(실패해도 소모).
4. WHEN Magic_Cast_Failure가 발생하면, THE Battle_Service SHALL 몬스터 행동을 정상 처리한다(공격이면 피해, 방어면 양쪽 0).
5. THE 판정 순서 SHALL 턴 시작 시 10% 굴림 → 실패면 상성/데미지 계산을 스킵한다.
6. WHEN Magic_Cast_Failure가 발생하면, THE Action_Log SHALL `"{스킬명} 캐스팅 실패!"` 로그를 남긴다(플레이어 로그 자리).

### Requirement 9: 자원 소모

**User Story:** 플레이어로서, 스킬이 정의한 자원(스태미나/MP)을 소모하고, 부족하면 명확한 안내를 받고 싶다.

#### Acceptance Criteria

1. THE Resource_Cost SHALL 근접/활은 스태미나, 마법은 MP를 `Skill.resourceCost`만큼 소모한다.
2. IF 자원이 스킬 비용보다 적으면, THEN THE Battle_Service SHALL 턴을 진행하지 않고(자원 차감·데미지 없음) 몬스터 행동도 없이 유지한다.
3. WHEN 자원 부족으로 턴이 미진행되면, THE Myrpg_Web_Module SHALL `alert("MP가 부족합니다!")`(마법) 또는 `alert("스태미나가 부족합니다!")`(근접·활)를 표시한다.
4. WHERE 자원이 충분하면, THE Battle_Service SHALL 자원을 차감한 뒤 턴을 진행한다.
5. THE 마법 SHALL Magic_Cast_Failure(§8) 시에도 MP를 소모한다.

### Requirement 10: 턴 진행 오케스트레이션

**User Story:** 플레이어로서, 스킬 버튼을 누르면 한 턴이 계산되어 피해·로그·게이지가 갱신되기를 원한다.

#### Acceptance Criteria

1. WHEN 스킬 버튼을 클릭하면, THE Myrpg_Web_Module SHALL 클릭 즉시 `alert("{스킬명} 스킬을 사용하였습니다.")`를 표시한 뒤 `POST /battle/turn?skillId=`를 호출한다.
2. WHEN `takeTurn`이 처리되면, THE Battle_Service SHALL 순서대로 (a) 플레이어 행동 타입 결정 + `Monster_Ai_Service.nextAction()` 몬스터 타입 결정, (b) 자원 검사·소모(+마법 캐스팅 실패 판정), (c) 재능 분기(활 1턴 선제 또는 `Battle_Resolver` 매트릭스), (d) 선후공 결정·선공 처치 시 후공 스킵, (e) `damageHp`·`monsterCurrentHp` 차감을 수행한다.
3. WHEN 한 턴이 종료되면, THE Battle_Service SHALL `Skill_Service.onSkillUsed`를 호출하고, 몬스터 처치 시 `onSkillKill`을 호출한다(§16).
4. WHEN 플레이어가 공격 행동을 한 턴이면, THE Battle_Service SHALL 장착 장비 `reduceDurability(0.2)`를 호출한다(§15).
5. WHEN 한 턴이 종료되면, THE Battle_Service SHALL `Character_Service.saveTurn`과 `Battle_State` 저장을 수행한다(§1.4).
6. WHEN `POST /battle/turn`이 응답하면, THE Myrpg_Web_Module SHALL `Top_Bar_Fragment`(플레이어 게이지) + `Battle_View_Fragment`(몬스터 HP 바) + `Action_Log`(2줄)를 교체하여 실시간 갱신한다.
7. THE Battle_Service SHALL 각 턴 결과를 `Battle_Turn_Result`로 반환한다.

### Requirement 11: HP 감소·사망·부활

**User Story:** 플레이어로서, 전투에서 HP가 0이 되면 티르코네일에서 부활하되 과도한 손실은 없기를 원한다.

#### Acceptance Criteria

1. THE Character_Progress SHALL `damageHp(int amount)`를 신설하여 `hpCurrent = max(0, hpCurrent − amount)`로 0을 바닥으로 감소시킨다.
2. THE Character_Progress SHALL `isDead()`(`hpCurrent == 0`) 헬퍼를 제공한다.
3. WHEN 플레이어 HP가 0이 되면, THE Battle_Service SHALL Death_Respawn을 수행한다: 경험치 −10%(`applyDeathPenalty`) + HP/MP/스태미나 풀 회복(`fullRecover`) + `currentNodeId`를 `tir-chonaill`로 강제 이동.
4. WHEN Death_Respawn이 수행되면, THE Battle_Service SHALL 골드·아이템을 변경하지 않는다.
5. WHEN Death_Respawn이 수행되면, THE Battle_Service SHALL `Battle_State`를 `active=false`로 전환한다.
6. WHEN 패배가 확정되면, THE Myrpg_Web_Module SHALL `alert("정신을 잃고 쓰러졌습니다… 티르코네일에서 되살아납니다.")`를 표시한 뒤 사망 처리를 실행하고 `.center`를 티르코네일 노드 화면으로 복원한다.

### Requirement 12: 도망

**User Story:** 플레이어로서, 위험할 때 전투에서 도망칠 수단을 원한다.

#### Acceptance Criteria

1. THE Battle_View SHALL 도망 버튼을 상시 노출한다.
2. WHEN 도망 버튼을 클릭하면, THE Myrpg_Web_Module SHALL `POST /battle/flee`를 호출한다.
3. WHEN `flee`가 처리되면, THE Battle_Service SHALL 50% 성공을 판정(Random_Bean)한다.
4. WHERE 도망이 성공하면, THE Battle_Service SHALL `Battle_State`를 `active=false`로 전환(전투 종료, 노드 유지)하고 `.center`를 일반 플레이 화면으로 복원한다.
5. WHERE 도망이 실패하면, THE Battle_Service SHALL 턴을 소모하고 몬스터 1회 공격(일방 피해, `damageHp`) + `saveTurn`을 수행하고 전투를 계속한다.
6. WHEN 도망 실패로 플레이어 HP가 0이 되면, THE Battle_Service SHALL Death_Respawn(§11)을 수행한다.

### Requirement 13: 처치 보상 및 인벤토리 획득

**User Story:** 플레이어로서, 몬스터를 처치하면 골드·아이템·경험치를 획득하고 싶다.

#### Acceptance Criteria

1. WHEN 몬스터가 처치되면, THE Battle_Service SHALL `Monster_Reward_Service.rollDrop(monster)`로 드랍을 계산하고 `Inventory_Service.acquire`로 적재한 뒤 `Progression_Service.gainExperience`로 경험치를 지급한다.
2. THE Inventory_Service SHALL `acquire(progress, DropResult)`를 신설하여 골드는 항상 `gainGold`로 가산하고, 아이템은 `Owned_Item` 생성/수량 증가로 적재한다.
3. IF 아이템 적재 시 인벤토리 용량 30을 초과하면, THEN THE Inventory_Service SHALL 해당 아이템 획득을 실패(소실)시키고 Action_Log에 `"{아이템명} 획득 실패!"`를 남기며 나머지 아이템 처리는 계속한다(골드는 정상 획득).
4. WHEN 승리가 확정되면, THE Myrpg_Web_Module SHALL `alert("{몬스터명}이(가) 쓰러졌습니다!")`를 표시한다.
5. WHEN 승리가 확정되면, THE Action_Log SHALL 보상 3줄(골드 → 드랍 아이템 → 경험치, §20)을 순서대로 남긴다.
6. WHEN 승리로 전투가 종료되면, THE Battle_Service SHALL `Battle_State`를 `active=false`로 전환하고 `.center`를 현재 노드 플레이 화면으로 복원한다(몬스터는 정적·비리스폰이라 버튼이 다시 존재).

### Requirement 14: 스킬 카운트 훅 호출

**User Story:** 개발자로서, 전투에서 실제로 스킬 사용/막타가 스킬 숙련 카운트로 이어지기를 원한다.

#### Acceptance Criteria

1. WHEN 플레이어가 스킬을 사용한 턴이면(자원 부족으로 미진행한 경우 제외), THE Battle_Service SHALL `Skill_Service.onSkillUsed(characterId, skillId)`를 호출한다.
2. WHEN 플레이어 스킬이 몬스터를 처치하면(막타), THE Battle_Service SHALL `Skill_Service.onSkillKill(characterId, skillId)`를 호출한다.
3. THE Myrpg_Web_Module SHALL 임시 스킬 드라이버(`Skill_Controller`의 `dev/fill-usage`·`dev/fill-kill`)를 제거하고 실제 전투 훅으로 대체한다(§23).

### Requirement 15: 내구도 감소 및 자동 장착 해제

**User Story:** 플레이어로서, 장비가 전투로 마모되며 완전히 닳으면 벗겨지는 것을 원한다.

#### Acceptance Criteria

1. WHEN 플레이어가 공격 행동을 한 턴이면, THE Battle_Service SHALL 장착 장비의 `reduceDurability(0.2)`를 호출한다.
2. WHEN 내구도가 0에 도달하면, THE Inventory_Service SHALL 해당 장비를 자동 장착 해제(unequip)하여 보너스를 소멸시킨다.
3. WHEN 자동 장착 해제가 발생하면, THE Action_Log SHALL `"{아이템명} 내구도 0 — 장착 해제됨"`을 남긴다.
4. THE 내구도 수리(대장간) SHALL 본 스펙 범위 밖(7순위)이며, 전투에서는 파손 시 자동 해제까지만 처리한다.

### Requirement 16: 전투 스킬 노출 및 실시간 갱신

**User Story:** 플레이어로서, 전투에서 지금 든 무기로 쓸 수 있는 스킬만 보고 싶다.

#### Acceptance Criteria

1. THE Combat_Skill_List SHALL 착용 무기 재능 스킬 + 공통 스킬(방어)만 노출한다(예: 활 착용 → 활 재능 스킬 + 공통).
2. THE Inventory_Service SHALL 착용 무기 재능 + 공통을 기준으로 전투 스킬 목록을 조회하는 기능을 제공한다.
3. WHEN `GET /battle/skills`가 요청되면, THE Battle_Controller SHALL 현재 착용 무기 기준 `Battle_Skills_Fragment`(`battle-view :: battle-skills`)를 반환한다.
4. WHERE 착용 무기 재능 ≠ 캐릭터 재능이면, THE Battle_Service SHALL 특성(1턴 선제·계수 등)은 착용 무기 재능을 따르되 주스탯은 캐릭터가 키운 실제 값을 사용한다(자연 페널티).

### Requirement 17: 기습 (강제 전투 돌입)

**User Story:** 플레이어로서, 몬스터가 사는 필드에 들어설 때 가끔 강제로 전투에 돌입하는 긴장감을 느끼고 싶다.

#### Acceptance Criteria

1. WHEN 이동이 성공(`Movement_Result.Moved`)하고 그 노드에 몬스터가 있으면, THE Play_Screen_Controller SHALL `Monster_Encounter_Service.rollPreemptiveStrike`(5%)로 강제 전투 돌입을 판정한다.
2. WHEN 기습이 발동하면, THE Myrpg_Web_Module SHALL `alert("매복하고 있던 {몬스터명}이(가) 기습해옵니다!")`를 표시한 뒤 플레이어 선택 없이 자동으로 `battle/start`(해당 몬스터)를 수행한다.
3. THE Ambush SHALL 선후공과 별개 개념이며, 기습으로 시작한 전투의 첫 턴 선후공도 일반 규칙(§6, 기본 50:50)을 따른다.
4. THE 기습 alert SHALL 기존 선공 `alert("몬스터 선공 발동")`을 대체한다.
5. THE Myrpg_Web_Module SHALL 기습 판정을 `GET /`(새로고침)·이동 거부(`Blocked`)에서는 수행하지 않는다(진입 시 1회).
6. THE Battle_State SHALL 기습으로 시작했는지를 `ambush` 플래그로 보관한다.

### Requirement 18: 전투 뷰 레이아웃 (전용 프래그먼트)

**User Story:** 플레이어로서, 전투 중에는 몬스터 정보와 스킬 버튼이 명확히 보이는 화면을 원한다.

#### Acceptance Criteria

1. THE 전투 뷰 SHALL 전용 프래그먼트 `battle-view.html`(`th:fragment="battle-view"`)로 분리하며, `POST /battle/start`·`/battle/turn`·`/battle/flee` 응답이 이를 렌더해 `.center` 전체를 교체한다.
2. WHERE 전투 중이면, THE `.center` SHALL 기존 `situation`(상황멘트)·`npc-talk`(대사)·`interactions`(조우 버튼)을 감추고 `Battle_View_Fragment`로 대체한다.
3. THE Battle_View_Fragment SHALL 몬스터 이름 + 레벨, 몬스터 HP 바(`monsterCurrentHp / maxHp`, 기존 `.bar` 컴포넌트 재사용), 스킬 버튼 영역(`#battleSkills`), 도망 버튼(상시)을 포함한다.
4. THE Battle_View_Fragment SHALL 미니맵(`fragments/minimap`)을 포함하여 전투 중에도 현재 위치를 표시한다.
5. THE 플레이어 게이지바(HP/MP/스태미나)·EXP SHALL `.top-bar`의 `.stat-bars`에 그대로 유지되며(사이드바 아님), 턴 응답에서 `.top-bar`가 함께 교체되어 실시간 갱신된다.
6. THE `.left-sidebar`·`.move-pad`·`.action-log` SHALL 전투 중에도 노출된다.

### Requirement 19: 전투 중 이동 차단

**User Story:** 플레이어로서, 전투 중에는 도망 외에 필드 이동을 할 수 없어야 한다.

#### Acceptance Criteria

1. THE `.move-pad` SHALL 전투 중에도 그대로 출력된다.
2. WHEN 전투 중(`Battle_Active_Flag == true`) 방향 버튼을 클릭하면, THE Myrpg_Web_Module SHALL `POST /move`를 호출하지 않고 `alert("전투 중에는 이동할 수 없습니다.")`만 표시한다.
3. THE `move(dx, dy)` SHALL 진입부에서 `Battle_Active_Flag`를 검사하여 true면 alert 후 즉시 return한다.
4. THE Play_Screen_Controller SHALL 방어적으로 `POST /move` 요청 시 활성 전투가 있으면 이동을 거부한다.

### Requirement 20: 포션·장비 실시간 반영 (턴 무소모)

**User Story:** 플레이어로서, 전투 중에도 인벤토리에서 포션을 쓰고 장비를 바꿀 수 있으며, 그 결과가 즉시 화면에 반영되기를 원한다.

#### Acceptance Criteria

1. WHEN 전투 중 인벤토리에서 포션을 사용하면, THE Myrpg_Web_Module SHALL `POST /inventory/use`를 호출하되 턴을 소모하지 않고 몬스터 행동도 없다.
2. WHEN 포션이 사용되면, THE Myrpg_Web_Module SHALL 회복 결과를 `.top-bar` 게이지(HP/MP/스태미나)에 즉시 반영하며, 몬스터 현재 HP·`turnCount`는 불변이다.
3. WHEN 전투 중 인벤토리에서 장비를 착용/해제하면, THE Myrpg_Web_Module SHALL `POST /inventory/equip`·`/unequip`를 호출하되 턴을 소모하지 않고 몬스터 행동도 없다.
4. WHEN 무기가 교체되면, THE Myrpg_Web_Module SHALL `Battle_Active_Flag == true`일 때 `GET /battle/skills`를 재요청하여 `#battleSkills` 영역만 실시간으로 재렌더한다(Combat_Skill_List, §16).
5. WHEN 장비가 교체되면, THE Battle_Service SHALL 이후 턴의 재능 특성(§7·§8)·주스탯·공격력·방어를 새 장비 기준으로 즉시 반영한다.

### Requirement 21: 활동 로그 포맷

**User Story:** 플레이어로서, 매 턴 내 공격과 몬스터 공격 결과를 로그로 명확히 보고 싶다.

#### Acceptance Criteria

1. WHEN 한 턴이 종료되면, THE Action_Log SHALL 2줄(플레이어 행동·피해, 몬스터 행동·피해)을 `"combat"` 타입으로 남긴다.
2. THE 플레이어 로그 SHALL `"{스킬명}({타입})로 {몬스터명}에게 {N} 피해"` 형식이며, 크리티컬 시 `"… {N} 피해 (크리티컬!)"`, 방어 승 시 `"… 방어로 {N} 피해 (반격)"`으로 표기한다.
3. THE 몬스터 로그 SHALL `"{몬스터명}의 {타입}공격, {N} 피해를 입음"` 형식이며, 방어 성공 시 `"{몬스터명}의 {타입}공격을 방어 ({N} 피해)"`로 표기한다.
4. WHEN Bow_First_Strike가 발동하면, THE Action_Log SHALL `"선제 사격! {몬스터명}에게 {N} 피해"`를 남긴다(몬스터는 이 턴 반격 없음).
5. WHEN Magic_Cast_Failure가 발생하면, THE Action_Log SHALL `"{스킬명} 캐스팅 실패!"`를 남긴다.
6. WHEN 도망이 처리되면, THE Action_Log SHALL 성공 시 `"도망쳤다!"`, 실패 시 `"도망 실패! {몬스터명}에게 {N} 피해"`를 남긴다.
7. WHEN 승리 보상이 지급되면, THE Action_Log SHALL 골드(`"{N}골드를 획득하였습니다."`) → 드랍 아이템(성공 시 `"{아이템명}을(를) 획득하였습니다."`) → 경험치(`"{N} 경험치를 획득하였습니다."`) 순서로 남긴다.
8. THE Action_Log SHALL Death_Respawn 시 `"쓰러졌다… 티르코네일에서 부활 (경험치 -{N})"`을 남긴다.
9. THE 전투 로그 SHALL 기존 `"combat"` CSS(`.log-combat`)를 재사용한다(신규 스타일 불필요).

### Requirement 22: 몬스터 방어 상수

**User Story:** 개발자로서, 몬스터의 방어/반격을 스킬 없이 상수로 정의하고 몬스터별로 조정하고 싶다.

#### Acceptance Criteria

1. THE Monster SHALL optional `defenseBlockRate`(경감률)·`defenseCounterRate`(반격율) 필드를 보유하며, 미지정 시 전역 기본(경감 40% / 반격 30%)을 사용한다.
2. THE Monster_Service SHALL `monster.json`에서 두 필드를 optional 파싱한다(미지정 시 전역 기본, `theme` 등과 동일한 `has(...)` 방식).
3. WHERE 몬스터가 방어(DEFENSE) 행동을 하면, THE Battle_Resolver SHALL `defenseBlockRate`를 경감률로, `defenseCounterRate`를 반격율(`× attackPower`)로 사용한다.
4. THE 두 필드 SHALL 모두 %이므로 전 레벨 자동 스케일되며, 보스는 60% / 50%가 권장된다.
5. THE MapNode·기존 Monster 필드 SHALL 하위 호환을 유지하여 기존 `monster.json`·테스트가 회귀 없이 로드된다.

### Requirement 23: 임시 개발용 버튼·드라이버 제거

**User Story:** 개발자로서, 실제 전투/보상 루프가 생겼으니 임시 개발용 버튼을 정리하고 싶다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL 임시 골드 버튼(`Play_Screen_Controller`의 `POST /gold/gain`·`/gold/spend`, `left-sidebar.html` 버튼, `myrpg.js`의 `goldGain`/`goldSpend`)을 제거한다.
2. THE Myrpg_Web_Module SHALL 임시 경험치 버튼(`POST /exp/up`·`/exp/down`, `left-sidebar.html` 버튼, `myrpg.js`의 `expUp`/`expDown`)을 제거한다.
3. THE Myrpg_Web_Module SHALL 승급 팝업의 임시 횟수/처치수 버튼(`Skill_Controller`의 `dev/fill-usage`·`dev/fill-kill`, 승급 모달의 `.rankup-temp-btn` 및 관련 JS)을 제거한다.
4. WHEN 임시 버튼·엔드포인트가 제거되면, THE Myrpg_Web_Module SHALL 이를 참조하던 기존 컨트롤러/뷰 테스트를 함께 정리하여 완료 시점에 빌드가 그린이도록 한다.

### Requirement 24: 예외 처리·하위 호환·정합성

**User Story:** 개발자로서, 전투 데이터/상태 오류가 안전하게 처리되고 기존 산출물이 깨지지 않기를 원한다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL 커스텀 예외를 사용하며 `RuntimeException`을 직접 던지지 않는다(code-style). 전투 진행 무결성 위반(예: `monsterId` 소실)은 예외 없이 안전 종료(§1.7)로 처리한다.
2. THE Character_Progress·Play_Screen_View·Monster·MapNode SHALL 기존 필드/시그니처를 보존하고 추가만 하여 기존 호출부·테스트가 회귀 없이 통과한다(하위 호환).
3. WHEN `Play_Screen_Controller`·기존 `@WebMvcTest`에 전투 서비스 협력자가 추가되면, THE 기존 컨트롤러 테스트 SHALL 해당 협력자를 `@MockitoBean`으로 추가하고 기본 스텁(활성 전투 없음)으로 회귀 없이 통과한다.
4. THE Myrpg_Web_Module SHALL `Skill_Damage_Policy`·`Monster_Ai_Service` 등의 JavaDoc에서 전투를 "7순위"로 잘못 표기한 부분을 "6순위"로 정정한다.
5. THE 상단바 구조·사이드바(임시 버튼 제외)·미니맵·은행 SHALL 전투와 무관한 부분을 변경하지 않는다.

### Requirement 25: 이연 항목 명시

**User Story:** 개발자로서, 본 스펙이 어디까지이고 무엇이 이후 순위로 넘어가는지 코드/문서에 남기고 싶다.

#### Acceptance Criteria

1. THE Myrpg_Web_Module SHALL 던전 내부 전투를 10순위로 이연한다.
2. THE Myrpg_Web_Module SHALL 보스 실데이터·보스 인챈트 아이템 드랍을 인챈트 시스템 확정 후로 이연한다.
3. THE Myrpg_Web_Module SHALL 내구도 수리(대장간)를 7순위로 이연하며, 전투에서는 파손 시 자동 장착 해제까지만 처리한다.
4. THE 각 이연 seam SHALL 담당 순위·조건을 서술형 주석(JavaDoc)으로 명시한다(나열식 `// TODO` 금지).
5. THE 밸런싱 튜닝값(재능계수·몬스터 배율·크리티컬 배율·편차폭·몬스터 방어 상수)은 현재값을 채택하되 구현·밸런싱 단계에서 미세 조정 가능함을 명시한다.
