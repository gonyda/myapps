# Design Document

## Overview

본 설계는 `myrpg` Web 모듈(`com.myapps.web.myrpg`)에 **딜 스킬 2축 차별화(`hitCount`·`critBonus`)** 와 **전투 로그 UI 재설계**를 추가한다(스펙 009). 008(`battle-system`)의 전투 루프(`BattleResolver`·`BattleService`·`BattleController`·`battle-view.html`)와 005의 스킬 카탈로그(`DamageSkill`·`SkillCatalogService`) 위에서 동작하며, `data-balance-guide.md`(§0 데미지 공식, §4 딜 스킬 2축 설계)가 확정한 설계를 구현으로 옮긴다.

핵심 원칙(008의 "순수/저장/카탈로그 구분" 계승):

- **순수 도메인(`BattleResolver`)**: `multiplierByRank`를 1히트당 배율로 재해석하고, 플레이어 딜 스킬 피해를 `hitCount`번 반복 산출한다. 각 히트마다 감산(방어 차감)·크리티컬·편차를 **독립** 적용하고 합산한다. 결정적 부분(감산·상성)은 순수, 크리·편차만 주입 `Random`.
- **카탈로그(`DamageSkill`·`SkillCatalogService`)**: 딜 스킬에 `hitCount`(기본 1)·`critBonus`(기본 0) 필드를 추가하고, `skill.json`을 optional 파싱·검증한다.
- **오케스트레이션(`BattleService`)**: 스킬의 per-hit 배율·`hitCount`·`critBonus`를 조립해 `BattleResolver`에 넘기고, 히트별 상세를 턴 결과로 전달한다. 전투 액션 로그와 결산 로그를 분리한다.
- **표현(`BattleController` + `battle-view.html`)**: 전투 뷰 중앙(HP 바와 스킬 사이)에 이번 턴 로그 섹션을 렌더하고(매 턴 교체), 결산 로그는 하단 `ActionLog`에 유지한다.

방어 스킬(`defense`·`counter_attack`)은 이미 2축(경감률↔반격율)이라 **변경하지 않는다**. 몬스터 피해·방어 반격은 항상 단일 히트다.

### 이번 스펙에서 구현 vs 이연

- **구현**: `DamageSkill` 필드 확장·카탈로그 파싱/검증·`skill.json` 9개 딜 스킬 확정, 멀티히트 데미지 산출(히트별 감산·크리·편차 독립), 스킬 크리 보너스, 히트별 결과 노출, 멀티히트 로그(C안), 액션↔결산 로그 분리, 전투 뷰 중앙 로그 섹션(매 턴 교체·DRY 서브프래그먼트).
- **이연**: 부류 2 확장 축(상태이상·흡혈·자기 버프·조건부·행동 순서 — `data-balance-guide.md` "향후 확장 축").

## Architecture

### 모듈 변경 (009)

008과 동일한 DDD 4계층에서 아래를 확장한다. **[신규]**는 새 파일, **[확장]**은 기존 산출물 수정.

```
myrpg/src/
├── main/java/com/myapps/web/myrpg/
│   ├── interfaces/api/
│   │   └── BattleController.java               # [확장] turnLog 모델 속성(중앙 로그) 세팅, start 인트로 라인
│   ├── application/
│   │   ├── service/
│   │   │   ├── BattleService.java              # [확장] per-hit 배율·hitCount·critBonus 조립, 멀티히트 선제, 크리 보너스, 액션↔결산 로그 분리
│   │   │   └── BattleLogFormatter.java         # [확장] 멀티히트 하이브리드(C안) 포맷
│   │   └── dto/
│   │       ├── BattleLogInput.java             # [확장] List<HitResult> playerHits 추가
│   │       └── BattleView.java                 # [유지] (turnLog는 모델 속성으로 전달, DTO 무변경)
│   └── domain/
│       ├── model/
│       │   ├── DamageSkill.java                # [확장] hitCount·critBonus 필드 + 하위호환 보조 생성자
│       │   ├── HitResult.java                  # [신규] record(int damage, boolean critical)
│       │   ├── TurnInput.java                  # [확장] int playerHitCount 추가
│       │   ├── ResolvedTurn.java               # [확장] List<HitResult> playerHits 추가
│       │   └── BattleTurnResult.java           # [확장] List<HitResult> playerHits + List<String> combatLines(액션, 중앙용)
│       └── service/
│           ├── BattleResolver.java             # [확장] multiHitDamage(...) + 3개 공격 경로 멀티히트화
│           └── (재사용) RockPaperScissors
│   └── application/service/SkillCatalogService.java  # [확장] hitCount·critBonus optional 파싱·검증
└── main/resources/
    ├── data/
    │   └── skill.json                          # [확장] 9개 딜 스킬 hitCount·critBonus·per-hit 배율 확정
    ├── templates/fragments/
    │   └── battle-view.html                    # [확장] battle-center 공용 서브프래그먼트 + Battle_Log_Section(중앙)
    └── static/css/
        └── myrpg.css                           # [확장] .battle-log 섹션 스타일
```

> `ActionLog`(하단 footer)·`action-log.html`은 무변경. 전투 액션 로그를 더 이상 하단에 추가하지 않는 것은 `BattleService` 로직 변경으로 처리한다(결산 로그만 하단).

### 멀티히트 데미지 흐름

```
BattleService.takeTurn
  ├─ per-hit 배율 = SkillDamagePolicy.multiplier(skill, rank)   // multiplierByRank = 1히트당
  ├─ hitCount    = skill.hitCount()                            // DamageSkill, else 1
  ├─ 실효크리     = min(1000, 캐릭터크리 + skill.critBonus())    // DamageSkill critBonus, else +0
  ├─ TurnInput(playerHitCount 포함) → BattleResolver.resolve
  │     └─ (공격 승/무/일반패배) multiHitDamage 루프:
  │           for i in 1..hitCount:
  │              base = baseDamage(atk, perHitMult, targetDef)   // 방어 매 히트 차감
  │              crit = rollCritical(실효크리)                    // 히트별 독립
  │              dmg  = finalDamage(base, coeff, crit)            // 편차 히트별 독립
  │           → List<HitResult>, total=Σdmg, anyCrit
  ├─ ResolvedTurn(playerHits, playerDamage=Σ, ...) → TurnCombatResult(playerHits)
  ├─ BattleLogInput(playerHits) → BattleLogFormatter.combatLines  // C안 멀티히트
  └─ BattleTurnResult(combatLines=액션, playerHits, reward...)
        · 액션 라인 → turnResult.combatLines (중앙, actionLog에 추가 안 함)
        · 결산/사망 라인 → actionLog.add(bottom)
```

## Components and Interfaces

### HitResult (domain/model) [신규]

```java
/** 멀티히트 한 타의 결과. 로그 브레이크다운·합산의 소스. */
public record HitResult(int damage, boolean critical) {}
```

### DamageSkill (domain/model) [확장]

```java
public record DamageSkill(
        String id, String label, SkillType type, SkillTalent talent,
        int resourceCost, Map<SkillRank, Integer> multiplierByRank, String description,
        int hitCount, int critBonus) implements Skill {

    /** 하위호환 보조 생성자: hitCount=1, critBonus=0. 기존 7-인자 호출부·테스트 보존. */
    public DamageSkill(String id, String label, SkillType type, SkillTalent talent,
                       int resourceCost, Map<SkillRank, Integer> multiplierByRank, String description) {
        this(id, label, type, talent, resourceCost, multiplierByRank, description, 1, 0);
    }
}
```

- `multiplierByRank`는 **1히트당 배율**(Per_Hit_Multiplier)로 의미 재정의(필드 구조 불변). 기존 `multiplierByRank()` 접근자·의미(랭크별 배율) 그대로.
- 보조 생성자로 기존 7-인자 호출부(테스트 등) 하위호환.

### SkillCatalogService (application/service) [확장]

```java
private DamageSkill parseDamageSkill(...) {
    final Map<SkillRank,Integer> multiplierByRank = parseRankMap(skillNode, "multiplierByRank", id);
    final int hitCount  = extractOptionalInt(skillNode, "hitCount", id, DEFAULT_HIT_COUNT, MIN_HIT_COUNT, MAX_HIT_COUNT);
    final int critBonus = extractOptionalInt(skillNode, "critBonus", id, DEFAULT_CRIT_BONUS, MIN_CRIT_BONUS, MAX_CRIT_BONUS);
    return new DamageSkill(id, label, type, talent, resourceCost, multiplierByRank, description, hitCount, critBonus);
}
```

- `extractOptionalInt(node, field, skillId, default, min, max)`[신규]: 필드 부재 시 default, 존재하되 숫자 아님/범위 밖이면 `SkillDataException`.
- 상수: `DEFAULT_HIT_COUNT=1`, `MIN_HIT_COUNT=1`, `MAX_HIT_COUNT=8`(가이드 상한 여유), `DEFAULT_CRIT_BONUS=0`, `MIN_CRIT_BONUS=0`, `MAX_CRIT_BONUS=100`.
- `parseDefenseSkill`은 두 필드를 읽지 않는다(무시).

### BattleResolver (domain/service) [확장]

```java
/** 멀티히트 피해: hitCount번 반복(히트별 감산·크리·편차 독립). 순수 계산 + 주입 Random. */
public List<HitResult> multiHitDamage(int attackPower, int perHitMultiplierPercent, int targetDefense,
                                      double affinityCoefficient, int critChance, int hitCount) {
    final List<HitResult> hits = new ArrayList<>(hitCount);
    for (int i = 0; i < hitCount; i++) {
        final int base = baseDamage(attackPower, perHitMultiplierPercent, targetDefense);
        final boolean crit = rollCritical(critChance);
        hits.add(new HitResult(finalDamage(base, affinityCoefficient, crit), crit));
    }
    return hits;
}
```

- **난수 순서 보존**: 히트마다 `rollCritical`(크리) → `finalDamage`(편차) 순서로 소비 → `hitCount == 1`이면 기존 `resolveAttackWins` 등의 난수 시퀀스와 동일(하위호환).
- 3개 공격 경로(`resolveAttackWins`·`resolveDrawAttack`·`resolveNormalLosesToDefense`)가 단일 `finalDamage` 대신 `multiHitDamage(...)`를 호출하고, 합계를 `playerDamageToMonster`, 리스트를 `playerHits`로 담는다. 상성계수는 각 경로 값(승 1.0 / 무 0.5 / 경감 `blockCoeff`).
- 반격(`calculateCounterDamage`)·몬스터 피해·관통패(0)·교착(0)은 단일 유지, `playerHits`는 비운다.
- `resolve`는 `TurnInput.playerHitCount()`를 위 경로에 전달.

### TurnInput / ResolvedTurn (domain/model) [확장]

```java
// TurnInput: 마지막에 int playerHitCount 추가 (playerMultiplierPercent는 1히트당 배율 의미)
// ResolvedTurn: 마지막에 List<HitResult> playerHits 추가
//   playerDamageToMonster = playerHits 합계(공격 경로), 반격/0 경로는 기존 단일값·playerHits=List.of()
```

### BattleService (application/service) [확장]

```java
// 크리 보너스: DamageSkill이면 +critBonus, 그 외 +0. 상한 1000 보정.
private int resolvePlayerCritical(final Skill skill, final CharacterProgress progress) {
    final int base = /* 기존 캐릭터 크리 */;
    final int bonus = (skill instanceof DamageSkill ds) ? ds.critBonus() : 0;
    return Math.min(CRITICAL_ROLL_MAX, base + bonus);   // CRITICAL_ROLL_MAX=1000
}

// hitCount: DamageSkill이면 hitCount(), 그 외 1.
private int resolvePlayerHitCount(final Skill skill) {
    return (skill instanceof DamageSkill ds) ? ds.hitCount() : 1;
}
```

- `resolveNormalCombat`: `TurnInput`에 `playerHitCount` 포함, `playerCritical`은 `resolvePlayerCritical(skill, progress)`. `TurnCombatResult`에 `resolved.playerHits()` 전달.
- `resolveBowFirstStrike`: 단일 `baseDamage`/`finalDamage` → `resolver.multiHitDamage(playerAttack, perHitMult, monster.defense(), 1.0, 실효크리, hitCount)`로 교체(선제도 멀티히트 반영). `playerHits` 포함.
- `resolvePlayerMultiplier`: 변경 없음(정책이 반환하는 `multiplierByRank` 값이 곧 per-hit 배율).
- **로그 분리**:
  - `combatLines` = (castFailure면 `"{스킬} 캐스팅 실패!"` 선행) + `logFormatter.combatLines(logInput)` → `BattleTurnResult.combatLines`(중앙). **actionLog에 추가하지 않음**.
  - `processKillReward`·`handleDeath`의 결산/사망 라인 → `actionLog.add(line, LOG_TYPE_COMBAT)`(하단).
  - `start`: 하단 시작 로그 제거, 인트로는 컨트롤러가 `turnLog`로 표시.
  - `flee`: 성공 라인 → 하단(전투 종료 후 확인), 실패 라인(몬스터 1대) → 중앙 `combatLines`.
- `TurnCombatResult`(내부 record): `List<HitResult> playerHits` 추가.

### BattleTurnResult (domain/model) [확장]

```java
// 기존 컴포넌트 유지 + 추가:
//   List<HitResult> playerHits   // 플레이어 딜 스킬 히트별 상세(로그 브레이크다운 소스)
//   List<String>    combatLines  // 이번 턴 액션 로그(중앙, actionLog 미추가)
// 기존 logLines는 combatLines로 대체(액션 라인). playerDamage == Σ playerHits.damage.
```

### BattleLogInput (application/dto) [확장]

```java
// 마지막에 List<HitResult> playerHits 추가. (playerDamage는 합계로 유지 — 하위호환)
```

### BattleLogFormatter (application/service) [확장] — 멀티히트 하이브리드(C안)

```java
private void addPlayerLine(final List<String> lines, final BattleLogInput input) {
    // 방어 스킬 → 기존 addPlayerDefenseLine (변경 없음)
    // 피해 0 → "공격이 빗나갔다!" (변경 없음)
    // playerHits.size() >= 2 → 멀티히트:
    //   lines.add("{스킬}({타입}) {N}연타")
    //   lines.add("{d1}  {d2}(치명)  {d3} = {합계} 피해")
    // else → 기존 단일: "{스킬}({타입})로 {몬스터}에게 {N} 피해" (+" (크리티컬!)")
}
```

- 브레이크다운: 각 히트 `damage` 나열, `critical`이면 `"(치명)"` 접미. 구분자 두 칸 공백, 끝에 `= {합계} 피해`.
- 선제 사격(firstStrike)도 `playerHits.size() >= 2`면 멀티히트 브레이크다운으로 표기(헤더 `"선제 사격! {N}연타"`).
- 몬스터 라인·방어(반격/관통/교착)·캐스팅 실패·빗나감은 기존 형식 유지.

### BattleController (interfaces/api) [확장]

```java
// populateBattleModel: model.addAttribute("turnLog", <lines>)
//   - start(): turnLog = List.of("{monster.name()} Lv.{level} 출현!")  (인트로)
//   - buildOngoingBattleResponse(): turnLog = result.combatLines()
// battle-view 프래그먼트가 ${turnLog}를 Battle_Log_Section에 렌더.
```

### battle-view.html [확장] — 공용 서브프래그먼트 + 중앙 로그

```html
<!-- 공용 전투 화면 본문(DRY): battle-view / battle-response 양쪽에서 재사용 -->
<div th:fragment="battle-center">
    <div class="battle-monster-info">…이름 + Lv…</div>
    <div class="battle-hp-wrap">…몬스터 HP 바…</div>

    <!-- 신규: 이번 턴 전투 로그(중앙). 매 턴 교체, 누적 안 함 -->
    <div class="battle-log" id="battleLog">
        <div th:each="line : ${turnLog}" class="battle-log-line" th:text="${line}">로그</div>
    </div>

    <div id="battleSkills" th:fragment="battle-skills">…스킬 버튼…</div>
    <button class="flee-btn" …>도망</button>
    <div th:replace="~{fragments/minimap :: minimap}"></div>
</div>
```

- `battle-view`(start용)·`battle-response`(턴/도망 응답용) 모두 `battle-center`를 `th:replace`로 사용 → `Battle_Log_Section` 정의 1곳(DRY).
- `battle-skills` 서브프래그먼트는 `battle-center` 내부에 유지(`GET /battle/skills`가 이 조각만 교체 → 중앙 로그는 DOM에 남아 직전 턴 유지, R10.3).

## Data Models

### skill.json (9개 딜 스킬 확정, `data-balance-guide.md` §4)

| talent | id | type | hitCount | critBonus | per-hit 배율(F→MASTER) | 총 배율 |
|---|---|---|---|---|---|---|
| MELEE | slash | NORMAL | 1 | 0 | 90→170 | 90→170 |
| MELEE | windmill | NORMAL | 3 | 0 | 35→65 | 105→195 |
| MELEE | smash | HEAVY | 1 | 80 | 130→250 | 130→250 |
| ARCHERY | aimed_shot | NORMAL | 1 | 0 | 90→170 | 90→170 |
| ARCHERY | arrow_revolver | NORMAL | 4 | 0 | 27→50 | 108→200 |
| ARCHERY | magnum_shot | HEAVY | 1 | 100 | 140→260 | 140→260 |
| MAGIC | mana_bolt | NORMAL | 1 | 0 | 90→170 | 90→170 |
| MAGIC | icebolt | NORMAL | 3 | 0 | 35→65 | 105→195 |
| MAGIC | firebolt | HEAVY | 1 | 0 | 130→250 | 130→250 |

- per-hit 배율 맵은 16키(F→MASTER) 완비 + 단조 비감소. 다단 per-hit 맵은 단일과 동일한 16-스텝 곡선을 낮은 절대값으로 스케일(단조 유지).
- 단일 히트 스킬은 `hitCount`/`critBonus`를 명시하거나 생략 가능(생략 시 기본 1/0). 명시 권장(가독성).
- 방어 스킬(`defense`·`counter_attack`) 무변경.

### 멀티히트 데미지 공식 (`data-balance-guide.md` §0 일치)

```
공격력 = round(주스탯 × 재능계수)
[hitCount번 반복]
  기본피해(i) = max(1, floor(공격력 × per-hit배율% / 100) − 대상.defense)
  히트피해(i) = max(1, round(기본피해(i) × 상성계수 × (크리(i) ? 1.5 : 1) × rand(0.90~1.10)))
최종피해 = Σ 히트피해(i)      // 각 히트 ≥1 → 총 ≥ hitCount
크리(i) 판정확률 = min(1000, 캐릭터크리 + skill.critBonus)   // 히트별 독립
```

### 비영속 값

- `HitResult`·`TurnInput`·`ResolvedTurn`·`BattleTurnResult`·`BattleLogInput`·`BattleView`는 record. 영속 엔티티(`BattleState`) 변경 없음. `turnLog`는 모델 속성(비영속, 매 턴 생성).

## Correctness Properties

*프로퍼티는 시스템의 모든 유효한 실행에서 참이어야 하는 특성이다.* 순수/결정적 로직(멀티히트 산출·감산·크리·편차(시드)·크리 보너스·카탈로그 파싱·로그 포맷·로그 라우팅)을 대상으로 하며, 템플릿·CSS(SMOKE)와 고정 초기값(EXAMPLE)은 제외한다.

### Property 1: 단일 히트 하위호환 동치

*For any* `hitCount == 1`·`critBonus == 0`인 딜 스킬과 고정 시드 `Random`에 대해, `multiHitDamage`의 단일 결과와 총 피해는 009 이전 단일 `finalDamage` 산출과 동일한 값(동일 난수 시퀀스)을 낸다.

**Validates: Requirements 4.5, 11.1**

### Property 2: 멀티히트 합산·최소 보장

*For any* 공격력·per-hit 배율·방어·상성계수·`hitCount ≥ 1`과 고정 시드에 대해, `multiHitDamage`는 정확히 `hitCount`개의 `HitResult`를 만들고, 각 히트 피해는 ≥1이며, 총 피해는 각 히트 합과 같고 ≥ `hitCount`이다.

**Validates: Requirements 4.1, 4.4**

### Property 3: 히트별 방어 차감

*For any* 방어 > 0인 대상에 대해, `hitCount` 히트의 각 기본피해는 `max(1, floor(공격력×per-hit배율/100) − 방어)`로 방어가 히트마다 차감되며, 같은 총 배율을 단일로 때린 경우보다 다단 총 피해가 크지 않다(고방어일수록 다단 불리).

**Validates: Requirements 4.2, 4.7**

### Property 4: 히트별 독립 크리·편차 (결정성)

*For any* 고정 시드 `Random`과 `hitCount`에 대해, 각 히트는 `rollCritical`(크리) → `finalDamage`(편차) 순으로 난수를 소비하며, 동일 시드에서 히트별 크리 여부·피해가 결정적으로 재현된다.

**Validates: Requirements 4.3, 4.8**

### Property 5: 실효 크리 = 캐릭터 크리 + critBonus (상한)

*For any* 캐릭터 크리와 스킬 `critBonus`에 대해, 플레이어 딜 스킬 실효 크리 확률은 `min(1000, 캐릭터크리 + critBonus)`이고, 방어 스킬·몬스터 크리는 `critBonus`의 영향을 받지 않는다.

**Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5**

### Property 6: 카탈로그 파싱 기본값·검증

*For any* 스킬 노드에 대해, `hitCount`/`critBonus` 부재 시 기본값(1/0)으로 로드되고, `hitCount < 1` 또는 `critBonus ∉ [0,100]` 또는 숫자 아님이면 `SkillDataException`이 발생하며, `multiplierByRank` 16키·단조 검증은 유지된다.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.6**

### Property 7: 멀티히트 로그 포맷

*For any* `playerHits`에 대해, size ≥ 2면 헤더(`{스킬}({타입}) {N}연타`)+브레이크다운(각 히트 피해 나열, 크리 히트에 `(치명)`, 끝에 `= {합계} 피해`)을 생성하고, size ≤ 1이면 기존 단일 형식(`{스킬}({타입})로 … {N} 피해`(+`(크리티컬!)`))을 생성한다.

**Validates: Requirements 7.1, 7.2, 7.3, 6.5**

### Property 8: 액션↔결산 로그 라우팅

*For any* 턴에 대해, 전투 액션 라인(플레이어/몬스터 행동·선제·캐스팅 실패·도망 실패)은 `BattleTurnResult.combatLines`에 담기고 화면 하단 `ActionLog`에는 추가되지 않으며, 결산/사망/도망 성공 라인은 `ActionLog`에 추가된다.

**Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5**

### Property 9: 몬스터·반격 단일 히트 불변

*For any* 입력에 대해, 몬스터 피해·방어 반격 피해는 `hitCount`와 무관하게 단일 값으로 산출되고, 해당 경로의 `playerHits`(반격/0/교착)는 비어 있다.

**Validates: Requirements 4.6, 6.4**

### Property 10: skill.json 데이터 규격

*For any* 로드된 딜 스킬 카탈로그에 대해, 9개 스킬의 `hitCount`/`critBonus`가 §4 확정표와 일치하고, 모든 마법 스킬 `critBonus == 0`, 어떤 스킬도 `critBonus > 100` 아님, 모든 `multiplierByRank`가 16키·단조 비감소, 다단 총 배율(per-hit×hitCount)이 명시 밴드(3타 105→195, 4타 108→200) 안이다.

**Validates: Requirements 3.1, 3.2, 3.4, 3.5, 3.6, 12.2, 12.4, 12.5**

## Error Handling

| 상황 | 처리 |
|---|---|
| `hitCount` 숫자 아님/`< 1`(Req 2.2) | `SkillDataException`(로드 실패) |
| `critBonus` 숫자 아님/`∉[0,100]`(Req 2.3) | `SkillDataException`(로드 실패) |
| `hitCount`/`critBonus` 미지정(Req 2.1) | 기본값 1/0으로 로드(관용) |
| 방어 스킬 노드에 두 필드 존재(Req 2.5) | 무시(하위호환) |
| 실효 크리 > 1000(Req 5.5) | `min(1000, …)` 보정(항상 크리) |
| 플레이어 피해 0(캐스팅 실패·상성 패배·교착)(Req 6.3) | `playerHits` 비움, 로그는 기존 빗나감/실패 문구 |

- 커스텀 예외(`SkillDataException`)만 사용, `RuntimeException` 직접 금지(code-style).

## Testing Strategy

### 이중 테스트 접근

- **프로퍼티 테스트(jqwik)**: 위 Correctness Property 10개. `@Property(tries = 100)`, `@Mock` 금지(`Mockito.mock()` 직접), 태그 주석 `Feature: 009-skill-differentiation-and-battle-log, Property {번호}: {텍스트}`. 난수 로직은 시드 고정 `Random`.
  - `BattleResolverSingleHitBackwardCompatPropertyTest`(P1)
  - `BattleResolverMultiHitSumPropertyTest`(P2)
  - `BattleResolverMultiHitDefensePropertyTest`(P3)
  - `BattleResolverMultiHitDeterminismPropertyTest`(P4)
  - `BattleServiceEffectiveCriticalPropertyTest`(P5)
  - `SkillCatalogHitCountCritBonusPropertyTest`(P6)
  - `BattleLogFormatterMultiHitPropertyTest`(P7)
  - `BattleServiceLogRoutingPropertyTest`(P8)
  - `BattleResolverMonsterSingleHitPropertyTest`(P9)
  - `SkillCatalogDataConformancePropertyTest`(P10)
- **단위/예시 테스트**:
  - `DamageSkillTest`: 보조 생성자(7-인자→hitCount1/critBonus0), 9-인자 생성.
  - `BattleResolverTest`[확장]: 멀티히트 3타/4타 예시, 고방어 폭락 예시, 단일 동치.
  - `BattleLogFormatterTest`[확장]: C안 멀티히트 헤더+브레이크다운, 크리 `(치명)`, 단일 형식 보존.
- **서비스 통합**(Mockito verify):
  - `BattleServiceLogSplitTest`: 액션 라인은 `combatLines`, 결산/사망 라인만 `actionLog.add` 호출(verify), 시작 로그 하단 미추가.
  - `BattleServiceCritBonusTest`: `smash`/`magnum_shot` 실효 크리 = 캐릭터+보너스, 마법 스킬 +0.
- **카탈로그 로드**(`SkillCatalogService`): 기본값·범위 검증·기존 로드 무회귀.
- **컨트롤러 슬라이스**(`@WebMvcTest`+`@MockitoBean`):
  - `BattleControllerTest`[확장]: `/battle/turn` 응답에 `turnLog`(중앙 로그) 모델·`battle-log` 렌더, start 인트로 라인.
- **정적 리소스 보존**(`VisualJsPreservationAndJsonLoadingIntegrationTest`[확장]): `battle-view.html`(`battle-center` 서브프래그먼트·`battle-log` 섹션) 기대값 갱신, `skill.json` 로드.
- **회귀**: 008/005 기존 테스트(단일 히트 스킬 사용, `DamageSkill` 7-인자 생성부)가 보조 생성자·기본값으로 무회귀 통과.

### 생성기(Arbitraries)

- per-hit 배율·방어·상성계수·critical·hitCount(1~8)·시드 생성기(P1~P4).
- 캐릭터 크리 × critBonus(0~100) 경계 생성기(P5, 합 1000 초과 경계 포함).
- hitCount/critBonus 노드 값 생성기(P6, 경계 0/1/100/101/음수/비숫자).
- `HitResult` 목록 생성기(P7, size 0/1/2/3/4, 크리 혼합).

### 빌드 검증

- 각 구현 Task 완료 전 `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS`(steering `task-build-validation.md`). 소스 수정 Task는 미사용 import/변수 정리·매직넘버 상수화·메서드 분리 등 `code-style` 정리 항목 완료 후 처리.

## Migration 영향 범위 (기존 산출물)

- **`DamageSkill`**: 9-인자 record + 7-인자 보조 생성자. 기존 `multiplierByRank()` 등 접근자·의미 보존. 기존 7-인자 호출부(파서·테스트) 무회귀.
- **`SkillCatalogService`**: `parseDamageSkill`에 두 필드 optional 파싱 추가. 기존 검증(16키·단조·중복 id) 유지.
- **`BattleResolver`**: `multiHitDamage` 추가 + 3개 공격 경로 멀티히트화. `hitCount==1`·`critBonus==0`에서 난수 시퀀스·결과 불변(기존 008 프로퍼티/단위 테스트 무회귀).
- **`TurnInput`/`ResolvedTurn`/`BattleTurnResult`/`BattleLogInput`**: 컴포넌트 추가(끝에). 기존 컴포넌트 순서·의미 보존, 호출부 갱신.
- **`BattleService`**: `resolvePlayerCritical`(스킬 critBonus)·`resolvePlayerHitCount`·멀티히트 선제·로그 분리. 턴 순서/자원/사망/보상 로직 무변경.
- **`BattleLogFormatter`**: 멀티히트 분기 추가, 기존 단일/방어/몬스터/실패 문구 보존.
- **`BattleController`**: `turnLog` 모델 속성 추가. 엔드포인트 시그니처 무변경.
- **`battle-view.html`**: `battle-center` 공용 서브프래그먼트 추출 + `battle-log` 섹션. `action-log.html`·하단 footer 무변경(결산 로그만 하단).
- **`skill.json`**: 9개 딜 스킬 필드/배율 갱신. 방어 스킬 무변경.
- **`data-balance-guide.md`**: 이미 §0/§4에 반영됨(본 스펙은 그 설계의 구현).
