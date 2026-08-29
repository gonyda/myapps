# MyRPG 하드코딩 전수 분석 및 메시지·로그 외부화 리팩토링 계획서

> **문서 위치**: `docs/hardcoding-analysis-and-refactoring-plan.md`  
> **작성 일자**: 2026-08-29  
> **최종 검증**: 2026-08-29 18:26 (전체 소스 재검사 완료)  
> **대상 모듈**: `com.myapps.web.myrpg` (Spring Boot 4.0 / Java 21)

---

## 1. 개요 (Executive Summary)

현재 MyRPG는 아이템 카탈로그(`item.json`), 몬스터(`monster.json`), 스킬(`skill.json`), 맵(`map.json`), NPC 시간대별 대사(`ambience_dialogue.json`) 등 주요 게임 콘텐츠를 JSON 데이터 파일로 성공적으로 분리하여 관리하고 있습니다.

그러나 **하단 활동 로그(ActionLog)**, **전투 공방 로그(BattleLogFormatter + BattleService 인라인)**, **시스템 예외 및 클라이언트 토스트/alert 메시지**, **게임 밸런스 상수(확률, 스태미나 소모량, 슬롯 제한, 전투 계수 등)**, **인벤토리 상세 팝업(InventoryService.describe) 장비 포맷팅 문구**가 여전히 Java/JS 소스 코드 내에 문자열 및 매직 넘버(Magic Numbers)로 하드코딩되어 있습니다.

본 문서는 소스 코드 내에 잔존하는 하드코딩 요소들을 **전수 분석(정합성 검증 포함)**하고, 이를 **`messages.properties` 및 `application.yml`로 깔끔하게 분리·외부화(Externalization)**하기 위한 아키텍처 설계와 점진적 리팩토링 로드맵을 제시합니다.

---

## 2. 현 소스코드 내 하드코딩 영역 전수 분석

### 2.1. 하단 활동 로그 (ActionLog) 문자열 — 총 29건

8개 서비스/컨트롤러에서 `actionLog.add("...", LOG_TYPE)` 형태로 하드코딩된 한국어 메시지를 직접 적재하고 있습니다.

| # | 도메인/서비스 | 로그 유형 | 하드코딩 위치 | 현재 하드코딩된 코드 |
|---|---|---|---|---|
| 1 | `GatheringService` | `ITEM` | L191 | `"[채집] 🪵 단단한 장작을 1개 얻었습니다!"` → 향후 버섯/약초 등 채집물 확장을 고려하여 `"[채집] {아이템명} 획득!"` 범용 공통 멘트로 통일 |
| 2 | `GatheringService` | `SYSTEM` | L195 | `"[채집] 💨 헛도끼질을 하여 장작을 얻지 못했습니다."` → 특정 도구에 종속되지 않는 `"[채집] 채집에 실패했습니다."` 범용 멘트로 통일 |
| 3 | `ShopService` | `ITEM` | L338 | `"아이템을 구매했습니다: " + item.name()` |
| 4 | `ShopService` | `ITEM` | L381 | `"아이템을 판매했습니다: " + catalogItem.name()` |
| 5 | `InventoryService` | `ITEM` | L1445 | `itemName + " 획득 실패!"` |
| 6 | `InventoryService` | `ITEM` | L1469 | `itemName + " 획득 실패!"` |
| 7 | `InventoryService` | `ITEM` | L1574 | `itemName + " 내구도 0 — 장착 해제됨"` |
| 8 | `InventoryController` | `ITEM` | L96 | `"포션 사용: " + potion.name()` |
| 9 | `DungeonService` | `DUNGEON` | L137 | `spec.name() + "에 입장했습니다."` |
| 10 | `DungeonService` | `DUNGEON` | L166 | `"던전에서 나왔습니다."` |
| 11 | `DungeonService` | `DUNGEON` | L288 | `spec.name() + "을(를) 완전히 정복했습니다!"` |
| 12 | `DungeonService` | `DUNGEON` | L289-290 | `"던전 클리어 보상: EXP +" + exp + ", Gold +" + gold + "G"` |
| 13 | `DungeonService` | `DUNGEON` | L301 | `"보상 획득: " + itemName + " x" + qty` |
| 14 | `BattleService` | `COMBAT` | L281-283 | `"궁극기 쿨타임 대기 중입니다. (" + cooldown + "승 남음)"` |
| 15 | `BattleService` | `COMBAT` | L314 | `resourceKind.label() + "이(가) 부족합니다."` |
| 16 | `BattleService` | `GROWTH` | L1188-1194 | `"🎉 레벨업! Lv." + level + " 달성! (AP +" + ap + ")"` |
| 17 | `BattleService` | `COMBAT` | L1347 | `"도망쳤다!"` |
| 18 | `BattleController` | `COMBAT` | L112 | `"⚔️ " + monster.name() + " 조우!"` |
| 19 | `PlayScreenController` | `NOTIFICATION` | L213 | `"전투 중에는 이동할 수 없습니다."` |
| 20 | `PlayScreenController` | `COMBAT` | L243 | `"🚨 " + monster.name() + " 기습!"` |
| 21 | `PlayScreenController` | `NOTIFICATION` | L344 | `"환생했습니다 (재능: " + talent.label() + ")"` |
| 22 | `PlayScreenController` | `NOTIFICATION` | L349 | `"환생까지 " + hours + "시간 " + minutes + "분 남았습니다"` |
| 23 | `PlayScreenController` | `NOTIFICATION` | L372-377 | `"테스트 치트: 1,000 EXP를 획득했습니다! (Lv." + lv + " 달성, AP +" + ap + ")"` |
| 24 | `PlayScreenController` | `NOTIFICATION` | L380 | `"테스트 치트: 1,000 EXP를 획득했습니다!"` |
| 25 | `PlayScreenController` | `NOTIFICATION` | L400 | `"테스트 치트: 1,000 Gold를 획득했습니다!"` |
| 26 | `SkillController` | `GROWTH` | L134-140 | `"✨ [" + label + "] " + rank + "랭크로 승급되었습니다!"` |

---

### 2.2. 전투 공방 인라인 로그 (`BattleService.java` 내 `combatLines.add`) — 총 14건 (신규 발견)

> ⚠️ **이전 문서 누락**: `BattleLogFormatter.java` 외에도 `BattleService.java` 자체에 전투 인라인 로그가 **14건** 추가로 존재합니다.

| # | 위치 (BattleService.java) | 하드코딩된 코드 |
|---|---|---|
| 1 | L659 | `"🔮 [" + skill.label() + "] 캐스팅 실패! (집중이 흐트러짐)"` |
| 2 | L964 | `"💖 [" + recoverySkill.label() + "] HP +" + healed + " 회복"` |
| 3 | L1007 | `"⛓️ [" + ccSkill.label() + "] " + monster.name() + " 기절 성공! (1턴)"` |
| 4 | L1009 | `"⛓️ [" + ccSkill.label() + "] 저항으로 제어 효과 실패"` |
| 5 | L1028 | `"❄️ [" + monster.name() + "] 빙결/기절 상태로 행동 불가"` |
| 6 | L1052 | `"❄️ [빙결] " + monster.name() + " 꽁꽁 얼어붙음! (1턴 행동 불가)"` |
| 7 | L1075 | `"💢 [레이지 임팩트] 다음 물리 피해 +30% 증폭"` |
| 8 | L1141 | `"🩸 [지속 피해] " + monster.name() + " " + dotDmg + " 도트 피해"` |
| 9 | L1159 | `"🧘 [메디테이션] MP +" + regened + " 회복"` |
| 10 | L1175 | `"승리! EXP +" + exp + " | Gold +" + gold` (+ 아이템 드랍 요약) |
| 11 | L1267 | `"쓰러졌다… 티르코네일에서 부활 (경험치 -" + expLost + ")"` |
| 12 | L1281 | `"⏳ [시간 초과] 선제 공격 기회 상실"` |
| 13 | L1286 | `"⏳ [시간 초과] 무방비 피격"` |
| 14 | L1301 | `"[" + monster.name() + "] 🛡️ 방어 태세 유지"` |
| 15 | L1384 | `"⚠️ [도망 실패] " + monster.name() + "에게 저지당해 " + dmg + " 피해"` |

---

### 2.3. 전투 턴 상세 로그 (`BattleLogFormatter.java`) — 약 40건

`BattleLogFormatter.java`는 약 300줄에 걸쳐 가위바위보 상성 공방 로그를 생성하며, **40여 개 이상의 한국어 텍스트 및 이모지 조합**이 소스에 직접 작성되어 있습니다.

```java
// 현재 BattleLogFormatter.java의 하드코딩 예시 (L44~L282)
"⚠️ [적 선제공격] [" + input.monsterName() + "] 기습" + ARROW + input.monsterDamage() + " 피해 피격"
"👑 [결전 궁극기] " + skillTag + " " + hits.size() + "연타 (" + formatHits(hits) + ")" + ARROW + "총 " + damage + " 관통 피해"
"⚔️ " + skillTag + " 🛡️ " + input.monsterName() + "의 완전 방어에 가로막힘 (0 피해)"
"⚡ " + skillTag + " 💥 적 공격을 흘려내며" + ARROW + damage + " 치명 반격!"
"🛡️ " + skillTag + " 완벽 방어!" + ARROW + "빈틈 포착 (다음 턴 선제 찬스⚡)"
"[" + monsterName + "] " + actionLabel + ARROW + "🛡️ 방어로 경감되어 " + damage + " 피해"
"[" + monsterName + "] 🛡️ 공격 방어 성공" + ARROW + "반격 태세 (다음 턴 선제 주의⚠️)"
"⚡ [선제 공격] 선제 찬스였으나 " + skillTag + " 태세 유지"
"🗡️ " + skillTag + " 빗나감 (0 피해)"
"⚔️ " + skillTag + " " + hits.size() + "연타 (" + formatHits(hits) + ")" + ARROW + "총 " + damage + " 피해"
"🛡️ " + skillTag + " 적의 공격을 완벽히 막아냄 (0 피해)"
"🛡️ " + skillTag + " 맞방어 교착 상태"
"⚡ " + skillTag + " 적이 공격하지 않아 빗나감"
"⚠️ " + skillTag + " 몬스터 강공격에 방어선 관통!"
"[" + monsterName + "] 🛡️ 완전 방어 (0 피해)"
```

---

### 2.4. 인벤토리 상세 팝업 (`InventoryService.describe`) 장비 포맷팅 — 3건 (신규 발견)

| # | 위치 (InventoryService.java) | 하드코딩된 코드 |
|---|---|---|
| 1 | L1131 | `equipItem.kind().label() + " (" + equipItem.type().label() + ")"` |
| 2 | L1139 | `"방패와 함께 착용할 수 없습니다."` |
| 3 | L1147 | `"내구도: " + formatDurability(current) + "/" + max` |

> `item.description()`으로 포션·재료를 이관한 것처럼, 장비의 고정 문구("방패와 함께 착용할 수 없습니다.", "내구도:" 등)도 외부 템플릿 대상.

---

### 2.5. 도메인 예외(Exception) 메시지 — 50건 이상

각종 예외 클래스에서 한국어/영어 메시지가 소스에 하드코딩되어 있습니다.

| 도메인 | 예외 클래스 | 예시 |
|---|---|---|
| **아이템 카탈로그** | `ItemDataException` | `"아이템 '" + id + "'의 필수 필드 'kind'가 누락되었습니다."` |
| **몬스터 카탈로그** | `MonsterDataException` | `"몬스터 '" + id + "'의 level은 1 이상이어야 합니다"` |
| **던전 카탈로그** | `DungeonDataException` | `"던전 JSON 파일 로딩 실패"`, `"던전 '알비'의 'boss' 설정이 누락"` |
| **NPC/맵** | `NpcDataException`, `MapDataException` | `"NPC JSON 파일 로딩 실패"` |
| **전투/경제** | `EquipConflictException` | `"장착을 해제한 후 판매할 수 있습니다."`, `"착용 할 수 없습니다."` |
| **경제** | `InsufficientGoldException` | `"골드 부족: 소모 요청 " + amount` |
| **도메인 방어** | `IllegalArgumentException` | `"골드 획득량은 양수여야 합니다"`, `"스킬 슬롯 번호는 0~9"` |
| **인프라** | `IllegalStateException` | `"던전 그래프 직렬화 실패"`, `"방 상태 역직렬화 실패"` |

> **외부화 우선순위**: 서버→클라이언트 응답에 노출되는 `EquipConflictException`, `InsufficientGoldException`, `DungeonNotImplementedException` 등의 **유저 노출 메시지**가 1순위. 순수 개발자용 방어 로직(`IllegalArgumentException`, JSON 파싱 에러)은 낮은 우선순위.

---

### 2.6. 프론트엔드 JavaScript 하드코딩 (`myrpg.js`) — 총 23건 (신규 발견)

| # | 위치 (myrpg.js) | 하드코딩된 코드 |
|---|---|---|
| 1 | L311 | `'무기 스왑 중 오류가 발생했습니다.'` |
| 2 | L332 | `"전투 중에는 이동할 수 없습니다."` |
| 3 | L400 | `"매복하고 있던 " + monsterName + "이(가) 기습해옵니다!"` |
| 4 | L611 | `kindLabel + "이(가) 부족합니다."` (자원 부족) |
| 5 | L841 | `monsterName + "이(가) 쓰러졌습니다!"` |
| 6 | L844 | `"정신을 잃고 쓰러졌습니다… 티르코네일에서 되살아납니다."` |
| 7 | L847 | `"도망 성공!"` |
| 8 | L888 | `"추후 설계 예정입니다."` |
| 9 | L890 | `"구현 예정입니다"` |
| 10 | L903 | `'골드가 부족합니다.'` |
| 11 | L912 | `"치료되었습니다!"` |
| 12 | L966 | `'구매할 수 없습니다.'` |
| 13 | L989 | `'판매할 수 없습니다.'` |
| 14 | L1025 | `'수리할 수 없습니다.'` |
| 15 | L1039 | `'🔨 수리 성공!'` |
| 16 | L1041 | `'💥 수리 실패 (최대 내구도 1 감소)'` |
| 17 | L1081 | `'착용 할 수 없습니다'` |
| 18 | L1346 | `'1 이상의 금액을 입력해주세요.'` |
| 19 | L1681 | `'1부터 10 사이의 슬롯 번호를 입력해 주세요.'` |
| 20 | L1811 | `"스태미나가 부족합니다 (필요: 5 SP)"` |
| 21 | L1846 | `"채집 실패"` |
| 22 | L1859 | `"채집 중 오류가 발생했습니다."` |
| 23 | L1886-1891 | `"채집에 성공했습니다!"` / `"채집에 실패했습니다."` / `"🪵 장작을 획득했습니다!"` (서버 메시지 또는 `"{itemName} 획득!"` 공통 템플릿으로 통합 대상) |

---

### 2.7. 게임 밸런스 수치 상수 (Magic Numbers) — 총 60건 이상

주요 게임 밸런스 수치들이 클래스 내부 상수로 분산되어 있어 기획 수치 조정 시 소스 코드 수정이 필요합니다.

#### 2.7.1. 외부화 1순위 — 기획자 밸런싱 빈도가 높은 수치

| 수치 항목 | 현재 선언 위치 | 값 | 권장 외부화 키 |
|---|---|---|---|
| 장작 채집 성공률 | `GatheringService` | `50%` | `game.gathering.woodcut.success-rate` |
| 나무 스폰 확률 | `GatheringService` | `50%` | `game.gathering.woodcut.spawn-rate` |
| 채집 스태미나 소모량 | `GatheringService` | `5 SP` | `game.gathering.woodcut.stamina-cost` |
| 인벤토리 최대 용량 | `InventoryService` | `30` | `game.inventory.max-slots` |
| 기본 포션 수량 | `InventoryService` | `5` | `game.inventory.default-potion-qty` |
| 장비 기본 최대 내구도 | `InventoryService` | `20` | `game.inventory.equipment-max-durability` |
| 도망 성공 확률 | `BattleService` | `50%` | `game.battle.flee-success-rate` |
| 필드 기습 확률 | `MonsterEncounterService` | `5%` | `game.battle.ambush-rate` |
| 마법 캐스팅 실패 확률 | `BattleService` | `10%` | `game.battle.magic-fail-rate` |
| 공격당 내구도 소모 | `BattleService` | `0.05` | `game.battle.durability-per-attack` |
| 치료비 | `HealController` | `100G` | `game.heal.cost` |
| 수리 성공률 | `RepairController` | `95%` | `game.repair.success-rate` |
| 수리 회복량 | `RepairController` | `1` | `game.repair.amount` |
| 사망 경험치 패널티 | `ProgressionService` | `10%` | `game.progression.death-penalty-rate` |
| 최대 레벨 | `ProgressionService` | `100` | `game.progression.max-level` |
| 월드 이동 시간(분) | `MovementService` | `15분` | `game.movement.world-move-minutes` |
| 던전 이동 시간(분) | `DungeonService` | `5분` | `game.movement.dungeon-move-minutes` |
| 몬스터 AI 가중치 | `MonsterAiService` | `34/33/33` | `game.monster-ai.normal/heavy/defense-weight` |

#### 2.7.2. 외부화 2순위 — 전투 공식 계수 (수학적 밸런스)

| 수치 항목 | 현재 선언 위치 | 값 | 비고 |
|---|---|---|---|
| 근접 계수 | `BattleService` | `1.0` | `MELEE_COEF` |
| 궁술 계수 | `BattleService` | `0.85` | `ARCHERY_COEF` |
| 마법 계수 | `BattleService` | `1.2` | `MAGIC_COEF` |
| 크리티컬 배율 | `BattleResolver` | `1.5x` | `CRITICAL_MULTIPLIER` |
| 비긴 계수 | `BattleResolver` | `0.5` | `DRAW_COEFFICIENT` |
| 승리 계수 | `BattleResolver` | `1.0` | `WIN_COEFFICIENT` |
| 몬스터 일반 공격 배율 | `BattleService` | `100%` | `MONSTER_NORMAL_MULTIPLIER` |
| 몬스터 강공격 배율 | `BattleService` | `150%` | `MONSTER_HEAVY_MULTIPLIER` |

#### 2.7.3. 외부화 3순위 — 도메인 기본값 (변경 빈도 낮음)

| 수치 항목 | 현재 선언 위치 | 값 | 비고 |
|---|---|---|---|
| 기본 스탯(STR/DEX/INT) | `StatProgression` | `10` | 캐릭터 생성 시 기본값 |
| 레벨당 스탯 증가(STR/DEX/INT) | `StatProgression` | `3` | 레벨업 기본 성장 |
| 레벨당 방어력 증가 | `StatProgression` | `1` | `DEF_PER_LEVEL` |
| 기본 바이탈(HP/MP/SP) | `StatProgression` | `100` | `BASE_VITAL` |
| 레벨당 바이탈 증가 | `StatProgression` | `10` | `VITAL_PER_LEVEL` |
| 경험치 테이블 계수 | `ExperiencePolicy` | `50L / 15L` | 선형/2차 계수 |
| 활동 로그 최대 줄 수 | `ActionLog` | `10` | `MAX_ENTRIES` |
| 몬스터 방어 차단율 | `Monster` / `MonsterService` | `70%` | `DEFAULT_DEFENSE_BLOCK_RATE` |
| 스킬 랭크 AP 비용 | `SkillRankPolicy` | `{0,0,1,1,2,2,...}` | 배열 상수 |

---

## 3. 정합성 검증 결과 (Consistency Check)

전체 소스 코드를 `grep`, `codegraph`, 파일 뷰어로 재검사하여 기존 문서 대비 **누락/오류**를 식별한 결과:

### ✅ 기존 문서에서 정확했던 항목
- 2.1절 활동 로그(ActionLog) 영역: 8개 서비스/컨트롤러의 `actionLog.add` 호출 → **정확** (단, 건수 세분화 보완)
- 2.3절(현 2.2절) `BattleLogFormatter.java` 하드코딩 ~40건 → **정확**
- 2.4절(현 2.7절) 게임 밸런스 수치 → **부분 정확** (아래 누락 보완)

### 🟡 보완/추가된 항목 (이전 문서 대비)

| 누락 영역 | 건수 | 설명 |
|---|---|---|
| **`BattleService.java` 인라인 전투 로그** | **15건** | `BattleLogFormatter` 외에 `BattleService` 자체에 캐스팅 실패, 회복, CC, DoT, 메디테이션, 승리 보상, 사망, 타임아웃, 도망 실패 등 전투 인라인 로그가 별도 존재 |
| **`InventoryService.describe()` 장비 포맷팅** | **3건** | 장비 종류 라벨, "방패와 함께 착용할 수 없습니다.", "내구도:" 포맷팅 문구 |
| **프론트엔드 `myrpg.js` alert/toast** | **23건** | 전투 결과 alert, 상점/수리/치료 결과, 채집 결과, 스태미나 부족, 은행 금액 검증 등 |
| **도메인 예외 메시지** | **50건+** | `ItemDataException`, `MonsterDataException`, `EquipConflictException` 등 다수 (개발자용 방어 로직 포함) |
| **게임 밸런스 수치 추가** | **20건+** | `HealController.HEAL_COST`, `RepairController.REPAIR_SUCCESS_RATE_PERCENT`, `BattleService` 전투 계수, `StatProgression` 기본값, `ExperiencePolicy` 계수, `MonsterAiService` AI 가중치 등 |
| **`BattleService.MAGIC_FAIL_PERCENT`** | 1건 | 궁극기 쿨타임 로그 및 자원 부족 로그 (`"궁극기 쿨타임 대기 중입니다."`, `resourceKind.label() + "이(가) 부족합니다."`) |
| **`PlayScreenController` 치트 로그** | 3건 | `"테스트 치트: 1,000 EXP/Gold를 획득했습니다!"` |

### ❌ 기존 문서의 오류 수정

| 항목 | 기존 기술 | 실제 값 |
|---|---|---|
| 필드 기습 확률 | `30%` (`battle.ambush.rate`) | 실제: **5%** (`MonsterEncounterService.PREEMPTIVE_STRIKE_PERCENT = 5`) |
| 은행 보관함 위치 | `BankService` | 실제: 별도 `BankService` 클래스 없음, `InventoryService` 내부에서 `BANK` StorageKind로 관리 |

---

## 4. 리팩토링 아키텍처 설계안 (Externalization Strategy)

### 4.1. Spring `MessageSource` + `messages.properties` 표준 방식

Spring Boot 표준 다국어/메시지 관리 메커니즘을 사용하는 표준적이고 강력한 방식입니다.

* **파일 위치**: `src/main/resources/messages.properties` (필요 시 `messages_en.properties` 확장 가능)
* **포맷팅**: Java `MessageFormat` 기반 placeholder (`{0}`, `{1}`) 지원

```properties
# ============================================================
# 활동 로그 (Action Log)
# ============================================================
# 채집: 버섯, 약초, 광석 등 향후 채집물 확장에 종속되지 않는 범용 공통 멘트
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
log.combat.ambush=🚨 {0} 기습!
log.combat.flee.success=도망쳤다!
log.combat.resource_lack={0}이(가) 부족합니다.
log.combat.ultimate_cooldown=궁극기 쿨타임 대기 중입니다. ({0}승 남음)
log.dungeon.enter={0}에 입장했습니다.
log.dungeon.exit=던전에서 나왔습니다.
log.dungeon.clear={0}을(를) 완전히 정복했습니다!
log.dungeon.clear_reward=던전 클리어 보상: EXP +{0}, Gold +{1}G
log.dungeon.item_reward=보상 획득: {0} x{1}
log.system.move_blocked=전투 중에는 이동할 수 없습니다.
log.system.rebirth_done=환생했습니다 (재능: {0})
log.system.rebirth_wait=환생까지 {0}시간 {1}분 남았습니다

# ============================================================
# 전투 인라인 로그 (Battle Inline)
# ============================================================
battle.cast_fail=🔮 [{0}] 캐스팅 실패! (집중이 흐트러짐)
battle.recovery=💖 [{0}] HP +{1} 회복
battle.cc.success=⛓️ [{0}] {1} 기절 성공! (1턴)
battle.cc.resist=⛓️ [{0}] 저항으로 제어 효과 실패
battle.stun_frozen=❄️ [{0}] 빙결/기절 상태로 행동 불가
battle.freeze=❄️ [빙결] {0} 꽁꽁 얼어붙음! (1턴 행동 불가)
battle.rage=💢 [레이지 임팩트] 다음 물리 피해 +30% 증폭
battle.dot=🩸 [지속 피해] {0} {1} 도트 피해
battle.meditation=🧘 [메디테이션] MP +{0} 회복
battle.victory_reward=승리! EXP +{0} | Gold +{1}
battle.death=쓰러졌다… 티르코네일에서 부활 (경험치 -{0})
battle.timeout.preemptive=⏳ [시간 초과] 선제 공격 기회 상실
battle.timeout.hit=⏳ [시간 초과] 무방비 피격
battle.monster.defense_hold=[{0}] 🛡️ 방어 태세 유지
battle.flee.fail=⚠️ [도망 실패] {0}에게 저지당해 {1} 피해

# ============================================================
# 전투 턴 로그 (BattleLogFormatter)
# ============================================================
battle.first_strike.enemy=⚠️ [적 선제공격] [{0}] 기습 ➔ {1} 피해 피격
battle.first_strike.ultimate_multi=👑 [결전 궁극기] [{0}] {1}연타 ({2}) ➔ 총 {3} 관통 피해
battle.first_strike.ultimate_single=👑 [결전 궁극기] [{0}]{1} 100% 관통 ➔ {2} 피해
battle.first_strike.stance=⚡ [선제 공격] 선제 찬스였으나 [{0}] 태세 유지
battle.first_strike.player_multi=⚡ [선제 공격] [{0}] {1}연타 ({2}) ➔ 총 {3} 피해
battle.first_strike.player_single=⚡ [선제 공격] [{0}]{1} ➔ {2}에게 {3} 피해
battle.turn.block_perfect=⚔️ [{0}] 🛡️ {1}의 완전 방어에 가로막힘 (0 피해)
battle.turn.miss=🗡️ [{0}] 빗나감 (0 피해)
battle.turn.vs_defense_multi=⚔️ [{0}] {1}연타 ({2}) 🛡️ 적 방어에 막힘 ➔ 총 {3} 피해
battle.turn.vs_defense_single=⚔️ [{0}]{1} 🛡️ 적 방어에 막힘 ➔ {2} 피해
battle.turn.multi_hit=⚔️ [{0}] {1}연타 ({2}) ➔ 총 {3} 피해
battle.turn.single_hit=🗡️ [{0}]{1} ➔ {2}에게 {3} 피해
battle.turn.counter_crit=⚡ [{0}] 💥 적 공격을 흘려내며 ➔ {1} 치명 반격!
battle.turn.defense_success=🛡️ [{0}] 방어 성공 & 반격! ➔ {1}에게 {2} 반격 피해
battle.turn.defense_window=🛡️ [{0}] 완벽 방어! ➔ 빈틈 포착 (다음 턴 선제 찬스⚡)
battle.turn.defense_penetrated=⚠️ [{0}] 몬스터 강공격에 방어선 관통!
battle.turn.defense_all=🛡️ [{0}] 적의 공격을 완벽히 막아냄 (0 피해)
battle.turn.defense_stalemate=🛡️ [{0}] 맞방어 교착 상태
battle.turn.counter_miss=⚡ [{0}] 적이 공격하지 않아 빗나감
battle.monster.normal=[{0}] {1} ➔ {2} 피해 피격
battle.monster.blocked=[{0}] {1} ➔ 🛡️ 방어로 경감되어 {2} 피해
battle.monster.miss=[{0}] {1} ➔ 빗나감
battle.monster.counter_miss=[{0}] {1} ➔ 빗나감
battle.monster.defense_hold=[{0}] 🛡️ 방어 태세 유지
battle.monster.defense_counter=[{0}] 🛡️ 방어 성공 & 반격 ➔ {1} 피해 피격
battle.monster.defense_alert=[{0}] 🛡️ 공격 방어 성공 ➔ 반격 태세 (다음 턴 선제 주의⚠️)
battle.monster.defense_break=[{0}] 💥 방어선 관통됨!
battle.monster.defense_full=[{0}] 🛡️ 완전 방어 (0 피해)

# ============================================================
# 장비 상세 팝업 포맷팅 (describe)
# ============================================================
describe.equip.kind_type={0} ({1})
describe.equip.shield_conflict=방패와 함께 착용할 수 없습니다.
describe.equip.durability=내구도: {0}/{1}

# ============================================================
# 시스템 및 검증 알림
# ============================================================
system.inventory.full=인벤토리가 가득 찼습니다 (최대 {0}칸)
```

---

### 4.2. 메시지 헬퍼 서비스 (`GameMessageService.java`) 구축

소스 코드에서 문자열을 직접 조합하는 대신, 타입 세이프하고 직관적인 메시지 리졸버를 제공합니다.

```java
@Service
public class GameMessageService {

    private final MessageSource messageSource;

    public GameMessageService(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * 키와 인자를 받아 포맷팅된 메시지를 반환합니다.
     */
    public String get(final String code, final Object... args) {
        return messageSource.getMessage(code, args, Locale.KOREAN);
    }
}
```

* **사용 예시 (`GatheringService`)**:
```java
// 기존 (특정 아이템에 종속된 멘트)
actionLog.add("[채집] 🪵 단단한 장작을 1개 얻었습니다!", LOG_TYPE_ITEM);

// 개선 (아이템 카탈로그 연동 범용 공통 멘트: 향후 버섯, 약초 등 확장 가능)
final String itemName = resolveItemName(itemId);
actionLog.add(msg.get("log.gathering.success", itemName), LOG_TYPE_ITEM);
// 실패 시: actionLog.add(msg.get("log.gathering.failure"), LOG_TYPE_SYSTEM);
```

* **사용 예시 (`BattleService`)**:
```java
// 기존
combatLines.add("💖 [" + recoverySkill.label() + "] HP +" + actualHealed + " 회복");

// 개선
combatLines.add(msg.get("battle.recovery", recoverySkill.label(), actualHealed));
```

---

### 4.3. 게임 밸런스 설정 외부화 (`GameProperties.java`)

```yaml
# application-game.yml
game:
  gathering:
    woodcut:
      spawn-rate: 50
      success-rate: 50
      stamina-cost: 5
  inventory:
    max-slots: 30
    default-potion-qty: 5
    equipment-max-durability: 20
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
  heal:
    cost: 100
  repair:
    success-rate: 95
    amount: 1
  progression:
    max-level: 100
    death-penalty-rate: 0.10
  movement:
    world-move-minutes: 15
    dungeon-move-minutes: 5
  monster-ai:
    normal-weight: 34
    heavy-weight: 33
    defense-weight: 33
```

```java
@ConfigurationProperties(prefix = "game")
public record GameProperties(
        GatheringProperties gathering,
        InventoryProperties inventory,
        BattleProperties battle,
        HealProperties heal,
        RepairProperties repair,
        ProgressionProperties progression,
        MovementProperties movement,
        MonsterAiProperties monsterAi) {

    public record GatheringProperties(WoodcutProperties woodcut) {}
    public record WoodcutProperties(int spawnRate, int successRate, int staminaCost) {}
    public record InventoryProperties(int maxSlots, int defaultPotionQty, int equipmentMaxDurability) {}
    public record BattleProperties(
            int fleeSuccessRate, int ambushRate, int magicFailRate,
            double durabilityPerAttack, double meleeCoef, double archeryCoef,
            double magicCoef, double criticalMultiplier,
            int monsterNormalMultiplier, int monsterHeavyMultiplier) {}
    public record HealProperties(int cost) {}
    public record RepairProperties(int successRate, int amount) {}
    public record ProgressionProperties(int maxLevel, double deathPenaltyRate) {}
    public record MovementProperties(int worldMoveMinutes, int dungeonMoveMinutes) {}
    public record MonsterAiProperties(int normalWeight, int heavyWeight, int defenseWeight) {}
}
```

---

## 5. 점진적 마이그레이션 로드맵 (Phased Roadmap)

| 단계 | 작업 내용 | 주요 변경 대상 (건수) | 예상 효과 |
|---|---|---|---|
| **Phase 1: 메시지 인프라 구축 & 활동 로그 외부화** | `messages.properties` + `GameMessageService` 빈 생성, 8개 서비스/컨트롤러의 `actionLog.add` 문자열 이관 | 26건 (`actionLog.add` 호출) | 활동 로그 문구 중앙 관리 |
| **Phase 2: BattleService 인라인 전투 로그 외부화** | `combatLines.add` 15건의 인라인 전투 로그를 `messages.properties` `battle.*` 키로 분리 | 15건 (`combatLines.add` 호출) | 전투 스킬 효과 로그의 프로퍼티 기반 튜닝 |
| **Phase 3: BattleLogFormatter 외부화** | ~40개 턴 로그 템플릿을 `messages.properties`로 분리, `BattleLogFormatter`에 `GameMessageService` 주입 | ~40건 | 전투 톤앤매너의 무코딩 튜닝 |
| **Phase 4: 장비 상세 팝업 & 유저 노출 예외 외부화** | `InventoryService.describe()` 장비 포맷팅 3건 + `EquipConflictException` 등 유저 노출 예외 메시지 이관 | ~15건 | 유저 피드백 문구 일원화 |
| **Phase 5: 게임 밸런스 수치 외부화** | `application-game.yml` + `GameProperties` 레코드 생성, 18개 이상 서비스의 `static final` 상수를 `@ConfigurationProperties` 주입으로 전환 | ~50건 | 무재빌드 밸런스 패치 |
| **Phase 6: 프론트엔드 JS 메시지 외부화** | `myrpg.js` 내 alert/toast 메시지를 Thymeleaf `<script>` 블록 혹은 `/api/messages` JSON 엔드포인트로 분리 | 23건 | 서버-클라이언트 메시지 SSOT 통합 |

---

## 6. 기대 효과

1. **단일 진실 공급원 (SSOT for Strings)**:
   - 모든 인게임 메시지와 로그 템플릿이 `messages.properties` 한 곳에 모여 오탈자 수정, 이모지 변경, 텍스트 개선이 매우 쉬워집니다.
2. **비즈니스 로직과 프레젠테이션의 완벽한 분리**:
   - `BattleService`, `GatheringService` 등 도메인 서비스는 '순수 비즈니스 로직(판정, 수치 계산)'에만 집중하고, 문자열 렌더링은 메시지 시스템에 위임합니다.
3. **무중단 밸런스 패치 및 테스트 유연성**:
   - 단위 테스트에서 확률이나 수치를 mock/override하기 쉬워지며, 서버 재배포 시 설정 파일만으로 이벤트 확률(예: 채집 성공률 2배 이벤트)을 손쉽게 적용할 수 있습니다.
4. **프론트엔드 메시지 일원화**:
   - 서버와 클라이언트의 동일한 상황에서 출력되는 메시지(예: "전투 중에는 이동할 수 없습니다.")가 하나의 원본에서 관리됩니다.

---

## 7. 총 하드코딩 현황 요약 (정합성 검증 완료)

| 영역 | 건수 | 외부화 대상 파일 |
|---|---|---|
| 활동 로그 (`actionLog.add`) | **26건** | `messages.properties` |
| 전투 인라인 로그 (`combatLines.add` in `BattleService`) | **15건** | `messages.properties` |
| 전투 턴 로그 (`BattleLogFormatter`) | **~40건** | `messages.properties` |
| 장비 상세 팝업 (`InventoryService.describe`) | **3건** | `messages.properties` |
| 도메인 예외 메시지 | **50건+** | `messages.properties` (유저 노출분 우선) |
| 프론트엔드 JS alert/toast (`myrpg.js`) | **23건** | 서버 API 또는 Thymeleaf 주입 |
| 게임 밸런스 수치 상수 | **60건+** | `application-game.yml` |
| **총계** | **~220건** | — |
