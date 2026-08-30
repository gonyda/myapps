# MyRPG 하드코딩 전수 분석 및 메시지·로그 외부화 리팩토링 계획서

> **문서 위치**: `docs/hardcoding-analysis-and-refactoring-plan.md`  
> **작성 일자**: 2026-08-29  
> **최종 갱신**: 2026-08-29 20:38 (정합성 보정, 템플릿 중복 통합 및 3단계 로드맵 압축 반영)  
> **대상 모듈**: `com.myapps.web.myrpg` (Spring Boot 4.0 / Java 21)

---

## 1. 개요 (Executive Summary)

현재 MyRPG는 아이템 카탈로그(`item.json`), 몬스터(`monster.json`), 스킬(`skill.json`), 맵(`map.json`), NPC 시간대별 대사(`ambience_dialogue.json`) 등 주요 게임 콘텐츠를 JSON 데이터 파일로 분리하여 관리하고 있습니다.

그러나 **하단 활동 로그(ActionLog)**, **전투 공방 로그(BattleLogFormatter + BattleService 인라인)**, **유저 노출 비즈니스 예외 및 클라이언트 JS 토스트/alert 메시지**, **게임 밸런스 상수(확률, 스태미나 소모량, 슬롯 제한, 전투 계수 등)**, **인벤토리 상세 팝업(InventoryService.describe) 장비 포맷팅 문구**가 여전히 Java/JS 소스 코드 내에 문자열 및 매직 넘버(Magic Numbers)로 하드코딩되어 있습니다.

본 문서는 소스 코드 내에 잔존하는 하드코딩 요소들을 전수 분석하고, **서버-클라이언트 간 중복 텍스트 단일화(SSOT)**, **전투 턴 로그 템플릿 최적화**, **`messages.properties` 및 `application-game.yml` 기반의 효율적인 3단계 마이그레이션 아키텍처**를 제시합니다.

---

## 2. 현 소스코드 내 하드코딩 영역 전수 분석

### 2.1. 하단 활동 로그 (ActionLog) 문자열 — 총 26건

8개 서비스/컨트롤러에서 `actionLog.add("...", LOG_TYPE)` 형태로 하드코딩된 한국어 메시지를 직접 적재하고 있습니다.

| # | 도메인/서비스 | 로그 유형 | 하드코딩 위치 | 현재 하드코딩된 코드 | 개선 방향 (외부화 키) |
|---|---|---|---|---|---|
| 1 | `GatheringService` | `ITEM` | L191 | `"[채집] 🪵 단단한 장작을 1개 얻었습니다!"` | `log.gathering.success` (버섯/약초 확장 대비 `{0} 획득!` 범용화) |
| 2 | `GatheringService` | `SYSTEM` | L195 | `"[채집] 💨 헛도끼질을 하여 장작을 얻지 못했습니다."` | `log.gathering.failure` (`[채집] 채집에 실패했습니다.` 범용화) |
| 3 | `ShopService` | `ITEM` | L338 | `"아이템을 구매했습니다: " + item.name()` | `log.shop.buy` |
| 4 | `ShopService` | `ITEM` | L381 | `"아이템을 판매했습니다: " + catalogItem.name()` | `log.shop.sell` |
| 5 | `InventoryService` | `ITEM` | L1445 | `itemName + " 획득 실패!"` | `log.item.acquire_fail` |
| 6 | `InventoryService` | `ITEM` | L1469 | `itemName + " 획득 실패!"` | `log.item.acquire_fail` (중복 통합) |
| 7 | `InventoryService` | `ITEM` | L1574 | `itemName + " 내구도 0 — 장착 해제됨"` | `log.item.durability_broken` |
| 8 | `InventoryController` | `ITEM` | L96 | `"포션 사용: " + potion.name()` | `log.potion.use` |
| 9 | `DungeonService` | `DUNGEON` | L137 | `spec.name() + "에 입장했습니다."` | `log.dungeon.enter` |
| 10 | `DungeonService` | `DUNGEON` | L166 | `"던전에서 나왔습니다."` | `log.dungeon.exit` |
| 11 | `DungeonService` | `DUNGEON` | L288 | `spec.name() + "을(를) 완전히 정복했습니다!"` | `log.dungeon.clear` |
| 12 | `DungeonService` | `DUNGEON` | L289-290 | `"던전 클리어 보상: EXP +" + exp + ", Gold +" + gold + "G"` | `log.dungeon.clear_reward` |
| 13 | `DungeonService` | `DUNGEON` | L301 | `"보상 획득: " + itemName + " x" + qty` | `log.dungeon.item_reward` |
| 14 | `BattleService` | `COMBAT` | L281-283 | `"궁극기 쿨타임 대기 중입니다. (" + cooldown + "승 남음)"` | `log.combat.ultimate_cooldown` |
| 15 | `BattleService` | `COMBAT` | L314 | `resourceKind.label() + "이(가) 부족합니다."` | `system.resource_lack` (JS 메시지와 단일화) |
| 16 | `BattleService` | `GROWTH` | L1188-1194 | `"🎉 레벨업! Lv." + level + " 달성! (AP +" + ap + ")"` | `log.growth.levelup` |
| 17 | `BattleService` | `COMBAT` | L1347 | `"도망쳤다!"` | `battle.flee.success` (JS 메시지와 단일화) |
| 18 | `BattleController` | `COMBAT` | L112 | `"⚔️ " + monster.name() + " 조우!"` | `log.combat.encounter` |
| 19 | `PlayScreenController` | `NOTIFICATION` | L213 | `"전투 중에는 이동할 수 없습니다."` | `system.move_blocked` (JS 메시지와 단일화) |
| 20 | `PlayScreenController` | `COMBAT` | L243 | `"🚨 " + monster.name() + " 기습!"` | `battle.ambush` (JS 메시지와 단일화) |
| 21 | `PlayScreenController` | `NOTIFICATION` | L344 | `"환생했습니다 (재능: " + talent.label() + ")"` | `log.system.rebirth_done` |
| 22 | `PlayScreenController` | `NOTIFICATION` | L349 | `"환생까지 " + hours + "시간 " + minutes + "분 남았습니다"` | `log.system.rebirth_wait` |
| 23 | `PlayScreenController` | `NOTIFICATION` | L372-377 | `"테스트 치트: 1,000 EXP를 획득했습니다! (Lv." + lv + " 달성, AP +" + ap + ")"` | `log.cheat.exp_levelup` |
| 24 | `PlayScreenController` | `NOTIFICATION` | L380 | `"테스트 치트: 1,000 EXP를 획득했습니다!"` | `log.cheat.exp` |
| 25 | `PlayScreenController` | `NOTIFICATION` | L400 | `"테스트 치트: 1,000 Gold를 획득했습니다!"` | `log.cheat.gold` |
| 26 | `SkillController` | `GROWTH` | L134-140 | `"✨ [" + label + "] " + rank + "랭크로 승급되었습니다!"` | `log.growth.skill_rankup` |

---

### 2.2. 전투 공방 인라인 로그 (`BattleService.java` 내 `combatLines.add`) — 총 15건

`BattleService` 내부에서 스킬 효과, 상태이상, 부활, 전투 결과 등을 직접 문자열로 생성하고 있습니다.

| # | 위치 (BattleService.java) | 하드코딩된 코드 | 외부화 키 |
|---|---|---|---|
| 1 | L659 | `"🔮 [" + skill.label() + "] 캐스팅 실패! (집중이 흐트러짐)"` | `battle.cast_fail` |
| 2 | L964 | `"💖 [" + recoverySkill.label() + "] HP +" + healed + " 회복"` | `battle.recovery` |
| 3 | L1007 | `"⛓️ [" + ccSkill.label() + "] " + monster.name() + " 기절 성공! (1턴)"` | `battle.cc.success` |
| 4 | L1009 | `"⛓️ [" + ccSkill.label() + "] 저항으로 제어 효과 실패"` | `battle.cc.resist` |
| 5 | L1028 | `"❄️ [" + monster.name() + "] 빙결/기절 상태로 행동 불가"` | `battle.stun_frozen` |
| 6 | L1052 | `"❄️ [빙결] " + monster.name() + " 꽁꽁 얼어붙음! (1턴 행동 불가)"` | `battle.freeze` |
| 7 | L1075 | `"💢 [레이지 임팩트] 다음 물리 피해 +30% 증폭"` | `battle.rage` |
| 8 | L1141 | `"🩸 [지속 피해] " + monster.name() + " " + dotDmg + " 도트 피해"` | `battle.dot` |
| 9 | L1159 | `"🧘 [메디테이션] MP +" + regened + " 회복"` | `battle.meditation` |
| 10 | L1175 | `"승리! EXP +" + exp + " | Gold +" + gold` (+ 아이템 드랍 요약) | `battle.victory_reward` |
| 11 | L1267 | `"쓰러졌다… 티르코네일에서 부활 (경험치 -" + expLost + ")"` | `battle.death` (JS 메시지와 단일화) |
| 12 | L1281 | `"⏳ [시간 초과] 선제 공격 기회 상실"` | `battle.timeout.preemptive` |
| 13 | L1286 | `"⏳ [시간 초과] 무방비 피격"` | `battle.timeout.hit` |
| 14 | L1301 | `"[" + monster.name() + "] 🛡️ 방어 태세 유지"` | `battle.monster.defense_hold` |
| 15 | L1384 | `"⚠️ [도망 실패] " + monster.name() + "에게 저지당해 " + dmg + " 피해"` | `battle.flee.fail` |

---

### 2.3. 전투 턴 상세 로그 (`BattleLogFormatter.java`) — 최적화 템플릿 (~20건)

기존에는 40여 개의 문장이 개별 하드코딩되어 있었으나, **공통 문장 구조(단일/다단 타격, 방어, 빗나감)를 인자화하여 약 20건의 핵심 템플릿으로 통합**합니다.

```java
// 통합 전: 선제/일반/궁극기별로 40여 개 문장이 분산
"⚠️ [적 선제공격] [" + input.monsterName() + "] 기습 ➔ " + input.monsterDamage() + " 피해 피격"
"👑 [결전 궁극기] " + skillTag + " " + hits.size() + "연타 (" + formatHits(hits) + ") ➔ 총 " + damage + " 관통 피해"
"⚔️ " + skillTag + " " + hits.size() + "연타 (" + formatHits(hits) + ") ➔ 총 " + damage + " 피해"
"⚡ " + skillTag + " " + hits.size() + "연타 (" + formatHits(hits) + ") ➔ 총 " + damage + " 피해"
"🗡️ " + skillTag + " 빗나감 (0 피해)"
"⚡ " + skillTag + " 적이 공격하지 않아 빗나감"

// 통합 후: 템플릿 공통화 (플레이어 공격, 몬스터 공격, 방어 상성 단일화)
battle.attack.multi={0} [{1}] {2}연타 ({3}) ➔ 총 {4} 피해
battle.attack.single={0} [{1}]{2} ➔ {3}에게 {4} 피해
battle.attack.ultimate_multi=👑 [결전 궁극기] [{0}] {1}연타 ({2}) ➔ 총 {3} 관통 피해
battle.attack.ultimate_single=👑 [결전 궁극기] [{0}]{1} 100% 관통 ➔ {2} 피해
battle.turn.block_perfect=⚔️ [{0}] 🛡️ {1}의 완전 방어에 가로막힘 (0 피해)
battle.turn.vs_defense_multi=⚔️ [{0}] {1}연타 ({2}) 🛡️ 적 방어에 막힘 ➔ 총 {3} 피해
battle.turn.vs_defense_single=⚔️ [{0}]{1} 🛡️ 적 방어에 막힘 ➔ {2} 피해
battle.turn.counter_crit=⚡ [{0}] 💥 적 공격을 흘려내며 ➔ {1} 치명 반격!
battle.turn.defense_success=🛡️ [{0}] 방어 성공 & 반격! ➔ {1}에게 {2} 반격 피해
battle.turn.defense_window=🛡️ [{0}] 완벽 방어! ➔ 빈틈 포착 (다음 턴 선제 찬스⚡)
battle.turn.defense_penetrated=⚠️ [{0}] 몬스터 강공격에 방어선 관통!
battle.turn.defense_stalemate=🛡️ [{0}] 맞방어 교착 상태
battle.turn.miss={0} [{1}] 빗나감 (0 피해)
battle.monster.hit=[{0}] {1} ➔ {2} 피해 피격
battle.monster.blocked=[{0}] {1} ➔ 🛡️ 방어로 경감되어 {2} 피해
battle.monster.miss=[{0}] {1} ➔ 빗나감
battle.monster.defense_counter=[{0}] 🛡️ 방어 성공 & 반격 ➔ {1} 피해 피격
battle.monster.defense_alert=[{0}] 🛡️ 공격 방어 성공 ➔ 반격 태세 (다음 턴 선제 주의⚠️)
battle.monster.defense_break=[{0}] 💥 방어선 관통됨!
battle.monster.defense_full=[{0}] 🛡️ 완전 방어 (0 피해)
```

---

### 2.4. 인벤토리 상세 팝업 (`InventoryService.describe`) 장비 포맷팅 — 3건

| # | 위치 (InventoryService.java) | 하드코딩된 코드 | 외부화 키 |
|---|---|---|---|
| 1 | L1131 | `equipItem.kind().label() + " (" + equipItem.type().label() + ")"` | `describe.equip.kind_type` |
| 2 | L1139 | `"방패와 함께 착용할 수 없습니다."` | `item.equip_conflict.shield` |
| 3 | L1147 | `"내구도: " + formatDurability(current) + "/" + max` | `describe.equip.durability` |

---

### 2.5. 도메인 예외 메시지 분류 (오버엔지니어링 방지)

모든 예외를 무분별하게 외부화하면 코드 가독성이 떨어지므로, **유저 노출 비즈니스 예외**와 **개발자 디버깅용 방어 예외**를 명확히 구분합니다.

#### ⭕ 외부화 대상: 유저 노출 비즈니스 예외 (8건)
컨트롤러 및 웹 응답을 통해 화면/토스트에 전달되는 예외 메시지만 `messages.properties`로 관리합니다.

| 예외 클래스 | 메시지 예시 | 외부화 키 |
|---|---|---|
| `EquipConflictException` | `"방패와 함께 착용할 수 없습니다."` | `exception.equip.shield_conflict` |
| `EquipConflictException` | `"장착을 해제한 후 판매할 수 있습니다."` | `exception.equip.unequip_before_sell` |
| `InsufficientGoldException` | `"골드가 부족합니다 (필요: {0}G)"` | `exception.economy.insufficient_gold` |
| `InsufficientStaminaException` | `"스태미나가 부족합니다 (필요: {0} SP)"` | `exception.vital.insufficient_stamina` |
| `InventoryFullException` | `"인벤토리가 가득 찼습니다 (최대 {0}칸)"` | `system.inventory.full` |
| `DungeonNotImplementedException` | `"해당 던전은 아직 준비 중입니다."` | `exception.dungeon.not_ready` |

#### ❌ 외부화 제외: 개발자 디버깅/불변식 검증 예외 (Java 코드 유지)
- JSON 파싱 검증 (`ItemDataException`, `MonsterDataException`, `DungeonDataException` 등)
- 불변식 검증 방어 코드 (`IllegalArgumentException("골드 획득량은 양수여야 합니다")`, `IllegalStateException`)
- **이유**: 개발자 오류 추적용이며 다국어나 기획 튜닝 대상이 아니므로 소스 코드 인라인 유지가 유지보수에 유리함.

---

### 2.6. 프론트엔드 JavaScript 하드코딩 (`myrpg.js`) 및 서버 SSOT 통합 — 23건

프론트엔드 알림 중 서버와 동일한 의미를 갖는 메시지들은 **서버 프로퍼티 키와 1:1로 매핑하여 일원화**합니다.

| # | 위치 (`myrpg.js`) | 현재 JS 하드코딩 문구 | 서버 통합 키 |
|---|---|---|---|
| 1 | L311 | `'무기 스왑 중 오류가 발생했습니다.'` | `system.error.weapon_swap` |
| 2 | L332 | `"전투 중에는 이동할 수 없습니다."` | `system.move_blocked` |
| 3 | L400 | `"매복하고 있던 " + monsterName + "이(가) 기습해옵니다!"` | `battle.ambush` |
| 4 | L611 | `kindLabel + "이(가) 부족합니다."` | `system.resource_lack` |
| 5 | L841 | `monsterName + "이(가) 쓰러졌습니다!"` | `battle.monster_slain` |
| 6 | L844 | `"정신을 잃고 쓰러졌습니다… 티르코네일에서 되살아납니다."` | `battle.death` |
| 7 | L847 | `"도망 성공!"` | `battle.flee.success` |
| 8 | L888, L890 | `"추후 설계 예정입니다."`, `"구현 예정입니다"` | `system.notice.coming_soon` |
| 9 | L903 | `'골드가 부족합니다.'` | `exception.economy.insufficient_gold` |
| 10 | L912 | `"치료되었습니다!"` | `town.heal.success` |
| 11 | L966, L989 | `'구매할 수 없습니다.'`, `'판매할 수 없습니다.'` | `shop.cannot_buy`, `shop.cannot_sell` |
| 12 | L1025 | `'수리할 수 없습니다.'` | `town.repair.cannot_repair` |
| 13 | L1039 | `'🔨 수리 성공!'` | `town.repair.success` |
| 14 | L1041 | `'💥 수리 실패 (최대 내구도 1 감소)'` | `town.repair.failure` |
| 15 | L1081 | `'착용 할 수 없습니다'` | `exception.equip.cannot_equip` |
| 16 | L1346 | `'1 이상의 금액을 입력해주세요.'` | `bank.invalid_amount` |
| 17 | L1681 | `'1부터 10 사이의 슬롯 번호를 입력해 주세요.'` | `skill.invalid_slot` |
| 18 | L1811 | `"스태미나가 부족합니다 (필요: 5 SP)"` | `exception.vital.insufficient_stamina` |
| 19 | L1846, L1886 | `"채집 실패"`, `"채집에 실패했습니다."` | `log.gathering.failure` |
| 20 | L1859 | `"채집 중 오류가 발생했습니다."` | `system.error.gathering` |
| 21 | L1889 | `"🪵 장작을 획득했습니다!"` | `log.gathering.success` |

---

### 2.7. 게임 밸런스 수치 상수 (Magic Numbers) — 총 50여건

주요 게임 밸런스 수치들을 기획자 튜닝 빈도 및 도메인 성격에 맞게 분류합니다.

| 도메인 그룹 | 수치 항목 | 현재 선언 위치 | 현재 값 | 외부화 설정 키 (`application-game.yml`) |
|---|---|---|---|---|
| **채집 (Gathering)** | 장작 채집 성공률 | `GatheringService` | `50%` | `game.gathering.woodcut-success-rate` |
| | 나무 스폰 확률 | `GatheringService` | `50%` | `game.gathering.woodcut-spawn-rate` |
| | 채집 스태미나 소모량 | `GatheringService` | `5 SP` | `game.gathering.woodcut-stamina-cost` |
| **인벤토리 (Inventory)** | 인벤토리 최대 슬롯 | `InventoryService` | `30` | `game.inventory.max-slots` |
| | 기본 지급 포션 수량 | `InventoryService` | `5` | `game.inventory.default-potion-qty` |
| | 장비 기본 최대 내구도 | `InventoryService` | `20` | `game.inventory.equipment-max-durability` |
| **마을 편의 (Town)** | 성당 치료 비용 | `HealController` | `100G` | `game.town.heal-cost` |
| | 대장간 수리 성공률 | `RepairController` | `95%` | `game.town.repair-success-rate` |
| | 대장간 1회 수리량 | `RepairController` | `1` | `game.town.repair-amount` |
| **전투 (Battle)** | 도망 성공 확률 | `BattleService` | `50%` | `game.battle.flee-success-rate` |
| | 필드 기습 확률 | `MonsterEncounterService` | `5%` | `game.battle.ambush-rate` |
| | 마법 캐스팅 실패율 | `BattleService` | `10%` | `game.battle.magic-fail-rate` |
| | 타격당 내구도 소모 | `BattleService` | `0.05` | `game.battle.durability-per-attack` |
| | 근접 / 궁술 / 마법 계수 | `BattleService` | `1.0 / 0.85 / 1.2` | `game.battle.melee/archery/magic-coef` |
| | 크리티컬 배율 | `BattleResolver` | `1.5x` | `game.battle.critical-multiplier` |
| | 몬스터 일반/강공격 배율 | `BattleService` | `100% / 150%` | `game.battle.monster-normal/heavy-multiplier` |
| | 몬스터 AI 가중치 (일반/강/방어) | `MonsterAiService` | `34 / 33 / 33` | `game.battle.ai-weights` |
| **성장 (Progression)** | 최대 도달 레벨 | `ProgressionService` | `100` | `game.progression.max-level` |
| | 사망 시 경험치 패널티 | `ProgressionService` | `10%` | `game.progression.death-penalty-rate` |
| **이동 (Movement)** | 필드 이동 소요 시간 | `MovementService` | `15분` | `game.movement.world-move-minutes` |
| | 던전 이동 소요 시간 | `DungeonService` | `5분` | `game.movement.dungeon-move-minutes` |

---

## 3. 리팩토링 아키텍처 설계안 (Externalization Strategy)

### 3.1. 통합 메시지 프로퍼티 (`messages.properties`)

Spring Boot 표준 `MessageSource`를 기반으로 하며 중복을 제거한 표준 포맷입니다.

```properties
# ============================================================
# 1. 시스템 & 공통 알림 (System & Common)
# ============================================================
system.move_blocked=전투 중에는 이동할 수 없습니다.
system.resource_lack={0}이(가) 부족합니다.
system.inventory.full=인벤토리가 가득 찼습니다 (최대 {0}칸)
system.notice.coming_soon=추후 구현 예정입니다.
system.error.weapon_swap=무기 스왑 중 오류가 발생했습니다.
system.error.gathering=채집 중 오류가 발생했습니다.

# ============================================================
# 2. 활동 로그 (Action Log)
# ============================================================
log.gathering.success=[채집] {0} 획득!
log.gathering.failure=[채집] 채집에 실패했습니다.
log.shop.buy=아이템을 구매했습니다: {0}
log.shop.sell=아이템을 판매했습니다: {0}
log.potion.use=포션 사용: {0}
log.item.acquire_fail={0} 획득 실패!
log.item.durability_broken={0} 내구도 0 — 장착 해제됨
log.growth.levelup=🎉 레벨업! Lv.{0} 달성! (AP +{1})
log.growth.skill_rankup=✨ [{0}] {1}랭크로 승급되었습니다!
log.combat.encounter=⚔️ {0} 조우!
log.combat.ultimate_cooldown=궁극기 쿨타임 대기 중입니다. ({0}승 남음)
log.dungeon.enter={0}에 입장했습니다.
log.dungeon.exit=던전에서 나왔습니다.
log.dungeon.clear={0}을(를) 완전히 정복했습니다!
log.dungeon.clear_reward=던전 클리어 보상: EXP +{0}, Gold +{1}G
log.dungeon.item_reward=보상 획득: {0} x{1}
log.system.rebirth_done=환생했습니다 (재능: {0})
log.system.rebirth_wait=환생까지 {0}시간 {1}분 남았습니다
log.cheat.exp_levelup=테스트 치트: 1,000 EXP를 획득했습니다! (Lv.{0} 달성, AP +{1})
log.cheat.exp=테스트 치트: 1,000 EXP를 획득했습니다!
log.cheat.gold=테스트 치트: 1,000 Gold를 획득했습니다!

# ============================================================
# 3. 전투 인라인 & 상태이상 로그 (Battle Inline)
# ============================================================
battle.ambush=🚨 {0} 기습!
battle.flee.success=도망쳤다!
battle.flee.fail=⚠️ [도망 실패] {0}에게 저지당해 {1} 피해
battle.death=쓰러졌다… 티르코네일에서 부활 (경험치 -{0})
battle.monster_slain={0}이(가) 쓰러졌습니다!
battle.victory_reward=승리! EXP +{0} | Gold +{1}
battle.cast_fail=🔮 [{0}] 캐스팅 실패! (집중이 흐트러짐)
battle.recovery=💖 [{0}] HP +{1} 회복
battle.cc.success=⛓️ [{0}] {1} 기절 성공! (1턴)
battle.cc.resist=⛓️ [{0}] 저항으로 제어 효과 실패
battle.stun_frozen=❄️ [{0}] 빙결/기절 상태로 행동 불가
battle.freeze=❄️ [빙결] {0} 꽁꽁 얼어붙음! (1턴 행동 불가)
battle.rage=💢 [레이지 임팩트] 다음 물리 피해 +30% 증폭
battle.dot=🩸 [지속 피해] {0} {1} 도트 피해
battle.meditation=🧘 [메디테이션] MP +{0} 회복
battle.timeout.preemptive=⏳ [시간 초과] 선제 공격 기회 상실
battle.timeout.hit=⏳ [시간 초과] 무방비 피격

# ============================================================
# 4. 전투 턴 공방 템플릿 (Battle Turn Log)
# ============================================================
battle.attack.multi={0} [{1}] {2}연타 ({3}) ➔ 총 {4} 피해
battle.attack.single={0} [{1}]{2} ➔ {3}에게 {4} 피해
battle.attack.ultimate_multi=👑 [결전 궁극기] [{0}] {1}연타 ({2}) ➔ 총 {3} 관통 피해
battle.attack.ultimate_single=👑 [결전 궁극기] [{0}]{1} 100% 관통 ➔ {2} 피해
battle.turn.block_perfect=⚔️ [{0}] 🛡️ {1}의 완전 방어에 가로막힘 (0 피해)
battle.turn.vs_defense_multi=⚔️ [{0}] {1}연타 ({2}) 🛡️ 적 방어에 막힘 ➔ 총 {3} 피해
battle.turn.vs_defense_single=⚔️ [{0}]{1} 🛡️ 적 방어에 막힘 ➔ {2} 피해
battle.turn.counter_crit=⚡ [{0}] 💥 적 공격을 흘려내며 ➔ {1} 치명 반격!
battle.turn.defense_success=🛡️ [{0}] 방어 성공 & 반격! ➔ {1}에게 {2} 반격 피해
battle.turn.defense_window=🛡️ [{0}] 완벽 방어! ➔ 빈틈 포착 (다음 턴 선제 찬스⚡)
battle.turn.defense_penetrated=⚠️ [{0}] 몬스터 강공격에 방어선 관통!
battle.turn.defense_stalemate=🛡️ [{0}] 맞방어 교착 상태
battle.turn.miss={0} [{1}] 빗나감 (0 피해)
battle.monster.hit=[{0}] {1} ➔ {2} 피해 피격
battle.monster.blocked=[{0}] {1} ➔ 🛡️ 방어로 경감되어 {2} 피해
battle.monster.miss=[{0}] {1} ➔ 빗나감
battle.monster.defense_hold=[{0}] 🛡️ 방어 태세 유지
battle.monster.defense_counter=[{0}] 🛡️ 방어 성공 & 반격 ➔ {1} 피해 피격
battle.monster.defense_alert=[{0}] 🛡️ 공격 방어 성공 ➔ 반격 태세 (다음 턴 선제 주의⚠️)
battle.monster.defense_break=[{0}] 💥 방어선 관통됨!
battle.monster.defense_full=[{0}] 🛡️ 완전 방어 (0 피해)

# ============================================================
# 5. 마을 및 상점/장비 상세 (Town & Shop & Inventory Describe)
# ============================================================
town.heal.success=치료되었습니다!
town.repair.success=🔨 수리 성공!
town.repair.failure=💥 수리 실패 (최대 내구도 1 감소)
town.repair.cannot_repair=수리할 수 없습니다.
shop.cannot_buy=구매할 수 없습니다.
shop.cannot_sell=판매할 수 없습니다.
describe.equip.kind_type={0} ({1})
describe.equip.durability=내구도: {0}/{1}

# ============================================================
# 6. 유저 노출 비즈니스 예외 (Exceptions)
# ============================================================
exception.equip.shield_conflict=방패와 함께 착용할 수 없습니다.
exception.equip.unequip_before_sell=장착을 해제한 후 판매할 수 있습니다.
exception.equip.cannot_equip=착용할 수 없습니다.
exception.economy.insufficient_gold=골드가 부족합니다 (필요: {0}G)
exception.vital.insufficient_stamina=스태미나가 부족합니다 (필요: {0} SP)
exception.dungeon.not_ready=해당 던전은 아직 준비 중입니다.
bank.invalid_amount=1 이상의 금액을 입력해주세요.
skill.invalid_slot=1부터 10 사이의 슬롯 번호를 입력해 주세요.
```

---

### 3.2. 메시지 헬퍼 서비스 (`GameMessageService.java`)

```java
package com.myapps.web.myrpg.support;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class GameMessageService {

    private final MessageSource messageSource;

    public GameMessageService(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * 프로퍼티 키와 가변 인자를 받아 한국어 로케일로 포맷팅된 메시지를 반환합니다.
     */
    public String get(final String code, final Object... args) {
        return messageSource.getMessage(code, args, Locale.KOREAN);
    }
}
```

---

### 3.3. 게임 밸런스 설정 (`GameProperties.java` & `application-game.yml`)

불필요한 중첩 래핑을 플랫화하고 관련 도메인(마을 편의 기능 등)을 묶어 깔끔하게 정의합니다.

```yaml
# src/main/resources/application-game.yml
game:
  gathering:
    woodcut-spawn-rate: 50
    woodcut-success-rate: 50
    woodcut-stamina-cost: 5
  inventory:
    max-slots: 30
    default-potion-qty: 5
    equipment-max-durability: 20
  town:
    heal-cost: 100
    repair-success-rate: 95
    repair-amount: 1
  battle:
    flee-success-rate: 50
    ambush-rate: 5
    magic-fail-rate: 10
    durability-per-attack: 0.05
    melee-coef: 1.0
    archery-coef: 0.85
    magic-coef: 1.2
    critical-multiplier: 1.5
    monster-normal-multiplier: 100
    monster-heavy-multiplier: 150
    ai-normal-weight: 34
    ai-heavy-weight: 33
    ai-defense-weight: 33
  progression:
    max-level: 100
    death-penalty-rate: 0.10
  movement:
    world-move-minutes: 15
    dungeon-move-minutes: 5
```

```java
package com.myapps.web.myrpg.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "game")
public record GameProperties(
        GatheringProperties gathering,
        InventoryProperties inventory,
        TownProperties town,
        BattleProperties battle,
        ProgressionProperties progression,
        MovementProperties movement) {

    public record GatheringProperties(
            int woodcutSpawnRate,
            int woodcutSuccessRate,
            int woodcutStaminaCost) {}

    public record InventoryProperties(
            int maxSlots,
            int defaultPotionQty,
            int equipmentMaxDurability) {}

    public record TownProperties(
            int healCost,
            int repairSuccessRate,
            int repairAmount) {}

    public record BattleProperties(
            int fleeSuccessRate,
            int ambushRate,
            int magicFailRate,
            double durabilityPerAttack,
            double meleeCoef,
            double archeryCoef,
            double magicCoef,
            double criticalMultiplier,
            int monsterNormalMultiplier,
            int monsterHeavyMultiplier,
            int aiNormalWeight,
            int aiHeavyWeight,
            int aiDefenseWeight) {}

    public record ProgressionProperties(
            int maxLevel,
            double deathPenaltyRate) {}

    public record MovementProperties(
            int worldMoveMinutes,
            int dungeonMoveMinutes) {}
}
```

---

## 4. 점진적 마이그레이션 로드맵 (3대 핵심 Phase)

과도하게 세분화된 단계를 3개의 응집력 있는 Phase로 압축하여 빌드 및 검증 오버헤드를 최소화합니다.

```
┌──────────────────────────────────────────────────────────┐
│ [Phase 1] 서버 텍스트 & 로그 외부화 통합 (Messages SSOT)  │
│ - messages.properties + GameMessageService 인프라 구축   │
│ - ActionLog(26건) + 전투 로그(35건) + 유저 예외(8건) 일괄 이관│
└──────────────────────────┬───────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────┐
│ [Phase 2] 게임 밸런스 설정 외부화 (GameProperties)        │
│ - application-game.yml + GameProperties 바인딩          │
│ - 분산된 static final 매직 넘버(50여건) 설정 주입으로 전환│
└──────────────────────────┬───────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────┐
│ [Phase 3] 프론트엔드 메시지 SSOT 동기화 (Client Sync)     │
│ - myrpg.js 하드코딩 문구를 서버 프로퍼티 키와 일치화     │
│ - Thymeleaf 주입 또는 /api/messages JSON 엔드포인트 연동 │
└──────────────────────────────────────────────────────────┘
```

| 단계 | 주요 작업 내용 | 대상 건수 | 검증 기준 |
|---|---|---|---|
| **Phase 1: 서버 텍스트 & 로그 외부화 통합** | • `messages.properties` 및 `GameMessageService` 빈 생성<br>• `ActionLog`(26건), `BattleService` 인라인 로그(15건), `BattleLogFormatter` 턴 로그(~20건), 장비 포맷팅/유저 예외(8건) 이관 | 약 70건 | 단위 테스트 및 기존 공방 로그 포맷 일치 검증, 5대 가드레일 통과 |
| **Phase 2: 게임 밸런스 설정 외부화** | • `application-game.yml` 및 `GameProperties` 레코드 생성<br>• `GatheringService`, `InventoryService`, `BattleService`, `Heal/RepairController` 등의 상수를 프로퍼티 주입으로 교체 | 약 50건 | 설정 파일 값 변경 시 인게임 확률/수치 즉시 반영 검증 |
| **Phase 3: 프론트엔드 메시지 SSOT 동기화** | • `myrpg.js` 내 alert/toast 문구를 서버 메시지와 동기화<br>• 클라이언트 공통 다국어/메시지 리졸버 스크립트 연동 | 23건 | 클라이언트 알림 팝업 및 토스트 메시지 정상 렌더링 검증 |

---

## 5. 총 하드코딩 현황 요약 (정합성 보정 및 최적화 완료)

| 영역 | 이전 분석 | 최적화 후 건수 | 외부화 대상 파일 | 비고 |
|---|---|---|---|---|
| **활동 로그 (`ActionLog`)** | 29건 (표기 오류) | **26건** | `messages.properties` | 건수 정합성 보정 완료 |
| **전투 인라인 로그 (`BattleService`)** | 14건 (표기 오류) | **15건** | `messages.properties` | 건수 정합성 보정 완료 |
| **전투 턴 로그 (`BattleLogFormatter`)** | ~40건 | **~20건** | `messages.properties` | 공통 템플릿 통합으로 50% 슬림화 |
| **장비 상세 팝업 (`InventoryService.describe`)** | 3건 | **3건** | `messages.properties` | 장비 포맷팅 문구 분리 |
| **유저 노출 비즈니스 예외** | 50건+ (과다) | **8건** | `messages.properties` | 개발자 디버깅 예외 제외, 유저 노출분만 엄선 |
| **프론트엔드 JS alert/toast (`myrpg.js`)** | 23건 | **23건** | 서버 메시지와 SSOT 단일화 | 서버 키와 1:1 매핑 |
| **게임 밸런스 수치 상수** | 60건+ | **~50건** | `application-game.yml` | 도메인별 record 플랫화 |
| **총계** | ~220건 | **~145건** | — | **중복 제거 및 최적화로 관리 비용 35% 절감** |

---

## 6. 멘트 통합 및 변경 상세 비교표 (AS-IS ➔ TO-BE)

### 6.1. 서버(Java) ↔ 프론트엔드(JS) 중복 멘트 단일화

동일한 상황에서 서버 로그와 JS 브라우저 알림창이 각각 다른 문구로 하드코딩되어 있던 것을 단일 프로퍼티 키로 일원화합니다.

| 분류 / 상황 | AS-IS: Java 서버 코드 | AS-IS: JS 프론트 (`myrpg.js`) | TO-BE: 통합 키 & 단일화된 멘트 (`messages.properties`) |
|---|---|---|---|
| **전투 중 이동 불가** | `"전투 중에는 이동할 수 없습니다."` | `"전투 중에는 이동할 수 없습니다."` | `system.move_blocked`<br>➔ **`전투 중에는 이동할 수 없습니다.`** |
| **자원(HP/MP/SP) 부족** | `resourceKind.label() + "이(가) 부족합니다."` | `kindLabel + "이(가) 부족합니다."` | `system.resource_lack`<br>➔ **`{0}이(가) 부족합니다.`** |
| **몬스터 기습/조우** | `"🚨 " + monster.name() + " 기습!"` | `"매복하고 있던 " + name + "이(가) 기습해옵니다!"` | `battle.ambush`<br>➔ **`🚨 {0} 기습!`** |
| **도망 성공** | `"도망쳤다!"` | `"도망 성공!"` | `battle.flee.success`<br>➔ **`도망쳤다!`** |
| **사망 및 부활** | `"쓰러졌다… 티르코네일에서 부활 (경험치 -" + exp + ")"` | `"정신을 잃고 쓰러졌습니다… 되살아납니다."` | `battle.death`<br>➔ **`쓰러졌다… 티르코네일에서 부활 (경험치 -{0})`** |
| **몬스터 처치** | (인라인 승리 요약 로그) | `monsterName + "이(가) 쓰러졌습니다!"` | `battle.monster_slain`<br>➔ **`{0}이(가) 쓰러졌습니다!`** |
| **스태미나 부족** | (서비스 내부 체크) | `"스태미나가 부족합니다 (필요: 5 SP)"` | `exception.vital.insufficient_stamina`<br>➔ **`스태미나가 부족합니다 (필요: {0} SP)`** |
| **골드 부족** | `"골드 부족: 소모 요청 " + amount` | `'골드가 부족합니다.'` | `exception.economy.insufficient_gold`<br>➔ **`골드가 부족합니다 (필요: {0}G)`** |
| **성당 치료 완료** | (ActionLog 미기록) | `"치료되었습니다!"` | `town.heal.success`<br>➔ **`치료되었습니다!`** |
| **대장간 수리 결과** | (ActionLog 미기록) | `'🔨 수리 성공!'` / `'💥 수리 실패'` | `town.repair.success`<br>➔ **`🔨 수리 성공!`**<br>`town.repair.failure`<br>➔ **`💥 수리 실패 (최대 내구도 1 감소)`** |
| **인벤토리 가득 참** | `"인벤토리가 가득 찼습니다"` | `'인벤토리가 가득 찼습니다'` | `system.inventory.full`<br>➔ **`인벤토리가 가득 찼습니다 (최대 {0}칸)`** |

---

### 6.2. 특정 도구/아이템 종속 멘트 ➔ 범용 확장 템플릿화

신규 채집물(버섯, 약초, 광석 등) 확장에 대비하여 특정 아이템/도구에 고정된 멘트를 범용 템플릿으로 치환합니다.

| 상황 | AS-IS (하드코딩) | TO-BE (범용 외부화 템플릿) | 비고 |
|---|---|---|---|
| **채집 성공** | `"[채집] 🪵 단단한 장작을 1개 얻었습니다!"`<br>(JS: `"🪵 장작을 획득했습니다!"`) | `log.gathering.success=[채집] {0} 획득!` | 장작 외에도 버섯, 약초, 철광석 등 카탈로그 아이템명 자동 바인딩 |
| **채집 실패** | `"[채집] 💨 헛도끼질을 하여 장작을 얻지 못했습니다."`<br>(JS: `"채집 실패"`) | `log.gathering.failure=[채집] 채집에 실패했습니다.` | 도끼/장작에 종속되지 않는 범용 피드백 |
| **아이템 획득 실패** | `itemName + " 획득 실패!"` | `log.item.acquire_fail={0} 획득 실패!` | 인벤토리 가득 참 등으로 인한 실패 공통화 |
| **장비 내구도 소진** | `itemName + " 내구도 0 — 장착 해제됨"` | `log.item.durability_broken={0} 내구도 0 — 장착 해제됨` | 내구도 0 자동 탈착 공통 알림 |

---

### 6.3. 전투 턴 공방 템플릿 압축 및 통합 (~40건 ➔ ~20건)

문장 구조가 동일하고 앞머리 태그(일반 타격 `⚔️`, 선제 공격 `⚡ [선제 공격]`)만 다른 수십 개의 턴 로그를 파라미터형 공통 템플릿으로 통합합니다.

| 전투 액션 분류 | AS-IS (분산 하드코딩 예시) | TO-BE (통합 프로퍼티 템플릿) |
|---|---|---|
| **다단 히트 공격** | • `"⚔️ " + tag + " 2연타 (10, 10) ➔ 총 20 피해"`<br>• `"⚡ [선제 공격] " + tag + " 2연타 (10, 10) ➔ 총 20 피해"` | `battle.attack.multi={0} [{1}] {2}연타 ({3}) ➔ 총 {4} 피해`<br>*(태그 `{0}`에 `"⚔️"` 또는 `"⚡ [선제 공격]"` 주입)* |
| **단일 타격 공격** | • `"⚔️ " + tag + " ➔ 고블린에게 15 피해"`<br>• `"⚡ [선제 공격] " + tag + " ➔ 고블린에게 15 피해"` | `battle.attack.single={0} [{1}]{2} ➔ {3}에게 {4} 피해` |
| **결전 궁극기** | • `"👑 [결전 궁극기] " + tag + " 3연타 ➔ 총 90 관통 피해"`<br>• `"👑 [결전 궁극기] " + tag + " 100% 관통 ➔ 50 피해"` | `battle.attack.ultimate_multi=👑 [결전 궁극기] [{0}] {1}연타 ({2}) ➔ 총 {3} 관통 피해`<br>`battle.attack.ultimate_single=👑 [결전 궁극기] [{0}]{1} 100% 관통 ➔ {2} 피해` |
| **적 방어에 막힘** | • `"⚔️ " + tag + " 2연타 🛡️ 적 방어에 막힘 ➔ 총 10 피해"`<br>• `"⚔️ " + tag + " 🛡️ 적 방어에 막힘 ➔ 5 피해"` | `battle.turn.vs_defense_multi=⚔️ [{0}] {1}연타 ({2}) 🛡️ 적 방어에 막힘 ➔ 총 {3} 피해`<br>`battle.turn.vs_defense_single=⚔️ [{0}]{1} 🛡️ 적 방어에 막힘 ➔ {2} 피해` |
| **빗나감 (Miss)** | • `"🗡️ " + tag + " 빗나감 (0 피해)"`<br>• `"⚡ " + tag + " 적이 공격하지 않아 빗나감"` | `battle.turn.miss={0} [{1}] 빗나감 (0 피해)` |
| **반격 성공** | • `"⚡ " + tag + " 💥 적 공격을 흘려내며 ➔ 25 치명 반격!"`<br>• `"🛡️ " + tag + " 방어 성공 & 반격! ➔ 10 반격 피해"` | `battle.turn.counter_crit=⚡ [{0}] 💥 적 공격을 흘려내며 ➔ {1} 치명 반격!`<br>`battle.turn.defense_success=🛡️ [{0}] 방어 성공 & 반격! ➔ {1}에게 {2} 반격 피해` |
| **몬스터 피격/방어** | • `"[" + name + "] " + action + " ➔ 10 피해 피격"`<br>• `"[" + name + "] 🛡️ 방어 태세 유지"`<br>• `"[" + name + "] 🛡️ 완전 방어 (0 피해)"` | `battle.monster.hit=[{0}] {1} ➔ {2} 피해 피격`<br>`battle.monster.defense_hold=[{0}] 🛡️ 방어 태세 유지`<br>`battle.monster.defense_full=[{0}] 🛡️ 완전 방어 (0 피해)` |

---

### 6.4. 활동 로그 및 성장 멘트 (ActionLog)

| 이벤트 | AS-IS (하드코딩 코드) | TO-BE (`messages.properties`) |
|---|---|---|
| **상점 구매** | `"아이템을 구매했습니다: " + item.name()` | `log.shop.buy=아이템을 구매했습니다: {0}` |
| **상점 판매** | `"아이템을 판매했습니다: " + item.name()` | `log.shop.sell=아이템을 판매했습니다: {0}` |
| **포션 사용** | `"포션 사용: " + potion.name()` | `log.potion.use=포션 사용: {0}` |
| **레벨업** | `"🎉 레벨업! Lv." + lv + " 달성! (AP +" + ap + ")"` | `log.growth.levelup=🎉 레벨업! Lv.{0} 달성! (AP +{1})` |
| **스킬 승급** | `"✨ [" + label + "] " + rank + "랭크로 승급되었습니다!"` | `log.growth.skill_rankup=✨ [{0}] {1}랭크로 승급되었습니다!` |
| **던전 입장/퇴장** | `spec.name() + "에 입장했습니다."` / `"던전에서 나왔습니다."` | `log.dungeon.enter={0}에 입장했습니다.`<br>`log.dungeon.exit=던전에서 나왔습니다.` |
| **던전 정복/보상** | `spec.name() + "을(를) 완전히 정복했습니다!"`<br>`"던전 클리어 보상: EXP +" + exp + ", Gold +" + gold + "G"` | `log.dungeon.clear={0}을(를) 완전히 정복했습니다!`<br>`log.dungeon.clear_reward=던전 클리어 보상: EXP +{0}, Gold +{1}G`<br>`log.dungeon.item_reward=보상 획득: {0} x{1}` |
| **환생** | `"환생했습니다 (재능: " + talent + ")"`<br>`"환생까지 " + h + "시간 " + m + "분 남았습니다"` | `log.system.rebirth_done=환생했습니다 (재능: {0})`<br>`log.system.rebirth_wait=환생까지 {0}시간 {1}분 남았습니다` |
| **치트 (테스트용)** | `"테스트 치트: 1,000 EXP를 획득했습니다!"` | `log.cheat.exp_levelup=테스트 치트: 1,000 EXP를 획득했습니다! (Lv.{0} 달성, AP +{1})`<br>`log.cheat.exp=테스트 치트: 1,000 EXP를 획득했습니다!`<br>`log.cheat.gold=테스트 치트: 1,000 Gold를 획득했습니다!` |
